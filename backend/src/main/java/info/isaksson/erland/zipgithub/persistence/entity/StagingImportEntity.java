package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staging_import")
public class StagingImportEntity {
    @Id public UUID id;
    @Column(name="artifact_id", nullable=false, unique=true) public UUID artifactId;
    @Column(name="original_filename", nullable=false) public String originalFilename;
    @Column(name="storage_path", nullable=false) public String storagePath;
    @Column(name="size_bytes", nullable=false) public long sizeBytes;
    @Column(name="sha256", nullable=false) public String sha256;
    @Column(name="file_modes_json", nullable=false, columnDefinition="jsonb") public String fileModesJson;
    @Column(name="claim_token_sha256", nullable=false) public String claimTokenSha256;
    @Column(name="owner_user_id") public UUID ownerUserId;
    @Column(name="promoted_import_id") public UUID promotedImportId;
    @Column(name="status", nullable=false) public String status;
    @Column(name="created_at", nullable=false) public Instant createdAt;
    @Column(name="expires_at", nullable=false) public Instant expiresAt;
    @Column(name="artifact_retention_deadline", nullable=false) public Instant artifactRetentionDeadline;
    @Column(name="claimed_at") public Instant claimedAt;
    @Column(name="promoted_at") public Instant promotedAt;
    @Column(name="artifact_deleted_at") public Instant artifactDeletedAt;
    @Column(name="updated_at", nullable=false) public Instant updatedAt;
}
