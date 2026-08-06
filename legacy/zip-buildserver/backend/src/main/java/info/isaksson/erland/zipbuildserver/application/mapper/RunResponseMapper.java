package info.isaksson.erland.zipbuildserver.application.mapper;

import info.isaksson.erland.zipbuildserver.api.run.RunCommandResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunSummaryResponse;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationCommandResultEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationRunEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RunResponseMapper {
    public RunResponse toResponse(VerificationRunEntity entity, List<VerificationCommandResultEntity> commands) {
        return new RunResponse(
                entity.id,
                entity.sessionId,
                entity.sourcePackageId,
                entity.status,
                entity.planId,
                entity.requestedPlanId,
                entity.networkMode,
                entity.summary,
                entity.startedAt,
                entity.completedAt,
                entity.durationMillis,
                commands.stream().map(this::toCommandResponse).toList());
    }

    public RunSummaryResponse toSummaryResponse(VerificationRunEntity entity, List<VerificationCommandResultEntity> commands) {
        List<String> commandLabels = commands.stream()
                .map(command -> command.commandLabel)
                .toList();
        List<String> focusAreas = commands.stream()
                .filter(command -> command.status != CheckStatus.PASSED)
                .map(command -> command.failureMessage == null || command.failureMessage.isBlank()
                        ? command.commandLabel
                        : command.commandLabel + ": " + command.failureMessage)
                .toList();
        if (focusAreas.isEmpty()) {
            focusAreas = List.of("All fake verification commands completed successfully.");
        }
        return new RunSummaryResponse(
                entity.id,
                entity.status,
                entity.summary,
                entity.planId,
                commands.stream()
                        .filter(command -> command.status == CheckStatus.FAILED || command.status == CheckStatus.TIMED_OUT)
                        .map(command -> command.failureMessage)
                        .findFirst()
                        .orElse(null),
                commandLabels,
                focusAreas,
                false);
    }

    private RunCommandResponse toCommandResponse(VerificationCommandResultEntity entity) {
        return new RunCommandResponse(
                entity.id,
                entity.commandLabel,
                entity.workingDirectory,
                entity.commandDisplay,
                entity.status,
                entity.exitCode,
                entity.startedAt,
                entity.completedAt,
                entity.durationMillis,
                entity.logExcerpt,
                entity.failureCategory,
                entity.failureMessage,
                entity.stdoutArtifactRef,
                entity.stderrArtifactRef);
    }
}
