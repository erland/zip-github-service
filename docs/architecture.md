# MVP architecture

## Purpose

zip-github accepts a user-provided project ZIP, compares it with an authorized GitHub repository at an exact commit, presents an immutable review plan and delivers only the approved changes through a new branch and draft pull request.

GitHub remains the persistent source of truth. The service does not build arbitrary uploaded projects and does not mount the Docker socket. Target repositories use their own GitHub Actions workflows for compilation, tests and publishing.

## Components

### Browser frontend

The React/Vite frontend provides:

- GitHub login entry point;
- project and repository selection;
- mobile-friendly ZIP upload;
- immutable import-plan review and approval;
- delivery progress and result links;
- basic GitHub check status;
- import history and reopening.

Credentialed requests use the server session cookie. State-changing requests also carry the required request marker and are protected by exact-origin checks.

### Quarkus backend

The Java 21 backend owns:

- OAuth callback and server-side session handling;
- GitHub App installation-token creation;
- authorization and user ownership checks;
- upload streaming and retention;
- ZIP inspection, normalization and hashing;
- frozen repository snapshots;
- comparison, policy and immutable plan creation;
- exact approval records;
- isolated Git workspace preparation;
- branch, commit, push and draft-PR delivery;
- idempotency, retry classification and result metadata;
- check-status aggregation and import history.

### PostgreSQL

Flyway migrations define the intended persistent data model. The MVP release candidate still has several application services backed by in-memory stores. Until those stores are replaced, a backend restart loses those runtime records and horizontal scaling is unsupported.

### GitHub

Two separate GitHub integrations are used:

- OAuth identifies the human user and confirms their repository access.
- A GitHub App provides short-lived installation tokens for repository operations.

The service never exposes installation tokens to the frontend and does not embed them in Git remote URLs.

## Main trust boundaries

1. Browser to backend API.
2. Backend to local upload and workspace storage.
3. Backend to PostgreSQL.
4. Backend to GitHub OAuth and GitHub App APIs.
5. GitHub repository to repository-owned GitHub Actions.

The detailed threat model is documented in `threat-model.md`.

## Import data flow

1. User signs in with GitHub.
2. User selects an authorized repository and base branch.
3. User creates an import and uploads a ZIP.
4. Backend validates and inventories the archive.
5. Backend freezes the selected branch to an exact commit SHA.
6. ZIP and repository inventories are compared by SHA-256.
7. Policy produces a deterministic immutable import plan.
8. User approves the exact plan digest.
9. Backend prepares an isolated Git workspace from the frozen SHA.
10. Only approved added or modified files are applied and verified.
11. Backend creates one branch and one commit and pushes without force.
12. Backend creates or reuses a draft pull request.
13. Frontend displays permanent GitHub links and basic check status.

## Deployment shape

The documented MVP deployment uses:

- one frontend container;
- one backend container;
- one PostgreSQL instance;
- persistent upload/workspace volumes where required;
- reverse-proxy TLS termination;
- no Docker socket mount.

See `operations.md` and `configuration-reference.md`.
