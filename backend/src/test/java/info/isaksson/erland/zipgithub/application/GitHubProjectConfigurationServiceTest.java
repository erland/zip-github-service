package info.isaksson.erland.zipgithub.application;

import static org.junit.jupiter.api.Assertions.*;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
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

    private static final class FakeCatalog implements GitHubProjectCatalog {
        public List<GitHubAppClient.GitHubInstallation> listUserInstallations(String token) {
            return List.of(new GitHubAppClient.GitHubInstallation(10L, 1L, "erland", "User", "selected", null));
        }
        public List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String token, long id) {
            return List.of(new GitHubAppClient.GitHubRepository(20L, "erland/example", true, "main", "url"));
        }
        public boolean branchExists(String token, String repo, String branch) { return branch.equals("main"); }
    }
}
