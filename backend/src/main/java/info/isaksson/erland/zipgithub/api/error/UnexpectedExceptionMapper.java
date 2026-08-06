package info.isaksson.erland.zipgithub.api.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.UUID;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable exception) {
        String correlationId = UUID.randomUUID().toString();
        ProblemDetails problem = new ProblemDetails(
                "urn:zip-github:problem:internal_error", "Internal server error", 500,
                "The request could not be completed.", "INTERNAL_ERROR", correlationId, Instant.now());
        return Response.status(500).type(ApiExceptionMapper.PROBLEM_JSON)
                .header("X-Correlation-ID", correlationId).entity(problem).build();
    }
}
