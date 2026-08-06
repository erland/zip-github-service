package info.isaksson.erland.zipbuildserver.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HealthResourceTest {
    @Test
    void healthEndpointReturnsServiceStatus() {
        given()
            .when().get("/api/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("ok"))
            .body("service", equalTo("zip-buildserver-api"));
    }
}
