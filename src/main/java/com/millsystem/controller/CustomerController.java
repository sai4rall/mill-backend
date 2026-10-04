package com.millsystem.controller;

import com.millsystem.dto.CustomerDto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.CustomerService;
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
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Export buyers for a mill. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class CustomerController {

    private final CustomerService customerService;
    private final MillContextResolver millResolver;

    public CustomerController(CustomerService customerService, MillContextResolver millResolver) {
        this.customerService = customerService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List customers")
    public ResponseEntity<List<CustomerDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(customerService.getAll(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer")
    public ResponseEntity<CustomerDto> getById(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(customerService.getById(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Create customer")
    public ResponseEntity<CustomerDto> create(
            @Valid @RequestBody CustomerDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.create(dto, millResolver.resolve(principal, millId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer")
    public ResponseEntity<CustomerDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(customerService.update(id, dto, millResolver.resolve(principal, millId)));
    }
}
