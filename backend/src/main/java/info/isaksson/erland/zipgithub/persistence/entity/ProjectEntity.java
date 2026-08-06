package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the project table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "project")
public class ProjectEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "owner_user_id", nullable = false)
    public UUID ownerUserId;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "github_installation_id", nullable = true)
    public Long githubInstallationId;

    @Column(name = "github_repository_id", nullable = true)
    public Long githubRepositoryId;

    @Column(name = "repository_owner", nullable = true)
    public String repositoryOwner;

    @Column(name = "repository_name", nullable = true)
    public String repositoryName;

    @Column(name = "default_branch", nullable = false)
    public String defaultBranch;

    @Column(name = "active", nullable = false)
    public boolean active;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

}
