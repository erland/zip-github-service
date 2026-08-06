package info.isaksson.erland.zipbuildserver.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ApiTokenAuthenticationFilterTest.AuthEnabledProfile.class)
class ApiTokenAuthenticationFilterTest {
    @Test
    void allowsPublicHealthWithoutToken() {
        given()
                .when().get("/api/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("ok"));
    }

    @Test
    void rejectsApiRequestWithoutBearerToken() {
        given()
                .when().get("/api/verification-plans")
                .then()
                .statusCode(401)
                .body("code", equalTo("unauthorized"));
    }

    @Test
    void rejectsApiRequestWithWrongBearerToken() {
        given()
                .header("Authorization", "Bearer wrong-token")
                .when().get("/api/verification-plans")
                .then()
                .statusCode(401)
                .body("code", equalTo("unauthorized"));
    }

    @Test
    void allowsApiRequestWithConfiguredBearerToken() {
        given()
                .header("Authorization", "Bearer test-token")
                .when().get("/api/verification-plans")
                .then()
                .statusCode(200);
    }

    public static class AuthEnabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "zip-buildserver.auth.enabled", "true",
                    "zip-buildserver.auth.api-token", "test-token");
        }
    }
}
