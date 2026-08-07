package info.isaksson.erland.zipgithub.comparison;

import info.isaksson.erland.zipgithub.archive.*;
import info.isaksson.erland.zipgithub.snapshot.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ImportComparisonServiceSelfTest {
    public static void main(String[] args) {
        UUID id = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of(), List.of(new ArchiveInventoryEntry("a", 1, "11", true)));
        var repo = new RepositorySnapshot(id, "o/r", "main", "f".repeat(40),
                List.of(new RepositorySnapshotEntry("a", "100644", "blob", "a".repeat(40), 1, "11"),
                        new RepositorySnapshotEntry("b", "100644", "blob", "b".repeat(40), 1, "22")), Instant.EPOCH);
        var result = new ImportComparisonService().compare(archive, repo);
        if (result.entries().size() != 2 || result.entries().get(0).status() != ImportFileStatus.UNCHANGED
                || result.entries().get(1).status() != ImportFileStatus.WOULD_DELETE) throw new AssertionError(result);
        System.out.println("ImportComparisonServiceSelfTest passed");
    }
}
