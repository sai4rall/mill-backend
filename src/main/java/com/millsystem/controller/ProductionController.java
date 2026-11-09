package com.millsystem.controller;

import com.millsystem.dto.ProductionReportDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.ProductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Production Monitoring", description = "Daily production, recovery %, losses, downtime and shift / mill comparisons.")
public class ProductionController {

    private final ProductionService service;
    private final MillContextResolver millResolver;

    public ProductionController(ProductionService service, MillContextResolver millResolver) {
        this.service = service;
        this.millResolver = millResolver;
    }

    @GetMapping("/api/production")
    @Operation(summary = "Production report for a mill",
            description = "Totals, one row per day and per duty shift, and downtime by reason and machine. Defaults to the last 30 days; at most 366 days. "
                    + "SUPER_ADMIN: pass `X-Mill-Id` (own company's mills only).")
    public ResponseEntity<ProductionReportDto> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.report(millResolver.resolve(principal, millId), from, to));
    }

    @GetMapping("/api/admin/production")
    @Operation(summary = "Compare production across my mills (super admin)", description = "One row per mill of my company with its totals for the period.")
    public ResponseEntity<List<Map<String, Object>>> compareMills(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.compareMills(principal.getId(), from, to));
    }
}
