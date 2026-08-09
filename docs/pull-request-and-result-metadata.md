# Pull request and result metadata

Step 5.4 creates a draft pull request only after the approved branch and commit have been pushed. As refined by step 9.15, draft PR lookup/create uses the authenticated user's GitHub App user access token so GitHub attributes the PR to that user. Git push and other repository automation can still use short-lived installation tokens. Neither token type is ever returned to the browser. The PR body records the immutable plan digest, base commit and delivered commit for auditability.

Recorded result metadata is immutable per import: repository, base branch, import branch, commit SHA, plan digest, PR number, URL, draft flag, state and creation time. Re-reading metadata does not call GitHub.
