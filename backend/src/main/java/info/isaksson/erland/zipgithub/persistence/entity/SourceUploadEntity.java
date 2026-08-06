package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the source_upload table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "source_upload")
public class SourceUploadEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "import_session_id", nullable = false)
    public UUID importSessionId;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "original_filename", nullable = false)
    public String originalFilename;

    @Column(name = "storage_key", nullable = true)
    public String storageKey;

    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes;

    @Column(name = "sha256", nullable = true)
    public String sha256;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "retention_deadline", nullable = false)
    public Instant retentionDeadline;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

}
