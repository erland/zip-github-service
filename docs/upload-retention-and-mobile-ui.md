# Upload retention and mobile upload UI

Step 3.5 adds automatic cleanup of expired source ZIP files and connects the mobile upload page to the real import/upload API contract.

## Retention

`UploadRetentionService` runs on a configurable Quarkus schedule. It selects uploads whose `retentionDeadline` has passed, deletes the ZIP, removes its in-memory metadata and prunes empty import/owner directories. Failures are logged and retried on a later run; one failed deletion does not stop the rest.

Configuration:

- `ZIP_GITHUB_UPLOAD_RETENTION_HOURS` controls the deadline assigned at upload time.
- `ZIP_GITHUB_UPLOAD_CLEANUP_INTERVAL` controls the cleanup schedule and defaults to `1h`.

The current metadata store is in memory. When database repositories replace it, expired-upload selection and deletion acknowledgement must be transactional or otherwise retry-safe.

## Mobile upload flow

The new import page now:

1. creates an import session with `POST /api/projects/{projectId}/imports`;
2. uploads the selected ZIP using `PUT /api/imports/{importId}/upload`;
3. sends cookies with both requests;
4. displays upload progress and permits cancellation;
5. displays the returned checksum and retention deadline;
6. does not inspect, execute, compare or write the ZIP to GitHub in this step.

The raw-body upload avoids multipart parsing overhead and works with the browser/iOS file picker. The final Safari/iPhone acceptance test still belongs to phase 7.1 because this execution environment cannot run iOS Safari.
