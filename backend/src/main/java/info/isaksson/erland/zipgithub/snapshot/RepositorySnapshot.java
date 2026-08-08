package info.isaksson.erland.zipgithub.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable repository inventory bound to one import and one exact commit. */
public record RepositorySnapshot(
        UUID importId,
        String repositoryFullName,
        String branch,
        String baseCommitSha,
        List<RepositorySnapshotEntry> entries,
        Map<String, String> gitIgnoreFiles,
        Instant createdAt) {
    public RepositorySnapshot {
        entries = List.copyOf(entries);
        gitIgnoreFiles = gitIgnoreFiles == null ? Map.of() : Map.copyOf(gitIgnoreFiles);
    }

    /** Backward-compatible constructor for persisted/test snapshots created before gitignore metadata was captured. */
    public RepositorySnapshot(UUID importId, String repositoryFullName, String branch, String baseCommitSha,
                              List<RepositorySnapshotEntry> entries, Instant createdAt) {
        this(importId, repositoryFullName, branch, baseCommitSha, entries, Map.of(), createdAt);
    }
}
