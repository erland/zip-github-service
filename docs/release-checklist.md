# MVP release checklist

This checklist separates repository-complete controls from environment-dependent acceptance tests. The project may be tagged as `1.0.0-rc.8` when the repository controls pass. It must not be promoted to a production-ready `1.0.0` until all external checks are completed and recorded.

## Repository controls

- [x] All implementation steps 0.1–7.10 are completed and documented.
- [x] Exactly one post-MVP step is marked `NEXT`.
- [x] Backend build and tests are defined in GitHub Actions.
- [x] Frontend tests and production build are defined in GitHub Actions.
- [x] Structure and security regression scripts are part of CI.
- [x] ZIP traversal, symlink, special-file and resource-limit controls exist.
- [x] Repository comparison is locked to an exact base commit SHA.
- [x] Review and approval are bound to immutable plan and selection digests.
- [x] Delivery creates one non-force-pushed branch and atomic commit.
- [x] `.git/**` is hard blocked and cannot be selected even with an override.
- [x] `.github/**` and deletions require explicit per-path override audit before selection.
- [x] Prepared workspace diff is path-exact to the immutable selected path set and leaves excluded paths untouched.
- [x] Automated regressions cover mixed trees, partial selection, empty selection, overrides, hard blockers and stale work-branch movement.
- [x] Draft pull request creation is idempotent and recoverable.
- [x] CSRF, restricted CORS, security headers and rate limiting are present.
- [x] Phase 9 staging-create uses a separate deny-all-by-default deployment credential, exact POST-only CSRF exemption, 256-bit hash-only claim tokens and no anonymous list/read endpoint.
- [x] Docker Compose does not mount the Docker socket.
- [x] CI builds backend and frontend container images after tests succeed.
- [ ] GHCR backend and frontend images publish successfully from the release commit.
- [ ] Server can pull the exact `ZIP_GITHUB_VERSION` image tags.
- [x] Operations, backup, restore, incident and GitHub App setup are documented.
- [x] Version, changelog, architecture and release notes are present.

- [x] Phase 7 streamlined-flow regression covers automatic preparation, exact selection/override, one-click approval-to-delivery and retry without duplicate approval/commit intent.
- [x] Unchanged `.github/**` entries are regression-tested as non-overridable/no-change while actual workflow changes require explicit override.

## External acceptance checks required before production 1.0.0

- [ ] GitHub Actions CI is green on the release commit.
- [ ] Complete live E2E import succeeds against a dedicated private GitHub test repository.
- [ ] A moved base branch is rejected and requires a new plan.
- [ ] A repeated delivery request reuses the existing commit and pull request.
- [ ] GitHub Actions check status is visible and permanent links remain usable during API failure.
- [ ] Docker Compose starts cleanly and all health checks become healthy.
- [ ] PostgreSQL backup is created, checksum-verified and restored into a clean database.
- [ ] Retention removes expired uploads and abandoned temporary workspaces.
- [ ] GitHub OAuth secret and GitHub App private key rotation are rehearsed.
- [ ] iPhone Safari portrait and landscape flows complete without horizontal overflow.
- [ ] VoiceOver and keyboard-only flows complete login return, upload, review, approval and result navigation.
- [ ] Reverse proxy supplies TLS/HSTS and preserves the configured external origin.
- [ ] Logs are reviewed to confirm that tokens, ZIP contents and private keys are not emitted.

## Known release-candidate limitations

1. Some import/selection/delivery application state remains in memory and may be lost on backend restart.
2. Horizontal backend scaling is not supported.
3. Content-based secret scanning is not included; high-risk paths and filenames are blocked instead.
4. Controlled Actions dispatch/rerun remains phase 8 work; current artifact/error reads are read-only and bounded.
5. The service shows GitHub links but does not execute repository builds itself.

## Promotion rule

Production version `1.0.0` may be created only when every external acceptance item is checked, evidence is linked from the release record and no unresolved blocking security finding remains.


## Phase 7 resume/Work acceptance

- [ ] Create an import, reach review, sign out/in and continue without a second ZIP upload.
- [ ] Restart backend at review, after selection and after approval; continue without duplicate selection/approval/commit.
- [ ] Verify project view shows Git commits plus at most one active import, while historical imports remain available through the owner-scoped API.
- [ ] Verify another account cannot open or resume the import.
- [ ] Temporarily deny GitHub history access and verify the persisted Work-head fallback remains usable.

## Phase 7 Work-lifecycle final regression (7.24)

- [x] Cancel before approval produces no Git delivery and remains terminal after restart hydration.
- [x] Cancel after approval remains cleanup-eligible while audit metadata is retained.
- [x] Cancel after delivery is rejected.
- [x] At most one active import is exposed/accepted per Work.
- [x] Project actions are state-based: active import -> continue/cancel; idle Work -> one next-ZIP action.
- [x] Commit result exposes direct next-ZIP and finish-Work/PR actions.
- [x] PR creation has lost-response recovery that reuses an existing GitHub PR.

## Phase 8 Actions read verification

- GitHub App repository permissions include **Checks: Read-only** and **Actions: Read-only**; no Actions write permission is required for step 8.1.
- Existing installations have approved the added Actions read permission where GitHub requires re-approval.
- A test delivery shows workflow/job/check state for the exact commit and all full-detail links open on GitHub.
- `not_started` and temporary `unavailable` Actions states do not hide or block the normal Work result.
- Browser polling stops for terminal results and remains within the bounded backoff/observation policy documented in `docs/workflow-runs-and-jobs.md`.


## Phase 8 artifact/error verification (8.2)

- [x] Artifact metadata is bounded and no artifact archive bytes or authenticated archive URLs are stored/returned by zip-github.
- [x] Failed-job log input is capped before parsing and only a small bounded excerpt can reach the browser.
- [x] ANSI/control sequences and common credential/token patterns are sanitized before excerpt output.
- [x] Maven/Gradle, npm/Vite, Pandoc and xcodebuild are the initial recognized tool families; unknown formats are not guessed.
- [x] Every condensed error identifies workflow, job, failed step and a GitHub source URL.
- [x] Artifact/log detail failure does not hide the ordinary Work result or Actions status.
- [ ] Live private-repository verification confirms artifact listing and job-log redirects with a GitHub App installation token.


## Phase 8 Actions control acceptance

- [ ] GitHub App installation has Actions read/write only where controlled step-8.3 writes are intended.
- [ ] Empty dispatch/rerun allowlists expose no Actions write controls.
- [ ] An allowlisted `workflow_dispatch` succeeds only for the current active Work ref/commit.
- [ ] An allowlisted failed run can rerun failed jobs; non-failed/unlisted/other-SHA/other-branch runs are rejected.
- [ ] Duplicate requests using one idempotency key create at most one GitHub-side write and persist one owner-bound audit record.
- [ ] An old result tab becomes read-only after another import advances Work or the Work is finalized.

## Phase 9 step 9.1 gate

- [x] StagingImport lifecycle is durable and restart-safe.
- [x] Only claim-token SHA-256 is persisted; no raw claim token is stored.
- [x] Claim/promotion concurrency and idempotency rules are explicit.
- [x] Neutral stored-upload metadata can carry `100644`/`100755` without filename inference.
- [x] No anonymous staging API or GitHub authority was introduced in step 9.1.


## Phase 9 step 9.2 gate

- [x] Staging create is protected only by the dedicated deployment-scoped upload credential.
- [x] Raw claim token is returned once and only its SHA-256 is persisted.
- [x] No anonymous staging list/read/download route exists.
- [x] Capability staging-create is the only explicit CSRF exemption.

## Phase 9 step 9.3 gate

- [x] Claim token is captured from URL fragment into same-tab sessionStorage and the fragment is removed.
- [x] OAuth continuation carries only `/staging/claim`, never the claim token.
- [x] Claim requires the normal authenticated session and existing same-origin CSRF protection.
- [x] Ownership is bound atomically under a row lock and same-owner retry is idempotent.
- [x] Wrong, expired, taken and terminal claims share one neutral 410 response.
- [x] Claim creates no Project selection, ordinary Import or GitHub side effect.

## Phase 9 step 9.4 acceptance

- [x] Claimed staging state can be read only by its authenticated owner.
- [x] Project selection uses only ordinary owner-scoped active Projects.
- [x] Promotion reuses the stored ZIP and ordinary Import pipeline; no anonymous/staging-specific Git path exists.
- [x] Promotion recovery is restart-safe through a persisted non-secret staging source reference and converges on one Import.
- [x] Existing `ACTIVE_IMPORT_EXISTS`, inactive Project and Work guards remain authoritative.
- [x] Trustworthy ZIP executable metadata is captured for browser and staging uploads without filename inference.
- [x] Existing files with missing ZIP mode metadata preserve base-repository `100644`/`100755`; new files default to `100644`.
- [x] Mode-only changes are visible/reviewable and included in the immutable plan digest.
- [x] Only selected paths receive approved modes and staged Git index modes are verified before commit.


## Phase 9 step 9.5 acceptance

- [x] Browser and StagingImport reviews share one editable commit-message field; generated text is only a suggestion.
- [x] Interactive approval requires a non-empty server-normalized message capped at 500 characters and rejects unsupported control characters.
- [x] Commit message is persisted in restart-safe approval state and returned by approval recovery.
- [x] A recorded approval cannot be silently reused with a different commit message.
- [x] Delivery uses the approval-bound message and retry/restart does not regenerate it.
- [x] Legacy/internal approval data without the new field has a documented deterministic compatibility fallback.

## Phase 9.6 staging retention gate

- [x] AVAILABLE and CLAIMED have separate configurable short deadlines.
- [x] Expired/terminal staging artifacts are physically deleted with restart-safe retry markers.
- [x] Promotion and cleanup coordinate through database row locks; promoted artifacts are not staging-deleted.
- [x] Live staging object/byte quotas are enforced under serialized database accounting.
- [x] Per-capability/global rate limits remain; network-source limiting is opt-in only behind trusted forwarded headers.
- [x] Upload credential can be revoked/rotated without database migration or GitHub credential rotation.
- [x] Existing staging rows keep independent claim/TTL semantics across upload-credential rotation.
- [ ] Full Maven/JUnit/Quarkus suite to be confirmed by normal CI because sandbox Maven bootstrap was network-blocked.

## Phase 9 step 9.7 signed Shortcut gate

- [x] Authenticated release metadata/download endpoints exist and never synthesize an unsigned fallback.
- [x] `/shortcut` provides an authenticated mobile installation/update page and reports unpublished state safely.
- [x] Signed Shortcut bytes are deployment artifacts ignored by Git and mounted read-only in Compose.
- [x] Old/revoked staging credentials return the explicit `STAGING_SHORTCUT_OUTDATED` update path.
- [x] Manual trusted-Mac export/sign/publish and credential rotation procedures are documented.
- [x] A real reference Shortcut has been created in Apple Shortcuts with the documented flow and deployment credential; the operator verified its signed reference-client flow on iPhone.
- [x] The reference Shortcut has been signed for `anyone` in Apple Shortcuts and published in the deployment bundle as `shortcut/releases/zip-github.shortcut` (SHA-256 `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`).
- [x] The published signed artifact has been downloaded from `/shortcut` and accepted/imported on iOS. The served-copy gate is complete; the observed filename behavior is handled by the friendly `Content-Disposition` download name.


## Phase 9 step 9.10 final gate

- [x] Shortcut/staging upload, claim and promotion contracts are covered together by the final phase-9 gate and existing integration regressions.
- [x] Browser/stored-Upload convergence proves identical ZIP bytes reach equivalent inventory/comparison/policy/plan semantics; Git file-mode resolver covers executable preservation/defaults.
- [x] Work is activated only after remote branch readback; provisioning retry is recovery-safe and delivery refuses a missing remote Work branch.
- [x] Abandon without PR, optional remote-branch deletion, resume-existing-branch and soft project archive are implemented and owner scoped.
- [x] Work Actions status/details are exact-head-commit-bound, revisitable and reuse bounded/redacted error extraction.
- [x] Signed Shortcut was downloaded from `/shortcut` and imported on iPhone; friendly download filename, manifest hash and runtime readability are release-gated.
- [x] Old staging upload credentials are rejected immediately after rotation without GitHub credential rotation or database migration.
- [x] Operations, threat model, API contract and Shortcut release documentation reflect the completed phase-9 behavior.
- [ ] Full Maven/Quarkus and frontend Vitest suites must still be confirmed by normal CI for this revision because the sandbox cannot resolve/install all external dependencies.
