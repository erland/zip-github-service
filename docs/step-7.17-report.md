# Step 7.17 report — one-click approval and commit

Repository revision: `r0072`  
Application version: `1.0.0-rc.30`  
Completed: 7 August 2026

## Outcome

The normal review flow now has one user action between review and the pushed commit. Clicking **Godkänn valda förändringar** performs the existing security boundaries in order: immutable selection, persistent approval, verified workspace, commit and non-force push. The UI then opens the existing result page.

No GitHub write is attempted before the immutable selection has been validated and the approval has been recorded.

## Recovery and refresh

A recorded selection and approval can now be reloaded from the review page. `GET /api/imports/{importId}/plan/approval` exposes the existing owner-scoped approval metadata for recovery. On refresh the frontend restores the exact selected paths and overrides from the immutable selection and locks the tree.

If approval exists but delivery is not recorded, the normal approval button is replaced by **Försök skapa commit igen**. This action reuses the existing approval and idempotent workspace/delivery semantics rather than creating another approval or selection. If delivery is already recorded, reopening the review route redirects to the result page.

## Verification

- frontend regression covers the one-click sequence `selection -> approval -> workspace -> delivery -> result`;
- partial selection and explicit override payloads remain exact;
- hard blockers are never selected;
- refresh recovery restores selection/approval and only exposes the recovery delivery action;
- backend resource regression covers reading an already recorded approval;
- repository structure, source tracking, security regression, implementation ledger and release verification remain required release gates.
