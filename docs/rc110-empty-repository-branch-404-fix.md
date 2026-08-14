# rc.110 empty-repository branch 404 correction

Production diagnostic id `620d625a-770d-41e7-88f6-edfaab8e4278` showed that empty-repository Work startup
failed before Contents bootstrap. `verifyForWorkStart()` called `branchExists()` for the configured default
branch `main`; GitHub returned HTTP 404 because a completely empty repository has no branch yet.

`branchExists()` already attempted to map HTTP 404 to `false`, but `getJson()` wraps the status exception as
`IllegalStateException("GitHub API request failed", cause)`. The old check inspected only the outer message,
so the expected 404 was rethrown and bootstrap was never reached.

rc.110 walks the exception cause chain when checking for HTTP 404. This preserves fail-closed behavior for
401/403/409/5xx and other unexpected failures while allowing the intended empty-repository preflight path
to continue.

The rc.109 diagnostic logging remains enabled until the full production bootstrap path is confirmed.
