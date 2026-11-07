package com.millsystem.service;

import com.millsystem.dto.StockSummaryDto;
import com.millsystem.repository.ExportShipmentRepository;
import com.millsystem.repository.MillBatchRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class StockService {

    private final PaddyPurchaseRepository purchaseRepository;
    private final MillBatchRepository batchRepository;
    private final ExportShipmentRepository shipmentRepository;

    public StockService(PaddyPurchaseRepository purchaseRepository,
                        MillBatchRepository batchRepository, ExportShipmentRepository shipmentRepository) {
        this.purchaseRepository = purchaseRepository;
        this.batchRepository = batchRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public StockSummaryDto getSummary(Long millId) {
        // Stock comes from the batches themselves (their paddy input), not from optional batch-purchase links:
        // the UIs record a batch's paddy quantity without picking purchases, and the stock statement uses the same rule.
        BigDecimal totalPaddyPurchased = purchaseRepository.sumWeightByMillId(millId);
        BigDecimal paddyInMill = batchRepository.sumPaddyInputInProgressByMillId(millId);
        BigDecimal paddyOnHand = paddyOnHand(millId, totalPaddyPurchased);
        BigDecimal totalRiceProduced = batchRepository.sumActualRiceByMillId(millId);
        BigDecimal totalRiceExported = shipmentRepository.sumExportedByMillId(millId);
        BigDecimal riceOnHand = totalRiceProduced.subtract(totalRiceExported);
        return new StockSummaryDto(paddyOnHand, paddyInMill, riceOnHand,
                totalPaddyPurchased, totalRiceProduced, totalRiceExported);
    }

    /** Purchased paddy not yet sent to the mill. */
    public BigDecimal paddyOnHand(Long millId) {
        return paddyOnHand(millId, purchaseRepository.sumWeightByMillId(millId));
    }

    private BigDecimal paddyOnHand(Long millId, BigDecimal totalPurchased) {
        return totalPurchased.subtract(batchRepository.sumPaddyInputByMillId(millId));
    }

    /** Rice produced by completed batches and not yet shipped. */
    public BigDecimal riceOnHand(Long millId) {
        return batchRepository.sumActualRiceByMillId(millId).subtract(shipmentRepository.sumExportedByMillId(millId));
    }

}
