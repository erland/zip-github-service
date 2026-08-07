# Threat model — zip-github MVP

## Scope and security objective

The service accepts an untrusted ZIP archive from an authenticated user, compares it with one exact GitHub commit, requires an immutable user selection and approval bound to both plan and selection digests, and delivers only that exact selection to a managed work branch and pull request. The primary security objective is to prevent one user, archive or external system from reading, modifying or executing anything outside that explicitly approved scope.

## Assets

- GitHub OAuth user tokens and GitHub App installation tokens.
- GitHub App private key and OAuth client secret.
- Server-side web sessions and OAuth state values.
- User/project/import ownership relationships.
- Uploaded ZIP files and their content.
- Locked repository commit SHA, immutable plan digest, immutable selection digest, per-path override audit and approval audit.
- Temporary Git workspaces, branch and commit identity.
- PostgreSQL data and backups.
- Pull request, commit and check-status metadata.

## Actors

- Legitimate authenticated user.
- Another authenticated tenant attempting cross-user access.
- Unauthenticated internet client.
- Malicious archive author.
- Compromised or unavailable GitHub endpoint.
- Operator or deployment environment with accidental misconfiguration.

## Trust boundaries

1. Browser to public frontend/backend boundary.
2. Backend authentication/session boundary.
3. Tenant ownership boundary inside the application service and persistence layer.
4. Untrusted ZIP bytes to validated archive inventory boundary.
5. Approved immutable plan + immutable selection to temporary Git workspace boundary.
6. Backend to GitHub OAuth/App API and Git transport boundary.
7. Backend to PostgreSQL and backup storage boundary.
8. Container/reverse-proxy/host boundary.

## Threats and mitigations

| Threat | Impact | Mitigations | Residual risk |
|---|---|---|---|
| CSRF on state-changing API | Unauthorized import or delivery | Exact Origin check, custom request header, SameSite cookies, restricted credentialed CORS | Reverse proxy must preserve Origin and HTTPS correctly |
| Session theft/fixation | Account takeover | Opaque 256-bit tokens, HttpOnly/Secure/SameSite cookies, state consumption, expiry, logout invalidation | Sessions are currently in memory and not centrally revocable across replicas |
| Cross-tenant IDOR | Read/write another user's project/import | Ownership checks on project, import, upload, plan, approval, workspace, delivery and result access | Must be preserved when moving to database repositories |
| ZIP traversal/symlink/special file | Host file overwrite or escape | Canonical relative paths, duplicate/case collision checks, symlink/special-file rejection, destination-root checks, CREATE_NEW/part-file writes | Platform-specific filesystem semantics require release testing |
| ZIP bomb/resource exhaustion | Memory/disk/CPU denial of service | Compressed/uncompressed/file-count/single-file/path/ratio limits, streaming reads, retention cleanup | Many concurrent valid maximum-size uploads can still exhaust one node |
| Workflow or Git metadata injection | Arbitrary CI execution or repository corruption | `.git/**` is hard blocked and can never be selected; `.github/**` is excluded by default and requires explicit per-path override; immutable selection and exact diff verification | A user who explicitly approves a workflow change can intentionally cause that workflow to run after GitHub receives the commit |
| Plan/selection substitution after approval | Deliver content user did not approve | Canonical plan digest binds ZIP hash/base/policy; immutable selection digest binds selected/excluded paths and override acknowledgements; approval binds both digests; workspace diff must exactly equal selected paths | Cryptographic integrity depends on SHA-256 implementation and protected server state |
| Base/work-branch race | Commit based on stale review | Reviewed branch HEAD is locked to exact SHA and rechecked before delivery; normal non-force push rejects concurrent movement | New upstream changes require a new import/plan rather than automatic merge |
| Credential leakage through Git/errors/logs | Repository compromise | Short-lived installation tokens, GIT_ASKPASS, no token in URL, redaction, generic API errors, no token response fields | Process inspection on a compromised host remains out of scope |
| Duplicate commit/PR on retry | Confusing or repeated changes | Idempotent delivery metadata, deterministic branch, existing PR reconciliation | Crash before metadata persistence can still require GitHub reconciliation |
| API abuse | Availability loss/GitHub quota exhaustion | Per-session unsafe-request throttle, stricter upload throttle, bounded check polling | In-memory limiter is per instance; reverse-proxy/IP limits are still required |
| Malicious GitHub response or outage | Incorrect result or partial delivery | Schema/status validation, exact repository/branch/commit checks, retryable vs permanent errors, persistent result links | GitHub is an external trust dependency |
| Docker socket/host control | Full host compromise | No Docker socket mount; target builds run in GitHub Actions | Container runtime and host hardening remain operator responsibilities |
| Backup disclosure/destructive restore | Data loss or token exposure | Restricted permissions, checksum sidecar, explicit restore confirmation, secret rotation procedures | Backups must be encrypted and access-controlled by deployment platform |

## Security invariants

- No archive content is executed by the service.
- No target build runs on the service host.
- No delivery occurs without an approved plan digest and selection digest matching the current immutable plan and immutable selection.
- `.git/**` paths are hard blocked and can never be selected or written from archive content.
- `OVERRIDABLE_BLOCKED` paths can reach a workspace only when the immutable selection contains a matching explicit override audit record.
- The complete Git diff path set in the prepared workspace equals exactly `selectedPaths`; excluded paths remain unchanged.
- No force push is used.
- No active runtime mounts the Docker socket.
- A user-owned resource is always resolved together with the current user identity.
- GitHub credentials are never returned to the browser or embedded in Git remote URLs.

## Residual risks accepted for MVP

- Session, project and import application state is still partly in memory; horizontal scaling and restart persistence are not production-ready.
- Rate limiting is per backend instance and keyed by session, not a distributed edge control.
- Secret detection is path/name based, not a full content scanner.
- The real iPhone/Safari/VoiceOver matrix and a live GitHub App end-to-end test remain release-environment acceptance checks.
- Backup encryption and off-host replication are deployment responsibilities.

## Required release checks

Before 7.5 may be marked complete:

- `./scripts/security-regression.sh` passes.
- Full backend and frontend CI passes.
- Mixed selection regression covers ordinary changes, `.git/**`, `.github/**`, deletions, exclusions and stale branch movement.
- ZIP security fixtures pass.
- Local Git workspace/delivery/PR self-tests pass.
- Cleanup after failed and successful archive/Git operations is verified.
- Backup and restore are tested against a disposable PostgreSQL database.
- Live test-repository flow is completed with non-production GitHub credentials.
