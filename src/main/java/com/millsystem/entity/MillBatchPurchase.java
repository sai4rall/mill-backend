package com.millsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "mill_batch_purchases")
public class MillBatchPurchase {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private MillBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private PaddyPurchase purchase;

    @Column(name = "quintals_used", nullable = false, precision = 10, scale = 2)
    private BigDecimal quintalsUsed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MillBatch getBatch() { return batch; }
    public void setBatch(MillBatch batch) { this.batch = batch; }
    public PaddyPurchase getPurchase() { return purchase; }
    public void setPurchase(PaddyPurchase purchase) { this.purchase = purchase; }
    public BigDecimal getQuintalsUsed() { return quintalsUsed; }
    public void setQuintalsUsed(BigDecimal quintalsUsed) { this.quintalsUsed = quintalsUsed; }
}
