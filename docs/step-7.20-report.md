# Step 7.20 report — Git-centric Work view

## Result

The project page now treats the active Work branch and its Git commits as the primary user-facing history. The existing import-history API is retained for audit, diagnostics, idempotency and technical reopening, but historical imports are no longer rendered as the main project timeline.

## Work commit history

`GET /api/projects/{projectId}/work/commits` creates a short-lived GitHub App installation token and reads up to 50 commits from the active Work branch. Each UI row shows the abbreviated SHA, first line of the commit message, author, authored timestamp and a direct GitHub commit URL.

If GitHub history cannot be read temporarily, the endpoint returns the latest locally persisted Work head as a clearly marked fallback instead of making the entire project page unavailable.

## Active import

The page derives at most one non-terminal import from the retained owner-scoped import history and renders it as a dedicated `Pågående import` task with the correct resume action. Completed imports are not shown in the main Work timeline. While an import is active, finishing the Work and creating the pull request is disabled.

## Compatibility

No import-history data or API was removed. Existing audit and troubleshooting flows can continue to use `/api/projects/{projectId}/imports`. No new production environment variables are required.
