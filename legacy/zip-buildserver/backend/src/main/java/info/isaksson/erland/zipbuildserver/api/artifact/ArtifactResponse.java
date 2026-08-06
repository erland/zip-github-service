package info.isaksson.erland.zipbuildserver.api.artifact;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArtifactResponse(
        UUID id,
        UUID runId,
        ArtifactType type,
        long sizeBytes,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt) {
}
