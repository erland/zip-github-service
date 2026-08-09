# Step 9.11 report — Actions visibility, shared Work/result UI and richer diagnostics

Revision: `r0114`  
Version: `1.0.0-rc.66`  
Date: 2026-08-08

## Trigger

A real push workflow run (`31258714926`) matched the latest Work branch commit (`f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69`) in GitHub but was not visible reliably in zip-github. The Work Actions view was also functionally richer than the commit/result implementation in some areas, while condensed errors often omitted the useful lead-up to a failure.

## Changes

- Workflow runs are preserved if the secondary jobs endpoint or check-runs endpoint fails.
- A 403 from the primary Actions endpoint is returned as explicit `ACTIONS_PERMISSION_REQUIRED` diagnostics rather than `not_started`.
- The GitHub App guide now calls out owner approval after permission upgrades. Current product functionality requires Actions read/write because status/logs are read-only but allowlisted dispatch/rerun is write.
- Added one shared `ActionsPanel` and `ActionsControls` frontend implementation used by both Work and import-result views.
- Exposed owner-scoped `lastImportId` in the active Work response so Work can reuse the existing import-bound Actions control service and audit/idempotency policy.
- Failed jobs now return: existing condensed lines, a sanitized context window (40 before / 12 after), sanitized bounded job-log lines, and a truncation flag.
- Job-log download cap increased from 24 KiB excerpt-only to 128 KiB bounded diagnostics, with a 1600-line UI cap after sanitization.
- Shared UI offers `Kopiera fel med sammanhang` and `Kopiera jobblogg` plus the canonical GitHub job/run links.

## Security

All log text is sanitized before it enters API response fields. Existing ANSI/control stripping and GitHub token/Bearer/Authorization/common-secret redaction remains in force. Log bytes are request-local and are not persisted. Full original logs remain on GitHub.

## Verification

Passed in the delivery environment:

- `scripts/verify-implementation-status.sh`
- `scripts/verify-structure.sh`
- `scripts/security-regression.sh`
- `scripts/verify-source-tracking.sh`
- `scripts/verify-phase9-release.sh`
- `scripts/verify-release.sh`
- shell syntax for `scripts/*.sh`
- YAML parsing for CI and both Compose files
- syntax transpilation of all frontend TypeScript/TSX sources
- dependency-free Java compilation of the changed Actions DTO/model/log-condensing surface
- `ActionLogCondensorSelfTest`, including pre-error context and secret redaction

Backend regressions cover the observed run, Checks failure, primary Actions 403 and jobs-endpoint failure. Frontend regressions exercise the shared component through Work/result tests and a dedicated `ActionsPanel` regression.

A full `npm ci`/Vitest run could not be completed in the sandbox because the internal npm proxy returned 404 for `yallist-3.1.1.tgz`. A full Maven run could not bootstrap because `repo.maven.apache.org` could not be resolved. Those are environment limitations rather than test failures; normal GitHub Actions CI remains the authoritative full dependency-backed verification.
