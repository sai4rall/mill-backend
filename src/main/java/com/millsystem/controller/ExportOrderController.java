package com.millsystem.controller;

import com.millsystem.dto.ExportOrderDto;
import com.millsystem.entity.ExportOrder;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.ExportOrderService;
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
@RequestMapping("/api/orders")
@Tag(name = "Export Orders", description = "Rice export orders placed by customers. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class ExportOrderController {

    private final ExportOrderService orderService;
    private final MillContextResolver millResolver;

    public ExportOrderController(ExportOrderService orderService, MillContextResolver millResolver) {
        this.orderService = orderService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Returns all export orders with live shipped and remaining quantities.")
    public ResponseEntity<List<ExportOrderDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getAll(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order")
    public ResponseEntity<ExportOrderDto> getById(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getById(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Create order")
    public ResponseEntity<ExportOrderDto> create(
            @Valid @RequestBody ExportOrderDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(dto, millResolver.resolve(principal, millId)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Manually override order status (e.g. CANCELLED).")
    public ResponseEntity<ExportOrderDto> updateStatus(
            @PathVariable Long id,
            @RequestParam ExportOrder.OrderStatus status,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.updateStatus(id, status, millResolver.resolve(principal, millId)));
    }
}
