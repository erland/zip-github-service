package info.isaksson.erland.zipgithub.plan;

import java.util.Objects;

/** One immutable, normalized path decision stored in an import plan. */
public record ImmutableImportPlanEntry(
        String path,
        String status,
        String comparisonStatus,
        String severity,
        String blockerType,
        String policyCode,
        String message,
        Long archiveSizeBytes,
        String archiveSha256,
        Long repositorySizeBytes,
        String repositorySha256,
        String archiveMode,
        String repositoryMode,
        String effectiveMode,
        boolean modeChanged,
        boolean textCandidate) {

    public ImmutableImportPlanEntry(String path, String status, String comparisonStatus, String severity,
                                    String blockerType, String policyCode, String message, Long archiveSizeBytes,
                                    String archiveSha256, Long repositorySizeBytes, String repositorySha256, boolean textCandidate) {
        this(path, status, comparisonStatus, severity, blockerType, policyCode, message, archiveSizeBytes, archiveSha256,
                repositorySizeBytes, repositorySha256, null, null, null, false, textCandidate);
    }

    public ImmutableImportPlanEntry {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(blockerType, "blockerType");
        if (path.isBlank() || path.startsWith("/") || path.contains("\\")) {
            throw new IllegalArgumentException("path must be normalized and relative");
        }
    }
}
