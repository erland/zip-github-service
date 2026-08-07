package info.isaksson.erland.zipgithub.pullrequest;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PullRequestResult(UUID importId, String repositoryFullName, String baseBranch, String branchName,
                                String commitSha, String planDigestSha256, long pullRequestNumber,
                                String pullRequestUrl, boolean draft, String state, Instant createdAt) {
    public PullRequestResult {
        Objects.requireNonNull(importId); Objects.requireNonNull(repositoryFullName); Objects.requireNonNull(baseBranch);
        Objects.requireNonNull(branchName); Objects.requireNonNull(commitSha); Objects.requireNonNull(planDigestSha256);
        Objects.requireNonNull(pullRequestUrl); Objects.requireNonNull(state); Objects.requireNonNull(createdAt);
        if (pullRequestNumber <= 0) throw new IllegalArgumentException("pullRequestNumber must be positive");
    }
}
