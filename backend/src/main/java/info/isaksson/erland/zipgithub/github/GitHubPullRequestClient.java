package info.isaksson.erland.zipgithub.github;

import java.util.Optional;

public interface GitHubPullRequestClient {
    GitHubPullRequest createDraftPullRequest(String installationToken, String repositoryFullName,
                                              String title, String headBranch, String baseBranch, String body);

    Optional<GitHubPullRequest> findOpenPullRequest(String installationToken, String repositoryFullName,
                                                    String headBranch, String baseBranch);

    record GitHubPullRequest(long number, String htmlUrl, String state, boolean draft) { }
}
