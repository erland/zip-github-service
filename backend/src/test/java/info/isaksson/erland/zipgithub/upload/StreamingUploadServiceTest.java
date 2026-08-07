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
    private static final Instant NOW = Instant.parse("2026-08-07T16:00:00Z");

    @Test
    void webAdapterPreservesOwnershipAndNeutralArtifactMetadata() throws Exception {
        byte[] content = "PK\u0003\u0004test archive".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var service = service(1024);
        UUID owner = UUID.randomUUID();
        UUID importId = UUID.randomUUID();

        StoredUpload result = service.store(owner, importId, "project.zip", content.length,
                new ByteArrayInputStream(content));

        assertEquals(owner, result.ownerUserId());
        assertEquals(importId, result.importId());
        assertEquals(content.length, result.sizeBytes());
        assertEquals("ed7fa2c99aa61ee527233f2517ea1e352b6fb4b65d98dab593aac7223d8f75b8", result.sha256());
        assertEquals(NOW.plus(Duration.ofHours(24)), result.retentionDeadline());
        assertArrayEquals(content, Files.readAllBytes(result.storagePath()));
        assertEquals(importId.toString(), result.storagePath().getParent().getFileName().toString());
    }

    private StreamingUploadService service(long maximumBytes) {
        var ingestion = new ZipIngestionService(new UploadStorage(temporaryDirectory), maximumBytes,
                Duration.ofHours(24), Clock.fixed(NOW, ZoneOffset.UTC));
        return new StreamingUploadService(ingestion);
    }
}
