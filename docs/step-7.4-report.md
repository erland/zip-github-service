# Step 7.4 report — threat model and final security regression

## Result

Completed. The MVP threat model, trust boundaries, mitigations, accepted residual risks and release security checks are documented. A CI-enforced security regression baseline and in-process unsafe-request throttling were added.

## Added files

- `backend/src/main/java/info/isaksson/erland/zipgithub/security/FixedWindowRateLimiter.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/security/RequestRateLimitFilter.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/security/FixedWindowRateLimiterSelfTest.java`
- `scripts/security-regression.sh`
- `docs/threat-model.md`
- `docs/security-regression.md`
- `docs/step-7.4-report.md`

## Modified files

- `.env.example`
- `.github/workflows/ci.yml`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiException.java`
- `backend/src/main/resources/application.properties`
- `docs/configuration-reference.md`
- `docs/implementation-status.md`
- `scripts/README.md`

No files were moved or deleted.

## Verification performed

- security regression script passed;
- Java 21 compilation and standalone rate-limit self-test passed;
- active structure check passed;
- implementation ledger consistency passed after update;
- shell syntax and ZIP integrity passed.

## Limitations

Live GitHub App E2E, Docker/PostgreSQL restore drill, full Maven/Vitest execution and real-device accessibility testing require the release environment and remain explicit 7.5 acceptance criteria.
