# rc.78 – Step 9.17 frontend CI correction

## Bakgrund

GitHub Actions run `31323190916`, frontend job `93269292754`, failed in `Run frontend tests`. Backend and repository release/security gates were already green. The frontend build step was skipped because Vitest failed first.

Step 9.17 intentionally changed interactive delivery so the commit message starts empty and approval remains disabled until the user enters a non-blank message. Several older frontend regressions still clicked `Godkänn valda förändringar` without entering a message and therefore correctly remained on the review page while the tests incorrectly expected delivery/result state.

## Korrigering

- Delivery-path tests enter an explicit, test-specific commit message before approval.
- The default review test now expects approval to be disabled while the message is blank.
- The external-branch-change test verifies both independent gates: entering a commit message is insufficient until the external-change acknowledgement is also given.
- The simplified end-to-end import regression now includes explicit commit-message entry before one-click delivery.
- `scripts/verify-release.sh` contains focused checks for these rc.78 regressions.

## Scope

No production frontend/backend behavior is changed. Step 9.17 remains the completed implementation step; rc.78 is a test/release-gate correction only.
