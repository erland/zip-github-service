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

## Repository snapshot

`POST /api/imports/{importId}/repository-snapshot` resolves the import branch to an exact commit SHA, fetches that exact commit using a shallow temporary Git repository and returns a deterministic Git-tree inventory. The import must belong to the current session user. A GitHub or Git failure returns `502 REPOSITORY_SNAPSHOT_FAILED`; no installation token or workspace path is returned.

### Compare uploaded ZIP with frozen repository snapshot

`POST /api/imports/{importId}/comparison`

Requires a stored upload and repository snapshot. Returns stable SHA-256 based classifications: `ADDED`, `MODIFIED`, `UNCHANGED`, and `WOULD_DELETE`.

## Evaluate import policy

`POST /api/imports/{importId}/policy`

Recreates the normalized archive inventory and hash comparison against the import's locked repository snapshot, then applies policy version `mvp-1`.

The response contains deterministic path-sorted entries and an `approvable` flag. MVP blockers include `.git/**`, `.github/**`, repository deletions, files over the configured single-file limit, and high-risk private-key/credential filenames. Transport noise is returned as `IGNORED`; `.env` and environment-specific `.env.*` files are warnings, while `.env.example` remains allowed.

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

### Approve exact immutable plan

`POST /api/imports/{importId}/plan/approval`

Request:

```json
{
  "planDigestSha256": "<64 lower-case hex characters>"
}
```

The server requires ownership, an approvable immutable plan and exact equality with the stored plan digest. Repeating the same approval is idempotent. Digest mismatch and blocked plans return conflict problem responses.

## Prepare approved Git workspace

`POST /api/imports/{importId}/workspace`

Requires an exact approved immutable plan. The server fetches the locked base commit, applies only approved `ADDED` and `MODIFIED` files, verifies hashes and the complete local Git diff, and returns metadata without exposing the server-side workspace path or credentials.

Response fields include `importId`, `repositoryFullName`, `baseCommitSha`, `planDigestSha256`, `appliedFileCount`, sorted `appliedPaths`, `status=FILES_APPLIED`, and `preparedAt`.

### Deliver approved import

`POST /api/imports/{importId}/delivery`

Requires an exact approved plan and a verified applied workspace. Revalidates that the remote base branch still points at the approved commit, creates deterministic branch `zip-github/import-<importId>`, creates one commit and pushes without force. Returns branch and commit metadata with status `PUSHED`.

### Create draft pull request

`POST /api/imports/{importId}/pull-request` creates a draft pull request from the already pushed import branch to the frozen base branch. The response contains stable repository, branch, commit, plan digest, pull request number and URL metadata.

`GET /api/imports/{importId}/pull-request` returns the recorded metadata without contacting GitHub again.


## Idempotent delivery retry semantics

`POST /api/imports/{importId}/delivery` and `POST /api/imports/{importId}/pull-request` are idempotent after a result has been recorded. Replays return the stored result. Pull request creation also reconciles an existing open PR for the exact import head/base pair. Retryable Git transport failures use `502 GIT_DELIVERY_RETRYABLE`; permanent identity or branch conflicts use `409 GIT_DELIVERY_FAILED`.

## Basic check status

`GET /api/imports/{importId}/checks` returns the normalized delivered-commit state (`pending`, `success`, `failure`, `cancelled` or `unavailable`), terminal flag, counters, checked timestamp and permanent GitHub checks URL.

## Project import history

`GET /api/projects/{projectId}/imports` returns the authenticated owner's imports for the project, newest first. Each item contains current import status, available source/plan/PR metadata, and `resumeStage` (`UPLOAD`, `REVIEW`, or `RESULT`) for reopening the correct UI stage.
