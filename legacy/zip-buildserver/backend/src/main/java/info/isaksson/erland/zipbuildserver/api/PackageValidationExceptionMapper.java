package info.isaksson.erland.zipbuildserver.api;

import info.isaksson.erland.zipbuildserver.application.PackageValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PackageValidationExceptionMapper implements ExceptionMapper<PackageValidationException> {
    @Override
    public Response toResponse(PackageValidationException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.of("package_validation_failed", exception.getMessage()))
                .build();
    }
}
