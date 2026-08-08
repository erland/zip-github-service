# Step 9.8 report — Work lifecycle, project lifecycle and robust branch provisioning

## Result

Step 9.8 is complete. Persistent Work creation now uses `PROVISIONING -> ACTIVE`: the expected base SHA is persisted, the GitHub ref is created or an existing eligible branch is selected, and the ref is read back before activation. Retry after restart recovers an existing provisioning row when the remote SHA matches. A pre-9.8 ACTIVE row without a remote ref is repaired only before it has a recorded Work commit.

Delivery performs an independent remote Work preflight and refuses a missing or unexpected Work SHA; it does not recreate the branch.

The project UI now links the repository name to its configured default branch, allows Work to end without PR with optional remote branch deletion, lets a user resume an existing non-default/non-protected branch, and archives projects from the normal list without deleting history. Shortcut promotion with no active Work uses the same verified provisioning path.

## Security / lifecycle decisions

- Default and protected branches cannot be selected as Work branches.
- Active import must be cancelled before Work can be abandoned or the project archived.
- Remote branch deletion is explicit and off by default.
- Project removal is a soft archive (`archived_at`), not cascading deletion.
- Existing branch resume creates a new zip-github Work session; historical Work state is not reactivated.

## Verification

Repository structure, source tracking and security regression scripts pass. `GitDeliveryServiceSelfTest` was updated to require a pre-existing Work ref and to prove a missing Work ref is rejected. New `WorkLifecycleServiceTest` covers remote create/readback ordering, provisioning recovery and default/protected branch refusal. Full Maven and frontend dependency-backed runs could not execute in the sandbox because Maven Central DNS resolution failed and the internal npm proxy returned 404 for `yallist@3.1.1`; GitHub Actions remains the authoritative full build/test environment.
