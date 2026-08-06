package info.isaksson.erland.zipgithub.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthResourceTest {
    @Test void meRequiresSessionCookie() {
        given().when().get("/api/auth/me").then().statusCode(401).contentType("application/problem+json")
                .body("code", is("AUTH_REQUIRED"));
    }

    @Test void loginRedirectsAndSetsHttpOnlyStateCookie() {
        given().redirects().follow(false).when().get("/api/auth/github/login")
                .then().statusCode(303).header("Location", startsWith("https://github.com/login/oauth/authorize"))
                .header("Set-Cookie", containsString("zip_github_oauth_state="));
    }
}
