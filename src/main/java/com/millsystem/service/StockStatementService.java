package com.millsystem.service;

import com.millsystem.dto.StockMovementDto;
import com.millsystem.dto.StockMovementDto.MovementType;
import com.millsystem.dto.StockStatementDto;
import com.millsystem.entity.MillBatch;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.entity.ExportShipment;
import com.millsystem.repository.ExportShipmentRepository;
import com.millsystem.repository.MillBatchRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StockStatementService {

    private final PaddyPurchaseRepository purchaseRepository;
    private final MillBatchRepository batchRepository;
    private final ExportShipmentRepository shipmentRepository;

    public StockStatementService(PaddyPurchaseRepository purchaseRepository,
                                  MillBatchRepository batchRepository,
                                  ExportShipmentRepository shipmentRepository) {
        this.purchaseRepository = purchaseRepository;
        this.batchRepository = batchRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public StockStatementDto getStatement(Long millId, LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.of(2000, 1, 1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        List<PaddyPurchase> purchases = purchaseRepository
                .findByMillIdAndPurchaseDateBetweenOrderByPurchaseDateAsc(millId, from, to);
        List<MillBatch> batches = batchRepository
                .findByMillIdAndBatchDateBetweenOrderByBatchDateAsc(millId, from, to);
        List<ExportShipment> shipments = shipmentRepository
                .findByMillIdAndShipmentDateBetweenOrderByShipmentDateAsc(millId, from, to);

        return new StockStatementDto(
                buildPaddyMovements(purchases, batches),
                buildRiceMovements(batches, shipments)
        );
    }

    private List<StockMovementDto> buildPaddyMovements(List<PaddyPurchase> purchases, List<MillBatch> batches) {
        List<StockMovementDto> movements = new ArrayList<>();

        purchases.forEach(p -> movements.add(new StockMovementDto(
                p.getPurchaseDate(),
                MovementType.PADDY_IN,
                "Purchase from " + p.getFarmer().getName(),
                p.getWeightQuintals(),
                null,
                p.getId()
        )));

        batches.forEach(b -> movements.add(new StockMovementDto(
                b.getBatchDate(),
                MovementType.PADDY_OUT,
                "Sent to Mill Batch #" + b.getId()
                        + (b.getStatus() == MillBatch.BatchStatus.IN_PROGRESS ? " (In Progress)" : ""),
                null,
                b.getPaddyInputQuintals(),
                b.getId()
        )));

        movements.sort(Comparator.comparing(StockMovementDto::getDate)
                .thenComparing(m -> m.getType() == MovementType.PADDY_IN ? 0 : 1));

        BigDecimal balance = BigDecimal.ZERO;
        for (StockMovementDto m : movements) {
            if (m.getQuantityIn() != null) balance = balance.add(m.getQuantityIn());
            if (m.getQuantityOut() != null) balance = balance.subtract(m.getQuantityOut());
            m.setRunningBalance(balance);
        }
        return movements;
    }

    private List<StockMovementDto> buildRiceMovements(List<MillBatch> batches, List<ExportShipment> shipments) {
        List<StockMovementDto> movements = new ArrayList<>();

        batches.stream()
                .filter(b -> b.getStatus() == MillBatch.BatchStatus.COMPLETED && b.getActualRiceQuintals() != null)
                .forEach(b -> movements.add(new StockMovementDto(
                        b.getBatchDate(),
                        MovementType.RICE_IN,
                        "Batch #" + b.getId() + " output"
                                + (b.getConversionRate() != null ? " (" + b.getConversionRate() + "% yield)" : ""),
                        b.getActualRiceQuintals(),
                        null,
                        b.getId()
                )));

        shipments.forEach(s -> movements.add(new StockMovementDto(
                s.getShipmentDate(),
                MovementType.RICE_OUT,
                "Export to " + s.getOrder().getCustomer().getName()
                        + (s.getVehicleNumber() != null ? " [" + s.getVehicleNumber() + "]" : ""),
                null,
                s.getQuintalsExported(),
                s.getId()
        )));

        movements.sort(Comparator.comparing(StockMovementDto::getDate)
                .thenComparing(m -> m.getType() == MovementType.RICE_IN ? 0 : 1));

        BigDecimal balance = BigDecimal.ZERO;
        for (StockMovementDto m : movements) {
            if (m.getQuantityIn() != null) balance = balance.add(m.getQuantityIn());
            if (m.getQuantityOut() != null) balance = balance.subtract(m.getQuantityOut());
            m.setRunningBalance(balance);
        }
        return movements;
    }
}
