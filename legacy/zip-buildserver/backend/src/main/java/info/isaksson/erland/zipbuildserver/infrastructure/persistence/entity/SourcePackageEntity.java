package info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import info.isaksson.erland.zipbuildserver.domain.model.SourcePackageStatus;

@Entity
@Table(name = "source_package")
public class SourcePackageEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @Column(name = "session_id", nullable = false)
    public UUID sessionId;

    @Column(name = "original_filename")
    public String originalFilename;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    public String checksumSha256;

    @Column(name = "compressed_size_bytes", nullable = false)
    public long compressedSizeBytes;

    @Column(name = "extracted_size_bytes")
    public Long extractedSizeBytes;

    @Column(name = "file_count")
    public Integer fileCount;

    @Column(name = "top_level_entries")
    public String topLevelEntries;

    @Column(name = "storage_reference", nullable = false)
    public String storageReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SourcePackageStatus status;

    @Column(name = "rejection_reason")
    public String rejectionReason;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
