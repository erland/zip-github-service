# Correction after CI baseline — revision r0020

Date: 6 August 2026

## Reason

The first complete local backend/frontend execution after step 3.6 exposed two issues that the earlier partial environment could not detect:

1. `UploadRetentionService` uses Quarkus Scheduler but `quarkus-scheduler` was missing from `backend/pom.xml`.
2. `frontend/src/App.test.tsx` did not explicitly clean up the DOM between tests, causing multiple rendered application trees and an ambiguous `Öppna projekt` link query.

## Corrections

- Added the `io.quarkus:quarkus-scheduler` extension dependency.
- Added Testing Library `cleanup()` in `afterEach` for routing tests.

## Changed files

### Modified

- `backend/pom.xml`
- `frontend/src/App.test.tsx`
- `docs/implementation-status.md`

### Added

- `docs/step-3.6-correction-r0020.md`

No files were moved or removed.

## Expected verification

```bash
cd backend
./mvnw verify

cd ../frontend
npm test
npm run build
```

The implementation step remains 3.6 `DONE`; this revision is a corrective follow-up and the next implementation step remains 4.1.
