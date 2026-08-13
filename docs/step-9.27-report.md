# Step 9.27 report — confirmation before extending PR_OPEN Work

Completed in r0146 / 1.0.0-rc.98 on 2026-08-13.

## Result

- `NewImportPage` fetches reconciled Work state and blocks new ZIP selection while an existing PR is open until the user explicitly confirms continuation.
- The warning links to the current pull request when available and offers cancellation back to the project.
- Shortcut/staging promotion applies the same confirmation rule when the selected repository already maps to a project with `PR_OPEN` Work.
- `CreateImportRequest` carries an explicit `confirmOpenPullRequest` flag. Backend import creation performs strict PR reconciliation first and rejects unconfirmed reuse with `OPEN_PULL_REQUEST_CONFIRMATION_REQUIRED`.
- If GitHub reports the prior PR merged, existing lifecycle logic terminates that Work and starts a fresh Work from the current default branch.
- Existing resumable imports are not forced through the confirmation again.

## Regression coverage

- frontend: open PR warning + explicit continue, no warning without open PR, Shortcut confirmation;
- backend: unconfirmed PR_OPEN is rejected, confirmed PR_OPEN is reused, merged PR starts fresh Work, unavailable PR state fails closed.
