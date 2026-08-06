# Step 1.1 report — Domain model and state machines

Date: 6 August 2026  
Revision: r0006

## Delivered

- Added the six planned domain concepts: `Project`, `ImportSession`, `SourceUpload`, `ImportPlan`, `ImportPlanEntry` and `GitHubDelivery`.
- Added explicit `ownerUserId` ownership to every user-owned aggregate.
- Added centralized, allow-list-based status transitions.
- Added idempotent repetition semantics for all state machines.
- Added guards for immutable base SHA and GitHub delivery metadata.
- Added unit tests for allowed, forbidden and idempotent transitions, blocking rules and ownership.
- Added domain documentation.

## Verification

- Pure domain production sources compiled successfully with Java 21 using `javac`.
- Java source inventory verified.
- JUnit tests were added but Maven execution remains unavailable in the current environment, as documented in `docs/baseline-verification.md`.
- Active project structure check passed.
- ZIP integrity check passed.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/Project.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/ImportSession.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/SourceUpload.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/ImportPlan.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/ImportPlanEntry.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/GitHubDelivery.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/DomainTransitionException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/StateTransitions.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/ImportSessionStatus.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/SourceUploadStatus.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/ImportPlanStatus.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/GitHubDeliveryStatus.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/domain/StateTransitionsTest.java`
- `docs/domain-model.md`
- `docs/step-1.1-report.md`

### Modified

- `docs/implementation-status.md`

### Moved

None.

### Deleted

None.
