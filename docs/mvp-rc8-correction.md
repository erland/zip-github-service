# MVP RC8 operations correction — r0048

Version `1.0.0-rc.8` adds reproducible container-image publication and image-based server deployment before phase 8.

## Changes

- CI now builds backend and frontend Docker images only after structure, backend and frontend jobs pass.
- Successful `main`/tag runs publish both images to GHCR using `GITHUB_TOKEN` and job-scoped `packages: write`.
- RC images receive exact version, source-SHA and mutable `rc` tags; stable versions additionally receive major/minor and `latest` tags.
- `docker-compose.yml` now consumes published GHCR images.
- `docker-compose.build.yml` preserves local build-from-source operation.
- Frontend Docker builds use `npm ci`.
- Backend Docker builds invoke the repository Maven Wrapper.
- `.dockerignore` files reduce and harden both Docker build contexts.
- Server installation, upgrade, rollback and private-GHCR authentication are documented.

No phase 8 implementation step was consumed; `8.1` remains the single `NEXT` step.
