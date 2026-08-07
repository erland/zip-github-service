package info.isaksson.erland.zipgithub.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SameOriginPolicyTest {
    @Test void acceptsExactOriginAndDefaultPorts() {
        assertTrue(SameOriginPolicy.matches("https://example.test/app", "https://example.test"));
        assertTrue(SameOriginPolicy.matches("http://localhost:5173", "http://localhost:5173"));
    }
    @Test void rejectsHostPortSchemeAndMalformedValues() {
        assertFalse(SameOriginPolicy.matches("https://example.test", "https://evil.test"));
        assertFalse(SameOriginPolicy.matches("https://example.test", "http://example.test"));
        assertFalse(SameOriginPolicy.matches("http://localhost:5173", "http://localhost:5174"));
        assertFalse(SameOriginPolicy.matches("https://example.test", "not an origin"));
    }
}
