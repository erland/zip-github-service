> **Current handoff r0127 / 1.0.0-rc.79:** Phase 9 remains complete. Step 9.17 behavior is unchanged; rc.79 corrects the final frontend CI expectation so the PR-description helper is verified in chronological Work-commit order (oldest to newest).

> **Current handoff r0124 / 1.0.0-rc.76:** Phase 9 remains complete. This revision is a CI-only correction after step 9.16: the backend imports the external-branch DTO correctly and frontend regressions match the intended PR action label and mock the new external-branch review API. Step 9.16 production behavior is unchanged.

> **Planning refinement r0105 / 1.0.0-rc.57:** Phase 9.7 now explicitly requires the authenticated Shortcut download to expose `Skicka till zip-github.shortcut` via `Content-Disposition` and requires deployment verification that the backend runtime user can read the signed bind-mounted artifact (avoiding the observed `0600` failure). The same checks are included in the final 9.10 E2E gate. No 9.8 implementation has started.

> **Signed Shortcut integration r0103 / 1.0.0-rc.55:** The operator-provided Apple-signed reference Shortcut is now included in the deployment bundle at `shortcut/releases/zip-github.shortcut` (version 1 / g1, SHA-256 `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`). The file is gitignored; import review evaluates repository `.gitignore` rules generically and excludes matching untracked files from delivery without a Shortcut-specific hard block. Step 9.7 is now blocked only on deploying this bundle, downloading the artifact through authenticated `/shortcut`, and confirming that exact served copy installs on iOS.

> **rc.76 correction:** GitHub CI for rc.75 exposed one backend compile regression (missing `ExternalBranchChangesResponse` import) and three frontend test regressions (two stale PR-action labels plus one missing `getExternalBranchChanges` mock). All are corrected without changing 9.16 production behavior.

# Continuation handoff — phase 8 and later

> **CI correction r0102 / 1.0.0-rc.54:** The container-image job now downloads the already verified `backend-quarkus-app` and `frontend-dist` artifacts from its prerequisite jobs and assembles runtime-only images. It no longer reruns Maven/npm inside Docker, avoiding the independent Maven Central path that returned HTTP 429. Phase 9 remains blocked at 9.7 only on the external Apple signing/iOS installation gate.


Date: 9 August 2026  
> **rc.71 correction:** Frontend CI exposed a test-only race in the repository-first list regression. The test now waits for the asynchronous repository API result before asserting/filtering. No production behavior changed.

> **rc.72 correction:** The next frontend CI run passed all 51 Vitest tests but `tsc -b` exposed a stale pre-9.13 staging API signature. `promoteStagingImport` now accepts and serializes either an existing Project target or a repository target for lazy bootstrap. Backend behavior is unchanged.

Repository revision: r0120  
Application version: 1.0.0-rc.72  
Last completed implementation step: 9.13  
Current position: implementation plan complete; no NEXT step remains

## Why this file exists

This is the compact handoff for starting a new ChatGPT conversation from the complete project ZIP. It supplements, but does not replace, the authoritative status and implementation-step files.

A new chat should be able to start with this ZIP and resume the blocked 9.7 signing/install gate without needing the prior conversation transcript.

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
→ choose repository from GitHub App access
→ lazy internal Project bootstrap when work begins
→ one active Work per internal Project
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

PostgreSQL currently persists Project identity/configuration, Work sessions and owner-bound import resume payloads. Flyway migrations through V12 are present.

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

Phase 9 is fully decomposed into steps 9.1–9.8 in `docs/implementation-steps.md`. The governing architecture is `docs/shortcut-stagingimport-design.md`.

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

Phase 9 also owns Git file-mode preservation discovered during the phase-8 CI corrections. In particular, `100755` executable state must not be lost merely because a ZIP/import path lacks portable Unix metadata. Step 9.1 must establish a representation that can carry trustworthy ZIP mode metadata; step 9.4 must merge it with base-repository mode deterministically and include mode changes in ordinary review/approval/delivery; step 9.8 must regress the behavior. Never infer executable state from `.sh`, `mvnw` or other names. New files without trustworthy mode metadata default to `100644`; existing files without it preserve the base snapshot mode.


Phase 9 Shortcut distribution is now locked to a static pre-signed reference Shortcut for the first version. The Shortcut carries a deployment-scoped, low-privilege staging upload credential in a dedicated header; it is not user identity and grants no claim/Project/GitHub access. Runtime per-user Shortcut generation and GitHub-hosted automated signing are not required. A practical macOS Actions spike showed `shortcuts sign` requires an iCloud-signed-in environment. Credential rotation is therefore immediate revoke + publish/sign a replacement Shortcut; old installations get an update-required error. See `docs/planning-revision-r0092-shortcut-distribution.md`.

### 9.2 — Capability-protected staging upload — DONE

Implemented in r0094 as an exact `POST /api/staging-imports` transport route protected by `X-ZipGitHub-Upload-Credential`. It reuses `ZipIngestionService`, creates only `AVAILABLE` staging state, returns a one-time 256-bit claim token/fragment URL and exposes no anonymous read/list route. The capability is not user identity. See `docs/staging-upload.md` and `docs/step-9.2-report.md`.

### 9.3 — Authenticated browser claim — DONE

Implemented in r0095. `/staging/claim#token=...` captures the one-time token into same-tab `sessionStorage`, clears the fragment, survives ordinary GitHub OAuth via `returnTo=/staging/claim`, then performs an authenticated+CSRF-protected claim. Backend hashes the token, locks the matching row, binds exactly one owner and returns one neutral 410 for invalid/expired/taken states. Same-owner retry is idempotent. No Project selection, ordinary Import or GitHub side effect occurs yet. See `docs/staging-claim.md` and `docs/step-9.3-report.md`.


### 9.4 — Project selection and promotion to ordinary Import — DONE

Implemented in r0096. After authenticated claim, the user chooses one of their active Projects and the already stored staging artifact is promoted through the existing `createImportFromStoredUpload(...)` path with `ImportSource.STAGING_IMPORT`. Promotion is restart-safe through the persisted non-secret `staging-import:<id>` source reference and converges on exactly one ordinary Import without ZIP copy/re-stream. Existing `ACTIVE_IMPORT_EXISTS`, Project ownership and Work invariants remain authoritative.

The common browser/StagingImport pipeline now also preserves Git ordinary-file modes. Trustworthy ZIP `100644`/`100755` metadata wins; existing paths without it preserve the exact base-repository mode; new paths default to `100644`. Mode-only changes are `MODIFIED`, included in the immutable plan digest/approval, applied only to selected paths and verified in the staged Git index. Filename-based executable inference remains forbidden. See `docs/staging-promotion.md` and `docs/step-9.4-report.md`.

### 9.5 — User-controlled commit message — DONE

Implemented in r0097. The common browser/StagingImport review path now shows the former generated message as an editable suggestion. Interactive approval requires a server-normalized non-empty message (max 500 characters), persists it in restart-safe approval state and binds it to plan + selection + owner. Delivery receives the exact approval-bound value; refresh/restart/retry never regenerates it. Old persisted/internal approval data without the field uses only the documented deterministic legacy fallback. See `docs/user-controlled-commit-message.md` and `docs/step-9.5-report.md`. Final cross-source/idempotency regression still belongs to 9.8.

### 9.6 — Staging retention, abuse protection and security regression — DONE

Implemented in r0098. AVAILABLE and CLAIMED now have separate configurable short deadlines; cleanup is scheduled, batched and restart-safe through `artifact_deleted_at`. Promotion and cleanup serialize on the same PostgreSQL row lock, and a crash-window ordinary Import is reconciled through `staging-import:<id>` before cleanup may delete bytes. The original upload retention deadline is persisted independently so promotion transfers the artifact to ordinary Import retention without shortening it. Deployment-level live-object/live-byte quotas are transactionally serialized, rate limiting remains per capability/global with optional trusted-proxy network limiting, and upload credential rotation is documented as secret replacement + redeploy with no database migration or current/previous grace. Existing claim tokens/staging rows remain independent of credential rotation. See `docs/staging-retention-and-abuse.md` and `docs/step-9.6-report.md`.

## Suggested new-chat starting prompt

```text
Fortsätt utvecklingen av zip-github från den bifogade kompletta projekt-ZIP:en.

Börja med att läsa AGENTS.md, docs/implementation-status.md, docs/implementation-steps.md och docs/phase8-plus-continuation-handoff.md. Functional specification och development plan är styrande för produkt/arkitektur. Statusfilen är styrande för vilket steg som ska köras.

Genomför endast det steg som är markerat NEXT, vilket i denna revision ska vara 9.7 – Distribuera en signerad referens-Shortcut för iOS. Fas 8 är färdig; implementera inte senare fas-9-steg i förtid annat än en liten nödvändig förberedande abstraktion.

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

- Application version is `1.0.0-rc.67`; repository revision is r0115. Phase 9 is complete; r0115 is the frontend CI hardening correction for the shared ActionsPanel while retaining the rc.65 200M upload fix and rc.66 Actions UX/diagnostics work.
- Steps 9.1–9.9 are complete. The signed Shortcut is device-verified, Work lifecycle uses verified GitHub branches, and Actions status/failures are revisitable from the active Work view. Step 9.10 is NEXT.
- Keep exactly one `NEXT` step in `docs/implementation-status.md`.
- Every delivered ZIP must include one top-level `zip-github/` folder.
- Every step report/final response must explicitly list files added, modified, moved and deleted, per `AGENTS.md`.

## Future decisions intentionally not reopened now

- AI/assistant integration API is backlog, not phase 8.
- Advanced three-way/provenance detection for ZIPs created from older repository state is backlog; ordinary ZIPs are not required to contain zip-github metadata.
- Horizontal backend scaling remains unsupported until shared/persistent coordination is designed for all required runtime locks/state.

## Phase 9 current position — r0115

The zip-github runtime side of signed Shortcut distribution is implemented and r0103 now carries the operator-provided signed `.shortcut` in the deployment bundle. The binary embeds the deployment staging credential, so it remains gitignored. Since 9.12, ordinary Import review applies the repository `.gitignore` generically and marks the untracked artifact ignored/non-selectable instead of using a Shortcut-specific hard block.

9.7 is **DONE**. The operator-provided Apple-signed artifact is published as `shortcut/releases/zip-github.shortcut`; the authenticated `/shortcut` download was exercised and the downloaded copy imported on iPhone. The technical server filename is decoupled from the user-facing download filename, and the standard deployment mode is runtime-readable. Steps 9.8 and 9.9 are DONE; 9.10 is NEXT.


## r0104 planning refinement

Remaining phase 9 is now explicitly 9.8 Work/project lifecycle + robust remote branch provisioning, 9.9 revisitable Actions status/copyable condensed failures on Work page, and 9.10 final E2E/release regression. Step 9.7 is DONE after deployed `/shortcut` download/import verification. The signed Shortcut binary remains deployment-only; clean Git CI validates `shortcut/releases/release-manifest.txt` and verifies exact bytes only when the binary is present.


## Phase 9.9 completed in r0109 / rc.60

The active Work view now exposes exact-head GitHub Actions status/details using the existing phase-8 integration, including explicit refresh, bounded active polling, GitHub run links and copyable sanitized failure excerpts. Work status is keyed through `lastImportId` plus `headCommitSha`, never branch-only lookup. Step 9.10 is NEXT.


## Phase 9 completion — r0109 / rc.61

Step 9.10 completed the final cross-step regression/release gate. Phase 9 is complete and the active implementation ledger has no NEXT step. The signed Shortcut has real iPhone download/import evidence; Work provisioning is remote-verified before ACTIVE; delivery fails closed on missing/stale Work branches; Actions diagnostics are revisitable and commit-bound. Future AI/integration items remain a separate backlog, not an automatic continuation step.


### r0113 container upload-limit correction

The frontend nginx container now renders `client_max_body_size` from `ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE` (default `200M`). This removes nginx's implicit 1 MB request-body ceiling. The backend compressed upload default is aligned to 200 MiB and remains the authoritative byte-level limit.

### r0112 Actions visibility correction
Work Actions status now preserves successfully fetched workflow runs/jobs if the separate commit check-runs request fails (for example due to Checks permission drift or a transient GitHub error). This was verified against the shape of got-test-repo run `31258714926` at commit `f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69`.
