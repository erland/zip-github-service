# Repository snapshot

## Decision

Step 4.1 uses a temporary shallow Git workspace rather than the Git tree API.

Reasons:

- Git resolves the selected branch to an exact commit SHA.
- The exact SHA is fetched, rather than continuing to read a moving branch name.
- `git ls-tree` provides a complete deterministic inventory of the committed tree.
- The same temporary Git model can be reused for local diff verification and atomic delivery in phase 5.
- Large repository contents are not loaded into backend memory.

The Git tree API remains a possible future optimization for environments where executing the Git client is unsuitable.

## Flow

1. Resolve `refs/heads/<branch>` with `git ls-remote`.
2. Validate and lock the returned commit SHA.
3. Fetch that exact SHA with depth 1 into an isolated temporary repository.
4. Verify `FETCH_HEAD^{commit}` equals the locked SHA.
5. Run `git ls-tree -r -z --long <sha>`.
6. Store an immutable, path-sorted inventory.
7. Delete the temporary workspace and credential helper.

The branch may move after step 2 without changing the stored snapshot. Later comparison and delivery must use `baseCommitSha`, not resolve the branch again as their source of truth.

## Inventory fields

Each tree entry records:

- repository-relative path,
- Git mode,
- Git object type,
- Git object ID,
- blob size where Git supplies one.

Git object IDs are repository object identifiers. Stable SHA-256 content hashes for comparison are added in step 4.2.

## Credentials

A short-lived GitHub App installation token is requested server-side. It is passed to Git through a temporary `GIT_ASKPASS` helper and an environment variable. It is never placed in the remote URL, returned by the API or intentionally written to logs.

The helper and temporary Git workspace are deleted in a `finally` block.

## API

```http
POST /api/imports/{importId}/repository-snapshot
```

The authenticated user must own the import and its project. The response contains the locked base SHA and the deterministic tree inventory, but no credentials or local workspace paths.
