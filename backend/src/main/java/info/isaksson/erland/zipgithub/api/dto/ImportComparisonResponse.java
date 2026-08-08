package info.isaksson.erland.zipgithub.api.dto;

import java.util.List;
import java.util.UUID;

public record ImportComparisonResponse(
        UUID importId,
        String baseCommitSha,
        long added,
        long modified,
        long unchanged,
        long wouldDelete,
        List<Entry> entries) {
    public record Entry(
            String path,
            String status,
            Long archiveSizeBytes,
            String archiveSha256,
            Long repositorySizeBytes,
            String repositorySha256,
            String archiveMode, String repositoryMode, String effectiveMode, boolean modeChanged) { }
}
