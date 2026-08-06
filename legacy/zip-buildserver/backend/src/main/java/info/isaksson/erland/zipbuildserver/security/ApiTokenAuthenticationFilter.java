package info.isaksson.erland.zipbuildserver.security;

import info.isaksson.erland.zipbuildserver.api.ErrorResponse;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class ApiTokenAuthenticationFilter implements ContainerRequestFilter {
    private static final String UNAUTHORIZED_CODE = "unauthorized";

    @ConfigProperty(name = "zip-buildserver.auth.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "zip-buildserver.auth.api-token", defaultValue = "")
    String apiToken;

    private final TokenAuthenticationService authenticationService = new TokenAuthenticationService();

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!enabled || isPublicPath(requestContext.getUriInfo().getPath())) {
            return;
        }

        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (!authenticationService.isAuthorized(authorizationHeader, apiToken)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(ErrorResponse.of(UNAUTHORIZED_CODE, "A valid bearer token is required."))
                    .build());
        }
    }

    private boolean isPublicPath(String requestPath) {
        String normalized = requestPath == null ? "" : requestPath.strip();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.equals("api/health")
                || normalized.equals("api/health/")
                || normalized.equals("health")
                || normalized.equals("health/")
                || normalized.equals("q/openapi")
                || normalized.startsWith("q/openapi/");
    }
}
