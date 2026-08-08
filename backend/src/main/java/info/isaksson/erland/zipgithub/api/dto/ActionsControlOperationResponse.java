package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ActionsControlOperationResponse(UUID operationId, String operation, String status, boolean replayed,
                                              String workflowIdentifier, Long workflowId, Long workflowRunId,
                                              String branchRef, String targetCommitSha, String githubUrl,
                                              String errorCode, Instant createdAt, Instant updatedAt) {}
