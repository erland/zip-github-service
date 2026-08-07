package info.isaksson.erland.zipgithub.plan;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable snapshot of the exact upload, repository base and policy decisions presented for review. */
public record ImmutableImportPlan(
        UUID id,
        UUID importId,
        UUID ownerUserId,
        String sourceUploadSha256,
        String baseCommitSha,
        String policyVersion,
        String planDigestSha256,
        String status,
        boolean approvable,
        List<ImmutableImportPlanEntry> entries,
        Instant createdAt) {

    public ImmutableImportPlan {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        requireSha256(sourceUploadSha256, "sourceUploadSha256");
        if (baseCommitSha == null || !baseCommitSha.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("baseCommitSha must be a lower-case 40-character Git SHA");
        }
        if (policyVersion == null || policyVersion.isBlank()) throw new IllegalArgumentException("policyVersion is required");
        requireSha256(planDigestSha256, "planDigestSha256");
        Objects.requireNonNull(status, "status");
        entries = List.copyOf(entries);
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static void requireSha256(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be a lower-case SHA-256");
        }
    }
}
