# r0058 — flexible review planning update

Date: 2026-08-07
Application version: `1.0.0-rc.17` (unchanged)
Next implementation step: `7.6`

## Decision

The review model is extended before phase 8 with a hierarchical, selection-aware approval flow.

The original `ImportPlan` remains an immutable description of the complete ZIP-versus-base comparison. A later immutable `ApprovedSelection` will describe exactly what the user chose to include, exclude and override.

### Tree selection

- Changes are presented as a collapsible directory/file tree.
- Ordinary selectable `ADDED` and `MODIFIED` files are selected by default.
- Deselecting a directory deselects every selectable descendant.
- Selecting/deselecting children updates the directory to checked, unchecked or indeterminate state.
- Directories with mixed changes show aggregate counts/status rather than pretending the whole directory has one file status.

### Blocker semantics

- Archive-invalid conditions such as path traversal, NUL, unsafe special files and ZIP-bomb/resource-limit violations reject the ZIP before a plan is created and cannot be overridden.
- `.git/**` is `HARD_BLOCKED`: visible in review, excluded by default and never selectable. Its presence does not prevent the rest of the ZIP from being committed.
- `.github/**` and `WOULD_DELETE` are `OVERRIDABLE_BLOCKED` by default: excluded initially, but selectable after explicit, auditable override.
- Directory selection never implicitly overrides a blocked child.

This is intentionally slightly safer than "everything selected by default": ordinary changes are selected by default, while risky changes require a deliberate action.

## Planned implementation steps

- `7.6` blocker levels and non-fatal policy blockers.
- `7.7` immutable selection model and API.
- `7.8` hierarchical file/directory selection UI.
- `7.9` explicit overrides and exact selected delivery.
- `7.10` E2E/security regression and documentation completion.

Phase 8 returns to `PENDING` until these review enhancements are complete.

## Files added

- `docs/zip-github-functional-specification-v1.1.md`
- `docs/r0058-flexible-review-planning.md`

## Files modified

- `AGENTS.md`
- `CHANGELOG.md`
- `docs/README.md`
- `docs/implementation-status.md`
- `docs/implementation-steps.md`
- `scripts/verify-release.sh`

## Files retained for history

- `docs/zip-github-functional-specification-v1.0.md` remains unchanged as the previous specification revision.

## Verification

Passed:

```text
./scripts/verify-implementation-status.sh
./scripts/verify-structure.sh
./scripts/security-regression.sh
./scripts/verify-source-tracking.sh
./scripts/verify-release.sh
```

No application code, database schema or runtime configuration changed in r0058.
