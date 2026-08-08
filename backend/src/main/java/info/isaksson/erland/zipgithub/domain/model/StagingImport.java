package info.isaksson.erland.zipgithub.domain.model;

import info.isaksson.erland.zipgithub.domain.status.StagingImportStatus;
import info.isaksson.erland.zipgithub.domain.status.StateTransitions;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Short-lived transport record for a stored ZIP before it becomes an ordinary authenticated Import. */
public final class StagingImport {
    private final UUID id;
    private final StoredUploadArtifact artifact;
    private final String claimTokenSha256;
    private final Instant createdAt;
    private Instant expiresAt;
    private StagingImportStatus status;
    private UUID ownerUserId;
    private UUID promotedImportId;
    private Instant claimedAt;
    private Instant promotedAt;
    private Instant updatedAt;

    public StagingImport(UUID id, StoredUploadArtifact artifact, String claimTokenSha256, Instant createdAt, Instant expiresAt) {
        this(id, artifact, claimTokenSha256, createdAt, expiresAt, StagingImportStatus.AVAILABLE, null, null, null, null, createdAt);
    }

    private StagingImport(UUID id, StoredUploadArtifact artifact, String claimTokenSha256, Instant createdAt, Instant expiresAt,
                          StagingImportStatus status, UUID ownerUserId, UUID promotedImportId, Instant claimedAt,
                          Instant promotedAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.artifact = Objects.requireNonNull(artifact, "artifact");
        if (claimTokenSha256 == null || !claimTokenSha256.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("claimTokenSha256 must be lowercase SHA-256");
        this.claimTokenSha256 = claimTokenSha256;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("expiresAt must be after createdAt");
        this.status = Objects.requireNonNull(status, "status");
        this.ownerUserId = ownerUserId; this.promotedImportId = promotedImportId; this.claimedAt = claimedAt;
        this.promotedAt = promotedAt; this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        validateState();
    }

    public static StagingImport rehydrate(UUID id, StoredUploadArtifact artifact, String claimTokenSha256, Instant createdAt,
                                          Instant expiresAt, StagingImportStatus status, UUID ownerUserId, UUID promotedImportId,
                                          Instant claimedAt, Instant promotedAt, Instant updatedAt) {
        return new StagingImport(id, artifact, claimTokenSha256, createdAt, expiresAt, status, ownerUserId, promotedImportId, claimedAt, promotedAt, updatedAt);
    }

    public boolean claim(UUID owner, Instant at) {
        return claim(owner, at, expiresAt);
    }

    public boolean claim(UUID owner, Instant at, Instant claimedExpiresAt) {
        Objects.requireNonNull(owner, "owner"); Objects.requireNonNull(at, "at"); Objects.requireNonNull(claimedExpiresAt, "claimedExpiresAt");
        // Terminal lifecycle states must consistently fail through the domain transition contract.
        // The time-based expiry signal is reserved for an AVAILABLE/CLAIMED row whose deadline has
        // elapsed before cleanup has persisted the EXPIRED state.
        if (status.terminal()) {
            StateTransitions.transition(status, StagingImportStatus.CLAIMED, StagingImportStatus.allowedTransitions());
        }
        if (!at.isBefore(expiresAt)) throw new IllegalStateException("staging import has expired");
        if (!claimedExpiresAt.isAfter(at)) throw new IllegalArgumentException("claimedExpiresAt must be after claim time");
        if (status == StagingImportStatus.CLAIMED && owner.equals(ownerUserId)) return false;
        boolean changed = StateTransitions.transition(status, StagingImportStatus.CLAIMED, StagingImportStatus.allowedTransitions());
        if (changed) { status = StagingImportStatus.CLAIMED; ownerUserId = owner; claimedAt = at; expiresAt = claimedExpiresAt; updatedAt = at; }
        return changed;
    }

    public boolean promote(UUID owner, UUID importId, Instant at) {
        Objects.requireNonNull(owner, "owner"); Objects.requireNonNull(importId, "importId"); Objects.requireNonNull(at, "at");
        if (!owner.equals(ownerUserId)) throw new IllegalStateException("staging import is not owned by caller");
        if (status == StagingImportStatus.PROMOTED && importId.equals(promotedImportId)) return false;
        boolean changed = StateTransitions.transition(status, StagingImportStatus.PROMOTED, StagingImportStatus.allowedTransitions());
        if (changed) { status = StagingImportStatus.PROMOTED; promotedImportId = importId; promotedAt = at; updatedAt = at; }
        return changed;
    }

    public boolean expire(Instant at) { return terminalTransition(StagingImportStatus.EXPIRED, at); }
    public boolean cancel(Instant at) { return terminalTransition(StagingImportStatus.CANCELLED, at); }
    private boolean terminalTransition(StagingImportStatus target, Instant at) {
        boolean changed = StateTransitions.transition(status, target, StagingImportStatus.allowedTransitions());
        if (changed) { status = target; updatedAt = Objects.requireNonNull(at, "at"); } return changed;
    }
    private void validateState() {
        if ((status == StagingImportStatus.CLAIMED || status == StagingImportStatus.PROMOTED) && ownerUserId == null)
            throw new IllegalArgumentException("claimed/promoted staging import requires owner");
        if (status == StagingImportStatus.PROMOTED && promotedImportId == null)
            throw new IllegalArgumentException("promoted staging import requires import id");
    }

    public UUID id() { return id; } public StoredUploadArtifact artifact() { return artifact; }
    public String claimTokenSha256() { return claimTokenSha256; } public Instant createdAt() { return createdAt; }
    public Instant expiresAt() { return expiresAt; } public StagingImportStatus status() { return status; }
    public UUID ownerUserId() { return ownerUserId; } public UUID promotedImportId() { return promotedImportId; }
    public Instant claimedAt() { return claimedAt; } public Instant promotedAt() { return promotedAt; } public Instant updatedAt() { return updatedAt; }
}
