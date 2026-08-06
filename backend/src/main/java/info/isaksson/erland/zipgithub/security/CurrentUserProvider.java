package info.isaksson.erland.zipgithub.security;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.UUID;

@RequestScoped
public class CurrentUserProvider {
    public static final String SESSION_COOKIE = "zip_github_session";

    @Context HttpHeaders headers;
    @Inject WebSessionStore sessions;

    public UUID requireUserId() { return requireSession().userId(); }

    public WebSessionStore.SessionRecord requireSession() {
        Cookie cookie = headers.getCookies().get(SESSION_COOKIE);
        if (cookie == null) throw ApiException.unauthorized("AUTH_REQUIRED", "An authenticated user is required.");
        return sessions.find(cookie.getValue())
                .orElseThrow(() -> ApiException.unauthorized("SESSION_EXPIRED", "The web session is missing or expired."));
    }
}
