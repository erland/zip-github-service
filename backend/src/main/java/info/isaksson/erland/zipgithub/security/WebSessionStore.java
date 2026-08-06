package info.isaksson.erland.zipgithub.security;

import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebSessionStore {
    private static final Duration SESSION_TTL = Duration.ofHours(12);
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final Map<String, StateRecord> states = new ConcurrentHashMap<>();

    public String createState(String returnTo) {
        cleanup();
        String token = token();
        states.put(token, new StateRecord(returnTo, Instant.now().plus(STATE_TTL)));
        return token;
    }

    public Optional<StateRecord> consumeState(String state) {
        if (state == null) return Optional.empty();
        StateRecord record = states.remove(state);
        return record == null || record.expiresAt().isBefore(Instant.now()) ? Optional.empty() : Optional.of(record);
    }

    public String createSession(UUID userId, long githubUserId, String login, String avatarUrl, String githubUserAccessToken) {
        cleanup();
        String token = token();
        sessions.put(token, new SessionRecord(userId, githubUserId, login, avatarUrl, githubUserAccessToken, Instant.now().plus(SESSION_TTL)));
        return token;
    }

    public Optional<SessionRecord> find(String token) {
        if (token == null) return Optional.empty();
        SessionRecord record = sessions.get(token);
        if (record == null) return Optional.empty();
        if (record.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    public void invalidate(String token) { if (token != null) sessions.remove(token); }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void cleanup() {
        Instant now = Instant.now();
        states.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        sessions.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    public record StateRecord(String returnTo, Instant expiresAt) {}
    public record SessionRecord(UUID userId, long githubUserId, String login, String avatarUrl, String githubUserAccessToken, Instant expiresAt) {}
}
