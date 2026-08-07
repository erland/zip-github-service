# Changelog

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
