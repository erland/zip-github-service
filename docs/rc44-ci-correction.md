# RC44 CI correction

Date: 2026-08-07  
Repository revision: r0089  
Application version: 1.0.0-rc.44

## Scope

Correct the two CI regressions reported after r0088 without implementing phase 9 or changing the intended step-8.3 behavior.

## Corrections

### Empty controlled-Actions allowlists

Quarkus/SmallRye treats an explicitly empty String config value as absent/null for the built-in String converter. `ActionsControlPolicy` previously injected the two allowlist properties as required `String` values, so the intended empty/default-deny configuration prevented Quarkus startup. The CDI constructor now injects `Optional<String>` and maps absence/empty values to the existing empty-set parser. The policy therefore remains default-deny while allowing the application and tests to start with no configured controlled workflows.

The existing package-level String constructor is retained for direct unit tests of policy parsing and matching.

### Nested release-script execution

`verify-release.sh` still invoked three repository shell scripts directly. ZIP-to-GitHub delivery does not reliably preserve Git executable mode metadata, so the structure/security CI job could pass its first two explicit `bash` invocations and then fail when `verify-release.sh` nested into `security-regression.sh`. All three nested calls now use `bash ./scripts/...`.

## Security/invariant impact

No weakening. Empty/missing workflow allowlists still produce empty allowlists and therefore deny dispatch/rerun. Owner checks, current-Work ref/commit validation, Actions-write permission checks, workflow allowlists, persistent audit/idempotency, immutable import approval and non-force delivery remain unchanged. No phase-9 or AI/integration functionality was added.

## Verification performed

Successful in the packaging environment:

- `bash scripts/verify-implementation-status.sh`
- `bash scripts/verify-structure.sh`
- `bash scripts/security-regression.sh`
- `bash scripts/verify-source-tracking.sh`
- `bash scripts/verify-release.sh`
- `bash -n scripts/verify-release.sh` and all repository shell scripts
- static verification that `ActionsControlPolicy` uses optional CDI config while direct policy tests retain the String constructor

Environment-limited:

- Full Maven/Quarkus tests could not be run because the packaging environment cannot bootstrap Maven from Maven Central. The reported CI failure is specifically addressed by using optional config injection; the next normal GitHub CI run is the authoritative Quarkus startup verification.
- Full frontend tests/build were not rerun because this correction contains no frontend source changes and the packaging environment still lacks installable npm dependencies.

## Files added

- `docs/rc44-ci-correction.md`

## Files modified

- `VERSION`
- `CHANGELOG.md`
- `backend/src/main/java/info/isaksson/erland/zipgithub/actions/ActionsControlPolicy.java`
- `docs/implementation-status.md`
- `docs/phase8-plus-continuation-handoff.md`
- `scripts/verify-release.sh`

## Files moved

None.

## Files deleted

None.

## Next step

`9.1 — Define and persist the StagingImport lifecycle` remains `NEXT`.
