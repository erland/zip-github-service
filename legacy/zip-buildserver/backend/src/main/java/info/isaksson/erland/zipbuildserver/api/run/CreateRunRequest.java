package info.isaksson.erland.zipbuildserver.api.run;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateRunRequest(
        UUID packageId,
        @Size(max = 128) String requestedPlanId) {
}
