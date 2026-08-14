# rc.112 ImportResultPage asynchronous test correction

GitHub Actions job `94689443392` failed one frontend test after rc.111.

The production page intentionally waits for `getProjectWork()` before exposing any Pull Request creation
action. The existing test fixture already returned an `ACTIVE` Work without a PR, but the assertion used
`getByRole()` immediately after the delivery result loaded. That raced the separate Work-status effect.

rc.112 changes the assertion to `findByRole()` so the test waits for the Work status to resolve before
checking that `Skapa pull request` is enabled. Production code is unchanged.
