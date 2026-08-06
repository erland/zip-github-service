package info.isaksson.erland.zipbuildserver.api.assistant;

import jakarta.validation.constraints.Size;

public record AssistantCreateSessionRequest(
        @Size(max = 255) String label,
        @Size(max = 64) String retentionPolicy) {
}
