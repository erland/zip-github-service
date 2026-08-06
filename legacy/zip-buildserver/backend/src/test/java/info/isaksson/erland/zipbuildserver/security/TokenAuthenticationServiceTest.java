package info.isaksson.erland.zipbuildserver.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenAuthenticationServiceTest {
    private final TokenAuthenticationService service = new TokenAuthenticationService();

    @Test
    void acceptsMatchingBearerToken() {
        assertTrue(service.isAuthorized("Bearer secret-token", "secret-token"));
    }

    @Test
    void rejectsMissingMalformedOrWrongTokens() {
        assertFalse(service.isAuthorized(null, "secret-token"));
        assertFalse(service.isAuthorized("", "secret-token"));
        assertFalse(service.isAuthorized("Basic secret-token", "secret-token"));
        assertFalse(service.isAuthorized("Bearer wrong-token", "secret-token"));
        assertFalse(service.isAuthorized("Bearer secret-token", ""));
    }
}
