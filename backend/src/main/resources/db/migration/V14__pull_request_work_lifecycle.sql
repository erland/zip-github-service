-- Step 9.16: an open/closed pull request remains the same logical Work until merged/abandoned.
-- Older installations may have several terminal PULL_REQUEST_CREATED rows per project. Only the latest
-- candidate can become the currently open logical Work; older historical rows stay terminal.
WITH latest_pr_work AS (
    SELECT DISTINCT ON (project_id) id
    FROM work_session
    WHERE status = 'PULL_REQUEST_CREATED'
    ORDER BY project_id, created_at DESC
)
UPDATE work_session
SET status = 'PR_OPEN'
WHERE id IN (SELECT id FROM latest_pr_work);

DROP INDEX IF EXISTS uq_work_open_project;
CREATE UNIQUE INDEX uq_work_open_project ON work_session(project_id)
    WHERE status IN ('PROVISIONING', 'ACTIVE', 'PR_OPEN', 'PR_CLOSED');
