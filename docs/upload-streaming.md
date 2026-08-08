# Streaming ZIP ingestion and upload metadata

Version 1.1 — 7 August 2026

## Endpoint

`PUT /api/imports/{importId}/upload`

Required request properties:

- authenticated browser session;
- an import session owned by the authenticated user;
- `Content-Type: application/zip` or `application/octet-stream`;
- `X-Filename` containing a plain `.zip` filename, not a path;
- raw ZIP bytes as the request body.

Successful uploads return `201 Created` with upload ID, import ID, original filename, actual byte count, SHA-256, status and retention deadline. Storage paths are never returned.

## Source-neutral ingestion boundary

RC24 separates safe byte ingestion from user/import ownership.

`ZipIngestionService` is the reusable primitive. It accepts an opaque storage-scope UUID plus ZIP metadata/bytes and returns a neutral `StoredUploadArtifact`. The artifact contains:

- upload ID;
- original filename;
- actual compressed size;
- lowercase SHA-256;
- internal storage path;
- creation time;
- retention deadline.

It deliberately contains no user ID or import ID. A future staging-import channel can therefore use exactly the same ingestion limits without inventing a fake Zip-GitHub user.

`StreamingUploadService` is now the authenticated web-import adapter. It requires the existing owner/import IDs, calls the neutral ingestion service, and attaches the resulting artifact to the existing user/import-owned `StoredUpload` record. API ownership checks remain outside the neutral storage primitive.

## Streaming and limits

The backend reads a fixed 64 KiB buffer and writes directly to a `.part` file while updating SHA-256. It does not load the complete archive into memory.

Two compressed-size checks are used:

1. A positive `Content-Length` above the configured maximum is rejected before storage begins.
2. The number of bytes actually read is counted and the upload is aborted as soon as it exceeds the maximum. This remains authoritative when `Content-Length` is absent or incorrect.

The deployment default maximum is 200 MiB (`209715200` bytes). The same configured maximum is used by the neutral ingestion primitive regardless of the future source channel.

## Atomic completion and cleanup

Uploads are written below an opaque UUID storage-scope directory in the configured storage root. For current browser uploads the normal import UUID is used as this storage scope; the directory name itself has no authorization semantics.

The completed file is moved from `<upload-id>.part` to `<upload-id>.zip`, using an atomic move where supported. Failed, empty or oversized uploads remove their partial file.

If metadata registration fails after the move—for example because an upload already exists for the import—the completed file is deleted again. Retention cleanup removes the file and its empty scope directory, but not the configured storage root.

## Owned upload metadata

Once a neutral artifact is attached to a normal import, `StoredUpload` adds:

- owner user ID;
- import session ID;

while retaining the neutral artifact metadata. Only safe metadata is returned to the browser. The internal path and owner ID remain server-side.

## Configuration

- `ZIP_GITHUB_UPLOAD_STORAGE_ROOT`
- `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES`
- `ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE` (frontend-container proxy request-body limit; defaults to `200M`)
- `ZIP_GITHUB_UPLOAD_RETENTION_HOURS`

The frontend nginx proxy has a separate request-body ceiling (`ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE`, default `200M`) so proxy and backend limits can be coordinated explicitly.

## Scope boundary

Ingestion does not trust or unpack the ZIP. ZIP signatures, entry paths, symlinks, resource expansion and archive inventory remain the responsibility of the existing archive/import pipeline. A neutral stored artifact is not authorization and cannot by itself create an import, commit or GitHub operation.

## Promotion of an already stored ZIP

RC25 adds an application-level promotion boundary for source channels that already possess a safely ingested `StoredUploadArtifact`.

`ProjectApplicationService.createImportFromStoredUpload(...)` creates the same normal user-owned `Import` used by browser uploads, attaches the existing artifact as its `StoredUpload`, and returns both the normal import metadata and source-upload metadata. The physical ZIP path and upload ID are preserved; the bytes are not copied, moved or streamed a second time.

The operation requires an explicit idempotency key scoped to owner + project. Repeating the operation with the same key and artifact returns the original import. Reusing the key for different ZIP bytes is rejected, and one stored artifact cannot silently create multiple imports. This boundary is internal in step 7.12; no staging/claim HTTP API is exposed yet.

Once promoted, inventory, repository snapshot, comparison, policy, plan, selection, workspace and delivery continue through the existing normal-import pipeline.


## Alternative-ingestion regression contract

Step 7.14 locks the reusable-ingestion contract with regression tests. Identical ZIP bytes sent through the authenticated browser adapter or stored first as a neutral artifact must produce equivalent normalized inventory, comparison/policy decisions and plan entries when evaluated against the same base repository content. Both entry paths use the same `ZipIngestionService` absolute compressed-size limit. Promotion does not upload or copy the ZIP again; the attached ordinary `StoredUpload` points at the already stored artifact and then follows the normal retention/cleanup model.

Future staging/Shortcut code must call `ZipIngestionService.store(...)` rather than manufacture `StoredUploadArtifact` metadata from request values, then call `createImportFromStoredUpload(...)` after claim/project authorization.

## Automatic continuation to review

As of RC29, `201 Created` from the authenticated upload endpoint is not a normal stopping point in the browser flow. The frontend immediately calls `POST /api/imports/{importId}/prepare-review`, shows a processing status while the backend locks/reuses the repository snapshot and creates/reuses the immutable plan, then navigates directly to the review route.

If preparation fails after the ZIP has been stored, the UI keeps the stored upload identity and offers only a preparation retry. It does not ask the user to upload the same ZIP again, and the file picker is locked for that import. The preparation endpoint is idempotent with respect to an existing snapshot/plan, so retry preserves the previously locked base SHA.

