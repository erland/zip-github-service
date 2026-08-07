package info.isaksson.erland.zipgithub.upload;

import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;

/** Removes source packages after their retention deadline without exposing paths to clients. */
@ApplicationScoped
public class UploadRetentionService {
    private static final Logger LOG = Logger.getLogger(UploadRetentionService.class);

    @Inject ProjectApplicationService applicationService;
    private final Clock clock;

    public UploadRetentionService() {
        this(Clock.systemUTC());
    }

    UploadRetentionService(Clock clock) {
        this.clock = clock;
    }

    @Scheduled(every = "${zipgithub.upload.cleanup-interval:1h}", delayed = "30s")
    void scheduledCleanup() {
        cleanupExpired();
    }

    public CleanupResult cleanupExpired() {
        Instant now = clock.instant();
        int deleted = 0;
        int failed = 0;
        for (StoredUpload upload : applicationService.expiredUploads(now)) {
            try {
                Files.deleteIfExists(upload.storagePath());
                applicationService.removeExpiredUpload(upload.importId(), upload.id(), now);
                deleteEmptyParents(upload);
                deleted++;
            } catch (IOException | RuntimeException e) {
                failed++;
                LOG.warnf(e, "Could not remove expired upload %s", upload.id());
            }
        }
        return new CleanupResult(deleted, failed);
    }

    private static void deleteEmptyParents(StoredUpload upload) throws IOException {
        var importDirectory = upload.storagePath().getParent();
        if (importDirectory != null && Files.isDirectory(importDirectory)) {
            try (var entries = Files.list(importDirectory)) {
                if (entries.findAny().isEmpty()) Files.deleteIfExists(importDirectory);
            }
        }
    }

    public record CleanupResult(int deleted, int failed) { }
}
