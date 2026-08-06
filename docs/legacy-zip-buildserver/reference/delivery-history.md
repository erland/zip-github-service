# Delivery History

Archived from `docs/agent-progress.md` during Step 24 — Archive completed delivery history.

This file preserves historical completed step logs so `docs/agent-progress.md` can remain concise and focused on current workflow state.

## Archived step log

### Step 1 — Extract frontend formatting utilities

Status: completed.

Architecture pass:

- Affected files/modules: `frontend/src/pages/RunPage.tsx`, `frontend/src/components/CommandResultTable.tsx`, and new shared utility files under `frontend/src/utils/`.
- Stable contracts: visible duration formatting remains unchanged for null/missing values, sub-second durations, and second-plus durations.
- Tests: added focused unit coverage for `formatDuration`.
- Risk: low; this is a mechanical extraction of existing frontend formatting behavior.

Changed files:

- `docs/delivery-plan.md`
- `docs/agent-progress.md`
- `frontend/src/pages/RunPage.tsx`
- `frontend/src/components/CommandResultTable.tsx`
- `frontend/src/utils/format.ts`
- `frontend/src/utils/format.test.ts`

Verification:

```bash
cd frontend
npm test
npm run build
```

Result: not passed in this environment.

- `npm test` failed because `vitest` was not executable/available in the extracted frontend dependency tree.
- `npm run build` failed because frontend dependencies and type definitions such as React, Node, TypeScript, and Vitest were not available in the extracted dependency tree.

Local verification command:

```bash
cd frontend
npm ci
npm test
npm run build
```

Known follow-ups:

- Step 2 should introduce shared frontend page-state components.


### Step 2 — Introduce shared frontend page-state components

Status: completed.

Architecture pass:

- Affected files/modules: `frontend/src/pages/SessionPage.tsx`, `frontend/src/pages/RunPage.tsx`, and new shared page-state components under `frontend/src/components/`.
- Stable contracts: route handling, data fetching, displayed loading/error/missing-resource text, and navigation links remain equivalent.
- Tests: added focused component tests for the shared page-state components.
- Risk: low; this is a presentation-only extraction with no API, routing, or data-fetching behavior changes.

Changed files:

- `docs/agent-progress.md`
- `frontend/src/components/PageState.tsx`
- `frontend/src/components/PageState.test.tsx`
- `frontend/src/pages/SessionPage.tsx`
- `frontend/src/pages/RunPage.tsx`

Verification:

```bash
cd frontend
npm test
npm run build
```

Result: not passed in this environment.

- `npm test` failed because `vitest` was not executable/available in the extracted frontend dependency tree.
- `npm run build` failed because frontend dependencies and type definitions such as React, Node, TypeScript, and Vitest were not available in the extracted dependency tree.

Local verification command:

```bash
cd frontend
npm ci
npm test
npm run build
```

Known follow-ups:

- Step 3 should add characterization tests for backend response mapping.


### Step 3 — Add characterization tests for backend response mapping

Status: completed.

Architecture pass:

- Affected files/modules: backend API resource tests for browser-facing session/run responses and assistant-facing verification responses.
- Stable contracts: no production behavior was changed; tests now pin the current response fields, summary values, command fields, failure categories, artifact references, timestamps, and assistant compact mappings.
- Tests: strengthened existing Quarkus resource tests instead of adding new infrastructure.
- Risk: low; this step only expands assertions around existing API responses.

Changed files:

- `docs/agent-progress.md`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/api/session/SessionResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/api/run/RunResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/api/assistant/AssistantVerificationResourceTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment.

- The repository does not include `mvnw`.
- The execution environment does not have `mvn` installed.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 4 should extract `RunResponseMapper` while preserving the response shapes pinned by these tests.



### Step 4 — Extract `RunResponseMapper`

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationRunService` and new mapper code under `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/mapper/`.
- Stable contracts: browser-facing run, command, and summary response shapes remain unchanged.
- Tests: existing characterization coverage from Step 3 protects the response contract.
- Risk: low-medium; this is a mapper extraction without intended behavior changes.

Changed files:

- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/mapper/RunResponseMapper.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/VerificationRunService.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run successfully in this environment because `mvn` is not installed and the repository does not include `mvnw`.


### Step 5 — Extract `AssistantResponseMapper`

Status: completed.

Architecture pass:

- Affected files/modules: `AssistantVerificationResource` and new `AssistantResponseMapper`.
- Stable contracts: assistant-facing session, run, summary, and failed-log-excerpt response shapes remain unchanged.
- Tests: existing characterization coverage from Step 3 protects the assistant response contract.
- Risk: low-medium; this keeps resource methods thin while preserving response mapping behavior.

Changed files:

- `backend/src/main/java/info/isaksson/erland/zipbuildserver/api/assistant/AssistantVerificationResource.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/mapper/AssistantResponseMapper.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run successfully in this environment because `mvn` is not installed and the repository does not include `mvnw`.


### Step 6 — Extract run summary/status logic

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationExecutionService` and new `RunStatusCalculator`.
- Stable contracts: run status precedence and summary text remain unchanged.
- Tests: added focused unit coverage for status precedence, skip decisions, and summary strings.
- Risk: medium; this changes execution internals while preserving externally visible results.

Changed files:

- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/RunStatusCalculator.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/VerificationExecutionService.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/run/RunStatusCalculatorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run successfully in this environment because `mvn` is not installed and the repository does not include `mvnw`.


### Step 7 — Add characterization tests for `NetworkMode`

Status: completed.

Architecture pass:

- Affected files/modules: `domain.model.NetworkMode`, `domain.model.verification.NetworkMode`, `VerificationPlanService`, and `VerificationRunEntity`.
- Stable contracts: network-mode enum names remain `NONE`, `DEPENDENCY`, and `FULL`; verification-plan modes map to run modes by enum name; run persistence stores the network mode as a string enum name.
- Tests: added characterization coverage before the planned deduplication in Step 8.
- Risk: low; no production behavior was changed for this step.

Changed files:

- `docs/agent-progress.md`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/domain/model/NetworkModeCharacterizationTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run successfully in this environment because `mvn` is not installed and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 8 should deduplicate `NetworkMode`.



### Step 8 — Deduplicate `NetworkMode`

Status: completed.

Architecture pass:

- Affected files/modules: verification plan model, verification plan parsing, run creation, and network mode characterization tests.
- Stable contracts: persisted run network mode remains the canonical `domain.model.NetworkMode` stored as `EnumType.STRING`; YAML plan values still parse by the same enum names.
- Tests: updated characterization coverage to use the canonical enum directly.
- Risk: medium; enum imports and conversion points were kept narrow.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/domain/model/verification/VerificationPlan.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanService.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/VerificationRunService.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanServiceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/domain/model/NetworkModeCharacterizationTest.java`

Removed files:

- `backend/src/main/java/info/isaksson/erland/zipbuildserver/domain/model/verification/NetworkMode.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not available and the repository does not include `mvnw`.

Known follow-ups:

- Step 9 should add parser characterization tests before parser extraction.


### Step 9 — Add characterization tests for verification plan parsing

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationPlanServiceTest`.
- Stable contracts: plan parsing behavior, defaults, validation messages, disabled-plan filtering, and enabled-plan sorting remain documented before extraction.
- Tests: expanded parser and plan-catalog characterization coverage.
- Risk: low; production code was not intentionally changed for this step.

Changed files:

- `docs/agent-progress.md`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanServiceTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not available and the repository does not include `mvnw`.

Known follow-ups:

- Step 10 should extract parser logic without changing the behavior covered by these tests.


### Step 10 — Extract `VerificationPlanParser`

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationPlanService` and new `VerificationPlanParser`.
- Stable contracts: `VerificationPlanService.parsePlan(...)` remains available and returns the same parsed plan model.
- Tests: existing parser characterization tests continue to cover behavior through the public service method.
- Risk: medium; parsing code moved but behavior was preserved.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanParser.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanService.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not available and the repository does not include `mvnw`.

Known follow-ups:

- Step 11 should extract validation rules out of parser construction.


### Step 11 — Extract `VerificationPlanValidator`

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationPlanParser`, new `VerificationPlanValidator`, and validator-focused tests.
- Stable contracts: `VerificationPlanService.parsePlan(...)` still reports the same validation messages for missing root fields, missing commands, missing labels, and missing command display values.
- Tests: added focused validator tests and retained parser characterization coverage through the service.
- Risk: medium; validation moved out of parser builders, so exception messages were kept unchanged.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanParser.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanValidator.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanValidatorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not available and the repository does not include `mvnw`.

Known follow-ups:

- Step 12 should extract `VerificationPlanSelector`.

### Repair — Step 2 frontend page-state test isolation

Status: completed.

Reason:

- Local frontend verification reported `PageState.test.tsx` leaving previous renders mounted between test cases.
- The missing-resource test queried for a link label that also existed in the previous error-card render.

Changed files:

- `docs/agent-progress.md`
- `frontend/src/components/PageState.test.tsx`

Verification:

```bash
cd frontend
npm test -- --run src/components/PageState.test.tsx
```

Result: not passed in this environment.

- The command could not execute because `vitest` is unavailable or not executable in the extracted dependency tree (`Permission denied` / missing executable).

Local verification command:

```bash
cd frontend
npm ci
npm test -- --run src/components/PageState.test.tsx
npm test
npm run build
```

Known follow-ups:

- Continue with Step 12 — Extract `VerificationPlanSelector` after confirming the repaired frontend test passes locally.

### Repair — Remove unused SessionPage Link import

- Fixed a TypeScript no-unused-locals error introduced during the shared page-state refactor.
- Changed files:
  - `frontend/src/pages/SessionPage.tsx`
- Verification:
  - Not run in this environment because frontend dependencies are unavailable/executable here.
  - Run locally: `cd frontend && npm ci && npm run build && npm test`
- Follow-up:
  - Next unfinished step remains Step 12 — Extract `VerificationPlanSelector`.


### Step 12 — Extract `VerificationPlanSelector`

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationPlanService`, new `VerificationPlanSelector`, and focused selector tests.
- Stable contracts: `VerificationPlanService.selectPlan(...)` keeps the same public API and selection behavior.
- Tests: added direct selector coverage for matching, default selection reason, and no-match behavior.
- Risk: medium; this separates selection logic while preserving plan catalog filtering and sorting in the service.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanSelector.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanService.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanSelectorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not available and the repository does not include `mvnw`.

Known follow-ups:

- Step 13 should consider whether to replace the hand-rolled YAML-like parser.


### Step 13 — Consider replacing the hand-rolled YAML-like parser

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationPlanParser`, parser characterization tests, and verification plan format reference documentation.
- Stable contracts: existing built-in verification plan files continue to load; quoted values, inline comments, defaults, command ordering, and validation remain supported.
- Tests: expanded parser characterization coverage for quoted `#` characters and clearer invalid-input failures.
- Risk: medium; this step intentionally avoided adding a general YAML dependency and instead tightened the explicit supported format to reduce parser ambiguity and deserialization risk.

Decision:

- Kept an explicit small-format parser rather than introducing a general YAML parser.
- Added line-numbered failures for unsupported root keys, unsupported command keys, invalid booleans, invalid integers, and invalid enum values.
- Made comment stripping quote-aware so `#` inside quoted scalar values is preserved.
- Documented the supported verification plan format in `docs/reference/verification-plan-format.md`.

Changed files:

- `docs/agent-progress.md`
- `docs/reference/verification-plan-format.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanParser.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/verification/VerificationPlanServiceTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run successfully in this environment because Maven is not installed and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 14 should add characterization tests for Docker command construction.


### Step 14 — Add characterization tests for Docker command construction

Status: completed.

Architecture pass:

- Affected files/modules: `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerCommandExecutorTest.java`.
- Stable contracts: Docker CLI argument order, resource limit flags, container name, network mode, workspace volume mount, container working directory, worker image, shell invocation, and command display are now explicitly characterized.
- Tests: expanded existing Docker command construction tests without requiring a Docker daemon.
- Risk: low; this step adds characterization tests only and does not change production code.

Changed files:

- `docs/agent-progress.md`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerCommandExecutorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because `mvn` is not installed/executable and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 15 should extract `DockerRunCommandBuilder` while preserving the command behavior pinned by these tests.



### Step 15 — Extract `DockerRunCommandBuilder`

Status: completed.

Architecture pass:

- Affected files/modules: Docker worker command construction in `DockerCommandExecutor`, new `DockerRunCommandBuilder`, and Docker command characterization tests.
- Stable contracts: Docker CLI argument order, resource limit arguments, workspace volume mapping, container working directory mapping, worker image, and shell invocation remain unchanged.
- Tests: moved Step 14 command-construction characterization coverage to target `DockerRunCommandBuilder` directly.
- Risk: medium; this isolates Docker command construction while leaving process execution, output capture, timeout handling, and cleanup behavior unchanged.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerCommandExecutor.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerRunCommandBuilder.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerRunCommandBuilderTest.java`

Removed files:

- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerCommandExecutorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment.

- Maven is not installed/executable and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 16 should extract `DockerWorkspacePathMapper`.

### Step 16 — Extract `DockerWorkspacePathMapper`

Status: completed.

Architecture pass:

- Affected files/modules: `DockerRunCommandBuilder`, new `DockerWorkspacePathMapper`, and Docker path-mapping tests.
- Stable contracts: Docker volume mount host path mapping and container working-directory mapping remain unchanged.
- Tests: moved path-specific characterization coverage to the extracted mapper and kept builder coverage for the full Docker command shape.
- Risk: medium; path mapping is execution-sensitive, so the extraction keeps behavior identical and scoped.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerRunCommandBuilder.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerWorkspacePathMapper.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerRunCommandBuilderTest.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerWorkspacePathMapperTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because `mvn` is not installed and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 17 should extract bounded process output capture.

### Step 17 — Extract bounded process output capture

Status: completed.

Architecture pass:

- Affected files/modules: `DockerCommandExecutor`, new `BoundedProcessOutputCollector`, and focused worker/docker tests.
- Stable contracts: Docker execution result status, stdout/stderr capture, truncation marker text, timeout handling, and error messages remain equivalent.
- Tests: added focused collector coverage for small, oversized, unfinished, read-error, and zero-byte output cases.
- Risk: medium; this touches process-output handling used by Docker verification commands.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/BoundedProcessOutputCollector.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerCommandExecutor.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/BoundedProcessOutputCollectorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not installed and the repository does not include `mvnw`.

Additional targeted verification:

```bash
javac --release 21
```

Result: passed for the affected production classes using local annotation stubs for Jakarta and MicroProfile annotations.

Known follow-ups:

- Step 18 should extract container cleanup behavior.

### Step 18 — Extract container cleanup behavior

Status: completed.

Architecture pass:

- Affected files/modules: `DockerCommandExecutor`, new `DockerContainerCleanup`, and focused worker/docker tests.
- Stable contracts: timed-out and interrupted Docker executions still attempt `docker rm -f <containerName>` cleanup and still ignore cleanup failures.
- Tests: added focused cleanup tests for command construction, bounded wait behavior, ignored start failures, and interrupted cleanup waits restoring the interrupted flag.
- Risk: low to medium; cleanup is operationally important but extraction preserves the same Docker command and wait timeout.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerCommandExecutor.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerContainerCleanup.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/worker/docker/DockerContainerCleanupTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because Maven is not installed and the repository does not include `mvnw`.

Additional targeted verification:

```bash
javac --release 21
```

Result: passed for the affected production classes using local annotation stubs for Jakarta and MicroProfile annotations.

Known follow-ups:

- Step 19 should extract artifact persistence.

### Step 19 — Extract artifact persistence

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationExecutionService`, new `CommandArtifactService`, and focused artifact service tests.
- Stable contracts: stdout/stderr artifact types, command labels, run associations, and command-result artifact references remain unchanged.
- Tests: added focused unit coverage for storing stdout/stderr artifacts and attaching their IDs to command results.
- Risk: medium; artifact persistence remains delegated to the existing `ArtifactStorageService`.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/CommandArtifactService.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/VerificationExecutionService.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/run/CommandArtifactServiceTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because `mvn` is not installed and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 20 should extract command result persistence from execution orchestration.

### Step 20 — Extract command result persistence

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationExecutionService`, new `CommandResultPersister`, and focused command-result persistence tests.
- Stable contracts: command labels, working directories, command display values, statuses, exit codes, timings, log excerpts, failure classifications, and output artifact references remain unchanged.
- Tests: added focused unit coverage for successful, failed, timed-out, and skipped command-result persistence.
- Risk: medium; persistence details were moved out of execution orchestration while preserving repository and artifact-storage behavior.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/CommandResultPersister.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/VerificationExecutionService.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/run/CommandResultPersisterTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because `mvn` is not installed and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 21 should introduce a `RunExecutor` coordinator.



### Step 21 — Introduce `RunExecutor` coordinator

Status: completed.

Architecture pass:

- Affected files/modules: `VerificationExecutionService`, new `RunExecutor`, and focused backend tests for run execution orchestration.
- Stable contracts: `VerificationExecutionService.execute(...)` remains the public entry point and delegates to the new coordinator.
- Tests: added unit coverage for the extracted coordinator to preserve command execution, project path substitution, fallback project behavior, status/summary updates, initial persistence, workspace cleanup, and skipped command persistence after failure.
- Risk: medium to high; this step moved the high-level run execution flow but kept existing collaborators and behavior unchanged.

Changed files:

- `docs/agent-progress.md`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/RunExecutor.java`
- `backend/src/main/java/info/isaksson/erland/zipbuildserver/application/run/VerificationExecutionService.java`
- `backend/src/test/java/info/isaksson/erland/zipbuildserver/application/run/RunExecutorTest.java`

Verification:

```bash
cd backend
mvn test
```

Result: not run in this environment because `mvn` is not installed and the repository does not include `mvnw`.

Local verification command:

```bash
cd backend
mvn test
```

Known follow-ups:

- Step 22 should document the API contract synchronization strategy.

### Step 22 — Document API contract synchronization strategy

Status: completed.

Architecture pass:

- Affected files/modules: documentation under `docs/reference/`.
- Stable contracts: no application source, API DTO, frontend type, routing, or runtime behavior changes were made.
- Tests: no code tests were added because this is a documentation-only step.
- Risk: low; the change records the current manual synchronization strategy and names the files to review when API contracts change.

Changed files:

- `docs/agent-progress.md`
- `docs/reference/api-contract-sync.md`

Verification:

Documentation-only step. No automated verification was required or run.

Local verification command for future API contract changes:

```bash
cd backend
mvn test
cd ../frontend
npm test
npm run build
```

Known follow-ups:

- Step 23 should evaluate generated TypeScript API types from the backend OpenAPI output.


### Step 23 — Evaluate generated TypeScript API types

Status: completed.

Architecture pass:

- Affected files/modules: documentation under `docs/reference/`.
- Stable contracts: no application source, generated files, frontend API types, API clients, backend DTOs, or build scripts were changed.
- Tests: no code tests were added because this is a documentation-only evaluation step.
- Risk: low; the step documents the decision to defer generated TypeScript API types rather than introducing tooling churn.

Decision:

- Generated TypeScript API types are deferred.
- The active strategy remains manual frontend API types in `frontend/src/api/types.ts` with the synchronization checklist in `docs/reference/api-contract-sync.md`.
- Future generated-type adoption requires a dedicated step that defines the generator, OpenAPI input source, output location, migration boundary, and stale-output verification.

Changed files:

- `docs/agent-progress.md`
- `docs/reference/api-contract-sync.md`
- `docs/reference/generated-types-evaluation.md`

Verification:

Documentation-only step. No automated verification was required or run.

Local verification command for future API contract or generated-type tooling changes:

```bash
cd backend
mvn test
cd ../frontend
npm test
npm run build
```

Known follow-ups:

- Step 24 should archive completed delivery history.
