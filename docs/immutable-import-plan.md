# Immutable import plan

Step 4.4 stores the exact review material produced from one uploaded ZIP and one locked repository commit.

## Identity

An import plan contains the source upload SHA-256, base commit SHA, policy version, sorted entries and a canonical SHA-256 plan digest. The digest excludes generated identifiers and timestamps, so recomputing the same source material yields the same identity.

## Immutability and idempotency

Only one plan may be stored per import session. Repeating plan creation with the same digest returns the existing plan. A different digest is rejected with `IMPORT_PLAN_IMMUTABLE`; callers must create a new import session rather than mutate reviewed material.

## Status

An approvable plan is stored as `READY`. A plan containing blocking policy entries is stored as `DRAFT` and remains available for review, but cannot be approved in step 5.1.

## Current persistence boundary

The active application service still uses the prototype in-memory store. The database schema already contains `import_plan` and `import_plan_entry`; migration V3 adds source-upload and canonical plan digests so the same immutability identity can be persisted when repository-backed application services replace the temporary maps.
