package com.millsystem.controller;

import com.millsystem.dto.FarmerDto;
import com.millsystem.dto.OtpSendRequest;
import com.millsystem.dto.OtpVerifyRequest;
import com.millsystem.entity.FarmerPhoto;
import com.millsystem.security.MillContextResolver;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.FarmerService;
import com.millsystem.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/farmers")
@Tag(name = "Farmers", description = "Farmer registry for a mill. MILL_ADMIN/OPERATOR: uses their own mill. SUPER_ADMIN: pass `X-Mill-Id` header.")
public class FarmerController {

    private final FarmerService farmerService;
    private final OtpService otpService;
    private final MillContextResolver millResolver;

    public FarmerController(FarmerService farmerService, OtpService otpService, MillContextResolver millResolver) {
        this.farmerService = farmerService;
        this.otpService = otpService;
        this.millResolver = millResolver;
    }

    @GetMapping
    @Operation(summary = "List farmers")
    public ResponseEntity<List<FarmerDto>> getAll(
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getAll(millResolver.resolve(principal, millId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get farmer")
    public ResponseEntity<FarmerDto> getById(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.getById(id, millResolver.resolve(principal, millId)));
    }

    @PostMapping
    @Operation(summary = "Create farmer")
    public ResponseEntity<FarmerDto> create(
            @Valid @RequestBody FarmerDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmerService.create(dto, millResolver.resolve(principal, millId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update farmer")
    public ResponseEntity<FarmerDto> update(
            @PathVariable Long id,
            @Valid @RequestBody FarmerDto dto,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.update(id, dto, millResolver.resolve(principal, millId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete farmer")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        farmerService.delete(id, millResolver.resolve(principal, millId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Send mobile OTP",
            description = "Sends a 6-digit OTP to the farmer's mobile number. Required before registering a farmer or changing their number.")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody OtpSendRequest request,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        int expiresIn = otpService.send(millResolver.resolve(principal, millId), request.getPhone());
        return ResponseEntity.ok(Map.of("message", "OTP sent", "expiresInSeconds", expiresIn));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify mobile OTP",
            description = "Returns a single-use `verificationToken` to send as `phoneVerificationToken` when creating/updating the farmer.")
    public ResponseEntity<Map<String, String>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        String token = otpService.verify(millResolver.resolve(principal, millId), request.getPhone(), request.getOtp());
        return ResponseEntity.ok(Map.of("verificationToken", token));
    }

    @GetMapping("/{id}/photo")
    @Operation(summary = "Get farmer photo")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        FarmerPhoto photo = farmerService.getPhoto(id, millResolver.resolve(principal, millId));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .body(photo.getData());
    }

    @PutMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload / replace farmer photo", description = "Multipart field `file`: JPEG, PNG or WebP, max 2 MB.")
    public ResponseEntity<FarmerDto> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(farmerService.uploadPhoto(id, file, millResolver.resolve(principal, millId)));
    }

    @DeleteMapping("/{id}/photo")
    @Operation(summary = "Remove farmer photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long id,
            @Parameter(description = "Required for SUPER_ADMIN. Ignored for MILL_ADMIN/OPERATOR.")
            @RequestHeader(value = "X-Mill-Id", required = false) Long millId,
            @AuthenticationPrincipal UserPrincipal principal) {
        farmerService.deletePhoto(id, millResolver.resolve(principal, millId));
        return ResponseEntity.noContent().build();
    }
}
