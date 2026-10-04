package com.millsystem.controller;

import com.millsystem.dto.PurchaseDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.PurchaseService;
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
@RequestMapping("/api/purchases")
@Tag(name = "Paddy Purchases", description = "Paddy procurement from farmers. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final MillContextResolver millResolver;

    public PurchaseController(PurchaseService purchaseService, MillContextResolver millResolver) {
        this.purchaseService = purchaseService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List purchases", description = "Returns all paddy purchases with live payment summary.")
    public ResponseEntity<List<PurchaseDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(purchaseService.getAll(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase")
    public ResponseEntity<PurchaseDto> getById(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(purchaseService.getById(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Create purchase", description = "Records a new paddy purchase. Total = weight × price per quintal.")
    public ResponseEntity<PurchaseDto> create(
            @Valid @RequestBody PurchaseDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseService.create(dto, millResolver.resolve(principal, millId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update purchase", description = "Update notes or manually override payment status.")
    public ResponseEntity<PurchaseDto> update(
            @PathVariable Long id,
            @RequestBody PurchaseDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(purchaseService.update(id, dto, millResolver.resolve(principal, millId)));
    }
}
