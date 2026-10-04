package com.millsystem.repository;

import com.millsystem.entity.MillBatchPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface MillBatchPurchaseRepository extends JpaRepository<MillBatchPurchase, Long> {

    @Query("SELECT COALESCE(SUM(mbp.quintalsUsed), 0) FROM MillBatchPurchase mbp WHERE mbp.batch.mill.id = :millId")
    BigDecimal sumQuintalsUsedByMillId(Long millId);

    @Query("SELECT COALESCE(SUM(mbp.quintalsUsed), 0) FROM MillBatchPurchase mbp WHERE mbp.batch.mill.id = :millId AND mbp.batch.status = 'IN_PROGRESS'")
    BigDecimal sumQuintalsInProgressByMillId(Long millId);
}
