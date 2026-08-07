package info.isaksson.erland.zipgithub.github;

/** Creates short-lived server-side credentials for one GitHub App installation. */
public interface GitHubInstallationTokenProvider {
    String createInstallationToken(long installationId);
}
