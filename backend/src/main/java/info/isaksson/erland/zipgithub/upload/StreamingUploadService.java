package info.isaksson.erland.zipgithub.upload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Web-import upload adapter. Generic byte ingestion lives in {@link ZipIngestionService}; this
 * service only attaches the resulting stored artifact to the authenticated import ownership model.
 */
@ApplicationScoped
public class StreamingUploadService {
    private final ZipIngestionService ingestion;

    @Inject
    public StreamingUploadService(ZipIngestionService ingestion) {
        this.ingestion = Objects.requireNonNull(ingestion, "ingestion");
    }

    public StoredUpload store(UUID ownerUserId, UUID importId, String originalFilename,
                              long declaredContentLength, InputStream input) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(importId, "importId");
        StoredUploadArtifact artifact = ingestion.store(importId, originalFilename, declaredContentLength, input);
        return StoredUpload.attach(ownerUserId, importId, artifact);
    }
}
