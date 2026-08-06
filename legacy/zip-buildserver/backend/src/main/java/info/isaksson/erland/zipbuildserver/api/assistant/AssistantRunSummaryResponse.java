package info.isaksson.erland.zipbuildserver.api.assistant;

import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import java.util.List;
import java.util.UUID;

public record AssistantRunSummaryResponse(
        UUID runId,
        RunStatus status,
        String summary,
        String primaryFailure,
        List<String> failedFiles,
        List<String> failedTests,
        List<String> commandsRun,
        List<AssistantCommandSummaryResponse> failedChecks,
        List<String> suggestedFocus,
        String fullLogReference,
        boolean partial) {
}
