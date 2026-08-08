# Staging retention, capacity and credential incident model

Implemented in r0098 / 1.0.0-rc.50 (phase 9 step 9.6).

## Lifecycle deadlines

Staging has its own short lifecycle and is not ordinary Import retention.

- `AVAILABLE`: default 60 minutes (`ZIP_GITHUB_STAGING_AVAILABLE_TTL_MINUTES`).
- On the first successful authenticated claim, `expires_at` is moved to a separate claimed grace deadline, default 240 minutes (`ZIP_GITHUB_STAGING_CLAIMED_TTL_MINUTES`).
- Same-owner claim retry is idempotent and does not keep extending the deadline.
- `AVAILABLE` or `CLAIMED` rows past their current deadline become `EXPIRED` and are cleanup candidates.
- `EXPIRED` and `CANCELLED` artifacts are physically deleted. The row remains as a tombstone and `artifact_deleted_at` is written only after deletion succeeds, so failed deletion is retried deterministically.
- `PROMOTED` artifacts are never deleted by staging cleanup. Storage ownership has passed to the ordinary Import/source-upload retention path.

The original `StoredUploadArtifact.retentionDeadline` is persisted separately as `artifact_retention_deadline`. This prevents the short staging deadline from accidentally shortening the ordinary Import retention window after promotion.

## Promotion/cleanup race safety

Promotion and cleanup serialize on the same PostgreSQL staging row lock.

- Promotion uses `SELECT ... FOR UPDATE` and keeps that lock while it creates or recovers the ordinary Import and records `PROMOTED`.
- Cleanup uses `FOR UPDATE SKIP LOCKED`, so an in-flight promotion is not expired/deleted.
- Before cleanup expires rows, it reconciles the crash window where an ordinary `STAGING_IMPORT` Import with source reference `staging-import:<stagingId>` exists but the staging row was not yet marked `PROMOTED`. Such a row is repaired to `PROMOTED`, not deleted.

This is database-coordinated and therefore does not depend on a single JVM instance.

## Capacity limits

The deployment has configurable staging-only limits:

- `ZIP_GITHUB_STAGING_MAX_LIVE_OBJECTS` (default 100)
- `ZIP_GITHUB_STAGING_MAX_LIVE_BYTES` (default 1 GiB)

Quota accounting is serialized with a short PostgreSQL transaction advisory lock around usage calculation plus staging-row insertion. This avoids parallel uploads all passing the same stale quota snapshot. Not-yet-promoted artifacts count until they have been physically deleted; promoted artifacts no longer count as staging storage.

A quota rejection returns `429 STAGING_CAPACITY_EXCEEDED`. ZIP bytes that were already safely ingested before the durable quota decision are immediately removed if the insert is rejected.

Existing archive limits still apply before staging: compressed-size limit, path/type safety, entry count, expanded-size limit, per-file limit and compression-ratio protection.

## Request abuse controls

Staging-create keeps its dedicated limits:

- per presented upload credential (keyed only by SHA-256 digest),
- deployment-global staging-create rate,
- optional per-network-source limit when the deployment explicitly trusts a reverse proxy that strips/rebuilds `X-Forwarded-For`.

`ZIP_GITHUB_TRUST_FORWARDED_FOR` defaults to `false`. Do not enable it when clients can inject the header directly.

No raw upload credential, claim token, ZIP content or filesystem path is used as a rate-limit/log identity.

## Credential revoke and rotation

The deployment upload credential remains a low-privilege staging-create secret, not user authentication.

Normal/incident rotation requires no database migration:

1. Generate a new strong random `ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL`.
2. Replace the secret in deployment configuration and restart/redeploy all backend instances.
3. The old signed Shortcut immediately receives the neutral `401 STAGING_UPLOAD_UNAUTHORIZED` response; it has no grace period in the first implementation.
4. Existing staging rows are unaffected. Their independent claim-token, owner and TTL rules continue to apply.
5. Rebuild/sign/publish the new static Shortcut containing the new credential (step 9.7) and direct users to install it.

If the credential is suspected compromised, rotate first; do not wait for old staging uploads to expire. GitHub App credentials, user sessions and existing claim tokens do not need rotation solely because this low-privilege upload credential leaked.

## Security boundary preserved

Before authenticated promotion, staging has no code path to Project/GitHub operations. The capability can only create a bounded stored ZIP plus claim token. Claim establishes an owner but still does not access GitHub. Only owner-checked promotion invokes the existing ordinary Import pipeline.
