package info.isaksson.erland.zipgithub.actions;

import info.isaksson.erland.zipgithub.github.GitHubActionsClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ImportActionsStatusService {
    private static final Duration ACTIVE_TTL = Duration.ofSeconds(8);
    private static final Duration TERMINAL_TTL = Duration.ofMinutes(5);

    @Inject GitHubInstallationTokenProvider tokens;
    @Inject GitHubActionsClient github;
    private final Clock clock;
    private final Map<String, CachedStatus> cache = new ConcurrentHashMap<>();

    public ImportActionsStatusService() { this(Clock.systemUTC()); }
    ImportActionsStatusService(Clock clock) { this.clock = clock; }

    public ImportActionsStatus read(UUID importId, long installationId, String repositoryFullName, String commitSha) {
        Instant now = Instant.now(clock);
        String key = importId + ":" + commitSha;
        CachedStatus cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) return cached.status();

        var status = github.readCommitActions(tokens.createInstallationToken(installationId), repositoryFullName, commitSha);
        ImportActionsStatus result = new ImportActionsStatus(importId, repositoryFullName, commitSha, status.state(),
                status.terminal(), status.detailsUrl(), status.workflows(), status.checks(), status.diagnosticCode(), status.diagnosticMessage(), now);
        Duration ttl = status.terminal() ? TERMINAL_TTL : ACTIVE_TTL;
        cache.put(key, new CachedStatus(result, now.plus(ttl)));
        return result;
    }

    private record CachedStatus(ImportActionsStatus status, Instant expiresAt) {}
}
