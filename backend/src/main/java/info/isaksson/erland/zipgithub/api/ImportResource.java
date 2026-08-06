package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.api.dto.SourceUploadResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.upload.StreamingUploadService;
import info.isaksson.erland.zipgithub.upload.UploadTooLargeException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Path("/api/imports")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {
    @Inject CurrentUserProvider currentUser;
    @Inject ProjectApplicationService service;
    @Inject StreamingUploadService uploads;

    @GET @Path("/{importId}")
    public ImportResponse get(@PathParam("importId") UUID importId) {
        return service.getImport(currentUser.requireUserId(), importId);
    }

    @PUT
    @Path("/{importId}/upload")
    @Consumes({"application/zip", MediaType.APPLICATION_OCTET_STREAM})
    public Response upload(@PathParam("importId") UUID importId,
                           @HeaderParam("X-Filename") String filename,
                           @HeaderParam("Content-Length") long contentLength,
                           InputStream input) {
        UUID ownerUserId = currentUser.requireUserId();
        service.requireOwnedImport(ownerUserId, importId);
        try {
            var stored = uploads.store(ownerUserId, importId, filename, contentLength, input);
            try {
                SourceUploadResponse response = service.recordUpload(ownerUserId, importId, stored);
                return Response.status(Response.Status.CREATED).entity(response).build();
            } catch (RuntimeException e) {
                try { Files.deleteIfExists(stored.storagePath()); } catch (IOException ignored) { }
                throw e;
            }
        } catch (UploadTooLargeException e) {
            throw ApiException.payloadTooLarge("UPLOAD_TOO_LARGE", e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_UPLOAD", e.getMessage());
        }
    }
}
