# rc.102 — Step 9.29 empty default-branch correction

## Symptom

Starting Work for a completely new GitHub repository could return `The request could not be completed.`

## Root cause

Before the first commit GitHub may not expose a usable `default_branch` value. rc.101 accepted the repository as empty but could carry the blank value into project persistence. The database intentionally rejects blank `project.default_branch`, so the insert escaped the normal API error contract as an unexpected persistence failure.

## Correction

- Normalize null/blank GitHub default-branch metadata.
- If the repository has zero branches, resolve the bootstrap branch to `main` before persisting the project.
- Keep initialized repositories fail-closed when default-branch metadata is unavailable.
- Never interpret JSON null as a literal branch named `null`.

The empty-root-commit bootstrap and all existing exact-selection, locked-SHA, delivery-parent and PR invariants remain unchanged.
