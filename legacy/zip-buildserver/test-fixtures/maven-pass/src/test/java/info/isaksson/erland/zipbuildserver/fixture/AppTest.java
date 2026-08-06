package info.isaksson.erland.zipbuildserver.fixture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void messageReturnsFixtureName() {
        assertEquals("maven-pass", App.message());
    }
}
