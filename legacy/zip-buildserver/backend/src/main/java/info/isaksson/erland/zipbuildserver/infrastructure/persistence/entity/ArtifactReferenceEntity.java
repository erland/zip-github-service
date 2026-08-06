package info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import info.isaksson.erland.zipbuildserver.domain.model.ArtifactType;

@Entity
@Table(name = "artifact_reference")
public class ArtifactReferenceEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @Column(name = "run_id", nullable = false)
    public UUID runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ArtifactType type;

    @Column(name = "storage_reference", nullable = false)
    public String storageReference;

    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "expires_at")
    public OffsetDateTime expiresAt;
}
