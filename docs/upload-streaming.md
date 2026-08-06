# Streaming ZIP upload and metadata

Version 1.0 — 6 August 2026

## Endpoint

`PUT /api/imports/{importId}/upload`

Required request properties:

- authenticated browser session;
- an import session owned by the authenticated user;
- `Content-Type: application/zip` or `application/octet-stream`;
- `X-Filename` containing a plain `.zip` filename, not a path;
- raw ZIP bytes as the request body.

Successful uploads return `201 Created` with upload ID, import ID, original filename, actual byte count, SHA-256, status and retention deadline. Storage paths are never returned.

## Streaming and limits

The backend reads a fixed 64 KiB buffer and writes directly to a `.part` file while updating SHA-256. It does not load the complete archive into memory.

Two compressed-size checks are used:

1. A positive `Content-Length` above the configured maximum is rejected before storage begins.
2. The number of bytes actually read is counted and the upload is aborted as soon as it exceeds the maximum. This remains authoritative when `Content-Length` is absent or incorrect.

The initial default maximum is 100 MiB (`104857600` bytes).

## Atomic completion and cleanup

Uploads are written below an owner/import-specific directory in the configured storage root. The completed file is moved from `<upload-id>.part` to `<upload-id>.zip`, using an atomic move where supported. Failed, empty or oversized uploads remove their partial file.

If metadata registration fails after the move—for example because an upload already exists for the import—the completed file is deleted again.

## Metadata

The stored metadata consists of:

- upload ID;
- owner user ID;
- import session ID;
- original filename;
- actual compressed size;
- lowercase SHA-256;
- internal storage path;
- creation time;
- retention deadline;
- status `STORED`.

Only safe metadata is returned to the browser. The internal path and owner ID remain server-side.

## Configuration

- `ZIP_GITHUB_UPLOAD_STORAGE_ROOT`
- `ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES`
- `ZIP_GITHUB_UPLOAD_RETENTION_HOURS`

Automatic deletion at the retention deadline is implemented in step 3.5. This step records the deadline and structures storage so cleanup can be deterministic.

## Scope boundary

This step does not trust or unpack the ZIP. ZIP signatures, entry paths, symlinks, resource expansion and archive inventory are handled by steps 3.2–3.4.
