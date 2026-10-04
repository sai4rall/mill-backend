package com.millsystem.service;

import com.millsystem.dto.GpsPingDto;
import com.millsystem.dto.TruckDto;
import com.millsystem.dto.TruckPathPointDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.TruckLocation;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.TruckLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TruckTrackingService {

    private final TruckLocationRepository truckRepo;
    private final MillRepository millRepo;

    public TruckTrackingService(TruckLocationRepository truckRepo, MillRepository millRepo) {
        this.truckRepo = truckRepo;
        this.millRepo = millRepo;
    }

    @Transactional
    public void recordPing(GpsPingDto dto) {
        Mill mill = millRepo.findById(dto.getMillId())
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + dto.getMillId()));

        TruckLocation loc = new TruckLocation();
        loc.setMill(mill);
        loc.setTruckNumber(dto.getTruckNumber().toUpperCase().trim());
        loc.setLatitude(dto.getLatitude());
        loc.setLongitude(dto.getLongitude());
        loc.setSpeed(dto.getSpeed());
        loc.setRecordedAt(dto.getRecordedAt() != null ? dto.getRecordedAt() : LocalDateTime.now());
        truckRepo.save(loc);
    }

    public List<TruckDto> getActiveTrucks(Long millId) {
        return truckRepo.findLatestPerTruckByMillId(millId).stream()
                .map(t -> new TruckDto(t.getTruckNumber(), t.getLatitude(), t.getLongitude(),
                        t.getSpeed(), t.getRecordedAt()))
                .collect(Collectors.toList());
    }

    public List<TruckPathPointDto> getTruckPath(Long millId, String truckNumber,
                                                  LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDate.now().atStartOfDay();
        LocalDateTime end   = to   != null ? to   : LocalDateTime.now();
        return truckRepo.findByMillIdAndTruckNumberAndRecordedAtBetweenOrderByRecordedAtAsc(
                millId, truckNumber.toUpperCase().trim(), start, end)
                .stream().map(TruckPathPointDto::from).collect(Collectors.toList());
    }

    public List<String> getTruckNumbers(Long millId) {
        return truckRepo.findDistinctTruckNumbersByMillId(millId);
    }
}
