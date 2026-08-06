package info.isaksson.erland.zipgithub.domain.model;

import info.isaksson.erland.zipgithub.domain.status.ImportSessionStatus;
import info.isaksson.erland.zipgithub.domain.status.StateTransitions;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Coordinates one user-owned ZIP import from upload through GitHub delivery. */
public final class ImportSession {
    private final UUID id;
    private final UUID projectId;
    private final UUID ownerUserId;
    private final String baseBranch;
    private final Instant createdAt;
    private ImportSessionStatus status;
    private String baseCommitSha;
    private Instant updatedAt;

    public ImportSession(UUID id, UUID projectId, UUID ownerUserId, String baseBranch, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.projectId = Objects.requireNonNull(projectId, "projectId");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId");
        if (baseBranch == null || baseBranch.isBlank()) {
            throw new IllegalArgumentException("baseBranch must not be blank");
        }
        this.baseBranch = baseBranch;
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
        this.status = ImportSessionStatus.CREATED;
    }

    public boolean transitionTo(ImportSessionStatus target, Instant now) {
        Objects.requireNonNull(now, "now");
        boolean changed = StateTransitions.transition(status, target, ImportSessionStatus.allowedTransitions());
        if (changed) {
            status = target;
            updatedAt = now;
        }
        return changed;
    }

    public void lockBaseCommit(String commitSha, Instant now) {
        if (commitSha == null || !commitSha.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("base commit must be a 40-character Git SHA");
        }
        if (baseCommitSha != null && !baseCommitSha.equalsIgnoreCase(commitSha)) {
            throw new IllegalStateException("base commit is immutable once locked");
        }
        baseCommitSha = commitSha.toLowerCase();
        updatedAt = Objects.requireNonNull(now, "now");
    }

    public UUID id() { return id; }
    public UUID projectId() { return projectId; }
    public UUID ownerUserId() { return ownerUserId; }
    public String baseBranch() { return baseBranch; }
    public String baseCommitSha() { return baseCommitSha; }
    public ImportSessionStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public boolean isOwnedBy(UUID userId) { return ownerUserId.equals(userId); }
}
