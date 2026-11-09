package com.millsystem.service;

import com.millsystem.dto.MillBatchDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillBatch;
import com.millsystem.entity.MillBatchPurchase;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillBatchPurchaseRepository;
import com.millsystem.repository.MillBatchRepository;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.PaddyPurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MillBatchService {

    private final MillBatchRepository millBatchRepository;
    private final MillRepository millRepository;
    private final PaddyPurchaseRepository purchaseRepository;
    private final MillBatchPurchaseRepository batchPurchaseRepository;
    private final StockService stockService;

    public MillBatchService(MillBatchRepository millBatchRepository, MillRepository millRepository,
                            PaddyPurchaseRepository purchaseRepository, MillBatchPurchaseRepository batchPurchaseRepository,
                            StockService stockService) {
        this.millBatchRepository = millBatchRepository;
        this.millRepository = millRepository;
        this.purchaseRepository = purchaseRepository;
        this.batchPurchaseRepository = batchPurchaseRepository;
        this.stockService = stockService;
    }

    public List<MillBatchDto> getAll(Long millId) {
        return millBatchRepository.findByMillIdOrderByBatchDateDesc(millId)
                .stream().map(MillBatchDto::from).collect(Collectors.toList());
    }

    public MillBatchDto getById(Long id, Long millId) {
        return millBatchRepository.findByIdAndMillId(id, millId)
                .map(MillBatchDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
    }

    @Transactional
    public MillBatchDto create(MillBatchDto dto, Long millId) {
        // Lock the mill so concurrent batches cannot both use the same paddy.
        Mill mill = millRepository.findByIdForUpdate(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        BigDecimal available = stockService.paddyOnHand(millId);
        if (dto.getPaddyInputQuintals().compareTo(available) > 0) {
            throw new IllegalArgumentException("Paddy input (" + dto.getPaddyInputQuintals().stripTrailingZeros().toPlainString()
                    + " Qtl) exceeds paddy on hand (" + available.setScale(2, RoundingMode.HALF_UP).toPlainString() + " Qtl)");
        }

        MillBatch batch = new MillBatch();
        batch.setMill(mill); batch.setBatchDate(dto.getBatchDate());
        batch.setPaddyInputQuintals(dto.getPaddyInputQuintals());
        batch.setExpectedRiceQuintals(dto.getExpectedRiceQuintals());
        batch.setNotes(dto.getNotes());
        batch.setShift(dto.getShift());

        // Optional links to the purchases the paddy came from (traceability). When given they must add up to the input
        // and stay within what each purchase has left.
        List<MillBatchPurchase> entries = new ArrayList<>();
        if (dto.getPurchaseEntries() != null && !dto.getPurchaseEntries().isEmpty()) {
            BigDecimal linked = BigDecimal.ZERO;
            for (MillBatchDto.BatchPurchaseEntry entry : dto.getPurchaseEntries()) {
                if (entry.getPurchaseId() == null || entry.getQuintalsUsed() == null || entry.getQuintalsUsed().signum() <= 0) {
                    throw new IllegalArgumentException("Each purchase entry needs a purchase and a quantity greater than 0");
                }
                PaddyPurchase purchase = purchaseRepository.findByIdAndMillId(entry.getPurchaseId(), millId)
                        .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + entry.getPurchaseId()));
                BigDecimal left = purchase.getWeightQuintals().subtract(batchPurchaseRepository.sumQuintalsUsedByPurchaseId(purchase.getId()));
                if (entry.getQuintalsUsed().compareTo(left) > 0) {
                    throw new IllegalArgumentException("Purchase #" + purchase.getId() + " has only " + left.toPlainString() + " Qtl left");
                }
                linked = linked.add(entry.getQuintalsUsed());
                MillBatchPurchase mbp = new MillBatchPurchase();
                mbp.setBatch(batch); mbp.setPurchase(purchase); mbp.setQuintalsUsed(entry.getQuintalsUsed());
                entries.add(mbp);
            }
            if (linked.compareTo(dto.getPaddyInputQuintals()) != 0) {
                throw new IllegalArgumentException("Purchase entries add up to " + linked.toPlainString()
                        + " Qtl but the paddy input is " + dto.getPaddyInputQuintals().toPlainString() + " Qtl");
            }
        }
        batch.setBatchPurchases(entries);
        return MillBatchDto.from(millBatchRepository.save(batch));
    }

    @Transactional
    public MillBatchDto completeBatch(Long id, MillBatchDto dto, Long millId) {
        MillBatch batch = millBatchRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
        if (batch.getStatus() == MillBatch.BatchStatus.COMPLETED) {
            throw new IllegalArgumentException("Batch is already completed");
        }
        if (dto.getActualRiceQuintals() == null || dto.getActualRiceQuintals().signum() < 0) {
            throw new IllegalArgumentException("Actual rice output is required and cannot be negative");
        }
        if (dto.getActualRiceQuintals().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Rice output can have at most 2 decimal places");
        }
        if (dto.getActualRiceQuintals().compareTo(batch.getPaddyInputQuintals()) > 0) {
            throw new IllegalArgumentException("Rice output (" + dto.getActualRiceQuintals().toPlainString()
                    + " Qtl) cannot exceed the paddy input (" + batch.getPaddyInputQuintals().toPlainString() + " Qtl)");
        }
        // By-products are optional; together with the rice they cannot weigh more than the paddy that went in.
        BigDecimal outputs = dto.getActualRiceQuintals();
        for (BigDecimal v : new BigDecimal[]{dto.getBrokenRiceQuintals(), dto.getBranQuintals(), dto.getHuskQuintals()}) {
            if (v != null) {
                if (v.signum() < 0) throw new IllegalArgumentException("By-product quantities cannot be negative");
                outputs = outputs.add(v);
            }
        }
        if (outputs.compareTo(batch.getPaddyInputQuintals()) > 0) {
            throw new IllegalArgumentException("Rice and by-products (" + outputs.stripTrailingZeros().toPlainString()
                    + " Qtl) cannot exceed the paddy input (" + batch.getPaddyInputQuintals().toPlainString() + " Qtl)");
        }
        batch.setBrokenRiceQuintals(dto.getBrokenRiceQuintals());
        batch.setBranQuintals(dto.getBranQuintals());
        batch.setHuskQuintals(dto.getHuskQuintals());
        if (dto.getShift() != null && batch.getShift() == null) batch.setShift(dto.getShift());
        batch.setActualRiceQuintals(dto.getActualRiceQuintals());
        if (batch.getPaddyInputQuintals().compareTo(BigDecimal.ZERO) > 0) {
            batch.setConversionRate(dto.getActualRiceQuintals()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(batch.getPaddyInputQuintals(), 2, RoundingMode.HALF_UP));
        }
        batch.setStatus(MillBatch.BatchStatus.COMPLETED);
        if (dto.getNotes() != null) batch.setNotes(dto.getNotes());
        return MillBatchDto.from(millBatchRepository.save(batch));
    }
}
