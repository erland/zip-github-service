package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.api.dto.CreateImportRequest;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.application.ProjectApplicationService;
import info.isaksson.erland.zipgithub.application.StoredUploadImportResult;
import info.isaksson.erland.zipgithub.domain.model.ImportAuditMetadata;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Promotes an authenticated claimed staging upload exactly once into the ordinary import pipeline. */
@ApplicationScoped
public class StagingPromotionService {
    private final StagingImportPersistenceStore staging;
    private final ProjectApplicationService projects;
    private final Clock clock;

    @Inject
    public StagingPromotionService(StagingImportPersistenceStore staging, ProjectApplicationService projects) {
        this(staging, projects, Clock.systemUTC());
    }

    StagingPromotionService(StagingImportPersistenceStore staging, ProjectApplicationService projects, Clock clock) {
        this.staging = Objects.requireNonNull(staging, "staging");
        this.projects = Objects.requireNonNull(projects, "projects");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Promotion promote(UUID stagingId, UUID owner, UUID projectId, String gitName, String gitEmail) {
        if (stagingId == null || projectId == null) throw ApiException.badRequest("VALIDATION_ERROR", "stagingId and projectId are required.");
        projects.getProject(owner, projectId); // ownership check before any ordinary Import is created
        String sourceReference = "staging-import:" + stagingId;
        Instant now = clock.instant();

        var outcome = staging.promoteWithLock(stagingId, owner, now, item -> {
            var recovered = projects.findImportBySourceReference(owner, ImportSource.STAGING_IMPORT, sourceReference);
            StoredUploadImportResult result;
            if (recovered.isPresent()) {
                if (!recovered.get().projectId().equals(projectId))
                    throw ApiException.conflict("STAGING_ALREADY_PROMOTED", "The staging import is already bound to another project.");
                result = projects.ensureStoredUploadAttached(owner, recovered.get().id(), item.artifact());
            } else {
                result = projects.createImportFromStoredUpload(owner, projectId, new CreateImportRequest(null, null), gitName, gitEmail,
                        item.artifact(), sourceReference,
                        new ImportAuditMetadata(ImportSource.STAGING_IMPORT, sourceReference));
            }
            return result.importSession().id();
        });

        return switch (outcome.result()) {
            case PROMOTED -> new Promotion(outcome.importId(), projectId, false);
            case ALREADY_PROMOTED -> {
                var existing = projects.getImport(owner, outcome.importId());
                if (!existing.projectId().equals(projectId))
                    throw ApiException.conflict("STAGING_ALREADY_PROMOTED", "The staging import was already promoted to another project.");
                yield new Promotion(existing.id(), existing.projectId(), true);
            }
            case EXPIRED -> throw ApiException.gone("STAGING_PROMOTION_EXPIRED", "The claimed staging import expired before promotion. Upload the ZIP again.");
            case NOT_CLAIMED -> throw ApiException.conflict("STAGING_NOT_CLAIMED", "The staging import must be claimed before promotion.");
            case NOT_AVAILABLE -> throw ApiException.notFound("STAGING_IMPORT_NOT_FOUND", "The staging import was not found.");
        };
    }

    public record Promotion(UUID importId, UUID projectId, boolean alreadyPromoted) { }
}
