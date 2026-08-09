package info.isaksson.erland.zipgithub.github;

import java.time.Instant;
import java.util.List;

public interface GitHubActionsClient {
    GitHubActionsStatus readCommitActions(String installationToken, String repositoryFullName, String commitSha);

    record GitHubActionsStatus(String state, boolean terminal, String detailsUrl,
                               List<WorkflowRun> workflows, List<CheckRun> checks,
                               String diagnosticCode, String diagnosticMessage) {}

    record WorkflowRun(long id, long workflowId, String workflowPath, String headBranch, String headSha,
                       String name, String state, boolean terminal, String event, String htmlUrl,
                       Instant createdAt, Instant updatedAt, List<Job> jobs) {}

    record Job(long id, String name, String state, boolean terminal, String htmlUrl,
               Instant startedAt, Instant completedAt) {}

    record CheckRun(long id, String name, String state, boolean terminal, String htmlUrl,
                    String appName, Instant startedAt, Instant completedAt) {}
}
