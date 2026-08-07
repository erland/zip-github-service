# MVP RC10 correction — real GitHub login and project configuration UI

## Reason

The deployed MVP still showed the phase 1 demo project list even though the backend authentication, GitHub App catalogue and project APIs had already been implemented. This correction removes the final demo-only entry point before phase 8.

## Changes

- `AppLayout` checks `/api/auth/me`. Anonymous users receive the real GitHub OAuth login action; authenticated users see their GitHub login and can log out.
- `ProjectListPage` loads the owner-scoped project collection from `/api/projects` and has a real empty state.
- `CreateProjectPage` loads `/api/github/installations`, then the selected installation's repositories, prefills the repository default branch and creates the project through `POST /api/projects`.
- The backend remains authoritative for installation/repository visibility and branch validation.
- The obsolete `demoProjects.ts` fixture was removed from production source.
- Routing and project-creation tests were updated for the real API flow.

## Operations merge

This ZIP starts from `r0049` and also includes the later changes committed directly to `erland/zip-github-service`: backup and restore scripts resolve the project root, read only their supported keys from `.env`, preserve explicitly exported overrides, and keep destructive restore confirmation outside persistent configuration.

## Verification

Repository structure, source tracking, shell syntax, security regression and release-ledger checks are run locally. Full npm test/build could not execute in the packaging environment because its internal npm mirror still returns 404 for `yallist@3.1.1`; GitHub CI/local developer execution remains the authoritative full frontend build.
