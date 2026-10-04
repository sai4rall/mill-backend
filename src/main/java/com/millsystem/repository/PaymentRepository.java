package com.millsystem.repository;

import com.millsystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPurchaseIdAndMillIdOrderByPaymentDateDesc(Long purchaseId, Long millId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.purchase.id = :purchaseId")
    BigDecimal sumAmountByPurchaseId(Long purchaseId);
}
