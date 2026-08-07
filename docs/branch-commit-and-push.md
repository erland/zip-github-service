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
