package info.isaksson.erland.zipgithub.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.delivery.GitCommitIdentity;
import info.isaksson.erland.zipgithub.delivery.GitDeliveryResult;
import info.isaksson.erland.zipgithub.domain.model.ImportAuditMetadata;
import info.isaksson.erland.zipgithub.domain.model.ImportSource;
import info.isaksson.erland.zipgithub.plan.ImmutableImportPlan;
import info.isaksson.erland.zipgithub.plan.ImportPlanApproval;
import info.isaksson.erland.zipgithub.selection.ApprovedSelection;
import info.isaksson.erland.zipgithub.snapshot.RepositorySnapshot;
import info.isaksson.erland.zipgithub.upload.StoredUpload;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Durable state required to resume an import after logout or backend restart. */
@ApplicationScoped
public class ImportResumePersistenceStore {
    @Inject AgroalDataSource dataSource;
    @Inject ObjectMapper objectMapper;
    @ConfigProperty(name = "zipgithub.persistence.projects.enabled", defaultValue = "true") boolean enabled;

    public boolean enabled() { return enabled; }

    public void insertImport(UUID ownerUserId, ImportResponse response, GitCommitIdentity identity, ImportAuditMetadata audit) {
        if (!enabled) return;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement s = c.prepareStatement("""
                    INSERT INTO import_session
                      (id, project_id, owner_user_id, base_branch, base_commit_sha, status, source_type, source_reference, created_at, updated_at)
                    VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, ?)
                    """)) {
                s.setObject(1, response.id()); s.setObject(2, response.projectId()); s.setObject(3, ownerUserId);
                s.setString(4, response.baseBranch()); s.setString(5, response.status()); s.setString(6, audit.source().name());
                s.setString(7, audit.sourceReference()); s.setTimestamp(8, Timestamp.from(response.createdAt()));
                s.setTimestamp(9, Timestamp.from(response.createdAt())); s.executeUpdate();
            }
            try (PreparedStatement s = c.prepareStatement("""
                    INSERT INTO import_resume_payload (import_session_id, owner_user_id, git_identity_json, updated_at)
                    VALUES (?, ?, ?, ?)""")) {
                s.setObject(1, response.id()); s.setObject(2, ownerUserId); s.setString(3, json(identity));
                s.setTimestamp(4, Timestamp.from(Instant.now())); s.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) { throw new IllegalStateException("Could not persist import session.", e); }
    }

    public void updateStatus(UUID ownerUserId, UUID importId, String status, String baseCommitSha) {
        if (!enabled) return;
        execute("""
                UPDATE import_session SET status = ?, base_commit_sha = COALESCE(?, base_commit_sha), updated_at = ?
                WHERE id = ? AND owner_user_id = ?""", s -> {
            s.setString(1, status); s.setString(2, baseCommitSha); s.setTimestamp(3, Timestamp.from(Instant.now()));
            s.setObject(4, importId); s.setObject(5, ownerUserId);
        });
    }

    public void saveUpload(UUID owner, UUID importId, StoredUpload value) { save(owner, importId, "upload_json", json(PersistedUpload.from(value))); }
    public void saveSnapshot(UUID owner, UUID importId, RepositorySnapshot value) { save(owner, importId, "snapshot_json", json(value)); }
    public void savePlan(UUID owner, UUID importId, ImmutableImportPlan value) { save(owner, importId, "plan_json", json(value)); }
    public void saveSelection(UUID owner, UUID importId, ApprovedSelection value) { save(owner, importId, "selection_json", json(value)); }
    public void saveApproval(UUID owner, UUID importId, ImportPlanApproval value) { save(owner, importId, "approval_json", json(value)); }
    public void saveDelivery(UUID owner, UUID importId, GitDeliveryResult value) { save(owner, importId, "delivery_json", json(value)); }

    public Optional<ResumeState> find(UUID owner, UUID importId) {
        if (!enabled) return Optional.empty();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                SELECT i.id, i.project_id, i.base_branch, i.status, i.source_type, i.source_reference, i.created_at,
                       p.upload_json, p.snapshot_json, p.plan_json, p.selection_json, p.approval_json,
                       p.git_identity_json, p.delivery_json
                FROM import_session i JOIN import_resume_payload p ON p.import_session_id = i.id
                WHERE i.id = ? AND i.owner_user_id = ?""")) {
            s.setObject(1, importId); s.setObject(2, owner);
            try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(map(owner, r)) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException("Could not load resumable import.", e); }
    }

    public List<ResumeState> list(UUID owner, UUID projectId) {
        if (!enabled) return List.of();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                SELECT i.id, i.project_id, i.base_branch, i.status, i.source_type, i.source_reference, i.created_at,
                       p.upload_json, p.snapshot_json, p.plan_json, p.selection_json, p.approval_json,
                       p.git_identity_json, p.delivery_json
                FROM import_session i JOIN import_resume_payload p ON p.import_session_id = i.id
                WHERE i.owner_user_id = ? AND i.project_id = ? ORDER BY i.created_at DESC""")) {
            s.setObject(1, owner); s.setObject(2, projectId);
            try (ResultSet r = s.executeQuery()) {
                List<ResumeState> out = new ArrayList<>(); while (r.next()) out.add(map(owner, r)); return List.copyOf(out);
            }
        } catch (SQLException e) { throw new IllegalStateException("Could not list resumable imports.", e); }
    }


    public List<StoredUpload> listExpiredTerminalUploads(Instant now) {
        if (!enabled) return List.of();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement("""
                SELECT p.upload_json FROM import_session i
                JOIN import_resume_payload p ON p.import_session_id = i.id
                WHERE i.status IN ('PUSHED', 'PULL_REQUEST_CREATED', 'CANCELLED') AND p.upload_json IS NOT NULL
                """)) {
            try (ResultSet r = s.executeQuery()) {
                List<StoredUpload> out = new ArrayList<>();
                while (r.next()) {
                    StoredUpload upload = read(r.getString(1), PersistedUpload.class).map(PersistedUpload::toDomain).orElse(null);
                    if (upload != null && !upload.retentionDeadline().isAfter(now)) out.add(upload);
                }
                return List.copyOf(out);
            }
        } catch (SQLException e) { throw new IllegalStateException("Could not list expired resumable uploads.", e); }
    }

    public void clearUpload(UUID owner, UUID importId) {
        if (!enabled) return;
        execute("UPDATE import_resume_payload SET upload_json = NULL, updated_at = ? WHERE import_session_id = ? AND owner_user_id = ?", s -> {
            s.setTimestamp(1, Timestamp.from(Instant.now())); s.setObject(2, importId); s.setObject(3, owner);
        });
    }

    private ResumeState map(UUID owner, ResultSet r) throws SQLException {
        ImportResponse response = new ImportResponse(r.getObject("id", UUID.class), r.getObject("project_id", UUID.class),
                r.getString("base_branch"), r.getString("status"), r.getTimestamp("created_at").toInstant());
        ImportAuditMetadata audit = new ImportAuditMetadata(ImportSource.valueOf(r.getString("source_type")), r.getString("source_reference"));
        return new ResumeState(owner, response, read(r.getString("upload_json"), PersistedUpload.class).map(PersistedUpload::toDomain).orElse(null),
                read(r.getString("snapshot_json"), RepositorySnapshot.class).orElse(null),
                read(r.getString("plan_json"), ImmutableImportPlan.class).orElse(null),
                read(r.getString("selection_json"), ApprovedSelection.class).orElse(null),
                read(r.getString("approval_json"), ImportPlanApproval.class).orElse(null),
                read(r.getString("git_identity_json"), GitCommitIdentity.class).orElse(null), audit,
                read(r.getString("delivery_json"), GitDeliveryResult.class).orElse(null));
    }

    private void save(UUID owner, UUID importId, String column, String json) {
        if (!enabled) return;
        if (!List.of("upload_json","snapshot_json","plan_json","selection_json","approval_json","delivery_json").contains(column))
            throw new IllegalArgumentException("Unsupported resume column");
        execute("UPDATE import_resume_payload SET " + column + " = ?, updated_at = ? WHERE import_session_id = ? AND owner_user_id = ?", s -> {
            s.setString(1, json); s.setTimestamp(2, Timestamp.from(Instant.now())); s.setObject(3, importId); s.setObject(4, owner);
        });
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not serialize resumable import state.", e); }
    }
    private <T> Optional<T> read(String value, Class<T> type) {
        if (value == null) return Optional.empty();
        try { return Optional.of(objectMapper.readValue(value, type)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Could not deserialize resumable import state.", e); }
    }
    private void execute(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { binder.bind(s); s.executeUpdate(); }
        catch (SQLException e) { throw new IllegalStateException("Could not persist resumable import state.", e); }
    }
    @FunctionalInterface private interface Binder { void bind(PreparedStatement s) throws SQLException; }

    public record ResumeState(UUID ownerUserId, ImportResponse response, StoredUpload upload, RepositorySnapshot snapshot,
                              ImmutableImportPlan plan, ApprovedSelection selection, ImportPlanApproval approval,
                              GitCommitIdentity identity, ImportAuditMetadata auditMetadata, GitDeliveryResult delivery) {}
    private record PersistedUpload(UUID id, UUID importId, UUID ownerUserId, String originalFilename, long sizeBytes,
                                   String sha256, String storagePath, Instant createdAt, Instant retentionDeadline) {
        static PersistedUpload from(StoredUpload u) { return new PersistedUpload(u.id(), u.importId(), u.ownerUserId(), u.originalFilename(), u.sizeBytes(), u.sha256(), u.storagePath().toString(), u.createdAt(), u.retentionDeadline()); }
        StoredUpload toDomain() { return new StoredUpload(id, importId, ownerUserId, originalFilename, sizeBytes, sha256, Path.of(storagePath), createdAt, retentionDeadline); }
    }
}
