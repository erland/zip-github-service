package info.isaksson.erland.zipgithub.plan;

import java.util.UUID;

public final class CommitMessagePolicySelfTest {
    public static void main(String[] args) {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String normalized = CommitMessagePolicy.requireInteractive("  Fix CI\r\n\r\nKeep executable bit  ");
        if (!normalized.equals("Fix CI\n\nKeep executable bit")) throw new AssertionError(normalized);
        if (!CommitMessagePolicy.defaultSuggestion(id).equals("Apply approved ZIP import " + id)) throw new AssertionError();
        reject("   ");
        reject("bad\u0000message");
        reject("x".repeat(CommitMessagePolicy.MAX_LENGTH + 1));
        System.out.println("CommitMessagePolicySelfTest passed");
    }
    private static void reject(String value) {
        try { CommitMessagePolicy.requireInteractive(value); throw new AssertionError("accepted invalid commit message"); }
        catch (IllegalArgumentException expected) { }
    }
}
