package info.isaksson.erland.zipgithub.archive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Builds a deterministic, content-hashed inventory without executing archive content. */
@ApplicationScoped
public class ArchiveInventoryService {
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Set<String> ROOT_NOISE = Set.of(".DS_Store");

    private final ArchiveInspectionService inspectionService;

    @Inject
    public ArchiveInventoryService(ArchiveInspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    public ArchiveInventory createInventory(Path zipFile) throws IOException {
        List<ArchiveEntryDescriptor> inspected = inspectionService.inspect(zipFile);
        List<String> filePaths = inspected.stream()
                .filter(entry -> entry.type() == ArchiveEntryType.REGULAR_FILE)
                .map(ArchiveEntryDescriptor::path)
                .toList();
        String wrapper = ArchiveNormalization.detectSingleWrapper(filePaths);

        Map<String, ArchiveInventoryEntry> files = new LinkedHashMap<>();
        Map<String, String> gitIgnoreFiles = new LinkedHashMap<>();
        List<String> ignored = new ArrayList<>();
        try (InputStream input = new BufferedInputStream(Files.newInputStream(zipFile));
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String original = entry.getName();
                String normalized = ArchiveNormalization.stripWrapper(original, wrapper);
                if (isIgnored(original, normalized)) {
                    ignored.add(original);
                    drain(zip);
                    continue;
                }
                HashedContent hashed = hash(zip, isGitIgnorePath(normalized));
                files.put(normalized, new ArchiveInventoryEntry(
                        normalized, hashed.size(), hashed.sha256(), hashed.textCandidate()));
                if (hashed.capturedText() != null) gitIgnoreFiles.put(normalized, hashed.capturedText());
            }
        }

        List<ArchiveInventoryEntry> sortedFiles = files.values().stream()
                .sorted(Comparator.comparing(ArchiveInventoryEntry::path))
                .toList();
        ignored.sort(String::compareTo);
        return new ArchiveInventory(wrapper, ignored, sortedFiles, gitIgnoreFiles);
    }

    private static boolean isIgnored(String original, String normalized) {
        return ArchiveNormalization.isTransportNoise(original) || ROOT_NOISE.contains(normalized);
    }

    private static boolean isGitIgnorePath(String normalized) {
        return ".gitignore".equals(normalized) || normalized.endsWith("/.gitignore");
    }

    private static HashedContent hash(InputStream input, boolean captureText) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream captured = captureText ? new ByteArrayOutputStream() : null;
        long size = 0;
        boolean textCandidate = true;
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            if (captured != null) captured.write(buffer, 0, read);
            size += read;
            if (textCandidate) {
                for (int i = 0; i < read; i++) {
                    if (buffer[i] == 0) {
                        textCandidate = false;
                        break;
                    }
                }
            }
        }
        String capturedText = captured == null ? null : captured.toString(StandardCharsets.UTF_8);
        return new HashedContent(size, HexFormat.of().formatHex(digest.digest()), textCandidate, capturedText);
    }

    private static void drain(InputStream input) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (input.read(buffer) != -1) {
            // Consume current ZIP entry so the next entry can be read.
        }
    }

    private record HashedContent(long size, String sha256, boolean textCandidate, String capturedText) {
    }
}
