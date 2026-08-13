package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.CreateImportRequest;
import info.isaksson.erland.zipgithub.api.dto.CreateProjectRequest;
import info.isaksson.erland.zipgithub.api.error.ApiException;
import info.isaksson.erland.zipgithub.persistence.ProjectPersistenceStore;
import info.isaksson.erland.zipgithub.persistence.WorkPersistenceStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Step 7.23: one Work may have at most one active import at a time. */
class ActiveImportInvariantTest {
    @Test
    void blocksParallelImportUntilCurrentImportIsCancelled() {
        ProjectApplicationService service = service();
        UUID owner = UUID.randomUUID();
        var project = service.createProject(owner, "token",
                new CreateProjectRequest("Example", 10L, 20L, "main"));

        var first = service.createImport(owner, project.id(), new CreateImportRequest(null, null, null),
                "Erland", "erland@example.invalid");

        ApiException conflict = assertThrows(ApiException.class, () -> service.createImport(owner, project.id(),
                new CreateImportRequest(null, null, null), "Erland", "erland@example.invalid"));
        assertEquals(409, conflict.status());
        assertEquals("ACTIVE_IMPORT_EXISTS", conflict.code());

        service.cancelImport(owner, first.id());
        var second = service.createImport(owner, project.id(), new CreateImportRequest(null, null, null),
                "Erland", "erland@example.invalid");
        assertNotEquals(first.id(), second.id());
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
