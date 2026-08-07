package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.upload.StoredUpload;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImportPlanApproval;
import info.isaksson.erland.zipgithub.workspace.AppliedImportWorkspace;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.pullrequest.PullRequestResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory application store; database repositories replace it in a later persistence step. */
@ApplicationScoped
public class ProjectApplicationService {
    private final Map<UUID, OwnedProject> projects = new ConcurrentHashMap<>();
    private final Map<UUID, OwnedImport> imports = new ConcurrentHashMap<>();
    private final Map<UUID, StoredUpload> uploadsByImport = new ConcurrentHashMap<>();
    private final Map<UUID, RepositorySnapshot> snapshotsByImport = new ConcurrentHashMap<>();
    private final Map<UUID, ImmutableImportPlan> plansByImport = new ConcurrentHashMap<>();
    private final Map<UUID, ImportPlanApproval> approvalsByImport = new ConcurrentHashMap<>();
    private final Map<UUID, AppliedImportWorkspace> workspacesByImport = new ConcurrentHashMap<>();
    private final Map<UUID, GitDeliveryResult> deliveriesByImport = new ConcurrentHashMap<>();
    private final Map<UUID, PullRequestResult> pullRequestsByImport = new ConcurrentHashMap<>();
    @Inject GitHubProjectConfigurationService githubConfiguration;
    @Inject ProjectPersistenceStore persistentProjects;

    /** Clears the temporary in-memory store between Quarkus tests.
     *  This method must be removed when persistent repositories replace the prototype store.
     */
    public void clearInMemoryStateForTests() {
        projects.clear();
        imports.clear();
        uploadsByImport.clear();
        snapshotsByImport.clear();
        plansByImport.clear();
        approvalsByImport.clear();
        workspacesByImport.clear();
        deliveriesByImport.clear();
        pullRequestsByImport.clear();
    }

    public List<ProjectResponse> listProjects(UUID ownerUserId) {
        if (persistentProjects.enabled()) return persistentProjects.listProjects(ownerUserId);
        return projects.values().stream().filter(project -> project.ownerUserId.equals(ownerUserId))
                .map(OwnedProject::response).sorted(Comparator.comparing(ProjectResponse::createdAt)).toList();
    }

    public ProjectResponse createProject(UUID ownerUserId, String userAccessToken, CreateProjectRequest request) {
        String name = requireText(request == null ? null : request.name(), "name");
        ensureUniqueName(ownerUserId, name, null);
        var verified = githubConfiguration.verify(userAccessToken, request.githubInstallationId(), request.githubRepositoryId(), request.defaultBranch());
        Instant now = Instant.now();
        ProjectResponse response = new ProjectResponse(UUID.randomUUID(), name, verified.installationId(), verified.repositoryId(),
                verified.fullName(), verified.privateRepository(), verified.defaultBranch(), true, now, now);
        if (persistentProjects.enabled()) {
            persistentProjects.upsertInstallation(ownerUserId, verified.installationId(),
                    verified.installationAccountLogin(), verified.repositorySelection());
            persistentProjects.insertProject(ownerUserId, response);
        }
        projects.put(response.id(), new OwnedProject(ownerUserId, response));
        return response;
    }

    public ProjectResponse updateProject(UUID ownerUserId, String userAccessToken, UUID projectId, UpdateProjectRequest request) {
        OwnedProject owned = requireOwnedProject(ownerUserId, projectId);
        String name = request == null || request.name() == null ? owned.response.name() : requireText(request.name(), "name");
        ensureUniqueName(ownerUserId, name, projectId);
        Long installationId = request == null || request.githubInstallationId() == null ? owned.response.githubInstallationId() : request.githubInstallationId();
        Long repositoryId = request == null || request.githubRepositoryId() == null ? owned.response.githubRepositoryId() : request.githubRepositoryId();
        String branch = request == null || request.defaultBranch() == null ? owned.response.defaultBranch() : request.defaultBranch();
        var verified = githubConfiguration.verify(userAccessToken, installationId, repositoryId, branch);
        boolean active = request == null || request.active() == null ? owned.response.active() : request.active();
        ProjectResponse updated = new ProjectResponse(projectId, name, verified.installationId(), verified.repositoryId(),
                verified.fullName(), verified.privateRepository(), verified.defaultBranch(), active,
                owned.response.createdAt(), Instant.now());
        if (persistentProjects.enabled()) {
            persistentProjects.upsertInstallation(ownerUserId, verified.installationId(),
                    verified.installationAccountLogin(), verified.repositorySelection());
            persistentProjects.updateProject(ownerUserId, updated);
        }
        projects.put(projectId, new OwnedProject(ownerUserId, updated));
        return updated;
    }

    public ProjectResponse getProject(UUID ownerUserId, UUID projectId) { return requireOwnedProject(ownerUserId, projectId).response; }

    public ImportResponse createImport(UUID ownerUserId, UUID projectId, CreateImportRequest request) {
        ProjectResponse project = requireOwnedProject(ownerUserId, projectId).response;
        if (!project.active()) throw ApiException.conflict("PROJECT_INACTIVE", "The project is inactive.");
        String branch = request == null || request.baseBranch() == null || request.baseBranch().isBlank()
                ? project.defaultBranch() : normalizeBranch(request.baseBranch());
        ImportResponse response = new ImportResponse(UUID.randomUUID(), projectId, branch, "CREATED", Instant.now());
        imports.put(response.id(), new OwnedImport(ownerUserId, response));
        return response;
    }

    public ImportResponse getImport(UUID ownerUserId, UUID importId) {
        return requireOwnedImport(ownerUserId, importId).response;
    }

    public List<ImportHistoryResponse> listProjectImports(UUID ownerUserId, UUID projectId) {
        requireOwnedProject(ownerUserId, projectId);
        return imports.values().stream()
                .filter(item -> item.ownerUserId.equals(ownerUserId) && item.response.projectId().equals(projectId))
                .map(item -> {
                    ImportResponse itemResponse = item.response;
                    StoredUpload upload = uploadsByImport.get(itemResponse.id());
                    ImmutableImportPlan plan = plansByImport.get(itemResponse.id());
                    PullRequestResult pullRequest = pullRequestsByImport.get(itemResponse.id());
                    return new ImportHistoryResponse(itemResponse.id(), itemResponse.projectId(), itemResponse.baseBranch(),
                            itemResponse.status(), itemResponse.createdAt(),
                            upload == null ? null : upload.originalFilename(),
                            upload == null ? null : upload.sizeBytes(),
                            plan == null ? null : plan.planDigestSha256(),
                            pullRequest == null ? null : pullRequest.pullRequestNumber(),
                            pullRequest == null ? null : pullRequest.pullRequestUrl(),
                            resumeStage(itemResponse.status(), plan, pullRequest));
                })
                .sorted(Comparator.comparing(ImportHistoryResponse::createdAt).reversed())
                .toList();
    }

    private static String resumeStage(String status, ImmutableImportPlan plan, PullRequestResult pullRequest) {
        if (pullRequest != null || "PULL_REQUEST_CREATED".equals(status)) return "RESULT";
        if (plan != null || Set.of("READY_FOR_REVIEW", "BLOCKED", "APPROVED", "FILES_APPLIED", "PUSHED").contains(status)) return "REVIEW";
        return "UPLOAD";
    }


    public void assertOwnedImport(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
    }

    public SourceUploadResponse recordUpload(UUID ownerUserId, UUID importId, StoredUpload upload) {
        getImport(ownerUserId, importId);
        if (!upload.ownerUserId().equals(ownerUserId) || !upload.importId().equals(importId))
            throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        StoredUpload existing = uploadsByImport.putIfAbsent(importId, upload);
        if (existing != null) throw ApiException.conflict("UPLOAD_ALREADY_EXISTS", "This import already has a source upload.");
        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "UPLOADING", current.createdAt())));
        return new SourceUploadResponse(upload.id(), upload.importId(), upload.originalFilename(), upload.sizeBytes(),
                upload.sha256(), "STORED", upload.createdAt(), upload.retentionDeadline());
    }

    public SnapshotTarget snapshotTarget(UUID ownerUserId, UUID importId) {
        OwnedImport ownedImport = requireOwnedImport(ownerUserId, importId);
        OwnedProject project = requireOwnedProject(ownerUserId, ownedImport.response.projectId());
        return new SnapshotTarget(project.response.githubInstallationId(), project.response.repositoryFullName(), ownedImport.response.baseBranch());
    }

    public RepositorySnapshot recordRepositorySnapshot(UUID ownerUserId, UUID importId, RepositorySnapshot snapshot) {
        OwnedImport ownedImport = requireOwnedImport(ownerUserId, importId);
        if (!snapshot.importId().equals(importId)) throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        RepositorySnapshot existing = snapshotsByImport.putIfAbsent(importId, snapshot);
        if (existing != null) {
            if (existing.baseCommitSha().equals(snapshot.baseCommitSha())) return existing;
            throw ApiException.conflict("REPOSITORY_SNAPSHOT_EXISTS", "This import is already locked to a repository snapshot.");
        }
        ImportResponse current = ownedImport.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "INSPECTING", current.createdAt())));
        return snapshot;
    }

    public Optional<RepositorySnapshot> findRepositorySnapshot(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(snapshotsByImport.get(importId));
    }

    public ComparisonSources comparisonSources(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        StoredUpload upload = uploadsByImport.get(importId);
        if (upload == null) throw ApiException.conflict("SOURCE_UPLOAD_REQUIRED", "Upload a ZIP before comparing the import.");
        RepositorySnapshot snapshot = snapshotsByImport.get(importId);
        if (snapshot == null) throw ApiException.conflict("REPOSITORY_SNAPSHOT_REQUIRED", "Create a repository snapshot before comparing the import.");
        return new ComparisonSources(upload, snapshot);
    }


    public ImmutableImportPlan recordImportPlan(UUID ownerUserId, UUID importId, ImmutableImportPlan plan) {
        requireOwnedImport(ownerUserId, importId);
        if (!plan.importId().equals(importId) || !plan.ownerUserId().equals(ownerUserId)) {
            throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        }
        ImmutableImportPlan existing = plansByImport.putIfAbsent(importId, plan);
        if (existing != null) {
            if (existing.planDigestSha256().equals(plan.planDigestSha256())) return existing;
            throw ApiException.conflict("IMPORT_PLAN_IMMUTABLE",
                    "An immutable import plan already exists for this import.");
        }
        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(),
                current.baseBranch(), plan.approvable() ? "READY_FOR_REVIEW" : "BLOCKED", current.createdAt())));
        return plan;
    }

    public ImmutableImportPlan getImportPlan(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        ImmutableImportPlan plan = plansByImport.get(importId);
        if (plan == null) throw ApiException.notFound("IMPORT_PLAN_NOT_FOUND", "The import plan was not found.");
        return plan;
    }

    public ImportPlanApproval approveImportPlan(UUID ownerUserId, UUID importId, String submittedDigest) {
        requireOwnedImport(ownerUserId, importId);
        ImmutableImportPlan plan = getImportPlan(ownerUserId, importId);
        if (submittedDigest == null || !submittedDigest.matches("[0-9a-f]{64}")) {
            throw ApiException.badRequest("INVALID_PLAN_DIGEST", "planDigestSha256 must be a lower-case SHA-256.");
        }
        if (!plan.planDigestSha256().equals(submittedDigest)) {
            throw ApiException.conflict("IMPORT_PLAN_DIGEST_MISMATCH",
                    "The submitted plan digest does not match the immutable plan currently stored for this import.");
        }
        if (!plan.approvable()) {
            throw ApiException.conflict("IMPORT_PLAN_BLOCKED", "A blocked import plan cannot be approved.");
        }

        ImportPlanApproval candidate = new ImportPlanApproval(importId, plan.id(), ownerUserId,
                plan.planDigestSha256(), Instant.now());
        ImportPlanApproval existing = approvalsByImport.putIfAbsent(importId, candidate);
        ImportPlanApproval approval = existing == null ? candidate : existing;
        if (!approval.planDigestSha256().equals(submittedDigest) || !approval.approvedByUserId().equals(ownerUserId)) {
            throw ApiException.conflict("IMPORT_PLAN_ALREADY_APPROVED",
                    "This import was already approved with a different plan identity.");
        }

        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(),
                current.baseBranch(), "APPROVED", current.createdAt())));
        return approval;
    }

    public Optional<ImportPlanApproval> findImportPlanApproval(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(approvalsByImport.get(importId));
    }


    public DeliverySources deliverySources(UUID ownerUserId, UUID importId) {
        OwnedImport ownedImport = requireOwnedImport(ownerUserId, importId);
        OwnedProject project = requireOwnedProject(ownerUserId, ownedImport.response.projectId());
        StoredUpload upload = uploadsByImport.get(importId);
        RepositorySnapshot snapshot = snapshotsByImport.get(importId);
        ImmutableImportPlan plan = plansByImport.get(importId);
        ImportPlanApproval approval = approvalsByImport.get(importId);
        if (upload == null || snapshot == null || plan == null || approval == null) {
            throw ApiException.conflict("IMPORT_NOT_APPROVED",
                    "The source upload, repository snapshot, immutable plan and exact approval are required.");
        }
        if (!approval.planDigestSha256().equals(plan.planDigestSha256())
                || !snapshot.baseCommitSha().equals(plan.baseCommitSha())
                || !upload.sha256().equals(plan.sourceUploadSha256())) {
            throw ApiException.conflict("IMPORT_IDENTITY_MISMATCH",
                    "The approved plan no longer matches the stored import sources.");
        }
        return new DeliverySources(project.response.githubInstallationId(), project.response.repositoryFullName(),
                upload, snapshot, plan, approval);
    }

    public AppliedImportWorkspace recordAppliedWorkspace(UUID ownerUserId, UUID importId,
                                                          AppliedImportWorkspace workspace) {
        requireOwnedImport(ownerUserId, importId);
        ImmutableImportPlan plan = getImportPlan(ownerUserId, importId);
        if (!workspace.importId().equals(importId)
                || !workspace.planDigestSha256().equals(plan.planDigestSha256())
                || !workspace.baseCommitSha().equals(plan.baseCommitSha())) {
            throw ApiException.conflict("WORKSPACE_IDENTITY_MISMATCH",
                    "The prepared workspace does not match the approved import plan.");
        }
        AppliedImportWorkspace existing = workspacesByImport.putIfAbsent(importId, workspace);
        AppliedImportWorkspace result = existing == null ? workspace : existing;
        if (!result.planDigestSha256().equals(workspace.planDigestSha256())) {
            throw ApiException.conflict("WORKSPACE_ALREADY_EXISTS",
                    "A workspace already exists for a different plan identity.");
        }
        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(),
                current.baseBranch(), "FILES_APPLIED", current.createdAt())));
        return result;
    }

    public Optional<AppliedImportWorkspace> findAppliedWorkspace(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(workspacesByImport.get(importId));
    }


    public GitDeliveryResult recordGitDelivery(UUID ownerUserId, UUID importId, GitDeliveryResult delivery) {
        OwnedImport owned = requireOwnedImport(ownerUserId, importId);
        AppliedImportWorkspace workspace = workspacesByImport.get(importId);
        if (workspace == null || !delivery.importId().equals(importId)
                || !delivery.baseCommitSha().equals(workspace.baseCommitSha())
                || !delivery.planDigestSha256().equals(workspace.planDigestSha256())) {
            throw ApiException.conflict("DELIVERY_IDENTITY_MISMATCH",
                    "The Git delivery does not match the approved workspace.");
        }
        GitDeliveryResult existing = deliveriesByImport.putIfAbsent(importId, delivery);
        GitDeliveryResult result = existing == null ? delivery : existing;
        if (!result.commitSha().equals(delivery.commitSha()) || !result.branchName().equals(delivery.branchName())) {
            throw ApiException.conflict("DELIVERY_ALREADY_EXISTS",
                    "This import already has a different Git delivery.");
        }
        ImportResponse current = owned.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(),
                current.baseBranch(), "PUSHED", current.createdAt())));
        return result;
    }

    public Optional<GitDeliveryResult> findGitDelivery(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(deliveriesByImport.get(importId));
    }

    public PullRequestResult recordPullRequest(UUID ownerUserId, UUID importId, PullRequestResult result) {
        OwnedImport owned = requireOwnedImport(ownerUserId, importId);
        GitDeliveryResult delivery = deliveriesByImport.get(importId);
        if (delivery == null || !result.importId().equals(importId)
                || !result.commitSha().equals(delivery.commitSha())
                || !result.branchName().equals(delivery.branchName())
                || !result.planDigestSha256().equals(delivery.planDigestSha256())) {
            throw ApiException.conflict("PULL_REQUEST_IDENTITY_MISMATCH",
                    "The pull request does not match the recorded Git delivery.");
        }
        PullRequestResult existing = pullRequestsByImport.putIfAbsent(importId, result);
        PullRequestResult stored = existing == null ? result : existing;
        if (stored.pullRequestNumber() != result.pullRequestNumber()
                || !stored.pullRequestUrl().equals(result.pullRequestUrl())) {
            throw ApiException.conflict("PULL_REQUEST_ALREADY_EXISTS",
                    "This import already has different pull request metadata.");
        }
        ImportResponse current = owned.response;
        imports.put(importId, new OwnedImport(ownerUserId, new ImportResponse(current.id(), current.projectId(),
                current.baseBranch(), "PULL_REQUEST_CREATED", current.createdAt())));
        return stored;
    }

    public Optional<PullRequestResult> findPullRequest(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(pullRequestsByImport.get(importId));
    }

    public List<StoredUpload> expiredUploads(Instant now) {
        return uploadsByImport.values().stream()
                .filter(upload -> !upload.retentionDeadline().isAfter(now))
                .toList();
    }

    public boolean removeExpiredUpload(UUID importId, UUID uploadId, Instant now) {
        StoredUpload current = uploadsByImport.get(importId);
        if (current == null || !current.id().equals(uploadId) || current.retentionDeadline().isAfter(now)) return false;
        return uploadsByImport.remove(importId, current);
    }

    private OwnedImport requireOwnedImport(UUID ownerUserId, UUID importId) {
        OwnedImport item = imports.get(importId);
        if (item == null || !item.ownerUserId.equals(ownerUserId)) throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        return item;
    }

    private OwnedProject requireOwnedProject(UUID ownerUserId, UUID projectId) {
        if (persistentProjects.enabled()) {
            ProjectResponse response = persistentProjects.findProject(ownerUserId, projectId)
                    .orElseThrow(() -> ApiException.notFound("PROJECT_NOT_FOUND", "The project was not found."));
            return new OwnedProject(ownerUserId, response);
        }
        OwnedProject item = projects.get(projectId);
        if (item == null || !item.ownerUserId.equals(ownerUserId)) throw ApiException.notFound("PROJECT_NOT_FOUND", "The project was not found.");
        return item;
    }

    private void ensureUniqueName(UUID ownerUserId, String name, UUID excludedId) {
        if (persistentProjects.enabled()) {
            if (persistentProjects.projectNameExists(ownerUserId, name, excludedId))
                throw ApiException.conflict("PROJECT_NAME_EXISTS", "A project with this name already exists.");
            return;
        }
        boolean duplicate = projects.values().stream().anyMatch(project -> project.ownerUserId.equals(ownerUserId)
                && !project.response.id().equals(excludedId) && project.response.name().equalsIgnoreCase(name));
        if (duplicate) throw ApiException.conflict("PROJECT_NAME_EXISTS", "A project with this name already exists.");
    }

    private static String normalizeBranch(String branch) {
        String value = branch == null || branch.isBlank() ? "main" : branch.trim();
        if (value.contains("..") || value.startsWith("/") || value.endsWith("/") || value.contains("\\"))
            throw ApiException.badRequest("INVALID_BRANCH", "The branch name is invalid.");
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw ApiException.badRequest("VALIDATION_ERROR", field + " is required.");
        return value.trim();
    }

    public record SnapshotTarget(long githubInstallationId, String repositoryFullName, String branch) {}
    public record ComparisonSources(StoredUpload upload, RepositorySnapshot snapshot) {}
    public record DeliverySources(long githubInstallationId, String repositoryFullName, StoredUpload upload,
                                  RepositorySnapshot snapshot, ImmutableImportPlan plan,
                                  ImportPlanApproval approval) {}

    private record OwnedProject(UUID ownerUserId, ProjectResponse response) {}
    private record OwnedImport(UUID ownerUserId, ImportResponse response) {}
}
