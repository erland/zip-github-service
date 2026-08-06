# Step 3.4 report — normalization and file inventory

## Outcome

Implemented deterministic archive normalization and content inventory after the existing path/type and resource-limit inspection.

## Added behavior

- filters `__MACOSX`, `.DS_Store`, and AppleDouble files;
- detects and strips one common wrapper directory;
- preserves multi-root archives unchanged;
- streams each included file and records actual size and SHA-256;
- emits sorted immutable inventory and sorted ignored-path list;
- records a conservative text/binary hint without treating it as authoritative.

## Verification

- standalone Java wrapper-normalization self-test passed;
- new production classes compile with Java 21 together with their archive dependencies;
- project structure verification passed;
- ZIP integrity verification passed;
- Maven/JUnit execution remains unavailable in the current environment.

## Changed files

### Added

- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveInventory.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveInventoryEntry.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveInventoryService.java`
- `backend/src/main/java/info/isaksson/erland/zipgithub/archive/ArchiveNormalization.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/archive/ArchiveInventoryServiceSelfTest.java`
- `docs/archive-normalization-and-inventory.md`
- `docs/step-3.4-report.md`

### Modified

- `docs/implementation-status.md`

### Moved or deleted

None.
