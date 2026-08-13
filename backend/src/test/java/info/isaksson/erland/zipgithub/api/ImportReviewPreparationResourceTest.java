package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlanEntry;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.security.WebSessionStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class ImportReviewPreparationResourceTest {
    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PLAN_DIGEST = "a".repeat(64);
    private static final String BASE = "1".repeat(40);

    @Inject WebSessionStore sessions;
    @Inject ProjectApplicationService service;
    @InjectMock GitHubProjectCatalog catalog;

    private String cookie;
    private UUID importId;

    @BeforeEach
    void setUp() {
        service.clearInMemoryStateForTests();
        var installation = new GitHubAppClient.GitHubInstallation(10L, 1L, "erland", "User", "selected", null, "write");
        var repository = new GitHubAppClient.GitHubRepository(20L, "erland/example", true, "main", "https://github.com/erland/example");
        when(catalog.listUserInstallations(anyString())).thenReturn(List.of(installation));
        when(catalog.listUserInstallationRepositories(anyString(), eq(10L))).thenReturn(List.of(repository));
        when(catalog.branchExists(anyString(), eq("erland/example"), eq("main"))).thenReturn(true);

        cookie = sessions.createSession(USER, 1L, "erland", null, "Erland", "1+erland@users.noreply.github.com", "token");
        String projectId = given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie).contentType(ContentType.JSON)
                .body("{\"name\":\"Project\",\"githubInstallationId\":10,\"githubRepositoryId\":20,\"defaultBranch\":\"main\"}")
                .post("/api/projects").then().statusCode(201).extract().path("id");
        importId = UUID.fromString(given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie).contentType(ContentType.JSON)
                .body("{}").post("/api/projects/{projectId}/imports", projectId).then().statusCode(201).extract().path("id"));
        service.recordImportPlan(USER, importId, plan(importId));
    }

    @Test
    void retryReturnsTheAlreadyLockedImmutablePlan() {
        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .post("/api/imports/{id}/prepare-review", importId).then().statusCode(200)
                .body("planDigestSha256", equalTo(PLAN_DIGEST))
                .body("baseCommitSha", equalTo(BASE));

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .post("/api/imports/{id}/prepare-review", importId).then().statusCode(200)
                .body("planDigestSha256", equalTo(PLAN_DIGEST))
                .body("baseCommitSha", equalTo(BASE));
    }

    private static ImmutableImportPlan plan(UUID importId) {
        return new ImmutableImportPlan(UUID.randomUUID(), importId, USER, "f".repeat(64), BASE,
                "mvp-3", PLAN_DIGEST, "READY", true, List.of(
                new ImmutableImportPlanEntry("src/App.java", "MODIFIED", "MODIFIED", "NONE", "NONE",
                        null, null, 1L, "c".repeat(64), 1L, "d".repeat(64), true)
        ), Instant.now());
    }
}
