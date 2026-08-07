package info.isaksson.erland.zipgithub.plan;

import java.util.Objects;

/** One immutable, normalized path decision stored in an import plan. */
public record ImmutableImportPlanEntry(
        String path,
        String status,
        String comparisonStatus,
        String severity,
        String policyCode,
        String message,
        Long archiveSizeBytes,
        String archiveSha256,
        Long repositorySizeBytes,
        String repositorySha256,
        boolean textCandidate) {

    public ImmutableImportPlanEntry {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(severity, "severity");
        if (path.isBlank() || path.startsWith("/") || path.contains("\\")) {
            throw new IllegalArgumentException("path must be normalized and relative");
        }
    }
}
