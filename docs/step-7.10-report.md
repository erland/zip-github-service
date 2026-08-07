# Step 7.10 report — selection, override and security regression

Revision: `r0063`  
Version: `1.0.0-rc.22`  
Date: 2026-08-07

## Goal

Close the flexible-review phase with regression coverage proving that hierarchical selection, hard blockers, explicit overrides and exact selected delivery preserve the original security invariants.

## Added regression coverage

### Backend selection

`ImportSelectionFactoryTest` now verifies:

- `.git/**` hard blockers cannot be selected even when a client submits an override acknowledgement;
- `.github/**` selection requires an explicit override and the audit record retains path/policy/acknowledgement;
- changing the acknowledgement changes immutable selection identity;
- deletions require explicit override;
- empty, stale plan/base and unknown path requests remain rejected.

### Real Git workspace

`ImportWorkspaceServiceSelfTest` now uses a mixed ZIP/plan with:

- selected ordinary modification;
- excluded ordinary modification and addition;
- selected `.github/workflows/**` override;
- selected deletion override;
- a malicious `.git/config` archive entry that is hard blocked;
- unchanged/unplanned content.

The test asserts the prepared Git diff contains exactly the selected three paths, excluded paths retain their base content, the selected deletion is removed, and archive `.git/config` bytes never reach repository metadata.

### Delivery race

`GitDeliveryServiceSelfTest` now advances the managed work branch after workspace review and verifies delivery rejects the stale approved SHA before creating/pushing another commit.

### Frontend review

The review-tree/page tests now cover:

- directory subtree deselection and partial selection;
- hard blocker disabled state;
- workflow/deletion explicit override;
- exact selection API payload;
- exclusion of hard-blocked/deselected paths;
- disabled approval for an empty selection.

The same DOM interaction model is used at responsive widths, while CSS/mobile layout remains covered by the phase-7 mobile implementation and external Safari acceptance checklist.

## Documentation/security updates

Updated:

- `docs/threat-model.md`
- `docs/security-regression.md`
- `docs/architecture.md`
- `docs/api-contract.md`
- `docs/release-checklist.md`
- `docs/mvp-release.md`
- `scripts/security-regression.sh`

## Verification

Repository release, structure, implementation-ledger, source-tracking and security regression scripts passed.

Additional executable evidence in the packaging environment:

- `ImportWorkspaceServiceSelfTest` compiled with Java 21 plus minimal framework annotation stubs and passed against a real local bare Git repository.
- `GitDeliveryServiceSelfTest` compiled with Java 21 and passed, including sequential work-branch commits and the stale-branch rejection regression.
- A standalone `ImportSelectionFactory` regression runner compiled with Java 21 and passed hard-block, required-override and audited-deletion/workflow scenarios.
- The modified frontend TSX regression files passed TypeScript `transpileModule` syntax diagnostics.

`backend/./mvnw test` was attempted, but Maven Wrapper bootstrap could not resolve `repo.maven.apache.org` in the isolated packaging environment. Full Maven/Vitest/production builds therefore remain the user's local/GitHub CI acceptance authority.


## Files changed in r0063

Added:

- `docs/step-7.10-report.md`

Modified:

- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryServiceSelfTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactoryTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceServiceSelfTest.java`
- `docs/api-contract.md`
- `docs/architecture.md`
- `docs/implementation-status.md`
- `docs/mvp-release.md`
- `docs/release-checklist.md`
- `docs/security-regression.md`
- `docs/threat-model.md`
- `frontend/src/components/ReviewFileTree.test.tsx`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `scripts/security-regression.sh`
- `scripts/verify-release.sh`

Moved: none.

Deleted: none.

## Result

Flexible review steps 7.6–7.10 are complete. The next implementation step is `8.1 — Workflow runs och jobs`.
