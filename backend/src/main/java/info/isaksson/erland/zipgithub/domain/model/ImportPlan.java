package info.isaksson.erland.zipgithub.domain.model;

import info.isaksson.erland.zipgithub.domain.status.ImportPlanStatus;
import info.isaksson.erland.zipgithub.domain.status.StateTransitions;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable file comparison snapshot with a small approval state machine. */
public final class ImportPlan {
    private final UUID id;
    private final UUID importSessionId;
    private final UUID ownerUserId;
    private final String baseCommitSha;
    private final String policyVersion;
    private final List<ImportPlanEntry> entries;
    private final Instant createdAt;
    private ImportPlanStatus status;

    public ImportPlan(UUID id, UUID importSessionId, UUID ownerUserId, String baseCommitSha,
                      String policyVersion, List<ImportPlanEntry> entries, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.importSessionId = Objects.requireNonNull(importSessionId, "importSessionId");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId");
        if (baseCommitSha == null || !baseCommitSha.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("baseCommitSha must be a 40-character Git SHA");
        }
        if (policyVersion == null || policyVersion.isBlank()) {
            throw new IllegalArgumentException("policyVersion must not be blank");
        }
        this.baseCommitSha = baseCommitSha.toLowerCase();
        this.policyVersion = policyVersion;
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = ImportPlanStatus.DRAFT;
    }

    public boolean transitionTo(ImportPlanStatus target) {
        if (target == ImportPlanStatus.APPROVED && hasBlockingEntries()) {
            throw new IllegalStateException("a plan with blocked entries cannot be approved");
        }
        boolean changed = StateTransitions.transition(status, target, ImportPlanStatus.allowedTransitions());
        if (changed) status = target;
        return changed;
    }

    public boolean hasBlockingEntries() {
        return entries.stream().anyMatch(ImportPlanEntry::blocked);
    }

    public UUID id() { return id; }
    public UUID importSessionId() { return importSessionId; }
    public UUID ownerUserId() { return ownerUserId; }
    public String baseCommitSha() { return baseCommitSha; }
    public String policyVersion() { return policyVersion; }
    public List<ImportPlanEntry> entries() { return entries; }
    public ImportPlanStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public boolean isOwnedBy(UUID userId) { return ownerUserId.equals(userId); }
}
