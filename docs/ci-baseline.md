# CI baseline

The project has one GitHub Actions workflow at `.github/workflows/ci.yml`.

## Triggers and permissions

The workflow runs on push, pull request and manual `workflow_dispatch`. Its repository permission is limited to `contents: read`; it does not need GitHub credentials, application secrets, write access or access to uploaded project ZIP contents.

## Jobs

### Structure and security checks

- runs `scripts/verify-structure.sh`;
- validates shell syntax, including `backend/mvnw`;
- verifies that the implementation ledger has exactly one `NEXT` step and that Current position matches the ledger;
- checks that generated dependency/build directories are not tracked.

### Backend tests and package

- uses Temurin Java 21;
- caches Maven dependencies;
- bootstraps pinned Maven 3.9.11 through `backend/mvnw`;
- runs `./mvnw --batch-mode --no-transfer-progress verify`;
- uploads Surefire/Failsafe reports when present.

GitHub-hosted Ubuntu runners provide the Docker runtime required by Testcontainers. The database migration test is therefore expected to run in CI even when it is skipped in a local environment without Docker.

### Frontend tests and build

- uses Node.js 22;
- caches npm dependencies from `frontend/package-lock.json`;
- runs `npm ci`, `npm test` and `npm run build`;
- uploads `frontend/dist` as a temporary workflow artifact.

## Maven bootstrap

`backend/mvnw` and `backend/mvnw.cmd` are lightweight project-local bootstrap scripts. They read the pinned distribution URL from `.mvn/wrapper/maven-wrapper.properties`, download Maven into the user's wrapper cache on first use and execute the project's `pom.xml`. No Maven binary or wrapper JAR is committed.

## Branch protection recommendation

When `erland/zip-github-service` is created, require the three CI jobs before pull requests can be merged. Do not allow the service's ZIP-import path to modify `.github/**` during MVP; changes to this workflow must continue through ordinary reviewed Git development.
