# Archive normalization and inventory

Step 3.4 adds a deterministic inventory stage after the security and resource-limit inspection.

## Rules

- Ignore macOS transport noise: `__MACOSX/**`, `.DS_Store`, and AppleDouble `._*` files.
- Strip one enclosing wrapper directory only when every relevant file is below the same first path segment.
- Never strip a wrapper when a relevant file already exists at archive root or relevant files have multiple top-level roots.
- Keep all resulting paths relative and preserve case after the collision checks from step 3.2.
- Sort inventory entries by normalized path.
- Record actual streamed size and SHA-256 for every included file.
- Mark a file as a text candidate only as a cheap hint; a NUL byte makes it binary. This is not a final MIME or encoding decision.

The service reads each entry as a stream and never executes archive content. The inventory is intended to become the deterministic ZIP-side input to the repository comparison in phase 4.
