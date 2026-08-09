# rc.76 — step 9.16 CI correction

Date: 9 August 2026  
Repository revision: `r0124`  
Application version: `1.0.0-rc.76`

## Observed GitHub Actions failures

Run `31320689419` failed in both backend job `93263066218` and frontend job `93263066255`.

### Backend

Maven compilation stopped in `ImportResource`: the new step-9.16 endpoint used `ExternalBranchChangesResponse` without importing the DTO class. The correction adds the missing import only.

### Frontend

Vitest reported 3 failures while 52 tests passed:

1. Two `ProjectDetailPage` assertions still expected the pre-9.16 label `Arbetet är klart – skapa pull request`; the intended 9.16 UI label is `Skapa pull request`.
2. `SimplifiedImportFlow` mocked `../api/imports` without the newly used `getExternalBranchChanges` export, so `ImportReviewPage` failed before rendering the file tree. The mock now returns a neutral no-external-changes response.

## Scope

No production behavior, database schema, policy, Work lifecycle, PR lifecycle or external-branch warning semantics changed. Step 9.16 remains complete.
