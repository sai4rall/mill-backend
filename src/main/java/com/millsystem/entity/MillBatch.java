package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mill_batches")
public class MillBatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @Column(name = "batch_date", nullable = false)
    private LocalDate batchDate;

    @Column(name = "paddy_input_quintals", nullable = false, precision = 10, scale = 2)
    private BigDecimal paddyInputQuintals;

    @Column(name = "expected_rice_quintals", precision = 10, scale = 2)
    private BigDecimal expectedRiceQuintals;

    @Column(name = "actual_rice_quintals", precision = 10, scale = 2)
    private BigDecimal actualRiceQuintals;

    @Column(name = "conversion_rate", precision = 5, scale = 2)
    private BigDecimal conversionRate;

    /** Duty shift that ran the batch (null for batches recorded before shifts existed). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, columnDefinition = "VARCHAR(20)")
    private Shift shift;

    // By-products recorded when the batch is completed (optional). Only actualRiceQuintals counts as rice stock.
    @Column(name = "broken_rice_quintals", precision = 10, scale = 2)
    private BigDecimal brokenRiceQuintals;

    @Column(name = "bran_quintals", precision = 10, scale = 2)
    private BigDecimal branQuintals;

    @Column(name = "husk_quintals", precision = 10, scale = 2)
    private BigDecimal huskQuintals;

    public Shift getShift() { return shift; }
    public void setShift(Shift shift) { this.shift = shift; }
    public BigDecimal getBrokenRiceQuintals() { return brokenRiceQuintals; }
    public void setBrokenRiceQuintals(BigDecimal v) { this.brokenRiceQuintals = v; }
    public BigDecimal getBranQuintals() { return branQuintals; }
    public void setBranQuintals(BigDecimal v) { this.branQuintals = v; }
    public BigDecimal getHuskQuintals() { return huskQuintals; }
    public void setHuskQuintals(BigDecimal v) { this.huskQuintals = v; }

    /** Paddy not accounted for by rice and recorded by-products (null until completed). */
    public BigDecimal getUnaccountedQuintals() {
        if (status != BatchStatus.COMPLETED || actualRiceQuintals == null) return null;
        BigDecimal out = actualRiceQuintals;
        for (BigDecimal v : new BigDecimal[]{brokenRiceQuintals, branQuintals, huskQuintals}) if (v != null) out = out.add(v);
        return paddyInputQuintals.subtract(out);
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.IN_PROGRESS;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MillBatchPurchase> batchPurchases;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum BatchStatus { IN_PROGRESS, COMPLETED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public LocalDate getBatchDate() { return batchDate; }
    public void setBatchDate(LocalDate batchDate) { this.batchDate = batchDate; }
    public BigDecimal getPaddyInputQuintals() { return paddyInputQuintals; }
    public void setPaddyInputQuintals(BigDecimal paddyInputQuintals) { this.paddyInputQuintals = paddyInputQuintals; }
    public BigDecimal getExpectedRiceQuintals() { return expectedRiceQuintals; }
    public void setExpectedRiceQuintals(BigDecimal expectedRiceQuintals) { this.expectedRiceQuintals = expectedRiceQuintals; }
    public BigDecimal getActualRiceQuintals() { return actualRiceQuintals; }
    public void setActualRiceQuintals(BigDecimal actualRiceQuintals) { this.actualRiceQuintals = actualRiceQuintals; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<MillBatchPurchase> getBatchPurchases() { return batchPurchases; }
    public void setBatchPurchases(List<MillBatchPurchase> batchPurchases) { this.batchPurchases = batchPurchases; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
