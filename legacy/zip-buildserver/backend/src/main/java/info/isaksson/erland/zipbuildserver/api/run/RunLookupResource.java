package info.isaksson.erland.zipbuildserver.api.run;

import info.isaksson.erland.zipbuildserver.application.run.VerificationRunService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/runs")
@Produces(MediaType.APPLICATION_JSON)
public class RunLookupResource {
    private final VerificationRunService service;

    public RunLookupResource(VerificationRunService service) {
        this.service = service;
    }

    @GET
    @Path("/{runId}")
    public RunResponse get(@PathParam("runId") UUID runId) {
        return service.get(runId);
    }

    @GET
    @Path("/{runId}/summary")
    public RunSummaryResponse summary(@PathParam("runId") UUID runId) {
        return service.summary(runId);
    }
}
