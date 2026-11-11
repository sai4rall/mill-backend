package com.millsystem.dto;

import com.millsystem.entity.QualityGrade;
import com.millsystem.entity.QualityGradeLimit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** The limits a sample must meet for one grade. A blank limit is not checked at this grade. */
public class GradeLimitsDto {
    private static final String RANGE = "Limit must be between 0 and 100";
    private static final String DIGITS = "Limit can have at most 2 decimal places";

    @NotNull(message = "Grade is required")
    private QualityGrade grade;

    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal maxMoisturePct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal maxBrokenPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal maxForeignMatterPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal maxDamagedPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal maxChalkyPct;
    @DecimalMin(value = "0.00", message = RANGE) @DecimalMax(value = "100.00", message = RANGE) @Digits(integer = 3, fraction = 2, message = DIGITS)
    private BigDecimal minHeadRicePct;

    public GradeLimitsDto() {}

    /** Limits in order: moisture, broken, foreign matter, damaged, chalky (maximums) and head rice (minimum). */
    public static GradeLimitsDto of(QualityGrade grade, String moisture, String broken, String foreign, String damaged, String chalky, String headRice) {
        GradeLimitsDto d = new GradeLimitsDto();
        d.grade = grade;
        d.maxMoisturePct = pct(moisture); d.maxBrokenPct = pct(broken); d.maxForeignMatterPct = pct(foreign);
        d.maxDamagedPct = pct(damaged); d.maxChalkyPct = pct(chalky); d.minHeadRicePct = pct(headRice);
        return d;
    }

    public static GradeLimitsDto from(QualityGradeLimit g) {
        GradeLimitsDto d = new GradeLimitsDto();
        d.grade = g.getGrade();
        d.maxMoisturePct = g.getMaxMoisturePct(); d.maxBrokenPct = g.getMaxBrokenPct(); d.maxForeignMatterPct = g.getMaxForeignMatterPct();
        d.maxDamagedPct = g.getMaxDamagedPct(); d.maxChalkyPct = g.getMaxChalkyPct(); d.minHeadRicePct = g.getMinHeadRicePct();
        return d;
    }

    private static BigDecimal pct(String v) { return v == null ? null : new BigDecimal(v).setScale(2); }

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
}
