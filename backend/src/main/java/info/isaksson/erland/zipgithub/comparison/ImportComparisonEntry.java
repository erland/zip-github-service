package info.isaksson.erland.zipgithub.comparison;

public record ImportComparisonEntry(
        String path,
        ImportFileStatus status,
        Long archiveSizeBytes,
        String archiveSha256,
        Long repositorySizeBytes,
        String repositorySha256,
        String archiveMode,
        String repositoryMode,
        String effectiveMode,
        boolean modeChanged) {
    public ImportComparisonEntry(String path, ImportFileStatus status, Long archiveSizeBytes, String archiveSha256,
                                 Long repositorySizeBytes, String repositorySha256) {
        this(path, status, archiveSizeBytes, archiveSha256, repositorySizeBytes, repositorySha256,
                null, null, null, false);
    }
}

