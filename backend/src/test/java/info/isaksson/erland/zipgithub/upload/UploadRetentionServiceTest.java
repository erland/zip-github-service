package info.isaksson.erland.zipgithub.upload;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UploadRetentionServiceTest {
    @Test
    void storedUploadCarriesAnExplicitRetentionDeadline() throws Exception {
        var file = Files.createTempFile("retention-test", ".zip");
        try {
            Instant created = Instant.parse("2026-08-06T10:00:00Z");
            Instant deadline = created.plusSeconds(3600);
            var upload = new StoredUpload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "project.zip", 12, "0".repeat(64), file, created, deadline);
            assertEquals(deadline, upload.retentionDeadline());
            assertFalse(upload.retentionDeadline().isAfter(deadline));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
