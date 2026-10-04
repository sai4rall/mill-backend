package com.millsystem.controller;

import com.millsystem.dto.CreateMillRequest;
import com.millsystem.dto.CreateMillResponse;
import com.millsystem.dto.MillDto;
import com.millsystem.service.MillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/mills")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin — Mills", description = "Mill lifecycle management. Requires SUPER_ADMIN role.")
public class AdminMillController {

    private final MillService millService;

    public AdminMillController(MillService millService) {
        this.millService = millService;
    }

    @GetMapping
    @Operation(summary = "List all mills", description = "Returns every mill in the system with its admin username and status.")
    public ResponseEntity<List<MillDto>> getAllMills() {
        return ResponseEntity.ok(millService.getAllMills());
    }

    @PostMapping
    @Operation(summary = "Create mill", description = "Creates a new mill and auto-generates a MILL_ADMIN account. The generated password is returned once — save it immediately.")
    public ResponseEntity<CreateMillResponse> createMill(@Valid @RequestBody CreateMillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(millService.createMill(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update mill details", description = "Update name, location, or contact info of a mill.")
    public ResponseEntity<MillDto> updateMill(@PathVariable Long id, @Valid @RequestBody CreateMillRequest request) {
        return ResponseEntity.ok(millService.updateMill(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle mill status", description = "Switches a mill between ACTIVE and INACTIVE. Inactive mill logins are blocked.")
    public ResponseEntity<Map<String, String>> toggleStatus(@PathVariable Long id) {
        millService.toggleStatus(id);
        return ResponseEntity.ok(Map.of("message", "Mill status updated"));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset mill admin password", description = "Generates a new random password for the mill's MILL_ADMIN account. The new password is returned once — save it immediately.")
    public ResponseEntity<Map<String, String>> resetAdminPassword(@PathVariable Long id) {
        String newPassword = millService.resetAdminPassword(id);
        return ResponseEntity.ok(Map.of(
                "generatedPassword", newPassword,
                "message", "Password reset successfully. Save this password — it will not be shown again."
        ));
    }
}
