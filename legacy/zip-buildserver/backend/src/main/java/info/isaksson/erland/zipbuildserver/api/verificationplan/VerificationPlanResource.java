package info.isaksson.erland.zipbuildserver.api.verificationplan;

import info.isaksson.erland.zipbuildserver.application.verification.VerificationPlanService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/verification-plans")
@Produces(MediaType.APPLICATION_JSON)
public class VerificationPlanResource {
    private final VerificationPlanService service;

    public VerificationPlanResource(VerificationPlanService service) {
        this.service = service;
    }

    @GET
    public VerificationPlanResponse list() {
        return new VerificationPlanResponse(service.listPlans());
    }
}
