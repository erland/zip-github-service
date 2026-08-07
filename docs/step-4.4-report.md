# Step 4.4 report — immutable import plan

## Result

Implemented immutable, owner-scoped import-plan creation and retrieval. The plan is tied to the stored ZIP SHA-256, locked repository commit, policy version and canonical sorted entries. A deterministic SHA-256 digest provides the exact identity that step 5.1 will approve.

## API

- `POST /api/imports/{importId}/plan`
- `GET /api/imports/{importId}/plan`

Creation is idempotent for an unchanged digest and rejects replacement with `409 IMPORT_PLAN_IMMUTABLE`.

## Verification

- Standalone Java digest self-test passed.
- Plan entries are defensively copied and sorted.
- Approveable and blocked status mapping is covered by JUnit tests.
- Structure and implementation-status checks passed.
- ZIP integrity passed.
