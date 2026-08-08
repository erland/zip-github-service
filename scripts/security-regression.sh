#!/usr/bin/env bash
set -euo pipefail

fail() { printf 'Security regression failed: %s\n' "$*" >&2; exit 1; }

# Deployment and secret invariants.
! grep -R -n --exclude='*.md' --exclude-dir=target --exclude-dir=node_modules --exclude-dir=legacy --exclude='security-regression.sh' '/var/run/docker.sock' backend frontend docker-compose.yml || fail 'Docker socket reference found'
! grep -R -n --exclude='*.md' --exclude='*.java' --exclude='.env.example' --exclude-dir=target --exclude-dir=node_modules --exclude-dir=legacy -E 'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|gh[pousr]_[A-Za-z0-9]{20,}' . || fail 'probable committed secret found'
! find . -path './legacy' -prune -o -type f \( -name '*.pem' -o -name '*.key' -o -name '*.p12' -o -name '*.pfx' \) -print | grep -q . || fail 'private-key file tracked'

grep -q 'X-Zip-GitHub-Request' backend/src/main/java/info/isaksson/erland/zipgithub/security/CsrfProtectionFilter.java || fail 'CSRF marker missing'
grep -q 'SameOriginPolicy.matches' backend/src/main/java/info/isaksson/erland/zipgithub/security/CsrfProtectionFilter.java || fail 'origin check missing'
grep -q 'RATE_LIMIT_EXCEEDED' backend/src/main/java/info/isaksson/erland/zipgithub/security/RequestRateLimitFilter.java || fail 'rate limiting missing'
grep -q 'X-Frame-Options' backend/src/main/java/info/isaksson/erland/zipgithub/security/SecurityHeadersFilter.java || fail 'security headers missing'
grep -q 'quarkus.http.cors.access-control-allow-credentials=true' backend/src/main/resources/application.properties || fail 'credentialed CORS setting missing'

# Archive and delivery invariants.
grep -q 'SYMLINK' backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveSecurityCode.java || fail 'symlink rejection missing'
grep -q 'max-compression-ratio' backend/src/main/resources/application.properties || fail 'ZIP bomb ratio limit missing'
grep -q 'GIT_TERMINAL_PROMPT' backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java || fail 'noninteractive Git guard missing'
grep -q '/usr/local/bin/zip-github-git-askpass' backend/src/main/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotService.java || fail 'fixed Git askpass helper missing from snapshot service'
grep -q 'COPY docker/git-askpass.sh /usr/local/bin/zip-github-git-askpass' backend/Dockerfile || fail 'Git askpass helper missing from backend image'
! grep -R -n --exclude='*.md' --exclude-dir=target 'createTempFile.*git-askpass' backend/src/main/java || fail 'temporary askpass script creation found'
! grep -R -n --exclude='*.md' --exclude-dir=target 'push.*--force\|--force.*push' backend/src/main/java || fail 'force push found'
grep -q 'HARD_BLOCKED_PATH_SELECTED' backend/src/main/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactory.java || fail 'hard-blocked selection rejection missing'
grep -q 'OVERRIDE_REQUIRED' backend/src/main/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactory.java || fail 'explicit override validation missing'
grep -q 'isRepositoryChange(entry.status())' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java || fail 'protected-path policy is not diff-aware'
grep -q 'selectionDigestSha256' backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanApproval.java || fail 'approval is not bound to selection digest'
grep -q 'Hard-blocked path reached workspace preparation' backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java || fail 'workspace hard-block guard missing'
grep -q 'Selected blocker lacks explicit override audit' backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java || fail 'workspace override audit guard missing'
grep -q 'changed.equals(expected.keySet())' backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java || fail 'exact workspace diff verification missing'
grep -q 'The base branch moved after approval' backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java || fail 'stale base delivery guard missing'

grep -q 'allowed-dispatch-workflows' backend/src/main/resources/application.properties || fail 'Actions dispatch allowlist configuration missing'
grep -q 'allowed-rerun-workflows' backend/src/main/resources/application.properties || fail 'Actions rerun allowlist configuration missing'
grep -q 'WORKFLOW_NOT_ALLOWED' backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java || fail 'Actions workflow allowlist enforcement missing'
grep -q 'STALE_WORK' backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java || fail 'Actions stale Work guard missing'
grep -q 'uq_actions_control_idempotency' backend/src/main/resources/db/migration/V9__actions_control_audit.sql || fail 'Actions control idempotency uniqueness missing'
grep -q 'owner_user_id' backend/src/main/resources/db/migration/V9__actions_control_audit.sql || fail 'Actions audit owner binding missing'
grep -q 'ACTIONS_WRITE_PERMISSION_REQUIRED' backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java || fail 'Actions write permission guard missing'


# Phase 9 staging-create invariants.
grep -q 'X-ZipGitHub-Upload-Credential' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java || fail 'staging capability header missing'
grep -q 'MessageDigest.isEqual' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadCredential.java || fail 'staging credential constant-time digest comparison missing'
grep -q 'new byte\[32\]' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingSecretCodec.java || fail '256-bit staging claim token generation missing'
grep -q 'claim.sha256()' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadService.java || fail 'staging claim hash persistence boundary missing'
grep -q 'api/staging-imports' backend/src/main/java/info/isaksson/erland/zipgithub/security/CsrfProtectionFilter.java || fail 'exact staging CSRF exemption missing'
grep -q 'stagingUploads = new FixedWindowRateLimiter' backend/src/main/java/info/isaksson/erland/zipgithub/security/RequestRateLimitFilter.java || fail 'staging upload rate limit missing'
! grep -R -n --exclude='*.md' --exclude-dir=target 'claim.raw()' backend/src/main/java/info/isaksson/erland/zipgithub/persistence || fail 'raw claim token reached persistence package'

# Phase 9 authenticated-claim invariants.
grep -q '@Path("/claim")' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java || fail 'authenticated staging claim endpoint missing'
grep -q 'currentUser.requireUserId()' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java || fail 'staging claim does not require authenticated owner'
grep -q 'claimByTokenHash' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java || fail 'atomic hash-based staging claim missing'
grep -q 'FOR UPDATE' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java || fail 'staging claim row lock missing'
grep -q 'STAGING_CLAIM_UNAVAILABLE' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingClaimService.java || fail 'neutral staging claim error missing'
grep -q 'sessionStorage' frontend/src/components/AppLayout.tsx || fail 'claim token is not held in same-tab browser state'
grep -q 'replaceState' frontend/src/components/AppLayout.tsx || fail 'claim URL fragment is not cleared'
! grep -R -n --exclude='*.md' --exclude-dir=target --exclude-dir=node_modules 'returnTo=.*token\|state=.*token' backend/src/main frontend/src || fail 'claim token appears to enter OAuth continuation state'


# Phase 9 promotion/file-mode invariants.
grep -q '@Path("/{stagingId}/promote")' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java || fail 'owner-authenticated staging promotion endpoint missing'
grep -q 'projects.getProject(owner, projectId)' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java || fail 'promotion project ownership guard missing'
grep -q 'ImportSource.STAGING_IMPORT' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java || fail 'staging promotion source classification missing'
grep -q 'findImportBySourceReference' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java || fail 'restart-safe staging promotion recovery missing'
grep -q 'GitFileModeResolver.effectiveMode' backend/src/main/java/info/isaksson/erland/zipgithub/comparison/ImportComparisonService.java || fail 'deterministic file-mode resolution missing'
grep -q 'modeChanged' backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanFactory.java || fail 'file mode missing from immutable plan identity'
grep -q 'applyFileModes' backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java || fail 'approved file modes not applied in workspace'
grep -q 'verifyStagedModes' backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java || fail 'staged file modes not verified before commit'



# Phase 9.6 staging retention/abuse invariants.
grep -q 'insertWithinLimits' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadService.java || fail 'serialized staging capacity enforcement missing'
grep -q 'pg_advisory_xact_lock' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java || fail 'staging quota serialization lock missing'
grep -q 'FOR UPDATE SKIP LOCKED' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java || fail 'staging cleanup row locking missing'
grep -q 'promoteWithLock' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java || fail 'promotion/cleanup coordination missing'
grep -q "source_reference=('staging-import:'" backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java || fail 'promotion crash reconciliation missing'
grep -q 'artifact_deleted_at' backend/src/main/resources/db/migration/V12__staging_retention_and_cleanup.sql || fail 'restart-safe staging deletion marker missing'
grep -q 'artifact_retention_deadline' backend/src/main/resources/db/migration/V12__staging_retention_and_cleanup.sql || fail 'ordinary artifact retention separation missing'
grep -q 'trust-forwarded-for.*false' backend/src/main/resources/application.properties || fail 'forwarded-source rate limit must default to untrusted'
grep -q 'STAGING_CAPACITY_EXCEEDED' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java || fail 'staging storage capacity response missing'


# Phase 9.7 signed Shortcut distribution invariants.
grep -q 'currentUser.requireUserId()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ShortcutReleaseResource.java || fail 'signed Shortcut download is not authenticated'
grep -q 'Cache-Control.*private, no-store' backend/src/main/java/info/isaksson/erland/zipgithub/api/ShortcutReleaseResource.java || fail 'signed Shortcut download cache control missing'
grep -q 'STAGING_SHORTCUT_OUTDATED' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java || fail 'old/revoked Shortcut update error missing'
grep -q '/shortcut/releases/\*.shortcut' .gitignore || fail 'secret-bearing signed Shortcut artifacts are not ignored'
! find shortcut/releases -maxdepth 1 -type f -name '*.shortcut' -print -quit | grep -q . || fail 'a signed/secret-bearing Shortcut binary must not be committed in source'
grep -q 'shortcuts sign --mode anyone' scripts/sign-shortcut-release.sh || fail 'trusted-Mac Shortcut signing helper missing'

echo 'Security regression checks passed.'

# Step 9.4: staging promotion correlation is unique and contains no claim token.
grep -q 'uq_import_session_staging_source_reference' backend/src/main/resources/db/migration/V11__staging_import_source_idempotency.sql
grep -q "source_type = 'STAGING_IMPORT'" backend/src/main/resources/db/migration/V11__staging_import_source_idempotency.sql
! grep -qi 'claim_token' backend/src/main/resources/db/migration/V11__staging_import_source_idempotency.sql || { echo 'V11 must not persist claim-token material' >&2; exit 1; }
