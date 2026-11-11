package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of a company's quality standard: the limits a sample of one product must meet to earn one grade.
 * Set by the super admin ({@code ownerId}) and applied to all of their mills. A null limit is not checked at that grade.
 * Companies that never saved a standard use {@link com.millsystem.quality.QualityDefaults}.
 */
@Entity
@Table(name = "quality_grade_limits", uniqueConstraints =
        @UniqueConstraint(name = "uk_quality_grade_owner_product_grade", columnNames = {"owner_id", "product", "grade"}))
public class QualityGradeLimit {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The super admin (company) the standard belongs to. */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private QualityTest.Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private QualityGrade grade;

    @Column(name = "max_moisture_pct", precision = 5, scale = 2) private BigDecimal maxMoisturePct;
    @Column(name = "max_broken_pct", precision = 5, scale = 2) private BigDecimal maxBrokenPct;
    @Column(name = "max_foreign_matter_pct", precision = 5, scale = 2) private BigDecimal maxForeignMatterPct;
    @Column(name = "max_damaged_pct", precision = 5, scale = 2) private BigDecimal maxDamagedPct;
    @Column(name = "max_chalky_pct", precision = 5, scale = 2) private BigDecimal maxChalkyPct;
    @Column(name = "min_head_rice_pct", precision = 5, scale = 2) private BigDecimal minHeadRicePct;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public QualityTest.Product getProduct() { return product; }
    public void setProduct(QualityTest.Product product) { this.product = product; }
    public QualityGrade getGrade() { return grade; }
    public void setGrade(QualityGrade grade) { this.grade = grade; }
    public BigDecimal getMaxMoisturePct() { return maxMoisturePct; }
    public void setMaxMoisturePct(BigDecimal v) { this.maxMoisturePct = v; }
    public BigDecimal getMaxBrokenPct() { return maxBrokenPct; }
    public void setMaxBrokenPct(BigDecimal v) { this.maxBrokenPct = v; }
    public BigDecimal getMaxForeignMatterPct() { return maxForeignMatterPct; }
    public void setMaxForeignMatterPct(BigDecimal v) { this.maxForeignMatterPct = v; }
    public BigDecimal getMaxDamagedPct() { return maxDamagedPct; }
    public void setMaxDamagedPct(BigDecimal v) { this.maxDamagedPct = v; }
    public BigDecimal getMaxChalkyPct() { return maxChalkyPct; }
    public void setMaxChalkyPct(BigDecimal v) { this.maxChalkyPct = v; }
    public BigDecimal getMinHeadRicePct() { return minHeadRicePct; }
    public void setMinHeadRicePct(BigDecimal v) { this.minHeadRicePct = v; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
