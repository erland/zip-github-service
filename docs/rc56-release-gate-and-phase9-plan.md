# rc.56 — signed Shortcut release gate correction and phase 9 plan refinement

## CI failure analysed

GitHub Actions run 31259483069 job 93107950297 passed structure, source tracking, security regression, shell syntax and implementation-ledger verification. `scripts/verify-release.sh` then failed after the phase 9.6 assertions because rc.55 required `shortcut/releases/zip-github.shortcut` to exist in the clean Git checkout. The same file is intentionally ignored and must not be source-tracked because it embeds the deployment-scoped staging upload credential.

## Correction

- Added source-tracked `shortcut/releases/release-manifest.txt` containing filename, Shortcut version/generation, byte size and SHA-256.
- Release verification now always checks the manifest and security policy.
- If the signed binary is present (for example in the delivered deployment ZIP), verification checks exact size/SHA-256.
- If it is absent (normal clean GitHub Actions checkout), verification asserts that it is not source-tracked instead of failing.
- The deployment ZIP continues to contain the signed artifact.

## Remaining phase 9 plan

The former monolithic step 9.8 is split into:

- 9.8 — Work lifecycle, project archive and robust remote branch provisioning/recovery.
- 9.9 — persistent/revisitable GitHub Actions status and copyable condensed failures from the Work page.
- 9.10 — final phase-9 E2E/regression/release gate.

Step 9.7 remains blocked only on deployed `/shortcut` download and iOS installation verification. No implementation work from 9.8–9.10 is included in this revision.
