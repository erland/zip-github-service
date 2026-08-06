package info.isaksson.erland.zipgithub.auth;

import info.isaksson.erland.zipgithub.api.dto.AuthenticatedUserResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.security.WebSessionStore;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Path("/api/auth")
public class AuthResource {
    public static final String STATE_COOKIE = "zip_github_oauth_state";
    @Inject WebSessionStore sessions;
    @Inject GitHubOAuthClient github;
    @Inject CurrentUserProvider currentUser;
    @ConfigProperty(name="zipgithub.frontend-url") String frontendUrl;
    @ConfigProperty(name="zipgithub.cookies.secure", defaultValue="true") boolean secureCookies;

    @GET @Path("/github/login")
    public Response login(@QueryParam("returnTo") @DefaultValue("/projects") String returnTo) {
        String safeReturnTo = returnTo.startsWith("/") && !returnTo.startsWith("//") ? returnTo : "/projects";
        String state = sessions.createState(safeReturnTo);
        NewCookie stateCookie = cookie(STATE_COOKIE, state, 600);
        return Response.seeOther(github.authorizationUri(state)).cookie(stateCookie).build();
    }

    @GET @Path("/github/callback")
    public Response callback(@QueryParam("code") String code, @QueryParam("state") String state,
                             @CookieParam(STATE_COOKIE) Cookie stateCookie) {
        if (code == null || code.isBlank() || stateCookie == null || !state.equals(stateCookie.getValue())) {
            throw ApiException.unauthorized("INVALID_OAUTH_STATE", "The GitHub login state is invalid or missing.");
        }
        WebSessionStore.StateRecord stateRecord = sessions.consumeState(state)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_OAUTH_STATE", "The GitHub login state is invalid, expired or already used."));
        GitHubOAuthClient.GitHubUser githubUser = github.exchangeAndLoadUser(code);
        UUID userId = UUID.nameUUIDFromBytes(("github:" + githubUser.id()).getBytes(StandardCharsets.UTF_8));
        String session = sessions.createSession(userId, githubUser.id(), githubUser.login(), githubUser.avatarUrl(), githubUser.accessToken());
        URI target = URI.create(frontendUrl + stateRecord.returnTo());
        return Response.seeOther(target)
                .cookie(cookie(CurrentUserProvider.SESSION_COOKIE, session, 43200), expiredCookie(STATE_COOKIE)).build();
    }

    @GET @Path("/me") @Produces(MediaType.APPLICATION_JSON)
    public AuthenticatedUserResponse me() {
        var session = currentUser.requireSession();
        return new AuthenticatedUserResponse(session.userId(), session.githubUserId(), session.login(), session.avatarUrl());
    }

    @POST @Path("/logout")
    public Response logout(@CookieParam(CurrentUserProvider.SESSION_COOKIE) Cookie cookie) {
        if (cookie != null) sessions.invalidate(cookie.getValue());
        return Response.noContent().cookie(expiredCookie(CurrentUserProvider.SESSION_COOKIE)).build();
    }

    private NewCookie cookie(String name, String value, int maxAge) {
        return new NewCookie.Builder(name).value(value).path("/").httpOnly(true).secure(secureCookies)
                .sameSite(NewCookie.SameSite.LAX).maxAge(maxAge).build();
    }
    private NewCookie expiredCookie(String name) { return cookie(name, "", 0); }
}
