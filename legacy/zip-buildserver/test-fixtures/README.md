# Test Fixtures

Small source projects used by `./scripts/verify-local.sh`.

The script creates temporary zip archives from these directories at runtime; binary zip files are intentionally not committed.

## Fixtures

- `node-pass/` — npm project expected to pass.
- `node-fail/` — npm project expected to fail during `npm test -- --runInBand`.
- `maven-pass/` — Maven project expected to pass.
- `maven-fail/` — Maven project expected to fail during compilation.

These fixtures are intentionally minimal so they exercise the package upload, project detection, verification plan, worker execution, and result reporting path without adding generated dependencies.
