package info.isaksson.erland.zipgithub.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migrationCreatesTablesAndOwnerForeignKeysPreventCrossUserLinks() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = POSTGRES.createConnection("")) {
            UUID ownerA = UUID.randomUUID();
            UUID ownerB = UUID.randomUUID();
            UUID project = UUID.randomUUID();
            insertUser(connection, ownerA, 1001L, "owner-a");
            insertUser(connection, ownerB, 1002L, "owner-b");
            // The same GitHub installation may be visible to multiple users; tenant identity includes owner_user_id.
            execute(connection, "INSERT INTO github_installation (id, owner_user_id, account_login, permissions_snapshot, repository_selection, created_at, updated_at) VALUES (10, ?, 'shared-org', '{}'::jsonb, 'selected', now(), now())", ownerA);
            execute(connection, "INSERT INTO github_installation (id, owner_user_id, account_login, permissions_snapshot, repository_selection, created_at, updated_at) VALUES (10, ?, 'shared-org', '{}'::jsonb, 'selected', now(), now())", ownerB);
            execute(connection, "INSERT INTO project (id, owner_user_id, name, github_installation_id, github_repository_id, repository_owner, repository_name, default_branch, active, created_at, updated_at, private_repository) VALUES (?, ?, 'A project', 10, 20, 'shared-org', 'repo', 'main', true, now(), now(), true)", project, ownerA);

            SQLException violation = assertThrows(SQLException.class, () -> execute(connection,
                    "INSERT INTO import_session (id, project_id, owner_user_id, base_branch, status, created_at, updated_at) VALUES (?, ?, ?, 'main', 'CREATED', now(), now())",
                    UUID.randomUUID(), project, ownerB));
            assertTrue(violation.getSQLState().startsWith("23"));
        }
    }

    private static void insertUser(Connection connection, UUID id, long githubId, String login) throws SQLException {
        execute(connection, "INSERT INTO user_account (id, github_user_id, github_login, created_at, last_login_at) VALUES (?, ?, ?, ?, ?)",
                id, githubId, login, Instant.now(), Instant.now());
    }

    private static void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value instanceof Instant instant) {
                    statement.setObject(i + 1, instant.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
                } else {
                    statement.setObject(i + 1, value);
                }
            }
            statement.executeUpdate();
        }
    }
}
