package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.upload.StoredUpload;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import info.isaksson.erland.zipgithub.domain.model.ImportAuditMetadata;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImportPlanApproval;
import info.isaksson.erland.zipgithub.plan.CommitMessagePolicy;
import info.isaksson.erland.zipgithub.selection.ApprovedSelection;
import info.isaksson.erland.zipgithub.workspace.AppliedImportWorkspace;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.delivery.GitCommitIdentity;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.ImportResumePersistenceStore;
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
    private final Map<UUID, ApprovedSelection> selectionsByImport = new ConcurrentHashMap<>();
    private final Map<UUID, AppliedImportWorkspace> workspacesByImport = new ConcurrentHashMap<>();
    private final Map<UUID, GitDeliveryResult> deliveriesByImport = new ConcurrentHashMap<>();
    private final Map<UUID, PullRequestResult> pullRequestsByImport = new ConcurrentHashMap<>();
    private final Map<UUID, GitCommitIdentity> gitIdentitiesByImport = new ConcurrentHashMap<>();
    private final Map<UUID, ImportAuditMetadata> auditMetadataByImport = new ConcurrentHashMap<>();
    private final Map<UUID, WorkSession> workByProject = new ConcurrentHashMap<>();
    private final Map<StoredUploadPromotionKey, StoredUploadPromotion> storedUploadPromotions = new ConcurrentHashMap<>();
    private final Map<UUID, StoredUploadPromotion> storedUploadPromotionsByArtifact = new ConcurrentHashMap<>();
    @Inject GitHubProjectConfigurationService githubConfiguration;
    @Inject ProjectPersistenceStore persistentProjects;
    @Inject WorkPersistenceStore persistentWork;
    @Inject ImportResumePersistenceStore persistentImports;

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
        selectionsByImport.clear();
        workspacesByImport.clear();
        deliveriesByImport.clear();
        pullRequestsByImport.clear();
        gitIdentitiesByImport.clear();
        auditMetadataByImport.clear();
        workByProject.clear();
        storedUploadPromotions.clear();
        storedUploadPromotionsByArtifact.clear();
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

    public Optional<WorkSession> activeWork(UUID ownerUserId, UUID projectId) {
        requireOwnedProject(ownerUserId, projectId);
        if (persistentProjects.enabled()) return persistentWork.findActive(ownerUserId, projectId);
        WorkSession work = workByProject.get(projectId);
        return work != null && work.ownerUserId().equals(ownerUserId) && "ACTIVE".equals(work.status()) ? Optional.of(work) : Optional.empty();
    }

    public WorkSession requireActiveWork(UUID ownerUserId, UUID projectId) {
        return activeWork(ownerUserId, projectId).orElseThrow(() ->
                ApiException.conflict("ACTIVE_WORK_REQUIRED", "The project has no active work."));
    }

    public String workBranchForImport(UUID ownerUserId, UUID importId) {
        OwnedImport owned = requireOwnedImport(ownerUserId, importId);
        return requireActiveWork(ownerUserId, owned.response.projectId()).branchName();
    }

    public ProjectWorkSource activeWorkSource(UUID ownerUserId, UUID projectId) {
        ProjectResponse project = requireOwnedProject(ownerUserId, projectId).response;
        WorkSession work = requireActiveWork(ownerUserId, projectId);
        if (!work.hasCommit() || work.lastImportId() == null || work.lastPlanDigestSha256() == null || work.baseCommitSha() == null)
            throw ApiException.conflict("WORK_HAS_NO_COMMITS", "The active work has no committed ZIP imports yet.");
        return new ProjectWorkSource(project.githubInstallationId(), project.repositoryFullName(), work);
    }

    public WorkSession recordWorkPullRequest(UUID ownerUserId, UUID projectId, PullRequestResult result) {
        WorkSession work = requireActiveWork(ownerUserId, projectId);
        if (!work.branchName().equals(result.branchName()) || !work.headCommitSha().equals(result.commitSha()))
            throw ApiException.conflict("WORK_PULL_REQUEST_MISMATCH", "The pull request does not match the active work head.");
        if (persistentProjects.enabled()) return persistentWork.recordPullRequest(ownerUserId, projectId, result.pullRequestNumber(), result.pullRequestUrl());
        WorkSession completed = new WorkSession(work.id(), work.projectId(), work.ownerUserId(), work.baseBranch(), work.branchName(),
                "PULL_REQUEST_CREATED", work.headCommitSha(), work.baseCommitSha(), work.lastImportId(), work.lastPlanDigestSha256(),
                result.pullRequestNumber(), result.pullRequestUrl(), work.createdAt(), Instant.now());
        workByProject.put(projectId, completed);
        return completed;
    }

    public ImportResponse createImport(UUID ownerUserId, UUID projectId, CreateImportRequest request,
                                       String committerName, String committerEmail) {
        return createImport(ownerUserId, projectId, request, committerName, committerEmail,
                new ImportAuditMetadata(ImportSource.WEB_UPLOAD, null));
    }

    private synchronized ImportResponse createImport(UUID ownerUserId, UUID projectId, CreateImportRequest request,
                                        String committerName, String committerEmail,
                                        ImportAuditMetadata auditMetadata) {
        ProjectResponse project = requireOwnedProject(ownerUserId, projectId).response;
        if (!project.active()) throw ApiException.conflict("PROJECT_INACTIVE", "The project is inactive.");
        assertNoActiveImport(ownerUserId, projectId);
        WorkSession work = getOrCreateWork(ownerUserId, projectId, project.defaultBranch());
        String branch = work.hasCommit() ? work.branchName() : work.baseBranch();
        String requestedName = request == null ? null : request.authorName();
        String requestedEmail = request == null ? null : request.authorEmail();
        boolean customAuthor = (requestedName != null && !requestedName.isBlank()) || (requestedEmail != null && !requestedEmail.isBlank());
        if (customAuthor && (requestedName == null || requestedName.isBlank() || requestedEmail == null || requestedEmail.isBlank()))
            throw ApiException.badRequest("INVALID_GIT_AUTHOR", "Both author name and email are required for another author.");
        try {
            GitCommitIdentity identity = new GitCommitIdentity(
                    customAuthor ? requestedName : committerName,
                    customAuthor ? requestedEmail : committerEmail,
                    committerName, committerEmail);
            ImportResponse response = new ImportResponse(UUID.randomUUID(), projectId, branch, "CREATED", Instant.now());
            imports.put(response.id(), new OwnedImport(ownerUserId, response));
            gitIdentitiesByImport.put(response.id(), identity);
            ImportAuditMetadata lockedAudit = Objects.requireNonNull(auditMetadata, "auditMetadata");
            auditMetadataByImport.put(response.id(), lockedAudit);
            if (persistentImportsEnabled()) persistentImports.insertImport(ownerUserId, response, identity, lockedAudit);
            return response;
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_GIT_IDENTITY", e.getMessage());
        }
    }


    /**
     * Creates a normal user-owned import from an already safely stored ZIP artifact without
     * copying or re-streaming the physical ZIP. The idempotency key is scoped to owner/project.
     */
    public synchronized StoredUploadImportResult createImportFromStoredUpload(
            UUID ownerUserId, UUID projectId, CreateImportRequest request,
            String committerName, String committerEmail,
            StoredUploadArtifact artifact, String idempotencyKey) {
        return createImportFromStoredUpload(ownerUserId, projectId, request, committerName, committerEmail, artifact,
                idempotencyKey, new ImportAuditMetadata(ImportSource.STORED_UPLOAD, "stored-upload:" + artifact.id()));
    }

    public synchronized StoredUploadImportResult createImportFromStoredUpload(
            UUID ownerUserId, UUID projectId, CreateImportRequest request,
            String committerName, String committerEmail, StoredUploadArtifact artifact, String idempotencyKey,
            ImportAuditMetadata auditMetadata) {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(auditMetadata, "auditMetadata");
        requireOwnedProject(ownerUserId, projectId);
        String normalizedKey = requireText(idempotencyKey, "idempotencyKey");
        if (normalizedKey.length() > 200)
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY", "idempotencyKey is too long.");

        StoredUploadPromotionKey key = new StoredUploadPromotionKey(ownerUserId, projectId, normalizedKey);
        StoredUploadPromotion existingByKey = storedUploadPromotions.get(key);
        if (existingByKey != null) {
            if (!existingByKey.artifactId().equals(artifact.id())
                    || !existingByKey.artifactSha256().equals(artifact.sha256()))
                throw ApiException.conflict("STORED_UPLOAD_PROMOTION_KEY_REUSED",
                        "The idempotency key is already bound to another stored ZIP.");
            return storedUploadImportResult(ownerUserId, existingByKey.importId());
        }

        StoredUploadPromotion existingByArtifact = storedUploadPromotionsByArtifact.get(artifact.id());
        if (existingByArtifact != null) {
            if (existingByArtifact.ownerUserId().equals(ownerUserId)
                    && existingByArtifact.projectId().equals(projectId)
                    && existingByArtifact.artifactSha256().equals(artifact.sha256()))
                return storedUploadImportResult(ownerUserId, existingByArtifact.importId());
            throw ApiException.conflict("STORED_UPLOAD_ALREADY_PROMOTED",
                    "The stored ZIP is already attached to another import.");
        }

        ImportResponse created = createImport(ownerUserId, projectId, request, committerName, committerEmail, auditMetadata);
        StoredUpload attached = StoredUpload.attach(ownerUserId, created.id(), artifact);
        SourceUploadResponse upload = recordUpload(ownerUserId, created.id(), attached);
        StoredUploadPromotion promotion = new StoredUploadPromotion(ownerUserId, projectId, created.id(),
                artifact.id(), artifact.sha256(), normalizedKey);
        storedUploadPromotions.put(key, promotion);
        storedUploadPromotionsByArtifact.put(artifact.id(), promotion);
        return new StoredUploadImportResult(created, upload);
    }

    private StoredUploadImportResult storedUploadImportResult(UUID ownerUserId, UUID importId) {
        ImportResponse imported = getImport(ownerUserId, importId);
        StoredUpload upload = uploadsByImport.get(importId);
        if (upload == null)
            throw ApiException.conflict("STORED_UPLOAD_PROMOTION_INCOMPLETE",
                    "The promoted import is missing its stored ZIP attachment.");
        return new StoredUploadImportResult(imported, toSourceUploadResponse(upload));
    }

    private static SourceUploadResponse toSourceUploadResponse(StoredUpload upload) {
        return new SourceUploadResponse(upload.id(), upload.importId(), upload.originalFilename(), upload.sizeBytes(),
                upload.sha256(), "STORED", upload.createdAt(), upload.retentionDeadline());
    }

    public Optional<ImportResponse> findImportBySourceReference(UUID ownerUserId, ImportSource source, String sourceReference) {
        if (persistentImportsEnabled()) {
            var state = persistentImports.findBySourceReference(ownerUserId, source, sourceReference);
            if (state.isPresent()) { hydrate(state.get()); return Optional.of(state.get().response()); }
        }
        return imports.values().stream().filter(item -> item.ownerUserId.equals(ownerUserId))
                .filter(item -> { var audit = auditMetadataByImport.get(item.response.id()); return audit != null && audit.source() == source && Objects.equals(audit.sourceReference(), sourceReference); })
                .map(item -> item.response).findFirst();
    }

    public StoredUploadImportResult ensureStoredUploadAttached(UUID ownerUserId, UUID importId, StoredUploadArtifact artifact) {
        requireOwnedImport(ownerUserId, importId);
        StoredUpload existing = uploadsByImport.get(importId);
        if (existing != null) {
            if (!existing.id().equals(artifact.id()) || !existing.sha256().equals(artifact.sha256()))
                throw ApiException.conflict("STORED_UPLOAD_PROMOTION_INCOMPLETE", "The import is already attached to another ZIP.");
            return new StoredUploadImportResult(getImport(ownerUserId, importId), toSourceUploadResponse(existing));
        }
        StoredUpload attached = StoredUpload.attach(ownerUserId, importId, artifact);
        return new StoredUploadImportResult(getImport(ownerUserId, importId), recordUpload(ownerUserId, importId, attached));
    }


    public ImportAuditMetadata importAuditMetadata(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return auditMetadataByImport.getOrDefault(importId, ImportAuditMetadata.webUpload());
    }

    public GitCommitIdentity gitCommitIdentity(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        GitCommitIdentity identity = gitIdentitiesByImport.get(importId);
        if (identity == null) throw ApiException.conflict("GIT_IDENTITY_MISSING", "The import has no locked Git identity.");
        return identity;
    }

    public ImportResponse getImport(UUID ownerUserId, UUID importId) {
        return requireOwnedImport(ownerUserId, importId).response;
    }

    private void assertNoActiveImport(UUID ownerUserId, UUID projectId) {
        if (persistentImportsEnabled()) persistentImports.list(ownerUserId, projectId).forEach(this::hydrate);
        boolean active = imports.values().stream()
                .anyMatch(item -> item.ownerUserId.equals(ownerUserId)
                        && item.response.projectId().equals(projectId)
                        && !isTerminalImportStatus(item.response.status()));
        if (active) {
            throw ApiException.conflict("ACTIVE_IMPORT_EXISTS",
                    "The work already has an active import. Complete or cancel it before uploading another ZIP.");
        }
    }

    private static boolean isTerminalImportStatus(String status) {
        return Set.of("PUSHED", "PULL_REQUEST_CREATED", "CANCELLED").contains(status);
    }

    public List<ImportHistoryResponse> listProjectImports(UUID ownerUserId, UUID projectId) {
        requireOwnedProject(ownerUserId, projectId);
        if (persistentImportsEnabled()) {
            persistentImports.list(ownerUserId, projectId).forEach(this::hydrate);
        }
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
                            auditMetadataByImport.getOrDefault(itemResponse.id(), ImportAuditMetadata.webUpload()).source().name(),
                            auditMetadataByImport.getOrDefault(itemResponse.id(), ImportAuditMetadata.webUpload()).sourceReference(),
                            resumeStage(itemResponse.status(), plan, pullRequest));
                })
                .sorted(Comparator.comparing(ImportHistoryResponse::createdAt).reversed())
                .toList();
    }

    private static String resumeStage(String status, ImmutableImportPlan plan, PullRequestResult pullRequest) {
        if ("CANCELLED".equals(status) || pullRequest != null || "PULL_REQUEST_CREATED".equals(status) || "PUSHED".equals(status)) return "RESULT";
        if (plan != null || Set.of("READY_FOR_REVIEW", "BLOCKED", "APPROVED", "FILES_APPLIED", "PUSHED").contains(status)) return "REVIEW";
        return "UPLOAD";
    }


    public void assertOwnedImport(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
    }

    public SourceUploadResponse recordUpload(UUID ownerUserId, UUID importId, StoredUpload upload) {
        requireMutableImport(ownerUserId, importId);
        if (!upload.ownerUserId().equals(ownerUserId) || !upload.importId().equals(importId))
            throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        StoredUpload existing = uploadsByImport.putIfAbsent(importId, upload);
        if (existing != null) throw ApiException.conflict("UPLOAD_ALREADY_EXISTS", "This import already has a source upload.");
        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        ImportResponse updated = new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "UPLOADING", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, updated));
        if (persistentImportsEnabled()) { persistentImports.saveUpload(ownerUserId, importId, upload); persistentImports.updateStatus(ownerUserId, importId, updated.status(), null); }
        return toSourceUploadResponse(upload);
    }

    public SnapshotTarget snapshotTarget(UUID ownerUserId, UUID importId) {
        OwnedImport ownedImport = requireOwnedImport(ownerUserId, importId);
        OwnedProject project = requireOwnedProject(ownerUserId, ownedImport.response.projectId());
        return new SnapshotTarget(project.response.githubInstallationId(), project.response.repositoryFullName(), ownedImport.response.baseBranch());
    }

    public RepositorySnapshot recordRepositorySnapshot(UUID ownerUserId, UUID importId, RepositorySnapshot snapshot) {
        OwnedImport ownedImport = requireMutableImport(ownerUserId, importId);
        if (!snapshot.importId().equals(importId)) throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        RepositorySnapshot existing = snapshotsByImport.putIfAbsent(importId, snapshot);
        if (existing != null) {
            if (existing.baseCommitSha().equals(snapshot.baseCommitSha())) return existing;
            throw ApiException.conflict("REPOSITORY_SNAPSHOT_EXISTS", "This import is already locked to a repository snapshot.");
        }
        ImportResponse current = ownedImport.response;
        ImportResponse updated = new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "INSPECTING", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, updated));
        if (persistentImportsEnabled()) { persistentImports.saveSnapshot(ownerUserId, importId, snapshot); persistentImports.updateStatus(ownerUserId, importId, updated.status(), snapshot.baseCommitSha()); }
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
        requireMutableImport(ownerUserId, importId);
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
        ImportResponse updated = new ImportResponse(current.id(), current.projectId(), current.baseBranch(),
                plan.approvable() ? "READY_FOR_REVIEW" : "BLOCKED", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, updated));
        if (persistentImportsEnabled()) { persistentImports.savePlan(ownerUserId, importId, plan); persistentImports.updateStatus(ownerUserId, importId, updated.status(), plan.baseCommitSha()); }
        return plan;
    }

    public Optional<ImmutableImportPlan> findImportPlan(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(plansByImport.get(importId));
    }

    public ImmutableImportPlan getImportPlan(UUID ownerUserId, UUID importId) {
        return findImportPlan(ownerUserId, importId)
                .orElseThrow(() -> ApiException.notFound("IMPORT_PLAN_NOT_FOUND", "The import plan was not found."));
    }

    public ImportPlanApproval approveImportPlan(UUID ownerUserId, UUID importId, String submittedDigest, String submittedSelectionDigest, String submittedCommitMessage) {
        requireMutableImport(ownerUserId, importId);
        ImmutableImportPlan plan = getImportPlan(ownerUserId, importId);
        if (submittedDigest == null || !submittedDigest.matches("[0-9a-f]{64}")) {
            throw ApiException.badRequest("INVALID_PLAN_DIGEST", "planDigestSha256 must be a lower-case SHA-256.");
        }
        if (!plan.planDigestSha256().equals(submittedDigest)) {
            throw ApiException.conflict("IMPORT_PLAN_DIGEST_MISMATCH",
                    "The submitted plan digest does not match the immutable plan currently stored for this import.");
        }
        ApprovedSelection selection = getImportSelection(ownerUserId, importId);
        if (submittedSelectionDigest == null || !submittedSelectionDigest.matches("[0-9a-f]{64}")) {
            throw ApiException.badRequest("INVALID_SELECTION_DIGEST",
                    "selectionDigestSha256 must be a lower-case SHA-256.");
        }
        if (!selection.selectionDigestSha256().equals(submittedSelectionDigest)) {
            throw ApiException.conflict("IMPORT_SELECTION_DIGEST_MISMATCH",
                    "The submitted selection digest does not match the immutable selection.");
        }

        final String commitMessage;
        try {
            commitMessage = CommitMessagePolicy.requireInteractive(submittedCommitMessage);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("INVALID_COMMIT_MESSAGE", e.getMessage());
        }
        ImportPlanApproval candidate = new ImportPlanApproval(importId, plan.id(), ownerUserId,
                plan.planDigestSha256(), selection.selectionDigestSha256(), commitMessage, Instant.now());
        ImportPlanApproval existing = approvalsByImport.putIfAbsent(importId, candidate);
        ImportPlanApproval approval = existing == null ? candidate : existing;
        if (!approval.planDigestSha256().equals(submittedDigest)
                || !approval.selectionDigestSha256().equals(submittedSelectionDigest)
                || !approval.commitMessage().equals(commitMessage)
                || !approval.approvedByUserId().equals(ownerUserId)) {
            throw ApiException.conflict("IMPORT_PLAN_ALREADY_APPROVED",
                    "This import was already approved with a different plan, selection or commit message.");
        }

        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        ImportResponse updated = new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "APPROVED", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, updated));
        if (persistentImportsEnabled()) { persistentImports.saveApproval(ownerUserId, importId, approval); persistentImports.updateStatus(ownerUserId, importId, updated.status(), plan.baseCommitSha()); }
        return approval;
    }

    /** Compatibility path for legacy/internal callers; interactive API must submit a commit message. */
    public ImportPlanApproval approveImportPlan(UUID ownerUserId, UUID importId, String submittedDigest, String submittedSelectionDigest) {
        return approveImportPlan(ownerUserId, importId, submittedDigest, submittedSelectionDigest,
                CommitMessagePolicy.defaultSuggestion(importId));
    }

    public Optional<ImportPlanApproval> findImportPlanApproval(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(approvalsByImport.get(importId));
    }

    public ApprovedSelection recordImportSelection(UUID ownerUserId, UUID importId, ApprovedSelection selection) {
        requireMutableImport(ownerUserId, importId);
        ImmutableImportPlan plan = getImportPlan(ownerUserId, importId);
        if (!selection.importId().equals(importId)
                || !selection.ownerUserId().equals(ownerUserId)
                || !selection.planId().equals(plan.id())
                || !selection.planDigestSha256().equals(plan.planDigestSha256())
                || !selection.baseCommitSha().equals(plan.baseCommitSha())) {
            throw ApiException.conflict("IMPORT_SELECTION_IDENTITY_MISMATCH",
                    "The selection does not match the current immutable import plan.");
        }
        ApprovedSelection existing = selectionsByImport.putIfAbsent(importId, selection);
        ApprovedSelection result = existing == null ? selection : existing;
        if (!result.selectionDigestSha256().equals(selection.selectionDigestSha256())
                || !result.ownerUserId().equals(ownerUserId)) {
            throw ApiException.conflict("IMPORT_SELECTION_IMMUTABLE",
                    "An immutable selection already exists for this import.");
        }
        if (persistentImportsEnabled()) persistentImports.saveSelection(ownerUserId, importId, result);
        return result;
    }

    public ApprovedSelection getImportSelection(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        ApprovedSelection selection = selectionsByImport.get(importId);
        if (selection == null) throw ApiException.notFound("IMPORT_SELECTION_NOT_FOUND",
                "No immutable selection has been stored for this import.");
        return selection;
    }

    public Optional<ApprovedSelection> findImportSelection(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(selectionsByImport.get(importId));
    }


    public DeliverySources deliverySources(UUID ownerUserId, UUID importId) {
        OwnedImport ownedImport = requireMutableImport(ownerUserId, importId);
        OwnedProject project = requireOwnedProject(ownerUserId, ownedImport.response.projectId());
        StoredUpload upload = uploadsByImport.get(importId);
        RepositorySnapshot snapshot = snapshotsByImport.get(importId);
        ImmutableImportPlan plan = plansByImport.get(importId);
        ImportPlanApproval approval = approvalsByImport.get(importId);
        ApprovedSelection selection = selectionsByImport.get(importId);
        if (upload == null || snapshot == null || plan == null || selection == null || approval == null) {
            throw ApiException.conflict("IMPORT_NOT_APPROVED",
                    "The source upload, repository snapshot, immutable plan, selection and exact approval are required.");
        }
        if (!approval.planDigestSha256().equals(plan.planDigestSha256())
                || !approval.selectionDigestSha256().equals(selection.selectionDigestSha256())
                || !selection.planDigestSha256().equals(plan.planDigestSha256())
                || !snapshot.baseCommitSha().equals(plan.baseCommitSha())
                || !upload.sha256().equals(plan.sourceUploadSha256())) {
            throw ApiException.conflict("IMPORT_IDENTITY_MISMATCH",
                    "The approved plan no longer matches the stored import sources.");
        }
        return new DeliverySources(project.response.githubInstallationId(), project.response.repositoryFullName(),
                upload, snapshot, plan, selection, approval);
    }

    public AppliedImportWorkspace recordAppliedWorkspace(UUID ownerUserId, UUID importId,
                                                          AppliedImportWorkspace workspace) {
        requireMutableImport(ownerUserId, importId);
        ImmutableImportPlan plan = getImportPlan(ownerUserId, importId);
        ApprovedSelection selection = getImportSelection(ownerUserId, importId);
        if (!workspace.importId().equals(importId)
                || !workspace.planDigestSha256().equals(plan.planDigestSha256())
                || !workspace.selectionDigestSha256().equals(selection.selectionDigestSha256())
                || !workspace.baseCommitSha().equals(plan.baseCommitSha())) {
            throw ApiException.conflict("WORKSPACE_IDENTITY_MISMATCH",
                    "The prepared workspace does not match the approved import plan.");
        }
        AppliedImportWorkspace existing = workspacesByImport.putIfAbsent(importId, workspace);
        AppliedImportWorkspace result = existing == null ? workspace : existing;
        if (!result.planDigestSha256().equals(workspace.planDigestSha256())
                || !result.selectionDigestSha256().equals(workspace.selectionDigestSha256())) {
            throw ApiException.conflict("WORKSPACE_ALREADY_EXISTS",
                    "A workspace already exists for a different plan identity.");
        }
        OwnedImport owned = imports.get(importId);
        ImportResponse current = owned.response;
        ImportResponse updated = new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "FILES_APPLIED", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, updated));
        if (persistentImportsEnabled()) persistentImports.updateStatus(ownerUserId, importId, updated.status(), workspace.baseCommitSha());
        return result;
    }

    public Optional<AppliedImportWorkspace> findAppliedWorkspace(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(workspacesByImport.get(importId));
    }


    public GitDeliveryResult recordGitDelivery(UUID ownerUserId, UUID importId, GitDeliveryResult delivery) {
        OwnedImport owned = requireMutableImport(ownerUserId, importId);
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
        recordWorkCommit(ownerUserId, current.projectId(), importId, delivery);
        if (persistentImportsEnabled()) { persistentImports.saveDelivery(ownerUserId, importId, result); persistentImports.updateStatus(ownerUserId, importId, "PUSHED", delivery.baseCommitSha()); }
        return result;
    }

    public Optional<GitDeliveryResult> findGitDelivery(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(deliveriesByImport.get(importId));
    }

    public ImportResponse cancelImport(UUID ownerUserId, UUID importId) {
        OwnedImport owned = requireOwnedImport(ownerUserId, importId);
        if (deliveriesByImport.containsKey(importId) || Set.of("PUSHED", "PULL_REQUEST_CREATED").contains(owned.response.status())) {
            throw ApiException.conflict("IMPORT_ALREADY_DELIVERED",
                    "The import has already been delivered to GitHub and can no longer be cancelled.");
        }
        if ("CANCELLED".equals(owned.response.status())) return owned.response;

        workspacesByImport.remove(importId);
        ImportResponse current = owned.response;
        ImportResponse cancelled = new ImportResponse(current.id(), current.projectId(), current.baseBranch(),
                "CANCELLED", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, cancelled));
        if (persistentImportsEnabled()) persistentImports.updateStatus(ownerUserId, importId, "CANCELLED", null);
        return cancelled;
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
        ImportResponse updated = new ImportResponse(current.id(), current.projectId(), current.baseBranch(), "PULL_REQUEST_CREATED", current.createdAt());
        imports.put(importId, new OwnedImport(ownerUserId, updated));
        if (persistentImportsEnabled()) persistentImports.updateStatus(ownerUserId, importId, updated.status(), delivery.baseCommitSha());
        return stored;
    }

    public Optional<PullRequestResult> findPullRequest(UUID ownerUserId, UUID importId) {
        requireOwnedImport(ownerUserId, importId);
        return Optional.ofNullable(pullRequestsByImport.get(importId));
    }

    private WorkSession getOrCreateWork(UUID ownerUserId, UUID projectId, String baseBranch) {
        if (persistentProjects.enabled()) return persistentWork.getOrCreateActive(ownerUserId, projectId, baseBranch);
        return workByProject.compute(projectId, (id, existing) -> {
            if (existing != null && "ACTIVE".equals(existing.status())) return existing;
            UUID workId = UUID.randomUUID(); Instant now = Instant.now();
            return new WorkSession(workId, projectId, ownerUserId, baseBranch, "zip-github/work-" + workId, "ACTIVE",
                    null, null, null, null, null, null, now, now);
        });
    }

    private void recordWorkCommit(UUID ownerUserId, UUID projectId, UUID importId, GitDeliveryResult delivery) {
        if (persistentProjects.enabled()) {
            persistentWork.recordCommit(ownerUserId, projectId, importId, delivery.baseCommitSha(), delivery.commitSha(), delivery.planDigestSha256());
            return;
        }
        WorkSession work = requireActiveWork(ownerUserId, projectId);
        workByProject.put(projectId, new WorkSession(work.id(), work.projectId(), work.ownerUserId(), work.baseBranch(), work.branchName(),
                "ACTIVE", delivery.commitSha(), work.baseCommitSha() == null ? delivery.baseCommitSha() : work.baseCommitSha(),
                importId, delivery.planDigestSha256(), null, null, work.createdAt(), Instant.now()));
    }

    public List<StoredUpload> expiredUploads(Instant now) {
        Map<UUID, StoredUpload> candidates = new LinkedHashMap<>();
        if (persistentImportsEnabled()) {
            for (StoredUpload upload : persistentImports.listExpiredTerminalUploads(now)) {
                candidates.put(upload.id(), upload);
                uploadsByImport.putIfAbsent(upload.importId(), upload);
            }
        }
        uploadsByImport.values().stream()
                .filter(upload -> !upload.retentionDeadline().isAfter(now))
                .filter(upload -> {
                    OwnedImport item = imports.get(upload.importId());
                    return item != null && Set.of("PUSHED", "PULL_REQUEST_CREATED", "CANCELLED").contains(item.response.status());
                })
                .forEach(upload -> candidates.put(upload.id(), upload));
        return List.copyOf(candidates.values());
    }

    public boolean removeExpiredUpload(UUID importId, UUID uploadId, Instant now) {
        StoredUpload current = uploadsByImport.get(importId);
        if (current == null || !current.id().equals(uploadId) || current.retentionDeadline().isAfter(now)) return false;
        OwnedImport item = imports.get(importId);
        if (item != null && !Set.of("PUSHED", "PULL_REQUEST_CREATED", "CANCELLED").contains(item.response.status())) return false;
        if (item == null && !persistentImportsEnabled()) return false;
        boolean removed = uploadsByImport.remove(importId, current);
        if (persistentImportsEnabled()) persistentImports.clearUpload(current.ownerUserId(), importId);
        return removed || persistentImportsEnabled();
    }

    private OwnedImport requireMutableImport(UUID ownerUserId, UUID importId) {
        OwnedImport item = requireOwnedImport(ownerUserId, importId);
        if ("CANCELLED".equals(item.response.status())) {
            throw ApiException.conflict("IMPORT_CANCELLED", "The import has been cancelled and cannot be changed.");
        }
        return item;
    }

    private boolean persistentImportsEnabled() {
        return persistentImports != null && persistentImports.enabled();
    }

    private OwnedImport requireOwnedImport(UUID ownerUserId, UUID importId) {
        OwnedImport item = imports.get(importId);
        if (item == null && persistentImportsEnabled()) {
            persistentImports.find(ownerUserId, importId).ifPresent(this::hydrate);
            item = imports.get(importId);
        }
        if (item == null || !item.ownerUserId.equals(ownerUserId)) throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        return item;
    }

    private void hydrate(ImportResumePersistenceStore.ResumeState state) {
        UUID importId = state.response().id();
        imports.putIfAbsent(importId, new OwnedImport(state.ownerUserId(), state.response()));
        if (state.upload() != null) uploadsByImport.putIfAbsent(importId, state.upload());
        if (state.snapshot() != null) snapshotsByImport.putIfAbsent(importId, state.snapshot());
        if (state.plan() != null) plansByImport.putIfAbsent(importId, state.plan());
        if (state.selection() != null) selectionsByImport.putIfAbsent(importId, state.selection());
        if (state.approval() != null) approvalsByImport.putIfAbsent(importId, state.approval());
        if (state.identity() != null) gitIdentitiesByImport.putIfAbsent(importId, state.identity());
        auditMetadataByImport.putIfAbsent(importId, state.auditMetadata());
        if (state.delivery() != null) deliveriesByImport.putIfAbsent(importId, state.delivery());
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
                                  RepositorySnapshot snapshot, ImmutableImportPlan plan, ApprovedSelection selection,
                                  ImportPlanApproval approval) {}
    public record ProjectWorkSource(long githubInstallationId, String repositoryFullName, WorkSession work) {}

    private record StoredUploadPromotionKey(UUID ownerUserId, UUID projectId, String idempotencyKey) {}
    private record StoredUploadPromotion(UUID ownerUserId, UUID projectId, UUID importId, UUID artifactId,
                                         String artifactSha256, String idempotencyKey) {}
    private record OwnedProject(UUID ownerUserId, ProjectResponse response) {}
    private record OwnedImport(UUID ownerUserId, ImportResponse response) {}
}
