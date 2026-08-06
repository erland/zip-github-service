# Step report — 0.2 Build and test the legacy baseline

Date: 2026-08-06  
Revision: `r0003`  
Status: `DONE WITH ENVIRONMENT LIMITATIONS`

## Scope completed

- Identified Java, Maven, Node.js, npm, and Docker requirements.
- Recorded exact versions available in the execution environment.
- Attempted deterministic frontend installation with `npm ci`.
- Documented why backend tests/build and Docker E2E could not run.
- Syntax-checked all shell scripts.
- Verified the input revision ZIP integrity.
- Added a reproducible verification sequence for a fully equipped environment.

## Verification summary

| Check | Outcome |
|---|---|
| Java compatibility | Passed: Java 21 available |
| Maven availability | Blocked: Maven absent |
| Docker availability | Blocked: Docker absent |
| Frontend `npm ci` | Blocked: internal registry returned 404 for `yallist@3.1.1` |
| Frontend tests/build | Not run because dependencies were unavailable |
| Shell-script syntax | Passed |
| Input ZIP integrity | Passed |

Full evidence is in `docs/baseline-verification.md`.

## Changed files

- `AGENTS.md`
- `docs/baseline-verification.md`
- `docs/implementation-status.md`
- `docs/step-0.2-report.md`

## Deliberately unchanged

- Legacy backend and frontend source code.
- Legacy dependency versions.
- Legacy Docker worker and Compose setup.
- Product domain and API behavior.

## Limitations and follow-up

A later run on a machine with Maven, Docker, and unrestricted npm package access should execute the commands recorded in `docs/baseline-verification.md`. Step `0.3` may proceed because the baseline build paths and current verification limits are now explicit, but it must not claim that all legacy tests pass.

## Next step

`0.3` — Create the reuse and migration assessment.
