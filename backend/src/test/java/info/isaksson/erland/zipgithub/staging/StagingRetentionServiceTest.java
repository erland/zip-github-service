package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StagingRetentionServiceTest {
    @TempDir Path temp;

    @Test
    void deletesOnlyStoreClaimedTerminalCandidatesAndMarksPhysicalCleanup() throws Exception {
        Instant now = Instant.parse("2026-08-08T07:00:00Z");
        Path zip = temp.resolve("staging").resolve("project.zip");
        Files.createDirectories(zip.getParent());
        Files.writeString(zip, "zip");
        UUID stagingId = UUID.randomUUID();
        var artifact = new StoredUploadArtifact(UUID.randomUUID(), "project.zip", 3, "a".repeat(64), zip,
                now.minusSeconds(7200), now.minusSeconds(60), Map.of());
        var store = mock(StagingImportPersistenceStore.class);
        when(store.claimCleanupCandidates(now, 100)).thenReturn(List.of(new StagingImportPersistenceStore.CleanupCandidate(stagingId, artifact)));
        var service = new StagingRetentionService(Clock.fixed(now, ZoneOffset.UTC));
        service.store = store;
        service.batchSize = 100;

        var result = service.cleanupExpired();

        assertEquals(1, result.deleted());
        assertEquals(0, result.failed());
        assertFalse(Files.exists(zip));
        verify(store).markArtifactDeleted(stagingId, now);
    }

    @Test
    void failedPhysicalDeleteRemainsRetryable() throws Exception {
        Instant now = Instant.parse("2026-08-08T07:00:00Z");
        Path directoryInsteadOfFile = temp.resolve("not-a-file");
        Files.createDirectories(directoryInsteadOfFile.resolve("child"));
        UUID stagingId = UUID.randomUUID();
        var artifact = new StoredUploadArtifact(UUID.randomUUID(), "project.zip", 3, "a".repeat(64), directoryInsteadOfFile,
                now.minusSeconds(7200), now.minusSeconds(60), Map.of());
        var store = mock(StagingImportPersistenceStore.class);
        when(store.claimCleanupCandidates(now, 100)).thenReturn(List.of(new StagingImportPersistenceStore.CleanupCandidate(stagingId, artifact)));
        var service = new StagingRetentionService(Clock.fixed(now, ZoneOffset.UTC));
        service.store = store; service.batchSize = 100;

        var result = service.cleanupExpired();

        assertEquals(0, result.deleted());
        assertEquals(1, result.failed());
        verify(store, never()).markArtifactDeleted(any(), any());
    }
}
