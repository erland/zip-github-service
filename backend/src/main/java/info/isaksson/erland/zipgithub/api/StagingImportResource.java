package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.StagingClaimRequest;
import info.isaksson.erland.zipgithub.api.dto.StagingClaimResponse;
import info.isaksson.erland.zipgithub.api.dto.StagingUploadResponse;
import info.isaksson.erland.zipgithub.api.dto.StagingPromotionRequest;
import info.isaksson.erland.zipgithub.api.dto.StagingPromotionResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.staging.StagingClaimService;
import info.isaksson.erland.zipgithub.staging.StagingUploadCredential;
import info.isaksson.erland.zipgithub.staging.StagingUploadService;
import info.isaksson.erland.zipgithub.staging.StagingCapacityExceededException;
import info.isaksson.erland.zipgithub.staging.StagingPromotionService;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.upload.UploadTooLargeException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/** Narrow unauthenticated transport endpoint. The deployment credential grants staging-create only. */
@Path("/api/staging-imports")
@Produces(MediaType.APPLICATION_JSON)
public class StagingImportResource {
    public static final String CREDENTIAL_HEADER = "X-ZipGitHub-Upload-Credential";

    @Inject StagingUploadCredential credential;
    @Inject StagingUploadService uploads;
    @Inject StagingClaimService claims;
    @Inject StagingPromotionService promotions;
    @Inject ProjectApplicationService projects;
    @Inject info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore stagingStore;
    @Inject CurrentUserProvider currentUser;

    @POST
    @Path("/claim")
    @Consumes(MediaType.APPLICATION_JSON)
    public StagingClaimResponse claim(StagingClaimRequest request) {
        var claimed = claims.claim(request == null ? null : request.token(), currentUser.requireUserId());
        return new StagingClaimResponse(claimed.stagingId(), claimed.originalFilename(), claimed.sizeBytes(),
                claimed.sha256(), claimed.expiresAt(), claimed.claimedAt());
    }


    @GET
    @Path("/{stagingId}")
    public StagingClaimResponse getClaimed(@PathParam("stagingId") java.util.UUID stagingId) {
        var owner = currentUser.requireUserId();
        var item = stagingStore.findOwned(stagingId, owner)
                .orElseThrow(() -> ApiException.notFound("STAGING_IMPORT_NOT_FOUND", "The staging import was not found."));
        if (item.status() != info.isaksson.erland.zipgithub.domain.status.StagingImportStatus.CLAIMED
                && item.status() != info.isaksson.erland.zipgithub.domain.status.StagingImportStatus.PROMOTED)
            throw ApiException.notFound("STAGING_IMPORT_NOT_FOUND", "The staging import was not found.");
        return new StagingClaimResponse(item.id(), item.artifact().originalFilename(), item.artifact().sizeBytes(),
                item.artifact().sha256(), item.expiresAt(), item.claimedAt());
    }

    @POST
    @Path("/{stagingId}/promote")
    @Consumes(MediaType.APPLICATION_JSON)
    public StagingPromotionResponse promote(@PathParam("stagingId") java.util.UUID stagingId, StagingPromotionRequest request) {
        var session = currentUser.requireSession();
        java.util.UUID projectId = request == null ? null : request.projectId();
        if (projectId == null) {
            if (request == null || request.githubInstallationId() == null || request.githubRepositoryId() == null)
                throw info.isaksson.erland.zipgithub.api.error.ApiException.badRequest("STAGING_REPOSITORY_REQUIRED", "Choose a repository before continuing.");
            projectId = projects.ensureProjectForRepository(session.userId(), session.githubUserAccessToken(),
                    request.githubInstallationId(), request.githubRepositoryId()).id();
        }
        var promoted = promotions.promote(stagingId, session.userId(), projectId, session.gitName(), session.gitEmail());
        return new StagingPromotionResponse(stagingId, promoted.projectId(), promoted.importId(), "PROMOTED", promoted.alreadyPromoted());
    }

    @POST
    @Consumes({"application/zip", MediaType.APPLICATION_OCTET_STREAM})
    public Response create(@HeaderParam(CREDENTIAL_HEADER) String presentedCredential,
                           @HeaderParam("X-Filename") String filename,
                           @HeaderParam("Content-Length") Long contentLength,
                           InputStream input) {
        if (!credential.accepts(presentedCredential)) {
            throw ApiException.forbidden("STAGING_SHORTCUT_OUTDATED",
                    "This zip-github Shortcut is outdated or its upload credential has been revoked. Sign in to zip-github and install the current Shortcut release.");
        }
        try {
            var created = uploads.create(filename, contentLength == null ? -1 : contentLength, input);
            return Response.status(Response.Status.CREATED).entity(new StagingUploadResponse(
                    created.stagingId(), created.originalFilename(), created.sizeBytes(), created.sha256(),
                    created.expiresAt(), created.claimUrl())).build();
        } catch (StagingCapacityExceededException e) {
            throw ApiException.tooManyRequests("STAGING_CAPACITY_EXCEEDED", "Staging capacity is temporarily full. Retry after cleanup or install the latest Shortcut if the credential was rotated.");
        } catch (UploadTooLargeException e) {
            throw ApiException.payloadTooLarge("UPLOAD_TOO_LARGE", "The ZIP exceeds the configured upload limit.");
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_STAGING_UPLOAD", "The staging upload is not a valid ZIP upload request.");
        }
    }
}
