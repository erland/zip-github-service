package info.isaksson.erland.zipgithub.actions;

import info.isaksson.erland.zipgithub.github.GitHubActionsDetailsClient;
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
public class ImportActionsDetailsService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Inject GitHubInstallationTokenProvider tokens;
    @Inject GitHubActionsDetailsClient github;
    private final Clock clock;
    private final Map<String, CachedDetails> cache = new ConcurrentHashMap<>();

    public ImportActionsDetailsService() { this(Clock.systemUTC()); }
    ImportActionsDetailsService(Clock clock) { this.clock = clock; }

    public ImportActionsDetails read(UUID importId, long installationId, String repositoryFullName, String commitSha) {
        Instant now = Instant.now(clock);
        String key = importId + ":" + commitSha;
        CachedDetails cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) return cached.details();

        var details = github.readCommitActionDetails(tokens.createInstallationToken(installationId), repositoryFullName, commitSha);
        var result = new ImportActionsDetails(importId, repositoryFullName, commitSha, details.detailsUrl(),
                details.artifacts(), details.failures(), now);
        cache.put(key, new CachedDetails(result, now.plus(CACHE_TTL)));
        return result;
    }

    private record CachedDetails(ImportActionsDetails details, Instant expiresAt) {}
}
