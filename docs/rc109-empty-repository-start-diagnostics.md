# rc.109 empty-repository Work-start diagnostics

## Context

A real `Starta arbete` request for the completely empty GitHub repository `erland/repo-fleet` still returns `REPOSITORY_WORK_START_FAILED` in rc.107. Production inspection established that:

- the GitHub repository still has no branches after the attempt;
- PostgreSQL contains the GitHub installation, but no `project` row for repository id `1333469082`;
- the backend container remains healthy;
- the existing generic catch in `RepositoryResource` converted the unexpected runtime exception to HTTP 502 without logging the original exception.

Therefore the next release deliberately adds diagnostics rather than another speculative bootstrap behavior change.

## Diagnostics

`POST /api/repositories/{installationId}/{repositoryId}/work` now creates a random diagnostic id and records these high-level stages:

1. `require-session`
2. `installation-visibility`
3. `prepare-project`
4. `start-work`
5. `build-response`

Unexpected runtime failures are logged with the diagnostic id, stage, installation id, repository id and original stack trace. The safe API fallback includes the same diagnostic id.

The empty-repository verification/bootstrap path additionally logs safe milestones for repository verification, branch inventory, boolean Contents-write preflight and bootstrap marker create/cleanup progress.

## Security boundaries

Diagnostics must never log:

- GitHub user or installation access tokens;
- `Authorization` headers;
- Contents API request payloads;
- GitHub response bodies;
- marker/blob SHA values.

Repository full name, installation/repository ids, branch name, project/work ids, API problem code/status and boolean permission result are allowed operational metadata.

## Production use

After reproducing the failure, search the backend log for the diagnostic id shown to the user, for example:

```bash
sudo docker compose logs --tail=500 backend | grep -F '<diagnostic-id>' -A40 -B10
```

The resulting stage and stack trace should identify the next correction without changing empty-repository behavior speculatively.
