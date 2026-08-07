package info.isaksson.erland.zipgithub.comparison;

public record ImportComparisonEntry(
        String path,
        ImportFileStatus status,
        Long archiveSizeBytes,
        String archiveSha256,
        Long repositorySizeBytes,
        String repositorySha256) {
}
