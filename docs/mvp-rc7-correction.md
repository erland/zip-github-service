# MVP RC7 correction

Revision `r0047` / version `1.0.0-rc.7` fixes the remaining PostgreSQL/Testcontainers CI integration-test failure reported after RC6.

## Root cause

The Flyway PostgreSQL plugin and migrations were working correctly in CI. `DatabaseMigrationTest` failed later when its generic JDBC helper called `PreparedStatement.setObject(...)` with a `java.time.Instant`. The PostgreSQL JDBC driver does not infer a PostgreSQL SQL type for `Instant`.

## Correction

The test helper now detects `Instant` parameters, converts them to UTC `OffsetDateTime`, and binds them explicitly with `Types.TIMESTAMP_WITH_TIMEZONE`. Other parameter types continue to use normal `setObject`.

This keeps the test aligned with the schema's timestamp-with-time-zone semantics and avoids machine-specific JDBC conversion behavior.

No production runtime behavior was changed. Step `8.1` remains the next implementation step.
