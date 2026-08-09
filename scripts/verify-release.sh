#!/usr/bin/env bash
set -euo pipefail

expected_version="1.0.0-rc.78"
actual_version=$(tr -d '[:space:]' < VERSION)
[[ "$actual_version" == "$expected_version" ]] || {
  printf 'Expected VERSION %s, found %s.\n' "$expected_version" "$actual_version" >&2
  exit 1
}

grep -q '## 1.0.0-rc.78 - 2026-08-09' CHANGELOG.md

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

grep -q 'Repository revision: `r0126`' docs/implementation-status.md
grep -q "await screen.findByRole('link', { name: 'example-book-project' })" frontend/src/App.test.tsx
test -s docs/rc72-frontend-staging-promotion-build-correction.md
test -s frontend/src/api/staging.test.ts
grep -q 'export type StagingPromotionTarget' frontend/src/api/staging.ts
grep -q 'body: JSON.stringify(target)' frontend/src/api/staging.ts
grep -q 'Last completed step: `9.17`' docs/implementation-status.md
test -s docs/rc78-step-9.17-frontend-ci-correction.md
grep -q "Deliver reviewed changes" frontend/src/pages/ImportReviewPage.test.tsx
grep -q "Retry delivery without duplicate approval" frontend/src/pages/ImportReviewPage.test.tsx
grep -q "Apply reviewed ZIP changes" frontend/src/pages/SimplifiedImportFlow.test.tsx
grep -q "Preserve reviewed external changes" frontend/src/pages/ImportReviewPage.test.tsx
grep -q 'Overall state: `MVP RELEASE CANDIDATE — PHASE 9 COMPLETE — STEP 9.17 COMPLETE`' docs/implementation-status.md
grep -Fq 'client_max_body_size ${ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE};' frontend/nginx.conf
grep -q 'ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE=200M' frontend/Dockerfile frontend/Dockerfile.runtime
grep -q 'ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE: ${ZIP_GITHUB_NGINX_CLIENT_MAX_BODY_SIZE:-200M}' docker-compose.yml
grep -q 'ZIP_GITHUB_UPLOAD_MAX_COMPRESSED_BYTES=209715200' .env.example
test -s docs/step-9.11-report.md
test -s docs/rc67-frontend-actions-panel-ci-correction.md
test -s docs/step-9.12-report.md
test -s docs/step-9.13-report.md

test -s docs/step-9.15-report.md
grep -q '| `9.15` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.16` .*\*\*DONE\*\*' docs/implementation-status.md
test -s docs/step-9.16-report.md
test -s docs/step-9.17-report.md
grep -q '| `9.17` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q "const \[commitMessage, setCommitMessage\] = useState('')" frontend/src/pages/ImportReviewPage.tsx
test -s frontend/src/components/PullRequestComposer.tsx
grep -q 'Fyll från commitmeddelanden' frontend/src/components/PullRequestComposer.tsx
grep -q 'PULL_REQUEST_METADATA_INVALID' backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'PullRequestMetadataPolicy.requireTitle' backend/src/main/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestService.java
! grep -q 'Complete zip-github work' backend/src/main/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestService.java
test -s docs/rc76-step-9.16-ci-correction.md
grep -q 'import info.isaksson.erland.zipgithub.api.dto.ExternalBranchChangesResponse;' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q "name: 'Skapa pull request'" frontend/src/pages/ProjectDetailPage.test.tsx
grep -q 'getExternalBranchChanges: mocks.getExternalBranchChanges' frontend/src/pages/SimplifiedImportFlow.test.tsx
test -s backend/src/main/resources/db/migration/V14__pull_request_work_lifecycle.sql
grep -q "status='PR_OPEN'" backend/src/main/java/info/isaksson/erland/zipgithub/persistence/WorkPersistenceStore.java
grep -q "'PR_OPEN','PR_CLOSED'" backend/src/main/java/info/isaksson/erland/zipgithub/persistence/WorkPersistenceStore.java
grep -q 'syncWorkPullRequestState' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q 'changedPaths' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubBranchClient.java backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q 'external-branch-changes' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java frontend/src/api/imports.ts
grep -q 'Externa ändringar' frontend/src/pages/ImportReviewPage.tsx
grep -q 'Ändrad på GitHub' frontend/src/components/ReviewFileTree.tsx
grep -q 'Work-branchen har ändrats på GitHub' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'remoteHeadCommitSha' backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/WorkSessionResponse.java frontend/src/api/projects.ts
grep -q 'stale base branch was accepted' backend/src/test/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryServiceSelfTest.java
grep -q 'createOrReuseDraft(String userAccessToken' backend/src/main/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestService.java
! grep -q 'GitHubInstallationTokenProvider' backend/src/main/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestService.java
grep -q 'session.githubUserAccessToken(), delivery' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q 'PR create must use authenticated user access token' backend/src/test/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestServiceSelfTest.java
test -s docs/rc69-frontend-review-ci-correction.md
test -s frontend/src/components/ActionsPanel.tsx
test -s frontend/src/components/ActionsControls.tsx
test -s frontend/src/components/ActionsPanel.test.tsx
grep -q 'ACTIONS_PERMISSION_REQUIRED' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java frontend/src/components/ActionsPanel.tsx docs/github-app-setup.md
grep -q '128 \* 1024' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q 'ActionLogCondensor.context' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q 'Kopiera jobblogg' frontend/src/components/ActionsPanel.tsx
grep -q 'lastImportId' backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/WorkSessionResponse.java frontend/src/api/projects.ts
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
grep -q '| `8.1` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `8.4` .*\*\*SKIPPED\*\*' docs/implementation-status.md
grep -q '| `9.1` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.2` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.3` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.5` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.6` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.7` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.11` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.12` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `9.13` .*\*\*DONE\*\*' docs/implementation-status.md
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
grep -q 'showPullRequestComposer' frontend/src/pages/ImportResultPage.tsx
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
grep -q 'POLICY_VERSION = "mvp-4"' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java
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
grep -q 'zip-github/work-' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'Skapa pull request' frontend/src/pages/ProjectDetailPage.tsx
! grep -q 'ZIP_GITHUB_GIT_AUTHOR_' .env.example docker-compose.yml backend/src/main/resources/application.properties

bash ./scripts/verify-structure.sh
bash ./scripts/verify-implementation-status.sh
bash ./scripts/security-regression.sh

printf 'MVP release candidate artifacts verified for %s.\n' "$actual_version"

grep -q 'Step 7.24 report' docs/step-7.24-report.md
grep -q 'cancelsBeforeApprovalAndRemainsCancelledAfterInMemoryRestart' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportCancellationTest.java
grep -q 'cancels the active import and exposes exactly one next-ZIP action afterwards' frontend/src/pages/ProjectDetailPage.test.tsx
grep -q 'retries direct finish-work after a transient failure without creating a second UI action' frontend/src/pages/ImportResultPage.test.tsx
grep -q 'response lost after GitHub created the PR' backend/src/test/java/info/isaksson/erland/zipgithub/pullrequest/PullRequestServiceSelfTest.java

grep -q 'Step 8.1 report' docs/step-8.1-report.md
grep -q '@Path("/{importId}/actions")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'readCommitActions' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q 'Actions.*Read and write' docs/github-app-setup.md
grep -q 'GitHub Actions' frontend/src/pages/ImportResultPage.tsx
grep -Fq '| `8.2` | Fas 8 — efter MVP: integrerade Actions-resultat | Artifacts och kondenserade fel | **DONE**' docs/implementation-status.md
grep -Fq '| `8.3` | Fas 8 — efter MVP: integrerade Actions-resultat | Kontrollerad workflow dispatch och omkörning | **DONE**' docs/implementation-status.md


grep -q 'Step 8.2 report' docs/step-8.2-report.md
grep -q '@Path("/{importId}/actions/details")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'readCommitActionDetails' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q '128 \* 1024' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java
grep -q 'REDACTED_TOKEN' backend/src/main/java/info/isaksson/erland/zipgithub/github/ActionLogCondensor.java
grep -q 'ActionsPanel' frontend/src/pages/ImportResultPage.tsx
grep -q 'Artifacts' frontend/src/components/ActionsPanel.tsx


grep -q 'Step 8.3 report' docs/step-8.3-report.md
grep -q '@Path("/{importId}/actions/dispatch")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q '@Path("/{importId}/actions/rerun-failed")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'ACTIONS_WRITE_PERMISSION_REQUIRED' backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java
grep -q 'WORKFLOW_NOT_ALLOWED' backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java
grep -q 'STALE_WORK' backend/src/main/java/info/isaksson/erland/zipgithub/actions/ImportActionsControlService.java
grep -q 'uq_actions_control_idempotency' backend/src/main/resources/db/migration/V9__actions_control_audit.sql
grep -q 'dispatchImportWorkflow' frontend/src/pages/ImportResultPage.tsx
grep -q 'rerunImportWorkflowFailedJobs' frontend/src/pages/ImportResultPage.tsx
grep -Fq '| `9.1` | Fas 9 — Shortcut/StagingImport | Definiera och persistiera StagingImport-livscykeln | **DONE**' docs/implementation-status.md
grep -Fq '| `9.2` | Fas 9 — Shortcut/StagingImport | Capability-skyddad staging-upload | **DONE**' docs/implementation-status.md
grep -Fq '| `9.3` | Fas 9 — Shortcut/StagingImport | Autentiserad claim från webbläsaren | **DONE**' docs/implementation-status.md
grep -Fq '| `9.4` | Fas 9 — Shortcut/StagingImport | Projektval och promotion till vanlig Import | **DONE**' docs/implementation-status.md
grep -Fq '| `9.5` | Fas 9 — gemensam commit UX | Användarstyrt commitmeddelande i approval/delivery | **DONE**' docs/implementation-status.md
grep -Fq '| `9.6` | Fas 9 — Shortcut/StagingImport | Retention, abuse-skydd och säkerhetsregression | **DONE**' docs/implementation-status.md
grep -Fq '| `9.8` | Fas 9 — Work lifecycle | Projektlivscykel och robust branch-provisionering | **DONE**' docs/implementation-status.md
grep -Fq '| `9.9` | Fas 9 — Work/GitHub visibility | Actions-status och kopierbara fel på Work-sidan | **DONE**' docs/implementation-status.md
grep -Fq '| `9.10` | Fas 9 — regression/release | E2E-regression, drift och slutlig releasegrind | **DONE**' docs/implementation-status.md
test -s docs/step-9.10-report.md
test -x scripts/verify-phase9-release.sh
grep -q '^## Steg 9.5 - Låt användaren ange commitmeddelandet$' docs/implementation-steps.md
grep -q '^## Steg 9.8 - Work lifecycle, projektlivscykel och robust branch-provisionering$' docs/implementation-steps.md
grep -q '^## Steg 9.9 - GitHub Actions-status och fel direkt på Work-sidan$' docs/implementation-steps.md
grep -q '^## Steg 9.10 - E2E-regression, drift och slutlig releasegrind för fas 9$' docs/implementation-steps.md
grep -q '^## Planning revision r0092 - 2026-08-08$' CHANGELOG.md
test -s docs/planning-revision-r0091-commit-message.md
test -s docs/planning-revision-r0092-shortcut-distribution.md
grep -q 'X-ZipGitHub-Upload-Credential' docs/implementation-steps.md
grep -q 'static, pre-signed' docs/shortcut-stagingimport-design.md
grep -q 'GitHub-hosted macOS runner' docs/planning-revision-r0092-shortcut-distribution.md


# Phase 9 step 9.1
grep -q 'Step 9.1 report' docs/step-9.1-report.md
grep -q 'CREATE TABLE staging_import' backend/src/main/resources/db/migration/V10__staging_import_lifecycle.sql
grep -q 'claim_token_sha256' backend/src/main/resources/db/migration/V10__staging_import_lifecycle.sql
grep -q 'CREATE UNIQUE INDEX uq_import_session_staging_source_reference' backend/src/main/resources/db/migration/V11__staging_import_source_idempotency.sql
grep -q "WHERE source_type = 'STAGING_IMPORT' AND source_reference IS NOT NULL" backend/src/main/resources/db/migration/V11__staging_import_source_idempotency.sql
grep -q 'SELECT \* FROM staging_import WHERE claim_token_sha256=? FOR UPDATE' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java
grep -q 'enum StagingImportStatus' backend/src/main/java/info/isaksson/erland/zipgithub/domain/status/StagingImportStatus.java
grep -q 'Map<String, GitFileMode> fileModes' backend/src/main/java/info/isaksson/erland/zipgithub/upload/StoredUploadArtifact.java


# Phase 9 step 9.2
grep -q 'Step 9.2 report' docs/step-9.2-report.md
grep -q '@Path("/api/staging-imports")' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java
grep -q 'X-ZipGitHub-Upload-Credential' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java
grep -q 'ZIP_GITHUB_STAGING_UPLOAD_CREDENTIAL' .env.example backend/src/main/resources/application.properties docker-compose.yml
grep -q 'StagingSecretCodecSelfTest passed' backend/src/test/java/info/isaksson/erland/zipgithub/staging/StagingSecretCodecSelfTest.java
grep -q 'no anonymous GET/list/download endpoint' docs/staging-upload.md


# Phase 9 step 9.3
grep -q 'Step 9.3 report' docs/step-9.3-report.md
grep -q '@Path("/claim")' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java
grep -q 'claimByTokenHash' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java
grep -q 'STAGING_CLAIM_UNAVAILABLE' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingClaimService.java
grep -q 'STAGING_CLAIM_TOKEN_KEY' frontend/src/staging/claimToken.ts
grep -q 'replaceState' frontend/src/components/AppLayout.tsx
grep -q 'Fortsätt till granskning' frontend/src/pages/StagingClaimPage.tsx


# Phase 9 step 9.4
grep -q 'Step 9.4 report' docs/step-9.4-report.md
grep -q '@Path("/{stagingId}/promote")' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java
grep -q 'ImportSource.STAGING_IMPORT' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java
grep -q 'staging-import:' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java
grep -q 'findBySourceReference' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/ImportResumePersistenceStore.java
grep -q 'GitFileModeResolver' backend/src/main/java/info/isaksson/erland/zipgithub/comparison/ImportComparisonService.java
grep -q 'modeChanged' backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanFactory.java
grep -q 'verifyStagedModes' backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java
grep -q 'Mode .*→' frontend/src/components/ReviewFileTree.tsx


# Phase 9 step 9.5
grep -q 'Step 9.5 report' docs/step-9.5-report.md
grep -q 'class CommitMessagePolicy' backend/src/main/java/info/isaksson/erland/zipgithub/plan/CommitMessagePolicy.java
grep -q 'String commitMessage' backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanApproval.java
grep -q 'request.commitMessage()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'sources.approval().commitMessage()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java
grep -q 'Commitmeddelande' frontend/src/pages/ImportReviewPage.tsx
grep -q 'requires the user to enter an explicit commit message' frontend/src/pages/ImportReviewPage.test.tsx
grep -Fq '| `9.6` | Fas 9 — Shortcut/StagingImport | Retention, abuse-skydd och säkerhetsregression | **DONE**' docs/implementation-status.md
grep -q 'Resume-safe message' backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportResumeRecoveryTest.java

grep -Fq '| `9.7` | Fas 9 — Shortcut/StagingImport | iOS Shortcut referensklient och installationsguide | **DONE**' docs/implementation-status.md


# Phase 9 step 9.6
grep -q 'Step 9.6 report' docs/step-9.6-report.md
grep -q 'artifact_retention_deadline' backend/src/main/resources/db/migration/V12__staging_retention_and_cleanup.sql
grep -q 'artifact_deleted_at' backend/src/main/resources/db/migration/V12__staging_retention_and_cleanup.sql
grep -q 'FOR UPDATE SKIP LOCKED' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/StagingImportPersistenceStore.java
grep -q 'promoteWithLock' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingPromotionService.java
grep -q 'insertWithinLimits' backend/src/main/java/info/isaksson/erland/zipgithub/staging/StagingUploadService.java
grep -q 'STAGING_CAPACITY_EXCEEDED' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java
grep -q 'ZIP_GITHUB_STAGING_CLAIMED_TTL_MINUTES' .env.example docker-compose.yml
grep -q 'ZIP_GITHUB_STAGING_MAX_LIVE_BYTES' .env.example docker-compose.yml
grep -qi 'credential revoke and rotation' docs/staging-retention-and-abuse.md
grep -Fq '| `9.7` | Fas 9 — Shortcut/StagingImport | iOS Shortcut referensklient och installationsguide | **DONE**' docs/implementation-status.md

printf 'Phase 9.6 release assertions verified for %s.\n' "$actual_version"


# Phase 9 step 9.7 (signed artifact distribution completed and device-verified).
grep -q 'Step 9.7 report' docs/step-9.7-report.md
grep -q '@Path("/api/shortcut-release")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ShortcutReleaseResource.java
grep -q 'ShortcutDownloadHeaders.contentDisposition()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ShortcutReleaseResource.java
grep -Fq 'DOWNLOAD_FILENAME = "Skicka till zip-github.shortcut"' backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutDownloadHeaders.java
grep -Fq 'chmod 0644 "$output"' scripts/sign-shortcut-release.sh
grep -q 'class ShortcutReleaseService' backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutReleaseService.java
grep -q 'SHORTCUT_RELEASE_UNAVAILABLE' backend/src/main/java/info/isaksson/erland/zipgithub/shortcut/ShortcutReleaseService.java
grep -q 'Ladda ner aktuell Shortcut' frontend/src/pages/ShortcutInstallPage.tsx
grep -q 'STAGING_SHORTCUT_OUTDATED' backend/src/main/java/info/isaksson/erland/zipgithub/api/StagingImportResource.java
grep -q 'zipgithub.shortcut.release-path' backend/src/main/resources/application.properties
grep -q './shortcut/releases:/var/lib/zip-github/shortcut:ro' docker-compose.yml
test -s shortcut/releases/release-manifest.txt
grep -Fxq 'filename=zip-github.shortcut' shortcut/releases/release-manifest.txt
grep -Fxq 'version=1' shortcut/releases/release-manifest.txt
grep -Fxq 'generation=g1' shortcut/releases/release-manifest.txt
grep -Fxq 'size_bytes=23821' shortcut/releases/release-manifest.txt
grep -Fxq 'sha256=21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a' shortcut/releases/release-manifest.txt
grep -q 'GITIGNORE_IGNORED' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java
! grep -q 'SIGNED_SHORTCUT_SECRET_ARTIFACT' backend/src/main/java/info/isaksson/erland/zipgithub/policy/ImportPolicyService.java
grep -q 'GitIgnoreMatcher' backend/src/main/java/info/isaksson/erland/zipgithub/comparison/ImportComparisonService.java
grep -q 'gitIgnoreFiles' backend/src/main/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshot.java
grep -Fq '{candidate.label} ({filterCount(plan, candidate.id, externalChangedPaths)})' frontend/src/pages/ImportReviewPage.tsx
grep -q 'ZIP_GITHUB_SHORTCUT_VERSION:-1' docker-compose.yml
grep -q 'ZIP_GITHUB_SHORTCUT_GENERATION:-g1' docker-compose.yml
grep -Fq '| `9.7` | Fas 9 — Shortcut/StagingImport | iOS Shortcut referensklient och installationsguide | **DONE**' docs/implementation-status.md
grep -Fq '| `9.8` | Fas 9 — Work lifecycle | Projektlivscykel och robust branch-provisionering | **DONE**' docs/implementation-status.md
grep -Fq '| `9.9` | Fas 9 — Work/GitHub visibility | Actions-status och kopierbara fel på Work-sidan | **DONE**' docs/implementation-status.md
grep -Fq '| `9.10` | Fas 9 — regression/release | E2E-regression, drift och slutlig releasegrind | **DONE**' docs/implementation-status.md
test -s docs/step-9.10-report.md
test -x scripts/verify-phase9-release.sh
grep -Fq 'initialt **`Skicka till zip-github.shortcut`**' docs/implementation-steps.md
grep -Fq 'observerade `0600`-fallet' docs/implementation-steps.md
grep -Fq 'downloaded from `/shortcut` and accepted/imported on iOS' docs/release-checklist.md
grep -q 'ShortcutDownloadHeadersSelfTest OK' backend/src/test/java/info/isaksson/erland/zipgithub/shortcut/ShortcutDownloadHeadersSelfTest.java
if [[ -f shortcut/releases/zip-github.shortcut ]]; then
  [[ "$(wc -c < shortcut/releases/zip-github.shortcut | tr -d '[:space:]')" == "23821" ]]
  [[ "$(sha256sum shortcut/releases/zip-github.shortcut | awk '{print $1}')" == "21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a" ]]
  mode=$(stat -c '%a' shortcut/releases/zip-github.shortcut)
  [[ "$mode" == "644" ]] || { printf 'Expected signed Shortcut mode 644 for runtime readability, found %s.\n' "$mode" >&2; exit 1; }
  printf 'Phase 9.7 signed Shortcut bytes and runtime-readable mode verified from deployment bundle.\n'
else
  ! git ls-files --error-unmatch shortcut/releases/zip-github.shortcut >/dev/null 2>&1
  printf 'Phase 9.7 signed Shortcut binary intentionally absent from clean source checkout; manifest verified.\n'
fi
printf 'Phase 9.7 signed-artifact publication assertions verified for %s.\n' "$actual_version"

# rc.54 container-image correction: images must package artifacts already verified by prerequisite jobs.
grep -q 'name: backend-quarkus-app' .github/workflows/ci.yml
grep -q 'uses: actions/download-artifact@v4' .github/workflows/ci.yml
grep -q 'file: ./backend/Dockerfile.runtime' .github/workflows/ci.yml
grep -q 'file: ./frontend/Dockerfile.runtime' .github/workflows/ci.yml
grep -q 'COPY target/quarkus-app/' backend/Dockerfile.runtime
grep -q 'COPY dist/' frontend/Dockerfile.runtime
! grep -Eq '\b(mvn|npm ci|npm run build)\b' backend/Dockerfile.runtime frontend/Dockerfile.runtime
printf 'rc.54 runtime-only container assembly assertions verified for %s.\n' "$actual_version"

# Phase 9 step 9.8 (Work lifecycle and verified branch provisioning).
test -s docs/step-9.8-report.md
test -s backend/src/main/resources/db/migration/V13__work_lifecycle_and_project_archive.sql
grep -q "status='PROVISIONING'" backend/src/main/java/info/isaksson/erland/zipgithub/persistence/WorkPersistenceStore.java
grep -q 'githubBranches.createBranch' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'githubBranches.branchHeadSha' backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java
grep -q 'The remote Work branch is missing or moved after review' backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java
grep -q 'Avsluta utan PR' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Fortsätt på vald branch' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Ta bort från zip-github' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'archived_at' backend/src/main/java/info/isaksson/erland/zipgithub/persistence/ProjectPersistenceStore.java
grep -q 'class WorkLifecycleServiceTest' backend/src/test/java/info/isaksson/erland/zipgithub/application/WorkLifecycleServiceTest.java
printf 'Phase 9.8 Work lifecycle assertions verified for %s.\n' "$actual_version"


# Phase 9 step 9.9 (revisitable, exact-commit Actions status/details on Work).
test -s docs/step-9.9-report.md
grep -q '@Path("/{projectId}/work/actions")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q '@Path("/{projectId}/work/actions/details")' backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q 'work.lastImportId()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q 'work.headCommitSha()' backend/src/main/java/info/isaksson/erland/zipgithub/api/ProjectResource.java
grep -q 'getProjectWorkActions' frontend/src/pages/ProjectDetailPage.tsx
grep -q 'Uppdatera status' frontend/src/components/ActionsPanel.tsx
grep -q 'Kopiera fel med sammanhang' frontend/src/components/ActionsPanel.tsx
grep -q 'copies commit-correct condensed Actions failures' frontend/src/pages/ProjectDetailPage.test.tsx
grep -q 'return new State("in_progress", false)' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsStatusMapper.java
grep -q 'return new State("queued", false)' backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubActionsStatusMapper.java
printf 'Phase 9.9 Work Actions assertions verified for %s.\n' "$actual_version"

# Phase 9 final cross-step gate
bash ./scripts/verify-phase9-release.sh
printf 'Phase 9 final release assertions verified for %s.\n' "$actual_version"

grep -q 'workflow.headSha || actions.commitSha || commitSha' frontend/src/components/ActionsPanel.tsx
grep -q 'falls back to the panel commit when a workflow payload has no headSha' frontend/src/components/ActionsPanel.test.tsx


# Phase 9 step 9.14 (manual GitHub-triggered production deployment).
test -s docs/step-9.14-report.md
test -s docs/production-deployment.md
test -s .github/workflows/deploy-production.yml
test -x ops/production/deploy.sh
test -x ops/production/deploy-ssh-command.sh
test -s ops/production/zip-github-deploy.sudoers
grep -Fq '| `9.14` | Fas 9 — Production deployment | Manuell GitHub Actions-deploy med begränsad serveridentitet | **DONE**' docs/implementation-status.md
grep -q 'workflow_dispatch:' .github/workflows/deploy-production.yml
! grep -Eq '^  push:|^  pull_request:' .github/workflows/deploy-production.yml
grep -q 'contents: read' .github/workflows/deploy-production.yml
grep -q 'group: production-deployment' .github/workflows/deploy-production.yml
grep -q 'cancel-in-progress: false' .github/workflows/deploy-production.yml
grep -q 'name: production' .github/workflows/deploy-production.yml
grep -q 'StrictHostKeyChecking=yes' .github/workflows/deploy-production.yml
grep -q 'PRODUCTION_SSH_KNOWN_HOSTS' .github/workflows/deploy-production.yml
! grep -q 'appleboy/' .github/workflows/deploy-production.yml
grep -Fq '"deploy ${DEPLOY_VERSION}"' .github/workflows/deploy-production.yml
grep -q 'git -C "$APP_DIR" pull --ff-only' ops/production/deploy.sh
! grep -q 'git clean' ops/production/deploy.sh
grep -q 'No automatic rollback was attempted because database migrations are forward-only' ops/production/deploy.sh
grep -q 'ZIP_GITHUB_VERSION=' ops/production/deploy.sh
grep -q 'SSH_ORIGINAL_COMMAND' ops/production/deploy-ssh-command.sh
grep -q 'This SSH key may only run: deploy <version>' ops/production/deploy-ssh-command.sh
grep -q '^zip-github-deploy ALL=(root) NOPASSWD: /opt/zip-github/bin/deploy.sh \*$' ops/production/zip-github-deploy.sudoers
grep -q 'restrict,command=' docs/production-deployment.md
printf 'Phase 9.14 production deployment assertions verified for %s.\n' "$actual_version"
