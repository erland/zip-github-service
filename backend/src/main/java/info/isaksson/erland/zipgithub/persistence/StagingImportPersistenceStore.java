package info.isaksson.erland.zipgithub.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.isaksson.erland.zipgithub.domain.model.StagingImport;
import info.isaksson.erland.zipgithub.domain.status.StagingImportStatus;
import info.isaksson.erland.zipgithub.staging.StagingCapacityExceededException;
import info.isaksson.erland.zipgithub.upload.GitFileMode;
import info.isaksson.erland.zipgithub.upload.StoredUploadArtifact;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Durable transactional store for the pre-authentication staging lifecycle. No raw claim token enters this store. */
@ApplicationScoped
public class StagingImportPersistenceStore {
    @Inject AgroalDataSource dataSource;
    @Inject ObjectMapper objectMapper;

    public void insert(StagingImport value) {
        insertWithinLimits(value, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Serializes staging-create quota decisions in PostgreSQL so concurrent Shortcut uploads cannot all pass
     * a stale capacity check. The quota counts every not-yet-promoted artifact that has not been physically deleted.
     */
    public void insertWithinLimits(StagingImport value, long maximumObjects, long maximumBytes) {
        if (maximumObjects <= 0 || maximumBytes <= 0) throw new IllegalArgumentException("staging limits must be positive");
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            // One short transaction-level lock only around quota accounting + row insert.
            try (PreparedStatement lock = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                lock.setLong(1, 0x5A495047485542L); // "ZIPGHUB"-scoped deterministic key
                lock.execute();
            }
            LiveUsage usage;
            try (PreparedStatement q = c.prepareStatement("""
                    SELECT count(*), COALESCE(sum(size_bytes),0)
                    FROM staging_import
                    WHERE status <> 'PROMOTED' AND artifact_deleted_at IS NULL
                    """)) {
                try (ResultSet r = q.executeQuery()) {
                    r.next(); usage = new LiveUsage(r.getLong(1), r.getLong(2));
                }
            }
            long requestedBytes = value.artifact().sizeBytes();
            if (usage.objects() >= maximumObjects || requestedBytes > maximumBytes - Math.min(usage.bytes(), maximumBytes)) {
                c.rollback();
                throw new StagingCapacityExceededException("Staging capacity is temporarily full.");
            }
            insert(c, value);
            c.commit();
        } catch (StagingCapacityExceededException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not persist staging import.", e);
        }
    }

    private void insert(Connection c, StagingImport value) throws SQLException {
        try (PreparedStatement s=c.prepareStatement("""
                INSERT INTO staging_import
                (id,artifact_id,original_filename,storage_path,size_bytes,sha256,file_modes_json,claim_token_sha256,status,created_at,expires_at,artifact_retention_deadline,updated_at)
                VALUES (?,?,?,?,?,?,?::jsonb,?,'AVAILABLE',?,?,?,?)""")) {
            var a=value.artifact();
            s.setObject(1,value.id()); s.setObject(2,a.id()); s.setString(3,a.originalFilename()); s.setString(4,a.storagePath().toString());
            s.setLong(5,a.sizeBytes()); s.setString(6,a.sha256()); s.setString(7,json(a.fileModes())); s.setString(8,value.claimTokenSha256());
            s.setTimestamp(9,Timestamp.from(value.createdAt())); s.setTimestamp(10,Timestamp.from(value.expiresAt())); s.setTimestamp(11,Timestamp.from(a.retentionDeadline())); s.setTimestamp(12,Timestamp.from(value.updatedAt())); s.executeUpdate();
        }
    }

    public Optional<StagingImport> find(UUID id) {
        try(Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM staging_import WHERE id=?")){
            s.setObject(1,id); try(ResultSet r=s.executeQuery()){ return r.next()?Optional.of(map(r)):Optional.empty(); }
        } catch(SQLException e){ throw new IllegalStateException("Could not load staging import.",e); }
    }

    public Optional<StagingImport> findOwned(UUID id, UUID owner) {
        try(Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM staging_import WHERE id=? AND owner_user_id=?")){
            s.setObject(1,id); s.setObject(2,owner); try(ResultSet r=s.executeQuery()){ return r.next()?Optional.of(map(r)):Optional.empty(); }
        } catch(SQLException e){ throw new IllegalStateException("Could not load owned staging import.",e); }
    }

    /** Atomically claims an AVAILABLE, unexpired row by bearer-token hash. Same owner retry is idempotent. */
    public ClaimOutcome claimByTokenHash(String claimTokenSha256, UUID owner, Instant now, Instant claimedExpiresAt) {
        if (!claimedExpiresAt.isAfter(now)) throw new IllegalArgumentException("claimedExpiresAt must be after now");
        try(Connection c=dataSource.getConnection()){ c.setAutoCommit(false);
            StagingImport current;
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM staging_import WHERE claim_token_sha256=? FOR UPDATE")){
                s.setString(1,claimTokenSha256); try(ResultSet r=s.executeQuery()){
                    if(!r.next()){ c.rollback(); return new ClaimOutcome(ClaimResult.NOT_AVAILABLE, null); }
                    current=map(r);
                }}
            if(current.status()==StagingImportStatus.CLAIMED && owner.equals(current.ownerUserId())){
                if(now.isBefore(current.expiresAt())) {
                    c.commit(); return new ClaimOutcome(ClaimResult.ALREADY_CLAIMED_BY_OWNER, current);
                }
                try(PreparedStatement u=c.prepareStatement("UPDATE staging_import SET status='EXPIRED',updated_at=? WHERE id=? AND status='CLAIMED'")){
                    u.setTimestamp(1,Timestamp.from(now));u.setObject(2,current.id());u.executeUpdate();
                }
                c.commit(); return new ClaimOutcome(ClaimResult.NOT_AVAILABLE, null);
            }
            if(current.status()!=StagingImportStatus.AVAILABLE){ c.rollback(); return new ClaimOutcome(ClaimResult.NOT_AVAILABLE, null); }
            if(!now.isBefore(current.expiresAt())){
                try(PreparedStatement u=c.prepareStatement("UPDATE staging_import SET status='EXPIRED',updated_at=? WHERE id=? AND status='AVAILABLE'")){
                    u.setTimestamp(1,Timestamp.from(now)); u.setObject(2,current.id()); u.executeUpdate();
                }
                c.commit(); return new ClaimOutcome(ClaimResult.NOT_AVAILABLE, null);
            }
            try(PreparedStatement u=c.prepareStatement("""
                    UPDATE staging_import
                    SET status='CLAIMED',owner_user_id=?,claimed_at=?,expires_at=?,updated_at=?
                    WHERE id=? AND status='AVAILABLE'""")){
                u.setObject(1,owner); u.setTimestamp(2,Timestamp.from(now)); u.setTimestamp(3,Timestamp.from(claimedExpiresAt));
                u.setTimestamp(4,Timestamp.from(now)); u.setObject(5,current.id());
                if(u.executeUpdate()!=1) throw new SQLException("staging claim lost row lock invariant");
            }
            current.claim(owner, now, claimedExpiresAt);
            c.commit(); return new ClaimOutcome(ClaimResult.CLAIMED, current);
        } catch(SQLException e){ throw new IllegalStateException("Could not claim staging import.",e); }
    }

    /** Compatibility overload for older internal/unit callers; it preserves the existing deadline. */
    public ClaimOutcome claimByTokenHash(String claimTokenSha256, UUID owner, Instant now) {
        Optional<StagingImport> existing = findByTokenHash(claimTokenSha256);
        Instant deadline = existing.map(StagingImport::expiresAt).filter(value -> value.isAfter(now)).orElse(now.plusSeconds(1));
        return claimByTokenHash(claimTokenSha256, owner, now, deadline);
    }

    private Optional<StagingImport> findByTokenHash(String tokenHash) {
        try(Connection c=dataSource.getConnection(); PreparedStatement s=c.prepareStatement("SELECT * FROM staging_import WHERE claim_token_sha256=?")){
            s.setString(1, tokenHash); try(ResultSet r=s.executeQuery()){ return r.next()?Optional.of(map(r)):Optional.empty(); }
        } catch(SQLException e){ throw new IllegalStateException("Could not load staging import.",e); }
    }

    /**
     * Holds the staging row lock for the whole create/recover ordinary-Import operation. Cleanup uses the same
     * row locks, therefore it cannot expire/delete the artifact while promotion is in flight, even across nodes.
     */
    public PromotionOutcome promoteWithLock(UUID id, UUID owner, Instant now, PromotionAction action) {
        try(Connection c=dataSource.getConnection()){ c.setAutoCommit(false);
            StagingImport current;
            try(PreparedStatement s=c.prepareStatement("SELECT * FROM staging_import WHERE id=? FOR UPDATE")){
                s.setObject(1,id); try(ResultSet r=s.executeQuery()){
                    if(!r.next() || !owner.equals(r.getObject("owner_user_id",UUID.class))){ c.rollback(); return new PromotionOutcome(PromotionResult.NOT_AVAILABLE,null); }
                    current=map(r);
                }}
            if(current.status()==StagingImportStatus.PROMOTED){ c.commit(); return new PromotionOutcome(PromotionResult.ALREADY_PROMOTED,current.promotedImportId()); }
            if(current.status()!=StagingImportStatus.CLAIMED){ c.rollback(); return new PromotionOutcome(PromotionResult.NOT_CLAIMED,null); }
            if(!now.isBefore(current.expiresAt())){
                try(PreparedStatement u=c.prepareStatement("UPDATE staging_import SET status='EXPIRED',updated_at=? WHERE id=? AND status='CLAIMED'")){
                    u.setTimestamp(1,Timestamp.from(now));u.setObject(2,id);u.executeUpdate();
                }
                c.commit(); return new PromotionOutcome(PromotionResult.EXPIRED,null);
            }
            UUID importId = action.createOrRecover(current);
            if(importId==null) throw new IllegalStateException("promotion action returned no import id");
            try(PreparedStatement u=c.prepareStatement("""
                    UPDATE staging_import SET status='PROMOTED',promoted_import_id=?,promoted_at=?,updated_at=?
                    WHERE id=? AND owner_user_id=? AND status='CLAIMED'""")){
                u.setObject(1,importId);u.setTimestamp(2,Timestamp.from(now));u.setTimestamp(3,Timestamp.from(now));u.setObject(4,id);u.setObject(5,owner);
                if(u.executeUpdate()!=1) throw new SQLException("staging promotion lost row lock invariant");
            }
            c.commit(); return new PromotionOutcome(PromotionResult.PROMOTED,importId);
        } catch(SQLException e){ throw new IllegalStateException("Could not promote staging import.",e); }
    }

    /** Existing primitive retained for compatibility; normal phase-9 promotion uses promoteWithLock. */
    public boolean markPromoted(UUID id, UUID owner, UUID importId, Instant now) {
        PromotionOutcome result = promoteWithLock(id, owner, now, ignored -> importId);
        if (result.result()==PromotionResult.ALREADY_PROMOTED && importId.equals(result.importId())) return false;
        if (result.result()!=PromotionResult.PROMOTED) throw new IllegalStateException("staging import could not be promoted: "+result.result());
        return true;
    }

    /**
     * Reconciles crash windows and claims terminal artifacts for physical deletion. Returned rows are already
     * terminal, so promotion cannot begin after this method commits. PROMOTED rows are never returned.
     */
    public List<CleanupCandidate> claimCleanupCandidates(Instant now, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
        try(Connection c=dataSource.getConnection()){ c.setAutoCommit(false);
            // If an ordinary Import exists from a prior crash window, staging ownership has already transferred.
            try(PreparedStatement reconcile=c.prepareStatement("""
                    UPDATE staging_import s
                    SET status='PROMOTED', promoted_import_id=i.id,
                        promoted_at=COALESCE(s.promoted_at, i.created_at), updated_at=?
                    FROM import_session i
                    WHERE s.status='CLAIMED'
                      AND i.owner_user_id=s.owner_user_id
                      AND i.source_type='STAGING_IMPORT'
                      AND i.source_reference=('staging-import:' || s.id::text)
                    """)){
                reconcile.setTimestamp(1,Timestamp.from(now)); reconcile.executeUpdate();
            }
            List<CleanupCandidate> out=new ArrayList<>();
            try(PreparedStatement s=c.prepareStatement("""
                    SELECT * FROM staging_import
                    WHERE artifact_deleted_at IS NULL
                      AND (
                        status IN ('EXPIRED','CANCELLED')
                        OR (status IN ('AVAILABLE','CLAIMED') AND expires_at <= ?)
                      )
                    ORDER BY expires_at,id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                    """)){
                s.setTimestamp(1,Timestamp.from(now));s.setInt(2,limit);
                try(ResultSet r=s.executeQuery()){
                    while(r.next()){
                        StagingImport item=map(r);
                        if(item.status()==StagingImportStatus.AVAILABLE || item.status()==StagingImportStatus.CLAIMED){
                            try(PreparedStatement u=c.prepareStatement("UPDATE staging_import SET status='EXPIRED',updated_at=? WHERE id=? AND status IN ('AVAILABLE','CLAIMED')")){
                                u.setTimestamp(1,Timestamp.from(now));u.setObject(2,item.id());u.executeUpdate();
                            }
                        }
                        out.add(new CleanupCandidate(item.id(),item.artifact()));
                    }
                }
            }
            c.commit();return out;
        }catch(SQLException e){throw new IllegalStateException("Could not claim staging cleanup candidates.",e);}
    }

    public void markArtifactDeleted(UUID id, Instant now) {
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement("""
                UPDATE staging_import SET artifact_deleted_at=?,updated_at=?
                WHERE id=? AND status IN ('EXPIRED','CANCELLED') AND artifact_deleted_at IS NULL
                """)){
            s.setTimestamp(1,Timestamp.from(now));s.setTimestamp(2,Timestamp.from(now));s.setObject(3,id);s.executeUpdate();
        }catch(SQLException e){throw new IllegalStateException("Could not record staging artifact cleanup.",e);}
    }

    public LiveUsage liveUsage() {
        try(Connection c=dataSource.getConnection();PreparedStatement s=c.prepareStatement("""
                SELECT count(*),COALESCE(sum(size_bytes),0) FROM staging_import
                WHERE status <> 'PROMOTED' AND artifact_deleted_at IS NULL
                """)){
            try(ResultSet r=s.executeQuery()){r.next();return new LiveUsage(r.getLong(1),r.getLong(2));}
        }catch(SQLException e){throw new IllegalStateException("Could not read staging capacity.",e);}
    }

    private StagingImport map(ResultSet r) throws SQLException {
        Map<String,GitFileMode> modes; try{ modes=objectMapper.readValue(r.getString("file_modes_json"),new TypeReference<>(){}); }catch(Exception e){throw new IllegalStateException("Could not deserialize staging file modes.",e);}
        StoredUploadArtifact a=new StoredUploadArtifact(r.getObject("artifact_id",UUID.class),r.getString("original_filename"),r.getLong("size_bytes"),r.getString("sha256"),Path.of(r.getString("storage_path")),r.getTimestamp("created_at").toInstant(),r.getTimestamp("artifact_retention_deadline").toInstant(),modes);
        return StagingImport.rehydrate(r.getObject("id",UUID.class),a,r.getString("claim_token_sha256"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("expires_at").toInstant(),StagingImportStatus.valueOf(r.getString("status")),r.getObject("owner_user_id",UUID.class),r.getObject("promoted_import_id",UUID.class),instant(r,"claimed_at"),instant(r,"promoted_at"),r.getTimestamp("updated_at").toInstant());
    }
    private Instant instant(ResultSet r,String name)throws SQLException{Timestamp t=r.getTimestamp(name);return t==null?null:t.toInstant();}
    private String json(Object v){try{return objectMapper.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException("Could not serialize staging file modes.",e);}}

    public record ClaimOutcome(ClaimResult result, StagingImport stagingImport) { }
    public enum ClaimResult { CLAIMED, ALREADY_CLAIMED_BY_OWNER, NOT_AVAILABLE }
    public record LiveUsage(long objects,long bytes) { }
    public record CleanupCandidate(UUID stagingId, StoredUploadArtifact artifact) { }
    public record PromotionOutcome(PromotionResult result, UUID importId) { }
    public enum PromotionResult { PROMOTED, ALREADY_PROMOTED, NOT_CLAIMED, EXPIRED, NOT_AVAILABLE }
    @FunctionalInterface public interface PromotionAction { UUID createOrRecover(StagingImport stagingImport); }
}
