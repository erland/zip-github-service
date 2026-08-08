# Workflow runs and jobs

Step 8.1 extends the delivered-commit result with a bounded, read-only GitHub Actions view. GitHub remains the canonical UI and full source of execution information.

## Authorization and scope

`GET /api/imports/{importId}/actions` follows the same owner-scoped delivery lookup as the existing check-status endpoint. The backend:

1. resolves the import through the authenticated owner,
2. requires a recorded Git delivery,
3. obtains the repository installation id from the existing owner-checked import source metadata,
4. creates a short-lived GitHub App installation token server-side, and
5. reads Actions/check information for the immutable delivered commit SHA.

No GitHub token, App private key or installation token is returned to the browser or persisted by this feature.

## Read model

The endpoint returns:

- overall state for the exact commit,
- up to 10 workflow runs for the commit,
- up to 50 jobs for each returned workflow run,
- up to 50 check runs for the commit,
- direct GitHub URLs for runs/jobs/checks when GitHub supplies them,
- observation timestamp and the permanent commit checks URL.

The stable state model is:

- `not_started` — no workflow/check has been observed yet,
- `pending` — at least one observed item is queued/in progress,
- `success` — all observed items are terminal and success/neutral/skipped,
- `failure` — at least one terminal item has a failure-like conclusion,
- `cancelled` — no failures remain but at least one terminal item is cancelled/stale,
- `unavailable` — GitHub could not be read at this observation.

`not_started` and `unavailable` are deliberately non-terminal because a workflow may appear later or a transient GitHub failure may recover. An observation containing only checks but no workflow run also remains non-terminal, so an external check cannot prematurely stop polling before a GitHub Actions run has had time to register.

## Polling and API bounds

The backend caches non-terminal observations for 8 seconds and terminal observations for 5 minutes. This avoids minting a fresh installation token and re-reading GitHub on rapid browser refreshes.

The result page starts one immediate read and backs off through 8, 15, 30 and 60 second intervals, capped at eight observations. It stops scheduling reads as soon as the backend reports a terminal aggregate state.

These bounds intentionally favor a compact status overview over exhaustive GitHub mirroring.

## Graceful degradation

If Actions is disabled, absent or has not registered a run yet, the import/commit result remains fully usable and the UI links to GitHub. Temporary GitHub API failure behaves the same way: the Actions panel degrades without hiding repository, branch, commit or Work actions.

## GitHub App permission

Reading workflow runs/jobs requires **Actions: Read-only** repository permission in addition to the existing read-only Checks permission. Existing GitHub App installations may require permission approval after the App permission is changed.
