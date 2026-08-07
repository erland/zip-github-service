# MVP RC13 correction

## Scope

RC13 fixes two production findings from RC12:

1. Git authentication failed because temporary askpass scripts were created under a tmpfs mount that can be non-executable.
2. Project configuration lived only in the backend JVM and disappeared on deploy/restart.

## Git authentication

The backend image now installs a fixed `/usr/local/bin/zip-github-git-askpass` helper. Snapshot, workspace and delivery services reference that helper through `GIT_ASKPASS`; the installation token is supplied only through the child-process environment and sanitized from Git error output.

## Project persistence

Successful GitHub user login upserts `user_account`. Project creation/update upserts the per-user GitHub installation binding and stores the project in PostgreSQL. Project list/get/name uniqueness use PostgreSQL in production. Test profile retains the isolated in-memory project store.

Flyway V5 makes GitHub installation identity tenant-safe using `(id, owner_user_id)` and adds `project.private_repository`.

## Remaining persistence limitation

Active import execution state is not yet fully durable. A backend restart can still require an in-progress import to be restarted, but project configuration itself survives.
