-- Phase 9.6: retain the storage artifact's ordinary retention deadline separately from the
-- staging lifecycle deadline, and make physical cleanup restart-safe.
ALTER TABLE staging_import
    ADD COLUMN artifact_retention_deadline TIMESTAMPTZ,
    ADD COLUMN artifact_deleted_at TIMESTAMPTZ;

-- r0093-r0097 staging rows did not persist the ingestion artifact deadline separately.
-- The ordinary upload default has been 24h; never shorten an already-longer staging deadline.
UPDATE staging_import
SET artifact_retention_deadline = GREATEST(expires_at, created_at + INTERVAL '24 hours')
WHERE artifact_retention_deadline IS NULL;

ALTER TABLE staging_import
    ALTER COLUMN artifact_retention_deadline SET NOT NULL;

ALTER TABLE staging_import
    ADD CONSTRAINT ck_staging_artifact_retention
        CHECK (artifact_retention_deadline > created_at);

CREATE INDEX idx_staging_cleanup_candidates
    ON staging_import (expires_at, status)
    WHERE artifact_deleted_at IS NULL AND status IN ('AVAILABLE','CLAIMED','EXPIRED','CANCELLED');

COMMENT ON COLUMN staging_import.artifact_retention_deadline IS
    'Original StoredUploadArtifact retention deadline; transferred unchanged to the ordinary Import on promotion.';
COMMENT ON COLUMN staging_import.artifact_deleted_at IS
    'Set only after the staging ZIP has been physically removed. PROMOTED artifacts are owned by ordinary Import retention and are never staging-cleaned.';
