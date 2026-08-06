# Step 25 Regression Verification

Date: 2026-05-08

## Summary

Full regression verification was attempted from the repository root after Step 24 and the cleanup-test repair.

The available execution environment could not complete the full suite because Maven, Vitest, TypeScript dependencies, and Docker are not executable or fully installed in the extracted workspace.

No production source changes were made for this step.

## Commands attempted

### Backend tests

```bash
cd backend
mvn test
```

Result: failed to start.

```text
/bin/sh: 1: mvn: Permission denied
```

The repository does not include `mvnw`, so there was no wrapper fallback available in this environment.

### Frontend tests

```bash
cd frontend
npm test -- --run
```

Result: failed to start.

```text
> zip-buildserver-web@0.1.0 test
> vitest run --run

sh: 1: vitest: Permission denied
```

### Frontend production build

```bash
cd frontend
npm run build
```

Result: failed in this environment due to missing/unavailable local dependency type information.

Representative errors:

```text
error TS2688: Cannot find type definition file for 'node'.
src/utils/format.test.ts(1,38): error TS2307: Cannot find module 'vitest' or its corresponding type declarations.
src/pages/SessionPage.tsx(...): error TS7026: JSX element implicitly has type 'any' because no interface 'JSX.IntrinsicElements' exists.
```

These errors are consistent with an incomplete/unusable extracted `node_modules` tree in this environment.

### Docker build

```bash
docker compose build
```

Result: failed to start.

```text
/bin/sh: 1: docker: Permission denied
```

### Docker smoke check

```bash
docker compose up --abort-on-container-exit
```

Result: failed to start.

```text
/bin/sh: 1: docker: Permission denied
```

## Local verification commands

Run these from a local development machine with Maven, Node, npm, and Docker available:

```bash
cd backend
mvn test

cd ../frontend
npm ci
npm test -- --run
npm run build

cd ..
docker compose build
docker compose up --abort-on-container-exit
```

## Follow-up

If local verification finds failures, repair those failures before continuing with additional refactoring or feature work.
