# Step 7.18 report — E2E regression for the streamlined import flow

Revision: `r0073`  
Application version: `1.0.0-rc.31`

## Goal

Lock the phase-7 user flow so the normal path is exactly:

```text
choose ZIP -> upload/automatic preparation -> review/select/override -> approve -> commit/push -> result
```

No normal intermediate “create plan” or “create commit” click is allowed.

## Added regression coverage

### Cross-page frontend flow

`frontend/src/pages/SimplifiedImportFlow.test.tsx` renders `NewImportPage` and `ImportReviewPage` in the same router and verifies:

- a custom author is passed when the import is created;
- ZIP upload occurs once;
- review preparation starts automatically once and navigates directly to review;
- no manual “Skapa granskningsplan” action is present;
- a normal changed file can be deselected;
- an unchanged `.github/workflows/**` file has no override control;
- an actually modified workflow requires an explicit override;
- the immutable selection contains exactly the intended selected path and override;
- approval, workspace and delivery are each invoked exactly once;
- delivery represents the active `zip-github/work-*` branch;
- the same approval click ends on the result route.

### Slow automatic preparation

`NewImportPage.test.tsx` now keeps review preparation pending and verifies the UI remains in a disabled preparation state and cannot send a duplicate preparation request.

### Failure between approval and push

`ImportReviewPage.test.tsx` now simulates the first delivery attempt failing after selection and approval have already succeeded. The recovery action retries delivery while asserting that selection and approval were each created only once.

Existing refresh recovery coverage additionally verifies that a persisted selection/approval is restored rather than recreated after a page reload.

## Existing backend evidence reused by this quality gate

The cross-page UI regression is intentionally combined with the already established backend regressions rather than introducing a second fake Git pipeline:

- `ImportWorkspaceServiceSelfTest` verifies the Git diff exactly equals the immutable selected paths, including approved deletion/override cases.
- `GitDeliveryServiceSelfTest` verifies work-branch delivery and stale-head rejection.
- `ImportPolicyServiceTest` verifies unchanged workflows require no override while added/modified/deleted workflows do.
- `ImportReviewPreparationResourceTest` verifies idempotent review preparation and reuse of the locked immutable plan.
- `ImportSelectionResourceTest` verifies approval readback/recovery.

## Phase 7 quality gate

Phase 7 is complete when all repository verification scripts pass and the frontend/backend test suites pass in an environment with normal Maven/npm dependency access. The next implementation step is `8.1`.

## Files

Added:
- `frontend/src/pages/SimplifiedImportFlow.test.tsx`
- `docs/step-7.18-report.md`

Modified:
- `frontend/src/pages/NewImportPage.test.tsx`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `docs/mvp-release.md`
- `docs/release-checklist.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/verify-release.sh`

Moved: none.
Deleted: none.
