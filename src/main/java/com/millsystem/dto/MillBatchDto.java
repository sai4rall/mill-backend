package com.millsystem.dto;

import com.millsystem.entity.MillBatch;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MillBatchDto {
    private Long id;
    @NotNull private LocalDate batchDate;
    @NotNull @Positive private BigDecimal paddyInputQuintals;
    private BigDecimal expectedRiceQuintals;
    private BigDecimal actualRiceQuintals;
    private BigDecimal conversionRate;
    private MillBatch.BatchStatus status;
    private String notes;
    private List<BatchPurchaseEntry> purchaseEntries;
    private LocalDateTime createdAt;

    public static class BatchPurchaseEntry {
        private Long purchaseId;
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
        dto.conversionRate = b.getConversionRate(); dto.status = b.getStatus();
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
    public MillBatch.BatchStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<BatchPurchaseEntry> getPurchaseEntries() { return purchaseEntries; }
    public void setPurchaseEntries(List<BatchPurchaseEntry> purchaseEntries) { this.purchaseEntries = purchaseEntries; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
