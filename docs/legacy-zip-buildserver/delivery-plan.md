# Refactoring Implementation Plan

Generated for the uploaded `digest.zip` repository.

## Purpose

This plan converts the refactoring analysis into an incremental, testable implementation sequence. It is designed to be copied into `docs/delivery-plan.md` or used as a standalone roadmap.

## Ground rules

- Implement one step at a time.
- Preserve existing behavior unless a step explicitly changes it.
- Add characterization tests before changing high-risk behavior.
- Avoid unrelated cleanup.
- Keep each step reviewable.
- Run verification after each step.
- Update `docs/agent-progress.md` after each completed step if using the repository's AGENTS workflow.

## Suggested default verification commands

Use the commands that match the repository environment.

```bash
# Backend
./mvnw test

# Frontend
cd frontend
npm install
npm test
npm run build

# Full container smoke check, when Docker is available
docker compose build
docker compose up --abort-on-container-exit
```

If these commands differ from the repository scripts, prefer the commands already documented in the repository.

---

# Phase 1 — Low-risk cleanup and test scaffolding

## Step 1 — Extract frontend formatting utilities

### Goal

Move page-local formatting helpers into shared frontend utilities.

### Scope

- Move `formatDuration` from `frontend/src/pages/RunPage.tsx` into a shared utility module, for example:
  - `frontend/src/utils/format.ts`
- Update imports in affected components.
- Add or update tests for formatting behavior.

### Acceptance criteria

- `RunPage.tsx` no longer owns generic formatting helpers.
- Duration formatting behavior remains unchanged.
- Utility has focused tests for null, short, long, and boundary durations.

### Suggested tests

- `formatDuration(undefined)` or equivalent missing value handling.
- Seconds-only duration.
- Minute-plus duration.
- Hour-plus duration if currently supported.

### Verification

```bash
cd frontend
npm test
npm run build
```

### Risk

Low.

---

## Step 2 — Introduce shared frontend page-state components

### Goal

Reduce repeated loading, error, missing-resource, and empty-state markup across pages.

### Scope

Create small shared components such as:

- `LoadingCard`
- `ErrorCard`
- `MissingResourceCard`
- optionally `PageCard`

Update only the most obvious duplicated usages in:

- `frontend/src/pages/SessionPage.tsx`
- `frontend/src/pages/RunPage.tsx`

### Acceptance criteria

- Existing page behavior and text remain equivalent.
- Shared components are simple and presentation-focused.
- No routing or data-fetching behavior changes.

### Suggested tests

- Existing page tests continue to pass.
- Add component tests only if frontend test setup already supports them cleanly.

### Verification

```bash
cd frontend
npm test
npm run build
```

### Risk

Low.

---

## Step 3 — Add characterization tests for backend response mapping

### Goal

Lock in current API response shapes before extracting mapper classes.

### Scope

Add or strengthen tests around browser-facing and assistant-facing responses, especially:

- session response fields,
- run response fields,
- command response fields,
- summary/status fields,
- missing/failed states.

### Acceptance criteria

- Tests document current response shape.
- Tests fail if important response fields are dropped or renamed.
- No production behavior changes.

### Suggested tests

Use existing resource/service tests where possible instead of introducing broad new infrastructure.

### Verification

```bash
./mvnw test
```

### Risk

Low.

---

# Phase 2 — API and service responsibility cleanup

## Step 4 — Extract `RunResponseMapper`

### Goal

Move browser API DTO/entity mapping out of service/resource classes.

### Scope

Create a mapper class such as:

- `backend/src/main/java/.../application/mapper/RunResponseMapper.java`

Move existing mapping logic for run, command, artifact, and summary responses into the mapper.

### Acceptance criteria

- Service classes no longer contain browser response mapping details.
- API response payloads remain unchanged.
- Existing tests pass without expectation changes.

### Suggested tests

- Existing response-shape tests from Step 3 should cover this.
- Add mapper unit tests if mapping has complex conditional behavior.

### Verification

```bash
./mvnw test
```

### Risk

Low to medium.

---

## Step 5 — Extract `AssistantResponseMapper`

### Goal

Move assistant API response mapping out of assistant resource classes.

### Scope

Create a mapper class such as:

- `backend/src/main/java/.../application/mapper/AssistantResponseMapper.java`

Move mappings for assistant session, run, command, and status responses.

### Acceptance criteria

- Assistant resource class becomes thinner.
- Assistant API response format remains unchanged.
- Existing assistant API tests pass unchanged.

### Suggested tests

- Assistant API response tests covering success and error-like states.
- Mapper tests for any special assistant-specific formatting.

### Verification

```bash
./mvnw test
```

### Risk

Low to medium.

---

## Step 6 — Extract run summary/status logic

### Goal

Separate summary/status derivation from orchestration services.

### Scope

Create one or both:

- `RunSummaryService`
- `RunStatusCalculator`

Move logic that derives final run state, command counts, skipped/failed summaries, and display summaries.

### Acceptance criteria

- `VerificationRunService` and/or `VerificationExecutionService` delegate summary/status calculation.
- Existing run status semantics are unchanged.
- Tests cover success, failure, timeout, skipped, and partial-result scenarios.

### Suggested tests

- All commands passed.
- One command failed.
- One command timed out.
- Commands skipped after failure if that is current behavior.
- No commands available.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

# Phase 3 — Domain model consolidation

## Step 7 — Add characterization tests for `NetworkMode`

### Goal

Protect current behavior before removing duplicate network-mode definitions.

### Scope

Add tests around:

- verification plan parsing,
- run creation,
- execution configuration,
- API serialization if applicable,
- persistence if applicable.

### Acceptance criteria

- Tests document how network modes are parsed, stored, and exposed.
- No production behavior changes.

### Verification

```bash
./mvnw test
```

### Risk

Low.

---

## Step 8 — Deduplicate `NetworkMode`

### Goal

Converge on one canonical network-mode enum.

### Scope

Recommended canonical type:

- `backend/src/main/java/.../domain/model/NetworkMode.java`

Remove or replace:

- `backend/src/main/java/.../domain/model/verification/NetworkMode.java`

Update imports and mapping code accordingly.

### Acceptance criteria

- Only one application-level network-mode enum remains, unless a separate DTO/config enum is clearly justified.
- All existing network-mode values remain supported.
- Plan parsing and execution behavior remain unchanged.
- Tests from Step 7 pass.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

# Phase 4 — Verification plan parsing and selection cleanup

## Step 9 — Add characterization tests for verification plan parsing

### Goal

Freeze current parsing behavior before extracting or replacing parser logic.

### Scope

Cover built-in plan examples and edge cases:

- valid plan,
- missing required fields,
- command list parsing,
- network mode parsing,
- disabled/enabled behavior,
- invalid values,
- default values.

### Acceptance criteria

- Current parser behavior is explicitly tested.
- No production behavior changes.

### Verification

```bash
./mvnw test
```

### Risk

Low.

---

## Step 10 — Extract `VerificationPlanParser`

### Goal

Move parsing logic out of `VerificationPlanService`.

### Scope

Create:

- `VerificationPlanParser`

Move only parsing responsibilities into the new class. Keep behavior identical.

### Acceptance criteria

- `VerificationPlanService` delegates parsing.
- Parser tests pass without expectation changes.
- No plan-selection behavior changes.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 11 — Extract `VerificationPlanValidator`

### Goal

Separate validation rules from parsing and service orchestration.

### Scope

Create:

- `VerificationPlanValidator`

Move validation for required fields, supported values, and command definitions.

### Acceptance criteria

- Parser creates structured data.
- Validator reports invalid plan definitions.
- Existing validation behavior remains unchanged unless tests reveal an intentional improvement is needed.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 12 — Extract `VerificationPlanSelector`

### Goal

Separate plan selection from catalog loading and parsing.

### Scope

Create:

- `VerificationPlanSelector`

Move logic that chooses the best plan for a detected project/package.

### Acceptance criteria

- Selection behavior remains unchanged.
- Tests cover at least one matching and one no-match case.
- `VerificationPlanService` becomes a coordinator/catalog facade.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 13 — Consider replacing the hand-rolled YAML-like parser

### Goal

Improve parser safety and maintainability after behavior is characterized.

### Scope

Evaluate replacing custom YAML-like parsing with a real parser or stricter explicit format reader.

### Acceptance criteria

- Existing plan files continue to load.
- Invalid plans fail clearly.
- Parser behavior is documented.
- No security regressions are introduced.

### Suggested approach

Do not combine this with earlier extraction steps. First extract and test the current behavior; then replace internals in one isolated change.

### Verification

```bash
./mvnw test
```

### Risk

Medium to high.

---

# Phase 5 — Docker execution decomposition

## Step 14 — Add characterization tests for Docker command construction

### Goal

Protect security-sensitive Docker invocation behavior before refactoring.

### Scope

Test command construction for:

- image name,
- working directory,
- mounted workspace,
- network mode,
- timeout-related behavior if encoded in command,
- command arguments,
- environment variables if applicable.

### Acceptance criteria

- Current Docker command arguments are explicitly tested.
- Tests avoid requiring Docker daemon unless the project already has integration-test support.

### Verification

```bash
./mvnw test
```

### Risk

Low.

---

## Step 15 — Extract `DockerRunCommandBuilder`

### Goal

Separate Docker CLI argument construction from process execution.

### Scope

Create:

- `DockerRunCommandBuilder`

Move only command/argument construction into the builder.

### Acceptance criteria

- `DockerCommandExecutor` delegates command building.
- Command output and process handling remain unchanged.
- Characterization tests from Step 14 pass.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 16 — Extract `DockerWorkspacePathMapper`

### Goal

Isolate host/container workspace path resolution.

### Scope

Create:

- `DockerWorkspacePathMapper`

Move path normalization, workspace mount path calculation, and working-directory mapping.

### Acceptance criteria

- Path behavior remains unchanged.
- Tests cover normal paths, nested working directories, missing directories, and path traversal-like inputs if currently handled.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 17 — Extract bounded process output capture

### Goal

Separate stdout/stderr capture limits from Docker execution orchestration.

### Scope

Create:

- `BoundedProcessOutputCollector`
- or similar focused class.

Move bounded capture and truncation behavior.

### Acceptance criteria

- Existing output limits remain unchanged.
- Tests cover small output, oversized output, stderr, stdout, and truncation markers if present.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 18 — Extract container cleanup behavior

### Goal

Make cleanup behavior explicit and testable.

### Scope

Create:

- `DockerContainerCleanup`
- or a similarly named collaborator.

Move cleanup command construction and invocation.

### Acceptance criteria

- Container cleanup still runs in the same success/failure cases as before.
- Cleanup failures are handled exactly as before unless intentionally documented.
- Tests cover cleanup after success, failure, and timeout if feasible.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

# Phase 6 — Verification execution orchestration cleanup

## Step 19 — Extract artifact persistence

### Goal

Move stdout/stderr artifact persistence out of execution orchestration.

### Scope

Create:

- `CommandArtifactService`
- or `ArtifactCaptureService`

Move logic that creates and stores command artifacts.

### Acceptance criteria

- Artifact file names, media types, sizes, and associations remain unchanged.
- Existing artifact download tests pass.
- New focused tests cover artifact creation behavior.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 20 — Extract command result persistence

### Goal

Separate command-result persistence from execution coordination.

### Scope

Create:

- `CommandResultPersister`

Move logic that records command exit codes, durations, timeout state, skipped state, and output artifact references.

### Acceptance criteria

- Run and command records are unchanged for equivalent executions.
- Failure and timeout states remain unchanged.
- Tests cover success, failure, timeout, and skipped results.

### Verification

```bash
./mvnw test
```

### Risk

Medium.

---

## Step 21 — Introduce `RunExecutor` coordinator

### Goal

Make `VerificationExecutionService` a thin entry point or replace it with a clearer coordinator.

### Scope

Create:

- `RunExecutor`

Move high-level execution flow into the coordinator after supporting collaborators have been extracted.

### Acceptance criteria

- Public service contracts remain stable.
- Execution behavior remains unchanged.
- Docker and persistence tests continue to pass.

### Verification

```bash
./mvnw test
```

### Risk

Medium to high.

---

# Phase 7 — Frontend/backend contract hardening

## Step 22 — Document API contract synchronization strategy

### Goal

Prevent frontend TypeScript API types from silently drifting from backend DTOs.

### Scope

Add documentation explaining how frontend API types should be kept in sync with backend responses.

Possible location:

- `docs/reference/api-contract-sync.md`

### Acceptance criteria

- The chosen sync strategy is documented.
- The document names the relevant frontend and backend files.
- The document explains when to update frontend types.

### Verification

Documentation-only step.

### Risk

Low.

---

## Step 23 — Evaluate generated TypeScript API types

### Goal

Decide whether to generate frontend types from OpenAPI.

### Scope

Investigate current OpenAPI output and frontend build tooling.

Possible outcomes:

1. Adopt generated types.
2. Keep manual types with a sync checklist.
3. Add runtime validation for critical API responses.

### Acceptance criteria

- Decision is documented.
- If generation is adopted, generated output location and command are documented.
- If generation is deferred, manual sync process is clear.

### Verification

Depends on selected outcome. At minimum:

```bash
cd frontend
npm test
npm run build
```

and, if backend OpenAPI generation is involved:

```bash
./mvnw test
```

### Risk

Medium.

---

# Phase 8 — Workflow documentation cleanup

## Step 24 — Archive completed delivery history

### Goal

Reduce the active size of `docs/agent-progress.md`.

### Scope

Move historical completed step logs into:

- `docs/reference/delivery-history.md`

Keep `docs/agent-progress.md` focused on:

- current status,
- active checklist,
- recent completion summary,
- pointer to archived history.

### Acceptance criteria

- No historical information is lost.
- Active progress file is substantially shorter.
- `AGENTS.md` workflow remains usable.
- References to archived history are clear.

### Verification

Documentation-only step.

### Risk

Low.

---

# Phase 9 — Final validation

## Step 25 — Run full regression verification

### Goal

Confirm the refactoring sequence preserved behavior.

### Scope

Run all practical verification commands.

### Acceptance criteria

- Backend test suite passes.
- Frontend test suite passes.
- Frontend production build passes.
- Docker smoke check passes if Docker is available.
- Any failures are documented with exact commands and logs.

### Verification

```bash
./mvnw test

cd frontend
npm test
npm run build
cd ..

docker compose build
docker compose up --abort-on-container-exit
```

### Risk

Low.

---

# Recommended first implementation step

Start with **Step 1 — Extract frontend formatting utilities**.

It is low-risk, creates immediate cleanup, and establishes the pattern of making small, reviewable changes with tests.

# Recommended first backend implementation step

Start backend refactoring with **Step 3 — Add characterization tests for backend response mapping**, followed by **Step 4 — Extract `RunResponseMapper`**.

This reduces service/resource responsibilities without touching security-sensitive Docker execution.

# Notes for AGENTS.md workflow use

If this plan is copied into `docs/delivery-plan.md`, initialize `docs/agent-progress.md` with the numbered steps above. Then each prompt can use:

```text
Follow AGENTS.md and implement next step.
```

The implementation assistant should complete exactly one unchecked step per response.
