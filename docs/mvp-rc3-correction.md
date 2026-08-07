# MVP RC3 correction — r0043

Product version: `1.0.0-rc.3`

## Reason

Full local test execution of `r0042` exposed three remaining regressions. This correction does not advance the implementation-step ledger; `8.1` remains the single `NEXT` step.

## Corrections

1. `ImportPolicyServiceTest` expected five blockers even though its fixture contains four blockers and one warning. The assertion now matches the policy contract.
2. `RepositorySnapshotServiceTest` encoded `\\t` as literal backslash+t in its synthetic `git ls-tree` rows. The fixture now uses a real tab delimiter, matching Git output and the production parser contract.
3. Route focus management previously ran immediately on pathname changes and could miss asynchronously rendered headings. `AppLayout` now observes `#main-content` until the route `h1` is rendered, focuses it once, and disconnects the observer.

## Verification

Repository release, structure, security-regression, status-ledger, shell syntax and ZIP-integrity checks were rerun for this correction. Full Maven/Vitest execution should be rerun locally/CI as the authoritative confirmation of the reported failures.

## Changed files

- `backend/src/test/java/info/isaksson/erland/zipgithub/policy/ImportPolicyServiceTest.java`
- `backend/src/test/java/info/isaksson/erland/zipgithub/snapshot/RepositorySnapshotServiceTest.java`
- `frontend/src/components/AppLayout.tsx`
- `VERSION`
- `CHANGELOG.md`
- `README.md`
- `docs/mvp-release.md`
- `docs/release-checklist.md`
- `docs/implementation-status.md`
- `docs/mvp-rc3-correction.md`
- `scripts/verify-release.sh`
