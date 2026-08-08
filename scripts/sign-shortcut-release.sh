#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: $0 <exported-unsigned.shortcut> <signed-output.shortcut>" >&2
  exit 64
fi

input=$1
output=$2

if [[ ! -f "$input" ]]; then
  echo "Input Shortcut does not exist: $input" >&2
  exit 66
fi

if ! command -v shortcuts >/dev/null 2>&1; then
  echo "Apple's shortcuts CLI is required. Run this on macOS." >&2
  exit 69
fi

mkdir -p "$(dirname "$output")"
shortcuts sign --mode anyone --input "$input" --output "$output"
chmod 0600 "$output" || true
printf 'Signed Shortcut written to %s\n' "$output"
