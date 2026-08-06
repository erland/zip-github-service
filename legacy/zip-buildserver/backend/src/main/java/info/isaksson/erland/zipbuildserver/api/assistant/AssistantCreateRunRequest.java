package info.isaksson.erland.zipbuildserver.api.assistant;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AssistantCreateRunRequest(
        UUID packageId,
        @Size(max = 128) String requestedPlanId) {
}
