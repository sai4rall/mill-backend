package com.millsystem.dto;

import com.millsystem.entity.PaddyPurchase;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PurchaseDto {
    private Long id;
    @NotNull private Long farmerId;
    private String farmerName;
    @NotNull private LocalDate purchaseDate;
    @NotNull @Positive private BigDecimal weightQuintals;
    @NotNull @Positive private BigDecimal pricePerQuintal;
    private BigDecimal totalAmount;
    private String truckNumber;
    private PaddyPurchase.PaymentStatus paymentStatus;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private String notes;
    private LocalDateTime createdAt;

    public static PurchaseDto from(PaddyPurchase p, BigDecimal amountPaid) {
        PurchaseDto dto = new PurchaseDto();
        dto.id = p.getId(); dto.farmerId = p.getFarmer().getId(); dto.farmerName = p.getFarmer().getName();
        dto.purchaseDate = p.getPurchaseDate(); dto.weightQuintals = p.getWeightQuintals();
        dto.pricePerQuintal = p.getPricePerQuintal(); dto.totalAmount = p.getTotalAmount();
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
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getWeightQuintals() { return weightQuintals; }
    public void setWeightQuintals(BigDecimal weightQuintals) { this.weightQuintals = weightQuintals; }
    public BigDecimal getPricePerQuintal() { return pricePerQuintal; }
    public void setPricePerQuintal(BigDecimal pricePerQuintal) { this.pricePerQuintal = pricePerQuintal; }
    public BigDecimal getTotalAmount() { return totalAmount; }
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
