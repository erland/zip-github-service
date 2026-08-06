package info.isaksson.erland.zipbuildserver.api.run;

import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import java.util.List;
import java.util.UUID;

public record RunSummaryResponse(
        UUID runId,
        RunStatus status,
        String summary,
        String planId,
        String primaryFailure,
        List<String> commandsRun,
        List<String> suggestedFocus,
        boolean partial) {
}
