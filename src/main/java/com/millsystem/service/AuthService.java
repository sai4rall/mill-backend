package com.millsystem.service;

import com.millsystem.dto.LoginRequest;
import com.millsystem.dto.LoginResponse;
import com.millsystem.security.JwtUtil;
import com.millsystem.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtUtil.generateToken(principal);
        return new LoginResponse(token, principal.getUsername(), principal.getRole().name(),
                principal.getMillId(), principal.getUsername());
    }
}
