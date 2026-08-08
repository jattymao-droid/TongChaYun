#!/usr/bin/env bash
# TongChaYun production env (BaoTa / wj.xmls.vip / CentOS7)

export APP_NAME="tongchayun"
export DOMAIN="wj.xmls.vip"
export APP_HOME="${APP_HOME:-/www/wwwroot/${DOMAIN}}"

# Backend
export SERVER_PORT="${SERVER_PORT:-8080}"
export JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx1024m -Duser.timezone=Asia/Shanghai}"
export RUOYI_PROFILE="${RUOYI_PROFILE:-${APP_HOME}/uploadPath}"
export TOKEN_SECRET="${TOKEN_SECRET:-TongChaYun-ChangeMe-$(hostname | tr -cd 'A-Za-z0-9' | cut -c1-16)}"
export SWAGGER_ENABLED="${SWAGGER_ENABLED:-false}"

# PostgreSQL (BaoTa Docker maps host 35432 -> container 5432)
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-35432}"
export DB_NAME="${DB_NAME:-tongchayun}"
export DB_USERNAME="${DB_USERNAME:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-CHANGE_ME}"

# Host psql path if you installed PG10+ client (optional on CentOS7)
export PSQL_BIN="${PSQL_BIN:-}"

# BaoTa Docker PostgreSQL �� use container psql to avoid host 9.2 SCRAM error
# docker ps | grep postgres  ��  postgresql_18_p5mm-postgresql_18_p5mm-1
export DOCKER_PSQL_CONTAINER="${DOCKER_PSQL_CONTAINER:-postgresql_18_p5mm-postgresql_18_p5mm-1}"

# Redis (BaoTa)
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-26739}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-CHANGE_ME}"
export REDIS_DB="${REDIS_DB:-0}"

# true = import when empty; force = drop & recreate; false = skip
export INIT_DB="${INIT_DB:-true}"
# 1 = skip DB init (app start only)
export SKIP_DB_INIT="${SKIP_DB_INIT:-0}"
