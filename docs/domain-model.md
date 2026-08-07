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

### ImportPlan, ImportPlanEntry and ApprovedSelection

`ImportPlan` is an immutable comparison snapshot containing base SHA, policy version and immutable entries. `ImportPlanEntry` classifies one normalized path and its policy result. Flexible review does not mutate this plan: the complete ZIP-versus-base result remains available for audit.

`ApprovedSelection` is a second immutable object bound to the owner, import ID, plan ID, plan digest and locked base SHA. It contains canonical selected paths, server-computed excluded paths, explicit override audit records and a deterministic `selectionDigestSha256`. `HARD_BLOCKED` paths can never be selected. From step 7.9 this selection is the exact workspace/delivery contract. Approval binds the plan and selection digests together, and prepared workspaces carry the selection digest in addition to the plan digest.

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

## Persistent work session (RC15)

A project has at most one `ACTIVE` work session. The work session owns a stable Git branch (`zip-github/work-<workId>`) and records its base branch, initial base commit, current head commit, latest import, and optional final pull request metadata. Each ZIP import remains an independently reviewed immutable plan, but successful deliveries append one commit to the active work branch. Creating the final pull request changes the work session status to `PULL_REQUEST_CREATED`; the next import then creates a new work session.


## Import source audit metadata

Every normal import has an explicit non-secret `ImportSource`: `WEB_UPLOAD`, `STORED_UPLOAD` or the reserved future `STAGING_IMPORT`. An optional `sourceReference` may contain an internal correlation identifier such as `stored-upload:<artifact-uuid>`. Capability tokens, claim tokens, OAuth tokens and credentials must never be stored in source audit metadata. The source is diagnostic only and must not change comparison, policy, selection, plan digest or Git delivery semantics.
