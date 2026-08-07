package info.isaksson.erland.zipgithub.application;

import java.time.Instant;
import java.util.UUID;

public record WorkSession(UUID id, UUID projectId, UUID ownerUserId, String baseBranch, String branchName,
                          String status, String headCommitSha, String baseCommitSha, UUID lastImportId,
                          String lastPlanDigestSha256, Long pullRequestNumber, String pullRequestUrl,
                          Instant createdAt, Instant updatedAt) {
    public boolean hasCommit() { return headCommitSha != null && !headCommitSha.isBlank(); }
}
