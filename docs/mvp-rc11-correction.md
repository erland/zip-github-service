# MVP RC11 correction — GitHub App user authorization

## Problem

RC10 successfully authenticated users through a separate OAuth App, but `/api/github/installations` calls GitHub's `/user/installations` endpoint. That endpoint requires a GitHub App user access token, so GitHub returned HTTP 403 when the OAuth App token was supplied.

## Correction

RC11 uses one GitHub App for both browser user authorization and repository automation.

- User flow: GitHub App Client ID + Client Secret -> GitHub App user access token.
- Discovery: user access token -> `/user/installations` and `/user/installations/{id}/repositories`.
- Delivery: GitHub App ID + private key -> JWT -> short-lived installation token.

No GitHub token is exposed to the browser.

## Production migration

Replace the old `GITHUB_OAUTH_*` values with the GitHub App's Client ID, Client Secret and callback URL under the new `GITHUB_APP_*` names. Configure the callback URL on the GitHub App itself. The separate OAuth App can be retained temporarily for rollback but is not used by RC11.
