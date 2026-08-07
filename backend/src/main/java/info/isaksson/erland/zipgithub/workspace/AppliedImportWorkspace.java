package info.isaksson.erland.zipgithub.workspace;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Isolated Git workspace containing exactly one approved immutable selection applied to its locked base commit. */
public record AppliedImportWorkspace(
        UUID importId,
        String repositoryFullName,
        String baseCommitSha,
        String planDigestSha256,
        String selectionDigestSha256,
        Path workspacePath,
        List<String> appliedPaths,
        Instant preparedAt) {
    public AppliedImportWorkspace {
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(repositoryFullName, "repositoryFullName");
        Objects.requireNonNull(baseCommitSha, "baseCommitSha");
        Objects.requireNonNull(planDigestSha256, "planDigestSha256");
        Objects.requireNonNull(selectionDigestSha256, "selectionDigestSha256");
        Objects.requireNonNull(workspacePath, "workspacePath");
        appliedPaths = List.copyOf(appliedPaths);
        Objects.requireNonNull(preparedAt, "preparedAt");
    }
}
