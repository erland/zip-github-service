package info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import info.isaksson.erland.zipbuildserver.domain.model.SessionStatus;

@Entity
@Table(name = "verification_session")
public class VerificationSessionEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    public String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SessionStatus status;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "closed_at")
    public OffsetDateTime closedAt;

    @Column(name = "created_by")
    public String createdBy;

    @Column(name = "retention_policy", nullable = false)
    public String retentionPolicy;
}
