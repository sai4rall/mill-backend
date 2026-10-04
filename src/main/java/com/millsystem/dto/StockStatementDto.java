package com.millsystem.dto;

import java.util.List;

public class StockStatementDto {
    private final List<StockMovementDto> paddyMovements;
    private final List<StockMovementDto> riceMovements;

    public StockStatementDto(List<StockMovementDto> paddyMovements, List<StockMovementDto> riceMovements) {
        this.paddyMovements = paddyMovements;
        this.riceMovements = riceMovements;
    }

    public List<StockMovementDto> getPaddyMovements() { return paddyMovements; }
    public List<StockMovementDto> getRiceMovements() { return riceMovements; }
}
