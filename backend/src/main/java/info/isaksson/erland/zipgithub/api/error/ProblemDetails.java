package info.isaksson.erland.zipgithub.api.error;

import java.time.Instant;

public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String code,
        String correlationId,
        Instant timestamp) {
}
