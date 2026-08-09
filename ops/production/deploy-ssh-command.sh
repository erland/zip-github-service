#!/usr/bin/env bash
set -euo pipefail

PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
export PATH

original=${SSH_ORIGINAL_COMMAND:-}
read -r command version extra <<< "$original"

if [[ "$command" != "deploy" || -z "${version:-}" || -n "${extra:-}" ]]; then
  echo "This SSH key may only run: deploy <version>" >&2
  exit 2
fi

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-rc\.[0-9]+)?$ ]]; then
  echo "Invalid deployment version." >&2
  exit 2
fi

exec /usr/bin/sudo /opt/zip-github/bin/deploy.sh "$version"
