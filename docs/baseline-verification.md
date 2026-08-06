# Baseline verification — legacy zip-buildserver

Step: `0.2`  
Date: 2026-08-06  
Repository revision: `r0003`

## Purpose

Establish a reproducible technical baseline for the unpacked legacy project before any product-domain migration begins. This report records required tool versions, attempted verification commands, results, and environment limitations.

## Detected toolchain

| Tool | Project requirement / source | Available in verification environment | Result |
|---|---|---:|---|
| Java | Java 21, from `backend/pom.xml` (`maven.compiler.release=21`) | OpenJDK `21.0.10` | Compatible |
| Maven | Maven build; backend Dockerfile uses Maven `3.9.9` | Not installed | Backend tests/build not executable directly |
| Node.js | `>=20`, from `frontend/package.json` | `v22.16.0` | Compatible |
| npm | Lockfile v3 | `10.9.2` | Compatible in principle |
| Docker / Docker Compose | Required by legacy end-to-end and worker-image flow | Not installed | Container and E2E verification unavailable |
| Python | Used by legacy verification scripts | Available | Supporting scripts usable |

## Version guidance

The legacy baseline is expected to use:

- Java 21.
- Maven 3.9.x; the backend Dockerfile specifically uses Maven 3.9.9.
- Node.js 20 or newer.
- npm compatible with lockfile version 3.
- Docker with Docker Compose only for the legacy worker/E2E path.

The project does not contain Maven Wrapper, `.nvmrc`, or `.node-version`. For the baseline, versions are documented here rather than changing legacy build mechanics. The clean `zip-github` base in step `0.4` should add explicit wrappers/version files as appropriate.

## Verification attempts

### Backend tests

Planned command:

```bash
cd backend
mvn test
```

Result: **not run**.

Reason: `mvn` is not installed in the available environment, and the project has no Maven Wrapper. Docker could not be used as a fallback because Docker is also unavailable.

This is an environment limitation. It is not evidence that backend tests pass or fail.

### Backend production build

Planned command:

```bash
cd backend
mvn -B -ntp package -DskipTests
```

Alternative legacy path:

```bash
docker build -t zip-buildserver-backend ./backend
```

Result: **not run** for the same Maven/Docker limitations.

### Frontend dependency installation

Command:

```bash
cd frontend
npm ci
```

Result: **failed before tests/build started**.

Observed error:

```text
npm ERR! code E404
npm ERR! 404 Not Found ... yallist-3.1.1.tgz
```

The configured internal npm proxy did not provide `yallist@3.1.1`. The lockfile itself references the public npm registry, but the execution environment rewrote retrieval through its internal package gateway.

This is classified as a dependency-registry/environment failure, not a confirmed frontend code or test failure.

### Frontend tests

Planned command:

```bash
cd frontend
npm test
```

Result: **not run**, because `npm ci` did not complete and `node_modules` was absent.

### Frontend production build

Planned command:

```bash
cd frontend
npm run build
```

Result: **not run**, because dependency installation did not complete.

### Legacy Docker end-to-end verification

Planned command:

```bash
./scripts/verify-local.sh
```

Result: **not run**.

Reason: Docker is unavailable. The script explicitly requires Docker and builds worker images before starting the Compose stack.

### Static shell-script verification

Command:

```bash
for f in scripts/*.sh; do bash -n "$f"; done
```

Result: **passed** for all shell scripts under `scripts/`.

### Input ZIP integrity

Command:

```bash
python3 -m zipfile -t zip-github-r0002-step-0.1.zip
```

Result: **passed**.

## Reproducible verification on a fully equipped machine

From the repository root:

```bash
java -version
mvn -version
node --version
npm --version
docker --version
docker compose version

cd backend
mvn test
mvn -B -ntp package -DskipTests

cd ../frontend
npm ci
npm test
npm run build

cd ..
./scripts/verify-local.sh
```

Expected prerequisite versions:

```text
Java: 21
Maven: 3.9.x
Node.js: >=20
npm: lockfile-v3 compatible
Docker: recent version with Compose v2
```

## Baseline conclusion

The legacy codebase has identifiable and documented build paths for backend, frontend, and Docker-based end-to-end verification. In the current environment:

- toolchain requirements were identified;
- shell scripts were syntax-checked successfully;
- the source ZIP passed integrity verification;
- backend execution was blocked by missing Maven and Docker;
- frontend execution was blocked by an unavailable transitive package in the environment's npm proxy.

No source-code changes were made to work around these environment limitations. This preserves the legacy baseline for the reuse assessment in step `0.3`.
