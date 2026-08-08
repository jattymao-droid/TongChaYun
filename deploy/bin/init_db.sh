#!/usr/bin/env bash
# Init PostgreSQL database and import SQL scripts
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/env.sh"
# shellcheck source=/dev/null
source "${ROOT_DIR}/bin/psql_path.sh"

SQL_DIR="${ROOT_DIR}/sql"
ORDER_FILE="${ROOT_DIR}/sql/init_order.txt"
MARK="${APP_HOME}/.db_initialized"

if [[ "${SKIP_DB_INIT}" == "1" || "${INIT_DB}" == "false" ]]; then
  echo "==> Skip DB init (SKIP_DB_INIT/INIT_DB)"
  exit 0
fi

if ! resolve_psql; then
  echo "ERROR: no usable psql (need PG10+ client or Docker PostgreSQL)."
  echo "  Set DOCKER_PSQL_CONTAINER=postgresql_18_p5mm-postgresql_18_p5mm-1"
  echo "  or install postgresql18 client and set PSQL_BIN=/usr/pgsql-18/bin/psql"
  exit 1
fi

echo "==> Checking PostgreSQL via ${PSQL_MODE} (${DOCKER_PSQL_CONTAINER:-${PSQL}})"
run_psql postgres -c "SELECT 1;" >/dev/null

EXISTS="$(run_psql postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'")"
EXISTS="$(echo "${EXISTS}" | tr -d '[:space:]')"
if [[ "${EXISTS}" != "1" ]]; then
  echo "==> Creating database ${DB_NAME}"
  run_psql postgres -c "CREATE DATABASE ${DB_NAME} ENCODING 'UTF8' TEMPLATE template0;"
else
  echo "==> Database ${DB_NAME} already exists"
fi

HAS_TABLE="$(run_psql "${DB_NAME}" -tAc \
  "SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name='sys_user' LIMIT 1" || true)"
HAS_TABLE="$(echo "${HAS_TABLE}" | tr -d '[:space:]')"

if [[ "${HAS_TABLE}" == "1" && "${INIT_DB}" != "force" ]]; then
  echo "==> sys_user exists, skip full import (set INIT_DB=force to rebuild)"
  mkdir -p "${APP_HOME}"
  touch "${MARK}"
  exit 0
fi

if [[ "${INIT_DB}" == "force" && "${HAS_TABLE}" == "1" ]]; then
  echo "==> INIT_DB=force, resetting public schema"
  run_psql "${DB_NAME}" \
    -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO ${DB_USERNAME}; GRANT ALL ON SCHEMA public TO public;"
fi

# Prefer full snapshot if packaged
FULL_DUMP="${SQL_DIR}/full_dump.sql"
if [[ -f "${FULL_DUMP}" ]]; then
  echo "==> Restoring full_dump.sql (packaged DB snapshot)"
  tmp_sql="$(mktemp)"
  cp "${FULL_DUMP}" "${tmp_sql}"
  run_psql_file "${DB_NAME}" "${tmp_sql}" >/dev/null || true
  rm -f "${tmp_sql}"
  mkdir -p "${APP_HOME}"
  touch "${MARK}"
  echo "==> DB restore from full_dump done"
  run_psql "${DB_NAME}" -tAc \
    "SELECT 'tables='||count(*) FROM information_schema.tables WHERE table_schema='public';"
  exit 0
fi

if [[ ! -f "${ORDER_FILE}" ]]; then
  echo "Missing ${ORDER_FILE}"
  exit 1
fi

echo "==> Importing SQL scripts"
while IFS= read -r line || [[ -n "${line}" ]]; do
  # strip CR, comments, whitespace
  line="${line%$'\r'}"
  line="$(echo "${line}" | sed 's/#.*//;s/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "${line}" ]] && continue
  # only accept *.sql filenames (ignore garbled comment leftovers)
  case "${line}" in
    *.sql) ;;
    *) echo "  ! skip non-sql line: ${line}"; continue ;;
  esac
  f="${SQL_DIR}/${line}"
  if [[ ! -f "${f}" ]]; then
    echo "  ! skip missing: ${line}"
    continue
  fi
  echo "  -> ${line}"
  # ensure UTF-8 feed for PG (CentOS7 may ship GBK sql)
  tmp_sql="$(mktemp)"
  if command -v iconv >/dev/null 2>&1; then
    if ! iconv -f UTF-8 -t UTF-8 "${f}" >"${tmp_sql}" 2>/dev/null; then
      iconv -f GBK -t UTF-8 "${f}" >"${tmp_sql}" 2>/dev/null || cp "${f}" "${tmp_sql}"
    fi
  else
    cp "${f}" "${tmp_sql}"
  fi
  if [[ "${line}" == ry_postgresql.sql || "${line}" == quartz_postgresql.sql || "${line}" == biz_postgresql.sql ]]; then
    run_psql_file "${DB_NAME}" "${tmp_sql}" -v ON_ERROR_STOP=1 >/dev/null
  else
    run_psql_file "${DB_NAME}" "${tmp_sql}" >/dev/null || true
  fi
  rm -f "${tmp_sql}"
done < "${ORDER_FILE}"

mkdir -p "${APP_HOME}"
touch "${MARK}"
echo "==> DB init done"
run_psql "${DB_NAME}" -tAc \
  "SELECT 'tables='||count(*) FROM information_schema.tables WHERE table_schema='public';"
