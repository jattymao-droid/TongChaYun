#!/usr/bin/env bash
# Start TongChaYun backend
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/env.sh"

JAR="${ROOT_DIR}/app/ruoyi-admin.jar"
PID_FILE="${APP_HOME}/tongchayun.pid"
LOG_FILE="${APP_HOME}/logs/app.log"

if [[ ! -f "${JAR}" ]]; then
  echo "ERROR: missing ${JAR}"
  exit 1
fi

mkdir -p "${APP_HOME}/logs" \
  "${RUOYI_PROFILE}" \
  "${RUOYI_PROFILE}/upload" \
  "${RUOYI_PROFILE}/avatar" \
  "${RUOYI_PROFILE}/download" \
  "${RUOYI_PROFILE}/import"
# Ensure the Java process can write uploads (safe no-op if already correct)
chmod -R u+rwX,go+rX "${RUOYI_PROFILE}" 2>/dev/null || true

if [[ -f "${PID_FILE}" ]]; then
  old="$(cat "${PID_FILE}" || true)"
  if [[ -n "${old}" ]] && kill -0 "${old}" 2>/dev/null; then
    echo "Already running (pid=${old}). Use ./bin/stop.sh to restart."
    exit 0
  fi
fi

echo "==> Starting backend on :${SERVER_PORT}"
nohup env \
  DB_HOST="${DB_HOST}" \
  DB_PORT="${DB_PORT}" \
  DB_NAME="${DB_NAME}" \
  DB_USERNAME="${DB_USERNAME}" \
  DB_PASSWORD="${DB_PASSWORD}" \
  TOKEN_SECRET="${TOKEN_SECRET}" \
  SWAGGER_ENABLED="${SWAGGER_ENABLED}" \
  RUOYI_PROFILE="${RUOYI_PROFILE}" \
  java ${JAVA_OPTS} \
  -jar "${JAR}" \
  --server.port="${SERVER_PORT}" \
  --spring.data.redis.host="${REDIS_HOST}" \
  --spring.data.redis.port="${REDIS_PORT}" \
  --spring.data.redis.password="${REDIS_PASSWORD}" \
  --spring.data.redis.database="${REDIS_DB}" \
  --ruoyi.profile="${RUOYI_PROFILE}" \
  >"${LOG_FILE}" 2>&1 &

echo $! > "${PID_FILE}"
sleep 3

if kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
  echo "==> Started pid=$(cat "${PID_FILE}")"
  echo "    log : ${LOG_FILE}"
  echo "    site: https://${DOMAIN}/"
  echo "    H5  : https://${DOMAIN}/h5/"
  echo "    API : https://${DOMAIN}/prod-api/"
  echo "    login: admin / admin123"
else
  echo "==> Start failed, see ${LOG_FILE}"
  tail -n 50 "${LOG_FILE}" || true
  exit 1
fi
