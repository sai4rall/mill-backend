package com.millsystem.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.millsystem.entity.Mill;
import com.millsystem.entity.User;
import com.millsystem.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureAccessInterceptorTest {

    @Mock private FeatureFlagService flags;
    private FeatureAccessInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new FeatureAccessInterceptor(new FeatureAccessRules(), flags, new ObjectMapper());
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginAs(User.Role role, Long millId) {
        User u = new User();
        u.setUsername("u");
        u.setPassword("x");
        u.setRole(role);
        u.setActive(true);
        if (millId != null) {
            Mill m = new Mill();
            m.setId(millId);
            u.setMill(m);
        }
        UserPrincipal p = new UserPrincipal(u);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities()));
    }

    private boolean call(String method, String path, MockHttpServletResponse response) throws Exception {
        return interceptor.preHandle(new MockHttpServletRequest(method, path), response, new Object());
    }

    @Test
    void blocksDisabledModuleWith403() throws Exception {
        loginAs(User.Role.MILL_OPERATOR, 4L);
        when(flags.enabledMap(4L)).thenReturn(Map.of("farmers", false, "purchases", true));
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(call("POST", "/api/farmers", res)).isFalse();
        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(res.getContentAsString())
                .contains("The Farmers module is not enabled for your mill")
                .contains("FEATURE_DISABLED");
    }

    @Test
    void allowsSharedListWhenAnotherScreenUsingItIsEnabled() throws Exception {
        loginAs(User.Role.MILL_OPERATOR, 4L);
        when(flags.enabledMap(4L)).thenReturn(Map.of("farmers", false, "purchases", true));
        assertThat(call("GET", "/api/farmers", new MockHttpServletResponse())).isTrue();
    }

    @Test
    void blocksSharedListWhenAllScreensUsingItAreDisabled() throws Exception {
        loginAs(User.Role.MILL_OPERATOR, 4L);
        when(flags.enabledMap(4L)).thenReturn(Map.of("farmers", false, "purchases", false));
        assertThat(call("GET", "/api/farmers", new MockHttpServletResponse())).isFalse();
    }

    @Test
    void allowsEnabledModule() throws Exception {
        loginAs(User.Role.MILL_ADMIN, 4L);
        when(flags.enabledMap(4L)).thenReturn(Map.of("farmers", true));
        assertThat(call("POST", "/api/farmers", new MockHttpServletResponse())).isTrue();
    }

    @Test
    void featureMissingFromDatabaseCountsAsEnabled() throws Exception {
        loginAs(User.Role.MILL_ADMIN, 4L);
        when(flags.enabledMap(4L)).thenReturn(Map.of());
        assertThat(call("GET", "/api/trucks/numbers", new MockHttpServletResponse())).isTrue();
    }

    @Test
    void superAdminIsNeverBlocked() throws Exception {
        loginAs(User.Role.SUPER_ADMIN, null);
        assertThat(call("POST", "/api/farmers", new MockHttpServletResponse())).isTrue();
        verifyNoInteractions(flags);
    }

    @Test
    void unauthenticatedAndUnmappedRequestsPassThrough() throws Exception {
        assertThat(call("POST", "/api/gps/ping", new MockHttpServletResponse())).isTrue();
        loginAs(User.Role.MILL_ADMIN, 4L);
        assertThat(call("GET", "/api/features", new MockHttpServletResponse())).isTrue();
        verifyNoInteractions(flags);
    }
}
