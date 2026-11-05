package com.millsystem.controller;

import com.millsystem.dto.BrandingDto;
import com.millsystem.entity.MillLogo;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.MillBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/** Branding of the logged-in user's own mill (for the app header). Users without a mill get 204 No Content. */
@RestController
@RequestMapping("/api/mill")
@Tag(name = "Mill Branding", description = "Name and logo of the current user's mill.")
public class MillBrandingController {

    private final MillBrandingService branding;

    public MillBrandingController(MillBrandingService branding) {
        this.branding = branding;
    }

    @GetMapping("/branding")
    @Operation(summary = "My mill's name and logo status")
    public ResponseEntity<BrandingDto> getBranding(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getMillId() == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(branding.getBranding(principal.getMillId()));
    }

    @GetMapping("/logo")
    @Operation(summary = "My mill's logo image", description = "Clients add `?v=<logoUpdatedAt>` so a new logo bypasses the cache.")
    public ResponseEntity<byte[]> getLogo(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getMillId() == null) return ResponseEntity.noContent().build();
        MillLogo logo = branding.getLogo(principal.getMillId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.getContentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate()).body(logo.getData());
    }
}
