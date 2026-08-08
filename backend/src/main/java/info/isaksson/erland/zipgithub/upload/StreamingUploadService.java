package info.isaksson.erland.zipgithub.upload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

/**
 * Web-import upload adapter. Generic byte ingestion lives in {@link ZipIngestionService}; this
 * service only attaches the resulting stored artifact to the authenticated import ownership model.
 */
@ApplicationScoped
public class StreamingUploadService {
    private final ZipIngestionService ingestion;
    private final UploadFileModeService fileModes;

    @Inject
    public StreamingUploadService(ZipIngestionService ingestion, UploadFileModeService fileModes) {
        this.ingestion = Objects.requireNonNull(ingestion, "ingestion");
        this.fileModes = Objects.requireNonNull(fileModes, "fileModes");
    }

    public StreamingUploadService(ZipIngestionService ingestion) {
        this.ingestion = Objects.requireNonNull(ingestion, "ingestion");
        this.fileModes = null;
    }

    public StoredUpload store(UUID ownerUserId, UUID importId, String originalFilename,
                              long declaredContentLength, InputStream input) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(importId, "importId");
        StoredUploadArtifact artifact = ingestion.store(importId, originalFilename, declaredContentLength, input);
        if (fileModes != null) {
            try { artifact = artifact.withFileModes(fileModes.inspect(artifact.storagePath())); }
            catch (IOException | info.isaksson.erland.zipgithub.archive.ArchiveSecurityException e) {
                try { Files.deleteIfExists(artifact.storagePath()); } catch (IOException ignored) { }
                throw new IllegalArgumentException("The uploaded ZIP failed secure archive inspection.", e);
            }
        }
        return StoredUpload.attach(ownerUserId, importId, artifact);
    }
}
