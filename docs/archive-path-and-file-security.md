# ZIP path and file-type security

Version 1.0  
Date: 6 August 2026

## Purpose

This document describes the archive security rules implemented in step 3.2. The rules are applied before any ZIP entry is extracted.

## Implemented controls

The inspector rejects:

- parent traversal segments such as `../secret.txt`;
- absolute Unix paths;
- Windows drive paths such as `C:/temp/file.txt`;
- backslashes, which otherwise create platform-dependent path semantics;
- NUL characters;
- empty and dot path segments;
- exact duplicate paths;
- Unicode-normalized duplicates;
- case-insensitive collisions such as `README.md` and `readme.md`;
- file/directory tree conflicts such as a file named `docs` together with `docs/readme.md`;
- Unix symbolic links;
- Unix special files, including devices, sockets and FIFOs;
- malformed central-directory structures and invalid entry-name encodings.

Only regular files and directories proceed to later inspection stages.

## Design

`SecureZipInspector` reads only the ZIP end record, central-directory headers and entry names through `FileChannel`. It does not load file contents or the complete archive into memory and does not extract anything.

Unix file type information is read from the central-directory external attributes when the archive identifies the creator as Unix. Symlinks and special files are rejected before extraction.

`ArchivePathValidator` normalizes entry names to Unicode NFC and maintains exact and case-folded path registries. The same validated canonical paths must be used by the future extractor in step 3.4; extraction must not perform a second, weaker path interpretation.

## Deliberate boundaries

Step 3.2 does not yet enforce limits for:

- number of entries;
- compressed or uncompressed size;
- individual entry size;
- path length;
- compression ratio;
- ZIP64 acceptance.

Those controls belong to step 3.3. ZIP64 is currently rejected explicitly rather than processed without limits.

The archive is not extracted and no uploaded code is executed.
