package info.isaksson.erland.zipgithub.archive;

import java.util.List;

public record ArchiveInventory(
        String strippedWrapperDirectory,
        List<String> ignoredPaths,
        List<ArchiveInventoryEntry> files) {
    public ArchiveInventory {
        ignoredPaths = List.copyOf(ignoredPaths);
        files = List.copyOf(files);
    }
}
