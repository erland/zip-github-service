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
        ImportPlanApproval approval = new ImportPlanApproval(importId, planId, userId, "a".repeat(64), approvedAt);
        assertEquals(importId, approval.importId());
        assertEquals(planId, approval.planId());
        assertEquals(userId, approval.approvedByUserId());
        assertEquals("a".repeat(64), approval.planDigestSha256());
        assertEquals(approvedAt, approval.approvedAt());
    }

    @Test
    void rejectsInvalidDigest() {
        assertThrows(IllegalArgumentException.class, () -> new ImportPlanApproval(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "bad", Instant.now()));
    }
}
