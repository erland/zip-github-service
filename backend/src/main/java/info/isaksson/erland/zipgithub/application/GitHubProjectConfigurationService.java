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
        GitHubAppClient.GitHubRepository repository = catalog
                .listUserInstallationRepositories(userAccessToken, installationId).stream()
                .filter(item -> item.id() == repositoryId)
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("GITHUB_REPOSITORY_NOT_FOUND", "The GitHub repository was not found."));
        String selectedBranch = branch == null || branch.isBlank() ? repository.defaultBranch() : branch.trim();
        if (!catalog.branchExists(userAccessToken, repository.fullName(), selectedBranch)) {
            throw ApiException.badRequest("GITHUB_BRANCH_NOT_FOUND", "The selected branch does not exist in the repository.");
        }
        return new VerifiedRepository(installationId, repository.id(), repository.fullName(), repository.privateRepository(), selectedBranch);
    }

    public record VerifiedRepository(long installationId, long repositoryId, String fullName,
                                     boolean privateRepository, String defaultBranch) {}
}
