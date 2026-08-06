package info.isaksson.erland.zipbuildserver.api;

import info.isaksson.erland.zipbuildserver.application.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorResponse.of("not_found", exception.getMessage()))
                .build();
    }
}
