package info.isaksson.erland.zipbuildserver.api.assistant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.builder.MultiPartSpecBuilder;

import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import info.isaksson.erland.zipbuildserver.worker.fake.FakeCommandExecutor;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AssistantVerificationResourceTest {
    @Inject
    FakeCommandExecutor fakeCommandExecutor;

    @BeforeEach
    void resetFakeExecutor() {
        fakeCommandExecutor.reset();
    }

    @Test
    void createsSessionAndReturnsCompactRunSummary() throws IOException {
        String sessionId = given()
                .contentType("application/json")
                .body("{\"label\":\"assistant smoke\"}")
                .when().post("/api/assistant/verification-sessions")
                .then()
                .statusCode(201)
                .body("sessionId", notNullValue())
                .body("status", equalTo("OPEN"))
                .body("label", equalTo("assistant smoke"))
                .body("retentionPolicy", equalTo("default"))
                .body("createdAt", notNullValue())
                .extract().path("sessionId");

        String packageId = given()
                .multiPart(new MultiPartSpecBuilder(Files.readAllBytes(createNodeZip()))
                        .controlName("file")
                        .fileName("node-project.zip")
                        .mimeType("application/zip")
                        .build())
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .extract().path("id");

        String runId = given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/assistant/verification-sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(201)
                .body("runId", notNullValue())
                .body("sessionId", equalTo(sessionId))
                .body("packageId", equalTo(packageId))
                .body("status", equalTo("PASSED"))
                .body("summary", equalTo("Verification passed. 3 approved command(s) completed."))
                .body("planId", equalTo("node-default"))
                .body("structuredSummary.runId", notNullValue())
                .body("structuredSummary.status", equalTo("PASSED"))
                .body("structuredSummary.summary", equalTo("Verification passed. 3 approved command(s) completed."))
                .body("structuredSummary.primaryFailure", equalTo(null))
                .body("structuredSummary.failedFiles", hasSize(0))
                .body("structuredSummary.failedTests", hasSize(0))
                .body("structuredSummary.commandsRun", hasSize(3))
                .body("structuredSummary.failedChecks", hasSize(0))
                .body("structuredSummary.suggestedFocus", hasSize(1))
                .body("structuredSummary.fullLogReference", notNullValue())
                .body("structuredSummary.partial", equalTo(false))
                .extract().path("runId");

        given()
                .when().get("/api/assistant/verification-runs/{runId}/summary", runId)
                .then()
                .statusCode(200)
                .body("runId", equalTo(runId))
                .body("status", equalTo("PASSED"))
                .body("summary", equalTo("Verification passed. 3 approved command(s) completed."))
                .body("primaryFailure", equalTo(null))
                .body("failedFiles", hasSize(0))
                .body("failedTests", hasSize(0))
                .body("commandsRun", hasSize(3))
                .body("failedChecks", hasSize(0))
                .body("suggestedFocus", hasSize(1))
                .body("fullLogReference", equalTo("/api/runs/%s/artifacts".formatted(runId)))
                .body("partial", equalTo(false));
    }

    @Test
    void returnsFailedLogExcerptsOnlyForFailedChecks() throws IOException {
        fakeCommandExecutor.returns(CommandExecutionResult.failed(
                "Install dependencies",
                1,
                Duration.ofMillis(25),
                "",
                "npm ERR dependency resolution failed",
                "Dependency installation failed."));

        String sessionId = given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/assistant/verification-sessions")
                .then()
                .statusCode(201)
                .extract().path("sessionId");

        String packageId = given()
                .multiPart(new MultiPartSpecBuilder(Files.readAllBytes(createNodeZip()))
                        .controlName("file")
                        .fileName("node-project.zip")
                        .mimeType("application/zip")
                        .build())
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .extract().path("id");

        String runId = given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/assistant/verification-sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("FAILED"))
                .body("summary", equalTo("Verification failed. Review command-level failure details."))
                .body("structuredSummary.status", equalTo("FAILED"))
                .body("structuredSummary.summary", equalTo("Verification failed. Review command-level failure details."))
                .body("structuredSummary.primaryFailure", equalTo("Dependency installation failed."))
                .body("structuredSummary.failedChecks", hasSize(1))
                .body("structuredSummary.failedChecks[0].label", equalTo("Install dependencies"))
                .body("structuredSummary.failedChecks[0].command", equalTo("npm ci"))
                .body("structuredSummary.failedChecks[0].workingDirectory", equalTo("."))
                .body("structuredSummary.failedChecks[0].status", equalTo("FAILED"))
                .body("structuredSummary.failedChecks[0].exitCode", equalTo(1))
                .body("structuredSummary.failedChecks[0].failureCategory", equalTo("dependency"))
                .body("structuredSummary.failedChecks[0].failureMessage", equalTo("Dependency installation failed."))
                .body("structuredSummary.failedChecks[0].logExcerpt", equalTo("npm ERR dependency resolution failed"))
                .extract().path("runId");

        given()
                .when().get("/api/assistant/verification-runs/{runId}/failed-log-excerpts", runId)
                .then()
                .statusCode(200)
                .body("runId", equalTo(runId))
                .body("failedLogExcerpts", hasSize(1))
                .body("failedLogExcerpts[0].label", equalTo("Install dependencies"))
                .body("failedLogExcerpts[0].command", equalTo("npm ci"))
                .body("failedLogExcerpts[0].workingDirectory", equalTo("."))
                .body("failedLogExcerpts[0].status", equalTo("FAILED"))
                .body("failedLogExcerpts[0].exitCode", equalTo(1))
                .body("failedLogExcerpts[0].failureCategory", equalTo("dependency"))
                .body("failedLogExcerpts[0].failureMessage", equalTo("Dependency installation failed."))
                .body("failedLogExcerpts[0].logExcerpt", equalTo("npm ERR dependency resolution failed"));
    }

    private static Path createNodeZip() throws IOException {
        Path zip = Files.createTempFile("assistant-node-project", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("package.json"));
            output.write("""
                    {
                      "scripts": {
                        "test": "echo test",
                        "build": "echo build"
                      }
                    }
                    """.getBytes());
            output.closeEntry();
        }
        return zip;
    }
}
