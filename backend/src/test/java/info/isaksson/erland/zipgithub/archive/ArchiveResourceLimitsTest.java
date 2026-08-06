package info.isaksson.erland.zipgithub.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchiveResourceLimitsTest {
    @TempDir Path tempDir;

    @Test
    void rejectsCompressedArchiveSize() throws Exception {
        Path zip = createZip("compressed-limit.zip", 1, 64, false);
        ArchiveResourceLimits limits = new ArchiveResourceLimits(10, 10_000, 10, 5_000, 200, 1_000);
        assertCode(ArchiveSecurityCode.COMPRESSED_SIZE_LIMIT_EXCEEDED,
                () -> new SecureZipInspector().inspect(zip, limits));
    }

    @Test
    void rejectsTooManyEntriesBeforeInflation() throws Exception {
        Path zip = createZip("entry-limit.zip", 3, 8, false);
        ArchiveResourceLimits limits = new ArchiveResourceLimits(100_000, 100_000, 2, 100_000, 200, 1_000);
        assertCode(ArchiveSecurityCode.ENTRY_COUNT_LIMIT_EXCEEDED,
                () -> new SecureZipInspector().inspect(zip, limits));
    }

    @Test
    void rejectsSingleFileAndTotalUncompressedLimits() throws Exception {
        Path single = createZip("single-limit.zip", 1, 2_048, false);
        ArchiveResourceLimits singleLimits = new ArchiveResourceLimits(100_000, 10_000, 10, 1_024, 200, 10_000);
        assertCode(ArchiveSecurityCode.SINGLE_FILE_SIZE_LIMIT_EXCEEDED,
                () -> new SecureZipInspector().inspect(single, singleLimits));

        Path total = createZip("total-limit.zip", 3, 800, false);
        ArchiveResourceLimits totalLimits = new ArchiveResourceLimits(100_000, 2_000, 10, 1_000, 200, 10_000);
        assertCode(ArchiveSecurityCode.UNCOMPRESSED_SIZE_LIMIT_EXCEEDED,
                () -> new SecureZipInspector().inspect(total, totalLimits));
    }

    @Test
    void rejectsLongPathsAndExtremeCompressionRatio() throws Exception {
        Path longPath = tempDir.resolve("long-path.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(longPath))) {
            out.putNextEntry(new ZipEntry("a".repeat(80) + ".txt"));
            out.write("ok".getBytes());
            out.closeEntry();
        }
        ArchiveResourceLimits pathLimits = new ArchiveResourceLimits(100_000, 100_000, 10, 100_000, 40, 1_000);
        assertCode(ArchiveSecurityCode.PATH_LENGTH_LIMIT_EXCEEDED,
                () -> new SecureZipInspector().inspect(longPath, pathLimits));

        Path bomb = createZip("ratio-limit.zip", 1, 200_000, true);
        ArchiveResourceLimits ratioLimits = new ArchiveResourceLimits(1_000_000, 1_000_000, 10, 500_000, 200, 20);
        assertCode(ArchiveSecurityCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
                () -> new SecureZipInspector().inspect(bomb, ratioLimits));
    }

    @Test
    void acceptsArchiveInsideAllLimits() throws Exception {
        Path zip = createZip("valid.zip", 2, 128, false);
        ArchiveResourceLimits limits = new ArchiveResourceLimits(100_000, 100_000, 10, 10_000, 200, 1_000);
        assertEquals(2, new SecureZipInspector().inspect(zip, limits).size());
    }

    private Path createZip(String name, int files, int bytesPerFile, boolean repeated) throws IOException {
        Path zip = tempDir.resolve(name);
        byte[] content = new byte[bytesPerFile];
        for (int i = 0; i < content.length; i++) content[i] = repeated ? 0 : (byte) (i * 31 + 7);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (int i = 0; i < files; i++) {
                out.putNextEntry(new ZipEntry("file-" + i + ".bin"));
                out.write(content);
                out.closeEntry();
            }
        }
        return zip;
    }

    private static void assertCode(ArchiveSecurityCode expected, ThrowingRunnable action) {
        ArchiveSecurityException exception = assertThrows(ArchiveSecurityException.class, action::run);
        assertEquals(expected, exception.code());
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
