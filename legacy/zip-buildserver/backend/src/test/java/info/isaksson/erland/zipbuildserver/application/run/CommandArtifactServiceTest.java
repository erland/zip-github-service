package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import info.isaksson.erland.zipbuildserver.storage.ArtifactStorageService;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandArtifactServiceTest {
    private final RecordingArtifactStorageService storageService = new RecordingArtifactStorageService();
    private final CommandArtifactService service = new CommandArtifactService(storageService);

    @Test
    void storesStdoutAndStderrArtifactsAndAttachesReferences() {
        UUID runId = UUID.randomUUID();
        VerificationCommand command = new VerificationCommand("build", "${project.path}", "mvn test", 60, false);
        CommandExecutionResult result = CommandExecutionResult.passed(
                "build",
                Duration.ofMillis(125),
                "stdout text",
                "stderr text");
        VerificationCommandResultEntity commandResult = new VerificationCommandResultEntity();

        service.storeCommandArtifacts(runId, command, result, commandResult);

        assertEquals(2, storageService.storedArtifacts.size());

        StoredArtifact stdout = storageService.storedArtifacts.get(0);
        assertEquals(runId, stdout.runId);
        assertEquals(ArtifactType.STDOUT, stdout.type);
        assertEquals("build", stdout.commandLabel);
        assertEquals("stdout text", stdout.content);

        StoredArtifact stderr = storageService.storedArtifacts.get(1);
        assertEquals(runId, stderr.runId);
        assertEquals(ArtifactType.STDERR, stderr.type);
        assertEquals("build", stderr.commandLabel);
        assertEquals("stderr text", stderr.content);

        assertEquals(stdout.id, commandResult.stdoutArtifactRef);
        assertEquals(stderr.id, commandResult.stderrArtifactRef);
    }

    @Test
    void storesNormalizedEmptyOutputFromCommandExecutionResult() {
        UUID runId = UUID.randomUUID();
        VerificationCommand command = new VerificationCommand("test", ".", "npm test", 30, false);
        CommandExecutionResult result = new CommandExecutionResult(
                "test",
                CheckStatus.FAILED,
                1,
                Duration.ofSeconds(1),
                false,
                null,
                null,
                null);
        VerificationCommandResultEntity commandResult = new VerificationCommandResultEntity();

        service.storeCommandArtifacts(runId, command, result, commandResult);

        assertEquals("", storageService.storedArtifacts.get(0).content);
        assertEquals("", storageService.storedArtifacts.get(1).content);
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
