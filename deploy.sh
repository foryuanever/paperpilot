#!/bin/bash
set -euo pipefail

REPO_ROOT="/Users/yuan/Desktop/Solve Paper"
REMOTE="root@106.53.136.108"
MUX="/tmp/ssh_mux"
SITE_ROOT="/www/wwwroot/papersolver"
BACKEND_REMOTE_DIR="/www/wwwroot"
BACKEND_REMOTE_JAR="$BACKEND_REMOTE_DIR/paperpilot-server-0.0.1-SNAPSHOT.jar"
DESKTOP_VERSION="$(node -p "require('$REPO_ROOT/desktop/package.json').version")"
EXPECTED_BACKEND_BUILD="${PAPER_SOLVER_EXPECTED_BACKEND_BUILD:-$DESKTOP_VERSION}"
MAC_DMG="$REPO_ROOT/desktop/release/PaperSolver-${DESKTOP_VERSION}-arm64.dmg"
MAC_DMG_BLOCKMAP="$MAC_DMG.blockmap"
MAC_ZIP="$REPO_ROOT/desktop/release/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip"
MAC_ZIP_BLOCKMAP="$MAC_ZIP.blockmap"
WINDOWS_INSTALLER="$REPO_ROOT/desktop/release/PaperSolver Setup ${DESKTOP_VERSION}.exe"
WINDOWS_INSTALLER_BLOCKMAP="$WINDOWS_INSTALLER.blockmap"
FRONT_DIST="$REPO_ROOT/desktop/front-dist"
BACKEND_JAR="$REPO_ROOT/backend/target/paperpilot-server-0.0.1-SNAPSHOT.jar"
DEPENDENCY_MANIFEST="$REPO_ROOT/desktop/scratch/manifest/dependency-manifest.json"
UPDATE_MANIFEST="$REPO_ROOT/desktop/scratch/manifest/update-manifest.json"
AUTO_UPDATE_WIN_MANIFEST="$REPO_ROOT/desktop/release/latest.yml"
AUTO_UPDATE_MAC_MANIFEST="$REPO_ROOT/desktop/release/latest-mac.yml"
CAPTURE_SOURCE="$REPO_ROOT/browser-extension/papersolver-capture"
CAPTURE_RELEASE_DIR="$REPO_ROOT/desktop/scratch/capture-release"
CAPTURE_CHROME_ZIP="$CAPTURE_RELEASE_DIR/papersolver-capture-chrome.zip"
CAPTURE_EDGE_ZIP="$CAPTURE_RELEASE_DIR/papersolver-capture-edge.zip"
MS_REPO="${PAPER_SOLVER_MODELSCOPE_REPO:-foryuanever/papersolver-dependencies}"
# A full release must publish the runtime that the new desktop build expects.
# Set PAPER_SOLVER_UPLOAD_DEPENDENCIES=0 only for a web/backend-only rehearsal.
# Use "windows" when only the Windows runtime package changed.
UPLOAD_DEPENDENCIES="${PAPER_SOLVER_UPLOAD_DEPENDENCIES:-1}"
UPLOAD_MAC_DEPENDENCY="1"
UPLOAD_WIN_DEPENDENCY="1"
if [ "$UPLOAD_DEPENDENCIES" = "windows" ]; then
  UPLOAD_MAC_DEPENDENCY="0"
  UPLOAD_WIN_DEPENDENCY="1"
fi
USE_PREBUILT="${PAPER_SOLVER_USE_PREBUILT:-0}"
RELEASE_NOTES="${PAPER_SOLVER_RELEASE_NOTES:-修复与体验优化。}"
UV_BIN="${UV_BIN:-$(command -v uv || true)}"
MAC_DEPENDENCY="$REPO_ROOT/desktop/scratch/offline-release/papersolver-local-dependency-macos-arm64.zip"
WIN_DEPENDENCY="$REPO_ROOT/desktop/scratch/windows-cross/offline-release/papersolver-local-dependency-windows-x64.zip"
VERSIONED_MAC_DEPENDENCY="$REPO_ROOT/desktop/scratch/offline-release/papersolver-local-dependency-macos-arm64-${DESKTOP_VERSION}.zip"
VERSIONED_WIN_DEPENDENCY="$REPO_ROOT/desktop/scratch/windows-cross/offline-release/papersolver-local-dependency-windows-x64-${DESKTOP_VERSION}.zip"
NGINX_CONF="$REPO_ROOT/deploy/bt/papersolver.conf"

quick_deploy() {
  local front_tar="$REPO_ROOT/release/quick/papersolver-front-dist.tar.gz"
  local mux="/tmp/papersolver_quick_deploy_$$.sock"

  cleanup_quick_deploy() {
    # The EXIT trap may run after the function-local mux variable has gone out
    # of scope while nounset is enabled. Treat a missing socket as already
    # cleaned up instead of turning a successful deployment into exit 1.
    if [ -n "${mux:-}" ]; then
      ssh -O exit -S "$mux" "$REMOTE" >/dev/null 2>&1 || true
    fi
  }
  trap cleanup_quick_deploy EXIT

  echo "========================================="
  echo "PaperSolver 快捷部署：前端 + 后端"
  echo "========================================="

  echo "1/5 构建后端 JAR..."
  mvn -q -DskipTests package -f "$REPO_ROOT/backend/pom.xml"

  echo "2/5 构建网站前端并同步桌面端 front-dist..."
  npm --prefix "$REPO_ROOT/desktop" run build:front

  echo "3/5 打包前端 dist..."
  mkdir -p "$(dirname "$front_tar")"
  tar -C "$REPO_ROOT/front/dist" -czf "$front_tar" .

  echo "4/5 建立 SSH 复用连接。若出现微信安全登录，请扫码确认一次..."
  # Run a normal foreground command for the first connection so Tencent Cloud's
  # WeChat keyboard-interactive verification can finish before multiplexing.
  ssh -M -S "$mux" -o ControlPath="$mux" -o ControlPersist=15m -o IdentitiesOnly=yes "$REMOTE" "true"

  echo "5/5 上传并重启线上服务..."
  ssh -o ControlPath="$mux" "$REMOTE" "mkdir -p '$SITE_ROOT' /www/wwwroot"

  scp -o ControlPath="$mux" "$front_tar" "$REMOTE:$SITE_ROOT/papersolver-front-dist.tar.gz.uploading"
  ssh -o ControlPath="$mux" "$REMOTE" "\
    mv '$SITE_ROOT/papersolver-front-dist.tar.gz.uploading' '$SITE_ROOT/papersolver-front-dist.tar.gz' && \
    tar -xzf '$SITE_ROOT/papersolver-front-dist.tar.gz' -C '$SITE_ROOT'"

  echo "  -> 打包并极速同步官方 PPT Master 运行时..."
  local runtime_tar="$REPO_ROOT/release/quick/ppt-master-runtime.tar.gz"
  mkdir -p "$(dirname "$runtime_tar")"
  tar -C "$REPO_ROOT/backend" -czf "$runtime_tar" ppt-master-runtime
  scp -o ControlPath="$mux" "$runtime_tar" "$REMOTE:/www/wwwroot/ppt-master-runtime.tar.gz"
  ssh -o ControlPath="$mux" "$REMOTE" "tar -xzf /www/wwwroot/ppt-master-runtime.tar.gz -C /www/wwwroot/ && rm -f /www/wwwroot/ppt-master-runtime.tar.gz"

  echo "  -> 检查并确保服务器 Codex CLI、Node.js 与 Python 运行环境就绪..."
  ssh -o ControlPath="$mux" "$REMOTE" "\
    if ! command -v node >/dev/null 2>&1; then \
      if command -v nodejs >/dev/null 2>&1; then \
        ln -sf \"\$(command -v nodejs)\" /usr/local/bin/node; \
      else \
        echo '     正在服务器安装 Node.js 基础运行环境...' && \
        (apt-get update -qq && apt-get install -y -qq nodejs npm || true) && \
        ([ -f /usr/bin/nodejs ] && ln -sf /usr/bin/nodejs /usr/local/bin/node || true); \
      fi; \
    fi; \
    if [ ! -x /www/wwwroot/.ppt-master-venv/bin/python ]; then \
      python3 -m venv /www/wwwroot/.ppt-master-venv 2>/dev/null || (apt-get update -qq && apt-get install -y -qq python3-venv && python3 -m venv /www/wwwroot/.ppt-master-venv); \
    fi; \
    /www/wwwroot/.ppt-master-venv/bin/pip install -q -r /www/wwwroot/ppt-master-runtime/requirements.txt; \
    if ! command -v codex >/dev/null 2>&1; then \
      echo '     正在服务器安装官方 Codex CLI...' && npm install -g @openai/codex@0.147.0; \
    fi; \
    codex --version; \
    mkdir -p /www/wwwroot/.papersolver-codex"

  echo "  -> 上传最新后端服务包..."
  scp -o ControlPath="$mux" "$BACKEND_JAR" "$REMOTE:$BACKEND_REMOTE_JAR.uploading"
  local local_backend_sha
  local remote_backend_sha
  local_backend_sha="$(openssl dgst -sha256 "$BACKEND_JAR" | awk '{print $NF}')"
  remote_backend_sha="$(ssh -o ControlPath="$mux" "$REMOTE" "sha256sum '$BACKEND_REMOTE_JAR.uploading' | cut -d ' ' -f1")"
  if [ "$local_backend_sha" != "$remote_backend_sha" ]; then
    echo "❌ 后端 JAR 上传校验失败：本地 $local_backend_sha，服务器 $remote_backend_sha"
    exit 1
  fi
  echo "  ✅ 后端 JAR SHA256 校验通过：$remote_backend_sha"
  ssh -o ControlPath="$mux" "$REMOTE" "\
    mv '$BACKEND_REMOTE_JAR.uploading' '$BACKEND_REMOTE_JAR' && \
    mkdir -p /etc/papersolver && \
    touch /etc/papersolver/papersolver.env && \
    if ! grep -q '^PAPERPILOT_SESSION_SECRET=.' /etc/papersolver/papersolver.env; then \
      printf '\nPAPERPILOT_SESSION_SECRET=%s\n' \"\$(openssl rand -hex 32)\" >> /etc/papersolver/papersolver.env; \
    fi; \
    set_env() { key=\"\$1\"; value=\"\$2\"; if grep -q \"^\${key}=\" /etc/papersolver/papersolver.env; then sed -i \"s|^\${key}=.*|\${key}=\${value}|\" /etc/papersolver/papersolver.env; else printf '%s=%s\\n' \"\$key\" \"\$value\" >> /etc/papersolver/papersolver.env; fi; }; \
    set_env PPT_MASTER_SKILL_DIR /www/wwwroot/ppt-master-runtime; \
    set_env PPT_MASTER_PYTHON /www/wwwroot/.ppt-master-venv/bin/python; \
    set_env PPT_MASTER_CODEX \"\$(command -v codex)\"; \
    set_env PPT_MASTER_CODEX_HOME /www/wwwroot/.papersolver-codex; \
    set_env PPT_MASTER_AGENT_TIMEOUT_MINUTES 120; \
    set_env PAPERPILOT_DEMO_SEED false; \
    set_env PAPERPILOT_PRIVILEGED_MACHINE_IDS adc9d51ef1b4aa4d52eedcfee6132db46f998a9b8343af87abb6ee008a4ce98c; \
    chmod 600 /etc/papersolver/papersolver.env"

  echo "  -> 通过 systemd 重启后端服务..."
  ssh -o ControlPath="$mux" "$REMOTE" "systemctl stop papersolver || true; pkill -f '[p]aperpilot-server-0.0.1-SNAPSHOT.jar' 2>/dev/null || true; sleep 2; systemctl start papersolver && systemctl is-active --quiet papersolver"

  echo "等待后端启动并确认构建版本..."
  local health=""
  local verified=0
  for _ in {1..20}; do
    health="$(curl -fsS "https://papersolver.cn/api/health?ts=$(date +%s)" 2>/dev/null || true)"
    if [[ "$health" == *"$EXPECTED_BACKEND_BUILD"* ]]; then
      verified=1
      break
    fi
    sleep 2
  done
  if [ "$verified" != "1" ]; then
    echo "❌ 后端进程版本校验失败，线上健康接口未返回 $EXPECTED_BACKEND_BUILD"
    echo "实际响应：$health"
    exit 1
  fi
  echo "✅ 线上后端已确认：$health"

  echo "远程后端状态："
  ssh -o ControlPath="$mux" "$REMOTE" "\
    echo '--- PPT Master preflight ---'; \
    codex --version; \
    test -f /www/wwwroot/ppt-master-runtime/SKILL.md && echo 'PPT Master skill: OK'; \
    /www/wwwroot/.ppt-master-venv/bin/python -c 'import fitz, pptx, svglib, reportlab; print(\"PPT Master Python: OK\")'; \
    grep -E '^(PPT_MASTER_SKILL_DIR|PPT_MASTER_PYTHON|PPT_MASTER_CODEX|PPT_MASTER_CODEX_HOME)=' /etc/papersolver/papersolver.env; \
    ls -lh '$BACKEND_REMOTE_JAR'; \
    pgrep -af 'paperpilot-server-0.0.1-SNAPSHOT.jar' || true; \
    echo '--- backend.log tail ---'; \
    tail -40 /www/wwwroot/backend.log || true"

  echo "========================================="
  echo "快捷部署完成：https://papersolver.cn"
  echo "桌面端本地资源也已更新：请完全退出并重新打开 PaperSolver 桌面端。"
  echo "========================================="
}

case "${1:-quick}" in
  quick|web|hotfix)
    quick_deploy
    exit 0
    ;;
  full)
    shift
    ;;
  -h|--help)
    echo "Usage:"
    echo "  ./deploy.sh quick   # 只部署网站前端和后端，默认"
    echo "  ./deploy.sh full    # 完整发布：桌面安装包、插件、依赖清单、前端、后端"
    echo "  PAPER_SOLVER_RELEASE_NOTES='更新说明' ./deploy.sh full"
    exit 0
    ;;
esac

echo "========================================="
echo "🚀 开始一键部署 PaperSolver 到远程服务器"
echo "========================================="

if [ "$USE_PREBUILT" = "1" ] || [ "$USE_PREBUILT" = "true" ]; then
  echo "📦 复用已校验的预构建发布物，不重新构建安装包。"
else
  echo "🔨 正在构建后端 JAR..."
  mvn -q -DskipTests package -f "$REPO_ROOT/backend/pom.xml"
  echo "🎨 正在构建桌面端前端静态资源..."
  npm --prefix "$REPO_ROOT/desktop" run build:front
  echo "🧩 正在打包浏览器插件..."
  mkdir -p "$CAPTURE_RELEASE_DIR"
  (cd "$CAPTURE_SOURCE" && /usr/bin/zip -q -FSr "$CAPTURE_CHROME_ZIP" .)
  cp "$CAPTURE_CHROME_ZIP" "$CAPTURE_EDGE_ZIP"
  echo "🖥️ 正在重新生成桌面安装包..."
  npm --prefix "$REPO_ROOT/desktop" run dist:mac
  npm --prefix "$REPO_ROOT/desktop" run dist:win
fi

package_dependency_if_incomplete() {
  local source="$1"
  local output="$2"
  local platform="$3"
  local arch="$4"
  local archive="$output/papersolver-local-dependency-$platform-$arch.zip"
  local required_entry="$5"
  local required_entries=(
    "$required_entry"
    "papersolver-dependency.json"
  )
  if [ "$platform" = "macos" ]; then
    required_entries+=(
      ".runtime-venv/lib/python3.12/encodings/__init__.py"
      ".runtime-venv/lib/python3.12/os.py"
    )
  else
    required_entries+=(
      ".runtime-venv/Lib/encodings/__init__.py"
      ".runtime-venv/Lib/os.py"
    )
  fi
  local package_complete=1
  if [ ! -f "$archive" ]; then
    package_complete=0
  else
    for required in "${required_entries[@]}"; do
      if ! unzip -Z1 "$archive" 2>/dev/null | grep -Fqx "$required"; then
        package_complete=0
        break
      fi
    done
  fi
  if [ "${PAPER_SOLVER_REBUILD_DEPENDENCIES:-0}" = "1" ] || [ "$package_complete" != "1" ]; then
    echo "🧩 正在重新打包完整 $platform-$arch 本机依赖（含结构化解析运行时）..."
    PAPER_SOLVER_DEPENDENCY_SOURCE="$source" \
    PAPER_SOLVER_DEPENDENCY_OUTPUT="$output" \
    PAPER_SOLVER_DEPENDENCY_PLATFORM="$platform" \
    PAPER_SOLVER_DEPENDENCY_ARCH="$arch" \
      node "$REPO_ROOT/desktop/scripts/package-local-dependency.mjs"
  else
    echo "✅ 已复用完整 $platform-$arch 本机依赖包"
  fi
}

package_dependency_if_incomplete \
  "$REPO_ROOT/desktop/scratch/offline-dependency-source" \
  "$REPO_ROOT/desktop/scratch/offline-release" \
  "macos" "arm64" \
  ".runtime-venv/lib/python3.12/site-packages/torch/__init__.py"
package_dependency_if_incomplete \
  "$REPO_ROOT/desktop/scratch/windows-cross/offline-dependency-source-windows-x64" \
  "$REPO_ROOT/desktop/scratch/windows-cross/offline-release" \
  "windows" "x64" \
  ".runtime-venv/Lib/site-packages/torch/__init__.py"

for dependency in "$MAC_DEPENDENCY" "$WIN_DEPENDENCY"; do
  if [ ! -f "$dependency" ]; then
    echo "❌ 缺少精简依赖包：$dependency"
    exit 1
  fi
done
if [ "$UPLOAD_DEPENDENCIES" != "0" ]; then
  cp -f "$MAC_DEPENDENCY" "$VERSIONED_MAC_DEPENDENCY"
  cp -f "$WIN_DEPENDENCY" "$VERSIONED_WIN_DEPENDENCY"
fi
if [ "$UPLOAD_DEPENDENCIES" != "0" ] && [ -z "$UV_BIN" ]; then
  echo "❌ 未找到 uv。请先安装 uv，或设置 UV_BIN=/path/to/uv 后重试。"
  exit 1
fi

echo "📋 正在生成桌面客户端更新清单..."
python3 - "$UPDATE_MANIFEST" "$DESKTOP_VERSION" "$RELEASE_NOTES" <<'PY'
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
version = sys.argv[2]
release_notes = sys.argv[3]
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps({
    "version": version,
    "channel": "latest",
    "releaseNotes": release_notes,
    "platforms": {
        "win32-x64": {"downloadUrl": f"https://papersolver.cn/downloads/PaperSolver-{version}.exe"},
        "darwin-arm64": {"downloadUrl": f"https://papersolver.cn/downloads/PaperSolver-{version}-arm64.dmg"}
    }
}, ensure_ascii=False, indent=2) + "\n")
PY

echo "📋 正在生成依赖清单..."
if [ "$UPLOAD_DEPENDENCIES" != "0" ]; then
python3 - "$DEPENDENCY_MANIFEST" "$VERSIONED_MAC_DEPENDENCY" "$VERSIONED_WIN_DEPENDENCY" "$DESKTOP_VERSION" <<'PY'
import hashlib, json, pathlib, sys
manifest, mac, win = map(pathlib.Path, sys.argv[1:4])
desktop_version = sys.argv[4]
def item(path, platform, package_id):
    digest, size = hashlib.sha256(), 0
    with path.open('rb') as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk); size += len(chunk)
    return {"id": package_id, "platforms": [platform], "size": size, "weight": size,
            "sha256": digest.hexdigest(), "urls": [
            f"https://modelscope.cn/datasets/foryuanever/papersolver-dependencies/resolve/master/{path.name}"
            ]}
manifest.parent.mkdir(parents=True, exist_ok=True)
manifest.write_text(json.dumps({"version": f"{desktop_version}-offline-full-pipeline", "packages": [
    item(mac, "macos-arm64", "runtime-full"), item(win, "windows-x64", "runtime-full")
]}, ensure_ascii=False, indent=2) + "\n")
PY
else
echo "📋 依赖包未变化：保留现有依赖清单，不重写版本或 SHA256。"
fi

upload_modelscope() {
  local file="$1"
  echo "📤 上传 ModelScope：$(basename "$file")"
  "$UV_BIN" tool run --from modelscope-hub ms-hub upload "$MS_REPO" "$file" "$(basename "$file")" \
    --repo-type dataset --commit-message "PaperSolver ${DESKTOP_VERSION} compact dependency" --max-workers 1 --use-cache
}
for required in "$MAC_DMG" "$MAC_DMG_BLOCKMAP" "$MAC_ZIP" "$MAC_ZIP_BLOCKMAP" "$WINDOWS_INSTALLER" "$WINDOWS_INSTALLER_BLOCKMAP" "$FRONT_DIST" "$BACKEND_JAR" "$NGINX_CONF" "$UPDATE_MANIFEST" "$AUTO_UPDATE_WIN_MANIFEST" "$AUTO_UPDATE_MAC_MANIFEST" "$CAPTURE_CHROME_ZIP" "$CAPTURE_EDGE_ZIP"; do
  if [ ! -e "$required" ]; then
    echo "❌ 缺少部署文件：$required"
    exit 1
  fi
done

# Publish the large runtime archives before exposing their new checksums in the
# public manifest. Otherwise a transient ModelScope failure leaves clients with
# a manifest that points to an unavailable dependency package.
if [ "$UPLOAD_DEPENDENCIES" != "0" ]; then
  if [ "$UPLOAD_MAC_DEPENDENCY" = "1" ]; then
    upload_modelscope "$VERSIONED_MAC_DEPENDENCY"
  else
    echo "⏭️ 跳过 macOS 依赖包上传（本次仅更新 Windows）"
  fi
  upload_modelscope "$VERSIONED_WIN_DEPENDENCY"
else
  echo "⏭️ 跳过 ModelScope 依赖包上传（仅发布客户端/网站/后端）。"
fi

# 1. 建立临时安全通道（只需在此步骤扫码并输入密码一次）
echo "🔑 正在建立 SSH 连接通道，请按提示完成登录："
ssh -M -S "$MUX" -o ControlPath="$MUX" -o ControlPersist=15m -o IdentitiesOnly=yes "$REMOTE" "true"

# 确认通道是否成功建立
if [ ! -S "$MUX" ]; then
    echo "❌ SSH 通道建立失败，部署终止。"
    exit 1
fi

# 2. 创建服务器上的站点根目录
ssh -o ControlPath="$MUX" "$REMOTE" "mkdir -p '$SITE_ROOT/downloads/dependencies'"

# 3. 部署前端静态网页文件 (等同于 scp -r dist/*)
echo "🧹 正在清理服务器旧版静态资源缓存..."
ssh -o ControlPath="$MUX" "$REMOTE" "rm -rf '$SITE_ROOT/assets/'*"

echo "📤 正在上传前端静态网页..."
scp -o ControlPath="$MUX" -r "$FRONT_DIST/"* "$REMOTE:$SITE_ROOT/"

# 4. 保留宝塔现有的唯一站点配置，只做语法检查和重载。
# 不再新增 papersolver.conf，避免与 papersolver.cn.conf 重复声明同一域名。
echo "⚙️ 正在校验并重载现有 Nginx 站点规则..."
ssh -o ControlPath="$MUX" "$REMOTE" "set -e; nginx_output=\$(nginx -T 2>&1); printf '%s\\n' \"\$nginx_output\"; if printf '%s\\n' \"\$nginx_output\" | grep -q 'conflicting server name.*papersolver\\.cn'; then echo '❌ 检测到 papersolver.cn 重复 Nginx 站点配置，请先移除旧的重复配置后再发布。' >&2; exit 1; fi; nginx -s reload || /etc/init.d/nginx reload"

# 4.5 上传桌面端安装包与本地依赖引导程序
echo "💾 正在上传桌面客户端安装包与依赖引导程序..."
echo "📤 正在上传 macOS 安装包..."
scp -o ControlPath="$MUX" "$MAC_DMG" "$REMOTE:$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg.uploading' '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg'"
scp -o ControlPath="$MUX" "$MAC_DMG_BLOCKMAP" "$REMOTE:$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg.blockmap.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg.blockmap.uploading' '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg.blockmap'"
scp -o ControlPath="$MUX" "$MAC_ZIP" "$REMOTE:$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip.uploading' '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip'"
scp -o ControlPath="$MUX" "$MAC_ZIP_BLOCKMAP" "$REMOTE:$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip.blockmap.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip.blockmap.uploading' '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip.blockmap'"
ssh -o ControlPath="$MUX" "$REMOTE" "cp '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg' '$SITE_ROOT/downloads/PaperSolver-arm64.dmg.uploading' && mv '$SITE_ROOT/downloads/PaperSolver-arm64.dmg.uploading' '$SITE_ROOT/downloads/PaperSolver-arm64.dmg'"
echo "  ✅ PaperSolver ${DESKTOP_VERSION} macOS 安装包上传完成"

echo "📤 正在上传 Windows 安装包..."
scp -o ControlPath="$MUX" "$WINDOWS_INSTALLER" "$REMOTE:$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe.uploading' '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe'"
scp -o ControlPath="$MUX" "$WINDOWS_INSTALLER_BLOCKMAP" "$REMOTE:$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe.blockmap.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe.blockmap.uploading' '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe.blockmap'"
ssh -o ControlPath="$MUX" "$REMOTE" "cp '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe' '$SITE_ROOT/downloads/PaperSolver Setup ${DESKTOP_VERSION}.exe.uploading' && mv '$SITE_ROOT/downloads/PaperSolver Setup ${DESKTOP_VERSION}.exe.uploading' '$SITE_ROOT/downloads/PaperSolver Setup ${DESKTOP_VERSION}.exe'"
ssh -o ControlPath="$MUX" "$REMOTE" "cp '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe.blockmap' '$SITE_ROOT/downloads/PaperSolver Setup ${DESKTOP_VERSION}.exe.blockmap.uploading' && mv '$SITE_ROOT/downloads/PaperSolver Setup ${DESKTOP_VERSION}.exe.blockmap.uploading' '$SITE_ROOT/downloads/PaperSolver Setup ${DESKTOP_VERSION}.exe.blockmap'"
ssh -o ControlPath="$MUX" "$REMOTE" "cp '$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe' '$SITE_ROOT/downloads/PaperSolver.exe.uploading' && mv '$SITE_ROOT/downloads/PaperSolver.exe.uploading' '$SITE_ROOT/downloads/PaperSolver.exe'"
echo "  ✅ PaperSolver ${DESKTOP_VERSION} Windows 安装包上传完成"

# Every public artifact is verified after the atomic rename. This prevents a
# stale CDN/server file from being mistaken for the newly uploaded release.
verify_uploaded_sha256() {
  local local_file="$1"
  local remote_file="$2"
  local label="$3"
  local local_sha remote_sha
  local_sha="$(openssl dgst -sha256 "$local_file" | awk '{print $NF}')"
  remote_sha="$(ssh -o ControlPath="$MUX" "$REMOTE" "sha256sum '$remote_file' | awk '{print \$1}'")"
  if [ -z "$remote_sha" ] || [ "$local_sha" != "$remote_sha" ]; then
    echo "❌ $label SHA256 校验失败：本地 $local_sha，线上 $remote_sha"
    exit 1
  fi
  echo "  ✅ $label SHA256 一致：$remote_sha"
}

verify_uploaded_sha256 "$MAC_DMG" "$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64.dmg" "macOS DMG"
verify_uploaded_sha256 "$MAC_ZIP" "$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}-arm64-mac.zip" "macOS ZIP"
verify_uploaded_sha256 "$WINDOWS_INSTALLER" "$SITE_ROOT/downloads/PaperSolver-${DESKTOP_VERSION}.exe" "Windows 安装包"
verify_uploaded_sha256 "$MAC_DMG" "$SITE_ROOT/downloads/PaperSolver-arm64.dmg" "macOS 当前下载别名"
verify_uploaded_sha256 "$WINDOWS_INSTALLER" "$SITE_ROOT/downloads/PaperSolver.exe" "Windows 当前下载别名"

echo "📋 正在上传客户端更新清单..."
scp -o ControlPath="$MUX" "$UPDATE_MANIFEST" "$REMOTE:$SITE_ROOT/downloads/manifest.json.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/manifest.json.uploading' '$SITE_ROOT/downloads/manifest.json'"
REMOTE_UPDATE_VERSION="$(ssh -o ControlPath="$MUX" "$REMOTE" "python3 -c 'import json; print(json.load(open(\"$SITE_ROOT/downloads/manifest.json\"))[\"version\"])'")"
if [ "$REMOTE_UPDATE_VERSION" != "$DESKTOP_VERSION" ]; then
  echo "❌ 服务器更新清单版本校验失败：期望 $DESKTOP_VERSION，实际 $REMOTE_UPDATE_VERSION"
  exit 1
fi
echo "  ✅ 服务器更新清单已确认：$REMOTE_UPDATE_VERSION"
verify_uploaded_sha256 "$UPDATE_MANIFEST" "$SITE_ROOT/downloads/manifest.json" "客户端更新清单"
scp -o ControlPath="$MUX" "$AUTO_UPDATE_WIN_MANIFEST" "$REMOTE:$SITE_ROOT/downloads/latest.yml.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/latest.yml.uploading' '$SITE_ROOT/downloads/latest.yml'"
scp -o ControlPath="$MUX" "$AUTO_UPDATE_MAC_MANIFEST" "$REMOTE:$SITE_ROOT/downloads/latest-mac.yml.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/latest-mac.yml.uploading' '$SITE_ROOT/downloads/latest-mac.yml'"

echo "🧩 正在上传浏览器插件..."
scp -o ControlPath="$MUX" "$CAPTURE_CHROME_ZIP" "$REMOTE:$SITE_ROOT/downloads/papersolver-capture-chrome.zip.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/papersolver-capture-chrome.zip.uploading' '$SITE_ROOT/downloads/papersolver-capture-chrome.zip'"
scp -o ControlPath="$MUX" "$CAPTURE_EDGE_ZIP" "$REMOTE:$SITE_ROOT/downloads/papersolver-capture-edge.zip.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/papersolver-capture-edge.zip.uploading' '$SITE_ROOT/downloads/papersolver-capture-edge.zip'"

# 只有依赖压缩包同步成功时才更新清单。跳过大包上传时保留线上旧清单，
# 避免清单指向一个尚未上传、或校验值不同的远端依赖包。
if [ "$UPLOAD_DEPENDENCIES" != "0" ]; then
  echo "📋 正在上传 ModelScope 依赖清单..."
  scp -o ControlPath="$MUX" "$DEPENDENCY_MANIFEST" "$REMOTE:$SITE_ROOT/downloads/dependencies/dependency-manifest.json.uploading"
  ssh -o ControlPath="$MUX" "$REMOTE" "mv '$SITE_ROOT/downloads/dependencies/dependency-manifest.json.uploading' '$SITE_ROOT/downloads/dependencies/dependency-manifest.json'"
  REMOTE_DEPENDENCY_VERSION="$(ssh -o ControlPath="$MUX" "$REMOTE" "python3 -c 'import json; print(json.load(open(\"$SITE_ROOT/downloads/dependencies/dependency-manifest.json\"))[\"version\"])'")"
  EXPECTED_DEPENDENCY_VERSION="${DESKTOP_VERSION}-offline-full-pipeline"
  if [ "$REMOTE_DEPENDENCY_VERSION" != "$EXPECTED_DEPENDENCY_VERSION" ]; then
    echo "❌ 服务器依赖清单版本校验失败：期望 $EXPECTED_DEPENDENCY_VERSION，实际 $REMOTE_DEPENDENCY_VERSION"
    exit 1
  fi
  REMOTE_WIN_SHA="$(ssh -o ControlPath="$MUX" "$REMOTE" "python3 -c 'import json; m=json.load(open(\"$SITE_ROOT/downloads/dependencies/dependency-manifest.json\")); print(next(x[\"sha256\"] for x in m[\"packages\"] if \"windows-x64\" in x.get(\"platforms\", [])))'")"
  LOCAL_WIN_SHA="$(openssl dgst -sha256 "$WIN_DEPENDENCY" | awk '{print $NF}')"
  if [ "$REMOTE_WIN_SHA" != "$LOCAL_WIN_SHA" ]; then
    echo "❌ 服务器依赖清单 SHA256 校验失败：本地 $LOCAL_WIN_SHA，线上 $REMOTE_WIN_SHA"
    exit 1
  fi
  verify_uploaded_sha256 "$DEPENDENCY_MANIFEST" "$SITE_ROOT/downloads/dependencies/dependency-manifest.json" "依赖清单"
  echo "  ✅ 依赖清单版本与 Windows 包 SHA256 已确认：$REMOTE_DEPENDENCY_VERSION / $REMOTE_WIN_SHA"
else
  echo "📋 依赖压缩包未变化：不上传依赖包、不覆盖线上依赖清单。"
fi

# 4.7 同步微信支付证书到服务器 (如果本地存在)
echo "🔒 正在检查本地微信支付证书并同步到服务器..."
LOCAL_CERT_DIR="/Users/yuan/cert/1748953326_20260802_cert"
if [ -d "$LOCAL_CERT_DIR" ]; then
    ssh -o ControlPath="$MUX" "$REMOTE" "mkdir -p '$SITE_ROOT/certs'"
    scp -o ControlPath="$MUX" "$LOCAL_CERT_DIR"/* "$REMOTE:$SITE_ROOT/certs/"
    echo "  ✅ 微信支付证书同步完成"
else
    echo "  ⚠️ 本地未找到微信支付证书目录，跳过证书同步"
fi

# 4.8 同步 PPT Master 渲染环境与运行时脚本到服务器
echo "📊 正在打包并同步官方 PPT Master 运行时到服务器..."
ssh -o ControlPath="$MUX" "$REMOTE" "mkdir -p /www/wwwroot"
FULL_RUNTIME_TAR="/tmp/ppt-master-full-runtime-$$.tar.gz"
tar -C "$REPO_ROOT/backend" -czf "$FULL_RUNTIME_TAR" ppt-master-runtime
scp -o ControlPath="$MUX" "$FULL_RUNTIME_TAR" "$REMOTE:/www/wwwroot/ppt-master-runtime.tar.gz"
ssh -o ControlPath="$MUX" "$REMOTE" "tar -xzf /www/wwwroot/ppt-master-runtime.tar.gz -C /www/wwwroot/ && rm -f /www/wwwroot/ppt-master-runtime.tar.gz"
rm -f "$FULL_RUNTIME_TAR"
echo "  ✅ PPT Master 运行时同步完成"

# 5. 上传后端 Java 服务 JAR 包
echo "📤 正在上传后端 Java 程序包..."
  scp -o ControlPath="$MUX" "$BACKEND_JAR" "$REMOTE:$BACKEND_REMOTE_JAR.uploading"
ssh -o ControlPath="$MUX" "$REMOTE" "mv '$BACKEND_REMOTE_JAR.uploading' '$BACKEND_REMOTE_JAR'"
LOCAL_BACKEND_SHA="$(openssl dgst -sha256 "$BACKEND_JAR" | awk '{print $NF}')"
REMOTE_BACKEND_SHA="$(ssh -o ControlPath="$MUX" "$REMOTE" "sha256sum '$BACKEND_REMOTE_JAR' | cut -d ' ' -f1")"
if [ "$LOCAL_BACKEND_SHA" != "$REMOTE_BACKEND_SHA" ]; then
  echo "❌ 服务器后端 JAR 校验失败：上传文件与本地构建不一致"
  exit 1
fi
echo "  ✅ 服务器后端 JAR SHA256 校验通过：$REMOTE_BACKEND_SHA"

# 6. Ensure the server has a durable signing secret before restarting. Never print it.
ssh -o ControlPath="$MUX" "$REMOTE" "mkdir -p /etc/papersolver; touch /etc/papersolver/papersolver.env; grep -q '^PAPERPILOT_SESSION_SECRET=.' /etc/papersolver/papersolver.env || printf '\nPAPERPILOT_SESSION_SECRET=%s\n' \"\$(openssl rand -hex 32)\" >> /etc/papersolver/papersolver.env; set_env() { key=\"\$1\"; value=\"\$2\"; if grep -q \"^\${key}=\" /etc/papersolver/papersolver.env; then sed -i \"s|^\${key}=.*|\${key}=\${value}|\" /etc/papersolver/papersolver.env; else printf '%s=%s\\n' \"\$key\" \"\$value\" >> /etc/papersolver/papersolver.env; fi; }; set_env PAPERPILOT_PRIVILEGED_MACHINE_IDS adc9d51ef1b4aa4d52eedcfee6132db46f998a9b8343af87abb6ee008a4ce98c; chmod 600 /etc/papersolver/papersolver.env"

# 7. 只允许 systemd 管理唯一的后端进程，避免 nohup 遗留进程占用 8080。
echo "🔄 正在通过 systemd 重启后端服务..."
ssh -o ControlPath="$MUX" "$REMOTE" "set_env() { key=\"\$1\"; value=\"\$2\"; if grep -q \"^\${key}=\" /etc/papersolver/papersolver.env; then sed -i \"s|^\${key}=.*|\${key}=\${value}|\" /etc/papersolver/papersolver.env; else printf '%s=%s\\n' \"\$key\" \"\$value\" >> /etc/papersolver/papersolver.env; fi; }; set_env PAPERPILOT_DEMO_SEED false; systemctl stop papersolver || true; pkill -f '[p]aperpilot-server-0.0.1-SNAPSHOT.jar' 2>/dev/null || true; sleep 2; systemctl start papersolver && systemctl is-active --quiet papersolver"

echo "🔎 正在确认线上运行的是最新后端构建..."
HEALTH_RESPONSE=""
BACKEND_VERIFIED=0
for _ in {1..20}; do
  HEALTH_RESPONSE="$(curl -fsS "https://papersolver.cn/api/health?ts=$(date +%s)" 2>/dev/null || true)"
  if [[ "$HEALTH_RESPONSE" == *"$EXPECTED_BACKEND_BUILD"* ]]; then
    BACKEND_VERIFIED=1
    break
  fi
  sleep 2
done
if [ "$BACKEND_VERIFIED" != "1" ]; then
  echo "❌ 后端进程版本校验失败，线上健康接口未返回 $EXPECTED_BACKEND_BUILD"
  echo "实际响应：$HEALTH_RESPONSE"
  exit 1
fi
echo "✅ 线上后端已确认：$HEALTH_RESPONSE"

# 7. 关闭临时安全通道
ssh -O exit -S "$MUX" "$REMOTE"

echo "✅ 核心服务已上线：前端、后端 JAR、Nginx 和客户端安装包已上传。"

echo "========================================="
echo "✨ 部署成功！您可以直接访问 http://papersolver.cn"
echo "========================================="
