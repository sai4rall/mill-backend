package com.millsystem.controller;

import com.millsystem.dto.ChangePasswordRequest;
import com.millsystem.dto.LoginRequest;
import com.millsystem.dto.LoginResponse;
import com.millsystem.security.UserPrincipal;
import com.millsystem.service.AuthService;
import com.millsystem.service.PasswordChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login endpoint for all roles")
public class AuthController {

    private final AuthService authService;
    private final PasswordChangeService passwordChangeService;
    private final com.millsystem.security.LoginAttemptService loginAttempts;
    private final com.millsystem.security.JwtUtil jwtUtil;
    private final com.millsystem.security.TokenRevocationService revocationService;

    public AuthController(AuthService authService, PasswordChangeService passwordChangeService,
                          com.millsystem.security.LoginAttemptService loginAttempts, com.millsystem.security.JwtUtil jwtUtil,
                          com.millsystem.security.TokenRevocationService revocationService) {
        this.authService = authService;
        this.passwordChangeService = passwordChangeService;
        this.loginAttempts = loginAttempts;
        this.jwtUtil = jwtUtil;
        this.revocationService = revocationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username and password. Returns a JWT token to use in the Authorization header as `Bearer <token>`.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest http) {
        // Brute-force protection: repeated failures lock the account / client IP for a while (429).
        String username = com.millsystem.service.UsernamePolicy.normalize(request.getUsername());
        String ip = http.getRemoteAddr();
        loginAttempts.checkAllowed(username, ip);
        try {
            LoginResponse response = authService.login(request);
            loginAttempts.recordSuccess(username);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            loginAttempts.recordFailure(username, ip);
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out", description = "Revokes the presented token so it stops working immediately. Always 204.")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                revocationService.revoke(jwtUtil.extractAllClaims(authorization.substring(7)));
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
                // Expired or invalid tokens are already unusable.
            }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change my password",
            description = "Any signed-in user. Requires the current password. Signs out other sessions and returns a fresh token.")
    public ResponseEntity<LoginResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(passwordChangeService.changePassword(principal.getUsername(), request));
    }
}
