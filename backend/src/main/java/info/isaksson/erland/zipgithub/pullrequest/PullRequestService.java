package info.isaksson.erland.zipgithub.pullrequest;

import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import info.isaksson.erland.zipgithub.github.GitHubPullRequestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;

@ApplicationScoped
public class PullRequestService {
    private final GitHubInstallationTokenProvider tokens;
    private final GitHubPullRequestClient client;
    private final Clock clock;

    @Inject
    public PullRequestService(GitHubInstallationTokenProvider tokens, GitHubPullRequestClient client) {
        this(tokens, client, Clock.systemUTC());
    }

    PullRequestService(GitHubInstallationTokenProvider tokens, GitHubPullRequestClient client, Clock clock) {
        this.tokens = tokens; this.client = client; this.clock = clock;
    }

    public PullRequestResult createOrReuseDraft(long installationId, GitDeliveryResult delivery) {
        String token = tokens.createInstallationToken(installationId);
        var existing = client.findOpenPullRequest(token, delivery.repositoryFullName(),
                delivery.branchName(), delivery.baseBranch());
        if (existing.isPresent()) return toResult(delivery, existing.get());

        String shortDigest = delivery.planDigestSha256().substring(0, 12);
        String title = "Import approved ZIP plan " + shortDigest;
        String body = "Created by zip-github from an immutable approved import plan.\n\n"
                + "- Base commit: `" + delivery.baseCommitSha() + "`\n"
                + "- Commit: `" + delivery.commitSha() + "`\n"
                + "- Plan digest: `" + delivery.planDigestSha256() + "`\n";
        try {
            return toResult(delivery, client.createDraftPullRequest(token, delivery.repositoryFullName(), title,
                    delivery.branchName(), delivery.baseBranch(), body));
        } catch (IllegalStateException creationFailure) {
            return client.findOpenPullRequest(token, delivery.repositoryFullName(), delivery.branchName(),
                    delivery.baseBranch()).map(pr -> toResult(delivery, pr)).orElseThrow(() -> creationFailure);
        }
    }

    private PullRequestResult toResult(GitDeliveryResult delivery, GitHubPullRequestClient.GitHubPullRequest pr) {
        if (!pr.draft() || pr.number() <= 0 || pr.htmlUrl() == null || pr.htmlUrl().isBlank())
            throw new IllegalStateException("GitHub returned incomplete draft pull request metadata");
        return new PullRequestResult(delivery.importId(), delivery.repositoryFullName(), delivery.baseBranch(),
                delivery.branchName(), delivery.commitSha(), delivery.planDigestSha256(), pr.number(), pr.htmlUrl(),
                true, pr.state(), clock.instant());
    }
}
