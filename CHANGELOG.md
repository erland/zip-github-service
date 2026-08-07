## Planning revision r0084 - 2026-08-07

- Moved former step 8.4 (AI/read-only API, Custom GPT/MCP and branch ZIP export) out of the active execution plan into a future backlog.
- Expanded phase 8 steps 8.1–8.3 with explicit workflow/job, artifact/error-extraction and controlled dispatch/rerun quality requirements.
- Added phase 9 steps 9.1–9.7 for capability-protected iOS Shortcut staging upload, authenticated claim, project promotion through the existing stored-upload path, retention/abuse protection, reference Shortcut guidance and E2E regression.
- Added a self-contained phase 8+ continuation handoff for starting a new chat without reconstructing architecture, status or known constraints.
- Application version remains 1.0.0-rc.39 because this revision changes planning/documentation only.

## 1.0.0-rc.39 - 2026-08-07

- Completed step 7.24 with final cancel/Work-lifecycle regression coverage.
- Added restart-safe cancellation regression proving no Git delivery is created and audit metadata survives terminal cancellation.
- Added project-view regression proving cancellation releases exactly one next-ZIP action.
- Added direct post-commit pull-request retry regression and strengthened the pull-request self-test for lost-response recovery without duplicate PR creation.
- Phase 7 quality gate is complete; step 8.1 is next.

## 1.0.0-rc.38 - 2026-08-07

- Completed step 7.23 with state-based Work actions.
- Enforced at most one active import per Work/project server-side with `409 ACTIVE_IMPORT_EXISTS`; cancelling the active import releases the slot.
- Removed the redundant `Fortsätt arbete` path: active import means continue/cancel, otherwise an open Work exposes one `Ladda upp nästa ZIP` action.
- Added direct `Arbetet är klart – skapa pull request` on the post-commit result page using the existing idempotent Work PR operation.

## Planning revision r0080 - 2026-08-07

- Reopened phase 7 with steps 7.22–7.24 for explicit import cancellation, state-based Work actions and final regression.
- Defined the invariant that a Work may have at most one active import; a new ZIP is blocked until the active import is committed or cancelled.
- Removed the planned redundant `Fortsätt arbete` path in favor of state-specific actions.
- Added direct post-commit actions for `Ladda upp nästa ZIP` and `Arbetet är klart – skapa pull request`.
- Application version remains 1.0.0-rc.36 because this revision changes planning/status only.

## 1.0.0-rc.36 - 2026-08-07

- Corrected two backend promotion/regression tests to match step 7.19 retention semantics: active resumable imports are intentionally excluded from expired-upload cleanup until terminal delivery.
- Updated the authenticated app-routing test to mock the Work commit-history endpoint introduced in step 7.20.
- Made the streamlined import E2E regression await the loaded review tree instead of using the review heading as a readiness signal.
- No intended production behavior change; phase 8 step 8.1 remains NEXT.

## 1.0.0-rc.35 - 2026-08-07

- Corrected pure unit-test compatibility after restart-safe import persistence: `ProjectApplicationService` now treats `ImportResumePersistenceStore` as optional when the service is manually constructed outside CDI.
- Corrected `ImportReviewPage.test.tsx` fixture typing by using the exported `ImportSelectionResponse` type, preventing empty arrays from being inferred as `never[]`.
- No intended production behavior change; phase 8 step 8.1 remains NEXT.

# 1.0.0-rc.33 — 2026-08-07

Completed step 7.20: Git-centric Work view with one resumable active import.

### Added

- Added owner-scoped `GET /api/projects/{projectId}/work/commits` backed by a short-lived GitHub App installation token.
- Added Work commit rows with SHA, first-line commit message, author, timestamp and direct GitHub commit link.
- Added a degraded fallback that shows the latest locally persisted Work head when GitHub commit history is temporarily unavailable.

### Changed

- Removed the full technical import-history list from the primary project view; import history remains available from the existing backend API for audit and diagnostics.
- The project page now shows at most one non-terminal import as a dedicated resumable task.
- Pull-request completion is disabled while an import is still active.
- Step 7.21 is now NEXT.

# 1.0.0-rc.32 — 2026-08-07

Completed step 7.19: restart-safe resumable imports.

### Added

- Added PostgreSQL-backed resume payload state for source upload, repository snapshot, immutable plan, selection, approval, Git identity and completed delivery.
- Added lazy owner-scoped hydration so review/delivery state survives logout/login and backend restart without re-upload.
- Added Flyway V8 owner-bound resume storage and migration regression coverage.

### Changed

- Active, not-yet-delivered imports are no longer source-ZIP cleanup candidates solely because their original retention deadline passed.
- Temporary Git workspaces remain ephemeral and are safely recreated from persisted approved state after restart.
- Step 7.20 is now NEXT.

# Planning update r0074 — 2026-08-07

Extended phase 7 with restart-safe import resume and a Git-centric Work view. Application version remains `1.0.0-rc.31` because this revision changes planning/status documentation only.

### Planned

- Step 7.19: resume an uploaded/reviewable import after logout/login or backend restart without re-uploading the ZIP.
- Step 7.20: make Work-branch Git commit history the primary user-facing history and show at most one active import as a resumable task.
- Step 7.21: final E2E/security regression for resume persistence, Work UI and ownership isolation.
- Phase 8 step 8.1 is deferred until 7.19–7.21 are complete.

# 1.0.0-rc.31 — 2026-08-07

Completed step 7.18: end-to-end regression for the streamlined import flow.

### Added

- Added a cross-page frontend regression covering `upload -> automatic review -> selection/override -> one-click commit/result` in one router flow.
- Added explicit slow-plan-build coverage proving preparation cannot be triggered twice while the automatic preparation is pending.
- Added post-approval delivery failure/retry coverage proving immutable selection and approval are not recreated when push is retried.

### Verified

- Custom author choice survives the streamlined upload path.
- Unchanged `.github/**` workflow entries expose no override control, while an actually modified workflow still requires explicit override.
- Partial selection and explicit override reach the immutable selection API exactly once.
- Successful delivery targets an active `zip-github/work-*` branch and opens the result without a separate commit step.
- Phase 7 quality gate is complete; step 8.1 is now NEXT.

# 1.0.0-rc.30 — 2026-08-07

- Step 7.17: review approval and Git delivery are now one normal user action.
- The review page performs immutable selection, persistent approval, verified workspace, commit and push before opening the result page.
- Added owner-scoped approval readback so a refresh can restore a previously approved selection and offer only safe delivery retry.
- Reopening review after an already recorded delivery redirects to the result page rather than risking a duplicate commit.

# 1.0.0-rc.29 — 2026-08-07

Completed step 7.16: automatic upload-to-review preparation.

### Added

- Added idempotent `POST /api/imports/{importId}/prepare-review` orchestration that reuses an existing immutable plan or locked repository snapshot before doing any new work.
- Added frontend regression coverage for automatic upload -> preparation -> review navigation and retry without re-upload.
- Added backend retry coverage proving an already locked immutable plan is returned unchanged.

### Changed

- Successful ZIP upload now immediately starts archive/repository preparation and navigates to the review view when the immutable plan is ready.
- Removed the normal separate “Skapa granskningsplan” user step; a retry action is shown only when preparation fails after upload.
- The uploaded ZIP/file input is locked after successful upload so recovery cannot accidentally attempt a second upload to the same import.
- Step 7.17 is now the single `NEXT` step.

# 1.0.0-rc.28 — 2026-08-07

Completed step 7.15: unchanged protected-path policy semantics.

### Changed

- Bumped import policy identity to `mvp-3`.
- `.github/**` now requires explicit override only for actual repository changes: `ADDED`, `MODIFIED` or `WOULD_DELETE`.
- `UNCHANGED` workflow/protected entries remain unchanged and require no change override.
- Preserved archive/content hard-safety rules that are intentionally independent of repository diff status.
- Added regression for unchanged, modified, added and deleted workflow files.
- Step 7.16 is now the single `NEXT` step.

# 1.0.0-rc.27 — 2026-08-07

Completed step 7.14: alternative ZIP-ingestion regression.

### Added

- Added end-to-end regression coverage showing browser upload and an already stored ZIP converge on equivalent inventory, comparison, policy and immutable plan entries for identical bytes/base content.
- Added shared size-boundary coverage proving both paths use the same source-neutral `ZipIngestionService` limit.
- Added promotion retry, ownership, retention/cleanup and plan/selection digest-stability regression assertions.
- Documented the exact future `StagingImport`/iOS Shortcut convergence point.

### Changed

- Test-state reset now clears import-source audit metadata as well as the other in-memory import state.
- Step 7.15 is now the single `NEXT` step.

# 1.0.0-rc.26 — 2026-08-07

Completed step 7.13: explicit import-source and audit metadata.

### Added

- Added `ImportSource` (`WEB_UPLOAD`, `STORED_UPLOAD`, reserved `STAGING_IMPORT`) and bounded non-secret `ImportAuditMetadata`.
- Added Flyway V7 source columns on `import_session` and corresponding entity fields.
- Added import-history source metadata and human-readable source labels in the project history UI.
- Added regression tests for browser/stored source classification and safe source-reference handling.

### Changed

- Stored-upload promotion now records an internal `stored-upload:<artifact UUID>` correlation reference without copying any secret token material.
- Step 7.14 is now the single `NEXT` step.

# 1.0.0-rc.25 — 2026-08-07

Completed step 7.12: create a normal Import from an already stored ZIP.

### Added

- Added an internal stored-upload promotion operation that attaches an existing neutral `StoredUploadArtifact` to the ordinary user-owned import model without re-uploading or copying the ZIP bytes.
- Added owner/project-scoped idempotency for promotion retries and duplicate-artifact protection.
- Added regression tests for same-request retries, idempotency-key misuse and duplicate artifact promotion.

### Changed

- Stored artifacts can now converge on the existing normal import pipeline before inventory/comparison/policy rather than requiring an HTTP upload path.
- Step 7.13 is now the single `NEXT` step.

# 1.0.0-rc.24 — 2026-08-07

Completed step 7.11: reusable, source-neutral ZIP ingestion and storage.

### Added

- Added `ZipIngestionService` as the single source-neutral implementation of filename validation, declared/actual compressed-size enforcement, streaming storage, SHA-256 calculation and retention metadata.
- Added `StoredUploadArtifact`, which deliberately contains no user ID or import ID and can therefore be attached to a normal import or a future staging import without fake identity data.
- Added focused ingestion tests proving neutral storage, checksum/retention equivalence, preflight size rejection, streaming-limit cleanup and filename safety.

### Changed

- `StreamingUploadService` is now a thin authenticated-web-import adapter that calls the neutral ingestion service and attaches the artifact to the existing `StoredUpload` ownership model.
- `UploadStorage` now receives an opaque storage scope UUID rather than user/import semantics. Normal web uploads use the import UUID as that storage scope.
- Upload cleanup now removes only the empty opaque scope directory and never attempts to remove the configured storage root.
- The public upload endpoint and returned metadata remain unchanged.
- Step 7.12 is now the single `NEXT` step.

# Planning revision r0065 — 2026-08-07

- Extended phase 7 with steps 7.11–7.14 for reusable ZIP ingestion, creation of a normal Import from an already stored ZIP, import-source audit metadata and alternative-ingestion regression.
- Added step 7.15 so unchanged `.github/**` entries never require a change override; only actual added/modified/deleted protected paths do.
- Added step 7.16 to automatically continue from successful ZIP upload to immutable plan/review without a separate “Skapa granskningsplan” action.
- Added step 7.17 so “Godkänn valda förändringar” is the single normal user action that records approval and continues directly to exact commit/push.
- Added step 7.18 for E2E regression of the streamlined flow.
- Added functional specification v1.2 and made it authoritative. Application version remains `1.0.0-rc.23`; this revision changes planning/specification only.

# 1.0.0-rc.23 — 2026-08-07

- Correct backend `ImportSelectionResourceTest` list assertions to use an unambiguous Hamcrest matcher accepted by RestAssured.
- Correct review-page tests for the hierarchical tree: file nodes render the basename as visible text and retain the complete repository path in `title`/accessible controls.
- No production behavior changes from RC22.

# 1.0.0-rc.22 — 2026-08-07

Completed step 7.10: selection, override and security regression.

### Added

- Expanded backend selection regressions for hard-block bypass attempts, audited `.github/**` overrides, explicit deletion approval and stale plan/base identity.
- Expanded the real-Git workspace self-test to a mixed selection containing selected ordinary changes, an explicitly overridden workflow change, an explicitly approved deletion, excluded ordinary changes and a hard-blocked `.git/**` archive entry.
- Added exact Git diff assertions proving excluded paths remain untouched and `.git/**` content never reaches repository metadata.
- Added a stale work-branch delivery regression proving delivery stops when the reviewed base moves before push.
- Added frontend mixed-tree regressions for directory partial selection, hard blockers, workflow/deletion overrides, empty selections and exact selection API payloads.

### Security/documentation

- Updated the threat model for immutable selections, audited overrides and hard-vs-overridable blocker boundaries.
- Extended the security regression script with selection/delivery invariants.
- Updated architecture, API contract and release checklist for the flexible-review security model.
- Phase 7 flexible review is complete; step 8.1 is now the single `NEXT` step.

# 1.0.0-rc.21 — 2026-08-07

Implemented step 7.9 of flexible review.

### Added

- Connected the hierarchical review tree to immutable selection creation and exact approval.
- Added explicit per-path risk acknowledgement for `OVERRIDABLE_BLOCKED` entries such as `.github/**` changes and `WOULD_DELETE`.
- Bound plan approval to both the immutable plan digest and immutable selection digest.
- Applied only selected paths to the temporary Git workspace, including explicitly approved deletions.
- Added defense-in-depth verification that hard blockers never reach the workspace and overridable blockers always carry matching override audit records.
- Bound prepared workspaces to the selection digest and verified the complete Git diff against exactly the selected path set before commit.

### Changed

- Partial selections are now fully supported; the temporary RC20 partial-selection guard has been removed.
- Review controls lock as soon as an immutable selection has been created, preventing UI changes after selection identity is fixed.
- A blocker-only plan can be approved when it contains a non-empty valid selection of explicitly overridden entries.

# 1.0.0-rc.20 — 2026-08-07

Implemented step 7.8 of flexible review.

### Added

- Replaced the flat review file list with a collapsible hierarchical directory/file tree.
- Added tri-state directory selection where directory toggles affect every selectable descendant and child changes propagate `checked`/`indeterminate` state upward.
- Added per-directory aggregate counts for new, modified, deleted, blocked and warning entries.
- Added explicit file status/badge rendering for added, modified, would-delete, ignored and blocker classes.
- Added responsive/mobile tree layout, long-path wrapping, keyboard-focusable disclosure controls and checkbox labels for assistive technology.
- Added frontend tests for default selection, directory subtree deselection, indeterminate parents, collapse behavior, deletion/blocker labels and partial-selection safety.

### Safety boundary

- Ordinary `ADDED`/`MODIFIED` entries are selected by default. Hard and overridable blockers remain unselected/disabled in this step.
- Until step 7.9 connects immutable selections to exact workspace/delivery, changing the default selection disables the legacy whole-plan approval path. This prevents a user-visible deselection from being ignored by the current delivery implementation.

# 1.0.0-rc.19 — 2026-08-07

Implemented step 7.7 of flexible review.

### Added

- Added immutable `ApprovedSelection` and `ApprovedSelectionOverride` domain records bound to one import plan, base commit and owner.
- Added deterministic `selection-1` digest generation over selected paths, excluded paths and explicit override audit records.
- Added `POST /api/imports/{importId}/selection` and `GET /api/imports/{importId}/selection`.
- Added validation for stale plan/base identity, unknown or duplicate paths, empty selections, hard blockers, invalid overrides and cross-user access.
- Added domain/API tests for deterministic selection identity, immutable replay, stale plans and owner isolation.

### Scope

- Selection is stored server-side and immutable, but does not yet drive workspace/delivery. Hierarchical selection UI is step 7.8 and exact selected delivery is step 7.9.

# 1.0.0-rc.18 — 2026-08-07

Implemented step 7.6 of flexible review.

### Changed

- Added explicit `HARD_BLOCKED` and `OVERRIDABLE_BLOCKED` policy taxonomy (`mvp-2`).
- `.git/**`, oversized files and high-risk key/credential filenames are hard blocked and cannot be selected.
- `.github/**` and repository deletions are overridable blockers, excluded by default pending the selection/override steps.
- Mixed ZIPs with ordinary safe changes can continue even when blocked paths are present; blocker-only plans remain non-approvable to avoid empty commits.
- Policy/plan API and review UI now expose hard/overridable blocker counts and per-entry blocker type.
- Blocker type is included in the immutable plan digest.

# r0058 — flexible review planning/specification update

- Added implementation steps 7.6–7.10 before phase 8 for blocker taxonomy, immutable selection, hierarchical tree selection, explicit overrides and exact selected delivery.
- Added functional specification v1.1 with `HARD_BLOCKED`/`OVERRIDABLE_BLOCKED`, `.git/**` hard exclusion, `.github/**`/deletion overrides and tri-state directory selection.
- Updated authoritative documentation pointers and implementation status so 7.6 is the single `NEXT` step.
- Application/container version remains `1.0.0-rc.17`; this revision changes planning documentation only.

## 1.0.0-rc.17 — 2026-08-07

Corrective frontend release candidate for the import author selector layout.

### Fixed

- Prevented the generic full-width import input styling from applying to radio buttons.
- Kept the author option text immediately beside each radio control on desktop and responsive layouts.
- Preserved conditional author name/e-mail fields so they are only shown when `Någon annan` is selected.

## 1.0.0-rc.16 — 2026-08-07

Corrective MVP release candidate after exercising the RC15 project-detail frontend tests.

### Fixed

- Restored stage-specific import-history link labels: `Öppna resultat`, `Fortsätt granska`, and `Fortsätt import`.
- Preserved the RC15 persistent work-branch workflow unchanged.
- Confirmed that `DatabaseMigrationTest` is intentionally skipped when Testcontainers cannot find a Docker environment; no backend code change is required for that local condition.

## 1.0.0-rc.15 — 2026-08-07

- Replaced the one-import/one-branch workflow with one persistent active work branch per project.
- Each approved ZIP import now creates one sequential commit on the active work branch.
- The first import creates `zip-github/work-<workId>` from the project default branch; later imports compare against the latest work-branch HEAD.
- Pull requests are now created explicitly from the project page when the work is complete.
- Work branch/head metadata is persisted in PostgreSQL through Flyway migration V6 so work survives backend redeploys.
- A single ZIP import uses the same workflow without requiring a mode choice.

## 1.0.0-rc.14 — 2026-08-07

Corrective release candidate for per-import Git author metadata and authenticated committer identity.

### Changed

- Default Git author and committer now come from the authenticated GitHub user instead of global server environment variables.
- GitHub profile name/email are used when available; login and the GitHub user-specific noreply address are used as fallbacks.
- New imports let the user explicitly select “Någon annan” and provide an alternate author name/email.
- The committer is never client-selectable and remains the authenticated GitHub user who creates/approves the import.
- Git delivery sets `GIT_AUTHOR_*` and `GIT_COMMITTER_*` separately so Git history/blame reflects the author while commit metadata retains the approving user as committer.
- Removed the obsolete global `ZIP_GITHUB_GIT_AUTHOR_NAME` / `ZIP_GITHUB_GIT_AUTHOR_EMAIL` runtime configuration.

## 1.0.0-rc.13 — 2026-08-07

Corrective release candidate for Git authentication execution and durable project configuration.

### Fixed

- Replaced per-workspace temporary Git askpass scripts with a fixed executable helper built into the backend image, avoiding `noexec` tmpfs failures.
- Applied the fixed askpass helper consistently to snapshot, workspace preparation and delivery Git operations.
- Persisted authenticated user identities, visible GitHub App installation ownership and project configuration in PostgreSQL.
- Project lists and project details now survive backend restarts and deployments.
- Changed GitHub installation persistence identity to `(installation_id, owner_user_id)` so one GitHub App installation can safely be visible to multiple zip-github users without cross-tenant reassignment.
- Added Flyway migration V5 for durable project metadata including repository privacy.
- Kept in-progress import execution state in memory for now; only durable project configuration is covered by this correction.

## 1.0.0-rc.12 — 2026-08-07

Corrective release candidate for production container runtime prerequisites and persistent-volume ownership.

### Fixed

- Installed `git` in the backend runtime image; repository snapshot, workspace and delivery services invoke the Git CLI at runtime.
- Added a one-shot `storage-init` Compose service that prepares upload and delivery volumes for backend UID/GID `10001:10001`.
- Backend waits for storage initialization to complete successfully before starting.
- Kept the backend itself non-root; no `chmod 777` or Docker socket access is introduced.
- Preserved the RC11 GitHub App user-authorization flow and earlier backup/restore fixes.

## 1.0.0-rc.11 — 2026-08-07

Corrective release candidate aligning browser authentication with the GitHub App user-access-token model required by `/user/installations`.

### Fixed

- Removed the separate GitHub OAuth App requirement.
- Browser login now uses the GitHub App Client ID and Client Secret to mint a GitHub App user access token.
- Renamed production variables to `GITHUB_APP_CLIENT_ID`, `GITHUB_APP_CLIENT_SECRET` and `GITHUB_APP_CALLBACK_URL`.
- Kept `GITHUB_APP_ID` and `GITHUB_APP_PRIVATE_KEY` for short-lived installation access tokens.
- Added clearer token-exchange/user-lookup HTTP diagnostics without logging tokens.
- Updated GitHub App setup/configuration documentation.
- Preserved the RC10 real project frontend and the direct-GitHub backup/restore fixes.

## 1.0.0-rc.10 — 2026-08-07

Corrective release candidate replacing the remaining frontend demo shell with the real GitHub-authenticated project flow.

### Fixed

- Replaced the demo project list with `GET /api/projects`.
- Added GitHub OAuth session gating and a real "Logga in med GitHub" entry point.
- Added authenticated account display and logout.
- Added project creation from GitHub App installations and installation-scoped repositories.
- Added repository/default-branch selection and `POST /api/projects` integration.
- Removed `frontend/src/data/demoProjects.ts` and updated source-tracking regression checks.
- Preserved the production backup/restore `.env` loading changes previously committed directly to GitHub.

## 1.0.0-rc.9 — 2026-08-07

Corrective release candidate for the GHCR backend image build.

### Fixed

- Build the backend image with the Maven 3.9.11 binary already provided by `maven:3.9.11-eclipse-temurin-21` instead of bootstrapping Maven Wrapper inside the container.
- Removed the Docker-build dependency on `unzip`, which caused the GitHub Actions Buildx job to fail before Maven could start.
- Kept the Maven Wrapper as the authoritative host/CI test entry point; only the Docker build stage uses the matching preinstalled Maven binary.

# Changelog

## 1.0.0-rc.37 - 2026-08-07

- Added explicit owner-scoped import cancellation before Git delivery.
- Cancellation is idempotent, persisted as `CANCELLED`, removes disposable workspaces and preserves audit state.
- Cancelled uploads participate in ordinary terminal retention cleanup after their deadline.
- Review UI now offers an explicit confirmed `Avbryt import` action and returns to the project after cancellation.
- Added cancellation regression coverage for idempotency, owner isolation and delivered-import protection.


## 1.0.0-rc.34 - 2026-08-07

- Closed phase 7 with restart/resume regression coverage across review, selection, approval and completed delivery.
- Added owner-isolation regression ensuring another user cannot hydrate or resume durable import state.
- Strengthened Work-view tests so only the newest active import is shown while Git commits remain primary history.
- Added degraded GitHub-history regression using the persisted Work-head fallback.
- Updated release/operations/architecture acceptance guidance for restart-safe imports.

## 1.0.0-rc.8 - 2026-08-07

- Added CI Docker image builds gated on structure/backend/frontend success.
- Publish backend and frontend images to GHCR from successful `main` and tag builds.
- Added exact-version, source-SHA and RC/stable convenience tags without assigning `latest` to release candidates.
- Switched server Compose to published images and added a separate local-build Compose override.
- Made Docker builds reproducible with Maven Wrapper and `npm ci`.
- Added container deployment, upgrade and rollback documentation.

## 1.0.0-rc.7 - 2026-08-07

- Fixed PostgreSQL migration integration test parameter binding for `Instant` values by using JDBC `TIMESTAMP_WITH_TIMEZONE`.
- CI can now execute the owner-isolation migration test consistently with PostgreSQL 16.

## 1.0.0-rc.6 - 2026-08-07

- Fixed frontend production build by removing an unused `describe` import from `ImportResultPage.test.tsx`.

## 1.0.0-rc.5 - 2026-08-07

### Fixed
- Anchored runtime storage ignore rules (`/data/`, `/uploads/`, `/workspaces/`, `/tmp/`, `/temp/`) to the repository root so source directories such as `frontend/src/data/` are tracked by Git and available in CI.
- Added a repository-source regression check that fails if required frontend source files are accidentally ignored.

## 1.0.0-rc.4 - 2026-08-07

- Fixed CI-only Flyway PostgreSQL migration test failure by adding the required `org.flywaydb:flyway-database-postgresql` module.
- Updated relocated Quarkus test dependencies from `quarkus-junit5`/`quarkus-junit5-mockito` to `quarkus-junit`/`quarkus-junit-mockito`.


## 1.0.0-rc.3 — 2026-08-07

Corrective MVP release candidate after the remaining backend and frontend test regressions were exercised locally.

### Fixed

- Corrected `ImportPolicyServiceTest` to expect the four blocking policy entries actually produced by the fixture; `.env.local` remains a warning, not a blocker.
- Corrected the `RepositorySnapshotServiceTest` `git ls-tree` fixture to use real tab delimiters instead of the literal characters `\\t`.
- Made route focus management robust for asynchronously rendered pages by observing the main content until the new route's `h1` appears, then moving focus once.

## 1.0.0-rc.2 — 2026-08-07

Corrective MVP release candidate after full local build/test verification of `r0041`.

### Fixed

- Restored the missing `AppliedImportWorkspaceResponse` import in `ImportResource`.
- Removed the conflicting public/private `requireOwnedImport(UUID, UUID)` signatures while retaining a public ownership assertion API.
- Aligned import-history pull request numbers with the `long` PR number used by GitHub delivery metadata.
- Updated the application routing test to mock the project-detail and import-history API calls introduced in phase 6.3.
- Centralized test cleanup of stubbed globals to prevent cross-test leakage.

## 1.0.0-rc.1 — 2026-08-06

First MVP release candidate of zip-github.

### Included

- GitHub OAuth login and user-scoped GitHub App repository access.
- Project configuration and branch selection.
- Streaming ZIP upload with retention, checksum and resource limits.
- ZIP path, type, symlink, traversal and ZIP-bomb protections.
- Deterministic archive normalization and inventory.
- Repository snapshots locked to an exact Git commit SHA.
- SHA-256 comparison, import policy, blockers and immutable import plans.
- Exact plan approval and isolated Git workspace application.
- Atomic branch/commit/push delivery and draft pull request creation.
- Idempotent retry and recovery behavior.
- Result page, check status, import history and reopening.
- Mobile/accessibility baseline, CSRF/CORS/security headers and rate limiting.
- Docker Compose operations model, backup/restore documentation and security regression checks.

### Release-candidate limitations

- Session, project, import and result application stores are still partly in memory.
- Horizontal backend scaling is not supported.
- Production release still requires the external acceptance checks listed in `docs/release-checklist.md`.
- Phase 8 functionality is intentionally excluded from the MVP.
