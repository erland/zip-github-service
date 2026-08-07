package info.isaksson.erland.zipgithub.github;

import java.time.Instant;
import java.util.List;

public interface GitHubCommitHistoryClient {
    List<Commit> listBranchCommits(String installationToken, String repositoryFullName, String branch, int limit);

    record Commit(String sha, String message, String authorName, String authorEmail, Instant authoredAt, String htmlUrl) {}
}
