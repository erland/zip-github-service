# Step 1.2 report — Database model and Flyway migrations

Date: 6 August 2026  
Revision: r0007

## Scope completed

- Added target-domain PostgreSQL schema in Flyway migration V2.
- Added JPA/Panache-compatible persistence row classes separated from pure domain classes.
- Added constraints, indexes and audit timestamps.
- Added composite owner-aware foreign keys for defense-in-depth tenant isolation.
- Added PostgreSQL/Testcontainers migration integration test.
- Documented the database model and ownership rules.

## Verification

Performed in the available environment:

- `backend/pom.xml` parsed as valid XML.
- Migration contains all expected target tables, ownership columns, composite foreign keys and indexes.
- Java domain sources still compile with Java 21 independently of Quarkus dependencies.
- Persistence source structure and imports were inspected statically.
- Shell structure verification passed.
- ZIP integrity test passed after packaging.

Not executable in this environment:

- Maven compilation and JUnit/Testcontainers execution, because Maven and Docker are unavailable as documented in `docs/baseline-verification.md`.
- The Testcontainers test uses `disabledWithoutDocker = true`, so it runs in CI/development environments with Docker and is skipped explicitly otherwise.

## Files added

- `backend/src/main/resources/db/migration/V2__target_domain.sql`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/UserAccountEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/GitHubInstallationEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/ProjectEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/ImportSessionEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/SourceUploadEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/ImportPlanEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/ImportPlanEntryEntity.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/persistence/entity/GitHubDeliveryEntity.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/persistence/DatabaseMigrationTest.java`
- `docs/database-model.md`
- `docs/step-1.2-report.md`

## Files modified

- `backend/pom.xml`
- `docs/implementation-status.md`

## Files moved

None.

## Files deleted

None.

## Follow-up

Step 1.3 should create API/application boundaries without exposing persistence entities directly. Repository operations must always accept or derive the authenticated owner id and query with owner scoping.
