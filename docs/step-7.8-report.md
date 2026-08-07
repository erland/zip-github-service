# Step 7.8 report — hierarchical file and directory selection

Date: 2026-08-07  
Revision: `r0061`  
Version: `1.0.0-rc.20`

## Implemented

- Replaced the flat review list with a collapsible tree derived from normalized plan paths.
- Ordinary `ADDED` and `MODIFIED` files start selected.
- Directory checkboxes toggle every selectable descendant in the displayed subtree.
- Parent directory checkboxes reflect `checked`, `unchecked` and DOM `indeterminate` state as children change.
- `HARD_BLOCKED`, `OVERRIDABLE_BLOCKED`, `UNCHANGED` and `IGNORED` files cannot be selected in step 7.8.
- Selecting a parent directory never implicitly selects a blocker.
- Directory rows show aggregate counts for new, modified, would-delete, blocked and warning entries.
- File rows expose status and blocker labels, including `WOULD_DELETE` as `Borttagen`.
- Disclosure buttons support keyboard operation and expose `aria-expanded`; file/directory checkboxes have path-specific accessible labels.
- Mobile CSS reduces indentation, moves badges/counts below names and preserves long-path wrapping.

## Transitional safety rule

Step 7.8 intentionally does not connect the client-side selection to Git workspace/delivery; that is the responsibility of step 7.9. To avoid misleading or unsafe behavior, the existing whole-plan approval button is disabled whenever the user changes the default selection. The user can restore the default selection and use the existing delivery path, or continue to step 7.9 where immutable selection and exact selected delivery are connected end-to-end.

## Tests added/updated

- `ReviewFileTree.test.tsx`
  - safe changes selected by default,
  - hard/overridable blockers disabled,
  - directory subtree deselection,
  - parent indeterminate state,
  - collapse/expand behavior,
  - aggregate change counts,
  - deletion and blocker labels.
- `ImportReviewPage.test.tsx`
  - updated for the tree landmark,
  - verifies that a partial selection disables legacy whole-plan approval and that restoring defaults re-enables it.

## Verification

Passed in the packaging environment:

- `scripts/verify-structure.sh`
- `scripts/verify-implementation-status.sh`
- `scripts/security-regression.sh`
- `scripts/verify-source-tracking.sh`
- `scripts/verify-release.sh`
- shell/YAML/JSON repository checks where applicable
- TypeScript syntax transpilation for the changed TSX/test files using the globally available TypeScript compiler API
- final ZIP integrity check

Frontend Vitest/build could not run in the packaging environment because the mounted `node_modules` is incomplete and the `vitest` executable is absent. The added tests therefore require the normal local/CI dependency environment for final execution.

## Files added

- `frontend/src/components/ReviewFileTree.tsx`
- `frontend/src/components/ReviewFileTree.test.tsx`
- `docs/step-7.8-report.md`

## Files modified

- `frontend/src/pages/ImportReviewPage.tsx`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `frontend/src/styles/global.css`
- `VERSION`
- `CHANGELOG.md`
- `docs/implementation-status.md`
- `scripts/verify-release.sh`

## Files moved/deleted

None.

## Next step

`7.9` — implement explicit overrides and exact selected delivery.
