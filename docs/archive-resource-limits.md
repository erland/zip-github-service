# Archive resource limits and ZIP-bomb protection

Step 3.3 adds resource limits on top of the path and file-type rules from step 3.2.
The archive is still never executed.

## Default limits

| Limit | Default |
|---|---:|
| Compressed ZIP | 100 MiB |
| Total uncompressed content | 500 MiB |
| Entries | 20,000 |
| One regular file | 50 MiB |
| Entry path length | 1,024 characters |
| Compression ratio per file | 100:1 |

All values are configurable through environment variables documented in `.env.example`.

## Two-layer validation

`SecureZipInspector` first validates central-directory metadata before allocating an entry list or inflating file content. It rejects archives whose declared counts, sizes, paths or ratios exceed policy.

It then opens each regular entry as a stream and counts the bytes that are actually inflated. The actual counters enforce:

- the single-file limit;
- the total uncompressed limit;
- the compression-ratio limit while bytes are being read.

This second layer protects against misleading size metadata and prevents later extraction code from relying solely on declared ZIP fields.

## Failure codes

- `COMPRESSED_SIZE_LIMIT_EXCEEDED`
- `UNCOMPRESSED_SIZE_LIMIT_EXCEEDED`
- `ENTRY_COUNT_LIMIT_EXCEEDED`
- `SINGLE_FILE_SIZE_LIMIT_EXCEEDED`
- `PATH_LENGTH_LIMIT_EXCEEDED`
- `COMPRESSION_RATIO_LIMIT_EXCEEDED`

ZIP64 remains blocked until a future implementation can apply equivalent bounded parsing and extraction semantics.

## Integration

`ArchiveInspectionService` converts the application configuration into an immutable `ArchiveResourceLimits` policy and invokes the secure inspector. Step 3.4 will use the validated result for normalization and deterministic file inventory.
