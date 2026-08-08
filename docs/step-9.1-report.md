# Step 9.1 report — StagingImport lifecycle and persistence

## Scope

Implemented only step 9.1. No capability-protected upload endpoint, browser claim route, project selection, promotion API, Shortcut distribution or commit-message UX was added.

## Implemented

- Added `StagingImport` domain state and explicit transitions for AVAILABLE, CLAIMED, PROMOTED, EXPIRED and CANCELLED.
- Added V10 durable persistence with claim-token digest only, ownership/promotion timestamps and restart-safe artifact metadata.
- Added transactional persistence primitives for one-winner claim and idempotent one-import promotion.
- Added neutral per-file Git mode metadata (`100644`/`100755`) to `StoredUploadArtifact` with absence as the only representation of unknown mode.
- Preserved compatibility for existing stored-upload callers through the previous seven-argument artifact constructor.

## Verification

Verification performed:

- `bash scripts/verify-implementation-status.sh` — PASS (`9.2` is the single NEXT step).
- `bash scripts/verify-structure.sh` — PASS.
- `bash scripts/security-regression.sh` — PASS.
- `bash scripts/verify-source-tracking.sh` — PASS.
- `bash scripts/verify-release.sh` — PASS for `1.0.0-rc.45`.
- Dependency-free `javac`/`java` execution of `StagingImportLifecycleSelfTest` — PASS.
- `bash backend/mvnw test` — BLOCKED before build because the sandbox cannot resolve `repo.maven.apache.org`.
- `npm ci` — BLOCKED because the sandbox npm proxy returns 404 for `yallist-3.1.1.tgz`; Vitest/build therefore could not run here.

No live staging HTTP API exists in 9.1, so no external Shortcut/GitHub integration test is applicable to this step.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/StagingImport.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/StagingImportStatus.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/StagingImportEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/GitFileMode.java`
- `backend/src/main/resources/db/migration/V10__staging_import_lifecycle.sql`
- `backend/src/test/java/info/isaksson/erland/zipgithub/domain/StagingImportLifecycleTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/domain/StagingImportLifecycleSelfTest.java`
- `docs/staging-import-lifecycle.md`
- `docs/step-9.1-report.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/StoredUploadArtifact.java`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

9.2 — capability-protected staging upload.
