package com.millsystem.dto;

import com.millsystem.validation.NotFutureDate;
import com.millsystem.entity.PaddyPurchase;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PurchaseDto {
    private Long id;
    @NotNull private Long farmerId;
    private String farmerName;
    // FARMER, AGENT or SUPPLIER: who the paddy was bought from (for vendor / agent-wise reports).
    private String farmerType;
    @NotNull @NotFutureDate(message = "Purchase date cannot be in the future") private LocalDate purchaseDate;
    @NotNull @Positive @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places") private BigDecimal weightQuintals;
    @NotNull @Positive @Digits(integer = 8, fraction = 2, message = "Price can have at most 8 digits and 2 decimal places") private BigDecimal pricePerQuintal;
    // Required for new purchases while the moisture_deduction feature is enabled for the mill (checked in
    // PurchaseService); drives the deduction looked up from the moisture table.
    @DecimalMin(value = "0.00", message = "Moisture % must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "Moisture % must be between 0 and 100")
    @Digits(integer = 3, fraction = 2, message = "Moisture % can have at most 2 decimals")
    @Digits(integer = 3, fraction = 2, message = "Percentage can have at most 2 decimal places")
    private BigDecimal moisturePct;
    private BigDecimal deductionPct;     // read-only: stored at purchase time
    private BigDecimal grossAmount;      // read-only: weight × price
    private BigDecimal deductionAmount;  // read-only: gross − total
    private BigDecimal totalAmount;      // net payable after deduction
    @Size(max = 20, message = "Must be at most 20 characters")
    private String truckNumber;
    // Read-only: derived from payments (see PaymentService); ignored if a client sends it.
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    private PaddyPurchase.PaymentStatus paymentStatus;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    @Size(max = 2000, message = "Must be at most 2000 characters")
    private String notes;
    private LocalDateTime createdAt;

    public static PurchaseDto from(PaddyPurchase p, BigDecimal amountPaid) {
        PurchaseDto dto = new PurchaseDto();
        dto.id = p.getId(); dto.farmerId = p.getFarmer().getId(); dto.farmerName = p.getFarmer().getName(); dto.farmerType = p.getFarmer().getType().name();
        dto.purchaseDate = p.getPurchaseDate(); dto.weightQuintals = p.getWeightQuintals();
        dto.pricePerQuintal = p.getPricePerQuintal(); dto.totalAmount = p.getTotalAmount();
        dto.moisturePct = p.getMoisturePct(); dto.deductionPct = p.getDeductionPct();
        dto.grossAmount = p.getWeightQuintals().multiply(p.getPricePerQuintal()).setScale(2, RoundingMode.HALF_UP);
        dto.deductionAmount = dto.grossAmount.subtract(p.getTotalAmount());
        dto.truckNumber = p.getTruckNumber();
        dto.paymentStatus = p.getPaymentStatus(); dto.amountPaid = amountPaid;
        dto.amountDue = p.getTotalAmount().subtract(amountPaid);
        dto.notes = p.getNotes(); dto.createdAt = p.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getFarmerId() { return farmerId; }
    public void setFarmerId(Long farmerId) { this.farmerId = farmerId; }
    public String getFarmerName() { return farmerName; }
    public String getFarmerType() { return farmerType; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getWeightQuintals() { return weightQuintals; }
    public void setWeightQuintals(BigDecimal weightQuintals) { this.weightQuintals = weightQuintals; }
    public BigDecimal getPricePerQuintal() { return pricePerQuintal; }
    public void setPricePerQuintal(BigDecimal pricePerQuintal) { this.pricePerQuintal = pricePerQuintal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getMoisturePct() { return moisturePct; }
    public void setMoisturePct(BigDecimal moisturePct) { this.moisturePct = moisturePct; }
    public BigDecimal getDeductionPct() { return deductionPct; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getDeductionAmount() { return deductionAmount; }
    public PaddyPurchase.PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaddyPurchase.PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getAmountDue() { return amountDue; }
    public String getTruckNumber() { return truckNumber; }
    public void setTruckNumber(String truckNumber) { this.truckNumber = truckNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
