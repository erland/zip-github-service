package info.isaksson.erland.zipbuildserver.application.retention;

public record RetentionCleanupSummary(
        int packageFilesDeleted,
        int artifactsDeleted,
        int workspacesDeleted,
        int sessionsDeleted) {
}
