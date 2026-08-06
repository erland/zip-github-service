# Operations

This document describes local operation for `zip-buildserver`.

## Configuration

Copy the environment template:

```bash
cp .env.example .env
```

Set a strong token before exposing the service:

```bash
ZIP_BUILDSERVER_AUTH_ENABLED=true
ZIP_BUILDSERVER_API_TOKEN=replace-with-a-long-random-token
```

## Docker Compose

Start the local stack:

```bash
docker compose up --build
```

Stop and remove local containers and volumes:

```bash
docker compose down -v
```

The Compose stack starts:

- `zip-buildserver-api`
- `zip-buildserver-web`
- `postgres`

## Static API token authentication

Protected API endpoints require:

```text
Authorization: Bearer <ZIP_BUILDSERVER_API_TOKEN>
```

Examples:

```bash
curl -i http://localhost:8080/api/sessions

curl -i \
  -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN" \
  http://localhost:8080/api/sessions
```

Expected behavior:

- Requests without a valid bearer token receive `401 unauthorized`.
- Requests with the configured token are allowed.
- `/api/health` remains public for health checks.
- `/q/openapi` remains public for local OpenAPI inspection.

Disable authentication only for private development:

```bash
ZIP_BUILDSERVER_AUTH_ENABLED=false
```

Do not disable authentication on a network-exposed deployment.

## Storage paths

Default backend storage settings:

```bash
ZIP_BUILDSERVER_DATA_DIR=/data/zip-buildserver
ZIP_BUILDSERVER_PACKAGES_DIR=/data/zip-buildserver/packages
ZIP_BUILDSERVER_WORKSPACES_DIR=/data/zip-buildserver/workspaces
ZIP_BUILDSERVER_ARTIFACTS_DIR=/data/zip-buildserver/artifacts
```

Uploaded packages, extracted workspaces, and verification artifacts should be treated as confidential user content.

## Worker execution modes

Use fake execution for deterministic API/UI development:

```bash
ZIP_BUILDSERVER_WORKER_EXECUTOR=fake
```

Use Docker execution for real verification runs:

```bash
./scripts/build-worker-image.sh
ZIP_BUILDSERVER_WORKER_EXECUTOR=docker docker compose up --build
```

Docker execution requires the backend to be able to create worker containers and mount run workspaces. For Compose-based local E2E verification, `scripts/verify-local.sh` configures a host bind mount for `/data/zip-buildserver`.

## Local build checks

Run all local build checks:

```bash
./scripts/build-all.sh
```

The script runs:

- backend Maven tests
- frontend dependency installation, tests, and build
- worker image build

## End-to-end verification

Run the complete local fixture flow:

```bash
./scripts/verify-local.sh
```

The script:

1. Builds the local worker image.
2. Starts Docker Compose with Docker worker execution enabled.
3. Creates zip archives from `test-fixtures/`.
4. Creates sessions.
5. Uploads packages.
6. Starts runs.
7. Polls run status.
8. Verifies expected pass/fail results.

Expected fixtures:

| Fixture | Expected status |
| --- | --- |
| `node-pass` | `PASSED` |
| `node-fail` | `FAILED` |
| `maven-pass` | `PASSED` |
| `maven-fail` | `FAILED` |

## Retention cleanup

Default retention settings:

```bash
ZIP_BUILDSERVER_RETENTION_CLEANUP_ENABLED=true
ZIP_BUILDSERVER_RETENTION_CLEANUP_INTERVAL=24h
ZIP_BUILDSERVER_PACKAGE_RETENTION_DAYS=7
ZIP_BUILDSERVER_ARTIFACT_RETENTION_DAYS=14
ZIP_BUILDSERVER_SESSION_RETENTION_DAYS=90
ZIP_BUILDSERVER_WORKSPACE_CLEANUP_GRACE_MINUTES=60
```

Cleanup behavior:

- Expired uploaded package files are deleted from storage while metadata is retained until session metadata expires.
- Expired artifact files and references are removed.
- Old workspace directories are removed after the workspace cleanup grace period.
- Closed sessions older than the metadata retention period are deleted according to database cascade rules.
- Cleanup events are recorded as audit events when retained data is removed.

Disable scheduled cleanup for troubleshooting only:

```bash
ZIP_BUILDSERVER_RETENTION_CLEANUP_ENABLED=false
```

## OpenAPI

Start the backend and retrieve OpenAPI:

```bash
cd backend
mvn quarkus:dev
curl http://localhost:8080/q/openapi
```

Use the `/api/assistant/*` endpoints for assistant integrations because they return compact summaries.

## Troubleshooting

### Backend cannot reach PostgreSQL

Check Compose service health and database variables in `.env`.

### `401 unauthorized`

Use the configured bearer token or disable auth only for private local development.

### Docker worker cannot mount workspace

Set `ZIP_BUILDSERVER_DATA_HOST_DIR` to an absolute host directory and ensure Docker can mount it.

### Worker image missing

Run:

```bash
./scripts/build-worker-image.sh
```

### Frontend cannot reach backend

Confirm `VITE_API_BASE_URL` or the configured reverse proxy path matches the backend URL.

### Full logs are missing

Confirm artifact retention settings and inspect:

```text
GET /api/runs/{runId}/artifacts
GET /api/artifacts/{artifactId}
```
