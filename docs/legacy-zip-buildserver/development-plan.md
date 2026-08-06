# Development Plan: zip-buildserver

## 1. Goal

Implement `zip-buildserver`, a self-hosted service that verifies uploaded source-code zip packages by running predefined build and test checks in an isolated execution environment and returning concise structured reports suitable for both humans and AI assistant integrations such as Custom GPT Actions.

The repository is assumed to be created as:

```text
github.com/erland/zip-buildserver
```

The service shall support the workflow where a user or assistant uploads an updated source-code zip, the service runs configured verification checks, and the user or assistant receives a compact build/test result without needing to run the project locally.

The service shall verify packages only. It shall not modify source code, create commits, deploy applications, or run arbitrary user-supplied shell commands.

## 2. Technology Choices

### 2.1 Frontend

Use:

```text
React
TypeScript
Vite
React Router
TanStack Query
CSS Modules or plain CSS
Vitest + React Testing Library
```

Rationale:

- React and TypeScript match the preferred frontend stack.
- Vite keeps the frontend lightweight and easy to containerize.
- TanStack Query is suitable for polling verification runs and managing API state.
- The UI can stay focused: upload package, start run, view results, inspect logs.

### 2.2 Backend

Use:

```text
Java 21
Quarkus
Maven
RESTEasy Reactive / Jackson
Hibernate Validator
SmallRye OpenAPI
PostgreSQL
Flyway
JUnit 5 + RestAssured + Testcontainers
```

Rationale:

- Quarkus fits well with Java-based service development and containerized deployment.
- SmallRye OpenAPI makes it straightforward to expose an OpenAPI contract for Custom GPT Actions.
- PostgreSQL gives durable storage for sessions, runs, package metadata, command results, artifacts, and audit data.
- Testcontainers is useful for database-backed integration tests.

### 2.3 Execution Worker

Use Docker-based ephemeral worker containers for verification runs.

Initial model:

```text
zip-buildserver-api
  |
  | Docker API
  v
ephemeral worker container per verification run
```

The backend orchestrates workers but does not execute uploaded package code directly inside the API container.

Important security note: mounting the Docker socket into the backend container is convenient but powerful. It effectively grants the backend broad control over the Docker host. This is acceptable only for a trusted self-hosted first version. Keep the execution layer behind an interface so it can later be replaced with rootless Docker, Podman, Kubernetes Jobs, a separate worker host, or microVM isolation.

### 2.4 Packaging

Use Docker Compose for the initial deployment:

```text
docker-compose.yml
  zip-buildserver-api
  zip-buildserver-web
  postgres
```

Use separate API and web containers rather than a single all-in-one container for the first version. This keeps responsibilities clear and makes local development simpler. An all-in-one image can be added later if desired.

## 3. Assumptions

- The service is self-hosted by a trusted operator.
- Uploaded source packages are untrusted.
- The first version is single-admin or trusted-user oriented.
- Docker is available on the target server.
- Verification commands are predefined by server-side plans.
- Uploaded package documentation, such as `README.md` or `AGENTS.md`, may be read as metadata but must not define commands to execute.
- Some projects may require network access to fetch dependencies.
- The first supported project types are Maven and Node/npm projects.
- The service is intended for development-time verification, not production deployment.

## 4. Target Repository Structure

```text
zip-buildserver/
  README.md
  AGENTS.md
  docker-compose.yml
  .env.example
  .gitignore

  docs/
    functional-specification.md
    development-plan.md
    api-overview.md
    security-model.md
    verification-plans.md
    operations.md

  backend/
    pom.xml
    Dockerfile
    src/
      main/
        java/dev/erland/zipbuildserver/
          api/
          application/
          domain/
          infrastructure/
          worker/
          storage/
          security/
          config/
        resources/
          application.properties
          db/migration/
          verification-plans/
      test/
        java/dev/erland/zipbuildserver/

  frontend/
    package.json
    Dockerfile
    vite.config.ts
    tsconfig.json
    src/
      main.tsx
      App.tsx
      api/
      components/
      pages/
      routes/
      styles/

  worker-images/
    node-maven/
      Dockerfile

  scripts/
    dev-up.sh
    dev-down.sh
    build-all.sh
    build-worker-image.sh
    verify-local.sh

  test-fixtures/
    README.md
```

## 5. Backend Architecture

### 5.1 Layers

Use a layered backend structure:

```text
api/
  REST resources, request DTOs, response DTOs, error mapping

application/
  use cases and orchestration

domain/
  core models, state transitions, validation rules

infrastructure/
  persistence, Docker integration, filesystem storage

worker/
  execution abstractions and worker adapters

storage/
  package and artifact storage services

security/
  authentication, authorization, audit

config/
  runtime configuration and verification-plan loading
```

### 5.2 Core Domain Objects

```text
VerificationSession
  id
  label
  status
  createdAt
  closedAt
  createdBy
  retentionPolicy

SourcePackage
  id
  sessionId
  originalFilename
  checksumSha256
  compressedSizeBytes
  extractedSizeBytes
  fileCount
  topLevelEntries
  storageReference
  status
  rejectionReason
  createdAt

VerificationRun
  id
  sessionId
  sourcePackageId
  status
  planId
  requestedPlanId
  networkMode
  summary
  startedAt
  completedAt
  durationMillis

VerificationCommandResult
  id
  runId
  commandLabel
  workingDirectory
  commandDisplay
  status
  exitCode
  startedAt
  completedAt
  durationMillis
  logExcerpt
  failureCategory
  failureMessage
  stdoutArtifactRef
  stderrArtifactRef

ArtifactReference
  id
  runId
  type
  storageReference
  sizeBytes
  createdAt
  expiresAt
```

### 5.3 API Endpoints

Initial human/API endpoints:

```text
POST   /api/sessions
GET    /api/sessions/{sessionId}
POST   /api/sessions/{sessionId}/close

POST   /api/sessions/{sessionId}/packages
GET    /api/packages/{packageId}

POST   /api/sessions/{sessionId}/runs
GET    /api/runs/{runId}
GET    /api/runs/{runId}/summary
POST   /api/runs/{runId}/cancel

GET    /api/runs/{runId}/artifacts
GET    /api/artifacts/{artifactId}

GET    /api/verification-plans
GET    /api/health
```

Initial package upload can be multipart:

```text
POST /api/sessions/{sessionId}/packages
Content-Type: multipart/form-data
file=<zip>
```

Assistant-friendly endpoints:

```text
POST /api/assistant/verification-sessions
POST /api/assistant/verification-sessions/{sessionId}/runs
GET  /api/assistant/verification-runs/{runId}/summary
GET  /api/assistant/verification-runs/{runId}/failed-log-excerpts
```

The assistant endpoints should return compact structured JSON, not full logs by default.

## 6. Frontend Architecture

### 6.1 Pages

```text
HomePage
  Create a new verification session and explain the service.

SessionPage
  Show session metadata, package upload, and run list.

RunPage
  Show live status, command-level results, failure summary, and log excerpts.

PlansPage
  Show available verification plans and supported project structures.

AboutPage
  Show version, configured limits, and security/retention summary.
```

### 6.2 Components

```text
SessionCreateForm
PackageUploadDropzone
RunStatusBadge
ProjectDetectionSummary
CommandResultTable
FailureSummaryCard
LogExcerptPanel
ArtifactList
VerificationPlanList
PollingRunStatus
```

### 6.3 API Client

```text
frontend/src/api/client.ts
frontend/src/api/types.ts
frontend/src/api/sessions.ts
frontend/src/api/packages.ts
frontend/src/api/runs.ts
frontend/src/api/plans.ts
```

TanStack Query hooks:

```text
useCreateSession
useSession
useUploadPackage
useCreateRun
useRun
useRunSummary
useVerificationPlans
```

## 7. Verification Plans

### 7.1 Initial Plans

#### node-default

Detection:

```text
package.json
```

Checks:

```text
npm ci
npm test -- --runInBand
npm run build
```

Rules:

- Skip test if no `test` script exists, unless policy requires tests.
- Skip build if no `build` script exists, unless policy requires build.
- Report skipped scripts as warnings.

#### maven-default

Detection:

```text
pom.xml
```

Checks:

```text
mvn test
mvn package -DskipTests
```

Rules:

- Use Maven wrapper only if policy allows it.
- Otherwise use Maven installed in the worker image.
- Classify compilation failures separately from test failures.

#### multi-project-default

Detection:

```text
backend/pom.xml
frontend/package.json
```

Checks:

```text
backend: mvn test
backend: mvn package -DskipTests
frontend: npm ci
frontend: npm test -- --runInBand
frontend: npm run build
```

Rules:

- Run sequentially in the MVP.
- Later versions may support parallel independent checks.

### 7.2 Plan Configuration

Store verification plans server-side:

```text
backend/src/main/resources/verification-plans/node-default.yml
backend/src/main/resources/verification-plans/maven-default.yml
backend/src/main/resources/verification-plans/multi-project-default.yml
```

Do not execute commands defined by uploaded files.

## 8. Execution Worker

### 8.1 Worker Image

Create:

```text
worker-images/node-maven/Dockerfile
```

The first worker image should include:

```text
Java 21
Maven
Node.js LTS
npm
basic shell tools
non-root execution user
```

### 8.2 Run Flow

For each verification run:

1. Create an isolated workspace.
2. Extract the uploaded package safely.
3. Detect project structure.
4. Select verification plan.
5. Start an ephemeral worker container.
6. Run approved commands in order.
7. Capture stdout, stderr, exit code, duration, and timeout state.
8. Store full logs as artifacts.
9. Generate concise excerpts and structured failure details.
10. Remove worker container.
11. Delete workspace according to retention policy.
12. Persist final run status.

### 8.3 Initial Resource Limits

Make these configurable:

```text
max archive size: 100 MB
max extracted size: 500 MB
max files: 20,000
max total run duration: 15 minutes
max command duration: 10 minutes
max stdout/stderr retained per command: 10 MB
default memory limit: 2 GB
default CPU limit: 2 cores
```

### 8.4 Network Modes

Support:

```text
none
dependency
full
```

Initial default:

```text
dependency
```

For the MVP, `dependency` may initially mean normal outbound access with clear documentation. Later, it can be tightened with proxying, registry allowlists, or network policies.

## 9. Storage

### 9.1 Database

Use PostgreSQL tables:

```text
verification_session
source_package
verification_run
verification_command_result
artifact_reference
audit_event
```

### 9.2 Filesystem Storage

Initial storage:

```text
/data/zip-buildserver/packages/
/data/zip-buildserver/artifacts/
/data/zip-buildserver/workspaces/
```

Abstract storage behind service interfaces so S3-compatible storage can be added later.

### 9.3 Retention Defaults

```text
uploaded packages: 7 days
extracted workspaces: delete immediately after run
logs/artifacts: 14 days
session metadata: 90 days
```

## 10. Security Model

### 10.1 Authentication

MVP authentication:

```text
static API token for API access
```

Example configuration:

```text
ZIP_BUILDSERVER_API_TOKEN=change-me
```

Do not place this token in a public frontend bundle. For a private self-hosted UI, prefer same-origin access behind a reverse proxy or add a future login/session mechanism.

### 10.2 Uploaded Code Safety

The service must:

- Treat uploaded packages as malicious.
- Validate archives before extraction.
- Prevent path traversal.
- Avoid exposing secrets to workers.
- Run worker containers with resource limits.
- Avoid mounting host paths except the run workspace.
- Return only concise logs to assistants by default.
- Delete workspaces after runs.

### 10.3 Docker Socket Warning

Document that Docker socket access is powerful. Recommended production hardening path:

```text
1. Run on a dedicated host.
2. Use rootless Docker or Podman if possible.
3. Move execution to a separate worker host.
4. Consider Kubernetes Jobs or microVMs for stronger isolation.
```

## 11. Implementation Steps

## Step 1: Initialize Repository Skeleton

Create the repository structure, documentation placeholders, `.gitignore`, `.env.example`, and root README.

Deliverables:

```text
README.md
AGENTS.md
.gitignore
.env.example
docs/functional-specification.md
docs/development-plan.md
backend/
frontend/
worker-images/
scripts/
```

Verification:

```bash
find . -maxdepth 3 -type f | sort
```

Expected outcome:

- Repository has a clear root structure.
- Documentation is present.
- No generated dependencies or build artifacts are committed.

## Step 2: Create Backend Quarkus Project

Initialize the backend Maven project with REST, validation, OpenAPI, database, Flyway, and test dependencies.

Deliverables:

```text
backend/pom.xml
backend/src/main/java/dev/erland/zipbuildserver/
backend/src/main/resources/application.properties
backend/src/test/java/dev/erland/zipbuildserver/
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Backend compiles.
- Basic health/resource test passes.

## Step 3: Create Frontend React Project

Initialize the Vite React/TypeScript frontend.

Deliverables:

```text
frontend/package.json
frontend/src/main.tsx
frontend/src/App.tsx
frontend/src/api/
frontend/src/pages/
frontend/src/components/
```

Verification:

```bash
cd frontend
npm install
npm run build
```

Expected outcome:

- Frontend builds successfully.
- Initial page renders service title and placeholder navigation.

## Step 4: Add Docker Compose Development Environment

Create Docker Compose configuration for backend, frontend, PostgreSQL, and the Docker execution setup.

Deliverables:

```text
docker-compose.yml
backend/Dockerfile
frontend/Dockerfile
.env.example
scripts/dev-up.sh
scripts/dev-down.sh
```

Verification:

```bash
docker compose up --build
```

Expected outcome:

- PostgreSQL starts.
- Backend starts and reaches the database.
- Frontend starts and reaches the backend health endpoint.

## Step 5: Implement Database Schema and Core Entities

Add Flyway migration and persistence mapping.

Deliverables:

```text
backend/src/main/resources/db/migration/V1__initial_schema.sql
backend/src/main/java/dev/erland/zipbuildserver/domain/
backend/src/main/java/dev/erland/zipbuildserver/infrastructure/persistence/
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Migration applies in tests.
- Repository tests pass.

## Step 6: Implement Session API

Implement session creation, reading, listing, and closing.

Deliverables:

```text
POST /api/sessions
GET /api/sessions/{sessionId}
POST /api/sessions/{sessionId}/close
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Session lifecycle works.
- Invalid IDs return controlled errors.

## Step 7: Implement Package Upload and Archive Validation

Implement multipart zip upload, checksum calculation, metadata extraction, and safe archive validation.

Deliverables:

```text
POST /api/sessions/{sessionId}/packages
GET /api/packages/{packageId}
ArchiveValidationService
PackageStorageService
```

Validation must cover:

```text
zip format
size limits
file count limits
extracted size limits
path traversal
absolute paths
malformed entries
unsafe links or special files according to policy
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Valid zip accepted.
- Unsafe zip rejected.
- Package metadata recorded.

## Step 8: Implement Project Detection

Implement detection for Maven, Node, and multi-project layouts.

Deliverables:

```text
ProjectDetectionService
DetectedProject model
Package detection summary in API response
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- `pom.xml`, `package.json`, and `backend` + `frontend` layouts are detected.
- Unsupported packages do not trigger command execution.

## Step 9: Implement Verification Plan Configuration

Add configured verification plans and selection logic.

Deliverables:

```text
verification-plans/node-default.yml
verification-plans/maven-default.yml
verification-plans/multi-project-default.yml
VerificationPlanService
GET /api/verification-plans
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Plans load from server configuration.
- Plans are selected deterministically.
- Uploaded files cannot define commands.

## Step 10: Implement Run Creation and State Machine

Implement run creation and state transitions without real Docker execution yet.

Deliverables:

```text
POST /api/sessions/{sessionId}/runs
GET /api/runs/{runId}
GET /api/runs/{runId}/summary
VerificationRunService
RunStatus transition rules
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Runs can be created for accepted packages.
- Runs cannot be created for rejected packages or closed sessions.
- Initial structured summary is available.

## Step 11: Implement Worker Image

Create the first worker image.

Deliverables:

```text
worker-images/node-maven/Dockerfile
scripts/build-worker-image.sh
```

Verification:

```bash
docker build -t zip-buildserver-worker-node-maven:local worker-images/node-maven
docker run --rm zip-buildserver-worker-node-maven:local java -version
docker run --rm zip-buildserver-worker-node-maven:local mvn -version
docker run --rm zip-buildserver-worker-node-maven:local node --version
docker run --rm zip-buildserver-worker-node-maven:local npm --version
```

Expected outcome:

- Worker image contains required tools.
- Worker runs as non-root where practical.

## Step 12: Implement Execution Abstraction

Create an execution interface and a fake implementation for tests.

Deliverables:

```text
CommandExecutor
CommandExecutionRequest
CommandExecutionResult
FakeCommandExecutor
DockerCommandExecutor skeleton
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Application logic can execute verification plans through an abstraction.
- Unit tests do not require Docker.

## Step 13: Implement Fake Verification Execution

Wire execution flow using the fake executor for deterministic tests.

Deliverables:

```text
VerificationExecutionService
CommandResult persistence
LogExcerptService
FailureClassificationService initial version
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- A run can transition from queued to running to passed or failed.
- Command results are persisted.
- Summaries include failure details.

## Step 14: Implement Docker-Based Execution

Implement real Docker worker execution.

Deliverables:

```text
DockerCommandExecutor
DockerWorkspaceService
ResourceLimitConfig
Timeout handling
Log capture
Container cleanup
```

Verification:

```bash
cd backend
mvn test
```

Manual verification:

```bash
docker compose up --build
# upload a small Maven or Node zip through the API
# create a run
# inspect summary
```

Expected outcome:

- Worker containers are created per run.
- Commands execute in the extracted workspace.
- Containers are removed after completion.
- Timeouts and failures are captured.

## Step 15: Implement Artifact Storage

Store full logs separately and expose authorized retrieval.

Deliverables:

```text
ArtifactStorageService
GET /api/runs/{runId}/artifacts
GET /api/artifacts/{artifactId}
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Default summary contains short excerpts.
- Full logs are stored separately.
- Artifact IDs are opaque.

## Step 16: Implement Frontend Session and Upload Flow

Build the UI for creating sessions and uploading zip packages.

Deliverables:

```text
HomePage
SessionPage
PackageUploadDropzone
API hooks for sessions and package upload
```

Verification:

```bash
cd frontend
npm test
npm run build
```

Manual verification:

```bash
docker compose up --build
# create session in browser
# upload test zip
```

Expected outcome:

- User can create a session.
- User can upload a zip.
- Validation errors are displayed.

## Step 17: Implement Frontend Run Flow

Build the UI for starting runs and polling run status.

Deliverables:

```text
RunPage
RunStatusBadge
CommandResultTable
FailureSummaryCard
LogExcerptPanel
ArtifactList
Polling behavior
```

Verification:

```bash
cd frontend
npm test
npm run build
```

Manual verification:

```bash
docker compose up --build
# upload package
# start run
# watch status update
```

Expected outcome:

- User can start a run.
- Run status updates automatically.
- Passed, failed, rejected, and timed-out states are clear.

## Step 18: Add Assistant-Friendly API and OpenAPI Refinement

Add compact assistant-specific endpoints and verify OpenAPI output is suitable for a Custom GPT Action.

Deliverables:

```text
/api/assistant/verification-sessions
/api/assistant/verification-sessions/{sessionId}/runs
/api/assistant/verification-runs/{runId}/summary
/q/openapi
docs/api-overview.md
```

Verification:

```bash
cd backend
mvn test
curl http://localhost:8080/q/openapi
```

Expected outcome:

- Assistant endpoints return compact structured JSON.
- OpenAPI schema is available.
- Large logs are not returned by default.

## Step 19: Add Authentication and Basic Access Control

Implement static token authentication for API access.

Deliverables:

```text
Auth filter or interceptor
Configurable API token
Protected write endpoints
docs/operations.md updated
```

Verification:

```bash
cd backend
mvn test
```

Manual verification:

```bash
curl -i http://localhost:8080/api/sessions
curl -i -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN" http://localhost:8080/api/sessions
```

Expected outcome:

- Protected endpoints require token.
- Health endpoint remains available if configured.
- Documentation explains token setup.

## Step 20: Add Retention Cleanup

Implement scheduled cleanup of expired packages, artifacts, and workspaces.

Deliverables:

```text
RetentionCleanupService
Retention configuration
Audit events for cleanup
```

Verification:

```bash
cd backend
mvn test
```

Expected outcome:

- Expired artifacts are removed.
- Metadata retention follows policy.
- Workspaces are deleted after runs.

## Step 21: Add End-to-End Docker Verification

Add small sample fixtures and a local verification script.

Deliverables:

```text
scripts/verify-local.sh
test-fixtures/node-pass/
test-fixtures/node-fail/
test-fixtures/maven-pass/
test-fixtures/maven-fail/
```

The script can zip fixtures during execution rather than committing binary zip files.

Verification:

```bash
./scripts/verify-local.sh
```

Expected outcome:

- Docker Compose stack starts.
- Fixture package uploads.
- Run executes.
- Script verifies expected passed/failed status.

## Step 22: Complete Documentation and Release Readiness

Complete user and operator documentation.

Deliverables:

```text
README.md
docs/security-model.md
docs/verification-plans.md
docs/operations.md
docs/api-overview.md
.env.example
```

README must explain:

- What the service does.
- What it does not do.
- Security warning about untrusted code.
- Docker Compose installation.
- Basic UI workflow.
- Basic API workflow.
- Custom GPT Action usage notes.
- Troubleshooting.

Verification:

```bash
./scripts/build-all.sh
./scripts/verify-local.sh
```

Expected outcome:

- A new user can clone the repo, configure `.env`, run Docker Compose, upload a zip, and see verification results.

## 12. MVP Scope

Include in MVP:

```text
React/TypeScript frontend
Quarkus backend
PostgreSQL metadata storage
local filesystem package/artifact storage
Docker Compose deployment
zip upload
archive validation
Maven and Node project detection
configured verification plans
Docker worker execution
concise structured reports
full log artifacts
basic static-token API authentication
retention cleanup
OpenAPI output for Custom GPT Action configuration
```

Exclude from MVP:

```text
source-code modification
automatic Git commits
GitHub PR creation
arbitrary command execution
multi-user RBAC
private dependency registry support
Kubernetes worker execution
microVM isolation
advanced AI log summarization
```

## 13. Suggested Milestones

### Milestone 1: Foundation

Steps 1-5.

Result:

- Repository, backend, frontend, Docker Compose, database schema.

### Milestone 2: Package Intake

Steps 6-10.

Result:

- Sessions, zip upload, archive validation, project detection, plan selection, run state.

### Milestone 3: Execution

Steps 11-15.

Result:

- Worker image, execution abstraction, Docker execution, log capture, artifacts.

### Milestone 4: User Interface

Steps 16-17.

Result:

- Browser UI for sessions, uploads, runs, reports.

### Milestone 5: Assistant and Operations

Steps 18-22.

Result:

- Assistant-friendly API, OpenAPI, auth, cleanup, docs, local verification script.

## 14. Local Development Commands

Root-level scripts:

```bash
./scripts/dev-up.sh
./scripts/dev-down.sh
./scripts/build-all.sh
./scripts/verify-local.sh
```

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

Docker:

```bash
docker compose up --build
docker compose down -v
```

Worker image:

```bash
docker build -t zip-buildserver-worker-node-maven:local worker-images/node-maven
```

## 15. Risks and Mitigations

### Risk: Docker socket access is powerful

Mitigations:

- Document clearly.
- Deploy on a dedicated host.
- Keep the service admin-only initially.
- Encapsulate Docker integration behind an interface.
- Avoid mounting secrets into worker containers.

### Risk: Uploaded code is malicious

Mitigations:

- Validate archives.
- Run in isolated containers.
- Apply resource limits and timeouts.
- Avoid exposing secrets.
- Restrict network access where practical.
- Delete workspaces after runs.

### Risk: Logs are too large for assistant use

Mitigations:

- Store full logs as artifacts.
- Return concise structured summaries.
- Add log truncation and failure extraction.

### Risk: Project detection is incomplete

Mitigations:

- Start with Maven and Node.
- Clearly report unsupported structures.
- Add plans incrementally.

### Risk: Dependency downloads are slow or unsafe

Mitigations:

- Make network mode explicit.
- Add dependency cache later.
- Consider registry allowlists or proxying.

### Risk: Long-running builds consume server resources

Mitigations:

- Queue jobs.
- Enforce CPU, memory, disk, and time limits.
- Add concurrency limits.

## 16. Future Enhancements

Potential future features:

```text
Git repository verification mode
patch/diff verification mode
GitHub pull request integration
S3-compatible artifact storage
multi-user accounts and RBAC
per-user quotas
Kubernetes Jobs execution backend
Podman/rootless execution backend
dependency cache management
private registry credentials with secret isolation
richer failure classification
compare two verification runs
webhook notification on completion
downloadable verification report
Custom GPT Action package generator
```

## 17. Definition of Done for First Release

The first release is done when:

1. A user can run the service with Docker Compose.
2. A user can create a session from the UI.
3. A user can upload a zip file.
4. The service validates the archive safely.
5. The service detects Maven and Node projects.
6. The service runs predefined verification commands in a worker container.
7. The service returns passed, failed, timed-out, rejected, and incomplete statuses.
8. The UI shows command-level results and failure summaries.
9. Full logs are stored separately and accessible when authorized.
10. The assistant-friendly API returns compact structured summaries.
11. OpenAPI output is available for configuring a Custom GPT Action.
12. Basic static-token authentication is available.
13. Retention cleanup removes expired packages and artifacts.
14. Documentation explains install, usage, security model, and limitations.
15. `./scripts/verify-local.sh` demonstrates the complete flow.
