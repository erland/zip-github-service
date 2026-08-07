package info.isaksson.erland.zipgithub.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

public final class FixedWindowRateLimiterSelfTest {
    public static void main(String[] args) {
        var clock = Clock.fixed(Instant.parse("2026-08-06T20:00:00Z"), ZoneOffset.UTC);
        var limiter = new FixedWindowRateLimiter(2, Duration.ofMinutes(1), clock);
        if (!limiter.allow("a") || !limiter.allow("a") || limiter.allow("a")) throw new AssertionError("limit failed");
        if (!limiter.allow("b")) throw new AssertionError("keys must be isolated");
    }
}
