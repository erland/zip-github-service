package info.isaksson.erland.zipgithub.persistence;

import info.isaksson.erland.zipgithub.application.WorkSession;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WorkPersistenceStore {
    @Inject AgroalDataSource dataSource;

    public Optional<WorkSession> findActive(UUID ownerUserId, UUID projectId) {
        return find("SELECT * FROM work_session WHERE owner_user_id=? AND project_id=? AND status='ACTIVE'", ownerUserId, projectId);
    }

    public WorkSession getOrCreateActive(UUID ownerUserId, UUID projectId, String baseBranch) {
        var existing = findActive(ownerUserId, projectId);
        if (existing.isPresent()) return existing.get();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String branch = "zip-github/work-" + id;
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                INSERT INTO work_session (id, project_id, owner_user_id, base_branch, branch_name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                ON CONFLICT DO NOTHING
                """)) {
            s.setObject(1,id); s.setObject(2,projectId); s.setObject(3,ownerUserId); s.setString(4,baseBranch); s.setString(5,branch);
            s.setTimestamp(6,Timestamp.from(now)); s.setTimestamp(7,Timestamp.from(now)); s.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Could not create work session.", e); }
        return findActive(ownerUserId, projectId).orElseThrow(() -> new IllegalStateException("Could not load active work session."));
    }

    public WorkSession recordCommit(UUID ownerUserId, UUID projectId, UUID importId, String baseCommitSha,
                                    String commitSha, String planDigestSha256) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET head_commit_sha=?, base_commit_sha=COALESCE(base_commit_sha, ?),
                    last_import_id=?, last_plan_digest_sha256=?, updated_at=?
                WHERE owner_user_id=? AND project_id=? AND status='ACTIVE'
                """)) {
            s.setString(1,commitSha); s.setString(2,baseCommitSha); s.setObject(3,importId); s.setString(4,planDigestSha256);
            s.setTimestamp(5,Timestamp.from(Instant.now())); s.setObject(6,ownerUserId); s.setObject(7,projectId);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No active work session could be updated.");
        } catch (SQLException e) { throw new IllegalStateException("Could not persist work commit.", e); }
        return findActive(ownerUserId, projectId).orElseThrow();
    }

    public WorkSession recordPullRequest(UUID ownerUserId, UUID projectId, long number, String url) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET status='PULL_REQUEST_CREATED', pull_request_number=?, pull_request_url=?, updated_at=?
                WHERE owner_user_id=? AND project_id=? AND status='ACTIVE' AND head_commit_sha IS NOT NULL
                """)) {
            s.setLong(1,number); s.setString(2,url); s.setTimestamp(3,Timestamp.from(Instant.now())); s.setObject(4,ownerUserId); s.setObject(5,projectId);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No committable active work session could be finalized.");
        } catch (SQLException e) { throw new IllegalStateException("Could not persist work pull request.", e); }
        return findLatest(ownerUserId, projectId).orElseThrow();
    }

    public Optional<WorkSession> findLatest(UUID ownerUserId, UUID projectId) {
        return find("SELECT * FROM work_session WHERE owner_user_id=? AND project_id=? ORDER BY created_at DESC LIMIT 1", ownerUserId, projectId);
    }

    private Optional<WorkSession> find(String sql, UUID ownerUserId, UUID projectId) {
        try (Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setObject(1,ownerUserId); s.setObject(2,projectId);
            try (ResultSet r=s.executeQuery()) { return r.next()?Optional.of(map(r)):Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException("Could not load work session.", e); }
    }

    private static WorkSession map(ResultSet r) throws SQLException {
        return new WorkSession(r.getObject("id",UUID.class), r.getObject("project_id",UUID.class), r.getObject("owner_user_id",UUID.class),
                r.getString("base_branch"), r.getString("branch_name"), r.getString("status"), r.getString("head_commit_sha"),
                r.getString("base_commit_sha"), r.getObject("last_import_id",UUID.class), r.getString("last_plan_digest_sha256"),
                (Long)r.getObject("pull_request_number"), r.getString("pull_request_url"), r.getTimestamp("created_at").toInstant(), r.getTimestamp("updated_at").toInstant());
    }
}
