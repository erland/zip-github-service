package info.isaksson.erland.zipgithub.staging;

import info.isaksson.erland.zipgithub.persistence.StagingImportPersistenceStore;
import info.isaksson.erland.zipgithub.upload.ZipIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StagingUploadServiceTest {
    @TempDir Path temp;

    @Test
    void createsAvailableStagingWithOpaqueClaimUrlAndPersistsOnlyHash() {
        byte[] bytes = "PK-test-zip-bytes".getBytes(StandardCharsets.UTF_8);
        var ingestion = new ZipIngestionService(temp.toString(), 1024, 24);
        var store = mock(StagingImportPersistenceStore.class);
        var service = new StagingUploadService(ingestion, store, new StagingClaimTokenFactory(), Duration.ofHours(1),
                "https://zip-github.example/", Clock.fixed(Instant.parse("2026-08-08T05:00:00Z"), ZoneOffset.UTC));

        var created = service.create("project.zip", bytes.length, new ByteArrayInputStream(bytes));

        assertEquals("project.zip", created.originalFilename());
        assertEquals(bytes.length, created.sizeBytes());
        assertEquals(Instant.parse("2026-08-08T06:00:00Z"), created.expiresAt());
        assertTrue(created.claimUrl().startsWith("https://zip-github.example/staging/claim#token="));
        String rawToken = created.claimUrl().substring(created.claimUrl().indexOf("#token=") + 7);
        assertFalse(rawToken.isBlank());
        verify(store).insertWithinLimits(argThat(staging ->
                staging.id().equals(created.stagingId())
                        && staging.claimTokenSha256().equals(StagingSecretCodec.digestHex(rawToken))
                        && !staging.claimTokenSha256().contains(rawToken)), eq(100L), eq(1073741824L));
    }

    @Test
    void durableCapacityRejectionRemovesJustStoredBytes() throws Exception {
        byte[] bytes = "PK-test-zip-bytes".getBytes(StandardCharsets.UTF_8);
        var ingestion = new ZipIngestionService(temp.toString(), 1024, 24);
        var store = mock(StagingImportPersistenceStore.class);
        doThrow(new StagingCapacityExceededException("full")).when(store).insertWithinLimits(any(), eq(1L), eq(1024L));
        var service = new StagingUploadService(ingestion, store, new StagingClaimTokenFactory(), null, Duration.ofHours(1),
                1, 1024, "https://zip-github.example", Clock.systemUTC());

        assertThrows(StagingCapacityExceededException.class,
                () -> service.create("project.zip", bytes.length, new ByteArrayInputStream(bytes)));
        try (var files = Files.walk(temp)) {
            assertEquals(0, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void persistenceFailureRemovesStoredBytes() throws Exception {
        byte[] bytes = "PK-test-zip-bytes".getBytes(StandardCharsets.UTF_8);
        var ingestion = new ZipIngestionService(temp.toString(), 1024, 24);
        var store = mock(StagingImportPersistenceStore.class);
        doThrow(new IllegalStateException("db unavailable")).when(store).insertWithinLimits(any(), anyLong(), anyLong());
        var service = new StagingUploadService(ingestion, store, new StagingClaimTokenFactory(), Duration.ofHours(1),
                "https://zip-github.example", Clock.systemUTC());

        assertThrows(IllegalStateException.class,
                () -> service.create("project.zip", bytes.length, new ByteArrayInputStream(bytes)));
        try (var files = Files.walk(temp)) {
            assertEquals(0, files.filter(Files::isRegularFile).count());
        }
    }
}
