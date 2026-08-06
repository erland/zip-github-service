# Correction r0022 — optional GitHub App credentials in tests

## Problem

Quarkus test startup failed because `zipgithub.github.app-private-key` was injected as a required `String`. An empty default is treated as a missing value by SmallRye Config, so unrelated tests could not boot without production GitHub App credentials.

## Correction

- Changed the private-key injection to `Optional<String>`.
- Added an explicit runtime guard before creating a GitHub App JWT.
- Missing credentials now fail only when installation-token functionality is invoked.
- Removed deprecated `quarkus.hibernate-orm.database.generation=none`; the test profile already disables Hibernate ORM and Flyway owns schema creation.
- Added a unit contract test for the missing-credentials behavior.

## Changed files

- `backend/src/main/java/info/isaksson/erland/zipgithub/github/GitHubAppClient.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/info/isaksson/erland/zipgithub/github/GitHubAppClientContractTest.java`
- `docs/implementation-status.md`
- `docs/step-3.6-correction-r0022.md`

## Expected behavior

Ordinary backend tests start without `GITHUB_APP_ID` or `GITHUB_APP_PRIVATE_KEY`. Calls that create installation tokens still require both values and return a clear configuration error when absent.
