package com.millsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityTest;
import com.millsystem.validation.NotFutureDate;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * A laboratory test. Moisture is always required; broken % is required for rice.
 * Result, grade, failure reasons, the limits used and the certificate are worked out by the server.
 */
public class QualityTestDto {
    private static final String RANGE = "Must be between 0 and 100";
    private static final String DIGITS = "Can have at most 2 decimal places";

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String sampleCode;

    @NotNull(message = "Test date is required")
    @NotFutureDate(message = "Test date cannot be in the future")
    private LocalDate testDate;

    @NotNull(message = "Product is required")
    private QualityTest.Product product;

    private Long batchId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDate batchDate;

    @Size(max = 50, message = "Lot reference must be at most 50 characters") private String lotReference;
    @Size(max = 50, message = "Variety must be at most 50 characters") private String variety;

    @NotNull(message = "Moisture % is required")
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal moisturePct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal brokenPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal foreignMatterPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal damagedPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal chalkyPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal headRicePct;

    @Size(max = 100, message = "Tested by must be at most 100 characters") private String testedBy;
    @Size(max = 2000, message = "Remarks must be at most 2000 characters") private String remarks;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private QualityTest.Result result;
    /** A_PLUS, A, B_PLUS, B or C; null when the sample failed. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private QualityGrade grade;
    /** "A+", "A", … for display. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String gradeLabel;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private List<String> failureReasons = List.of();
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private QualityStandardDto standard;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String certificateNumber;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime certificateIssuedAt;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String certificateIssuedBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private String recordedBy;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;

    public static QualityTestDto from(QualityTest t, QualityStandardDto standard) {
        QualityTestDto d = new QualityTestDto();
        d.id = t.getId(); d.sampleCode = "LAB-" + t.getId(); d.testDate = t.getTestDate(); d.product = t.getProduct();
        if (t.getBatch() != null) { d.batchId = t.getBatch().getId(); d.batchDate = t.getBatch().getBatchDate(); }
        d.lotReference = t.getLotReference(); d.variety = t.getVariety();
        d.moisturePct = t.getMoisturePct(); d.brokenPct = t.getBrokenPct(); d.foreignMatterPct = t.getForeignMatterPct();
        d.damagedPct = t.getDamagedPct(); d.chalkyPct = t.getChalkyPct(); d.headRicePct = t.getHeadRicePct();
        d.testedBy = t.getTestedBy(); d.remarks = t.getRemarks(); d.result = t.getResult();
        d.grade = t.getGrade(); d.gradeLabel = t.getGrade() == null ? null : t.getGrade().label();
        d.failureReasons = t.getFailureReasons() == null || t.getFailureReasons().isBlank()
                ? List.of() : Arrays.asList(t.getFailureReasons().split("\n"));
        d.standard = standard;
        d.certificateNumber = t.getCertificateNumber(); d.certificateIssuedAt = t.getCertificateIssuedAt();
        d.certificateIssuedBy = t.getCertificateIssuedBy(); d.recordedBy = t.getRecordedBy(); d.createdAt = t.getCreatedAt();
        return d;
    }

    /** Broken grains are the main grade factor for rice. */
    @JsonIgnore
    @AssertTrue(message = "Broken % is required for rice")
    public boolean isBrokenGiven() {
        return product != QualityTest.Product.RICE || brokenPct != null;
    }

    private static String clean(String s) { return s == null || s.isBlank() ? null : s.trim().replaceAll("\\s+", " "); }

    public Long getId() { return id; }
    public String getSampleCode() { return sampleCode; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate testDate) { this.testDate = testDate; }
    public QualityTest.Product getProduct() { return product; }
    public void setProduct(QualityTest.Product product) { this.product = product; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public LocalDate getBatchDate() { return batchDate; }
    public String getLotReference() { return lotReference; }
    public void setLotReference(String v) { this.lotReference = clean(v); }
    public String getVariety() { return variety; }
    public void setVariety(String v) { this.variety = clean(v); }
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
    public String getTestedBy() { return testedBy; }
    public void setTestedBy(String v) { this.testedBy = clean(v); }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v == null || v.isBlank() ? null : v.trim(); }
    public QualityTest.Result getResult() { return result; }
    public QualityGrade getGrade() { return grade; }
    public String getGradeLabel() { return gradeLabel; }
    public List<String> getFailureReasons() { return failureReasons; }
    public QualityStandardDto getStandard() { return standard; }
    public String getCertificateNumber() { return certificateNumber; }
    public LocalDateTime getCertificateIssuedAt() { return certificateIssuedAt; }
    public String getCertificateIssuedBy() { return certificateIssuedBy; }
    public String getRecordedBy() { return recordedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
