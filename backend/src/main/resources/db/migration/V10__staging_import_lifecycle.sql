CREATE TABLE staging_import (
    id UUID PRIMARY KEY,
    artifact_id UUID NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    storage_path TEXT NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    file_modes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    claim_token_sha256 CHAR(64) NOT NULL,
    owner_user_id UUID REFERENCES user_account(id) ON DELETE CASCADE,
    promoted_import_id UUID REFERENCES import_session(id),
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    claimed_at TIMESTAMPTZ,
    promoted_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_staging_filename CHECK (btrim(original_filename) <> ''),
    CONSTRAINT ck_staging_storage_path CHECK (btrim(storage_path) <> ''),
    CONSTRAINT ck_staging_size CHECK (size_bytes > 0),
    CONSTRAINT ck_staging_sha CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_staging_claim_hash CHECK (claim_token_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_staging_expiry CHECK (expires_at > created_at),
    CONSTRAINT ck_staging_status CHECK (status IN ('AVAILABLE','CLAIMED','PROMOTED','EXPIRED','CANCELLED')),
    CONSTRAINT ck_staging_owner_state CHECK ((status NOT IN ('CLAIMED','PROMOTED')) OR owner_user_id IS NOT NULL),
    CONSTRAINT ck_staging_promotion_state CHECK ((status <> 'PROMOTED') OR promoted_import_id IS NOT NULL),
    CONSTRAINT ck_staging_claimed_at CHECK ((status NOT IN ('CLAIMED','PROMOTED')) OR claimed_at IS NOT NULL),
    CONSTRAINT ck_staging_promoted_at CHECK ((status <> 'PROMOTED') OR promoted_at IS NOT NULL)
);

CREATE INDEX idx_staging_available_expiry ON staging_import(expires_at) WHERE status = 'AVAILABLE';
CREATE INDEX idx_staging_owner_status ON staging_import(owner_user_id, status) WHERE owner_user_id IS NOT NULL;
CREATE UNIQUE INDEX uq_staging_claim_hash ON staging_import(claim_token_sha256);
CREATE UNIQUE INDEX uq_staging_promoted_import ON staging_import(promoted_import_id) WHERE promoted_import_id IS NOT NULL;
