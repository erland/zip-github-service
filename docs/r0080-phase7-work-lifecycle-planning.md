# r0080 — Phase 7 Work-lifecycle planning refinement

This documentation-only revision reopens phase 7 after step 7.21 and adds steps 7.22–7.24.

## Decisions

- A user can explicitly cancel an active import before Git delivery.
- Cancellation is owner-scoped, idempotent, terminal and frees the Work for a new ZIP while preserving required audit data.
- A Work has at most one active import; new ZIP creation is blocked while one is active.
- The project UI uses state-specific actions instead of a redundant generic “Fortsätt arbete” action.
- After a successful commit, the result page offers both the next ZIP and explicit Work completion / pull-request creation.
- Phase 8 step 8.1 remains pending until 7.22–7.24 are complete.

## Scope

No runtime, database or configuration changes are made in r0080. Application version remains `1.0.0-rc.36`.

## Changed files

Added:
- `docs/r0080-phase7-work-lifecycle-planning.md`

Modified:
- `CHANGELOG.md`
- `docs/implementation-steps.md`
- `docs/implementation-status.md`
- `scripts/verify-release.sh`

Moved: none.

Deleted: none.
