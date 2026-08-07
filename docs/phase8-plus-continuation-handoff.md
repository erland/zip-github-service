# Continuation handoff — phase 8 and later

Date: 7 August 2026  
Repository revision: r0088  
Application version: 1.0.0-rc.43  
Last completed implementation step: 8.3  
Next step: 9.1 — Define and persist the StagingImport lifecycle

## Why this file exists

This is the compact handoff for starting a new ChatGPT conversation from the complete project ZIP. It supplements, but does not replace, the authoritative status and implementation-step files.

A new chat should be able to start with this ZIP and continue from 9.1 without needing the prior conversation transcript.

## Read these first

Follow `AGENTS.md`. In practice, read in this order:

1. `docs/implementation-status.md` — authoritative NEXT/DONE/PENDING ledger.
2. `docs/implementation-steps.md` — exact scope and quality gate for 8.1 onward.
3. `docs/zip-github-development-plan-v1.1.md`.
4. `docs/zip-github-functional-specification-v1.2.md`.
5. `docs/architecture.md`, `docs/api-contract.md`, `docs/security-model.md` / `docs/threat-model.md` and `docs/operations.md` as relevant.
6. For phase 9 specifically, `docs/shortcut-stagingimport-design.md`.

Do not infer the next step from this handoff if it conflicts with `docs/implementation-status.md`; the status ledger wins.

## Current product behavior

The service currently provides the full phase-7 flow:

```text
GitHub App user login
→ choose/configure Project
→ one active Work per Project
→ upload ZIP
→ secure ingestion + inspection
→ exact repository snapshot/base SHA
→ comparison/policy
→ immutable plan
→ hierarchical selection + explicit overrides
→ immutable approval
→ exact temporary Git workspace
→ commit/push to Work branch
→ repeat with next ZIP as another Work commit
→ explicitly finish Work and create PR
```

Important UX/state behavior:

- At most one active import may exist in an open Work.
- Active import means the user must continue or cancel it before uploading another ZIP.
- A pending import can be cancelled before Git delivery.
- A pending import survives logout/login and backend restart because resume state is persisted.
- The primary Work history is Git commit history; old import rows remain audit/diagnostic data.
- After a successful commit the result page directly offers `Ladda upp nästa ZIP` or `Arbetet är klart – skapa pull request`.
- Pull-request creation is explicit and idempotent.

## Core security/consistency invariants that later phases must preserve

- GitHub user authorization establishes user identity; repository writes use short-lived GitHub App installation tokens server-side.
- Tokens/private keys are never sent to frontend and installation tokens are not stored permanently.
- Every user-owned resource is owner-checked server-side.
- ZIP bytes are never executed by zip-github.
- `.git/**` is hard blocked. Actual `.github/**` changes and deletions require explicit override; unchanged protected paths do not.
- ImportPlan is immutable.
- ApprovedSelection is separate, immutable and digest-bound to plan/base SHA.
- Delivery applies exactly the selected paths and verifies staged diff before commit.
- A Work branch update is rejected when the remote head no longer matches the approved expected base SHA.
- Normal Git push is non-force.
- Temporary Git workspaces/credentials are cleaned deterministically.
- Current deployment model does not support horizontal backend scaling; same-process synchronization is used for the single-active-import creation race.

## Persistence state

PostgreSQL currently persists Project identity/configuration, Work sessions and owner-bound import resume payloads. Flyway migrations through V8 are present.

Resume persistence includes enough source upload/snapshot/plan/selection/approval/Git-identity/delivery data to recover imports after backend restart. Temporary Git workspaces are deliberately not persisted; they are recreated and verified from persisted approved state.

## Phase 8 — current active roadmap

### 8.1 — Workflow runs and jobs — DONE

Implemented as an owner-scoped, read-only Actions view for the exact delivered commit, with bounded workflow runs/jobs/checks, normalized states, direct GitHub links, server-side cache and bounded frontend backoff. GitHub remains the canonical full UI. See `docs/workflow-runs-and-jobs.md` and `docs/step-8.1-report.md`.

Likely code areas:

- `backend/.../github/GitHubAppClient.java`
- `backend/.../github/GitHubCheckStatusClient.java`
- `backend/.../checks/ImportCheckStatusService.java`
- `backend/.../api/ProjectResource.java` and/or `ImportResource.java`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `frontend/src/pages/ImportResultPage.tsx`
- API types under `frontend/src/api/`

Do not implement 8.2/8.3 in the same step except for a tiny prerequisite abstraction.

### 8.2 — Artifacts and condensed errors — DONE

Implemented as an owner-scoped detail endpoint with bounded artifact metadata, GitHub-run links and conservative sanitized failed-job summaries. Artifact bytes and raw logs are not persisted or returned to the browser. See `docs/actions-artifacts-and-condensed-errors.md` and `docs/step-8.2-report.md`.

### 8.3 — Controlled workflow dispatch/rerun — DONE

Implemented default-deny dispatch/rerun allowlists, exact current-Work guards, persistent audit/idempotency and explicit mobile controls. See `docs/controlled-workflow-actions.md` and `docs/step-8.3-report.md`.

### Former 8.4

AI/read-only integration API, Custom GPT/MCP and AI branch ZIP export have been deliberately moved to the future backlog. Do not implement them as part of phase 8.

## Phase 9 — Shortcut / StagingImport

Phase 9 is fully decomposed into steps 9.1–9.7 in `docs/implementation-steps.md`. The governing architecture is `docs/shortcut-stagingimport-design.md`.

The critical convergence already exists:

- `ZipIngestionService`
- `StoredUploadArtifact`
- `ProjectApplicationService.createImportFromStoredUpload(...)`
- `ImportSource.STAGING_IMPORT`

The intended boundary is:

```text
Shortcut bytes
→ StagingImport transport/claim only
→ authenticated Project selection
→ existing stored-upload promotion
→ ordinary Import
→ existing review/delivery pipeline
```

Do not create an anonymous ordinary Import, and do not create a second staging-specific import pipeline.

## Suggested new-chat starting prompt

```text
Fortsätt utvecklingen av zip-github från den bifogade kompletta projekt-ZIP:en.

Börja med att läsa AGENTS.md, docs/implementation-status.md, docs/implementation-steps.md och docs/phase8-plus-continuation-handoff.md. Functional specification och development plan är styrande för produkt/arkitektur. Statusfilen är styrande för vilket steg som ska köras.

Genomför endast det steg som är markerat NEXT, vilket i denna revision ska vara 9.1 – Definiera och persistiera StagingImport-livscykeln. Fas 8 är färdig; implementera inte senare fas-9-steg i förtid annat än en liten nödvändig förberedande abstraktion.

Efter ändringen:
- kör relevanta backend/frontendtester och builds när miljön tillåter,
- kör repositoryns verifieringsskript,
- uppdatera dokumentation, implementation-status och nästa steg,
- lista uttryckligen alla tillagda/ändrade/flyttade/raderade filer,
- paketera hela repositoryt i en ny revisionsmärkt ZIP med zip-github/ som toppkatalog.
```

## Build / verification commands

Run from repository root where applicable:

```bash
./scripts/verify-implementation-status.sh
./scripts/verify-structure.sh
./scripts/security-regression.sh
./scripts/verify-source-tracking.sh
./scripts/verify-release.sh

cd backend
./mvnw test
# or the repository/CI Maven verify command where appropriate

cd ../frontend
npm ci
npm test -- --run
npm run build
```

Use the actual package scripts/workflow definitions in the repository if they differ; do not invent replacement commands without checking `package.json`, Maven config and `.github/workflows/`.

## Known verification/environment note

During the recent ChatGPT packaging sessions, Maven could not start because the execution environment could not DNS-resolve `repo.maven.apache.org`. That is an environment limitation, not a known application test failure. The user has repeatedly run full backend/frontend CI/local tests and reported regressions, which were corrected through RC36; later phase-7 work was guarded by repository/static checks plus targeted test additions. A new chat should attempt the full tests again rather than assume the DNS limitation still exists.

## Release/packaging discipline

- Application version is `1.0.0-rc.43`; r0088 is a corrective CI/build revision after step 8.3; phase 8 remains complete.
- The next implementation revision should increment repository revision and normally the RC application version when runtime code changes.
- Keep exactly one `NEXT` step in `docs/implementation-status.md`.
- Every delivered ZIP must include one top-level `zip-github/` folder.
- Every step report/final response must explicitly list files added, modified, moved and deleted, per `AGENTS.md`.

## Future decisions intentionally not reopened now

- AI/assistant integration API is backlog, not phase 8.
- Advanced three-way/provenance detection for ZIPs created from older repository state is backlog; ordinary ZIPs are not required to contain zip-github metadata.
- Horizontal backend scaling remains unsupported until shared/persistent coordination is designed for all required runtime locks/state.
