package info.isaksson.erland.zipgithub.api.dto;

import java.util.UUID;

public record WorkBranchCleanupCandidateResponse(
        long githubInstallationId,
        long githubRepositoryId,
        String repositoryFullName,
        String repositoryUrl,
        UUID projectId,
        String defaultBranch,
        String branchName,
        String branchUrl,
        String commitSha,
        Long pullRequestNumber,
        String pullRequestUrl,
        String classification,
        String reason,
        boolean deletable
) {}
