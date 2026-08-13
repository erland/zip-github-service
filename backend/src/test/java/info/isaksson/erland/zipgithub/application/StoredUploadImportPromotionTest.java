package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.CreateImportRequest;
import info.isaksson.erland.zipgithub.api.dto.CreateProjectRequest;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StoredUploadImportPromotionTest {
    @TempDir Path temp;

    @Test
    void promotesStoredArtifactWithoutRestreamAndRetriesIdempotently() {
        Fixture fixture = fixture();
        StoredUploadArtifact artifact = artifact("one.zip", "a".repeat(64));

        var first = fixture.service.createImportFromStoredUpload(
                fixture.owner, fixture.projectId, new CreateImportRequest(null, null, null),
                "Erland", "erland@example.invalid", artifact, "staging-123");
        var retry = fixture.service.createImportFromStoredUpload(
                fixture.owner, fixture.projectId, new CreateImportRequest(null, null, null),
                "Erland", "erland@example.invalid", artifact, "staging-123");

        assertEquals(first.importSession().id(), retry.importSession().id());
        assertEquals(artifact.id(), first.sourceUpload().id());
        assertEquals(artifact.sha256(), first.sourceUpload().sha256());
        var expired = fixture.service.expiredUploads(Instant.parse("2100-01-01T00:00:00Z"));
        assertTrue(expired.isEmpty(),
                "an active resumable import must remain protected from source-ZIP cleanup until delivery is terminal");
        assertEquals(1, fixture.service.listProjectImports(fixture.owner, fixture.projectId).size());
        var audit = fixture.service.importAuditMetadata(fixture.owner, first.importSession().id());
        assertEquals(ImportSource.STORED_UPLOAD, audit.source());
        assertEquals("stored-upload:" + artifact.id(), audit.sourceReference());
    }

    @Test
    void rejectsReusingIdempotencyKeyForDifferentArtifact() {
        Fixture fixture = fixture();
        fixture.service.createImportFromStoredUpload(
                fixture.owner, fixture.projectId, null, "Erland", "erland@example.invalid",
                artifact("one.zip", "a".repeat(64)), "staging-123");

        ApiException error = assertThrows(ApiException.class, () -> fixture.service.createImportFromStoredUpload(
                fixture.owner, fixture.projectId, null, "Erland", "erland@example.invalid",
                artifact("two.zip", "b".repeat(64)), "staging-123"));
        assertEquals(409, error.status());
        assertEquals("STORED_UPLOAD_PROMOTION_KEY_REUSED", error.code());
    }

    @Test
    void sameStoredArtifactCannotCreateDuplicateImport() {
        Fixture fixture = fixture();
        StoredUploadArtifact artifact = artifact("one.zip", "a".repeat(64));
        var first = fixture.service.createImportFromStoredUpload(
                fixture.owner, fixture.projectId, null, "Erland", "erland@example.invalid", artifact, "first-key");
        var retryWithNewKey = fixture.service.createImportFromStoredUpload(
                fixture.owner, fixture.projectId, null, "Erland", "erland@example.invalid", artifact, "retry-key");

        assertEquals(first.importSession().id(), retryWithNewKey.importSession().id());
        assertEquals(1, fixture.service.listProjectImports(fixture.owner, fixture.projectId).size());
    }

    private StoredUploadArtifact artifact(String filename, String sha) {
        Instant now = Instant.parse("2026-08-07T16:00:00Z");
        return new StoredUploadArtifact(UUID.randomUUID(), filename, 123, sha,
                temp.resolve(filename), now, now.plusSeconds(3600));
    }

    private static Fixture fixture() {
        ProjectApplicationService service = new ProjectApplicationService();
        service.persistentProjects = mock(ProjectPersistenceStore.class);
        service.persistentWork = mock(WorkPersistenceStore.class);
        service.githubConfiguration = mock(GitHubProjectConfigurationService.class);
        when(service.persistentProjects.enabled()).thenReturn(false);
        when(service.githubConfiguration.verify(anyString(), anyLong(), anyLong(), anyString()))
                .thenReturn(new GitHubProjectConfigurationService.VerifiedRepository(
                        10L, "erland", "selected", 20L, "erland/example", true, "main"));
        UUID owner = UUID.randomUUID();
        var project = service.createProject(owner, "token",
                new CreateProjectRequest("Example", 10L, 20L, "main"));
        return new Fixture(service, owner, project.id());
    }

    private record Fixture(ProjectApplicationService service, UUID owner, UUID projectId) { }
}
