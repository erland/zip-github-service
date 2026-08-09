package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkSessionResponse(UUID id, UUID projectId, String baseBranch, String branchName, String status,
                                  String headCommitSha, String remoteHeadCommitSha, boolean branchChangedExternally,
                                  UUID lastImportId, Long pullRequestNumber, String pullRequestUrl,
                                  Instant createdAt, Instant updatedAt) {}
