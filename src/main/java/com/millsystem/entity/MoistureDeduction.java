package com.millsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * One moisture band and its price deduction. {@code mill == null} rows are a company's default table, owned by the
 * super admin in {@code ownerId}; otherwise the row belongs to a mill's own table, which fully replaces the company
 * default for that mill.
 * The band covers fromPct ≤ moisture < toPct; a null toPct means "and above".
 */
@Entity
@Table(name = "moisture_deductions", indexes = @Index(name = "idx_moisture_mill", columnList = "mill_id"))
public class MoistureDeduction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id")
    private Mill mill;

    /** Company default rows only: the owning super admin's user id. */
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "from_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal fromPct;

    @Column(name = "to_pct", precision = 5, scale = 2)
    private BigDecimal toPct;

    @Column(name = "deduction_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal deductionPct;

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public BigDecimal getFromPct() { return fromPct; }
    public void setFromPct(BigDecimal fromPct) { this.fromPct = fromPct; }
    public BigDecimal getToPct() { return toPct; }
    public void setToPct(BigDecimal toPct) { this.toPct = toPct; }
    public BigDecimal getDeductionPct() { return deductionPct; }
    public void setDeductionPct(BigDecimal deductionPct) { this.deductionPct = deductionPct; }
}
