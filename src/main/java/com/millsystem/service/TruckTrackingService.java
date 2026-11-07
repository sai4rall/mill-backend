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
    /** Stores a ping for the mill that owns the device key (resolved by the controller). */
    public void recordPing(Mill mill, GpsPingDto dto) {
        if (dto.getMillId() != null && !dto.getMillId().equals(mill.getId())) {
            throw new IllegalArgumentException("millId does not match the device key");
        }
        LocalDateTime now = LocalDateTime.now();
        if (dto.getRecordedAt() != null && (dto.getRecordedAt().isAfter(now.plusMinutes(5)) || dto.getRecordedAt().isBefore(now.minusDays(7)))) {
            throw new IllegalArgumentException("recordedAt must be within the last 7 days and not in the future");
        }

        TruckLocation loc = new TruckLocation();
        loc.setMill(mill);
        loc.setTruckNumber(dto.getTruckNumber().toUpperCase().trim());
        loc.setLatitude(dto.getLatitude());
        loc.setLongitude(dto.getLongitude());
        loc.setSpeed(dto.getSpeed());
        loc.setRecordedAt(dto.getRecordedAt() != null ? dto.getRecordedAt() : now);
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
