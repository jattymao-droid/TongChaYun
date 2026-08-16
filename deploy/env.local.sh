#!/usr/bin/env bash
# Local development overrides (Mac). Source AFTER deploy/env.sh or alone.
# Usage:
#   set -a && source deploy/env.sh && source deploy/env.local.sh && set +a

export DOMAIN="${DOMAIN:-localhost}"
export APP_HOME="${APP_HOME_LOCAL:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
export RUOYI_PROFILE="${RUOYI_PROFILE_LOCAL:-${HOME}/ruoyi/uploadPath}"

# Local Postgres / Redis (override production BaoTa ports)
export DB_HOST="${DB_HOST_LOCAL:-127.0.0.1}"
export DB_PORT="${DB_PORT_LOCAL:-5432}"
export DB_NAME="${DB_NAME_LOCAL:-tcy-db}"
export DB_USERNAME="${DB_USERNAME_LOCAL:-postgres}"
# keep DB_PASSWORD from env.sh / shell if already set

export REDIS_HOST="${REDIS_HOST_LOCAL:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT_LOCAL:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD_LOCAL:-}"
export REDIS_DB="${REDIS_DB_LOCAL:-0}"

export SERVER_PORT="${SERVER_PORT:-8080}"
export SWAGGER_ENABLED="${SWAGGER_ENABLED:-true}"

mkdir -p \
  "${RUOYI_PROFILE}" \
  "${RUOYI_PROFILE}/upload" \
  "${RUOYI_PROFILE}/avatar" \
  "${RUOYI_PROFILE}/download" \
  "${RUOYI_PROFILE}/import"

echo "[env.local] APP_HOME=${APP_HOME}"
echo "[env.local] RUOYI_PROFILE=${RUOYI_PROFILE}"
echo "[env.local] DB=${DB_HOST}:${DB_PORT}/${DB_NAME}"
