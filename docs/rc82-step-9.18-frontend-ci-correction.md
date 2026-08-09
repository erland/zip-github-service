# rc.82 — Step 9.18 frontend CI correction

GitHub CI for rc.81 reached the complete frontend test suite and passed 56 of 57 tests. The remaining failure was test-only.

`ActionsPanel` intentionally renders a check name as a link and the check app name as adjacent text. The Step 9.18 regression incorrectly queried the concatenated visual string as one DOM text node.

rc.82 changes only the test assertion:

- verifies the `CodeQL` link exists;
- verifies its enclosing row contains `GitHub Advanced Security`;
- verifies the `Dependency review` link exists;
- verifies its enclosing row contains `GitHub Actions`.

No production behavior, workflow configuration, backend Actions collection, Work lifecycle, or Step 9.17 metadata behavior changes.
