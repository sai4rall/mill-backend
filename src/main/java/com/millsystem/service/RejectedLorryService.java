package com.millsystem.service;

import com.millsystem.dto.RejectedLorryDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.RejectedLorry;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.RejectedLorryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lorries turned away at the mill. Records only: no effect on stock, purchases or payments. */
@Service
public class RejectedLorryService {

    private final RejectedLorryRepository repository;
    private final MillRepository mills;
    private final FarmerRepository farmers;

    public RejectedLorryService(RejectedLorryRepository repository, MillRepository mills, FarmerRepository farmers) {
        this.repository = repository;
        this.mills = mills;
        this.farmers = farmers;
    }

    @Transactional(readOnly = true)
    public List<RejectedLorryDto> getAll(Long millId) {
        return repository.findByMillIdOrderByRejectionDateDescIdDesc(millId).stream().map(RejectedLorryDto::from).toList();
    }

    @Transactional
    public RejectedLorryDto create(RejectedLorryDto dto, Long millId, String recordedBy) {
        Mill mill = mills.findById(millId).orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        RejectedLorry r = new RejectedLorry();
        r.setMill(mill);
        r.setRecordedBy(recordedBy);
        apply(r, dto, millId);
        return RejectedLorryDto.from(repository.save(r));
    }

    @Transactional
    public RejectedLorryDto update(Long id, RejectedLorryDto dto, Long millId) {
        RejectedLorry r = find(id, millId);
        apply(r, dto, millId);
        return RejectedLorryDto.from(repository.save(r));
    }

    @Transactional
    public void delete(Long id, Long millId) {
        repository.delete(find(id, millId));
    }

    private void apply(RejectedLorry r, RejectedLorryDto dto, Long millId) {
        // The farmer / agent / supplier must belong to this mill.
        r.setFarmer(dto.getFarmerId() == null ? null : farmers.findByIdAndMillId(dto.getFarmerId(), millId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + dto.getFarmerId())));
        r.setRejectionDate(dto.getRejectionDate());
        r.setTruckNumber(dto.getTruckNumber());
        r.setApproxWeightQuintals(dto.getApproxWeightQuintals());
        r.setMoisturePct(dto.getMoisturePct());
        r.setReason(dto.getReason());
        r.setNotes(dto.getNotes());
    }

    private RejectedLorry find(Long id, Long millId) {
        return repository.findByIdAndMillId(id, millId).orElseThrow(() -> new ResourceNotFoundException("Rejected lorry not found: " + id));
    }
}
