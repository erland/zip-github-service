# rc.54 container image CI correction

Revision: `r0102`  
Version: `1.0.0-rc.54`  
Phase state: unchanged — 9.7 remains `BLOCKED` on the external Apple signing/iOS installation gate; 9.8 is not started.

## Failure analyzed

GitHub Actions run `31244452768`, job `93070707302` completed the prerequisite backend test/package job successfully, then failed while the backend Dockerfile executed a second Maven build. That isolated Docker build had its own empty Maven repository and Maven Central returned HTTP `429 Too Many Requests` while resolving the Quarkus BOM/plugin.

## Correction

The CI dependency graph now treats successful build outputs as immutable inputs to image assembly:

- backend job uploads `backend/target/quarkus-app/` as `backend-quarkus-app` after `mvn verify` succeeds;
- frontend job continues to upload `frontend/dist/` as `frontend-dist` after tests/build succeed;
- container job downloads both artifacts;
- `backend/Dockerfile.runtime` only assembles the verified Quarkus runtime and required Git/curl runtime dependencies;
- `frontend/Dockerfile.runtime` only assembles the verified static frontend into nginx;
- the source-building Dockerfiles remain available for local `docker compose build` workflows.

This removes duplicate dependency resolution from the image job rather than masking HTTP 429 with retries.

## Verification

- CI YAML parsed successfully.
- Runtime Dockerfiles are source-only runtime packagers and contain no Maven/npm build commands.
- `.dockerignore` rules admit only the required generated `quarkus-app` / `dist` artifact directories while retaining normal exclusions.
- Repository structure, source tracking, security regression, implementation ledger and release verification pass.
- Full GitHub-hosted image build cannot be reproduced in this sandbox; the next CI execution is the authoritative integration verification.

## Changed files

### Added
- `backend/Dockerfile.runtime`
- `frontend/Dockerfile.runtime`
- `docs/rc54-container-image-ci-correction.md`

### Modified
- `.github/workflows/ci.yml`
- `backend/.dockerignore`
- `frontend/.dockerignore`
- `VERSION`
- `CHANGELOG.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `scripts/verify-release.sh`

### Moved
- None.

### Deleted
- None.
