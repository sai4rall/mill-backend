package com.millsystem.dto;

import jakarta.validation.constraints.*;
import com.millsystem.validation.NotFutureDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.millsystem.entity.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentDto {
    private Long id;
    @NotNull private Long purchaseId;
    @NotNull @Positive @Digits(integer = 10, fraction = 2, message = "Amount can have at most 10 digits and 2 decimal places") private BigDecimal amount;
    @NotNull @NotFutureDate(message = "Payment date cannot be in the future") private LocalDate paymentDate;
    private Payment.PaymentMode paymentMode;
    @Size(max = 100, message = "Must be at most 100 characters")
    private String referenceNumber;
    @Size(max = 2000, message = "Must be at most 2000 characters")
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime otpVerifiedAt;
    // OTP the farmer received for this exact purchase + amount (POST /api/payments/otp/send). Never returned.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String otp;

    public static PaymentDto from(Payment p) {
        PaymentDto dto = new PaymentDto();
        dto.id = p.getId(); dto.purchaseId = p.getPurchase().getId(); dto.amount = p.getAmount();
        dto.paymentDate = p.getPaymentDate(); dto.paymentMode = p.getPaymentMode();
        dto.referenceNumber = p.getReferenceNumber(); dto.notes = p.getNotes(); dto.createdAt = p.getCreatedAt();
        dto.otpVerifiedAt = p.getOtpVerifiedAt();
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
    public LocalDateTime getOtpVerifiedAt() { return otpVerifiedAt; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
