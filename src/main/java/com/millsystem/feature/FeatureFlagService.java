package com.millsystem.feature;

import com.millsystem.dto.FeatureFlagDto;
import com.millsystem.dto.FeatureFlagUpdateRequest;
import com.millsystem.dto.MillFeatureFlagDto;
import com.millsystem.entity.Feature;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillFeatureOverride;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.FeatureRepository;
import com.millsystem.repository.MillFeatureOverrideRepository;
import com.millsystem.repository.MillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Global feature flags with optional per-mill overrides. */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureRepository featureRepository;
    private final MillFeatureOverrideRepository overrideRepository;
    private final MillRepository millRepository;

    public FeatureFlagService(FeatureRepository featureRepository, MillFeatureOverrideRepository overrideRepository,
                              MillRepository millRepository) {
        this.featureRepository = featureRepository;
        this.overrideRepository = overrideRepository;
        this.millRepository = millRepository;
    }

    /** Inserts any {@link FeatureKey} missing from the table (shown + enabled). Existing rows are untouched. */
    @Transactional
    public void seedMissing() {
        Set<String> existing = featureRepository.findAll().stream().map(Feature::getFeatureKey).collect(Collectors.toSet());
        for (FeatureKey k : FeatureKey.values()) {
            if (!existing.contains(k.key())) {
                Feature f = new Feature();
                f.setFeatureKey(k.key());
                f.setName(k.label());
                featureRepository.save(f);
                log.info("Seeded feature flag '{}' (shown, enabled)", k.key());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getGlobal() {
        return sortedFeatures().stream()
                .map(f -> new FeatureFlagDto(f.getFeatureKey(), f.getName(), f.getDescription(), f.isShowInMenu(), f.isEnabled()))
                .collect(Collectors.toList());
    }

    /** Effective flags for a mill (global values with the mill's overrides applied). Null millId → global. */
    @Transactional(readOnly = true)
    public List<FeatureFlagDto> getEffective(Long millId) {
        if (millId == null) return getGlobal();
        return getMillFlags(millId).stream()
                .map(m -> new FeatureFlagDto(m.getKey(), m.getName(), null, m.isShowInMenu(), m.isEnabled()))
                .collect(Collectors.toList());
    }

    /** key → effective "enabled" for a mill. Used for API enforcement. */
    @Transactional(readOnly = true)
    public Map<String, Boolean> enabledMap(Long millId) {
        return getEffective(millId).stream().collect(Collectors.toMap(FeatureFlagDto::getKey, FeatureFlagDto::isEnabled));
    }

    /** Effective "enabled" for one feature at a mill (unknown features count as enabled). */
    @Transactional(readOnly = true)
    public boolean isEnabled(Long millId, FeatureKey key) {
        return enabledMap(millId).getOrDefault(key.key(), true);
    }

    @Transactional(readOnly = true)
    public List<MillFeatureFlagDto> getMillFlags(Long millId) {
        Map<String, MillFeatureOverride> overrides = overrideRepository.findByMillId(millId).stream()
                .collect(Collectors.toMap(o -> o.getFeature().getFeatureKey(), Function.identity()));
        return sortedFeatures().stream().map(f -> {
            MillFeatureOverride o = overrides.get(f.getFeatureKey());
            return new MillFeatureFlagDto(f.getFeatureKey(), f.getName(), f.isShowInMenu(), f.isEnabled(),
                    o == null ? null : o.getShowInMenu(), o == null ? null : o.getEnabled());
        }).collect(Collectors.toList());
    }

    /** Updates the global flags; null fields are left unchanged. */
    @Transactional
    public FeatureFlagDto updateGlobal(String key, FeatureFlagUpdateRequest req) {
        Feature f = findFeature(key);
        if (req.getShowInMenu() != null) f.setShowInMenu(req.getShowInMenu());
        if (req.getEnabled() != null) f.setEnabled(req.getEnabled());
        f = featureRepository.save(f);
        log.info("Global feature '{}' set to showInMenu={}, enabled={}", key, f.isShowInMenu(), f.isEnabled());
        return new FeatureFlagDto(f.getFeatureKey(), f.getName(), f.getDescription(), f.isShowInMenu(), f.isEnabled());
    }

    /** Sets a mill's override; null fields inherit the global value. Both null removes the override. */
    @Transactional
    public MillFeatureFlagDto setOverride(Long millId, String key, FeatureFlagUpdateRequest req) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + millId));
        Feature feature = findFeature(key);
        Optional<MillFeatureOverride> existing = overrideRepository.findByMillIdAndFeatureFeatureKey(millId, key);

        if (req.getShowInMenu() == null && req.getEnabled() == null) {
            existing.ifPresent(overrideRepository::delete);
        } else {
            MillFeatureOverride o = existing.orElseGet(() -> {
                MillFeatureOverride n = new MillFeatureOverride();
                n.setMill(mill);
                n.setFeature(feature);
                return n;
            });
            o.setShowInMenu(req.getShowInMenu());
            o.setEnabled(req.getEnabled());
            overrideRepository.save(o);
        }
        log.info("Mill {} feature '{}' override set to showInMenu={}, enabled={}",
                millId, key, req.getShowInMenu(), req.getEnabled());
        return new MillFeatureFlagDto(feature.getFeatureKey(), feature.getName(), feature.isShowInMenu(), feature.isEnabled(),
                req.getShowInMenu(), req.getEnabled());
    }

    @Transactional
    public void clearOverride(Long millId, String key) {
        findFeature(key);
        overrideRepository.findByMillIdAndFeatureFeatureKey(millId, key).ifPresent(overrideRepository::delete);
    }

    private Feature findFeature(String key) {
        return featureRepository.findById(key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + key));
    }

    /** Features in {@link FeatureKey} declaration order (menu order); unknown DB rows go last. */
    private List<Feature> sortedFeatures() {
        List<String> order = Arrays.stream(FeatureKey.values()).map(FeatureKey::key).toList();
        return featureRepository.findAll().stream()
                .sorted(Comparator.comparingInt(f -> {
                    int i = order.indexOf(f.getFeatureKey());
                    return i < 0 ? Integer.MAX_VALUE : i;
                }))
                .collect(Collectors.toList());
    }
}
