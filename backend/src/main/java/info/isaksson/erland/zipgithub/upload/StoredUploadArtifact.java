package info.isaksson.erland.zipgithub.upload;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/**
 * Neutral result of safely ingesting ZIP bytes into controlled storage.
 *
 * <p>The artifact deliberately has no user/import ownership semantics. A caller may attach it to a
 * normal import, a future staging import or another ingestion source after storage has completed.</p>
 */
public record StoredUploadArtifact(UUID id, String originalFilename, long sizeBytes, String sha256,
                                   Path storagePath, Instant createdAt, Instant retentionDeadline) { }
