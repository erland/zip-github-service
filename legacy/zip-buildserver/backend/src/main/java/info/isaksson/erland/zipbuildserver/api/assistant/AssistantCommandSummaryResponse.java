package info.isaksson.erland.zipbuildserver.api.assistant;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;

public record AssistantCommandSummaryResponse(
        String label,
        String command,
        String workingDirectory,
        CheckStatus status,
        Integer exitCode,
        String failureCategory,
        String failureMessage,
        String logExcerpt) {
}
