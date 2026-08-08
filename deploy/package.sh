#!/usr/bin/env bash
# Build backend + admin-ui + H5 and pack deploy/release/*.tar.gz
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_DIR="${PROJECT_ROOT}/deploy"
STAGING="${DEPLOY_DIR}/.staging"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_TGZ="${DEPLOY_DIR}/release/tongchayun-${STAMP}.tar.gz"
DOMAIN="${DOMAIN:-wj.xmls.vip}"

export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# Prefer JDK 21 on macOS Homebrew if present
if [[ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

echo "==> Project: ${PROJECT_ROOT}"
echo "==> Java: $(java -version 2>&1 | head -n1)"
rm -rf "${STAGING}"
mkdir -p "${STAGING}/app" "${STAGING}/sql" "${STAGING}/bin" "${STAGING}/nginx" \
  "${STAGING}/logs" "${DEPLOY_DIR}/release"

# ---------- Backend ----------
echo "==> Maven package (ruoyi-admin)"
cd "${PROJECT_ROOT}"
mvn -pl ruoyi-admin -am clean package -DskipTests -q
cp -f "${PROJECT_ROOT}/ruoyi-admin/target/ruoyi-admin.jar" "${STAGING}/app/ruoyi-admin.jar"

# ---------- Admin UI ----------
echo "==> Build admin-ui (ruoyi-ui)"
cd "${PROJECT_ROOT}/ruoyi-ui"
cat > .env.production.local <<EOF
VUE_APP_TITLE = 通查云
ENV = 'production'
VUE_APP_BASE_API = '/prod-api'
VUE_APP_H5_BASE = 'https://${DOMAIN}/h5'
EOF
npm install --registry=https://registry.npmmirror.com
npm run build:prod
rm -rf "${STAGING}/admin-ui"
cp -R dist "${STAGING}/admin-ui"
rm -f .env.production.local

# ---------- H5 ----------
echo "==> Build H5 (ruoyi-h5)"
cd "${PROJECT_ROOT}/ruoyi-h5"
cat > .env.production.local <<EOF
VITE_BASE=/h5/
VITE_APP_BASE_API=/prod-api
VITE_SITE_NAME=通查云
VITE_SITE_URL=https://${DOMAIN}/login
EOF
npm install --registry=https://registry.npmmirror.com
npm run build
rm -rf "${STAGING}/h5"
cp -R dist "${STAGING}/h5"
rm -f .env.production.local

# ---------- Scripts / nginx / SQL ----------
echo "==> Copy deploy scripts + SQL (UTF-8)"
# strip CR and office lock junk
cp -f "${DEPLOY_DIR}/env.sh" "${STAGING}/env.sh"
cp -f "${DEPLOY_DIR}/start.sh" "${STAGING}/start.sh"
for f in "${DEPLOY_DIR}/bin/"*.sh; do
  base="$(basename "${f}")"
  case "${base}" in
    .\!*) continue ;;
  esac
  python3 - "${f}" "${STAGING}/bin/${base}" <<'PY'
from pathlib import Path
import sys
src, dst = Path(sys.argv[1]), Path(sys.argv[2])
text = src.read_bytes().decode("utf-8", errors="replace").replace("\r\n", "\n").replace("\r", "\n")
dst.write_text(text, encoding="utf-8", newline="\n")
PY
done
cp -f "${DEPLOY_DIR}/nginx/"*.conf "${STAGING}/nginx/" 2>/dev/null || true
# macOS tr can choke on non-C locale; use python for CRLF strip
python3 - "${DEPLOY_DIR}/sql/init_order.txt" "${STAGING}/sql/init_order.txt" <<'PY'
from pathlib import Path
import sys
src, dst = Path(sys.argv[1]), Path(sys.argv[2])
text = src.read_bytes().decode("utf-8", errors="replace").replace("\r\n", "\n").replace("\r", "\n")
dst.write_text(text, encoding="utf-8", newline="\n")
PY

copy_sql_utf8() {
  local src="$1"
  local dst="$2"
  python3 - "$src" "$dst" <<'PY'
import sys
from pathlib import Path
src, dst = Path(sys.argv[1]), Path(sys.argv[2])
raw = src.read_bytes()
text = None
for enc in ("utf-8-sig", "utf-8", "gb18030", "gbk"):
    try:
        text = raw.decode(enc)
        break
    except UnicodeDecodeError:
        continue
if text is None:
    text = raw.decode("latin1")
text = text.replace("\r\n", "\n").replace("\r", "\n")
dst.write_text(text, encoding="utf-8", newline="\n")
PY
}

while IFS= read -r line || [[ -n "${line}" ]]; do
  line="${line%$'\r'}"
  line="$(echo "${line}" | sed 's/#.*//;s/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "${line}" ]] && continue
  case "${line}" in
    *.sql) ;;
    *) continue ;;
  esac
  src="${PROJECT_ROOT}/sql/${line}"
  if [[ -f "${src}" ]]; then
    copy_sql_utf8 "${src}" "${STAGING}/sql/${line}"
  else
    echo "  ! missing SQL: ${line}"
  fi
done < "${DEPLOY_DIR}/sql/init_order.txt"

# ---------- Full DB dump (local tcy-db by default) ----------
DUMP_DB_NAME="${DUMP_DB_NAME:-tcy-db}"
DUMP_DB_HOST="${DUMP_DB_HOST:-127.0.0.1}"
DUMP_DB_PORT="${DUMP_DB_PORT:-5432}"
DUMP_DB_USER="${DUMP_DB_USER:-postgres}"
# local dump password: prefer explicit DUMP_DB_PASSWORD, else deploy/env.sh DB_PASSWORD
if [[ -z "${DUMP_DB_PASSWORD:-}" ]]; then
  # shellcheck source=/dev/null
  source "${DEPLOY_DIR}/env.sh" >/dev/null 2>&1 || true
  DUMP_DB_PASSWORD="${DB_PASSWORD:-}"
fi
INCLUDE_DB_DUMP="${INCLUDE_DB_DUMP:-1}"

if [[ "${INCLUDE_DB_DUMP}" == "1" ]]; then
  echo "==> Dump database ${DUMP_DB_NAME} (${DUMP_DB_HOST}:${DUMP_DB_PORT}) -> sql/full_dump.sql"
  export PGPASSWORD="${DUMP_DB_PASSWORD}"
  if ! pg_dump -h "${DUMP_DB_HOST}" -p "${DUMP_DB_PORT}" -U "${DUMP_DB_USER}" \
      -d "${DUMP_DB_NAME}" \
      --no-owner --no-acl --clean --if-exists \
      -F p -f "${STAGING}/sql/full_dump.sql"; then
    echo "ERROR: pg_dump failed for ${DUMP_DB_NAME}"
    echo "  Set DUMP_DB_PASSWORD / DUMP_DB_* or INCLUDE_DB_DUMP=0 to skip"
    exit 1
  fi
  echo "full_dump.sql" > "${STAGING}/sql/USE_FULL_DUMP"
  DUMP_SIZE="$(du -h "${STAGING}/sql/full_dump.sql" | awk '{print $1}')"
  echo "  dump size: ${DUMP_SIZE}"
fi

# README in English to avoid mojibake
cat > "${STAGING}/README.md" <<EOF
# TongChaYun deploy (${DOMAIN})

## Start
\`\`\`bash
tar -xzf tongchayun-latest.tar.gz
chmod +x start.sh bin/*.sh
./start.sh
\`\`\`

## Stop
\`\`\`bash
./bin/stop.sh
\`\`\`

## Database
- Incremental SQL scripts are under sql/ (init_order.txt)
- If sql/full_dump.sql exists, init_db.sh restores it (full snapshot from build machine)
- Force rebuild: INIT_DB=force ./start.sh

## URLs
- Admin: https://${DOMAIN}/
- H5: https://${DOMAIN}/h5/
- API: https://${DOMAIN}/prod-api/

Default login: admin / admin123

CentOS7 + BaoTa Docker PostgreSQL: set DOCKER_PSQL_CONTAINER in env.sh
EOF

chmod +x "${STAGING}/start.sh" "${STAGING}/bin/"*.sh "${STAGING}/env.sh"
# normalize scripts to LF (no .tmp leftovers)
python3 - "${STAGING}" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1])
files = [root / "start.sh", root / "env.sh", *sorted((root / "bin").glob("*.sh"))]
for p in files:
    if not p.is_file():
        continue
    text = p.read_bytes().decode("utf-8", errors="replace").replace("\r\n", "\n").replace("\r", "\n")
    p.write_text(text, encoding="utf-8", newline="\n")
    p.chmod(0o755)
PY
# remove accidental tmp junk
find "${STAGING}" -name '*.tmp' -delete
find "${STAGING}" -name '.!*' -delete

# ---------- Pack ----------
echo "==> Packing ${OUT_TGZ}"
tar -C "${STAGING}" -czf "${OUT_TGZ}" .
cp -f "${OUT_TGZ}" "${DEPLOY_DIR}/release/tongchayun-latest.tar.gz"

SIZE="$(du -h "${OUT_TGZ}" | awk '{print $1}')"
echo ""
echo "========================================"
echo " Built: ${OUT_TGZ}"
echo " Size : ${SIZE}"
echo " Alias: ${DEPLOY_DIR}/release/tongchayun-latest.tar.gz"
echo "========================================"
echo "Upload example:"
echo "  scp ${DEPLOY_DIR}/release/tongchayun-latest.tar.gz root@SERVER:/www/wwwroot/${DOMAIN}/"
echo "On server:"
echo "  cd /www/wwwroot/${DOMAIN} && tar -xzf tongchayun-latest.tar.gz && ./bin/stop.sh; ./start.sh"
