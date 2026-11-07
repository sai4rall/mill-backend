package com.millsystem.controller;

import com.millsystem.dto.ShipmentDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.ShipmentService;
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
@RequestMapping("/api/shipments")
@Tag(name = "Export Shipments", description = "Rice dispatches against export orders. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final MillContextResolver millResolver;

    public ShipmentController(ShipmentService shipmentService, MillContextResolver millResolver) {
        this.shipmentService = shipmentService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List shipments", description = "All shipments of the mill, newest first; pass orderId to limit to one order.")
    public ResponseEntity<List<ShipmentDto>> getByOrder(
            @RequestParam(required = false) Long orderId,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long mill = millResolver.resolve(principal, millId);
        return ResponseEntity.ok(orderId == null ? shipmentService.getAll(mill) : shipmentService.getByOrder(orderId, mill));
    }

    @PostMapping
    @Operation(summary = "Record shipment", description = "Logs a rice dispatch. Order status updates automatically (PENDING → PARTIAL → FULFILLED).")
    public ResponseEntity<ShipmentDto> create(
            @Valid @RequestBody ShipmentDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shipmentService.create(dto, millResolver.resolve(principal, millId)));
    }
}
