#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
SQL="${ROOT}/prod_patch_p28_p30.sql"
if [[ -f "${ROOT}/../env.sh" ]]; then
  # shellcheck source=/dev/null
  source "${ROOT}/../env.sh"
fi
DB_NAME="${DB_NAME:-tongchayun}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DOCKER_PSQL_CONTAINER="${DOCKER_PSQL_CONTAINER:-postgresql_18_p5mm-postgresql_18_p5mm-1}"
echo "==> Apply ${SQL} -> ${DB_NAME}"
if [[ -n "${DOCKER_PSQL_CONTAINER}" ]] && command -v docker >/dev/null && docker inspect "${DOCKER_PSQL_CONTAINER}" >/dev/null 2>&1; then
  docker exec -i "${DOCKER_PSQL_CONTAINER}" psql -U "${DB_USERNAME}" -d "${DB_NAME}" < "${SQL}"
else
  export PGPASSWORD="${DB_PASSWORD:-}"
  psql -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-35432}" -U "${DB_USERNAME}" -d "${DB_NAME}" -f "${SQL}"
fi
echo "==> Done"
