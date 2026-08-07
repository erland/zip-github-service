# MVP release checklist

This checklist separates repository-complete controls from environment-dependent acceptance tests. The project may be tagged as `1.0.0-rc.4` when the repository controls pass. It must not be promoted to a production-ready `1.0.0` until all external checks are completed and recorded.

## Repository controls

- [x] All implementation steps 0.1–7.5 are completed and documented.
- [x] Exactly one post-MVP step is marked `NEXT`.
- [x] Backend build and tests are defined in GitHub Actions.
- [x] Frontend tests and production build are defined in GitHub Actions.
- [x] Structure and security regression scripts are part of CI.
- [x] ZIP traversal, symlink, special-file and resource-limit controls exist.
- [x] Repository comparison is locked to an exact base commit SHA.
- [x] Review and approval are bound to an immutable plan digest.
- [x] Delivery creates one non-force-pushed branch and atomic commit.
- [x] Draft pull request creation is idempotent and recoverable.
- [x] CSRF, restricted CORS, security headers and rate limiting are present.
- [x] Docker Compose does not mount the Docker socket.
- [x] Operations, backup, restore, incident and GitHub App setup are documented.
- [x] Version, changelog, architecture and release notes are present.

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

1. Several application stores remain in memory and are lost on backend restart.
2. Horizontal backend scaling is not supported.
3. Content-based secret scanning is not included; high-risk paths and filenames are blocked instead.
4. Integrated workflow/job/artifact details are phase 8 work.
5. The service shows GitHub links but does not execute repository builds itself.

## Promotion rule

Production version `1.0.0` may be created only when every external acceptance item is checked, evidence is linked from the release record and no unresolved blocking security finding remains.
