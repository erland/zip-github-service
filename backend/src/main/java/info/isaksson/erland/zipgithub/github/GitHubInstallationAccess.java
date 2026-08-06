package info.isaksson.erland.zipgithub.github;

import info.isaksson.erland.zipgithub.api.error.ApiException;

import java.util.List;

public final class GitHubInstallationAccess {
    private GitHubInstallationAccess() {}

    public static void requireVisible(long installationId, List<GitHubAppClient.GitHubInstallation> visibleInstallations) {
        boolean visible = visibleInstallations.stream().anyMatch(installation -> installation.id() == installationId);
        if (!visible) {
            throw ApiException.notFound("GITHUB_INSTALLATION_NOT_FOUND", "The GitHub App installation was not found.");
        }
    }
}
