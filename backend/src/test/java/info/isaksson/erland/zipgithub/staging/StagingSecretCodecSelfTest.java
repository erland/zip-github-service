package info.isaksson.erland.zipgithub.staging;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Dependency-free regression for the secret properties required by staging upload/claim. */
public final class StagingSecretCodecSelfTest {
    public static void main(String[] args) {
        String tokenA = StagingSecretCodec.randomUrlToken(new SecureRandom());
        String tokenB = StagingSecretCodec.randomUrlToken(new SecureRandom());
        if (tokenA.equals(tokenB)) throw new AssertionError("claim tokens must not repeat");
        if (Base64.getUrlDecoder().decode(tokenA).length != 32) throw new AssertionError("claim token must contain 256 bits");
        String hash = StagingSecretCodec.digestHex(tokenA);
        if (!hash.matches("[0-9a-f]{64}")) throw new AssertionError("claim token hash must be lowercase SHA-256");
        if (!MessageDigest.isEqual(StagingSecretCodec.digestBytes("credential"), StagingSecretCodec.digestBytes("credential")))
            throw new AssertionError("equal credentials must compare equal");
        if (MessageDigest.isEqual(StagingSecretCodec.digestBytes("credential"), StagingSecretCodec.digestBytes("other")))
            throw new AssertionError("different credentials must not compare equal");
        System.out.println("StagingSecretCodecSelfTest passed");
    }
}
