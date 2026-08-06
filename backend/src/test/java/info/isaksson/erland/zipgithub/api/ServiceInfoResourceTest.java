package info.isaksson.erland.zipgithub.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ServiceInfoResourceTest {
    @Test
    void healthReturnsServiceIdentity() {
        given().when().get("/api/health").then().statusCode(200)
            .body("service", is("zip-github"))
            .body("status", is("UP"));
    }
}
