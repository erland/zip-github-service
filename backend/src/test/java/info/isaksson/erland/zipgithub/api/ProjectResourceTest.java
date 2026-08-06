package info.isaksson.erland.zipgithub.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.security.WebSessionStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProjectResourceTest {
    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    @Inject WebSessionStore sessions;
    @InjectMock GitHubProjectCatalog catalog;

    @BeforeEach
    void githubCatalogue() {
        var installation = new GitHubAppClient.GitHubInstallation(10L, 1L, "erland", "User", "selected", null);
        var repository = new GitHubAppClient.GitHubRepository(20L, "erland/example", true, "main", "https://github.com/erland/example");
        when(catalog.listUserInstallations(anyString())).thenReturn(List.of(installation));
        when(catalog.listUserInstallationRepositories(anyString(), eq(10L))).thenReturn(List.of(repository));
        when(catalog.branchExists(anyString(), eq("erland/example"), eq("main"))).thenReturn(true);
    }

    @Test
    void authenticationIsRequired() {
        given().when().get("/api/projects").then().statusCode(401)
                .contentType("application/problem+json").body("code", equalTo("AUTH_REQUIRED"));
    }

    @Test
    void projectBindingAndOwnerIsolationAreEnforced() {
        String cookieA = sessions.createSession(USER_A, 1L, "user-a", null, "token-a");
        String cookieB = sessions.createSession(USER_B, 2L, "user-b", null, "token-b");
        String projectId = given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieA)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Novel\",\"githubInstallationId\":10,\"githubRepositoryId\":20,\"defaultBranch\":\"main\"}")
                .post("/api/projects").then().statusCode(201)
                .body("repositoryFullName", equalTo("erland/example"))
                .extract().path("id");

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookieB)
                .get("/api/projects/{id}", projectId).then().statusCode(404)
                .body("code", equalTo("PROJECT_NOT_FOUND"));
    }

    @Test
    void unknownRepositoryIsRejected() {
        String cookie = sessions.createSession(USER_A, 1L, "user-a", null, "token-a");
        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie).contentType(ContentType.JSON)
                .body("{\"name\":\"Novel\",\"githubInstallationId\":10,\"githubRepositoryId\":999,\"defaultBranch\":\"main\"}")
                .post("/api/projects").then().statusCode(404)
                .body("code", equalTo("GITHUB_REPOSITORY_NOT_FOUND"));
    }
}
