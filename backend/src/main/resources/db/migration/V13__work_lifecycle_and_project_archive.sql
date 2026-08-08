ALTER TABLE project ADD COLUMN archived_at TIMESTAMPTZ;
ALTER TABLE work_session DROP CONSTRAINT IF EXISTS uq_work_branch;

DROP INDEX IF EXISTS uq_work_active_project;
CREATE UNIQUE INDEX uq_work_open_project ON work_session(project_id)
    WHERE status IN ('PROVISIONING', 'ACTIVE');
CREATE INDEX idx_project_owner_unarchived ON project(owner_user_id, created_at)
    WHERE archived_at IS NULL;
