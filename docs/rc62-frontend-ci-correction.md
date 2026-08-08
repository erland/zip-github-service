# rc.62 frontend CI correction

Revision: r0110  
Application version: 1.0.0-rc.62

## Observed CI failure

GitHub Actions run `31264592717`, frontend job `93120582768`, completed `npm ci` successfully and ran 47 Vitest tests. 44 passed and three frontend tests failed.

## Root causes and corrections

1. `App.test.tsx` still expected the removed pre-9.8 `Starta arbete` link. The test now exercises the real final lifecycle: open project -> create verified Work branch -> open `Ladda upp nästa ZIP`.
2. `ProjectDetailPage.test.tsx` used a global single-element SHA assertion even though 9.9 intentionally renders the same current Work SHA in Actions status, workflow run and commit history. The assertion is now scoped to the `Commits i arbetet` section.
3. The clipboard spy was installed before `userEvent.setup()`, which replaces the jsdom clipboard shim. The test now installs its `navigator.clipboard.writeText` spy after user-event setup.

No production code changed and phase 9 remains complete.

## Files

Added:
- `docs/rc62-frontend-ci-correction.md`

Modified:
- `CHANGELOG.md`
- `VERSION`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `frontend/src/App.test.tsx`
- `frontend/src/pages/ProjectDetailPage.test.tsx`
- `scripts/verify-release.sh`

Moved: none.  
Deleted: none.
