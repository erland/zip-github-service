package info.isaksson.erland.zipbuildserver.api.assistant;

import java.util.List;
import java.util.UUID;

public record AssistantFailedLogExcerptResponse(
        UUID runId,
        List<AssistantCommandSummaryResponse> failedLogExcerpts) {
}
