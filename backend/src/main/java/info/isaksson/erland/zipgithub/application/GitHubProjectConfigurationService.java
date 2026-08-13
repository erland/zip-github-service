package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationAccess;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubProjectConfigurationService {
    @Inject GitHubProjectCatalog catalog;

    public VerifiedRepository verify(String userAccessToken, Long installationId, Long repositoryId, String branch) {
        if (installationId == null || repositoryId == null) {
            throw ApiException.badRequest("GITHUB_BINDING_REQUIRED", "GitHub installation and repository are required.");
        }
        var installations = catalog.listUserInstallations(userAccessToken);
        GitHubInstallationAccess.requireVisible(installationId, installations);
        GitHubAppClient.GitHubInstallation installation = installations.stream()
                .filter(item -> item.id() == installationId)
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("GITHUB_INSTALLATION_NOT_FOUND", "The GitHub App installation was not found."));
        GitHubAppClient.GitHubRepository repository = catalog
                .listUserInstallationRepositories(userAccessToken, installationId).stream()
                .filter(item -> item.id() == repositoryId)
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("GITHUB_REPOSITORY_NOT_FOUND", "The GitHub repository was not found."));
        boolean explicitBranch = branch != null && !branch.isBlank();
        String reportedDefaultBranch = usableBranch(repository.defaultBranch());
        Boolean hasBranches = null;
        String selectedBranch;
        if (explicitBranch) {
            selectedBranch = branch.trim();
        } else if (reportedDefaultBranch != null) {
            selectedBranch = reportedDefaultBranch;
        } else {
            hasBranches = catalog.repositoryHasBranches(userAccessToken, repository.fullName());
            if (hasBranches) {
                throw ApiException.badGateway("GITHUB_DEFAULT_BRANCH_UNAVAILABLE",
                        "GitHub did not report a usable default branch for the initialized repository.");
            }
            // A brand-new GitHub repository may have no default_branch value until its first commit.
            // Use a deterministic bootstrap branch without persisting a blank value to PostgreSQL.
            selectedBranch = "main";
        }
        if (!catalog.branchExists(userAccessToken, repository.fullName(), selectedBranch)) {
            if (hasBranches == null) hasBranches = catalog.repositoryHasBranches(userAccessToken, repository.fullName());
            boolean defaultChoice = reportedDefaultBranch != null
                    ? selectedBranch.equals(reportedDefaultBranch)
                    : selectedBranch.equals("main");
            if (hasBranches || !defaultChoice) {
                throw ApiException.badRequest("GITHUB_BRANCH_NOT_FOUND", "The selected branch does not exist in the repository.");
            }
        }
        return new VerifiedRepository(installationId, installation.accountLogin(), installation.repositorySelection(),
                repository.id(), repository.fullName(), repository.privateRepository(), selectedBranch);
    }

    private static String usableBranch(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() || "null".equalsIgnoreCase(normalized) ? null : normalized;
    }

    public record VerifiedRepository(long installationId, String installationAccountLogin, String repositorySelection,
                                     long repositoryId, String fullName, boolean privateRepository, String defaultBranch) {}
}
