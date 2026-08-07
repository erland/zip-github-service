# zip-github

`zip-github` is an MVP release-candidate multi-user service for safely reviewing a project ZIP against an authorized GitHub repository and delivering approved changes through an import branch and pull request. GitHub remains the persistent source of truth and GitHub Actions performs project-specific builds, tests and publishing.

## Current state

- Product version: `1.0.0-rc.8`
- Repository revision: `r0048`
- Completed implementation step: `7.5 — MVP-release och Definition of Done`
- Next implementation step: `8.1 — Workflow runs och jobs` (post-MVP)
- Overall state: **MVP RELEASE CANDIDATE**
- Status register: [`docs/implementation-status.md`](docs/implementation-status.md)
- Release decision: [`docs/mvp-release.md`](docs/mvp-release.md)
- Production acceptance checklist: [`docs/release-checklist.md`](docs/release-checklist.md)

The end-to-end MVP flow is implemented: GitHub login, repository selection, safe ZIP upload, frozen repository snapshot, immutable review and approval, isolated delivery, draft pull request, result links, check status and import history.

The release candidate is intended for controlled deployment and live acceptance testing. It is not yet production-ready because several application stores remain in memory and the external checks in `docs/release-checklist.md` still require evidence.

## Continue implementation

Attach the latest ZIP and write:

> Kör nästa steg.

`AGENTS.md` and the status ledger require exactly one step to be implemented, verified, documented and repackaged per prompt.

## Architecture and user flow

- Architecture: [`docs/architecture.md`](docs/architecture.md)
- MVP release notes: [`docs/mvp-release.md`](docs/mvp-release.md)
- Changelog: [`CHANGELOG.md`](CHANGELOG.md)

## Active structure

- `backend/` — clean Quarkus/Java 21 shell
- `frontend/` — clean React/TypeScript/Vite shell
- `docs/` — specifications, plans, status and step evidence
- `scripts/` — active project verification helpers
- `legacy/zip-buildserver/` — archived migration reference, not active runtime code
## Continuous integration

The project includes `.github/workflows/ci.yml`. It runs structure/security checks, backend Maven verification on Java 21, frontend tests/build on Node.js 22, then builds both runtime container images. Successful `main` builds publish versioned images to GitHub Container Registry (GHCR); pull requests build the images without publishing them. Use `backend/mvnw verify` for the same pinned backend build locally. See `docs/ci-baseline.md` and `docs/container-images.md`.


## Operations

- Server/container topology using published GHCR images: [`docker-compose.yml`](docker-compose.yml)
- Local image build override: [`docker-compose.build.yml`](docker-compose.build.yml)
- Operations, backup, retention and incidents: [`docs/operations.md`](docs/operations.md)
- GitHub OAuth/App configuration: [`docs/github-app-setup.md`](docs/github-app-setup.md)
- Configuration reference: [`docs/configuration-reference.md`](docs/configuration-reference.md)

The runtime never mounts the Docker socket. GitHub Actions remains responsible for target-project builds and tests.
