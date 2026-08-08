package info.isaksson.erland.zipgithub.shortcut;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/** Dependency-free filesystem rules for a pre-signed Shortcut release artifact. */
public final class ShortcutReleaseArtifact {
    private ShortcutReleaseArtifact() {}

    public static Optional<Path> available(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) return Optional.empty();
        Path path = Path.of(configuredPath.trim()).toAbsolutePath().normalize();
        if (path.getFileName() == null || !path.getFileName().toString().toLowerCase().endsWith(".shortcut")) return Optional.empty();
        return Files.isRegularFile(path) && Files.isReadable(path) ? Optional.of(path) : Optional.empty();
    }

    public static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
    }
}
