package info.isaksson.erland.zipgithub.snapshot;

/** One immutable entry from an exact Git tree. */
public record RepositorySnapshotEntry(
        String path,
        String mode,
        String objectType,
        String objectId,
        long sizeBytes,
        String sha256) {
}
