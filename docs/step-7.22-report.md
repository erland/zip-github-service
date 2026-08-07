# Step 7.22 report — cancel and close active import

## Result

Implemented explicit import cancellation before Git delivery. `POST /api/imports/{importId}/cancel` is owner-scoped and idempotent, persists `CANCELLED`, removes any temporary Git workspace and leaves immutable review/audit records intact. A recorded Git delivery blocks cancellation with `409 IMPORT_ALREADY_DELIVERED`.

The review UI and resumable upload/retry view now expose `Avbryt import` with an explicit confirmation. Successful cancellation returns to the project so a later step can expose the correct next ZIP action. Cancelled source ZIPs are treated as terminal for retention cleanup once their normal retention deadline has elapsed.

## Verification

- Added `ImportCancellationTest` for pre-delivery cancellation, idempotent retry, owner isolation, cleanup eligibility and post-delivery rejection.
- Added frontend review regression ensuring cancellation performs only the cancel request and does not create selection, approval or delivery.
- Added resumable-upload regression for cancelling an existing import instead of retrying the upload/preparation flow.
- Repository structure, security regression, source tracking and release verification are run before packaging.

## Scope boundary

Step 7.23 remains responsible for enforcing at most one active import per Work and for simplifying the project/result action set.
