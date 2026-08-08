package info.isaksson.erland.zipgithub.github;

import java.time.Instant;
import java.util.List;

public interface GitHubActionsDetailsClient {
    GitHubActionsDetails readCommitActionDetails(String installationToken, String repositoryFullName, String commitSha);

    record GitHubActionsDetails(String detailsUrl, List<Artifact> artifacts, List<FailureExcerpt> failures) {}

    record Artifact(long id, String name, long sizeBytes, boolean expired, Instant createdAt, Instant expiresAt,
                    long workflowRunId, String workflowName, String githubUrl) {}

    record FailureExcerpt(long workflowRunId, String workflowName, long jobId, String jobName, String stepName,
                          String tool, List<String> lines, List<String> contextLines, List<String> jobLogLines,
                          boolean logTruncated, String githubUrl) {}
}
