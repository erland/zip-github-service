package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.staging.StagingUploadCredential;
import info.isaksson.erland.zipgithub.staging.StagingUploadService;
import info.isaksson.erland.zipgithub.staging.StagingCapacityExceededException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class StagingImportResourceTest {
    @InjectMock StagingUploadCredential credential;
    @InjectMock StagingUploadService uploads;

    @Test
    void rejectsMissingCapabilityWithoutTouchingStorage() {
        when(credential.accepts(null)).thenReturn(false);
        given().contentType("application/zip").header("X-Filename", "project.zip").body(zipBody())
                .when().post("/api/staging-imports")
                .then().statusCode(403).body("code", equalTo("STAGING_SHORTCUT_OUTDATED"));
        verifyNoInteractions(uploads);
    }

    @Test
    void capabilityCreatesOnlyOpaqueStagingMetadata() {
        UUID id = UUID.randomUUID();
        when(credential.accepts("capability")).thenReturn(true);
        when(uploads.create(eq("project.zip"), anyLong(), any())).thenReturn(new StagingUploadService.CreatedStagingUpload(
                id, "project.zip", 5, "a".repeat(64), Instant.parse("2026-08-08T06:00:00Z"),
                "https://zip-github.example/staging/claim#token=opaque"));

        given().contentType("application/zip")
                .header(StagingImportResource.CREDENTIAL_HEADER, "capability")
                .header("X-Filename", "project.zip")
                .body(zipBody())
                .when().post("/api/staging-imports")
                .then().statusCode(201)
                .body("stagingId", equalTo(id.toString()))
                .body("claimUrl", org.hamcrest.Matchers.endsWith("#token=opaque"))
                .body("ownerUserId", nullValue())
                .body("projectId", nullValue());
    }

    @Test
    void oversizedStagingZipKeepsOrdinaryArchiveLimitContract() {
        when(credential.accepts("capability")).thenReturn(true);
        when(uploads.create(eq("huge.zip"), anyLong(), any())).thenThrow(new info.isaksson.erland.zipgithub.upload.UploadTooLargeException(100));

        given().contentType("application/zip")
                .header(StagingImportResource.CREDENTIAL_HEADER, "capability")
                .header("X-Filename", "huge.zip")
                .body(zipBody())
                .when().post("/api/staging-imports")
                .then().statusCode(413).body("code", equalTo("UPLOAD_TOO_LARGE"));
    }

    @Test
    void fullStagingCapacityReturnsRetryable429WithoutGitHubSideEffects() {
        when(credential.accepts("capability")).thenReturn(true);
        when(uploads.create(eq("project.zip"), anyLong(), any())).thenThrow(new StagingCapacityExceededException("full"));

        given().contentType("application/zip")
                .header(StagingImportResource.CREDENTIAL_HEADER, "capability")
                .header("X-Filename", "project.zip")
                .body(zipBody())
                .when().post("/api/staging-imports")
                .then().statusCode(429).body("code", equalTo("STAGING_CAPACITY_EXCEEDED"));
    }
    private static java.io.InputStream zipBody() {
        return new java.io.ByteArrayInputStream("bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

}

