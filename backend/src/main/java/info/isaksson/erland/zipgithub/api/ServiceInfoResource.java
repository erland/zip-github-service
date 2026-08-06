package info.isaksson.erland.zipgithub.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceInfoResource {
    @GET
    @Path("/health")
    public Map<String, String> health() {
        return Map.of("service", "zip-github", "status", "UP");
    }
}
