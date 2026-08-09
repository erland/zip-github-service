# Step 9.15 report — user-attributed pull requests

## Problem

Draft pull requests were created through a GitHub App installation token. GitHub therefore displayed `zip-github-service[bot]` as the PR actor even though an authenticated user explicitly initiated the operation. Commits did not have this problem: their author and committer metadata were already locked from the authenticated web session when the Import was created.

## Change

- `PullRequestService` no longer creates an installation token.
- Both idempotent open-PR lookup and draft-PR creation receive the authenticated GitHub user access token from the current server-side session.
- The retry/recovery lookup after an uncertain create response uses the same user access token.
- Import and persistent Work PR endpoints both require the session and pass `githubUserAccessToken()` to the PR service.
- `GitHubPullRequestClient` token parameter naming is generalized from installation token to access token.
- Git delivery/push remains installation-token authenticated; commit author/committer behavior is unchanged.

## Security and attribution

The user access token remains server-side in the existing session store and is never exposed through API responses. GitHub limits user-to-server requests to the intersection of the user's repository access and the GitHub App's permissions. The app already requires Pull requests: write for PR creation.

## Regression

`PullRequestServiceSelfTest` now asserts that PR lookup, create and retry lookup receive exactly the authenticated user access token.

## Result

Step 9.15 is complete. New draft PRs initiated through zip-github are expected to be attributed by GitHub to the authenticated user rather than the app installation bot. Existing PRs retain their historical GitHub actor.
