package com.millsystem.service;

import com.millsystem.dto.MillBatchDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillBatch;
import com.millsystem.entity.MillBatchPurchase;
import com.millsystem.entity.PaddyPurchase;
import com.millsystem.exception.ResourceNotFoundException;
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

    public MillBatchService(MillBatchRepository millBatchRepository, MillRepository millRepository,
                            PaddyPurchaseRepository purchaseRepository) {
        this.millBatchRepository = millBatchRepository;
        this.millRepository = millRepository;
        this.purchaseRepository = purchaseRepository;
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
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));

        MillBatch batch = new MillBatch();
        batch.setMill(mill); batch.setBatchDate(dto.getBatchDate());
        batch.setPaddyInputQuintals(dto.getPaddyInputQuintals());
        batch.setExpectedRiceQuintals(dto.getExpectedRiceQuintals());
        batch.setNotes(dto.getNotes());

        List<MillBatchPurchase> entries = new ArrayList<>();
        if (dto.getPurchaseEntries() != null) {
            for (MillBatchDto.BatchPurchaseEntry entry : dto.getPurchaseEntries()) {
                PaddyPurchase purchase = purchaseRepository.findByIdAndMillId(entry.getPurchaseId(), millId)
                        .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + entry.getPurchaseId()));
                MillBatchPurchase mbp = new MillBatchPurchase();
                mbp.setBatch(batch); mbp.setPurchase(purchase); mbp.setQuintalsUsed(entry.getQuintalsUsed());
                entries.add(mbp);
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
