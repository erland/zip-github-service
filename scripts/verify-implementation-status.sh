#!/usr/bin/env bash
set -euo pipefail
status_file="docs/implementation-status.md"
test -f "$status_file"
next_count=$(grep -c '| \*\*NEXT\*\* |' "$status_file" || true)
blocked_count=$(grep -c '| \*\*BLOCKED\*\* |' "$status_file" || true)

if [[ "$next_count" -eq 1 && "$blocked_count" -eq 0 ]]; then
  current_next=$(sed -n 's/^- Next step: `\([^`]*\)`.*/\1/p' "$status_file")
  ledger_next=$(awk -F'|' '/\*\*NEXT\*\*/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); gsub(/`/, "", $2); print $2}' "$status_file")
  if [[ -z "$current_next" || "$current_next" != "$ledger_next" ]]; then
    printf 'Current position NEXT (%s) does not match ledger NEXT (%s).\n' "$current_next" "$ledger_next" >&2
    exit 1
  fi
  printf 'Implementation ledger verified: next step %s.\n' "$ledger_next"
  exit 0
fi

# AGENTS.md permits a delivered revision without NEXT when the current step is blocked.
if [[ "$next_count" -eq 0 && "$blocked_count" -eq 1 ]]; then
  blocked_step=$(awk -F'|' '/\*\*BLOCKED\*\*/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); gsub(/`/, "", $2); print $2}' "$status_file")
  grep -Fq -- "- Next step: blocked at \`$blocked_step\`" "$status_file" || {
    printf 'Blocked ledger step %s is not reflected in Current position.\n' "$blocked_step" >&2
    exit 1
  }
  printf 'Implementation ledger verified: blocked at step %s; no NEXT step may run.\n' "$blocked_step"
  exit 0
fi

printf 'Expected exactly one NEXT, or zero NEXT with exactly one BLOCKED step; found NEXT=%s BLOCKED=%s.\n' "$next_count" "$blocked_count" >&2
exit 1
