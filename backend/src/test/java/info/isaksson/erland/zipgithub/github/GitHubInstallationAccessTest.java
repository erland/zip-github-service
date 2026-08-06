package info.isaksson.erland.zipgithub.github;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubInstallationAccessTest {
    private static GitHubAppClient.GitHubInstallation installation(long id) {
        return new GitHubAppClient.GitHubInstallation(id, 10L, "owner", "User", "selected", null);
    }

    @Test
    void visibleInstallationIsAccepted() {
        assertDoesNotThrow(() -> GitHubInstallationAccess.requireVisible(7L, List.of(installation(7L))));
    }

    @Test
    void anotherUsersInstallationIsHiddenAsNotFound() {
        ApiException error = assertThrows(ApiException.class,
                () -> GitHubInstallationAccess.requireVisible(9L, List.of(installation(7L))));
        assertEquals(404, error.status());
        assertEquals("GITHUB_INSTALLATION_NOT_FOUND", error.code());
    }
}
