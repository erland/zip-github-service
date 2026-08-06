# GitHub-backed project configuration

## Purpose

A project is now a verified binding between one authenticated zip-github user and one repository visible through that user's GitHub App installation.

## API

### Create project

`POST /api/projects`

```json
{
  "name": "My project",
  "githubInstallationId": 123456,
  "githubRepositoryId": 987654,
  "defaultBranch": "main"
}
```

The backend verifies, in order:

1. the installation is visible to the current GitHub user;
2. the repository is returned by that user-scoped installation;
3. the selected branch exists in that repository;
4. the project name is unique for the current zip-github user.

When `defaultBranch` is omitted, the repository's GitHub default branch is used.

### Update project

`PATCH /api/projects/{projectId}` accepts the same GitHub binding fields plus `active`. Omitted values retain their current value. The complete resulting GitHub binding is revalidated before it is stored.

## Isolation rules

- The browser supplies only the opaque zip-github session cookie.
- GitHub's user access token remains server-side.
- Installation and repository identifiers supplied by the browser are never trusted without GitHub verification.
- A project is always stored with the current zip-github `ownerUserId`.
- Reading or updating another user's project returns `404 PROJECT_NOT_FOUND`.
- An invisible installation returns `404 GITHUB_INSTALLATION_NOT_FOUND`.
- A repository outside the selected visible installation returns `404 GITHUB_REPOSITORY_NOT_FOUND`.
- A missing branch returns `400 GITHUB_BRANCH_NOT_FOUND`.

## Current persistence limitation

`ProjectApplicationService` still uses an in-memory store. The Flyway/JPA model from step 1.2 already contains the required GitHub identifiers and owner-aware constraints, but repository-backed persistence is intentionally deferred to a later implementation step.
