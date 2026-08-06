package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.*;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.upload.StoredUpload;
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
    @Inject GitHubProjectConfigurationService githubConfiguration;

    public List<ProjectResponse> listProjects(UUID ownerUserId) {
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
        OwnedImport item = imports.get(importId);
        if (item == null || !item.ownerUserId.equals(ownerUserId)) throw ApiException.notFound("IMPORT_NOT_FOUND", "The import was not found.");
        return item.response;
    }


    public void requireOwnedImport(UUID ownerUserId, UUID importId) {
        getImport(ownerUserId, importId);
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

    private OwnedProject requireOwnedProject(UUID ownerUserId, UUID projectId) {
        OwnedProject item = projects.get(projectId);
        if (item == null || !item.ownerUserId.equals(ownerUserId)) throw ApiException.notFound("PROJECT_NOT_FOUND", "The project was not found.");
        return item;
    }

    private void ensureUniqueName(UUID ownerUserId, String name, UUID excludedId) {
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

    private record OwnedProject(UUID ownerUserId, ProjectResponse response) {}
    private record OwnedImport(UUID ownerUserId, ImportResponse response) {}
}
