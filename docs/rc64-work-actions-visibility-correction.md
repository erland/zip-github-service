# rc.64 Work Actions visibility correction

## Observed production case

GitHub Actions run `31258714926` in `erland/got-test-repo` is a completed successful `push` run on branch `zip-github/work-322395a5-db12-4a5f-b49f-50871147c4a9` with head SHA `f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69`. GitHub's Actions API returns that run when queried by the exact head SHA, yet zip-github did not display it.

## Root cause and correction

`GitHubAppClient.readCommitActions` fetched matching workflow runs and jobs, then fetched commit check-runs inside the same outer failure boundary. Any exception from the Checks endpoint caused the complete result to degrade to `unavailable`, discarding workflow data that had already been fetched successfully.

The Checks request is now isolated. Workflow runs/jobs remain visible if check-runs are unavailable due to permission drift, 403/404, or a transient GitHub failure. The existing top-level fail-closed behavior remains for a failure of the workflow-run query itself.

## Regression

`GitHubAppClientActionsResilienceTest` models the observed run/SHA and forces the check-runs request to fail with HTTP 403. It requires the workflow to remain present with normalized `success`, terminal state, exact SHA, and `push` event.
