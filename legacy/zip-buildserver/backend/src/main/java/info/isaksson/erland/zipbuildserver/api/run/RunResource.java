package info.isaksson.erland.zipbuildserver.api.run;

import info.isaksson.erland.zipbuildserver.application.run.VerificationRunService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/sessions/{sessionId}/runs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RunResource {
    private final VerificationRunService service;

    public RunResource(VerificationRunService service) {
        this.service = service;
    }

    @POST
    public RestResponse<RunResponse> create(
            @PathParam("sessionId") UUID sessionId,
            @Valid CreateRunRequest request) {
        RunResponse response = service.create(sessionId, request);
        return RestResponse.status(RestResponse.Status.CREATED, response);
    }

    @GET
    public RunListResponse listForSession(@PathParam("sessionId") UUID sessionId) {
        return new RunListResponse(service.listForSession(sessionId));
    }
}
