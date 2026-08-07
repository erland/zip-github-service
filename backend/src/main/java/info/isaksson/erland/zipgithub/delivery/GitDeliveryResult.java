package info.isaksson.erland.zipgithub.delivery;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable metadata for one branch, commit and push created from an approved workspace. */
public record GitDeliveryResult(UUID importId, String repositoryFullName, String baseBranch,
                                String branchName, String baseCommitSha, String commitSha,
                                String planDigestSha256, Instant pushedAt) {
    public GitDeliveryResult {
        Objects.requireNonNull(importId); Objects.requireNonNull(repositoryFullName);
        Objects.requireNonNull(baseBranch); Objects.requireNonNull(branchName);
        Objects.requireNonNull(baseCommitSha); Objects.requireNonNull(commitSha);
        Objects.requireNonNull(planDigestSha256); Objects.requireNonNull(pushedAt);
    }
}
