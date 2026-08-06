package info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @Column(name = "event_type", nullable = false)
    public String eventType;

    public String actor;

    @Column(name = "resource_type")
    public String resourceType;

    @Column(name = "resource_id")
    public UUID resourceId;

    public String details;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;
}
