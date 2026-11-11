package com.millsystem.repository;

import com.millsystem.entity.QualityTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QualityTestRepository extends JpaRepository<QualityTest, Long> {
    List<QualityTest> findByMillIdOrderByTestDateDescIdDesc(Long millId);
    List<QualityTest> findByMillIdAndBatchIdOrderByTestDateDescIdDesc(Long millId, Long batchId);
    Optional<QualityTest> findByIdAndMillId(Long id, Long millId);
    long countByMillIdAndCertificateNumberStartingWith(Long millId, String prefix);
}
