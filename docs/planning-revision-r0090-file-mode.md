# Planning revision r0090 — Git file-mode preservation in phase 9

Date: 8 August 2026  
Application version: 1.0.0-rc.44 (unchanged; planning-only revision)  
Next implementation step: 9.1

## Reason

CI after phase 8 exposed that a ZIP→GitHub roundtrip cannot safely rely on Unix executable-bit metadata surviving every ZIP creation/extraction/write path. Invoking shell files through `bash` is a robust CI mitigation, but zip-github should still preserve Git file modes as project semantics.

## Planning decision

Phase 9 now owns the correction so the StagingImport representation is designed correctly from the start:

- 9.1: carry trustworthy per-entry executable metadata and define deterministic fallback rules.
- 9.4: integrate file modes into the ordinary import comparison/review/approval/delivery path; preserve base mode for existing files when source metadata is absent; default new files to `100644`; never infer executable status from filename.
- 9.7 at r0090 planning time (renumbered to 9.8 by r0091): E2E/regression for mode preservation/change and equivalence between browser upload and StagingImport.

Supported ordinary-file Git modes are initially `100644` and `100755`. Mode changes are approval-bound changes and must be part of staged-diff verification before commit.

## Verification

This revision contains documentation/planning changes only. Repository verification scripts were run after the update; no phase-9 runtime code was implemented.

## Files

### Added
- `docs/planning-revision-r0090-file-mode.md`

### Modified
- `CHANGELOG.md`
- `docs/implementation-status.md`
- `docs/implementation-steps.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/shortcut-stagingimport-design.md`
- `scripts/verify-release.sh`

### Moved
- None.

### Deleted
- None.
