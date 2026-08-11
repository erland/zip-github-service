package info.isaksson.erland.zipgithub.archive;

import java.util.List;
import java.util.Map;

public record ArchiveInventory(
        String strippedWrapperDirectory,
        List<String> ignoredPaths,
        List<ArchiveInventoryEntry> files,
        Map<String, String> gitIgnoreFiles) {
    public ArchiveInventory {
        ignoredPaths = List.copyOf(ignoredPaths);
        files = List.copyOf(files);
        gitIgnoreFiles = gitIgnoreFiles == null ? Map.of() : Map.copyOf(gitIgnoreFiles);
    }

    /** Backward-compatible constructor for tests/callers that do not need prospective .gitignore metadata. */
    public ArchiveInventory(String strippedWrapperDirectory, List<String> ignoredPaths, List<ArchiveInventoryEntry> files) {
        this(strippedWrapperDirectory, ignoredPaths, files, Map.of());
    }
}
