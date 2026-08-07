#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

required=(
  "frontend/src/api/auth.ts"
  "frontend/src/api/github.ts"
  "frontend/src/api/projects.ts"
  "frontend/src/pages/ProjectListPage.tsx"
  "frontend/src/pages/CreateProjectPage.tsx"
)

for path in "${required[@]}"; do
  if [[ ! -f "$path" ]]; then
    echo "Required source file is missing: $path" >&2
    exit 1
  fi
done

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  for path in "${required[@]}"; do
    if git check-ignore -q "$path"; then
      echo "Required source file is ignored by Git: $path" >&2
      git check-ignore -v "$path" >&2 || true
      exit 1
    fi
  done
fi

echo "Source tracking checks passed."
