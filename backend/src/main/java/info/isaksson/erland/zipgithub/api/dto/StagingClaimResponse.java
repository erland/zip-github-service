package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Owner-safe metadata returned only after authenticated claim. */
public record StagingClaimResponse(UUID stagingId, String originalFilename, long sizeBytes, String sha256,
                                   Instant expiresAt, Instant claimedAt) { }
