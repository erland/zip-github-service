package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import info.isaksson.erland.zipgithub.upload.ZipIngestionService;
import info.isaksson.erland.zipgithub.upload.UploadFileModeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Creates transport-only staging rows from ZIP bytes after capability authorization. */
@ApplicationScoped
public class StagingUploadService {
    private final ZipIngestionService ingestion;
    private final StagingImportPersistenceStore store;
    private final StagingClaimTokenFactory tokens;
    private final UploadFileModeService fileModes;
    private final Duration availableTtl;
    private final long maximumLiveObjects;
    private final long maximumLiveBytes;
    private final String frontendUrl;
    private final Clock clock;

    @Inject
    public StagingUploadService(ZipIngestionService ingestion,
                                StagingImportPersistenceStore store,
                                StagingClaimTokenFactory tokens, UploadFileModeService fileModes,
                                @ConfigProperty(name = "zipgithub.staging.available-ttl-minutes", defaultValue = "60") long availableTtlMinutes,
                                @ConfigProperty(name = "zipgithub.staging.max-live-objects", defaultValue = "100") long maximumLiveObjects,
                                @ConfigProperty(name = "zipgithub.staging.max-live-bytes", defaultValue = "1073741824") long maximumLiveBytes,
                                @ConfigProperty(name = "zipgithub.frontend-url") String frontendUrl) {
        this(ingestion, store, tokens, fileModes, Duration.ofMinutes(availableTtlMinutes), maximumLiveObjects, maximumLiveBytes, frontendUrl, Clock.systemUTC());
    }

    StagingUploadService(ZipIngestionService ingestion, StagingImportPersistenceStore store,
                         StagingClaimTokenFactory tokens, Duration availableTtl, String frontendUrl, Clock clock) {
        this(ingestion, store, tokens, null, availableTtl, 100, 1024L * 1024 * 1024, frontendUrl, clock);
    }

    StagingUploadService(ZipIngestionService ingestion, StagingImportPersistenceStore store,
                         StagingClaimTokenFactory tokens, UploadFileModeService fileModes, Duration availableTtl, String frontendUrl, Clock clock) {
        this(ingestion, store, tokens, fileModes, availableTtl, 100, 1024L * 1024 * 1024, frontendUrl, clock);
    }

    StagingUploadService(ZipIngestionService ingestion, StagingImportPersistenceStore store,
                         StagingClaimTokenFactory tokens, UploadFileModeService fileModes, Duration availableTtl,
                         long maximumLiveObjects, long maximumLiveBytes, String frontendUrl, Clock clock) {
        this.ingestion = Objects.requireNonNull(ingestion, "ingestion");
        this.store = Objects.requireNonNull(store, "store");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.fileModes = fileModes;
        if (availableTtl.isZero() || availableTtl.isNegative()) throw new IllegalArgumentException("availableTtl must be positive");
        if (maximumLiveObjects <= 0 || maximumLiveBytes <= 0) throw new IllegalArgumentException("staging capacity limits must be positive");
        this.availableTtl = availableTtl;
        this.maximumLiveObjects = maximumLiveObjects;
        this.maximumLiveBytes = maximumLiveBytes;
        this.frontendUrl = normalizeFrontendUrl(frontendUrl);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CreatedStagingUpload create(String originalFilename, long declaredContentLength, InputStream input) {
        if (declaredContentLength > maximumLiveBytes) throw new StagingCapacityExceededException("Staging capacity is temporarily full.");
        UUID stagingId = UUID.randomUUID();
        StoredUploadArtifact artifact = ingestion.store(stagingId, originalFilename, declaredContentLength, input);
        if (fileModes != null) {
            try { artifact = artifact.withFileModes(fileModes.inspect(artifact.storagePath())); }
            catch (IOException | info.isaksson.erland.zipgithub.archive.ArchiveSecurityException e) {
                deleteQuietly(artifact); throw new IllegalArgumentException("The uploaded ZIP failed secure archive inspection.", e);
            }
        }
        StagingClaimTokenFactory.ClaimToken claim = tokens.create();
        Instant createdAt = clock.instant();
        Instant expiresAt = createdAt.plus(availableTtl);
        StagingImport staging = new StagingImport(stagingId, artifact, claim.sha256(), createdAt, expiresAt);
        try {
            store.insertWithinLimits(staging, maximumLiveObjects, maximumLiveBytes);
        } catch (RuntimeException e) {
            deleteQuietly(artifact);
            throw e;
        }
        return new CreatedStagingUpload(stagingId, artifact.originalFilename(), artifact.sizeBytes(), artifact.sha256(),
                expiresAt, frontendUrl + "/staging/claim#token=" + claim.raw());
    }

    private static String normalizeFrontendUrl(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("frontendUrl is required");
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        return trimmed;
    }

    private static void deleteQuietly(StoredUploadArtifact artifact) {
        try { Files.deleteIfExists(artifact.storagePath()); } catch (IOException ignored) { }
    }

    public record CreatedStagingUpload(UUID stagingId, String originalFilename, long sizeBytes, String sha256,
                                       Instant expiresAt, String claimUrl) { }
}
