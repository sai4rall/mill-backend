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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    private static final long MILL_ID = 3L;

    @Mock private FeatureRepository featureRepository;
    @Mock private MillFeatureOverrideRepository overrideRepository;
    @Mock private MillRepository millRepository;
    private FeatureFlagService service;
    private Mill mill;

    @BeforeEach
    void setUp() {
        service = new FeatureFlagService(featureRepository, overrideRepository, millRepository);
        mill = new Mill();
        mill.setId(MILL_ID);
        lenient().when(featureRepository.save(any(Feature.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static Feature feature(String key, boolean show, boolean enabled) {
        Feature f = new Feature();
        f.setFeatureKey(key);
        f.setName(key.toUpperCase());
        f.setShowInMenu(show);
        f.setEnabled(enabled);
        return f;
    }

    private static MillFeatureOverride override(Feature f, Boolean show, Boolean enabled) {
        MillFeatureOverride o = new MillFeatureOverride();
        o.setFeature(f);
        o.setShowInMenu(show);
        o.setEnabled(enabled);
        return o;
    }

    @Test
    void seedsOnlyMissingFeaturesAsShownAndEnabled() {
        // "farmers" already exists and is disabled: it must not be touched.
        when(featureRepository.findAll()).thenReturn(List.of(feature("farmers", false, false)));

        service.seedMissing();

        ArgumentCaptor<Feature> saved = ArgumentCaptor.forClass(Feature.class);
        verify(featureRepository, times(FeatureKey.values().length - 1)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Feature::getFeatureKey).doesNotContain("farmers");
        assertThat(saved.getAllValues()).allSatisfy(f -> {
            assertThat(f.isShowInMenu()).isTrue();
            assertThat(f.isEnabled()).isTrue();
        });
        assertThat(saved.getAllValues()).extracting(Feature::getName).contains("Truck Tracking");
    }

    @Test
    void globalFlagsAreReturnedInMenuOrder() {
        when(featureRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                feature("operators", true, true), feature("custom_extra", true, true), feature("dashboard", true, false))));

        assertThat(service.getGlobal()).extracting(FeatureFlagDto::getKey)
                .containsExactly("dashboard", "operators", "custom_extra");
    }

    @Test
    void effectiveFlagsApplyMillOverridesFieldByField() {
        Feature farmers = feature("farmers", true, true);
        Feature trucks = feature("truck_tracking", true, false);
        Feature stock = feature("stock_report", false, true);
        when(featureRepository.findAll()).thenReturn(List.of(farmers, trucks, stock));
        when(overrideRepository.findByMillId(MILL_ID)).thenReturn(List.of(
                override(farmers, null, false),   // disable only; menu inherits (shown)
                override(trucks, null, true)));    // pilot: enabled for this mill only

        var byKey = service.getEffective(MILL_ID).stream()
                .collect(java.util.stream.Collectors.toMap(FeatureFlagDto::getKey, f -> f));

        assertThat(byKey.get("farmers").isShowInMenu()).isTrue();
        assertThat(byKey.get("farmers").isEnabled()).isFalse();
        assertThat(byKey.get("truck_tracking").isEnabled()).isTrue();
        assertThat(byKey.get("stock_report").isShowInMenu()).isFalse();   // no override → global
        assertThat(byKey.get("stock_report").isEnabled()).isTrue();
    }

    @Test
    void effectiveFlagsWithoutMillAreGlobal() {
        when(featureRepository.findAll()).thenReturn(List.of(feature("farmers", true, false)));
        assertThat(service.getEffective(null)).singleElement()
                .satisfies(f -> assertThat(f.isEnabled()).isFalse());
        verifyNoInteractions(overrideRepository);
    }

    @Test
    void enabledMapReflectsEffectiveValues() {
        Feature farmers = feature("farmers", true, true);
        when(featureRepository.findAll()).thenReturn(List.of(farmers, feature("purchases", true, true)));
        when(overrideRepository.findByMillId(MILL_ID)).thenReturn(List.of(override(farmers, null, false)));

        assertThat(service.enabledMap(MILL_ID)).containsEntry("farmers", false).containsEntry("purchases", true);
    }

    @Test
    void millFlagsExposeGlobalOverrideAndEffective() {
        Feature farmers = feature("farmers", true, true);
        when(featureRepository.findAll()).thenReturn(List.of(farmers));
        when(overrideRepository.findByMillId(MILL_ID)).thenReturn(List.of(override(farmers, false, null)));

        MillFeatureFlagDto f = service.getMillFlags(MILL_ID).get(0);
        assertThat(f.isGlobalShowInMenu()).isTrue();
        assertThat(f.getOverrideShowInMenu()).isFalse();
        assertThat(f.getOverrideEnabled()).isNull();
        assertThat(f.isShowInMenu()).isFalse();
        assertThat(f.isEnabled()).isTrue();
    }

    @Test
    void globalUpdateChangesOnlyProvidedFields() {
        Feature f = feature("farmers", true, true);
        when(featureRepository.findById("farmers")).thenReturn(Optional.of(f));
        FeatureFlagUpdateRequest req = new FeatureFlagUpdateRequest();
        req.setEnabled(false);

        FeatureFlagDto result = service.updateGlobal("farmers", req);

        assertThat(result.isEnabled()).isFalse();
        assertThat(result.isShowInMenu()).isTrue();
    }

    @Test
    void unknownFeatureIsNotFound() {
        when(featureRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateGlobal("nope", new FeatureFlagUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void overrideIsCreatedForMill() {
        Feature f = feature("farmers", true, true);
        when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
        when(featureRepository.findById("farmers")).thenReturn(Optional.of(f));
        when(overrideRepository.findByMillIdAndFeatureFeatureKey(MILL_ID, "farmers")).thenReturn(Optional.empty());
        FeatureFlagUpdateRequest req = new FeatureFlagUpdateRequest();
        req.setEnabled(false);

        MillFeatureFlagDto result = service.setOverride(MILL_ID, "farmers", req);

        ArgumentCaptor<MillFeatureOverride> saved = ArgumentCaptor.forClass(MillFeatureOverride.class);
        verify(overrideRepository).save(saved.capture());
        assertThat(saved.getValue().getMill()).isSameAs(mill);
        assertThat(saved.getValue().getEnabled()).isFalse();
        assertThat(saved.getValue().getShowInMenu()).isNull();
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.isShowInMenu()).isTrue();
    }

    @Test
    void overrideWithBothNullIsRemoved() {
        Feature f = feature("farmers", true, true);
        MillFeatureOverride existing = override(f, false, false);
        when(millRepository.findById(MILL_ID)).thenReturn(Optional.of(mill));
        when(featureRepository.findById("farmers")).thenReturn(Optional.of(f));
        when(overrideRepository.findByMillIdAndFeatureFeatureKey(MILL_ID, "farmers")).thenReturn(Optional.of(existing));

        service.setOverride(MILL_ID, "farmers", new FeatureFlagUpdateRequest());

        verify(overrideRepository).delete(existing);
        verify(overrideRepository, never()).save(any());
    }

    @Test
    void overrideForUnknownMillIsNotFound() {
        when(millRepository.findById(MILL_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setOverride(MILL_ID, "farmers", new FeatureFlagUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void allFeatureKeysAreUniqueAndSnakeCase() {
        assertThat(Arrays.stream(FeatureKey.values()).map(FeatureKey::key))
                .doesNotHaveDuplicates()
                .allMatch(k -> k.matches("[a-z]+(_[a-z]+)*"));
    }
}
