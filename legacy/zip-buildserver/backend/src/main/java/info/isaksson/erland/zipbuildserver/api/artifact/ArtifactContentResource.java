package info.isaksson.erland.zipbuildserver.api.artifact;

import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.storage.ArtifactStorageService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/artifacts/{artifactId}")
@Produces(MediaType.APPLICATION_JSON)
public class ArtifactContentResource {
    private final ArtifactStorageService artifactStorageService;

    public ArtifactContentResource(ArtifactStorageService artifactStorageService) {
        this.artifactStorageService = artifactStorageService;
    }

    @GET
    public ArtifactContentResponse get(@PathParam("artifactId") UUID artifactId) {
        ArtifactReferenceEntity artifact = artifactStorageService.get(artifactId);
        return new ArtifactContentResponse(
                artifact.id,
                artifact.runId,
                artifact.type,
                artifactStorageService.readText(artifactId));
    }
}
