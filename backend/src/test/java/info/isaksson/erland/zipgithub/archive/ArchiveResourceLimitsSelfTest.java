package info.isaksson.erland.zipgithub.archive;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ArchiveResourceLimitsSelfTest {
    private ArchiveResourceLimitsSelfTest() {}

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("zip-github-limits-");
        try {
            Path valid = createZip(directory.resolve("valid.zip"), 2, 128, false);
            ArchiveResourceLimits permissive = new ArchiveResourceLimits(100_000, 100_000, 10, 10_000, 200, 1_000);
            if (new SecureZipInspector().inspect(valid, permissive).size() != 2) {
                throw new AssertionError("Valid ZIP inventory mismatch");
            }

            Path bomb = createZip(directory.resolve("bomb.zip"), 1, 200_000, true);
            ArchiveResourceLimits strictRatio = new ArchiveResourceLimits(1_000_000, 1_000_000, 10, 500_000, 200, 20);
            expect(ArchiveSecurityCode.COMPRESSION_RATIO_LIMIT_EXCEEDED,
                    () -> new SecureZipInspector().inspect(bomb, strictRatio));
            System.out.println("Archive resource limit self-test passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static Path createZip(Path path, int files, int size, boolean repeated) throws Exception {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) content[i] = repeated ? 0 : (byte) (i * 31 + 7);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path))) {
            for (int i = 0; i < files; i++) {
                out.putNextEntry(new ZipEntry("file-" + i + ".bin"));
                out.write(content);
                out.closeEntry();
            }
        }
        return path;
    }

    private static void expect(ArchiveSecurityCode code, ThrowingRunnable action) throws Exception {
        try {
            action.run();
            throw new AssertionError("Expected " + code);
        } catch (ArchiveSecurityException e) {
            if (e.code() != code) throw new AssertionError("Expected " + code + " but got " + e.code());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
