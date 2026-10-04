package com.millsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockMovementDto {

    public enum MovementType { PADDY_IN, PADDY_OUT, RICE_IN, RICE_OUT }

    private LocalDate date;
    private MovementType type;
    private String description;
    private BigDecimal quantityIn;
    private BigDecimal quantityOut;
    private BigDecimal runningBalance;
    private Long referenceId;

    public StockMovementDto(LocalDate date, MovementType type, String description,
                             BigDecimal quantityIn, BigDecimal quantityOut, Long referenceId) {
        this.date = date;
        this.type = type;
        this.description = description;
        this.quantityIn = quantityIn;
        this.quantityOut = quantityOut;
        this.referenceId = referenceId;
    }

    public LocalDate getDate() { return date; }
    public MovementType getType() { return type; }
    public String getDescription() { return description; }
    public BigDecimal getQuantityIn() { return quantityIn; }
    public BigDecimal getQuantityOut() { return quantityOut; }
    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }
    public Long getReferenceId() { return referenceId; }
}
