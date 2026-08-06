#!/usr/bin/env bash
set -euo pipefail

# Read-only preflight for a GitHub App installation token.
# The actual write spike must target a disposable repository/branch and requires
# explicit confirmation before invoking write endpoints.

: "${GITHUB_TOKEN:?Set GITHUB_TOKEN to a short-lived installation access token}"
: "${GITHUB_REPOSITORY:?Set GITHUB_REPOSITORY as owner/repository}"
TARGET_BRANCH="${TARGET_BRANCH:-main}"
API_VERSION="${GITHUB_API_VERSION:-2022-11-28}"

api() {
  curl --fail-with-body --silent --show-error \
    -H "Accept: application/vnd.github+json" \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "X-GitHub-Api-Version: ${API_VERSION}" \
    "$1"
}

repo_json="$(api "https://api.github.com/repos/${GITHUB_REPOSITORY}")"
ref_json="$(api "https://api.github.com/repos/${GITHUB_REPOSITORY}/git/ref/heads/${TARGET_BRANCH}")"

python3 - "$repo_json" "$ref_json" <<'PY'
import json
import sys
repo = json.loads(sys.argv[1])
ref = json.loads(sys.argv[2])
print(f"repository={repo['full_name']}")
print(f"private={str(repo['private']).lower()}")
print(f"default_branch={repo['default_branch']}")
print(f"target_branch_sha={ref['object']['sha']}")
PY
