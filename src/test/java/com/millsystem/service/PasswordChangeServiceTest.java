package com.millsystem.service;

import com.millsystem.dto.ChangePasswordRequest;
import com.millsystem.dto.LoginResponse;
import com.millsystem.entity.User;
import com.millsystem.repository.UserRepository;
import com.millsystem.security.JwtUtil;
import com.millsystem.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private PasswordChangeService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(userRepository, encoder, jwtUtil);
        user = new User();
        user.setId(3L);
        user.setUsername("ramesh_admin");
        user.setRole(User.Role.MILL_ADMIN);
        user.setActive(true);
        user.setPassword(encoder.encode("OldPass123"));
        lenient().when(userRepository.findByUsername("ramesh_admin")).thenReturn(Optional.of(user));
        lenient().when(jwtUtil.generateToken(any(UserPrincipal.class))).thenReturn("fresh-token");
    }

    private static ChangePasswordRequest req(String current, String next) {
        ChangePasswordRequest r = new ChangePasswordRequest();
        r.setCurrentPassword(current);
        r.setNewPassword(next);
        return r;
    }

    @Test
    void changesPasswordStampsTimeAndReturnsFreshToken() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        LoginResponse res = service.changePassword("ramesh_admin", req("OldPass123", "NewPass456"));

        assertThat(encoder.matches("NewPass456", user.getPassword())).isTrue();
        assertThat(user.getPasswordChangedAt()).isAfter(before);
        assertThat(res.getToken()).isEqualTo("fresh-token");
        assertThat(res.getRole()).isEqualTo("MILL_ADMIN");
        verify(userRepository).save(user);
    }

    @Test
    void wrongCurrentPasswordIsRejected() {
        assertThatThrownBy(() -> service.changePassword("ramesh_admin", req("nope", "NewPass456")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Current password is incorrect");
        verify(userRepository, never()).save(any());
    }

    @Test
    void newPasswordMustDiffer() {
        assertThatThrownBy(() -> service.changePassword("ramesh_admin", req("OldPass123", "OldPass123")))
                .hasMessage("New password must be different from the current password");
    }

    @Test
    void newPasswordMustNotContainUsername() {
        assertThatThrownBy(() -> service.changePassword("ramesh_admin", req("OldPass123", "x1RAMESH_ADMINx")))
                .hasMessage("New password must not contain your username");
    }

    @Test
    void anyPasswordSetOrResetStampsTheChangeTime() {
        User u = new User();
        assertThat(u.getPasswordChangedAt()).isNull();
        u.setPassword("hash");
        assertThat(u.getPasswordChangedAt()).isNotNull();
    }
}
