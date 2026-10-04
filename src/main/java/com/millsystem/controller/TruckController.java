package com.millsystem.controller;

import com.millsystem.dto.TruckDto;
import com.millsystem.dto.TruckPathPointDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.TruckTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trucks")
@Tag(name = "Truck Tracking", description = "Live truck positions and travel paths for a mill. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class TruckController {

    private final TruckTrackingService truckService;
    private final MillContextResolver millResolver;

    public TruckController(TruckTrackingService truckService, MillContextResolver millResolver) {
        this.truckService = truckService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List active trucks",
            description = "Returns each truck's latest known position for the mill.")
    public ResponseEntity<List<TruckDto>> getActiveTrucks(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(truckService.getActiveTrucks(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/numbers")
    @Operation(summary = "List truck numbers",
            description = "Distinct truck numbers that have reported GPS data for this mill.")
    public ResponseEntity<List<String>> getTruckNumbers(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(truckService.getTruckNumbers(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{truckNumber}/path")
    @Operation(summary = "Get truck path",
            description = "Ordered GPS points for one truck. Filter by fromDate / toDate (ISO date-time). Defaults to today.")
    public ResponseEntity<List<TruckPathPointDto>> getTruckPath(
            @PathVariable String truckNumber,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long resolvedMillId = millResolver.resolve(principal, millId);
        return ResponseEntity.ok(truckService.getTruckPath(resolvedMillId, truckNumber, fromDate, toDate));
    }
}
