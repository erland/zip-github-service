# Step 9.25 report — Safe orphaned Work branch cleanup

## Delivered

- Added authenticated `Underhåll → Work-brancher` navigation and a global read-only inventory over repositories visible through the user's GitHub App installations.
- Candidate namespace is intentionally strict: only UUID-shaped `zip-github/work-*` branches created by the current Work implementation are considered. Foreign branch names are ignored.
- A branch is deletable only when it is not default/protected, no non-terminal Work for that repository uses it, no open pull request uses it as head, and all required GitHub/database checks succeed.
- Inventory failures are surfaced as unverifiable issues instead of being interpreted as an empty/safe result.
- Deletion requires an explicit UI acknowledgement and sends only repository/branch identity. Backend never trusts preview safety state and performs a fresh classification immediately before each delete.
- Bulk deletion isolates results per target; one error does not make any later branch safe or abort the safety checks for the rest.
- Added bounded pagination for GitHub installations, installation repositories and repository branches so the previous first-100 behavior does not silently truncate large installations.

## Security properties

- No scheduled or automatic deletion.
- Fail closed on incomplete GitHub/database state or lost visibility.
- Cross-project/non-terminal Work usage is checked by repository binding, not only against the current user's selected project.
- Open pull requests are checked by head branch independent of base branch.
- Preview staleness is harmless because mutation performs a full fresh safety check.

## Regression coverage

- Backend unit coverage: safe orphan, active Work, open PR, protected branch, unverifiable GitHub state, stale preview and successful freshly revalidated deletion.
- Frontend coverage: preview rendering, explicit acknowledgement gate and submission of only currently deletable candidates.

## Verification expectation

Run `backend/mvnw verify`, frontend `npm test` + `npm run build`, and all repository release/security/structure gates. If dependency/network access prevents a full local run, GitHub CI remains the integrated compile/test verification.
