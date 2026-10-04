package com.millsystem.controller;

import com.millsystem.dto.OperatorDto;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.OperatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mill/operators")
@PreAuthorize("hasRole('MILL_ADMIN')")
@Tag(name = "Mill Admin — Operators", description = "Operator account management within the authenticated mill. Requires MILL_ADMIN role.")
public class OperatorController {

    private final OperatorService operatorService;

    public OperatorController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @GetMapping
    @Operation(summary = "List operators", description = "Returns all MILL_OPERATOR accounts belonging to the authenticated mill.")
    public ResponseEntity<List<OperatorDto>> getOperators(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(operatorService.getOperators(principal.getMillId()));
    }

    @PostMapping
    @Operation(summary = "Create operator", description = "Creates a MILL_OPERATOR account with an auto-generated password. The password is returned once.")
    public ResponseEntity<OperatorDto> createOperator(@AuthenticationPrincipal UserPrincipal principal,
                                                       @Valid @RequestBody OperatorDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(operatorService.createOperator(principal.getMillId(), request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle operator status", description = "Enable or disable an operator account.")
    public ResponseEntity<Map<String, String>> toggleStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long id) {
        operatorService.toggleStatus(principal.getMillId(), id);
        return ResponseEntity.ok(Map.of("message", "Operator status updated"));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset operator password", description = "Generates a new password for an operator. Returned once — save immediately.")
    public ResponseEntity<Map<String, String>> resetPassword(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id) {
        String newPassword = operatorService.resetPassword(principal.getMillId(), id);
        return ResponseEntity.ok(Map.of(
                "generatedPassword", newPassword,
                "message", "Password reset. Save this — it will not be shown again."
        ));
    }
}
