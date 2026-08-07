# Import policy and blockers

Step 4.3 introduced deterministic policy evaluation after archive normalization and hash comparison. Step 7.6 refines that model so a policy-blocked path no longer necessarily blocks the entire ZIP.

## Policy version

The flexible-review policy is identified as `mvp-2`. The version is returned by the API and is included in the immutable plan digest so approval can always be tied to the exact rules that were applied.

## Outcomes and blocker types

Each policy entry retains its normalized comparison state and receives a policy severity plus a blocker type:

- `NONE`: ordinary or warning-only entry.
- `HARD_BLOCKED`: may never be selected for delivery.
- `OVERRIDABLE_BLOCKED`: excluded by default; a later explicit selection/override may include it.

Blocked entries continue to use file status `BLOCKED` in the current API, while `comparisonStatus` preserves the underlying `ADDED`, `MODIFIED` or `WOULD_DELETE` meaning.

A mixed plan is reviewable when at least one ordinary `ADDED` or `MODIFIED` entry remains. Policy-blocked entries are excluded from the current default delivery set, so the presence of `.git/**` or `.github/**` does not force the user to create a new ZIP merely to deliver unrelated safe files. Step 7.7 makes this default set explicit in a separate immutable selection model.

A plan containing only blocked/ignored/unchanged entries remains non-approvable because the current implementation would otherwise create an empty commit.

## Hard blockers

- `.git` and `.git/**`: repository metadata may never be imported or selected.
- Files larger than `zipgithub.archive.max-single-file-bytes`: hard resource-policy boundary.
- High-risk credential filenames and key containers: common SSH private-key names plus `.pem`, `.key`, `.p12`, `.pfx`, `.jks`, and `.keystore` files.

These paths are visible in the plan for transparency but cannot become part of a commit.

## Overridable blockers

- `.github` and `.github/**`: excluded by default; explicit override support is introduced in steps 7.7–7.9.
- `WOULD_DELETE`: deletion is excluded by default; explicit override is required before a later selection may include it.

Step 7.6 only introduces the taxonomy and non-fatal default exclusion. It intentionally does not yet provide a UI/API for selecting an overridable blocker.

## Warning rules

`.env` and environment-specific `.env.*` files receive a warning because they commonly contain secrets. `.env.example` is explicitly allowed without warning.

The high-risk-secret hard-block rule remains filename based. Content scanning for secret material can be added as defence in depth later; filenames alone must not be treated as proof that a secret exists.

## API

`POST /api/imports/{importId}/policy` and the persisted plan response expose:

- total blocker count,
- `hardBlocked` count,
- `overridableBlocked` count,
- per-entry `blockerType`,
- named `policyCode` and message,
- locked base SHA and policy version.

The plan digest includes `blockerType`, so changing blocker classification changes the immutable plan identity.
