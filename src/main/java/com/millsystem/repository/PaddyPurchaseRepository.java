package com.millsystem.repository;

import com.millsystem.entity.PaddyPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaddyPurchaseRepository extends JpaRepository<PaddyPurchase, Long> {
    List<PaddyPurchase> findByMillIdOrderByPurchaseDateDesc(Long millId);
    List<PaddyPurchase> findByMillIdOrderByPurchaseDateAsc(Long millId);
    List<PaddyPurchase> findByMillIdAndPurchaseDateBetweenOrderByPurchaseDateAsc(Long millId, LocalDate from, LocalDate to);
    List<PaddyPurchase> findByMillIdAndFarmerIdOrderByPurchaseDateDesc(Long millId, Long farmerId);
    Optional<PaddyPurchase> findByIdAndMillId(Long id, Long millId);

    @Query("SELECT COALESCE(SUM(p.weightQuintals), 0) FROM PaddyPurchase p WHERE p.mill.id = :millId")
    BigDecimal sumWeightByMillId(Long millId);
}
