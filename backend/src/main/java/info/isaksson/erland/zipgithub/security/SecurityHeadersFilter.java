package info.isaksson.erland.zipgithub.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class SecurityHeadersFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        var headers = response.getHeaders();
        headers.putSingle("X-Content-Type-Options", "nosniff");
        headers.putSingle("X-Frame-Options", "DENY");
        headers.putSingle("Referrer-Policy", "no-referrer");
        headers.putSingle("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        headers.putSingle("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
        headers.putSingle("Cross-Origin-Resource-Policy", "same-origin");
        if (request.getUriInfo().getPath().startsWith("api/")) {
            headers.putSingle("Cache-Control", "no-store");
            headers.putSingle("Pragma", "no-cache");
        }
    }
}
