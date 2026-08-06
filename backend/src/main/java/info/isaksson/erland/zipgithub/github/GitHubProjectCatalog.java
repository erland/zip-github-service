package info.isaksson.erland.zipgithub.github;

import java.util.List;

/** User-scoped GitHub catalogue used when a project is configured. */
public interface GitHubProjectCatalog {
    List<GitHubAppClient.GitHubInstallation> listUserInstallations(String userAccessToken);
    List<GitHubAppClient.GitHubRepository> listUserInstallationRepositories(String userAccessToken, long installationId);
    boolean branchExists(String userAccessToken, String repositoryFullName, String branch);
}
