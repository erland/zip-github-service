# Step 9.24 report — explicit blocker decisions

Revision: `r0140`  
Release candidate: `1.0.0-rc.92`  
Date: 2026-08-13

## Result

Step 9.24 is complete. A blocking review entry can no longer disappear from an import merely because the user did not interact with it.

## User-facing behavior

- Every overridable blocker starts unresolved and requires `Ta inte med` or `Godkänn och ta med`.
- Every hard blocker stays unselectable and requires acknowledgement that it will be omitted.
- The review summary reports how many blocking entries still require decisions.
- The primary approval/delivery action remains disabled while any blocker is unresolved.
- The active category can explicitly exclude all overridable blockers or explicitly approve/select them in bulk.
- Ordinary directory/category selection cannot implicitly change blocker decisions.

## Immutable selection contract

- New selections use `selection-2`.
- The request/response contract carries `blockerDecisions`.
- Decisions are persisted with the immutable selection and included in the selection SHA-256 identity.
- Backend validation requires exact blocker-decision coverage and consistency with selected paths and override audit records.
- Hard-blocked paths remain impossible to select.
- Existing/legacy selections without complete blocker decisions cannot newly pass approval or resume delivery when their plan contains blockers.

## Verification

Static project/release/security gates are run for this revision. Full Maven and frontend dependency-based verification may require network-accessible dependency repositories; any unavailable gate is reported separately in the delivery note.
