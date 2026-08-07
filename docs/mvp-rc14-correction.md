# MVP RC14 correction — Git author and committer identity

Revision: `r0054`  
Version: `1.0.0-rc.14`

## Purpose

Replace the server-global Git author with an identity model aligned with Git semantics and zip-github's approval flow.

## Behavior

- Normal import: author = committer = authenticated GitHub user.
- “Någon annan”: author = entered name/email; committer = authenticated GitHub user.
- Committer is derived server-side and cannot be supplied by the browser.
- GitHub profile `name` and public `email` are used when available.
- Fallback name is GitHub login.
- Fallback email is `<github-id>+<login>@users.noreply.github.com`.
- The identity is locked when the import is created and is used when the delivery commit is produced.

## Security

Author/committer values reject blank values, CR/LF/NUL and unreasonable lengths. The alternate author affects metadata only and does not change repository authorization or approval ownership.

## Changed files

- backend auth/session DTO and GitHub profile loading
- create-import request handling
- `GitCommitIdentity`
- Git delivery commit environment
- frontend import author selector
- runtime configuration cleanup
- tests and release metadata

## Verification

Repository release/security/source checks are run for the packaged revision. Full Maven/npm verification remains delegated to local/CI when dependency downloads are unavailable in the packaging environment.
