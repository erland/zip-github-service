package info.isaksson.erland.zipgithub.domain.model;

import info.isaksson.erland.zipgithub.domain.status.SourceUploadStatus;
import info.isaksson.erland.zipgithub.domain.status.StateTransitions;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Metadata for a temporary ZIP upload. The archive bytes are stored outside the domain object. */
public final class SourceUpload {
    private final UUID id;
    private final UUID importSessionId;
    private final UUID ownerUserId;
    private final String originalFilename;
    private final Instant createdAt;
    private final Instant retentionDeadline;
    private SourceUploadStatus status;
    private long sizeBytes;
    private String sha256;

    public SourceUpload(UUID id, UUID importSessionId, UUID ownerUserId, String originalFilename,
                        Instant createdAt, Instant retentionDeadline) {
        this.id = Objects.requireNonNull(id, "id");
        this.importSessionId = Objects.requireNonNull(importSessionId, "importSessionId");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId");
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("originalFilename must not be blank");
        }
        this.originalFilename = originalFilename;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.retentionDeadline = Objects.requireNonNull(retentionDeadline, "retentionDeadline");
        if (!retentionDeadline.isAfter(createdAt)) {
            throw new IllegalArgumentException("retentionDeadline must be after createdAt");
        }
        this.status = SourceUploadStatus.CREATED;
    }

    public boolean transitionTo(SourceUploadStatus target) {
        boolean changed = StateTransitions.transition(status, target, SourceUploadStatus.allowedTransitions());
        if (target == SourceUploadStatus.STORED && (sha256 == null || sizeBytes < 0)) {
            throw new IllegalStateException("stored upload requires checksum and size");
        }
        if (changed) status = target;
        return changed;
    }

    public void recordStoredContent(long sizeBytes, String sha256) {
        if (status != SourceUploadStatus.UPLOADING) {
            throw new IllegalStateException("content metadata can only be recorded while uploading");
        }
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes must not be negative");
        if (sha256 == null || !sha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("sha256 must be a 64-character hexadecimal digest");
        }
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256.toLowerCase();
    }

    public UUID id() { return id; }
    public UUID importSessionId() { return importSessionId; }
    public UUID ownerUserId() { return ownerUserId; }
    public String originalFilename() { return originalFilename; }
    public SourceUploadStatus status() { return status; }
    public long sizeBytes() { return sizeBytes; }
    public String sha256() { return sha256; }
    public Instant createdAt() { return createdAt; }
    public Instant retentionDeadline() { return retentionDeadline; }
    public boolean isOwnedBy(UUID userId) { return ownerUserId.equals(userId); }
}
