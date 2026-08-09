package info.isaksson.erland.zipgithub.github;

import java.util.Optional;

public interface GitHubPullRequestClient {
    GitHubPullRequest createDraftPullRequest(String accessToken, String repositoryFullName,
                                              String title, String headBranch, String baseBranch, String body);

    Optional<GitHubPullRequest> findOpenPullRequest(String accessToken, String repositoryFullName,
                                                    String headBranch, String baseBranch);

    default GitHubPullRequest getPullRequest(String accessToken, String repositoryFullName, long pullRequestNumber) {
        throw new UnsupportedOperationException("Pull request read is not implemented");
    }

    record GitHubPullRequest(long number, String htmlUrl, String state, boolean draft, boolean merged, String headSha) {
        public GitHubPullRequest(long number, String htmlUrl, String state, boolean draft) {
            this(number, htmlUrl, state, draft, false, null);
        }
    }
}
