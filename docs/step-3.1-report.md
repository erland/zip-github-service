# Step 3.1 report — streaming upload and metadata

Date: 6 August 2026  
Revision: `r0014`

## Outcome

Implemented authenticated raw ZIP upload to temporary isolated storage. The implementation streams bytes to disk, calculates SHA-256 during transfer, enforces both declared and actual compressed-size limits, records safe metadata and a retention deadline, and removes partial files after failures.

Ownership is checked before any upload file is created. Metadata registration repeats the owner/import association check. The browser never receives the internal storage path.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/UploadStorage.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/UploadTooLargeException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/StoredUpload.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/upload/StreamingUploadService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/dto/SourceUploadResponse.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/upload/StreamingUploadServiceTest.java`
- `docs/upload-streaming.md`
- `docs/step-3.1-report.md`

### Modified

- `.env.example`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/ImportResource.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/api/error/ApiException.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/application/ProjectApplicationService.java`
- `backend/src/main/resources/application.properties`
- `docs/api-contract.md`
- `docs/implementation-status.md`

### Moved or deleted

None.

## Verification

- Static source and brace checks: passed.
- XML and JSON parsing: passed.
- Shell syntax and active structure verification: passed.
- Storage/digest logic reviewed against deterministic fixture and SHA-256 value.
- Partial-file cleanup and actual streaming-limit tests added.
- Ownership checks exist before storage and metadata registration.
- Exactly one implementation step is marked `NEXT`.
- Final ZIP integrity: passed.

Maven/Quarkus/JUnit execution remains unavailable because Maven is not installed in this execution environment. The tests are included for CI or a full local environment.

## Limitations and follow-up

- Metadata remains in the current in-memory application store; database repository wiring is still pending.
- Scheduled retention deletion is step 3.5.
- ZIP structure and entry safety are deliberately deferred to steps 3.2–3.4.
- Resumable/chunked upload is not part of the MVP step.
