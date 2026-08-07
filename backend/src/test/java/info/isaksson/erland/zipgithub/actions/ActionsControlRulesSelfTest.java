package info.isaksson.erland.zipgithub.actions;

import java.util.UUID;

public final class ActionsControlRulesSelfTest {
    public static void main(String[] args) {
        UUID current = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String ref = "zip-github/work-123";
        String sha = "a".repeat(40);
        assertTrue(ActionsControlRules.currentWork(current, ref, sha, current, ref, sha), "matching Work should be current");
        assertFalse(ActionsControlRules.currentWork(current, ref, sha, other, ref, sha), "older import must be stale");
        assertFalse(ActionsControlRules.currentWork(current, ref, sha, current, ref, "b".repeat(40)), "moved head must be stale");
        assertTrue(ActionsControlRules.exactRun(ref, sha, ref, sha), "exact run should match");
        assertFalse(ActionsControlRules.exactRun(ref, sha, ref, "b".repeat(40)), "different run SHA must be rejected");
        assertFalse(ActionsControlRules.exactRun(ref, sha, "main", sha), "different run ref must be rejected");
        System.out.println("ActionsControlRulesSelfTest passed");
    }
    private static void assertTrue(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void assertFalse(boolean value, String message) { if (value) throw new AssertionError(message); }
}
