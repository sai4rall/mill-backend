package com.millsystem.controller;

import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.GpsDeviceKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mill/gps-key")
@Tag(name = "GPS Device Key", description = "Mill admin: the key GPS devices send as X-Device-Key")
public class GpsKeyController {

    private final GpsDeviceKeyService deviceKeys;
    private final MillContextResolver millResolver;

    public GpsKeyController(GpsDeviceKeyService deviceKeys, MillContextResolver millResolver) {
        this.deviceKeys = deviceKeys;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "Whether a device key exists and when it was generated (the key itself is never shown again)")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal UserPrincipal principal) {
        Long millId = millResolver.resolve(principal, null);
        Map<String, Object> body = new HashMap<>();
        deviceKeys.createdAt(millId).ifPresentOrElse(
                at -> { body.put("configured", true); body.put("createdAt", at); },
                () -> body.put("configured", false));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/rotate")
    @Operation(summary = "Generate a new device key (the old one stops working). Returned once.")
    public ResponseEntity<Map<String, String>> rotate(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("deviceKey", deviceKeys.rotate(millResolver.resolve(principal, null))));
    }
}
