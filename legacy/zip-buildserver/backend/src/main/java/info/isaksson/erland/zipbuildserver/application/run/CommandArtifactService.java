package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.ArtifactReferenceEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import info.isaksson.erland.zipbuildserver.storage.ArtifactStorageService;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class CommandArtifactService {
    private final ArtifactStorageService artifactStorageService;

    public CommandArtifactService(ArtifactStorageService artifactStorageService) {
        this.artifactStorageService = artifactStorageService;
    }

    public void storeCommandArtifacts(
            UUID runId,
            VerificationCommand command,
            CommandExecutionResult result,
            VerificationCommandResultEntity commandResult) {
        ArtifactReferenceEntity stdoutArtifact = artifactStorageService.storeText(
                runId,
                ArtifactType.STDOUT,
                command.label(),
                result.stdout());
        ArtifactReferenceEntity stderrArtifact = artifactStorageService.storeText(
                runId,
                ArtifactType.STDERR,
                command.label(),
                result.stderr());

        commandResult.stdoutArtifactRef = stdoutArtifact.id;
        commandResult.stderrArtifactRef = stderrArtifact.id;
    }
}
