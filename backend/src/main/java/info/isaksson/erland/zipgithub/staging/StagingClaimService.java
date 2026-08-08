package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Authenticated boundary that converts a one-time bearer claim token into staging ownership. */
@ApplicationScoped
public class StagingClaimService {
    @Inject StagingImportPersistenceStore store;
    private final Clock clock;
    private final Duration claimedGrace;

    @Inject
    public StagingClaimService(@ConfigProperty(name = "zipgithub.staging.claimed-ttl-minutes", defaultValue = "240") long claimedTtlMinutes) {
        this(Clock.systemUTC(), Duration.ofMinutes(claimedTtlMinutes));
    }

    public StagingClaimService() { this(Clock.systemUTC(), Duration.ofHours(4)); }
    StagingClaimService(Clock clock) { this(clock, Duration.ofHours(4)); }
    StagingClaimService(Clock clock, Duration claimedGrace) {
        this.clock = clock;
        if (claimedGrace == null || claimedGrace.isZero() || claimedGrace.isNegative())
            throw new IllegalArgumentException("claimedGrace must be positive");
        this.claimedGrace = claimedGrace;
    }

    public ClaimedStaging claim(String rawToken, UUID ownerUserId) {
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            throw unavailable();
        }
        String tokenSha256 = StagingSecretCodec.digestHex(rawToken);
        Instant now = Instant.now(clock);
        var outcome = store.claimByTokenHash(tokenSha256, ownerUserId, now, now.plus(claimedGrace));
        if (outcome.result() == StagingImportPersistenceStore.ClaimResult.NOT_AVAILABLE || outcome.stagingImport() == null) {
            throw unavailable();
        }
        StagingImport staging = outcome.stagingImport();
        return new ClaimedStaging(staging.id(), staging.artifact().originalFilename(), staging.artifact().sizeBytes(),
                staging.artifact().sha256(), staging.expiresAt(), staging.claimedAt());
    }

    private static ApiException unavailable() {
        // Deliberately neutral for wrong, expired, already-taken and otherwise unusable tokens.
        return ApiException.gone("STAGING_CLAIM_UNAVAILABLE",
                "The staging upload is unavailable or has expired. Create a new upload and try again.");
    }

    public record ClaimedStaging(UUID stagingId, String originalFilename, long sizeBytes, String sha256,
                                 Instant expiresAt, Instant claimedAt) { }
}
