package info.isaksson.erland.zipbuildserver.api.run;

import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RunResponse(
        UUID id,
        UUID sessionId,
        UUID sourcePackageId,
        RunStatus status,
        String planId,
        String requestedPlanId,
        NetworkMode networkMode,
        String summary,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Long durationMillis,
        List<RunCommandResponse> commands) {
}
