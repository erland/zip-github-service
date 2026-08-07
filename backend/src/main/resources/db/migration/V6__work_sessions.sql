CREATE TABLE work_session (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    base_branch VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    head_commit_sha CHAR(40),
    base_commit_sha CHAR(40),
    last_import_id UUID,
    last_plan_digest_sha256 CHAR(64),
    pull_request_number BIGINT,
    pull_request_url TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_work_project_owner FOREIGN KEY (project_id, owner_user_id)
        REFERENCES project(id, owner_user_id) ON DELETE CASCADE,
    CONSTRAINT ck_work_branch_not_blank CHECK (btrim(base_branch) <> '' AND btrim(branch_name) <> ''),
    CONSTRAINT ck_work_head_sha CHECK (head_commit_sha IS NULL OR head_commit_sha ~ '^[0-9a-f]{40}$'),
    CONSTRAINT ck_work_base_sha CHECK (base_commit_sha IS NULL OR base_commit_sha ~ '^[0-9a-f]{40}$'),
    CONSTRAINT ck_work_digest CHECK (last_plan_digest_sha256 IS NULL OR last_plan_digest_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_work_pr CHECK (pull_request_number IS NULL OR pull_request_number > 0),
    CONSTRAINT uq_work_branch UNIQUE (project_id, branch_name)
);

CREATE UNIQUE INDEX uq_work_active_project ON work_session(project_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_work_owner_project ON work_session(owner_user_id, project_id, created_at DESC);
