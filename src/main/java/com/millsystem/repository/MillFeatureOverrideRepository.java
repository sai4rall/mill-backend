package com.millsystem.repository;

import com.millsystem.entity.MillFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MillFeatureOverrideRepository extends JpaRepository<MillFeatureOverride, Long> {
    List<MillFeatureOverride> findByMillId(Long millId);
    Optional<MillFeatureOverride> findByMillIdAndFeatureFeatureKey(Long millId, String featureKey);
}
