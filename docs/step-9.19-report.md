# Step 9.19 report — prospective `.gitignore` and category bulk selection

## Context

A real `fyrens-vaktare-v0.8.8-repo-cleanup.zip` import exposed a consistency gap. The ZIP added `.gitignore` rules for `__pycache__/` and `*.pyc` while also containing four new `scripts/__pycache__/*.pyc` files. Review used only the repository's old `.gitignore`, selected the `.pyc` files as additions, but workspace Git later hid them with the newly applied `.gitignore`, producing `Local Git diff does not match the approved selection.` The same cleanup also contained a large number of deletion overrides, making per-file acknowledgement impractical.

## Implementation

- `ArchiveInventory` now carries the UTF-8 contents of `.gitignore` files found in the normalized ZIP.
- `ImportComparisonService` builds a prospective ignore map from the complete ZIP: repository `.gitignore` files deleted by the ZIP are removed, ZIP versions overlay retained rules, and only untracked/new files can become `IGNORED`.
- Workspace mismatch errors now state missing and unexpected paths.
- Review adds category-scoped bulk controls. Ordinary changes can be selected/deselected together. Overridable changes use one explicit risk acknowledgement that selects and audits every overridable entry in the active filter/category. Hard blockers are never included.

## Regression coverage

- ZIP-provided `.gitignore` ignores a new `scripts/__pycache__/*.pyc` path in the same import.
- A repository `.gitignore` deleted by a complete ZIP no longer suppresses new files.
- Bulk override selects multiple deletion overrides and a workflow override but leaves a hard-blocked `.git/config` path disabled and absent from the submitted selection.

## Compatibility

No database migration or API shape change is required. Existing already-persisted plans remain immutable; an import whose old plan already selected a now-ignored addition should be cancelled and re-uploaded so rc.84 can create a new plan with prospective ignore semantics.
