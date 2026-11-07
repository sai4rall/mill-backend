package com.millsystem.controller;

import com.millsystem.dto.MoistureRangeDto;
import com.millsystem.dto.MoistureTableDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.MoistureDeductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@org.springframework.validation.annotation.Validated
@Tag(name = "Moisture Deductions", description = "Moisture % bands and their price deduction. Global table (super admin) and per-mill override (mill admin).")
public class MoistureDeductionController {

    private final MoistureDeductionService service;
    private final MillContextResolver millResolver;

    public MoistureDeductionController(MoistureDeductionService service, MillContextResolver millResolver) {
        this.service = service;
        this.millResolver = millResolver;
    }

    // ---- Mill (effective table + mill admin override) ----

    @GetMapping("/api/moisture-deductions")
    @Operation(summary = "My mill's moisture table", description = "Effective ranges plus the mill's own and the global ranges. `source`: MILL, GLOBAL or NONE.")
    public ResponseEntity<MoistureTableDto> getForMill(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getForMill(millResolver.resolve(principal, millId)));
    }

    @PutMapping("/api/moisture-deductions")
    @PreAuthorize("hasRole('MILL_ADMIN')")
    @Operation(summary = "Replace my mill's moisture table (mill admin)", description = "Overrides the global table for this mill. An empty list falls back to the global table.")
    public ResponseEntity<MoistureTableDto> replaceMill(@RequestBody List<@jakarta.validation.Valid MoistureRangeDto> ranges,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.replaceMill(principal.getMillId(), ranges));
    }

    @DeleteMapping("/api/moisture-deductions")
    @PreAuthorize("hasRole('MILL_ADMIN')")
    @Operation(summary = "Reset my mill to the global table (mill admin)")
    public ResponseEntity<MoistureTableDto> resetMill(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.resetMill(principal.getMillId()));
    }

    // ---- Global (super admin; /api/admin/** is SUPER_ADMIN-only) ----

    @GetMapping("/api/admin/moisture-deductions")
    @Operation(summary = "Global moisture table")
    public ResponseEntity<List<MoistureRangeDto>> getGlobal() {
        return ResponseEntity.ok(service.getGlobal());
    }

    @PutMapping("/api/admin/moisture-deductions")
    @Operation(summary = "Replace the global moisture table (super admin)", description = "Applies to every mill without its own table.")
    public ResponseEntity<List<MoistureRangeDto>> replaceGlobal(@RequestBody List<@jakarta.validation.Valid MoistureRangeDto> ranges) {
        return ResponseEntity.ok(service.replaceGlobal(ranges));
    }
}
