package info.isaksson.erland.zipbuildserver.api.artifact;

import java.util.List;

public record ArtifactListResponse(List<ArtifactResponse> artifacts) {
}
