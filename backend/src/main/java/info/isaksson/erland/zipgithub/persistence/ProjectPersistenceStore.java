package info.isaksson.erland.zipgithub.persistence;

import info.isaksson.erland.zipgithub.api.dto.ProjectResponse;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persists durable user, GitHub installation and project configuration state in PostgreSQL. */
@ApplicationScoped
public class ProjectPersistenceStore {
    @Inject AgroalDataSource dataSource;
    @ConfigProperty(name = "zipgithub.persistence.projects.enabled", defaultValue = "true") boolean enabled;

    public boolean enabled() { return enabled; }

    public void upsertUser(UUID userId, long githubUserId, String login, String avatarUrl) {
        if (!enabled) return;
        Instant now = Instant.now();
        execute("""
                INSERT INTO user_account (id, github_user_id, github_login, avatar_url, created_at, last_login_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    github_user_id = EXCLUDED.github_user_id,
                    github_login = EXCLUDED.github_login,
                    avatar_url = EXCLUDED.avatar_url,
                    last_login_at = EXCLUDED.last_login_at
                """, statement -> {
            statement.setObject(1, userId);
            statement.setLong(2, githubUserId);
            statement.setString(3, login);
            statement.setString(4, avatarUrl);
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setTimestamp(6, Timestamp.from(now));
        });
    }

    public void upsertInstallation(UUID ownerUserId, long installationId, String accountLogin, String repositorySelection) {
        if (!enabled) return;
        Instant now = Instant.now();
        execute("""
                INSERT INTO github_installation
                    (id, owner_user_id, account_login, permissions_snapshot, repository_selection, created_at, updated_at)
                VALUES (?, ?, ?, '{}'::jsonb, ?, ?, ?)
                ON CONFLICT (id, owner_user_id) DO UPDATE SET
                    account_login = EXCLUDED.account_login,
                    repository_selection = EXCLUDED.repository_selection,
                    updated_at = EXCLUDED.updated_at
                """, statement -> {
            statement.setLong(1, installationId);
            statement.setObject(2, ownerUserId);
            statement.setString(3, accountLogin);
            statement.setString(4, repositorySelection == null || repositorySelection.isBlank() ? "selected" : repositorySelection);
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setTimestamp(6, Timestamp.from(now));
        });
    }

    public void insertProject(UUID ownerUserId, ProjectResponse project) {
        if (!enabled) return;
        String[] repository = splitRepository(project.repositoryFullName());
        execute("""
                INSERT INTO project
                    (id, owner_user_id, name, github_installation_id, github_repository_id,
                     repository_owner, repository_name, default_branch, active,
                     created_at, updated_at, private_repository)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, statement -> bindProject(statement, ownerUserId, project, repository));
    }

    public void updateProject(UUID ownerUserId, ProjectResponse project) {
        if (!enabled) return;
        String[] repository = splitRepository(project.repositoryFullName());
        execute("""
                UPDATE project SET
                    name = ?, github_installation_id = ?, github_repository_id = ?,
                    repository_owner = ?, repository_name = ?, default_branch = ?, active = ?,
                    updated_at = ?, private_repository = ?
                WHERE id = ? AND owner_user_id = ?
                """, statement -> {
            statement.setString(1, project.name());
            statement.setLong(2, project.githubInstallationId());
            statement.setLong(3, project.githubRepositoryId());
            statement.setString(4, repository[0]);
            statement.setString(5, repository[1]);
            statement.setString(6, project.defaultBranch());
            statement.setBoolean(7, project.active());
            statement.setTimestamp(8, Timestamp.from(project.updatedAt()));
            statement.setBoolean(9, project.privateRepository());
            statement.setObject(10, project.id());
            statement.setObject(11, ownerUserId);
        });
    }

    public List<ProjectResponse> listProjects(UUID ownerUserId) {
        if (!enabled) return List.of();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, github_installation_id, github_repository_id,
                            repository_owner, repository_name, private_repository,
                            default_branch, active, created_at, updated_at
                     FROM project WHERE owner_user_id = ? ORDER BY created_at
                     """)) {
            statement.setObject(1, ownerUserId);
            try (ResultSet result = statement.executeQuery()) {
                List<ProjectResponse> projects = new ArrayList<>();
                while (result.next()) projects.add(mapProject(result));
                return List.copyOf(projects);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load project configuration.", e);
        }
    }

    public Optional<ProjectResponse> findProject(UUID ownerUserId, UUID projectId) {
        if (!enabled) return Optional.empty();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, name, github_installation_id, github_repository_id,
                            repository_owner, repository_name, private_repository,
                            default_branch, active, created_at, updated_at
                     FROM project WHERE owner_user_id = ? AND id = ?
                     """)) {
            statement.setObject(1, ownerUserId);
            statement.setObject(2, projectId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapProject(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load project configuration.", e);
        }
    }

    public boolean projectNameExists(UUID ownerUserId, String name, UUID excludedId) {
        if (!enabled) return false;
        String sql = excludedId == null
                ? "SELECT 1 FROM project WHERE owner_user_id = ? AND lower(name) = lower(?) LIMIT 1"
                : "SELECT 1 FROM project WHERE owner_user_id = ? AND lower(name) = lower(?) AND id <> ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, ownerUserId);
            statement.setString(2, name);
            if (excludedId != null) statement.setObject(3, excludedId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not validate project name.", e);
        }
    }

    private void bindProject(PreparedStatement statement, UUID ownerUserId, ProjectResponse project, String[] repository)
            throws SQLException {
        statement.setObject(1, project.id());
        statement.setObject(2, ownerUserId);
        statement.setString(3, project.name());
        statement.setLong(4, project.githubInstallationId());
        statement.setLong(5, project.githubRepositoryId());
        statement.setString(6, repository[0]);
        statement.setString(7, repository[1]);
        statement.setString(8, project.defaultBranch());
        statement.setBoolean(9, project.active());
        statement.setTimestamp(10, Timestamp.from(project.createdAt()));
        statement.setTimestamp(11, Timestamp.from(project.updatedAt()));
        statement.setBoolean(12, project.privateRepository());
    }

    private static ProjectResponse mapProject(ResultSet result) throws SQLException {
        String fullName = result.getString("repository_owner") + "/" + result.getString("repository_name");
        return new ProjectResponse(
                result.getObject("id", UUID.class), result.getString("name"),
                result.getLong("github_installation_id"), result.getLong("github_repository_id"),
                fullName, result.getBoolean("private_repository"), result.getString("default_branch"),
                result.getBoolean("active"), result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant());
    }

    private static String[] splitRepository(String fullName) {
        String[] parts = fullName == null ? new String[0] : fullName.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank())
            throw new IllegalArgumentException("repositoryFullName is invalid");
        return parts;
    }

    private void execute(String sql, SqlBinder binder) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not persist project configuration.", e);
        }
    }

    @FunctionalInterface
    private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
}
