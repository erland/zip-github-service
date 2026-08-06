package info.isaksson.erland.zipbuildserver.api.assistant;

import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import java.util.UUID;

public record AssistantRunResponse(
        UUID runId,
        UUID sessionId,
        UUID packageId,
        RunStatus status,
        String summary,
        String planId,
        AssistantRunSummaryResponse structuredSummary) {
}
