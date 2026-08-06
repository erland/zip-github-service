package info.isaksson.erland.zipbuildserver.api.assistant;

import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AssistantSessionResponse(
        UUID sessionId,
        SessionStatus status,
        String label,
        String retentionPolicy,
        OffsetDateTime createdAt) {
}
