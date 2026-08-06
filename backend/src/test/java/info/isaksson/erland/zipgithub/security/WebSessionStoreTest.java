package info.isaksson.erland.zipgithub.security;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WebSessionStoreTest {
    @Test void oauthStateIsSingleUse() {
        WebSessionStore store = new WebSessionStore();
        String state = store.createState("/projects");
        assertTrue(store.consumeState(state).isPresent());
        assertTrue(store.consumeState(state).isEmpty());
    }

    @Test void sessionCanBeCreatedReadAndInvalidated() {
        WebSessionStore store = new WebSessionStore();
        UUID userId = UUID.randomUUID();
        String token = store.createSession(userId, 123L, "erland", null, "test-user-token");
        assertEquals(userId, store.find(token).orElseThrow().userId());
        store.invalidate(token);
        assertTrue(store.find(token).isEmpty());
    }
}
