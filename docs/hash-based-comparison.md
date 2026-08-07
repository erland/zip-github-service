# Hash-based import comparison

The comparison is bound to the immutable repository snapshot created for the import.

## Stable hashes

Archive files already use SHA-256 while streaming their uncompressed content. Repository snapshots now calculate SHA-256 from each Git blob using `git cat-file blob`. Git object IDs are retained as Git metadata but are not used as cross-source content hashes.

## Classification

For every normalized path in the union of the ZIP and repository inventories:

- `ADDED`: present only in the ZIP.
- `MODIFIED`: present in both but SHA-256 differs.
- `UNCHANGED`: present in both and SHA-256 matches.
- `WOULD_DELETE`: present only in the repository.

Results are sorted lexicographically by normalized relative path. Policy decisions such as blocking deletions and protected paths belong to step 4.3.

## API

`POST /api/imports/{importId}/comparison`

The import must have both a stored ZIP and a repository snapshot. The response contains the frozen base commit SHA, counts by classification and per-file archive/repository sizes and hashes.
