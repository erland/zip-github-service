package info.isaksson.erland.zipgithub.comparison;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryEntry;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshotEntry;
import info.isaksson.erland.zipgithub.upload.GitFileMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportComparisonServiceTest {
    @Test
    void classifiesAllFourStatusesAndSortsByPath() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(
                new ArchiveInventoryEntry("same.txt", 4, "aaaa", true),
                new ArchiveInventoryEntry("new.txt", 3, "bbbb", true),
                new ArchiveInventoryEntry("changed.txt", 7, "cccc", true)));
        var snapshot = new RepositorySnapshot(importId, "owner/repo", "main", "f".repeat(40), List.of(
                new RepositorySnapshotEntry("removed.txt", "100644", "blob", "1".repeat(40), 8, "dddd"),
                new RepositorySnapshotEntry("same.txt", "100644", "blob", "2".repeat(40), 4, "aaaa"),
                new RepositorySnapshotEntry("changed.txt", "100644", "blob", "3".repeat(40), 7, "eeee")), Instant.EPOCH);

        ImportComparison comparison = new ImportComparisonService().compare(archive, snapshot);

        assertEquals(List.of("changed.txt", "new.txt", "removed.txt", "same.txt"),
                comparison.entries().stream().map(ImportComparisonEntry::path).toList());
        assertEquals(List.of(ImportFileStatus.MODIFIED, ImportFileStatus.ADDED,
                        ImportFileStatus.WOULD_DELETE, ImportFileStatus.UNCHANGED),
                comparison.entries().stream().map(ImportComparisonEntry::status).toList());
    }
    @Test
    void treatsApprovedModeChangeAsModifiedAndPreservesRepositoryModeWhenZipModeUnknown() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(
                new ArchiveInventoryEntry("run.sh", 4, "aaaa", true),
                new ArchiveInventoryEntry("keep.sh", 4, "bbbb", true),
                new ArchiveInventoryEntry("new.sh", 4, "cccc", true)));
        var snapshot = new RepositorySnapshot(importId, "owner/repo", "main", "f".repeat(40), List.of(
                new RepositorySnapshotEntry("run.sh", "100644", "blob", "1".repeat(40), 4, "aaaa"),
                new RepositorySnapshotEntry("keep.sh", "100755", "blob", "2".repeat(40), 4, "bbbb")), Instant.EPOCH);

        ImportComparison comparison = new ImportComparisonService().compare(archive, snapshot, Map.of("run.sh", GitFileMode.EXECUTABLE));
        var byPath = comparison.entries().stream().collect(java.util.stream.Collectors.toMap(ImportComparisonEntry::path, e -> e));
        assertEquals(ImportFileStatus.MODIFIED, byPath.get("run.sh").status());
        assertEquals("100755", byPath.get("run.sh").effectiveMode());
        assertEquals(true, byPath.get("run.sh").modeChanged());
        assertEquals(ImportFileStatus.UNCHANGED, byPath.get("keep.sh").status());
        assertEquals("100755", byPath.get("keep.sh").effectiveMode());
        assertEquals("100644", byPath.get("new.sh").effectiveMode());
    }

    @Test
    void marksOnlyUntrackedZipPathsMatchingRepositoryGitignoreAsIgnored() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(
                new ArchiveInventoryEntry("shortcut/releases/zip-github.shortcut", 20, "aaaa", false),
                new ArchiveInventoryEntry("generated/cache.bin", 5, "bbbb", false),
                new ArchiveInventoryEntry("tracked.log", 4, "cccc", true),
                new ArchiveInventoryEntry("keep.log", 3, "dddd", true)));
        var snapshot = new RepositorySnapshot(importId, "owner/repo", "main", "f".repeat(40), List.of(
                new RepositorySnapshotEntry(".gitignore", "100644", "blob", "1".repeat(40), 40, "eeee"),
                new RepositorySnapshotEntry("tracked.log", "100644", "blob", "2".repeat(40), 4, "ffff")),
                Map.of(".gitignore", "/shortcut/releases/*.shortcut\n*.log\n!keep.log\ngenerated/\n"), Instant.EPOCH);

        ImportComparison comparison = new ImportComparisonService().compare(archive, snapshot);
        var byPath = comparison.entries().stream().collect(java.util.stream.Collectors.toMap(ImportComparisonEntry::path, e -> e));

        assertEquals(ImportFileStatus.IGNORED, byPath.get("shortcut/releases/zip-github.shortcut").status());
        assertEquals(ImportFileStatus.IGNORED, byPath.get("generated/cache.bin").status());
        assertEquals(ImportFileStatus.MODIFIED, byPath.get("tracked.log").status(), "tracked files remain tracked even when .gitignore matches");
        assertEquals(ImportFileStatus.ADDED, byPath.get("keep.log").status(), "later negation must re-include a path");
    }

    @Test
    void nestedGitignoreOverridesParentForItsSubtree() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(
                new ArchiveInventoryEntry("build/drop.txt", 1, "a", true),
                new ArchiveInventoryEntry("build/keep.txt", 1, "b", true)));
        var snapshot = new RepositorySnapshot(importId, "owner/repo", "main", "f".repeat(40), List.of(),
                Map.of(".gitignore", "build/*\n", "build/.gitignore", "!keep.txt\n"), Instant.EPOCH);

        var byPath = new ImportComparisonService().compare(archive, snapshot).entries().stream()
                .collect(java.util.stream.Collectors.toMap(ImportComparisonEntry::path, e -> e));
        assertEquals(ImportFileStatus.IGNORED, byPath.get("build/drop.txt").status());
        assertEquals(ImportFileStatus.ADDED, byPath.get("build/keep.txt").status());
    }

    @Test
    void usesGitignoreFromUploadedZipForProspectiveUntrackedFiles() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(
                new ArchiveInventoryEntry(".gitignore", 24, "aaaa", true),
                new ArchiveInventoryEntry("scripts/__pycache__/build.cpython-313.pyc", 10, "bbbb", false),
                new ArchiveInventoryEntry("src/new.txt", 3, "cccc", true)),
                Map.of(".gitignore", "__pycache__/\n*.pyc\n"));
        var snapshot = new RepositorySnapshot(importId, "owner/repo", "main", "f".repeat(40), List.of(), Map.of(), Instant.EPOCH);

        var byPath = new ImportComparisonService().compare(archive, snapshot).entries().stream()
                .collect(java.util.stream.Collectors.toMap(ImportComparisonEntry::path, e -> e));

        assertEquals(ImportFileStatus.ADDED, byPath.get(".gitignore").status());
        assertEquals(ImportFileStatus.IGNORED, byPath.get("scripts/__pycache__/build.cpython-313.pyc").status());
        assertEquals(ImportFileStatus.ADDED, byPath.get("src/new.txt").status());
    }

    @Test
    void removesRepositoryGitignoreRulesWhenCompleteZipDeletesThatGitignore() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(
                new ArchiveInventoryEntry("generated/new.txt", 3, "aaaa", true)));
        var snapshot = new RepositorySnapshot(importId, "owner/repo", "main", "f".repeat(40), List.of(
                new RepositorySnapshotEntry(".gitignore", "100644", "blob", "1".repeat(40), 11, "bbbb")),
                Map.of(".gitignore", "generated/\n"), Instant.EPOCH);

        var byPath = new ImportComparisonService().compare(archive, snapshot).entries().stream()
                .collect(java.util.stream.Collectors.toMap(ImportComparisonEntry::path, e -> e));

        assertEquals(ImportFileStatus.WOULD_DELETE, byPath.get(".gitignore").status());
        assertEquals(ImportFileStatus.ADDED, byPath.get("generated/new.txt").status(),
                "a .gitignore deleted by the complete ZIP must not keep hiding new paths");
    }

}
