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
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class ImportSelectionResourceTest {
    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PLAN_DIGEST = "a".repeat(64);
    private static final String BASE = "1".repeat(40);

    @Inject WebSessionStore sessions;
    @Inject ProjectApplicationService service;
    @InjectMock GitHubProjectCatalog catalog;

    private String cookieA;
    private String cookieB;
    private UUID importId;

    @BeforeEach
    void setUp() {
        service.clearInMemoryStateForTests();
        var installation = new GitHubAppClient.GitHubInstallation(10L, 1L, "erland", "User", "selected", null);
        var repository = new GitHubAppClient.GitHubRepository(20L, "erland/example", true, "main", "https://github.com/erland/example");
        when(catalog.listUserInstallations(anyString())).thenReturn(List.of(installation));
        when(catalog.listUserInstallationRepositories(anyString(), eq(10L))).thenReturn(List.of(repository));
        when(catalog.branchExists(anyString(), eq("erland/example"), eq("main"))).thenReturn(true);

        cookieA = sessions.createSession(USER_A, 1L, "user-a", null, "User A", "1+user-a@users.noreply.github.com", "token-a");
        cookieB = sessions.createSession(USER_B, 2L, "user-b", null, "User B", "2+user-b@users.noreply.github.com", "token-b");
        String projectId = given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{\"name\":\"Novel\",\"githubInstallationId\":10,\"githubRepositoryId\":20,\"defaultBranch\":\"main\"}")
                .post("/api/projects").then().statusCode(201).extract().path("id");
        String id = given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{}").post("/api/projects/{projectId}/imports", projectId).then().statusCode(201).extract().path("id");
        importId = UUID.fromString(id);
        service.recordImportPlan(USER_A, importId, plan(importId));
    }

    @Test
    void createsReadsAndKeepsSelectionImmutable() {
        String body = """
                {"planDigestSha256":"%s","baseCommitSha":"%s","selectedPaths":["src/App.java"],"overrides":[],"blockerDecisions":[{"path":".git/config","decision":"ACKNOWLEDGE_EXCLUSION"},{"path":".github/workflows/ci.yml","decision":"EXCLUDE"}]}
                """.formatted(PLAN_DIGEST, BASE);

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON).body(body)
                .post("/api/imports/{id}/selection", importId).then().statusCode(201)
                .body("selectionVersion", equalTo("selection-2"))
                .body("selectedPaths", equalTo(List.of("src/App.java")))
                .body("excludedPaths", hasItems(".git/config", ".github/workflows/ci.yml"));

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA)
                .get("/api/imports/{id}/selection", importId).then().statusCode(200)
                .body("selectedPaths", equalTo(List.of("src/App.java")));

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{\"planDigestSha256\":\"%s\",\"baseCommitSha\":\"%s\",\"selectedPaths\":[\".github/workflows/ci.yml\"],\"overrides\":[{\"path\":\".github/workflows/ci.yml\",\"acknowledgement\":\"accepted\"}],\"blockerDecisions\":[{\"path\":\".git/config\",\"decision\":\"ACKNOWLEDGE_EXCLUSION\"},{\"path\":\".github/workflows/ci.yml\",\"decision\":\"INCLUDE_OVERRIDE\"}]}".formatted(PLAN_DIGEST, BASE))
                .post("/api/imports/{id}/selection", importId).then().statusCode(409)
                .body("code", equalTo("IMPORT_SELECTION_IMMUTABLE"));
    }

    @Test
    void readsRecordedApprovalForRecoveryAfterRefresh() {
        String selectionBody = """
                {"planDigestSha256":"%s","baseCommitSha":"%s","selectedPaths":["src/App.java"],"overrides":[],"blockerDecisions":[{"path":".git/config","decision":"ACKNOWLEDGE_EXCLUSION"},{"path":".github/workflows/ci.yml","decision":"EXCLUDE"}]}
                """.formatted(PLAN_DIGEST, BASE);
        String selectionDigest = given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body(selectionBody).post("/api/imports/{id}/selection", importId).then().statusCode(201)
                .extract().path("selectionDigestSha256");

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{\"planDigestSha256\":\"%s\",\"selectionDigestSha256\":\"%s\",\"commitMessage\":\"Refresh-safe test commit\"}".formatted(PLAN_DIGEST, selectionDigest))
                .post("/api/imports/{id}/plan/approval", importId).then().statusCode(200);

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA)
                .get("/api/imports/{id}/plan/approval", importId).then().statusCode(200)
                .body("planDigestSha256", equalTo(PLAN_DIGEST))
                .body("selectionDigestSha256", equalTo(selectionDigest))
                .body("commitMessage", equalTo("Refresh-safe test commit"))
                .body("status", equalTo("APPROVED"));
    }

    @Test
    void rejectsSelectionWhenBlockingEntriesHaveNotBeenExplicitlyDecided() {
        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{\"planDigestSha256\":\"%s\",\"baseCommitSha\":\"%s\",\"selectedPaths\":[\"src/App.java\"],\"overrides\":[],\"blockerDecisions\":[]}".formatted(PLAN_DIGEST, BASE))
                .post("/api/imports/{id}/selection", importId).then().statusCode(400)
                .body("code", equalTo("BLOCKER_DECISION_REQUIRED"));
    }

    @Test
    void rejectsStaleHardBlockedAndCrossUserRequests() {
        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{\"planDigestSha256\":\"%s\",\"baseCommitSha\":\"%s\",\"selectedPaths\":[\"src/App.java\"],\"overrides\":[]}".formatted("b".repeat(64), BASE))
                .post("/api/imports/{id}/selection", importId).then().statusCode(409)
                .body("code", equalTo("IMPORT_PLAN_DIGEST_MISMATCH"));

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA).contentType(ContentType.JSON)
                .body("{\"planDigestSha256\":\"%s\",\"baseCommitSha\":\"%s\",\"selectedPaths\":[\".git/config\"],\"overrides\":[]}".formatted(PLAN_DIGEST, BASE))
                .post("/api/imports/{id}/selection", importId).then().statusCode(400)
                .body("code", equalTo("HARD_BLOCKED_PATH_SELECTED"));

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieB)
                .get("/api/imports/{id}/selection", importId).then().statusCode(404)
                .body("code", equalTo("IMPORT_NOT_FOUND"));
    }

    private static ImmutableImportPlan plan(UUID importId) {
        return new ImmutableImportPlan(UUID.randomUUID(), importId, USER_A, "f".repeat(64), BASE,
                "mvp-3", PLAN_DIGEST, "READY", true, List.of(
                new ImmutableImportPlanEntry("src/App.java", "MODIFIED", "MODIFIED", "NONE", "NONE",
                        null, null, 1L, "c".repeat(64), 1L, "d".repeat(64), true),
                new ImmutableImportPlanEntry(".git/config", "BLOCKED", "MODIFIED", "BLOCKING", "HARD_BLOCKED",
                        "GIT_METADATA_PROTECTED", "blocked", 1L, "c".repeat(64), 1L, "d".repeat(64), true),
                new ImmutableImportPlanEntry(".github/workflows/ci.yml", "BLOCKED", "MODIFIED", "BLOCKING", "OVERRIDABLE_BLOCKED",
                        "GITHUB_WORKFLOW_PROTECTED", "blocked", 1L, "c".repeat(64), 1L, "d".repeat(64), true)
        ), Instant.now());
    }
}
