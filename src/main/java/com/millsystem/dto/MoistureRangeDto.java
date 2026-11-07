package com.millsystem.dto;

import jakarta.validation.constraints.*;
import com.millsystem.entity.MoistureDeduction;
import java.math.BigDecimal;

/** One moisture band: fromPct ≤ moisture < toPct (toPct null = "and above") → deductionPct off the price. */
public class MoistureRangeDto {
    @Digits(integer = 3, fraction = 2, message = "Percentage can have at most 2 decimal places")
    private BigDecimal fromPct;
    @Digits(integer = 3, fraction = 2, message = "Percentage can have at most 2 decimal places")
    private BigDecimal toPct;
    @Digits(integer = 3, fraction = 2, message = "Percentage can have at most 2 decimal places")
    private BigDecimal deductionPct;

    public MoistureRangeDto() {}

    public MoistureRangeDto(BigDecimal fromPct, BigDecimal toPct, BigDecimal deductionPct) {
        this.fromPct = fromPct; this.toPct = toPct; this.deductionPct = deductionPct;
    }

    public static MoistureRangeDto from(MoistureDeduction d) {
        return new MoistureRangeDto(d.getFromPct(), d.getToPct(), d.getDeductionPct());
    }

    public BigDecimal getFromPct() { return fromPct; }
    public void setFromPct(BigDecimal fromPct) { this.fromPct = fromPct; }
    public BigDecimal getToPct() { return toPct; }
    public void setToPct(BigDecimal toPct) { this.toPct = toPct; }
    public BigDecimal getDeductionPct() { return deductionPct; }
    public void setDeductionPct(BigDecimal deductionPct) { this.deductionPct = deductionPct; }
}
