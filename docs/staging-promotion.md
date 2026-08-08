# StagingImport project selection and promotion

Phase 9 step 9.4 turns an already authenticated `CLAIMED` staging upload into an ordinary zip-github Import. Staging remains transport/claim state only; after promotion all review, policy, selection, approval, Work delivery and pull-request rules are the existing common pipeline.

## Authorization boundary

- `GET /api/staging-imports/{stagingId}` is owner-only and returns only the caller's claimed/promoted staging metadata.
- Project choices come from the ordinary authenticated `/api/projects` endpoint and therefore contain only the user's own projects.
- `POST /api/staging-imports/{stagingId}/promote` requires the normal session, CSRF marker, staging ownership and project ownership.
- Promotion never grants repository authority itself. The selected Project's existing GitHub App configuration remains the only repository authority.

## Promotion/idempotency

Promotion uses the already stored `StoredUploadArtifact`; it does not upload, copy or re-stream ZIP bytes. The ordinary Import is created through `createImportFromStoredUpload(...)` with `ImportSource.STAGING_IMPORT` and the non-secret source reference `staging-import:<stagingId>`.

That source reference is persisted on the ordinary Import, protected by a unique `(owner, source type, source reference)` database index, and is the restart recovery key. If the process stops after creating the Import but before marking the staging row `PROMOTED`, retry finds the same Import, ensures the same stored artifact is attached and then completes the staging transition. A staging object can therefore converge on exactly one ordinary Import.

The ordinary `ACTIVE_IMPORT_EXISTS`, inactive-project, ownership and Work invariants are reused without a staging-specific bypass.

## Git file modes

Both browser uploads and staging uploads extract Git-relevant mode metadata only when the ZIP central directory contains trustworthy Unix metadata. Only ordinary-file modes `100644` and `100755` are represented.

Effective mode resolution is deterministic:

1. trustworthy ZIP mode when present;
2. otherwise the exact base-repository `100644`/`100755` mode for an existing path;
3. otherwise `100644` for a new file.

No executable inference is made from `.sh`, `mvnw`, file extensions or names.

A content-identical file whose effective mode differs from the repository is `MODIFIED`. Archive/repository/effective mode and the `modeChanged` flag enter the immutable plan digest. Only selected paths have their mode applied in the isolated workspace. Delivery verifies the staged Git index mode before commit, so exclusions cannot accidentally change executable state.

## Step 9.6 cleanup coordination

Promotion now holds the staging PostgreSQL row lock for the entire create/recover ordinary-Import operation. Staging cleanup uses the same row lock with `SKIP LOCKED`, so the artifact cannot be expired/deleted during promotion even with multiple backend instances. If a crash leaves an ordinary `STAGING_IMPORT` Import persisted before the staging status is updated, cleanup reconciles the stable `staging-import:<id>` source reference to `PROMOTED` before considering deletion. The original artifact retention deadline is stored separately from staging expiry and is transferred unchanged to the ordinary Import.
