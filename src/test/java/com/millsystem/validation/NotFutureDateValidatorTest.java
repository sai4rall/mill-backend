package com.millsystem.validation;

import com.millsystem.config.AppTime;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotFutureDateValidatorTest {

    private final NotFutureDateValidator v = new NotFutureDateValidator();
    private final ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);

    @BeforeEach
    void at() {
        // 2026-11-05 20:00 UTC = 2026-11-06 01:30 in India: the server's UTC date is still the 5th.
        AppTime.useClock(Clock.fixed(Instant.parse("2026-11-05T20:00:00Z"), ZoneId.of("Asia/Kolkata")));
    }

    @AfterEach
    void reset() { AppTime.useClock(Clock.system(ZoneId.of("Asia/Kolkata"))); }

    @Test
    void acceptsTodayInIndiaEvenWhenUtcIsStillYesterday() {
        assertThat(v.isValid(LocalDate.of(2026, 11, 6), ctx)).isTrue();
        assertThat(v.isValid(LocalDate.of(2026, 11, 5), ctx)).isTrue();
        assertThat(v.isValid(null, ctx)).isTrue();
    }

    @Test
    void rejectsTomorrowInIndia() {
        assertThat(v.isValid(LocalDate.of(2026, 11, 7), ctx)).isFalse();
    }

    @Test
    void rejectsImplausiblyOldDates() {
        assertThat(v.isValid(LocalDate.of(1990, 1, 1), ctx)).isFalse();
        assertThat(v.isValid(LocalDate.of(2000, 1, 1), ctx)).isTrue();
    }
}
