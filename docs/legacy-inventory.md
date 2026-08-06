# Legacy inventory — zip-buildserver

## Metadata

- Step: `0.1`
- Status: `DONE`
- Completed: 6 August 2026
- Source archive: `zip-buildserver-main(1).zip`
- Source product: `zip-buildserver`
- Target product: `zip-github`

## Scope and method

The legacy archive was unpacked into a clean working directory. The `__MACOSX` metadata tree was excluded from the project copy. This step inventories the material only; it does not run builds or tests and does not yet decide final reuse at class level. Build verification belongs to step 0.2 and the formal reuse decision belongs to step 0.3.

## Repository structure

| Area | Observed content | Initial relevance |
|---|---|---|
| `backend/` | Java 21, Quarkus 3.34.3, REST API, Panache persistence, Flyway, scheduler, archive handling, storage, retention, workers and tests | Strong technical starting point, but old run/verification domain must be replaced |
| `frontend/` | React 19, TypeScript 5.8, Vite 6, React Router 7, TanStack Query, Vitest and Testing Library | Reusable application shell and UI/test patterns |
| `docs/` | Functional specification, development/delivery plans, API, security, operations and agent workflow | Useful reference material; not authoritative for the new product |
| `scripts/` | Build, local verification, Docker lifecycle, worker image and cleanup scripts | Some build/cleanup patterns may be adapted; worker-specific scripts are legacy |
| `test-fixtures/` | Passing and failing Maven and Node projects | Useful for understanding the old execution flow; may be archived after migration |
| `worker-images/` | Docker worker image for Node/Maven execution | Must not remain in the new MVP critical path |
| `docker-compose.yml` | PostgreSQL, backend, frontend and Docker socket mount | PostgreSQL/app composition is relevant; Docker socket mount must be removed later |

## Backend inventory

### API layer

The backend contains resources and response models for:

- health,
- sessions,
- package upload and lookup,
- verification plans,
- verification runs and summaries,
- artifacts and artifact contents,
- compact assistant-oriented verification endpoints,
- exception mapping and a shared error response.

The API structure and OpenAPI-oriented patterns can inform the new API. The session/package/run semantics do not match the new `Project`/`ImportSession`/`ImportPlan`/`GitHubDelivery` domain.

### Application services

Notable application areas include:

- `ArchiveValidationService` and `ArchiveValidationResult`,
- source package orchestration,
- project technology detection,
- retention cleanup and scheduled cleanup,
- verification plan parsing, validation, selection and lookup,
- run execution and status calculation,
- command result persistence,
- log excerpt and failure classification,
- artifact handling,
- assistant response mapping.

Archive validation, storage, retention, audit and generic mapping/error patterns are candidates for later adaptation. Verification-plan and execution services are specific to the legacy product.

### Domain and persistence

The legacy domain models sessions, source packages, verification plans, runs, command results and artifacts. Persistence uses Panache entities and repositories. The initial Flyway migration creates the legacy schema. These names and relationships are not suitable as the permanent zip-github domain, but the persistence conventions and audit entity pattern may be reusable.

### Security

The current browser/API model uses a configurable static bearer token through `ApiTokenAuthenticationFilter` and `TokenAuthenticationService`. This is not sufficient for the planned multi-user product. It must later be replaced for browser use by GitHub identity, server-side sessions, resource ownership checks and GitHub App installation authorization.

### Storage and retention

The backend has package storage, artifact storage and scheduled retention cleanup. This is directly relevant to temporary ZIP storage and workspace cleanup, subject to security review and terminology changes.

### Worker execution

The backend includes worker abstractions and Docker-based command execution. The Docker Compose configuration mounts `/var/run/docker.sock` into the backend container. The new MVP explicitly must not execute uploaded project code locally, so the worker execution path and Docker socket dependency are legacy-only and must be removed from the critical path.

## Frontend inventory

The frontend is a React/Vite single-page application with:

- routing and application shell,
- home, session, run, plan and about pages,
- session creation and package upload components,
- polling and status badges,
- artifact and command result presentation,
- a typed API client,
- component, page, routing and utility tests.

The shell, responsive styling patterns, API client conventions, page-state handling, polling and status components may be adapted. Session/run/verification-plan pages and terminology must be replaced by project/import/review/delivery flows.

## Database and migrations

- Database: PostgreSQL.
- Migration tool: Flyway.
- Legacy migration: `V1__initial_schema.sql`.
- Persistence stack: Hibernate ORM with Panache.
- Integration-test support: Testcontainers PostgreSQL dependency is present.

The new domain should use new migrations and clearly named tables rather than disguising legacy verification tables as import entities.

## Test inventory

The repository contains backend unit/API/integration-oriented tests, frontend Vitest/Testing Library tests, and fixture projects for Docker-based end-to-end verification. The exact baseline result has not been established in this step. Step 0.2 must run and document:

- backend tests and build,
- frontend tests and production build,
- available wrapper/version requirements,
- any Docker-dependent verification separately.

## Documentation inventory

The legacy documentation covers product behavior, delivery planning, agent progress, review checklist, operations, API, security and verification plans. It is retained under `docs/legacy-zip-buildserver/` for reference. The authoritative target documentation remains the zip-github functional specification, development plan, implementation steps and status register.

## Transport cleanup performed

- Excluded the archive-level `__MACOSX/` tree.
- Did not include AppleDouble `._*` transport files in the new project copy.
- Preserved source code and configuration otherwise for baseline verification.

## Initial risks and constraints

1. The legacy application is centered on verification runs rather than GitHub delivery.
2. Docker socket access is a significant security boundary and conflicts with the target MVP.
3. Static bearer-token authentication is not a multi-user authorization model.
4. Existing database terminology can create misleading reuse if retained unchanged.
5. Archive validation must be reviewed against the stricter target ZIP policy before reuse.
6. The frontend workflow must be substantially replaced even if its shell and components are reused.

## Step result

Step 0.1 is complete. The legacy source needed for baseline verification is present in the project ZIP, transport metadata has been removed, and the inventory identifies backend, frontend, database, tests, Docker dependencies and documentation.

## Next step

`0.2 — Bygg och testa legacybaslinjen`
