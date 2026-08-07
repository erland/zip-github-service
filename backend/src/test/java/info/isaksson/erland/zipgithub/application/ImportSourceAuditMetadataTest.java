package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.CreateImportRequest;
import info.isaksson.erland.zipgithub.api.dto.CreateProjectRequest;
import info.isaksson.erland.zipgithub.domain.model.ImportAuditMetadata;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportSourceAuditMetadataTest {
    @Test
    void browserImportDefaultsToWebUploadAndHistoryExposesNonSecretSource() {
        ProjectApplicationService service = service();
        UUID owner = UUID.randomUUID();
        var project = service.createProject(owner, "token", new CreateProjectRequest("Example", 10L, 20L, "main"));
        var imported = service.createImport(owner, project.id(), new CreateImportRequest(null, null),
                "Erland", "erland@example.invalid");

        ImportAuditMetadata audit = service.importAuditMetadata(owner, imported.id());
        assertEquals(ImportSource.WEB_UPLOAD, audit.source());
        assertNull(audit.sourceReference());
        var history = service.listProjectImports(owner, project.id());
        assertEquals("WEB_UPLOAD", history.getFirst().sourceType());
        assertNull(history.getFirst().sourceReference());
    }

    @Test
    void auditReferenceIsBoundedAndBlankNormalizesToNull() {
        assertNull(new ImportAuditMetadata(ImportSource.STAGING_IMPORT, "  ").sourceReference());
        assertThrows(IllegalArgumentException.class, () ->
                new ImportAuditMetadata(ImportSource.STAGING_IMPORT, "x".repeat(256)));
    }

    private static ProjectApplicationService service() {
        ProjectApplicationService service = new ProjectApplicationService();
        service.persistentProjects = mock(ProjectPersistenceStore.class);
        service.persistentWork = mock(WorkPersistenceStore.class);
        service.githubConfiguration = mock(GitHubProjectConfigurationService.class);
        when(service.persistentProjects.enabled()).thenReturn(false);
        when(service.githubConfiguration.verify(anyString(), anyLong(), anyLong(), anyString()))
                .thenReturn(new GitHubProjectConfigurationService.VerifiedRepository(
                        10L, "erland", "selected", 20L, "erland/example", true, "main"));
        return service;
    }
}
