# Controlled workflow dispatch and rerun

Step 8.3 adds an intentionally narrow write surface for GitHub Actions. GitHub remains the authoritative execution UI and arbitrary workflow execution is not exposed.

## Default-deny allowlists

Two independent server settings control write operations:

- `ZIP_GITHUB_ACTIONS_ALLOWED_DISPATCH_WORKFLOWS`
- `ZIP_GITHUB_ACTIONS_ALLOWED_RERUN_WORKFLOWS`

Each is a comma-separated list of workflow numeric ids, filenames such as `ci.yml`, or `.github/workflows/...` paths. Empty means deny all. A workflow allowed for dispatch is not automatically allowed for rerun, and vice versa.

The backend resolves configured workflows through the same repository's GitHub App installation token and re-checks the resolved workflow id/path before every write.

## Dispatch

`POST /api/imports/{importId}/actions/dispatch` accepts only a configured workflow and the exact Work ref/commit currently displayed to the user. No arbitrary inputs are exposed in step 8.3. The dispatch uses the active Work branch as `ref`.

Before dispatch the backend verifies:

- authenticated import ownership;
- repository/installation association from the stored delivery sources;
- the import is still the latest committed import of the active Work;
- displayed `expectedRef` and `expectedCommitSha` still equal the immutable delivery result;
- the resolved workflow is in the dispatch allowlist and is active;
- the current GitHub App installation explicitly reports `Actions: write`;
- an explicit confirmation flag and bounded idempotency key are present.

## Rerun failed jobs

`POST /api/imports/{importId}/actions/rerun-failed` reruns only failed jobs for one existing workflow run. Before calling GitHub, the backend fetches the run and verifies its `head_sha`, `head_branch` and workflow id/path against the current Work head and rerun allowlist. Non-failed runs are rejected.

GitHub reruns retain the original run's SHA/ref; zip-github does not alter them.

## Audit and idempotency

Flyway migration `V9__actions_control_audit.sql` persists one owner-bound audit record per control attempt. It stores only non-secret metadata: owner/project/import ids, operation, workflow/run identifiers, Work ref, target commit SHA, idempotency key, status, GitHub URL/error code and timestamps.

The unique key `(owner_user_id, import_id, operation, idempotency_key)` claims the external side effect before GitHub is called. Concurrent duplicate requests therefore cannot both invoke GitHub. A repeated key returns the already recorded operation and a key cannot be rebound to another workflow/run/ref/commit target.

If a GitHub call fails or its outcome is uncertain, the audit is marked failed and the same idempotency key is not used to create another external run. The UI tells the user to refresh before making a new explicit attempt.

## Stale Work behavior

Actions controls are available only while the import result is exactly the active Work head. Once another import is committed or the Work is finalized into a pull request, the older result remains readable but its control surface is disabled. This prevents an old browser tab from dispatching or rerunning work for a stale Work state.

## GitHub App permission

Read-only phase-8 functionality needs Actions read. Step 8.3 write operations additionally require **Actions: Read and write** for the GitHub App installation. Before every write, the backend reads the owner-scoped installation metadata with the App JWT and requires `permissions.actions == write`; GitHub also enforces the permission on dispatch/rerun endpoints. Installations may need owner approval after the App permission is upgraded.
