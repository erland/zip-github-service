CREATE TABLE import_resume_payload (
    import_session_id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    upload_json TEXT,
    snapshot_json TEXT,
    plan_json TEXT,
    selection_json TEXT,
    approval_json TEXT,
    git_identity_json TEXT,
    delivery_json TEXT,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_import_resume_payload_session_owner FOREIGN KEY (import_session_id, owner_user_id)
        REFERENCES import_session(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT uq_import_resume_payload_owner UNIQUE (import_session_id, owner_user_id)
);

CREATE INDEX idx_import_resume_payload_owner ON import_resume_payload(owner_user_id);
