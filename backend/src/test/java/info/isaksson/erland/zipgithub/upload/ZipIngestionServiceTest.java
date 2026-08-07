package info.isaksson.erland.zipgithub.upload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ZipIngestionServiceTest {
    @TempDir Path temporaryDirectory;
    private static final Instant NOW = Instant.parse("2026-08-07T16:00:00Z");

    @Test
    void storesNeutralArtifactWithoutUserOrImportIdentity() throws Exception {
        byte[] content = "PK\u0003\u0004test archive".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        UUID scope = UUID.randomUUID();
        var service = service(1024);

        StoredUploadArtifact result = service.store(scope, "project.zip", content.length,
                new ByteArrayInputStream(content));

        assertEquals(content.length, result.sizeBytes());
        assertEquals("ed7fa2c99aa61ee527233f2517ea1e352b6fb4b65d98dab593aac7223d8f75b8", result.sha256());
        assertEquals(NOW.plus(Duration.ofHours(24)), result.retentionDeadline());
        assertEquals(scope.toString(), result.storagePath().getParent().getFileName().toString());
        assertArrayEquals(content, Files.readAllBytes(result.storagePath()));
        assertFalse(Files.exists(result.storagePath().resolveSibling(result.id() + ".part")));
    }

    @Test
    void rejectsDeclaredLengthBeforeCreatingAFile() throws Exception {
        var service = service(8);
        assertThrows(UploadTooLargeException.class, () -> service.store(UUID.randomUUID(), "project.zip", 9,
                new ByteArrayInputStream(new byte[1])));
        try (var stream = Files.walk(temporaryDirectory)) {
            assertEquals(1, stream.count());
        }
    }

    @Test
    void enforcesActualStreamingLimitAndRemovesPartialFile() throws Exception {
        var service = service(8);
        assertThrows(UploadTooLargeException.class, () -> service.store(UUID.randomUUID(), "project.zip", -1,
                new ByteArrayInputStream(new byte[9])));
        try (var stream = Files.walk(temporaryDirectory)) {
            assertTrue(stream.noneMatch(path -> path.getFileName().toString().endsWith(".part")));
        }
    }

    @Test
    void rejectsEmptyAndUnsafeFilename() {
        var service = service(1024);
        UUID scope = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.store(scope, "../project.zip", 1,
                new ByteArrayInputStream(new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> service.store(scope, "project.txt", 1,
                new ByteArrayInputStream(new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> service.store(scope, "project.zip", 0,
                new ByteArrayInputStream(new byte[0])));
    }

    private ZipIngestionService service(long maximumBytes) {
        return new ZipIngestionService(new UploadStorage(temporaryDirectory), maximumBytes,
                Duration.ofHours(24), Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
