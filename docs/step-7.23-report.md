# Step 7.23 report — state-based Work actions

Date: 2026-08-07  
Repository revision: `r0082`  
Application version: `1.0.0-rc.38`

## Outcome

Step 7.23 is complete. Work actions now follow the actual lifecycle state instead of exposing redundant or invalid paths.

- A project with an active import shows only the resumable import actions (`Fortsätt import`/`Fortsätt granska`) plus explicit `Avbryt import`; it does not offer a parallel ZIP upload.
- A project with an open Work and no active import exposes one primary `Ladda upp nästa ZIP` action. The generic `Fortsätt arbete` action has been removed.
- Backend import creation enforces the same invariant under the current single-backend-instance model and returns `409 ACTIVE_IMPORT_EXISTS` when a second active import is attempted for the same Work/project.
- Cancelling the active import releases the invariant so a replacement ZIP can be started.
- After successful commit/push, the result page directly offers both `Ladda upp nästa ZIP` and `Arbetet är klart – skapa pull request`.
- Pull-request creation from the result page reuses the existing idempotent Work PR operation and disables the action after success while exposing the GitHub PR link.

## Verification

- Added backend regression for the one-active-import invariant and cancel/retry transition.
- Updated alternative-ingestion regression so the first independent path is explicitly cancelled before starting the second path, consistent with the new Work invariant.
- Updated project-view tests for active-import actions, removal of `Fortsätt arbete`, and a single next-ZIP action when Work is idle.
- Updated result-page tests for direct PR creation after commit.
- Repository structure, source tracking, security regression, implementation ledger and release verification pass in the packaging environment.
- Full Maven/Vitest execution remains delegated to local/CI where project dependencies are installed and Maven Central is reachable.

## Files added

- `backend/src/test/java/info/isaksson/erland/zipgithub/application/ActiveImportInvariantTest.java`
- `docs/step-7.23-report.md`

## Files modified

- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/application/AlternativeZipIngestionRegressionTest.java`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `frontend/src/pages/ProjectDetailPage.test.tsx`
- `frontend/src/pages/ImportResultPage.tsx`
- `frontend/src/pages/ImportResultPage.test.tsx`
- `docs/api-contract.md`
- `docs/architecture.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`7.24 – Regression för cancel och state-baserade Work-actions`.
