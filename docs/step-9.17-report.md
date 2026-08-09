# Step 9.17 report — explicit commit and PR metadata

Revision: r0125  
Version: 1.0.0-rc.77  
Date: 9 August 2026

## Result

Interactive commit messages now start empty and remain approval-bound. Pull-request creation no longer synthesizes a title or description: both fields are entered and reviewed by the user before the GitHub API call.

The Work and post-commit views use the same PR composer. `Fyll från commitmeddelanden` is an optional convenience action that reads the current Work's GitHub commit history, excludes the Work base commit and older base-branch history, and creates an editable Markdown draft in chronological order. If trustworthy GitHub commit history is unavailable, the helper refuses to invent content and manual entry remains available.

Backend validation rejects blank/oversized/unsupported PR metadata with `400 PULL_REQUEST_METADATA_INVALID`. The authenticated GitHub user access token from step 9.15 remains the PR attribution credential, and step 9.16 Work/PR lifecycle behavior is unchanged.

## rc.78 frontend CI correction

GitHub Actions run `31323190916` exposed stale frontend regressions that still attempted approval with the newly mandatory commit-message field blank. rc.78 updates those tests to enter explicit messages where delivery is intended, asserts the blank-message gate where no delivery is intended, and strengthens `scripts/verify-release.sh`. Production behavior from step 9.17 is unchanged. See `docs/rc78-step-9.17-frontend-ci-correction.md`.

## rc.79 CI correction

The post-commit PR composer regression test now asserts the intended chronological (oldest-to-newest) order when `Fyll från commitmeddelanden` reverses GitHub's newest-first commit history. Production behavior is unchanged.
