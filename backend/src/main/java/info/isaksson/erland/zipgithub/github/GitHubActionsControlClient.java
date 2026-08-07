package info.isaksson.erland.zipgithub.github;

public interface GitHubActionsControlClient {
    boolean hasActionsWritePermission(long installationId);
    Workflow workflow(String installationToken, String repositoryFullName, String workflowIdentifier);
    WorkflowRun workflowRun(String installationToken, String repositoryFullName, long runId);
    DispatchResult dispatch(String installationToken, String repositoryFullName, String workflowIdentifier, String ref);
    RerunResult rerunFailedJobs(String installationToken, String repositoryFullName, long runId);

    record Workflow(long id, String name, String path, String state, String htmlUrl) {}
    record WorkflowRun(long id, long workflowId, String workflowPath, String name, String headSha,
                       String headBranch, String status, String conclusion, String htmlUrl) {}
    record DispatchResult(Long workflowRunId, String htmlUrl) {}
    record RerunResult(long workflowRunId, String htmlUrl) {}
}
