# Exact import-plan approval

Step 5.1 adds an explicit approval record for the immutable import plan. The client must submit the exact `planDigestSha256` returned by the plan API. The server compares the submitted digest with the stored immutable plan before recording approval.

## API

`POST /api/imports/{importId}/plan/approval`

```json
{
  "planDigestSha256": "<64 lower-case hex characters>"
}
```

A successful response contains the import ID, plan ID, approved digest, `APPROVED` status and approval timestamp. Repeating the same request is idempotent and returns the original approval. A different digest is rejected with `409 IMPORT_PLAN_DIGEST_MISMATCH`. A plan with blocking policy entries is rejected with `409 IMPORT_PLAN_BLOCKED`.

## Security and integrity

Approval requires ownership of the import and plan. The server never trusts a client-supplied approvability flag or plan contents. It only accepts the digest of the immutable server-side plan and changes the import session status to `APPROVED` after the audit record has been created.

The current application service persists the approval in its temporary in-memory store. Migration `V4__import_plan_approval_audit.sql` prepares the database model with `approved_at` and `approved_by_user_id` consistency constraints for the later repository-backed persistence step.
