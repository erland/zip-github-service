# r0103 / rc.55 — signed Shortcut integration

Date: 8 August 2026

The operator supplied an Apple-signed, iPhone-verified reference Shortcut. This revision includes it as the deployment artifact `shortcut/releases/zip-github.shortcut`.

- Shortcut version: `1`
- generation: `g1`
- size: 23821 bytes
- SHA-256: `21a9e220067681994ff42326a0b430261fe84583bfbc614297c634ae752af50a`

The binary embeds the deployment staging-upload capability. It remains gitignored and is now also hard-blocked by `ImportPolicyService` using `SIGNED_SHORTCUT_SECRET_ARTIFACT`, so importing this complete delivery ZIP into zip-github cannot intentionally select the release binary for Git delivery.

The signing blocker from r0099 is resolved. Step 9.7 remains blocked only on deployed-path verification: `/shortcut` must serve this exact file and an iOS device must accept the downloaded copy.

The security regression was updated to allow exactly the expected deployment-bundle Shortcut only when the path remains gitignored and hard-blocked from ordinary Import delivery.

## Verification

Repository implementation/status, structure, security regression, source tracking, release verification, shell syntax and workflow YAML parsing all passed. A targeted Maven run for `ImportPolicyServiceTest` could not start because the sandbox could not resolve `repo.maven.apache.org` while downloading Maven 3.9.11; CI remains the executable Maven verification environment.
