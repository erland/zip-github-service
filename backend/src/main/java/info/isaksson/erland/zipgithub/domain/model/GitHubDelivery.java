package info.isaksson.erland.zipgithub.domain.model;

import info.isaksson.erland.zipgithub.domain.status.GitHubDeliveryStatus;
import info.isaksson.erland.zipgithub.domain.status.StateTransitions;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Tracks one idempotent delivery of an approved plan to GitHub. */
public final class GitHubDelivery {
    private final UUID id;
    private final UUID importSessionId;
    private final UUID importPlanId;
    private final UUID ownerUserId;
    private final String idempotencyKey;
    private final Instant createdAt;
    private GitHubDeliveryStatus status;
    private String branchName;
    private String commitSha;
    private Long pullRequestNumber;
    private String pullRequestUrl;

    public GitHubDelivery(UUID id, UUID importSessionId, UUID importPlanId, UUID ownerUserId,
                          String idempotencyKey, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.importSessionId = Objects.requireNonNull(importSessionId, "importSessionId");
        this.importPlanId = Objects.requireNonNull(importPlanId, "importPlanId");
        this.ownerUserId = Objects.requireNonNull(ownerUserId, "ownerUserId");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = GitHubDeliveryStatus.CREATED;
    }

    public boolean transitionTo(GitHubDeliveryStatus target) {
        boolean changed = StateTransitions.transition(status, target, GitHubDeliveryStatus.allowedTransitions());
        if (changed) status = target;
        return changed;
    }

    public void recordBranch(String branchName) {
        if (branchName == null || branchName.isBlank() || branchName.contains("..") || branchName.startsWith("/")) {
            throw new IllegalArgumentException("invalid branchName");
        }
        if (this.branchName != null && !this.branchName.equals(branchName)) {
            throw new IllegalStateException("branchName is immutable once recorded");
        }
        this.branchName = branchName;
    }

    public void recordCommit(String commitSha) {
        if (commitSha == null || !commitSha.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("commitSha must be a 40-character Git SHA");
        }
        if (this.commitSha != null && !this.commitSha.equalsIgnoreCase(commitSha)) {
            throw new IllegalStateException("commitSha is immutable once recorded");
        }
        this.commitSha = commitSha.toLowerCase();
    }

    public void recordPullRequest(long number, String url) {
        if (number <= 0) throw new IllegalArgumentException("pull request number must be positive");
        if (url == null || url.isBlank()) throw new IllegalArgumentException("pull request URL must not be blank");
        if (pullRequestNumber != null && (!pullRequestNumber.equals(number) || !pullRequestUrl.equals(url))) {
            throw new IllegalStateException("pull request metadata is immutable once recorded");
        }
        pullRequestNumber = number;
        pullRequestUrl = url;
    }

    public UUID id() { return id; }
    public UUID importSessionId() { return importSessionId; }
    public UUID importPlanId() { return importPlanId; }
    public UUID ownerUserId() { return ownerUserId; }
    public String idempotencyKey() { return idempotencyKey; }
    public GitHubDeliveryStatus status() { return status; }
    public String branchName() { return branchName; }
    public String commitSha() { return commitSha; }
    public Long pullRequestNumber() { return pullRequestNumber; }
    public String pullRequestUrl() { return pullRequestUrl; }
    public Instant createdAt() { return createdAt; }
    public boolean isOwnedBy(UUID userId) { return ownerUserId.equals(userId); }
}
