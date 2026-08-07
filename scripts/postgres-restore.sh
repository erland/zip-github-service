#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backup.dump>" >&2
  exit 64
fi

backup="$1"
database="${POSTGRES_DB:-zip_github}"
user="${POSTGRES_USER:-zip_github}"

if [[ ! -r "$backup" ]]; then
  echo "Backup is not readable: $backup" >&2
  exit 66
fi

if [[ -f "${backup}.sha256" ]]; then
  sha256sum --check "${backup}.sha256"
fi

if [[ "${ZIP_GITHUB_CONFIRM_RESTORE:-}" != "RESTORE ${database}" ]]; then
  echo "Restore refused. Set ZIP_GITHUB_CONFIRM_RESTORE='RESTORE ${database}'." >&2
  exit 65
fi

cat "$backup" | docker compose exec -T postgres pg_restore \
  --username "$user" \
  --dbname "$database" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges

printf 'Restored %s into %s\n' "$backup" "$database"
