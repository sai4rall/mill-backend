package com.millsystem.controller;

import com.millsystem.dto.DowntimeDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.DowntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/downtime")
@Tag(name = "Machine Downtime", description = "Machine stoppages on the mill floor. SUPER_ADMIN: pass `X-Mill-Id` header (read-only).")
public class DowntimeController {

    private final DowntimeService service;
    private final MillContextResolver millResolver;

    public DowntimeController(DowntimeService service, MillContextResolver millResolver) {
        this.service = service;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List downtime entries", description = "Newest first.")
    public ResponseEntity<List<DowntimeDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getAll(millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Record a machine stoppage",
            description = "Shifts: MORNING, AFTERNOON, NIGHT. Reasons: BREAKDOWN, POWER_CUT, MAINTENANCE, NO_PADDY, LABOUR_SHORTAGE, OTHER (OTHER needs notes). "
                    + "Times as HH:mm; an end time before the start time means the stoppage ran past midnight.")
    public ResponseEntity<DowntimeDto> create(
            @Valid @RequestBody DowntimeDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto, millResolver.resolve(principal, millId), principal.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Correct a downtime entry")
    public ResponseEntity<DowntimeDto> update(
            @PathVariable Long id, @Valid @RequestBody DowntimeDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, dto, millResolver.resolve(principal, millId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a downtime entry (entered by mistake)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, millResolver.resolve(principal, millId));
        return ResponseEntity.noContent().build();
    }
}
