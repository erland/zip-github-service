package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;

public record WorkBranchCleanupRequest(List<Target> targets) {
    public record Target(long githubInstallationId, long githubRepositoryId, String repositoryFullName, String branchName) {}
}
