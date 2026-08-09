package info.isaksson.erland.zipgithub.pullrequest;

import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.github.GitHubPullRequestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;

@ApplicationScoped
public class PullRequestService {
    private final GitHubPullRequestClient client;
    private final Clock clock;

    @Inject
    public PullRequestService(GitHubPullRequestClient client) {
        this(client, Clock.systemUTC());
    }

    PullRequestService(GitHubPullRequestClient client, Clock clock) {
        this.client = client; this.clock = clock;
    }

    /** Creates or reuses the draft PR on behalf of the authenticated GitHub user. */
    public PullRequestResult createOrReuseDraft(String userAccessToken, GitDeliveryResult delivery, String submittedTitle, String submittedDescription) {
        if (userAccessToken == null || userAccessToken.isBlank())
            throw new IllegalArgumentException("GitHub user access token is required for pull request attribution.");
        String token = userAccessToken;
        String title = PullRequestMetadataPolicy.requireTitle(submittedTitle);
        String body = PullRequestMetadataPolicy.requireDescription(submittedDescription);
        var existing = client.findOpenPullRequest(token, delivery.repositoryFullName(),
                delivery.branchName(), delivery.baseBranch());
        if (existing.isPresent()) return toResult(delivery, existing.get());

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
