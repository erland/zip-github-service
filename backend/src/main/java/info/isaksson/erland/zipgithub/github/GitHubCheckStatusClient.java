package info.isaksson.erland.zipgithub.github;

public interface GitHubCheckStatusClient {
    GitHubCheckStatus readCommitChecks(String installationToken, String repositoryFullName, String commitSha);

    record GitHubCheckStatus(String state, boolean terminal, int total, int pending, int successful,
                             int failed, int cancelled, String detailsUrl) {}
}
