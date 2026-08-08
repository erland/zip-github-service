package info.isaksson.erland.zipgithub.policy;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryEntry;
import info.isaksson.erland.zipgithub.comparison.ImportComparison;
import info.isaksson.erland.zipgithub.comparison.ImportComparisonEntry;
import info.isaksson.erland.zipgithub.comparison.ImportFileStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImportPolicyServiceTest {
    @Test
    void classifiesHardAndOverridableBlockersAndKeepsMixedPlanReviewable() {
        UUID importId = UUID.randomUUID();
        var archive = new ArchiveInventory(null, List.of("__MACOSX/._README.md"), List.of(
                new ArchiveInventoryEntry(".github/workflows/ci.yml", 10, "a", true),
                new ArchiveInventoryEntry(".env.local", 10, "b", true),
                new ArchiveInventoryEntry("secret.pem", 10, "c", true),
                new ArchiveInventoryEntry("large.bin", 101, "d", false)));
        var comparison = new ImportComparison(importId, "a".repeat(40), List.of(
                entry(".github/workflows/ci.yml", ImportFileStatus.ADDED, 10L),
                entry(".env.local", ImportFileStatus.ADDED, 10L),
                entry("large.bin", ImportFileStatus.ADDED, 101L),
                entry("old.txt", ImportFileStatus.WOULD_DELETE, null),
                entry("secret.pem", ImportFileStatus.ADDED, 10L),
                entry("generated.bin", ImportFileStatus.IGNORED, 23_821L),
                entry("src/App.java", ImportFileStatus.MODIFIED, 10L)));

        ImportPolicyResult result = new ImportPolicyService(100).evaluate(archive, comparison);

        assertTrue(result.approvable());
        assertEquals(4, result.blockers());
        assertEquals(2, result.hardBlockers());
        assertEquals(2, result.overridableBlockers());
        assertEquals(2, result.warnings());
        assertEquals(2, result.count(ImportFileStatus.IGNORED));
        assertEquals(List.of(".env.local", ".github/workflows/ci.yml", "__MACOSX/._README.md", "generated.bin", "large.bin", "old.txt", "secret.pem", "src/App.java"),
                result.entries().stream().map(ImportPolicyEntry::path).toList());
        assertEquals("ENVIRONMENT_FILE_WARNING", result.entries().get(0).policyCode());
        assertEquals(ImportPolicyBlockerType.OVERRIDABLE_BLOCKED,
                result.entries().stream().filter(e -> e.path().startsWith(".github/")).findFirst().orElseThrow().blockerType());
        assertEquals(ImportPolicyBlockerType.OVERRIDABLE_BLOCKED,
                result.entries().stream().filter(e -> e.path().equals("old.txt")).findFirst().orElseThrow().blockerType());
        assertEquals(ImportPolicyBlockerType.HARD_BLOCKED,
                result.entries().stream().filter(e -> e.path().equals("secret.pem")).findFirst().orElseThrow().blockerType());
        var ignored = result.entries().stream().filter(e -> e.path().equals("generated.bin")).findFirst().orElseThrow();
        assertEquals(ImportFileStatus.IGNORED, ignored.status());
        assertEquals(ImportPolicySeverity.WARNING, ignored.severity());
        assertEquals(ImportPolicyBlockerType.NONE, ignored.blockerType());
        assertEquals("GITIGNORE_IGNORED", ignored.policyCode());
    }

    @Test
    void gitMetadataIsHardBlockedButDoesNotBlockOrdinaryChanges() {
        var comparison = new ImportComparison(UUID.randomUUID(), "c".repeat(40), List.of(
                entry(".git/config", ImportFileStatus.ADDED, 10L),
                entry("src/App.java", ImportFileStatus.MODIFIED, 10L)));
        var result = new ImportPolicyService(100).evaluate(new ArchiveInventory(null, List.of(), List.of()), comparison);

        assertTrue(result.approvable());
        assertEquals(1, result.hardBlockers());
        assertEquals(0, result.overridableBlockers());
        var git = result.entries().stream().filter(e -> e.path().equals(".git/config")).findFirst().orElseThrow();
        assertEquals(ImportFileStatus.BLOCKED, git.status());
        assertEquals(ImportPolicyBlockerType.HARD_BLOCKED, git.blockerType());
    }

    @Test
    void planWithOnlyBlockedChangesIsNotApprovableUntilSelectionCanChooseSomething() {
        var comparison = new ImportComparison(UUID.randomUUID(), "d".repeat(40), List.of(
                entry(".github/workflows/ci.yml", ImportFileStatus.ADDED, 10L),
                entry("old.txt", ImportFileStatus.WOULD_DELETE, null)));
        var result = new ImportPolicyService(100).evaluate(new ArchiveInventory(null, List.of(), List.of()), comparison);

        assertFalse(result.approvable());
        assertEquals(0, result.hardBlockers());
        assertEquals(2, result.overridableBlockers());
    }


    @Test
    void unchangedWorkflowDoesNotRequireOverrideButActualWorkflowChangesDo() {
        var comparison = new ImportComparison(UUID.randomUUID(), "e".repeat(40), List.of(
                entry(".github/workflows/unchanged.yml", ImportFileStatus.UNCHANGED, 10L),
                entry(".github/workflows/modified.yml", ImportFileStatus.MODIFIED, 10L),
                entry(".github/workflows/added.yml", ImportFileStatus.ADDED, 10L),
                entry(".github/workflows/deleted.yml", ImportFileStatus.WOULD_DELETE, null)));

        var result = new ImportPolicyService(100).evaluate(new ArchiveInventory(null, List.of(), List.of()), comparison);

        var unchanged = result.entries().stream().filter(e -> e.path().endsWith("unchanged.yml")).findFirst().orElseThrow();
        assertEquals(ImportFileStatus.UNCHANGED, unchanged.status());
        assertEquals(ImportPolicyBlockerType.NONE, unchanged.blockerType());
        assertNull(unchanged.policyCode());

        for (String filename : List.of("modified.yml", "added.yml", "deleted.yml")) {
            var changed = result.entries().stream().filter(e -> e.path().endsWith(filename)).findFirst().orElseThrow();
            assertEquals(ImportFileStatus.BLOCKED, changed.status(), filename);
            assertEquals(ImportPolicyBlockerType.OVERRIDABLE_BLOCKED, changed.blockerType(), filename);
            assertEquals("GITHUB_WORKFLOW_PROTECTED", changed.policyCode(), filename);
        }
    }

    @Test
    void exampleEnvironmentFileIsAllowedWithoutWarning() {
        var comparison = new ImportComparison(UUID.randomUUID(), "b".repeat(40),
                List.of(entry(".env.example", ImportFileStatus.ADDED, 10L)));
        var result = new ImportPolicyService(100).evaluate(new ArchiveInventory(null, List.of(), List.of()), comparison);
        assertTrue(result.approvable());
        assertEquals(0, result.warnings());
        assertEquals(ImportFileStatus.ADDED, result.entries().getFirst().status());
    }

    private static ImportComparisonEntry entry(String path, ImportFileStatus status, Long archiveSize) {
        return new ImportComparisonEntry(path, status, archiveSize, archiveSize == null ? null : "a",
                status == ImportFileStatus.ADDED ? null : 10L, status == ImportFileStatus.ADDED ? null : "b");
    }
}
