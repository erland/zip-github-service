package info.isaksson.erland.zipgithub.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable repository inventory bound to one import and one exact commit. */
public record RepositorySnapshot(
        UUID importId,
        String repositoryFullName,
        String branch,
        String baseCommitSha,
        List<RepositorySnapshotEntry> entries,
        Instant createdAt) {
    public RepositorySnapshot {
        entries = List.copyOf(entries);
    }
}
