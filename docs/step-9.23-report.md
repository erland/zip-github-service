# Step 9.23 report — product naming in active web client

Date: 13 August 2026  
Revision: `r0139`  
Version: `1.0.0-rc.91`

## Scope

Step 9.23 corrects the active browser document title while deliberately preserving historical `zip-buildserver` references in legacy/migration documentation. It also records the two user-observed follow-up improvements as Steps 9.24 and 9.25 without implementing them in this revision.

## Changes

- `frontend/index.html` now uses `<title>zip-GitHub</title>`.
- A repository-wide inventory confirmed that the remaining `zip-buildserver` occurrences outside `legacy/` are historical migration/baseline documentation, not active runtime product branding. They remain unchanged.
- `scripts/verify-release.sh` now asserts the active title and rejects a return of the legacy title in `frontend/index.html`.
- Step 9.24 is `NEXT`: explicit decisions for every blocking review entry before delivery.
- Step 9.25 is `PENDING`: conservative global cleanup of orphaned `zip-github/work-*` branches across repositories available through the authenticated GitHub App installations.

## Files changed

Added:

- `docs/step-9.23-report.md`

Modified:

- `VERSION`
- `CHANGELOG.md`
- `docs/implementation-steps.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `frontend/index.html`
- `scripts/verify-release.sh`

Moved/deleted: none.

## Safety impact

No authorization, import, review, Work, GitHub mutation or deployment behavior changes in this step. The signed Shortcut is untouched. Steps 9.24 and 9.25 are planning only in this revision.

## Verification

Passed in the delivery environment:

- `bash scripts/verify-implementation-status.sh`
- `bash scripts/verify-structure.sh`
- `bash scripts/security-regression.sh`
- `bash scripts/verify-source-tracking.sh`
- `bash scripts/verify-release.sh`
- `bash scripts/verify-phase9-release.sh`

Full dependency-based verification could not be completed in this environment:

- Backend `bash ./mvnw --batch-mode --no-transfer-progress verify` could not download Maven because `repo.maven.apache.org` could not be DNS-resolved.
- Frontend `npm ci` could not establish a complete dependency installation in the execution environment, so Vitest and `npm run build` could not be run reliably here.

The static/release gates therefore pass, but GitHub CI should remain the integrated verification for Maven and frontend dependency-based tests/build.

## Next step

`9.24` — explicit blocker decisions.
