# Step 9.30 report — Maintenance reconciliation and navigation

## Problem

Underhåll previously trusted persisted non-terminal Work status before consulting GitHub. A stale `PR_OPEN` or `PR_CLOSED` therefore caused `Behåll` until another page happened to call Work/PR reconciliation. Visiting the project page could update the same Work to `MERGED`, after which a later maintenance inventory suddenly showed the branch as safe.

## Implementation

- Maintenance now loads all non-terminal Work sessions for each candidate branch.
- Every PR-backed Work is strictly reconciled through the shared `ProjectApplicationService` lifecycle logic before classification.
- The branch's Work usage is re-read after reconciliation.
- Any reconciliation/GitHub uncertainty returns `UNVERIFIED` and disables deletion.
- The independent open-PR-by-head guard remains in place after Work reconciliation.
- Candidate metadata now includes the current user's own project id, repository URL, GitHub branch URL and known PR number/URL.
- The UI has a separate PR column; PR numbers are not duplicated in status/reason text.
- Repository links only expose the current user's own zip-GitHub project. Work sessions owned by other users still participate in safety classification but never contribute an internal project link.
- Branch and PR links open GitHub directly.
- Cleanup still re-runs the full classification immediately before deletion.

## Verification focus

Regression coverage includes stale PR->merged reconciliation in the same maintenance preview, fail-closed GitHub errors, active Work, independent open PR, current-user project-link isolation, GitHub navigation links and fresh classification immediately before delete.
