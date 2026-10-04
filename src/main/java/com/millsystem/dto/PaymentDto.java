package com.millsystem.dto;

import com.millsystem.entity.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentDto {
    private Long id;
    @NotNull private Long purchaseId;
    @NotNull @Positive private BigDecimal amount;
    @NotNull private LocalDate paymentDate;
    private Payment.PaymentMode paymentMode;
    private String referenceNumber;
    private String notes;
    private LocalDateTime createdAt;

    public static PaymentDto from(Payment p) {
        PaymentDto dto = new PaymentDto();
        dto.id = p.getId(); dto.purchaseId = p.getPurchase().getId(); dto.amount = p.getAmount();
        dto.paymentDate = p.getPaymentDate(); dto.paymentMode = p.getPaymentMode();
        dto.referenceNumber = p.getReferenceNumber(); dto.notes = p.getNotes(); dto.createdAt = p.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public Payment.PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(Payment.PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
