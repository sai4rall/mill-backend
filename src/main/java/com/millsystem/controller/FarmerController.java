package com.millsystem.controller;

import com.millsystem.dto.FarmerDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.FarmerService;
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
@RequestMapping("/api/farmers")
@Tag(name = "Farmers", description = "Farmer registry for a mill. MILL_ADMIN/OPERATOR: uses their own mill. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class FarmerController {

    private final FarmerService farmerService;
    private final MillContextResolver millResolver;

    public FarmerController(FarmerService farmerService, MillContextResolver millResolver) {
        this.farmerService = farmerService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List farmers")
    public ResponseEntity<List<FarmerDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getAll(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get farmer")
    public ResponseEntity<FarmerDto> getById(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getById(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Create farmer")
    public ResponseEntity<FarmerDto> create(
            @Valid @RequestBody FarmerDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmerService.create(dto, millResolver.resolve(principal, millId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update farmer")
    public ResponseEntity<FarmerDto> update(
            @PathVariable Long id,
            @Valid @RequestBody FarmerDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.update(id, dto, millResolver.resolve(principal, millId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete farmer")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        farmerService.delete(id, millResolver.resolve(principal, millId));
        return ResponseEntity.noContent().build();
    }
}
