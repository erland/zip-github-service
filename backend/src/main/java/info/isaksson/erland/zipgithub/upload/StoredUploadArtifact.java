package info.isaksson.erland.zipgithub.upload;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Neutral result of safely ingesting ZIP bytes into controlled storage. */
public record StoredUploadArtifact(UUID id, String originalFilename, long sizeBytes, String sha256,
                                   Path storagePath, Instant createdAt, Instant retentionDeadline,
                                   Map<String, GitFileMode> fileModes) {
    public StoredUploadArtifact {
        fileModes = fileModes == null ? Map.of() : Map.copyOf(fileModes);
    }
    public StoredUploadArtifact(UUID id, String originalFilename, long sizeBytes, String sha256, Path storagePath,
                                Instant createdAt, Instant retentionDeadline) {
        this(id, originalFilename, sizeBytes, sha256, storagePath, createdAt, retentionDeadline, Map.of());
    }
    public StoredUploadArtifact withFileModes(Map<String, GitFileMode> modes) {
        return new StoredUploadArtifact(id, originalFilename, sizeBytes, sha256, storagePath, createdAt, retentionDeadline,
                Objects.requireNonNull(modes, "modes"));
    }
}
