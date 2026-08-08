# Database model

Version 1.0  
Date: 6 August 2026  
Implementation step: 1.2

## Principles

- PostgreSQL is the system of record for application metadata; GitHub remains the system of record for project files and build history.
- Every user-owned row includes `owner_user_id`.
- Composite foreign keys include both resource id and `owner_user_id`. This prevents a child row from being attached to another user's project, import, plan, or delivery even if application authorization contains a defect.
- Domain statuses are stored as readable strings. Status transitions remain controlled by the domain/application layer rather than database updates from REST resources.
- Temporary archive bytes are not stored in PostgreSQL; `source_upload` stores metadata and a future `storage_key`.
- Timestamps use `TIMESTAMPTZ`.

## Tables

### `user_account`
GitHub identity, login metadata and last-login timestamp. GitHub numeric user id is unique.

### `github_installation`
A GitHub App installation associated with the user who selected/configured it. The installation id and owner form a composite candidate key used by projects.

### `project`
A user-owned configuration for one repository and target branch. GitHub installation/repository fields are either all configured or all absent. Project names are unique per user.

### `import_session`
Coordinates one import for one project. The composite project/owner foreign key enforces tenant isolation. Base SHA is nullable until frozen and then expected to be immutable in application code.

### `source_upload`
Temporary ZIP metadata, SHA-256, storage reference, retention deadline and lifecycle status.

### `import_plan` and `import_plan_entry`
Immutable comparison snapshot and normalized path entries. One active snapshot row is currently allowed per import session in the initial model. Paths are unique inside a plan.

### `github_delivery`
Idempotent branch/commit/pull-request delivery metadata. The idempotency key is unique per owner and a plan has at most one delivery row.

## Isolation constraints

The schema uses composite foreign keys for these ownership chains:

- `project(github_installation_id, owner_user_id)` → `github_installation(id, owner_user_id)`
- `import_session(project_id, owner_user_id)` → `project(id, owner_user_id)`
- `source_upload(import_session_id, owner_user_id)` → `import_session(id, owner_user_id)`
- `import_plan(import_session_id, owner_user_id)` → `import_session(id, owner_user_id)`
- `import_plan_entry(import_plan_id, owner_user_id)` → `import_plan(id, owner_user_id)`
- `github_delivery(import_session_id, owner_user_id)` → `import_session(id, owner_user_id)`
- `github_delivery(import_plan_id, owner_user_id)` → `import_plan(id, owner_user_id)`

Application queries must still scope every read and write by the authenticated user's id. Database constraints are defense in depth, not a substitute for authorization.

## Migration and tests

- `V1__clean_baseline.sql` establishes the clean migration baseline.
- `V2__target_domain.sql` creates the target-domain schema, constraints and indexes.
- `DatabaseMigrationTest` starts PostgreSQL 16 with Testcontainers, runs Flyway, and verifies that a cross-owner import-to-project link is rejected.


## Import source metadata (V7)

`import_session` contains `source_type` (`WEB_UPLOAD`, `STORED_UPLOAD`, `STAGING_IMPORT`) and nullable `source_reference`. Existing rows default to `WEB_UPLOAD`. `source_reference` is deliberately limited to non-secret correlation data and must never contain capability/claim tokens or credentials.


## Resumable import state (V8)

`import_resume_payload` stores the restart-critical serialized state for an `import_session`: upload metadata, locked repository snapshot, immutable plan, immutable selection, approval, Git identity and delivery metadata. Its `(import_session_id, owner_user_id)` foreign key is bound to the same owner on `import_session`. Temporary workspace paths are intentionally excluded and rebuilt after restart.


## Phase 9.8 lifecycle persistence

Migration `V13__work_lifecycle_and_project_archive.sql` adds `project.archived_at`, permits repeated historical use of the same branch name across completed Work sessions, and enforces at most one open (`PROVISIONING`/`ACTIVE`) Work per project.
