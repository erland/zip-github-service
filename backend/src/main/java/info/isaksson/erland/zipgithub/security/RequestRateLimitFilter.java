package info.isaksson.erland.zipgithub.security;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION - 50)
public class RequestRateLimitFilter implements ContainerRequestFilter {
    private final FixedWindowRateLimiter standard = new FixedWindowRateLimiter(120, Duration.ofMinutes(1));
    private final FixedWindowRateLimiter uploads = new FixedWindowRateLimiter(12, Duration.ofMinutes(1));

    @ConfigProperty(name = "zipgithub.security.rate-limit.enabled", defaultValue = "true") boolean enabled;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        if (!enabled || !path.startsWith("api/") || isSafe(request.getMethod())) return;
        String key = clientKey(request);
        FixedWindowRateLimiter limiter = path.matches("api/imports/[^/]+/upload") ? uploads : standard;
        if (!limiter.allow(key)) {
            throw ApiException.tooManyRequests("RATE_LIMIT_EXCEEDED", "Too many requests. Retry after a short delay.");
        }
    }

    private static String clientKey(ContainerRequestContext request) {
        Cookie cookie = request.getCookies().get(CurrentUserProvider.SESSION_COOKIE);
        String value = cookie == null ? "anonymous" : cookie.getValue();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isSafe(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}
