package com.millsystem.controller;

import com.millsystem.dto.FeatureFlagDto;
import com.millsystem.entity.User;
import com.millsystem.feature.FeatureFlagService;
import com.millsystem.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/features")
@Tag(name = "Feature Flags", description = "Which modules are shown in the menu and enabled for the current user's mill.")
public class FeatureController {

    private final FeatureFlagService flags;

    public FeatureController(FeatureFlagService flags) {
        this.flags = flags;
    }

    @GetMapping
    @Operation(summary = "Effective feature flags for my mill",
            description = "Mill users get global flags with their mill's overrides applied. SUPER_ADMIN gets global flags, or a mill's flags with `X-Mill-Id`.")
    public ResponseEntity<List<FeatureFlagDto>> getMine(
            @Parameter(description = "Optional for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long headerMillId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean platformUser = principal.getRole() == User.Role.SUPER_ADMIN || principal.getRole() == User.Role.DEV_ADMIN;
        Long millId = platformUser ? headerMillId : principal.getMillId();
        return ResponseEntity.ok(flags.getEffective(millId));
    }
}
