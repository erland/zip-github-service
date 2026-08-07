package info.isaksson.erland.zipgithub.plan;

import info.isaksson.erland.zipgithub.archive.ArchiveInventory;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryEntry;
import info.isaksson.erland.zipgithub.comparison.ImportFileStatus;
import info.isaksson.erland.zipgithub.policy.ImportPolicyEntry;
import info.isaksson.erland.zipgithub.policy.ImportPolicyResult;
import info.isaksson.erland.zipgithub.policy.ImportPolicySeverity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImportPlanFactoryTest {
    private static final String SHA_A = "a".repeat(64);
    private static final String SHA_B = "b".repeat(64);
    private static final String BASE = "1".repeat(40);

    @Test
    void createsDeterministicImmutablePlanIdentity() {
        UUID owner = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        ArchiveInventory archive = new ArchiveInventory(null, List.of(".DS_Store"), List.of(
                new ArchiveInventoryEntry("src/App.java", 10, SHA_A, true)));
        ImportPolicyResult policy = new ImportPolicyResult(importId, BASE, "mvp-1", true, List.of(
                new ImportPolicyEntry("src/App.java", ImportFileStatus.MODIFIED, ImportFileStatus.MODIFIED,
                        ImportPolicySeverity.NONE, null, null, 10L, SHA_A, 8L, SHA_B),
                new ImportPolicyEntry(".DS_Store", ImportFileStatus.IGNORED, null,
                        ImportPolicySeverity.NONE, "TRANSPORT_NOISE", "ignored", null, null, null, null)));

        ImportPlanFactory factory = new ImportPlanFactory();
        var first = factory.create(owner, SHA_B, archive, policy, Instant.parse("2026-08-06T19:00:00Z"));
        var second = factory.create(owner, SHA_B, archive, policy, Instant.parse("2026-08-06T20:00:00Z"));

        assertNotEquals(first.id(), second.id());
        assertEquals(first.planDigestSha256(), second.planDigestSha256());
        assertEquals("READY", first.status());
        assertTrue(first.approvable());
        assertEquals(List.of(".DS_Store", "src/App.java"), first.entries().stream().map(ImmutableImportPlanEntry::path).toList());
        assertTrue(first.entries().get(1).textCandidate());
        assertThrows(UnsupportedOperationException.class, () -> first.entries().add(first.entries().get(0)));
    }

    @Test
    void blockedPolicyCreatesDraftPlan() {
        UUID importId = UUID.randomUUID();
        ArchiveInventory archive = new ArchiveInventory(null, List.of(), List.of());
        ImportPolicyResult policy = new ImportPolicyResult(importId, BASE, "mvp-1", false, List.of(
                new ImportPolicyEntry(".github/workflows/ci.yml", ImportFileStatus.BLOCKED, ImportFileStatus.ADDED,
                        ImportPolicySeverity.BLOCKING, "GITHUB_WORKFLOW_PROTECTED", "blocked", 1L, SHA_A, null, null)));

        var plan = new ImportPlanFactory().create(UUID.randomUUID(), SHA_B, archive, policy, Instant.now());
        assertEquals("DRAFT", plan.status());
        assertFalse(plan.approvable());
    }
}
