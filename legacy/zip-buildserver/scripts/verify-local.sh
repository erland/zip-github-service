#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:${BACKEND_PORT:-8080}}"
API_TOKEN="${ZIP_BUILDSERVER_API_TOKEN:-change-me}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-zip-buildserver-e2e}"
DATA_HOST_DIR="${ZIP_BUILDSERVER_DATA_HOST_DIR:-$ROOT_DIR/.local/zip-buildserver-data}"
TMP_DIR="${ROOT_DIR}/target/e2e"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

json_field() {
  local field="$1"
  python3 -c 'import json,sys; data=json.load(sys.stdin); value=data
for part in sys.argv[1].split("."):
    value=value[part]
print(value)' "$field"
}

request_json() {
  local method="$1"
  local url="$2"
  local payload="${3:-}"
  if [[ -n "$payload" ]]; then
    curl -fsS -X "$method" \
      -H "Authorization: Bearer ${API_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "$payload" \
      "$url"
  else
    curl -fsS -X "$method" \
      -H "Authorization: Bearer ${API_TOKEN}" \
      "$url"
  fi
}

wait_for_backend() {
  echo "Waiting for backend health at ${API_BASE_URL}/api/health ..."
  for _ in $(seq 1 90); do
    if curl -fsS "${API_BASE_URL}/api/health" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "Backend did not become healthy." >&2
  docker compose logs backend >&2 || true
  exit 1
}

zip_fixture() {
  local fixture_name="$1"
  local source_dir="${ROOT_DIR}/test-fixtures/${fixture_name}"
  local zip_path="${TMP_DIR}/${fixture_name}.zip"

  if [[ ! -d "$source_dir" ]]; then
    echo "Fixture not found: $source_dir" >&2
    exit 1
  fi

  python3 - "$source_dir" "$zip_path" <<'PY'
import os
import sys
import zipfile
from pathlib import Path

source = Path(sys.argv[1])
target = Path(sys.argv[2])
target.parent.mkdir(parents=True, exist_ok=True)
if target.exists():
    target.unlink()

with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(source.rglob("*")):
        if path.is_file():
            archive.write(path, path.relative_to(source).as_posix())
print(target)
PY
}

create_session() {
  local label="$1"
  request_json POST "${API_BASE_URL}/api/sessions" "{\"label\":\"${label}\"}" | json_field id
}

upload_package() {
  local session_id="$1"
  local zip_path="$2"
  curl -fsS -X POST \
    -H "Authorization: Bearer ${API_TOKEN}" \
    -F "file=@${zip_path}" \
    "${API_BASE_URL}/api/sessions/${session_id}/packages" | json_field id
}

create_run() {
  local session_id="$1"
  local package_id="$2"
  request_json POST \
    "${API_BASE_URL}/api/sessions/${session_id}/runs" \
    "{\"packageId\":\"${package_id}\"}"
}

verify_fixture() {
  local fixture_name="$1"
  local expected_status="$2"
  local zip_path="${TMP_DIR}/${fixture_name}.zip"

  echo
  echo "Verifying fixture ${fixture_name}; expecting ${expected_status}."
  zip_fixture "$fixture_name" >/dev/null

  local session_id package_id run_response run_id actual_status summary
  session_id="$(create_session "e2e-${fixture_name}")"
  package_id="$(upload_package "$session_id" "$zip_path")"
  run_response="$(create_run "$session_id" "$package_id")"
  run_id="$(printf '%s' "$run_response" | json_field id)"
  actual_status="$(printf '%s' "$run_response" | json_field status)"
  summary="$(request_json GET "${API_BASE_URL}/api/runs/${run_id}/summary")"

  echo "Run ${run_id} completed with status ${actual_status}."
  printf '%s\n' "$summary" | python3 -m json.tool

  if [[ "$actual_status" != "$expected_status" ]]; then
    echo "Expected ${fixture_name} to return ${expected_status}, but got ${actual_status}." >&2
    exit 1
  fi
}

cleanup() {
  if [[ "${ZIP_BUILDSERVER_E2E_KEEP_STACK:-false}" != "true" ]]; then
    COMPOSE_PROJECT_NAME="$COMPOSE_PROJECT_NAME" docker compose down -v --remove-orphans >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

main() {
  require_command docker
  require_command curl
  require_command python3

  mkdir -p "$TMP_DIR" "$DATA_HOST_DIR"

  echo "Building worker image."
  "${ROOT_DIR}/scripts/build-worker-image.sh"

  echo "Starting Docker Compose stack for end-to-end verification."
  COMPOSE_PROJECT_NAME="$COMPOSE_PROJECT_NAME" \
  ZIP_BUILDSERVER_AUTH_ENABLED=true \
  ZIP_BUILDSERVER_API_TOKEN="$API_TOKEN" \
  ZIP_BUILDSERVER_WORKER_EXECUTOR=docker \
  ZIP_BUILDSERVER_DATA_HOST_DIR="$DATA_HOST_DIR" \
  ZIP_BUILDSERVER_WORKER_HOST_WORKSPACES_DIR="${DATA_HOST_DIR}/workspaces" \
  ZIP_BUILDSERVER_BACKEND_USER="${ZIP_BUILDSERVER_BACKEND_USER:-root}" \
  docker compose up --build -d

  wait_for_backend

  verify_fixture node-pass PASSED
  verify_fixture node-fail FAILED
  verify_fixture maven-pass PASSED
  verify_fixture maven-fail FAILED

  echo
  echo "End-to-end Docker verification passed."
}

main "$@"
