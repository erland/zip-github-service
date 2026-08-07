# Step 7.11 report — reusable ZIP ingestion and storage

Revision: `r0066`  
Application version: `1.0.0-rc.24`  
Completed: 2026-08-07

## Goal

Remove user/import identity from the byte-ingestion/storage primitive while preserving the existing authenticated browser-upload behavior and security limits.

## Result

The upload package now has two layers:

1. `ZipIngestionService` owns source-neutral ingestion. It validates the plain `.zip` filename, checks declared and actual compressed size, streams with a fixed buffer into controlled temporary storage, calculates SHA-256 during the write, performs atomic completion and emits retention metadata.
2. `StreamingUploadService` is the normal web-import adapter. It requires the existing owner/import IDs, asks `ZipIngestionService` to store the bytes in the import's opaque storage scope, then attaches the neutral artifact to the existing `StoredUpload` ownership model.

`StoredUploadArtifact` contains only storage facts: upload ID, original filename, byte size, SHA-256, internal path, creation time and retention deadline. It contains no user or import identity and is therefore suitable for the future staging-import path.

## Compatibility

`PUT /api/imports/{importId}/upload` and `ProjectApplicationService.recordUpload(...)` continue using the existing `StoredUpload` contract. No frontend/API or configuration change is required for RC24.

The physical path for newly ingested web uploads is now `<storage-root>/<import-id>/<upload-id>.zip` instead of embedding the owner ID. Authorization never depended on the path and remains enforced through application ownership checks. Storage scope UUIDs carry no authorization meaning.

## Security invariants retained

- the same configured absolute compressed-size maximum applies to every ingestion source;
- actual streamed bytes remain authoritative even with absent/incorrect `Content-Length`;
- partial files are removed on failures/limit violations;
- filenames cannot contain paths/NUL and must end in `.zip`;
- SHA-256 is calculated while bytes are streamed;
- internal storage paths are never returned to clients;
- source-neutral storage does not imply ownership or authorization.

## Verification

Added/refactored unit coverage for neutral ingestion and the web adapter. Repository structure, source tracking, security regression and release metadata are also part of the RC24 release gate. Full Maven execution remains environment-dependent where Maven Central is unavailable.

## Next

Step `7.12` will consume this boundary by allowing a previously stored neutral ZIP artifact to be registered/promoted into a normal user-owned import without a second network upload.
