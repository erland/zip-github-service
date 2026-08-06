package info.isaksson.erland.zipgithub.upload;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public record StoredUpload(UUID id, UUID importId, UUID ownerUserId, String originalFilename,
                           long sizeBytes, String sha256, Path storagePath,
                           Instant createdAt, Instant retentionDeadline) { }
