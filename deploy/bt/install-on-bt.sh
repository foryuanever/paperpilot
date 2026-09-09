#!/usr/bin/env bash
set -euo pipefail

APP_NAME="papersolver"
DOMAIN="${PAPERSOLVER_DOMAIN:-papersolver.cn}"
WEB_ROOT="/www/wwwroot/${APP_NAME}"
APP_ROOT="/www/server/${APP_NAME}"
BACKEND_ROOT="${APP_ROOT}/backend"
LOG_ROOT="${APP_ROOT}/logs"
ENV_ROOT="/etc/${APP_NAME}"
ENV_FILE="${ENV_ROOT}/papersolver.env"
SERVICE_FILE="/etc/systemd/system/papersolver.service"
BT_NGINX_CONF="/www/server/panel/vhost/nginx/${DOMAIN}.conf"

cd "$(dirname "$0")"

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: $1 not found. Please install it first."
    exit 1
  fi
}

need_cmd java
need_cmd systemctl

mkdir -p "$WEB_ROOT" "$BACKEND_ROOT" "$LOG_ROOT" "$ENV_ROOT"

mask_env() {
  sed -E \
    -e 's/(PASSWORD=).*/\1***hidden***/' \
    -e 's/(API_V3_KEY=).*/\1***hidden***/' \
    -e 's/(SECRET[^=]*=).*/\1***hidden***/' \
    -e 's/(PRIVATE_KEY=).*/\1***hidden***/'
}

dump_backend_diagnostics() {
  echo
  echo "========== PaperSolver diagnostics =========="
  echo "[systemd]"
  systemctl status papersolver --no-pager || true
  echo
  echo "[journalctl]"
  journalctl -u papersolver -n 160 --no-pager || true
  echo
  echo "[backend.log]"
  tail -n 160 "$LOG_ROOT/backend.log" 2>/dev/null || true
  echo
  echo "[backend-error.log]"
  tail -n 160 "$LOG_ROOT/backend-error.log" 2>/dev/null || true
  echo
  echo "[env masked]"
  if [ -f "$ENV_FILE" ]; then
    mask_env < "$ENV_FILE" || true
  fi
  echo "========== diagnostics end =========="
  echo
}

env_value() {
  local key="$1"
  grep -E "^${key}=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2-
}

echo "Deploying frontend to ${WEB_ROOT}"
if command -v rsync >/dev/null 2>&1; then
  rsync -a --delete frontend/ "$WEB_ROOT"/
else
  find "$WEB_ROOT" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
  cp -a frontend/. "$WEB_ROOT"/
fi

echo "Deploying backend jar to ${BACKEND_ROOT}"
cp backend/paperpilot-server-0.0.1-SNAPSHOT.jar "$BACKEND_ROOT"/paperpilot-server-0.0.1-SNAPSHOT.jar

if [ -s certs/apiclient_key.pem ]; then
  echo "Installing WeChat merchant private key"
  mkdir -p "$WEB_ROOT/certs"
  install -m 600 certs/apiclient_key.pem "$WEB_ROOT/certs/apiclient_key.pem"
fi

if [ -s certs/wechatpay_public_key.pem ]; then
  echo "Installing WeChat Pay public key (公钥模式)"
  mkdir -p "$WEB_ROOT/certs"
  install -m 644 certs/wechatpay_public_key.pem "$WEB_ROOT/certs/wechatpay_public_key.pem"
fi

if [ -f papersolver.env ]; then
  if [ -f "$ENV_FILE" ] && grep -q '^PAPER_DB_PASSWORD=__KEEP_EXISTING__' papersolver.env; then
    existing_db_password="$(grep -E '^PAPER_DB_PASSWORD=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)"
    if [ -n "$existing_db_password" ]; then
      EXISTING_DB_PASSWORD="$existing_db_password" python3 - <<'PY'
from pathlib import Path
import os

path = Path("papersolver.env")
lines = path.read_text().splitlines()
password = os.environ.get("EXISTING_DB_PASSWORD", "")
path.write_text("\n".join(
    f"PAPER_DB_PASSWORD={password}" if line == "PAPER_DB_PASSWORD=__KEEP_EXISTING__" else line
    for line in lines
) + "\n")
PY
    fi
  fi
  if [ -f "$ENV_FILE" ]; then
    cp "$ENV_FILE" "${ENV_FILE}.bak.$(date +%Y%m%d%H%M%S)"
  fi
  install -m 600 papersolver.env "$ENV_FILE"
elif [ ! -f "$ENV_FILE" ]; then
  echo "Creating ${ENV_FILE}. Fill secrets before first start."
  install -m 600 papersolver.env.example "$ENV_FILE"
else
  echo "Keeping existing ${ENV_FILE}"
fi

install -m 644 papersolver.service "$SERVICE_FILE"
systemctl daemon-reload
systemctl enable papersolver >/dev/null

ensure_nginx() {
  if [ ! -f "$BT_NGINX_CONF" ]; then
    echo "WARNING: BaoTa Nginx config not found: ${BT_NGINX_CONF}"
    echo "Add deploy/bt/nginx-api-location.conf into the ${DOMAIN} server block manually."
    return 0
  fi
  local backup="${BT_NGINX_CONF}.bak.$(date +%Y%m%d%H%M%S)"
  cp "$BT_NGINX_CONF" "$backup"
  python3 - "$BT_NGINX_CONF" "$WEB_ROOT" <<'PY'
from pathlib import Path
import re
import sys

conf_path = Path(sys.argv[1])
web_root = sys.argv[2]
text = conf_path.read_text()

managed = """
    # PaperSolver managed routes BEGIN
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header X-Frame-Options "SAMEORIGIN" always;

    location ~ /\.(?!well-known) {
        deny all;
    }

    location ~* \.(?:env|bak|sql|log|old)$ {
        deny all;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        client_max_body_size 130m;
    }

    location /actuator/ {
        return 404;
    }
    # PaperSolver managed routes END
"""

pattern = re.compile(r"\n\s*# PaperSolver managed routes BEGIN.*?# PaperSolver managed routes END\n", re.S)
root_pattern = re.compile(r"(?m)^(\s*)root\s+[^;]+;")
if root_pattern.search(text):
    text = root_pattern.sub(rf"\1root {web_root};", text, count=1)
else:
    server_pos = text.find("{")
    if server_pos != -1:
        text = text[:server_pos + 1] + f"\n    root {web_root};\n    index index.html index.htm;\n" + text[server_pos + 1:]

if pattern.search(text):
    text = pattern.sub("\n" + managed + "\n", text)
else:
    pos = text.rfind("}")
    if pos == -1:
        raise SystemExit("Nginx config has no closing brace")
    text = text[:pos] + "\n" + managed + "\n" + text[pos:]

conf_path.write_text(text)
PY
  if nginx -t >/tmp/papersolver-nginx-test.log 2>&1; then
    nginx -s reload >/dev/null 2>&1 || systemctl reload nginx || true
    echo "Nginx routes updated for ${DOMAIN}"
  else
    cat /tmp/papersolver-nginx-test.log
    cp "$backup" "$BT_NGINX_CONF"
    echo "ERROR: Nginx config test failed. Restored ${backup}."
    exit 1
  fi
}

if grep -Eq "replace-with-new-api-v3-key|change-me|你的APIv3密钥" "$ENV_FILE"; then
  echo "ERROR: ${ENV_FILE} still contains placeholder values."
  echo "Edit it, then run: systemctl restart papersolver"
  exit 1
fi

if ! grep -Eq '^PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_PATH=' "$ENV_FILE"; then
  echo "ERROR: WeChat private key path is missing in ${ENV_FILE}"
  exit 1
fi

PRIVATE_KEY_PATH="$(grep -E '^PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_PATH=' "$ENV_FILE" | tail -n1 | cut -d= -f2-)"
if [ ! -s "$PRIVATE_KEY_PATH" ]; then
  echo "ERROR: WeChat merchant private key not found: ${PRIVATE_KEY_PATH}"
  exit 1
fi

ensure_database() {
  if ! command -v mysql >/dev/null 2>&1; then
    echo "WARNING: mysql client not found. Skip database auto-create."
    return 0
  fi

  local db_host db_port db_name db_user db_pass
  db_host="$(env_value PAPER_DB_HOST)"
  db_port="$(env_value PAPER_DB_PORT)"
  db_name="$(env_value PAPER_DB_NAME)"
  db_user="$(env_value PAPER_DB_USERNAME)"
  db_pass="$(env_value PAPER_DB_PASSWORD)"
  db_host="${db_host:-127.0.0.1}"
  db_port="${db_port:-3306}"
  db_name="${db_name:-paper}"
  db_user="${db_user:-root}"

  echo "Ensuring MySQL database '${db_name}' exists"
  MYSQL_PWD="$db_pass" mysql -h "$db_host" -P "$db_port" -u "$db_user" \
    -e "CREATE DATABASE IF NOT EXISTS \`${db_name}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" \
    >/dev/null
}

ensure_nginx

# ensure_database (已拥有数据库，跳过 root 密码校验)

if [ -f docker-compose.pdfmathtranslate.yml ]; then
  echo "Deploying PDFMathTranslate compose file..."
  cp docker-compose.pdfmathtranslate.yml "$APP_ROOT/docker-compose.pdfmathtranslate.yml"
  if command -v docker >/dev/null 2>&1; then
    echo "Starting PDFMathTranslate service via docker compose..."
    docker compose -f "$APP_ROOT/docker-compose.pdfmathtranslate.yml" up -d || true
  else
    echo "WARNING: docker command not found. Please install Docker to run the translation service."
  fi
fi

systemctl restart papersolver

echo "Waiting for backend on 127.0.0.1:8080 ..."
backend_ready=0
for i in $(seq 1 45); do
  if ! systemctl is-active --quiet papersolver; then
    echo "Backend service is not active."
    dump_backend_diagnostics
    exit 1
  fi
  if curl -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
    backend_ready=1
    break
  fi
  sleep 2
done

if [ "$backend_ready" != "1" ]; then
  echo "ERROR: Backend did not become ready within 90 seconds."
  dump_backend_diagnostics
  exit 1
fi

WECHAT_HEALTH="$(curl -fsS http://127.0.0.1:8080/api/payments/wechat/health || true)"
echo "WeChat health: ${WECHAT_HEALTH}"
if ! printf '%s' "$WECHAT_HEALTH" | grep -q '"ok":true'; then
  echo "ERROR: WeChat Pay health check failed."
  dump_backend_diagnostics
  exit 1
fi

echo
echo "Done."
echo "Useful commands:"
echo "  systemctl status papersolver --no-pager"
echo "  journalctl -u papersolver -n 120 --no-pager"
echo "  tail -n 120 ${LOG_ROOT}/backend.log"
echo "  curl -s http://127.0.0.1:8080/api/health"
