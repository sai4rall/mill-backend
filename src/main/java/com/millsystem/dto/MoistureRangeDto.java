package com.millsystem.dto;

import com.millsystem.entity.MoistureDeduction;
import java.math.BigDecimal;

/** One moisture band: fromPct ≤ moisture < toPct (toPct null = "and above") → deductionPct off the price. */
public class MoistureRangeDto {
    private BigDecimal fromPct;
    private BigDecimal toPct;
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
