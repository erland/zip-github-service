# StagingImport lifecycle

Step 9.1 establishes the durable transport model used by later Shortcut work. It intentionally does **not** expose an upload, claim or promotion HTTP API.

## Boundary

`StagingImport` owns only temporary ZIP transport/claim state. It does not represent an ordinary Import and grants no Project or GitHub authority. The stored ZIP remains a neutral `StoredUploadArtifact`.

Statuses are `AVAILABLE`, `CLAIMED`, `PROMOTED`, `EXPIRED` and `CANCELLED`. `PROMOTED`, `EXPIRED` and `CANCELLED` are terminal. An object starts without an owner; ownership appears only during authenticated claim.

## Secrets

Only a lowercase SHA-256 digest of the future high-entropy claim token is persisted. Raw claim tokens must never be put in this table, `sourceReference`, audit text or logs. Step 9.2/9.3 will generate and validate the actual bearer token around this model.

## Concurrency and idempotency

Claim and promotion use row locking (`SELECT ... FOR UPDATE`) and state predicates. Only one claimant may transition `AVAILABLE -> CLAIMED`. Repeating claim with the same owner and same token digest is idempotent after a lost response. A different owner or token receives only a generic unavailable result from the persistence primitive.

Promotion is owner-bound and only allowed from `CLAIMED`. The first promotion binds exactly one ordinary Import id. Retrying with that same id is idempotent; attempting to bind a different Import is rejected. This allows step 9.4 to create the ordinary Import once and safely recover after response loss.

## File modes

`StoredUploadArtifact` can now carry a path-to-`GitFileMode` map. Only Git-relevant ordinary-file values are represented: `100644` and `100755`. Missing metadata is represented by absence, never by filename-based inference.

The existing secure ZIP inspector already extracts Unix mode bits from trustworthy Unix-host central-directory metadata. Step 9.4 will align that metadata with normalized inventory paths and merge absence with the base repository rule: existing paths preserve the snapshot mode; new paths default to `100644`. No `.sh`, `mvnw` or other naming heuristic may create executable state.

## Persistence

Flyway V10 stores artifact metadata, file-mode JSON, claim-token digest, nullable claimed owner, promotion correlation and lifecycle timestamps. Database constraints enforce valid digests, expiry, statuses and owner/promotion presence for claimed/promoted states.
