package info.isaksson.erland.zipbuildserver.api.packageupload;

import info.isaksson.erland.zipbuildserver.application.SourcePackageService;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/sessions/{sessionId}/packages")
@Produces(MediaType.APPLICATION_JSON)
public class PackageResource {
    private final SourcePackageService sourcePackageService;

    public PackageResource(SourcePackageService sourcePackageService) {
        this.sourcePackageService = sourcePackageService;
    }

    @POST
    @Blocking
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public RestResponse<PackageResponse> submit(
            @PathParam("sessionId") UUID sessionId,
            @RestForm("file") FileUpload file) {
        if (file == null) {
            throw new BadRequestException("Multipart field 'file' is required.");
        }
        PackageResponse response = sourcePackageService.submit(sessionId, file.uploadedFile(), file.fileName());
        return RestResponse.status(RestResponse.Status.CREATED, response);
    }
}
