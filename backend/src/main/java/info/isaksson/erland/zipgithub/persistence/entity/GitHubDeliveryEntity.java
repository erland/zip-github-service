package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the github_delivery table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "github_delivery")
public class GitHubDeliveryEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "import_session_id", nullable = false)
    public UUID importSessionId;

    @Column(name = "import_plan_id", nullable = false)
    public UUID importPlanId;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "idempotency_key", nullable = false)
    public String idempotencyKey;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "branch_name", nullable = true)
    public String branchName;

    @Column(name = "commit_sha", nullable = true)
    public String commitSha;

    @Column(name = "pull_request_number", nullable = true)
    public Long pullRequestNumber;

    @Column(name = "pull_request_url", nullable = true)
    public String pullRequestUrl;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

}
