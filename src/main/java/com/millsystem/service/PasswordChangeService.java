package com.millsystem.service;

import com.millsystem.dto.ChangePasswordRequest;
import com.millsystem.dto.LoginResponse;
import com.millsystem.entity.User;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.UserRepository;
import com.millsystem.security.JwtUtil;
import com.millsystem.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets any signed-in user change their own password. Changing it signs out every other session
 * (older tokens are rejected by JwtAuthFilter); the caller gets a fresh token to stay signed in.
 */
@Service
public class PasswordChangeService {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangeService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public PasswordChangeService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public LoginResponse changePassword(String username, ChangePasswordRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // 400 (not 401): a wrong current password must not look like an expired session to the client.
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        if (req.getNewPassword().toLowerCase().contains(user.getUsername().toLowerCase())) {
            throw new IllegalArgumentException("New password must not contain your username");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("User {} changed their password", user.getUsername());

        UserPrincipal principal = new UserPrincipal(user);
        return new LoginResponse(jwtUtil.generateToken(principal), user.getUsername(), user.getRole().name(),
                principal.getMillId(), user.getFullName());
    }
}
