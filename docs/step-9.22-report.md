# Step 9.22 report — Smart Shortcut repository suggestion

## Result

Step 9.22 adds a conservative, explicit repository suggestion to the Shortcut claim flow.

- The uploaded ZIP filename is normalized by removing `.zip`, common revision/version/date markers and generic release/cleanup suffixes.
- Generic repository-type prefixes such as `roman-`, `bradspel-` and `pwa-` are ignored for the stable project-name comparison.
- The repository catalog now exposes only the latest source filename and latest use timestamp for the authenticated user's existing project, so prior upload naming can contribute without cross-user metadata leakage or one request per project.
- Filename/repository-name similarity is the primary signal. A matching prior upload filename is a strong additional signal. Client recency and server last-used time are only small tie breakers.
- A suggestion is shown only when the best candidate is high confidence and clearly separated from the runner-up. Ambiguous or weak cases fall back to the shared searchable/alphabetical Step 9.21 picker.
- The suggested repository is never implicitly selected or promoted. The user must first choose `Använd detta repository` and then still choose `Fortsätt till granskning`.
- `Välj ett annat repository` always exposes the complete shared picker, and a user can return to the suggestion.

## Regression coverage

- deterministic normalization of version/revision/cleanup suffixes and generic repo prefixes
- strong filename/repository match
- strong prior-upload filename match
- ambiguous candidates produce no suggestion
- recency remains a small tie breaker
- Shortcut UI requires explicit acceptance and never promotes on suggestion alone
- ambiguous Shortcut filenames render the searchable picker directly

## Security and privacy

Suggestion metadata is produced only for repositories visible through the authenticated user's GitHub App installations. Historical filename/last-use metadata is read only from that user's own internal project mapping. No other user's project or upload metadata is exposed.
