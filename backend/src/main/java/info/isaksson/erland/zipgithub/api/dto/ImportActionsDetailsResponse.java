package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportActionsDetailsResponse(UUID importId, String repositoryFullName, String commitSha, String detailsUrl,
                                           List<ArtifactResponse> artifacts, List<FailureResponse> failures, Instant checkedAt) {
    public record ArtifactResponse(long id, String name, long sizeBytes, boolean expired, Instant createdAt,
                                   Instant expiresAt, long workflowRunId, String workflowName, String githubUrl) {}
    public record FailureResponse(long workflowRunId, String workflowName, long jobId, String jobName, String stepName,
                                  String tool, List<String> lines, List<String> contextLines, List<String> jobLogLines,
                                  boolean logTruncated, String githubUrl) {}
}
