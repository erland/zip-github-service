# MVP RC9 correction

Revision `r0049` / version `1.0.0-rc.9` corrects the backend container build introduced in RC8.

## Root cause

The backend Docker build stage already used `maven:3.9.11-eclipse-temurin-21`, but still invoked `./mvnw`. The project wrapper bootstraps Maven by downloading and unpacking it, and the Maven base image did not contain the `unzip` utility required by that bootstrap script. GitHub Actions therefore failed with `unzip is required to bootstrap Maven`.

## Correction

The backend Dockerfile now runs `mvn --batch-mode --no-transfer-progress package -DskipTests` directly. This uses the Maven 3.9.11 binary that is already present in the pinned build image. Host and CI verification continue to use `./mvnw verify`, so the normal test path remains wrapper-based.

## Changed files

- `backend/Dockerfile`
- `VERSION`
- `CHANGELOG.md`
- `docs/implementation-status.md`
- `docs/mvp-rc9-correction.md`
- `scripts/verify-release.sh`

No runtime Java source code changed.
