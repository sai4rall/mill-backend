package com.millsystem.config;

import com.millsystem.entity.PaddyPurchase;
import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.repository.PaymentRepository;
import com.millsystem.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time data repair on startup: purchase payment status used to be editable through PUT /api/purchases/{id},
 * so a purchase could say PAID with nothing paid. Recompute every status from the recorded payments.
 * Idempotent: purchases that are already correct are left alone.
 */
@Component
@Order(100)
public class PaymentStatusReconciler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusReconciler.class);
    private final PaddyPurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;

    public PaymentStatusReconciler(PaddyPurchaseRepository purchaseRepository, PaymentRepository paymentRepository) {
        this.purchaseRepository = purchaseRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int fixed = 0;
        for (PaddyPurchase p : purchaseRepository.findAll()) {
            PaddyPurchase.PaymentStatus actual = PaymentService.statusFor(paymentRepository.sumAmountByPurchaseId(p.getId()), p.getTotalAmount());
            if (p.getPaymentStatus() != actual) {
                log.warn("Purchase #{} payment status {} did not match its payments; corrected to {}", p.getId(), p.getPaymentStatus(), actual);
                p.setPaymentStatus(actual);
                purchaseRepository.save(p);
                fixed++;
            }
        }
        if (fixed > 0) log.warn("Payment status reconciliation corrected {} purchase(s)", fixed);
    }
}
