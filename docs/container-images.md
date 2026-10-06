# Container images and GHCR publication

## Published images

The CI workflow builds two application images after structure, backend and frontend jobs succeed:

- `ghcr.io/erland/zip-github-service-backend`
- `ghcr.io/erland/zip-github-service-frontend`

PostgreSQL continues to use the official `postgres:16-alpine` image.

## When images are published

Every pull request and branch push builds both Dockerfiles, which makes image construction part of CI. Normal CI pushes images only for successful runs on `main`. Published GitHub Releases use the dedicated release workflow, which checks out the release tag and publishes both application images from that exact tagged source. The container jobs have `packages: write`; test jobs retain read-only repository permissions.

## Tags

For normal `main` CI, image versions continue to come from `VERSION`.

For a published GitHub Release, the release tag is authoritative. A tag such as `v1.2.3` or `1.2.3` produces application version `1.2.3`. The release workflow verifies that the tagged commit's `VERSION` contains the same normalized version and fails before publication if they differ.

Each release publish receives:

- exact version derived from the GitHub Release tag;
- immutable source tag `sha-<12-character-commit>`;
- mutable `rc` tag for release-candidate versions.

A stable release such as `1.2.3` additionally receives `1.2`, `1`, and `latest`. `latest` is never assigned to an RC release. Production/server Compose should normally pin the exact release version rather than a mutable convenience tag.

## Registry authentication

GitHub Actions publishes with the repository `GITHUB_TOKEN`; no long-lived package secret is required in CI. The workflow job requests `contents: read` and `packages: write`.

For a server:

- public packages require no registry login;
- private packages require `docker login ghcr.io` with a GitHub token allowed to read the package (`read:packages`).

Package visibility is controlled in GitHub package settings and is independent of application OAuth/GitHub App credentials. Do not reuse the service's GitHub App private key for registry login.

## Server deployment

`docker-compose.yml` uses the published images and selects them with:

```dotenv
ZIP_GITHUB_VERSION=1.0.0-rc.8
ZIP_GITHUB_BACKEND_IMAGE=ghcr.io/erland/zip-github-service-backend
ZIP_GITHUB_FRONTEND_IMAGE=ghcr.io/erland/zip-github-service-frontend
```

Deploy or upgrade with:

```bash
docker compose pull
docker compose up -d
```

Rollback by setting `ZIP_GITHUB_VERSION` to a previous version and repeating those commands, subject to database migration compatibility.

## Local builds

Local source builds remain available through the Compose override:

```bash
docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build
```

The backend Dockerfile invokes the repository Maven Wrapper. The frontend Dockerfile uses `npm ci`, so image builds use the same locked dependencies as CI.
