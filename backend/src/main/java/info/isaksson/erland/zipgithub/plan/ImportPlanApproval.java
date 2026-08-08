package info.isaksson.erland.zipgithub.plan;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Audit record proving which immutable plan, selection and commit message a user approved. */
public record ImportPlanApproval(
        UUID importId,
        UUID planId,
        UUID approvedByUserId,
        String planDigestSha256,
        String selectionDigestSha256,
        String commitMessage,
        Instant approvedAt) {

    public ImportPlanApproval {
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(approvedByUserId, "approvedByUserId");
        if (planDigestSha256 == null || !planDigestSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("planDigestSha256 must be a lower-case SHA-256");
        }
        if (selectionDigestSha256 == null || !selectionDigestSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("selectionDigestSha256 must be a lower-case SHA-256");
        }
        commitMessage = CommitMessagePolicy.persistedOrLegacyFallback(commitMessage, importId);
        Objects.requireNonNull(approvedAt, "approvedAt");
    }

    /** Compatibility constructor for legacy/internal callers that predate interactive commit messages. */
    public ImportPlanApproval(UUID importId, UUID planId, UUID approvedByUserId,
                              String planDigestSha256, String selectionDigestSha256, Instant approvedAt) {
        this(importId, planId, approvedByUserId, planDigestSha256, selectionDigestSha256,
                CommitMessagePolicy.defaultSuggestion(importId), approvedAt);
    }
}
