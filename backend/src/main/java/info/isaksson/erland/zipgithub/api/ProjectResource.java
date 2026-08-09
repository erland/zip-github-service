package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.actions.ImportActionsDetailsService;
import info.isaksson.erland.zipgithub.actions.ImportActionsStatusService;
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
    @Inject ImportActionsStatusService actionsStatuses;
    @Inject ImportActionsDetailsService actionsDetails;

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

    @GET @Path("/{projectId}/work/branches")
    public List<WorkBranchResponse> listWorkBranches(@PathParam("projectId") UUID projectId) {
        return service.availableWorkBranches(currentUser.requireUserId(), projectId).stream()
                .map(branch -> new WorkBranchResponse(branch.name(), branch.commitSha())).toList();
    }

    @POST @Path("/{projectId}/work")
    public WorkSessionResponse startWork(@PathParam("projectId") UUID projectId, StartWorkRequest request) {
        var item = service.startWork(currentUser.requireUserId(), projectId, request == null ? null : request.existingBranch());
        return workResponse(item);
    }

    @POST @Path("/{projectId}/work/abandon")
    public WorkSessionResponse abandonWork(@PathParam("projectId") UUID projectId, AbandonWorkRequest request) {
        var item = service.abandonWork(currentUser.requireUserId(), projectId, request != null && request.shouldDeleteBranch());
        return workResponse(item);
    }

    @DELETE @Path("/{projectId}")
    public Response archive(@PathParam("projectId") UUID projectId) {
        service.archiveProject(currentUser.requireUserId(), projectId);
        return Response.noContent().build();
    }

    @GET @Path("/{projectId}/work")
    public Response getWork(@PathParam("projectId") UUID projectId) {
        var work = service.activeWork(currentUser.requireUserId(), projectId);
        if (work.isEmpty()) return Response.noContent().build();
        var item = work.get();
        return Response.ok(workResponse(item)).build();
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

    @GET @Path("/{projectId}/work/actions")
    public ImportActionsStatusResponse getWorkActions(@PathParam("projectId") UUID projectId) {
        UUID ownerUserId = currentUser.requireUserId();
        var source = service.activeWorkSource(ownerUserId, projectId);
        var work = source.work();
        var status = actionsStatuses.read(work.lastImportId(), source.githubInstallationId(),
                source.repositoryFullName(), work.headCommitSha());
        var workflows = status.workflows().stream().map(workflow -> new ImportActionsStatusResponse.WorkflowRunResponse(
                workflow.id(), workflow.workflowId(), workflow.workflowPath(), workflow.headBranch(), workflow.headSha(),
                workflow.name(), workflow.state(), workflow.terminal(), workflow.event(), workflow.htmlUrl(),
                workflow.createdAt(), workflow.updatedAt(), workflow.jobs().stream().map(job -> new ImportActionsStatusResponse.JobResponse(
                        job.id(), job.name(), job.state(), job.terminal(), job.htmlUrl(), job.startedAt(), job.completedAt())).toList())).toList();
        var checks = status.checks().stream().map(check -> new ImportActionsStatusResponse.CheckRunResponse(
                check.id(), check.name(), check.state(), check.terminal(), check.htmlUrl(), check.appName(),
                check.startedAt(), check.completedAt())).toList();
        return new ImportActionsStatusResponse(status.importId(), status.repositoryFullName(), status.commitSha(),
                status.state(), status.terminal(), status.detailsUrl(), workflows, checks, status.diagnosticCode(), status.diagnosticMessage(), status.checkedAt());
    }

    @GET @Path("/{projectId}/work/actions/details")
    public ImportActionsDetailsResponse getWorkActionDetails(@PathParam("projectId") UUID projectId) {
        UUID ownerUserId = currentUser.requireUserId();
        var source = service.activeWorkSource(ownerUserId, projectId);
        var work = source.work();
        var details = actionsDetails.read(work.lastImportId(), source.githubInstallationId(),
                source.repositoryFullName(), work.headCommitSha());
        var artifacts = details.artifacts().stream().map(artifact -> new ImportActionsDetailsResponse.ArtifactResponse(
                artifact.id(), artifact.name(), artifact.sizeBytes(), artifact.expired(), artifact.createdAt(), artifact.expiresAt(),
                artifact.workflowRunId(), artifact.workflowName(), artifact.githubUrl())).toList();
        var failures = details.failures().stream().map(failure -> new ImportActionsDetailsResponse.FailureResponse(
                failure.workflowRunId(), failure.workflowName(), failure.jobId(), failure.jobName(), failure.stepName(),
                failure.tool(), failure.lines(), failure.contextLines(), failure.jobLogLines(), failure.logTruncated(), failure.githubUrl())).toList();
        return new ImportActionsDetailsResponse(details.importId(), details.repositoryFullName(), details.commitSha(),
                details.detailsUrl(), artifacts, failures, details.checkedAt());
    }

    @POST @Path("/{projectId}/work/pull-request")
    public PullRequestResponse createWorkPullRequest(@PathParam("projectId") UUID projectId) {
        var session = currentUser.requireSession();
        UUID ownerUserId = session.userId();
        var source = service.activeWorkSource(ownerUserId, projectId);
        var work = source.work();
        var delivery = new GitDeliveryResult(work.lastImportId(), source.repositoryFullName(), work.baseBranch(),
                work.branchName(), work.baseCommitSha(), work.headCommitSha(), work.lastPlanDigestSha256(), work.updatedAt());
        try {
            var created = pullRequests.createOrReuseDraft(session.githubUserAccessToken(), delivery);
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
    static WorkSessionResponse workResponse(info.isaksson.erland.zipgithub.application.WorkSession item) {
        return new WorkSessionResponse(item.id(), item.projectId(), item.baseBranch(), item.branchName(),
                item.status(), item.headCommitSha(), item.lastImportId(), item.pullRequestNumber(), item.pullRequestUrl(), item.createdAt(), item.updatedAt());
    }

}
