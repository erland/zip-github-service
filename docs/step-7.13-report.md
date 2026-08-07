# Step 7.13 report — import source and audit metadata

Completed 2026-08-07 in repository revision `r0068` / application version `1.0.0-rc.26`.

## Implemented

- Added `ImportSource` with `WEB_UPLOAD`, `STORED_UPLOAD`, and reserved `STAGING_IMPORT`.
- Added bounded, non-secret `ImportAuditMetadata` with optional source reference.
- Normal browser-created imports are tagged `WEB_UPLOAD`.
- Stored-artifact promotion is tagged `STORED_UPLOAD` with `stored-upload:<artifact UUID>` correlation data.
- Import history exposes source type/reference and the project UI shows a human-readable source label.
- Added Flyway V7 columns/check constraint and persistence-entity fields. Existing rows default to `WEB_UPLOAD`.
- Documented that capability tokens, claim tokens, credentials and other secrets must never be copied into source references.
- Source metadata is diagnostic only; it is not included in policy, plan/selection digests, ownership checks or Git delivery decisions.

## Verification

- Added backend tests for browser-source defaults, stored-upload source audit and bounded references.
- Added frontend history coverage for source labels.
- Repository structure, source tracking, security regression, implementation ledger, release checks, shell syntax and JSON/YAML validation pass.
- Full Maven/Vitest execution remains dependent on normal external dependency availability/CI.
