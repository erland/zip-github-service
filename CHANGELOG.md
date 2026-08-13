## 1.0.0-rc.99 - 2026-08-13

### Fixed

- Corrects Step 9.27 frontend regressions found by GitHub CI: staging serialization tests now expect the explicit `confirmOpenPullRequest: false` field and the simplified import-flow mock exposes `getProjectWork`.
- Corrects Step 9.27 backend test compilation by importing `CreateImportRequest` in `WorkLifecycleServiceTest`.
- No production behavior changed; this is a focused CI/test correction after rc.98.

## 1.0.0-rc.98 - 2026-08-13

### Added

- Completes Step 9.27 with explicit confirmation before a new ZIP extends a Work whose pull request is still open.
- The regular upload page and Shortcut promotion flow both show the current PR and require an explicit continue action before creating/promoting the import.
- Backend creation reuses the existing strict PR reconciliation and independently rejects unconfirmed `PR_OPEN` reuse, so direct API calls cannot bypass the warning; merged PRs still terminate old Work and start fresh from the current default branch.

## 1.0.0-rc.97 - 2026-08-13

### Added

- Completes Step 9.26 by grouping multiple GitHub workflow runs for the same workflow and commit into one top-level Actions card while preserving each actual run in an expandable detail view.
- Typical `push` + `pull_request` runs created when a Work already has an open PR no longer make the same workflow appear duplicated.
- Group status is conservative so a failed run is never hidden by a successful sibling run, and distinct workflow IDs remain separate even when their display names match.
- Documents Step 9.27 as the next improvement: explicit confirmation before extending a Work that already has an open PR.

## 1.0.0-rc.96 - 2026-08-13

### Fixed

- Corrects the Step 9.25 frontend CI test bootstrap by explicitly importing `beforeEach`, `afterEach`, `expect` and `test` from Vitest in `MaintenancePage.test.tsx`.
- No production behavior changed; this is a focused CI correction after rc.95.

## 1.0.0-rc.95 - 2026-08-13

### Added

- Completes Step 9.25 with a global authenticated maintenance view for conservative cleanup of orphaned zip-GitHub Work branches across GitHub App repositories.
- Inventory is strictly limited to UUID-shaped `zip-github/work-*` branches, shows a read-only preview, requires a separate explicit bulk acknowledgement and never deletes automatically.
- Backend classification fails closed for protected/default branches, any non-terminal Work in the repository, any open pull request using the branch as head, incomplete GitHub/database status or lost repository visibility.
- Cleanup re-resolves current visibility, branch state, Work usage and open-PR state immediately before every individual delete; mixed bulk results are reported independently per branch.
- GitHub App installation/repository and branch listing now use bounded pagination so installations with more than 100 repositories are inventoried rather than silently truncated.
- Adds focused backend/frontend regressions for safe candidates, active Work, open PR, protected/unverifiable state, stale preview and explicit UI acknowledgement.

## 1.0.0-rc.94 - 2026-08-13

### Fixed

- Corrected the remaining Step 9.24 backend test compilation failure in `AlternativeZipIngestionRegressionTest` by passing the explicit blocker-decision list required by the `selection-2` `ImportSelectionFactory.create(...)` contract.
- No product behavior changed; this is a focused CI correction after rc.93.

## 1.0.0-rc.93 - 2026-08-13

- Corrects the Step 9.24 backend compilation failure by importing `ImmutableImportPlanEntry` in `ProjectApplicationService`.
- No production behavior or blocker-decision semantics change from rc.92; this is a focused CI/build correction.
- Step 9.25 remains `NEXT`.

## 1.0.0-rc.92 - 2026-08-13

- Completes Step 9.24 with explicit decisions for every blocking review entry before an import can be approved or delivered.
- Overridable blockers now require an explicit `Ta inte med` or `Godkänn och ta med` decision; hard blockers require an explicit acknowledgement that they will be omitted and remain impossible to select.
- Adds category-level explicit exclusion for overridable blockers while preserving the existing explicit bulk override action.
- Upgrades immutable import selections to `selection-2`; blocker decisions are persisted, included in the selection digest and returned by the API.
- Backend validation rejects missing, inconsistent or legacy blocker-decision coverage before selection storage/approval and again before delivery resume, preserving the exact-selection invariant even for manipulated or legacy clients.
- Adds focused backend/frontend regressions and release-gate assertions for blocker decisions. Step 9.25 is now `NEXT`.

## 1.0.0-rc.91 - 2026-08-13

- Completes Step 9.23 by correcting the active frontend browser title from the legacy `zip-buildserver` name to `zip-GitHub`.
- Preserves historical `zip-buildserver` references in migration/baseline/legacy documentation instead of rewriting historical context.
- Adds release verification for the active browser title.
- Adds planned Steps 9.24 (explicit decisions for every blocking review entry) and 9.25 (safe global cleanup of orphaned `zip-github/work-*` branches), with 9.24 now `NEXT`.

## 1.0.0-rc.90 - 2026-08-11

- Completes Step 9.22 with conservative Shortcut repository suggestions based on normalized ZIP/repository names, the authenticated user’s latest upload filename for an existing project, and a smaller recency bonus.
- High-confidence suggestions require explicit `Använd detta repository` confirmation and never implicitly promote an upload; ambiguous cases fall back to the shared searchable Step 9.21 picker.
- Repository catalog responses now include user/project-isolated `lastSourceFilename` and `lastUsedAt` metadata used only for suggestion ranking.
- Added regressions for normalization, direct/history matching, ambiguity, recency tie-breaking and explicit Shortcut confirmation.

## 1.0.0-rc.89 - 2026-08-11

- Step 9.21 adds a shared repository picker to the repository landing page and Shortcut claim flow.
- Shortcut repository selection now has repository search, a separately scrollable list and an always-visible selected-repository summary above the continue action.
- Up to five recently used repositories are kept as a client-side convenience and shown above the unchanged searchable/alphabetical full list.
- Documents Step 9.22 as the next improvement: confidence-ranked Shortcut repository suggestions from ZIP filename, prior upload filenames and repository recency, always requiring user confirmation.

## 1.0.0-rc.88 - 2026-08-11

- Corrected the Step 9.20 frontend regression test to address the PR title and description by their labels instead of using an ambiguous display-value query that correctly matched both fields.
- No production behavior changes from rc.87.

## 1.0.0-rc.87 - 2026-08-11

- Added step 9.20 to simplify commit-derived pull request metadata: `Fyll från commitmeddelanden` now produces only the chronological Markdown bullet list, without an `Ingående commits` heading.
- When the PR title is still blank, the helper now fills it from the first chronological Work commit's subject line; an already entered title is preserved unchanged.
- Added focused frontend regressions for both automatic-title and preserve-existing-title behavior.

## 1.0.0-rc.86 - 2026-08-11

- Corrected the remaining Step 9.19 frontend TypeScript fixture widening: newly appended review-plan entries are now explicitly checked as `ImportPlanEntry`, preserving literal union types through array spreads.
- GitHub Actions had already shown all 58 frontend runtime tests passing in rc.85; this correction only addresses the `tsc -b` build error and does not change production behavior.

## 1.0.0-rc.85 - 2026-08-11

- Corrected step 9.19 CI fixtures after the complete-ZIP prospective `.gitignore` change: repository ignore tests now include the `.gitignore` files they intend to preserve, and the bulk-review frontend test uses the API plan type so deletion metadata may legitimately be `null`.
- No production behavior change from rc.84.

## 1.0.0-rc.84 - 2026-08-11

- Added step 9.19 to make import review use the complete uploaded ZIP's prospective `.gitignore`, so new files ignored by rules introduced in the same ZIP are classified as `IGNORED` before selection/approval.
- Removed stale repository `.gitignore` rules from prospective comparison when the complete ZIP deletes that `.gitignore`.
- Added category-scoped bulk selection in review, including one explicit bulk override acknowledgement for all overridable entries in the active category while hard blockers remain impossible to select.
- Improved workspace mismatch diagnostics with explicit missing and unexpected path lists.
- Added backend and frontend regressions for prospective `.gitignore`, deleted `.gitignore`, bulk deletion override and hard-blocker exclusion.

## 1.0.0-rc.82 - 2026-08-09

- Corrected the Step 9.18 frontend regression test to assert the visible CodeQL and Dependency review rows without assuming the link text and app label are one DOM text node.
- No production behavior changes.

## 1.0.0-rc.81 - 2026-08-09

- Added step 9.18 to remove duplicate GitHub Actions status presentation without changing repository workflow configuration.
- Workflow runs and their jobs remain the primary Actions view; GitHub Actions checks with a matching displayed job for the same commit are suppressed from the secondary list.
- Remaining checks from other apps, or GitHub Actions checks without a corresponding displayed job, are shown under `Övriga kontroller`.
- Added frontend regressions for both mixed checks and the all-duplicated case.

## 1.0.0-rc.80 - 2026-08-09

- Corrected the Step 9.16 race where a pull request could be merged on GitHub before the persisted Work state had synchronized, allowing a later web/Shortcut import to reuse the already-merged Work branch.
- New imports now strictly verify current PR state before reusing a Work with PR metadata; merged PRs close the old Work and start a fresh Work from the current default-branch HEAD.
- GitHub PR-state unavailability now fails new-import reuse closed with `WORK_PULL_REQUEST_STATUS_UNAVAILABLE` instead of risking a commit on a merged branch.
- Git delivery re-verifies PR state immediately before push and rejects delivery with `WORK_PULL_REQUEST_MERGED_REVIEW_REQUIRED` if the PR merged after review.
- Added regression coverage for merge-before-upload, GitHub-status-unavailable and merge-after-review-before-delivery scenarios.

## 1.0.0-rc.79 - 2026-08-09

- Corrected the Step 9.17 frontend regression test to expect Work commit messages in chronological order (oldest to newest), matching the implemented PR description behavior.
- Added a release-gate assertion for the chronological commit-message draft order.
- No production behavior changes.

## 1.0.0-rc.78 - 2026-08-09

- Corrected frontend CI regressions after step 9.17 without changing production behavior.
- Existing import-flow tests now enter an explicit commit message before approval/delivery, matching the new mandatory metadata contract.
- Review-state assertions now keep approval disabled while the commit message is blank, including after external-branch-change acknowledgement.
- Added release-gate checks so the simplified import flow and delivery/retry regressions cannot silently bypass the explicit commit-message requirement again.


## 1.0.0-rc.77 - 2026-08-09

- Added step 9.17 explicit commit and pull-request metadata. New interactive commit messages now start empty and must be entered by the user.
- Draft PR creation now requires an explicit user-entered title and description; the previous generated PR title/body were removed.
- Added a reusable PR composer on both Work and post-commit views with server-side validation and clear 400 responses for invalid metadata.
- Added `Fyll från commitmeddelanden`, which fills an editable Markdown description from the commits belonging to the current Work only, in chronological order.
- Work commit history is now bounded to commits after the Work base SHA so base-branch history cannot leak into the PR-description helper.

## 1.0.0-rc.76 - 2026-08-09

- Corrected CI regressions introduced with step 9.16 without changing production behavior.
- Backend `ImportResource` now imports the new `ExternalBranchChangesResponse` DTO so the Quarkus module compiles.
- Project-detail tests now assert the intentional 9.16 action label `Skapa pull request`.
- Simplified import-flow E2E now mocks the external-branch-change API with a neutral response so review rendering reaches the existing selection/override assertions.

## 1.0.0-rc.75 - 2026-08-09

- Added step 9.16: a pull request no longer terminates Work; `PR_OPEN` and `PR_CLOSED` remain continuable until merge/abandon.
- Added Flyway migration V14 to migrate only the latest historical `PULL_REQUEST_CREATED` Work row per project to `PR_OPEN` and include PR states in the one-open-Work invariant.
- Work status now refreshes the PR from GitHub; merged PRs close the logical Work while closed-unmerged PRs can be continued or replaced with a new PR.
- Work Actions/checks now follow the current remote Work-branch HEAD so GitHub-side commits and their CI results are visible.
- Added GitHub compare-based external branch change detection and review warnings/filtering for ZIP paths that would replace later GitHub-side changes.
- Selected overlapping external changes require an explicit UI acknowledgement before commit, while existing SHA-bound stale-plan delivery remains the hard guard against post-review branch movement.

## 1.0.0-rc.74 - 2026-08-09

- Added step 9.15 to attribute draft pull request creation to the authenticated GitHub user instead of `zip-github-service[bot]`.
- Draft PR lookup/create/retry now uses the session's GitHub App user access token; repository push and other server-side automation continue to use short-lived installation tokens.
- Confirmed commit author/committer identity was already locked to the authenticated user's GitHub identity (or explicit alternate author) and did not require a behavior change.
- Added regression coverage that rejects PR creation paths using any token other than the authenticated user access token.

## 1.0.0-rc.73 - 2026-08-09

- Added step 9.14 with an explicit manual GitHub Actions production-deployment workflow using an immutable image version.
- Added a dedicated restricted `zip-github-deploy` SSH/sudo path instead of using the operator's personal server account or Docker-group membership.
- Added a root-owned deployment script that updates the main checkout with `pull --ff-only`, changes only `ZIP_GITHUB_VERSION`, pulls/starts Compose images and waits for backend/frontend health.
- Added a complete production setup guide for directory ownership, SSH key/host-key verification, GitHub Environment variables/secrets, deployment and safe manual rollback.

## 1.0.0-rc.72 - 2026-08-09

- Corrected the rc.71 TypeScript build failure in the repository-first Shortcut promotion flow.
- Frontend staging promotion now accepts either an existing `projectId` or a GitHub installation/repository target and serializes that target directly, matching the backend DTO introduced by step 9.13.
- Added focused API regressions for both promotion target forms; no backend behavior changed.

## 1.0.0-rc.71 - 2026-08-09

- Corrected the rc.70 frontend CI race in the repository-list filtering regression.
- The App test now waits for the asynchronously loaded repository link before exercising client-side filtering.
- No production frontend or backend behavior changed; step 9.13 remains complete.

## 1.0.0-rc.70 - 2026-08-09

- Added step 9.13 repository-first UX: the authenticated start page now lists GitHub App repositories instead of user-created Projects.
- Added client-side repository filtering on short name and `owner/repo`, with only repository names shown by default and full names used to disambiguate duplicates.
- Removed manual Project creation from normal routing; `/projects/new` now returns to the repository list.
- Added lazy `ensureProject` so opening/listing a repository creates no Project; the internal owner-bound Project is created/reused only when Work starts or Shortcut promotion needs it.
- Updated Shortcut claim/promotion to select repositories and bootstrap the internal Project transparently before entering the existing import pipeline.
- Kept all established Project/Work/import ownership, branch, approval and delivery invariants behind the repository-first UI.

## 1.0.0-rc.69 - 2026-08-08

- Corrected the rc.68 frontend CI regressions after the review-filter UX change.
- Gitignored `IGNORED` entries are now rendered as informational rows without a disabled selection checkbox, matching their non-selectable/no-acknowledgement contract.
- Updated the simplified import-flow regression to match counted filter labels such as `Oförändrade (1)` and `Blockerade (1)`.
- No backend policy or `.gitignore` matching behavior changed; step 9.12 remains complete.

## 1.0.0-rc.68 - 2026-08-08

- Added corrective step 9.12 so import planning evaluates the target repository's tracked `.gitignore` files before policy classification.
- New ZIP paths ignored by Git are now informational warnings (`GITIGNORE_IGNORED`) rather than blockers; they are never selectable and require no override or acknowledgement.
- Removed the project-specific `shortcut/releases/zip-github.shortcut` hard block. The signed Shortcut remains protected generically by the repository `.gitignore`; `.git/**` stays hard-blocked independently of ignore rules.
- Preserved tracked-file semantics: an already tracked repository path is still compared normally even if a `.gitignore` rule matches it.
- Simplified the review summary to one neutral statistics strip and kept one explicit filter-control row, with counts on the actual filter buttons.

## 1.0.0-rc.67 - 2026-08-08

- Corrected the shared ActionsPanel runtime crash observed by GitHub Actions frontend CI when legacy/partial workflow payloads omitted `headSha`.
- ActionsPanel now falls back from workflow `headSha` to the response commit SHA and finally the panel commit SHA, and tolerates missing workflow/check/job arrays during rolling upgrades.
- Added a focused frontend regression proving a workflow without `headSha` still renders against the current commit.
- No backend behavior, release scope, or completed implementation-step state changed.

## 1.0.0-rc.66 - 2026-08-08

- Added post-phase step 9.11 for the observed Work Actions visibility gap and unified Work/result Actions UX.
- Workflow runs remain visible when secondary jobs/check endpoints fail; Actions HTTP 403 now carries an explicit `ACTIONS_PERMISSION_REQUIRED` diagnostic instead of looking like no workflow ran.
- Work and commit/result views now render the same Actions component and Work reuses the same import-bound, allowlisted dispatch/rerun controls.
- Failed jobs now expose a redacted condensed error, contextual lines around the failure, and an expandable bounded sanitized job log (128 KiB / 1600-line display cap), plus separate copy actions.
- Phase 9 remains complete after the corrective 9.11 quality gate.

## 1.0.0-rc.65 - 2026-08-08

- Fixed HTTP 413 for ZIP uploads above nginx's 1 MB default by giving the frontend proxy an explicit runtime-configurable `client_max_body_size`.
- Added `ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE` with a `200M` container/Compose default and switched frontend nginx config to the official runtime template path.
- Aligned the backend compressed-upload default and `.env.example` to 200 MiB (`209715200` bytes); backend streaming enforcement remains authoritative.
- Phase 9 remains complete; this is a post-phase deployment correction.

## 1.0.0-rc.64 - 2026-08-08

- Fixed Work GitHub Actions visibility so a failure reading commit check-runs no longer discards successfully fetched workflow runs/jobs.
- Added a regression based on run 31258714926 / commit f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69 that simulates unavailable Checks while requiring the push workflow to remain visible.
- Phase 9 remains complete; this is a post-phase production correction.

## 1.0.0-rc.63 - 2026-08-08

### Changed
- Corrected the final remaining frontend CI test ambiguity after the phase-9 Actions UI introduced multiple simultaneous live-status regions.
- The degraded Work-history regression now asserts the exact GitHub-history fallback message instead of assuming it is the page's only `role="status"` element.
- Production code and completed phase-9 behavior are unchanged.

## 1.0.0-rc.62 - 2026-08-08

### Changed
- Corrected three frontend CI regressions exposed after the completed 9.8/9.9 UI lifecycle changes; production behavior is unchanged.
- Updated the app routing test to create a verified Work branch before navigating to ZIP upload instead of looking for the removed pre-9.8 `Starta arbete` link.
- Scoped the Work commit SHA assertion to the commit-history section because the exact Work head is intentionally also shown by the revisitable Actions UI.
- Fixed the clipboard test setup so the `navigator.clipboard.writeText` spy is installed after `userEvent.setup()` and therefore observes the actual copy action.
- Phase 9 remains complete; this correction does not reopen any implementation step.

## 1.0.0-rc.61 - 2026-08-08

### Changed
- Completed phase 9 step 9.10 with a final cross-step release gate covering staging credential rotation, claim/promotion convergence, Work provisioning/recovery, missing-branch delivery refusal, file-mode invariants, revisitable Actions diagnostics and the signed Shortcut release manifest.
- Added `scripts/verify-phase9-release.sh` so the phase-9 contracts are checked together instead of relying only on isolated step gates.
- Updated operations, threat model, API contract, release checklist and Shortcut release guidance to reflect the completed persistent import/Work lifecycle and the deployed signed Shortcut verification.
- The active implementation plan is complete; no `NEXT` step remains. Future AI/integration work stays in the explicitly separate backlog.

## 1.0.0-rc.60 - 2026-08-08

### Changed
- Completed phase 9 step 9.9 by exposing the existing commit-scoped GitHub Actions status/detail services directly from the active Work view.
- Added project Work Actions endpoints that are bound to the Work session's `lastImportId` and exact `headCommitSha`, preventing stale runs for an older branch commit from being shown as current status.
- Added revisitable workflow/job status, GitHub run links, explicit refresh, bounded polling while active, condensed failure details and copyable AI/support-friendly failure text on the project page.
- Reused the existing phase-8 GitHub failure extraction/redaction path; no permanent backend monitor or new background authorization was introduced.
- Step 9.10 is NEXT; final phase-9 E2E/release regression remains intentionally out of scope for this revision.

## 1.0.0-rc.59 - 2026-08-08

### Changed
- Completed phase 9 step 9.8 with explicit Work provisioning (`PROVISIONING -> ACTIVE`) backed by real GitHub branch creation/readback before imports can proceed.
- Added restart-safe provisioning recovery and a compatibility repair for pre-9.8 ACTIVE Work rows whose remote branch was never created.
- Delivery now requires the remote Work branch to already exist at the approved base SHA; it never recreates a missing Work branch implicitly.
- Added project-page GitHub default-branch link, explicit end-without-PR with optional branch deletion, resumable existing non-default/non-protected branches, and soft project archive/removal.
- Step 9.9 is NEXT; Actions status on the Work page remains intentionally out of scope for this revision.

## 1.0.0-rc.58 - 2026-08-08

### Changed
- Completed phase 9 step 9.7 after the deployed authenticated `/shortcut` path was exercised and the downloaded Apple-signed Shortcut was imported on iPhone.
- The authenticated download now exposes `Skicka till zip-github.shortcut` through `Content-Disposition` while serving the exact signed bytes from the technical server path `zip-github.shortcut`.
- `scripts/sign-shortcut-release.sh` now publishes signed releases as mode `0644` instead of `0600`, matching the read-only bind-mount deployment model where the backend runs as a separate runtime user.
- Release verification now asserts the friendly download identity, runtime-readable deployment mode when the binary is present, and the signed artifact manifest/hash invariants.
- Step 9.8 is NEXT; no 9.8 implementation is included in this revision.

## 1.0.0-rc.57 - 2026-08-08

### Planned
- Refined phase 9.7 verification after real deployment testing: the authenticated Shortcut download must expose `Skicka till zip-github.shortcut` via `Content-Disposition` while the server may keep the technical filename `zip-github.shortcut`.
- Added an explicit deployment/runtime readability gate for the signed Shortcut so signing/publication cannot leave the bind-mounted artifact unreadable by the backend (the observed `0600` failure); `0644` or equivalent readable ownership/ACL is the recommended simple deployment mode.
- Added the same filename/hash/runtime-readability assertions to the final phase 9.10 Shortcut E2E release gate. No 9.8 implementation is included.

## 1.0.0-rc.56 - 2026-08-08

### Fixed
- Corrected the rc.55 Structure and security CI failure: clean Git checkouts no longer require the intentionally ignored credential-bearing `zip-github.shortcut` binary.
- Added a tracked signed-Shortcut release manifest and conditional byte/hash verification when the deployment artifact is present.

### Planned
- Split the remaining phase 9 work into 9.8 Work/project lifecycle and robust branch provisioning, 9.9 revisitable GitHub Actions status/copyable failures, and 9.10 final E2E/release regression.
- No 9.8 implementation is included; step 9.7 remains blocked only on deployed download/install verification.

## 1.0.0-rc.55 - 2026-08-08

### Changed
- Integrated the user-created and iOS-verified Apple-signed reference Shortcut as the deployment release artifact at `shortcut/releases/zip-github.shortcut`.
- Default Compose metadata now identifies the first Shortcut release as version `1`, generation `g1`; the published artifact is 23821 bytes with SHA-256 `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`.
- Hard-blocked `shortcut/releases/zip-github.shortcut` in the ordinary import policy so the credential-bearing deployment artifact cannot accidentally be selected and committed to Git when this delivery ZIP is used as an import source.
- The former Apple-signing blocker is resolved. Step 9.7 remains BLOCKED only until the deployed `/shortcut` download path has been exercised and the downloaded artifact accepted by iOS; step 9.8 has not started.

## 1.0.0-rc.54 - 2026-08-08

### Fixed
- Reworked the container-image CI job to package the backend and frontend artifacts already built and verified by their prerequisite jobs instead of running Maven and npm a second time inside Docker builds.
- Added thin runtime-only Dockerfiles for CI image assembly, eliminating the image job's separate Maven Central dependency-resolution path that failed with HTTP 429.

# Changelog

## 1.0.0-rc.83 - 2026-08-10

- Corrected the container upload ingress chain so Quarkus no longer keeps its lower default HTTP body-size ceiling when zip-github is configured for 200 MiB ZIP uploads.
- Backend Compose now sets `QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE=${QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE:-200M}`, aligned with frontend nginx and the application-level compressed-upload limit.
- Added the Quarkus body-size setting to `.env.example`, upload-streaming documentation and the configuration reference.
- No import, authorization, archive-inspection or delivery policy behavior changed.

## 1.0.0-rc.53 - 2026-08-08

- Corrected the remaining backend CI regressions discovered in workflow run 31242966490 without changing the phase 9.7 BLOCKED state.
- Updated approval recovery API coverage to send and verify the required approval-bound `commitMessage`.
- Updated staging upload API tests to stream binary request bodies for `application/zip` instead of asking RestAssured to serialize text/byte arrays through an unknown content-type encoder.
- Aligned the missing/revoked staging credential expectation with the phase-9.7 `403 STAGING_SHORTCUT_OUTDATED` contract.
- Made terminal StagingImport claim attempts consistently fail through `DomainTransitionException`, while deadline expiry before persisted cleanup remains the separate time-expiry signal.
- Phase 9.7 remains blocked only on the external Apple signing/iOS installation gate; phase 9.8 has not started.

## 1.0.0-rc.52 - 2026-08-08

- Corrected CI regressions discovered after r0099 without changing the phase 9.7 BLOCKED state.
- Disambiguated the Hamcrest `endsWith` matcher in `StagingImportResourceTest` from Mockito's matcher of the same name.
- Added explicit Testing Library cleanup in `ShortcutInstallPage.test.tsx` so the published-release render cannot leak a download link into the subsequent unavailable-release test.
- Phase 9.7 remains blocked only on the external Apple signing/iOS installation gate; phase 9.8 has not started.

## 1.0.0-rc.51 - 2026-08-08

- Implemented the zip-github side of phase 9 step 9.7: authenticated static signed-Shortcut release metadata/download plus a mobile installation/update page.
- Added explicit `STAGING_SHORTCUT_OUTDATED` handling for revoked/old Shortcut upload credentials and documented trusted-Mac signing/publication/rotation.
- Signed `.shortcut` binaries are deliberately ignored by Git and mounted read-only as deployment artifacts because they embed the staging-create credential.
- Step 9.7 remains **BLOCKED**, not DONE: this environment cannot produce or iOS-verify the required Apple-signed `anyone` release artifact because `shortcuts sign` requires an iCloud-signed-in Apple environment.
- Step 9.8 remains PENDING and has not started.

## 1.0.0-rc.50 - 2026-08-08

- Completed phase 9 step 9.6 with deterministic staging retention, cleanup race protection, durable storage quotas and credential-rotation incident guidance.
- Successful claim now moves an AVAILABLE upload onto a separate configurable claimed grace deadline; same-owner retry does not extend it indefinitely.
- Added database-coordinated promotion/cleanup locking, crash-window reconciliation and restart-safe physical deletion markers; promoted artifacts remain owned by ordinary Import retention.
- Added serialized staging object/byte capacity limits plus optional trusted-proxy network-source rate limiting alongside existing per-capability/global limits.
- Deployment upload credential can be revoked/rotated by secret replacement + backend redeploy without database migration; existing staging claim/TTL state is unaffected.
- Step 9.7 is next.

## 1.0.0-rc.49 - 2026-08-08

- Completed phase 9 step 9.5 with a user-editable commit message in the common browser/StagingImport review and approval path.
- The generated `Apply approved ZIP import <id>` text is now only an editable suggestion; interactive approval requires a non-empty server-normalized message of at most 500 characters.
- Commit message is persisted inside restart-safe approval state, is part of approval identity and is reused unchanged for delivery retry after refresh/logout/restart.
- Git delivery now commits with the exact approval-bound message instead of regenerating a hidden message at delivery time.
- Legacy/internal resume data without the new field uses the deterministic old message only as an explicit compatibility fallback.
- Step 9.6 — staging retention, abuse protection and security regression — is next.

## 1.0.0-rc.48 - 2026-08-08

- Completed phase 9 step 9.4 with authenticated Project selection and restart-safe promotion of a claimed StagingImport into exactly one ordinary Import.
- Promotion uses the existing stored-upload import path with `ImportSource.STAGING_IMPORT`, a stable non-secret staging source reference and no ZIP copy/re-stream.
- Added a unique persistent staging source-reference index so restart/retry cannot create a second ordinary Import for the same staging object.
- Added deterministic Git file-mode preservation across browser and StagingImport uploads: trustworthy ZIP `100644`/`100755` metadata wins, existing repository mode is the fallback, and new files without mode metadata default to `100644`.
- Mode-only changes are reviewable `MODIFIED` entries, included in the immutable plan digest/approval, applied only to selected paths and verified in the staged Git index before commit.
- The staging browser flow now lists only the authenticated user's active projects and continues into the existing ordinary import review; `ACTIVE_IMPORT_EXISTS` remains the single-active-import guard.
- Step 9.5 — user-controlled commit message in the common approval/delivery path — is next.

## 1.0.0-rc.47 - 2026-08-08

- Completed phase 9 step 9.3 with authenticated, atomic browser claim of short-lived StagingImport uploads.
- Added fragment-to-sessionStorage handling so raw claim tokens do not enter OAuth state, query parameters or normal server access logs.
- Added neutral 410 behavior for unavailable claims and idempotent same-owner retry after lost responses.
- Claim returns only owner-safe staging metadata and still creates no ordinary Import, Project selection or GitHub side effect.
- Step 9.4 — project selection and promotion to ordinary Import — is next.

## 1.0.0-rc.46 - 2026-08-08

- Completed phase 9 step 9.2 with a capability-protected transport-only `POST /api/staging-imports` endpoint.
- Added deny-all-by-default deployment staging credential validation, exact CSRF boundary and a dedicated staging upload rate bucket.
- Added 256-bit one-time claim-token creation with hash-only persistence and fragment-based claim URL response; no anonymous list/read/download endpoint exists.
- Reused the existing `ZipIngestionService` so staging creation shares browser upload byte limits, streaming storage, filename validation and SHA-256 behavior.
- Step 9.3 — authenticated browser claim — is next; no claim or promotion API is included in this revision.

## 1.0.0-rc.45 - 2026-08-08

- Completed phase 9 step 9.1 with a durable `StagingImport` lifecycle and Flyway V10 persistence.
- Added restart-safe `AVAILABLE`/`CLAIMED`/`PROMOTED`/`EXPIRED`/`CANCELLED` state, hashed claim-token storage and transactional claim/promotion primitives.
- Extended the neutral stored-upload representation with explicit Git-relevant `100644`/`100755` per-file mode metadata without filename-based executable inference.
- Documented ownership, locking and idempotency requirements for later claim/promotion steps; no anonymous upload/claim API is introduced in 9.1.
- Step 9.2 — capability-protected staging upload — is next.

## Planning revision r0092 - 2026-08-08

- Locked phase 9 to a static pre-signed reference Shortcut for the initial release.
- Defined the staging upload credential as deployment-scoped, low privilege and independently revocable, using a dedicated HTTP header.
- Removed any phase-9 requirement for per-user dynamic Shortcut generation, current/previous credential overlap or automated GitHub-hosted signing.
- Recorded the practical macOS Actions spike showing `shortcuts sign` requires an iCloud-signed-in environment.
- Defined rotation as immediate revoke plus publication of a newly signed Shortcut; old installations receive an update-required error.
- Application version remains `1.0.0-rc.44`; this revision changes planning only.

## Planning revision r0091 - 2026-08-08

- Added dedicated phase-9 step 9.5 for user-controlled commit messages in the common browser/StagingImport approval and delivery path.
- The existing generated message may remain as an editable suggestion, while the final user-selected message must be validated, persisted, approval-bound and stable across restart/retry.
- Renumbered staging retention, Shortcut documentation and final E2E/release work to steps 9.6–9.8 and extended final regression to commit-message parity/idempotency.
- Application version remains 1.0.0-rc.44 because this revision changes planning/documentation only; step 9.1 remains NEXT.

## Planning revision r0090 - 2026-08-08

- Assigned Git file-mode/executable-bit preservation to phase 9 after CI exposed that ZIP→GitHub imports cannot safely rely on executable metadata being retained.
- Step 9.1 now requires a neutral per-file representation for trustworthy ZIP mode metadata plus deterministic fallback rules.
- Step 9.4 now requires review/approval/delivery of `100644`/`100755` mode changes and preservation of base-repository mode when ZIP metadata is absent.
- Step 9.7 now includes explicit file-mode and browser-vs-Shortcut equivalence regression.
- Application version remains 1.0.0-rc.44 because this revision changes planning/documentation only; step 9.1 remains NEXT.

## 1.0.0-rc.44 - 2026-08-07

- Corrected Quarkus startup when controlled-Actions workflow allowlists are intentionally empty: configuration is now injected as `Optional<String>` and still defaults to deny-all.
- Corrected `scripts/verify-release.sh` to invoke its nested repository checks through `bash`, removing the remaining executable-bit dependency in the structure/security CI job.
- No intended feature-scope change; phase 8 remains complete and step 9.1 remains next.

## 1.0.0-rc.43 - 2026-08-07

- Corrected CI shell entrypoints to invoke repository scripts and the Maven wrapper through `bash`, so ZIP-to-GitHub imports do not depend on Git executable-bit preservation.
- Corrected the step-8.3 GitHub App Actions permission lookup to use the GitHub API base URL explicitly.
- Corrected the controlled-Actions frontend regression to scope its Work-branch assertion to the controls section when the same branch is also shown in delivery metadata.
- No intended runtime scope change; phase 8 remains complete and step 9.1 remains next.

## 1.0.0-rc.42 - 2026-08-07

- Completed step 8.3 with default-deny, operation-specific workflow dispatch/rerun allowlists.
- Added exact current-Work ref/commit guards, workflow/run identity validation, persistent audit and idempotency claims before GitHub Actions writes.
- Added mobile-friendly explicit dispatch and failed-job rerun controls without exposing arbitrary workflow inputs or GitHub credentials.
- GitHub App Actions permission is now read/write when controlled writes are enabled; phase 8 is complete.
- Step 9.1 — Define and persist the StagingImport lifecycle — is next; no phase-9 implementation is included in this revision.

## 1.0.0-rc.41 - 2026-08-07

- Completed step 8.2 with owner-scoped bounded GitHub Actions artifact metadata and condensed failed-job errors for the exact delivered commit.
- Added safe GitHub run/job links without artifact proxying or permanent artifact/log storage.
- Added 24 KiB failed-job log caps, maximum three excerpts, eight-line/180-character output bounds, terminal-sequence cleanup and common credential redaction.
- Added conservative Maven/Gradle, npm/Vite, Pandoc and xcodebuild recognition; unknown log formats are not guessed.
- Step 8.3 — Controlled workflow dispatch and rerun — is next; phase 9 and the future AI backlog remain untouched.

## 1.0.0-rc.40 - 2026-08-07

- Completed step 8.1 with owner-scoped, read-only GitHub Actions workflow-run, job and check status for the exact delivered Work commit.
- Added bounded GitHub reads (10 workflow runs, 50 jobs per run, 50 checks), normalized status mapping, short server caching and exponential frontend polling that stops for terminal results.
- Added a mobile-friendly Actions overview to the commit result while retaining permanent GitHub links and graceful `not_started`/`unavailable` states.
- Documented the required GitHub App Actions read permission and the exact verification limits of this packaging environment.
- Step 8.2 — Artifacts and condensed errors — is next; phase 9 and the future AI backlog remain untouched.

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
