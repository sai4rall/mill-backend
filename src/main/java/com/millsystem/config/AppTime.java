package com.millsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The business calendar. Mills operate in one time zone (default Asia/Kolkata, set app.timezone to change), so
 * "today" for date validation comes from that zone, not from the server's clock (Railway/AWS run in UTC).
 */
@Component
public class AppTime {

    private static volatile Clock clock = Clock.system(ZoneId.of("Asia/Kolkata"));

    public AppTime(@Value("${app.timezone:Asia/Kolkata}") String zone) {
        clock = Clock.system(ZoneId.of(zone));
    }

    public static LocalDate today() { return LocalDate.now(clock); }

    /** Tests only. */
    public static void useClock(Clock c) { clock = c; }
}
