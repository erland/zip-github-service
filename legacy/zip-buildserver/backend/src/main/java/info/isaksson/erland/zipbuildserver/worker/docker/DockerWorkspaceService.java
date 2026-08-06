package info.isaksson.erland.zipbuildserver.worker.docker;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class DockerWorkspaceService {
    private final Path workspaceDirectory;

    public DockerWorkspaceService(
            @ConfigProperty(name = "zip-buildserver.storage.workspaces-dir", defaultValue = "target/zip-buildserver/workspaces")
            String workspaceDirectory) {
        this.workspaceDirectory = Path.of(workspaceDirectory);
    }

    public Path createWorkspace(Path zipPackagePath) {
        try {
            Files.createDirectories(workspaceDirectory);
            Path workspace = Files.createTempDirectory(workspaceDirectory, "run-");
            extractZip(zipPackagePath, workspace);
            return workspace;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not prepare Docker execution workspace.", exception);
        }
    }

    public void cleanup(Path workspace) {
        if (workspace == null || !Files.exists(workspace)) {
            return;
        }
        try (var paths = Files.walk(workspace)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("Could not delete workspace path: " + path, exception);
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not clean Docker execution workspace.", exception);
        }
    }

    private void extractZip(Path zipPackagePath, Path workspace) throws IOException {
        try (InputStream input = Files.newInputStream(zipPackagePath);
             ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = safeTarget(workspace, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        }
    }

    private Path safeTarget(Path workspace, String entryName) {
        Path normalizedWorkspace = workspace.toAbsolutePath().normalize();
        Path target = normalizedWorkspace.resolve(entryName).normalize();
        if (!target.startsWith(normalizedWorkspace)) {
            throw new IllegalArgumentException("Zip entry escapes workspace: " + entryName);
        }
        return target;
    }
}
