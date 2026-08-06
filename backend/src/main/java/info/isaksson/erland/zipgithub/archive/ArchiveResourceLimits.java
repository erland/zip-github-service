package info.isaksson.erland.zipgithub.archive;

/** Resource limits applied while inspecting and inflating an uploaded ZIP. */
public record ArchiveResourceLimits(
        long maxCompressedBytes,
        long maxUncompressedBytes,
        int maxEntries,
        long maxSingleFileBytes,
        int maxPathLength,
        double maxCompressionRatio) {

    public ArchiveResourceLimits {
        if (maxCompressedBytes <= 0 || maxUncompressedBytes <= 0 || maxEntries <= 0
                || maxSingleFileBytes <= 0 || maxPathLength <= 0 || maxCompressionRatio <= 0) {
            throw new IllegalArgumentException("All archive resource limits must be positive");
        }
        if (maxSingleFileBytes > maxUncompressedBytes) {
            throw new IllegalArgumentException("Single-file limit cannot exceed total uncompressed limit");
        }
    }

    public static ArchiveResourceLimits defaults() {
        return new ArchiveResourceLimits(
                100L * 1024 * 1024,
                500L * 1024 * 1024,
                20_000,
                50L * 1024 * 1024,
                1_024,
                100.0);
    }
}
