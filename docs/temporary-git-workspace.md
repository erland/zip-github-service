# Temporary Git workspace and approved file application

Step 5.2 prepares an isolated delivery workspace from the exact immutable plan approved in step 5.1.

## Preconditions

The backend requires the same owner to have:

- a stored source ZIP,
- a locked repository snapshot,
- an immutable import plan,
- an approval whose `planDigestSha256` equals the stored plan,
- matching ZIP SHA-256 and base commit SHA across all stored artifacts.

## Preparation flow

`POST /api/imports/{importId}/workspace`:

1. creates a clean workspace below `zipgithub.delivery.workspace-root`,
2. initializes Git and fetches the exact approved `baseCommitSha`,
3. checks out that commit detached,
4. removes the remote immediately after fetch,
5. reads the ZIP without executing archive content,
6. strips the same detected wrapper directory used by the inventory,
7. writes only plan entries whose final status is `ADDED` or `MODIFIED`,
8. verifies every written file against the approved size and SHA-256,
9. compares the local Git diff with the exact approved path set.

`UNCHANGED`, `IGNORED`, `BLOCKED`, transport-noise and unplanned ZIP files are never applied.

## Security properties

- The installation token is supplied through a temporary `GIT_ASKPASS` script and environment variable.
- The token is never placed in the remote URL or response.
- The remote and askpass file are removed before the prepared workspace is returned.
- Paths are normalized and must remain below the workspace root.
- Files are first written to `.part` files, hashed, and atomically moved into place.
- A mismatch between ZIP content and the approved plan aborts and deletes the workspace.
- A mismatch between `git diff` and the approved path set aborts and deletes the workspace.

The successful workspace remains only for step 5.3, which creates the branch, commit and push. That step must delete it after success or terminal failure.
