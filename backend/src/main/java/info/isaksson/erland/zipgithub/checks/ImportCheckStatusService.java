package info.isaksson.erland.zipgithub.checks;

import info.isaksson.erland.zipgithub.github.GitHubCheckStatusClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ImportCheckStatusService {
    @Inject GitHubInstallationTokenProvider tokens;
    @Inject GitHubCheckStatusClient github;
    private final Clock clock;

    public ImportCheckStatusService() { this(Clock.systemUTC()); }
    ImportCheckStatusService(Clock clock) { this.clock = clock; }

    public ImportCheckStatus read(UUID importId, long installationId, String repositoryFullName, String commitSha) {
        var status = github.readCommitChecks(tokens.createInstallationToken(installationId), repositoryFullName, commitSha);
        return new ImportCheckStatus(importId, repositoryFullName, commitSha, status.state(), status.terminal(),
                status.total(), status.pending(), status.successful(), status.failed(), status.cancelled(),
                status.detailsUrl(), Instant.now(clock));
    }
}
