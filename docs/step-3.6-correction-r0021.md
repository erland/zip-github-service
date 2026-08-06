# Correction r0021 — CDI constructor for StreamingUploadService

Date: 6 August 2026
Related step: `3.6 – Etablera komplett CI-baslinje`

## Problem observed

A full Quarkus test startup failed during Arc validation because `ImportResource` injects `StreamingUploadService`, but the service had two constructors and neither constructor was marked as the CDI injection constructor. Quarkus therefore skipped the class during bean discovery with the message `does not declare a valid bean constructor`.

## Correction

The configuration constructor in `StreamingUploadService` is now annotated with `@Inject`. The package-private constructor remains available for deterministic unit tests, while Arc has one explicit constructor to use in production.

## Changed files

### Modified

- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/StreamingUploadService.java`
- `docs/implementation-status.md`

### Added

- `docs/step-3.6-correction-r0021.md`

### Moved

- None.

### Deleted

- None.

## Verification

- Confirmed that `StreamingUploadService` is `@ApplicationScoped` and now has exactly one `@Inject` constructor.
- Reviewed the other active `@ApplicationScoped` classes with multiple constructors. `ArchiveInspectionService` already has an explicit `@Inject` constructor; `UploadRetentionService` has a valid public no-argument constructor.
- Project structure and implementation status checks pass.
- Full Quarkus execution remains to be confirmed by the user's local Maven run or GitHub Actions.

## Status impact

This is a corrective revision. Step `3.6` remains `DONE` and step `4.1` remains the single `NEXT` step.
