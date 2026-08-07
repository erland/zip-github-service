package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the import_plan table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "import_plan")
public class ImportPlanEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "import_session_id", nullable = false)
    public UUID importSessionId;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "base_commit_sha", nullable = false)
    public String baseCommitSha;

    @Column(name = "policy_version", nullable = false)
    public String policyVersion;

    @Column(name = "source_upload_sha256", nullable = true)
    public String sourceUploadSha256;

    @Column(name = "plan_digest_sha256", nullable = true)
    public String planDigestSha256;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "approved_at", nullable = true)
    public Instant approvedAt;

    @Column(name = "approved_by_user_id", nullable = true)
    public UUID approvedByUserId;

}
