package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationAccess;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.github.GitRepositoryBootstrapService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubProjectConfigurationService {
    @Inject GitHubProjectCatalog catalog;
    @Inject GitRepositoryBootstrapService repositoryBootstrap;

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

    /** Verifies a repository for starting Work and initializes a truly empty repository before persistence. */
    public VerifiedRepository verifyForWorkStart(String userAccessToken, Long installationId, Long repositoryId) {
        VerifiedRepository verified = verify(userAccessToken, installationId, repositoryId, null);
        boolean branchExists;
        try {
            branchExists = catalog.branchExists(userAccessToken, verified.fullName(), verified.defaultBranch());
        } catch (RuntimeException e) {
            throw ApiException.badGateway("GITHUB_BRANCH_STATE_UNAVAILABLE",
                    "GitHub branch status could not be verified before starting work.");
        }
        if (branchExists) return verified;

        final boolean hasBranches;
        try {
            hasBranches = catalog.repositoryHasBranches(userAccessToken, verified.fullName());
        } catch (RuntimeException e) {
            throw ApiException.badGateway("GITHUB_BRANCH_STATE_UNAVAILABLE",
                    "GitHub repository state could not be verified before starting work.");
        }
        if (hasBranches) {
            throw ApiException.conflict("GITHUB_DEFAULT_BRANCH_MISSING",
                    "The repository is initialized but its configured default branch does not exist.");
        }
        if (repositoryBootstrap == null) {
            throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_UNAVAILABLE",
                    "The empty repository could not be initialized by zip-GitHub.");
        }
        try {
            repositoryBootstrap.bootstrapEmptyRepository(verified.installationId(), verified.fullName(), verified.defaultBranch());
        } catch (RuntimeException bootstrapFailure) {
            // Accept only the narrow race where another actor initialized exactly our selected branch.
            try {
                if (!catalog.branchExists(userAccessToken, verified.fullName(), verified.defaultBranch())) {
                    throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                            "GitHub rejected initialization of the empty repository.");
                }
            } catch (ApiException e) {
                throw e;
            } catch (RuntimeException stateFailure) {
                throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                        "GitHub rejected initialization of the empty repository.");
            }
        }
        try {
            if (!catalog.branchExists(userAccessToken, verified.fullName(), verified.defaultBranch())) {
                throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                        "The repository was initialized but the default branch could not be verified.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                    "The repository was initialized but the default branch could not be verified.");
        }
        // Re-read repository metadata after the first commit so only normal initialized state is persisted.
        return verify(userAccessToken, installationId, repositoryId, verified.defaultBranch());
    }

    private static String usableBranch(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() || "null".equalsIgnoreCase(normalized) ? null : normalized;
    }

    public record VerifiedRepository(long installationId, String installationAccountLogin, String repositorySelection,
                                     long repositoryId, String fullName, boolean privateRepository, String defaultBranch) {}
}
