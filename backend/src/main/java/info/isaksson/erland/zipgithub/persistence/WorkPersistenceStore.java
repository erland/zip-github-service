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
        return find("SELECT * FROM work_session WHERE owner_user_id=? AND project_id=? AND status IN ('ACTIVE','PR_OPEN','PR_CLOSED')", ownerUserId, projectId);
    }

    public WorkSession createProvisioning(UUID ownerUserId, UUID projectId, String baseBranch, String branchName, String baseCommitSha) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                INSERT INTO work_session (id, project_id, owner_user_id, base_branch, branch_name, status, base_commit_sha, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'PROVISIONING', ?, ?, ?)
                """)) {
            s.setObject(1,id); s.setObject(2,projectId); s.setObject(3,ownerUserId); s.setString(4,baseBranch); s.setString(5,branchName); s.setString(6,baseCommitSha);
            s.setTimestamp(7,Timestamp.from(now)); s.setTimestamp(8,Timestamp.from(now)); s.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Could not create provisioning work session.", e); }
        return findOpen(ownerUserId, projectId).orElseThrow(() -> new IllegalStateException("Could not load provisioning work session."));
    }

    public Optional<WorkSession> findOpen(UUID ownerUserId, UUID projectId) {
        return find("SELECT * FROM work_session WHERE owner_user_id=? AND project_id=? AND status IN ('PROVISIONING','ACTIVE','PR_OPEN','PR_CLOSED')", ownerUserId, projectId);
    }

    public WorkSession activate(UUID ownerUserId, UUID projectId, String expectedBranch, String baseCommitSha) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET status='ACTIVE', base_commit_sha=?, updated_at=?
                WHERE owner_user_id=? AND project_id=? AND status='PROVISIONING' AND branch_name=?
                """)) {
            s.setString(1,baseCommitSha); s.setTimestamp(2,Timestamp.from(Instant.now())); s.setObject(3,ownerUserId); s.setObject(4,projectId); s.setString(5,expectedBranch);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No provisioning work session could be activated.");
        } catch (SQLException e) { throw new IllegalStateException("Could not activate work session.", e); }
        return findActive(ownerUserId, projectId).orElseThrow();
    }

    public WorkSession abandon(UUID ownerUserId, UUID projectId) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET status='ABANDONED', updated_at=?
                WHERE owner_user_id=? AND project_id=? AND status IN ('PROVISIONING','ACTIVE','PR_OPEN','PR_CLOSED')
                """)) {
            s.setTimestamp(1,Timestamp.from(Instant.now())); s.setObject(2,ownerUserId); s.setObject(3,projectId);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No active work session could be abandoned.");
        } catch (SQLException e) { throw new IllegalStateException("Could not abandon work session.", e); }
        return findLatest(ownerUserId, projectId).orElseThrow();
    }

    public boolean activeBranchInUse(UUID ownerUserId, UUID projectId, String branchName) {
        try (Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("SELECT 1 FROM work_session WHERE owner_user_id=? AND project_id=? AND status IN ('PROVISIONING','ACTIVE','PR_OPEN','PR_CLOSED') AND branch_name=? LIMIT 1")) {
            s.setObject(1,ownerUserId); s.setObject(2,projectId); s.setString(3,branchName);
            try (ResultSet r=s.executeQuery()) { return r.next(); }
        } catch (SQLException e) { throw new IllegalStateException("Could not validate active work branch.", e); }
    }

    public boolean nonTerminalBranchInUse(long installationId, long repositoryId, String branchName) {
        try (Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("""
                SELECT 1
                FROM work_session w
                JOIN project p ON p.id=w.project_id AND p.owner_user_id=w.owner_user_id
                WHERE p.github_installation_id=? AND p.github_repository_id=?
                  AND w.status IN ('PROVISIONING','ACTIVE','PR_OPEN','PR_CLOSED')
                  AND w.branch_name=?
                LIMIT 1
                """)) {
            s.setLong(1,installationId); s.setLong(2,repositoryId); s.setString(3,branchName);
            try (ResultSet r=s.executeQuery()) { return r.next(); }
        } catch (SQLException e) { throw new IllegalStateException("Could not validate repository Work branch usage.", e); }
    }

    public WorkSession recordCommit(UUID ownerUserId, UUID projectId, UUID importId, String baseCommitSha,
                                    String commitSha, String planDigestSha256) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET head_commit_sha=?, base_commit_sha=COALESCE(base_commit_sha, ?),
                    last_import_id=?, last_plan_digest_sha256=?, updated_at=?
                WHERE owner_user_id=? AND project_id=? AND status IN ('ACTIVE','PR_OPEN','PR_CLOSED')
                """)) {
            s.setString(1,commitSha); s.setString(2,baseCommitSha); s.setObject(3,importId); s.setString(4,planDigestSha256);
            s.setTimestamp(5,Timestamp.from(Instant.now())); s.setObject(6,ownerUserId); s.setObject(7,projectId);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No active work session could be updated.");
        } catch (SQLException e) { throw new IllegalStateException("Could not persist work commit.", e); }
        return findActive(ownerUserId, projectId).orElseThrow();
    }

    public WorkSession recordPullRequest(UUID ownerUserId, UUID projectId, long number, String url) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET status='PR_OPEN', pull_request_number=?, pull_request_url=?, updated_at=?
                WHERE owner_user_id=? AND project_id=? AND status IN ('ACTIVE','PR_CLOSED','PR_OPEN') AND head_commit_sha IS NOT NULL
                """)) {
            s.setLong(1,number); s.setString(2,url); s.setTimestamp(3,Timestamp.from(Instant.now())); s.setObject(4,ownerUserId); s.setObject(5,projectId);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No committable work session could record the pull request.");
        } catch (SQLException e) { throw new IllegalStateException("Could not persist work pull request.", e); }
        return findLatest(ownerUserId, projectId).orElseThrow();
    }


    public WorkSession updatePullRequestState(UUID ownerUserId, UUID projectId, String status) {
        if (!java.util.Set.of("PR_OPEN", "PR_CLOSED", "MERGED").contains(status))
            throw new IllegalArgumentException("Unsupported pull request work status: " + status);
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                UPDATE work_session SET status=?, updated_at=?
                WHERE owner_user_id=? AND project_id=? AND pull_request_number IS NOT NULL
                  AND status IN ('PR_OPEN','PR_CLOSED')
                """)) {
            s.setString(1,status); s.setTimestamp(2,Timestamp.from(Instant.now())); s.setObject(3,ownerUserId); s.setObject(4,projectId);
            if (s.executeUpdate()!=1) throw new IllegalStateException("No pull-request Work session could be updated.");
        } catch (SQLException e) { throw new IllegalStateException("Could not update Work pull request state.", e); }
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
