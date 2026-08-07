# r0084 — Phase 8/9 planning and continuation handoff

Date: 7 August 2026

## Purpose

Planning-only revision. Application version remains `1.0.0-rc.39`; last completed runtime step remains `7.24`; `8.1` remains `NEXT`.

## Decisions

- Former step 8.4 (AI/read-only integration API, Custom GPT/MCP and AI branch-ZIP export) is removed from the active phase-8 execution path and recorded as future backlog.
- Phase 8 now ends at 8.3 after integrated Actions runs/jobs, artifacts/error summaries and controlled dispatch/rerun.
- A new phase 9 contains the Shortcut/StagingImport implementation required to send a ZIP from iOS before normal web authentication.
- Staging is transport-only and must converge through the existing `ZipIngestionService` -> `StoredUploadArtifact` -> `createImportFromStoredUpload(...)` path.
- Upload capability is not user authentication and never grants GitHub/project/read/claim access.
- Claim uses a high-entropy one-time token with only its hash persisted; unclaimed uploads cannot be listed/read and expire quickly.
- Project selection and all GitHub authorization occur after normal login/claim.
- The Shortcut side is included as a reference/client documentation step because it materially validates the staging API, but it remains deliberately thin and contains no GitHub credential or import policy.

## Phase 9 decomposition

- 9.1 persistent StagingImport lifecycle
- 9.2 capability-protected staging upload
- 9.3 authenticated browser claim
- 9.4 Project selection and promotion to ordinary Import
- 9.5 retention/abuse/security regression
- 9.6 iOS Shortcut reference client/install guide
- 9.7 E2E/operations/release gate

## New-chat handoff

`docs/phase8-plus-continuation-handoff.md` is added so the complete r0084 ZIP can be used as the starting artifact in a fresh chat. It records current behavior/invariants, phase 8/9 scope, likely code areas, verification discipline, known environment limitations and a ready-to-use starting prompt.

## Files added

- `docs/shortcut-stagingimport-design.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/r0084-phase8-9-handoff-planning.md`

## Files modified

- `CHANGELOG.md`
- `docs/implementation-steps.md`
- `docs/implementation-status.md`
- `docs/zip-github-development-plan-v1.1.md`
- `docs/zip-github-functional-specification-v1.2.md`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.
