# Reuse and migration assessment — zip-buildserver to zip-github

## Metadata

- Step: `0.3`
- Status: `DONE`
- Completed: 6 August 2026
- Source baseline: `zip-github-r0003-step-0.2`
- Target product: `zip-github`

## Purpose

This document is the package- and component-level migration map for transforming the legacy `zip-buildserver` codebase into `zip-github`. It classifies existing material as:

- **reuse** — retain with only naming, configuration or minor compatibility changes;
- **adapt** — retain the design or implementation core, but reshape it for the new domain and security model;
- **replace** — remove the old implementation from the active product and implement a new component for the new domain;
- **archive** — preserve only as historical/reference material; it must not remain in the runtime-critical path.

The functional specification and development plan remain authoritative. This assessment does not implement the migration; step `0.4` creates the clean technical baseline.

## Executive decision

The legacy repository is suitable as a **technical donor**, not as the product architecture to preserve. The strongest reuse candidates are the Quarkus/React application shells, persistence and migration conventions, archive/storage primitives, retention scheduling, audit patterns, shared API/error patterns and generic status UI. The old verification-session/run/command model and the entire Docker-based worker execution path conflict with the new product goals and must be replaced or archived.

No uploaded project code may execute in the zip-github backend. GitHub and GitHub Actions become the repository and execution boundary.

## Migration principles

1. Create a clean `zip-github` baseline rather than renaming every legacy concept in place.
2. Move only explicitly approved reusable/adaptable components into the clean baseline.
3. Introduce new packages and database tables for `Project`, `ImportSession`, `SourceUpload`, `ImportPlan`, `ImportPlanEntry` and `GitHubDelivery`.
4. Do not carry worker, verification-plan or command-execution dependencies into the active runtime.
5. Preserve tests only when they verify behavior that remains relevant; rewrite tests whose terminology or assumptions belong to the old product.
6. Enforce ownership and authorization server-side for every user-owned resource.
7. Keep the legacy code available as reference during migration, but outside the final active source tree.

## Repository-level classification

| Area | Decision | Migration action | Reason |
|---|---|---|---|
| `backend/pom.xml` and Quarkus application shell | **adapt** | Retain Quarkus 3 / Java 21 structure and relevant dependencies; rename artifact/package and remove worker-specific needs | Current framework is suitable, but product identity and dependency surface change |
| `frontend/package.json`, Vite/React/TypeScript shell | **adapt** | Retain stack and test setup; replace product routes, API clients and domain UI | Stack matches the target; current user flows do not |
| PostgreSQL + Flyway | **reuse/adapt** | Retain technology and migration mechanism; create new migrations/schema rather than renaming legacy tables | Technology is appropriate; legacy schema is semantically wrong |
| `docker-compose.yml` | **adapt** | Keep PostgreSQL/backend/frontend composition pattern; remove Docker socket and worker concerns | Local operation remains useful; Docker socket is prohibited |
| `scripts/` | **adapt** | Keep generic build/dev/cleanup ideas; rewrite scripts for new structure and remove worker image handling | Some operational patterns remain useful |
| `docs/` legacy material | **archive** | Preserve under legacy documentation only | Useful context, not authoritative product documentation |
| `test-fixtures/` Maven/Node execution projects | **archive** | Remove from active baseline; possibly reuse later only for Actions error parsing tests | They target local code execution, excluded from MVP |
| `worker-images/` | **archive** | Exclude from active baseline and final runtime | GitHub Actions replaces server-side execution |

## Backend package and component map

### Application and API shell

| Existing component | Decision | Target treatment |
|---|---|---|
| Quarkus REST resource structure | **adapt** | Retain resource/service separation and OpenAPI conventions; create auth, projects, uploads, imports, plans, delivery and checks resources |
| Shared API error response and exception mapping | **adapt** | Retain pattern; align with a consistent problem-style contract, correlation ID and machine-readable codes |
| Health resource | **reuse** | Retain and later extend with database/storage readiness where appropriate |
| Response mapper patterns | **adapt** | Retain mapping separation, replace legacy DTOs with new domain DTOs |
| Assistant-oriented verification resource | **archive** | Remove from MVP runtime; revisit as a small status/control API in step `8.4` |

### Archive upload and storage

| Existing component | Decision | Target treatment |
|---|---|---|
| `ArchiveValidationService` | **adapt** | Use as a behavioral starting point only; add traversal, absolute path, NUL, symlink/special-file, duplicate path, case collision, count/size/path/compression-ratio and wrapper-root controls |
| `ArchiveValidationResult` | **adapt** | Replace result vocabulary with normalized inventory, violations, warnings and limits |
| `PackageStorageService` | **adapt** | Rename concept to source upload storage; retain streaming/file-storage and checksum ideas, add ownership, retention metadata and strict lifecycle |
| `SourcePackageService` and upload orchestration | **replace** | Implement `SourceUpload` and `ImportSession` orchestration with user/project ownership and immutable inspection flow |
| `ArtifactStorageService` | **archive/adapt later** | Not needed for MVP build artifacts; GitHub remains source. Generic safe storage ideas may be reused for temporary uploads only |
| SHA-256 handling | **reuse** | Keep deterministic checksum behavior for ZIP and file inventories |

### Retention and audit

| Existing component | Decision | Target treatment |
|---|---|---|
| Retention cleanup service | **adapt** | Retain cleanup scheduling and deterministic deletion pattern; target uploads and temporary workspaces |
| Scheduled cleanup job | **adapt** | Retain scheduler pattern and add clear ownership/resource metadata and metrics |
| Audit event pattern/entity | **adapt** | Retain event model concept; add actor user ID, project/import/delivery identifiers and safe structured metadata |
| Legacy retention policy tied to verification sessions | **replace** | Define explicit deadlines and states for `SourceUpload` and workspace resources |

### Persistence

| Existing component | Decision | Target treatment |
|---|---|---|
| Panache entity/repository pattern | **reuse** | Retain persistence approach and repository boundaries |
| `V1__initial_schema.sql` | **archive** | Preserve only as legacy reference; do not evolve the new product by renaming old verification tables |
| Verification session/source package/run/command/artifact entities | **replace** | Create new entities and migrations for the target domain |
| Existing indexes/foreign-key conventions | **adapt** | Reuse conventions while adding owner, installation, repository and idempotency constraints |

### Legacy verification and execution domain

| Existing component | Decision | Target treatment |
|---|---|---|
| Verification plan parser/selector/validator | **archive** | Remove from runtime; repository workflows define build/test behavior |
| YAML verification plans under `resources/verification-plans` | **archive** | Exclude from clean baseline |
| Run service/executor/status calculator | **replace** | Replace with import planning, delivery and workflow observation services |
| Command result persistence and failure classification | **archive/adapt later** | Exclude from MVP; limited parsing concepts may inform step `8.2` for GitHub Actions error summaries |
| Project technology detection | **archive/adapt later** | Not required for ZIP-to-PR MVP; may support future AI/export or error summarization |
| `RunStatusTransitions` and old status enums | **replace** | Implement explicit `ImportSession` and `GitHubDelivery` state machines |

### Worker and Docker execution

| Existing component | Decision | Target treatment |
|---|---|---|
| `CommandExecutor`, runtime/fake executors | **archive** | Do not include in active source tree |
| Docker command executor and command builder | **archive** | Remove from runtime-critical path |
| Docker workspace mapping/cleanup/resource limits | **archive** | Do not reuse for code execution; generic temporary workspace cleanup must be implemented without Docker execution semantics |
| Docker socket mount | **replace/remove** | Must not exist in target compose/deployment configuration |
| Worker image build script and `worker-images/` | **archive** | Preserve only in legacy reference material |

### Authentication and authorization

| Existing component | Decision | Target treatment |
|---|---|---|
| Static API bearer-token filter/service | **replace** | Implement GitHub login, secure server-side web session, CSRF/state protection and logout |
| Bearer-token pattern for future machine API | **archive/adapt later** | Reconsider only for an explicitly scoped integration API after MVP |
| Current resource access model | **replace** | Every project/import/upload/plan/delivery lookup must be owner-scoped and installation/repository authorization-checked |

## Frontend component map

### Reusable application foundation

| Existing component | Decision | Target treatment |
|---|---|---|
| React/Vite/TypeScript bootstrap | **reuse/adapt** | Retain stack and build configuration; rename product and routes |
| React Router setup | **adapt** | Replace legacy route tree with login, project list, project detail, new import, review, result and history routes |
| TanStack Query setup | **reuse/adapt** | Retain server-state pattern; replace endpoints/query keys and add auth-aware error handling |
| Testing Library/Vitest setup | **reuse** | Retain test infrastructure and accessibility-oriented component tests |
| Global CSS and responsive shell | **adapt** | Retain useful reset/layout patterns; redesign for mobile-first import flow |
| Generic page state/loading/error component | **reuse/adapt** | Keep generic states and update wording/accessibility as needed |
| Generic status badge styling | **adapt** | Retain visual pattern; replace legacy status vocabulary |

### Components to adapt

| Existing component | Decision | Target treatment |
|---|---|---|
| `PackageUploadDropzone` | **adapt** | Convert to mobile-friendly ZIP selector/uploader with progress, cancellation and iOS Files support |
| `PollingRunStatus` | **adapt later** | Rework for bounded polling of GitHub checks/workflow observations in phase 6 |
| `ArtifactList` | **adapt later** | Use only for links to GitHub Actions artifacts, not local artifact storage |
| Failure/log panels | **archive/adapt later** | Exclude from MVP; concepts may return for condensed GitHub Actions failures |

### Components and pages to replace

| Existing component | Decision | Target treatment |
|---|---|---|
| Session create form/page | **replace** | Project configuration and new import flow |
| Run page and command result table | **replace** | Import review, delivery result and check status views |
| Plans page | **archive** | Verification plans are removed from the product |
| API clients for sessions/packages/runs/artifacts | **replace** | New clients for auth, GitHub installations/repositories, projects, imports, plans, delivery and checks |
| Legacy API types | **replace** | Generate or define target-domain contracts |
| Home/about copy | **adapt** | Update product purpose and navigation |

## Test migration map

| Test area | Decision | Target treatment |
|---|---|---|
| Archive validation tests | **adapt** | Expand into malicious ZIP fixture matrix and deterministic inventory tests |
| Retention tests | **adapt** | Retarget temporary uploads/workspaces and ownership-safe cleanup |
| API exception/health tests | **reuse/adapt** | Keep structure and update contracts |
| Persistence integration pattern/Testcontainers | **reuse** | Keep PostgreSQL integration strategy |
| Verification/run/worker tests | **archive** | Remove from active suite with archived code |
| Authentication filter tests | **replace** | Add OAuth state, session, CSRF, logout and authorization/ownership tests |
| Frontend page-state/status tests | **reuse/adapt** | Keep generic coverage, replace statuses and flows |
| Upload component tests | **adapt** | Add mobile file input, progress, validation and recovery scenarios |
| Negative cross-user access tests | **new** | Mandatory for projects, imports, uploads, plans and deliveries |
| Git delivery tests against local bare repository | **new** | Verify exact files, branch, atomic commit and idempotency |

## What moves into the clean baseline in step 0.4

Step `0.4` should create a minimal active codebase containing only:

- Quarkus/Java 21 backend shell;
- React/TypeScript/Vite frontend shell;
- PostgreSQL/Flyway integration;
- shared API error and health patterns where they can be moved cleanly;
- generic frontend page state/status patterns;
- test infrastructure;
- local development composition without Docker socket;
- current zip-github documentation and status ledger.

Archive/storage/retention/audit code should be moved only when it can compile under target naming and without pulling legacy run/worker dependencies. If selective extraction becomes risky in step `0.4`, keep a thinner clean shell and migrate those components in their dedicated later steps.

## Components forbidden from the target critical path

The following must not remain active dependencies of the MVP application:

- Docker socket access;
- worker containers and worker images;
- execution of Maven, npm, Pandoc or arbitrary uploaded commands by the backend;
- verification-plan YAML selection and execution;
- verification run and command-result orchestration;
- long-lived static bearer token as the web authentication mechanism;
- direct write to the default branch;
- modification of `.github/**` from an uploaded ZIP;
- unscoped entity lookup by user-provided UUID without ownership verification.

## Migration order

1. Step `0.4`: create the clean framework baseline and archive/remove legacy runtime paths.
2. Phase 1: introduce target domain, schema, API shell and frontend routes.
3. Phase 2: replace authentication and implement GitHub installation authorization.
4. Phase 3: migrate/adapt upload, archive, storage and retention behavior.
5. Phase 4: implement repository snapshot, comparison and immutable import plan.
6. Phase 5: implement isolated Git workspace and GitHub delivery.
7. Phase 6 onward: adapt status/artifact presentation only for GitHub-hosted results.

## Acceptance result for step 0.3

- Legacy packages and major components are classified as `reuse`, `adapt`, `replace` or `archive`.
- The components intended for the clean baseline are identified.
- The legacy areas forbidden from the new critical path are explicitly listed.
- No product source code was changed in this step.
