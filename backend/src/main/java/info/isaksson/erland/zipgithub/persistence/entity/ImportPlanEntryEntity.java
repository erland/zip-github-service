package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the import_plan_entry table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "import_plan_entry")
public class ImportPlanEntryEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "import_plan_id", nullable = false)
    public UUID importPlanId;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "path", nullable = false)
    public String path;

    @Column(name = "change_type", nullable = false)
    public String changeType;

    @Column(name = "source_sha256", nullable = true)
    public String sourceSha256;

    @Column(name = "target_sha256", nullable = true)
    public String targetSha256;

    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes;

    @Column(name = "is_text", nullable = false)
    public boolean text;

    @Column(name = "policy_result", nullable = false)
    public String policyResult;

    @Column(name = "policy_message", nullable = true)
    public String policyMessage;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

}
