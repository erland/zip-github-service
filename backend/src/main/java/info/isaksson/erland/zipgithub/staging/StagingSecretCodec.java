package info.isaksson.erland.zipgithub.staging;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Dependency-free cryptographic helpers for staging capabilities and one-time claim tokens. */
final class StagingSecretCodec {
    private StagingSecretCodec() { }

    static byte[] digestBytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    static String digestHex(String value) {
        return HexFormat.of().formatHex(digestBytes(value));
    }

    static String randomUrlToken(SecureRandom random) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
