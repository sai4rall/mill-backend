package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A lorry of paddy that arrived at the mill and was turned away (not bought). Kept for the record and for
 * supplier / agent-wise rejection reports; it never touches stock, purchases or payments.
 */
@Entity
@Table(name = "rejected_lorries", indexes = {
        @Index(name = "idx_rejected_mill_date", columnList = "mill_id, rejection_date"),
        @Index(name = "idx_rejected_farmer", columnList = "farmer_id")})
public class RejectedLorry {

    public enum Reason { HIGH_MOISTURE, POOR_QUALITY, FOREIGN_MATTER, WRONG_VARIETY, OTHER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    /** Farmer, agent or supplier who brought the lorry; optional (may be unknown or not registered). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @Column(name = "rejection_date", nullable = false)
    private LocalDate rejectionDate;

    @Column(name = "truck_number", nullable = false, length = 20)
    private String truckNumber;

    @Column(name = "approx_weight_quintals", precision = 10, scale = 2)
    private BigDecimal approxWeightQuintals;

    @Column(name = "moisture_pct", precision = 5, scale = 2)
    private BigDecimal moisturePct;

    @Enumerated(EnumType.STRING)
    // Plain VARCHAR (not a MySQL ENUM) so new reasons can be added without a schema change.
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private Reason reason;

    @Column(length = 2000)
    private String notes;

    /** Username of whoever recorded it. */
    @Column(name = "recorded_by", length = 50)
    private String recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public Farmer getFarmer() { return farmer; }
    public void setFarmer(Farmer farmer) { this.farmer = farmer; }
    public LocalDate getRejectionDate() { return rejectionDate; }
    public void setRejectionDate(LocalDate rejectionDate) { this.rejectionDate = rejectionDate; }
    public String getTruckNumber() { return truckNumber; }
    public void setTruckNumber(String truckNumber) { this.truckNumber = truckNumber; }
    public BigDecimal getApproxWeightQuintals() { return approxWeightQuintals; }
    public void setApproxWeightQuintals(BigDecimal approxWeightQuintals) { this.approxWeightQuintals = approxWeightQuintals; }
    public BigDecimal getMoisturePct() { return moisturePct; }
    public void setMoisturePct(BigDecimal moisturePct) { this.moisturePct = moisturePct; }
    public Reason getReason() { return reason; }
    public void setReason(Reason reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
