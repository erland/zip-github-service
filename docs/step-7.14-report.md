# Step 7.14 report — alternative ZIP ingestion regression

Repository revision: `r0069`  
Application version: `1.0.0-rc.27`

## Goal

Prove that an already stored ZIP can converge on the ordinary import pipeline without a second upload and without introducing a weaker security/digest path than the browser upload.

## Implemented regression coverage

- Added `AlternativeZipIngestionRegressionTest` covering both the authenticated browser adapter and the source-neutral stored-artifact path with identical ZIP bytes.
- Both paths are inspected through the same `ArchiveInventoryService`, compared against snapshots locked to the same base SHA, evaluated by the same import policy and converted by the same immutable plan factory.
- The regression compares normalized inventory, comparison entries, policy entries and immutable plan entries. Plan digests are intentionally different between distinct imports because `importId` is part of plan identity.
- Added a shared absolute compressed-size regression proving both the neutral ingestion entry point and the browser adapter enforce the same `ZipIngestionService` limit.
- Promotion retry remains idempotent, the promoted upload participates in the ordinary retention/cleanup model and another user cannot read the promoted import.
- Repeated plan/selection creation with identical immutable inputs produces stable digests despite different creation timestamps. Import-source audit metadata remains outside both digest contracts.
- Test-state cleanup now also clears import audit metadata to avoid cross-test leakage.

## Future staging integration point

A future `StagingImport` / iOS Shortcut implementation must use this sequence:

```text
external ZIP stream
  -> ZipIngestionService.store(...)
  -> StoredUploadArtifact
  -> authenticated claim/project authorization
  -> ProjectApplicationService.createImportFromStoredUpload(...)
  -> ordinary Import + StoredUpload
  -> ArchiveInventoryService
  -> RepositorySnapshotService
  -> ImportComparisonService
  -> ImportPolicyService
  -> ImportPlanFactory
  -> selection / approval / workspace / delivery
```

There must not be a staging-specific archive inventory, policy, plan or Git-delivery implementation. The capability/claim layer may decide who can promote an artifact, but after promotion the ordinary user-owned import is authoritative.

## Security boundary

`StoredUploadArtifact` is an internal value returned by the controlled ingestion layer. Future external staging endpoints must never construct artifact metadata directly from client-supplied size/hash/path values; they must obtain the artifact from `ZipIngestionService`, which owns filename validation, absolute compressed-size enforcement, streaming storage and SHA-256 calculation.

## Verification

Repository/release verification is recorded in the revision ledger. Full Maven execution remains dependent on dependency availability in the build environment; the added JUnit regression is intended to run in the normal local/CI build.

## Files

Added:
- `backend/src/test/java/info/isaksson/erland/zipgithub/application/AlternativeZipIngestionRegressionTest.java`
- `docs/step-7.14-report.md`

Modified:
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `docs/architecture.md`
- `docs/upload-streaming.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/verify-release.sh`

Moved: none.  
Deleted: none.
