# r0101 / 1.0.0-rc.53 — backend CI correction

## Scope

Correct the backend test failures observed in GitHub Actions workflow run `31242966490`, job `93066839493`. This correction does not advance phase 9: step 9.7 remains `BLOCKED` on the external Apple signing/iOS installation gate and 9.8 is not started.

## Root causes and corrections

1. `ImportSelectionResourceTest.readsRecordedApprovalForRecoveryAfterRefresh` still used the pre-9.5 approval request shape. The fixture now sends an explicit `commitMessage` and verifies that the same value is returned by the persisted approval recovery endpoint.
2. `StagingImportResourceTest` submitted a body under `application/zip` in a form that RestAssured attempted to serialize through its encoder registry. The tests now pass an `InputStream`, matching the resource's real binary streaming contract.
3. The same staging resource test still expected the pre-9.7 missing-credential response. It now expects `403 STAGING_SHORTCUT_OUTDATED`, matching the runtime contract for missing/revoked/outdated Shortcut credentials.
4. `StagingImportLifecycleTest` expected terminal lifecycle violations to use the common domain transition exception, while `claim()` checked the deadline first. Terminal states are now routed through `StateTransitions` before the deadline check; a deadline that elapses while the persisted state is still AVAILABLE/CLAIMED remains a distinct time-expiry `IllegalStateException`.

## Verification

- Inspected the complete failing GitHub Actions job log via the connected GitHub integration.
- Confirmed the CI run compiled all 173 production and 62 test sources and successfully started Quarkus/PostgreSQL migrations; the failures were test-contract regressions, not a general build/bootstrap failure.
- Attempted targeted Maven execution locally: `bash ./mvnw --batch-mode --no-transfer-progress -Dtest=ImportSelectionResourceTest,StagingImportResourceTest,StagingImportLifecycleTest test`. The local sandbox could not DNS-resolve `repo.maven.apache.org`, so the targeted Maven suite could not execute here.
- Repository structure/status/security/source/release checks are run before packaging this revision.

## Files added

- `docs/rc53-ci-correction.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/domain/model/StagingImport.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/ImportSelectionResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/api/StagingImportResourceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/domain/StagingImportLifecycleSelfTest.java`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.
