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
