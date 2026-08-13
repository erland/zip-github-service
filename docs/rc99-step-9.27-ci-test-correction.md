# rc.99 — Step 9.27 CI/test correction

GitHub Actions run `31725664952` exposed test-only regressions after Step 9.27. Production frontend/backend code reached the relevant test phases; the failures were stale test contracts.

## Frontend

- `frontend/src/api/staging.test.ts` now expects `confirmOpenPullRequest: false` in promotion payloads when no explicit confirmation is supplied.
- `frontend/src/pages/SimplifiedImportFlow.test.tsx` now mocks `getProjectWork` and resolves it to `null` for the baseline flow.

## Backend

- `backend/src/test/java/info/isaksson/erland/zipgithub/application/WorkLifecycleServiceTest.java` now imports `CreateImportRequest`, matching the new Step 9.27 regression cases.

No production semantics changed from rc.98.
