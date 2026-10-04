package com.millsystem.controller;

import com.millsystem.dto.MillBatchDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.MillBatchService;
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
@RequestMapping("/api/mill-batches")
@Tag(name = "Mill Batches", description = "Paddy-to-rice processing batches. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class MillBatchController {

    private final MillBatchService millBatchService;
    private final MillContextResolver millResolver;

    public MillBatchController(MillBatchService millBatchService, MillContextResolver millResolver) {
        this.millBatchService = millBatchService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List batches")
    public ResponseEntity<List<MillBatchDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(millBatchService.getAll(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch")
    public ResponseEntity<MillBatchDto> getById(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(millBatchService.getById(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Start batch", description = "Creates a new IN_PROGRESS batch.")
    public ResponseEntity<MillBatchDto> create(
            @Valid @RequestBody MillBatchDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(millBatchService.create(dto, millResolver.resolve(principal, millId)));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Complete batch", description = "Marks batch COMPLETED. Provide `actualRiceQuintals` — conversion rate is auto-calculated.")
    public ResponseEntity<MillBatchDto> complete(
            @PathVariable Long id,
            @RequestBody MillBatchDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(millBatchService.completeBatch(id, dto, millResolver.resolve(principal, millId)));
    }
}
