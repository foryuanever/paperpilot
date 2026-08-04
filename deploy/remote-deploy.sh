#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE_DIR="${ROOT_DIR}/release/remote"
BT_DIR="${ROOT_DIR}/deploy/bt"
STAMP="$(date +%Y%m%d-%H%M%S)"
PACKAGE_NAME="papersolver-bt-deploy-${STAMP}"
PACKAGE_DIR="${RELEASE_DIR}/${PACKAGE_NAME}"
PACKAGE_TAR="${RELEASE_DIR}/${PACKAGE_NAME}.tar.gz"

SSH_HOST="${PAPERSOLVER_SSH_HOST:-106.53.136.108}"
SSH_USER="${PAPERSOLVER_SSH_USER:-root}"
SSH_PORT="${PAPERSOLVER_SSH_PORT:-22}"
REMOTE_ROOT="${PAPERSOLVER_REMOTE_ROOT:-/www/server/papersolver}"
DOMAIN="${PAPERSOLVER_DOMAIN:-papersolver.cn}"

WECHAT_APP_ID="${PAPERPILOT_WECHAT_PAY_APP_ID:-wxd84d54269bfdf677}"
WECHAT_MCH_ID="${PAPERPILOT_WECHAT_PAY_MCH_ID:-1748953326}"
WECHAT_SERIAL_NO="${PAPERPILOT_WECHAT_PAY_SERIAL_NO:-2204B643BB2B207B0B8DCEA66F1DE8C5CA84D662}"
PUBLIC_BASE_URL="${PAPERPILOT_PUBLIC_BASE_URL:-https://papersolver.cn}"
PRIVATE_KEY_PATH="${PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_PATH:-/www/wwwroot/papersolver/certs/apiclient_key.pem}"
DEFAULT_PRIVATE_KEY_LOCAL_PATH="/Users/yuan/cert/1748953326_20260802_cert/apiclient_key.pem"
PRIVATE_KEY_LOCAL_PATH="${PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_LOCAL_PATH:-}"
if [[ -z "$PRIVATE_KEY_LOCAL_PATH" && -s "$DEFAULT_PRIVATE_KEY_LOCAL_PATH" ]]; then
  PRIVATE_KEY_LOCAL_PATH="$DEFAULT_PRIVATE_KEY_LOCAL_PATH"
fi
NOTIFY_URL="${PAPERPILOT_WECHAT_PAY_NOTIFY_URL:-https://papersolver.cn/api/payments/notify/wechat}"

# 微信支付公钥模式（新商户必须使用，替代平台证书）
PUBLIC_KEY_ID="${PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_ID:-}"
PUBLIC_KEY_REMOTE_PATH="${PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_PATH:-/www/wwwroot/papersolver/certs/wechatpay_public_key.pem}"
DEFAULT_PUBLIC_KEY_LOCAL_PATH="/Users/yuan/cert/1748953326_20260802_cert/wechatpay_public_key.pem"
FALLBACK_PUBLIC_KEY_LOCAL_PATH="/Users/yuan/cert/1748953326_20260802_cert/pub_key.pem"
PUBLIC_KEY_LOCAL_PATH="${PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_LOCAL_PATH:-}"
if [[ -z "$PUBLIC_KEY_LOCAL_PATH" ]]; then
  if [[ -s "$DEFAULT_PUBLIC_KEY_LOCAL_PATH" ]]; then
    PUBLIC_KEY_LOCAL_PATH="$DEFAULT_PUBLIC_KEY_LOCAL_PATH"
  elif [[ -s "$FALLBACK_PUBLIC_KEY_LOCAL_PATH" ]]; then
    PUBLIC_KEY_LOCAL_PATH="$FALLBACK_PUBLIC_KEY_LOCAL_PATH"
  fi
fi

usage() {
  cat <<EOF
Usage:
  PAPERSOLVER_SSH_PASS='******' PAPER_DB_PASSWORD='******' PAPERPILOT_WECHAT_PAY_API_V3_KEY='******' ./deploy/remote-deploy.sh
  PAPERSOLVER_PACKAGE_ONLY=1 PAPERSOLVER_KEEP_REMOTE_DB_PASSWORD=1 PAPERPILOT_WECHAT_PAY_API_V3_KEY='******' ./deploy/remote-deploy.sh

Optional env:
  PAPERSOLVER_SSH_HOST=${SSH_HOST}
  PAPERSOLVER_SSH_USER=${SSH_USER}
  PAPERSOLVER_SSH_PORT=${SSH_PORT}
  PAPERSOLVER_DOMAIN=${DOMAIN}
  PAPERPILOT_WECHAT_PAY_APP_ID=${WECHAT_APP_ID}
  PAPERPILOT_WECHAT_PAY_MCH_ID=${WECHAT_MCH_ID}
  PAPERPILOT_WECHAT_PAY_SERIAL_NO=${WECHAT_SERIAL_NO}
  PAPERPILOT_PUBLIC_BASE_URL=${PUBLIC_BASE_URL}
  PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_LOCAL_PATH=/local/path/to/apiclient_key.pem
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "ERROR: $1 not found."
    exit 1
  fi
}

read_secret() {
  local var_name="$1"
  local prompt="$2"
  local value="${!var_name:-}"
  if [[ -z "$value" ]]; then
    printf "%s" "$prompt" >&2
    stty -echo
    IFS= read -r value
    stty echo
    printf "\n" >&2
  fi
  if [[ -z "$value" ]]; then
    echo "ERROR: ${var_name} is required." >&2
    exit 1
  fi
  printf "%s" "$value"
}

ssh_cmd() {
  if [[ -n "${PAPERSOLVER_SSH_PASS:-}" ]] && command -v sshpass >/dev/null 2>&1; then
    sshpass -p "$PAPERSOLVER_SSH_PASS" ssh -p "$SSH_PORT" -o StrictHostKeyChecking=accept-new "$SSH_USER@$SSH_HOST" "$@"
  else
    ssh -p "$SSH_PORT" -o StrictHostKeyChecking=accept-new "$SSH_USER@$SSH_HOST" "$@"
  fi
}

scp_cmd() {
  if [[ -n "${PAPERSOLVER_SSH_PASS:-}" ]] && command -v sshpass >/dev/null 2>&1; then
    sshpass -p "$PAPERSOLVER_SSH_PASS" scp -P "$SSH_PORT" -o StrictHostKeyChecking=accept-new "$@"
  else
    scp -P "$SSH_PORT" -o StrictHostKeyChecking=accept-new "$@"
  fi
}

need_cmd npm
need_cmd tar

if [[ "${PAPERSOLVER_KEEP_REMOTE_ENV:-}" != "1" && "${PAPERSOLVER_KEEP_REMOTE_ENV:-}" != "true" ]]; then
  if [[ "${PAPERSOLVER_KEEP_REMOTE_DB_PASSWORD:-}" == "1" || "${PAPERSOLVER_KEEP_REMOTE_DB_PASSWORD:-}" == "true" ]]; then
    DB_PASSWORD="__KEEP_EXISTING__"
  else
    # 允许空密码：如果环境变量中没有，交互式提示用户输入，用户可以直接回车代表空密码
    DB_PASSWORD="${PAPER_DB_PASSWORD:-}"
    if [[ -z "${DB_PASSWORD+x}" || -z "$DB_PASSWORD" ]]; then
      printf "MySQL root password (留空请直接回车): " >&2
      stty -echo
      IFS= read -r DB_PASSWORD || true
      stty echo
      printf "\n" >&2
    fi
  fi
  WECHAT_API_V3_KEY="$(read_secret PAPERPILOT_WECHAT_PAY_API_V3_KEY "WeChat Pay API v3 key: ")"
fi

echo "Building frontend..."
cd "$ROOT_DIR/front"
npm install
npm run build

echo "Building backend..."
cd "$ROOT_DIR/backend"
if [[ -x ./mvnw ]]; then
  ./mvnw -q -DskipTests package
else
  need_cmd mvn
  mvn -q -DskipTests package
fi

rm -rf "$PACKAGE_DIR"
mkdir -p "$PACKAGE_DIR/frontend" "$PACKAGE_DIR/backend"
cp -a "$ROOT_DIR/front/dist/." "$PACKAGE_DIR/frontend/"
cp "$ROOT_DIR/backend/target/paperpilot-server-0.0.1-SNAPSHOT.jar" "$PACKAGE_DIR/backend/"
cp "$BT_DIR/install-on-bt.sh" "$PACKAGE_DIR/"
cp "$BT_DIR/papersolver.service" "$PACKAGE_DIR/"
cp "$BT_DIR/nginx-api-location.conf" "$PACKAGE_DIR/"
cp "$BT_DIR/papersolver.env.example" "$PACKAGE_DIR/"


if [[ -n "$PRIVATE_KEY_LOCAL_PATH" ]]; then
  if [[ ! -s "$PRIVATE_KEY_LOCAL_PATH" ]]; then
    echo "ERROR: local WeChat private key not found: ${PRIVATE_KEY_LOCAL_PATH}"
    exit 1
  fi
  mkdir -p "$PACKAGE_DIR/certs"
  install -m 600 "$PRIVATE_KEY_LOCAL_PATH" "$PACKAGE_DIR/certs/apiclient_key.pem"
else
  echo "NOTE: no PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_LOCAL_PATH provided."
  echo "      Remote must already have: ${PRIVATE_KEY_PATH}"
fi

# 打包微信支付公钥（新公钥模式）
if [[ -n "$PUBLIC_KEY_LOCAL_PATH" ]]; then
  if [[ ! -s "$PUBLIC_KEY_LOCAL_PATH" ]]; then
    echo "ERROR: local WeChat Pay public key not found: ${PUBLIC_KEY_LOCAL_PATH}"
    exit 1
  fi
  mkdir -p "$PACKAGE_DIR/certs"
  install -m 644 "$PUBLIC_KEY_LOCAL_PATH" "$PACKAGE_DIR/certs/wechatpay_public_key.pem"
  echo "NOTE: WeChat Pay public key will be uploaded (公钥模式)"
elif [[ -n "$PUBLIC_KEY_ID" ]]; then
  echo "NOTE: PUBLIC_KEY_ID set but no local public key file found."
  echo "      Remote must already have: ${PUBLIC_KEY_REMOTE_PATH}"
fi

if [[ "${PAPERSOLVER_KEEP_REMOTE_ENV:-}" != "1" && "${PAPERSOLVER_KEEP_REMOTE_ENV:-}" != "true" ]]; then
  cat > "$PACKAGE_DIR/papersolver.env" <<EOF
SERVER_PORT=8080

PAPER_DB_HOST=${PAPER_DB_HOST:-127.0.0.1}
PAPER_DB_PORT=${PAPER_DB_PORT:-3306}
PAPER_DB_NAME=${PAPER_DB_NAME:-paper}
PAPER_DB_USERNAME=${PAPER_DB_USERNAME:-root}
PAPER_DB_PASSWORD=${DB_PASSWORD}

PAPERPILOT_PUBLIC_BASE_URL=${PUBLIC_BASE_URL}
PAPERPILOT_WECHAT_PAY_APP_ID=${WECHAT_APP_ID}
PAPERPILOT_WECHAT_PAY_MCH_ID=${WECHAT_MCH_ID}
PAPERPILOT_WECHAT_PAY_SERIAL_NO=${WECHAT_SERIAL_NO}
PAPERPILOT_WECHAT_PAY_API_V3_KEY=${WECHAT_API_V3_KEY}
PAPERPILOT_WECHAT_PAY_PRIVATE_KEY_PATH=${PRIVATE_KEY_PATH}
PAPERPILOT_WECHAT_PAY_NOTIFY_URL=${NOTIFY_URL}
PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_ID=${PUBLIC_KEY_ID}
PAPERPILOT_WECHAT_PAY_PUBLIC_KEY_PATH=${PUBLIC_KEY_REMOTE_PATH}

BAIDU_APP_ID=${BAIDU_APP_ID:-}
BAIDU_SECRET=${BAIDU_SECRET:-}
MICROSOFT_TRANSLATOR_KEY=${MICROSOFT_TRANSLATOR_KEY:-}
MICROSOFT_TRANSLATOR_REGION=${MICROSOFT_TRANSLATOR_REGION:-eastasia}
TENCENT_TRANSLATE_SECRET_ID=${TENCENT_TRANSLATE_SECRET_ID:-}
TENCENT_TRANSLATE_SECRET_KEY=${TENCENT_TRANSLATE_SECRET_KEY:-}
TENCENT_TRANSLATE_REGION=${TENCENT_TRANSLATE_REGION:-ap-guangzhou}

PDFMATH_TRANSLATE_URL=${PDFMATH_TRANSLATE_URL:-http://127.0.0.1:11008}
MINERU_BINARY=${MINERU_BINARY:-/www/server/papersolver/.mineru-venv/bin/mineru}
MINERU_MODEL_SOURCE=${MINERU_MODEL_SOURCE:-modelscope}

PPT_MASTER_SKILL_DIR=${PPT_MASTER_SKILL_DIR:-/www/server/papersolver/ppt-master}
PPT_MASTER_PYTHON=${PPT_MASTER_PYTHON:-}
PPT_MASTER_CODEX=${PPT_MASTER_CODEX:-}
PPT_MASTER_AGENT_TIMEOUT_MINUTES=${PPT_MASTER_AGENT_TIMEOUT_MINUTES:-120}
EOF
  chmod 600 "$PACKAGE_DIR/papersolver.env"
else
  echo "Skipped packaging papersolver.env. Existing remote config will be preserved."
fi

mkdir -p "$RELEASE_DIR"
COPYFILE_DISABLE=1 tar -C "$RELEASE_DIR" -czf "$PACKAGE_TAR" "$PACKAGE_NAME"

echo "Uploading package to ${SSH_USER}@${SSH_HOST}:${REMOTE_ROOT}/"
if [[ "${PAPERSOLVER_PACKAGE_ONLY:-}" == "1" || "${PAPERSOLVER_PACKAGE_ONLY:-}" == "true" ]]; then
  echo "Package only mode. Skipped upload."
  echo "Package: ${PACKAGE_TAR}"
  exit 0
fi
ssh_cmd "mkdir -p '$REMOTE_ROOT'"
scp_cmd "$PACKAGE_TAR" "$SSH_USER@$SSH_HOST:$REMOTE_ROOT/"

echo "Running remote install..."
ssh_cmd "cd '$REMOTE_ROOT' && tar --warning=no-unknown-keyword -xzf '$(basename "$PACKAGE_TAR")' && PAPERSOLVER_DOMAIN='$DOMAIN' bash '$PACKAGE_NAME/install-on-bt.sh'"

echo
echo "Deployment finished."
echo "Frontend: https://${DOMAIN}/"
echo "Health:   https://${DOMAIN}/api/health"
