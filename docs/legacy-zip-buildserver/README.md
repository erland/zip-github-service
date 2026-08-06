# zip-buildserver

`zip-buildserver` is a self-hosted build and test verification service for source-code zip archives.

It is designed for workflows where a person or AI assistant produces an updated source-code zip, uploads it to a trusted self-hosted service, and receives a compact verification result without running the project locally.

## What the service does

- Accepts uploaded source-code zip packages.
- Validates archives before extraction.
- Detects supported Maven and Node/npm project structures.
- Selects administrator-controlled verification plans.
- Runs approved build and test commands in an isolated worker container.
- Captures command results, concise log excerpts, and full log artifacts.
- Provides browser-oriented APIs and assistant-friendly compact JSON APIs.
- Exposes OpenAPI output for integration with tools such as Custom GPT Actions.
- Enforces configured time, resource, storage, and retention controls.

## What the service does not do

- It does not modify source code.
- It does not automatically fix failing builds or tests.
- It does not create commits or pull requests.
- It does not deploy applications.
- It does not run arbitrary commands from users, assistants, `README.md`, or `AGENTS.md` files in uploaded packages.
- It does not replace human review for security, licensing, or production readiness.


### Cleanup

Remove generated temporary files and local E2E data:

```bash
./scripts/clean-temp.sh
```

For a deeper cleanup that also removes `frontend/node_modules/` and the E2E Docker Compose stack:

```bash
./scripts/clean-temp.sh --all --docker
```

## Security warning

Uploaded packages must be treated as untrusted code.

The MVP uses Docker-based worker execution for real verification runs. This is suitable for a trusted self-hosted first version, especially on a dedicated host, but Docker control is powerful. Do not expose this service publicly without reviewing `docs/security-model.md`, setting a strong API token, and understanding the Docker socket risk.

## Repository layout

```text
backend/          Quarkus API, persistence, verification orchestration, workers
frontend/         React/Vite browser UI
worker-images/    Docker worker image definitions
scripts/          Local development and verification scripts
test-fixtures/    Small passing/failing Node and Maven projects for E2E checks
docs/             Product, API, operations, security, and workflow documentation
```

## Prerequisites

For local development and full verification:

- Docker with Docker Compose
- Java 21 and Maven
- Node.js LTS and npm
- `curl`
- `python3`
- `zip`

The backend and frontend can be tested independently with Maven and npm. The complete local verification flow requires Docker.

## Quick start with Docker Compose

Copy the environment template and set a token:

```bash
cp .env.example .env
```

Edit `.env` and set:

```bash
ZIP_BUILDSERVER_AUTH_ENABLED=true
ZIP_BUILDSERVER_API_TOKEN=replace-with-a-long-random-token
```

Start the stack:

```bash
docker compose up --build
```

Open the UI:

```text
http://localhost:5173
```

Stop the stack:

```bash
docker compose down -v
```

## Real verification with Docker workers

Build the worker image and run the stack with Docker execution enabled:

```bash
./scripts/build-worker-image.sh
ZIP_BUILDSERVER_WORKER_EXECUTOR=docker docker compose up --build
```

For the end-to-end fixture flow, run:

```bash
./scripts/verify-local.sh
```

The script zips the fixtures at runtime, uploads them, starts verification runs, and validates expected pass/fail statuses.

## Basic UI workflow

1. Open the frontend.
2. Create a verification session.
3. Upload a `.zip` source package.
4. Start a verification run for the uploaded package.
5. Watch run status update.
6. Review command results, failure summaries, log excerpts, and artifact references.

## Basic API workflow

Create a session:

```bash
curl -fsS -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"label":"local verification"}'
```

Upload a package:

```bash
curl -fsS -X POST "http://localhost:8080/api/sessions/$SESSION_ID/packages" \
  -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN" \
  -F "file=@/path/to/source.zip"
```

Start a run:

```bash
curl -fsS -X POST "http://localhost:8080/api/sessions/$SESSION_ID/runs" \
  -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{"packageId":"$PACKAGE_ID"}"
```

Read the summary:

```bash
curl -fsS "http://localhost:8080/api/runs/$RUN_ID/summary" \
  -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN"
```

See `docs/api-overview.md` for endpoint details.

## Custom GPT Action usage notes

Use the assistant-specific endpoints for compact responses:

```text
POST /api/assistant/verification-sessions
POST /api/assistant/verification-sessions/{sessionId}/runs
GET  /api/assistant/verification-runs/{runId}/summary
GET  /api/assistant/verification-runs/{runId}/failed-log-excerpts
```

Fetch OpenAPI from:

```text
GET /q/openapi
```

For an action, configure bearer-token authentication and prefer the assistant summary endpoints instead of full artifact endpoints. The service intentionally returns concise excerpts by default so assistant context is not flooded with logs.

## Local development commands

Backend:

```bash
cd backend
mvn test
mvn quarkus:dev
```

Frontend:

```bash
cd frontend
npm install
npm test
npm run build
npm run dev
```

All local build checks:

```bash
./scripts/build-all.sh
```

End-to-end Docker verification:

```bash
./scripts/verify-local.sh
```

## Troubleshooting

### `401 unauthorized`

Set `ZIP_BUILDSERVER_API_TOKEN` and include:

```text
Authorization: Bearer <token>
```

### Worker image not found

Build it first:

```bash
./scripts/build-worker-image.sh
```

### Docker worker cannot mount workspaces

When using Docker execution from Docker Compose, set `ZIP_BUILDSERVER_DATA_HOST_DIR` to an absolute host path that is bind-mounted into the backend container and visible to worker containers.

### Builds fail during dependency download

Check network mode and registry access. The MVP `dependency` mode is documented as normal outbound access; harden it with proxies or allowlists for production-like environments.

### Logs are truncated in summaries

This is intentional. Use artifact endpoints for retained full logs.

## Delivery workflow

This repository includes an `AGENTS.md` workflow and progress tracking under `docs/agent-progress.md`. The implementation plan is complete through Step 22.
