# Step 3.5 report — retention and mobile upload view

Date: 2026-08-06  
Revision: r0018

## Delivered

- Scheduled cleanup for expired uploaded ZIP files.
- Safe metadata removal and empty-directory pruning.
- Configurable cleanup interval.
- Frontend API client for import creation and raw ZIP upload.
- Mobile-oriented file picker, progress, cancellation, retry messaging, checksum and retention display.
- Documentation and tests for the new behavior.

## Verification

- Backend standalone sources unrelated to Quarkus compiled with Java 21 where possible.
- `pom.xml`, `package.json` and TypeScript source structure validated statically.
- Shell scripts passed `bash -n`.
- `scripts/verify-structure.sh` passed.
- Exactly one implementation step is `NEXT`.
- Packaged ZIP passed `unzip -t`.

Full Maven/JUnit and npm/Vitest execution remains blocked by the previously documented environment limitations. A real iPhone Safari test cannot be performed in this environment and remains an explicit later acceptance activity.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/UploadRetentionService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/upload/UploadRetentionServiceTest.java`
- `frontend/src/api/imports.ts`
- `docs/upload-retention-and-mobile-ui.md`
- `docs/step-3.5-report.md`

### Modified

- `.env.example`
- `backend/pom.xml`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/resources/application.properties`
- `frontend/src/pages/NewImportPage.tsx`
- `frontend/src/styles/global.css`
- `docs/implementation-status.md`

### Moved

None.

### Deleted

None.
