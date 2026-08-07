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

echo 'Security regression checks passed.'
