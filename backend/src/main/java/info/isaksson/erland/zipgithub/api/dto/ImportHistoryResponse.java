package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ImportHistoryResponse(
        UUID id,
        UUID projectId,
        String baseBranch,
        String status,
        Instant createdAt,
        String sourceFilename,
        Long sourceSizeBytes,
        String planDigestSha256,
        Long pullRequestNumber,
        String pullRequestUrl,
        String resumeStage) {}
