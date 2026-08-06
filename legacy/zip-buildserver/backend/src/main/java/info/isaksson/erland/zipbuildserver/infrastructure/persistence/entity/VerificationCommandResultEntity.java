package info.isaksson.erland.zipbuildserver.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import info.isaksson.erland.zipbuildserver.domain.model.CheckStatus;

@Entity
@Table(name = "verification_command_result")
public class VerificationCommandResultEntity extends PanacheEntityBase {
    @Id
    public UUID id;

    @Column(name = "run_id", nullable = false)
    public UUID runId;

    @Column(name = "command_label", nullable = false)
    public String commandLabel;

    @Column(name = "working_directory", nullable = false)
    public String workingDirectory;

    @Column(name = "command_display", nullable = false)
    public String commandDisplay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public CheckStatus status;

    @Column(name = "exit_code")
    public Integer exitCode;

    @Column(name = "started_at")
    public OffsetDateTime startedAt;

    @Column(name = "completed_at")
    public OffsetDateTime completedAt;

    @Column(name = "duration_millis")
    public Long durationMillis;

    @Column(name = "log_excerpt")
    public String logExcerpt;

    @Column(name = "failure_category")
    public String failureCategory;

    @Column(name = "failure_message")
    public String failureMessage;

    @Column(name = "stdout_artifact_ref")
    public UUID stdoutArtifactRef;

    @Column(name = "stderr_artifact_ref")
    public UUID stderrArtifactRef;
}
