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

class StreamingUploadServiceTest {
    @TempDir Path temporaryDirectory;
    private static final Instant NOW = Instant.parse("2026-08-06T14:00:00Z");

    @Test
    void streamsContentCalculatesDigestAndRetention() throws Exception {
        byte[] content = "PK\u0003\u0004test archive".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var service = service(1024);
        UUID owner = UUID.randomUUID();
        UUID importId = UUID.randomUUID();

        StoredUpload result = service.store(owner, importId, "project.zip", content.length,
                new ByteArrayInputStream(content));

        assertEquals(content.length, result.sizeBytes());
        assertEquals("ed7fa2c99aa61ee527233f2517ea1e352b6fb4b65d98dab593aac7223d8f75b8", result.sha256());
        assertEquals(NOW.plus(Duration.ofHours(24)), result.retentionDeadline());
        assertArrayEquals(content, Files.readAllBytes(result.storagePath()));
        assertTrue(result.storagePath().startsWith(temporaryDirectory.toAbsolutePath()));
        assertFalse(Files.exists(result.storagePath().resolveSibling(result.id() + ".part")));
    }

    @Test
    void rejectsDeclaredLengthBeforeCreatingAFile() throws Exception {
        var service = service(8);
        assertThrows(UploadTooLargeException.class, () -> service.store(UUID.randomUUID(), UUID.randomUUID(),
                "project.zip", 9, new ByteArrayInputStream(new byte[1])));
        try (var stream = Files.walk(temporaryDirectory)) {
            assertEquals(1, stream.count());
        }
    }

    @Test
    void enforcesActualStreamingLimitAndRemovesPartialFile() throws Exception {
        var service = service(8);
        assertThrows(UploadTooLargeException.class, () -> service.store(UUID.randomUUID(), UUID.randomUUID(),
                "project.zip", -1, new ByteArrayInputStream(new byte[9])));
        try (var stream = Files.walk(temporaryDirectory)) {
            assertTrue(stream.noneMatch(path -> path.getFileName().toString().endsWith(".part")));
        }
    }

    @Test
    void rejectsEmptyOrPathLikeFilename() {
        var service = service(1024);
        assertThrows(IllegalArgumentException.class, () -> service.store(UUID.randomUUID(), UUID.randomUUID(),
                "../project.zip", 1, new ByteArrayInputStream(new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> service.store(UUID.randomUUID(), UUID.randomUUID(),
                "project.txt", 1, new ByteArrayInputStream(new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> service.store(UUID.randomUUID(), UUID.randomUUID(),
                "project.zip", 0, new ByteArrayInputStream(new byte[0])));
    }

    private StreamingUploadService service(long maximumBytes) {
        return new StreamingUploadService(new UploadStorage(temporaryDirectory), maximumBytes,
                Duration.ofHours(24), Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
