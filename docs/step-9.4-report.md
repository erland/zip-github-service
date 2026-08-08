# Step 9.4 report — project selection and ordinary Import promotion

**Revision:** r0096  
**Application version:** 1.0.0-rc.48  
**Date:** 2026-08-08  
**Status:** DONE  
**Next:** 9.5 — user-controlled commit message

## Implemented

- Added owner-only staging metadata read and authenticated project promotion API.
- Added mobile project selection after browser claim and continuation into the existing `prepare-review`/review route.
- Reused `ProjectApplicationService.createImportFromStoredUpload(...)` with `ImportSource.STAGING_IMPORT`; no second import pipeline and no ZIP copy/re-stream were introduced.
- Added restart-safe promotion recovery through the persisted non-secret source reference `staging-import:<id>` before the staging row is marked `PROMOTED`, backed by a unique database index for the staging source correlation.
- Reused existing `ACTIVE_IMPORT_EXISTS`, project ownership, project-active and Work invariants.
- Added common browser/StagingImport extraction of trustworthy ZIP Unix modes.
- Added deterministic effective-mode resolution: ZIP metadata -> base repository mode for existing paths -> `100644` for new paths.
- Made mode-only changes reviewable and included archive/repository/effective modes in the immutable plan digest.
- Applied modes only to selected workspace paths and added staged Git-index mode verification before commit.

## Verification performed

- Maven compile/test was attempted, but Maven Wrapper bootstrap remained blocked because the sandbox could not resolve `repo.maven.apache.org`.
- Dependency-free `GitFileModeResolverSelfTest` compiled and passed with local `javac/java`.
- Repository implementation-status, structure, security-regression, source-tracking and release verification scripts were run after updating the revision metadata.
- Frontend dependency installation/test/build was attempted; `npm ci` was blocked by the sandbox npm proxy with HTTP 404 for `yallist-3.1.1.tgz`, so the new Vitest regression could not be executed here.
- Shell syntax and GitHub workflow YAML parsing passed locally.
- ZIP integrity and top-level-folder checks were performed before packaging.

## Security/invariant notes

- Staging/project ownership is checked server-side before promotion.
- Promotion capability is not added to the anonymous staging credential; only an authenticated owner can promote.
- Stable source-reference recovery plus the V11 unique source-correlation index closes the create-Import/mark-PROMOTED restart window without allowing a second ordinary Import.
- File modes are approval-bound and exclusion-bound; filename-based executable inference is forbidden.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/StagingPromotionRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/StagingPromotionResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/GitFileModeResolver.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/UploadFileModeService.java`
- `backend/src/main/resources/db/migration/V11__staging_import_source_idempotency.sql`
- `backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingPromotionServiceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/upload/GitFileModeResolverSelfTest.java`
- `docs/staging-promotion.md`
- `docs/step-9.4-report.md`
- `frontend/src/pages/StagingClaimPage.test.tsx`

## Files modified

- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportComparisonResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportPlanResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/comparison/ImportComparisonEntry.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/comparison/ImportComparisonService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/ImportResumePersistenceStore.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImmutableImportPlanEntry.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanFactory.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/StoredUpload.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/StreamingUploadService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/workspace/AppliedImportWorkspace.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/comparison/ImportComparisonServiceTest.java`
- `docs/api-contract.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/shortcut-stagingimport-design.md`
- `docs/threat-model.md`
- `frontend/src/api/imports.ts`
- `frontend/src/api/staging.ts`
- `frontend/src/components/ReviewFileTree.tsx`
- `frontend/src/pages/StagingClaimPage.tsx`
- `frontend/src/styles/global.css`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.
