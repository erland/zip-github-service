# Domain model and state machines

Version 1.0  
Date: 6 August 2026  
Implementation step: 1.1

## Design rules

- Every user-owned aggregate carries `ownerUserId`; later repositories and API services must scope reads and writes by both resource id and owner.
- Domain objects do not depend on REST, persistence or GitHub client code.
- Status changes go through named transition methods and the shared `StateTransitions` guard.
- Repeating the current status is idempotent and returns `false`.
- Any transition not explicitly listed is rejected with `DomainTransitionException`.
- Comparison base SHA, delivery branch, commit SHA and pull request metadata become immutable once recorded.

## Aggregates and entities

### Project

A user-owned configuration binding zip-github to one GitHub repository and default branch. GitHub installation and repository identifiers must be configured together.

### ImportSession

Coordinates one import from creation, upload and inspection through approval and delivery. It owns the frozen base branch and base commit SHA.

Main path:

`CREATED → UPLOADING → INSPECTING → PLAN_READY → APPROVED → DELIVERING → PULL_REQUEST_CREATED → COMPLETED`

Failure/terminal states include `UPLOAD_FAILED`, `INSPECTION_FAILED`, `BLOCKED`, `DELIVERY_FAILED`, `CANCELLED` and `EXPIRED`. `DELIVERY_FAILED` may retry delivery.

### SourceUpload

Stores metadata for the temporary ZIP package. Archive bytes remain outside the domain object. A stored upload requires recorded byte size and SHA-256.

Main path:

`CREATED → UPLOADING → STORED → VALIDATING → VALIDATED`

Rejected, failed and expired uploads can be deleted.

### ImportPlan and ImportPlanEntry

`ImportPlan` is an immutable comparison snapshot containing base SHA, policy version and immutable entries. `ImportPlanEntry` classifies one normalized path and its policy result. A plan with blocked entries cannot be approved.

Plan path:

`DRAFT → READY → APPROVED`

A ready plan may instead be rejected, superseded or expired.

### GitHubDelivery

Tracks one idempotent delivery of an approved plan. It records branch, commit and pull request metadata once those values exist.

Main path:

`CREATED → PREPARING → COMMITTING → PUSHING → BRANCH_PUSHED → CREATING_PULL_REQUEST → PULL_REQUEST_CREATED → COMPLETED`

Failures retain enough state for a controlled retry. Exact retry rules will be tightened with delivery implementation in step 5.5.

## Persistence boundary

Step 1.1 defines the domain only. Database entities, constraints and Flyway migrations are intentionally deferred to step 1.2.
