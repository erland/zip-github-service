# Step 0.4 report — clean zip-github baseline

## Result

Step `0.4` created a clean active application baseline. The active backend and frontend no longer contain the legacy verification session, run, command-result, artifact, verification-plan or Docker-worker implementation. The complete prior source tree remains under `legacy/zip-buildserver/` as migration reference only.

## Active baseline

- Quarkus 3 / Java 21 backend named `zip-github-backend`.
- Minimal `/api/health` endpoint and test.
- PostgreSQL/Flyway configuration with an intentionally empty clean baseline migration; target-domain tables are deferred to step `1.2`.
- React 19 / TypeScript / Vite frontend named `zip-github-web`.
- Minimal responsive home/about shell without legacy routes.
- Docker Compose with PostgreSQL, backend and frontend and **no Docker socket mount**.
- Structural verification script that rejects legacy runtime identifiers in active source.

## Verification

Passed:

- `scripts/verify-structure.sh`
- XML parsing of `backend/pom.xml`
- JSON parsing of `frontend/package.json` and `frontend/package-lock.json`
- shell syntax for active scripts
- search confirmed no `/var/run/docker.sock` in active Compose/configuration
- ZIP integrity test after packaging

Not run in this environment:

- Maven tests/build: Maven is unavailable.
- npm install/tests/build: the configured package proxy previously failed to provide a locked transitive package.
- Docker Compose build: Docker is unavailable.

These are environment limitations already documented in `docs/baseline-verification.md`, not successful build claims.

## Changed files

### Added

- clean active backend files under `backend/`
- clean active frontend files under `frontend/`
- `scripts/verify-structure.sh`
- `legacy/README.md`
- `docs/step-0.4-report.md`

### Changed

- `README.md`
- `.env.example`
- `docker-compose.yml`
- `scripts/README.md`
- `docs/implementation-status.md`

### Moved

The previous active legacy trees were moved unchanged under `legacy/zip-buildserver/`:

- previous `backend/`
- previous `frontend/`
- previous `scripts/`
- `test-fixtures/`
- `worker-images/`
- previous `docker-compose.yml`
- previous `.env.example`

### Removed

No legacy source was deleted; it was archived outside the active build path.
