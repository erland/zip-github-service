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
