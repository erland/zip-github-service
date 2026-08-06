# Step 2.1 report — GitHub technical spike

Date: 6 August 2026  
Revision: `r0010`

## Outcome

`DONE`

The GitHub delivery chain was verified against `erland/got-test-repo` using an isolated branch and draft pull request. Repository access, immutable base-SHA resolution, branch creation, commit creation, pull-request creation and commit-status lookup all completed successfully.

Evidence is recorded in `docs/github-technical-spike.md`. GitHub pull request `#2` is retained as external audit evidence and was not merged.

## Verification performed

- Authenticated GitHub profile resolved to `erland`.
- Private repository metadata and write permissions read successfully.
- `main` resolved to `bf0058cc0871daa556c6b65292096b0e03efbd94`.
- Branch `zip-github/spike-20260806-1506` created from the exact SHA.
- One file committed with SHA `dd1fa5d2c06da887ce2e1e34ef6d8381a51c598c`.
- Draft PR `#2` opened against `main`.
- Combined commit-status lookup succeeded and returned an empty status collection.
- `scripts/github-app-spike.sh` passed `bash -n`.
- Project structure and ZIP integrity checks passed.

## Limitations

- The spike used the connected GitHub identity, not a newly registered zip-github GitHub App.
- OAuth login, App JWT generation and installation-token lifecycle are intentionally deferred.
- The test repository has no status checks for the spike commit, so the empty-status behavior was verified rather than a running Actions workflow.

## Files changed

### Added

- `docs/github-technical-spike.md`
- `docs/step-2.1-report.md`
- `scripts/github-app-spike.sh`

### Modified

- `docs/implementation-status.md`

### Moved

None.

### Deleted

None.
