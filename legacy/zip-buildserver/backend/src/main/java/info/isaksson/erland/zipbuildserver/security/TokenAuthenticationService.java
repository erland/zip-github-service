package info.isaksson.erland.zipbuildserver.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TokenAuthenticationService {
    public boolean isAuthorized(String authorizationHeader, String expectedToken) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return false;
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }

        String prefix = "Bearer ";
        if (!authorizationHeader.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return false;
        }

        String suppliedToken = authorizationHeader.substring(prefix.length()).trim();
        if (suppliedToken.isBlank()) {
            return false;
        }

        byte[] supplied = suppliedToken.getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(supplied, expected);
    }
}
