package info.isaksson.erland.zipgithub.actions;

import java.time.Instant;
import java.util.UUID;

public record ActionsControlAudit(UUID id, UUID ownerUserId, UUID projectId, UUID importId, String operation,
                                  String workflowIdentifier, Long workflowId, Long workflowRunId, String branchRef,
                                  String targetCommitSha, String idempotencyKey, String status, String githubUrl,
                                  String errorCode, Instant createdAt, Instant updatedAt) {}
