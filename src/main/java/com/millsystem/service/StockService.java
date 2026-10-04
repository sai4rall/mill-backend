package com.millsystem.service;

import com.millsystem.dto.StockSummaryDto;
import com.millsystem.repository.ExportShipmentRepository;
import com.millsystem.repository.MillBatchPurchaseRepository;
import com.millsystem.repository.MillBatchRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class StockService {

    private final PaddyPurchaseRepository purchaseRepository;
    private final MillBatchPurchaseRepository batchPurchaseRepository;
    private final MillBatchRepository batchRepository;
    private final ExportShipmentRepository shipmentRepository;

    public StockService(PaddyPurchaseRepository purchaseRepository, MillBatchPurchaseRepository batchPurchaseRepository,
                        MillBatchRepository batchRepository, ExportShipmentRepository shipmentRepository) {
        this.purchaseRepository = purchaseRepository;
        this.batchPurchaseRepository = batchPurchaseRepository;
        this.batchRepository = batchRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public StockSummaryDto getSummary(Long millId) {
        BigDecimal totalPaddyPurchased = purchaseRepository.sumWeightByMillId(millId);
        BigDecimal totalPaddyUsedInBatches = batchPurchaseRepository.sumQuintalsUsedByMillId(millId);
        BigDecimal paddyInMill = batchPurchaseRepository.sumQuintalsInProgressByMillId(millId);
        BigDecimal paddyOnHand = totalPaddyPurchased.subtract(totalPaddyUsedInBatches);
        BigDecimal totalRiceProduced = batchRepository.sumActualRiceByMillId(millId);
        BigDecimal totalRiceExported = shipmentRepository.sumExportedByMillId(millId);
        BigDecimal riceOnHand = totalRiceProduced.subtract(totalRiceExported);
        return new StockSummaryDto(paddyOnHand, paddyInMill, riceOnHand,
                totalPaddyPurchased, totalRiceProduced, totalRiceExported);
    }
}
