package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;

/** Deterministically expires and removes short-lived staging artifacts without touching promoted uploads. */
@ApplicationScoped
public class StagingRetentionService {
    private static final Logger LOG = Logger.getLogger(StagingRetentionService.class);

    @Inject StagingImportPersistenceStore store;
    @ConfigProperty(name = "zipgithub.staging.cleanup-batch-size", defaultValue = "100") int batchSize;
    private final Clock clock;

    public StagingRetentionService() { this(Clock.systemUTC()); }
    StagingRetentionService(Clock clock) { this.clock = clock; }

    @Scheduled(every = "${zipgithub.staging.cleanup-interval:5m}", delayed = "45s")
    void scheduledCleanup() { cleanupExpired(); }

    public CleanupResult cleanupExpired() {
        Instant now = clock.instant();
        int deleted = 0, failed = 0;
        for (var candidate : store.claimCleanupCandidates(now, batchSize)) {
            try {
                Files.deleteIfExists(candidate.artifact().storagePath());
                deleteEmptyParent(candidate.artifact().storagePath());
                store.markArtifactDeleted(candidate.stagingId(), now);
                deleted++;
            } catch (IOException | RuntimeException e) {
                failed++;
                // The row remains terminal with artifact_deleted_at NULL, so the next cleanup run retries it.
                LOG.warnf(e, "Could not remove staging artifact %s", candidate.stagingId());
            }
        }
        return new CleanupResult(deleted, failed);
    }

    private static void deleteEmptyParent(java.nio.file.Path storagePath) throws IOException {
        var directory = storagePath.getParent();
        if (directory != null && Files.isDirectory(directory)) {
            try (var entries = Files.list(directory)) {
                if (entries.findAny().isEmpty()) Files.deleteIfExists(directory);
            }
        }
    }

    public record CleanupResult(int deleted, int failed) { }
}
