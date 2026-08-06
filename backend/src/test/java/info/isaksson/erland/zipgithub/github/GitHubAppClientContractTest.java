package info.isaksson.erland.zipgithub.github;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

class GitHubAppClientContractTest {
    @Test
    void repositoryDtoKeepsOnlySafeRepositoryMetadata() {
        var repository = new GitHubAppClient.GitHubRepository(42L, "owner/repo", true, "main", "https://github.com/owner/repo");
        assertEquals("owner/repo", repository.fullName());
        assertTrue(repository.privateRepository());
        assertEquals("main", repository.defaultBranch());
    }

    @Test
    void installationDtoContainsNoCredentialMaterial() {
        var installation = new GitHubAppClient.GitHubInstallation(7L, 9L, "owner", "User", "selected", "https://github.com/settings/installations/7");
        assertEquals(7L, installation.id());
        assertEquals("selected", installation.repositorySelection());
    }

    @Test
    void missingAppCredentialsDoNotPreventBeanCreationButFailWhenTokenIsRequested() {
        var client = new GitHubAppClient();
        client.appId = 0;
        client.privateKeyPem = Optional.empty();

        IllegalStateException error = assertThrows(IllegalStateException.class, client::createAppJwt);
        assertTrue(error.getMessage().contains("GITHUB_APP_ID"));
    }
}

