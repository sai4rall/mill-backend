package com.millsystem.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class PaymentOtpRequest {
    @NotNull private Long purchaseId;
    @NotNull @Positive @Digits(integer = 10, fraction = 2, message = "Amount can have at most 10 digits and 2 decimal places") private BigDecimal amount;

    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
