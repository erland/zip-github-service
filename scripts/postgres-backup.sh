#!/usr/bin/env bash
set -euo pipefail

backup_dir="${ZIP_GITHUB_BACKUP_DIR:-./backups/postgres}"
retention_days="${ZIP_GITHUB_BACKUP_RETENTION_DAYS:-14}"
database="${POSTGRES_DB:-zip_github}"
user="${POSTGRES_USER:-zip_github}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
output="${backup_dir}/${database}-${timestamp}.dump"

mkdir -p "$backup_dir"
umask 077

docker compose exec -T postgres pg_dump \
  --username "$user" \
  --dbname "$database" \
  --format=custom \
  --no-owner \
  --no-privileges > "$output"

if [[ ! -s "$output" ]]; then
  rm -f "$output"
  echo "Backup failed: empty output." >&2
  exit 1
fi

sha256sum "$output" > "${output}.sha256"
find "$backup_dir" -type f \( -name '*.dump' -o -name '*.dump.sha256' \) \
  -mtime "+${retention_days}" -delete

printf 'Created %s\n' "$output"
