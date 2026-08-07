# RC36 regression correction

Repository revision: `r0079`  
Application version: `1.0.0-rc.36`

This corrective release contains no intended production behavior change.

## Backend

Step 7.19 deliberately changed source-ZIP cleanup semantics: an active resumable import remains protected even after its original retention deadline. Two older stored-upload tests still expected such imports to appear in `expiredUploads(...)`. They now assert the intended protected state instead. Terminal cleanup remains covered by the dedicated persistence/retention paths.

## Frontend

`App.test.tsx` now mocks `/api/projects/{projectId}/work/commits`, which the project detail view started requesting in step 7.20.

`SimplifiedImportFlow.test.tsx` now waits for the actual review-tree checkbox before interacting with the review page. The review heading renders while the plan is still being loaded, so it is not a valid readiness signal.

## Files

Added: `docs/rc36-regression-correction.md`.

Modified: `backend/src/test/java/info/isaksson/erland/zipgithub/application/AlternativeZipIngestionRegressionTest.java`, `backend/src/test/java/info/isaksson/erland/zipgithub/application/StoredUploadImportPromotionTest.java`, `frontend/src/App.test.tsx`, `frontend/src/pages/SimplifiedImportFlow.test.tsx`, `VERSION`, `CHANGELOG.md`, `docs/implementation-status.md`, `scripts/verify-release.sh`.

Moved: none. Deleted: none.
