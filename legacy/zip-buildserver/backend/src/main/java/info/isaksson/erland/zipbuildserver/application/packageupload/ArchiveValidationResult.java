package info.isaksson.erland.zipbuildserver.application.packageupload;

public record ArchiveValidationResult(
        long extractedSizeBytes,
        int fileCount,
        String topLevelEntries) {
}
