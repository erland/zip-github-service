# MVP RC4 correction — CI Flyway PostgreSQL support

Revision: `r0044`  
Version: `1.0.0-rc.4`

## Reported failure

GitHub Actions reached `DatabaseMigrationTest` and successfully started PostgreSQL 16 through Testcontainers, but direct `Flyway.configure()` failed with `No Flyway database plugin found to handle jdbc:postgresql`.

## Root cause

Modern Flyway versions separate database-specific support from `flyway-core`. The project had Quarkus Flyway and the PostgreSQL JDBC extension but did not include Flyway's PostgreSQL database module. Local runs that did not execute the Docker-backed migration test did not expose the missing runtime module.

## Correction

Added `org.flywaydb:flyway-database-postgresql`, with its version supplied by the Quarkus platform BOM. Also replaced the relocated Quarkus test artifacts with `io.quarkus:quarkus-junit` and `io.quarkus:quarkus-junit-mockito`.

## Expected result

`DatabaseMigrationTest` can now discover PostgreSQL support when constructing Flyway directly, while Quarkus runtime migrations use the same module. No user or GitHub secret configuration is required for this test.

## Changed files

- `backend/pom.xml`
- `VERSION`
- `CHANGELOG.md`
- `docs/implementation-status.md`
- `docs/mvp-rc4-correction.md`
- `docs/mvp-release.md`
- `docs/release-checklist.md`
- `README.md`
- `scripts/verify-release.sh`
