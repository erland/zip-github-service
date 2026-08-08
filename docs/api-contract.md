# API contract baseline

Version 0.1 — step 1.3

## Scope

This is the first executable API skeleton for projects and imports. It intentionally uses an in-memory application service until repository adapters are introduced. The database schema from step 1.2 remains the target persistence model.

## Temporary user context

Until GitHub-backed web sessions are implemented in step 2.2, authenticated test/development requests provide a UUID in:

```http
X-Zip-Github-User: <uuid>
```

This is not a production authentication mechanism. Missing or malformed context returns `401` using the problem contract. Every project/import lookup is owner-scoped; a resource owned by another user is returned as `404`.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/projects` | List projects owned by the current user |
| `POST` | `/api/projects` | Create a local project skeleton |
| `GET` | `/api/projects/{projectId}` | Read an owned project |
| `POST` | `/api/projects/{projectId}/imports` | Create an empty import session for an owned project |
| `GET` | `/api/imports/{importId}` | Read an owned import session |
| `POST` | `/api/imports/{importId}/cancel` | Cancel an owned import before Git delivery |

The existing `GET /api/health` endpoint remains available.

## Error contract

Errors use `Content-Type: application/problem+json` and contain:

- `type`
- `title`
- `status`
- `detail`
- `code`
- `correlationId`
- `timestamp`

The same correlation ID is returned in the `X-Correlation-ID` response header.

Initial machine-readable codes:

- `AUTH_REQUIRED`
- `INVALID_USER_CONTEXT`
- `VALIDATION_ERROR`
- `INVALID_BRANCH`
- `PROJECT_NAME_EXISTS`
- `PROJECT_NOT_FOUND`
- `IMPORT_NOT_FOUND`
- `INTERNAL_ERROR`

## OpenAPI

Quarkus SmallRye OpenAPI discovers the JAX-RS resources and exposes the generated contract at `/q/openapi`. The DTO records define the initial request and response schemas. Authentication will be represented as a session security scheme when step 2.2 replaces the temporary header adapter.

## Step 2.4 GitHub-backed project contract

Project creation now requires `githubInstallationId` and `githubRepositoryId`. The backend verifies both against the authenticated user's GitHub App view and verifies the selected branch before storing the project. `PATCH /api/projects/{projectId}` updates project name, GitHub binding, default branch or active state and repeats the same verification. Safe project responses include repository full name and privacy flag but no GitHub credentials.

## Source package upload

### `PUT /api/imports/{importId}/upload`

Streams one ZIP source package for an import owned by the current browser session.

Headers:

- `Content-Type: application/zip` or `application/octet-stream`
- `X-Filename: project.zip`
- `Content-Length` when known

Response: `201 Created` with `SourceUploadResponse` containing ID, filename, actual size, SHA-256, status and retention deadline. The storage path and owner ID are not exposed.

Errors include:

- `401 AUTH_REQUIRED`
- `404 IMPORT_NOT_FOUND`
- `400 INVALID_UPLOAD`
- `409 UPLOAD_ALREADY_EXISTS`
- `413 UPLOAD_TOO_LARGE`


## Import cancellation (step 7.22)

`POST /api/imports/{importId}/cancel` closes an owned import that the user no longer wants to commit. The operation is owner-scoped and idempotent. It is allowed before Git delivery, including after immutable selection/approval or temporary workspace preparation. The backend marks the import `CANCELLED`, removes any temporary Git workspace for that import and keeps immutable audit/review records for troubleshooting.

A cancelled upload becomes eligible for the ordinary retention cleanup once its retention deadline has passed. Cancellation never rewrites Git and never removes an already delivered commit. If delivery has already been recorded, the endpoint returns `409 IMPORT_ALREADY_DELIVERED`. Unknown or cross-user imports remain `404`.

## Repository snapshot

`POST /api/imports/{importId}/repository-snapshot` resolves the import branch to an exact commit SHA, fetches that exact commit using a shallow temporary Git repository and returns a deterministic Git-tree inventory. The import must belong to the current session user. A GitHub or Git failure returns `502 REPOSITORY_SNAPSHOT_FAILED`; no installation token or workspace path is returned.

### Compare uploaded ZIP with frozen repository snapshot

`POST /api/imports/{importId}/comparison`

Requires a stored upload and repository snapshot. Returns stable SHA-256 based classifications: `ADDED`, `MODIFIED`, `UNCHANGED`, and `WOULD_DELETE`.

## Evaluate import policy

`POST /api/imports/{importId}/policy`

Recreates the normalized archive inventory and hash comparison against the import's locked repository snapshot, then applies policy version `mvp-4`.

The response contains deterministic path-sorted entries and an `approvable` flag. Policy blockers are typed. `.git/**`, oversized files and high-risk private-key/credential filenames are `HARD_BLOCKED`; `.github/**` and repository deletions are `OVERRIDABLE_BLOCKED` and require explicit per-path override before selection. Transport noise is returned as `IGNORED`; `.env` and environment-specific `.env.*` files are warnings, while `.env.example` remains allowed.

This endpoint evaluates policy only. Step 4.4 persists the immutable import plan.

## Create immutable import plan

`POST /api/imports/{importId}/plan`

Recreates the normalized archive inventory, hash comparison and policy result from the import's stored ZIP and locked repository snapshot, then stores the exact result as an immutable import plan.

The plan identity binds together:

- source upload SHA-256,
- locked base commit SHA,
- policy version,
- canonical sorted plan entries,
- a deterministic `planDigestSha256`.

Repeated calls are idempotent when the canonical plan digest is unchanged. If a different plan already exists for the import, the endpoint returns `409 IMPORT_PLAN_IMMUTABLE`.

`GET /api/imports/{importId}/plan` returns the stored plan without recomputing it.

## Automatic review preparation

`POST /api/imports/{importId}/prepare-review` is the normal post-upload orchestration endpoint. It requires the owned source upload and then:

1. returns the already stored immutable plan immediately when one exists;
2. otherwise reuses an already locked repository snapshot when preparation previously progressed that far;
3. otherwise resolves and records one exact repository snapshot;
4. runs the existing archive inventory, comparison, `mvp-4` policy and immutable plan creation;
5. returns the resulting `ImportPlanResponse`.

The endpoint does not introduce a second plan implementation: it delegates to the existing snapshot and plan services. Retry/refresh therefore preserves the first locked base SHA and immutable plan identity rather than resolving a new moving branch after partial success. The older granular snapshot/plan endpoints remain available for diagnostics and compatibility, but the normal UI no longer requires separate user actions for them.

### Approve exact immutable plan

`POST /api/imports/{importId}/plan/approval`

Request body includes `planDigestSha256`, `selectionDigestSha256` and the user-selected `commitMessage`. The selection must already exist, belong to the current user/import and match the immutable plan/base identity. Approval locks both digests **and the normalized commit message**; a different selection or commit message cannot be substituted after approval.

Request:

```json
{
  "planDigestSha256": "<64 lower-case hex characters>",
  "selectionDigestSha256": "<64 lower-case hex characters>",
  "commitMessage": "Describe the approved change"
}
```

The server requires ownership and exact equality with both the stored plan digest and immutable selection digest. Interactive commit messages normalize CRLF/CR to LF, trim surrounding whitespace, reject empty/whitespace-only values, reject control characters other than LF, and are capped at 500 characters. A plan containing blockers may still be approved when the immutable selection contains at least one valid selected change and every selected overridable blocker has its explicit audit record. Repeating the same approval including the same normalized commit message is idempotent; a different message conflicts with the already recorded approval.

`GET /api/imports/{importId}/plan/approval` returns the owner-scoped recorded approval including `commitMessage`, or `404` if no approval exists. This is a recovery/readback endpoint: it never creates or changes an approval. Legacy persisted approvals that predate step 9.5 receive the deterministic previous message `Apply approved ZIP import <importId>` during hydration; new interactive approvals never rely on that fallback.

### Normal review-to-commit orchestration (step 7.17)

The normal UI exposes an editable commit-message field followed by one action, **Godkänn valda förändringar**. The final confirmation shows the chosen message, locked base ref and selected-file count. That one click still preserves the internal security boundaries in this order: create/reuse immutable selection, record/reuse approval including commit message, prepare and exactly verify the workspace, then commit and push. The browser calls the existing endpoints in that order; no GitHub write occurs before approval has been recorded.

If approval exists but delivery did not complete, refresh restores the immutable selection and approval and exposes only the recovery action **Försök skapa commit igen**. Delivery/workspace retries reuse existing identities and are idempotent. If delivery is already recorded, reopening review proceeds to the result page.

## Prepare approved Git workspace

`POST /api/imports/{importId}/workspace`

Requires an exact approved immutable plan and immutable selection. The server fetches the locked base commit, applies only `selectedPaths`, performs explicitly approved `WOULD_DELETE` operations, verifies archive hashes for selected files and verifies that the complete local Git diff exactly equals the immutable selection. The response includes the selection digest but never exposes the server-side workspace path or credentials.

Response fields include `importId`, `repositoryFullName`, `baseCommitSha`, `planDigestSha256`, `appliedFileCount`, sorted `appliedPaths`, `status=FILES_APPLIED`, and `preparedAt`.

### Deliver approved import

`POST /api/imports/{importId}/delivery`

Requires an exact approved plan and a verified applied workspace. Revalidates that the remote base branch still points at the approved commit, uses the import's active `zip-github/work-<workId>` branch, creates one commit using the **approval-bound commit message** and pushes without force. Returns branch and commit metadata with status `PUSHED`.

### Create draft pull request

`POST /api/imports/{importId}/pull-request` creates a draft pull request from the already pushed import branch to the frozen base branch. The response contains stable repository, branch, commit, plan digest, pull request number and URL metadata.

`GET /api/imports/{importId}/pull-request` returns the recorded metadata without contacting GitHub again.


## Idempotent delivery retry semantics

`POST /api/imports/{importId}/delivery` and `POST /api/imports/{importId}/pull-request` are idempotent after a result has been recorded. Replays return the stored result. Pull request creation also reconciles an existing open PR for the exact import head/base pair. Retryable Git transport failures use `502 GIT_DELIVERY_RETRYABLE`; permanent identity or branch conflicts use `409 GIT_DELIVERY_FAILED`.

## Basic check status

`GET /api/imports/{importId}/checks` returns the normalized delivered-commit state (`pending`, `success`, `failure`, `cancelled` or `unavailable`), terminal flag, counters, checked timestamp and permanent GitHub checks URL.

## Workflow runs, jobs and checks (step 8.1)

`GET /api/imports/{importId}/actions` is an owner-scoped, read-only status view for the exact delivered commit SHA. It returns an aggregate state (`not_started`, `pending`, `success`, `failure`, `cancelled` or `unavailable`), a terminal flag, observation timestamp, permanent GitHub checks URL, bounded workflow runs with bounded jobs, and bounded check runs. Each returned run/job/check includes a stable normalized state and its GitHub URL when available. The endpoint never returns GitHub credentials and does not expose workflow dispatch/rerun operations.

## Actions artifacts and condensed errors (step 8.2)

`GET /api/imports/{importId}/actions/details` returns bounded read-only artifact metadata plus sanitized failed-job diagnostics for the exact delivered commit: condensed lines, contextual lines around the failure, bounded job-log lines and a truncation flag. The endpoint is owner-scoped through the existing import/delivery lookup and uses a short-lived GitHub App installation token server-side.

The response is deliberately limited: at most 20 artifact metadata entries across at most 10 matching workflow runs and at most three failed-job summaries. Artifact bytes and authenticated archive URLs are never returned or persisted; each artifact instead links to its owning GitHub workflow run. Each candidate failed-job log read is capped at 24 KiB before parsing. Summaries recognize Maven/Gradle, npm/Vite, Pandoc and xcodebuild only when robust patterns match, sanitize terminal/control sequences and common credential patterns, and identify workflow, job, failed step, detected tool and the GitHub job URL. Unknown log formats are not guessed.

A detail-read failure does not invalidate the ordinary import result or the step-8.1 Actions status. GitHub remains the source for complete logs and artifact downloads.

## Project import history

`GET /api/projects/{projectId}/imports` returns the authenticated owner's imports for the project, newest first. Each item contains current import status, available source/plan/PR metadata, and `resumeStage` (`UPLOAD`, `REVIEW`, or `RESULT`) for reopening the correct UI stage.

## Immutable import selection (step 7.7)

`ImportPlan` remains the immutable description of the complete ZIP-versus-base comparison. A separate immutable selection records exactly which plan paths the user chose for a later commit.

### `POST /api/imports/{importId}/selection`

Request:

```json
{
  "planDigestSha256": "<current immutable plan digest>",
  "baseCommitSha": "<current locked 40-character Git SHA>",
  "selectedPaths": ["src/App.java"],
  "overrides": [
    {
      "path": ".github/workflows/ci.yml",
      "acknowledgement": "I understand that this changes repository automation."
    }
  ]
}
```

The backend validates all selection identity and policy rules server-side:

- the submitted plan digest and base SHA must exactly match the current immutable plan,
- at least one changed path must be selected,
- every path must occur in the plan and may appear only once,
- `HARD_BLOCKED` paths can never be selected,
- ordinary selectable paths must be `ADDED` or `MODIFIED`,
- selecting an `OVERRIDABLE_BLOCKED` path requires an explicit per-path acknowledgement,
- overrides for excluded, unknown or non-overridable paths are rejected.

The server computes `excludedPaths` from the complete plan and creates a deterministic `selectionDigestSha256` using selection version `selection-1`. The digest binds the owner, import, plan identity, base SHA, selected paths, excluded paths and override audit entries. Repeating the identical selection is idempotent; attempting to replace it with a different selection returns `409 IMPORT_SELECTION_IMMUTABLE`.

Response: `201 Created` for the first immutable selection and `200 OK` for an identical replay.

### `GET /api/imports/{importId}/selection`

Returns the stored owner-scoped immutable selection. Cross-user access is intentionally indistinguishable from a missing import (`404`).

As of step 7.9 the immutable selection is the exact delivery contract. Plan approval must include both `planDigestSha256` and `selectionDigestSha256`; workspace preparation and commit verification are bound to the same selection identity.


### Selection delivery invariants

- `.git/**` can appear in a plan for transparency but is never valid in `selectedPaths`.
- `.github/**` and `WOULD_DELETE` require explicit per-path acknowledgement and the acknowledgement is part of the immutable selection digest.
- Empty selections are rejected.
- Workspace preparation applies only `selectedPaths`; selected deletions are removed explicitly.
- The complete Git diff must equal the selected path set before delivery.
- Delivery rejects a stale reviewed base/work-branch SHA.


## Import history source metadata

Project import-history entries expose `sourceType` and nullable `sourceReference` for troubleshooting/audit. These fields are informational only and never authorize access or alter policy/delivery behavior. Current source types are `WEB_UPLOAD`, `STORED_UPLOAD`, and reserved `STAGING_IMPORT`.


## Work commit history

`GET /api/projects/{projectId}/work/commits` requires the normal authenticated owner session. It returns `{ commits, githubAvailable }`; each commit contains `sha`, `message`, `authorName`, `authorEmail`, `authoredAt`, `htmlUrl` and `fallback`. GitHub is queried with a short-lived installation token. On temporary GitHub failure the response remains successful with `githubAvailable=false` and, when known, one fallback entry for the persisted Work head.

## Step 7.23 Work-action lifecycle

`POST /api/projects/{projectId}/imports` now enforces one active import per Work/project. If a non-terminal import already exists for the authenticated owner, the request returns `409 ACTIVE_IMPORT_EXISTS`. Terminal states (`PUSHED`, `PULL_REQUEST_CREATED`, `CANCELLED`) do not block the next import. This invariant is server-side and cannot be bypassed by calling the create-import API directly.

The existing owner-scoped `POST /api/projects/{projectId}/work/pull-request` remains the single explicit Work-completion operation and may be invoked directly from the post-commit result view. Its existing idempotency and Work-head validation continue to apply.

## Controlled Actions writes (step 8.3)

`GET /api/imports/{importId}/actions/control` returns the authenticated owner's current control context for the exact delivered Work commit. It includes `branchRef`, `commitSha`, whether this import is still the active Work head, an optional disabled reason, and only server-configured workflow options resolved through the same GitHub App installation.

`POST /api/imports/{importId}/actions/dispatch` requires JSON fields `workflowIdentifier`, `expectedRef`, `expectedCommitSha`, `idempotencyKey` and `confirmed=true`. The backend rejects stale Work/view state, non-allowlisted workflows and reused idempotency keys bound to a different target. No arbitrary `workflow_dispatch` inputs are accepted in step 8.3.

`POST /api/imports/{importId}/actions/rerun-failed` requires `workflowRunId`, `expectedRef`, `expectedCommitSha`, `idempotencyKey` and `confirmed=true`. The backend fetches the GitHub run and requires an exact current Work SHA/ref match, an explicitly rerun-allowlisted workflow and `conclusion=failure` before calling GitHub's failed-jobs rerun endpoint.

Successful/replayed control responses contain only non-secret audit/result metadata (`operationId`, operation/status, replay flag, workflow/run ids, branch/ref, target commit SHA, GitHub URL and timestamps). Installation/user tokens are never returned.


## Phase 9 staging transport — step 9.2

### `POST /api/staging-imports`

Unauthenticated web-session-wise, but protected by the deployment-scoped `X-ZipGitHub-Upload-Credential`. Accepts `application/zip` or `application/octet-stream` plus `X-Filename`. This capability grants only creation of one transport staging object and is not user authentication.

Success `201` body:

```json
{
  "stagingId": "uuid",
  "originalFilename": "project.zip",
  "sizeBytes": 12345,
  "sha256": "...",
  "expiresAt": "2026-08-08T07:00:00Z",
  "claimUrl": "https://example/staging/claim#token=<one-time-token>"
}
```

The raw claim token is returned only here and only its SHA-256 is persisted. There is no anonymous GET/list/download endpoint. Missing/invalid capability returns generic `STAGING_UPLOAD_UNAUTHORIZED`; oversize input returns `UPLOAD_TOO_LARGE`; invalid upload metadata returns generic `INVALID_STAGING_UPLOAD`. Claim semantics are step 9.3.


### Authenticated staging claim (step 9.3)

`POST /api/staging-imports/claim` requires the normal web session and the same-origin CSRF marker. Request body:

```json
{"token":"<raw one-time claim token>"}
```

The browser obtains the token from `/staging/claim#token=...`, moves it to same-tab `sessionStorage`, clears the fragment and never places it in OAuth `state`, `returnTo` or a query parameter. Success returns owner-safe staging metadata (`stagingId`, original filename, size, SHA-256, expiry and claim time). Invalid, expired, already-taken, terminal or otherwise unavailable claims all return `410 STAGING_CLAIM_UNAVAILABLE`. A retry by the same authenticated owner is idempotent. Claim does not select a Project or create an ordinary Import.

### Authenticated staging project selection and promotion (step 9.4)

`GET /api/staging-imports/{stagingId}` requires the normal authenticated web session and returns owner-safe metadata only when the staging object is owned by the caller and is `CLAIMED` or `PROMOTED`.

`POST /api/staging-imports/{stagingId}/promote` requires the normal session and CSRF marker. Request body:

```json
{"projectId":"uuid"}
```

The backend verifies staging ownership and Project ownership, reuses the ordinary single-active-import/Work invariants, and promotes the already stored ZIP without copy/re-stream through the existing stored-upload import path. The resulting ordinary Import is classified as `STAGING_IMPORT` with non-secret source reference `staging-import:<stagingId>`.

Success body:

```json
{
  "stagingId":"uuid",
  "projectId":"uuid",
  "importId":"uuid",
  "status":"PROMOTED",
  "alreadyPromoted":false
}
```

Retry is restart-safe: the persisted source reference is searched before creating an Import, and an already promoted staging object returns the same Import for the same Project. Selecting a Project with an active non-terminal import continues to return the ordinary `ACTIVE_IMPORT_EXISTS` conflict.

The common comparison/plan response now also carries `archiveMode`, `repositoryMode`, `effectiveMode` and `modeChanged` per path. Only `100644`/`100755` are accepted as ordinary-file Git modes. Mode changes are part of the immutable plan identity and therefore the normal selection/approval contract.

## Phase 9 staging retention/capacity errors (step 9.6)

`POST /api/staging-imports` may return `429 STAGING_CAPACITY_EXCEEDED` when the configured live staging object/byte quota is full. Existing archive-size rejection remains `413 UPLOAD_TOO_LARGE`; request-rate exhaustion remains `429 RATE_LIMIT_EXCEEDED`; a revoked/old deployment credential remains the neutral `401 STAGING_UPLOAD_UNAUTHORIZED`. Credential rotation does not invalidate an already-created staging claim token.

The `expiresAt` returned after upload is the AVAILABLE deadline. After successful claim it is the claimed grace deadline. Same-owner claim retry does not extend that deadline.

## Signed Shortcut distribution (phase 9.7)

Authenticated browser session only:

```text
GET /api/shortcut-release
GET /api/shortcut-release/download
```

Metadata returns `available`, non-secret `version`/`generation`, filename, byte size, SHA-256 and a download URL only when a readable pre-signed `.shortcut` artifact is configured. The backend never synthesizes an unsigned substitute. Download is `private, no-store` and requires the normal authenticated zip-github owner session.

Capability staging uploads using a missing/old/revoked `X-ZipGitHub-Upload-Credential` now return `403 STAGING_SHORTCUT_OUTDATED` with instructions to sign in and install the current Shortcut release. This error conveys no user, Project or GitHub authorization information.


## Phase 9.8 Work lifecycle API

- `GET /api/projects/{projectId}/work/branches` lists owner-scoped, non-default, non-protected GitHub branches eligible for a new Work.
- `POST /api/projects/{projectId}/work` starts a new verified Work branch or resumes an explicitly selected existing branch. New Work is persisted as `PROVISIONING`, the GitHub ref is created/read back, then state becomes `ACTIVE`.
- `POST /api/projects/{projectId}/work/abandon` ends Work without a PR; `deleteBranch=true` separately removes the remote Work branch.
- `DELETE /api/projects/{projectId}` archives the project from normal lists; active Work/imports must be ended first.


## Work Actions status (step 9.9)

- `GET /api/projects/{projectId}/work/actions` returns the same bounded Actions status contract as the import result path, but is resolved from the authenticated project's active Work and exact `headCommitSha`.
- `GET /api/projects/{projectId}/work/actions/details` returns the same shared sanitized artifacts/failure-detail contract for the exact Work-head commit.
- `GET /api/projects/{projectId}/work` includes the owner-scoped `lastImportId` for an active Work with a delivered head so the frontend can reuse the same import-bound Actions control policy instead of introducing a separate dispatch/rerun authorization path.
- Actions status responses may include `diagnosticCode`/`diagnosticMessage`; `ACTIONS_PERMISSION_REQUIRED` means GitHub returned an authorization failure for the Actions endpoint and must not be interpreted as `not_started`.
- Both return a conflict when the active Work has no delivered commit yet. They never search by branch alone.


## Phase 9 final contract invariants

- Staging capability create never conveys Project, user or GitHub authorization; invalid/rotated capabilities use `STAGING_SHORTCUT_OUTDATED`.
- Claim binds exactly one authenticated owner and promotion uses stable `staging-import:<id>` source identity to recover/reuse exactly one ordinary Import.
- Work creation may remain `PROVISIONING` until GitHub confirms the branch at the expected SHA; `ACTIVE` is not returned merely from a locally generated branch name.
- Existing Work branches may be selected only when they are neither the default branch nor protected/in use by another active Work.
- Work Actions endpoints are bound to both the active Work and its exact head commit.
- Signed Shortcut download returns the configured signed bytes with `Content-Disposition` filename `Skicka till zip-github.shortcut`; the technical server filename is not part of the user-facing contract.

## Repository ignore semantics

Import plan creation evaluates tracked `.gitignore` files from the exact locked repository snapshot. A ZIP path that is not already tracked and matches those rules is returned as `IGNORED` with policy code `GITIGNORE_IGNORED`, severity `WARNING`, blocker type `NONE`, and is not selectable for delivery. Existing tracked files remain comparable even when an ignore pattern matches them. `.git/**` is always hard-blocked independently of ignore rules.
