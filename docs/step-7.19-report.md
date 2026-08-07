# Step 7.19 report — resumable active imports

## Outcome

A user-owned import is no longer dependent on JVM memory once it has been created. The state required to reopen review or safely retry delivery is persisted in PostgreSQL and lazily hydrated when the import is opened again after logout/login or a backend restart.

## Persisted resume state

- import identity, owner, project, branch, status and source audit metadata
- locked Git author/committer identity
- stored ZIP metadata and storage path
- exact repository snapshot/base SHA
- immutable import plan
- immutable selected paths and explicit overrides
- exact approval
- completed Git delivery metadata

The temporary Git workspace is deliberately not persisted. After a restart following approval, it is recreated and reverified from the persisted upload, snapshot, plan and selection before delivery.

## Retention

Expired source ZIPs are now cleanup candidates only after the import is terminal (`PUSHED` or `PULL_REQUEST_CREATED`). An active reviewable import is therefore not made unresumable merely because the original upload retention deadline passes. Terminal expired uploads are also discoverable from persistent state after a process restart.

## Security

`import_resume_payload` is owner-bound to `import_session` through a composite foreign key. Lazy hydration always uses both owner user ID and import ID, so restart recovery does not weaken tenant isolation.

## Verification

- Flyway migration adds restart-safe resume payload storage and owner-bound foreign key.
- Database migration regression covers cross-owner resume-state mutation.
- Repository structure, implementation ledger, source-tracking, security regression and release checks pass.
- Full Maven execution remains dependent on Maven Central availability in the packaging environment.
