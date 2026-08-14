package info.isaksson.erland.zipgithub.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.github.GitHubAppClient;
import info.isaksson.erland.zipgithub.github.GitHubProjectCatalog;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.security.WebSessionStore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RepositoryResourceTest {
    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Inject WebSessionStore sessions;
    @Inject ProjectApplicationService projects;
    @InjectMock GitHubProjectCatalog catalog;

    @BeforeEach
    void setup() {
        projects.clearInMemoryStateForTests();
        var installation = new GitHubAppClient.GitHubInstallation(10L, 1L, "erland", "User", "selected", null, "write");
        var repository = new GitHubAppClient.GitHubRepository(20L, "erland/got-test-repo", true, "main", "https://github.com/erland/got-test-repo");
        when(catalog.listUserInstallations(anyString())).thenReturn(List.of(installation));
        when(catalog.listUserInstallationRepositories(anyString(), eq(10L))).thenReturn(List.of(repository));
        when(catalog.branchExists(anyString(), eq("erland/got-test-repo"), eq("main"))).thenReturn(true);
    }

    @Test
    void listingRepositoriesDoesNotCreateInternalProjects() {
        String cookie = sessions.createSession(USER, 1L, "erland", null, "Erland", "1+erland@users.noreply.github.com", "token");
        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .get("/api/repositories").then().statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].repositoryName", equalTo("got-test-repo"))
                .body("[0].repositoryFullName", equalTo("erland/got-test-repo"))
                .body("[0].projectId", nullValue());

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .get("/api/projects").then().statusCode(200).body("size()", equalTo(0));
    }

    @Test
    void startingRepositoryWorkCreatesAndThenReusesTheInternalProject() {
        String cookie = sessions.createSession(USER, 1L, "erland", null, "Erland", "1+erland@users.noreply.github.com", "token");
        String projectId = given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .post("/api/repositories/10/20/work").then().statusCode(200)
                .body("project.repositoryFullName", equalTo("erland/got-test-repo"))
                .body("project.name", equalTo("got-test-repo"))
                .body("work.status", equalTo("ACTIVE"))
                .extract().path("project.id");

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .get("/api/repositories").then().statusCode(200)
                .body("[0].projectId", equalTo(projectId));

        given().cookie(CurrentUserProvider.SESSION_COOKIE, cookie)
                .post("/api/repositories/10/20/work").then().statusCode(200)
                .body("project.id", equalTo(projectId));
    }
}
