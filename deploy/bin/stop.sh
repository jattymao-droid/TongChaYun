#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=/dev/null
source "${ROOT_DIR}/env.sh"
PID_FILE="${APP_HOME}/tongchayun.pid"

if [[ -f "${PID_FILE}" ]]; then
  pid="$(cat "${PID_FILE}")"
  if kill -0 "${pid}" 2>/dev/null; then
    kill "${pid}" || true
    sleep 2
    kill -9 "${pid}" 2>/dev/null || true
    echo "Stopped pid=${pid}"
  else
    echo "Process not running"
  fi
  rm -f "${PID_FILE}"
else
  if command -v lsof >/dev/null 2>&1; then
    pids="$(lsof -tiTCP:"${SERVER_PORT}" -sTCP:LISTEN || true)"
    if [[ -n "${pids}" ]]; then
      kill ${pids} 2>/dev/null || true
      echo "Stopped by port ${SERVER_PORT}: ${pids}"
    else
      echo "No running service found"
    fi
  else
    echo "No pid file found"
  fi
fi
