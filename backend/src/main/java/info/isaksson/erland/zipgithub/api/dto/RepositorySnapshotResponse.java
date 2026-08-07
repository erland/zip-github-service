package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RepositorySnapshotResponse(
        UUID importId,
        String repositoryFullName,
        String branch,
        String baseCommitSha,
        int entryCount,
        List<Entry> entries,
        Instant createdAt) {
    public record Entry(String path, String mode, String objectType, String objectId, long sizeBytes, String sha256) { }
}
