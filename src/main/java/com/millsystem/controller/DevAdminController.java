package com.millsystem.controller;

import com.millsystem.dto.*;
import com.millsystem.entity.MillLogo;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** DEV_ADMIN only (secured by the /api/dev/** rule in SecurityConfig). */
@RestController
@RequestMapping("/api/dev")
@Tag(name = "Dev Admin", description = "Feature flags, mill logos and super admin accounts. DEV_ADMIN only.")
public class DevAdminController {

    private final FeatureFlagService flags;
    private final MillBrandingService branding;
    private final SuperAdminService superAdmins;

    public DevAdminController(FeatureFlagService flags, MillBrandingService branding, SuperAdminService superAdmins) {
        this.flags = flags;
        this.branding = branding;
        this.superAdmins = superAdmins;
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
}
