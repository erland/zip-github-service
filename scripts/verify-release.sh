#!/usr/bin/env bash
set -euo pipefail

expected_version="1.0.0-rc.17"
actual_version=$(tr -d '[:space:]' < VERSION)
[[ "$actual_version" == "$expected_version" ]] || {
  printf 'Expected VERSION %s, found %s.\n' "$expected_version" "$actual_version" >&2
  exit 1
}

for required in \
  CHANGELOG.md \
  docs/architecture.md \
  docs/mvp-release.md \
  docs/release-checklist.md \
  docs/operations.md \
  docs/threat-model.md \
  docs/security-regression.md \
  docs/container-images.md \
  docker-compose.build.yml; do
  test -s "$required" || { printf 'Missing or empty release artifact: %s\n' "$required" >&2; exit 1; }
done

grep -q 'Repository revision: `r0057`' docs/implementation-status.md
grep -q 'Last completed step: `7.5`' docs/implementation-status.md
grep -q 'Overall state: `MVP RELEASE CANDIDATE`' docs/implementation-status.md
grep -q '| `7.5` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `8.1` .*\*\*NEXT\*\*' docs/implementation-status.md


# Container runtime requirements used by repository snapshot/workspace/delivery.
grep -q 'apt-get install -y --no-install-recommends curl git' backend/Dockerfile
grep -q '^  storage-init:' docker-compose.yml
grep -q 'condition: service_completed_successfully' docker-compose.yml
grep -q 'chown -R 10001:10001' docker-compose.yml
grep -q 'GIT_COMMITTER_NAME' backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java
grep -q 'authorMode' frontend/src/pages/NewImportPage.tsx
grep -q 'CREATE TABLE work_session' backend/src/main/resources/db/migration/V6__work_sessions.sql
grep -q 'zip-github/work-' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/WorkPersistenceStore.java
grep -q 'Arbetet är klart' frontend/src/pages/ProjectDetailPage.tsx
! grep -q 'ZIP_GITHUB_GIT_AUTHOR_' .env.example docker-compose.yml backend/src/main/resources/application.properties

./scripts/verify-structure.sh
./scripts/verify-implementation-status.sh
./scripts/security-regression.sh

printf 'MVP release candidate artifacts verified for %s.\n' "$actual_version"
