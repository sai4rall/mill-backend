package com.millsystem.controller;

import com.millsystem.dto.*;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillLogo;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.DevAccountService;
import com.millsystem.feature.FeatureFlagService;
import com.millsystem.service.MillBrandingService;
import com.millsystem.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** DEV_ADMIN only (secured by the /api/dev/** rule in SecurityConfig). */
@RestController
@RequestMapping("/api/dev")
@Tag(name = "Dev Admin", description = "Feature flags, mill logos, mills, super admins and every account's password. DEV_ADMIN only.")
public class DevAdminController {

    private final FeatureFlagService flags;
    private final MillBrandingService branding;
    private final SuperAdminService superAdmins;
    private final DevAccountService accounts;

    public DevAdminController(FeatureFlagService flags, MillBrandingService branding, SuperAdminService superAdmins,
                              DevAccountService accounts) {
        this.flags = flags;
        this.branding = branding;
        this.superAdmins = superAdmins;
        this.accounts = accounts;
    }

    // ---- Feature flags ----

    @GetMapping("/features")
    @Operation(summary = "List global feature flags")
    public ResponseEntity<List<FeatureFlagDto>> getGlobal() {
        return ResponseEntity.ok(flags.getGlobal());
    }

    @PutMapping("/features/{key}")
    @Operation(summary = "Update a global feature flag", description = "Body: `{showInMenu?, enabled?}` — omitted/null fields are unchanged.")
    public ResponseEntity<FeatureFlagDto> updateGlobal(@PathVariable String key, @RequestBody FeatureFlagUpdateRequest req) {
        return ResponseEntity.ok(flags.updateGlobal(key, req));
    }

    @GetMapping("/mills/{millId}/features")
    @Operation(summary = "List a mill's flags", description = "Global value, the mill's override (null = inherit) and the effective value for each module.")
    public ResponseEntity<List<MillFeatureFlagDto>> getMillFlags(@PathVariable Long millId) {
        branding.getBranding(millId); // 404 for unknown mill
        return ResponseEntity.ok(flags.getMillFlags(millId));
    }

    @PutMapping("/mills/{millId}/features/{key}")
    @Operation(summary = "Override a flag for one mill",
            description = "Body: `{showInMenu?, enabled?}` — a null field inherits the global value; both null removes the override.")
    public ResponseEntity<MillFeatureFlagDto> setOverride(@PathVariable Long millId, @PathVariable String key,
                                                          @RequestBody FeatureFlagUpdateRequest req) {
        return ResponseEntity.ok(flags.setOverride(millId, key, req));
    }

    @DeleteMapping("/mills/{millId}/features/{key}")
    @Operation(summary = "Remove a mill's override", description = "The mill goes back to the global value.")
    public ResponseEntity<Void> clearOverride(@PathVariable Long millId, @PathVariable String key) {
        flags.clearOverride(millId, key);
        return ResponseEntity.noContent().build();
    }

    // ---- Mills & logos ----

    @GetMapping("/mills")
    @Operation(summary = "List all mills with their logo status")
    public ResponseEntity<List<BrandingDto>> getMills() {
        return ResponseEntity.ok(branding.getAllMills());
    }

    @GetMapping("/mills/{millId}/logo")
    @Operation(summary = "Get a mill's logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable Long millId) {
        MillLogo logo = branding.getLogo(millId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.getContentType()))
                .cacheControl(CacheControl.noCache()).body(logo.getData());
    }

    @PutMapping(value = "/mills/{millId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload / replace a mill's logo", description = "Multipart field `file`: PNG, JPEG or WebP, max 1 MB.")
    public ResponseEntity<BrandingDto> uploadLogo(@PathVariable Long millId, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(branding.uploadLogo(millId, file));
    }

    @DeleteMapping("/mills/{millId}/logo")
    @Operation(summary = "Remove a mill's logo")
    public ResponseEntity<Void> deleteLogo(@PathVariable Long millId) {
        branding.deleteLogo(millId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/mills/{millId}/status")
    @Operation(summary = "Activate / deactivate a mill",
            description = "Body: `{\"status\": \"ACTIVE\" | \"INACTIVE\"}`. An inactive mill's users are signed out and cannot sign in.")
    public ResponseEntity<Map<String, String>> setMillStatus(@PathVariable Long millId, @RequestBody Map<String, String> body) {
        Mill.MillStatus status;
        try {
            status = Mill.MillStatus.valueOf(String.valueOf(body.get("status")).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status must be ACTIVE or INACTIVE");
        }
        Mill mill = accounts.setMillStatus(millId, status);
        return ResponseEntity.ok(Map.of("millId", String.valueOf(mill.getId()), "status", mill.getStatus().name()));
    }

    @DeleteMapping("/mills/{millId}")
    @Operation(summary = "Delete a mill and all its data",
            description = "Permanent. Removes the mill, its users, farmers, purchases, payments, batches, orders, shipments, GPS points and settings. "
                    + "`confirm` must be the mill's exact name. Returns the rows removed per kind.")
    public ResponseEntity<Map<String, Integer>> deleteMill(@PathVariable Long millId, @RequestParam(required = false) String confirm) {
        return ResponseEntity.ok(accounts.deleteMill(millId, confirm));
    }

    @PutMapping("/mills/{millId}/owner")
    @Operation(summary = "Assign a mill to a company (super admin)",
            description = "Body: `{\"ownerId\": <super admin user id>}`. Only that super admin will see and manage the mill.")
    public ResponseEntity<BrandingDto> assignOwner(@PathVariable Long millId, @RequestBody Map<String, Long> body) {
        Long ownerId = body.get("ownerId");
        if (ownerId == null) throw new IllegalArgumentException("ownerId is required");
        return ResponseEntity.ok(BrandingDto.withOwner(accounts.assignOwner(millId, ownerId)));
    }

    // ---- All accounts ----

    @GetMapping("/users")
    @Operation(summary = "List every account", description = "All roles, with their mill. No password data.")
    public ResponseEntity<List<UserAccountDto>> getUsers() {
        return ResponseEntity.ok(accounts.getAllUsers());
    }

    @PostMapping("/users/{id}/reset-password")
    @Operation(summary = "Reset any account's password",
            description = "Generates a new password, returned once in `generatedPassword`. The user's existing sessions end. Not for your own account.")
    public ResponseEntity<Map<String, String>> resetUserPassword(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(Map.of("generatedPassword", accounts.resetPassword(principal.getId(), id)));
    }

    // ---- Super admins ----

    @GetMapping("/super-admins")
    @Operation(summary = "List super admin accounts")
    public ResponseEntity<List<SuperAdminDto>> getSuperAdmins() {
        return ResponseEntity.ok(superAdmins.getAll());
    }

    @PostMapping("/super-admins")
    @Operation(summary = "Create a super admin", description = "A password is generated and returned once in `generatedPassword`.")
    public ResponseEntity<SuperAdminDto> createSuperAdmin(@Valid @RequestBody SuperAdminDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(superAdmins.create(request));
    }

    @PatchMapping("/super-admins/{id}/status")
    @Operation(summary = "Enable / disable a super admin")
    public ResponseEntity<SuperAdminDto> toggleSuperAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(superAdmins.toggleStatus(id));
    }

    @PostMapping("/super-admins/{id}/reset-password")
    @Operation(summary = "Reset a super admin's password", description = "Returns the new password once.")
    public ResponseEntity<Map<String, String>> resetSuperAdminPassword(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("generatedPassword", superAdmins.resetPassword(id)));
    }

    @DeleteMapping("/super-admins/{id}")
    @Operation(summary = "Delete a super admin", description = "Permanent. Their sessions stop working immediately.")
    public ResponseEntity<Void> deleteSuperAdmin(@PathVariable Long id) {
        accounts.deleteSuperAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
