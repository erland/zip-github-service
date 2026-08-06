package info.isaksson.erland.zipbuildserver.application.retention;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;
import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;
import info.isaksson.erland.zipbuildserver.domain.model.SourcePackageStatus;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationSessionEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.ArtifactReferenceRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.SourcePackageRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationRunRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationSessionRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(RetentionCleanupServiceTest.RetentionProfile.class)
class RetentionCleanupServiceTest {
    @Inject
    RetentionCleanupService cleanupService;

    @Inject
    VerificationSessionRepository sessionRepository;

    @Inject
    SourcePackageRepository packageRepository;

    @Inject
    VerificationRunRepository runRepository;

    @Inject
    ArtifactReferenceRepository artifactRepository;

    @Test
    @TestTransaction
    void cleanupDeletesExpiredArtifactsAndPackageFilesButPreservesMetadata() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        VerificationSessionEntity session = newSession(now);
        sessionRepository.persist(session);

        Path packageFile = Files.createTempFile("zip-buildserver-package", ".zip");
        SourcePackageEntity sourcePackage = newSourcePackage(session.id, packageFile, now.minusDays(8));
        packageRepository.persist(sourcePackage);

        VerificationRunEntity run = newRun(session.id, sourcePackage.id);
        runRepository.persist(run);

        Path artifactFile = Files.createTempFile("zip-buildserver-artifact", ".log");
        ArtifactReferenceEntity artifact = newArtifact(run.id, artifactFile, now.minusDays(1));
        artifactRepository.persist(artifact);

        RetentionCleanupSummary summary = cleanupService.runCleanup();

        assertTrue(summary.packageFilesDeleted() >= 1);
        assertTrue(summary.artifactsDeleted() >= 1);
        assertFalse(Files.exists(packageFile));
        assertFalse(Files.exists(artifactFile));
        assertTrue(packageRepository.findById(sourcePackage.id).storageReference.startsWith("deleted:"));
        assertFalse(artifactRepository.findByIdOptional(artifact.id).isPresent());
    }

    private VerificationSessionEntity newSession(OffsetDateTime now) {
        VerificationSessionEntity session = new VerificationSessionEntity();
        session.id = UUID.randomUUID();
        session.label = "retention-test";
        session.status = SessionStatus.OPEN;
        session.createdAt = now.minusDays(10);
        session.createdBy = "test";
        session.retentionPolicy = "default";
        return session;
    }

    private SourcePackageEntity newSourcePackage(UUID sessionId, Path packageFile, OffsetDateTime createdAt) {
        SourcePackageEntity sourcePackage = new SourcePackageEntity();
        sourcePackage.id = UUID.randomUUID();
        sourcePackage.sessionId = sessionId;
        sourcePackage.originalFilename = "source.zip";
        sourcePackage.checksumSha256 = "0".repeat(64);
        sourcePackage.compressedSizeBytes = 1;
        sourcePackage.extractedSizeBytes = 1L;
        sourcePackage.fileCount = 1;
        sourcePackage.topLevelEntries = "README.md";
        sourcePackage.storageReference = packageFile.toString();
        sourcePackage.status = SourcePackageStatus.ACCEPTED;
        sourcePackage.createdAt = createdAt;
        return sourcePackage;
    }

    private VerificationRunEntity newRun(UUID sessionId, UUID packageId) {
        VerificationRunEntity run = new VerificationRunEntity();
        run.id = UUID.randomUUID();
        run.sessionId = sessionId;
        run.sourcePackageId = packageId;
        run.status = RunStatus.PASSED;
        run.planId = "node-default";
        run.networkMode = NetworkMode.DEPENDENCY;
        return run;
    }

    private ArtifactReferenceEntity newArtifact(UUID runId, Path artifactFile, OffsetDateTime expiresAt) {
        ArtifactReferenceEntity artifact = new ArtifactReferenceEntity();
        artifact.id = UUID.randomUUID();
        artifact.runId = runId;
        artifact.type = ArtifactType.STDOUT;
        artifact.storageReference = artifactFile.toString();
        artifact.sizeBytes = 1;
        artifact.createdAt = expiresAt.minusDays(1);
        artifact.expiresAt = expiresAt;
        return artifact;
    }

    public static class RetentionProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "zip-buildserver.retention.cleanup-enabled", "false",
                    "zip-buildserver.packages.retention-days", "7",
                    "zip-buildserver.sessions.retention-days", "90",
                    "zip-buildserver.workspaces.cleanup-grace-minutes", "0"
            );
        }
    }
}
