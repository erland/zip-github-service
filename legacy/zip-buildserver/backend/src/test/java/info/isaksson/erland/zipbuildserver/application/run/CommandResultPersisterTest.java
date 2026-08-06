package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationCommandResultRepository;
import info.isaksson.erland.zipbuildserver.storage.ArtifactStorageService;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CommandResultPersisterTest {
    private final RecordingCommandResultRepository commandRepository = new RecordingCommandResultRepository();
    private final RecordingArtifactStorageService artifactStorageService = new RecordingArtifactStorageService();
    private final CommandResultPersister persister = new CommandResultPersister(
            commandRepository,
            new LogExcerptService(),
            new FailureClassificationService(),
            new CommandArtifactService(artifactStorageService));

    @Test
    void persistsSuccessfulCommandResultWithOutputArtifacts() {
        UUID runId = UUID.randomUUID();
        VerificationCommand command = command("build");
        CommandExecutionResult result = CommandExecutionResult.passed(
                "build",
                Duration.ofMillis(123),
                "build stdout",
                "build stderr");

        persister.persistResult(runId, command, "app", result);

        VerificationCommandResultEntity entity = onlyPersistedCommand();

        assertEquals(runId, entity.runId);
        assertEquals("build", entity.commandLabel);
        assertEquals("app", entity.workingDirectory);
        assertEquals("mvn test", entity.commandDisplay);
        assertEquals(CheckStatus.PASSED, entity.status);
        assertEquals(0, entity.exitCode);
        assertEquals(123L, entity.durationMillis);
        assertEquals("build stdout\nbuild stderr", entity.logExcerpt);
        assertNull(entity.failureCategory);
        assertNull(entity.failureMessage);
        assertNotNull(entity.id);
        assertNotNull(entity.startedAt);
        assertNotNull(entity.completedAt);
        assertEquals(2, artifactStorageService.storedArtifacts.size());
        assertEquals(artifactStorageService.storedArtifacts.get(0).id, entity.stdoutArtifactRef);
        assertEquals(artifactStorageService.storedArtifacts.get(1).id, entity.stderrArtifactRef);
    }

    @Test
    void persistsFailedCommandResultWithFailureClassification() {
        UUID runId = UUID.randomUUID();
        VerificationCommand command = command("test");
        CommandExecutionResult result = CommandExecutionResult.failed(
                "test",
                1,
                Duration.ofSeconds(2),
                "assert failed",
                "",
                "Tests failed");

        persister.persistResult(runId, command, ".", result);

        VerificationCommandResultEntity entity = onlyPersistedCommand();

        assertEquals(CheckStatus.FAILED, entity.status);
        assertEquals(1, entity.exitCode);
        assertEquals(2_000L, entity.durationMillis);
        assertEquals("test", entity.failureCategory);
        assertEquals("Tests failed", entity.failureMessage);
        assertEquals(2, artifactStorageService.storedArtifacts.size());
    }

    @Test
    void persistsTimedOutCommandResultWithTimeoutFailureDetails() {
        UUID runId = UUID.randomUUID();
        VerificationCommand command = command("slow");
        CommandExecutionResult result = CommandExecutionResult.timedOut(
                "slow",
                Duration.ofSeconds(30),
                "partial stdout",
                "partial stderr");

        persister.persistResult(runId, command, "service", result);

        VerificationCommandResultEntity entity = onlyPersistedCommand();

        assertEquals(CheckStatus.TIMED_OUT, entity.status);
        assertEquals(-1, entity.exitCode);
        assertEquals(30_000L, entity.durationMillis);
        assertEquals("timeout", entity.failureCategory);
        assertEquals("Command timed out.", entity.failureMessage);
        assertEquals(2, artifactStorageService.storedArtifacts.size());
    }

    @Test
    void persistsSkippedCommandResultWithoutOutputArtifacts() {
        UUID runId = UUID.randomUUID();
        VerificationCommand command = command("integration");
        OffsetDateTime before = OffsetDateTime.now();

        persister.persistSkipped(runId, command, "integration-tests", "Skipped because an earlier command failed.");

        OffsetDateTime after = OffsetDateTime.now();
        VerificationCommandResultEntity entity = onlyPersistedCommand();

        assertEquals(runId, entity.runId);
        assertEquals("integration", entity.commandLabel);
        assertEquals("integration-tests", entity.workingDirectory);
        assertEquals("mvn test", entity.commandDisplay);
        assertEquals(CheckStatus.SKIPPED, entity.status);
        assertNull(entity.exitCode);
        assertEquals(0L, entity.durationMillis);
        assertEquals("", entity.logExcerpt);
        assertEquals("skipped", entity.failureCategory);
        assertEquals("Skipped because an earlier command failed.", entity.failureMessage);
        assertNull(entity.stdoutArtifactRef);
        assertNull(entity.stderrArtifactRef);
        assertEquals(0, artifactStorageService.storedArtifacts.size());
        assertNotNull(entity.startedAt);
        assertNotNull(entity.completedAt);
        assertEquals(entity.startedAt, entity.completedAt);
        // Keeps timestamps generated during persistence rather than copying stale command times.
        assertFalse(entity.startedAt.isBefore(before) || entity.completedAt.isAfter(after.plusSeconds(1)));
    }

    private VerificationCommandResultEntity onlyPersistedCommand() {
        assertEquals(1, commandRepository.persistedCommands.size());
        return commandRepository.persistedCommands.get(0);
    }

    private static VerificationCommand command(String label) {
        return new VerificationCommand(label, "${project.path}", "mvn test", 60, false);
    }

    private static final class RecordingCommandResultRepository extends VerificationCommandResultRepository {
        private final List<VerificationCommandResultEntity> persistedCommands = new ArrayList<>();

        @Override
        public void persist(VerificationCommandResultEntity entity) {
            persistedCommands.add(entity);
        }
    }

    private static final class RecordingArtifactStorageService extends ArtifactStorageService {
        private final List<StoredArtifact> storedArtifacts = new ArrayList<>();

        private RecordingArtifactStorageService() {
            super(null, ".", 14);
        }

        @Override
        public ArtifactReferenceEntity storeText(UUID runId, ArtifactType type, String commandLabel, String content) {
            ArtifactReferenceEntity artifact = new ArtifactReferenceEntity();
            artifact.id = UUID.randomUUID();
            artifact.runId = runId;
            artifact.type = type;
            artifact.sizeBytes = content == null ? 0 : content.length();

            storedArtifacts.add(new StoredArtifact(artifact.id, runId, type, commandLabel, content));
            return artifact;
        }
    }

    private record StoredArtifact(
            UUID id,
            UUID runId,
            ArtifactType type,
            String commandLabel,
            String content) {
    }
}
