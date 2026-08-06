package info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import info.isaksson.erland.zipbuildserver.domain.model.NetworkMode;
import info.isaksson.erland.zipbuildserver.domain.model.RunStatus;

@Entity
@Table(name = "verification_run")
public class VerificationRunEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @Column(name = "session_id", nullable = false)
    public UUID sessionId;

    @Column(name = "source_package_id", nullable = false)
    public UUID sourcePackageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RunStatus status;

    @Column(name = "plan_id")
    public String planId;

    @Column(name = "requested_plan_id")
    public String requestedPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "network_mode", nullable = false)
    public NetworkMode networkMode;

    public String summary;

    @Column(name = "started_at")
    public OffsetDateTime startedAt;

    @Column(name = "completed_at")
    public OffsetDateTime completedAt;

    @Column(name = "duration_millis")
    public Long durationMillis;
}
