package com.millsystem.repository;

import com.millsystem.entity.MoistureDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoistureDeductionRepository extends JpaRepository<MoistureDeduction, Long> {
    List<MoistureDeduction> findByMillIsNullOrderByFromPctAsc();
    List<MoistureDeduction> findByMillIdOrderByFromPctAsc(Long millId);

    @Modifying
    @Query("DELETE FROM MoistureDeduction d WHERE d.mill IS NULL")
    void deleteGlobal();

    @Modifying
    @Query("DELETE FROM MoistureDeduction d WHERE d.mill.id = :millId")
    void deleteByMillId(Long millId);
}
