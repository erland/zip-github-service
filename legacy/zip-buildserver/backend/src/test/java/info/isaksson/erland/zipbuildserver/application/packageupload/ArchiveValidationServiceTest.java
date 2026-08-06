package info.isaksson.erland.zipbuildserver.application.packageupload;

import info.isaksson.erland.zipbuildserver.application.PackageValidationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveValidationServiceTest {
    private final ArchiveValidationService service = new ArchiveValidationService(1024 * 1024, 1024 * 1024, 10, 256);

    @Test
    void acceptsSafeZipAndReturnsMetadata() throws IOException {
        Path zip = Files.createTempFile("safe-package", ".zip");
        writeZip(zip, new Entry("README.md", "hello"), new Entry("src/App.tsx", "export {};"));

        ArchiveValidationResult result = service.validate(zip, Files.size(zip));

        assertEquals(2, result.fileCount());
        assertEquals("README.md,src", result.topLevelEntries());
        assertTrue(result.extractedSizeBytes() > 0);
    }

    @Test
    void rejectsPathTraversalEntries() throws IOException {
        Path zip = Files.createTempFile("unsafe-package", ".zip");
        writeZip(zip, new Entry("../escape.txt", "bad"));

        PackageValidationException exception = assertThrows(
                PackageValidationException.class,
                () -> service.validate(zip, Files.size(zip)));

        assertTrue(exception.getMessage().contains("path traversal"));
    }

    @Test
    void rejectsPackagesThatExceedFileLimit() throws IOException {
        Path zip = Files.createTempFile("too-many-files", ".zip");
        writeZip(zip,
                new Entry("1.txt", "a"), new Entry("2.txt", "a"), new Entry("3.txt", "a"),
                new Entry("4.txt", "a"), new Entry("5.txt", "a"), new Entry("6.txt", "a"),
                new Entry("7.txt", "a"), new Entry("8.txt", "a"), new Entry("9.txt", "a"),
                new Entry("10.txt", "a"), new Entry("11.txt", "a"));

        PackageValidationException exception = assertThrows(
                PackageValidationException.class,
                () -> service.validate(zip, Files.size(zip)));

        assertTrue(exception.getMessage().contains("file count"));
    }

    private static void writeZip(Path zip, Entry... entries) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Entry entry : entries) {
                output.putNextEntry(new ZipEntry(entry.name()));
                output.write(entry.content().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
    }

    private record Entry(String name, String content) {
    }
}
