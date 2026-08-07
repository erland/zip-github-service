package info.isaksson.erland.zipgithub.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImportPlanResponse(
        UUID id,
        UUID importId,
        String sourceUploadSha256,
        String baseCommitSha,
        String policyVersion,
        String planDigestSha256,
        String status,
        boolean approvable,
        long added,
        long modified,
        long unchanged,
        long ignored,
        long blocked,
        long hardBlocked,
        long overridableBlocked,
        long warnings,
        List<Entry> entries,
        Instant createdAt) {
    public record Entry(
            String path, String status, String comparisonStatus, String severity, String blockerType,
            String policyCode, String message, Long archiveSizeBytes, String archiveSha256,
            Long repositorySizeBytes, String repositorySha256, boolean textCandidate) { }
}
