package info.isaksson.erland.zipbuildserver.api.session;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SessionResourceTest {
    @Test
    void createsReadsListsAndClosesSession() {
        String sessionId = given()
                .contentType("application/json")
                .body("""
                        {
                          "label": "Assistant verification run",
                          "retentionPolicy": "default"
                        }
                        """)
                .when().post("/api/sessions")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("label", equalTo("Assistant verification run"))
                .body("status", equalTo("OPEN"))
                .body("createdAt", notNullValue())
                .body("closedAt", equalTo(null))
                .body("createdBy", equalTo(null))
                .body("retentionPolicy", equalTo("default"))
                .extract().path("id");

        given()
                .when().get("/api/sessions/{sessionId}", sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId))
                .body("label", equalTo("Assistant verification run"))
                .body("status", equalTo("OPEN"))
                .body("createdAt", notNullValue())
                .body("closedAt", equalTo(null))
                .body("createdBy", equalTo(null))
                .body("retentionPolicy", equalTo("default"));

        given()
                .when().get("/api/sessions")
                .then()
                .statusCode(200)
                .body("sessions.findAll { it.id == '%s' }".formatted(sessionId), hasSize(1))
                .body("sessions.find { it.id == '%s' }.label".formatted(sessionId), equalTo("Assistant verification run"))
                .body("sessions.find { it.id == '%s' }.status".formatted(sessionId), equalTo("OPEN"))
                .body("sessions.find { it.id == '%s' }.createdAt".formatted(sessionId), notNullValue())
                .body("sessions.find { it.id == '%s' }.retentionPolicy".formatted(sessionId), equalTo("default"));

        given()
                .when().post("/api/sessions/{sessionId}/close", sessionId)
                .then()
                .statusCode(200)
                .body("id", equalTo(sessionId))
                .body("label", equalTo("Assistant verification run"))
                .body("status", equalTo("CLOSED"))
                .body("createdAt", notNullValue())
                .body("closedAt", notNullValue())
                .body("createdBy", equalTo(null))
                .body("retentionPolicy", equalTo("default"));
    }

    @Test
    void unknownSessionReturnsControlledNotFound() {
        given()
                .when().get("/api/sessions/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404)
                .body("code", equalTo("not_found"));
    }
}
