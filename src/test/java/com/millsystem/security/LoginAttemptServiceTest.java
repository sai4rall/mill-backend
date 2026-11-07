package com.millsystem.security;

import com.millsystem.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    @Test
    void locksAnAccountAfterRepeatedFailuresAndUnlocksLater() {
        LoginAttemptService s = new LoginAttemptService(3, 100, 15);
        Instant t0 = Instant.parse("2026-11-07T10:00:00Z");
        s.useClock(Clock.fixed(t0, ZoneOffset.UTC));
        for (int i = 0; i < 3; i++) s.recordFailure("ravi", "1.1.1.1");
        assertThatThrownBy(() -> s.checkAllowed("ravi", "2.2.2.2")).isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> s.checkAllowed("sita", "1.1.1.1")).doesNotThrowAnyException(); // other accounts unaffected
        s.useClock(Clock.fixed(t0.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        assertThatCode(() -> s.checkAllowed("ravi", "1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void successClearsTheCounter() {
        LoginAttemptService s = new LoginAttemptService(3, 100, 15);
        s.recordFailure("ravi", "ip"); s.recordFailure("ravi", "ip");
        s.recordSuccess("ravi");
        s.recordFailure("ravi", "ip"); s.recordFailure("ravi", "ip");
        assertThatCode(() -> s.checkAllowed("ravi", "ip")).doesNotThrowAnyException();
    }

    @Test
    void locksAnIpThatTriesManyAccounts() {
        LoginAttemptService s = new LoginAttemptService(100, 4, 15);
        for (int i = 0; i < 4; i++) s.recordFailure("user" + i, "6.6.6.6");
        assertThatThrownBy(() -> s.checkAllowed("someone", "6.6.6.6")).hasMessageContaining("network");
    }
}
