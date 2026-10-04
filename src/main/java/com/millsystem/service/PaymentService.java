package com.millsystem.service;

import com.millsystem.dto.PaymentDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.entity.Payment;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaddyPurchaseRepository purchaseRepository;
    private final MillRepository millRepository;

    public PaymentService(PaymentRepository paymentRepository, PaddyPurchaseRepository purchaseRepository,
                          MillRepository millRepository) {
        this.paymentRepository = paymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.millRepository = millRepository;
    }

    public List<PaymentDto> getByPurchase(Long purchaseId, Long millId) {
        return paymentRepository.findByPurchaseIdAndMillIdOrderByPaymentDateDesc(purchaseId, millId)
                .stream().map(PaymentDto::from).collect(Collectors.toList());
    }

    @Transactional
    public PaymentDto create(PaymentDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        PaddyPurchase purchase = purchaseRepository.findByIdAndMillId(dto.getPurchaseId(), millId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + dto.getPurchaseId()));

        Payment payment = new Payment();
        payment.setMill(mill); payment.setPurchase(purchase);
        payment.setAmount(dto.getAmount()); payment.setPaymentDate(dto.getPaymentDate());
        if (dto.getPaymentMode() != null) payment.setPaymentMode(dto.getPaymentMode());
        payment.setReferenceNumber(dto.getReferenceNumber()); payment.setNotes(dto.getNotes());
        paymentRepository.save(payment);

        BigDecimal totalPaid = paymentRepository.sumAmountByPurchaseId(purchase.getId());
        if (totalPaid.compareTo(purchase.getTotalAmount()) >= 0) {
            purchase.setPaymentStatus(PaddyPurchase.PaymentStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus(PaddyPurchase.PaymentStatus.PARTIAL);
        }
        purchaseRepository.save(purchase);
        return PaymentDto.from(payment);
    }
}
