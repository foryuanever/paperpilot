#!/usr/bin/env bash
set -euo pipefail

APP_NAME="papersolver"
WEB_ROOT="/www/wwwroot/${APP_NAME}"
APP_ROOT="/www/server/${APP_NAME}"
BACKEND_ROOT="${APP_ROOT}/backend"
LOG_ROOT="${APP_ROOT}/logs"
ENV_ROOT="/etc/${APP_NAME}"
ENV_FILE="${ENV_ROOT}/papersolver.env"
SERVICE_FILE="/etc/systemd/system/papersolver.service"

cd "$(dirname "$0")"

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: java not found. Install OpenJDK 17 first: apt update && apt install -y openjdk-17-jre-headless"
  exit 1
fi

mkdir -p "$WEB_ROOT" "$BACKEND_ROOT" "$LOG_ROOT" "$ENV_ROOT"

echo "Deploying frontend to ${WEB_ROOT}"
rsync -a frontend/ "$WEB_ROOT"/

echo "Deploying backend jar to ${BACKEND_ROOT}"
cp backend/paperpilot-server-0.0.1-SNAPSHOT.jar "$BACKEND_ROOT"/paperpilot-server-0.0.1-SNAPSHOT.jar

if [ ! -f "$ENV_FILE" ]; then
  echo "Creating ${ENV_FILE}. Fill secrets before first start."
  cp papersolver.env.example "$ENV_FILE"
  chmod 600 "$ENV_FILE"
else
  echo "Keeping existing ${ENV_FILE}"
fi

cp papersolver.service "$SERVICE_FILE"
systemctl daemon-reload
systemctl enable papersolver

if grep -q "replace-with-new-api-v3-key" "$ENV_FILE"; then
  echo "WARNING: ${ENV_FILE} still has placeholder WeChat API v3 key."
  echo "Edit it now, then run: systemctl restart papersolver"
else
  systemctl restart papersolver
fi

echo
echo "Done."
echo "Useful commands:"
echo "  systemctl status papersolver --no-pager"
echo "  journalctl -u papersolver -n 120 --no-pager"
echo "  tail -n 120 ${LOG_ROOT}/backend.log"
echo "  curl -s http://127.0.0.1:8080/api/health"
