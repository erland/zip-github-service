CREATE TABLE actions_control_audit (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    project_id UUID NOT NULL,
    import_id UUID NOT NULL,
    operation VARCHAR(32) NOT NULL,
    workflow_identifier VARCHAR(200) NOT NULL,
    workflow_id BIGINT,
    workflow_run_id BIGINT,
    branch_ref VARCHAR(255) NOT NULL,
    target_commit_sha CHAR(40) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(24) NOT NULL,
    github_url TEXT,
    error_code VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_actions_control_project_owner FOREIGN KEY (project_id, owner_user_id)
        REFERENCES project(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT fk_actions_control_import_owner FOREIGN KEY (import_id, owner_user_id)
        REFERENCES import_session(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_actions_control_operation CHECK (operation IN ('WORKFLOW_DISPATCH','RERUN_FAILED_JOBS')),
    CONSTRAINT ck_actions_control_status CHECK (status IN ('STARTED','SUCCEEDED','FAILED')),
    CONSTRAINT ck_actions_control_commit CHECK (target_commit_sha ~ '^[0-9a-f]{40}$'),
    CONSTRAINT uq_actions_control_idempotency UNIQUE (owner_user_id, import_id, operation, idempotency_key)
);
CREATE INDEX idx_actions_control_import_created ON actions_control_audit(owner_user_id, import_id, created_at DESC);
