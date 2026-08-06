package info.isaksson.erland.zipbuildserver.api.artifact;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;

import java.util.UUID;

public record ArtifactContentResponse(
        UUID id,
        UUID runId,
        ArtifactType type,
        String content) {
}
