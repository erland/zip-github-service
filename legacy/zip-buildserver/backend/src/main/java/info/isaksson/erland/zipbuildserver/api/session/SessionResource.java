package info.isaksson.erland.zipbuildserver.api.session;

import info.isaksson.erland.zipbuildserver.application.VerificationSessionService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/sessions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {
    private final VerificationSessionService service;

    public SessionResource(VerificationSessionService service) {
        this.service = service;
    }

    @POST
    public SessionResponse create(@Valid CreateSessionRequest request) {
        return service.create(request);
    }

    @GET
    public SessionListResponse list() {
        return new SessionListResponse(service.list());
    }

    @GET
    @Path("/{sessionId}")
    public SessionResponse get(@PathParam("sessionId") UUID sessionId) {
        return service.get(sessionId);
    }

    @POST
    @Path("/{sessionId}/close")
    @Consumes(MediaType.WILDCARD)
    public SessionResponse close(@PathParam("sessionId") UUID sessionId) {
        return service.close(sessionId);
    }
}
