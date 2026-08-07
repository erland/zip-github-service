# Step 7.12 report — create normal Import from an already stored ZIP

Date: 2026-08-07  
Revision: r0067  
Version: 1.0.0-rc.25

## Implemented

- Added `ProjectApplicationService.createImportFromStoredUpload(...)` as the internal promotion boundary from a neutral `StoredUploadArtifact` to the ordinary user-owned import model.
- Promotion reuses the existing artifact ID, checksum, retention metadata and storage path; no byte copy or second network upload occurs.
- Added an explicit idempotency key scoped to owner + project.
- Same-key/same-artifact retries return the original import instead of creating duplicates.
- Reusing an idempotency key for another stored artifact is rejected.
- Re-promoting the same artifact cannot create a second normal import.
- Promotion creates the same normal `ImportResponse`, Git identity and owned `StoredUpload` expected by the existing inventory/snapshot/comparison/policy/plan/selection/workspace/delivery pipeline.
- No staging/claim/public promotion endpoint was added; that remains a later integration concern.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/application/StoredUploadImportResult.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/application/StoredUploadImportPromotionTest.java`
- `docs/step-7.12-report.md`

## Files modified

- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `docs/upload-streaming.md`
- `docs/architecture.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Verification

- Stored-promotion source inspection and Java signature checks.
- Repository structure verification.
- Implementation ledger verification.
- Security regression verification.
- Source-tracking verification.
- Release-artifact verification.
- Shell/JSON/YAML syntax checks.
- ZIP integrity verification.

Full Maven/JUnit execution remains delegated to local/CI when Maven dependencies are available in the execution environment.

## Next step

`7.13` — formalize import source and audit metadata.
