package info.isaksson.erland.zipgithub.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FixedWindowRateLimiter {
    private final int maximumRequests;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maximumRequests, Duration window) {
        this(maximumRequests, window, Clock.systemUTC());
    }

    FixedWindowRateLimiter(int maximumRequests, Duration window, Clock clock) {
        if (maximumRequests < 1 || window.isZero() || window.isNegative()) throw new IllegalArgumentException();
        this.maximumRequests = maximumRequests;
        this.window = window;
        this.clock = clock;
    }

    public boolean allow(String key) {
        Instant now = clock.instant();
        Counter updated = counters.compute(key, (ignored, existing) -> {
            if (existing == null || !now.isBefore(existing.windowStart.plus(window))) return new Counter(now, 1);
            return new Counter(existing.windowStart, existing.count + 1);
        });
        if (counters.size() > 10_000) counters.entrySet().removeIf(e -> !now.isBefore(e.getValue().windowStart.plus(window)));
        return updated.count <= maximumRequests;
    }

    private record Counter(Instant windowStart, int count) {}
}
