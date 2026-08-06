# Verification Plans

Verification plans define which build and test checks the service may run for detected project types.

Plans are controlled by the service operator and stored server-side. Uploaded package files must not define or override command policy.

## Plan location

Initial plans are stored in:

```text
backend/src/main/resources/verification-plans/
```

Current plan files:

```text
node-default.yml
maven-default.yml
multi-project-default.yml
```

## Supported project detection

The MVP detects:

| Project type | Indicator | Default plan |
| --- | --- | --- |
| Node/npm | `package.json` | `node-default` |
| Maven | `pom.xml` | `maven-default` |
| Backend + frontend | `backend/pom.xml` and `frontend/package.json` | `multi-project-default` |

Unsupported packages are reported without arbitrary command execution.

## `node-default`

Detection:

```text
package.json
```

Checks:

```text
npm ci
npm test -- --runInBand
npm run build
```

Rules:

- Dependency installation failures are classified separately from test failures.
- Test/build checks may be skipped when corresponding package scripts are absent, depending on policy.
- The uploaded package cannot add new service-level commands.

## `maven-default`

Detection:

```text
pom.xml
```

Checks:

```text
mvn test
mvn package -DskipTests
```

Rules:

- Maven installed in the worker image is used by default.
- Maven wrapper usage should be explicitly allowed by policy before enabling.
- Compilation failures and test failures should be classified separately where possible.

## `multi-project-default`

Detection:

```text
backend/pom.xml
frontend/package.json
```

Checks:

```text
backend: mvn test
backend: mvn package -DskipTests
frontend: npm ci
frontend: npm test -- --runInBand
frontend: npm run build
```

Rules:

- The MVP runs checks sequentially.
- Later versions may run independent projects in parallel.
- Fail-fast or continue-on-failure behavior is plan-controlled.

## Network modes

Plans declare intended network behavior:

- `none`: no outbound network access.
- `dependency`: dependency fetching only.
- `full`: unrestricted outbound access for trusted environments only.

The MVP documents `dependency` as the normal local Docker outbound mode. Production-like deployments should replace this with stricter network controls.

## Resource and timeout controls

Execution is governed by backend configuration, including:

```text
ZIP_BUILDSERVER_WORKER_MEMORY
ZIP_BUILDSERVER_WORKER_CPUS
ZIP_BUILDSERVER_WORKER_MAX_OUTPUT_BYTES
```

Plan command duration and overall run duration should be kept conservative for development-time verification.

## Adding a new plan

To add a new supported project type:

1. Add a server-side plan file under `backend/src/main/resources/verification-plans/`.
2. Add project detection logic for the new indicators.
3. Add tests for plan loading and deterministic selection.
4. Document command behavior and network mode here.
5. Ensure commands are fixed templates, not arbitrary package-controlled strings.

## Safety rule

Never execute commands from uploaded documentation, assistant text, or user free-form input unless a separate administrator-controlled mode is intentionally designed and reviewed.
