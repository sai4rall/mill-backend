package com.millsystem.controller;

import com.millsystem.dto.GpsPingDto;
import com.millsystem.service.TruckTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gps")
@Tag(name = "GPS Device Ingest", description = "Public endpoint for GPS devices on trucks to report location. No authentication — identify the truck by millId + truckNumber in the body.")
public class GpsController {

    private final TruckTrackingService truckService;

    public GpsController(TruckTrackingService truckService) {
        this.truckService = truckService;
    }

    @PostMapping("/ping")
    @Operation(summary = "Report truck location",
            description = "A GPS device POSTs its current position here. Body: millId, truckNumber, latitude, longitude, optional speed and recordedAt (defaults to now).")
    public ResponseEntity<Map<String, String>> ping(@Valid @RequestBody GpsPingDto dto) {
        truckService.recordPing(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Location recorded for truck " + dto.getTruckNumber()));
    }
}
