package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One laboratory test of a sample (rice from a milling batch, or paddy / rice identified by a lot or stack reference).
 * The result and grade are worked out against the company's quality standard when the test is saved, and the limits used are
 * kept with the test so its history and certificate do not change if the standard is edited later.
 * Once a certificate is issued the test is locked.
 */
@Entity
@Table(name = "quality_tests",
        indexes = {
                @Index(name = "idx_quality_mill_date", columnList = "mill_id, test_date"),
                @Index(name = "idx_quality_batch", columnList = "batch_id")},
        uniqueConstraints = @UniqueConstraint(name = "uk_quality_certificate", columnNames = {"mill_id", "certificate_number"}))
public class QualityTest {

    public enum Product { PADDY, RICE }
    public enum Result { PASS, FAIL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private Product product;

    /** Milling batch the sample came from (rice, usually). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private MillBatch batch;

    /** Free-text lot / bag / stack reference for samples not tied to a batch. */
    @Column(name = "lot_reference", length = 50)
    private String lotReference;

    @Column(length = 50)
    private String variety;

    @Column(name = "moisture_pct", nullable = false, precision = 5, scale = 2) private BigDecimal moisturePct;
    @Column(name = "broken_pct", precision = 5, scale = 2) private BigDecimal brokenPct;
    @Column(name = "foreign_matter_pct", precision = 5, scale = 2) private BigDecimal foreignMatterPct;
    @Column(name = "damaged_pct", precision = 5, scale = 2) private BigDecimal damagedPct;
    @Column(name = "chalky_pct", precision = 5, scale = 2) private BigDecimal chalkyPct;
    @Column(name = "head_rice_pct", precision = 5, scale = 2) private BigDecimal headRicePct;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private Result result;

    /** Grade earned (A+ … C); null when the sample failed. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10, columnDefinition = "VARCHAR(10)")
    private QualityGrade grade;

    /** Human-readable reasons the sample failed, one per line (null when it passed). */
    @Column(name = "failure_reasons", length = 2000)
    private String failureReasons;

    /** The grade limits the sample was judged against, as JSON (see QualityStandardDto). */
    @Column(name = "standard_snapshot", length = 4000)
    private String standardSnapshot;

    /** Lab technician / chemist who did the test. */
    @Column(name = "tested_by", length = 100)
    private String testedBy;

    @Column(length = 2000)
    private String remarks;

    @Column(name = "certificate_number", length = 30)
    private String certificateNumber;

    @Column(name = "certificate_issued_at")
    private LocalDateTime certificateIssuedAt;

    @Column(name = "certificate_issued_by", length = 50)
    private String certificateIssuedBy;

    /** Username of whoever entered the result. */
    @Column(name = "recorded_by", length = 50)
    private String recordedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate testDate) { this.testDate = testDate; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public MillBatch getBatch() { return batch; }
    public void setBatch(MillBatch batch) { this.batch = batch; }
    public String getLotReference() { return lotReference; }
    public void setLotReference(String lotReference) { this.lotReference = lotReference; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }
    public BigDecimal getMoisturePct() { return moisturePct; }
    public void setMoisturePct(BigDecimal v) { this.moisturePct = v; }
    public BigDecimal getBrokenPct() { return brokenPct; }
    public void setBrokenPct(BigDecimal v) { this.brokenPct = v; }
    public BigDecimal getForeignMatterPct() { return foreignMatterPct; }
    public void setForeignMatterPct(BigDecimal v) { this.foreignMatterPct = v; }
    public BigDecimal getDamagedPct() { return damagedPct; }
    public void setDamagedPct(BigDecimal v) { this.damagedPct = v; }
    public BigDecimal getChalkyPct() { return chalkyPct; }
    public void setChalkyPct(BigDecimal v) { this.chalkyPct = v; }
    public BigDecimal getHeadRicePct() { return headRicePct; }
    public void setHeadRicePct(BigDecimal v) { this.headRicePct = v; }
    public Result getResult() { return result; }
    public void setResult(Result result) { this.result = result; }
    public QualityGrade getGrade() { return grade; }
    public void setGrade(QualityGrade grade) { this.grade = grade; }
    public String getFailureReasons() { return failureReasons; }
    public void setFailureReasons(String failureReasons) { this.failureReasons = failureReasons; }
    public String getStandardSnapshot() { return standardSnapshot; }
    public void setStandardSnapshot(String standardSnapshot) { this.standardSnapshot = standardSnapshot; }
    public String getTestedBy() { return testedBy; }
    public void setTestedBy(String testedBy) { this.testedBy = testedBy; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public String getCertificateNumber() { return certificateNumber; }
    public void setCertificateNumber(String certificateNumber) { this.certificateNumber = certificateNumber; }
    public LocalDateTime getCertificateIssuedAt() { return certificateIssuedAt; }
    public void setCertificateIssuedAt(LocalDateTime v) { this.certificateIssuedAt = v; }
    public String getCertificateIssuedBy() { return certificateIssuedBy; }
    public void setCertificateIssuedBy(String v) { this.certificateIssuedBy = v; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
