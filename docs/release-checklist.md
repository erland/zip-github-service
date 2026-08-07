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
4. Integrated workflow/job/artifact details are phase 8 work.
5. The service shows GitHub links but does not execute repository builds itself.

## Promotion rule

Production version `1.0.0` may be created only when every external acceptance item is checked, evidence is linked from the release record and no unresolved blocking security finding remains.


## Phase 7 resume/Work acceptance

- [ ] Create an import, reach review, sign out/in and continue without a second ZIP upload.
- [ ] Restart backend at review, after selection and after approval; continue without duplicate selection/approval/commit.
- [ ] Verify project view shows Git commits plus at most one active import, while historical imports remain available through the owner-scoped API.
- [ ] Verify another account cannot open or resume the import.
- [ ] Temporarily deny GitHub history access and verify the persisted Work-head fallback remains usable.
