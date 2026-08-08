# Planning revision r0091 — user-controlled commit message

Date: 2026-08-08  
Application version: `1.0.0-rc.44` (unchanged)  
Next implementation step: `9.1`

## Purpose

Add the missing explicit commit-message choice to phase 9 without implementing runtime code yet. The feature belongs to the ordinary Import approval/delivery pipeline and therefore applies equally to browser upload and StagingImport promotion.

## Planning decision

- New dedicated step 9.5 implements an editable commit-message field in the common review/approval flow.
- The current generated message may be offered as an editable default suggestion; the user can replace it completely.
- Interactive flows validate a non-empty normalized message server-side.
- The final message is persisted in restart-safe state, bound to explicit approval/delivery and reused unchanged for retry/idempotency.
- Compatibility fallback is limited to legacy resume/internal callers that lack the new field.
- Former steps 9.5–9.7 become 9.6–9.8.
- Final 9.8 regression covers browser/StagingImport parity, restart and retry/no-duplicate behavior for the selected message.

## Verification

Planning-only revision; no runtime source was changed. Repository verification scripts were run after the documentation/ledger update.

## Files added

- `docs/planning-revision-r0091-commit-message.md`

## Files modified

- `CHANGELOG.md`
- `docs/implementation-status.md`
- `docs/implementation-steps.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/planning-revision-r0090-file-mode.md`
- `docs/shortcut-stagingimport-design.md`
- `scripts/verify-release.sh`

## Files moved

- None.

## Files deleted

- None.
