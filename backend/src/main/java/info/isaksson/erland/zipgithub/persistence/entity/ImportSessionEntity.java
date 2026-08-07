package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the import_session table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "import_session")
public class ImportSessionEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "project_id", nullable = false)
    public UUID projectId;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "base_branch", nullable = false)
    public String baseBranch;

    @Column(name = "base_commit_sha", nullable = false)
    public String baseCommitSha;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "source_type", nullable = false)
    public String sourceType;

    @Column(name = "source_reference")
    public String sourceReference;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

}
