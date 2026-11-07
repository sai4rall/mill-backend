package com.millsystem.controller;

import com.millsystem.dto.RejectedLorryDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.RejectedLorryService;
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
@RequestMapping("/api/rejected-lorries")
@Tag(name = "Rejected Lorries", description = "Paddy lorries turned away at the mill (not purchased). SUPER_ADMIN: pass `X-Mill-Id` header (read-only).")
public class RejectedLorryController {

    private final RejectedLorryService service;
    private final MillContextResolver millResolver;

    public RejectedLorryController(RejectedLorryService service, MillContextResolver millResolver) {
        this.service = service;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List rejected lorries", description = "Newest first.")
    public ResponseEntity<List<RejectedLorryDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getAll(millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Record a rejected lorry",
            description = "Reasons: HIGH_MOISTURE, POOR_QUALITY, FOREIGN_MATTER, WRONG_VARIETY, OTHER (OTHER needs notes). Does not affect stock or payments.")
    public ResponseEntity<RejectedLorryDto> create(
            @Valid @RequestBody RejectedLorryDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(dto, millResolver.resolve(principal, millId), principal.getUsername()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Correct a rejected lorry entry")
    public ResponseEntity<RejectedLorryDto> update(
            @PathVariable Long id, @Valid @RequestBody RejectedLorryDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.update(id, dto, millResolver.resolve(principal, millId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a rejected lorry entry (entered by mistake)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, millResolver.resolve(principal, millId));
        return ResponseEntity.noContent().build();
    }
}
