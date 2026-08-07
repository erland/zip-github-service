#!/usr/bin/env bash
set -euo pipefail

expected_version="1.0.0-rc.10"
actual_version=$(tr -d '[:space:]' < VERSION)
[[ "$actual_version" == "$expected_version" ]] || {
  printf 'Expected VERSION %s, found %s.\n' "$expected_version" "$actual_version" >&2
  exit 1
}

for required in \
  CHANGELOG.md \
  docs/architecture.md \
  docs/mvp-release.md \
  docs/release-checklist.md \
  docs/operations.md \
  docs/threat-model.md \
  docs/security-regression.md \
  docs/container-images.md \
  docker-compose.build.yml; do
  test -s "$required" || { printf 'Missing or empty release artifact: %s\n' "$required" >&2; exit 1; }
done

grep -q 'Repository revision: `r0050`' docs/implementation-status.md
grep -q 'Last completed step: `7.5`' docs/implementation-status.md
grep -q 'Overall state: `MVP RELEASE CANDIDATE`' docs/implementation-status.md
grep -q '| `7.5` .*\*\*DONE\*\*' docs/implementation-status.md
grep -q '| `8.1` .*\*\*NEXT\*\*' docs/implementation-status.md

./scripts/verify-structure.sh
./scripts/verify-implementation-status.sh
./scripts/security-regression.sh

printf 'MVP release candidate artifacts verified for %s.\n' "$actual_version"
