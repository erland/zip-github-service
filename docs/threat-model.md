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
| Arbitrary/stale Actions control | Trigger unexpected workflow execution or rerun old code | Separate default-deny dispatch/rerun allowlists, owner/repository/installation checks, exact current Work ref+SHA validation, run workflow/SHA/ref verification, explicit UI action, persisted audit and pre-side-effect idempotency claim | GitHub remains an external side-effect boundary; an ambiguous network failure is not automatically retried |
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
- Actions writes are deny-all by default, operation-specific, current-Work scoped, audited and idempotency-claimed before GitHub is called.

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


## Phase 9 staging-upload boundary (step 9.2)

Threat: a signed Shortcut/deployment capability leaks and is used to consume staging capacity. Mitigations: capability grants staging-create only, is separate from all identity/GitHub secrets, can be revoked independently, staging-create has a dedicated hard rate bucket, uploads share existing byte limits, and possession grants no list/read/claim/Project/GitHub authority.

Threat: claim bearer token leaks through persistence/logging. Mitigations: 256-bit random token, raw value returned once, URL fragment placement, hash-only database persistence, and no raw token in audit/log text. Browser handling/claim is completed in step 9.3.

Threat: CSRF exemption broadens browser write authority. Mitigation: the exemption is exact to `POST api/staging-imports`; every other unsafe API route keeps same-origin CSRF enforcement, and staging-create itself requires the separate deployment capability.


### Phase 9 authenticated claim

Threat: raw claim token leaks through URL/server/OAuth state. Mitigations: token is carried in URL fragment only, copied into same-tab `sessionStorage`, fragment is removed, OAuth return state contains only the route, and the token is submitted only in the authenticated same-origin POST body.

Threat: attacker learns claim existence/owner/state or races a victim. Mitigations: 256-bit bearer token, SHA-256 lookup, database row lock, one winning owner, same-owner idempotency and one neutral 410 response for wrong/expired/taken/terminal claims. Claim itself grants no Project or GitHub authority.

## Phase 9.4 promotion and file-mode boundary

A claimed StagingImport is still not repository authority. Promotion requires an authenticated owner and an owner-scoped Project and merely converts the already stored artifact into the ordinary Import pipeline. The stable `staging-import:<id>` source reference contains no secret and exists only to make promotion restart-safe/idempotent. A staging upload cannot select another user's Project or bypass `ACTIVE_IMPORT_EXISTS`.

Executable state is treated as content-adjacent review state. Only trustworthy Unix metadata can request `100755`; missing metadata never triggers filename/extension inference. For existing files the exact base tree mode is preserved, while new unknown-mode files default to `100644`. Mode changes are plan-digest/selection/approval bound and verified in the staged index, preventing excluded paths from receiving executable changes.

## Phase 9.6 staging retention/abuse additions

- **Leaked upload credential fills disk:** mitigated by existing ZIP limits, per-capability/global request rate limiting, serialized live-object/live-byte staging quotas and short TTL cleanup. The credential still cannot claim, identify a user, choose a Project or access GitHub.
- **Spoofed network identity bypasses limits:** forwarded-address limiting is disabled by default and may only be enabled behind a trusted ingress that sanitizes `X-Forwarded-For`.
- **Cleanup deletes bytes during promotion:** promotion holds the same PostgreSQL staging row lock cleanup needs; cleanup uses `FOR UPDATE SKIP LOCKED`. A persisted ordinary Import from a crash window is reconciled to PROMOTED before deletion.
- **Crash after terminal state but before unlink:** `artifact_deleted_at` remains null and cleanup retries the physical deletion.
- **Credential compromise forces broad credential rotation:** staging credential is independently replaceable; old Shortcut fails after redeploy while existing claim tokens and GitHub/session credentials remain unchanged.

## Phase 9.7 — signed Shortcut distribution

The downloadable `.shortcut` is treated as a deploy-time secret-bearing artifact because it embeds the staging-create credential. It is never committed to source control, dynamically generated by the backend or logged. Distribution requires an authenticated zip-github session, but possession of the extracted credential still grants only staging-create and no identity, claim, Project or GitHub authority.

The backend refuses to substitute an unsigned artifact when no signed release is configured. Credential compromise is handled by immediate deployment-credential revoke plus a newly Apple-signed Shortcut generation. Invalid/revoked credentials receive the generic `STAGING_SHORTCUT_OUTDATED` response; already-created staging rows retain their independent claim-token/TTL model.


## Phase 9 final cross-flow assertions

The Shortcut capability authorizes only staging creation and is intentionally independent from user/GitHub authority. Rotation immediately invalidates the old capability while existing claim tokens retain their own TTL/ownership semantics. Claim/promotion is single-owner and source-reference-idempotent; retries must converge on one ordinary Import.

Work branch existence is a security boundary: persistence alone never proves a branch exists. Production Work activation requires GitHub create/readback at the expected SHA, and delivery fails closed for a missing or stale remote branch. Default/protected branches cannot be selected as resumable Work. The signed Shortcut release remains ignored by Git and hard-blocked by import policy because it embeds the low-privilege deployment capability.

Actions error text exposed from Work reuses bounded/redacted diagnostics and is bound to the exact Work head commit so older branch runs cannot be mistaken for current status.
