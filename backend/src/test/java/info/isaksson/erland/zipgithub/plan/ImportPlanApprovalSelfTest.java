package info.isaksson.erland.zipgithub.plan;

import java.time.Instant;
import java.util.UUID;

public final class ImportPlanApprovalSelfTest {
    public static void main(String[] args) {
        var approval = new ImportPlanApproval(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "b".repeat(64), Instant.parse("2026-08-06T20:00:00Z"));
        if (!approval.planDigestSha256().equals("b".repeat(64))) throw new AssertionError("Digest mismatch");
        try {
            new ImportPlanApproval(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "invalid", Instant.now());
            throw new AssertionError("Invalid digest accepted");
        } catch (IllegalArgumentException expected) { }
    }
}
