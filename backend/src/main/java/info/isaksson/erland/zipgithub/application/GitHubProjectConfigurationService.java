package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationAccess;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.github.GitRepositoryBootstrapService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GitHubProjectConfigurationService {
    private static final Logger LOG = Logger.getLogger(GitHubProjectConfigurationService.class);
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
        LOG.infof("Repository Work preflight started installationId=%s repositoryId=%s", installationId, repositoryId);
        VerifiedRepository verified = verify(userAccessToken, installationId, repositoryId, null);
        LOG.infof("Repository Work preflight repository verified installationId=%d repositoryId=%d repository=%s defaultBranch=%s",
                verified.installationId(), verified.repositoryId(), verified.fullName(), verified.defaultBranch());
        boolean branchExists;
        try {
            branchExists = catalog.branchExists(userAccessToken, verified.fullName(), verified.defaultBranch());
        } catch (RuntimeException e) {
            throw ApiException.badGateway("GITHUB_BRANCH_STATE_UNAVAILABLE",
                    "GitHub branch status could not be verified before starting work.");
        }
        if (branchExists) {
            LOG.infof("Repository Work preflight default branch already exists installationId=%d repositoryId=%d repository=%s branch=%s",
                    verified.installationId(), verified.repositoryId(), verified.fullName(), verified.defaultBranch());
            return verified;
        }

        final boolean hasBranches;
        try {
            hasBranches = catalog.repositoryHasBranches(userAccessToken, verified.fullName());
        } catch (RuntimeException e) {
            throw ApiException.badGateway("GITHUB_BRANCH_STATE_UNAVAILABLE",
                    "GitHub repository state could not be verified before starting work.");
        }
        LOG.infof("Repository Work preflight branch inventory installationId=%d repositoryId=%d repository=%s hasBranches=%s",
                verified.installationId(), verified.repositoryId(), verified.fullName(), hasBranches);
        if (hasBranches) {
            throw ApiException.conflict("GITHUB_DEFAULT_BRANCH_MISSING",
                    "The repository is initialized but its configured default branch does not exist.");
        }

        GitHubAppClient.GitHubInstallation installation = catalog.listUserInstallations(userAccessToken).stream()
                .filter(item -> item.id() == verified.installationId())
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("GITHUB_INSTALLATION_NOT_FOUND",
                        "The GitHub App installation was not found."));
        LOG.infof("Repository Work preflight installation permission installationId=%d repositoryId=%d contentsWritable=%s",
                verified.installationId(), verified.repositoryId(), installation.contentsWritable());
        if (!installation.contentsWritable()) {
            throw ApiException.forbidden("GITHUB_CONTENTS_WRITE_PERMISSION_REQUIRED",
                    "The GitHub App installation needs Contents: Read and write before zip-GitHub can initialize an empty repository. "
                            + "Approve the updated GitHub App permissions for the installation and try again.");
        }

        if (repositoryBootstrap == null) {
            throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_UNAVAILABLE",
                    "The empty repository could not be initialized by zip-GitHub.");
        }
        try {
            LOG.infof("Empty repository bootstrap starting installationId=%d repositoryId=%d repository=%s branch=%s",
                    verified.installationId(), verified.repositoryId(), verified.fullName(), verified.defaultBranch());
            repositoryBootstrap.bootstrapEmptyRepository(verified.installationId(), verified.fullName(), verified.defaultBranch());
            LOG.infof("Empty repository bootstrap completed installationId=%d repositoryId=%d repository=%s branch=%s",
                    verified.installationId(), verified.repositoryId(), verified.fullName(), verified.defaultBranch());
        } catch (GitRepositoryBootstrapService.GitHubContentsBootstrapException bootstrapFailure) {
            // Accept only the narrow race where another actor initialized exactly our selected branch.
            try {
                if (catalog.branchExists(userAccessToken, verified.fullName(), verified.defaultBranch())) {
                    return verify(userAccessToken, installationId, repositoryId, verified.defaultBranch());
                }
            } catch (RuntimeException ignored) {
                // Map the original GitHub response below; it is more useful than a follow-up read failure.
            }
            throw mapBootstrapFailure(bootstrapFailure);
        } catch (RuntimeException bootstrapFailure) {
            try {
                if (!catalog.branchExists(userAccessToken, verified.fullName(), verified.defaultBranch())) {
                    throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                            "zip-GitHub could not initialize the empty repository before creating its Work branch.");
                }
            } catch (ApiException e) {
                throw e;
            } catch (RuntimeException stateFailure) {
                throw ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                        "zip-GitHub could not initialize the empty repository before creating its Work branch.");
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
        LOG.infof("Empty repository bootstrap verified installationId=%d repositoryId=%d repository=%s branch=%s",
                verified.installationId(), verified.repositoryId(), verified.fullName(), verified.defaultBranch());
        return verify(userAccessToken, installationId, repositoryId, verified.defaultBranch());
    }

    private static ApiException mapBootstrapFailure(GitRepositoryBootstrapService.GitHubContentsBootstrapException failure) {
        String githubMessage = failure.githubMessage();
        String suffix = githubMessage == null || githubMessage.isBlank() ? "" : " GitHub: " + githubMessage;
        return switch (failure.statusCode()) {
            case 401 -> ApiException.badGateway("GITHUB_BOOTSTRAP_AUTHENTICATION_FAILED",
                    "GitHub rejected the installation credential while initializing the empty repository." + suffix);
            case 403 -> ApiException.forbidden("GITHUB_CONTENTS_WRITE_PERMISSION_REQUIRED",
                    "GitHub denied the bootstrap write. Verify that the GitHub App installation has Contents: Read and write "
                            + "and that any updated App permissions have been approved." + suffix);
            case 404 -> ApiException.badGateway("GITHUB_BOOTSTRAP_REPOSITORY_UNAVAILABLE",
                    "GitHub could not expose the repository to the installation credential used for bootstrap." + suffix);
            case 409 -> ApiException.conflict("GITHUB_EMPTY_REPOSITORY_CONFLICT",
                    "GitHub reported a conflict while initializing the empty repository. Retry after GitHub has finished creating the repository."
                            + suffix);
            case 422 -> ApiException.badGateway("GITHUB_BOOTSTRAP_VALIDATION_FAILED",
                    "GitHub rejected the empty-repository bootstrap request as invalid." + suffix);
            default -> ApiException.badGateway("EMPTY_REPOSITORY_BOOTSTRAP_FAILED",
                    "GitHub rejected initialization of the empty repository with HTTP " + failure.statusCode() + "." + suffix);
        };
    }

    private static String usableBranch(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() || "null".equalsIgnoreCase(normalized) ? null : normalized;
    }

    public record VerifiedRepository(long installationId, String installationAccountLogin, String repositorySelection,
                                     long repositoryId, String fullName, boolean privateRepository, String defaultBranch) {}
}
