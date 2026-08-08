package info.isaksson.erland.zipgithub.plan;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ImportPlanApprovalTest {
    @Test
    void recordsExactDigestAndApprover() {
        UUID importId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-08-06T20:00:00Z");
        ImportPlanApproval approval = new ImportPlanApproval(importId, planId, userId, "a".repeat(64), "b".repeat(64), "Describe exact change", approvedAt);
        assertEquals(importId, approval.importId());
        assertEquals(planId, approval.planId());
        assertEquals(userId, approval.approvedByUserId());
        assertEquals("a".repeat(64), approval.planDigestSha256());
        assertEquals("b".repeat(64), approval.selectionDigestSha256());
        assertEquals("Describe exact change", approval.commitMessage());
        assertEquals(approvedAt, approval.approvedAt());
    }

    @Test
    void rejectsInvalidDigest() {
        assertThrows(IllegalArgumentException.class, () -> new ImportPlanApproval(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "bad", "b".repeat(64), Instant.now()));
    }
    @Test
    void rejectsInvalidSelectionDigest() {
        assertThrows(IllegalArgumentException.class, () -> new ImportPlanApproval(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), "bad", Instant.now()));
    }

    @Test
    void legacyConstructorUsesDeterministicFallback() {
        UUID importId = UUID.randomUUID();
        ImportPlanApproval approval = new ImportPlanApproval(importId, UUID.randomUUID(), UUID.randomUUID(),
                "a".repeat(64), "b".repeat(64), Instant.now());
        assertEquals("Apply approved ZIP import " + importId, approval.commitMessage());
    }

}
