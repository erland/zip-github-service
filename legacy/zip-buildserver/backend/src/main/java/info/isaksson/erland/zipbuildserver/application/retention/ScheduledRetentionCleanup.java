package info.isaksson.erland.zipbuildserver.application.retention;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ScheduledRetentionCleanup {
    private final RetentionCleanupService cleanupService;
    private final boolean enabled;

    public ScheduledRetentionCleanup(
            RetentionCleanupService cleanupService,
            @ConfigProperty(name = "zip-buildserver.retention.cleanup-enabled", defaultValue = "true") boolean enabled) {
        this.cleanupService = cleanupService;
        this.enabled = enabled;
    }

    @Scheduled(every = "{zip-buildserver.retention.cleanup-interval}")
    void cleanupExpiredRetainedData() {
        if (enabled) {
            cleanupService.runCleanup();
        }
    }
}
