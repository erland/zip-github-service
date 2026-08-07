package info.isaksson.erland.zipgithub.api.dto;

public record DispatchWorkflowRequest(String workflowIdentifier, String expectedRef, String expectedCommitSha,
                                      String idempotencyKey, boolean confirmed) {}
