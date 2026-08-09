# rc.80 — Step 9.16 merged-PR race correction

Date: 2026-08-09
Revision: r0128
Version: 1.0.0-rc.80

## Problem

Step 9.16 synchronized pull-request state when Work was viewed, but that synchronization was intentionally best-effort. A PR could therefore be merged on GitHub while the persisted Work still remained `PR_OPEN`. If a new web/Shortcut import arrived before a later UI refresh synchronized the merge, zip-github could reuse the already-completed Work branch.

A second, narrower race also existed if the PR was merged after a new import had been reviewed but before Git delivery.

## Correction

- New import creation performs a strict PR-state reconciliation before reusing any Work that has a pull request.
- If GitHub reports the PR merged, the old Work is persisted as `MERGED` and a new Work is provisioned from the current default-branch HEAD before the import is created.
- The same path is used by normal web upload and stored-upload/Shortcut promotion.
- If PR state cannot be verified, import creation fails closed with `502 WORK_PULL_REQUEST_STATUS_UNAVAILABLE`; zip-github does not risk continuing on a possibly merged branch.
- Git delivery performs the same strict reconciliation immediately before push. If the PR merged after review, delivery is rejected with `409 WORK_PULL_REQUEST_MERGED_REVIEW_REQUIRED` and the ZIP must be reviewed again against the current default branch.
- Presentation-oriented PR synchronization remains best-effort so a temporary GitHub outage does not make normal Work viewing fail.

## Regression coverage

`WorkLifecycleServiceTest` now covers:

1. merged PR detected before a new import -> old Work becomes `MERGED`, new Work/branch starts from current default HEAD;
2. PR status unavailable before a new import -> fail closed and create no new branch/import;
3. PR merges after import/review begins -> delivery preflight blocks the push and marks Work merged when the merge is visible at the final preflight.

No database migration is required.
