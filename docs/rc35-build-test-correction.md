# RC35 build/test correction

## Scope

Correct build/test regressions reported after r0077 without changing intended runtime behavior.

## Backend

`ProjectApplicationService` is CDI-managed in production, where `ImportResumePersistenceStore` is injected. Several pure unit tests instantiate the service manually, leaving that field null. A private `persistentImportsEnabled()` guard now treats missing persistence as disabled in those isolated tests while preserving the injected production path. All persistence call sites use the guard.

## Frontend

`ImportReviewPage.test.tsx` now types its selection fixture and helper parameter as `ImportSelectionResponse`. This prevents `excludedPaths: []` and `overrides: []` from being inferred as `never[]`, while retaining the exact API response shape.

## Verification

- Repository structure/security/source/release checks: run for r0078.
- Maven test execution attempted but the packaging environment could not resolve `repo.maven.apache.org`.
- Full local/CI Maven and TypeScript test/build remain the authoritative final verification.

## Files

### Added
- `docs/rc35-build-test-correction.md`

### Modified
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `VERSION`
- `CHANGELOG.md`
- `docs/implementation-status.md`
- `scripts/verify-release.sh`

### Moved
- None.

### Deleted
- None.
