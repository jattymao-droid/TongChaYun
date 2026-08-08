#!/usr/bin/env bash
# TongChaYun one-click start (CentOS7 + BaoTa Docker PostgreSQL 18 OK)
# Flow: deps -> DB init (docker/host psql) -> start jar
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "${ROOT_DIR}"

# Avoid Chinese mojibake on CentOS7 minimal locales
export LANG="${LANG:-en_US.UTF-8}"
export LC_ALL="${LC_ALL:-en_US.UTF-8}"

chmod +x bin/*.sh env.sh 2>/dev/null || true
# shellcheck source=/dev/null
source "${ROOT_DIR}/env.sh"

export APP_HOME="${APP_HOME:-${ROOT_DIR}}"
export RUOYI_PROFILE="${RUOYI_PROFILE:-${APP_HOME}/uploadPath}"

# Export for child scripts
export DB_HOST DB_PORT DB_NAME DB_USERNAME DB_PASSWORD
export REDIS_HOST REDIS_PORT REDIS_PASSWORD REDIS_DB
export TOKEN_SECRET SWAGGER_ENABLED RUOYI_PROFILE SERVER_PORT JAVA_OPTS
export PSQL_BIN DOCKER_PSQL_CONTAINER SKIP_DB_INIT INIT_DB DOMAIN APP_HOME

echo "========================================"
echo " TongChaYun start"
echo " Domain : ${DOMAIN}"
echo " Home   : ${APP_HOME}"
echo " DB     : ${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo " Redis  : ${REDIS_HOST}:${REDIS_PORT}"
if [[ -n "${DOCKER_PSQL_CONTAINER:-}" ]]; then
  echo " PSQL   : docker://${DOCKER_PSQL_CONTAINER}"
elif [[ -n "${PSQL_BIN:-}" ]]; then
  echo " PSQL   : ${PSQL_BIN}"
else
  echo " PSQL   : auto (host>=10 or docker)"
fi
echo "========================================"

bash "${ROOT_DIR}/bin/install_deps.sh"
bash "${ROOT_DIR}/bin/init_db.sh"
bash "${ROOT_DIR}/bin/start_app.sh"

echo ""
echo "Static sites:"
echo "  ${APP_HOME}/admin-ui/"
echo "  ${APP_HOME}/h5/"
echo "Nginx sample: ${ROOT_DIR}/nginx/wj.xmls.vip.conf"
echo "Default login: admin / admin123"
echo ""
echo "Stop:  ${ROOT_DIR}/bin/stop.sh"
echo "Log:   ${APP_HOME}/logs/app.log"
