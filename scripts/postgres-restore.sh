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

load_env_value_if_unset POSTGRES_DB
load_env_value_if_unset POSTGRES_USER

cd "$project_dir"

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
