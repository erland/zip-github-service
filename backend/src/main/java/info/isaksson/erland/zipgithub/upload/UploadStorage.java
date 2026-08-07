package info.isaksson.erland.zipgithub.upload;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

/** Isolated temporary storage for uploaded source packages. */
public final class UploadStorage {
    private final Path root;

    public UploadStorage(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    public PendingUpload begin(UUID storageScopeId, UUID uploadId) throws IOException {
        Objects.requireNonNull(storageScopeId, "storageScopeId");
        Objects.requireNonNull(uploadId, "uploadId");
        Path directory = root.resolve(storageScopeId.toString()).normalize();
        requireInsideRoot(directory);
        Files.createDirectories(directory);
        Path temporary = directory.resolve(uploadId + ".part").normalize();
        Path completed = directory.resolve(uploadId + ".zip").normalize();
        requireInsideRoot(temporary);
        return new PendingUpload(temporary, completed,
                Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
    }

    private void requireInsideRoot(Path path) {
        if (!path.startsWith(root)) throw new IllegalArgumentException("upload path escapes storage root");
    }

    public record PendingUpload(Path temporaryPath, Path completedPath, OutputStream output) implements AutoCloseable {
        public Path complete() throws IOException {
            output.close();
            try {
                return Files.move(temporaryPath, completedPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                return Files.move(temporaryPath, completedPath);
            }
        }

        public void abort() {
            try { output.close(); } catch (IOException ignored) { }
            try { Files.deleteIfExists(temporaryPath); } catch (IOException ignored) { }
        }

        @Override public void close() { abort(); }
    }
}
