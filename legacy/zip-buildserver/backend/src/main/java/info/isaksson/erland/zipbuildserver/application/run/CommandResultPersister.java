package info.isaksson.erland.zipbuildserver.application.run;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.domain.model.verification.VerificationCommand;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationCommandResultRepository;
import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class CommandResultPersister {
    private final VerificationCommandResultRepository commandRepository;
    private final LogExcerptService logExcerptService;
    private final FailureClassificationService failureClassificationService;
    private final CommandArtifactService commandArtifactService;

    public CommandResultPersister(
            VerificationCommandResultRepository commandRepository,
            LogExcerptService logExcerptService,
            FailureClassificationService failureClassificationService,
            CommandArtifactService commandArtifactService) {
        this.commandRepository = commandRepository;
        this.logExcerptService = logExcerptService;
        this.failureClassificationService = failureClassificationService;
        this.commandArtifactService = commandArtifactService;
    }

    public void persistResult(UUID runId, VerificationCommand command, String workingDirectory, CommandExecutionResult result) {
        VerificationCommandResultEntity entity = new VerificationCommandResultEntity();
        OffsetDateTime started = OffsetDateTime.now().minus(result.duration());
        OffsetDateTime completed = OffsetDateTime.now();
        entity.id = UUID.randomUUID();
        entity.runId = runId;
        entity.commandLabel = command.label();
        entity.workingDirectory = workingDirectory;
        entity.commandDisplay = command.commandDisplay();
        entity.status = result.status();
        entity.exitCode = result.exitCode();
        entity.startedAt = started;
        entity.completedAt = completed;
        entity.durationMillis = result.duration().toMillis();
        entity.logExcerpt = logExcerptService.excerpt(result.stdout(), result.stderr());
        entity.failureCategory = result.status() == CheckStatus.PASSED ? null : failureClassificationService.category(result);
        entity.failureMessage = result.status() == CheckStatus.PASSED ? null : failureClassificationService.message(result);
        commandArtifactService.storeCommandArtifacts(runId, command, result, entity);
        commandRepository.persist(entity);
    }

    public void persistSkipped(UUID runId, VerificationCommand command, String workingDirectory, String reason) {
        VerificationCommandResultEntity entity = new VerificationCommandResultEntity();
        OffsetDateTime now = OffsetDateTime.now();
        entity.id = UUID.randomUUID();
        entity.runId = runId;
        entity.commandLabel = command.label();
        entity.workingDirectory = workingDirectory;
        entity.commandDisplay = command.commandDisplay();
        entity.status = CheckStatus.SKIPPED;
        entity.exitCode = null;
        entity.startedAt = now;
        entity.completedAt = now;
        entity.durationMillis = 0L;
        entity.logExcerpt = "";
        entity.failureCategory = "skipped";
        entity.failureMessage = reason;
        commandRepository.persist(entity);
    }
}
