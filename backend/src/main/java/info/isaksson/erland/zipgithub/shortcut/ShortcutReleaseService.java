package info.isaksson.erland.zipgithub.shortcut;

import info.isaksson.erland.zipgithub.api.error.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Resolves the statically pre-signed iOS Shortcut release artifact. */
@ApplicationScoped
public class ShortcutReleaseService {
    private final Optional<Path> configuredPath;
    private final String version;
    private final String generation;

    @Inject
    public ShortcutReleaseService(
            @ConfigProperty(name = "zipgithub.shortcut.release-path") Optional<String> releasePath,
            @ConfigProperty(name = "zipgithub.shortcut.version", defaultValue = "unpublished") String version,
            @ConfigProperty(name = "zipgithub.shortcut.generation", defaultValue = "unpublished") String generation) {
        this.configuredPath = releasePath.flatMap(ShortcutReleaseArtifact::available);
        this.version = cleanLabel(version, "unpublished");
        this.generation = cleanLabel(generation, "unpublished");
    }

    ShortcutReleaseService(String releasePath, String version, String generation) {
        this(Optional.ofNullable(releasePath), version, generation);
    }

    public ReleaseMetadata metadata() {
        Optional<Path> artifact = availableArtifact();
        if (artifact.isEmpty()) return new ReleaseMetadata(false, version, generation, null, null, null);
        Path path = artifact.get();
        try {
            return new ReleaseMetadata(true, version, generation, path.getFileName().toString(), Files.size(path), ShortcutReleaseArtifact.sha256(path));
        } catch (IOException e) {
            throw ApiException.notFound("SHORTCUT_RELEASE_UNAVAILABLE", "The signed Shortcut release is not available.");
        }
    }

    public Path requireArtifact() {
        return availableArtifact().orElseThrow(() -> ApiException.notFound("SHORTCUT_RELEASE_UNAVAILABLE",
                "The signed Shortcut release is not available. An administrator must publish the current signed .shortcut artifact."));
    }

    private Optional<Path> availableArtifact() {
        return configuredPath.filter(path -> Files.isRegularFile(path) && Files.isReadable(path));
    }

    private static String cleanLabel(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        if (normalized.length() > 80 || normalized.chars().anyMatch(ch -> Character.isISOControl(ch))) return fallback;
        return normalized;
    }


    public record ReleaseMetadata(boolean available, String version, String generation,
                                  String filename, Long sizeBytes, String sha256) {}
}
