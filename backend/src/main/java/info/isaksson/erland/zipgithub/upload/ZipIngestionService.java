package info.isaksson.erland.zipgithub.upload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Source-neutral ZIP ingestion: validates metadata, streams bytes to controlled storage, enforces
 * the absolute compressed-size limit and calculates SHA-256 while writing.
 */
@ApplicationScoped
public class ZipIngestionService {
    private final UploadStorage storage;
    private final long maximumBytes;
    private final Duration retention;
    private final Clock clock;

    @Inject
    public ZipIngestionService(
            @ConfigProperty(name = "zipgithub.upload.storage-root") String storageRoot,
            @ConfigProperty(name = "zipgithub.upload.max-compressed-bytes") long maximumBytes,
            @ConfigProperty(name = "zipgithub.upload.retention-hours") long retentionHours) {
        this(new UploadStorage(Path.of(storageRoot)), maximumBytes, Duration.ofHours(retentionHours), Clock.systemUTC());
    }

    ZipIngestionService(UploadStorage storage, long maximumBytes, Duration retention, Clock clock) {
        if (maximumBytes <= 0) throw new IllegalArgumentException("maximumBytes must be positive");
        if (retention.isZero() || retention.isNegative()) throw new IllegalArgumentException("retention must be positive");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.maximumBytes = maximumBytes;
        this.retention = Objects.requireNonNull(retention, "retention");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Stores one ZIP in an opaque storage scope. The scope is storage-only and carries no identity
     * or authorization meaning; normal imports currently use their import UUID as the scope.
     */
    public StoredUploadArtifact store(UUID storageScopeId, String originalFilename,
                                      long declaredContentLength, InputStream input) {
        Objects.requireNonNull(storageScopeId, "storageScopeId");
        Objects.requireNonNull(input, "input");
        String filename = normalizeFilename(originalFilename);
        if (declaredContentLength > maximumBytes) throw new UploadTooLargeException(maximumBytes);
        if (declaredContentLength == 0) throw new IllegalArgumentException("Upload must not be empty.");

        UUID uploadId = UUID.randomUUID();
        Instant createdAt = clock.instant();
        UploadStorage.PendingUpload pending = null;
        try {
            pending = storage.begin(storageScopeId, uploadId);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > maximumBytes) throw new UploadTooLargeException(maximumBytes);
                pending.output().write(buffer, 0, read);
                digest.update(buffer, 0, read);
            }
            if (total == 0) throw new IllegalArgumentException("Upload must not be empty.");
            pending.output().flush();
            Path completedPath = pending.complete();
            pending = null;
            return new StoredUploadArtifact(uploadId, filename, total,
                    HexFormat.of().formatHex(digest.digest()), completedPath,
                    createdAt, createdAt.plus(retention));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store upload.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        } finally {
            if (pending != null) pending.abort();
        }
    }

    static String normalizeFilename(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("X-Filename is required.");
        String normalized = value.trim();
        if (normalized.indexOf('\0') >= 0 || normalized.contains("/") || normalized.contains("\\"))
            throw new IllegalArgumentException("Filename must not contain a path.");
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".zip"))
            throw new IllegalArgumentException("Filename must end with .zip.");
        if (normalized.length() > 255) throw new IllegalArgumentException("Filename is too long.");
        return normalized;
    }
}
