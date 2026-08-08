package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record StagingUploadResponse(UUID stagingId, String originalFilename, long sizeBytes, String sha256,
                                    Instant expiresAt, String claimUrl) { }
