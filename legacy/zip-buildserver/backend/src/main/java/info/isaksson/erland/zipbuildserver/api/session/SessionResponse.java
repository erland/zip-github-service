package info.isaksson.erland.zipbuildserver.api.session;

import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String label,
        SessionStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime closedAt,
        String createdBy,
        String retentionPolicy) {
}
