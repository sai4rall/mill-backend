package com.millsystem.repository;

import com.millsystem.entity.QualityGradeLimit;
import com.millsystem.entity.QualityTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityGradeLimitRepository extends JpaRepository<QualityGradeLimit, Long> {
    List<QualityGradeLimit> findByOwnerIdAndProduct(Long ownerId, QualityTest.Product product);

    @Modifying
    @Query("DELETE FROM QualityGradeLimit g WHERE g.ownerId = :ownerId AND g.product = :product")
    void deleteStandard(Long ownerId, QualityTest.Product product);
}
