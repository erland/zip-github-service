package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SourceUploadResponse(UUID id, UUID importId, String originalFilename, long sizeBytes,
                                   String sha256, String status, Instant createdAt, Instant retentionDeadline) { }
