#!/usr/bin/env bash
set -euo pipefail
status_file="docs/implementation-status.md"
test -f "$status_file"
next_count=$(grep -c '| \*\*NEXT\*\* |' "$status_file" || true)
if [[ "$next_count" -ne 1 ]]; then
  printf 'Expected exactly one NEXT step, found %s.\n' "$next_count" >&2
  exit 1
fi
current_next=$(sed -n 's/^- Next step: `\([^`]*\)`.*/\1/p' "$status_file")
ledger_next=$(awk -F'|' '/\*\*NEXT\*\*/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); gsub(/`/, "", $2); print $2}' "$status_file")
if [[ -z "$current_next" || "$current_next" != "$ledger_next" ]]; then
  printf 'Current position NEXT (%s) does not match ledger NEXT (%s).\n' "$current_next" "$ledger_next" >&2
  exit 1
fi
printf 'Implementation ledger verified: next step %s.\n' "$ledger_next"
