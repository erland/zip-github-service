# rc.79 — Step 9.17 commit-order CI correction

## Scope

This is a test-only correction after rc.78. Production behavior from Step 9.17 is unchanged.

## Root cause

GitHub commit history is returned newest-first. `PullRequestComposer` intentionally reverses the Work commit list before filling the pull request description so the generated draft is chronological (oldest to newest). The `ImportResultPage` regression test mocked newest-first history correctly but asserted the unreversed order.

## Correction

The test now expects and submits:

```markdown
## Ingående commits

- Keep Work open after PR
- Add explicit PR metadata
```

This preserves the Step 9.17 decision that generated PR descriptions present Work commits oldest to newest.
