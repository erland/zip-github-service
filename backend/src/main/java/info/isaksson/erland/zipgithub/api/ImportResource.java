package info.isaksson.erland.zipgithub.api;

import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.api.dto.SourceUploadResponse;
import info.isaksson.erland.zipgithub.api.dto.RepositorySnapshotResponse;
import info.isaksson.erland.zipgithub.api.dto.ImportComparisonResponse;
import info.isaksson.erland.zipgithub.api.dto.ImportPolicyResponse;
import info.isaksson.erland.zipgithub.api.dto.ImportPlanResponse;
import info.isaksson.erland.zipgithub.api.dto.ApproveImportPlanRequest;
import info.isaksson.erland.zipgithub.api.dto.ImportPlanApprovalResponse;
import info.isaksson.erland.zipgithub.api.dto.CreateImportSelectionRequest;
import info.isaksson.erland.zipgithub.api.dto.ImportSelectionResponse;
import info.isaksson.erland.zipgithub.api.dto.AppliedImportWorkspaceResponse;
import info.isaksson.erland.zipgithub.api.dto.GitDeliveryResponse;
import info.isaksson.erland.zipgithub.api.dto.PullRequestResponse;
import info.isaksson.erland.zipgithub.api.dto.ImportCheckStatusResponse;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.security.CurrentUserProvider;
import info.isaksson.erland.zipgithub.upload.StreamingUploadService;
import info.isaksson.erland.zipgithub.upload.UploadTooLargeException;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshotException;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshotService;
import info.isaksson.erland.zipgithub.archive.ArchiveInventoryService;
import info.isaksson.erland.zipgithub.comparison.ImportComparisonService;
import info.isaksson.erland.zipgithub.comparison.ImportFileStatus;
import info.isaksson.erland.zipgithub.policy.ImportPolicyService;
import info.isaksson.erland.zipgithub.plan.ImportPlanFactory;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.selection.ApprovedSelection;
import info.isaksson.erland.zipgithub.selection.ImportSelectionFactory;
import info.isaksson.erland.zipgithub.workspace.ImportWorkspaceException;
import info.isaksson.erland.zipgithub.workspace.ImportWorkspaceService;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryException;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryService;
import info.isaksson.erland.zipgithub.pullrequest.PullRequestService;
import info.isaksson.erland.zipgithub.checks.ImportCheckStatusService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Path("/api/imports")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {
    @Inject CurrentUserProvider currentUser;
    @Inject ProjectApplicationService service;
    @Inject StreamingUploadService uploads;
    @Inject RepositorySnapshotService snapshots;
    @Inject ArchiveInventoryService archiveInventories;
    @Inject ImportComparisonService comparisons;
    @Inject ImportPolicyService importPolicy;
    @Inject ImportPlanFactory importPlans;
    @Inject ImportSelectionFactory importSelections;
    @Inject ImportWorkspaceService importWorkspaces;
    @Inject GitDeliveryService gitDelivery;
    @Inject PullRequestService pullRequests;
    @Inject ImportCheckStatusService checkStatuses;

    @GET @Path("/{importId}")
    public ImportResponse get(@PathParam("importId") UUID importId) {
        return service.getImport(currentUser.requireUserId(), importId);
    }

    @POST
    @Path("/{importId}/repository-snapshot")
    public RepositorySnapshotResponse createRepositorySnapshot(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var target = service.snapshotTarget(ownerUserId, importId);
        try {
            var snapshot = snapshots.create(importId, target.githubInstallationId(), target.repositoryFullName(), target.branch());
            var stored = service.recordRepositorySnapshot(ownerUserId, importId, snapshot);
            return new RepositorySnapshotResponse(stored.importId(), stored.repositoryFullName(), stored.branch(),
                    stored.baseCommitSha(), stored.entries().size(),
                    stored.entries().stream().map(entry -> new RepositorySnapshotResponse.Entry(
                            entry.path(), entry.mode(), entry.objectType(), entry.objectId(), entry.sizeBytes(), entry.sha256())).toList(),
                    stored.createdAt());
        } catch (RepositorySnapshotException e) {
            throw ApiException.badGateway("REPOSITORY_SNAPSHOT_FAILED", e.getMessage());
        }
    }


    @POST
    @Path("/{importId}/comparison")
    public ImportComparisonResponse compare(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var sources = service.comparisonSources(ownerUserId, importId);
        try {
            var archive = archiveInventories.createInventory(sources.upload().storagePath());
            var comparison = comparisons.compare(archive, sources.snapshot());
            return new ImportComparisonResponse(comparison.importId(), comparison.baseCommitSha(),
                    comparison.count(ImportFileStatus.ADDED), comparison.count(ImportFileStatus.MODIFIED),
                    comparison.count(ImportFileStatus.UNCHANGED), comparison.count(ImportFileStatus.WOULD_DELETE),
                    comparison.entries().stream().map(entry -> new ImportComparisonResponse.Entry(
                            entry.path(), entry.status().name(), entry.archiveSizeBytes(), entry.archiveSha256(),
                            entry.repositorySizeBytes(), entry.repositorySha256())).toList());
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) throw apiException;
            throw ApiException.badRequest("IMPORT_COMPARISON_FAILED", e.getMessage());
        }
    }

    @POST
    @Path("/{importId}/policy")
    public ImportPolicyResponse evaluatePolicy(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var sources = service.comparisonSources(ownerUserId, importId);
        try {
            var archive = archiveInventories.createInventory(sources.upload().storagePath());
            var comparison = comparisons.compare(archive, sources.snapshot());
            var result = importPolicy.evaluate(archive, comparison);
            return new ImportPolicyResponse(result.importId(), result.baseCommitSha(), result.policyVersion(), result.approvable(),
                    result.count(ImportFileStatus.ADDED), result.count(ImportFileStatus.MODIFIED),
                    result.count(ImportFileStatus.UNCHANGED), result.count(ImportFileStatus.IGNORED),
                    result.count(ImportFileStatus.BLOCKED), result.hardBlockers(), result.overridableBlockers(), result.warnings(),
                    result.entries().stream().map(entry -> new ImportPolicyResponse.Entry(
                            entry.path(), entry.status().name(),
                            entry.comparisonStatus() == null ? null : entry.comparisonStatus().name(),
                            entry.severity().name(), entry.blockerType().name(), entry.policyCode(), entry.message(),
                            entry.archiveSizeBytes(), entry.archiveSha256(),
                            entry.repositorySizeBytes(), entry.repositorySha256())).toList());
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) throw apiException;
            throw ApiException.badRequest("IMPORT_POLICY_FAILED", e.getMessage());
        }
    }


    @POST
    @Path("/{importId}/plan")
    public ImportPlanResponse createPlan(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var sources = service.comparisonSources(ownerUserId, importId);
        try {
            var archive = archiveInventories.createInventory(sources.upload().storagePath());
            var comparison = comparisons.compare(archive, sources.snapshot());
            var policy = importPolicy.evaluate(archive, comparison);
            var created = importPlans.create(ownerUserId, sources.upload().sha256(), archive, policy, java.time.Instant.now());
            return toPlanResponse(service.recordImportPlan(ownerUserId, importId, created));
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) throw apiException;
            throw ApiException.badRequest("IMPORT_PLAN_FAILED", e.getMessage());
        }
    }

    @GET
    @Path("/{importId}/plan")
    public ImportPlanResponse getPlan(@PathParam("importId") UUID importId) {
        return toPlanResponse(service.getImportPlan(currentUser.requireUserId(), importId));
    }

    @POST
    @Path("/{importId}/selection")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSelection(@PathParam("importId") UUID importId, CreateImportSelectionRequest request) {
        UUID ownerUserId = currentUser.requireUserId();
        var existing = service.findImportSelection(ownerUserId, importId);
        var plan = service.getImportPlan(ownerUserId, importId);
        var requestedOverrides = request == null || request.overrides() == null ? java.util.List.<ImportSelectionFactory.RequestedOverride>of()
                : request.overrides().stream().map(item -> new ImportSelectionFactory.RequestedOverride(
                        item.path(), item.acknowledgement())).toList();
        var created = importSelections.create(ownerUserId, plan,
                request == null ? null : request.planDigestSha256(),
                request == null ? null : request.baseCommitSha(),
                request == null ? null : request.selectedPaths(), requestedOverrides, java.time.Instant.now());
        var stored = service.recordImportSelection(ownerUserId, importId, created);
        return Response.status(existing.isPresent() ? Response.Status.OK : Response.Status.CREATED)
                .entity(toSelectionResponse(stored)).build();
    }

    @GET
    @Path("/{importId}/selection")
    public ImportSelectionResponse getSelection(@PathParam("importId") UUID importId) {
        return toSelectionResponse(service.getImportSelection(currentUser.requireUserId(), importId));
    }


    @POST
    @Path("/{importId}/plan/approval")
    @Consumes(MediaType.APPLICATION_JSON)
    public ImportPlanApprovalResponse approvePlan(@PathParam("importId") UUID importId,
                                                   ApproveImportPlanRequest request) {
        UUID ownerUserId = currentUser.requireUserId();
        var approval = service.approveImportPlan(ownerUserId, importId,
                request == null ? null : request.planDigestSha256(),
                request == null ? null : request.selectionDigestSha256());
        return new ImportPlanApprovalResponse(approval.importId(), approval.planId(),
                approval.planDigestSha256(), approval.selectionDigestSha256(), "APPROVED", approval.approvedAt());
    }


    @POST
    @Path("/{importId}/workspace")
    public AppliedImportWorkspaceResponse prepareWorkspace(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var sources = service.deliverySources(ownerUserId, importId);
        try {
            var archive = archiveInventories.createInventory(sources.upload().storagePath());
            var prepared = importWorkspaces.prepare(importId, sources.githubInstallationId(),
                    sources.repositoryFullName(), sources.upload().storagePath(),
                    archive.strippedWrapperDirectory(), sources.plan(), sources.selection());
            var stored = service.recordAppliedWorkspace(ownerUserId, importId, prepared);
            return new AppliedImportWorkspaceResponse(stored.importId(), stored.repositoryFullName(),
                    stored.baseCommitSha(), stored.planDigestSha256(), stored.selectionDigestSha256(), stored.appliedPaths().size(),
                    stored.appliedPaths(), "FILES_APPLIED", stored.preparedAt());
        } catch (IOException e) {
            throw ApiException.badRequest("ARCHIVE_READ_FAILED", e.getMessage());
        } catch (ImportWorkspaceException e) {
            throw ApiException.conflict("WORKSPACE_PREPARATION_FAILED", e.getMessage());
        }
    }


    @POST
    @Path("/{importId}/delivery")
    public GitDeliveryResponse deliver(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var existing = service.findGitDelivery(ownerUserId, importId);
        if (existing.isPresent()) {
            var stored = existing.get();
            return new GitDeliveryResponse(stored.importId(), stored.repositoryFullName(), stored.baseBranch(),
                    stored.branchName(), stored.baseCommitSha(), stored.commitSha(), stored.planDigestSha256(),
                    "PUSHED", stored.pushedAt());
        }
        var sources = service.deliverySources(ownerUserId, importId);
        var workspace = service.findAppliedWorkspace(ownerUserId, importId)
                .orElseThrow(() -> ApiException.conflict("WORKSPACE_REQUIRED",
                        "Prepare and verify the Git workspace before delivery."));
        try {
            var identity = service.gitCommitIdentity(ownerUserId, importId);
            var delivered = gitDelivery.deliver(sources.githubInstallationId(), sources.snapshot().branch(),
                    service.workBranchForImport(ownerUserId, importId), workspace, identity);
            var stored = service.recordGitDelivery(ownerUserId, importId, delivered);
            importWorkspaces.delete(workspace);
            return new GitDeliveryResponse(stored.importId(), stored.repositoryFullName(), stored.baseBranch(),
                    stored.branchName(), stored.baseCommitSha(), stored.commitSha(), stored.planDigestSha256(),
                    "PUSHED", stored.pushedAt());
        } catch (GitDeliveryException e) {
            if (e.retryable()) throw ApiException.badGateway("GIT_DELIVERY_RETRYABLE", e.getMessage());
            throw ApiException.conflict("GIT_DELIVERY_FAILED", e.getMessage());
        }
    }

    @GET
    @Path("/{importId}/delivery")
    public GitDeliveryResponse getDelivery(@PathParam("importId") UUID importId) {
        var stored = service.findGitDelivery(currentUser.requireUserId(), importId)
                .orElseThrow(() -> ApiException.notFound("GIT_DELIVERY_NOT_FOUND", "No Git delivery has been recorded for this import."));
        return new GitDeliveryResponse(stored.importId(), stored.repositoryFullName(), stored.baseBranch(),
                stored.branchName(), stored.baseCommitSha(), stored.commitSha(), stored.planDigestSha256(),
                "PUSHED", stored.pushedAt());
    }

    @POST
    @Path("/{importId}/pull-request")
    public PullRequestResponse createPullRequest(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var existing = service.findPullRequest(ownerUserId, importId);
        if (existing.isPresent()) {
            var stored = existing.get();
            return new PullRequestResponse(stored.importId(), stored.repositoryFullName(), stored.baseBranch(),
                    stored.branchName(), stored.commitSha(), stored.planDigestSha256(), stored.pullRequestNumber(),
                    stored.pullRequestUrl(), stored.draft(), stored.state(), "PULL_REQUEST_CREATED", stored.createdAt());
        }
        var sources = service.deliverySources(ownerUserId, importId);
        var delivery = service.findGitDelivery(ownerUserId, importId)
                .orElseThrow(() -> ApiException.conflict("GIT_DELIVERY_REQUIRED",
                        "Push the approved import branch before creating a pull request."));
        try {
            var created = pullRequests.createOrReuseDraft(sources.githubInstallationId(), delivery);
            var stored = service.recordPullRequest(ownerUserId, importId, created);
            return new PullRequestResponse(stored.importId(), stored.repositoryFullName(), stored.baseBranch(),
                    stored.branchName(), stored.commitSha(), stored.planDigestSha256(),
                    stored.pullRequestNumber(), stored.pullRequestUrl(), stored.draft(), stored.state(),
                    "PULL_REQUEST_CREATED", stored.createdAt());
        } catch (IllegalStateException e) {
            throw ApiException.badGateway("PULL_REQUEST_CREATION_FAILED", e.getMessage());
        }
    }

    @GET
    @Path("/{importId}/checks")
    public ImportCheckStatusResponse getChecks(@PathParam("importId") UUID importId) {
        UUID ownerUserId = currentUser.requireUserId();
        var sources = service.deliverySources(ownerUserId, importId);
        var delivery = service.findGitDelivery(ownerUserId, importId)
                .orElseThrow(() -> ApiException.conflict("GIT_DELIVERY_REQUIRED",
                        "The import must be pushed before check status can be read."));
        var status = checkStatuses.read(importId, sources.githubInstallationId(),
                delivery.repositoryFullName(), delivery.commitSha());
        return new ImportCheckStatusResponse(status.importId(), status.repositoryFullName(), status.commitSha(),
                status.state(), status.terminal(), status.total(), status.pending(), status.successful(),
                status.failed(), status.cancelled(), status.detailsUrl(), status.checkedAt());
    }

    @GET
    @Path("/{importId}/pull-request")
    public PullRequestResponse getPullRequest(@PathParam("importId") UUID importId) {
        var stored = service.findPullRequest(currentUser.requireUserId(), importId)
                .orElseThrow(() -> ApiException.notFound("PULL_REQUEST_NOT_FOUND",
                        "No pull request metadata has been recorded for this import."));
        return new PullRequestResponse(stored.importId(), stored.repositoryFullName(), stored.baseBranch(),
                stored.branchName(), stored.commitSha(), stored.planDigestSha256(),
                stored.pullRequestNumber(), stored.pullRequestUrl(), stored.draft(), stored.state(),
                "PULL_REQUEST_CREATED", stored.createdAt());
    }

    private static ImportSelectionResponse toSelectionResponse(ApprovedSelection selection) {
        return new ImportSelectionResponse(selection.id(), selection.importId(), selection.planId(),
                selection.planDigestSha256(), selection.baseCommitSha(), selection.selectionVersion(),
                selection.selectionDigestSha256(), selection.selectedPaths(), selection.excludedPaths(),
                selection.overrides().stream().map(item -> new ImportSelectionResponse.Override(
                        item.path(), item.blockerType(), item.policyCode(), item.acknowledgement())).toList(),
                selection.createdAt());
    }

    private static ImportPlanResponse toPlanResponse(ImmutableImportPlan plan) {
        long added = plan.entries().stream().filter(e -> e.status().equals("ADDED")).count();
        long modified = plan.entries().stream().filter(e -> e.status().equals("MODIFIED")).count();
        long unchanged = plan.entries().stream().filter(e -> e.status().equals("UNCHANGED")).count();
        long ignored = plan.entries().stream().filter(e -> e.status().equals("IGNORED")).count();
        long blocked = plan.entries().stream().filter(e -> e.status().equals("BLOCKED")).count();
        long hardBlocked = plan.entries().stream().filter(e -> e.blockerType().equals("HARD_BLOCKED")).count();
        long overridableBlocked = plan.entries().stream().filter(e -> e.blockerType().equals("OVERRIDABLE_BLOCKED")).count();
        long warnings = plan.entries().stream().filter(e -> e.severity().equals("WARNING")).count();
        return new ImportPlanResponse(plan.id(), plan.importId(), plan.sourceUploadSha256(), plan.baseCommitSha(),
                plan.policyVersion(), plan.planDigestSha256(), plan.status(), plan.approvable(), added, modified,
                unchanged, ignored, blocked, hardBlocked, overridableBlocked, warnings, plan.entries().stream().map(e -> new ImportPlanResponse.Entry(
                        e.path(), e.status(), e.comparisonStatus(), e.severity(), e.blockerType(), e.policyCode(), e.message(),
                        e.archiveSizeBytes(), e.archiveSha256(), e.repositorySizeBytes(), e.repositorySha256(),
                        e.textCandidate())).toList(), plan.createdAt());
    }

    @PUT
    @Path("/{importId}/upload")
    @Consumes({"application/zip", MediaType.APPLICATION_OCTET_STREAM})
    public Response upload(@PathParam("importId") UUID importId,
                           @HeaderParam("X-Filename") String filename,
                           @HeaderParam("Content-Length") long contentLength,
                           InputStream input) {
        UUID ownerUserId = currentUser.requireUserId();
        service.assertOwnedImport(ownerUserId, importId);
        try {
            var stored = uploads.store(ownerUserId, importId, filename, contentLength, input);
            try {
                SourceUploadResponse response = service.recordUpload(ownerUserId, importId, stored);
                return Response.status(Response.Status.CREATED).entity(response).build();
            } catch (RuntimeException e) {
                try { Files.deleteIfExists(stored.storagePath()); } catch (IOException ignored) { }
                throw e;
            }
        } catch (UploadTooLargeException e) {
            throw ApiException.payloadTooLarge("UPLOAD_TOO_LARGE", e.getMessage());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_UPLOAD", e.getMessage());
        }
    }
}
