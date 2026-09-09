const { app, BrowserWindow, Menu, shell, dialog, ipcMain, net, session } = require("electron");
const { autoUpdater } = require("electron-updater");
const path = require("node:path");
const netFetch = (url, options = {}) => {
  const { signal, ...rest } = options;
  if (signal && signal.aborted) {
    return Promise.reject(new Error("The operation was aborted."));
  }
  let abortPromise;
  let onAbort;
  if (signal) {
    abortPromise = new Promise((_, reject) => {
      onAbort = () => reject(new Error("The operation was aborted."));
      signal.addEventListener("abort", onAbort);
    });
  }
  const fetchPromise = net.fetch(url, rest).then(res => {
    if (signal && onAbort) {
      signal.removeEventListener("abort", onAbort);
    }
    return res;
  }).catch(err => {
    if (signal && onAbort) {
      signal.removeEventListener("abort", onAbort);
    }
    throw err;
  });
  if (abortPromise) {
    return Promise.race([fetchPromise, abortPromise]);
  }
  return fetchPromise;
};
const fs = require("node:fs");
const os = require("node:os");
const http = require("node:http");
const https = require("node:https");
const dns = require("node:dns");
const { randomUUID, createHash } = require("node:crypto");
// fetchNative runs standard Node.js http/https requests, completely bypassing Electron's
// overridden global fetch. This avoids Chromium's CORS, header stripping, and chunked POST
// issues (like Akamai returning 411/empty body on Bing translations).
// It automatically follows 301/302/307/308 redirects up to 5 times (useful for Bing).
function fetchNative(url, options = {}, redirectCount = 0) {
  return new Promise((resolve, reject) => {
    if (redirectCount > 5) {
      return reject(new Error("Too many redirects"));
    }
    try {
      const parsedUrl = new URL(url);
      const headers = options.headers || {};
      const method = options.method || "GET";

      let req;
      if (options.signal) {
        if (options.signal.aborted) {
          return reject(new Error("The operation was aborted."));
        }
        options.signal.addEventListener("abort", () => {
          if (req) req.destroy();
          reject(new Error("The operation was aborted."));
        });
      }

      let requestBody = null;
      if (options.body) {
        requestBody = typeof options.body === "string" ? Buffer.from(options.body, "utf-8") : options.body;
        headers["Content-Length"] = requestBody.length;
      }

      const reqOptions = {
        method,
        hostname: parsedUrl.hostname,
        port: parsedUrl.port || (parsedUrl.protocol === "https:" ? 443 : 80),
        path: parsedUrl.pathname + parsedUrl.search,
        headers,
        lookup: options.lookup,
        servername: options.servername || parsedUrl.hostname
      };

      const httpModule = parsedUrl.protocol === "https:" ? https : http;
      req = httpModule.request(reqOptions, (res) => {
        // Follow redirect (301, 302, 307, 308)
        if ([301, 302, 307, 308].includes(res.statusCode) && res.headers.location) {
          const redirectUrl = new URL(res.headers.location, url).toString();
          req.destroy();
          resolve(fetchNative(redirectUrl, options, redirectCount + 1));
          return;
        }

        const chunks = [];
        res.on("data", (chunk) => chunks.push(chunk));
        res.on("end", () => {
          const body = Buffer.concat(chunks).toString("utf-8");
          resolve({
            ok: res.statusCode >= 200 && res.statusCode < 300,
            status: res.statusCode,
            headers: {
              get: (name) => res.headers[name.toLowerCase()]
            },
            text: async () => body,
            json: async () => JSON.parse(body)
          });
        });
      });

      if (GOOGLE_HOSTS.includes(parsedUrl.hostname)) {
        req.on("socket", (socket) => {
          socket.once("lookup", (error, address, family) => {
            logTranslation(`Google socket DNS ${parsedUrl.hostname}: ${error ? error.message : `${address} (IPv${family})`}`);
          });
          socket.once("connect", () => {
            logTranslation(`Google TCP connected ${parsedUrl.hostname}: ${socket.localAddress || "unknown"} -> ${socket.remoteAddress || "unknown"}`);
          });
          socket.once("secureConnect", () => {
            logTranslation(`Google TLS connected ${parsedUrl.hostname}: ${socket.remoteAddress || "unknown"}`);
          });
          socket.once("error", (error) => {
            logTranslation(`Google socket error ${parsedUrl.hostname}: ${error.message}`);
          });
        });
      }

      req.on("error", (err) => {
        reject(err);
      });

      if (requestBody) {
        req.write(requestBody);
      }
      req.end();
    } catch (e) {
      reject(e);
    }
  });
}
const { spawn, execFile, execFileSync } = require("node:child_process");

const APP_PROTOCOL = "papersolver";
let mainWindow = null;
let pendingQqOAuthCallback = null;
let pendingQqLocalOAuthCallback = null;

function parseQqOAuthCallback(rawUrl) {
  try {
    const url = new URL(String(rawUrl || ""));
    if (url.protocol !== `${APP_PROTOCOL}:` || url.hostname !== "oauth") return null;
    const qqSession = url.searchParams.get("qqSession");
    const error = url.searchParams.get("error");
    if (!qqSession && !error) return null;
    return { qqSession: qqSession || "", error: error || "" };
  } catch {
    return null;
  }
}

function deliverQqOAuthCallback(rawUrl) {
  const payload = parseQqOAuthCallback(rawUrl);
  if (!payload) return false;
  appendQqOAuthLog(`external browser callback received: session=${Boolean(payload.qqSession)} error=${Boolean(payload.error)}`);
  pendingQqOAuthCallback = payload;
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.show();
    mainWindow.focus();
    mainWindow.webContents.send("desktop:oauth-qq-callback", payload);
    pendingQqOAuthCallback = null;
  }
  return true;
}

function sendQqOAuthCallback(payload) {
  if (!payload || (!payload.qqSession && !payload.error)) return false;
  const errorSummary = payload.error ? String(payload.error).replace(/[\r\n]+/g, " ").slice(0, 240) : "";
  appendQqOAuthLog(`OAuth callback delivered: session=${Boolean(payload.qqSession)} error=${errorSummary || "none"}`);
  pendingQqOAuthCallback = payload;
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.show();
    mainWindow.focus();
    mainWindow.webContents.send("desktop:oauth-qq-callback", payload);
    pendingQqOAuthCallback = null;
  }
  return true;
}

function closePendingQqLocalCallback() {
  const pending = pendingQqLocalOAuthCallback;
  pendingQqLocalOAuthCallback = null;
  if (!pending) return;
  if (pending.timeout) clearTimeout(pending.timeout);
  try { pending.server.close(); } catch {}
}

async function prepareQqLocalOAuthCallback(authState) {
  closePendingQqLocalCallback();
  const token = randomUUID().replace(/-/g, "");
  const encodedState = Buffer.from(String(authState || ""), "utf8").toString("base64url");
  const server = http.createServer((request, response) => {
    try {
      const callbackUrl = new URL(request.url || "/", "http://127.0.0.1");
      const callbackToken = callbackUrl.searchParams.get("token") || "";
      const qqSession = callbackUrl.searchParams.get("qqSession") || "";
      const error = callbackUrl.searchParams.get("error") || "";
      const accepted = callbackUrl.pathname === "/oauth" && callbackToken === token && (qqSession || error);
      response.writeHead(accepted ? 200 : 400, { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" });
      response.end(accepted
        ? "<!doctype html><html><body style='font-family:system-ui;text-align:center;padding-top:14vh'><h2>登录完成</h2><p>PaperSolver 已收到登录结果，可以关闭此页面并返回客户端。</p></body></html>"
        : "<!doctype html><html><body><h2>登录回调无效</h2><p>请回到 PaperSolver 后重新发起 QQ 登录。</p></body></html>");
      if (!accepted) return;
      closePendingQqLocalCallback();
      sendQqOAuthCallback({ qqSession, error });
    } catch (error) {
      appendQqOAuthLog(`local callback handling failed: ${error?.message || String(error)}`);
      try { response.writeHead(500); response.end(); } catch {}
    }
  });
  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      server.removeListener("error", reject);
      resolve();
    });
  });
  const address = server.address();
  const port = typeof address === "object" && address ? address.port : 0;
  if (!Number.isInteger(port) || port < 1024 || port > 65535) {
    try { server.close(); } catch {}
    throw new Error("无法创建 QQ 登录本地回调，请重试。");
  }
  const timeout = setTimeout(() => {
    appendQqOAuthLog("local OAuth callback timed out");
    closePendingQqLocalCallback();
  }, 5 * 60 * 1000);
  pendingQqLocalOAuthCallback = { server, timeout };
  appendQqOAuthLog(`prepared local OAuth callback on 127.0.0.1:${port}`);
  return { state: `desktop_local_${port}_${token}_${encodedState}` };
}

const hasSingleInstanceLock = app.requestSingleInstanceLock();
if (!hasSingleInstanceLock) {
  app.quit();
}

app.on("second-instance", (_event, commandLine) => {
  const callbackUrl = commandLine.find((item) => String(item).startsWith(`${APP_PROTOCOL}://`));
  if (callbackUrl) deliverQqOAuthCallback(callbackUrl);
});

app.on("open-url", (event, url) => {
  event.preventDefault();
  deliverQqOAuthCallback(url);
});

let cachedRuntimeLogDirectory = "";
let activeLocalDependencyLogFile = "";

function runtimeLogDirectory() {
  if (cachedRuntimeLogDirectory) return cachedRuntimeLogDirectory;
  const candidates = [
    app.isPackaged && process.platform === "win32"
      ? path.join(path.dirname(process.execPath), "logs")
      : "",
    app.isReady() ? path.join(app.getPath("userData"), "logs") : "",
    path.join(process.cwd(), "logs")
  ].filter(Boolean);
  for (const candidate of candidates) {
    try {
      fs.mkdirSync(candidate, { recursive: true });
      const probe = path.join(candidate, `.papersolver-write-test-${process.pid}`);
      fs.writeFileSync(probe, "ok", "utf8");
      fs.rmSync(probe, { force: true });
      cachedRuntimeLogDirectory = candidate;
      return candidate;
    } catch {}
  }
  return candidates[candidates.length - 1] || process.cwd();
}

function appendMainProcessLog(kind, error) {
  try {
    const dir = runtimeLogDirectory();
    fs.mkdirSync(dir, { recursive: true });
    fs.appendFileSync(
      path.join(dir, "main-process.log"),
      `[${new Date().toISOString()}] ${kind}: ${error?.stack || error?.message || String(error)}\n`
    );
  } catch {}
}

function appendQqOAuthLog(message) {
  try {
    const dir = runtimeLogDirectory();
    fs.mkdirSync(dir, { recursive: true });
    fs.appendFileSync(
      path.join(dir, "qq-oauth.log"),
      `[${new Date().toISOString()}] ${String(message || "")}\n`,
      "utf8"
    );
  } catch {}
}

async function appendLocalDependencyLog(message) {
  try {
    const logPath = localDependencyLogPath();
    await fs.promises.mkdir(path.dirname(logPath), { recursive: true });
    await fs.promises.appendFile(logPath, `[${new Date().toISOString()}] ${String(message || "")}\n`, "utf8");
  } catch {}
}

process.on("uncaughtException", (error) => {
  appendMainProcessLog("uncaughtException", error);
});

process.on("unhandledRejection", (reason) => {
  appendMainProcessLog("unhandledRejection", reason);
});

if (process.env.PAPER_SOLVER_DISABLE_GPU === "1") {
  app.disableHardwareAcceleration();
  app.commandLine.appendSwitch("disable-gpu");
  app.commandLine.appendSwitch("disable-gpu-compositing");
} else if (process.platform !== "darwin") {
  app.commandLine.appendSwitch("enable-gpu-rasterization");
  app.commandLine.appendSwitch("enable-zero-copy");
}

const isPackaged = app.isPackaged;
const ZOTERO_LOCAL_BASE = "http://127.0.0.1:23119";
const DEFAULT_API_BASE_URL = normalizeApiBaseUrl(process.env.PAPER_SOLVER_API_BASE) || "https://papersolver.cn";
const DEFAULT_PDFMATH_BASE_URL = normalizeApiBaseUrl(process.env.PAPER_SOLVER_PDFMATH_BASE) || "http://127.0.0.1:11008";
const DESKTOP_TRANSLATION_LABELS = {
  "google-web": "谷歌翻译",
  google: "谷歌翻译",
  bing: "微软翻译",
  youdao: "有道翻译",
  "360-web": "360 翻译",
  "tencent-transmart": "腾讯 TranSmart",
  huoshanweb: "火山翻译",
  deeplx: "DeepLX",
  libretranslate: "LibreTranslate",
  mtranserver: "MTranServer"
};
const MAX_TRANSLATION_CHUNK = 1000;
const DEFAULT_GOOGLE_PROXY_HOSTS = [];
const DEFAULT_DEPENDENCY_MANIFEST_URL = "https://papersolver.cn/downloads/dependencies/dependency-manifest.json";
const DEFAULT_UPDATE_FEED_URL = (process.env.PAPER_SOLVER_UPDATE_FEED || "https://papersolver.cn/downloads/").replace(/\/?$/, "/");
const LOCAL_CAPTURE_PORT = Number(process.env.PAPER_SOLVER_DESKTOP_CAPTURE_PORT) || 18765;
// Four requests keep long PDFs moving without creating an uncontrolled burst
// against the public translation providers. The PDF service applies the same
// cap for layout/PDFium work.
// TranSmart accepts the short health-check request but rate-limits bursts of
// long PDF fragments. Keep the bridge conservative; pdf2zh can still render
// pages in parallel while this queue protects the external engine.
const PDF_TRANSLATION_BRIDGE_CONCURRENCY = Math.max(1, Math.min(2, Math.floor(Number(process.env.PAPER_SOLVER_PDF_TRANSLATION_CONCURRENCY) || 2)));
let activePdfMathBaseUrl = "";
let localCaptureServer = null;
let activePdfTranslationBridgeRequests = 0;
let cachedDesktopNetworkStatus = null;
let cachedDesktopNetworkStatusAt = 0;
let cachedLocalDependencyReadiness = null;
let cachedLocalDependencyReadinessAt = 0;
const pendingPdfTranslationBridgeRequests = [];
const localDependencyProcesses = new Map();
let activeLocalDependencyDownload = null;
let localDependencyDownloadPaused = false;
const desktopPdfMathTasks = new Map();
const desktopPdfMathStartLocks = new Map();
const desktopStructuredParseTasks = new Map();
let autoUpdateConfigured = false;
let autoUpdateState = {
  status: "idle",
  currentVersion: app.getVersion(),
  latestVersion: "",
  updateAvailable: false,
  manualInstall: false,
  downloadUrl: "",
  downloaded: false,
  percent: 0,
  message: ""
};

function isMacUpdateManualInstall() {
  return process.platform === "darwin";
}

function isAutoUpdateSignatureError(error) {
  return /code signature|signature.*validation|签名|ShipIt/i.test(String(error?.message || error || ""));
}

function manualUpdateMessage(version = autoUpdateState.latestVersion) {
  return version
    ? `发现新版本 v${version}，请下载新版安装包后覆盖安装。`
    : "请下载新版安装包后覆盖安装。";
}

function isNewerAppVersion(candidate, current = app.getVersion()) {
  const parse = (value) => String(value || "")
    .trim()
    .replace(/^v/i, "")
    .split(".")
    .map(part => Number.parseInt(part, 10))
    .map(value => Number.isFinite(value) ? value : 0);
  const next = parse(candidate);
  const active = parse(current);
  if (!String(candidate || "").trim() || !String(current || "").trim()) return false;
  for (let index = 0; index < Math.max(next.length, active.length); index += 1) {
    const left = next[index] || 0;
    const right = active[index] || 0;
    if (left !== right) return left > right;
  }
  return false;
}

function appIndexPath() {
  if (isPackaged) {
    return path.join(app.getAppPath(), "front-dist", "index.html");
  }
  return path.join(__dirname, "..", "front-dist", "index.html");
}

function appIconPath() {
  if (isPackaged) {
    return path.join(process.resourcesPath, "icon.png");
  }
  return path.join(__dirname, "..", "build", "icon.png");
}

function createMainWindow() {
  const window = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 1180,
    minHeight: 760,
    title: "PaperSolver V1.0.0",
    icon: appIconPath(),
    backgroundColor: "#0f172a",
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
      webSecurity: true
    }
  });

  window.once("ready-to-show", () => {
    window.show();
  });
  window.webContents.on("did-finish-load", () => {
    if (pendingQqOAuthCallback) {
      window.webContents.send("desktop:oauth-qq-callback", pendingQqOAuthCallback);
      pendingQqOAuthCallback = null;
    }
  });
  window.on("closed", () => {
    if (mainWindow === window) mainWindow = null;
  });

  window.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:\/\//i.test(url)) {
      shell.openExternal(url);
      return { action: "deny" };
    }
    return { action: "allow" };
  });

  window.webContents.on("will-navigate", (event, url) => {
    const currentUrl = window.webContents.getURL();
    console.log(`[Main Window] will-navigate to: ${url}`);
    if (/^https?:\/\//i.test(url) && url !== currentUrl) {
      event.preventDefault();
      shell.openExternal(url);
    }
  });

  window.webContents.on("console-message", (event, level, message, line, sourceId) => {
    console.log(`[Renderer Console] LEVEL ${level}: ${message} (at ${sourceId}:${line})`);
  });

  const indexPath = appIndexPath();
  if (!fs.existsSync(indexPath)) {
    dialog.showErrorBox(
      "PaperSolver 启动失败",
      "没有找到前端构建产物。请先在 desktop 目录执行 npm run build:front。"
    );
  } else {
    window.loadFile(indexPath);
  }

  mainWindow = window;
  return window;
}

function buildMenu() {
  const template = [
    {
      label: "PaperSolver",
      submenu: [
        { role: "about", label: "关于 PaperSolver" },
        { type: "separator" },
        { role: "quit", label: "退出" }
      ]
    },
    {
      label: "编辑",
      submenu: [
        { role: "undo", label: "撤销" },
        { role: "redo", label: "重做" },
        { type: "separator" },
        { role: "cut", label: "剪切" },
        { role: "copy", label: "复制" },
        { role: "paste", label: "粘贴" },
        { role: "selectAll", label: "全选" }
      ]
    },
    {
      label: "视图",
      submenu: [
        { role: "reload", label: "重新载入" },
        { role: "toggleDevTools", label: "开发者工具" },
        { type: "separator" },
        { role: "resetZoom", label: "实际大小" },
        { role: "zoomIn", label: "放大" },
        { role: "zoomOut", label: "缩小" },
        { type: "separator" },
        { role: "togglefullscreen", label: "全屏" }
      ]
    }
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

function broadcastAutoUpdateState(patch = {}) {
  autoUpdateState = {
    ...autoUpdateState,
    ...patch,
    currentVersion: app.getVersion()
  };
  BrowserWindow.getAllWindows().forEach((window) => {
    if (!window.isDestroyed()) {
      window.webContents.send("desktop:update-state", autoUpdateState);
    }
  });
  return autoUpdateState;
}

function configureAutoUpdater() {
  if (autoUpdateConfigured) return;
  autoUpdateConfigured = true;
  autoUpdater.autoDownload = false;
  autoUpdater.autoInstallOnAppQuit = true;
  autoUpdater.allowPrerelease = false;
  autoUpdater.channel = "latest";
  autoUpdater.setFeedURL({
    provider: "generic",
    url: DEFAULT_UPDATE_FEED_URL
  });
  autoUpdater.on("checking-for-update", () => {
    broadcastAutoUpdateState({ status: "checking", message: "正在检测更新..." });
  });
  autoUpdater.on("update-available", (info) => {
    const manualInstall = isMacUpdateManualInstall();
    broadcastAutoUpdateState({
      status: "available",
      updateAvailable: true,
      latestVersion: info?.version || "",
      manualInstall,
      downloaded: false,
      percent: 0,
      message: manualInstall
        ? manualUpdateMessage(info?.version || "")
        : `发现新版本 v${info?.version || ""}，可直接下载并覆盖更新。`
    });
  });
  autoUpdater.on("update-not-available", (info) => {
    broadcastAutoUpdateState({
      status: "idle",
      updateAvailable: false,
      latestVersion: info?.version || app.getVersion(),
      manualInstall: false,
      downloaded: false,
      percent: 0,
      message: "当前已是最新版本。"
    });
  });
  autoUpdater.on("download-progress", (progress) => {
    broadcastAutoUpdateState({
      status: "downloading",
      percent: Math.max(0, Math.min(100, Number(progress?.percent) || 0)),
      message: `正在下载更新 ${Math.round(Number(progress?.percent) || 0)}%`
    });
  });
  autoUpdater.on("update-downloaded", (info) => {
    broadcastAutoUpdateState({
      status: "downloaded",
      updateAvailable: true,
      latestVersion: info?.version || autoUpdateState.latestVersion || "",
      downloaded: true,
      percent: 100,
      message: "更新已下载完成，重启后将自动覆盖安装。"
    });
  });
  autoUpdater.on("error", (error) => {
    broadcastAutoUpdateState({
      status: "error",
      message: error?.message || "自动更新失败，请稍后重试。"
    });
  });
}

app.whenReady().then(() => {
  app.setAsDefaultProtocolClient(APP_PROTOCOL);
  buildMenu();
  startLocalCaptureServer();
  createMainWindow();
  const startupCallbackUrl = process.argv.find((item) => String(item).startsWith(`${APP_PROTOCOL}://`));
  if (startupCallbackUrl) deliverQqOAuthCallback(startupCallbackUrl);
  configureAutoUpdater();
  cleanupLegacyUserPdfArtifacts().catch((error) => {
    console.warn("[Local Cache] Legacy PDF folder cleanup failed:", error?.message || error);
  });

  // Auto-start local dependency if configured
  const settings = readDesktopSettings();
  if (settings.setupCompleted && localDependencyInstalled()) {
    console.log("[Local Dependency] Setup completed. Auto-starting local services...");
    startLocalDependencyServices({ waitForReady: false }).catch((err) => {
      console.error("[Local Dependency] Auto-start failed:", err.message);
    });
  }

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
});

app.on("before-quit", () => {
  closePendingQqLocalCallback();
});

app.on("window-all-closed", () => {
  if (localCaptureServer) {
    localCaptureServer.close();
    localCaptureServer = null;
  }
  if (process.platform !== "darwin") {
    app.quit();
  }
});

ipcMain.handle("desktop:oauth-qq", async (_event, qqAuthUrl) => {
  return new Promise((resolve, reject) => {
    const CHROME_UA = process.platform === "win32"
      ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
      : "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    const popup = new BrowserWindow({
      width: 900,
      height: 680,
      title: "使用 QQ 登录 PaperSolver",
      show: false,
      webPreferences: {
        nodeIntegration: false,
        contextIsolation: true,
        partition: `qq_oauth_${Date.now()}_${Math.random().toString(36).slice(2)}`
      },
    });
    popup.webContents.setUserAgent(CHROME_UA);

    let settled = false;
    let loadedQqPage = false;
    const timeout = setTimeout(() => {
      if (!loadedQqPage) {
        settleReject("QQ 登录页面加载超时。请检查网络、代理或安全软件的 HTTPS 扫描后重试。", "load timeout");
      }
    }, 20_000);

    function closePopup() {
      if (!popup.isDestroyed()) popup.close();
    }

    function settleReject(message, detail) {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      let currentUrl = "";
      try { currentUrl = popup.webContents.getURL() || ""; } catch {}
      appendQqOAuthLog(`failed: ${detail || message}; url=${currentUrl}`);
      closePopup();
      reject(new Error(message));
    }

    function checkUrl(url) {
      console.log(`[QQ Popup] Navigated to: ${url}`);
      appendQqOAuthLog(`navigated: ${url}`);
      if (settled) return;
      try {
        const u = new URL(url);
        if ((u.pathname === "/" || u.pathname === "/login") &&
            (u.hostname.includes("papersolver.cn") || u.hostname === "106.53.136.108")) {
          const qqSession = u.searchParams.get("qqSession");
          const error = u.searchParams.get("error");
          console.log(`[QQ Popup] Match found! qqSession length: ${qqSession ? qqSession.length : 0}, error: ${error}`);
          settled = true;
          clearTimeout(timeout);
          appendQqOAuthLog(`callback received: session=${Boolean(qqSession)} error=${error || ""}`);
          closePopup();
          if (qqSession) resolve({ qqSession });
          else reject(new Error(decodeURIComponent(error || "QQ 授权失败")));
        }
      } catch (err) {
        console.error(`[QQ Popup] checkUrl exception: ${err.message}`);
      }
    }

    popup.webContents.on("will-navigate", (_e, url) => checkUrl(url));
    popup.webContents.on("did-navigate", (_e, url) => checkUrl(url));
    popup.webContents.on("did-navigate-in-page", (_e, url) => checkUrl(url));
    popup.webContents.on("did-finish-load", () => {
      const url = popup.webContents.getURL();
      if (url.includes("graph.qq.com")) loadedQqPage = true;
      appendQqOAuthLog(`finished loading: ${url}`);
      if (!popup.isDestroyed()) popup.show();
      if (url.includes("graph.qq.com")) {
        setTimeout(async () => {
          if (settled || popup.isDestroyed() || !popup.webContents.getURL().includes("graph.qq.com")) return;
          try {
            const pageState = await popup.webContents.executeJavaScript(`({
              textLength: (document.body?.innerText || '').trim().length,
              visibleControls: document.querySelectorAll('iframe, canvas, img, input, button, [role="button"]').length,
              bodyHeight: document.body?.scrollHeight || 0
            })`);
            appendQqOAuthLog(`page probe: ${JSON.stringify(pageState)}`);
            if (pageState.textLength === 0 && pageState.visibleControls === 0 && pageState.bodyHeight < 80) {
              settleReject("QQ 登录页面显示为空白。请关闭代理或安全软件的 HTTPS 扫描后重试。", "QQ page rendered empty");
            }
          } catch (error) {
            appendQqOAuthLog(`page probe failed: ${error?.message || String(error)}`);
          }
        }, 5_000);
      }
    });
    popup.webContents.on("did-fail-load", (_event, errorCode, errorDescription, validatedURL, isMainFrame) => {
      appendQqOAuthLog(`load failed: code=${errorCode} mainFrame=${isMainFrame} url=${validatedURL} error=${errorDescription}`);
      // ERR_ABORTED is emitted for a normal OAuth redirect and must not be treated as a failure.
      if (isMainFrame && errorCode !== -3) {
        settleReject("QQ 登录页面无法加载，请检查网络、代理或安全软件后重试。", `${errorCode}: ${errorDescription}`);
      }
    });
    popup.webContents.on("render-process-gone", (_event, details) => {
      settleReject("QQ 登录窗口意外退出，请重试或更新客户端。", `renderer gone: ${JSON.stringify(details)}`);
    });
    popup.webContents.on("console-message", (_event, level, message, line, sourceId) => {
      appendQqOAuthLog(`console level=${level} ${sourceId}:${line} ${message}`);
    });

    popup.on("closed", () => {
      if (!settled) {
        settleReject("用户关闭了 QQ 授权窗口", "window closed");
      }
    });

    appendQqOAuthLog(`opening authorization window: ${qqAuthUrl.replace(/([?&]state=)[^&]+/, "$1[redacted]")}`);
    popup.loadURL(qqAuthUrl).catch((error) => {
      settleReject("QQ 登录页面无法打开，请检查网络后重试。", error?.message || String(error));
    });
  });
});

ipcMain.handle("desktop:oauth-qq-external", async (_event, qqAuthUrl) => {
  let target;
  try {
    target = new URL(String(qqAuthUrl || ""));
  } catch {
    throw new Error("QQ 授权地址无效，请重试。");
  }
  if (target.protocol !== "https:" || target.hostname !== "graph.qq.com") {
    throw new Error("QQ 授权地址不受信任，请重试。");
  }
  await shell.openExternal(target.toString());
  appendQqOAuthLog("opened QQ authorization in the system browser");
  return { opened: true };
});

ipcMain.handle("desktop:oauth-qq-prepare-local-callback", async (_event, authState) => {
  return prepareQqLocalOAuthCallback(authState);
});

ipcMain.handle("desktop:save-translation-cache", async (_event, paperId, cacheMap) => {
  try {
    const dir = path.join(app.getPath("userData"), "translation_cache");
    fs.mkdirSync(dir, { recursive: true });
    const cleanPaperId = safeCacheKey(paperId);
    if (!cleanPaperId) return false;
    const file = path.join(dir, `${cleanPaperId}.json`);
    fs.writeFileSync(file, JSON.stringify(cacheMap || {}, null, 2), "utf8");
    return true;
  } catch (err) {
    console.error("Failed to save translation cache", err);
    return false;
  }
});

ipcMain.handle("desktop:load-translation-cache", async (_event, paperId) => {
  try {
    const cleanPaperId = safeCacheKey(paperId);
    if (!cleanPaperId) return {};
    const file = path.join(app.getPath("userData"), "translation_cache", `${cleanPaperId}.json`);
    if (fs.existsSync(file)) {
      const raw = fs.readFileSync(file, "utf8");
      return JSON.parse(raw);
    }
  } catch (err) {
    console.error("Failed to load translation cache", err);
  }
  return {};
});

ipcMain.handle("desktop:get-runtime-info", () => ({

  platform: process.platform,
  version: app.getVersion(),
  channel: "latest",
  packaged: isPackaged,
  apiBaseUrl: readDesktopSettings().apiBaseUrl,
  pdfStorageDir: readDesktopSettings().pdfStorageDir,
  pdfMathTranslateBaseUrl: readDesktopSettings().pdfMathTranslateBaseUrl,
  updatePolicy: {
    mode: isMacUpdateManualInstall() ? "manual-download" : "auto-download",
    currentVersion: app.getVersion(),
    channel: "latest",
    feedUrl: DEFAULT_UPDATE_FEED_URL,
    manifestUrl: process.env.PAPER_SOLVER_UPDATE_MANIFEST || "https://papersolver.cn/downloads/manifest.json",
    message: isMacUpdateManualInstall()
      ? "macOS 当前使用下载新版安装包覆盖安装，避免未签名包自动重启安装失败。"
      : "当前已开启应用内自动更新。有新版发布时，可直接下载并重启覆盖安装。"
  }
}));

ipcMain.handle("desktop:check-update", async () => {
  configureAutoUpdater();
  if (!isPackaged) {
    return checkLegacyUpdateManifest();
  }
  try {
    const result = await autoUpdater.checkForUpdates();
    const info = result?.updateInfo || {};
    const latestVersion = info.version || autoUpdateState.latestVersion || "";
    if (!latestVersion) throw new Error("更新服务没有返回有效版本号");
    return broadcastAutoUpdateState({
      status: autoUpdateState.updateAvailable ? "available" : "idle",
      latestVersion,
      updateAvailable: isNewerAppVersion(latestVersion),
      manualInstall: isMacUpdateManualInstall(),
      downloaded: false,
      percent: 0,
      releaseNotes: info.releaseNotes || "",
      message: isNewerAppVersion(latestVersion)
        ? (isMacUpdateManualInstall() ? manualUpdateMessage(latestVersion) : `发现新版本 v${latestVersion}，可直接下载并覆盖更新。`)
        : "当前已是最新版本。"
    });
  } catch (error) {
    const legacy = await checkLegacyUpdateManifest().catch(() => null);
    if (legacy?.latestVersion) {
      return {
        ...legacy,
        status: legacy.updateAvailable ? "available" : "idle",
        manualInstall: isMacUpdateManualInstall() || legacy.manualInstall || isAutoUpdateSignatureError(error),
        downloaded: false,
        message: legacy.updateAvailable
          ? manualUpdateMessage(legacy.latestVersion)
          : "当前已是最新版本。"
      };
    }
    return broadcastAutoUpdateState({
      status: "error",
      message: `自动更新检测失败：${error?.message || "更新服务暂不可用"}`
    });
  }
});

ipcMain.handle("desktop:download-update", async () => {
  configureAutoUpdater();
  if (!isPackaged) {
    throw new Error("开发模式不支持应用内安装更新。");
  }
  if (isMacUpdateManualInstall()) {
    const legacy = await checkLegacyUpdateManifest().catch(() => autoUpdateState);
    const downloadUrl = legacy?.downloadUrl || autoUpdateState.downloadUrl;
    if (downloadUrl) {
      await shell.openExternal(downloadUrl);
    }
    return broadcastAutoUpdateState({
      ...legacy,
      status: legacy?.updateAvailable ? "available" : "idle",
      manualInstall: true,
      downloaded: false,
      message: downloadUrl ? "已为你打开新版安装包下载地址，请下载后覆盖安装。" : "请从 PaperSolver 官网下载新版安装包覆盖安装。"
    });
  }
  broadcastAutoUpdateState({ status: "downloading", percent: 0, message: "正在下载更新..." });
  try {
    await autoUpdater.downloadUpdate();
  } catch (error) {
    if (isAutoUpdateSignatureError(error)) {
      const legacy = await checkLegacyUpdateManifest().catch(() => autoUpdateState);
      if (legacy?.downloadUrl) await shell.openExternal(legacy.downloadUrl);
      return broadcastAutoUpdateState({
        ...legacy,
        status: "available",
        manualInstall: true,
        downloaded: false,
        message: "自动更新签名校验未通过，已切换为下载新版安装包覆盖安装。"
      });
    }
    throw error;
  }
  return autoUpdateState;
});

ipcMain.handle("desktop:install-update", async () => {
  configureAutoUpdater();
  if (autoUpdateState.manualInstall || isMacUpdateManualInstall()) {
    const legacy = await checkLegacyUpdateManifest().catch(() => autoUpdateState);
    const downloadUrl = legacy?.downloadUrl || autoUpdateState.downloadUrl;
    if (downloadUrl) {
      await shell.openExternal(downloadUrl);
      return { ok: true, manualInstall: true };
    }
    throw new Error("当前安装包不支持自动重启安装，请从 PaperSolver 官网下载新版安装包覆盖安装。");
  }
  if (!autoUpdateState.downloaded) {
    throw new Error("更新尚未下载完成。");
  }
  try {
    autoUpdater.quitAndInstall(false, true);
  } catch (error) {
    if (isAutoUpdateSignatureError(error)) {
      const legacy = await checkLegacyUpdateManifest().catch(() => autoUpdateState);
      if (legacy?.downloadUrl) await shell.openExternal(legacy.downloadUrl);
      return { ok: true, manualInstall: true };
    }
    throw error;
  }
  return { ok: true };
});

async function checkLegacyUpdateManifest() {
  const manifestUrl = process.env.PAPER_SOLVER_UPDATE_MANIFEST || "https://papersolver.cn/downloads/manifest.json";
  const manifest = await requestJson(manifestUrl, { timeoutMs: 8000 });
  const platformKey = `${process.platform}-${process.arch}`;
  const platformRelease = manifest.platforms?.[platformKey] || manifest;
  const manualInstall = isMacUpdateManualInstall();
  return broadcastAutoUpdateState({
    ok: true,
    status: isNewerAppVersion(manifest.version) ? "available" : "idle",
    currentVersion: app.getVersion(),
    channel: manifest.channel || "latest",
    latestVersion: manifest.version || "",
    updateAvailable: isNewerAppVersion(manifest.version),
    manualInstall,
    downloaded: false,
    downloadUrl: platformRelease.downloadUrl || manifest.downloadUrl || "",
    releaseNotes: platformRelease.releaseNotes || manifest.releaseNotes || "",
    message: isNewerAppVersion(manifest.version)
      ? (manualInstall ? manualUpdateMessage(manifest.version) : "发现新版本。打包模式下可直接下载并重启覆盖安装。")
      : "当前已是最新版本。"
  });
}

ipcMain.handle("desktop:open-update-download", async (_event, rawUrl) => {
  let target;
  try {
    target = new URL(String(rawUrl || "").trim());
  } catch {
    throw new Error("更新地址无效，请联系管理员。");
  }
  const trustedHosts = new Set(["papersolver.cn", "www.papersolver.cn"]);
  if (target.protocol !== "https:" || !trustedHosts.has(target.hostname)) {
    throw new Error("更新地址不受信任，请联系管理员。");
  }
  await shell.openExternal(target.toString());
  return { ok: true };
});

ipcMain.handle("desktop:get-backend-config", () => readDesktopSettings());

ipcMain.handle("desktop:set-capture-session", (_event, payload) => {
  if (!payload || !payload.userId) {
    return writeDesktopSettings({ captureSession: null });
  }
  const userId = textValue(payload.userId);
  const userName = textValue(payload.userName);
  const email = textValue(payload.email);
  const accessToken = textValue(payload.accessToken);
  return writeDesktopSettings({
    captureSession: {
      userId,
      userName,
      email,
      accessToken,
      updatedAt: new Date().toISOString()
    }
  });
});

ipcMain.handle("desktop:reload-app", () => {
  const window = BrowserWindow.getFocusedWindow() || BrowserWindow.getAllWindows()[0];
  if (window && !window.isDestroyed()) {
    window.webContents.reloadIgnoringCache();
  }
  return { ok: true };
});

ipcMain.handle("desktop:set-backend-config", (_event, payload = {}) => {
  const apiBaseUrl = normalizeApiBaseUrl(payload.apiBaseUrl);
  if (!apiBaseUrl) {
    throw new Error("请输入有效的后端地址，例如 https://api.papersolver.cn");
  }
  const nextSettings = { apiBaseUrl };
  if (Object.prototype.hasOwnProperty.call(payload, "pdfStorageDir")) {
    nextSettings.pdfStorageDir = payload.pdfStorageDir;
  }
  if (Object.prototype.hasOwnProperty.call(payload, "translationEndpoints")) {
    nextSettings.translationEndpoints = payload.translationEndpoints;
  }
  if (Object.prototype.hasOwnProperty.call(payload, "pdfMathTranslateBaseUrl")) {
    nextSettings.pdfMathTranslateBaseUrl = payload.pdfMathTranslateBaseUrl;
  }
  if (Object.prototype.hasOwnProperty.call(payload, "setupCompleted")) {
    nextSettings.setupCompleted = Boolean(payload.setupCompleted);
  }
  return writeDesktopSettings(nextSettings);
});

ipcMain.handle("desktop:reset-backend-config", () => writeDesktopSettings({
  apiBaseUrl: DEFAULT_API_BASE_URL,
  pdfStorageDir: defaultPdfStorageDir(),
  pdfMathTranslateBaseUrl: DEFAULT_PDFMATH_BASE_URL,
  setupCompleted: false,
  translationEndpoints: defaultTranslationEndpoints()
}));

ipcMain.handle("desktop:select-pdf-storage-dir", async () => {
  const settings = readDesktopSettings();
  const result = await dialog.showOpenDialog({
    title: "选择 PaperSolver PDF 保存目录",
    defaultPath: settings.pdfStorageDir || defaultPdfStorageDir(),
    properties: ["openDirectory", "createDirectory"]
  });
  if (result.canceled || !result.filePaths?.[0]) {
    return { canceled: true, path: settings.pdfStorageDir || defaultPdfStorageDir() };
  }
  const nextSettings = writeDesktopSettings({ pdfStorageDir: result.filePaths[0] });
  await fs.promises.mkdir(nextSettings.pdfStorageDir, { recursive: true });
  return { canceled: false, path: nextSettings.pdfStorageDir, settings: nextSettings };
});

ipcMain.handle("desktop:zotero-import-local", async (_event, options = {}) => {
  const limit = Math.max(1, Math.min(200, Number(options.limit) || 100));
  return readLocalZoteroItems(limit);
});

ipcMain.handle("desktop:read-zotero-pdf", async (_event, pdfRef = {}) => {
  return readZoteroPdf(pdfRef);
});

ipcMain.handle("desktop:cache-pdf", async (_event, payload = {}) => {
  return cachePdf(payload);
});

ipcMain.handle("desktop:save-ppt-deck", async (_event, payload = {}) => {
  return savePptDeck(payload);
});

ipcMain.handle("desktop:save-note-markdown", async (_event, payload = {}) => {
  const content = textValue(payload.content);
  const rawName = payload.fileName || `note-${Date.now()}`;
  const baseDir = path.join(userPdfStorageDir(), "note");
  await fs.promises.mkdir(baseDir, { recursive: true });
  const cleanName = rawName.replace(/[\/\\?%*:|"<>]/g, "-").trim() + ".md";
  const targetPath = path.join(baseDir, cleanName);
  await fs.promises.writeFile(targetPath, content, "utf8");
  return {
    ok: true,
    path: targetPath,
    fileName: cleanName,
  };
});

ipcMain.handle("desktop:save-file", async (_event, payload = {}) => {
  const rawName = String(payload.fileName || `papersolver-export-${Date.now()}`).replace(/[\\/?%*:|\"<>]/g, "-");
  const result = await dialog.showSaveDialog({
    title: "保存转换后的文件",
    defaultPath: path.join(app.getPath("downloads"), rawName),
    buttonLabel: "保存",
  });
  if (result.canceled || !result.filePath) return { canceled: true };
  await fs.promises.writeFile(result.filePath, Buffer.from(payload.buffer));
  return { ok: true, path: result.filePath, fileName: path.basename(result.filePath) };
});

ipcMain.handle("desktop:get-cached-pdf", async (_event, payload = {}) => {
  return getCachedPdf(payload);
});

ipcMain.handle("desktop:open-cached-pdf", async (_event, payload = {}) => {
  const cached = await getCachedPdf(payload);
  if (!cached?.found || !cached.path) {
    throw new Error(cached?.error || "本机没有可打开的原文 PDF 缓存");
  }
  const error = await shell.openPath(cached.path);
  if (error) throw new Error(error);
  return { ok: true, path: cached.path, fileName: cached.fileName };
});

// Keep filesystem paths inside the main process. The renderer only receives a
// boolean for a known workspace id, which is enough to detect a moved device.
ipcMain.handle("desktop:has-cached-pdf", async (_event, payload = {}) => {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) return { found: false };
  for (const cacheDir of pdfCacheDirsForRead()) {
    const candidates = [
      path.join(cacheDir, `${workspaceId}.pdf`),
      path.join(cacheDir, "structured", workspaceId, "input", `${workspaceId}.pdf`),
    ];
    for (const pdfPath of candidates) {
      try {
        const stat = await fs.promises.stat(pdfPath);
        if (stat.isFile() && stat.size > 0) return { found: true };
      } catch (error) {
        if (error?.code !== "ENOENT") {
          console.warn("[Local Cache] PDF availability probe failed:", error?.message || error);
        }
      }
    }
  }
  return { found: false };
});

ipcMain.handle("desktop:ensure-cached-pdf", async (_event, payload = {}) => {
  return ensureCachedPdf(payload, { prompt: true });
});

ipcMain.handle("desktop:get-cache-info", async () => {
  return getCacheInfo();
});

ipcMain.handle("desktop:clear-pdf-cache", async () => {
  return clearPdfCache();
});

ipcMain.handle("desktop:open-cache-dir", async () => {
  await cleanupLegacyUserPdfArtifacts().catch(() => {});
  await fs.promises.mkdir(userPdfStorageDir(), { recursive: true });
  const error = await shell.openPath(userPdfStorageDir());
  if (error) throw new Error(error);
  return { ok: true, path: userPdfStorageDir() };
});

ipcMain.handle("desktop:get-translation-providers", () => {
  const settings = readDesktopSettings();
  const providers = [
    { id: "google", label: DESKTOP_TRANSLATION_LABELS.google, configured: true, local: true },
    { id: "bing", label: DESKTOP_TRANSLATION_LABELS.bing, configured: true, local: true },
    { id: "youdao", label: DESKTOP_TRANSLATION_LABELS.youdao, configured: true, local: true },
    { id: "huoshanweb", label: DESKTOP_TRANSLATION_LABELS.huoshanweb, configured: true, local: true },
    {
      id: "deeplx",
      label: DESKTOP_TRANSLATION_LABELS.deeplx,
      configured: Boolean(settings.translationEndpoints.deeplxEndpoint),
      local: true
    },
    {
      id: "libretranslate",
      label: DESKTOP_TRANSLATION_LABELS.libretranslate,
      configured: Boolean(settings.translationEndpoints.libreTranslateEndpoint),
      local: true
    },
    {
      id: "mtranserver",
      label: DESKTOP_TRANSLATION_LABELS.mtranserver,
      configured: Boolean(settings.translationEndpoints.mtranServerEndpoint),
      local: true
    }
  ];
  return providers.filter((provider) => provider.configured);
});

ipcMain.handle("desktop:test-translation-provider", async (_event, payload = {}) => {
  const provider = normalizeDesktopTranslationProvider(payload.provider);
  const settings = {
    ...readDesktopSettings(),
    translationEndpoints: normalizeTranslationEndpoints(payload.translationEndpoints || readDesktopSettings().translationEndpoints)
  };
  const translatedText = await translateWithDesktopProvider(provider, "Hello PaperSolver.", "en", "zh-CN", settings);
  return {
    ok: true,
    provider,
    providerLabel: DESKTOP_TRANSLATION_LABELS[provider] || provider,
    translatedText
  };
});

ipcMain.handle("desktop:translate", async (_event, payload = {}) => {
  return translateOnDesktop(payload);
});

ipcMain.handle("desktop:local-dependency-status", async (event) => {
  return getLocalDependencyStatus(event.sender);
});

ipcMain.handle("desktop:download-local-dependency", async (event, payload = {}) => {
  if (payload.liteMode !== undefined) {
    writeDesktopSettings({ localDependencyLiteMode: Boolean(payload.liteMode) });
  }
  if (activeLocalDependencyDownload) {
    throw new Error("本机能力正在下载或配置，请等待当前操作完成。");
  }
  const control = {
    paused: false,
    requests: new Set()
  };
  activeLocalDependencyDownload = control;
  cachedLocalDependencyReadiness = null;
  cachedLocalDependencyReadinessAt = 0;
  localDependencyDownloadPaused = false;
  try {
    return await downloadAndInstallLocalDependency(event.sender, payload, control);
  } catch (error) {
    if (control.paused || isDependencyDownloadPausedError(error)) {
      localDependencyDownloadPaused = true;
      return {
        ok: false,
        paused: true,
        installed: false,
        running: false,
        message: "本机能力下载已暂停。再次点击准备即可从断点继续。"
      };
    }
    throw error;
  } finally {
    if (activeLocalDependencyDownload === control) {
      activeLocalDependencyDownload = null;
    }
  }
});

ipcMain.handle("desktop:pause-local-dependency-download", async (event) => {
  const control = activeLocalDependencyDownload;
  if (!control) {
    return { ok: false, paused: false, message: "当前没有正在进行的本机能力下载。" };
  }
  control.paused = true;
  localDependencyDownloadPaused = true;
  for (const request of control.requests) {
    request.destroy(createDependencyDownloadPausedError());
  }
  emitDependencyProgress(event.sender, {
    stage: "paused",
    progress: 0,
    message: "本机能力下载已暂停",
    detail: "已保留下载进度，再次点击准备即可继续"
  });
  return { ok: true, paused: true };
});

ipcMain.handle("desktop:clear-local-dependency", async () => {
  if (activeLocalDependencyDownload) {
    throw new Error("请先暂停当前下载，再清除本机能力。");
  }
  await stopLocalDependencyProcesses();
  await removeLocalDependencyDir(localDependencyDir());
  await fs.promises.rm(dependencyDownloadRoot(), { recursive: true, force: true });
  activePdfMathBaseUrl = "";
  cachedLocalDependencyReadiness = null;
  cachedLocalDependencyReadinessAt = 0;
  localDependencyDownloadPaused = false;
  return {
    ok: true,
    installed: false,
    running: false,
    paused: false,
    message: "已清除本机能力和下载缓存。"
  };
});

ipcMain.handle("desktop:start-local-dependency", async (event) => {
  try {
    return await startLocalDependencyServices({ waitForReady: true, webContents: event.sender });
  } catch (error) {
    await appendLocalDependencyLog(`本机能力启动失败：${error?.stack || error?.message || String(error)}`);
    const logTail = await tailFile(localDependencyLogPath(), 2200);
    const detail = logTail ? `\n\n最近日志：\n${logTail.slice(-1200)}` : "";
    throw new Error(`${error?.message || "本机能力启动失败"}${detail}`);
  }
});

ipcMain.handle("desktop:open-local-dependency-log", async () => {
  return openLocalDependencyLog();
});

ipcMain.handle("desktop:pdfmath-start", async (_event, payload = {}) => {
  const workspaceId = safeCacheKey(payload?.workspaceId);
  if (!workspaceId) return startDesktopPdfMathTranslation(payload);
  const runningStart = desktopPdfMathStartLocks.get(workspaceId);
  if (runningStart) {
    const result = await runningStart;
    return { ...result, reused: true, concurrentReuse: true };
  }
  const startPromise = startDesktopPdfMathTranslation(payload);
  desktopPdfMathStartLocks.set(workspaceId, startPromise);
  try {
    return await startPromise;
  } finally {
    if (desktopPdfMathStartLocks.get(workspaceId) === startPromise) {
      desktopPdfMathStartLocks.delete(workspaceId);
    }
  }
});

ipcMain.handle("desktop:pdfmath-status", async (_event, payload = {}) => {
  return getDesktopPdfMathStatus(payload);
});

ipcMain.handle("desktop:pdfmath-dual-pdf", async (_event, payload = {}) => {
  return getDesktopPdfMathDualPdf(payload);
});

ipcMain.handle("desktop:structured-parse-start", async (_event, payload = {}) => {
  return startDesktopStructuredParse(payload);
});

ipcMain.handle("desktop:structured-parse-status", async (_event, payload = {}) => {
  return getDesktopStructuredParseStatus(payload);
});

ipcMain.handle("desktop:structured-document", async (_event, payload = {}) => {
  return getDesktopStructuredDocument(payload);
});

ipcMain.handle("desktop:structured-asset", async (_event, payload = {}) => {
  return getDesktopStructuredAsset(payload);
});

function startLocalCaptureServer() {
  if (localCaptureServer) return;
  localCaptureServer = http.createServer(async (request, response) => {
    const send = (status, body = {}) => {
      response.writeHead(status, {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET,POST,OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type",
        "Content-Type": "application/json; charset=utf-8"
      });
      response.end(JSON.stringify(body));
    };
    if (request.method === "OPTIONS") {
      send(204, {});
      return;
    }
    try {
      const requestUrl = new URL(request.url || "/", `http://127.0.0.1:${LOCAL_CAPTURE_PORT}`);
      if (request.method === "GET" && requestUrl.pathname === "/health") {
        const settings = readDesktopSettings();
        send(200, {
          ok: true,
          app: "PaperSolver Desktop",
          version: app.getVersion(),
          apiBaseUrl: settings.apiBaseUrl || DEFAULT_API_BASE_URL,
          session: settings.captureSession || null,
          pdfStorageDir: settings.pdfStorageDir || defaultPdfStorageDir()
        });
        return;
      }
      if (request.method === "POST" && requestUrl.pathname === "/cache-pdf") {
        const body = await readJsonRequestBody(request, 140 * 1024 * 1024);
        const dataUrl = textValue(body.pdfDataUrl);
        const base64 = dataUrl.includes(",") ? dataUrl.split(",").pop() : textValue(body.base64);
        const result = await cachePdf({
          workspaceId: body.workspaceId,
          base64,
          fileName: body.pdfFileName || body.fileName
        });
        send(200, { ...result, paperUrl: `desktop-cache://${result.workspaceId}`, cacheDir: pdfCacheDir() });
        return;
      }
      if (request.method === "POST" && requestUrl.pathname === "/translate") {
        const body = await readJsonRequestBody(request, 1024 * 1024);
        const translate = () => translateOnDesktop({
          provider: body.provider || "google",
          text: body.text || "",
          sourceLang: body.pdfTranslationBridge === true ? "auto" : (body.sourceLang || body.source_lang || "auto"),
          targetLang: body.targetLang || body.target_lang || "zh-CN",
          pdfTranslationBridge: body.pdfTranslationBridge === true
        });
        // pdf2zh emits many small requests in parallel. Serialize them so one
        // desktop IP does not burst through the provider's short-term limits.
        const result = body.pdfTranslationBridge === true
          ? await enqueuePdfTranslationBridge(translate)
          : await translate();
        send(200, {
          ok: true,
          translatedText: result.translatedText,
          provider: result.provider,
          route: result.route,
          latencyMs: result.latencyMs
        });
        return;
      }
      send(404, { ok: false, error: "Not found" });
    } catch (error) {
      if (request.method === "POST" && request.url?.startsWith("/translate")) {
        logTranslation(`PDF translation bridge rejected a request: ${error?.stack || error?.message || String(error)}`);
      }
      send(400, { ok: false, error: error?.message || "PaperSolver Desktop 本机接收失败" });
    }
  });
  localCaptureServer.on("error", () => {
    localCaptureServer = null;
  });
  localCaptureServer.listen(LOCAL_CAPTURE_PORT, "127.0.0.1");
}

function enqueuePdfTranslationBridge(task) {
  return new Promise((resolve, reject) => {
    pendingPdfTranslationBridgeRequests.push({ task, resolve, reject });
    drainPdfTranslationBridgeQueue();
  });
}

function drainPdfTranslationBridgeQueue() {
  while (activePdfTranslationBridgeRequests < PDF_TRANSLATION_BRIDGE_CONCURRENCY && pendingPdfTranslationBridgeRequests.length) {
    const next = pendingPdfTranslationBridgeRequests.shift();
    activePdfTranslationBridgeRequests += 1;
    Promise.resolve()
      .then(next.task)
      .then(next.resolve, next.reject)
      .finally(() => {
        activePdfTranslationBridgeRequests -= 1;
        drainPdfTranslationBridgeQueue();
      });
  }
}

function readJsonRequestBody(request, maxBytes) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    request.on("data", (chunk) => {
      size += chunk.length;
      if (size > maxBytes) {
        reject(new Error("PDF 太大，超过本机接收限制。"));
        request.destroy();
        return;
      }
      chunks.push(chunk);
    });
    request.on("end", () => {
      try {
        const raw = Buffer.concat(chunks).toString("utf8");
        resolve(raw ? JSON.parse(raw) : {});
      } catch {
        reject(new Error("请求内容不是有效 JSON。"));
      }
    });
    request.on("error", reject);
  });
}

async function readLocalZoteroItems(limit) {
  const requests = [];
  let start = 0;
  while (requests.length < limit && start < 200) {
    const root = await getLocalZoteroJson(`/api/users/0/items?format=json&limit=100&sort=dateAdded&direction=desc&start=${start}`);
    if (!Array.isArray(root) || root.length === 0) break;
    for (const item of root) {
      const request = requestFromZoteroItem(item);
      if (request) {
        const pdfInfo = await localPdfForZoteroItem(item);
        request.localPdfStatus = pdfInfo.status;
        request.localPdfMessage = pdfInfo.message;
        if (pdfInfo.pdf) {
          request.localPdf = pdfInfo.pdf;
        }
        requests.push(request);
      }
      if (requests.length >= limit) break;
    }
    if (root.length < 100) break;
    start += 100;
  }
  const deduped = dedupeZoteroRequests(requests).slice(0, limit);
  if (!deduped.length) {
    throw new Error("未从本机 Zotero 读取到文献。请确认 Zotero Desktop 已打开，并已允许本机通讯。");
  }
  return {
    fileName: "Zotero 本机同步",
    detected: deduped.length,
    local: true,
    items: deduped
  };
}

async function getLocalZoteroJson(apiPath) {
  let response;
  try {
    response = await fetch(`${ZOTERO_LOCAL_BASE}${apiPath}`, {
      headers: {
        "Zotero-API-Version": "3",
        "Accept": "application/json"
      }
    });
  } catch {
    throw new Error("未检测到本机 Zotero。请先打开 Zotero Desktop 后再导入。");
  }
  if (response.status === 403) {
    throw new Error("本机 Zotero 拒绝访问。请打开 Zotero 设置里的本机通讯开关后重启 Zotero。");
  }
  if (!response.ok) {
    throw new Error(`本机 Zotero 返回错误：${response.status}`);
  }
  return response.json();
}

async function getLocalZoteroArray(apiPath) {
  const json = await getLocalZoteroJson(apiPath);
  return Array.isArray(json) ? json : [];
}

async function localPdfForZoteroItem(item) {
  const key = textValue(item?.key || item?.data?.key);
  if (!key) {
    return { status: "failed", message: "Zotero 条目缺少 key，无法读取附件。", pdf: null };
  }
  let children = [];
  try {
    children = await getLocalZoteroArray(`/api/users/0/items/${encodeURIComponent(key)}/children?format=json&limit=100`);
  } catch (error) {
    return { status: "failed", message: error?.message || "读取 Zotero 附件列表失败。", pdf: null };
  }
  if (!children.length) {
    return { status: "missing", message: "Zotero 条目下没有附件。", pdf: null };
  }
  let attachmentCount = 0;
  let nonPdfCount = 0;
  let candidateWithoutFile = 0;
  for (const child of children) {
    const data = child?.data || {};
    const filename = firstNonBlank(data.filename, data.title, "zotero-attachment.pdf");
    const contentType = textValue(data.contentType).toLowerCase();
    if (textValue(data.itemType) !== "attachment") continue;
    attachmentCount += 1;
    if (contentType && !contentType.includes("pdf") && !filename.toLowerCase().endsWith(".pdf")) {
      nonPdfCount += 1;
      continue;
    }
    const enclosure = child?.links?.enclosure?.href || child?.links?.attachment?.href || "";
    const localPath = fileUrlToPath(enclosure);
    if (localPath) {
      return {
        status: "found",
        message: "已找到 Zotero 本机 PDF 附件。",
        pdf: { kind: "path", path: localPath, fileName: normalizedPdfName(filename) }
      };
    }
    const childKey = textValue(child?.key || data.key);
    if (childKey) {
      return {
        status: "found",
        message: "已找到 Zotero PDF 附件，将通过本机接口读取。",
        pdf: {
          kind: "zotero-api",
          apiPath: `/api/users/0/items/${encodeURIComponent(childKey)}/file`,
          fileName: normalizedPdfName(filename)
        }
      };
    }
    candidateWithoutFile += 1;
  }
  if (!attachmentCount) {
    return { status: "missing", message: "Zotero 条目下没有附件。", pdf: null };
  }
  if (nonPdfCount === attachmentCount) {
    return { status: "missing", message: "Zotero 条目有附件，但没有 PDF 文件。", pdf: null };
  }
  if (candidateWithoutFile) {
    return { status: "missing", message: "检测到 PDF 附件记录，但文件未在本机可读；请先在 Zotero 中下载附件。", pdf: null };
  }
  return { status: "missing", message: "没有找到可读取的 Zotero PDF 附件。", pdf: null };
}

async function readZoteroPdf(pdfRef) {
  const refKind = textValue(pdfRef.kind);
  if (refKind === "path") {
    const filePath = textValue(pdfRef.path);
    if (!filePath.toLowerCase().endsWith(".pdf")) {
      throw new Error("Zotero 附件不是 PDF 文件。");
    }
    const stat = await fs.promises.stat(filePath);
    assertReasonablePdfSize(stat.size);
    const buffer = await fs.promises.readFile(filePath);
    return {
      fileName: normalizedPdfName(pdfRef.fileName || path.basename(filePath)),
      mimeType: "application/pdf",
      size: buffer.length,
      base64: buffer.toString("base64")
    };
  }
  if (refKind === "zotero-api") {
    const apiPath = textValue(pdfRef.apiPath);
    if (!apiPath.startsWith("/api/users/0/items/") || !apiPath.endsWith("/file")) {
      throw new Error("Zotero 附件地址无效。");
    }
    const response = await fetch(`${ZOTERO_LOCAL_BASE}${apiPath}`, {
      headers: {
        "Zotero-API-Version": "3",
        "Accept": "application/pdf,application/octet-stream,*/*"
      }
    });
    if (response.status === 403) {
      throw new Error("本机 Zotero 拒绝读取 PDF，请确认本机通讯开关已开启。");
    }
    if (!response.ok) {
      throw new Error(`读取 Zotero PDF 失败：${response.status}`);
    }
    const arrayBuffer = await response.arrayBuffer();
    assertReasonablePdfSize(arrayBuffer.byteLength);
    const buffer = Buffer.from(arrayBuffer);
    return {
      fileName: normalizedPdfName(pdfRef.fileName || "zotero-attachment.pdf"),
      mimeType: response.headers.get("content-type") || "application/pdf",
      size: buffer.length,
      base64: buffer.toString("base64")
    };
  }
  throw new Error("没有找到可读取的 Zotero PDF 附件。");
}

function requestFromZoteroItem(item) {
  const data = item?.data || {};
  const itemType = textValue(data.itemType);
  if (["attachment", "note", "annotation"].includes(itemType)) return null;
  const title = firstNonBlank(data.title, data.shortTitle);
  if (!title) return null;
  const doi = textValue(data.DOI);
  const url = firstNonBlank(data.url, doi ? `https://doi.org/${doi}` : "");
  const source = firstNonBlank(
    data.publicationTitle,
    data.conferenceName,
    data.proceedingsTitle,
    data.publisher,
    "Zotero"
  );
  return {
    title,
    authors: authorsFromZoteroCreators(data.creators),
    source,
    publishYear: yearFromZoteroDate(data.date),
    abstractText: firstNonBlank(data.abstractNote, data.extra),
    paperId: doi,
    sourceUrl: url,
    paperUrl: url,
    importSource: "Zotero 本机同步",
    articleType: firstNonBlank(itemType, "journal-article")
  };
}

function authorsFromZoteroCreators(creators) {
  if (!Array.isArray(creators)) return "";
  return creators
    .map((creator) => firstNonBlank(
      creator.name,
      `${textValue(creator.firstName)} ${textValue(creator.lastName)}`.trim(),
      creator.lastName
    ))
    .filter(Boolean)
    .join(", ");
}

function yearFromZoteroDate(date) {
  const match = String(date || "").match(/(19|20)\d{2}/);
  return match ? match[0] : "";
}

function dedupeZoteroRequests(items) {
  const seen = new Set();
  const result = [];
  for (const item of items) {
    const key = `${String(item.title || "").trim().toLowerCase()}|${String(item.paperId || "").trim().toLowerCase()}`;
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(item);
  }
  return result;
}

function firstNonBlank(...values) {
  for (const value of values) {
    const text = textValue(value);
    if (text) return text;
  }
  return "";
}

function textValue(value) {
  return String(value ?? "").replace(/\s+/g, " ").trim();
}

function shouldKeepOriginalPdfBridgeText(value) {
  const text = textValue(value);
  if (!text) return true;
  const compact = text.replace(/\s+/g, "");
  if (compact.length <= 2) return true;
  if (!/[A-Za-zÀ-ž\u4e00-\u9fff]/.test(text)) return true;

  const latinLetters = text.match(/[A-Za-zÀ-ž]/g)?.length || 0;
  if (latinLetters > 0 && latinLetters / Math.max(compact.length, 1) < 0.28 && compact.length < 80) {
    return true;
  }

  const commaCount = (text.match(/[,，]/g) || []).length;
  const words = text.match(/[A-Za-zÀ-ž][A-Za-zÀ-ž'’.-]{1,}/g) || [];
  const hasSentencePunctuation = /[.!?。！？:：;]/.test(text);
  const longWordCount = words.filter(word => word.length >= 14).length;
  if (text.length < 260 && commaCount >= 3 && !hasSentencePunctuation && words.length >= 4 && longWordCount <= 1) {
    return true;
  }

  return false;
}

async function translateOnDesktop(payload) {
  let provider = normalizeDesktopTranslationProvider(payload.provider);
  if ((provider === "google" || provider === "google-web") && payload.autoNetworkProvider !== false) {
    const network = await getDesktopNetworkStatus();
    if (!network.googleReachable) provider = "tencent-transmart";
  }
  const text = textValue(payload.text);
  if (!text) throw new Error("待翻译文本不能为空");
  if (payload.pdfTranslationBridge === true && shouldKeepOriginalPdfBridgeText(text)) {
    return {
      provider,
      requestedProvider: provider,
      providerLabel: DESKTOP_TRANSLATION_LABELS[provider] || provider,
      sourceLang: normalizeTranslationLang(payload.sourceLang, "auto"),
      targetLang: normalizeTranslationLang(payload.targetLang, "zh-CN"),
      translatedText: text,
      route: "pdf-bridge-preserve",
      latencyMs: 0,
      networkProfile: undefined,
      fallback: false,
      local: true
    };
  }
  const sourceLang = normalizeTranslationLang(payload.sourceLang, "auto");
  const targetLang = normalizeTranslationLang(payload.targetLang, "zh-CN");
  const settings = readDesktopSettings();
  const { translatedText, actualProvider, route, latencyMs, networkProfile } = await translateWithDesktopFallback(provider, text, sourceLang, targetLang, settings, {
    pdfTranslationBridge: payload.pdfTranslationBridge === true
  });
  if (payload.pdfTranslationBridge === true && actualProvider !== provider) {
    try {
      await fs.promises.writeFile(
        path.join(localDependencyDir(), "translation-provider.json"),
        JSON.stringify({ provider: actualProvider, updatedAt: new Date().toISOString() }),
        "utf8"
      );
    } catch {
      // The request already succeeded; a later request can retry the fallback.
    }
  }
  return {
    provider: actualProvider,
    requestedProvider: provider,
    providerLabel: DESKTOP_TRANSLATION_LABELS[actualProvider] || actualProvider,
    sourceLang,
    targetLang,
    translatedText,
    route,
    latencyMs,
    networkProfile,
    fallback: actualProvider !== provider,
    local: true
  };
}

async function getDesktopNetworkStatus(options = {}) {
  const now = Date.now();
  if (!options.force && cachedDesktopNetworkStatus && now - cachedDesktopNetworkStatusAt < 30000) {
    return cachedDesktopNetworkStatus;
  }
  const startedAt = Date.now();
  let proxy = "DIRECT";
  try {
    proxy = (await session.defaultSession.resolveProxy("https://translate.googleapis.com")) || "DIRECT";
  } catch (error) {
    proxy = `UNKNOWN (${error.message})`;
  }
  let googleReachable = false;
  let googleLatencyMs = 0;
  try {
    const probeStartedAt = Date.now();
    const response = await fetchWithTimeout(
      "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=zh-CN&dt=t&q=PaperSolver",
      { headers: { Accept: "application/json,text/plain,*/*" } },
      8000
    );
    googleReachable = Boolean(response?.ok);
    googleLatencyMs = Date.now() - probeStartedAt;
  } catch (error) {
    googleReachable = false;
  }
  const vpnInterface = Object.keys(os.networkInterfaces()).find((name) =>
    /^(utun|ppp|wg|wireguard|tailscale|clash|sing-box|天融信|forti|globalprotect|anyconnect)/i.test(name)
  ) || "";
  const envProxy = [process.env.HTTPS_PROXY, process.env.https_proxy, process.env.ALL_PROXY, process.env.all_proxy]
    .map((value) => String(value || "").trim())
    .find(Boolean) || "";
  // Electron may report an automatic PAC entry such as `PROXY ...; DIRECT`
  // even when the request is going direct. Treat that as direct unless the
  // proxy is the only route; otherwise ordinary Windows PAC settings look
  // like a VPN to users.
  const proxyDetected = /(?:^|;)\s*(?:PROXY|HTTPS?|SOCKS)\s+\S+/i.test(proxy)
    && !/\bDIRECT\b/i.test(proxy);
  const explicitEnvProxy = Boolean(envProxy)
    && !/^(?:https?|socks):\/\/(?:127\.0\.0\.1|localhost)(?::\d+)?$/i.test(envProxy);
  const tunnelDetected = proxyDetected || Boolean(vpnInterface);
  // Proxifier and some VPN clients route only the app process, so no OS-level
  // tunnel interface is visible. For product routing, Google reachability is
  // the source of truth: if the user's desktop can reach Google, use Google.
  const vpnLikely = googleReachable;
  const latencyMs = Date.now() - startedAt;
  const result = {
    ok: true,
    googleReachable,
    googleLatencyMs,
    latencyMs,
    proxy,
    vpnLikely,
    vpnInterface,
    explicitEnvProxy,
    tunnelDetected,
    label: vpnLikely
      ? `Google 翻译可达${tunnelDetected && vpnInterface ? `（${vpnInterface}）` : ""}`
      : "Google 翻译不可达",
    checkedAt: new Date().toISOString()
  };
  cachedDesktopNetworkStatus = result;
  cachedDesktopNetworkStatusAt = Date.now();
  logTranslation(`Network status: proxy=${proxy}, googleReachable=${googleReachable}, googleLatencyMs=${googleLatencyMs}, vpnLikely=${result.vpnLikely}`);
  return result;
}

async function getLocalDependencyStatus(webContents = null) {
  await prepareDependencyStorageLocation();
  const settings = readDesktopSettings();
  const startedAt = Date.now();
  const readiness = await getLocalDependencyReadiness({
    parserTimeoutMs: 90000,
    onParserProgress: (elapsedSeconds) => emitDependencyProgress(webContents, {
      stage: "parser-smoke-test",
      progress: Math.min(98, 94 + Math.floor(elapsedSeconds / 25)),
      message: "正在首次启动版面解析器...",
      detail: `MinerU 正在加载运行组件，已等待 ${elapsedSeconds} 秒`
    })
  });
  const structuredParserAvailable = readiness.structuredParserAvailable;
  const installed = localDependencyInstalled();
  const installedFilesReady = Boolean(
    installed
    && readiness.structuredParserInstalled
    && readiness.formulaModelsAvailable
  );
  try {
    const service = await findRunningPdfBridge(settings, 2200);
    const bridgeReady = Boolean(service?.bridgeReady);
    const completeReady = bridgeReady && readiness.complete;
    if (bridgeReady && service?.baseUrl) {
      activePdfMathBaseUrl = service.baseUrl;
    }
    return {
      ok: completeReady,
      installed: installedFilesReady,
      running: completeReady,
      downloadPaused: localDependencyDownloadPaused,
      pdfServiceReady: bridgeReady,
      pdfMathTranslateBaseUrl: service?.baseUrl || pdfMathBaseUrl(settings),
      structuredParserAvailable,
      formulaModelsAvailable: readiness.formulaModelsAvailable,
      readiness,
      liteMode: settings.localDependencyLiteMode,
      latencyMs: Date.now() - startedAt,
      label: "PaperSolver 本机依赖",
      message: completeReady
        ? "本机依赖运行正常。"
        : bridgeReady
          ? readiness.message || "本机依赖尚未准备完成。请重新准备本机依赖。"
          : service?.staleService
            ? "检测到不兼容的旧本机服务进程。请点击重新准备，本程序会自动清理后重启。"
            : installedFilesReady
              ? "本机依赖已安装。请点击启动本机依赖。"
              : "本机依赖尚未准备完成。请点击准备本机依赖。"
    };
  } catch {
    const logTail = await tailFile(localDependencyLogPath(), 1800);
    return {
      ok: false,
      installed: installedFilesReady,
      running: false,
      downloadPaused: localDependencyDownloadPaused,
      structuredParserAvailable,
      formulaModelsAvailable: readiness.formulaModelsAvailable,
      readiness,
      liteMode: settings.localDependencyLiteMode,
      latencyMs: Date.now() - startedAt,
      label: "PaperSolver 本机依赖",
      dependencyDir: localDependencyDir(),
      logPath: localDependencyLogPath(),
      logTail,
      message: installedFilesReady
        ? dependencyStatusMessageFromLog(logTail)
        : "未检测到本机依赖。请先准备本机依赖。"
    };
  }
}

async function getDirectorySize(dirPath, onProgress = null) {
  let size = 0;
  try {
    const files = await fs.promises.readdir(dirPath, { withFileTypes: true });
    for (const file of files) {
      const filePath = path.join(dirPath, file.name);
      if (file.isDirectory()) {
        size += await getDirectorySize(filePath, onProgress);
      } else if (file.isFile() && !file.name.endsWith('.tmp') && !file.name.endsWith('.part')) {
        const stat = await fs.promises.stat(filePath).catch(() => null);
        if (stat) {
          size += stat.size;
          onProgress?.(size);
        }
      }
    }
  } catch (err) {
    // dir might not exist yet
  }
  return size;
}

function runSpawnCommand(cmd, args, options, onLog) {
  return new Promise((resolve, reject) => {
    const spawnEnv = {
      UV_PYTHON_INSTALL_MIRROR: "https://mirror.nju.edu.cn/github-release/astral-sh/python-build-standalone/",
      UV_INDEX_URL: "https://pypi.tuna.tsinghua.edu.cn/simple",
      ...process.env,
      ...(options?.env || {})
    };
    const child = spawn(cmd, args, { ...options, env: spawnEnv });
    let errAccumulator = "";
    child.stdout.on("data", (chunk) => {
      if (onLog) onLog(chunk.toString());
    });
    child.stderr.on("data", (chunk) => {
      const str = chunk.toString();
      errAccumulator += str;
      if (onLog) onLog(str);
    });
    child.on("error", (err) => reject(err));
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(errAccumulator.trim() || `Command exited with code ${code}`));
    });
  });
}

async function downloadAndInstallLocalDependency(webContents, options = {}, downloadControl = null) {
  // Older/bootstrap packages still use the compatibility install path. Keep
  // the force flag defined there; an undefined reference used to surface only
  // after the download reached the final setup stage.
  const force = Boolean(options?.force);
  await prepareDependencyStorageLocation();
  const tempRoot = path.join(dependencyDownloadRoot(), `papersolver-dependency-${Date.now()}`);
  const bundledArchive = bundledLocalDependencyArchive();
  const installDir = localDependencyDir();
  const emit = (payload) => emitDependencyProgress(webContents, payload);
  let cachedArchives = [];
  await fs.promises.mkdir(tempRoot, { recursive: true });

  try {
    await appendLocalDependencyLog("开始准备本机依赖。");

    emit({ stage: "prepare", progress: 4, message: "正在准备 PaperSolver 本机依赖..." });
    const installPlan = await resolveLocalDependencyInstallPlan(tempRoot, bundledArchive);

    if (installPlan.mode === "manifest") {
      emit({ stage: "download", progress: 8, message: "正在连接本机依赖服务..." });
      cachedArchives = await installDependencyPackages(
        installPlan.packages,
        tempRoot,
        installDir,
        emit,
        downloadControl,
        {
          beforeExtract: async () => {
            emit({ stage: "replace", progress: 57, message: "正在替换旧本机依赖...", detail: "已完成下载与完整性校验，正在完成准备" });
            await stopLocalDependencyProcesses();
            await removeLocalDependencyDir(installDir);
            await fs.promises.mkdir(installDir, { recursive: true });
          }
        }
      );
      emit({ stage: "extract", progress: 82, message: "正在整理本机依赖...", detail: "正在完成环境准备" });
      await normalizeInstalledDependencyLayout(installDir);
      emit({ stage: "extract", progress: 84, message: "本机依赖准备完成", detail: "正在完成最后校验" });
    } else {
      const archivePath = installPlan.archivePath;
      const url = installPlan.url;
      if (installPlan.bundled) {
        emit({ stage: "prepare", progress: 8, message: "正在准备本机依赖..." });
      } else {
        emit({ stage: "download", progress: 6, message: "正在连接本机依赖服务..." });
        await downloadFileToPath(url, archivePath, (download) => {
          const progress = typeof download === "number" ? download : Number(download?.percent) || 0;
          emit({
            stage: "download",
            progress: Math.max(8, Math.min(42, Math.round(progress * 0.34 + 8))),
            message: `正在下载本机依赖 ${Math.round(progress)}%`,
            ...(typeof download === "object" && download ? download : {})
          });
        }, "本机依赖", downloadControl);
      }

      emit({ stage: "verify", progress: 45, message: "正在校验本机依赖完整性..." });
      const stat = await fs.promises.stat(archivePath);
      if (!stat.size || stat.size < 1024 * 10) {
        throw new Error("本机能力组件下载不完整，请稍后重试。");
      }
      await assertZipArchive(archivePath, "本机依赖");

      emit({ stage: "extract", progress: 50, message: "正在准备本机依赖..." });
      await stopLocalDependencyProcesses();
      await removeLocalDependencyDir(installDir);
      await fs.promises.mkdir(installDir, { recursive: true });
      await extractArchive(archivePath, installDir, ({ percent, extractedEntries, totalEntries, phase }) => {
        emit({
          stage: "extract",
          progress: Math.round(50 + Math.max(0, Math.min(100, percent || 0)) * 0.3),
          message: phase === "scan" ? "正在检查本机依赖..." : `正在配置本机依赖 ${Math.round(percent || 0)}%`,
          detail: totalEntries ? "正在完成环境配置" : "正在读取安装内容"
        });
      });
      await normalizeInstalledDependencyLayout(installDir);
    }

    const dependencyManifest = readLocalDependencyManifest();

    // --- Path resolutions for Python Virtual Env & UV ---
    const isWin = process.platform === "win32";
    const uvBin = path.join(installDir, "tools", isWin ? "uv.exe" : "uv");
    const venvDir = path.join(installDir, ".runtime-venv");
    const pythonBin = path.join(venvDir, isWin ? "Scripts" : "bin", isWin ? "python.exe" : "python");
    const pdfReadyMarker = path.join(venvDir, ".papersolver-pdf-ready");
    const structuredReadyMarker = path.join(venvDir, ".papersolver-structured-ready");

    // --- Phase 2: Setup Python & Install pip dependencies (15% - 50%) ---
    emit({ stage: "python-env", progress: 85, message: "正在自检运行环境..." });

    if (localDependencyOfflineReady(dependencyManifest)) {
      emit({ stage: "python-env", progress: 86, message: "正在检查 Python 运行环境...", detail: "确认内置运行时与 PDF 服务入口" });
      await validateOfflineLocalDependency(installDir, dependencyManifest, ({ stage, progress, detail }) => {
        const normalized = Math.max(0, Math.min(1, (Number(progress) - 66) / 17));
        emit({ stage: "python-env", progress: Math.round(86 + normalized * 6), message: stage, detail });
      });
      emit({ stage: "python-env", progress: 92, message: "本机依赖校验完成", detail: "正在完成最后配置" });
    } else {
      // Development compatibility path. Release packages should set offlineReady=true
      // and include the prepared runtime, services, parser and models.
      const liteMode = false;
      if (!fs.existsSync(pythonBin)) {
        emit({ stage: "python-env", progress: 86, message: "正在构建本地沙箱环境...", detail: "准备内置 Python 运行时" });
        await runSpawnCommand(uvBin, ["python", "install", "3.12"], { cwd: installDir });
        await runSpawnCommand(uvBin, ["venv", "--python", "3.12", ".runtime-venv"], { cwd: installDir });
      }
      if (force || !fs.existsSync(pdfReadyMarker) || !fs.existsSync(structuredReadyMarker)) {
        emit({ stage: "python-env", progress: 87, message: "正在安装 PDF 阅读组件...", detail: "第一阶段：PDF 服务与字体处理" });

        // Install PDF service requirements (using Tsinghua mirror for fast downloads in China)
        await runSpawnCommand(
          uvBin,
          ["pip", "install", "--python", pythonBin, "-r", path.join(installDir, "services", "pdf", "requirements.txt"),
           "--index-url", "https://pypi.tuna.tsinghua.edu.cn/simple",
           "--extra-index-url", "https://pypi.org/simple"],
          { cwd: installDir },
          (log) => {
            if (log.includes("Downloading") || log.includes("Installing")) {
              emit({ stage: "python-env", progress: 88, message: "正在配置 PDF 阅读能力...", detail: "正在安装运行组件" });
            }
          }
        );

        if (process.platform === "win32") {
          emit({ stage: "python-env", progress: 89, message: "正在部署公式计算组件...", detail: "第二阶段：辅助计算库" });
          await runSpawnCommand(
            uvBin,
            ["pip", "install", "--python", pythonBin, "torch", "torchvision", "--index-url", "https://download.pytorch.org/whl/cpu"],
            { cwd: installDir }
          );
        }

        emit({ stage: "python-env", progress: 90, message: "正在安装版面解析组件...", detail: "第三阶段：段落、图表与表格解析" });

        // Install Structured parser requirements (using Tsinghua mirror for fast downloads in China)
        await runSpawnCommand(
          uvBin,
          ["pip", "install", "--python", pythonBin, "-r", path.join(installDir, "services", "structured", "requirements.txt"),
           "--index-url", "https://pypi.tuna.tsinghua.edu.cn/simple",
           "--extra-index-url", "https://pypi.org/simple"],
          { cwd: installDir },
          (log) => {
            if (log.includes("Downloading") || log.includes("Installing")) {
              emit({ stage: "python-env", progress: 91, message: "正在配置版面解析能力...", detail: "正在安装解析组件" });
            }
          }
        );
      }
      emit({ stage: "python-env", progress: 92, message: "本机依赖配置完成", detail: "正在完成最后配置" });
    }

    emit({ stage: "models", progress: 93, message: "正在确认本机依赖...", detail: "正在完成最后配置" });

    // --- Phase 4: Write ready markers & Start local services (90% - 100%) ---
    emit({ stage: "start", progress: 94, message: "正在完成系统最后配置..." });
    await fs.promises.writeFile(pdfReadyMarker, new Date().toISOString());
    await fs.promises.writeFile(structuredReadyMarker, new Date().toISOString());

    emit({ stage: "start", progress: 95, message: "正在启动本机阅读服务...", detail: "正在等待 PDF 服务响应" });
    await startLocalDependencyServices({ waitForReady: true, webContents, progressBase: 95 });

    const status = await getLocalDependencyStatus();
    if (!status.running) {
      throw new Error(status.message || "本机能力已安装，但服务启动失败。");
    }

    emit({ stage: "done", progress: 100, message: "本机依赖已准备完成！" });
    await Promise.all(cachedArchives.map((archivePath) => fs.promises.rm(archivePath, { force: true }).catch(() => {})));
    return { ok: true, ...status };

  } catch (error) {
    if (downloadControl?.paused || isDependencyDownloadPausedError(error)) {
      throw createDependencyDownloadPausedError();
    }
    await appendLocalDependencyLog(`本机能力准备失败：${error?.stack || error?.message || String(error)}`);
    const logTail = await tailFile(localDependencyLogPath(), 2200);
    const detail = logTail ? `\n\n最近日志：\n${logTail.slice(-1200)}` : "";
    throw new Error(`${error?.message || "本机能力处理失败"}${detail}`);
  } finally {
    await fs.promises.rm(tempRoot, { recursive: true, force: true }).catch(() => {});
  }
}

async function openLocalDependencyLog() {
  const logPath = localDependencyLogPath();
  await fs.promises.mkdir(path.dirname(logPath), { recursive: true });
  if (!fs.existsSync(logPath)) {
    await fs.promises.writeFile(logPath, "PaperSolver 本机依赖日志尚未产生。\n", "utf8");
  }
  const error = await shell.openPath(logPath);
  if (error) throw new Error(error);
  return { ok: true, path: logPath };
}

async function stopLocalDependencyProcesses() {
  for (const child of localDependencyProcesses.values()) {
    try {
      if (process.platform === "win32" && child.pid) {
        await runCommandCapture("taskkill.exe", ["/PID", String(child.pid), "/T", "/F"]);
      } else {
        child.kill();
      }
    } catch {}
  }
  localDependencyProcesses.clear();
  if (process.platform === "win32") {
    await stopWindowsDependencyProcesses();
    const pids = [...new Set([
      ...(await windowsListeningPids(11008)),
      ...(await windowsListeningPids(11009)),
      ...(await windowsListeningPids(11010))
    ])];
    for (const pid of pids) {
      try {
        await runCommandCapture("taskkill.exe", ["/PID", String(pid), "/T", "/F"]);
      } catch {}
    }
    await new Promise((resolve) => setTimeout(resolve, 1200));
  } else {
    // The dependency service is detached so it can survive a desktop restart.
    // When the packaged server.py changes, the child is no longer in our
    // in-memory process map; explicitly stop only PaperSolver's local ports
    // so the next start cannot silently reuse the old Python module.
    const pids = new Set();
    for (const port of [11008, 11009, 11010]) {
      try {
        const output = execFileSync("lsof", [`-tiTCP:${port}`, "-sTCP:LISTEN"], {
          encoding: "utf8",
          stdio: ["ignore", "pipe", "ignore"]
        });
        for (const value of String(output || "").split(/\s+/)) {
          const pid = Number(value);
          if (Number.isInteger(pid) && pid > 0) pids.add(pid);
        }
      } catch {}
    }
    for (const pid of pids) {
      try { process.kill(pid, "SIGTERM"); } catch {}
    }
    if (pids.size) await new Promise((resolve) => setTimeout(resolve, 800));
  }
}

async function stopWindowsDependencyProcesses() {
  if (process.platform !== "win32") return;
  // Match the actual per-user dependency directory. The old hard-coded
  // papersolver-desktop path only existed in development and missed installed
  // runtimes such as D:\PaperSolverData\dependencies.
  const dependencyNeedle = localDependencyDir().replaceAll("/", "\\\\").toLowerCase().replaceAll("'", "''");
  const script = [
    `$needle = '${dependencyNeedle}'`,
    "Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine.ToLower().Contains($needle) } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"
  ].join("; ");
  try {
    await runCommandCapture("powershell.exe", ["-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", script]);
  } catch {}
}

async function removeLocalDependencyDir(installDir) {
  let lastError = null;
  for (let attempt = 0; attempt < 4; attempt += 1) {
    try {
      await fs.promises.rm(installDir, { recursive: true, force: true });
      return;
    } catch (error) {
      lastError = error;
      if (!/EBUSY|EPERM|EACCES/i.test(String(error?.code || ""))) throw error;
      await stopLocalDependencyProcesses();
      await new Promise((resolve) => setTimeout(resolve, 700 * (attempt + 1)));
    }
  }
  if (lastError) {
    throw new Error("旧本机能力仍被 Windows 占用。请完全退出 PaperSolver 和黑色终端后重试；如果仍失败，请重启 Windows 后再点击重新准备本机能力。");
  }
}

function runCommandCapture(binary, args = []) {
  return new Promise((resolve, reject) => {
    const child = execFile(binary, args, { windowsHide: true }, (error, stdout = "", stderr = "") => {
      if (error) {
        reject(new Error(textValue(stderr || stdout || error.message)));
        return;
      }
      resolve(textValue(stdout));
    });
    child.unref?.();
  });
}

async function windowsListeningPids(port) {
  if (process.platform !== "win32") return [];
  try {
    const output = await runCommandCapture("netstat.exe", ["-ano", "-p", "tcp"]);
    const pids = new Set();
    for (const line of output.split(/\r?\n/)) {
      const columns = line.trim().split(/\s+/);
      if (columns.length < 5 || columns[0].toUpperCase() !== "TCP") continue;
      const localAddress = columns[1] || "";
      const state = columns[3] || "";
      if (state.toUpperCase() !== "LISTENING" || !localAddress.endsWith(`:${port}`)) continue;
      const pid = Number(columns[4]);
      if (pid > 0) pids.add(pid);
    }
    return [...pids];
  } catch {
    return [];
  }
}

function dependencyStatusMessageFromLog(logTail) {
  const text = textValue(logTail);
  if (/No module named|ModuleNotFoundError|DistributionNotFound/i.test(text)) {
    return "本机能力缺少必要组件。请重新准备本机能力。";
  }
  if (/timed out|timeout|ReadTimeout|ConnectTimeout/i.test(text)) {
    return "本机能力初始化较慢。请稍后重新检测。";
  }
  if (/Permission denied|operation not permitted/i.test(text)) {
    return "本机能力没有执行权限。请重新准备本机能力，或检查系统安全设置。";
  }
  return "已检测到本机能力，但服务暂未运行。请点击启动本机能力。";
}

function emitDependencyProgress(webContents, payload) {
  if (!webContents || webContents.isDestroyed()) return;
  webContents.send("desktop:local-dependency-progress", {
    progress: 0,
    message: "",
    stage: "idle",
    ...payload
  });
}

function localDependencyDownloadUrl() {
  const explicit = textValue(process.env.PAPER_SOLVER_DEPENDENCY_URL);
  if (explicit) return explicit;
  return "";
}

function localDependencyManifestUrl() {
  return textValue(process.env.PAPER_SOLVER_DEPENDENCY_MANIFEST) || DEFAULT_DEPENDENCY_MANIFEST_URL;
}

async function resolveLocalDependencyInstallPlan(tempRoot, bundledArchive) {
  if (bundledArchive) {
    return { mode: "legacy", bundled: true, archivePath: bundledArchive, url: "" };
  }
  const manifestUrl = localDependencyManifestUrl();
  if (manifestUrl) {
    try {
      const manifest = await requestJson(manifestUrl, { timeoutMs: 10000 });
      const packages = dependencyPackagesForCurrentPlatform(manifest);
      if (packages.length) {
        return { mode: "manifest", packages };
      }
      throw new Error("未找到适用于当前设备的本机能力组件。");
    } catch (error) {
      logTranslation(`Dependency manifest unavailable: ${error?.message || error}`);
    }
  }
  const url = localDependencyDownloadUrl();
  if (!url) {
    throw new Error("当前版本暂未获取本机能力安装清单，请稍后重试或联系管理员。");
  }
  return {
    mode: "legacy",
    bundled: false,
    archivePath: path.join(tempRoot, path.basename(new URL(url).pathname) || "papersolver-dependency.zip"),
    url
  };
}

function dependencyPackagesForCurrentPlatform(manifest = {}) {
  const platform = process.platform === "darwin" ? "macos" : process.platform === "win32" ? "windows" : "linux";
  const arch = process.arch === "arm64" ? "arm64" : "x64";
  const target = `${platform}-${arch}`;
  const packages = Array.isArray(manifest.packages) ? manifest.packages : [];
  return packages
    .filter((item) => {
      if (!item || item.enabled === false) return false;
      if (Array.isArray(item.platforms) && item.platforms.length) {
        return item.platforms.map(String).includes(target) || item.platforms.map(String).includes(platform) || item.platforms.map(String).includes("all");
      }
      if (item.platform && ![target, platform, "all"].includes(String(item.platform))) {
        return false;
      }
      if (item.arch && ![arch, "all"].includes(String(item.arch))) {
        return false;
      }
      return true;
    })
    .map((item, index) => ({
      id: textValue(item.id) || `component-${index + 1}`,
      urls: Array.isArray(item.urls) ? item.urls.map(textValue).filter(Boolean) : [textValue(item.url)].filter(Boolean),
      sha256: textValue(item.sha256),
      size: Number(item.size) || 0,
      weight: Number(item.weight) || Number(item.size) || 1
    }))
    .filter((item) => item.urls.length);
}

async function installDependencyPackages(packages, tempRoot, installDir, emit, downloadControl = null, options = {}) {
  const totalWeight = packages.reduce((sum, item) => sum + Math.max(1, item.weight), 0);
  const downloadRoot = dependencyDownloadRoot();
  await fs.promises.mkdir(downloadRoot, { recursive: true });
  const preparedPackages = [];
  let completedWeight = 0;
  for (const item of packages) {
    const label = dependencyProductLabel(item.id);
    const cacheKey = `${safeCacheKey(item.id) || randomUUID()}-${(item.sha256 || "package").slice(0, 12)}`;
    const archivePath = path.join(downloadRoot, `${cacheKey}.zip`);
    const reportDownload = (download) => {
      const progress = typeof download === "number" ? download : Number(download?.percent) || 0;
      const weighted = ((completedWeight + Math.max(1, item.weight) * (progress / 100)) / totalWeight) * 46;
      emit({
        stage: "download",
        progress: Math.max(10, Math.min(56, Math.round(10 + weighted))),
        message: `正在下载${label} ${Math.round(progress)}%`,
        ...(typeof download === "object" && download ? download : {})
      });
    };
    let verified = false;
    const cachedSize = await fs.promises.stat(archivePath).then((stat) => stat.size).catch(() => 0);
    if (cachedSize && (!item.size || cachedSize === item.size)) {
      try {
        await assertZipArchive(archivePath, label);
        if (item.sha256) await assertFileSha256(archivePath, item.sha256, label);
        verified = true;
        reportDownload({
          percent: 100,
          receivedBytes: cachedSize,
          totalBytes: item.size || cachedSize,
          bytesPerSecond: 0,
          etaSeconds: 0,
          detail: "已复用上次完整校验的下载文件"
        });
      } catch {
        await fs.promises.rm(archivePath, { force: true }).catch(() => {});
      }
    }
    for (let integrityAttempt = 1; !verified && integrityAttempt <= 2; integrityAttempt += 1) {
      await downloadDependencyPackage(item, archivePath, reportDownload, downloadControl);
      try {
        emit({ stage: "verify", progress: Math.min(57, Math.round(10 + ((completedWeight + Math.max(1, item.weight)) / totalWeight) * 46)), message: `正在校验${label}...` });
        await assertZipArchive(archivePath, label);
        if (item.sha256) {
          await assertFileSha256(archivePath, item.sha256, label);
        }
        verified = true;
        break;
      } catch (error) {
        await fs.promises.rm(archivePath, { force: true }).catch(() => {});
        if (integrityAttempt >= 2) throw error;
        emit({
          stage: "download",
          progress: Math.max(10, Math.round(10 + (completedWeight / totalWeight) * 46)),
          message: `${label}校验未通过，正在自动重新下载...`,
          detail: "已清除损坏的下载缓存"
        });
      }
    }
    if (!verified) {
      throw new Error(`${label}完整性校验失败`);
    }
    preparedPackages.push({ item, label, archivePath });
    completedWeight += Math.max(1, item.weight);
  }

  await options.beforeExtract?.();
  let extractedWeight = 0;
  for (const prepared of preparedPackages) {
    const itemWeight = Math.max(1, prepared.item.weight);
    emit({ stage: "extract", progress: Math.round(58 + (extractedWeight / totalWeight) * 24), message: `正在配置${prepared.label}...`, detail: "正在扫描组件文件" });
    await extractArchive(prepared.archivePath, installDir, ({ percent, extractedEntries, totalEntries, phase }) => {
      const fraction = Math.max(0, Math.min(100, Number(percent) || 0)) / 100;
      emit({
        stage: "extract",
        progress: Math.round(58 + ((extractedWeight + itemWeight * fraction) / totalWeight) * 24),
        message: phase === "scan" ? `正在扫描${prepared.label}...` : `正在配置${prepared.label} ${Math.round(fraction * 100)}%`,
        detail: totalEntries ? `已释放 ${extractedEntries || 0} / ${totalEntries} 个组件文件` : "正在读取组件目录"
      });
    });
    extractedWeight += itemWeight;
  }
  return preparedPackages.map((prepared) => prepared.archivePath);
}

async function downloadDependencyPackage(item, archivePath, onProgress, downloadControl = null) {
  let lastError = null;
  for (const url of item.urls) {
    try {
      await downloadFileToPath(url, archivePath, onProgress, dependencyProductLabel(item.id), downloadControl);
      return;
    } catch (error) {
      if (downloadControl?.paused || isDependencyDownloadPausedError(error)) throw error;
      lastError = error;
    }
  }
  throw lastError || new Error(`${dependencyProductLabel(item.id)}安装失败`);
}

function dependencyProductLabel(id) {
  const normalized = String(id || "").toLowerCase();
  if (normalized.includes("runtime")) return "本机阅读环境";
  if (normalized.includes("layout")) return "版面解析能力";
  if (normalized.includes("ocr")) return "文字识别能力";
  if (normalized.includes("mfr") || normalized.includes("formula")) return "公式识别能力";
  if (normalized.includes("pdf")) return "PDF 阅读能力";
  return "本机能力组件";
}

function bundledLocalDependencyArchive() {
  const platform = process.platform === "darwin" ? "macos" : process.platform === "win32" ? "windows" : "linux";
  const arch = process.arch === "arm64" ? "arm64" : "x64";
  const fileName = `papersolver-local-dependency-${platform}-${arch}.zip`;
  const candidates = [
    isPackaged ? path.join(process.resourcesPath, "dependencies", fileName) : "",
    path.join(__dirname, "..", "release", "dependencies", fileName),
    path.join(__dirname, "..", "dependencies", fileName)
  ].filter(Boolean);
  return candidates.find((candidate) => fs.existsSync(candidate)) || "";
}

function localDependencyDir() {
  const configured = readConfiguredDependencyDir();
  if (configured) return configured;
  if (process.platform === "win32") return defaultWindowsDependencyDir();
  return path.join(app.getPath("userData"), "dependencies");
}

function readConfiguredDependencyDir() {
  try {
    const settings = JSON.parse(fs.readFileSync(settingsPath(), "utf8"));
    const configured = String(settings?.localDependencyDir || "").trim();
    return configured ? path.resolve(configured) : "";
  } catch {
    return "";
  }
}

function defaultWindowsDependencyDir() {
  const drive = findWindowsDataDrive();
  return drive
    ? path.join(`${drive}:\\`, "PaperSolverData", "dependencies")
    : path.join(app.getPath("userData"), "dependencies");
}

function findWindowsDataDrive() {
  if (process.platform !== "win32") return "";
  try {
    const output = execFileSync(
      "powershell.exe",
      ["-NoProfile", "-NonInteractive", "-Command", "$min=12GB; Get-CimInstance Win32_LogicalDisk -Filter \"DriveType=3\" | Where-Object { $_.DeviceID -ne 'C:' -and $_.FreeSpace -gt $min } | Sort-Object DeviceID | Select-Object -First 1 -ExpandProperty DeviceID"],
      { encoding: "utf8", windowsHide: true, timeout: 5000 }
    );
    const match = String(output || "").trim().match(/^([A-Z]):/im);
    return match ? match[1].toUpperCase() : "";
  } catch {
    return "";
  }
}

function dependencyDownloadRoot() {
  return path.join(path.dirname(localDependencyDir()), "dependency-downloads");
}

async function prepareDependencyStorageLocation() {
  if (process.platform !== "win32") return localDependencyDir();
  const legacyDir = path.join(app.getPath("userData"), "dependencies");
  const targetDir = localDependencyDir();
  const legacyDownloadRoot = path.join(app.getPath("userData"), "dependency-downloads");
  const targetDownloadRoot = dependencyDownloadRoot();
  if (path.resolve(legacyDir).toLowerCase() === path.resolve(targetDir).toLowerCase()) return targetDir;
  const legacyExists = fs.existsSync(legacyDir);
  const targetExists = fs.existsSync(targetDir);
  if (legacyExists && !targetExists) {
    await fs.promises.mkdir(path.dirname(targetDir), { recursive: true });
    await fs.promises.cp(legacyDir, targetDir, { recursive: true, force: true });
    await fs.promises.rm(legacyDir, { recursive: true, force: true });
    await appendLocalDependencyLog(`已将本机依赖从 C 盘迁移到 ${targetDir}。`);
  }
  if (fs.existsSync(legacyDownloadRoot) && !fs.existsSync(targetDownloadRoot)) {
    await fs.promises.mkdir(path.dirname(targetDownloadRoot), { recursive: true });
    await fs.promises.cp(legacyDownloadRoot, targetDownloadRoot, { recursive: true, force: true });
    await fs.promises.rm(legacyDownloadRoot, { recursive: true, force: true });
    await appendLocalDependencyLog(`已将依赖下载缓存从 C 盘迁移到 ${targetDownloadRoot}。`);
  }
  if (!readConfiguredDependencyDir()) writeDesktopSettings({ localDependencyDir: targetDir });
  await fs.promises.mkdir(targetDir, { recursive: true });
  return targetDir;
}

function localDependencyModelDir() {
  const manifest = readLocalDependencyManifest();
  const modelPath = textValue(manifest?.models?.path) || "modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models";
  return path.resolve(localDependencyDir(), modelPath);
}

function localDependencyInstalled() {
  const root = localDependencyDir();
  return Boolean(
    fs.existsSync(path.join(root, "papersolver-dependency.json"))
    || fs.existsSync(path.join(root, ".papersolver-dependency-installed"))
    || desktopStructuredParserBinary()
    || findExecutableInDir(root, ["papersolver-dependency", "start-papersolver-dependency", "pdfmath", "pdf2zh"])
  );
}

async function getLocalDependencyReadiness(options = {}) {
  await prepareDependencyStorageLocation();
  const now = Date.now();
  if (cachedLocalDependencyReadiness?.complete && now - cachedLocalDependencyReadinessAt < 5 * 60 * 1000) {
    return cachedLocalDependencyReadiness;
  }
  const root = localDependencyDir();
  await ensureFastTextLanguageModel(root);
  const manifest = readLocalDependencyManifest();
  const isWin = process.platform === "win32";
  const parserBinary = desktopStructuredParserBinary();
  const structuredParserInstalled = Boolean(parserBinary && offlineMineruAvailable(root, isWin));
  const models = manifest.models && typeof manifest.models === "object" ? manifest.models : {};
  const modelRoot = path.resolve(root, textValue(models.path) || "modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models");
  const minimumModelBytes = Number(models.minimumBytes) || 850 * 1024 * 1024;
  const modelRootExists = fs.existsSync(modelRoot);
  let modelBytes = 0;
  if (modelRootExists && minimumModelBytes > 0) {
    modelBytes = await getDirectorySize(modelRoot);
  }
  const formulaModelsAvailable = modelRootExists && (!minimumModelBytes || modelBytes >= minimumModelBytes);
  const parserSmoke = structuredParserInstalled
    ? await runStructuredParserSmokeTest(parserBinary, options)
    : { ok: false, message: "本机解析器未安装" };
  const structuredParserAvailable = structuredParserInstalled && (parserSmoke.ok || parserSmoke.deferred);
  const complete = structuredParserAvailable && formulaModelsAvailable;
  let message = "";
  if (parserSmoke.deferred) {
    message = parserSmoke.message;
  } else if (!structuredParserAvailable) {
    message = parserSmoke.message || "本机解析器未完整安装。请重新准备本机能力。";
  } else if (!formulaModelsAvailable) {
    message = "公式识别模型不完整。请重新准备本机能力。";
  }
  const readiness = {
    complete,
    structuredParserAvailable,
    formulaModelsAvailable,
    structuredParserInstalled,
    parserSmoke,
    parserBinary,
    modelRoot,
    modelBytes,
    minimumModelBytes,
    message
  };
  if (readiness.complete) {
    cachedLocalDependencyReadiness = readiness;
    cachedLocalDependencyReadinessAt = now;
  }
  return readiness;
}

async function runStructuredParserSmokeTest(parserBinary, options = {}) {
  if (!parserBinary || !fs.existsSync(parserBinary)) {
    return { ok: false, message: "本机解析器启动入口不存在。请重新准备本机能力。" };
  }
  const runtimeRoot = path.join(localDependencyDir(), ".runtime-venv");
  const stdlibRoot = process.platform === "win32"
    ? path.join(runtimeRoot, "Lib")
    : path.join(runtimeRoot, "lib", "python3.12");
  const missingStdlib = ["encodings/__init__.py", "os.py"]
    .find((relativePath) => !fs.existsSync(path.join(stdlibRoot, relativePath)));
  if (missingStdlib) {
    return {
      ok: false,
      message: "本机 Python 标准库不完整（缺少 " + missingStdlib + "）。请重新下载本机依赖；当前安装包不完整，不能进行论文结构化解析。"
    };
  }
  const markerPath = path.join(localDependencyDir(), ".papersolver-parser-smoke-v4-ok");
  if (fs.existsSync(markerPath)) {
    return { ok: true, cached: true };
  }
  const preflight = await runStructuredParserRuntimePreflight(options);
  await appendLocalDependencyLog(`MinerU 运行时自检：${preflight.output || preflight.error || "无输出"}`);
  if (!preflight.ok) {
    const rawError = `${preflight.error || ""} ${preflight.output || ""}`;
    const windowsRuntimeMissing = process.platform === "win32"
      && /(WinError 126|c10\.dll|msvcp140|vcruntime140|Visual C\+\+|DLL load failed)/i.test(rawError);
    return {
      ok: false,
      message: windowsRuntimeMissing
        ? "Windows 原生运行库加载失败：请重新准备本机依赖；若仍失败，请安装 Microsoft Visual C++ 2015-2022 Redistributable x64 后重试。"
        : `本机解析运行组件自检失败：${preflight.error || "无法导入 MinerU pipeline 运行库"}`
    };
  }
  const smokeRoot = await fs.promises.mkdtemp(path.join(app.getPath("temp"), "papersolver-parser-smoke-"));
  const inputPath = path.join(smokeRoot, "papersolver-parser-smoke.pdf");
  const outputDir = path.join(smokeRoot, "output");
  try {
    const formulaSmokePdf = packagedFormulaSmokePdfPath();
    if (formulaSmokePdf) {
      await fs.promises.copyFile(formulaSmokePdf, inputPath);
    } else {
      const smokePdfBase64 = "JVBERi0xLjQKMSAwIG9iaiA8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4gZW5kb2JqCjIgMCBvYmogPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4gZW5kb2JqCjMgMCBvYmogPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAvUmVzb3VyY2VzIDw8IC9Gb250IDw8IC9GMSA0IDAgUiA+PiA+PiAvQ29udGVudHMgNSAwIFIgPj4gZW5kb2JqCjQgMCBvYmogPDwgL1R5cGUgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+IGVuZG9iago1IDAgb2JqIDw8IC9MZW5ndGggOTMgPj4gc3RyZWFtCkJUIC9GMSAxOCBUZiA3MiA3MjAgVGQgKEhlbGxvIFBhcGVyU29sdmVyLiBUaGlzIGlzIGEgZnVsbCBQREYgdHJhbnNsYXRpb24gc21va2UgdGVzdC4pIFRqIEVUCmVuZHN0cmVhbSBlbmRvYmoKeHJlZgowIDYKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAwMDAwMDExNSAwMDAwMCBuIAowMDAwMDAwMjQxIDAwMDAwIG4gCjAwMDAwMDAzMTEgMDAwMDAgbiAKdHJhaWxlciA8PCAvUm9vdCAxIDAgUiAvU2l6ZSA2ID4+CnN0YXJ0eHJlZgo0NTMKJSVFT0YK";
      await fs.promises.writeFile(inputPath, Buffer.from(smokePdfBase64, "base64"));
    }
    const result = await runSpawnProbe({
      binary: parserBinary,
      args: [
        "-p", inputPath,
        "-o", outputDir,
        "-b", "pipeline",
        "-f", "true",
        // The smoke PDF contains no tables. Disabling table recognition avoids
        // an unnecessary Windows OCR/table post-processing tail that can keep
        // MinerU alive after formula recognition has already succeeded.
        "-t", "false",
        "--client-side-output-generation", "true"
      ],
      cwd: localDependencyDir(),
      timeoutMs: structuredParserSmokeTimeoutMs(options.parserTimeoutMs),
      env: structuredParserEnv(),
      onProgress: options.onParserProgress,
      // On some Windows MinerU builds the client keeps the worker alive after
      // writing the result files. A valid content list is enough for readiness.
      successWhen: () => {
        const generated = findFileRecursive(outputDir, (filePath) => {
          const name = path.basename(filePath);
          return name.endsWith("_content_list.json") || name === "content_list.json";
        });
        if (!generated) return false;
        if (!formulaSmokePdf) return true;
        try {
          const parsed = JSON.parse(fs.readFileSync(generated, "utf8"));
          const blocks = Array.isArray(parsed)
            ? parsed
            : (Array.isArray(parsed?.content_list) ? parsed.content_list : []);
          return blocks.some((block) => {
            if (!/equation|formula/i.test(String(block?.type || block?.category || block?.block_type || ""))) return false;
            return Boolean(joinStructuredText(
              block?.latex,
              block?.text,
              block?.text_format,
              block?.formula,
              block?.equation,
              block?.content
            ));
          });
        } catch {
          return false;
        }
      },
      structuredParser: true
    });
    await appendLocalDependencyLog(`MinerU 真实 PDF 自检输出：\n${result.output || result.error || "无输出"}`);
    const contentList = findFileRecursive(outputDir, (filePath) => {
      const name = path.basename(filePath);
      return name.endsWith("_content_list.json") || name === "content_list.json";
    });
    let formulaVerified = false;
    if (contentList && formulaSmokePdf) {
      try {
        const parsed = JSON.parse(await fs.promises.readFile(contentList, "utf8"));
        const blocks = Array.isArray(parsed)
          ? parsed
          : (Array.isArray(parsed?.content_list) ? parsed.content_list : []);
        formulaVerified = blocks.some((block) => {
          if (!/equation|formula/i.test(String(block?.type || block?.category || block?.block_type || ""))) return false;
          // MinerU writes recognized LaTeX to `text` with text_format=latex.
          return Boolean(joinStructuredText(
            block?.latex,
            block?.text,
            block?.text_format,
            block?.formula,
            block?.equation,
            block?.content
          ));
        });
      } catch {}
    }
    // The generated structured output is authoritative. Some Windows MinerU
    // clients finish writing valid output before returning a non-zero code.
    if (contentList && (!formulaSmokePdf || formulaVerified)) {
      await fs.promises.writeFile(markerPath, new Date().toISOString(), "utf8").catch(() => {});
      return { ok: true };
    }
    const outputTree = await describeDirectoryTree(outputDir);
    const diagnosticRoot = await preserveParserSmokeDiagnostics(smokeRoot, result, outputTree);
    const reason = result.error || (contentList ? "没有生成可读取的公式文本" : "没有生成结构化内容");
    const detail = compactProcessOutput(result.output, 1400);
    if (result.error === "自检超时") {
      await appendLocalDependencyLog(`公式自检超时，已降级为延迟自检；诊断文件：${diagnosticRoot}`);
      return {
        ok: false,
        deferred: true,
        message: `公式自检耗时较长，已完成本机依赖安装，将在首次实际解析时继续验证。诊断文件：${diagnosticRoot}`
      };
    }
    return {
      ok: false,
      message: `本机解析器真实 PDF 与公式自检失败：${reason}${detail ? `；${detail}` : ""}。诊断文件：${diagnosticRoot}`
    };
  } finally {
    await fs.promises.rm(smokeRoot, { recursive: true, force: true }).catch(() => {});
  }
}

function structuredParserSmokeTimeoutMs(requestedTimeoutMs) {
  const platformMinimum = process.platform === "win32" ? 300000 : 120000;
  return Math.max(platformMinimum, Number(requestedTimeoutMs) || 0);
}

async function runStructuredParserRuntimePreflight(options = {}) {
  const root = localDependencyDir();
  const pythonBin = offlinePythonBinary(root, process.platform === "win32");
  if (!fs.existsSync(pythonBin)) {
    return { ok: false, error: "内置 Python 不存在", output: "" };
  }
  await installWindowsVcRuntimeIfAvailable(root);
  const script = [
    "import importlib.util, json, sys",
    "required = ['torch', 'torchvision', 'transformers', 'tokenizers', 'safetensors', 'pypdfium2', 'mineru']",
    "missing = [name for name in required if importlib.util.find_spec(name) is None]",
    "print(json.dumps({'python': sys.version.split()[0], 'missing': missing}))",
    "raise SystemExit(1 if missing else 0)"
  ].join("\n");
  return runSpawnProbe({
    binary: pythonBin,
    args: ["-c", script],
    cwd: root,
    // Importing the complete PyTorch/MinerU graph here can be held by Windows
    // Defender for a minute despite a healthy runtime. The following real PDF
    // smoke test is the authoritative native-library and formula validation.
    // A presence-only preflight must not import the complete PyTorch graph.
    // The real PDF smoke test below performs the authoritative import check.
    timeoutMs: Math.max(process.platform === "win32" ? 60000 : 20000, Number(options.parserTimeoutMs) || 0),
    env: structuredParserEnv()
  });
}

async function installWindowsVcRuntimeIfAvailable(root) {
  if (process.platform !== "win32") return { skipped: true, reason: "not-windows" };
  const installerPath = path.join(root, "tools", "vc_redist.x64.exe");
  if (!fs.existsSync(installerPath)) {
    await appendLocalDependencyLog("未找到随包 VC++ x64 运行库安装器，跳过 Windows 原生运行库自动修复。");
    return { skipped: true, reason: "missing-installer" };
  }
  const markerPath = path.join(root, ".papersolver-vc-redist-x64-ok");
  if (fs.existsSync(markerPath)) return { skipped: true, reason: "cached" };
  await appendLocalDependencyLog("开始安装随包 Microsoft Visual C++ 2015-2022 Redistributable x64。");
  const result = await runWindowsVcRedistInstaller(installerPath);
  await appendLocalDependencyLog(`VC++ x64 运行库安装结果：exitCode=${result.exitCode}; ${result.output || result.error || "无输出"}`);
  if (result.exitCode === 0 || result.exitCode === 3010) {
    await fs.promises.writeFile(markerPath, new Date().toISOString(), "utf8").catch(() => {});
  }
  return result;
}

function runWindowsVcRedistInstaller(installerPath) {
  return new Promise((resolve) => {
    let output = "";
    const child = spawn(installerPath, ["/install", "/quiet", "/norestart"], {
      cwd: path.dirname(installerPath),
      windowsHide: true,
      stdio: ["ignore", "pipe", "pipe"]
    });
    const finish = (payload) => resolve(payload);
    child.stdout.on("data", (chunk) => { output += chunk.toString(); });
    child.stderr.on("data", (chunk) => { output += chunk.toString(); });
    child.on("error", (error) => finish({ ok: false, exitCode: -1, error: error?.message || String(error), output: compactProcessOutput(output, 2000) }));
    child.on("close", (code) => {
      const exitCode = typeof code === "number" ? code : -1;
      finish({
        ok: exitCode === 0 || exitCode === 3010,
        exitCode,
        error: exitCode === 0 || exitCode === 3010 ? "" : `VC++ runtime installer exited with code ${exitCode}`,
        output: compactProcessOutput(output, 2000)
      });
    });
  });
}

async function preserveParserSmokeDiagnostics(smokeRoot, result, outputTree) {
  const diagnosticRoot = path.join(runtimeLogDirectory(), "parser-smoke-last");
  await fs.promises.rm(diagnosticRoot, { recursive: true, force: true }).catch(() => {});
  await fs.promises.cp(smokeRoot, diagnosticRoot, { recursive: true, force: true }).catch(() => {});
  await fs.promises.writeFile(
    path.join(diagnosticRoot, "parser-process.log"),
    `${result.output || result.error || "无进程输出"}\n\n生成目录：\n${outputTree || "(empty)"}\n`,
    "utf8"
  ).catch(() => {});
  await appendLocalDependencyLog(`MinerU 自检失败目录：${diagnosticRoot}\n生成目录：\n${outputTree || "(empty)"}`);
  return diagnosticRoot;
}

async function describeDirectoryTree(root, limit = 160) {
  if (!root || !fs.existsSync(root)) return "(empty)";
  const files = [];
  async function visit(current) {
    if (files.length >= limit) return;
    const entries = await fs.promises.readdir(current, { withFileTypes: true }).catch(() => []);
    for (const entry of entries) {
      if (files.length >= limit) break;
      const target = path.join(current, entry.name);
      if (entry.isDirectory()) await visit(target);
      else if (entry.isFile()) files.push(path.relative(root, target));
    }
  }
  await visit(root);
  return files.length ? files.join("\n") : "(empty)";
}

function compactProcessOutput(output, limit = 1200) {
  const normalized = String(output || "")
    .replace(/\x1b\[[0-9;]*[A-Za-z]/g, "")
    .replace(/\s+/g, " ")
    .trim();
  if (!normalized) return "";
  return normalized.slice(-Math.max(200, limit));
}

function structuredParserEnv() {
  const dependencyRoot = localDependencyDir();
  const venvRoot = path.join(dependencyRoot, ".runtime-venv");
  const torchLib = path.join(venvRoot, "Lib", "site-packages", "torch", "lib");
  const nativePath = process.platform === "win32"
    ? [dependencyRoot, venvRoot, torchLib, process.env.PATH || ""].filter(Boolean).join(path.delimiter)
    : process.env.PATH;
  return {
    MINERU_MODEL_SOURCE: process.env.PAPER_SOLVER_MODEL_SOURCE || "modelscope",
    MINERU_TOOLS_CONFIG_JSON: localDependencyMineruConfigPath(),
    // MPS is not reliable across the bundled PyTorch/MinerU combinations.
    // CPU is slower but makes the first-run self-test deterministic on Macs.
    MINERU_DEVICE_MODE: process.env.MINERU_DEVICE_MODE || "cpu",
    PAPER_SOLVER_MODELS_DIR: localDependencyModelDir(),
    MODELSCOPE_CACHE: path.join(localDependencyDir(), "modelscope-cache"),
    MODELSCOPE_HUB_FILE_LOCK: "false",
    FTLANG_CACHE: path.join(venvRoot, "Lib", "site-packages", "mineru", "resources", "fasttext-langdetect"),
    FASTTEXT_SMALL_MODEL_PATH: path.join(venvRoot, "Lib", "site-packages", "fast_langdetect", "ft_detect", "resources", "lid.176.ftz"),
    PYTHONHOME: path.join(localDependencyDir(), ".runtime-venv"),
    ...(nativePath ? { PATH: nativePath } : {}),
    MINERU_LOG_LEVEL: "INFO",
    PYTHONUNBUFFERED: "1",
    OMP_NUM_THREADS: "2",
    MKL_NUM_THREADS: "2",
    OPENBLAS_NUM_THREADS: "2",
    VECLIB_MAXIMUM_THREADS: "2",
    NUMEXPR_NUM_THREADS: "2"
  };
}

async function ensureFastTextLanguageModel(root = localDependencyDir()) {
  const packagedCandidates = [
    app.isPackaged ? path.join(process.resourcesPath, "compat", "fasttext", "lid.176.ftz") : "",
    path.join(app.getAppPath(), "dependency-service", "structured", "fasttext", "lid.176.ftz"),
    path.join(__dirname, "..", "dependency-service", "structured", "fasttext", "lid.176.ftz")
  ].filter(Boolean);
  const source = packagedCandidates.find((candidate) => {
    try { return fs.statSync(candidate).size >= 900000; } catch { return false; }
  });
  if (!source) return false;
  const sourceHash = await sha256File(source);
  const targets = [
    path.join(root, ".runtime-venv", "Lib", "site-packages", "fast_langdetect", "ft_detect", "resources", "lid.176.ftz"),
    path.join(root, ".runtime-venv", "Lib", "site-packages", "mineru", "resources", "fasttext-langdetect", "lid.176.ftz")
  ];
  let repaired = false;
  for (const target of targets) {
    const targetHash = await sha256File(target);
    if (targetHash !== sourceHash) {
      await fs.promises.mkdir(path.dirname(target), { recursive: true });
      await fs.promises.copyFile(source, target);
      repaired = true;
    }
  }
  const inferPath = path.join(root, ".runtime-venv", "Lib", "site-packages", "fast_langdetect", "ft_detect", "infer.py");
  if (fs.existsSync(inferPath)) {
    const sourceText = await fs.promises.readFile(inferPath, "utf8").catch(() => "");
    const fixedText = sourceText.replace(
      /LOCAL_SMALL_MODEL_PATH\s*=\s*Path\(__file__\)\.parent\s*\/\s*["']resources["']\s*\/\s*["']lid\.176\.ftz["']/, 
      'LOCAL_SMALL_MODEL_PATH = Path(os.getenv("FASTTEXT_SMALL_MODEL_PATH", str(Path(__file__).parent / "resources" / "lid.176.ftz")))'
    );
    if (fixedText !== sourceText) {
      await fs.promises.writeFile(inferPath, fixedText, "utf8");
      repaired = true;
    }
  }
  await appendLocalDependencyLog(repaired
    ? `已校验并修复 FastText 语言模型 lid.176.ftz（SHA256 ${sourceHash}）。`
    : `FastText 语言模型校验通过（SHA256 ${sourceHash}）。`);
  return true;
}

async function sha256File(filePath) {
  if (!filePath || !fs.existsSync(filePath)) return "";
  return new Promise((resolve) => {
    const hash = createHash("sha256");
    const stream = fs.createReadStream(filePath);
    stream.on("data", (chunk) => hash.update(chunk));
    stream.on("error", () => resolve(""));
    stream.on("end", () => resolve(hash.digest("hex")));
  });
}

function localDependencyMineruConfigPath(root = localDependencyDir()) {
  return path.join(root, "papersolver-mineru.json");
}

async function ensurePaperSolverMineruConfig(root) {
  const manifest = readLocalDependencyManifest();
  const modelPath = textValue(manifest?.models?.path) || "modelscope-cache/models/OpenDataLab/PDF-Extract-Kit-1___0/models";
  const modelRoot = path.resolve(root, modelPath);
  const config = {
    "model-source": process.env.PAPER_SOLVER_MODEL_SOURCE || "modelscope",
    "models-dir": {
      pipeline: path.dirname(modelRoot)
    },
    config_version: "papersolver-1"
  };
  const configPath = localDependencyMineruConfigPath(root);
  const next = `${JSON.stringify(config, null, 2)}\n`;
  const current = await fs.promises.readFile(configPath, "utf8").catch(() => "");
  if (current !== next) {
    await fs.promises.writeFile(configPath, next, "utf8");
    await fs.promises.rm(path.join(root, ".papersolver-parser-smoke-v3-ok"), { force: true }).catch(() => {});
    await appendLocalDependencyLog(`已固定 MinerU 模型目录：${config["models-dir"].pipeline}`);
  }
  return configPath;
}

function runSpawnProbe(command) {
  return new Promise((resolve) => {
    const requested = {
      binary: command.binary,
      args: command.args || [],
      cwd: command.cwd || localDependencyDir()
    };
    const normalized = command.structuredParser
      ? structuredParserSpawnCommand(requested)
      : normalizeLocalDependencySpawnCommand(requested);
    let settled = false;
    let output = "";
    let timer = null;
    let progressTimer = null;
    let successTimer = null;
    const child = spawn(normalized.binary, normalized.args || [], {
      cwd: normalized.cwd || command.cwd || localDependencyDir(),
      env: {
        ...process.env,
        ...(command.env || {})
      },
      windowsHide: true,
      stdio: ["ignore", "pipe", "pipe"]
    });
    const finish = (payload) => {
      if (settled) return;
      settled = true;
      if (timer) clearTimeout(timer);
      if (progressTimer) clearInterval(progressTimer);
      if (successTimer) clearInterval(successTimer);
      resolve(payload);
    };
    const startedAt = Date.now();
    progressTimer = setInterval(() => {
      command.onProgress?.(Math.max(1, Math.round((Date.now() - startedAt) / 1000)));
    }, 2000);
    if (typeof command.successWhen === "function") {
      successTimer = setInterval(() => {
        try {
          if (!command.successWhen()) return;
          const snapshot = compactProcessOutput(output, 8000);
          terminateProcessTree(child);
          finish({ ok: true, exitCode: 0, output: snapshot, earlySuccess: true });
        } catch {}
      }, 1000);
    }
    timer = setTimeout(() => {
      terminateProcessTree(child);
      finish({ ok: false, exitCode: -1, error: "自检超时", output: compactProcessOutput(output, 8000) });
    }, Math.max(1000, Number(command.timeoutMs) || 12000));
    child.stdout.on("data", (chunk) => { output += chunk.toString(); });
    child.stderr.on("data", (chunk) => { output += chunk.toString(); });
    child.on("error", (error) => finish({ ok: false, exitCode: -1, error: error?.message || String(error), output: compactProcessOutput(output, 8000) }));
    child.on("close", (code) => {
      const exitCode = typeof code === "number" ? code : -1;
      const text = compactProcessOutput(output, 8000);
      finish({
        ok: exitCode === 0,
        exitCode,
        error: exitCode === 0 ? "" : text || `退出码 ${exitCode}`,
        output: text
      });
    });
  });
}

function terminateProcessTree(child) {
  if (!child?.pid) return;
  if (process.platform === "win32") {
    execFile("taskkill.exe", ["/PID", String(child.pid), "/T", "/F"], { windowsHide: true }, () => {});
    return;
  }
  try { child.kill("SIGTERM"); } catch {}
}

function structuredParserSpawnCommand(command) {
  const root = localDependencyDir();
  const parserBinary = path.resolve(String(command?.binary || ""));
  const dependencyRoot = `${path.resolve(root)}${path.sep}`;
  const pythonBin = offlinePythonBinary(root, process.platform === "win32");
  const clientPath = path.join(localDependencySitePackages(root, process.platform === "win32"), "mineru", "cli", "client.py");
  if (parserBinary.startsWith(dependencyRoot) && fs.existsSync(pythonBin) && fs.existsSync(clientPath)) {
    return {
      ...command,
      binary: pythonBin,
      args: ["-m", "mineru.cli.client", ...(command.args || [])],
      cwd: root
    };
  }
  return normalizeLocalDependencySpawnCommand(command);
}

function readLocalDependencyManifest() {
  const manifestPath = path.join(localDependencyDir(), "papersolver-dependency.json");
  try {
    return JSON.parse(fs.readFileSync(manifestPath, "utf8"));
  } catch {
    return {};
  }
}

function localDependencyOfflineReady(manifest = {}) {
  return manifest.offlineReady === true || String(manifest.installMode || "").toLowerCase() === "offline";
}

async function validateOfflineLocalDependency(root, manifest = {}, onProgress = null) {
  const isWin = process.platform === "win32";
  const pythonBin = offlinePythonBinary(root, isWin);
  onProgress?.({ stage: "正在确认 Python 运行时", progress: 66, detail: "内置 Python 文件已找到" });
  if (!fs.existsSync(pythonBin)) {
    throw new Error("本机阅读环境不完整，请重新安装本机能力。");
  }

  const service = Array.isArray(manifest.services)
    ? manifest.services.find((item) => item && (item.id === "pdfmath" || item.default !== false))
    : null;
  const serviceCommand = textValue(service?.command);
  if (!serviceCommand || !fs.existsSync(path.resolve(root, serviceCommand))) {
    throw new Error("本机阅读能力不完整，请重新安装本机能力。");
  }
  onProgress?.({ stage: "正在确认 PDF 服务入口", progress: 69, detail: "PDF 服务启动文件已找到" });

  const capabilities = Array.isArray(manifest.requiredCapabilities)
    ? manifest.requiredCapabilities.map((item) => String(item || "").toLowerCase())
    : ["pdf2", "structured", "formula"];
  const needsStructured = capabilities.some((item) => ["structured", "layout", "ocr", "formula", "mfr"].includes(item));
  const needsFormula = capabilities.some((item) => ["formula", "mfr"].includes(item));
  if (needsStructured) {
    const parserCommand = textValue(manifest?.structuredParser?.command);
    const parserPath = parserCommand ? path.resolve(root, parserCommand) : "";
    if (!parserPath || !fs.existsSync(parserPath)) {
      throw new Error("本机解析能力不完整，请重新安装本机能力。");
    }
    if (!offlineMineruAvailable(root, isWin)) {
      throw new Error("本机解析能力不完整，请重新安装本机能力。");
    }
    const missingRuntime = missingStructuredParserRuntimePackages(root, isWin);
    if (missingRuntime.length) {
      throw new Error(`本机解析运行组件不完整（缺少 ${missingRuntime.join("、")}）。请安装最新版 PaperSolver 补全本机能力。`);
    }
    onProgress?.({ stage: "正在确认版面解析器", progress: 73, detail: "解析器文件已找到，准备检查公式模型" });
  }

  const models = manifest.models && typeof manifest.models === "object" ? manifest.models : {};
  const modelRoot = path.resolve(root, textValue(models.path) || "models");
  if (needsFormula && !fs.existsSync(modelRoot)) {
    throw new Error("公式识别能力不完整，请重新安装本机能力。");
  }
  const requiredModelPaths = Array.isArray(models.requiredPaths) ? models.requiredPaths.map(textValue).filter(Boolean) : [];
  for (const relativePath of requiredModelPaths) {
    if (!fs.existsSync(path.resolve(modelRoot, relativePath))) {
      throw new Error("本机解析能力不完整，请重新安装本机能力。");
    }
  }
  const minimumModelBytes = Number(models.minimumBytes) || (needsFormula ? 850 * 1024 * 1024 : 0);
  if (minimumModelBytes > 0) {
    let lastProgressAt = 0;
    const modelBytes = await getDirectorySize(modelRoot, (bytes) => {
      const now = Date.now();
      if (now - lastProgressAt < 180) return;
      lastProgressAt = now;
      const ratio = Math.max(0, Math.min(1, bytes / minimumModelBytes));
      onProgress?.({
        stage: "正在校验公式识别模型",
        progress: Math.round(74 + ratio * 9),
        detail: `已检查 ${formatBytes(bytes)} / 至少 ${formatBytes(minimumModelBytes)}`
      });
    });
    if (modelBytes < minimumModelBytes) {
      throw new Error("公式识别能力不完整，请重新安装本机能力。");
    }
  }
  return true;
}

function offlinePythonBinary(root, isWin) {
  const candidates = isWin
    ? [
      path.join(root, ".runtime-venv", "python.exe"),
      path.join(root, ".runtime-venv", "Scripts", "python.exe")
    ]
    : [
      path.join(root, ".runtime-venv", "bin", "python")
    ];
  return candidates.find((candidate) => fs.existsSync(candidate)) || candidates[0];
}

function offlineMineruAvailable(root, isWin) {
  const candidates = isWin
    ? [
      path.join(root, ".runtime-venv", "Scripts", "mineru.exe"),
      path.join(root, ".runtime-venv", "Lib", "site-packages", "mineru", "cli", "client.py")
    ]
    : [
      path.join(root, ".runtime-venv", "bin", "mineru")
    ];
  return candidates.some((candidate) => fs.existsSync(candidate));
}

function localDependencySitePackages(root, isWin) {
  if (isWin) return path.join(root, ".runtime-venv", "Lib", "site-packages");
  const libRoot = path.join(root, ".runtime-venv", "lib");
  let pythonDir = null;
  try {
    pythonDir = fs.readdirSync(libRoot, { withFileTypes: true }).find((entry) => {
      return entry.isDirectory() && /^python\d/i.test(entry.name);
    });
  } catch {}
  return pythonDir
    ? path.join(libRoot, pythonDir.name, "site-packages")
    : path.join(libRoot, "python3.12", "site-packages");
}

function missingStructuredParserRuntimePackages(root, isWin) {
  const sitePackages = localDependencySitePackages(root, isWin);
  return ["torch", "torchvision", "transformers", "tokenizers", "safetensors"].filter((packageName) => {
    return !fs.existsSync(path.join(sitePackages, packageName, "__init__.py"));
  });
}

function emitScaledDependencyProgress(webContents, payload = {}, progressBase = 0) {
  const rawProgress = Math.max(0, Math.min(100, Number(payload.progress) || 0));
  let progress = progressBase > 0
    ? Math.round(progressBase + (100 - progressBase) * (rawProgress / 100))
    : rawProgress;
  // Reserve 100% for the final ready event.
  if (payload.stage !== "done" && progress >= 100) progress = 99;
  emitDependencyProgress(webContents, { ...payload, progress });
}

async function startLocalDependencyServices({ waitForReady = false, webContents = null, progressBase = 0 } = {}) {
  const settings = readDesktopSettings();
  let baseUrl = pdfMathBaseUrl(settings);
  const servicePatched = await patchInstalledPdfDependencyService(localDependencyDir());
  await ensureMineruPdfTextCompatibility(localDependencyDir(), webContents, progressBase);
  emitScaledDependencyProgress(webContents, {
    stage: "start-check",
    progress: 35,
    message: "正在检测本机能力是否已运行..."
  }, progressBase);
  let runningBridge = await findRunningPdfBridge(settings, 1200);
  if (servicePatched && runningBridge?.bridgeReady) {
    await stopLocalDependencyProcesses();
    activePdfMathBaseUrl = "";
    runningBridge = { bridgeReady: false, reachable: false, staleService: false, baseUrl: "" };
  }
  if (runningBridge?.bridgeReady) {
    baseUrl = runningBridge.baseUrl;
    activePdfMathBaseUrl = baseUrl;
    const readiness = await getLocalDependencyReadiness({
      parserTimeoutMs: 90000,
      onParserProgress: (elapsedSeconds) => emitScaledDependencyProgress(webContents, {
        stage: "parser-smoke-test",
        progress: Math.min(98, 94 + Math.floor(elapsedSeconds / 25)),
        message: "正在首次启动版面解析器...",
        detail: `MinerU 正在加载运行组件，已等待 ${elapsedSeconds} 秒`
      }, progressBase)
    });
    if (!readiness.complete) {
      throw new Error(readiness.message || "本机能力不完整，请重新准备本机能力。");
    }
    emitScaledDependencyProgress(webContents, {
      stage: "done",
      progress: 100,
      message: "本机能力已运行。"
    }, progressBase);
    return { ok: true, alreadyRunning: true, running: true, baseUrl, readiness };
  }

  if (runningBridge?.staleService) {
    emitScaledDependencyProgress(webContents, {
      stage: "clear-stale-service",
      progress: 40,
      message: "正在清理旧版本本机服务...",
      detail: "发现占用本机服务端口的不兼容进程"
    }, progressBase);
    await stopLocalDependencyProcesses();
    activePdfMathBaseUrl = "";
  }

  baseUrl = await resolvePdfMathBaseUrlForStart(settings);
  activePdfMathBaseUrl = baseUrl;
  writeDesktopSettings({ pdfMathTranslateBaseUrl: baseUrl });
  const command = localDependencyStartCommand(baseUrl);
  if (!command?.binary) {
    throw new Error("未找到本机能力启动入口，请重新安装本机能力。");
  }
  if (!localDependencyProcesses.has(command.id)) {
    const logPath = nextLocalDependencyLogPath();
    await fs.promises.mkdir(path.dirname(logPath), { recursive: true }).catch(() => {});
    let logHandle = null;
    try {
      logHandle = await fs.promises.open(logPath, "a");
    } catch (error) {
      appendMainProcessLog("localDependencyLogOpen", error);
    }
    const spawnCommand = normalizeLocalDependencySpawnCommand(command);
    emitScaledDependencyProgress(webContents, {
      stage: "start-process",
      progress: 45,
      message: "正在启动本机能力进程..."
    }, progressBase);
    let child;
    try {
      const logTarget = logHandle ? logHandle.fd : "ignore";
      child = spawn(spawnCommand.binary, spawnCommand.args || [], {
        cwd: command.cwd || localDependencyDir(),
        detached: true,
        windowsHide: true,
        stdio: ["ignore", logTarget, logTarget],
        env: {
          ...process.env,
          PAPER_SOLVER_PDFMATH_BASE: baseUrl,
          PAPER_SOLVER_PORT: String(new URL(baseUrl).port || 11008),
          PAPER_SOLVER_DESKTOP_TRANSLATE_URL: `http://127.0.0.1:${LOCAL_CAPTURE_PORT}/translate`,
          PAPER_SOLVER_TRANSLATION_PROVIDER_FILE: path.join(localDependencyDir(), "translation-provider.json"),
          PAPER_SOLVER_MODELS_DIR: localDependencyModelDir(),
          MODELSCOPE_CACHE: path.join(localDependencyDir(), "modelscope-cache"),
          MODELSCOPE_HUB_FILE_LOCK: "false",
          PYTHONHOME: path.join(localDependencyDir(), ".runtime-venv"),
          MINERU_DEVICE_MODE: process.env.MINERU_DEVICE_MODE || "cpu",
          OMP_NUM_THREADS: "2",
          MKL_NUM_THREADS: "2",
          OPENBLAS_NUM_THREADS: "2",
          VECLIB_MAXIMUM_THREADS: "2",
          NUMEXPR_NUM_THREADS: "2"
        }
      });
    } finally {
      await logHandle?.close().catch(() => {});
    }
    child.unref();
    localDependencyProcesses.set(command.id, child);
    child.on("exit", () => localDependencyProcesses.delete(command.id));
  }
  if (waitForReady) {
    await waitForLocalDependencyReady(baseUrl, 90000, (payload) => {
      emitScaledDependencyProgress(webContents, payload, progressBase);
    });
  }
  const readiness = await getLocalDependencyReadiness({
    parserTimeoutMs: 90000,
    onParserProgress: (elapsedSeconds) => emitScaledDependencyProgress(webContents, {
      stage: "parser-smoke-test",
      progress: Math.min(98, 94 + Math.floor(elapsedSeconds / 25)),
      message: "正在首次启动版面解析器...",
      detail: `MinerU 正在加载运行组件，已等待 ${elapsedSeconds} 秒`
    }, progressBase)
  });
  if (!readiness.complete) {
    throw new Error(readiness.message || "本机能力不完整，请重新准备本机能力。");
  }
  emitScaledDependencyProgress(webContents, {
    stage: "done",
    progress: 100,
    message: "本机能力已启动。"
  }, progressBase);
  return { ok: true, started: true, running: true, baseUrl, readiness };
}

function normalizeLocalDependencySpawnCommand(command) {
  if (process.platform !== "win32") return command;
  const binary = String(command?.binary || "");
  if (!/\.(cmd|bat)$/i.test(binary)) return command;
  const args = Array.isArray(command.args) ? command.args.map(String) : [];
  return {
    ...command,
    binary: process.env.ComSpec || "cmd.exe",
    args: ["/d", "/c", "call", binary, ...args]
  };
}

function localDependencyStartCommand(baseUrl = DEFAULT_PDFMATH_BASE_URL) {
  const root = localDependencyDir();
  const directCommand = directPdfServiceCommand(root, baseUrl);
  if (directCommand) return directCommand;
  const manifest = readLocalDependencyManifest();
  const service = Array.isArray(manifest.services)
    ? manifest.services.find((item) => item && (item.id === "pdfmath" || item.default !== false))
    : null;
  if (service?.command) {
    const binary = path.resolve(root, service.command);
    return {
      id: textValue(service.id) || "papersolver-local-dependency",
      binary,
      args: Array.isArray(service.args) ? service.args.map(String) : [],
      cwd: service.cwd ? path.resolve(root, service.cwd) : root
    };
  }
  const binary = findExecutableInDir(root, [
    "start-papersolver-dependency",
    "papersolver-dependency",
    "papersolver-pdf-service",
    "pdfmath",
    "pdf2zh"
  ]);
  if (!binary) return null;
  return { id: "papersolver-local-dependency", binary, args: [], cwd: path.dirname(binary) };
}

function directPdfServiceCommand(root, baseUrl) {
  const serverPath = path.join(root, "services", "pdf", "server.py");
  if (!fs.existsSync(serverPath)) return null;
  const isWin = process.platform === "win32";
  const pythonCandidates = isWin
    ? [
      path.join(root, ".runtime-venv", "Scripts", "python.exe"),
      path.join(root, ".runtime-venv", "python.exe"),
      path.join(root, ".runtime-venv", "Scripts", "pythonw.exe"),
      path.join(root, ".runtime-venv", "pythonw.exe")
    ]
    : [
      path.join(root, ".runtime-venv", "bin", "python")
    ];
  const python = pythonCandidates.find((candidate) => fs.existsSync(candidate));
  if (!python) return null;
  const port = String(new URL(baseUrl).port || 11008);
  return {
    id: "pdfmath",
    binary: python,
    args: [serverPath, "--host", "127.0.0.1", "--port", port],
    cwd: root
  };
}

async function patchInstalledPdfDependencyService(root) {
  const targetPath = path.join(root, "services", "pdf", "server.py");
  if (!fs.existsSync(targetPath)) return false;
  const sourcePath = packagedPdfServiceTemplatePath();
  if (!sourcePath || !fs.existsSync(sourcePath)) return false;
  try {
    const next = await fs.promises.readFile(sourcePath, "utf8");
    if (!next.includes("PaperSolverGoogleTranslator") || !next.includes("PAPER_SOLVER_DESKTOP_TRANSLATE_URL")) {
      return false;
    }
    const current = await fs.promises.readFile(targetPath, "utf8").catch(() => "");
    if (current === next) return false;
    await fs.promises.copyFile(targetPath, `${targetPath}.bak`).catch(() => {});
    await fs.promises.writeFile(targetPath, next, "utf8");
    return true;
  } catch (error) {
    logTranslation(`Failed to patch local PDF dependency service: ${error?.message || error}`);
    return false;
  }
}

async function ensureMineruPdfTextCompatibility(root, webContents = null, progressBase = 0) {
  const runtimeRoot = path.join(root, ".runtime-venv");
  const sitePackageDirs = [path.join(runtimeRoot, "Lib", "site-packages")];
  const unixLibRoot = path.join(runtimeRoot, "lib");
  const pythonDirs = await fs.promises.readdir(unixLibRoot, { withFileTypes: true }).catch(() => []);
  for (const entry of pythonDirs) {
    if (entry.isDirectory() && /^python\d/i.test(entry.name)) {
      sitePackageDirs.push(path.join(unixLibRoot, entry.name, "site-packages"));
    }
  }
  await ensurePaperSolverMineruConfig(root);
  await ensureMineruPipelineMetadataCompatibility(sitePackageDirs);
  await ensureMineruApiClientCompatibility(sitePackageDirs);
  await ensureMineruFormulaRuntimeCompatibility(sitePackageDirs, webContents, progressBase);
  const installedVersions = sitePackageDirs.flatMap((sitePackages) => {
    if (!fs.existsSync(sitePackages)) return [];
    return fs.readdirSync(sitePackages, { withFileTypes: true }).filter((entry) => {
      return entry.isDirectory() && /^pdftext-[^-]+\.dist-info$/i.test(entry.name);
    }).map((entry) => entry.name.match(/^pdftext-([^-]+)\.dist-info$/i)?.[1] || "");
  }).filter(Boolean);
  const installedVersion = installedVersions.find((version) => version !== "0.6.3")
    || installedVersions[0]
    || "";
  if (!installedVersion) return false;
  if (installedVersions.every((version) => version === "0.6.3")) return true;

  await appendLocalDependencyLog(`检测到 pdftext ${installedVersion}，开始离线修复到 0.6.3。`);
  emitScaledDependencyProgress(webContents, {
    stage: "parser-compatibility",
    progress: 32,
    message: "正在修复版面解析兼容组件...",
    detail: `pdftext ${installedVersion} → 0.6.3（离线修复，不重新下载模型）`
  }, progressBase);
  const bundledCompatRoot = packagedPdfTextCompatibilityPath();
  if (bundledCompatRoot) {
    try {
      const bundledPackage = path.join(bundledCompatRoot, "pdftext");
      const bundledDistInfo = path.join(bundledCompatRoot, "pdftext-0.6.3.dist-info");
      for (const sitePackages of sitePackageDirs.filter((item) => fs.existsSync(item))) {
        const entries = await fs.promises.readdir(sitePackages, { withFileTypes: true });
        await fs.promises.rm(path.join(sitePackages, "pdftext"), { recursive: true, force: true });
        for (const entry of entries) {
          if (entry.isDirectory() && /^pdftext-[^-]+\.dist-info$/i.test(entry.name)) {
            await fs.promises.rm(path.join(sitePackages, entry.name), { recursive: true, force: true });
          }
        }
        await fs.promises.cp(bundledPackage, path.join(sitePackages, "pdftext"), { recursive: true, force: true });
        await fs.promises.cp(bundledDistInfo, path.join(sitePackages, "pdftext-0.6.3.dist-info"), { recursive: true, force: true });
      }
      await fs.promises.rm(path.join(root, ".papersolver-parser-smoke-v2-ok"), { force: true }).catch(() => {});
      await fs.promises.rm(path.join(root, ".papersolver-parser-smoke-v3-ok"), { force: true }).catch(() => {});
      await appendLocalDependencyLog("pdftext 0.6.3 离线修复完成，准备运行真实 PDF 自检。");
      return true;
    } catch (error) {
      await appendLocalDependencyLog(`pdftext 离线修复失败：${error?.stack || error?.message || String(error)}`);
      throw new Error(`版面解析兼容组件离线修复失败：${error?.message || String(error)}`);
    }
  }

  const isWin = process.platform === "win32";
  const uvBin = path.join(root, "tools", isWin ? "uv.exe" : "uv");
  const pythonBin = offlinePythonBinary(root, isWin);
  if (!fs.existsSync(uvBin) || !fs.existsSync(pythonBin)) {
    throw new Error(`版面解析兼容组件版本不正确（pdftext ${installedVersion}），且离线修复文件缺失。请安装最新版 PaperSolver。`);
  }
  await runSpawnCommand(uvBin, [
    "pip", "install",
    "--python", pythonBin,
    "pdftext==0.6.3",
    "--reinstall",
    "--no-deps",
    "--index-url", "https://pypi.tuna.tsinghua.edu.cn/simple",
    "--extra-index-url", "https://pypi.org/simple"
  ], { cwd: root });
  await fs.promises.rm(path.join(root, ".papersolver-parser-smoke-v2-ok"), { force: true }).catch(() => {});
  await fs.promises.rm(path.join(root, ".papersolver-parser-smoke-v3-ok"), { force: true }).catch(() => {});
  await appendLocalDependencyLog("pdftext 0.6.3 在线修复完成，准备运行真实 PDF 自检。");
  return true;
}

async function ensureMineruPipelineMetadataCompatibility(sitePackageDirs) {
  for (const sitePackages of sitePackageDirs.filter((item) => fs.existsSync(item))) {
    const entries = await fs.promises.readdir(sitePackages, { withFileTypes: true }).catch(() => []);
    for (const expected of [
      { packageName: "huggingface_hub", version: "0.36.0" },
      { packageName: "pypdfium2", version: "4.30.0" }
    ]) {
      const expectedName = `${expected.packageName}-${expected.version}.dist-info`;
      if (!fs.existsSync(path.join(sitePackages, expectedName))) continue;
      const pattern = new RegExp(`^${expected.packageName}-[^-]+\\.dist-info$`, "i");
      for (const entry of entries) {
        if (!entry.isDirectory() || !pattern.test(entry.name)) continue;
        if (entry.name.toLowerCase() === expectedName.toLowerCase()) continue;
        await fs.promises.rm(path.join(sitePackages, entry.name), { recursive: true, force: true });
        await appendLocalDependencyLog(`已清理不兼容的 ${entry.name}，固定 ${expected.packageName} ${expected.version}。`);
      }
    }
  }
}

async function ensureMineruApiClientCompatibility(sitePackageDirs) {
  const legacyStdinBlock = [
    "        if self._launch_mode == LOCAL_API_LAUNCH_MODE_SUBPROCESS:",
    "            stdin_target = subprocess.PIPE",
    "        else:",
    "            stdin_target = subprocess.DEVNULL"
  ].join("\n");
  const fixedStdinBlock = [
    "        if (",
    "            self._launch_mode == LOCAL_API_LAUNCH_MODE_SUBPROCESS",
    "            and self._use_stdin_shutdown_watcher",
    "        ):",
    "            stdin_target = subprocess.PIPE",
    "        else:",
    "            stdin_target = subprocess.DEVNULL"
  ].join("\n");
  for (const sitePackages of sitePackageDirs.filter((item) => fs.existsSync(item))) {
    const apiClientPath = path.join(sitePackages, "mineru", "cli", "api_client.py");
    if (!fs.existsSync(apiClientPath)) continue;
    const source = await fs.promises.readFile(apiClientPath, "utf8").catch(() => "");
    if (!source.includes(legacyStdinBlock)) continue;
    await fs.promises.writeFile(apiClientPath, source.replace(legacyStdinBlock, fixedStdinBlock), "utf8");
    await fs.promises.rm(path.join(localDependencyDir(), ".papersolver-parser-smoke-v3-ok"), { force: true }).catch(() => {});
    await appendLocalDependencyLog("已修复 MinerU Windows 临时 API 的 stdin 启动方式。");
  }
}

async function ensureMineruFormulaRuntimeCompatibility(sitePackageDirs, webContents = null, progressBase = 0) {
  const activeSitePackages = sitePackageDirs.filter((item) => fs.existsSync(item));
  const missingFormulaRuntime = activeSitePackages.some((sitePackages) => {
    return !fs.existsSync(path.join(sitePackages, "ftfy", "__init__.py"))
      || !fs.existsSync(path.join(sitePackages, "wcwidth", "__init__.py"));
  });
  if (!missingFormulaRuntime) return false;

  const bundledCompatRoot = packagedFormulaCompatibilityPath();
  if (!bundledCompatRoot) {
    throw new Error("公式识别运行组件缺失。请安装最新版 PaperSolver 后重试。");
  }
  emitScaledDependencyProgress(webContents, {
    stage: "formula-compatibility",
    progress: 30,
    message: "正在补全公式识别组件...",
    detail: "离线安装公式文本识别运行库，不重新下载模型"
  }, progressBase);
  await appendLocalDependencyLog("检测到公式识别运行库缺失，开始离线补全 ftfy 与 wcwidth。");
  try {
    for (const sitePackages of activeSitePackages) {
      for (const packageName of ["ftfy", "wcwidth"]) {
        if (!fs.existsSync(path.join(sitePackages, packageName, "__init__.py"))) {
          await fs.promises.cp(path.join(bundledCompatRoot, packageName), path.join(sitePackages, packageName), { recursive: true, force: true });
        }
      }
      const bundledMetadata = await fs.promises.readdir(bundledCompatRoot, { withFileTypes: true });
      for (const entry of bundledMetadata) {
        if (entry.isDirectory() && /^(ftfy|wcwidth)-[^/]+\.dist-info$/i.test(entry.name) && !fs.existsSync(path.join(sitePackages, entry.name))) {
          await fs.promises.cp(path.join(bundledCompatRoot, entry.name), path.join(sitePackages, entry.name), { recursive: true, force: true });
        }
      }
    }
    await fs.promises.rm(path.join(localDependencyDir(), ".papersolver-parser-smoke-v2-ok"), { force: true }).catch(() => {});
    await fs.promises.rm(path.join(localDependencyDir(), ".papersolver-parser-smoke-v3-ok"), { force: true }).catch(() => {});
    await appendLocalDependencyLog("公式识别运行组件离线补全完成。");
    return true;
  } catch (error) {
    await appendLocalDependencyLog(`公式识别运行组件补全失败：${error?.stack || error?.message || String(error)}`);
    throw new Error(`公式识别运行组件离线补全失败：${error?.message || String(error)}`);
  }
}

function packagedPdfTextCompatibilityPath() {
  const candidates = [
    app.isPackaged ? path.join(process.resourcesPath, "compat", "pdftext-0.6.3") : "",
    path.join(app.getAppPath(), "dependency-service", "structured", "pdftext-0.6.3"),
    path.join(__dirname, "..", "dependency-service", "structured", "pdftext-0.6.3")
  ].filter(Boolean);
  return candidates.find((candidate) => {
    return fs.existsSync(path.join(candidate, "pdftext", "pdf", "chars.py"))
      && fs.existsSync(path.join(candidate, "pdftext-0.6.3.dist-info", "METADATA"));
  }) || "";
}

function packagedFormulaCompatibilityPath() {
  const candidates = [
    app.isPackaged ? path.join(process.resourcesPath, "compat", "formula") : "",
    path.join(app.getAppPath(), "dependency-service", "structured", "formula-compat"),
    path.join(__dirname, "..", "dependency-service", "structured", "formula-compat")
  ].filter(Boolean);
  return candidates.find((candidate) => {
    return fs.existsSync(path.join(candidate, "ftfy", "__init__.py"))
      && fs.existsSync(path.join(candidate, "wcwidth", "__init__.py"));
  }) || "";
}

function packagedFormulaSmokePdfPath() {
  const compatRoot = packagedFormulaCompatibilityPath();
  const candidate = compatRoot ? path.join(compatRoot, "formula-smoke.pdf") : "";
  return candidate && fs.existsSync(candidate) ? candidate : "";
}

function packagedPdfServiceTemplatePath() {
  const candidates = [
    path.join(app.getAppPath(), "dependency-service", "pdf", "server.py"),
    path.join(__dirname, "..", "dependency-service", "pdf", "server.py"),
    path.join(__dirname, "..", "..", "desktop", "dependency-service", "pdf", "server.py")
  ];
  return candidates.find((candidate) => fs.existsSync(candidate)) || "";
}

async function waitForLocalDependencyReady(baseUrl, timeoutMs, onProgress = null) {
  const deadline = Date.now() + timeoutMs;
  let lastError = "";
  let lastEmit = 0;
  while (Date.now() < deadline) {
    const elapsed = timeoutMs - Math.max(0, deadline - Date.now());
    if (onProgress && Date.now() - lastEmit > 1600) {
      lastEmit = Date.now();
      onProgress({
        stage: "wait-ready",
        progress: Math.min(94, 50 + Math.round((elapsed / timeoutMs) * 44)),
        message: "正在等待本机能力响应...",
        detail: `已等待 ${Math.max(1, Math.round(elapsed / 1000))} 秒`
      });
    }
    try {
      const response = await fetchWithTimeout(baseUrl, {
        headers: {
          "Accept": "application/json,text/plain,*/*",
          "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
        }
      }, 1200);
      if (response.ok || response.status < 500) {
        const health = await safeJsonFromResponse(response);
        if (health?.paperSolverBridge) return true;
        lastError = "本机服务端口被不兼容进程占用，正在等待新的 PaperSolver 服务启动";
      }
    } catch (error) {
      lastError = error?.message || String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, 850));
  }
  throw new Error(`本机能力启动超时${lastError ? `：${lastError}` : ""}`);
}

function localDependencyLogPath() {
  return activeLocalDependencyLogFile || path.join(runtimeLogDirectory(), "local-dependency.log");
}

function nextLocalDependencyLogPath() {
  const stamp = new Date().toISOString().replace(/[:.]/g, "-");
  activeLocalDependencyLogFile = path.join(runtimeLogDirectory(), `local-dependency-${stamp}-${process.pid}.log`);
  return activeLocalDependencyLogFile;
}

function createDependencyDownloadPausedError() {
  const error = new Error("本机能力下载已暂停");
  error.code = "PAPER_SOLVER_DOWNLOAD_PAUSED";
  return error;
}

function isDependencyDownloadPausedError(error) {
  return error?.code === "PAPER_SOLVER_DOWNLOAD_PAUSED";
}

async function downloadFileToPath(url, targetPath, onProgress, label = "文件", downloadControl = null) {
  const maxAttempts = 8;
  let lastError = null;
  let lastProgress = null;
  const reportProgress = (payload) => {
    lastProgress = { ...(lastProgress || {}), ...(payload || {}) };
    onProgress?.(lastProgress);
  };

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    if (downloadControl?.paused) throw createDependencyDownloadPausedError();
    const resumeFrom = await fs.promises.stat(targetPath).then((stat) => stat.size).catch(() => 0);
    if (attempt > 1) {
      reportProgress({
        retrying: true,
        retryAttempt: attempt,
        maxAttempts,
        receivedBytes: Math.max(Number(lastProgress?.receivedBytes) || 0, resumeFrom),
        detail: `网络短暂中断，正在第 ${attempt}/${maxAttempts} 次重连，并从已下载位置继续`
      });
      await new Promise((resolve) => setTimeout(resolve, Math.min(6000, attempt * 1500)));
    }
    try {
      await downloadFileAttempt(url, targetPath, reportProgress, label, {
        resumeFrom,
        redirectCount: 0,
        downloadControl
      });
      return;
    } catch (error) {
      if (downloadControl?.paused || isDependencyDownloadPausedError(error)) {
        throw createDependencyDownloadPausedError();
      }
      lastError = error;
    }
  }

  throw new Error(`${label}下载多次中断：${lastError?.message || "网络连接不稳定"}`);
}

function downloadFileAttempt(url, targetPath, onProgress, label, options = {}) {
  return new Promise((resolve, reject) => {
    const downloadControl = options.downloadControl || null;
    if (downloadControl?.paused) {
      reject(createDependencyDownloadPausedError());
      return;
    }
    const resumeFrom = Math.max(0, Number(options.resumeFrom) || 0);
    const redirectCount = Math.max(0, Number(options.redirectCount) || 0);
    if (redirectCount > 8) {
      reject(new Error(`${label}下载重定向次数过多`));
      return;
    }
    const parsed = new URL(url);
    const client = parsed.protocol === "http:" ? http : https;
    const headers = {
      "User-Agent": "PaperSolver Desktop",
      "Accept": "application/octet-stream,*/*",
      "Accept-Encoding": "identity",
      "Connection": "keep-alive"
    };
    if (resumeFrom > 0) {
      headers.Range = `bytes=${resumeFrom}-`;
    }
    let activeFailureHandler = null;
    const request = client.get(parsed, { headers }, (response) => {
      if ([301, 302, 303, 307, 308].includes(response.statusCode) && response.headers.location) {
        response.resume();
        downloadFileAttempt(
          new URL(response.headers.location, url).toString(),
          targetPath,
          onProgress,
          label,
          { resumeFrom, redirectCount: redirectCount + 1, downloadControl }
        ).then(resolve, reject);
        return;
      }

      const contentRange = String(response.headers["content-range"] || "");
      const rangeMatch = contentRange.match(/bytes\s+(\d+)-(\d+)\/(\d+|\*)/i);
      const unsatisfiedMatch = contentRange.match(/bytes\s+\*\/(\d+)/i);
      if (response.statusCode === 416 && unsatisfiedMatch && resumeFrom >= Number(unsatisfiedMatch[1])) {
        response.resume();
        resolve();
        return;
      }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        response.resume();
        reject(new Error(`${label}下载失败（HTTP ${response.statusCode}）`));
        return;
      }
      const contentType = String(response.headers["content-type"] || "").toLowerCase();
      if (contentType.includes("text/html")) {
        response.resume();
        reject(new Error(`${label}完整性校验未通过，请稍后重试或联系管理员。`));
        return;
      }

      const append = response.statusCode === 206 && resumeFrom > 0;
      const initialBytes = append ? resumeFrom : 0;
      const responseBytes = Number(response.headers["content-length"]) || 0;
      const total = rangeMatch && rangeMatch[3] !== "*"
        ? Number(rangeMatch[3])
        : responseBytes
          ? initialBytes + responseBytes
          : 0;
      let received = initialBytes;
      const startedAt = Date.now();
      let lastProgressAt = 0;
      let settled = false;
      const output = fs.createWriteStream(targetPath, { flags: append ? "a" : "w" });
      const fail = (error) => {
        if (settled) return;
        settled = true;
        const failure = downloadControl?.paused ? createDependencyDownloadPausedError() : error;
        response.unpipe(output);
        if (output.destroyed || output.closed) {
          reject(failure);
          return;
        }
        output.end(() => reject(failure));
      };
      activeFailureHandler = fail;

      response.setTimeout(600000, () => {
        response.destroy(new Error(`${label}下载网络连续 600 秒没有收到数据`));
      });
      response.on("data", (chunk) => {
        received += chunk.length;
        const now = Date.now();
        if (onProgress && (now - lastProgressAt > 500 || (total && received >= total))) {
          lastProgressAt = now;
          const elapsedSeconds = Math.max(0.001, (now - startedAt) / 1000);
          const bytesPerSecond = Math.round((received - initialBytes) / elapsedSeconds);
          const etaSeconds = total && bytesPerSecond > 0 ? Math.max(0, Math.round((total - received) / bytesPerSecond)) : 0;
          onProgress({
            percent: total ? Math.min(100, (received / total) * 100) : 0,
            receivedBytes: received,
            totalBytes: total,
            bytesPerSecond,
            etaSeconds,
            retrying: false,
            detail: ""
          });
        }
      });
      response.on("aborted", () => fail(new Error(`${label}下载连接被中断`)));
      response.on("error", fail);
      output.on("error", fail);
      output.on("finish", () => {
        if (settled) return;
        settled = true;
        output.close(() => {
          onProgress?.({
            percent: 100,
            receivedBytes: received,
            totalBytes: total || received,
            bytesPerSecond: 0,
            etaSeconds: 0,
            retrying: false
          });
          resolve();
        });
      });
      response.pipe(output);
    });
    request.setTimeout(600000, () => {
      request.destroy(new Error(`${label}下载网络连续 600 秒没有收到数据`));
    });
    downloadControl?.requests?.add(request);
    request.on("close", () => downloadControl?.requests?.delete(request));
    request.on("error", (error) => {
      const failure = downloadControl?.paused ? createDependencyDownloadPausedError() : error;
      if (activeFailureHandler) {
        activeFailureHandler(failure);
        return;
      }
      reject(failure);
    });
  });
}

async function extractArchive(archivePath, targetDir, onProgress = null) {
  const lower = archivePath.toLowerCase();
  if (lower.endsWith(".zip")) {
    if (process.platform === "darwin") {
      onProgress?.({ phase: "extract", percent: 5, extractedEntries: 0, totalEntries: 0 });
      await runCommand("ditto", ["-x", "-k", archivePath, targetDir]);
      onProgress?.({ phase: "extract", percent: 100, extractedEntries: 0, totalEntries: 0 });
      return;
    }
    return extractArchiveWithTarProgress(archivePath, targetDir, ["-xf", archivePath, "-C", targetDir], onProgress);
  }
  if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
    return extractArchiveWithTarProgress(archivePath, targetDir, ["-xzf", archivePath, "-C", targetDir], onProgress);
  }
  throw new Error("本机能力组件格式不支持。");
}

async function countArchiveEntries(archivePath) {
  return new Promise((resolve, reject) => {
    const child = spawn("tar", ["-tf", archivePath], { stdio: ["ignore", "pipe", "pipe"], windowsHide: true });
    let count = 0;
    let carry = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => {
      const lines = `${carry}${chunk.toString()}`.split(/\r?\n/);
      carry = lines.pop() || "";
      count += lines.filter(Boolean).length;
    });
    child.stderr.on("data", (chunk) => { stderr = `${stderr}${chunk.toString()}`.slice(-4000); });
    child.on("error", reject);
    child.on("close", (code) => {
      if (carry) count += 1;
      if (code === 0) resolve(count);
      else reject(new Error(`tar 扫描失败：${textValue(stderr) || `退出码 ${code}`}`));
    });
  });
}

async function extractArchiveWithTarProgress(archivePath, targetDir, baseArgs, onProgress = null) {
  onProgress?.({ phase: "scan", percent: 0, extractedEntries: 0, totalEntries: 0 });
  const totalEntries = await countArchiveEntries(archivePath);
  return new Promise((resolve, reject) => {
    const args = [...baseArgs];
    args[0] = args[0].replace("x", "xv");
    const child = spawn("tar", args, { stdio: ["ignore", "pipe", "pipe"], windowsHide: true });
    let extractedEntries = 0;
    let carry = "";
    let stderr = "";
    let lastProgressAt = 0;
    const report = (force = false) => {
      const now = Date.now();
      if (!force && now - lastProgressAt < 120) return;
      lastProgressAt = now;
      onProgress?.({
        phase: "extract",
        percent: totalEntries ? Math.min(100, (extractedEntries / totalEntries) * 100) : 0,
        extractedEntries,
        totalEntries
      });
    };
    child.stdout.on("data", (chunk) => {
      const lines = `${carry}${chunk.toString()}`.split(/\r?\n/);
      carry = lines.pop() || "";
      extractedEntries += lines.filter(Boolean).length;
      report();
    });
    child.stderr.on("data", (chunk) => { stderr = `${stderr}${chunk.toString()}`.slice(-6000); });
    child.on("error", reject);
    child.on("close", (code) => {
      if (carry) extractedEntries += 1;
      report(true);
      if (code === 0) resolve();
      else reject(new Error(`tar 执行失败：${textValue(stderr) || `退出码 ${code}`}`));
    });
  });
}

function runCommand(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: ["ignore", "pipe", "pipe"], windowsHide: true });
    let stderr = "";
    child.stderr.on("data", (chunk) => { stderr += chunk.toString(); });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} 执行失败：${textValue(stderr) || `退出码 ${code}`}`));
    });
  });
}

async function assertZipArchive(archivePath, label = "本机能力") {
  const header = await fs.promises.readFile(archivePath, { encoding: null, flag: "r" }).then(buffer => buffer.subarray(0, 4));
  const isZip = header.length >= 4 && header[0] === 0x50 && header[1] === 0x4b;
  if (!isZip) {
    throw new Error(`${label}完整性校验失败，请稍后重试或联系管理员。`);
  }
}

function assertFileSha256(filePath, expectedHash, label = "本机能力") {
  return new Promise((resolve, reject) => {
    const hash = createHash("sha256");
    const stream = fs.createReadStream(filePath);
    stream.on("data", (chunk) => hash.update(chunk));
    stream.on("error", reject);
    stream.on("end", () => {
      const actual = hash.digest("hex").toLowerCase();
      const expected = String(expectedHash || "").trim().toLowerCase();
      if (actual !== expected) {
        reject(new Error(`${label}下载源与当前清单不一致（期望 ${expected.slice(0, 12)}，实际 ${actual.slice(0, 12)}）。请管理员更新依赖下载源后重试。`));
        return;
      }
      resolve();
    });
  });
}

async function normalizeInstalledDependencyLayout(root) {
  const entries = await fs.promises.readdir(root, { withFileTypes: true }).catch(() => []);
  const directManifest = path.join(root, "papersolver-dependency.json");
  if (!fs.existsSync(directManifest) && entries.length === 1 && entries[0].isDirectory()) {
    const nested = path.join(root, entries[0].name);
    const nestedEntries = await fs.promises.readdir(nested, { withFileTypes: true });
    for (const entry of nestedEntries) {
      await fs.promises.rename(path.join(nested, entry.name), path.join(root, entry.name));
    }
    await fs.promises.rm(nested, { recursive: true, force: true });
  }
  await makeExecutables(root);
  await fs.promises.writeFile(path.join(root, ".papersolver-dependency-installed"), new Date().toISOString());
}

async function makeExecutables(root) {
  if (process.platform === "win32") return;
  const names = ["bin", "scripts"];
  for (const name of names) {
    const dir = path.join(root, name);
    const files = await fs.promises.readdir(dir).catch(() => []);
    for (const file of files) {
      await fs.promises.chmod(path.join(dir, file), 0o755).catch(() => {});
    }
  }
  const rootFiles = await fs.promises.readdir(root).catch(() => []);
  for (const file of rootFiles) {
    if (/^(start-|papersolver-|pdfmath|pdf2zh|mineru)/i.test(file)) {
      await fs.promises.chmod(path.join(root, file), 0o755).catch(() => {});
    }
  }
}

function findExecutableInDir(root, names) {
  const suffixes = process.platform === "win32" ? [".exe", ".cmd", ".bat", ""] : [""];
  const dirs = [root, path.join(root, "bin"), path.join(root, "scripts")];
  for (const dir of dirs) {
    for (const name of names) {
      for (const suffix of suffixes) {
        const candidate = path.join(dir, `${name}${suffix}`);
        if (fs.existsSync(candidate)) return candidate;
      }
    }
  }
  return "";
}

async function startDesktopPdfMathTranslation(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法启动本机对照翻译。");
  }
  const cachedDualPath = findDesktopDualPdfPath(workspaceId);
  if (fs.existsSync(cachedDualPath)) {
    desktopPdfMathTasks.set(workspaceId, {
      taskId: "desktop-cached",
      baseUrl: pdfMathBaseUrl(readDesktopSettings()),
      completed: true,
      updatedAt: Date.now()
    });
    return {
      ok: true,
      local: true,
      cached: true,
      taskId: "desktop-cached",
      state: "SUCCESS",
      message: "已命中本机双语 PDF 缓存"
    };
  }

  // A tab switch must attach to the existing task instead of submitting the
  // same PDF again. The task lives in the desktop process, independently of
  // whichever reader view is currently mounted.
  const existingTask = desktopPdfMathTasks.get(workspaceId);
  if (existingTask?.taskId && existingTask.taskId !== "desktop-cached" && !existingTask.completed) {
    try {
      const existingStatus = await getDesktopPdfMathStatus({ workspaceId });
      return {
        ...existingStatus,
        ok: true,
        local: true,
        reused: true,
        cached: false,
        taskId: existingTask.taskId,
        state: textValue(existingStatus?.state || existingStatus?.status) || "RUNNING"
      };
    } catch (error) {
      // A stale task can be discarded and submitted again below.
      desktopPdfMathTasks.delete(workspaceId);
    }
  }

  const cachedPdf = await ensureCachedPdf({ ...payload, workspaceId }, { prompt: true });
  if (!cachedPdf?.found || !cachedPdf.base64) {
    throw new Error(cachedPdf?.error || "这篇文献没有可读取的原始 PDF，无法启动对照翻译。请重新选择原始 PDF。");
  }
  const pdfBuffer = Buffer.from(cachedPdf.base64, "base64");
  if (!looksLikePdfBuffer(pdfBuffer)) {
    throw new Error("本机缓存文件不是有效 PDF，无法提交对照翻译。");
  }
  const cacheKey = pdfContentFingerprint(pdfBuffer);
  const contentCachedDualPath = findDesktopDualPdfPath(workspaceId, cacheKey);
  if (contentCachedDualPath) {
    await materializeDesktopDualPdfCache(workspaceId, cacheKey, contentCachedDualPath);
    desktopPdfMathTasks.set(workspaceId, {
      taskId: "desktop-cached",
      baseUrl: pdfMathBaseUrl(readDesktopSettings()),
      cacheKey,
      completed: true,
      updatedAt: Date.now()
    });
    return {
      ok: true,
      local: true,
      cached: true,
      taskId: "desktop-cached",
      state: "SUCCESS",
      progress: 100,
      message: "已命中同一原始 PDF 的双语译文缓存"
    };
  }

  // PDF layout translation must use the domestic bridge. A reachable Google
  // endpoint is not a reason to route user text through Google, and caused
  // failures for users without a proxy.
  const preferredBridgeProvider = "tencent-transmart";
  await fs.promises.mkdir(localDependencyDir(), { recursive: true });
  await fs.promises.writeFile(
    path.join(localDependencyDir(), "translation-provider.json"),
    JSON.stringify({ provider: preferredBridgeProvider, updatedAt: new Date().toISOString() }),
    "utf8"
  );
  const serviceState = await startLocalDependencyServices({ waitForReady: true });
  if (!serviceState?.ok) {
    throw new Error(serviceState?.message || "本机能力未就绪，无法启动对照翻译。");
  }
  const baseUrl = serviceState.baseUrl || activePdfMathBaseUrl || pdfMathBaseUrl(readDesktopSettings());
  // pdf2zh internally selects its GoogleTranslator class for layout-preserving
  // PDF output. The patched translator below redirects that class to the
  // desktop Tencent -> Youdao chain without using Google's network.
  const service = "google";
  // Pick one provider for the whole PDF before submitting the task. This
  // keeps terminology and layout markers consistent while still surviving a
  // temporary outage of the preferred domestic provider.
  const bridgeProvider = await verifyDesktopTranslationBridge(preferredBridgeProvider);
  if (bridgeProvider !== preferredBridgeProvider) {
    await fs.promises.writeFile(
      path.join(localDependencyDir(), "translation-provider.json"),
      JSON.stringify({ provider: bridgeProvider, updatedAt: new Date().toISOString() }),
      "utf8"
    );
  }
  const thread = PDF_TRANSLATION_BRIDGE_CONCURRENCY;

  const boundary = `PaperSolverDesktop-${randomUUID()}`;
  const body = buildPdfMathMultipartBody(boundary, {
    fileName: cachedPdf.fileName || `${workspaceId}.pdf`,
    pdfBuffer,
    data: {
      lang_in: "auto",
      lang_out: "zh",
      service,
      thread,
      skip_subset_fonts: true
    }
  });
  const response = await fetchWithTimeout(`${baseUrl}/v1/translate`, {
    method: "POST",
    headers: {
      "Accept": "application/json,text/plain,*/*",
      "Content-Type": `multipart/form-data; boundary=${boundary}`,
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
    },
    body
  }, 45000);
  if (!response.ok) {
    throw new Error(`PaperSolver 本机能力提交失败（HTTP ${response.status}）：${await safeResponseText(response)}`);
  }
  const result = await response.json();
  const taskId = textValue(result.id || result.taskId || result.task_id);
  if (!taskId) {
    throw new Error("PaperSolver 本机能力没有返回任务编号。");
  }
  desktopPdfMathTasks.set(workspaceId, {
    taskId,
    baseUrl,
      service,
      cacheKey,
      startedAt: Date.now(),
    updatedAt: Date.now()
  });
  return {
    ...result,
    ok: true,
    local: true,
    cached: false,
    taskId,
    state: textValue(result.state || result.status) || "PENDING"
  };
}

async function getDesktopPdfMathStatus(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法查询本机对照翻译状态。");
  }
  const task = desktopPdfMathTasks.get(workspaceId);
  const cachedDualPath = findDesktopDualPdfPath(workspaceId, task?.cacheKey);
  if (fs.existsSync(cachedDualPath)) {
    return {
      ok: true,
      local: true,
      cached: true,
      taskId: "desktop-cached",
      state: "SUCCESS",
      progress: 100,
      message: "本机双语 PDF 已生成"
    };
  }
  // The library probes before opening the reader. Let that probe recognize a
  // completed translation for the same PDF even when it was re-imported under
  // a new workspace id, so it never asks for or consumes another quota unit.
  if (!task?.taskId) {
    const cachedPdf = await ensureCachedPdf({
      workspaceId,
      sourceUrl: payload.sourceUrl,
      sourceUrls: payload.sourceUrls
    }, { prompt: false }).catch(() => null);
    if (cachedPdf?.found && cachedPdf.base64) {
      const cacheKey = pdfContentFingerprint(Buffer.from(cachedPdf.base64, "base64"));
      const contentCachedDualPath = findDesktopDualPdfPath(workspaceId, cacheKey);
      if (contentCachedDualPath) {
        await materializeDesktopDualPdfCache(workspaceId, cacheKey, contentCachedDualPath);
        desktopPdfMathTasks.set(workspaceId, {
          taskId: "desktop-cached",
          baseUrl: pdfMathBaseUrl(readDesktopSettings()),
          cacheKey,
          completed: true,
          updatedAt: Date.now()
        });
        return {
          ok: true,
          local: true,
          cached: true,
          taskId: "desktop-cached",
          state: "SUCCESS",
          progress: 100,
          message: "已命中同一原始 PDF 的双语译文缓存"
        };
      }
    }
  }
  if (!task?.taskId) {
    throw new Error("尚未创建本机对照翻译任务。");
  }
  const response = await fetchWithTimeout(`${task.baseUrl}/v1/translate/${encodeURIComponent(task.taskId)}`, {
    headers: {
      "Accept": "application/json,text/plain,*/*",
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
    }
  }, 20000);
  if (!response.ok) {
    throw new Error(`PaperSolver 本机能力状态查询失败（HTTP ${response.status}）：${await safeResponseText(response)}`);
  }
  const result = await response.json();
  desktopPdfMathTasks.set(workspaceId, { ...task, updatedAt: Date.now() });
  return {
    ...result,
    ok: true,
    local: true,
    taskId: task.taskId,
    state: textValue(result.state || result.status) || "RUNNING"
  };
}

async function getDesktopPdfMathDualPdf(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法读取本机双语 PDF。");
  }
  const task = desktopPdfMathTasks.get(workspaceId);
  const cachedDualPath = findDesktopDualPdfPath(workspaceId, task?.cacheKey);
  if (fs.existsSync(cachedDualPath)) {
    const buffer = await fs.promises.readFile(cachedDualPath);
    if (!looksLikePdfBuffer(buffer)) {
      await fs.promises.rm(cachedDualPath, { force: true });
      throw new Error("本机双语 PDF 缓存已损坏，请重新生成。");
    }
    return {
      ok: true,
      local: true,
      cached: true,
      mimeType: "application/pdf",
      size: buffer.length,
      base64: buffer.toString("base64")
    };
  }
  if (!task?.taskId || task.taskId === "desktop-cached") {
    throw new Error("尚未创建本机对照翻译任务。");
  }
  const response = await fetch(`${task.baseUrl}/v1/translate/${encodeURIComponent(task.taskId)}/dual`, {
    headers: {
      "Accept": "application/pdf,*/*",
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
    }
  });
  if (!response.ok) {
    throw new Error(`PaperSolver 本机能力下载双语 PDF 失败（HTTP ${response.status}）：${await safeResponseText(response)}`);
  }
  const buffer = Buffer.from(await response.arrayBuffer());
  if (!looksLikePdfBuffer(buffer)) {
    throw new Error("PaperSolver 本机能力返回的双语文件不是有效 PDF。");
  }
  await writeDesktopDualPdfCache(workspaceId, task.cacheKey, buffer);
  desktopPdfMathTasks.set(workspaceId, { ...task, completed: true, updatedAt: Date.now() });
  return {
    ok: true,
    local: true,
    cached: false,
    mimeType: "application/pdf",
    size: buffer.length,
    base64: buffer.toString("base64")
  };
}

async function startDesktopStructuredParse(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  const force = Boolean(payload.force);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法启动本机结构化解析。");
  }
  const existing = desktopStructuredParseTasks.get(workspaceId);
  if (!force && existing?.state === "RUNNING") {
    return desktopStructuredStatusPayload(workspaceId, existing);
  }
  const cachedPdf = await ensureCachedPdf({ ...payload, workspaceId }, { prompt: true });
  if (!cachedPdf?.found || !cachedPdf.base64) {
    throw new Error(cachedPdf?.error || "这篇文献没有可读取的原始 PDF，无法进行沉浸翻译解析。请重新选择原始 PDF。");
  }
  const pdfBuffer = Buffer.from(cachedPdf.base64, "base64");
  if (!looksLikePdfBuffer(pdfBuffer)) {
    throw new Error("本机缓存文件不是有效 PDF，无法进行沉浸翻译解析。");
  }
  const cacheKey = pdfContentFingerprint(pdfBuffer);
  const expectedPages = pdfPageCountHint(pdfBuffer);
  if (!force && hasDesktopStructuredContent(desktopStructuredOutputDir(workspaceId), expectedPages)) {
    const ready = desktopStructuredTaskState("SUCCESS", "本机结构化解析已就绪", "", 100, { cacheKey, expectedPages });
    desktopStructuredParseTasks.set(workspaceId, ready);
    return desktopStructuredStatusPayload(workspaceId, ready);
  }
  if (!force && findDesktopContentList(workspaceId)) {
    // A previous MinerU run can leave only the title page's content list behind.
    // Never present that partial output as a successfully parsed paper.
    await fs.promises.rm(desktopStructuredOutputDir(workspaceId), { recursive: true, force: true });
  }
  const contentCachedOutput = findDesktopStructuredContentList(cacheKey);
  if (!force && contentCachedOutput && hasDesktopStructuredContent(desktopStructuredContentOutputDir(cacheKey), expectedPages)) {
    await materializeDesktopStructuredCache(workspaceId, cacheKey);
    const ready = desktopStructuredTaskState(
      "SUCCESS",
      "已命中同一原始 PDF 的结构化阅读缓存",
      "",
      100,
      { cacheKey, expectedPages }
    );
    desktopStructuredParseTasks.set(workspaceId, ready);
    return desktopStructuredStatusPayload(workspaceId, ready);
  }
  await ensureMineruPdfTextCompatibility(localDependencyDir());
  const readiness = await getLocalDependencyReadiness();
  if (!readiness.complete) {
    throw new Error(readiness.message || "本机能力不完整，请重新准备本机能力。");
  }
  const parserBinary = desktopStructuredParserBinary();
  if (!parserBinary) {
    throw new Error("未检测到 PaperSolver 本机能力的结构化解析组件。");
  }
  const running = desktopStructuredTaskState(
    "RUNNING",
    "正在调用本机解析能力识别段落、图表与阅读顺序",
    "",
    18,
    { cacheKey, expectedPages }
  );
  desktopStructuredParseTasks.set(workspaceId, running);
  parseDesktopStructuredInBackground(workspaceId, cachedPdf, parserBinary, force, cacheKey, expectedPages);
  return desktopStructuredStatusPayload(workspaceId, running);
}

async function getDesktopStructuredParseStatus(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法查询本机结构化解析状态。");
  }
  const state = desktopStructuredParseTasks.get(workspaceId);
  if (hasDesktopStructuredContent(desktopStructuredOutputDir(workspaceId), state?.expectedPages)) {
    const ready = desktopStructuredTaskState("SUCCESS", "段落、图表与阅读顺序解析完成", "", 100);
    desktopStructuredParseTasks.set(workspaceId, ready);
    return desktopStructuredStatusPayload(workspaceId, ready);
  }
  if (state?.cacheKey && hasDesktopStructuredContent(desktopStructuredContentOutputDir(state.cacheKey), state.expectedPages)) {
    await materializeDesktopStructuredCache(workspaceId, state.cacheKey);
    const ready = desktopStructuredTaskState(
      "SUCCESS",
      "已命中同一原始 PDF 的结构化阅读缓存",
      "",
      100,
      { cacheKey: state.cacheKey, expectedPages: state.expectedPages, createdAt: state.createdAt }
    );
    desktopStructuredParseTasks.set(workspaceId, ready);
    return desktopStructuredStatusPayload(workspaceId, ready);
  }
  if (state) return desktopStructuredStatusPayload(workspaceId, state);
  throw new Error("尚未创建本机结构化解析任务。");
}

async function getDesktopStructuredDocument(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法读取本机结构化解析结果。");
  }
  const contentList = findDesktopContentList(workspaceId);
  if (!contentList) {
    throw new Error("本机结构化解析结果尚未生成。");
  }
  const raw = JSON.parse(await fs.promises.readFile(contentList, "utf8"));
  const items = Array.isArray(raw) ? raw : [];
  const pageBlocks = new Map();
  let figures = 0;
  let tables = 0;
  let paragraphs = 0;
  let index = 0;
  for (const item of items) {
    const sourceType = textValue(item?.type).toLowerCase();
    if (["header", "footer", "page_number", "page_footnote"].includes(sourceType)) continue;
    const pageNumber = integerValue(item?.page_idx, 0) + 1;
    const kind = desktopStructuredBlockKind(sourceType, item);
    const text = desktopStructuredItemText(item, kind);
    if (isPublicationNoise(text)) continue;
    const imageUrl = desktopStructuredAssetUrl(workspaceId, contentList, item?.img_path);
    const html = kind === "table" ? sanitizeTableHtml(textValue(item?.table_body)) : "";
    if (isDecorativeFigure(kind, text, item)) continue;
    if (kind === "paragraph" && isEquationNumberArtifact(text)) {
      appendEquationNumber(pageBlocks.get(pageNumber) || [], text);
      continue;
    }
    if (!text && !imageUrl && !html) continue;
    if (kind === "figure") figures += 1;
    if (kind === "table") tables += 1;
    if (kind === "paragraph") paragraphs += 1;
    const block = {
      id: `desktop-structured-p${pageNumber}-b${index++}`,
      kind,
      text,
      imageUrl,
      html,
      equationNumber: "",
      textLevel: integerValue(item?.text_level, 0),
      bbox: item?.bbox || [],
      translation: "",
      translationProvider: "google",
      translating: false,
      translationError: ""
    };
    if (!pageBlocks.has(pageNumber)) pageBlocks.set(pageNumber, []);
    pageBlocks.get(pageNumber).push(block);
  }
  const pages = Array.from(pageBlocks.entries())
    .sort((left, right) => left[0] - right[0])
    .map(([pageNumber, blocks]) => ({ pageNumber, blocks }));
  return {
    engine: "PaperSolver 本机能力",
    pages,
    totalPages: pages.reduce((max, page) => Math.max(max, page.pageNumber), 0),
    paragraphCount: paragraphs,
    figureCount: figures,
    tableCount: tables
  };
}

async function getDesktopStructuredAsset(payload = {}) {
  const assetPath = textValue(payload.path || payload.assetPath);
  const parsed = parseDesktopStructuredAssetUrl(assetPath);
  if (!parsed.workspaceId || !parsed.relativePath) {
    throw new Error("本机图表资源地址无效。");
  }
  const contentList = findDesktopContentList(parsed.workspaceId);
  if (!contentList) {
    throw new Error("本机结构化解析结果不存在。");
  }
  const base = path.dirname(contentList);
  const requested = path.resolve(base, parsed.relativePath);
  if (!requested.startsWith(path.resolve(base)) || !fs.existsSync(requested)) {
    throw new Error("本机图表资源不存在。");
  }
  const buffer = await fs.promises.readFile(requested);
  return {
    ok: true,
    local: true,
    mimeType: mimeTypeForFile(requested),
    size: buffer.length,
    base64: buffer.toString("base64")
  };
}

async function parseDesktopStructuredInBackground(workspaceId, cachedPdf, parserBinary, force, cacheKey = "", expectedPages = 0) {
  const outputDir = desktopStructuredOutputDir(workspaceId);
  const inputDir = path.join(outputDir, "input");
  const inputPath = path.join(inputDir, `${workspaceId}.pdf`);
  const logPath = path.join(outputDir, "structured-parse.log");
  try {
    if (force) {
      await fs.promises.rm(outputDir, { recursive: true, force: true });
    }
    await fs.promises.mkdir(inputDir, { recursive: true });
    await fs.promises.writeFile(inputPath, Buffer.from(cachedPdf.base64, "base64"));
    desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState(
      "RUNNING",
      "本机解析能力正在处理论文版面",
      "",
      45,
      { cacheKey, expectedPages }
    ));
    const startedAt = Date.now();
    const progressTimer = setInterval(() => {
      const elapsed = Math.round((Date.now() - startedAt) / 1000);
      const progress = Math.min(98, 48 + Math.floor(elapsed / 3));
      desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState(
        "RUNNING",
        "正在解析论文版面与公式",
        `已运行 ${elapsed} 秒，正在生成结构化内容`,
        progress,
        { cacheKey, expectedPages, createdAt: startedAt }
      ));
    }, 1200);
    const result = await runDesktopStructuredParser(parserBinary, inputPath, outputDir, logPath, expectedPages);
    clearInterval(progressTimer);
    // A clean parser exit is authoritative. Some parser versions emit valid
    // content with incomplete page_idx metadata, which used to strand the UI
    // at 94% even though the document was readable.
    const contentReady = hasDesktopStructuredContent(outputDir, expectedPages)
      || (result.exitCode === 0 && hasDesktopStructuredContent(outputDir, 0));
    if (result.exitCode !== 0 || !contentReady) {
      const detail = await tailFile(logPath, 1800);
      const outputTree = await describeDirectoryTree(outputDir);
      const failureDetail = [result.error, detail, `生成目录：\n${outputTree}`].filter(Boolean).join("\n");
      await fs.promises.appendFile(logPath, `\n${failureDetail}\n`, "utf8").catch(() => {});
      desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState(
        "FAILURE",
        "论文结构化解析失败",
        failureDetail,
        100,
        { cacheKey, expectedPages, createdAt: startedAt }
      ));
      return;
    }
    await writeDesktopStructuredContentCache(workspaceId, cacheKey);
    desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState(
      "SUCCESS",
      "段落、图表与阅读顺序解析完成",
      "",
      100,
      { cacheKey, expectedPages, createdAt: startedAt }
    ));
  } catch (error) {
    desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState(
      "FAILURE",
      "论文结构化解析失败",
      error?.message || String(error),
      100,
      { cacheKey, expectedPages }
    ));
  }
}

function runDesktopStructuredParser(parserBinary, inputPath, outputDir, logPath, expectedPages = 0) {
  return new Promise((resolve) => {
    const args = [
      "-p", inputPath,
      "-o", outputDir,
      "-b", "pipeline",
      "-f", "true",
      "-t", "true",
      "--client-side-output-generation", "true"
    ];
    const logStream = fs.createWriteStream(logPath, { flags: "a" });
    const command = structuredParserSpawnCommand({
      binary: parserBinary,
      args,
      cwd: localDependencyDir()
    });
    const child = spawn(command.binary, command.args || [], {
      cwd: command.cwd || localDependencyDir(),
      env: {
        ...process.env,
        ...structuredParserEnv()
      },
      windowsHide: true,
      stdio: ["ignore", "pipe", "pipe"]
    });
    let settled = false;
    const finish = (result) => {
      if (settled) return;
      settled = true;
      clearInterval(outputReadyTimer);
      clearTimeout(timeoutTimer);
      logStream.end();
      resolve(result);
    };
    // Some MinerU builds leave a Python worker alive after producing the content
    // list. The content list is the real contract for the reader, so finish as
    // soon as it exists instead of leaving users stranded at 92-94%.
    const outputReadyTimer = setInterval(() => {
      if (hasDesktopStructuredContent(outputDir, expectedPages)) {
        terminateChildProcess(child);
        finish({ exitCode: 0, outputReady: true, error: "" });
      }
    }, 1000);
    const timeoutTimer = setTimeout(() => {
      terminateChildProcess(child);
      finish({ exitCode: -1, error: "本机结构化解析超过 45 分钟仍未生成可读取结果，请重试或更换 PDF。" });
    }, Math.max(15 * 60 * 1000, Number(process.env.PAPER_SOLVER_STRUCTURED_PARSE_TIMEOUT_MS) || 45 * 60 * 1000));
    child.stdout.on("data", (chunk) => logStream.write(chunk));
    child.stderr.on("data", (chunk) => logStream.write(chunk));
    child.on("error", (error) => {
      finish({ exitCode: -1, error: error?.message || String(error) });
    });
    child.on("close", (exitCode) => {
      const normalizedExitCode = typeof exitCode === "number" ? exitCode : -1;
      finish({
        exitCode: normalizedExitCode,
        error: normalizedExitCode === 0 ? "" : `MinerU 退出码 ${normalizedExitCode}`
      });
    });
  });
}

function desktopStructuredTaskState(state, message, detail = "", progress = 20, options = {}) {
  return {
    state,
    message,
    detail,
    progress,
    cacheKey: textValue(options.cacheKey),
    expectedPages: Math.max(0, Number(options.expectedPages) || 0),
    createdAt: Number(options.createdAt) || Date.now(),
    updatedAt: Date.now()
  };
}

function desktopStructuredStatusPayload(workspaceId, state) {
  return {
    ok: true,
    local: true,
    workspaceId,
    state: state.state,
    message: state.message,
    detail: state.detail || "",
    progress: state.progress || ("SUCCESS" === state.state ? 100 : 20),
    createdAt: state.createdAt || state.updatedAt || Date.now(),
    ready: state.state === "SUCCESS"
  };
}

function desktopStructuredParserBinary() {
  const fromEnv = textValue(process.env.PAPER_SOLVER_MINERU_BINARY || process.env.PAPER_SOLVER_STRUCTURED_PARSER);
  if (fromEnv) return fromEnv;
  const manifest = readLocalDependencyManifest();
  const manifestCommand = textValue(manifest?.structuredParser?.command);
  if (manifestCommand) {
    const manifestPath = path.join(localDependencyDir(), manifestCommand);
    if (fs.existsSync(manifestPath)) return manifestPath;
  }
  const candidates = [
    path.join(app.getPath("userData"), "dependencies", "bin", process.platform === "win32" ? "mineru.exe" : "mineru"),
    path.join(app.getPath("userData"), "dependencies", "bin", process.platform === "win32" ? "mineru.cmd" : "mineru"),
    path.join(app.getPath("userData"), "dependencies", process.platform === "win32" ? "mineru.exe" : "mineru"),
    path.join(app.getPath("userData"), "dependencies", process.platform === "win32" ? "mineru.cmd" : "mineru"),
    isPackaged ? path.join(process.resourcesPath, "dependencies", "bin", process.platform === "win32" ? "mineru.exe" : "mineru") : "",
    isPackaged ? path.join(process.resourcesPath, "dependencies", "bin", process.platform === "win32" ? "mineru.cmd" : "mineru") : "",
    isPackaged ? path.join(process.resourcesPath, "dependencies", process.platform === "win32" ? "mineru.exe" : "mineru") : "",
    isPackaged ? path.join(process.resourcesPath, "dependencies", process.platform === "win32" ? "mineru.cmd" : "mineru") : "",
    path.join(__dirname, "..", "dependencies", "bin", process.platform === "win32" ? "mineru.exe" : "mineru"),
    path.join(__dirname, "..", "dependencies", "bin", process.platform === "win32" ? "mineru.cmd" : "mineru")
  ].filter(Boolean);
  return candidates.find((candidate) => fs.existsSync(candidate)) || "";
}

function desktopStructuredRoot() {
  return path.join(pdfCacheDir(), "structured");
}

function desktopStructuredOutputDir(workspaceId) {
  return path.join(desktopStructuredRoot(), workspaceId);
}

function desktopStructuredContentOutputDir(cacheKey) {
  const normalized = String(cacheKey || "").toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(normalized)) return "";
  return path.join(desktopStructuredRoot(), "by-content", `${normalized}-structured-v1`);
}

function findDesktopContentList(workspaceId) {
  return findDesktopContentListIn(desktopStructuredOutputDir(workspaceId));
}

function findDesktopStructuredContentList(cacheKey) {
  const root = desktopStructuredContentOutputDir(cacheKey);
  return root ? findDesktopContentListIn(root) : "";
}

function findDesktopContentListIn(root) {
  if (!root || !fs.existsSync(root)) return "";
  return findFileRecursive(root, (filePath) => {
    const name = path.basename(filePath);
    return name.endsWith("_content_list.json") || name === "content_list.json";
  });
}

function pdfPageCountHint(buffer) {
  if (!Buffer.isBuffer(buffer)) return 0;
  // Page dictionaries are normally kept outside compressed streams. It is a
  // conservative hint: returning 0 simply disables the coverage threshold.
  const raw = buffer.toString("latin1");
  return (raw.match(/\/Type\s*\/Page\b/g) || []).length;
}

function hasDesktopStructuredContent(root, expectedPages = 0) {
  const contentList = findDesktopContentListIn(root);
  if (!contentList) return false;
  try {
    const parsed = JSON.parse(fs.readFileSync(contentList, "utf8"));
    if (!Array.isArray(parsed) || parsed.length === 0) return false;
    const pageIndexes = parsed
      .map((item) => Number(item?.page_idx))
      .filter((pageIndex) => Number.isInteger(pageIndex) && pageIndex >= 0);
    const parsedPages = pageIndexes.length ? Math.max(...pageIndexes) + 1 : 0;
    const readableBlocks = parsed.filter((item) => {
      const type = textValue(item?.type).toLowerCase();
      return !["header", "footer", "page_number", "page_footnote"].includes(type)
        && Boolean(textValue(item?.text) || textValue(item?.latex) || textValue(item?.table_body) || textValue(item?.img_path));
    }).length;
    if (readableBlocks === 0 || parsedPages === 0) return false;
    const expected = Math.max(0, Number(expectedPages) || 0);
    // For multi-page papers, a one-page title-only result is always broken.
    // The 70% threshold leaves room for intentionally blank PDF pages while
    // rejecting a parser that stopped after the beginning of the document.
    if (expected >= 3 && parsedPages < Math.max(2, Math.ceil(expected * 0.7))) return false;
    return true;
  } catch {
    return false;
  }
}

async function materializeDesktopStructuredCache(workspaceId, cacheKey) {
  const sourceDir = desktopStructuredContentOutputDir(cacheKey);
  const targetDir = desktopStructuredOutputDir(workspaceId);
  if (!sourceDir || !hasDesktopStructuredContent(sourceDir)) {
    throw new Error("本机结构化阅读缓存不存在或已损坏。");
  }
  if (path.resolve(sourceDir) === path.resolve(targetDir)) return;
  const stagingDir = `${targetDir}.tmp-${randomUUID()}`;
  await fs.promises.rm(stagingDir, { recursive: true, force: true });
  await fs.promises.mkdir(path.dirname(targetDir), { recursive: true });
  await fs.promises.cp(sourceDir, stagingDir, { recursive: true, force: true });
  await fs.promises.rm(targetDir, { recursive: true, force: true });
  await fs.promises.rename(stagingDir, targetDir);
}

async function writeDesktopStructuredContentCache(workspaceId, cacheKey) {
  const sourceDir = desktopStructuredOutputDir(workspaceId);
  const targetDir = desktopStructuredContentOutputDir(cacheKey);
  if (!targetDir || !hasDesktopStructuredContent(sourceDir) || hasDesktopStructuredContent(targetDir)) return;
  const stagingDir = `${targetDir}.tmp-${randomUUID()}`;
  await fs.promises.rm(stagingDir, { recursive: true, force: true });
  await fs.promises.mkdir(path.dirname(targetDir), { recursive: true });
  await fs.promises.cp(sourceDir, stagingDir, { recursive: true, force: true });
  await fs.promises.rm(targetDir, { recursive: true, force: true });
  await fs.promises.rename(stagingDir, targetDir);
}

function findFileRecursive(root, predicate) {
  if (!root || !fs.existsSync(root)) return "";
  const entries = fs.readdirSync(root, { withFileTypes: true });
  for (const entry of entries) {
    const entryPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      const nested = findFileRecursive(entryPath, predicate);
      if (nested) return nested;
    } else if (entry.isFile() && predicate(entryPath)) {
      return entryPath;
    }
  }
  return "";
}

function desktopStructuredBlockKind(sourceType, item) {
  if (sourceType === "text") {
    return integerValue(item?.text_level, 0) > 0 ? "heading" : "paragraph";
  }
  if (sourceType.includes("image") || sourceType.includes("chart")) return "figure";
  if (sourceType.includes("table")) return "table";
  if (sourceType.includes("equation") || sourceType.includes("formula")) return "equation";
  if (sourceType.includes("list")) {
    return textValue(item?.sub_type).toLowerCase() === "ref_text" ? "references" : "paragraph";
  }
  return "paragraph";
}

function desktopStructuredItemText(item, kind) {
  const direct = textValue(item?.text);
  if (direct) return normalizeStructuredText(direct);
  if (kind === "equation") {
    return joinStructuredText(item?.latex, item?.text_format, item?.formula, item?.equation, item?.content);
  }
  if (kind === "figure") {
    return joinStructuredText(item?.image_caption, item?.image_footnote);
  }
  if (kind === "table") {
    return joinStructuredText(item?.table_caption, item?.table_footnote);
  }
  return joinStructuredText(item?.list_items, item?.content);
}

function joinStructuredText(...values) {
  const parts = [];
  values.forEach((value) => flattenStructuredText(value, parts));
  return normalizeStructuredText(parts.join(" "));
}

function flattenStructuredText(value, parts) {
  if (value == null) return;
  if (Array.isArray(value)) {
    value.forEach((item) => flattenStructuredText(item, parts));
    return;
  }
  if (typeof value === "object") {
    Object.values(value).forEach((item) => flattenStructuredText(item, parts));
    return;
  }
  const text = textValue(value);
  if (text) parts.push(text);
}

function desktopStructuredAssetUrl(workspaceId, contentList, imgPath) {
  const source = textValue(imgPath);
  if (!source) return "";
  const base = path.dirname(contentList);
  const image = path.resolve(base, source);
  if (!image.startsWith(path.resolve(base)) || !fs.existsSync(image)) return "";
  const relative = path.relative(base, image).replaceAll(path.sep, "/");
  return `desktop-structured://${workspaceId}/${encodeURIComponent(relative)}`;
}

function parseDesktopStructuredAssetUrl(value) {
  const text = textValue(value);
  if (!text.startsWith("desktop-structured://")) return { workspaceId: "", relativePath: "" };
  const rest = text.slice("desktop-structured://".length);
  const slash = rest.indexOf("/");
  if (slash <= 0) return { workspaceId: "", relativePath: "" };
  return {
    workspaceId: safeCacheKey(rest.slice(0, slash)),
    relativePath: decodeURIComponent(rest.slice(slash + 1))
  };
}

function sanitizeTableHtml(html) {
  return textValue(html)
    .replace(/<(script|iframe|object|embed|style)[^>]*>.*?<\/\1>/gis, "")
    .replace(/\son\w+\s*=\s*(['"]).*?\1/gi, "")
    .replace(/javascript:/gi, "");
}

function normalizeStructuredText(value) {
  return cleanStructuredAcademicMathText(textValue(value)
    .replace(/<[^>]+>/gis, "")
    .replaceAll("&nbsp;", " ")
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replace(/\s+/g, " ")
    .trim());
}

function cleanStructuredAcademicMathText(text) {
  return textValue(text)
    .replace(/\$([^$]{1,220})\$/g, (_match, body) => normalizeStructuredInlineLatex(body))
    .replace(/\s+([,.;:，。；：])/g, "$1")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function normalizeStructuredInlineLatex(body) {
  let value = textValue(body);
  const original = value;
  value = value
    .replace(/\\boldsymbol\s*\{\s*\\mathbf\s*\{\s*\\ell\s*\}\s*\}\s*_\s*\{\s*-\s*\}/gi, "-")
    .replace(/\\boldsymbol\s*\{\s*\\ell\s*\}\s*_\s*\{\s*-\s*\}/gi, "-")
    .replace(/\\circ/g, "°")
    .replace(/\\pm/g, "±")
    .replace(/\\times/g, "×")
    .replace(/\\cdot/g, "·")
    .replace(/\\,/g, " ")
    .replace(/~/g, " ");
  for (let i = 0; i < 4; i += 1) {
    value = value.replace(/\\(?:mathrm|mathbf|mathit|text|boldsymbol)\s*\{\s*([^{}]+?)\s*\}/gi, "$1");
  }
  value = value
    .replace(/\^\s*\{\s*°\s*\}/g, "°")
    .replace(/_\s*\{\s*([0-9+\-]+)\s*\}/g, (_match, token) => structuredSubscript(token))
    .replace(/\^\s*\{\s*([0-9+\-]+)\s*\}/g, (_match, token) => structuredSuperscript(token))
    .replace(/[{}]/g, " ")
    .replace(/\\/g, "")
    .replace(/\s+/g, " ")
    .trim();
  value = value
    .replace(/(?<=\d)\s+(?=\d)/g, "")
    .replace(/\s+([₀-₉⁺⁻])/g, "$1")
    .replace(/\b([A-Z])(?:\s+([A-Z]))+(?=[₀-₉⁺⁻\b])/g, match => match.replace(/\s+/g, ""))
    .replace(/°\s*([A-Za-z])/g, "°$1")
    .replace(/-\s+([A-Za-z])/g, "-$1")
    .replace(/\s+([,.;:，。；：])/g, "$1")
    .trim();
  if (/\\(?:frac|sum|int|sqrt|begin|end)/i.test(original)) {
    return value || original;
  }
  return value || original;
}

function structuredSubscript(token) {
  const map = { "0": "₀", "1": "₁", "2": "₂", "3": "₃", "4": "₄", "5": "₅", "6": "₆", "7": "₇", "8": "₈", "9": "₉", "+": "₊", "-": "₋" };
  return textValue(token).split("").map(char => map[char] || char).join("");
}

function structuredSuperscript(token) {
  const map = { "0": "⁰", "1": "¹", "2": "²", "3": "³", "4": "⁴", "5": "⁵", "6": "⁶", "7": "⁷", "8": "⁸", "9": "⁹", "+": "⁺", "-": "⁻" };
  return textValue(token).split("").map(char => map[char] || char).join("");
}

function isEquationNumberArtifact(text) {
  const normalized = normalizeStructuredText(text)
    .replace(/\s+/g, "")
    .replaceAll("（", "(")
    .replaceAll("）", ")")
    .replaceAll("þ", "Þ");
  return /^[ð(]\d{1,3}[Þ)]$/.test(normalized);
}

function appendEquationNumber(blocks, text) {
  const number = textValue(text).match(/\d{1,3}/)?.[0] || "";
  if (!number) return;
  for (let index = blocks.length - 1; index >= 0; index -= 1) {
    if (blocks[index]?.kind === "equation") {
      blocks[index].equationNumber = `(${number})`;
      return;
    }
  }
}

function isPublicationNoise(text) {
  const normalized = normalizeStructuredText(text).toLowerCase();
  return normalized.startsWith("©")
    || normalized === "crossmark"
    || normalized.includes("check for updates")
    || normalized.includes("published by elsevier")
    || normalized.startsWith("peer-review under responsibility")
    || /^www\..+\/(locate|journal)\//.test(normalized);
}

function isDecorativeFigure(kind, text, item) {
  if (kind !== "figure") return false;
  const normalized = normalizeStructuredText(text).toLowerCase();
  if (normalized.includes("check for updates")
    || normalized.includes("crossmark")
    || normalized.includes("publisher logo")
    || normalized.includes("journal logo")
    || normalized.includes("sciencedirect")
    || normalized.includes("elsevier")
    || normalized.includes("creative commons")
    || normalized.includes("open access")) {
    return true;
  }
  const left = bboxNumber(item?.bbox, 0);
  const top = bboxNumber(item?.bbox, 1);
  const right = bboxNumber(item?.bbox, 2);
  const bottom = bboxNumber(item?.bbox, 3);
  const width = Math.max(0, right - left);
  const height = Math.max(0, bottom - top);
  if (!width || !height) return false;
  const hasCaption = Boolean(normalized);
  const tinyIcon = width <= 90 && height <= 90;
  const smallUncaptionedAsset = !hasCaption && width * height <= 12000 && Math.max(width, height) <= 160;
  const firstPageTopRightLogo = integerValue(item?.page_idx, 0) === 0
    && !hasCaption
    && left >= 300
    && top <= 260
    && width <= 220
    && height <= 180;
  return tinyIcon || smallUncaptionedAsset || firstPageTopRightLogo;
}

function bboxNumber(bbox, index) {
  if (!Array.isArray(bbox) || bbox.length <= index) return 0;
  return Number(bbox[index]) || 0;
}

function integerValue(value, fallback = 0) {
  const parsed = Number.parseInt(String(value ?? ""), 10);
  return Number.isFinite(parsed) ? parsed : fallback;
}

async function tailFile(filePath, maxChars) {
  try {
    const content = await fs.promises.readFile(filePath, "utf8");
    return content.length <= maxChars ? content : content.slice(content.length - maxChars);
  } catch {
    return "";
  }
}

function mimeTypeForFile(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  if ([".png"].includes(ext)) return "image/png";
  if ([".jpg", ".jpeg"].includes(ext)) return "image/jpeg";
  if ([".webp"].includes(ext)) return "image/webp";
  if ([".gif"].includes(ext)) return "image/gif";
  if ([".svg"].includes(ext)) return "image/svg+xml";
  return "application/octet-stream";
}

function pdfMathBaseUrl(settings) {
  return normalizeApiBaseUrl(activePdfMathBaseUrl)
    || normalizeApiBaseUrl(settings?.pdfMathTranslateBaseUrl)
    || localDependencyHealthUrl()
    || DEFAULT_PDFMATH_BASE_URL;
}

function localDependencyHealthUrl() {
  const manifest = readLocalDependencyManifest();
  const service = Array.isArray(manifest.services)
    ? manifest.services.find((item) => item && (item.id === "pdfmath" || item.default !== false))
    : null;
  return normalizeApiBaseUrl(service?.healthUrl);
}

function pdfMathBaseUrlCandidates(settings = {}) {
  const values = [
    activePdfMathBaseUrl,
    settings?.pdfMathTranslateBaseUrl,
    localDependencyHealthUrl(),
    DEFAULT_PDFMATH_BASE_URL,
    "http://127.0.0.1:11009",
    "http://127.0.0.1:11010"
  ].map(normalizeApiBaseUrl).filter(Boolean);
  return [...new Set(values)];
}

async function findRunningPdfBridge(settings = {}, timeoutMs = 1200) {
  let stale = false;
  for (const baseUrl of pdfMathBaseUrlCandidates(settings)) {
    const result = await probePdfService(baseUrl, timeoutMs);
    if (result.bridgeReady) return { ...result, baseUrl };
    if (result.reachable) stale = true;
  }
  return { bridgeReady: false, reachable: stale, staleService: stale, baseUrl: "" };
}

async function resolvePdfMathBaseUrlForStart(settings = {}) {
  for (const baseUrl of pdfMathBaseUrlCandidates(settings)) {
    const result = await probePdfService(baseUrl, 800);
    if (result.bridgeReady) return baseUrl;
    if (!result.reachable) return baseUrl;
  }
  return "http://127.0.0.1:11011";
}

async function probePdfService(baseUrl, timeoutMs = 1200) {
  if (!baseUrl) return { reachable: false, bridgeReady: false };
  try {
    const response = await fetchWithTimeout(baseUrl, {
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
      }
    }, timeoutMs);
    const health = await safeJsonFromResponse(response);
    return {
      reachable: Boolean(response.ok || response.status < 500),
      bridgeReady: Boolean(health?.paperSolverBridge),
      googleBridge: Boolean(health?.googleBridge),
      health
    };
  } catch (error) {
    return { reachable: false, bridgeReady: false, error: error?.message || String(error) };
  }
}

function desktopDualPdfPath(workspaceId) {
  return path.join(pdfCacheDir(), "translations", `${workspaceId}-dual-v2.pdf`);
}

function desktopDualPdfContentPath(cacheKey) {
  const normalized = String(cacheKey || "").toLowerCase();
  if (!/^[a-f0-9]{64}$/.test(normalized)) return "";
  return path.join(pdfCacheDir(), "translations", "by-content", `${normalized}-dual-v3.pdf`);
}

function pdfContentFingerprint(buffer) {
  return createHash("sha256").update(buffer).digest("hex");
}

function findDesktopDualPdfPath(workspaceId, cacheKey = "") {
  const candidates = [
    desktopDualPdfPath(workspaceId),
    desktopDualPdfContentPath(cacheKey)
  ].filter(Boolean);
  return candidates.find((candidate) => fs.existsSync(candidate)) || "";
}

async function materializeDesktopDualPdfCache(workspaceId, cacheKey, sourcePath) {
  const workspacePath = desktopDualPdfPath(workspaceId);
  if (path.resolve(sourcePath) !== path.resolve(workspacePath)) {
    const buffer = await fs.promises.readFile(sourcePath);
    if (!looksLikePdfBuffer(buffer)) {
      await fs.promises.rm(sourcePath, { force: true }).catch(() => {});
      throw new Error("本机双语 PDF 缓存已损坏，请重新生成。");
    }
    await writeFileAtomic(workspacePath, buffer);
  }
  const contentPath = desktopDualPdfContentPath(cacheKey);
  if (contentPath && path.resolve(sourcePath) !== path.resolve(contentPath) && !fs.existsSync(contentPath)) {
    const buffer = await fs.promises.readFile(sourcePath);
    await writeFileAtomic(contentPath, buffer);
  }
}

async function writeDesktopDualPdfCache(workspaceId, cacheKey, buffer) {
  if (!looksLikePdfBuffer(buffer)) {
    throw new Error("本机双语 PDF 缓存写入失败：不是有效 PDF。");
  }
  await writeFileAtomic(desktopDualPdfPath(workspaceId), buffer);
  const contentPath = desktopDualPdfContentPath(cacheKey);
  if (contentPath) await writeFileAtomic(contentPath, buffer);
}

function buildPdfMathMultipartBody(boundary, payload) {
  const fileName = normalizedPdfName(payload.fileName).replace(/"/g, "'");
  const data = JSON.stringify(payload.data || {});
  return Buffer.concat([
    Buffer.from(
      `--${boundary}\r\n`
      + `Content-Disposition: form-data; name="file"; filename="${fileName}"\r\n`
      + "Content-Type: application/pdf\r\n\r\n",
      "utf8"
    ),
    payload.pdfBuffer,
    Buffer.from(
      `\r\n--${boundary}\r\n`
      + "Content-Disposition: form-data; name=\"data\"\r\n"
      + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
      + `${data}\r\n`
      + `--${boundary}--\r\n`,
      "utf8"
    )
  ]);
}

async function safeResponseText(response) {
  try {
    const text = await response.text();
    return textValue(text).slice(0, 300) || "无错误详情";
  } catch {
    return "无错误详情";
  }
}

async function safeJsonFromResponse(response) {
  try {
    return await response.clone().json();
  } catch {
    return null;
  }
}

async function fetchWithTimeout(url, options = {}, timeoutMs = 2500) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function requestJson(url, options = {}) {
  const response = await fetchWithTimeout(url, {
    headers: { Accept: "application/json" }
  }, options.timeoutMs || 8000);
  if (!response.ok) {
    throw new Error(`更新清单读取失败：HTTP ${response.status}`);
  }
  return response.json();
}


const desktopRateLimiters = new Map();

function checkDesktopRateLimit(provider, limitPerMinute) {
  const now = Date.now();
  if (!desktopRateLimiters.has(provider)) {
    desktopRateLimiters.set(provider, []);
  }
  const timestamps = desktopRateLimiters.get(provider);
  const activeTimestamps = timestamps.filter(t => now - t < 60000);
  if (activeTimestamps.length >= limitPerMinute) {
    throw new Error("该接口已饱和，请替换其他接口");
  }
  activeTimestamps.push(now);
  desktopRateLimiters.set(provider, activeTimestamps);
}

async function translateWithDesktopProvider(provider, text, sourceLang, targetLang, settings, options = {}) {
  const limits = {
    "google": 300,
    "google-web": 300,
    "bing": 200,
    "youdao": 200,
    "tencent-transmart": 300,
    "huoshanweb": 180,
    "huoshan-web": 180
  };
  if (limits[provider] !== undefined && !options.pdfTranslationBridge) {
    checkDesktopRateLimit(provider, limits[provider]);
  }

  if (provider === "google" || provider === "google-web") {
    return translateWithGoogleWeb(text, sourceLang, targetLang, settings, options);
  }
  if (provider === "youdao") {
    return translateWithYoudaoWeb(text);
  }
  if (provider === "bing") {
    return translateWithBingWeb(text, sourceLang, targetLang);
  }
  if (provider === "tencent-transmart") {
    return translateWithTencentTransmartWeb(text, sourceLang, targetLang);
  }
  if (provider === "huoshanweb" || provider === "huoshan-web") {
    return translateWithHuoshanWeb(text, sourceLang, targetLang);
  }
  const endpoint = translationEndpointFor(provider, settings);
  if (!endpoint) {
    throw new Error(`${DESKTOP_TRANSLATION_LABELS[provider] || provider} 未配置本机服务地址`);
  }
  if (provider === "deeplx") {
    return translateWithDesktopDeepLX(text, sourceLang, targetLang, endpoint);
  }
  if (provider === "libretranslate") {
    return translateWithDesktopLibreTranslate(text, sourceLang, targetLang, endpoint);
  }
  if (provider === "mtranserver") {
    return translateWithDesktopMTranServer(text, sourceLang, targetLang, endpoint);
  }
  throw new Error(`桌面端暂不支持该本机翻译引擎：${provider}`);
}

function logTranslation(message) {
  const logFile = path.join(runtimeLogDirectory(), "translation.log");
  const timestamp = new Date().toISOString();
  const line = `[${timestamp}] ${message}\n`;
  try {
    fs.appendFileSync(logFile, line);
  } catch (err) {
    // Ignore log errors
  }
}

function looksLikeUntranslatedPdfBridgeText(sourceText, translatedText) {
  const source = textValue(sourceText).replace(/\s+/g, " ").trim().toLowerCase();
  const result = textValue(translatedText).replace(/\s+/g, " ").trim().toLowerCase();
  if (source.length < 24 || !/[a-zà-ž]/i.test(source) || /[\u3400-\u9fff]/.test(source)) return false;
  if (source === result) return true;
  return source.length >= 90 && result.includes(source);
}

async function verifyDesktopTranslationBridge(provider = "tencent-transmart") {
  try {
    const result = await translateWithDesktopFallback(
      provider,
      "PaperSolver network check",
      "en",
      "zh-CN",
      readDesktopSettings(),
      {}
    );
    return result.actualProvider || provider;
  } catch (error) {
    const detail = String(error?.message || "连接超时");
    throw new Error(`${DESKTOP_TRANSLATION_LABELS[provider] || provider} 翻译接口不可用。${detail}`);
  }
}

async function translateWithDesktopFallback(provider, text, sourceLang, targetLang, settings, options = {}) {
  const candidates = desktopTranslationFallbackChain(provider, settings, options);
  const errors = [];

  logTranslation(`--- New Translation Request ---`);
  logTranslation(`Requested Provider: ${provider}`);
  logTranslation(`Text Length: ${text.length}`);
  logTranslation(`Source Lang: ${sourceLang}, Target Lang: ${targetLang}`);
  logTranslation(`Fallback Chain: ${JSON.stringify(candidates)}`);

  for (const candidate of candidates) {
    try {
      logTranslation(`Attempting candidate: ${candidate}...`);
      const startTime = Date.now();
      let providerResult;
      for (let attempt = 1; attempt <= (options.pdfTranslationBridge ? 2 : 1); attempt += 1) {
        try {
          providerResult = await translateWithDesktopProvider(candidate, text, sourceLang, targetLang, settings, options);
          break;
        } catch (error) {
          const retryable = options.pdfTranslationBridge
            && attempt < 2
            && /busy|temporar|engine is not found|限流|繁忙|超时|timeout/i.test(String(error?.message || ""));
          if (!retryable) throw error;
          logTranslation(`Retrying candidate ${candidate} after transient bridge error: ${error.message}`);
          await new Promise(resolve => setTimeout(resolve, 900));
        }
      }
      const elapsed = Date.now() - startTime;
      const translatedText = typeof providerResult === "object" && providerResult !== null
        ? providerResult.translatedText
        : providerResult;
      if (options.pdfTranslationBridge && looksLikeUntranslatedPdfBridgeText(text, translatedText)) {
        throw new Error(`${DESKTOP_TRANSLATION_LABELS[candidate] || candidate} 返回原文`);
      }
      logTranslation(`Success with candidate: ${candidate} in ${elapsed}ms`);
      return {
        translatedText,
        actualProvider: candidate,
        route: typeof providerResult === "object" && providerResult !== null ? providerResult.route : undefined,
        latencyMs: elapsed,
        networkProfile: typeof providerResult === "object" && providerResult !== null ? providerResult.networkProfile : undefined
      };
    } catch (error) {
      logTranslation(`Failed candidate ${candidate}: ${error.message}`);
      errors.push(`${DESKTOP_TRANSLATION_LABELS[candidate] || candidate}: ${error.message}`);
    }
  }

  if (provider === "google" || provider === "google-web") {
    throw new Error(`当前网络无法连接谷歌翻译：${errors.at(-1) || "所有直连请求均失败"}`);
  }
  throw new Error(errors.at(-1) || "该接口暂不可用，请稍后重试");
}

function desktopTranslationFallbackChain(provider, settings, options = {}) {
  const chain = [provider];

  // A single PDF should normally use one engine, but a provider outage must
  // not leave a long-running job dead after hundreds of pages/blocks. The
  // pdf2zh bridge reads translation-provider.json for every request, so we
  // can switch the whole job to the next healthy domestic provider before
  // submitting the next block.
  if (options.pdfTranslationBridge) {
    return [provider, "youdao", "huoshanweb", "bing"].filter((item, index, list) => list.indexOf(item) === index);
  }

  if (provider === "tencent-transmart") {
    return ["tencent-transmart", "youdao"];
  }

  if (provider === "google" || provider === "google-web") {
    return ["google"];
  }

  const fallbacks = ["youdao", "huoshanweb", "bing", "tencent-transmart", "google"];
  for (const fallback of fallbacks) {
    if (!chain.includes(fallback)) {
      chain.push(fallback);
    }
  }
  return chain;
}

// Google direct routes stay first. Proxy hosts are only fallback capacity for
// networks where direct Google is unavailable.
const GOOGLE_HOSTS = [
  "translate.googleapis.com",
  "translate.google.com",
  "clients5.google.com",
  "translate.google.co.jp",
  "translate.google.com.hk",
  "translate.google.com.sg"
];

const GOOGLE_USER_AGENTS = [
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0",
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0",
  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
  "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
];

async function translateWithHuoshanWeb(text, sourceLang, targetLang) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];

  for (const chunk of chunks) {
    const from = sourceLang.toLowerCase() === "auto" ? "" : sourceLang.split("-")[0];
    const to = targetLang.toLowerCase() === "auto" ? "zh" : targetLang.split("-")[0];
    const url = "https://translate.volcengine.com/crx/translate/v1/";

    try {
      const response = await fetchNative(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        },
        body: JSON.stringify({
          source_language: from,
          target_language: to,
          text: chunk
        })
      });

      if (!response.ok) {
        throw new Error(`火山翻译返回 HTTP ${response.status}`);
      }

      const result = await response.json();
      const translation = result?.translation;
      if (translation) {
        translatedChunks.push(translation.trim());
      } else {
        throw new Error("火山翻译未返回译文");
      }
    } catch (err) {
      throw new Error(`火山翻译失败: ${err.message}`);
    }
  }

  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("火山翻译未返回译文");
  return result;
}

let googleOnline = true;
let lastGoogleFailureTime = 0;
let lastSuccessfulGoogleAttempt = null;
let googleProxyCursor = 0;
let googleNetworkDiagnosticsLogged = false;
const googleDnsResolver = new dns.Resolver();
googleDnsResolver.setServers(["1.1.1.1", "1.0.0.1"]);
const googleChinaDnsResolver = new dns.Resolver();
googleChinaDnsResolver.setServers(["223.5.5.5", "223.6.6.6"]);

function isGoogleAvailable() {
  if (googleOnline) return true;
  if (Date.now() - lastGoogleFailureTime > 10000) { // 10 seconds
    return true;
  }
  return false;
}

async function translateWithGoogleWeb(text, sourceLang, targetLang, settings = {}, options = {}) {
  if (!isGoogleAvailable()) {
    throw new Error("谷歌翻译目前不可用（网络连接刚刚失败，10 秒后会再次检测）");
  }
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  const successfulRoutes = [];

  await logGoogleNetworkDiagnostics();

  for (const chunk of chunks) {
    // Stagger delay to prevent burst limits
    await new Promise(resolve => setTimeout(resolve, 20 + Math.floor(Math.random() * 30)));

    let lastError = null;
    let success = false;

    // Attempts prioritize direct Google routes with Cloudflare DNS. Traffic still
    // exits from the user's own desktop network, which naturally distributes IPs.
    const nodeDirectAttempts = [
      googleAttempt("translate.googleapis.com", "/translate_a/single", "gtx", "cloudflare"),
      googleAttempt("translate.googleapis.com", "/translate_a/single", "gtx", "aliyun"),
      googleAttempt("translate.google.com", "/translate_a/single", "gtx", "cloudflare"),
      googleAttempt("translate.google.com", "/translate_a/single", "gtx", "aliyun"),
      googleAttempt("clients5.google.com", "/translate_a/t", "dict-chrome-ex", "cloudflare"),
      googleAttempt("clients5.google.com", "/translate_a/t", "dict-chrome-ex", "aliyun"),
      googleAttempt("translate.googleapis.com", "/translate_a/single", "at", "cloudflare"),
      googleAttempt("translate.google.com", "/translate_a/single", "at", "cloudflare"),
      googleAttempt("translate.googleapis.com", "/translate_a/single", "gtx", "system"),
      googleAttempt("translate.google.com", "/translate_a/single", "gtx", "system"),
      googleAttempt("clients5.google.com", "/translate_a/t", "dict-chrome-ex", "system")
    ];
    const chromiumAttempts = [
      googleAttempt("translate.googleapis.com", "/translate_a/single", "gtx", "electron"),
      googleAttempt("translate.google.com", "/translate_a/single", "gtx", "electron")
    ];
    // On Windows Chromium follows the user's system proxy and network policy,
    // while Node's HTTPS stack does not. Prefer it there; this remains a direct
    // Google request and does not switch translation providers.
    const directAttempts = process.platform === "win32"
      ? [...chromiumAttempts, ...nodeDirectAttempts]
      : [...nodeDirectAttempts, ...chromiumAttempts];
    const proxyAttempts = googleProxyHosts(settings).flatMap(host => [
      { host, path: "/translate_a/t", client: "dict-chrome-ex", fetcher: fetchNative, proxy: true },
      { host, path: "/translate_a/single", client: "gtx", fetcher: fetchNative, proxy: true },
      { host, path: "/translate_a/single", client: "at", fetcher: fetchNative, proxy: true }
    ]);
    const attempts = options.googleBridgeProbe
      ? directAttempts.slice(0, process.platform === "win32" ? 6 : 4)
      : [...directAttempts, ...proxyAttempts];

    // Cache optimization: Move the last successful attempt to the front to avoid timeouts on subsequent chunks
    if (lastSuccessfulGoogleAttempt) {
      const idx = attempts.findIndex(
        a => a.host === lastSuccessfulGoogleAttempt.host &&
             a.path === lastSuccessfulGoogleAttempt.path &&
             a.client === lastSuccessfulGoogleAttempt.client &&
             a.fetcher === lastSuccessfulGoogleAttempt.fetcher &&
             a.lookupName === lastSuccessfulGoogleAttempt.lookupName
      );
      if (idx > -1) {
        const [matched] = attempts.splice(idx, 1);
        attempts.unshift(matched);
      }
    }

    logTranslation(`Google translate starting: text length ${chunk.length}`);
    for (let attempt = 0; attempt < attempts.length; attempt++) {
      const opt = attempts[attempt];

      const userAgent = opt.client === "at"
        ? "AndroidTranslate/5.9.0.RC02.155752102 (Linux;U;Android 7.1.1;SAMSUNG-SM-G935A Build/MMB29M)"
        : GOOGLE_USER_AGENTS[Math.floor(Math.random() * GOOGLE_USER_AGENTS.length)];
      const sl = sourceLang.toLowerCase() === "auto" ? "auto" : sourceLang;

      let url = `https://${opt.host}${opt.path}?client=${opt.client}&sl=${encodeURIComponent(sl)}&tl=${encodeURIComponent(targetLang)}&q=${encodeURIComponent(chunk)}`;
      if (opt.client === "gtx") {
        url += "&dt=t";
      } else if (opt.client === "at") {
        url += "&dt=t&dt=ld&dt=qca&dt=rm&dt=bd&dj=1";
      }

      const controller = new AbortController();
      const timeoutMs = options.googleBridgeProbe ? 2000 : (opt.proxy ? 5000 : 1800);
      const timeoutId = setTimeout(() => {
        logTranslation(`Google attempt to ${opt.host} (${opt.client}) timed out after ${timeoutMs}ms`);
        controller.abort();
      }, timeoutMs);

      const startTime = Date.now();
      try {
        logTranslation(`Trying Google host: ${opt.host} (attempt ${attempt + 1}/${attempts.length}) using ${opt.fetcher === fetchNative ? 'fetchNative' : 'netFetch'} with client=${opt.client}, dns=${opt.lookupName || "default"}`);
        const requestOptions = {
          method: "GET",
          signal: controller.signal,
          headers: {
            "Accept": "application/json,text/plain,*/*",
            "User-Agent": userAgent,
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Cache-Control": "no-cache",
            "sec-ch-ua": '"Google Chrome";v="125", "Chromium";v="125", "Not.A/Brand";v="24"',
            "sec-ch-ua-mobile": "?0",
            "sec-ch-ua-platform": '"Windows"',
            "sec-fetch-dest": "empty",
            "sec-fetch-mode": "cors",
            "sec-fetch-site": "none"
          }
        };
        if (opt.fetcher === netFetch) {
          // Electron's net.fetch validates Chromium-only request headers more
          // strictly on Windows. Keep its request minimal so it can use the
          // system proxy/PAC configuration instead of failing locally.
          requestOptions.headers = {
            "Accept": "application/json,text/plain,*/*",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "User-Agent": userAgent,
            "Cache-Control": "no-cache"
          };
        }
        if (opt.fetcher === fetchNative) {
          requestOptions.lookup = opt.lookup;
          requestOptions.servername = opt.host;
        }
        const response = await opt.fetcher(url, requestOptions);
        clearTimeout(timeoutId);

        const duration = Date.now() - startTime;
        logTranslation(`Google host ${opt.host} responded in ${duration}ms with status ${response.status}`);

        const rawBody = await response.text();
        logTranslation(`Google host ${opt.host} response body (first 200 chars): ${rawBody.slice(0, 200)}`);

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${rawBody.slice(0, 100)}`);
        }

        let root;
        try {
          root = JSON.parse(rawBody);
        } catch (jsonErr) {
          throw new Error(`JSON parse failed: ${jsonErr.message}`);
        }

        const textPart = extractGoogleWebTranslation(root);
        if (textPart) {
          translatedChunks.push(textPart.trim());
          success = true;
          googleOnline = true;
          lastSuccessfulGoogleAttempt = { host: opt.host, path: opt.path, client: opt.client, fetcher: opt.fetcher, lookupName: opt.lookupName };
          successfulRoutes.push(`${opt.host}/${opt.client}/${opt.lookupName || (opt.proxy ? "proxy" : "default")}`);
          logTranslation(`Google host ${opt.host} translation successful`);
          break;
        } else {
          throw new Error("Empty translation in response");
        }
      } catch (err) {
        clearTimeout(timeoutId);
        const duration = Date.now() - startTime;
        lastError = err;
        logTranslation(`Google host ${opt.host} failed after ${duration}ms. Error: ${err.message}`);

        if (opt.proxy) {
          const delay = Math.pow(2, attempt) * 100 + Math.floor(Math.random() * 100);
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }
    }

    if (!success) {
      googleOnline = false;
      lastGoogleFailureTime = Date.now();
      throw new Error(`谷歌翻译重试耗尽失败: ${lastError ? lastError.message : "未知错误"}`);
    }
  }

  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("未获取到译文");
  return {
    translatedText: result,
    route: [...new Set(successfulRoutes)].join(", "),
    networkProfile: "desktop-direct-google-dns"
  };
}

function googleAttempt(host, path, client, route = "cloudflare") {
  if (route === "electron") {
    return { host, path, client, fetcher: netFetch, lookupName: "electron" };
  }
  return {
    host,
    path,
    client,
    fetcher: fetchNative,
    lookup: route === "cloudflare" ? cloudflareGoogleLookup : route === "aliyun" ? aliyunGoogleLookup : undefined,
    lookupName: route
  };
}

function cloudflareGoogleLookup(hostname, options, callback) {
  resolveGoogleHost(googleDnsResolver, hostname, options, callback, "Cloudflare");
}

function aliyunGoogleLookup(hostname, options, callback) {
  resolveGoogleHost(googleChinaDnsResolver, hostname, options, callback, "Aliyun");
}

function resolveGoogleHost(resolver, hostname, options, callback, label) {
  resolver.resolve4(hostname, (error, addresses) => {
    if (error || !Array.isArray(addresses) || addresses.length === 0) {
      callback(error || new Error(`${label} DNS returned no A record for ${hostname}`));
      return;
    }
    logTranslation(`${label} DNS ${hostname} -> ${addresses.join(", ")}`);
    if (options?.all) {
      callback(null, addresses.map(address => ({ address, family: 4 })));
      return;
    }
    const address = addresses[Math.floor(Math.random() * addresses.length)];
    callback(null, address, 4);
  });
}

async function logGoogleNetworkDiagnostics() {
  if (googleNetworkDiagnosticsLogged) return;
  googleNetworkDiagnosticsLogged = true;
  logTranslation(`Google network diagnostic: platform=${process.platform}, release=${process.release.name || "node"}`);
  try {
    const records = await dns.promises.lookup("translate.googleapis.com", { all: true, verbatim: true });
    logTranslation(`System DNS translate.googleapis.com -> ${records.map(record => `${record.address} (IPv${record.family})`).join(", ") || "no records"}`);
  } catch (error) {
    logTranslation(`System DNS translate.googleapis.com failed: ${error.message}`);
  }
  try {
    const proxy = await session.defaultSession.resolveProxy("https://translate.googleapis.com");
    logTranslation(`Electron system proxy for Google: ${proxy || "DIRECT"}`);
  } catch (error) {
    logTranslation(`Electron system proxy lookup failed: ${error.message}`);
  }
}

function googleProxyHosts(settings = {}) {
  const fromSettings = Array.isArray(settings.googleTranslateProxyHosts)
    ? settings.googleTranslateProxyHosts
    : [];
  const fromEnv = textValue(process.env.PAPER_SOLVER_GOOGLE_PROXY_HOSTS)
    .split(",")
    .map(item => item.trim())
    .filter(Boolean);
  const hosts = [...fromSettings, ...fromEnv, ...DEFAULT_GOOGLE_PROXY_HOSTS]
    .map(cleanGoogleProxyHost)
    .filter(Boolean)
    .filter((host, index, list) => list.indexOf(host) === index);
  if (!hosts.length) return [];
  const start = googleProxyCursor % hosts.length;
  googleProxyCursor += 1;
  return hosts.slice(start).concat(hosts.slice(0, start));
}

function cleanGoogleProxyHost(value) {
  const text = textValue(value).replace(/^https?:\/\//i, "").replace(/\/.*$/, "");
  if (!text || /[\s/]/.test(text)) return "";
  return text;
}

async function translateWithYoudaoWeb(text) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const url = "https://dict.youdao.com/jsonapi_s"
      + `?doctype=json&jsonversion=4&q=${encodeURIComponent(chunk)}`;
    const response = await fetchNative(url, {
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
      }
    });
    if (!response.ok) {
      throw new Error(`有道翻译返回 ${response.status}`);
    }
    const root = await response.json();
    const translated = textValue(root?.fanyi?.tran)
      || firstJsonText(root, "tran", "translation", "translatedText", "value");
    if (!translated) throw new Error(`有道翻译第 ${translatedChunks.length + 1} 段未返回译文`);
    translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("有道翻译未返回译文");
  return result;
}

async function translateWithBingWeb(text, sourceLang, targetLang) {
  const UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
  logTranslation("Bing: Fetching translator main page using fetchNative...");
  // Use fetchNative so it runs standard Node request directly
  const pageResp = await fetchNative("https://www.bing.com/translator", {
    headers: { "User-Agent": UA, "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8" }
  });
  logTranslation(`Bing main page response status: ${pageResp.status}`);
  if (!pageResp.ok) throw new Error(`微软翻译页面请求失败 ${pageResp.status}`);
  const pageHtml = await pageResp.text();
  logTranslation(`Bing main page HTML length: ${pageHtml.length}`);

  // 提取 IG（页面指纹）
  const igMatch = pageHtml.match(/IG:"([A-F0-9]+)"/i);
  const ig = igMatch ? igMatch[1] : "";

  // 提取 IID（实例ID）
  const iidMatch = pageHtml.match(/data-iid="([^"]+)"/);
  const iid = iidMatch ? iidMatch[1] : "translator.5024";

  // 提取 key 和 token（页面内嵌 params_AbusePreventionHelper = [key, "token", timeout]）
  const helperMatch = pageHtml.match(/params_AbusePreventionHelper\s*=\s*\[\s*(-?\d+)\s*,\s*"([^"]+)"/);
  const key = helperMatch ? helperMatch[1] : "";
  const pageToken = helperMatch ? helperMatch[2] : "";
  logTranslation(`Bing extracted params - IG: ${ig || 'null'}, IID: ${iid}, Key: ${key || 'null'}, Token: ${pageToken || 'null'}`);

  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const from = mapMicrosoftLang(sourceLang);
    const to = mapMicrosoftLang(targetLang);
    const params = new URLSearchParams({
      fromLang: from === "auto" ? "auto-detect" : from,
      to,
      tryFetchingGenderDebiasedTranslations: "true"
    });
    if (key) params.set("key", key);
    if (pageToken) params.set("token", pageToken);
    const url = `https://www.bing.com/ttranslatev3?isVertical=1&${ig ? `IG=${ig}&` : ""}IID=${encodeURIComponent(iid)}`;
    const body = new URLSearchParams({ fromLang: from === "auto" ? "auto-detect" : from, to, text: chunk });
    if (key) body.set("key", key);
    if (pageToken) body.set("token", pageToken);

    // Bing is accessible from China via Node fetchNative directly (no proxy needed)
    const response = await fetchNative(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": UA,
        "Referer": "https://www.bing.com/translator",
        "Origin": "https://www.bing.com"
      },
      body: body.toString()
    });
    // Read body as text FIRST (stream can only be consumed once)
    const rawBody = await response.text();
    logTranslation(`Bing response status: ${response.status}, body length: ${rawBody.length}, preview: ${rawBody.slice(0, 100)}`);
    if (!response.ok) throw new Error(`微软翻译接口返回 ${response.status}: ${rawBody.slice(0, 80)}`);
    let root;
    try {
      root = JSON.parse(rawBody);
    } catch (err) {
      throw new Error(`JSON解析失败，返回状态: ${response.status}，返回内容: ${rawBody.slice(0, 150)}`);
    }
    // ttranslatev3 返回格式: [{translations:[{text,to}]}]
    let translated = "";
    if (Array.isArray(root) && root[0]?.translations) {
      translated = root[0].translations.map(t => t.text).join("").trim();
    } else {
      translated = firstJsonText(root, "translations", "text", "translatedText");
    }
    if (!translated) throw new Error(`腾讯 TranSmart 第 ${translatedChunks.length + 1} 段未返回译文`);
    translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("微软翻译未返回译文");
  return result;
}

async function translateWithTencentTransmartWeb(text, sourceLang, targetLang) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const response = await fetchNative("https://transmart.qq.com/api/imt", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
        "Referer": "https://transmart.qq.com/zh-CN/index"
      },
      body: JSON.stringify({
        header: {
          fn: "auto_translation",
          client_key: "browser-chrome-110.0.0-Mac OS-df4bd4c5-a65d-44b2-a40f-42f34f3535f2-1677486696487"
        },
        type: "plain",
        model_category: "normal",
        source: {
          lang: mapTransmartLang(sourceLang),
          text_list: [chunk]
        },
        target: {
          lang: mapTransmartLang(targetLang)
        }
      })
    });
    if (!response.ok) {
      throw new Error(`腾讯 TranSmart 返回 HTTP ${response.status}`);
    }
    const data = await response.json();
    if (data?.header?.ret_code !== "succ") {
      throw new Error(data?.message || JSON.stringify(data));
    }
    const translated = data?.auto_translation?.join("\n").trim();
    if (translated) translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("腾讯 TranSmart 网页接口未返回译文");
  return result;
}

async function translateWithDesktopDeepLX(text, sourceLang, targetLang, endpoint) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const data = await postJson(endpointWithTranslatePath(endpoint), {
      text: chunk,
      source_lang: sourceLang.toLowerCase() === "auto" ? "AUTO" : mapDeepLSource(sourceLang),
      target_lang: mapDeepLTarget(targetLang)
    });
    const translated = firstJsonText(data, "data", "translation", "translatedText", "text", "result");
    if (translated) translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("DeepLX 未返回译文");
  return result;
}

async function translateWithDesktopLibreTranslate(text, sourceLang, targetLang, endpoint) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const data = await postJson(endpointWithTranslatePath(endpoint), {
      q: chunk,
      source: sourceLang.toLowerCase() === "auto" ? "auto" : mapLocalLang(sourceLang),
      target: mapLocalLang(targetLang),
      format: "text"
    });
    const translated = firstJsonText(data, "translatedText", "translation", "data", "text", "result");
    if (translated) translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("LibreTranslate 未返回译文");
  return result;
}

async function translateWithDesktopMTranServer(text, sourceLang, targetLang, endpoint) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const data = await postJson(endpointWithTranslatePath(endpoint), {
      text: chunk,
      from: sourceLang.toLowerCase() === "auto" ? "auto" : mapLocalLang(sourceLang),
      to: mapLocalLang(targetLang)
    });
    const translated = firstJsonText(data, "result", "translatedText", "translation", "data", "text");
    if (translated) translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("MTranServer 未返回译文");
  return result;
}

async function postJson(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Accept": "application/json,text/plain,*/*",
      "Content-Type": "application/json",
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    throw new Error(`本机翻译服务返回 ${response.status}`);
  }
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  return { text: await response.text() };
}

function extractGoogleWebTranslation(root) {
  if (root && typeof root === "object" && Array.isArray(root.sentences)) {
    return root.sentences
      .map(s => s.trans || "")
      .join("");
  }

  if (!Array.isArray(root) || root.length === 0) {
    return "";
  }

  // Case 1: ["你好"]
  if (typeof root[0] === "string") {
    return root[0];
  }

  if (Array.isArray(root[0])) {
    // Case 2: [["道德培训...", "en"]]
    if (typeof root[0][0] === "string") {
      return root[0][0];
    }

    // Case 3: [[["道德培训...", "Ethics...", ...]], null, "en"]
    return root[0]
      .map((segment) => {
        if (Array.isArray(segment)) {
          return textValue(segment[0]);
        }
        return typeof segment === "string" ? textValue(segment) : "";
      })
      .join("");
  }

  return "";
}

function splitTranslationTextWithLimit(text, limit = 4500) {
  if (text.length <= limit) return [text];
  const chunks = [];
  let current = "";

  // Split by double newlines first (paragraphs)
  for (const paragraph of text.split(/\n{2,}/)) {
    if (paragraph.length > limit) {
      // Flush current buffer if any
      if (current) {
        chunks.push(current);
        current = "";
      }

      // Split the oversized paragraph at sentence or space boundaries
      let start = 0;
      while (start < paragraph.length) {
        if (paragraph.length - start <= limit) {
          chunks.push(paragraph.slice(start).trim());
          break;
        }

        let end = start + limit;
        let found = false;

        // Search backwards for a sentence boundary (. , ? ! ; followed by space) up to 70% of the limit
        for (let i = end; i > start + Math.floor(limit * 0.7); i--) {
          const char = paragraph[i];
          const nextChar = paragraph[i + 1];
          if ((char === "." || char === "?" || char === "!" || char === ";" || char === ",") && nextChar === " ") {
            end = i + 1; // Include the punctuation
            found = true;
            break;
          }
        }

        // Fallback: search backwards for a space up to 50% of the limit
        if (!found) {
          for (let i = end; i > start + Math.floor(limit * 0.5); i--) {
            if (paragraph[i] === " ") {
              end = i;
              found = true;
              break;
            }
          }
        }

        chunks.push(paragraph.slice(start, end).trim());
        start = end;
      }
      continue;
    }

    if (current && current.length + paragraph.length + 2 > limit) {
      chunks.push(current);
      current = "";
    }
    current = current ? `${current}\n\n${paragraph}` : paragraph;
  }

  if (current) chunks.push(current);
  return chunks;
}

function splitTranslationText(text) {
  return splitTranslationTextWithLimit(text, MAX_TRANSLATION_CHUNK);
}

function normalizeDesktopTranslationProvider(provider) {
  const normalized = textValue(provider).toLowerCase();
  if (!normalized || normalized === "google") return "google";
  if (normalized === "google-web") return "google-web";
  if (["youdao", "bing", "microsoft-edge", "360-web", "tencent-transmart", "deeplx", "libretranslate", "mtranserver", "huoshanweb"].includes(normalized)) {
    return normalized === "microsoft-edge" ? "bing" : normalized;
  }
  throw new Error(`桌面端暂不支持该本机翻译引擎：${provider}`);
}

function normalizeTranslationLang(lang, fallback) {
  return textValue(lang) || fallback;
}

function translationEndpointFor(provider, settings) {
  const endpoints = normalizeTranslationEndpoints(settings?.translationEndpoints);
  if (provider === "deeplx") return endpoints.deeplxEndpoint;
  if (provider === "libretranslate") return endpoints.libreTranslateEndpoint;
  if (provider === "mtranserver") return endpoints.mtranServerEndpoint;
  return "";
}

function endpointWithTranslatePath(endpoint) {
  const normalized = normalizeOptionalUrl(endpoint);
  if (!normalized) return "";
  return /\/translate$/i.test(normalized) ? normalized : `${normalized}/translate`;
}

function firstJsonText(data, ...keys) {
  if (typeof data === "string") return textValue(data);
  if (!data || typeof data !== "object") return "";
  for (const key of keys) {
    const value = data[key];
    if (typeof value === "string") return textValue(value);
    if (Array.isArray(value)) {
      const joined = value.map((item) => typeof item === "string" ? item : firstJsonText(item, ...keys)).filter(Boolean).join("");
      if (joined) return textValue(joined);
    }
    if (value && typeof value === "object") {
      const nested = firstJsonText(value, ...keys);
      if (nested) return nested;
    }
  }
  return "";
}

function mapDeepLTarget(lang) {
  const normalized = mapLocalLang(lang).toUpperCase();
  if (normalized === "ZH" || normalized === "ZH-CN") return "ZH";
  if (normalized === "EN-US" || normalized === "EN-GB") return "EN";
  return normalized.split("-")[0] || "ZH";
}

function mapDeepLSource(lang) {
  return mapDeepLTarget(lang);
}

function mapLocalLang(lang) {
  const normalized = textValue(lang).toLowerCase();
  if (!normalized || normalized === "auto") return "auto";
  if (normalized.startsWith("zh")) return "zh";
  if (normalized.startsWith("en")) return "en";
  if (normalized.startsWith("ja") || normalized.startsWith("jp")) return "ja";
  if (normalized.startsWith("ko")) return "ko";
  if (normalized.startsWith("fr")) return "fr";
  if (normalized.startsWith("de")) return "de";
  if (normalized.startsWith("es")) return "es";
  if (normalized.startsWith("ru")) return "ru";
  return normalized.split("-")[0];
}

function mapMicrosoftLang(lang) {
  const normalized = textValue(lang).toLowerCase();
  if (!normalized || normalized === "auto") return "auto";
  if (normalized.startsWith("zh")) return "zh-Hans";
  if (normalized.startsWith("en")) return "en";
  if (normalized.startsWith("ja") || normalized.startsWith("jp")) return "ja";
  if (normalized.startsWith("ko")) return "ko";
  if (normalized.startsWith("fr")) return "fr";
  if (normalized.startsWith("de")) return "de";
  if (normalized.startsWith("es")) return "es";
  if (normalized.startsWith("ru")) return "ru";
  return normalized.split("-")[0] || "zh-Hans";
}

function mapTransmartLang(lang) {
  const normalized = textValue(lang).toLowerCase();
  if (!normalized || normalized === "auto") return "auto";
  if (normalized.startsWith("zh")) return "zh";
  if (normalized.startsWith("en")) return "en";
  if (normalized.startsWith("ja") || normalized.startsWith("jp")) return "ja";
  if (normalized.startsWith("ko")) return "ko";
  return normalized.split("-")[0];
}

function pdfCacheDir() {
  // Keep transient workspace/cache files outside the user's document folder.
  return path.join(app.getPath("userData"), "pdf-cache");
}

function internalPdfStorageDir() {
  return path.join(app.getPath("userData"), "PDFs");
}

function userPdfStorageDir() {
  return readDesktopSettings().pdfStorageDir || defaultPdfStorageDir();
}

function pdfCacheDirsForRead() {
  return [...new Set([
    pdfCacheDir(),
    userPdfStorageDir(),
    internalPdfStorageDir()
  ].map((item) => path.resolve(item)))];
}

function isPermissionFsError(error) {
  return ["EPERM", "EACCES"].includes(String(error?.code || "").toUpperCase());
}

function pdfFileAccessErrorMessage(filePath, error) {
  const reason = isPermissionFsError(error)
    ? "Windows 拒绝读取这个 PDF，通常是系统保护目录、杀毒软件拦截或文件正在被其他程序占用。"
    : `读取 PDF 失败：${error?.message || String(error)}`;
  return `${reason}请重新导入 PDF，或在首次配置里把 PDF 保存目录改到 PaperSolver 默认目录后重试。路径：${filePath}`;
}

async function writeFileAtomic(targetPath, buffer) {
  await fs.promises.mkdir(path.dirname(targetPath), { recursive: true });
  const tempPath = `${targetPath}.tmp-${randomUUID()}`;
  try {
    await fs.promises.writeFile(tempPath, buffer);
    await fs.promises.rename(tempPath, targetPath);
  } catch (error) {
    await fs.promises.rm(tempPath, { force: true }).catch(() => {});
    throw error;
  }
}

async function writePdfCacheFiles(cacheDir, workspaceId, buffer, metadata) {
  const pdfPath = path.join(cacheDir, `${workspaceId}.pdf`);
  const metaPath = path.join(cacheDir, `${workspaceId}.json`);
  await writeFileAtomic(pdfPath, buffer);
  await writeFileAtomic(metaPath, Buffer.from(JSON.stringify(metadata, null, 2), "utf8"));
  return pdfPath;
}

async function cachePdf(payload) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  const base64 = String(payload.base64 || "");
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法缓存 PDF。");
  }
  if (!base64) {
    throw new Error("PDF 内容为空，无法缓存。");
  }
  const buffer = Buffer.from(base64, "base64");
  assertReasonablePdfSize(buffer.length);
  if (!looksLikePdfBuffer(buffer)) {
    throw new Error("缓存失败：文件不是有效 PDF。");
  }
  const metadata = {
    workspaceId,
    fileName: normalizedPdfName(payload.fileName || `${workspaceId}.pdf`),
    mimeType: "application/pdf",
    size: buffer.length,
    cachedAt: new Date().toISOString()
  };
  let cacheDir = pdfCacheDir();
  try {
    const pdfPath = await writePdfCacheFiles(cacheDir, workspaceId, buffer, metadata);
    // The configured PDF folder is a user-facing library, so only place the
    // original PDF there. Metadata, parser outputs and temporary files stay in
    // the application cache.
    const userDir = userPdfStorageDir();
    const userPdfPath = path.join(userDir, `${workspaceId}.pdf`);
    if (path.resolve(userPdfPath) !== path.resolve(pdfPath)) {
      await writeFileAtomic(userPdfPath, buffer);
    }
    return { ok: true, workspaceId, size: buffer.length, path: userPdfPath, cachePath: pdfPath };
  } catch (error) {
    if (!isPermissionFsError(error)) throw error;
    cacheDir = internalPdfStorageDir();
    const pdfPath = await writePdfCacheFiles(cacheDir, workspaceId, buffer, metadata);
    return { ok: true, workspaceId, size: buffer.length, path: pdfPath, cachePath: pdfPath, fallbackDir: true };
  }
}

async function savePptDeck(payload = {}) {
  const url = textValue(payload.url);
  if (!/^https?:\/\//i.test(url)) {
    throw new Error("PPT 下载地址无效，无法保存到本机。");
  }
  const baseDir = path.join(userPdfStorageDir(), "PPT");
  await fs.promises.mkdir(baseDir, { recursive: true });
  const rawName = payload.fileName || payload.meetingTitle || payload.jobId || `meeting-deck-${Date.now()}`;
  const fileName = uniquePptName(baseDir, normalizedPptName(rawName));
  const targetPath = path.join(baseDir, fileName);
  await downloadFileToPath(url, targetPath, null, "PPT 文件");
  const stat = await fs.promises.stat(targetPath);
  if (stat.size <= 0) {
    await fs.promises.rm(targetPath, { force: true });
    throw new Error("PPT 文件为空，保存失败。");
  }
  return {
    ok: true,
    path: targetPath,
    fileName,
    size: stat.size,
  };
}

async function getCachedPdf(payload) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) return { found: false };
  let lastReadError = null;

  for (const cacheDir of pdfCacheDirsForRead()) {
    const paths = [
      path.join(cacheDir, `${workspaceId}.pdf`),
      path.join(cacheDir, "structured", workspaceId, "input", `${workspaceId}.pdf`)
    ];
    for (const pdfPath of paths) {
      try {
        const stat = await fs.promises.stat(pdfPath);
        if (!stat.isFile()) continue;
        const cached = await readCachedPdfFile(path.dirname(pdfPath), workspaceId, pdfPath);
        if (cached?.found) {
          if (pdfPath !== path.join(cacheDir, `${workspaceId}.pdf`)) {
            await cachePdf({
              workspaceId,
              fileName: cached.fileName,
              base64: cached.base64
            });
            return getCachedPdf({ workspaceId });
          }
          return cached;
        }
      } catch (err) {
        lastReadError = { path: pdfPath, error: err };
        if (err?.code !== "ENOENT") {
          console.error(`[Local Cache] Failed to read PDF ${pdfPath}:`, err?.message || err);
        }
      }
    }
  }

  const settings = readDesktopSettings();
  const apiBaseUrl = normalizeApiBaseUrl(settings.apiBaseUrl) || "https://papersolver.cn";
  const apiOrigin = apiBaseUrl.replace(/\/api$/i, "");
  const suppliedSourceUrls = [
    ...(Array.isArray(payload.sourceUrls) ? payload.sourceUrls : []),
    payload.sourceUrl,
    payload.pdfUrl,
    payload.paperUrl
  ].map(textValue).filter(Boolean);
  const candidates = [
    ...suppliedSourceUrls,
    `${apiOrigin}/api/papers/uploads/${workspaceId}.pdf`
  ].map((value) => {
    if (!value || value.toLowerCase().startsWith("desktop-cache://")) return "";
    if (value.startsWith("/")) return `${apiOrigin}${value}`;
    return value;
  }).filter((value, index, list) => /^https?:\/\//i.test(value) && list.indexOf(value) === index);
  const targetDir = lastReadError && isPermissionFsError(lastReadError.error) ? internalPdfStorageDir() : pdfCacheDir();
  let downloadError = null;
  for (const downloadUrl of candidates) {
    try {
      const downloaded = await downloadPdfToCache(downloadUrl, targetDir, workspaceId);
      return downloaded;
    } catch (error) {
      downloadError = error;
      console.error(`[Local Cache] Failed to download PDF from ${downloadUrl}:`, error?.message || error);
    }
  }
  try {
    throw downloadError || new Error("没有可用的 PDF 下载地址");
  } catch (error) {
    const permissionMessage = lastReadError && isPermissionFsError(lastReadError.error)
      ? pdfFileAccessErrorMessage(lastReadError.path, lastReadError.error)
      : "";
    const missingMessage = lastReadError?.error?.code === "ENOENT"
      ? `本机缓存文件不存在，且云端没有可读取的 PDF。请重新导入 PDF 或重新上传原文件。工作区：${workspaceId}`
      : "";
    const downloadMessage = error?.message || String(error);
    return {
      found: false,
      error: permissionMessage || missingMessage || `本机没有可读取的 PDF 缓存，也无法从云端取回 PDF：${downloadMessage}`,
      code: lastReadError?.error?.code || error?.code || "",
      path: lastReadError?.path || ""
    };
  }
}

async function ensureCachedPdf(payload = {}, { prompt = false } = {}) {
  const cached = await getCachedPdf(payload);
  // Translation starts from a library item. Never interrupt that flow with a
  // native file picker; the library's "关联 PDF" action is the single place
  // where a user chooses a source file.
  if (cached?.found || !prompt || payload.allowManualSelection !== true) return cached;

  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) return cached;
  const options = {
    title: "选择这篇文献的原始 PDF",
    buttonLabel: "选择 PDF",
    properties: ["openFile"],
    filters: [
      { name: "PDF 文档", extensions: ["pdf"] },
      { name: "所有文件", extensions: ["*"] }
    ]
  };
  const owner = BrowserWindow.getFocusedWindow();
  const selection = owner
    ? await dialog.showOpenDialog(owner, options)
    : await dialog.showOpenDialog(options);
  if (selection.canceled || !selection.filePaths?.[0]) {
    return {
      found: false,
      cancelled: true,
      error: "这篇文献的原始 PDF 在本机和云端都不存在。你已取消选择，请重新打开该功能并选择原始 PDF。"
    };
  }

  const selectedPath = selection.filePaths[0];
  let buffer;
  try {
    buffer = await fs.promises.readFile(selectedPath);
  } catch (error) {
    return {
      found: false,
      error: pdfFileAccessErrorMessage(selectedPath, error),
      code: error?.code || "",
      path: selectedPath
    };
  }
  assertReasonablePdfSize(buffer.length);
  if (!looksLikePdfBuffer(buffer)) {
    return { found: false, error: "选择的文件不是有效 PDF，请重新选择原始 PDF。", path: selectedPath };
  }
  await cachePdf({
    workspaceId,
    fileName: path.basename(selectedPath),
    base64: buffer.toString("base64")
  });
  return getCachedPdf({ workspaceId });
}

async function readCachedPdfFile(cacheDir, workspaceId, pdfPath) {
  const buffer = await fs.promises.readFile(pdfPath);
  if (!looksLikePdfBuffer(buffer)) return { found: false };
  let meta = {};
  try {
    meta = JSON.parse(await fs.promises.readFile(path.join(cacheDir, `${workspaceId}.json`), "utf8"));
  } catch {
    meta = {};
  }
  return {
    found: true,
    workspaceId,
    fileName: normalizedPdfName(meta.fileName || `${workspaceId}.pdf`),
    mimeType: "application/pdf",
    size: buffer.length,
    path: pdfPath,
    base64: buffer.toString("base64")
  };
}

async function downloadPdfToCache(downloadUrl, cacheDir, workspaceId) {
  console.log(`[Local Cache] PDF not found. Downloading from: ${downloadUrl}`);
  const response = await fetchWithTimeout(downloadUrl, {
    headers: { Accept: "application/pdf,application/octet-stream,*/*" }
  }, 30000);
  if (!response.ok) {
    throw new Error(`云端 PDF 下载失败（HTTP ${response.status}）`);
  }
  const buffer = Buffer.from(await response.arrayBuffer());
  if (!looksLikePdfBuffer(buffer)) {
    throw new Error("云端返回的文件不是有效 PDF。");
  }
  const metadata = {
    workspaceId,
    fileName: `${workspaceId}.pdf`,
    mimeType: "application/pdf",
    size: buffer.length,
    cachedAt: new Date().toISOString(),
    source: "cloud-download"
  };
  const pdfPath = await writePdfCacheFiles(cacheDir, workspaceId, buffer, metadata);
  const userPdfPath = path.join(userPdfStorageDir(), `${workspaceId}.pdf`);
  if (path.resolve(userPdfPath) !== path.resolve(pdfPath)) {
    await writeFileAtomic(userPdfPath, buffer).catch((error) => {
      if (!isPermissionFsError(error)) throw error;
    });
  }
  console.log(`[Local Cache] PDF downloaded successfully for workspace: ${workspaceId}`);
  return readCachedPdfFile(cacheDir, workspaceId, pdfPath);
}

async function getCacheInfo() {
  await cleanupLegacyUserPdfArtifacts().catch(() => {});
  const dir = userPdfStorageDir();
  const info = await walkDirectoryStats(dir);
  return {
    ok: true,
    path: dir,
    bytes: info.bytes,
    files: info.files,
    pdfs: info.pdfs,
    label: formatBytes(info.bytes)
  };
}

async function clearPdfCache() {
  const before = await getCacheInfo();
  await fs.promises.rm(pdfCacheDir(), { recursive: true, force: true });
  await fs.promises.mkdir(pdfCacheDir(), { recursive: true });
  await cleanupLegacyUserPdfArtifacts().catch(() => {});
  const after = await getCacheInfo();
  return {
    ok: true,
    clearedBytes: before.bytes,
    clearedFiles: before.files,
    label: formatBytes(before.bytes),
    bytes: after.bytes,
    files: after.files,
    pdfs: after.pdfs,
    currentLabel: after.label,
    path: after.path
  };
}

async function cleanupLegacyUserPdfArtifacts() {
  const userDir = userPdfStorageDir();
  const internalDir = internalPdfStorageDir();
  const cacheDir = pdfCacheDir();
  const targets = [userDir, internalDir]
    .filter(Boolean)
    .map((item) => path.resolve(item))
    .filter((item, index, list) => item && item !== path.resolve(cacheDir) && list.indexOf(item) === index);

  for (const dir of targets) {
    let entries = [];
    try {
      entries = await fs.promises.readdir(dir, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory() && /^(structured|translations|tmp|temp)$/i.test(entry.name)) {
        await fs.promises.rm(fullPath, { recursive: true, force: true }).catch(() => {});
        continue;
      }
      if (entry.isFile() && /\.json$/i.test(entry.name) && /^[a-z0-9_-]{8,}\.json$/i.test(entry.name)) {
        await fs.promises.rm(fullPath, { force: true }).catch(() => {});
      }
    }
  }
}

async function walkDirectoryStats(dir) {
  const result = { bytes: 0, files: 0, pdfs: 0 };
  let entries;
  try {
    entries = await fs.promises.readdir(dir, { withFileTypes: true });
  } catch {
    return result;
  }
  for (const entry of entries) {
    const entryPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const nested = await walkDirectoryStats(entryPath);
      result.bytes += nested.bytes;
      result.files += nested.files;
      result.pdfs += nested.pdfs;
      continue;
    }
    if (!entry.isFile() || /\.json$/i.test(entry.name)) continue;
    try {
      const stat = await fs.promises.stat(entryPath);
      result.bytes += stat.size;
      result.files += 1;
      if (entry.name.toLowerCase().endsWith(".pdf")) result.pdfs += 1;
    } catch {
      // Ignore files removed while scanning.
    }
  }
  return result;
}

function formatBytes(bytes) {
  const value = Number(bytes) || 0;
  if (value < 1024) return `${value} B`;
  const units = ["KB", "MB", "GB", "TB"];
  let size = value / 1024;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex += 1;
  }
  return `${size >= 10 ? size.toFixed(1) : size.toFixed(2)} ${units[unitIndex]}`;
}

function safeCacheKey(value) {
  return textValue(value).replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 120);
}

function settingsPath() {
  return path.join(app.getPath("userData"), "settings.json");
}

function defaultPdfStorageDir() {
  if (process.platform === "win32") {
    return internalPdfStorageDir();
  }
  return path.join(app.getPath("documents"), "PaperSolver", "PDFs");
}

function normalizePdfStorageDir(value) {
  const text = textValue(value);
  if (!text) return defaultPdfStorageDir();
  return path.resolve(text);
}

function readDesktopSettings() {
  const fallback = {
    apiBaseUrl: DEFAULT_API_BASE_URL,
    pdfStorageDir: defaultPdfStorageDir(),
    captureSession: null,
    pdfMathTranslateBaseUrl: DEFAULT_PDFMATH_BASE_URL,
    setupCompleted: false,
    localDependencyLiteMode: false,
    translationEndpoints: defaultTranslationEndpoints(),
    googleTranslateProxyHosts: []
  };
  try {
    const raw = fs.readFileSync(settingsPath(), "utf8");
    const parsed = JSON.parse(raw);
    return {
      ...fallback,
      ...parsed,
      apiBaseUrl: normalizeApiBaseUrl(parsed.apiBaseUrl) || fallback.apiBaseUrl,
      pdfStorageDir: normalizePdfStorageDir(parsed.pdfStorageDir || fallback.pdfStorageDir),
      pdfMathTranslateBaseUrl: normalizeApiBaseUrl(parsed.pdfMathTranslateBaseUrl) || fallback.pdfMathTranslateBaseUrl,
      setupCompleted: Boolean(parsed.setupCompleted),
      localDependencyLiteMode: false,
      translationEndpoints: normalizeTranslationEndpoints(parsed.translationEndpoints),
      googleTranslateProxyHosts: normalizeGoogleProxyHosts(parsed.googleTranslateProxyHosts)
    };
  } catch {
    return fallback;
  }
}

function writeDesktopSettings(settings) {
  const nextSettings = {
    ...readDesktopSettings(),
    ...settings
  };
  nextSettings.apiBaseUrl = normalizeApiBaseUrl(nextSettings.apiBaseUrl) || DEFAULT_API_BASE_URL;
  nextSettings.pdfStorageDir = normalizePdfStorageDir(nextSettings.pdfStorageDir);
  nextSettings.pdfMathTranslateBaseUrl = normalizeApiBaseUrl(nextSettings.pdfMathTranslateBaseUrl) || DEFAULT_PDFMATH_BASE_URL;
  nextSettings.setupCompleted = Boolean(nextSettings.setupCompleted);
  nextSettings.localDependencyLiteMode = false;
  nextSettings.translationEndpoints = normalizeTranslationEndpoints(nextSettings.translationEndpoints);
  nextSettings.googleTranslateProxyHosts = normalizeGoogleProxyHosts(nextSettings.googleTranslateProxyHosts);
  fs.mkdirSync(path.dirname(settingsPath()), { recursive: true });
  fs.writeFileSync(settingsPath(), JSON.stringify(nextSettings, null, 2));
  return nextSettings;
}

function defaultTranslationEndpoints() {
  return {
    deeplxEndpoint: "",
    libreTranslateEndpoint: "",
    mtranServerEndpoint: ""
  };
}

function normalizeTranslationEndpoints(endpoints = {}) {
  return {
    deeplxEndpoint: normalizeOptionalUrl(endpoints.deeplxEndpoint),
    libreTranslateEndpoint: normalizeOptionalUrl(endpoints.libreTranslateEndpoint),
    mtranServerEndpoint: normalizeOptionalUrl(endpoints.mtranServerEndpoint)
  };
}

function normalizeGoogleProxyHosts(value = []) {
  const list = Array.isArray(value) ? value : String(value || "").split(",");
  return list.map(cleanGoogleProxyHost).filter(Boolean);
}

function normalizeOptionalUrl(url) {
  const text = textValue(url);
  return text ? normalizeApiBaseUrl(text) : "";
}

function normalizeApiBaseUrl(url) {
  const text = textValue(url).replace(/\/+$/, "");
  if (!/^https?:\/\/[^/]+/i.test(text)) return "";
  return text;
}

function fileUrlToPath(url) {
  const text = textValue(url);
  if (!text.toLowerCase().startsWith("file://")) return "";
  try {
    return decodeURIComponent(new URL(text).pathname);
  } catch {
    return "";
  }
}

function normalizedPdfName(name) {
  const clean = textValue(name).replace(/[\\/:*?"<>|]+/g, "_") || "zotero-attachment.pdf";
  return clean.toLowerCase().endsWith(".pdf") ? clean : `${clean}.pdf`;
}

function normalizedPptName(name) {
  const clean = textValue(name)
    .replace(/[\\/:*?"<>|]+/g, "_")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, 120) || "meeting-deck";
  return clean.toLowerCase().endsWith(".pptx") ? clean : `${clean}.pptx`;
}

function uniquePptName(dir, fileName) {
  const ext = path.extname(fileName) || ".pptx";
  const base = path.basename(fileName, ext) || "meeting-deck";
  let candidate = `${base}${ext}`;
  let index = 2;
  while (fs.existsSync(path.join(dir, candidate))) {
    candidate = `${base}-${index}${ext}`;
    index += 1;
  }
  return candidate;
}

function assertReasonablePdfSize(size) {
  const maxBytes = 80 * 1024 * 1024;
  if (size > maxBytes) {
    throw new Error("Zotero PDF 超过 80MB，请手动上传或压缩后再导入。");
  }
}

function looksLikePdfBuffer(buffer) {
  return Buffer.isBuffer(buffer) && buffer.subarray(0, 5).toString("utf8") === "%PDF-";
}

// Device Fingerprint Generation for Anti-abuse
function getHardwareId() {
  const { execSync } = require("node:child_process");
  try {
    if (process.platform === "darwin") {
      const output = execSync("ioreg -rd1 -c IOPlatformExpertDevice", { encoding: "utf8" });
      const match = output.match(/"IOPlatformUUID"\s*=\s*"([^"]+)"/);
      if (match && match[1]) return match[1].trim();
    } else if (process.platform === "win32") {
      const output = execSync("wmic csproduct get uuid", { encoding: "utf8" });
      const lines = output.split("\n").map(line => line.trim()).filter(line => line.length > 0);
      if (lines.length > 1) return lines[1];
    } else if (process.platform === "linux") {
      if (fs.existsSync("/var/lib/dbus/machine-id")) {
        return fs.readFileSync("/var/lib/dbus/machine-id", "utf8").trim();
      }
    }
  } catch (e) {
    console.error("Failed to fetch hardware UUID:", e.message);
  }
  return null;
}

function getMachineId() {
  const hardwareId = getHardwareId();
  if (hardwareId) {
    return createHash("sha256").update(hardwareId).digest("hex");
  }
  const settings = readDesktopSettings();
  if (settings.machineId) {
    return settings.machineId;
  }
  const newId = createHash("sha256").update(randomUUID()).digest("hex");
  writeDesktopSettings({ machineId: newId });
  return newId;
}

ipcMain.handle("desktop:get-machine-id", () => getMachineId());
