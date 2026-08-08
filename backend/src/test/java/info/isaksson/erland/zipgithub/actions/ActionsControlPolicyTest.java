package info.isaksson.erland.zipgithub.actions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionsControlPolicyTest {
    @Test void allowlistsAreOperationSpecificAndDefaultDeny() {
        var policy = new ActionsControlPolicy("ci.yml,42", ".github/workflows/release.yml");
        assertTrue(policy.dispatchAllowed("ci.yml", 7, ".github/workflows/ci.yml"));
        assertTrue(policy.dispatchAllowed("other.yml", 42, ".github/workflows/other.yml"));
        assertFalse(policy.rerunAllowed("ci.yml", 7, ".github/workflows/ci.yml"));
        assertTrue(policy.rerunAllowed("release.yml", 9, ".github/workflows/release.yml"));
        var deny = new ActionsControlPolicy("", "");
        assertFalse(deny.dispatchAllowed("ci.yml", 1, ".github/workflows/ci.yml"));
        assertFalse(deny.rerunAllowed("ci.yml", 1, ".github/workflows/ci.yml"));
    }

    @Test void invalidIdentifierIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ActionsControlPolicy("ci.yml, https://evil.example", ""));
    }
}
