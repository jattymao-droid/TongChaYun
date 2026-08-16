#!/usr/bin/env bash
# Fix upload directory permissions for background/image uploads.
# Run on the production server (BaoTa):
#   cd /www/wwwroot/wj.xmls.vip && bash bin/fix_upload_path.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -f "${ROOT_DIR}/env.sh" ]]; then
  # shellcheck source=/dev/null
  source "${ROOT_DIR}/env.sh"
fi

DOMAIN="${DOMAIN:-wj.xmls.vip}"
APP_HOME="${APP_HOME:-/www/wwwroot/${DOMAIN}}"
RUOYI_PROFILE="${RUOYI_PROFILE:-${APP_HOME}/uploadPath}"

echo "==> APP_HOME=${APP_HOME}"
echo "==> RUOYI_PROFILE=${RUOYI_PROFILE}"

mkdir -p \
  "${RUOYI_PROFILE}" \
  "${RUOYI_PROFILE}/upload" \
  "${RUOYI_PROFILE}/avatar" \
  "${RUOYI_PROFILE}/download" \
  "${RUOYI_PROFILE}/import"

# Prefer the user that owns the site / runs Java
OWNER=""
if id www >/dev/null 2>&1; then
  OWNER="www"
elif id www-data >/dev/null 2>&1; then
  OWNER="www-data"
fi

# If jar is already running, match that process user
if command -v lsof >/dev/null 2>&1; then
  port_pid="$(lsof -tiTCP:"${SERVER_PORT:-8080}" -sTCP:LISTEN 2>/dev/null | head -1 || true)"
  if [[ -n "${port_pid}" ]]; then
    proc_user="$(ps -o user= -p "${port_pid}" 2>/dev/null | awk '{print $1}')"
    if [[ -n "${proc_user}" && "${proc_user}" != "root" ]]; then
      OWNER="${proc_user}"
    fi
  fi
fi

if [[ -n "${OWNER}" ]]; then
  echo "==> chown -R ${OWNER}:${OWNER} ${RUOYI_PROFILE}"
  chown -R "${OWNER}:${OWNER}" "${RUOYI_PROFILE}"
else
  echo "==> skip chown (no www/www-data user detected)"
fi

chmod -R u+rwX,go+rX "${RUOYI_PROFILE}"

# Write test as OWNER when possible
TEST_FILE="${RUOYI_PROFILE}/upload/.write_test_$$"
if [[ -n "${OWNER}" ]] && command -v sudo >/dev/null 2>&1; then
  if sudo -u "${OWNER}" touch "${TEST_FILE}" 2>/dev/null; then
    sudo -u "${OWNER}" rm -f "${TEST_FILE}"
    echo "==> write test OK as ${OWNER}"
  else
    echo "==> WARN: write test failed as ${OWNER}"
    ls -la "${RUOYI_PROFILE}" "${RUOYI_PROFILE}/upload" || true
    exit 1
  fi
else
  touch "${TEST_FILE}" && rm -f "${TEST_FILE}"
  echo "==> write test OK as $(whoami)"
fi

ls -ld "${RUOYI_PROFILE}" "${RUOYI_PROFILE}/upload"
echo "==> done. Re-try background image upload."
