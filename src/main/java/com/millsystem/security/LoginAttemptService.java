package com.millsystem.security;

import com.millsystem.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection for /api/auth/login. Counts consecutive failures per account and per client IP inside a
 * sliding window; once a limit is hit, further attempts get 429 until the lock expires. A successful login clears the
 * account's counter. In-memory: fine for the single-instance deployment; move to Redis if the API is scaled out.
 */
@Service
public class LoginAttemptService {

    private record Counter(int failures, Instant windowStart, Instant lockedUntil) {}

    private final Map<String, Counter> accounts = new ConcurrentHashMap<>();
    private final Map<String, Counter> ips = new ConcurrentHashMap<>();
    private final int maxPerAccount;
    private final int maxPerIp;
    private final Duration lockFor;
    private Clock clock = Clock.systemUTC();

    public LoginAttemptService(@Value("${app.login.max-failures-per-account:5}") int maxPerAccount,
                               @Value("${app.login.max-failures-per-ip:50}") int maxPerIp,
                               @Value("${app.login.lock-minutes:15}") long lockMinutes) {
        this.maxPerAccount = maxPerAccount;
        this.maxPerIp = maxPerIp;
        this.lockFor = Duration.ofMinutes(lockMinutes);
    }

    /** Throws 429 if this account or IP is currently locked. */
    public void checkAllowed(String username, String ip) {
        Instant now = clock.instant();
        check(accounts.get(username), now, "Too many failed sign-in attempts for this account. Try again in %d minute(s).");
        check(ips.get(ip), now, "Too many failed sign-in attempts from this network. Try again in %d minute(s).");
    }

    public void recordFailure(String username, String ip) {
        Instant now = clock.instant();
        bump(accounts, username, maxPerAccount, now);
        bump(ips, ip, maxPerIp, now);
    }

    public void recordSuccess(String username) {
        accounts.remove(username);
    }

    void useClock(Clock c) { this.clock = c; }

    private void check(Counter c, Instant now, String message) {
        if (c != null && c.lockedUntil() != null && now.isBefore(c.lockedUntil())) {
            long seconds = Math.max(1, Duration.between(now, c.lockedUntil()).getSeconds());
            throw new TooManyRequestsException(String.format(message, (seconds + 59) / 60), seconds);
        }
    }

    private void bump(Map<String, Counter> map, String key, int max, Instant now) {
        if (key == null) return;
        map.compute(key, (k, c) -> {
            if (c == null || now.isAfter(c.windowStart().plus(lockFor))) c = new Counter(0, now, null);
            int failures = c.failures() + 1;
            return new Counter(failures, c.windowStart(), failures >= max ? now.plus(lockFor) : c.lockedUntil());
        });
    }
}
