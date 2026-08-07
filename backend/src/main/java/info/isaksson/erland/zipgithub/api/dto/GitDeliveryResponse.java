package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record GitDeliveryResponse(UUID importId, String repositoryFullName, String baseBranch,
                                  String branchName, String baseCommitSha, String commitSha,
                                  String planDigestSha256, String status, Instant pushedAt) { }
