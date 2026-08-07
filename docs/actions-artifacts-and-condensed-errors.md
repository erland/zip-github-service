# Actions artifacts and condensed errors

Step 8.2 adds a bounded read-only detail view for completed GitHub Actions results. GitHub remains the canonical source for artifact downloads and complete logs.

## Authorization and endpoint

`GET /api/imports/{importId}/actions/details` uses the same owner-scoped import/delivery lookup as the step-8.1 Actions status endpoint. The backend obtains a short-lived installation token only after ownership and recorded delivery are verified.

The browser never receives GitHub credentials, artifact archive URLs or raw job logs.

## Artifact handling

For the exact delivered commit the backend reads at most 10 workflow runs and exposes metadata for at most 20 artifacts total. Returned metadata includes artifact name, size, expiry state/timestamps, workflow identity and a GitHub workflow-run URL.

zip-github does not download, proxy, persist or cache artifact archive bytes. GitHub's artifact archive endpoint returns a short-lived authenticated redirect; exposing that URL to the browser would either leak a transient capability or require a new proxy/download surface. Therefore the UI deliberately links to the owning GitHub run, where the authenticated user can inspect/download the artifact.

## Condensed error extraction

Only failed workflow jobs are candidates for log summarization. The implementation:

- reads at most three failed-job excerpts across the bounded workflow set;
- reads at most 24 KiB from each job log;
- records the failed step name from GitHub job metadata when available;
- recognizes only deliberately supported patterns: Maven/Gradle, npm/Vite, Pandoc and xcodebuild;
- returns at most eight distinct excerpt lines, each capped at 180 characters;
- returns no guessed excerpt when the log format cannot be recognized robustly.

The UI identifies workflow, job, failed step, detected tool and the source GitHub job URL. The full log is never returned by zip-github.

## Sanitization

Before pattern matching/output, excerpts are sanitized by:

- removing ANSI CSI/OSC terminal sequences;
- removing non-tab/newline control characters;
- redacting recognizable GitHub tokens;
- redacting Bearer credentials and Authorization header values;
- redacting common `token`, `secret`, `password`, `api-key` and `private-key` assignments.

This is a defense-in-depth summary filter, not a general secret scanner. Its hard byte/line limits and narrow recognized formats are equally important controls. Users must use GitHub for full logs.

## Redirect safety

GitHub job-log download endpoints may return short-lived HTTPS redirects. zip-github follows them server-side without forwarding the installation-token Authorization header. Redirect targets are restricted to GitHub/GitHubusercontent or Azure Blob hosts used for GitHub-hosted result delivery. Raw log bytes are held only in request-local memory and discarded after condensation.

## Caching and failure behavior

The owner-scoped details service caches the bounded metadata/excerpts in memory for five minutes. No artifact or raw-log bytes are persisted. If artifact/log detail retrieval is unavailable, the normal commit result and step-8.1 Actions status remain available and the UI links back to GitHub.

## Out of scope

Step 8.2 does not add artifact proxy downloads, permanent artifact storage, arbitrary log browsing, workflow dispatch, rerun, phase-9 staging or AI/integration APIs.
