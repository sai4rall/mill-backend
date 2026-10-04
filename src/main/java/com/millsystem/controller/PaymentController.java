package com.millsystem.controller;

import com.millsystem.dto.PaymentDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.PaymentService;
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
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment instalments against paddy purchases. Auto-updates purchase status. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class PaymentController {

    private final PaymentService paymentService;
    private final MillContextResolver millResolver;

    public PaymentController(PaymentService paymentService, MillContextResolver millResolver) {
        this.paymentService = paymentService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List payments for a purchase")
    public ResponseEntity<List<PaymentDto>> getByPurchase(
            @RequestParam Long purchaseId,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.getByPurchase(purchaseId, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Record payment", description = "Logs a payment instalment. Purchase status updates automatically (PENDING → PARTIAL → PAID).")
    public ResponseEntity<PaymentDto> create(
            @Valid @RequestBody PaymentDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(dto, millResolver.resolve(principal, millId)));
    }
}
