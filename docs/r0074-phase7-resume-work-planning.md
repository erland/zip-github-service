# r0074 — Phase 7 resume/Work planning refinement

## Scope

This documentation-only revision extends phase 7 with steps 7.19–7.21. No runtime, database or frontend/backend production code is changed. Application version remains `1.0.0-rc.31`.

## Added planning steps

### 7.19 — Gör pågående import fullt återupptagningsbar

Target: an uploaded import that has reached review must survive logout/login and backend restart/deploy without requiring another ZIP upload. Review must reopen against the same plan/base SHA and reuse any already-recorded selection/approval.

### 7.20 — Förenkla Work-vyn till Git-historik och pågående import

Target: Git commits on the active Work branch become the primary user-facing history. The UI should expose at most one active import as a resumable task, while full import history remains available internally for audit, diagnostics and idempotency.

### 7.21 — Slutregression för resume och Work-vy

Target: E2E/security regression for logout/login resume, backend restart at multiple import stages, Work commit history, one active import and cross-user isolation.

## Explicitly deferred

Advanced three-way/concurrent branch evolution detection is not part of these steps. Arbitrary uploaded ZIP files must not be required to contain Zip-GitHub-specific provenance metadata.

## Files changed

Added:
- `docs/r0074-phase7-resume-work-planning.md`

Modified:
- `CHANGELOG.md`
- `docs/implementation-steps.md`
- `docs/implementation-status.md`
- `scripts/verify-release.sh`

Moved: none.

Deleted: none.
