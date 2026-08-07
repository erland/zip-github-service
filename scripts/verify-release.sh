#!/usr/bin/env bash
set -euo pipefail

expected_version="1.0.0-rc.23"
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

grep -q 'Repository revision: `r0064`' docs/implementation-status.md
grep -q 'Last completed step: `7.10`' docs/implementation-status.md
grep -q 'Overall state: `MVP RELEASE CANDIDATE — FLEXIBLE REVIEW COMPLETE`' docs/implementation-status.md
grep -q '| `7.9` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.10` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `8.1` .*\*\*NEXT\*\*' docs/implementation-status.md


grep -q 'public enum ImportPolicyBlockerType' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyBlockerType.java
grep -q 'HARD_BLOCKED' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java
grep -q 'OVERRIDABLE_BLOCKED' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java
grep -q 'blockerType' backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImmutableImportPlanEntry.java
grep -q 'hardBlocked' frontend/src/api/imports.ts
grep -q 'public record ApprovedSelection' backend/src/main/java/info/isaksson/erland/zipgithub/selection/ApprovedSelection.java
grep -q 'SELECTION_VERSION = "selection-1"' backend/src/main/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactory.java
grep -q '@Path("/{importId}/selection")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'HARD_BLOCKED_PATH_SELECTED' backend/src/main/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactory.java
grep -q 'function ReviewFileTree' frontend/src/components/ReviewFileTree.tsx
grep -q 'indeterminate' frontend/src/components/ReviewFileTree.tsx
grep -q 'Godkänn valda förändringar' frontend/src/pages/ImportReviewPage.tsx
grep -q 'hierarchical, tri-state import selection tree' frontend/src/styles/global.css
grep -q 'selectionDigestSha256' backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanApproval.java
grep -q 'sources.selection()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'WOULD_DELETE' backend/src/main/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceService.java
grep -q 'explicit per-path policy overrides' frontend/src/styles/global.css
grep -q 'Jag förstår risken och vill ta med denna blockerade förändring' frontend/src/components/ReviewFileTree.tsx
grep -q 'hardBlockCannotBeBypassedWithAnOverrideRecord' backend/src/test/java/info/isaksson/erland/zipgithub/selection/ImportSelectionFactoryTest.java
grep -q 'workspace diff did not exactly match selection' backend/src/test/java/info/isaksson/erland/zipgithub/workspace/ImportWorkspaceServiceSelfTest.java
grep -q 'stale base branch was accepted' backend/src/test/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryServiceSelfTest.java
grep -q 'submits the exact partial selection and explicit override audit' frontend/src/pages/ImportReviewPage.test.tsx

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
