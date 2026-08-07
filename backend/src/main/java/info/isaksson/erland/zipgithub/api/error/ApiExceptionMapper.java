package info.isaksson.erland.zipgithub.api.error;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.UUID;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    public static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Response toResponse(ApiException exception) {
        String correlationId = UUID.randomUUID().toString();
        ProblemDetails problem = new ProblemDetails(
                "urn:zip-github:problem:" + exception.code().toLowerCase(),
                titleFor(exception.status()),
                exception.status(),
                exception.getMessage(),
                exception.code(),
                correlationId,
                Instant.now());
        return Response.status(exception.status())
                .type(PROBLEM_JSON)
                .header("X-Correlation-ID", correlationId)
                .entity(problem)
                .build();
    }

    private static String titleFor(int status) {
        return switch (status) {
            case 400 -> "Bad request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not found";
            case 409 -> "Conflict";
            default -> "Request failed";
        };
    }
}
