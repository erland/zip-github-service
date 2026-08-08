package info.isaksson.erland.zipgithub.staging;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;

/** Creates one-time high-entropy claim tokens. Only the SHA-256 digest is persistent. */
@ApplicationScoped
public class StagingClaimTokenFactory {
    private final SecureRandom random;

    public StagingClaimTokenFactory() {
        this(new SecureRandom());
    }

    StagingClaimTokenFactory(SecureRandom random) {
        this.random = random;
    }

    public ClaimToken create() {
        String raw = StagingSecretCodec.randomUrlToken(random);
        return new ClaimToken(raw, StagingSecretCodec.digestHex(raw));
    }

    public record ClaimToken(String raw, String sha256) { }
}
