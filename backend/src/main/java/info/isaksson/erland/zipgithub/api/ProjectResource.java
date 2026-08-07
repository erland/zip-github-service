package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProjectResource {
    @Inject CurrentUserProvider currentUser;
    @Inject ProjectApplicationService service;

    @GET
    public List<ProjectResponse> list() { return service.listProjects(currentUser.requireUserId()); }

    @POST
    public Response create(CreateProjectRequest request, @Context UriInfo uriInfo) {
        var session = currentUser.requireSession();
        ProjectResponse project = service.createProject(session.userId(), session.githubUserAccessToken(), request);
        URI location = uriInfo.getAbsolutePathBuilder().path(project.id().toString()).build();
        return Response.created(location).entity(project).build();
    }

    @GET @Path("/{projectId}")
    public ProjectResponse get(@PathParam("projectId") UUID projectId) {
        return service.getProject(currentUser.requireUserId(), projectId);
    }

    @PATCH @Path("/{projectId}")
    public ProjectResponse update(@PathParam("projectId") UUID projectId, UpdateProjectRequest request) {
        var session = currentUser.requireSession();
        return service.updateProject(session.userId(), session.githubUserAccessToken(), projectId, request);
    }

    @GET @Path("/{projectId}/imports")
    public List<ImportHistoryResponse> listImports(@PathParam("projectId") UUID projectId) {
        return service.listProjectImports(currentUser.requireUserId(), projectId);
    }

    @POST @Path("/{projectId}/imports")
    public Response createImport(@PathParam("projectId") UUID projectId, CreateImportRequest request, @Context UriInfo uriInfo) {
        ImportResponse created = service.createImport(currentUser.requireUserId(), projectId, request);
        URI location = uriInfo.getBaseUriBuilder().path("api/imports").path(created.id().toString()).build();
        return Response.created(location).entity(created).build();
    }
}
