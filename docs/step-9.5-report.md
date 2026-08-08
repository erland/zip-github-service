# Step 9.5 report — user-controlled commit message

Revision: `r0097`  
Application version: `1.0.0-rc.49`  
Date: 2026-08-08

## Scope completed

Step 9.5 is implemented only in the common ordinary Import review/approval/delivery path. Browser uploads and promoted StagingImports therefore share exactly the same commit-message behavior.

- The previous generated message `Apply approved ZIP import <importId>` is shown as an editable suggestion.
- The user can replace the whole message before approval.
- The final confirmation shows commit message, locked base ref and selected-file count.
- Interactive approval validates and normalizes the message server-side: CRLF/CR -> LF, surrounding whitespace stripped, non-empty, max 500 characters, and no ASCII control characters except LF.
- The normalized message is stored inside restart-safe `ImportPlanApproval` state and returned by the approval recovery endpoint.
- Approval idempotency now includes the commit message. A different message cannot silently reuse an existing approval.
- Git delivery uses the approval-bound message and no longer regenerates the normal interactive message.
- Legacy/internal callers or persisted approvals missing the new field use the previous deterministic message as a compatibility fallback only.
- No step 9.6 retention/cleanup/abuse work is implemented here.

## Verification performed

Successful in this environment:

- `CommitMessagePolicySelfTest` compiled and passed with local `javac/java`.
- `ImportPlanApprovalSelfTest` compiled and passed through the compatibility constructor.
- Repository implementation ledger verification.
- Repository structure verification.
- Security regression script.
- Source-tracking verification.
- Release verification.
- Shell syntax checks for repository scripts.
- Static inspection confirms delivery receives `sources.approval().commitMessage()` and `git commit` receives the message as a direct process argument rather than shell interpolation.

Full Maven/JUnit and frontend Vitest/build were attempted after the implementation. Maven wrapper bootstrap failed because `repo.maven.apache.org` could not be DNS-resolved (`curl: (6) Could not resolve host`). `npm ci` failed because the sandbox npm proxy returned HTTP 404 for `yallist-3.1.1.tgz`. Therefore the newly added JUnit/Vitest tests could not be executed here; normal CI remains the full verification environment.

## Files added

- `backend/src/main/java/info/isaksson/erland/zipgithub/plan/CommitMessagePolicy.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/plan/CommitMessagePolicySelfTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/plan/CommitMessagePolicyTest.java`
- `docs/step-9.5-report.md`
- `docs/user-controlled-commit-message.md`

## Files modified

- `CHANGELOG.md`
- `VERSION`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ApproveImportPlanRequest.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/ImportPlanApprovalResponse.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/plan/ImportPlanApproval.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/application/ImportResumeRecoveryTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/delivery/GitDeliveryServiceSelfTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/plan/ImportPlanApprovalTest.java`
- `docs/api-contract.md`
- `docs/branch-commit-and-push.md`
- `docs/exact-plan-approval.md`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `docs/release-checklist.md`
- `docs/shortcut-stagingimport-design.md`
- `frontend/src/api/imports.ts`
- `frontend/src/pages/ImportReviewPage.test.tsx`
- `frontend/src/pages/ImportReviewPage.tsx`
- `frontend/src/styles/global.css`
- `scripts/verify-release.sh`

## Files moved

- None.

## Files deleted

- None.

## Next step

`9.6 — Retention, abuse-skydd och säkerhetsregression för staging`.
