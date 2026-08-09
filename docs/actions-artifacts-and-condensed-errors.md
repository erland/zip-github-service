# Actions artifacts and condensed errors

Step 8.2 added bounded read-only Actions details. Step 9.11 extends that model with bounded, sanitized job-log context while GitHub remains the canonical source for complete original logs.

## Authorization and endpoint

`GET /api/imports/{importId}/actions/details` uses the same owner-scoped import/delivery lookup as the step-8.1 Actions status endpoint. The backend obtains a short-lived installation token only after ownership and recorded delivery are verified.

The browser never receives GitHub credentials or artifact archive URLs. For failed jobs it may receive only a bounded, sanitized copy of the job log after server-side redaction; the original GitHub log is never proxied unbounded.

## Artifact handling

For the exact delivered commit the backend reads at most 10 workflow runs and exposes metadata for at most 20 artifacts total. Returned metadata includes artifact name, size, expiry state/timestamps, workflow identity and a GitHub workflow-run URL.

zip-github does not download, proxy, persist or cache artifact archive bytes. GitHub's artifact archive endpoint returns a short-lived authenticated redirect; exposing that URL to the browser would either leak a transient capability or require a new proxy/download surface. Therefore the UI deliberately links to the owning GitHub run, where the authenticated user can inspect/download the artifact.

## Error extraction and bounded job-log context

Only failed workflow jobs are candidates for log diagnostics. The implementation:

- considers at most three failed jobs across the bounded workflow set;
- reads at most 128 KiB from each failed job log and records whether the log was truncated;
- records the failed step name from GitHub job metadata when available;
- retains the existing narrow condensed detector for Maven/Gradle, npm/Vite, Pandoc and xcodebuild (at most eight concise lines);
- additionally returns a sanitized context window centered on the first recognized failure (normally up to 40 lines before and 12 after);
- additionally returns a sanitized expandable job-log view capped at 1600 displayed lines;
- never treats the bounded copy as the canonical/full log; the job/run link always points back to GitHub.

The shared Work/result UI exposes **Kopiera fel med sammanhang** and **Kopiera jobblogg**. Copied text includes repository, branch, commit, workflow, job and step metadata and is generated only from the already sanitized bounded response.

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

The owner-scoped details service caches the bounded metadata/excerpts in memory for five minutes. No artifact or job-log bytes are persisted; only request-local bounded/sanitized data is returned and the existing details response may be cached in memory for five minutes. If artifact/log detail retrieval is unavailable, the normal commit result and step-8.1 Actions status remain available and the UI links back to GitHub.

## Out of scope

Step 8.2 does not add artifact proxy downloads, permanent artifact storage, arbitrary log browsing, workflow dispatch, rerun, phase-9 staging or AI/integration APIs.
