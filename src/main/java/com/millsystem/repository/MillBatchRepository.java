package com.millsystem.repository;

import com.millsystem.entity.MillBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MillBatchRepository extends JpaRepository<MillBatch, Long> {
    List<MillBatch> findByMillIdOrderByBatchDateDesc(Long millId);
    List<MillBatch> findByMillIdOrderByBatchDateAsc(Long millId);
    List<MillBatch> findByMillIdAndBatchDateBetweenOrderByBatchDateAsc(Long millId, LocalDate from, LocalDate to);
    Optional<MillBatch> findByIdAndMillId(Long id, Long millId);

    @Query("SELECT COALESCE(SUM(b.actualRiceQuintals), 0) FROM MillBatch b WHERE b.mill.id = :millId AND b.status = 'COMPLETED'")
    BigDecimal sumActualRiceByMillId(Long millId);
}
