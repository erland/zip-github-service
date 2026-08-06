CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    github_user_id BIGINT NOT NULL UNIQUE,
    github_login VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE github_installation (
    id BIGINT PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    account_login VARCHAR(255) NOT NULL,
    permissions_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    repository_selection VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_github_installation_owner UNIQUE (id, owner_user_id)
);

CREATE TABLE project (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    github_installation_id BIGINT,
    github_repository_id BIGINT,
    repository_owner VARCHAR(255),
    repository_name VARCHAR(255),
    default_branch VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_project_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_project_default_branch_not_blank CHECK (btrim(default_branch) <> ''),
    CONSTRAINT ck_project_github_binding CHECK (
        (github_installation_id IS NULL AND github_repository_id IS NULL AND repository_owner IS NULL AND repository_name IS NULL)
        OR
        (github_installation_id IS NOT NULL AND github_repository_id IS NOT NULL AND repository_owner IS NOT NULL AND repository_name IS NOT NULL)
    ),
    CONSTRAINT fk_project_installation_owner FOREIGN KEY (github_installation_id, owner_user_id)
        REFERENCES github_installation(id, owner_user_id),
    CONSTRAINT uq_project_owner_name UNIQUE (owner_user_id, name),
    CONSTRAINT uq_project_id_owner UNIQUE (id, owner_user_id)
);

CREATE TABLE import_session (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    base_branch VARCHAR(255) NOT NULL,
    base_commit_sha CHAR(40),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_import_session_project_owner FOREIGN KEY (project_id, owner_user_id)
        REFERENCES project(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_import_session_branch_not_blank CHECK (btrim(base_branch) <> ''),
    CONSTRAINT ck_import_session_sha CHECK (base_commit_sha IS NULL OR base_commit_sha ~ '^[0-9a-f]{40}$'),
    CONSTRAINT uq_import_session_id_owner UNIQUE (id, owner_user_id)
);

CREATE TABLE source_upload (
    id UUID PRIMARY KEY,
    import_session_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    original_filename VARCHAR(1024) NOT NULL,
    storage_key TEXT,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 CHAR(64),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    retention_deadline TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_source_upload_session_owner FOREIGN KEY (import_session_id, owner_user_id)
        REFERENCES import_session(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_source_upload_filename_not_blank CHECK (btrim(original_filename) <> ''),
    CONSTRAINT ck_source_upload_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_source_upload_sha CHECK (sha256 IS NULL OR sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_source_upload_retention CHECK (retention_deadline > created_at),
    CONSTRAINT uq_source_upload_id_owner UNIQUE (id, owner_user_id)
);

CREATE TABLE import_plan (
    id UUID PRIMARY KEY,
    import_session_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    base_commit_sha CHAR(40) NOT NULL,
    policy_version VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    CONSTRAINT fk_import_plan_session_owner FOREIGN KEY (import_session_id, owner_user_id)
        REFERENCES import_session(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_import_plan_sha CHECK (base_commit_sha ~ '^[0-9a-f]{40}$'),
    CONSTRAINT ck_import_plan_policy_not_blank CHECK (btrim(policy_version) <> ''),
    CONSTRAINT uq_import_plan_id_owner UNIQUE (id, owner_user_id),
    CONSTRAINT uq_import_plan_session UNIQUE (import_session_id)
);

CREATE TABLE import_plan_entry (
    id UUID PRIMARY KEY,
    import_plan_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    path TEXT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    source_sha256 CHAR(64),
    target_sha256 CHAR(64),
    size_bytes BIGINT NOT NULL,
    is_text BOOLEAN NOT NULL,
    policy_result VARCHAR(32) NOT NULL,
    policy_message TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_plan_entry_plan_owner FOREIGN KEY (import_plan_id, owner_user_id)
        REFERENCES import_plan(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_plan_entry_path CHECK (btrim(path) <> '' AND path !~ '^/' AND position(E'\\' in path) = 0),
    CONSTRAINT ck_plan_entry_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_plan_entry_source_sha CHECK (source_sha256 IS NULL OR source_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_plan_entry_target_sha CHECK (target_sha256 IS NULL OR target_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT uq_plan_entry_path UNIQUE (import_plan_id, path)
);

CREATE TABLE github_delivery (
    id UUID PRIMARY KEY,
    import_session_id UUID NOT NULL,
    import_plan_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL,
    branch_name VARCHAR(255),
    commit_sha CHAR(40),
    pull_request_number BIGINT,
    pull_request_url TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_delivery_session_owner FOREIGN KEY (import_session_id, owner_user_id)
        REFERENCES import_session(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_plan_owner FOREIGN KEY (import_plan_id, owner_user_id)
        REFERENCES import_plan(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_delivery_idempotency_not_blank CHECK (btrim(idempotency_key) <> ''),
    CONSTRAINT ck_delivery_commit_sha CHECK (commit_sha IS NULL OR commit_sha ~ '^[0-9a-f]{40}$'),
    CONSTRAINT ck_delivery_pr_number CHECK (pull_request_number IS NULL OR pull_request_number > 0),
    CONSTRAINT uq_delivery_owner_idempotency UNIQUE (owner_user_id, idempotency_key),
    CONSTRAINT uq_delivery_plan UNIQUE (import_plan_id)
);

CREATE INDEX idx_project_owner_active ON project(owner_user_id, active);
CREATE INDEX idx_project_repository ON project(github_repository_id) WHERE github_repository_id IS NOT NULL;
CREATE INDEX idx_import_session_owner_created ON import_session(owner_user_id, created_at DESC);
CREATE INDEX idx_import_session_project_created ON import_session(project_id, created_at DESC);
CREATE INDEX idx_import_session_status ON import_session(status);
CREATE INDEX idx_source_upload_retention ON source_upload(retention_deadline) WHERE status <> 'DELETED';
CREATE INDEX idx_import_plan_owner_created ON import_plan(owner_user_id, created_at DESC);
CREATE INDEX idx_plan_entry_plan_change ON import_plan_entry(import_plan_id, change_type);
CREATE INDEX idx_plan_entry_plan_policy ON import_plan_entry(import_plan_id, policy_result);
CREATE INDEX idx_delivery_owner_created ON github_delivery(owner_user_id, created_at DESC);
CREATE INDEX idx_delivery_status ON github_delivery(status);
