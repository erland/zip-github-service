# Step 3.3 report — resource limits and ZIP-bomb protection

Date: 2026-08-06  
Revision: `r0016`

## Delivered

- Configurable limits for compressed bytes, uncompressed bytes, entry count, single-file size, path length and compression ratio.
- Early central-directory checks before entry-list allocation or inflation.
- Streaming validation of actual inflated bytes using a fixed 64 KiB buffer.
- Explicit security codes for every resource-limit failure.
- CDI configuration adapter through `ArchiveInspectionService`.
- JUnit fixtures and a standalone Java self-test, including a highly compressible ZIP-bomb fixture.

## Verification

Passed:

- archive package compilation with Java 21;
- standalone `ArchiveResourceLimitsSelfTest`;
- existing structure verification;
- shell syntax checks;
- XML/JSON parsing;
- exactly one `NEXT` row;
- final ZIP integrity check.

Maven/JUnit execution remains unavailable in the current environment because Maven is not installed. The standalone test exercises the real inspector and actual DEFLATE inflation path.

## Files changed

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveResourceLimits.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveInspectionService.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/archive/ArchiveResourceLimitsTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/archive/ArchiveResourceLimitsSelfTest.java`
- `docs/archive-resource-limits.md`
- `docs/step-3.3-report.md`

### Modified

- `.env.example`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveSecurityCode.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/SecureZipInspector.java`
- `backend/src/main/resources/application.properties`
- `docs/implementation-status.md`

### Moved

None.

### Deleted

None.

## Follow-up

Step 3.4 will normalize the already validated archive, ignore transport noise and build a deterministic inventory. Any future extractor must retain the same live byte counters rather than trusting only the pre-inspection result.
