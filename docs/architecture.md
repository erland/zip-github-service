# MVP architecture

## Purpose

zip-github accepts a user-provided project ZIP, compares it with an authorized GitHub repository at an exact commit, presents an immutable review plan and delivers only the approved changes through a new branch and draft pull request.

GitHub remains the persistent source of truth. The service does not build arbitrary uploaded projects and does not mount the Docker socket. Target repositories use their own GitHub Actions workflows for compilation, tests and publishing.

## Components

### Browser frontend

The React/Vite frontend provides:

- GitHub login entry point;
- project and repository selection;
- mobile-friendly ZIP upload;
- immutable import-plan review and approval;
- delivery progress and result links;
- basic GitHub check status;
- import history and reopening.

Credentialed requests use the server session cookie. State-changing requests also carry the required request marker and are protected by exact-origin checks.

### Quarkus backend

The Java 21 backend owns:

- OAuth callback and server-side session handling;
- GitHub App installation-token creation;
- authorization and user ownership checks;
- source-neutral ZIP ingestion/storage plus user-owned upload orchestration and retention;
- ZIP inspection, normalization and hashing;
- frozen repository snapshots;
- comparison, policy and immutable plan creation;
- exact approval records;
- isolated Git workspace preparation;
- branch, commit, push and draft-PR delivery;
- idempotency, retry classification and result metadata;
- check-status aggregation and import history.

ZIP byte ingestion is deliberately separated from ownership: `ZipIngestionService` produces a neutral `StoredUploadArtifact` under an opaque storage scope, while `StreamingUploadService` attaches that artifact to the authenticated browser import. This keeps size/hash/storage safety reusable for future staging ingestion without making storage paths an authorization boundary.

### PostgreSQL

Flyway migrations define the intended persistent data model. The MVP release candidate still has several application services backed by in-memory stores. Until those stores are replaced, a backend restart loses those runtime records and horizontal scaling is unsupported.

### GitHub

Two separate GitHub integrations are used:

- OAuth identifies the human user and confirms their repository access.
- A GitHub App provides short-lived installation tokens for repository operations.

The service never exposes installation tokens to the frontend and does not embed them in Git remote URLs.

## Main trust boundaries

1. Browser to backend API.
2. Backend to local upload and workspace storage.
3. Backend to PostgreSQL.
4. Backend to GitHub OAuth and GitHub App APIs.
5. GitHub repository to repository-owned GitHub Actions.

The detailed threat model is documented in `threat-model.md`.

## Import data flow

1. User signs in with GitHub.
2. User selects an authorized repository and base branch.
3. User creates an import and uploads a ZIP.
4. Backend validates and inventories the archive.
5. Backend freezes the selected branch to an exact commit SHA.
6. ZIP and repository inventories are compared by SHA-256.
7. Policy produces a deterministic immutable import plan.
8. User approves the exact plan digest.
9. Backend prepares an isolated Git workspace from the frozen SHA.
10. Only approved added or modified files are applied and verified.
11. Backend creates one branch and one commit and pushes without force.
12. Backend creates or reuses a draft pull request.
13. Frontend displays permanent GitHub links and basic check status.

## Deployment shape

The documented MVP deployment uses:

- one frontend container;
- one backend container;
- one PostgreSQL instance;
- persistent upload/workspace volumes where required;
- reverse-proxy TLS termination;
- no Docker socket mount.

See `operations.md` and `configuration-reference.md`.

## Work-branch delivery model (RC15)

The normal delivery unit is a project work session rather than an individual import branch. The first import starts a durable work session from the configured project default branch. A successful import pushes one commit to the work branch. Later imports snapshot the current work-branch HEAD, preserving the existing immutable-plan/base-SHA safety check while allowing a sequence of small commits. Pull-request creation is an explicit project-level finalization action. Work-session metadata is stored in PostgreSQL so the branch can be continued after backend redeployment.


## Flexible review security boundary

The immutable `ImportPlan` describes the complete normalized ZIP-versus-branch comparison. It is never rewritten to represent UI choices. The user's commit intent is stored separately as an immutable `ApprovedSelection`.

`ApprovedSelection` binds the owner, import, plan digest, exact base commit SHA, selected paths, excluded paths and explicit per-path override audit records into `selectionDigestSha256`. Plan approval binds both the plan digest and selection digest.

Policy blockers have two delivery classes:

- `HARD_BLOCKED`: never selectable. `.git/**` belongs here and archive bytes for these paths can never be applied to the Git workspace.
- `OVERRIDABLE_BLOCKED`: excluded by default and selectable only after explicit risk acknowledgement. `.github/**` changes and `WOULD_DELETE` currently belong here.

The workspace starts from the exact reviewed commit, applies only selected archive files and explicitly selected deletions, then compares the complete Git diff path set and file hashes with the immutable selection. Delivery rechecks the current remote work-branch/base SHA before committing and pushes without force. This creates the security chain:

`ZIP SHA → immutable plan digest → immutable selection digest → approval → exact workspace diff → commit`.

### Stored-upload promotion boundary (RC25)

A source channel that has already used `ZipIngestionService` can convert its neutral `StoredUploadArtifact` into the ordinary user-owned import model through `ProjectApplicationService.createImportFromStoredUpload(...)`. Promotion attaches the existing physical artifact rather than copying or re-uploading bytes, is idempotent per owner/project promotion key, and converges immediately on the same downstream import pipeline as a browser upload. No alternate comparison/policy/delivery pipeline is introduced.


## Import-source convergence

All ingestion channels must converge to the ordinary user-owned import model before inventory/comparison. `ImportSource` records how the ZIP arrived (`WEB_UPLOAD`, `STORED_UPLOAD`, future `STAGING_IMPORT`) as non-secret audit metadata only. Source classification is intentionally outside policy, selection and Git delivery semantics.


## Alternative ingestion convergence (step 7.14)

Browser uploads and future staging/Shortcut uploads must converge before archive inspection. `ZipIngestionService` is the single controlled byte-ingestion boundary. A future staging flow stores bytes as a neutral `StoredUploadArtifact`, then—only after normal user/project authorization—calls `ProjectApplicationService.createImportFromStoredUpload(...)`. From that point onward the ordinary `Import` and existing inventory, snapshot, comparison, policy, immutable plan, selection, workspace and delivery services are the only pipeline. No staging-specific policy or Git path is allowed.

`StoredUploadArtifact` is an internal trusted hand-off object, not an external API DTO. Client-supplied path/hash/size values must never be used to fabricate one. Import-source audit metadata is deliberately outside plan and selection digest semantics.

## Automatic upload-to-review orchestration (step 7.16)

A successful browser ZIP upload now immediately calls `POST /api/imports/{importId}/prepare-review`. This is an orchestration layer only: archive inventory, repository snapshot, comparison, policy and immutable plan creation continue to use the existing services and security boundaries.

Preparation is restart-safe at the application-contract level. If an immutable plan already exists, it is returned unchanged. If only the repository snapshot exists, that exact locked SHA is reused and the remaining deterministic pipeline continues from it. A UI retry therefore never intentionally resolves a fresh branch head after the user already has a frozen snapshot. The standalone snapshot/plan endpoints remain available for diagnostics, while the normal interaction becomes `upload -> processing -> review`.



## Restart-safe import resume

Import review state is durable. `ProjectApplicationService` lazily hydrates owner-scoped import state from PostgreSQL when an import is reopened after a new login/session or backend restart. Physical ZIP files remain on the upload volume; immutable snapshot/plan/selection/approval data is restored from PostgreSQL. Ephemeral Git workspaces are recreated and reverified before retrying delivery.


## Git-centric Work history (step 7.20)

The active Work branch is the primary user-facing history. The backend reads branch commits through the GitHub App installation credential and exposes only compact commit metadata needed by the project page. Full import history remains owner-scoped application/audit data and is not treated as the canonical development timeline. If GitHub commit history is temporarily unavailable, the persisted Work head provides a degraded local fallback while retaining direct GitHub navigation.


## Phase 7 resume/Work regression closure

The phase-7 closure regression treats PostgreSQL resume state as authoritative after JVM loss. Temporary Git workspaces remain disposable and are rebuilt from persisted upload/snapshot/plan/selection/approval state. The primary project history is the Work branch commit history; technical import history remains owner-scoped audit data. If GitHub commit-history reads fail, persisted Work head metadata provides a degraded read-only fallback.
