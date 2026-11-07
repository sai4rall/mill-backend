package com.millsystem.service;

import com.millsystem.dto.SuperAdminDto;
import com.millsystem.entity.User;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock private UserRepository userRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private SuperAdminService service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminService(userRepository, encoder, new PasswordGeneratorService(), new UsernamePolicy(userRepository));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static SuperAdminDto request(String username, String fullName) {
        SuperAdminDto dto = new SuperAdminDto();
        dto.setUsername(username);
        dto.setFullName(fullName);
        return dto;
    }

    private User existing(User.Role role) {
        User u = new User();
        u.setId(5L);
        u.setUsername("ops_admin");
        u.setRole(role);
        u.setActive(true);
        u.setPassword("old");
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void createsActiveSuperAdminWithHashedGeneratedPassword() {
        SuperAdminDto result = service.create(request("ops_admin", "Ops Admin"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User u = saved.getValue();
        assertThat(u.getRole()).isEqualTo(User.Role.SUPER_ADMIN);
        assertThat(u.getMill()).isNull();
        assertThat(u.isActive()).isTrue();
        assertThat(u.getFullName()).isEqualTo("Ops Admin");
        assertThat(result.getGeneratedPassword()).hasSize(12);
        assertThat(u.getPassword()).isNotEqualTo(result.getGeneratedPassword());
        assertThat(encoder.matches(result.getGeneratedPassword(), u.getPassword())).isTrue();
    }

    @Test
    void fullNameDefaultsToUsername() {
        service.create(request("ops_admin", null));
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getFullName()).isEqualTo("ops_admin");
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("superadmin")).thenReturn(true);
        assertThatThrownBy(() -> service.create(request("superadmin", null)))
                .hasMessage("Username 'superadmin' is already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void togglesStatus() {
        User u = existing(User.Role.SUPER_ADMIN);
        assertThat(service.toggleStatus(5L).isActive()).isFalse();
        assertThat(u.isActive()).isFalse();
    }

    @Test
    void resetsPassword() {
        User u = existing(User.Role.SUPER_ADMIN);
        String pw = service.resetPassword(5L);
        assertThat(encoder.matches(pw, u.getPassword())).isTrue();
    }

    @Test
    void cannotManageNonSuperAdminAccounts() {
        existing(User.Role.MILL_ADMIN);
        assertThatThrownBy(() -> service.toggleStatus(5L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.resetPassword(5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
