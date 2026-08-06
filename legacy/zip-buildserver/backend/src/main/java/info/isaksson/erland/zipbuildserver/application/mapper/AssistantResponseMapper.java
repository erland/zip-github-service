package info.isaksson.erland.zipbuildserver.application.mapper;

import info.isaksson.erland.zipbuildserver.api.assistant.AssistantCommandSummaryResponse;
import info.isaksson.erland.zipbuildserver.api.assistant.AssistantFailedLogExcerptResponse;
import info.isaksson.erland.zipbuildserver.api.assistant.AssistantRunResponse;
import info.isaksson.erland.zipbuildserver.api.assistant.AssistantRunSummaryResponse;
import info.isaksson.erland.zipbuildserver.api.assistant.AssistantSessionResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunCommandResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunResponse;
import info.isaksson.erland.zipbuildserver.api.run.RunSummaryResponse;
import info.isaksson.erland.zipbuildserver.api.session.SessionResponse;
import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AssistantResponseMapper {
    public AssistantSessionResponse toAssistantSession(SessionResponse session) {
        return new AssistantSessionResponse(
                session.id(),
                session.status(),
                session.label(),
                session.retentionPolicy(),
                session.createdAt());
    }

    public AssistantRunResponse toAssistantRun(RunResponse run, RunSummaryResponse summary) {
        return new AssistantRunResponse(
                run.id(),
                run.sessionId(),
                run.sourcePackageId(),
                run.status(),
                run.summary(),
                run.planId(),
                toAssistantSummary(run, summary));
    }

    public AssistantRunSummaryResponse toAssistantSummary(RunResponse run, RunSummaryResponse summary) {
        List<AssistantCommandSummaryResponse> failedChecks = run.commands().stream()
                .filter(this::isFailedCommand)
                .map(this::toAssistantCommand)
                .toList();
        return new AssistantRunSummaryResponse(
                run.id(),
                run.status(),
                summary.summary(),
                summary.primaryFailure(),
                List.of(),
                List.of(),
                summary.commandsRun(),
                failedChecks,
                summary.suggestedFocus(),
                "/api/runs/%s/artifacts".formatted(run.id()),
                summary.partial());
    }

    public AssistantFailedLogExcerptResponse toFailedLogExcerpts(RunResponse run) {
        List<AssistantCommandSummaryResponse> failed = run.commands().stream()
                .filter(this::isFailedCommand)
                .map(this::toAssistantCommand)
                .toList();
        return new AssistantFailedLogExcerptResponse(run.id(), failed);
    }

    private boolean isFailedCommand(RunCommandResponse command) {
        return command.status() == CheckStatus.FAILED
                || command.status() == CheckStatus.TIMED_OUT
                || command.status() == CheckStatus.INTERNAL_ERROR;
    }

    private AssistantCommandSummaryResponse toAssistantCommand(RunCommandResponse command) {
        return new AssistantCommandSummaryResponse(
                command.commandLabel(),
                command.commandDisplay(),
                command.workingDirectory(),
                command.status(),
                command.exitCode(),
                command.failureCategory(),
                command.failureMessage(),
                command.logExcerpt());
    }
}
