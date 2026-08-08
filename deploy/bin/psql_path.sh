#!/usr/bin/env bash
# Resolve psql: PG10+ host binary, or BaoTa Docker PostgreSQL (CentOS7 / SCRAM).
# Exports: PSQL, PSQL_MODE=host|docker, DOCKER_PSQL_CONTAINER (if docker)

_psql_major() {
  local bin="${1:-psql}"
  "${bin}" --version 2>/dev/null | awk '{print $3}' | cut -d. -f1
}

_docker_running() {
  local name="$1"
  [[ -n "${name}" ]] || return 1
  command -v docker >/dev/null 2>&1 || return 1
  local st
  st="$(docker inspect -f '{{.State.Running}}' "${name}" 2>/dev/null || true)"
  [[ "${st}" == "true" ]]
}

_use_docker_psql() {
  local name="$1"
  if _docker_running "${name}"; then
    export DOCKER_PSQL_CONTAINER="${name}"
    export PSQL_MODE=docker
    export PSQL="docker:${name}"
    return 0
  fi
  return 1
}

resolve_psql() {
  # 1) Explicit Docker container (BaoTa App Store PG)
  if [[ -n "${DOCKER_PSQL_CONTAINER:-}" ]]; then
    if _use_docker_psql "${DOCKER_PSQL_CONTAINER}"; then
      return 0
    fi
    echo "  WARN: DOCKER_PSQL_CONTAINER=${DOCKER_PSQL_CONTAINER} not running, try fallback..."
  fi

  # 2) Explicit host psql path (must be >= 10 for SCRAM)
  if [[ -n "${PSQL_BIN:-}" && -x "${PSQL_BIN}" ]]; then
    local ver
    ver="$(_psql_major "${PSQL_BIN}")"
    if [[ -n "${ver}" && "${ver}" -ge 10 ]]; then
      export PSQL_MODE=host
      export PSQL="${PSQL_BIN}"
      return 0
    fi
    echo "  WARN: PSQL_BIN=${PSQL_BIN} is too old (need >=10 for SCRAM)"
  fi

  # 3) Host psql in PATH if >= 10
  if command -v psql >/dev/null 2>&1; then
    local ver
    ver="$(_psql_major psql)"
    if [[ -n "${ver}" && "${ver}" -ge 10 ]]; then
      export PSQL_MODE=host
      export PSQL="$(command -v psql)"
      return 0
    fi
  fi

  # 4) Common PG client install paths
  local cand
  for cand in \
    /usr/pgsql-18/bin/psql \
    /usr/pgsql-17/bin/psql \
    /usr/pgsql-16/bin/psql \
    /usr/pgsql-15/bin/psql \
    /usr/pgsql-14/bin/psql \
    /usr/pgsql-13/bin/psql \
    /www/server/pgsql/bin/psql
  do
    if [[ -x "${cand}" ]]; then
      local ver
      ver="$(_psql_major "${cand}")"
      if [[ -n "${ver}" && "${ver}" -ge 10 ]]; then
        export PSQL_MODE=host
        export PSQL="${cand}"
        return 0
      fi
    fi
  done

  # 5) Auto-detect running BaoTa / Docker PostgreSQL container
  if command -v docker >/dev/null 2>&1; then
    local c
    c="$(docker ps --format '{{.Names}}' 2>/dev/null | grep -iE 'postgres|pgsql' | head -n 1 || true)"
    if [[ -n "${c}" ]] && _use_docker_psql "${c}"; then
      echo "  Auto-selected docker psql: ${c}"
      return 0
    fi
  fi

  return 1
}

# run_psql <database> [extra psql args...]
run_psql() {
  local db="$1"
  shift
  if [[ "${PSQL_MODE:-}" == "docker" ]]; then
    docker exec -e PGPASSWORD="${DB_PASSWORD}" "${DOCKER_PSQL_CONTAINER}" \
      psql -U "${DB_USERNAME}" -d "${db}" "$@"
  else
    PGPASSWORD="${DB_PASSWORD}" "${PSQL}" \
      -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${db}" "$@"
  fi
}

# run_psql_file <database> <sqlfile> [extra psql args...]
run_psql_file() {
  local db="$1"
  local file="$2"
  shift 2
  if [[ "${PSQL_MODE:-}" == "docker" ]]; then
    # Host SQL files piped into container psql (avoids SCRAM / old libpq)
    docker exec -i -e PGPASSWORD="${DB_PASSWORD}" "${DOCKER_PSQL_CONTAINER}" \
      psql -U "${DB_USERNAME}" -d "${db}" "$@" < "${file}"
  else
    PGPASSWORD="${DB_PASSWORD}" "${PSQL}" \
      -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${db}" \
      -f "${file}" "$@"
  fi
}
