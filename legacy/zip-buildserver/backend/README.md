# Backend

Quarkus API service for `zip-buildserver`.

## Current scope

This backend skeleton contains:

- Java 21 Maven project metadata
- Quarkus REST/Jackson, validation, OpenAPI, PostgreSQL JDBC, and Flyway dependencies
- Initial `/api/health` resource
- A basic Quarkus/RestAssured test

## Local verification

```bash
mvn test
```

Database-backed functionality is introduced in later delivery steps.
