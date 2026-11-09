package com.millsystem.dto;

import jakarta.validation.constraints.*;
import com.millsystem.validation.NotFutureDate;
import com.millsystem.entity.MillBatch;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MillBatchDto {
    private Long id;
    @NotNull @NotFutureDate(message = "Batch date cannot be in the future") private LocalDate batchDate;
    @NotNull @Positive @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places") private BigDecimal paddyInputQuintals;
    @PositiveOrZero @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places")
    private BigDecimal expectedRiceQuintals;
    @PositiveOrZero @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places")
    private BigDecimal actualRiceQuintals;
    private BigDecimal conversionRate;
    /** Duty shift: MORNING, AFTERNOON or NIGHT. */
    private com.millsystem.entity.Shift shift;
    @PositiveOrZero @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places")
    private BigDecimal brokenRiceQuintals;
    @PositiveOrZero @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places")
    private BigDecimal branQuintals;
    @PositiveOrZero @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places")
    private BigDecimal huskQuintals;
    /** Read-only: paddy in minus rice and by-products, once completed. */
    private BigDecimal unaccountedQuintals;
    private MillBatch.BatchStatus status;
    @Size(max = 2000, message = "Must be at most 2000 characters")
    private String notes;
    private List<BatchPurchaseEntry> purchaseEntries;
    private LocalDateTime createdAt;

    public static class BatchPurchaseEntry {
        private Long purchaseId;
        @Digits(integer = 8, fraction = 2, message = "Quantity can have at most 8 digits and 2 decimal places")
        private BigDecimal quintalsUsed;
        public Long getPurchaseId() { return purchaseId; }
        public void setPurchaseId(Long purchaseId) { this.purchaseId = purchaseId; }
        public BigDecimal getQuintalsUsed() { return quintalsUsed; }
        public void setQuintalsUsed(BigDecimal quintalsUsed) { this.quintalsUsed = quintalsUsed; }
    }

    public static MillBatchDto from(MillBatch b) {
        MillBatchDto dto = new MillBatchDto();
        dto.id = b.getId(); dto.batchDate = b.getBatchDate(); dto.paddyInputQuintals = b.getPaddyInputQuintals();
        dto.expectedRiceQuintals = b.getExpectedRiceQuintals(); dto.actualRiceQuintals = b.getActualRiceQuintals();
        dto.conversionRate = b.getConversionRate(); dto.status = b.getStatus(); dto.shift = b.getShift();
        dto.brokenRiceQuintals = b.getBrokenRiceQuintals(); dto.branQuintals = b.getBranQuintals(); dto.huskQuintals = b.getHuskQuintals();
        dto.unaccountedQuintals = b.getUnaccountedQuintals();
        dto.notes = b.getNotes(); dto.createdAt = b.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public LocalDate getBatchDate() { return batchDate; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }
    public BigDecimal getPaddyInputQuintals() { return paddyInputQuintals; }
    public void setPaddyInputQuintals(BigDecimal paddyInputQuintals) { this.paddyInputQuintals = paddyInputQuintals; }
    public BigDecimal getExpectedRiceQuintals() { return expectedRiceQuintals; }
    public void setExpectedRiceQuintals(BigDecimal expectedRiceQuintals) { this.expectedRiceQuintals = expectedRiceQuintals; }
    public BigDecimal getActualRiceQuintals() { return actualRiceQuintals; }
    public void setActualRiceQuintals(BigDecimal actualRiceQuintals) { this.actualRiceQuintals = actualRiceQuintals; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public com.millsystem.entity.Shift getShift() { return shift; }
    public void setShift(com.millsystem.entity.Shift shift) { this.shift = shift; }
    public BigDecimal getBrokenRiceQuintals() { return brokenRiceQuintals; }
    public void setBrokenRiceQuintals(BigDecimal v) { this.brokenRiceQuintals = v; }
    public BigDecimal getBranQuintals() { return branQuintals; }
    public void setBranQuintals(BigDecimal v) { this.branQuintals = v; }
    public BigDecimal getHuskQuintals() { return huskQuintals; }
    public void setHuskQuintals(BigDecimal v) { this.huskQuintals = v; }
    public BigDecimal getUnaccountedQuintals() { return unaccountedQuintals; }
    public MillBatch.BatchStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<BatchPurchaseEntry> getPurchaseEntries() { return purchaseEntries; }
    public void setPurchaseEntries(List<BatchPurchaseEntry> purchaseEntries) { this.purchaseEntries = purchaseEntries; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
