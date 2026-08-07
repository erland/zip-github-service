# Step 7.15 report — unchanged protected paths

Completed: 2026-08-07  
Application version: `1.0.0-rc.28`  
Repository revision: `r0070`

## Implemented

- Bumped deterministic import-policy identity to `mvp-3`.
- `.github/**` is now `OVERRIDABLE_BLOCKED` only when the comparison says `ADDED`, `MODIFIED` or `WOULD_DELETE`.
- An `UNCHANGED` `.github/**` entry remains `UNCHANGED`, has blocker type `NONE`, and requires no change override.
- Kept archive/content safety independent of diff-aware workflow protection: `.git/**`, oversized files and high-risk credential filenames continue to use their existing hard safety rules.
- Added policy regression covering unchanged, modified, added and deleted workflow files in one deterministic test.

## Rationale

Override is an acknowledgement of a repository write risk. If bytes/path are unchanged, no repository write occurs for that file, so a path-only `.github/**` change override would be misleading and creates needless friction.

## Verification

- Policy self-test / focused Java compilation where available.
- `scripts/verify-structure.sh`
- `scripts/verify-implementation-status.sh`
- `scripts/security-regression.sh`
- `scripts/verify-source-tracking.sh`
- `scripts/verify-release.sh`
- ZIP integrity check.

## Files

Added:
- `docs/step-7.15-report.md`

Modified:
- `backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/policy/ImportPolicyServiceTest.java`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `docs/import-policy.md`
- `docs/api-contract.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

Moved: none.

Deleted: none.
