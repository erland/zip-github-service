package info.isaksson.erland.zipgithub.archive;

public record ArchiveEntryDescriptor(
        String path,
        ArchiveEntryType type,
        long compressedSize,
        long uncompressedSize,
        int unixMode) {
}
