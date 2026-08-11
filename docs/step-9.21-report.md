# Step 9.21 report — shared repository picker and recent repositories

Date: 2026-08-11
Revision: r0137
Version: 1.0.0-rc.89

## Delivered

- Added `RepositoryPicker` and reused it on the repository landing page and Shortcut claim page.
- Added repository-name/full-name search to Shortcut claim.
- Added a bounded, independently scrollable repository list.
- Added up to five recently used repositories backed by local browser storage as a non-authoritative convenience.
- Added an explicit selected-repository summary immediately before the Shortcut continue action.
- Kept the complete repository list in backend-provided/alphabetical order and did not add automatic scrolling.
- Documented Step 9.22 for confidence-ranked Shortcut repository suggestions using filename/history/recency.

## Security and behavior

Recent repository storage contains only installation/repository identifiers already visible to the authenticated browser session. It is advisory UI state only: repository availability and promotion still use the current authenticated backend repository list. A stale recent identifier is silently ignored if that repository is no longer available.

## Verification

- Existing project release/security/source-tracking gates.
- TypeScript compile/build where local dependencies permit.
- Frontend regression coverage for filtering, recent repositories and Shortcut selected-repository summary.

## Next

Step 9.22 — Smart Shortcut repository suggestion.
