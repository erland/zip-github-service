# rc.63 frontend CI correction

## Scope

This revision corrects the single frontend regression still failing in GitHub Actions run `31265080691`, job `93121812222`. Production behavior and the completed phase-9 implementation state are unchanged.

## Observed failure

GitHub Actions ran 47 frontend tests; 46 passed and only `ProjectDetailPage degraded Work history > shows the persisted Work head fallback while keeping the resumable import actionable` failed. The test used `getByRole('status')`, but the final 9.9 Work page legitimately renders two simultaneous live-status regions: the Actions loading state and the degraded Git-history fallback message.

## Correction

The regression now asserts the exact fallback text `GitHub-historiken kunde inte läsas just nu. Senaste lokalt kända commit visas.` instead of assuming the page contains exactly one `role="status"` element.

## Files

Added:
- `docs/rc63-frontend-ci-correction.md`

Modified:
- `CHANGELOG.md`
- `VERSION`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `frontend/src/pages/ProjectDetailPage.test.tsx`
- `scripts/verify-release.sh`

Moved: none.
Deleted: none.
