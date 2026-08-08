package info.isaksson.erland.zipgithub.plan;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CommitMessagePolicyTest {
    @Test
    void normalizesInteractiveMessage() {
        assertEquals("Fix CI\n\nKeep mode", CommitMessagePolicy.requireInteractive("  Fix CI\r\n\r\nKeep mode  "));
    }

    @Test
    void rejectsBlankControlCharactersAndOversize() {
        assertThrows(IllegalArgumentException.class, () -> CommitMessagePolicy.requireInteractive("   "));
        assertThrows(IllegalArgumentException.class, () -> CommitMessagePolicy.requireInteractive("bad\tmessage"));
        assertThrows(IllegalArgumentException.class, () -> CommitMessagePolicy.requireInteractive("x".repeat(501)));
    }

    @Test
    void legacyFallbackIsDeterministic() {
        UUID id = UUID.randomUUID();
        assertEquals("Apply approved ZIP import " + id, CommitMessagePolicy.persistedOrLegacyFallback(null, id));
    }
}
