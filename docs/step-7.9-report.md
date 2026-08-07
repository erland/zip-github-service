# Step 7.9 report — explicit overrides and exact selected delivery

Revision: `r0062`  
Version: `1.0.0-rc.21`

## Implemented

- Review selection is submitted to `POST /api/imports/{importId}/selection` before approval.
- `OVERRIDABLE_BLOCKED` paths require an explicit per-path acknowledgement in the review tree.
- Directory selection never selects blocker paths implicitly.
- Approval is bound to both `planDigestSha256` and `selectionDigestSha256`.
- Once a selection has been created, review controls are locked; a failed approval request can be retried against the same selection digest.
- Workspace preparation receives the approved selection and applies only selected changes.
- Selected `WOULD_DELETE` entries are deleted from the checked-out base commit.
- Selected archive files are verified byte-for-byte against plan SHA-256/size identity.
- The complete local Git diff must exactly equal `selectedPaths`; excluded paths therefore cannot leak into the commit.
- Hard blockers are rejected again at workspace preparation and selected overridable blockers must have corresponding override audit records.
- Prepared workspace metadata is bound to the selection digest.

## Verification

Repository/static release gates are run before packaging. Full Maven and frontend dependency-based tests require the normal local/CI environment because this packaging environment cannot download Maven/npm dependencies.

## Next

Step `7.10` — selection, override and security regression.
