package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;
import java.util.UUID;

public record ImportPolicyResponse(
        UUID importId,
        String baseCommitSha,
        String policyVersion,
        boolean approvable,
        long added,
        long modified,
        long unchanged,
        long ignored,
        long blocked,
        long warnings,
        List<Entry> entries) {
    public record Entry(
            String path,
            String status,
            String comparisonStatus,
            String severity,
            String policyCode,
            String message,
            Long archiveSizeBytes,
            String archiveSha256,
            Long repositorySizeBytes,
            String repositorySha256) { }
}
