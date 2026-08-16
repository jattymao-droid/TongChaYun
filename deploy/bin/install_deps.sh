#!/usr/bin/env bash
# Check runtime deps (Java 17+ / usable psql)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/env.sh"
# shellcheck source=/dev/null
source "${ROOT_DIR}/bin/psql_path.sh"

echo "==> Checking dependencies"

need_java=0
if ! command -v java >/dev/null 2>&1; then
  need_java=1
else
  ver="$(java -version 2>&1 | head -n1)"
  echo "  Java: ${ver}"
  major=""
  if [[ "${ver}" =~ \"([0-9]+) ]]; then
    major="${BASH_REMATCH[1]}"
  fi
  if [[ -z "${major}" ]]; then
    major="$(java -version 2>&1 | sed -n 's/.*version \"\([0-9]*\).*/\1/p' | head -n1)"
  fi
  if [[ -n "${major}" && "${major}" -lt 17 ]]; then
    echo "  Java ${major} is too old, need 17+"
    need_java=1
  fi
fi

if [[ "${need_java}" -eq 1 ]]; then
  echo "  ERROR: Java 17+ not found."
  echo "  Install OpenJDK 17/21 in BaoTa, or: yum install -y java-21-openjdk"
  exit 1
fi

if resolve_psql; then
  if [[ "${PSQL_MODE}" == "docker" ]]; then
    echo "  psql: docker exec ${DOCKER_PSQL_CONTAINER} (PG inside container)"
  else
    echo "  psql: ${PSQL} ($("${PSQL}" --version 2>/dev/null | head -n1))"
  fi
else
  echo "  ERROR: no usable psql (CentOS7 host psql 9.2 cannot SCRAM to PG18)."
  echo "  Fix options:"
  echo "    1) Use BaoTa Docker PG (recommended on CentOS7):"
  echo "       export DOCKER_PSQL_CONTAINER=postgresql_18_p5mm-postgresql_18_p5mm-1"
  echo "    2) Or skip DB init after manual import:"
  echo "       SKIP_DB_INIT=1 ./start.sh"
  if [[ "${SKIP_DB_INIT}" != "1" ]]; then
    exit 1
  fi
  echo "  SKIP_DB_INIT=1 set, continue without psql."
fi

mkdir -p "${APP_HOME}" "${RUOYI_PROFILE}" "${RUOYI_PROFILE}/upload" "${RUOYI_PROFILE}/avatar" "${RUOYI_PROFILE}/download" "${RUOYI_PROFILE}/import" "${APP_HOME}/logs"
chmod -R u+rwX,go+rX "${RUOYI_PROFILE}" 2>/dev/null || true
echo "==> Deps OK. APP_HOME=${APP_HOME}"
