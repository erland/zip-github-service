package info.isaksson.erland.zipgithub.application;

import static org.junit.jupiter.api.Assertions.*;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.github.GitRepositoryBootstrapService;
import java.util.List;
import org.junit.jupiter.api.Test;

class GitHubProjectConfigurationServiceTest {
    @Test
    void selectsRepositoryDefaultBranchAndRejectsInvisibleInstallation() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        service.catalog = new FakeCatalog();
        var verified = service.verify("token", 10L, 20L, null);
        assertEquals("erland/example", verified.fullName());
        assertEquals("main", verified.defaultBranch());
        ApiException error = assertThrows(ApiException.class, () -> service.verify("token", 11L, 20L, "main"));
        assertEquals("GITHUB_INSTALLATION_NOT_FOUND", error.code());
    }

    @Test
    void acceptsMissingDefaultBranchOnlyWhenRepositoryHasNoBranches() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        service.catalog = new FakeCatalog() {
            @Override public boolean branchExists(String token, String repo, String branch) { return false; }
            @Override public boolean repositoryHasBranches(String token, String repo) { return false; }
        };
        var verified = service.verify("token", 10L, 20L, null);
        assertEquals("main", verified.defaultBranch());

        service.catalog = new FakeCatalog() {
            @Override public boolean branchExists(String token, String repo, String branch) { return false; }
            @Override public boolean repositoryHasBranches(String token, String repo) { return true; }
        };
        ApiException error = assertThrows(ApiException.class, () -> service.verify("token", 10L, 20L, null));
        assertEquals("GITHUB_BRANCH_NOT_FOUND", error.code());
    }

    @Test
    void fallsBackToMainWhenAnEmptyRepositoryHasNoReportedDefaultBranch() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        service.catalog = new FakeCatalog() {
            @Override public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
                return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/empty", true, "", "url"));
            }
            @Override public boolean branchExists(String token, String repo, String branch) { return false; }
            @Override public boolean repositoryHasBranches(String token, String repo) { return false; }
        };

        var verified = service.verify("token", 10L, 20L, null);

        assertEquals("main", verified.defaultBranch());
    }

    @Test
    void rejectsMissingDefaultBranchMetadataForAnInitializedRepository() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        service.catalog = new FakeCatalog() {
            @Override public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
                return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/broken", true, "", "url"));
            }
            @Override public boolean repositoryHasBranches(String token, String repo) { return true; }
        };

        ApiException error = assertThrows(ApiException.class, () -> service.verify("token", 10L, 20L, null));

        assertEquals("GITHUB_DEFAULT_BRANCH_UNAVAILABLE", error.code());
    }

    @Test
    void bootstrapsEmptyRepositoryBeforeWorkVerificationAndThenRequiresTheBranch() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        final boolean[] bootstrapped = { false };
        service.catalog = new FakeCatalog() {
            @Override public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
                return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/empty", true, "", "url"));
            }
            @Override public boolean branchExists(String token, String repo, String branch) { return bootstrapped[0]; }
            @Override public boolean repositoryHasBranches(String token, String repo) { return bootstrapped[0]; }
        };
        service.repositoryBootstrap = new GitRepositoryBootstrapService() {
            @Override public String bootstrapEmptyRepository(long installationId, String repositoryFullName, String defaultBranch) {
                assertEquals(10L, installationId);
                assertEquals("erland/empty", repositoryFullName);
                assertEquals("main", defaultBranch);
                bootstrapped[0] = true;
                return "0123456789012345678901234567890123456789";
            }
        };

        var verified = service.verifyForWorkStart("token", 10L, 20L);

        assertTrue(bootstrapped[0]);
        assertEquals("main", verified.defaultBranch());
    }

    @Test
    void requiresContentsWriteBeforeBootstrappingAnEmptyRepository() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        service.catalog = new FakeCatalog() {
            @Override public List<GitHubAppClient.GitHubInstallation> listUserInstallations(String token) {
                return List.of(new GitHubAppClient.GitHubInstallation(
                        10L, 1L, "erland", "User", "selected", null, "read"));
            }
            @Override public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
                return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/empty", true, "main", "url"));
            }
            @Override public boolean branchExists(String token, String repo, String branch) { return false; }
            @Override public boolean repositoryHasBranches(String token, String repo) { return false; }
        };

        ApiException error = assertThrows(ApiException.class, () -> service.verifyForWorkStart("token", 10L, 20L));

        assertEquals("GITHUB_CONTENTS_WRITE_PERMISSION_REQUIRED", error.code());
        assertEquals(403, error.status());
    }

    @Test
    void mapsEmptyRepositoryBootstrapFailureToAnExplicitApiError() {
        GitHubProjectConfigurationService service = new GitHubProjectConfigurationService();
        service.catalog = new FakeCatalog() {
            @Override public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
                return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/empty", true, "", "url"));
            }
            @Override public boolean branchExists(String token, String repo, String branch) { return false; }
            @Override public boolean repositoryHasBranches(String token, String repo) { return false; }
        };
        service.repositoryBootstrap = new GitRepositoryBootstrapService() {
            @Override public String bootstrapEmptyRepository(long installationId, String repositoryFullName, String defaultBranch) {
                throw new IllegalStateException("simulated push rejection");
            }
        };

        ApiException error = assertThrows(ApiException.class, () -> service.verifyForWorkStart("token", 10L, 20L));

        assertEquals("EMPTY_REPOSITORY_BOOTSTRAP_FAILED", error.code());
    }

    private static class FakeCatalog implements GitHubProjectCatalog {
        public List<GitHubAppClient.GitHubInstallation> listUserInstallations(String token) {
            return List.of(new GitHubAppClient.GitHubInstallation(10L, 1L, "erland", "User", "selected", null, "write"));
        }
        public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
            return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/example", true, "main", "url"));
        }
        public boolean branchExists(String token, String repo, String branch) { return branch.equals("main"); }
        public boolean repositoryHasBranches(String token, String repo) { return true; }
    }
}
