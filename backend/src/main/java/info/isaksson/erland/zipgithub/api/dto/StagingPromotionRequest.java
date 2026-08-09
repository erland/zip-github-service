package info.isaksson.erland.zipgithub.api.dto;

import java.util.UUID;

public record StagingPromotionRequest(UUID projectId, Long githubInstallationId, Long githubRepositoryId) {
    public StagingPromotionRequest(UUID projectId) { this(projectId, null, null); }
}
