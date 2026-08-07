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
            execute(connection, "INSERT INTO project (id, owner_user_id, name, default_branch, active, created_at, updated_at) VALUES (?, ?, 'A project', 'main', true, now(), now())", project, ownerA);

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
