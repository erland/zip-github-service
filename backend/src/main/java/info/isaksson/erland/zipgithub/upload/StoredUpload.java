package info.isaksson.erland.zipgithub.upload;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** User/import-owned view of a neutral stored ZIP artifact. */
public record StoredUpload(UUID id, UUID importId, UUID ownerUserId, String originalFilename,
                           long sizeBytes, String sha256, Path storagePath,
                           Instant createdAt, Instant retentionDeadline, Map<String, GitFileMode> fileModes) {
    public StoredUpload { fileModes = fileModes == null ? Map.of() : Map.copyOf(fileModes); }
    public StoredUpload(UUID id, UUID importId, UUID ownerUserId, String originalFilename, long sizeBytes, String sha256, Path storagePath,
                        Instant createdAt, Instant retentionDeadline) {
        this(id, importId, ownerUserId, originalFilename, sizeBytes, sha256, storagePath, createdAt, retentionDeadline, Map.of());
    }
    public static StoredUpload attach(UUID ownerUserId, UUID importId, StoredUploadArtifact artifact) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(importId, "importId");
        Objects.requireNonNull(artifact, "artifact");
        return new StoredUpload(artifact.id(), importId, ownerUserId, artifact.originalFilename(),
                artifact.sizeBytes(), artifact.sha256(), artifact.storagePath(), artifact.createdAt(),
                artifact.retentionDeadline(), artifact.fileModes());
    }
}
