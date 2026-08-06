# Step 3.2 report — path and file-type security

Date: 6 August 2026  
Revision: `r0015`

## Outcome

Implemented a pre-extraction ZIP inspector and independent path validator. The inspector rejects traversal, absolute and platform-ambiguous paths, NUL, duplicates, case collisions, path type conflicts, symbolic links and Unix special files.

## Verification

Passed:

- production archive classes compile with Java 21 using `javac`;
- standalone archive security self-test;
- traversal and case-collision checks;
- static inspection that `SecureZipInspector` does not extract files;
- project structure verification;
- shell syntax verification;
- XML and JSON parsing;
- exactly one implementation step marked `NEXT`;
- final ZIP integrity check.

JUnit tests were added for path rules, valid inventories, traversal, Unix symlink metadata and Unix special-file metadata. They could not be run through Maven because Maven remains unavailable in the execution environment.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveSecurityCode.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveEntryType.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveEntryDescriptor.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveSecurityException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchivePathValidator.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/SecureZipInspector.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/archive/ArchivePathValidatorTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/archive/SecureZipInspectorTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/archive/ArchiveSecuritySelfTest.java`
- `docs/archive-path-and-file-security.md`
- `docs/step-3.2-report.md`

### Modified

- `docs/implementation-status.md`

### Moved

None.

### Deleted

None.

## Limitations and follow-up

Resource ceilings and ZIP-bomb protections are intentionally deferred to step 3.3. ZIP64 is rejected until those controls are implemented. The secure extractor planned for step 3.4 must consume the canonical inventory produced by these rules and must not trust raw entry names independently.
