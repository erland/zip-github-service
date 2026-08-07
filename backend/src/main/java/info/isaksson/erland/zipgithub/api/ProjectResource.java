package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.github.GitHubCommitHistoryClient;
import info.isaksson.erland.zipgithub.github.GitHubInstallationTokenProvider;
import info.isaksson.erland.zipgithub.pullrequest.PullRequestService;
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
    @Inject PullRequestService pullRequests;
    @Inject GitHubInstallationTokenProvider installationTokens;
    @Inject GitHubCommitHistoryClient commitHistory;

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

    @GET @Path("/{projectId}/work")
    public Response getWork(@PathParam("projectId") UUID projectId) {
        var work = service.activeWork(currentUser.requireUserId(), projectId);
        if (work.isEmpty()) return Response.noContent().build();
        var item = work.get();
        return Response.ok(new WorkSessionResponse(item.id(), item.projectId(), item.baseBranch(), item.branchName(),
                item.status(), item.headCommitSha(), item.pullRequestNumber(), item.pullRequestUrl(), item.createdAt(), item.updatedAt())).build();
    }


    @GET @Path("/{projectId}/work/commits")
    public WorkHistoryResponse getWorkCommits(@PathParam("projectId") UUID projectId) {
        UUID ownerUserId = currentUser.requireUserId();
        ProjectResponse project = service.getProject(ownerUserId, projectId);
        var work = service.activeWork(ownerUserId, projectId);
        if (work.isEmpty() || work.get().headCommitSha() == null) return new WorkHistoryResponse(List.of(), true);
        try {
            String installationToken = installationTokens.createInstallationToken(project.githubInstallationId());
            var commits = commitHistory.listBranchCommits(installationToken, project.repositoryFullName(), work.get().branchName(), 50)
                    .stream()
                    .map(item -> new WorkCommitResponse(item.sha(), item.message(), item.authorName(), item.authorEmail(),
                            item.authoredAt(), item.htmlUrl(), false))
                    .toList();
            return new WorkHistoryResponse(commits, true);
        } catch (RuntimeException e) {
            String sha = work.get().headCommitSha();
            String url = "https://github.com/" + project.repositoryFullName() + "/commit/" + sha;
            return new WorkHistoryResponse(List.of(new WorkCommitResponse(sha, "Senaste kända Work-commit",
                    "", "", work.get().updatedAt(), url, true)), false);
        }
    }

    @POST @Path("/{projectId}/work/pull-request")
    public PullRequestResponse createWorkPullRequest(@PathParam("projectId") UUID projectId) {
        UUID ownerUserId = currentUser.requireUserId();
        var source = service.activeWorkSource(ownerUserId, projectId);
        var work = source.work();
        var delivery = new GitDeliveryResult(work.lastImportId(), source.repositoryFullName(), work.baseBranch(),
                work.branchName(), work.baseCommitSha(), work.headCommitSha(), work.lastPlanDigestSha256(), work.updatedAt());
        try {
            var created = pullRequests.createOrReuseDraft(source.githubInstallationId(), delivery);
            service.recordWorkPullRequest(ownerUserId, projectId, created);
            return new PullRequestResponse(created.importId(), created.repositoryFullName(), created.baseBranch(),
                    created.branchName(), created.commitSha(), created.planDigestSha256(), created.pullRequestNumber(),
                    created.pullRequestUrl(), created.draft(), created.state(), "PULL_REQUEST_CREATED", created.createdAt());
        } catch (IllegalStateException e) {
            throw info.isaksson.erland.zipgithub.api.error.ApiException.badGateway("PULL_REQUEST_CREATION_FAILED", e.getMessage());
        }
    }

    @POST @Path("/{projectId}/imports")
    public Response createImport(@PathParam("projectId") UUID projectId, CreateImportRequest request, @Context UriInfo uriInfo) {
        var session = currentUser.requireSession();
        ImportResponse created = service.createImport(session.userId(), projectId, request, session.gitName(), session.gitEmail());
        URI location = uriInfo.getBaseUriBuilder().path("api/imports").path(created.id().toString()).build();
        return Response.created(location).entity(created).build();
    }
}
