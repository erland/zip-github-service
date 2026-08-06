package info.isaksson.erland.zipgithub.domain.model;

import java.util.Objects;
import java.util.UUID;

/** One normalized path in an import plan. */
public record ImportPlanEntry(
        UUID id,
        String path,
        ChangeType changeType,
        String sourceSha256,
        String targetSha256,
        long sizeBytes,
        boolean text,
        PolicyResult policyResult,
        String policyMessage) {

    public ImportPlanEntry {
        Objects.requireNonNull(id, "id");
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\")) {
            throw new IllegalArgumentException("path must be a normalized relative path");
        }
        Objects.requireNonNull(changeType, "changeType");
        Objects.requireNonNull(policyResult, "policyResult");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes must not be negative");
    }

    public boolean blocked() {
        return policyResult == PolicyResult.BLOCKED;
    }

    public enum ChangeType { ADDED, MODIFIED, UNCHANGED, IGNORED, BLOCKED, WOULD_DELETE }
    public enum PolicyResult { ALLOWED, WARNING, IGNORED, BLOCKED }
}
