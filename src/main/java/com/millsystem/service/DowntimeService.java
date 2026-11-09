package com.millsystem.service;

import com.millsystem.dto.DowntimeDto;
import com.millsystem.entity.MachineDowntime;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MachineDowntimeRepository;
import com.millsystem.repository.MillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Machine stoppages on the mill floor, for downtime and shift comparisons. */
@Service
public class DowntimeService {

    private final MachineDowntimeRepository repository;
    private final MillRepository mills;

    public DowntimeService(MachineDowntimeRepository repository, MillRepository mills) {
        this.repository = repository;
        this.mills = mills;
    }

    @Transactional(readOnly = true)
    public List<DowntimeDto> getAll(Long millId) {
        return repository.findByMillIdOrderByDowntimeDateDescStartTimeDesc(millId).stream().map(DowntimeDto::from).toList();
    }

    @Transactional
    public DowntimeDto create(DowntimeDto dto, Long millId, String recordedBy) {
        Mill mill = mills.findById(millId).orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        MachineDowntime d = new MachineDowntime();
        d.setMill(mill);
        d.setRecordedBy(recordedBy);
        apply(d, dto);
        return DowntimeDto.from(repository.save(d));
    }

    @Transactional
    public DowntimeDto update(Long id, DowntimeDto dto, Long millId) {
        MachineDowntime d = find(id, millId);
        apply(d, dto);
        return DowntimeDto.from(repository.save(d));
    }

    @Transactional
    public void delete(Long id, Long millId) {
        repository.delete(find(id, millId));
    }

    private void apply(MachineDowntime d, DowntimeDto dto) {
        d.setDowntimeDate(dto.getDowntimeDate());
        d.setShift(dto.getShift());
        d.setMachine(dto.getMachine());
        d.setStartTime(dto.getStartTime());
        d.setEndTime(dto.getEndTime());
        d.setDurationMinutes(MachineDowntime.minutesBetween(dto.getStartTime(), dto.getEndTime()));
        d.setReason(dto.getReason());
        d.setNotes(dto.getNotes());
    }

    private MachineDowntime find(Long id, Long millId) {
        return repository.findByIdAndMillId(id, millId).orElseThrow(() -> new ResourceNotFoundException("Downtime entry not found: " + id));
    }
}
