# Branch, atomic commit and push

Step 5.3 turns a verified temporary workspace into exactly one remote Git branch and one commit.

- Branch name: `zip-github/import-<import UUID>`.
- The remote base branch is resolved immediately before delivery and must still equal the approved base commit SHA.
- The workspace HEAD must equal that SHA.
- Only paths recorded in the applied workspace may be staged.
- One commit is created with the approved base commit as its single parent.
- Push is non-forced and fails if the deterministic delivery branch already exists.
- GitHub App credentials are supplied only through temporary `GIT_ASKPASS` state and are redacted from failures.
- The temporary workspace is deleted after a successful push; failed delivery keeps it for retry work in step 5.5.


## Step 9.5 — commit message source

Git delivery no longer generates the normal interactive commit message itself. It receives the already normalized, approval-bound message from `ImportPlanApproval` and passes it as a direct argument to `git commit -m`. The legacy overload keeps the former deterministic value only for compatibility with old/internal callers.


## Phase 9.8 remote Work invariant

A Work ref must exist on GitHub and be read back at the expected SHA before Work becomes `ACTIVE`. Delivery verifies the remote Work ref still exists and equals the approved workspace base SHA before committing/pushing; missing or moved Work refs are never implicitly recreated at delivery time.
