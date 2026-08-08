package info.isaksson.erland.zipgithub.actions;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.*;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ActionsControlAuditStore {
    @Inject AgroalDataSource dataSource;
    @ConfigProperty(name = "zipgithub.persistence.projects.enabled", defaultValue = "true") boolean persistent;
    private final Map<Key, ActionsControlAudit> memory = new ConcurrentHashMap<>();

    public CreateResult create(UUID ownerUserId, UUID projectId, UUID importId, String operation,
                               String workflowIdentifier, Long workflowId, Long workflowRunId, String branchRef,
                               String targetCommitSha, String idempotencyKey) {
        if (!persistent) return createMemory(ownerUserId, projectId, importId, operation, workflowIdentifier,
                workflowId, workflowRunId, branchRef, targetCommitSha, idempotencyKey);
        UUID id = UUID.randomUUID(); Instant now = Instant.now();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                INSERT INTO actions_control_audit
                  (id, owner_user_id, project_id, import_id, operation, workflow_identifier, workflow_id,
                   workflow_run_id, branch_ref, target_commit_sha, idempotency_key, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'STARTED', ?, ?)
                ON CONFLICT (owner_user_id, import_id, operation, idempotency_key) DO NOTHING
                """)) {
            s.setObject(1,id); s.setObject(2,ownerUserId); s.setObject(3,projectId); s.setObject(4,importId);
            s.setString(5,operation); s.setString(6,workflowIdentifier); setLong(s,7,workflowId); setLong(s,8,workflowRunId);
            s.setString(9,branchRef); s.setString(10,targetCommitSha); s.setString(11,idempotencyKey);
            s.setTimestamp(12,Timestamp.from(now)); s.setTimestamp(13,Timestamp.from(now));
            boolean created = s.executeUpdate() == 1;
            ActionsControlAudit audit = find(ownerUserId, importId, operation, idempotencyKey).orElseThrow();
            return new CreateResult(audit, created);
        } catch (SQLException e) { throw new IllegalStateException("Could not persist Actions control audit.", e); }
    }

    public ActionsControlAudit succeed(ActionsControlAudit audit, Long workflowId, Long workflowRunId, String githubUrl) {
        return update(audit, "SUCCEEDED", workflowId, workflowRunId, githubUrl, null);
    }
    public ActionsControlAudit fail(ActionsControlAudit audit, String errorCode) {
        return update(audit, "FAILED", audit.workflowId(), audit.workflowRunId(), audit.githubUrl(), errorCode);
    }

    private CreateResult createMemory(UUID ownerUserId, UUID projectId, UUID importId, String operation,
                                      String workflowIdentifier, Long workflowId, Long workflowRunId, String branchRef,
                                      String targetCommitSha, String idempotencyKey) {
        Key key = new Key(ownerUserId, importId, operation, idempotencyKey); Instant now = Instant.now();
        ActionsControlAudit fresh = new ActionsControlAudit(UUID.randomUUID(), ownerUserId, projectId, importId, operation,
                workflowIdentifier, workflowId, workflowRunId, branchRef, targetCommitSha, idempotencyKey,
                "STARTED", null, null, now, now);
        ActionsControlAudit existing = memory.putIfAbsent(key, fresh);
        return new CreateResult(existing == null ? fresh : existing, existing == null);
    }

    private ActionsControlAudit update(ActionsControlAudit audit, String status, Long workflowId, Long workflowRunId,
                                       String githubUrl, String errorCode) {
        Instant now = Instant.now();
        if (!persistent) {
            ActionsControlAudit updated = new ActionsControlAudit(audit.id(), audit.ownerUserId(), audit.projectId(), audit.importId(),
                    audit.operation(), audit.workflowIdentifier(), workflowId, workflowRunId, audit.branchRef(), audit.targetCommitSha(),
                    audit.idempotencyKey(), status, githubUrl, errorCode, audit.createdAt(), now);
            memory.put(new Key(audit.ownerUserId(), audit.importId(), audit.operation(), audit.idempotencyKey()), updated);
            return updated;
        }
        try (Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("""
                UPDATE actions_control_audit SET status=?, workflow_id=?, workflow_run_id=?, github_url=?, error_code=?, updated_at=?
                WHERE id=? AND owner_user_id=?
                """)) {
            s.setString(1,status); setLong(s,2,workflowId); setLong(s,3,workflowRunId); s.setString(4,githubUrl);
            s.setString(5,errorCode); s.setTimestamp(6,Timestamp.from(now)); s.setObject(7,audit.id()); s.setObject(8,audit.ownerUserId());
            if (s.executeUpdate()!=1) throw new IllegalStateException("Actions control audit disappeared during update.");
            return find(audit.ownerUserId(), audit.importId(), audit.operation(), audit.idempotencyKey()).orElseThrow();
        } catch (SQLException e) { throw new IllegalStateException("Could not update Actions control audit.", e); }
    }

    private Optional<ActionsControlAudit> find(UUID ownerUserId, UUID importId, String operation, String idempotencyKey) {
        if (!persistent) return Optional.ofNullable(memory.get(new Key(ownerUserId,importId,operation,idempotencyKey)));
        try (Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("""
                SELECT * FROM actions_control_audit WHERE owner_user_id=? AND import_id=? AND operation=? AND idempotency_key=?
                """)) {
            s.setObject(1,ownerUserId); s.setObject(2,importId); s.setString(3,operation); s.setString(4,idempotencyKey);
            try (ResultSet r=s.executeQuery()) { return r.next()?Optional.of(map(r)):Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException("Could not load Actions control audit.", e); }
    }

    private static ActionsControlAudit map(ResultSet r) throws SQLException {
        return new ActionsControlAudit(r.getObject("id",UUID.class), r.getObject("owner_user_id",UUID.class), r.getObject("project_id",UUID.class),
                r.getObject("import_id",UUID.class), r.getString("operation"), r.getString("workflow_identifier"),
                (Long)r.getObject("workflow_id"), (Long)r.getObject("workflow_run_id"), r.getString("branch_ref"), r.getString("target_commit_sha"),
                r.getString("idempotency_key"), r.getString("status"), r.getString("github_url"), r.getString("error_code"),
                r.getTimestamp("created_at").toInstant(), r.getTimestamp("updated_at").toInstant());
    }
    private static void setLong(PreparedStatement s, int index, Long value) throws SQLException {
        if (value == null) s.setNull(index, Types.BIGINT); else s.setLong(index, value);
    }
    public record CreateResult(ActionsControlAudit audit, boolean created) {}
    private record Key(UUID ownerUserId, UUID importId, String operation, String idempotencyKey) {}
}
