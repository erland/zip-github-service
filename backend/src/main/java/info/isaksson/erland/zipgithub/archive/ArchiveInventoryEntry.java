package info.isaksson.erland.zipgithub.archive;

public record ArchiveInventoryEntry(
        String path,
        long size,
        String sha256,
        boolean textCandidate) {
}
