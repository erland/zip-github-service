package info.isaksson.erland.zipgithub.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence row for the user_account table. Domain mapping belongs in the application layer. */
@Entity
@Table(name = "user_account")
public class UserAccountEntity {
    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "github_user_id", nullable = false)
    public long githubUserId;

    @Column(name = "github_login", nullable = false)
    public String githubLogin;

    @Column(name = "avatar_url", nullable = true)
    public String avatarUrl;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "last_login_at", nullable = true)
    public Instant lastLoginAt;

}
