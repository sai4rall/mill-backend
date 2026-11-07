package com.millsystem.security;

import com.millsystem.entity.Mill;
import com.millsystem.entity.User;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MillContextResolverTest {

    private final MillRepository mills = mock(MillRepository.class);
    private final MillContextResolver resolver = new MillContextResolver(mills);

    private static UserPrincipal principal(User.Role role, Long millId) {
        User u = new User();
        u.setId(42L); u.setUsername("u"); u.setPassword("x"); u.setRole(role); u.setActive(true);
        if (millId != null) { Mill m = new Mill(); m.setId(millId); u.setMill(m); }
        return new UserPrincipal(u);
    }

    @Test
    void millUsersAlwaysGetTheirOwnMill() {
        assertThat(resolver.resolve(principal(User.Role.MILL_ADMIN, 5L), 99L)).isEqualTo(5L);
        assertThat(resolver.resolve(principal(User.Role.MILL_OPERATOR, 5L), null)).isEqualTo(5L);
    }

    @Test
    void superAdminUsesHeaderAndMustProvideIt() {
        when(mills.existsByIdAndOwnerId(7L, 42L)).thenReturn(true);
        assertThat(resolver.resolve(principal(User.Role.SUPER_ADMIN, null), 7L)).isEqualTo(7L);
        assertThatThrownBy(() -> resolver.resolve(principal(User.Role.SUPER_ADMIN, null), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void superAdminCannotSeeAnotherCompanysMill() {
        when(mills.existsByIdAndOwnerId(8L, 42L)).thenReturn(false);
        assertThatThrownBy(() -> resolver.resolve(principal(User.Role.SUPER_ADMIN, null), 8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void devAdminCannotAccessMillData() {
        assertThatThrownBy(() -> resolver.resolve(principal(User.Role.DEV_ADMIN, null), 7L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
