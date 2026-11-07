package com.millsystem.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BUG-002: no secret in source; prod refuses to start without a strong one. */
class JwtSecretPolicyTest {

    private JwtUtil util(String secret, String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        JwtUtil u = new JwtUtil(env);
        ReflectionTestUtils.setField(u, "jwtSecret", secret);
        ReflectionTestUtils.setField(u, "jwtExpirationMs", 60_000L);
        return u;
    }

    @Test
    void prodWithoutSecretFailsFast() {
        assertThatThrownBy(() -> util("", "prod").initKey()).hasMessageContaining("JWT_SECRET must be set");
    }

    @Test
    void shortSecretIsRejectedEverywhere() {
        assertThatThrownBy(() -> util("too-short", "prod").initKey()).hasMessageContaining("at least 32 bytes");
        assertThatThrownBy(() -> util("too-short").initKey()).hasMessageContaining("at least 32 bytes");
    }

    @Test
    void devWithoutSecretUsesARandomOne() {
        assertThatCode(() -> util("").initKey()).doesNotThrowAnyException();
    }
}
