package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.ShortcutReleaseResponse;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.shortcut.ShortcutReleaseService;
import info.isaksson.erland.zipgithub.shortcut.ShortcutDownloadHeaders;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Authenticated distribution endpoint for the pre-signed reference Shortcut. */
@Path("/api/shortcut-release")
public class ShortcutReleaseResource {
    @Inject CurrentUserProvider currentUser;
    @Inject ShortcutReleaseService releases;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ShortcutReleaseResponse metadata() {
        currentUser.requireUserId();
        var value = releases.metadata();
        return new ShortcutReleaseResponse(value.available(), value.version(), value.generation(), value.filename(),
                value.sizeBytes(), value.sha256(), value.available() ? "/api/shortcut-release/download" : null);
    }

    @GET
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download() {
        currentUser.requireUserId();
        java.nio.file.Path artifact = releases.requireArtifact();
        return Response.ok(artifact.toFile(), MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ShortcutDownloadHeaders.contentDisposition())
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}
