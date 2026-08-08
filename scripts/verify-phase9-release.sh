#!/usr/bin/env bash
set -euo pipefail

# Final phase-9 gate: verify the cross-step contracts that must remain true together.
for required in \
  docs/step-9.1-report.md docs/step-9.2-report.md docs/step-9.3-report.md docs/step-9.4-report.md \
  docs/step-9.5-report.md docs/step-9.6-report.md docs/step-9.7-report.md docs/step-9.8-report.md docs/step-9.9-report.md \
  docs/signed-shortcut-release.md shortcut/releases/release-manifest.txt; do
  test -s "$required" || { echo "Missing phase-9 artifact: $required" >&2; exit 1; }
done

grep -q 'STAGING_SHORTCUT_OUTDATED' backend/src/test/java/info/isaksson/erland/zipgithub/api/StagingImportResourceTest.java
grep -q 'rotationImmediatelyRejectsOldCredentialWithoutDataMigration' backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingUploadCredentialTest.java
grep -q 'promotesClaimedUploadWithoutCopyUnderPersistenceLock' backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingPromotionServiceTest.java
grep -q 'browserAndStoredZipProduceEquivalentInventoryPolicyAndPlanEntries' backend/src/test/java/info/isaksson/erland/zipgithub/application/AlternativeZipIngestionRegressionTest.java
grep -q 'newWorkIsOnlyActivatedAfterRemoteBranchReadbackMatches' backend/src/test/java/info/isaksson/erland/zipgithub/application/WorkLifecycleServiceTest.java
grep -q 'retryRecoversProvisioningWorkInsteadOfCreatingAnotherBranch' backend/src/test/java/info/isaksson/erland/zipgithub/application/WorkLifecycleServiceTest.java
grep -q 'missing Work branch was recreated implicitly' backend/src/test/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryServiceSelfTest.java
grep -q 'GitHub Actions' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Kopiera fel' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Skicka till zip-github.shortcut' backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutDownloadHeaders.java
grep -q 'chmod 0644' scripts/sign-shortcut-release.sh
grep -q '100755' backend/src/test/java/info/isaksson/erland/zipgithub/upload/GitFileModeResolverSelfTest.java

manifest_sha=$(sed -n 's/^sha256=//p' shortcut/releases/release-manifest.txt)
manifest_size=$(sed -n 's/^size_bytes=//p' shortcut/releases/release-manifest.txt)
if [[ -f shortcut/releases/zip-github.shortcut ]]; then
  actual_sha=$(sha256sum shortcut/releases/zip-github.shortcut | awk '{print $1}')
  actual_size=$(wc -c < shortcut/releases/zip-github.shortcut | tr -d '[:space:]')
  [[ "$actual_sha" == "$manifest_sha" ]] || { echo 'Signed Shortcut SHA mismatch.' >&2; exit 1; }
  [[ "$actual_size" == "$manifest_size" ]] || { echo 'Signed Shortcut size mismatch.' >&2; exit 1; }
  [[ -r shortcut/releases/zip-github.shortcut ]] || { echo 'Signed Shortcut is not runtime-readable.' >&2; exit 1; }
fi

echo 'Phase 9 final release contracts verified.'
