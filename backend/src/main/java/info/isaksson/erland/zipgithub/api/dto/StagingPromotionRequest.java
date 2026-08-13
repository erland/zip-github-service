package info.isaksson.erland.zipgithub.api.dto;

import java.util.UUID;

public record StagingPromotionRequest(UUID projectId, Long githubInstallationId, Long githubRepositoryId, Boolean confirmOpenPullRequest) {
    public StagingPromotionRequest(UUID projectId) { this(projectId, null, null, null); }
    public boolean confirmsOpenPullRequest() { return Boolean.TRUE.equals(confirmOpenPullRequest); }
}
