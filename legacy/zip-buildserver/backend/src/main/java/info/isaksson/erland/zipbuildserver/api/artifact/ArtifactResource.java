package info.isaksson.erland.zipbuildserver.api.artifact;

import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.storage.ArtifactStorageService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/runs/{runId}/artifacts")
@Produces(MediaType.APPLICATION_JSON)
public class ArtifactResource {
    private final ArtifactStorageService artifactStorageService;

    public ArtifactResource(ArtifactStorageService artifactStorageService) {
        this.artifactStorageService = artifactStorageService;
    }

    @GET
    public ArtifactListResponse listForRun(@PathParam("runId") UUID runId) {
        return new ArtifactListResponse(artifactStorageService.listForRun(runId).stream()
                .map(this::toResponse)
                .toList());
    }

    private ArtifactResponse toResponse(ArtifactReferenceEntity artifact) {
        return new ArtifactResponse(
                artifact.id,
                artifact.runId,
                artifact.type,
                artifact.sizeBytes,
                artifact.createdAt,
                artifact.expiresAt);
    }
}
