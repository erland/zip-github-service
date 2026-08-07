# Step 7.7 report — immutable selection model and API

Date: 2026-08-07  
Repository revision: `r0060`  
Application version: `1.0.0-rc.19`

## Implemented

- Kept `ImmutableImportPlan` unchanged as the complete ZIP-versus-base review artifact.
- Added immutable `ApprovedSelection` containing owner/import/plan identity, base SHA, selection version/digest, selected paths, excluded paths, override audit entries and timestamp.
- Added immutable `ApprovedSelectionOverride` with path, blocker type, captured policy code and explicit acknowledgement.
- Added deterministic `selection-1` digest generation independent of request path order and timestamps.
- Added server-side validation for current plan digest/base SHA, normalized unique paths, non-empty selection and plan membership.
- `HARD_BLOCKED` entries cannot be selected under any circumstances.
- `OVERRIDABLE_BLOCKED` entries require an explicit matching acknowledgement before the API accepts them.
- Added owner-scoped immutable storage to `ProjectApplicationService`; an identical replay is accepted, but a different replacement is rejected.
- Added `POST /api/imports/{importId}/selection` and `GET /api/imports/{importId}/selection`.
- Added JUnit coverage for deterministic digest, immutability, hard blockers, override audit, stale plan/base, unknown paths and cross-user access.

## Deliberate scope boundary

The existing approval/workspace/delivery path is left unchanged in this step so the RC18 user flow is not broken before the review UI exists. Step 7.8 will create the hierarchical file/directory selector. Step 7.9 will bind approval, workspace application, deletions and Git delivery to exactly the immutable selection.

Selection storage currently follows the same server-side in-memory lifetime as import plans/import execution state. Project/work metadata persistence remains separate. Durable import/selection persistence is not introduced by step 7.7.

## Verification

Run where dependencies are available:

```bash
cd backend
./mvnw verify
```

Repository-local verification:

```bash
./scripts/verify-structure.sh
./scripts/verify-source-tracking.sh
./scripts/verify-implementation-status.sh
./scripts/security-regression.sh
./scripts/verify-release.sh
```

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/selection/ApprovedSelection.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/selection/ApprovedSelectionOverride.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactory.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/CreateImportSelectionRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportSelectionResponse.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactoryTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/ImportSelectionResourceTest.java`
- `docs/step-7.7-report.md`

## Files modified

- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `docs/api-contract.md`
- `docs/domain-model.md`
- `docs/implementation-status.md`
- `CHANGELOG.md`
- `VERSION`
- `scripts/verify-release.sh`

No files were moved or deleted.
