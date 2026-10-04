package com.millsystem.dto;

import java.math.BigDecimal;

public class StockSummaryDto {
    private final BigDecimal paddyOnHand;
    private final BigDecimal paddyInMill;
    private final BigDecimal riceOnHand;
    private final BigDecimal totalPaddyPurchased;
    private final BigDecimal totalRiceProduced;
    private final BigDecimal totalRiceExported;

    public StockSummaryDto(BigDecimal paddyOnHand, BigDecimal paddyInMill, BigDecimal riceOnHand,
                            BigDecimal totalPaddyPurchased, BigDecimal totalRiceProduced, BigDecimal totalRiceExported) {
        this.paddyOnHand = paddyOnHand; this.paddyInMill = paddyInMill; this.riceOnHand = riceOnHand;
        this.totalPaddyPurchased = totalPaddyPurchased; this.totalRiceProduced = totalRiceProduced;
        this.totalRiceExported = totalRiceExported;
    }

    public BigDecimal getPaddyOnHand() { return paddyOnHand; }
    public BigDecimal getPaddyInMill() { return paddyInMill; }
    public BigDecimal getRiceOnHand() { return riceOnHand; }
    public BigDecimal getTotalPaddyPurchased() { return totalPaddyPurchased; }
    public BigDecimal getTotalRiceProduced() { return totalRiceProduced; }
    public BigDecimal getTotalRiceExported() { return totalRiceExported; }
}
