# Step 8.3 report — controlled workflow dispatch and rerun

Date: 2026-08-07  
Repository revision: r0087  
Application version: 1.0.0-rc.42

## Baseline verification

The supplied r0086 ZIP matched the documented continuation point:

- `docs/implementation-status.md` had exactly `8.3` as `NEXT`;
- steps 8.1 and 8.2 and all earlier prerequisites were `DONE`;
- former 8.4 remained `SKIPPED` in the future AI/integration backlog;
- all phase-9 steps remained pending;
- `VERSION` was `1.0.0-rc.41`;
- owner-scoped Actions status, artifacts and condensed error reads from 8.1/8.2 were present.

No code/status discrepancy requiring correction was found before implementing 8.3.

## Implemented

- Added independent default-deny allowlists for workflow dispatch and rerun.
- Added an explicit server-side GitHub App installation permission check requiring `Actions: write` before every Actions write, plus active-workflow validation for dispatch.
- Added owner-scoped control options for the exact delivered Work ref/commit.
- Added manual `workflow_dispatch` for an explicitly allowed workflow on the active Work branch; arbitrary workflow inputs are intentionally not exposed.
- Added rerun of failed jobs only for an explicitly allowed failed workflow run whose GitHub `head_sha` and `head_branch` exactly match the current Work head.
- Added stale-Work and stale-view guards so old import results cannot control Actions after Work moves on or is finalized.
- Added persistent Actions control audit and database-backed idempotency claims before external side effects.
- Added duplicate-key target binding so an idempotency key cannot be reused for another workflow/run/ref/commit.
- Added mobile-friendly explicit Actions controls showing workflow, branch/ref and commit before the user invokes them.
- Extended workflow status with GitHub workflow id/path and run head identity so the UI can correlate only server-allowed reruns.
- Updated GitHub App configuration guidance from Actions read-only to Actions read/write for step 8.3.

No phase-9 staging/Shortcut behavior or future AI/integration backlog work was implemented.

## Security and consistency invariants

- Every control operation starts from authenticated owner-scoped import/project/delivery state.
- GitHub access continues through server-side App JWT/short-lived installation tokens; no GitHub credential reaches the browser.
- Every write explicitly verifies that the owner-scoped GitHub App installation grants `Actions: write`.
- Dispatch/rerun are deny-all when allowlists are empty.
- Dispatch and rerun permissions are independent.
- Rerun cannot target a run from another SHA, branch or workflow.
- Old browser tabs are rejected by exact `expectedRef` + `expectedCommitSha` checks and current Work-head validation.
- One persisted idempotency claim is created before a GitHub write; concurrent duplicates do not create parallel GitHub writes.
- Audit data contains no installation token, user access token, ZIP bytes, logs or artifacts.
- Immutable plan/selection/approval, exact base SHA, single-active-import Work semantics and non-force delivery are unchanged.

## Verification actually performed

Successful:

- unpacked and inventoried the complete r0086 ZIP and re-read `AGENTS.md`, status, handoff and exact 8.3 scope;
- verified current GitHub REST documentation for workflow dispatch and rerun permission/behavior;
- compiled and ran `ActionsControlRulesSelfTest` directly with installed `javac`/`java`; stale import/head/ref and exact-run guard assertions passed;
- added `ActionsControlAuditStoreTest` covering duplicate retry claim reuse and persisted-result replay semantics, plus a frontend regression proving an ambiguous GitHub dispatch error retains the same idempotency key on retry (full JUnit/Vitest execution remains dependency-environment limited);
- static source inspection of default-deny operation-specific allowlists, owner-scoped service path, persistent audit unique key and GitHub App-only write client;
- repository verification scripts listed below were run after final status/release updates.

Environment-limited:

- `backend/./mvnw test` could not bootstrap Maven because DNS access to `repo.maven.apache.org` is unavailable (`curl: (6) Could not resolve host`);
- `frontend/npm ci` failed because the sandbox npm proxy returned `404 Not Found` for `yallist-3.1.1.tgz`; therefore Vitest and the production frontend build could not be run from a clean dependency installation here;
- no live GitHub App installation with Actions write permission was available, so dispatch/rerun were not exercised against a real repository in this packaging environment.

Full Maven tests, `npm ci && npm test && npm run build`, database migration execution against PostgreSQL, and one live allowlisted dispatch/rerun acceptance run remain required in normal local/CI acceptance.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ActionsControlAudit.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ActionsControlAuditStore.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ActionsControlPolicy.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ActionsControlRules.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ActionsControlOperationResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/DispatchWorkflowRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportActionsControlOptionsResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/RerunWorkflowRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsControlClient.java`
- `backend/src/main/resources/db/migration/V9__actions_control_audit.sql`
- `backend/src/test/java/info/isaksson/erland/zipgithub/actions/ActionsControlPolicyTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/actions/ActionsControlRulesSelfTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/actions/ActionsControlAuditStoreTest.java`
- `docs/controlled-workflow-actions.md`
- `docs/step-8.3-report.md`

## Files modified

- `.env.example`
- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportActionsStatusResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsClient.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `backend/src/main/resources/application.properties`
- `docs/api-contract.md`
- `docs/github-app-setup.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/threat-model.md`
- `frontend/src/api/imports.ts`
- `frontend/src/pages/ImportResultPage.test.tsx`
- `frontend/src/pages/ImportResultPage.tsx`
- `frontend/src/styles/global.css`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`9.1 — Define and persist the StagingImport lifecycle` is `NEXT`. Phase 9 has not been implemented in this revision.
