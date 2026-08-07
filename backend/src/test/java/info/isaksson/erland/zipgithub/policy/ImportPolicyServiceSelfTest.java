package info.isaksson.erland.zipgithub.policy;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.comparison.*;
import java.util.List;
import java.util.UUID;

public final class ImportPolicyServiceSelfTest {
    public static void main(String[] args) {
        var comparison = new ImportComparison(UUID.randomUUID(), "a".repeat(40), List.of(
                new ImportComparisonEntry(".github/workflows/ci.yml", ImportFileStatus.ADDED, 4L, "a", null, null),
                new ImportComparisonEntry(".env", ImportFileStatus.MODIFIED, 4L, "b", 3L, "c"),
                new ImportComparisonEntry("old.txt", ImportFileStatus.WOULD_DELETE, null, null, 3L, "d")));
        var result = new ImportPolicyService(50).evaluate(new ArchiveInventory(null, List.of(".DS_Store"), List.of()), comparison);
        if (result.approvable() || result.blockers() != 2 || result.warnings() != 1 || result.count(ImportFileStatus.IGNORED) != 1) {
            throw new AssertionError("Unexpected policy result: " + result);
        }
    }
}
