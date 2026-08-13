package info.isaksson.erland.zipgithub.api.dto;

public record WorkBranchCleanupCandidateResponse(
        long githubInstallationId,
        long githubRepositoryId,
        String repositoryFullName,
        String defaultBranch,
        String branchName,
        String commitSha,
        String classification,
        String reason,
        boolean deletable
) {}
