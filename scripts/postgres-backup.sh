#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd -- "${script_dir}/.." && pwd)"
env_file="${ZIP_GITHUB_ENV_FILE:-${project_dir}/.env}"

load_env_value_if_unset() {
  local key="$1"
  local line value

  if [[ -n "${!key+x}" || ! -f "$env_file" ]]; then
    return 0
  fi

  line="$(grep -E "^[[:space:]]*${key}=" "$env_file" | tail -n 1 || true)"
  if [[ -z "$line" ]]; then
    return 0
  fi

  value="${line#*=}"
  value="${value%$'\r'}"

  if [[ "$value" == \"*\" && "$value" == *\" ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
    value="${value:1:${#value}-2}"
  fi

  printf -v "$key" '%s' "$value"
  export "$key"
}

load_env_value_if_unset ZIP_GITHUB_BACKUP_DIR
load_env_value_if_unset ZIP_GITHUB_BACKUP_RETENTION_DAYS
load_env_value_if_unset POSTGRES_DB
load_env_value_if_unset POSTGRES_USER

cd "$project_dir"

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
