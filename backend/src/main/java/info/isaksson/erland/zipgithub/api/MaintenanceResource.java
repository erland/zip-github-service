package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.application.WorkBranchMaintenanceService;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/maintenance/work-branches")
@Produces(MediaType.APPLICATION_JSON)
public class MaintenanceResource {
    @Inject CurrentUserProvider currentUser;
    @Inject WorkBranchMaintenanceService maintenance;

    @GET
    public WorkBranchCleanupPreviewResponse preview() {
        var session = currentUser.requireSession();
        return maintenance.preview(session.userId(), session.githubUserAccessToken());
    }

    @POST
    @Path("/cleanup")
    @Consumes(MediaType.APPLICATION_JSON)
    public WorkBranchCleanupResultResponse cleanup(WorkBranchCleanupRequest request) {
        var session = currentUser.requireSession();
        return maintenance.cleanup(session.userId(), session.githubUserAccessToken(), request);
    }
}
