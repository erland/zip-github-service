package info.isaksson.erland.zipbuildserver.api.session;

import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @Size(max = 255) String label,
        @Size(max = 64) String retentionPolicy) {
}
