package info.isaksson.erland.zipbuildserver.application;

import info.isaksson.erland.zipbuildserver.api.packageupload.PackageResponse;
import info.isaksson.erland.zipbuildserver.application.packageupload.ArchiveValidationResult;
import info.isaksson.erland.zipbuildserver.application.packageupload.ArchiveValidationService;
import info.isaksson.erland.zipbuildserver.application.project.ProjectDetectionService;
import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;
import info.isaksson.erland.zipbuildserver.domain.model.SourcePackageStatus;
import info.isaksson.erland.zipbuildserver.domain.model.project.ProjectDetectionSummary;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.SourcePackageEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity.VerificationSessionEntity;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.SourcePackageRepository;
import info.isaksson.erland.zipbuildserver.infrastructure.persistence.repository.VerificationSessionRepository;
import info.isaksson.erland.zipbuildserver.storage.PackageStorageService;
import info.isaksson.erland.zipbuildserver.storage.PackageStorageService.StoredPackage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class SourcePackageService {
    private final VerificationSessionRepository sessionRepository;
    private final SourcePackageRepository packageRepository;
    private final PackageStorageService storageService;
    private final ArchiveValidationService archiveValidationService;
    private final ProjectDetectionService projectDetectionService;

    public SourcePackageService(
            VerificationSessionRepository sessionRepository,
            SourcePackageRepository packageRepository,
            PackageStorageService storageService,
            ArchiveValidationService archiveValidationService,
            ProjectDetectionService projectDetectionService) {
        this.sessionRepository = sessionRepository;
        this.packageRepository = packageRepository;
        this.storageService = storageService;
        this.archiveValidationService = archiveValidationService;
        this.projectDetectionService = projectDetectionService;
    }

    @Transactional
    public PackageResponse submit(UUID sessionId, Path uploadedFile, String originalFilename) {
        VerificationSessionEntity session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new NotFoundException("Session was not found: " + sessionId));
        if (session.status == SessionStatus.CLOSED) {
            throw new BadRequestException("Cannot submit a package to a closed session.");
        }
        if (uploadedFile == null) {
            throw new BadRequestException("Multipart field 'file' is required.");
        }

        UUID packageId = UUID.randomUUID();
        StoredPackage stored = storageService.store(uploadedFile, packageId);

        SourcePackageEntity entity = new SourcePackageEntity();
        entity.id = packageId;
        entity.sessionId = sessionId;
        entity.originalFilename = normalizeFilename(originalFilename);
        entity.checksumSha256 = stored.checksumSha256();
        entity.compressedSizeBytes = stored.compressedSizeBytes();
        entity.storageReference = stored.storageReference();
        entity.createdAt = OffsetDateTime.now();

        try {
            ArchiveValidationResult validation = archiveValidationService.validate(Path.of(stored.storageReference()), stored.compressedSizeBytes());
            entity.status = SourcePackageStatus.ACCEPTED;
            entity.extractedSizeBytes = validation.extractedSizeBytes();
            entity.fileCount = validation.fileCount();
            entity.topLevelEntries = validation.topLevelEntries();
            entity.rejectionReason = null;
        } catch (PackageValidationException exception) {
            entity.status = SourcePackageStatus.REJECTED;
            entity.rejectionReason = exception.getMessage();
        }

        packageRepository.persist(entity);
        return toResponse(entity);
    }

    public PackageResponse get(UUID packageId) {
        SourcePackageEntity entity = packageRepository.findByIdOptional(packageId)
                .orElseThrow(() -> new NotFoundException("Package was not found: " + packageId));
        return toResponse(entity);
    }

    private PackageResponse toResponse(SourcePackageEntity entity) {
        ProjectDetectionSummary detection = detectProjects(entity);
        return new PackageResponse(
                entity.id,
                entity.sessionId,
                entity.originalFilename,
                entity.checksumSha256,
                entity.compressedSizeBytes,
                entity.extractedSizeBytes,
                entity.fileCount,
                entity.topLevelEntries,
                entity.storageReference,
                entity.status,
                entity.rejectionReason,
                entity.createdAt,
                detection);
    }

    private ProjectDetectionSummary detectProjects(SourcePackageEntity entity) {
        if (entity.status != SourcePackageStatus.ACCEPTED) {
            return ProjectDetectionSummary.unsupported("Project detection was skipped because the package was rejected.");
        }
        return projectDetectionService.detect(Path.of(entity.storageReference));
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        return Path.of(originalFilename).getFileName().toString();
    }
}
