package info.isaksson.erland.zipgithub.security;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class CsrfProtectionFilter implements ContainerRequestFilter {
    public static final String REQUEST_HEADER = "X-Zip-GitHub-Request";

    @ConfigProperty(name = "zipgithub.security.csrf.enabled", defaultValue = "true") boolean enabled;
    @ConfigProperty(name = "zipgithub.frontend-url") String frontendUrl;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();
        if (!enabled || !path.startsWith("api/") || isSafe(request.getMethod()) || isCapabilityUpload(path, request.getMethod())) return;
        String marker = request.getHeaderString(REQUEST_HEADER);
        String origin = request.getHeaderString("Origin");
        if (!"1".equals(marker) || !SameOriginPolicy.matches(frontendUrl, origin)) {
            throw ApiException.forbidden("CSRF_REJECTED", "The request did not pass the same-origin CSRF check.");
        }
    }

    private static boolean isCapabilityUpload(String path, String method) {
        return "POST".equals(method) && "api/staging-imports".equals(path);
    }

    private static boolean isSafe(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}
