# rc.96 — Step 9.25 frontend CI correction

## Problem

GitHub Actions run 31720050112 reached the frontend Vitest suite but failed while loading `MaintenancePage.test.tsx` because `beforeEach` was referenced without being imported from Vitest. The same file also used `afterEach`, `expect` and `test`, so the correction imports all lifecycle/assertion functions explicitly rather than relying on globals.

## Correction

- Import `afterEach`, `beforeEach`, `expect`, `test` and `vi` from `vitest` in `frontend/src/pages/MaintenancePage.test.tsx`.
- Preserve all Step 9.25 production behavior and safety invariants unchanged.
- Add release verification that the maintenance regression keeps explicit Vitest imports.

## Verification

Project release/structure/security/source-tracking gates are rerun for rc.96. Frontend `npm test` and `npm run build` are attempted when dependencies are available.
