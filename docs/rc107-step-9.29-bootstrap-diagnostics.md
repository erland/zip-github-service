# rc.107 step 9.29 bootstrap diagnostics correction

A real retry against `erland/repo-fleet` after rc.106 still left the GitHub repository with no branches.
That proves the first Contents API `PUT` did not complete; cleanup and Work branch creation were never reached.

rc.107 therefore verifies the GitHub App installation's actual `permissions.contents` value before bootstrap.
An installation without `contents: write` receives `GITHUB_CONTENTS_WRITE_PERMISSION_REQUIRED`.

If GitHub still rejects the bootstrap request, zip-GitHub retains only the safe upstream HTTP status and
top-level GitHub `message` and maps common statuses to explicit API problem codes. Tokens, response bodies,
headers and repository contents are not exposed.

This correction is diagnostic and permission-preflight hardening around step 9.29. Initialized repository
behavior is unchanged.
