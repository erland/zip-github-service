# Import policy and blockers

Step 4.3 introduces deterministic policy evaluation after archive normalization and hash comparison.

## Policy version

The initial policy is identified as `mvp-1`. The version is returned by the API so a later immutable plan can record exactly which rules were applied.

## Outcomes

- `IGNORED`: transport metadata already removed from the normalized archive, including `__MACOSX/**`, `.DS_Store`, and AppleDouble files.
- `BLOCKED`: the file or proposed operation cannot be approved in the MVP.
- Existing comparison states (`ADDED`, `MODIFIED`, `UNCHANGED`) remain when no blocking rule replaces them.
- Warnings do not change the comparison state but include a policy code and message.

## Blocking rules

- `.git` and `.git/**`: repository metadata may never be imported.
- `.github` and `.github/**`: workflow and repository automation changes are protected in the MVP.
- `WOULD_DELETE`: repository deletions are blocked in the MVP.
- Files larger than `zipgithub.archive.max-single-file-bytes` are blocked.
- High-risk credential filenames and key containers are blocked, including common SSH private-key names and `.pem`, `.key`, `.p12`, `.pfx`, `.jks`, and `.keystore` files.

The high-risk-secret rule is filename based in step 4.3. Content scanning for secret material can be added as defence in depth later; filenames alone must not be treated as proof that a secret exists.

## Warning rules

`.env` and environment-specific `.env.*` files receive a warning because they commonly contain secrets. `.env.example` is explicitly allowed without warning.

## API

`POST /api/imports/{importId}/policy` returns the policy version, locked base SHA, summary counts, `approvable`, and sorted entries. No plan is persisted by this endpoint; immutable storage is step 4.4.
