package info.isaksson.erland.zipgithub.selection;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable user selection bound to one immutable import plan and used as the exact delivery contract. */
public record ApprovedSelection(
        UUID id,
        UUID importId,
        UUID planId,
        UUID ownerUserId,
        String planDigestSha256,
        String baseCommitSha,
        String selectionVersion,
        String selectionDigestSha256,
        List<String> selectedPaths,
        List<String> excludedPaths,
        List<ApprovedSelectionOverride> overrides,
        List<ApprovedBlockerDecision> blockerDecisions,
        Instant createdAt) {

    public ApprovedSelection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        requireSha256(planDigestSha256, "planDigestSha256");
        if (baseCommitSha == null || !baseCommitSha.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("baseCommitSha must be a lower-case 40-character Git SHA");
        }
        if (selectionVersion == null || selectionVersion.isBlank()) {
            throw new IllegalArgumentException("selectionVersion is required");
        }
        requireSha256(selectionDigestSha256, "selectionDigestSha256");
        selectedPaths = List.copyOf(selectedPaths);
        excludedPaths = List.copyOf(excludedPaths);
        overrides = List.copyOf(overrides);
        blockerDecisions = blockerDecisions == null ? List.of() : List.copyOf(blockerDecisions);
        if (selectedPaths.isEmpty()) throw new IllegalArgumentException("selectedPaths must not be empty");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static void requireSha256(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + " must be a lower-case SHA-256");
        }
    }
}
