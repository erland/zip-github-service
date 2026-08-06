# zip-github

`zip-github` is a planned multi-user service for safely reviewing a project ZIP against an authorized GitHub repository and delivering approved changes through an import branch and pull request. GitHub remains the persistent source of truth and GitHub Actions performs project-specific builds, tests and publishing.

## Current state

- Current revision: `r0005`
- Completed implementation step: `0.4 — Skapa ren zip-github-bas`
- Next implementation step: `1.1 — Definiera domänmodell och statusmaskiner`
- Status register: [`docs/implementation-status.md`](docs/implementation-status.md)
- Latest step report: [`docs/step-0.4-report.md`](docs/step-0.4-report.md)

The active `backend/` and `frontend/` trees are now clean product shells. Legacy zip-buildserver source is retained under `legacy/zip-buildserver/` for selective reference and is outside the active build/runtime path.

## Continue implementation

Attach the latest ZIP and write:

> Kör nästa steg.

`AGENTS.md` and the status ledger require exactly one step to be implemented, verified, documented and repackaged per prompt.

## Active structure

- `backend/` — clean Quarkus/Java 21 shell
- `frontend/` — clean React/TypeScript/Vite shell
- `docs/` — specifications, plans, status and step evidence
- `scripts/` — active project verification helpers
- `legacy/zip-buildserver/` — archived migration reference, not active runtime code
## Continuous integration

The project includes `.github/workflows/ci.yml`. It runs structure/security checks, backend Maven verification on Java 21, and frontend tests/build on Node.js 22. Use `backend/mvnw verify` for the same pinned backend build locally. See `docs/ci-baseline.md`.

