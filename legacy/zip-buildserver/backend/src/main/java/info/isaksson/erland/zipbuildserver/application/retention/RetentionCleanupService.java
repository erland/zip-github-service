package info.isaksson.erland.zipbuildserver.application.retention;

import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.AuditEventEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationSessionEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.ArtifactReferenceRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.AuditEventRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.SourcePackageRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationSessionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class RetentionCleanupService {
    private static final String DELETED_STORAGE_PREFIX = "deleted:";

    private final ArtifactReferenceRepository artifactRepository;
    private final SourcePackageRepository packageRepository;
    private final VerificationSessionRepository sessionRepository;
    private final AuditEventRepository auditEventRepository;
    private final Path workspacesDir;
    private final int packageRetentionDays;
    private final int sessionMetadataRetentionDays;
    private final int workspaceGraceMinutes;
    private final Clock clock;

    @Inject
    public RetentionCleanupService(
            ArtifactReferenceRepository artifactRepository,
            SourcePackageRepository packageRepository,
            VerificationSessionRepository sessionRepository,
            AuditEventRepository auditEventRepository,
            @ConfigProperty(name = "zip-buildserver.storage.workspaces-dir") String workspacesDir,
            @ConfigProperty(name = "zip-buildserver.packages.retention-days", defaultValue = "7") int packageRetentionDays,
            @ConfigProperty(name = "zip-buildserver.sessions.retention-days", defaultValue = "90") int sessionMetadataRetentionDays,
            @ConfigProperty(name = "zip-buildserver.workspaces.cleanup-grace-minutes", defaultValue = "60") int workspaceGraceMinutes) {
        this(
                artifactRepository,
                packageRepository,
                sessionRepository,
                auditEventRepository,
                Path.of(workspacesDir),
                packageRetentionDays,
                sessionMetadataRetentionDays,
                workspaceGraceMinutes,
                Clock.systemUTC());
    }

    RetentionCleanupService(
            ArtifactReferenceRepository artifactRepository,
            SourcePackageRepository packageRepository,
            VerificationSessionRepository sessionRepository,
            AuditEventRepository auditEventRepository,
            Path workspacesDir,
            int packageRetentionDays,
            int sessionMetadataRetentionDays,
            int workspaceGraceMinutes,
            Clock clock) {
        this.artifactRepository = artifactRepository;
        this.packageRepository = packageRepository;
        this.sessionRepository = sessionRepository;
        this.auditEventRepository = auditEventRepository;
        this.workspacesDir = workspacesDir;
        this.packageRetentionDays = packageRetentionDays;
        this.sessionMetadataRetentionDays = sessionMetadataRetentionDays;
        this.workspaceGraceMinutes = workspaceGraceMinutes;
        this.clock = clock;
    }

    @Transactional
    public RetentionCleanupSummary runCleanup() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        int artifactsDeleted = deleteExpiredArtifacts(now);
        int packageFilesDeleted = deleteExpiredPackageFiles(now);
        int workspacesDeleted = deleteExpiredWorkspaces(now.toInstant());
        int sessionsDeleted = deleteExpiredClosedSessions(now);
        auditCleanup(now, packageFilesDeleted, artifactsDeleted, workspacesDeleted, sessionsDeleted);
        return new RetentionCleanupSummary(packageFilesDeleted, artifactsDeleted, workspacesDeleted, sessionsDeleted);
    }

    int deleteExpiredArtifacts(OffsetDateTime now) {
        List<ArtifactReferenceEntity> expiredArtifacts = artifactRepository.list(
                "expiresAt is not null and expiresAt <= ?1",
                now);
        int deleted = 0;
        for (ArtifactReferenceEntity artifact : expiredArtifacts) {
            deleteFileIfPresent(artifact.storageReference);
            artifactRepository.delete(artifact);
            deleted++;
        }
        return deleted;
    }

    int deleteExpiredPackageFiles(OffsetDateTime now) {
        OffsetDateTime cutoff = now.minusDays(packageRetentionDays);
        List<SourcePackageEntity> expiredPackages = packageRepository.list(
                "createdAt <= ?1 and storageReference not like ?2",
                cutoff,
                DELETED_STORAGE_PREFIX + "%");
        int deleted = 0;
        for (SourcePackageEntity sourcePackage : expiredPackages) {
            boolean removed = deleteFileIfPresent(sourcePackage.storageReference);
            sourcePackage.storageReference = DELETED_STORAGE_PREFIX + sourcePackage.id;
            if (removed) {
                deleted++;
            }
        }
        return deleted;
    }

    int deleteExpiredWorkspaces(Instant now) {
        if (!Files.isDirectory(workspacesDir)) {
            return 0;
        }

        Instant cutoff = now.minusSeconds(Math.max(0, workspaceGraceMinutes) * 60L);
        int deleted = 0;
        try (Stream<Path> stream = Files.list(workspacesDir)) {
            for (Path workspace : stream.toList()) {
                if (!Files.isDirectory(workspace)) {
                    continue;
                }
                Instant lastModified = Files.getLastModifiedTime(workspace).toInstant();
                if (lastModified.isAfter(cutoff)) {
                    continue;
                }
                deleteRecursively(workspace);
                deleted++;
            }
            return deleted;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to clean expired workspaces.", exception);
        }
    }

    int deleteExpiredClosedSessions(OffsetDateTime now) {
        OffsetDateTime cutoff = now.minusDays(sessionMetadataRetentionDays);
        List<VerificationSessionEntity> expiredSessions = sessionRepository.list(
                "closedAt is not null and closedAt <= ?1",
                cutoff);
        int deleted = 0;
        for (VerificationSessionEntity session : expiredSessions) {
            sessionRepository.delete(session);
            deleted++;
        }
        return deleted;
    }

    private boolean deleteFileIfPresent(String storageReference) {
        if (storageReference == null || storageReference.isBlank() || storageReference.startsWith(DELETED_STORAGE_PREFIX)) {
            return false;
        }
        try {
            return Files.deleteIfExists(Path.of(storageReference));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete retained file: " + storageReference, exception);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path current : paths) {
                Files.deleteIfExists(current);
            }
        }
    }

    private void auditCleanup(
            OffsetDateTime now,
            int packageFilesDeleted,
            int artifactsDeleted,
            int workspacesDeleted,
            int sessionsDeleted) {
        if (packageFilesDeleted == 0 && artifactsDeleted == 0 && workspacesDeleted == 0 && sessionsDeleted == 0) {
            return;
        }

        AuditEventEntity event = new AuditEventEntity();
        event.id = UUID.randomUUID();
        event.eventType = "retention.cleanup";
        event.actor = "system";
        event.resourceType = "retention";
        event.details = "packageFilesDeleted=" + packageFilesDeleted
                + ", artifactsDeleted=" + artifactsDeleted
                + ", workspacesDeleted=" + workspacesDeleted
                + ", sessionsDeleted=" + sessionsDeleted;
        event.createdAt = now;
        auditEventRepository.persist(event);
    }
}
