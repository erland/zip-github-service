#!/usr/bin/env bash
set -euo pipefail

expected_version="1.0.0-rc.39"
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

grep -q 'Repository revision: `r0084`' docs/implementation-status.md
grep -q 'Last completed step: `7.24`' docs/implementation-status.md
grep -q 'Overall state: `MVP RELEASE CANDIDATE — PHASES 8–9 PLANNED`' docs/implementation-status.md
grep -q '| `7.9` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.10` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.11` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.12` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.13` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.14` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.15` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.16` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.18` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.17` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.20` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.21` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.22` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.23` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `7.24` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `8.1` .*\*\*NEXT\*\*' docs/implementation-status.md
grep -q '| `8.4` .*\*\*SKIPPED\*\*' docs/implementation-status.md
grep -q '| `9.1` .*\*\*PENDING\*\*' docs/implementation-status.md
grep -q 'Fas 9 - Shortcut och kortlivad StagingImport' docs/implementation-steps.md
grep -q 'Framtida backlog - AI- och integrationsyta' docs/implementation-steps.md
test -s docs/phase8-plus-continuation-handoff.md
test -s docs/shortcut-stagingimport-design.md
grep -q 'CREATE TABLE import_resume_payload' backend/src/main/resources/db/migration/V8__resumable_import_state.sql
grep -q 'class ImportResumePersistenceStore' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/ImportResumePersistenceStore.java
grep -q 'persistentImports.find(ownerUserId, importId)' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'listExpiredTerminalUploads' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/ImportResumePersistenceStore.java
grep -q 'Step 7.19 report' docs/step-7.19-report.md
grep -q 'Step 7.20 report' docs/step-7.20-report.md
grep -q 'class ImportResumeRecoveryTest' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportResumeRecoveryTest.java
grep -q 'resumesReviewSelectionApprovalAndCompletedDeliveryAfterInMemoryRestart' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportResumeRecoveryTest.java
grep -q 'anotherOwnerCannotHydrateOrResumeTheImport' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportResumeRecoveryTest.java
grep -q 'older-draft.zip' frontend/src/pages/ProjectDetailPage.test.tsx
grep -q 'degraded Work history' frontend/src/pages/ProjectDetailPage.test.tsx
grep -q 'Step 7.21 report' docs/step-7.21-report.md
grep -q 'Phase 7 Work-lifecycle planning refinement' docs/r0080-phase7-work-lifecycle-planning.md
grep -q '@Path("/{importId}/cancel")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'IMPORT_ALREADY_DELIVERED' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'CANCELLED' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/ImportResumePersistenceStore.java
grep -q 'Avbryt import' frontend/src/pages/ImportReviewPage.tsx
grep -q 'class ImportCancellationTest' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportCancellationTest.java
grep -q 'Step 7.22 report' docs/step-7.22-report.md
grep -q 'ACTIVE_IMPORT_EXISTS' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'class ActiveImportInvariantTest' backend/src/test/java/info/isaksson/erland/zipgithub/application/ActiveImportInvariantTest.java
! grep -q 'Fortsätt arbete' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Arbetet är klart – skapa pull request' frontend/src/pages/ImportResultPage.tsx
grep -q 'Step 7.23 report' docs/step-7.23-report.md
grep -q 'work/commits' backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q 'listBranchCommits' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q 'Commits i arbetet' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Pågående import' frontend/src/pages/ProjectDetailPage.tsx
! grep -q 'Importhistorik' frontend/src/pages/ProjectDetailPage.tsx


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
grep -q 'submits explicit override audit and never includes a hard blocker' frontend/src/pages/ImportReviewPage.test.tsx
grep -q 'public class ZipIngestionService' backend/src/main/java/info/isaksson/erland/zipgithub/upload/ZipIngestionService.java
grep -q 'public record StoredUploadArtifact' backend/src/main/java/info/isaksson/erland/zipgithub/upload/StoredUploadArtifact.java
grep -q 'StoredUpload.attach' backend/src/main/java/info/isaksson/erland/zipgithub/upload/StreamingUploadService.java
grep -q 'createImportFromStoredUpload' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'STORED_UPLOAD_PROMOTION_KEY_REUSED' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'record StoredUploadImportResult' backend/src/main/java/info/isaksson/erland/zipgithub/application/StoredUploadImportResult.java
grep -q 'public enum ImportSource' backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/ImportSource.java
grep -q 'STAGING_IMPORT' backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/ImportSource.java
grep -q 'source_type' backend/src/main/resources/db/migration/V7__import_source_audit_metadata.sql
grep -q 'ImportSource.STORED_UPLOAD' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'class AlternativeZipIngestionRegressionTest' backend/src/test/java/info/isaksson/erland/zipgithub/application/AlternativeZipIngestionRegressionTest.java
grep -q 'browserAndStoredZipProduceEquivalentInventoryPolicyAndPlanEntries' backend/src/test/java/info/isaksson/erland/zipgithub/application/AlternativeZipIngestionRegressionTest.java
grep -q 'Alternative ingestion convergence (step 7.14)' docs/architecture.md
grep -q 'POLICY_VERSION = "mvp-3"' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java
grep -q 'unchangedWorkflowDoesNotRequireOverrideButActualWorkflowChangesDo' backend/src/test/java/info/isaksson/erland/zipgithub/policy/ImportPolicyServiceTest.java
grep -q '@Path("/{importId}/prepare-review")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'prepareImportReview' frontend/src/pages/NewImportPage.tsx
grep -q 'Försök skapa granskningsplan igen' frontend/src/pages/NewImportPage.tsx
grep -q 'automatic review preparation' frontend/src/pages/NewImportPage.test.tsx
grep -q 'retryReturnsTheAlreadyLockedImmutablePlan' backend/src/test/java/info/isaksson/erland/zipgithub/api/ImportReviewPreparationResourceTest.java
grep -q 'getPlanApproval' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'getImportPlanApproval' frontend/src/pages/ImportReviewPage.tsx
grep -q 'Försök skapa commit igen' frontend/src/pages/ImportReviewPage.tsx
grep -q 'one click to lock selection, approve, prepare workspace, deliver and open the result' frontend/src/pages/ImportReviewPage.test.tsx
grep -q 'simplified import flow E2E regression' frontend/src/pages/SimplifiedImportFlow.test.tsx
grep -q 'does not allow a slow automatic plan build to trigger duplicate preparation' frontend/src/pages/NewImportPage.test.tsx
grep -q 'retries delivery after approval without creating a second selection or approval' frontend/src/pages/ImportReviewPage.test.tsx
grep -q 'readsRecordedApprovalForRecoveryAfterRefresh' backend/src/test/java/info/isaksson/erland/zipgithub/api/ImportSelectionResourceTest.java

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

grep -q 'Step 7.24 report' docs/step-7.24-report.md
grep -q 'cancelsBeforeApprovalAndRemainsCancelledAfterInMemoryRestart' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportCancellationTest.java
grep -q 'cancels the active import and exposes exactly one next-ZIP action afterwards' frontend/src/pages/ProjectDetailPage.test.tsx
grep -q 'retries direct finish-work after a transient failure without creating a second UI action' frontend/src/pages/ImportResultPage.test.tsx
grep -q 'response lost after GitHub created the PR' backend/src/test/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestServiceSelfTest.java
