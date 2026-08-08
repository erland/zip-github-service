package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportActionsStatusResponse(UUID importId, String repositoryFullName, String commitSha, String state,
                                          boolean terminal, String detailsUrl, List<WorkflowRunResponse> workflows,
                                          List<CheckRunResponse> checks, String diagnosticCode, String diagnosticMessage, Instant checkedAt) {
    public record WorkflowRunResponse(long id, long workflowId, String workflowPath, String headBranch, String headSha,
                                      String name, String state, boolean terminal, String event, String htmlUrl,
                                      Instant createdAt, Instant updatedAt, List<JobResponse> jobs) {}
    public record JobResponse(long id, String name, String state, boolean terminal, String htmlUrl,
                              Instant startedAt, Instant completedAt) {}
    public record CheckRunResponse(long id, String name, String state, boolean terminal, String htmlUrl,
                                   String appName, Instant startedAt, Instant completedAt) {}
}
