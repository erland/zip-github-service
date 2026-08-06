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
