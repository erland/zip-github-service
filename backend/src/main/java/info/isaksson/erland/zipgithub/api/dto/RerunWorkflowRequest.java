package info.isaksson.erland.zipgithub.api.dto;

public record RerunWorkflowRequest(long workflowRunId, String expectedRef, String expectedCommitSha,
                                   String idempotencyKey, boolean confirmed) {}
