package info.isaksson.erland.zipbuildserver.api.run;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.restassured.builder.MultiPartSpecBuilder;

import info.isaksson.erland.zipbuildserver.worker.CommandExecutionResult;
import info.isaksson.erland.zipbuildserver.worker.fake.FakeCommandExecutor;
import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RunResourceTest {
    @Inject
    FakeCommandExecutor fakeCommandExecutor;

    @BeforeEach
    void resetFakeExecutor() {
        fakeCommandExecutor.reset();
    }

    @Test
    void createsRunAndExecutesFakeVerification() throws IOException {
        String sessionId = given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .extract().path("id");

        Path zip = createNodeZip();

        String packageId = given()
                .multiPart(new MultiPartSpecBuilder(Files.readAllBytes(zip))
                        .controlName("file")
                        .fileName("node-project.zip")
                        .mimeType("application/zip")
                        .build())
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("ACCEPTED"))
                .extract().path("id");

        String runId = given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("sessionId", equalTo(sessionId))
                .body("sourcePackageId", equalTo(packageId))
                .body("status", equalTo("PASSED"))
                .body("planId", equalTo("node-default"))
                .body("requestedPlanId", equalTo(null))
                .body("networkMode", equalTo("DEPENDENCY"))
                .body("summary", equalTo("Verification passed. 3 approved command(s) completed."))
                .body("startedAt", notNullValue())
                .body("completedAt", notNullValue())
                .body("durationMillis", greaterThanOrEqualTo(0))
                .body("commands", hasSize(3))
                .body("commands[0].id", notNullValue())
                .body("commands[0].commandLabel", equalTo("Install dependencies"))
                .body("commands[0].workingDirectory", equalTo("."))
                .body("commands[0].commandDisplay", equalTo("npm ci"))
                .body("commands[0].status", equalTo("PASSED"))
                .body("commands[0].exitCode", equalTo(0))
                .body("commands[0].startedAt", notNullValue())
                .body("commands[0].completedAt", notNullValue())
                .body("commands[0].durationMillis", greaterThanOrEqualTo(0))
                .body("commands[0].logExcerpt", notNullValue())
                .body("commands[0].failureCategory", equalTo(null))
                .body("commands[0].failureMessage", equalTo(null))
                .body("commands[0].stdoutArtifactRef", notNullValue())
                .body("commands[0].stderrArtifactRef", notNullValue())
                .extract().path("id");

        String artifactId = given()
                .when().get("/api/runs/{runId}/artifacts", runId)
                .then()
                .statusCode(200)
                .body("artifacts", hasSize(6))
                .body("artifacts[0].id", notNullValue())
                .body("artifacts[0].sizeBytes", greaterThanOrEqualTo(0))
                .extract().path("artifacts[0].id");

        given()
                .when().get("/api/artifacts/{artifactId}", artifactId)
                .then()
                .statusCode(200)
                .body("id", equalTo(artifactId))
                .body("runId", equalTo(runId))
                .body("content", notNullValue());

        given()
                .when().get("/api/runs/{runId}/summary", runId)
                .then()
                .statusCode(200)
                .body("runId", equalTo(runId))
                .body("status", equalTo("PASSED"))
                .body("summary", equalTo("Verification passed. 3 approved command(s) completed."))
                .body("planId", equalTo("node-default"))
                .body("primaryFailure", equalTo(null))
                .body("commandsRun", hasSize(3))
                .body("suggestedFocus", hasSize(1))
                .body("partial", equalTo(false));
    }


    @Test
    void fakeFailurePersistsCommandResultsAndSummary() throws IOException {
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
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .extract().path("id");

        Path zip = createNodeZip();

        String packageId = given()
                .multiPart(new MultiPartSpecBuilder(Files.readAllBytes(zip))
                        .controlName("file")
                        .fileName("node-project.zip")
                        .mimeType("application/zip")
                        .build())
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("ACCEPTED"))
                .extract().path("id");

        String runId = given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("FAILED"))
                .body("summary", equalTo("Verification failed. Review command-level failure details."))
                .body("commands", hasSize(3))
                .body("commands[0].status", equalTo("FAILED"))
                .body("commands[0].exitCode", equalTo(1))
                .body("commands[0].failureCategory", equalTo("dependency"))
                .body("commands[0].failureMessage", equalTo("Dependency installation failed."))
                .body("commands[0].logExcerpt", equalTo("npm ERR dependency resolution failed"))
                .body("commands[1].status", equalTo("SKIPPED"))
                .body("commands[1].exitCode", equalTo(null))
                .body("commands[1].failureCategory", equalTo("skipped"))
                .body("commands[1].failureMessage", equalTo("Skipped because an earlier command failed."))
                .extract().path("id");

        given()
                .when().get("/api/runs/{runId}/summary", runId)
                .then()
                .statusCode(200)
                .body("runId", equalTo(runId))
                .body("status", equalTo("FAILED"))
                .body("summary", equalTo("Verification failed. Review command-level failure details."))
                .body("planId", equalTo("node-default"))
                .body("primaryFailure", equalTo("Dependency installation failed."))
                .body("commandsRun", hasSize(3))
                .body("suggestedFocus", hasSize(3))
                .body("partial", equalTo(false));
    }

    @Test
    void rejectedPackageCannotCreateRun() throws IOException {
        String sessionId = given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .extract().path("id");

        Path zip = createUnsafeZip();

        String packageId = given()
                .multiPart(new MultiPartSpecBuilder(Files.readAllBytes(zip))
                        .controlName("file")
                        .fileName("unsafe.zip")
                        .mimeType("application/zip")
                        .build())
                .when().post("/api/sessions/{sessionId}/packages", sessionId)
                .then()
                .statusCode(201)
                .body("status", equalTo("REJECTED"))
                .extract().path("id");

        given()
                .contentType("application/json")
                .body("""
                        {
                          "packageId": "%s"
                        }
                        """.formatted(packageId))
                .when().post("/api/sessions/{sessionId}/runs", sessionId)
                .then()
                .statusCode(400);
    }

    private static Path createNodeZip() throws IOException {
        Path zip = Files.createTempFile("node-project", ".zip");
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

    private static Path createUnsafeZip() throws IOException {
        Path zip = Files.createTempFile("unsafe", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("../escape.txt"));
            output.write("escape".getBytes());
            output.closeEntry();
        }
        return zip;
    }
}
