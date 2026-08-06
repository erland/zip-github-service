package info.isaksson.erland.zipbuildserver.api.packageupload;

import info.isaksson.erland.zipbuildserver.application.SourcePackageService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

@Path("/api/packages")
@Produces(MediaType.APPLICATION_JSON)
public class PackageLookupResource {
    private final SourcePackageService sourcePackageService;

    public PackageLookupResource(SourcePackageService sourcePackageService) {
        this.sourcePackageService = sourcePackageService;
    }

    @GET
    @Path("/{packageId}")
    public PackageResponse get(@PathParam("packageId") UUID packageId) {
        return sourcePackageService.get(packageId);
    }
}
