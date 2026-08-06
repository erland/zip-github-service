package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the github_installation table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "github_installation")
public class GitHubInstallationEntity {
    @Id
    @Column(name = "id", nullable = false)
    public long id;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "account_login", nullable = false)
    public String accountLogin;

    @Column(name = "permissions_snapshot", nullable = true, columnDefinition = "jsonb")
    public String permissionsSnapshot;

    @Column(name = "repository_selection", nullable = false)
    public String repositorySelection;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

}
