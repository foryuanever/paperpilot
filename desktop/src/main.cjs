const { app, BrowserWindow, Menu, shell, dialog, ipcMain } = require("electron");
const path = require("node:path");
const fs = require("node:fs");
const http = require("node:http");
const https = require("node:https");
const { randomUUID, createHash } = require("node:crypto");
const { spawn } = require("node:child_process");

if (process.env.PAPER_SOLVER_DISABLE_GPU === "1") {
  app.disableHardwareAcceleration();
  app.commandLine.appendSwitch("disable-gpu");
  app.commandLine.appendSwitch("disable-gpu-compositing");
}
app.commandLine.appendSwitch("enable-gpu-rasterization");
app.commandLine.appendSwitch("enable-zero-copy");

const isPackaged = app.isPackaged;
const ZOTERO_LOCAL_BASE = "http://127.0.0.1:23119";
const DEFAULT_API_BASE_URL = normalizeApiBaseUrl(process.env.PAPER_SOLVER_API_BASE) || "http://127.0.0.1:8080";
const DEFAULT_PDFMATH_BASE_URL = normalizeApiBaseUrl(process.env.PAPER_SOLVER_PDFMATH_BASE) || "http://127.0.0.1:11008";
const DESKTOP_TRANSLATION_LABELS = {
  "google-web": "谷歌翻译",
  google: "谷歌翻译",
  bing: "微软翻译",
  youdao: "有道翻译",
  "360-web": "360 翻译",
  "tencent-transmart": "腾讯 TranSmart",
  deeplx: "DeepLX",
  libretranslate: "LibreTranslate",
  mtranserver: "MTranServer"
};
const MAX_TRANSLATION_CHUNK = 4500;
const LOCAL_CAPTURE_PORT = Number(process.env.PAPER_SOLVER_DESKTOP_CAPTURE_PORT) || 18765;
let localCaptureServer = null;
const localDependencyProcesses = new Map();
const desktopPdfMathTasks = new Map();
const desktopStructuredParseTasks = new Map();

function appIndexPath() {
  if (isPackaged) {
    return path.join(process.resourcesPath, "front-dist", "index.html");
  }
  return path.join(__dirname, "..", "..", "front", "dist", "index.html");
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
    title: "PaperSolver",
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

  window.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:\/\//i.test(url)) {
      shell.openExternal(url);
      return { action: "deny" };
    }
    return { action: "allow" };
  });

  window.webContents.on("will-navigate", (event, url) => {
    const currentUrl = window.webContents.getURL();
    if (/^https?:\/\//i.test(url) && url !== currentUrl) {
      event.preventDefault();
      shell.openExternal(url);
    }
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

app.whenReady().then(() => {
  buildMenu();
  startLocalCaptureServer();
  createMainWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
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

ipcMain.handle("desktop:get-runtime-info", () => ({
  platform: process.platform,
  version: app.getVersion(),
  channel: "beta",
  packaged: isPackaged,
  apiBaseUrl: readDesktopSettings().apiBaseUrl,
  pdfStorageDir: readDesktopSettings().pdfStorageDir,
  pdfMathTranslateBaseUrl: readDesktopSettings().pdfMathTranslateBaseUrl,
  updatePolicy: {
    mode: "manual",
    currentVersion: app.getVersion(),
    channel: "beta",
    manifestUrl: process.env.PAPER_SOLVER_UPDATE_MANIFEST || "",
    message: "当前为 0.1.0-beta 内测通道。正式发布后接入更新清单，客户端可检测更新、下载并重启安装。"
  }
}));

ipcMain.handle("desktop:check-update", async () => {
  const manifestUrl = process.env.PAPER_SOLVER_UPDATE_MANIFEST || "";
  if (!manifestUrl) {
    return {
      ok: true,
      updateAvailable: false,
      currentVersion: app.getVersion(),
      channel: "beta",
      message: "当前未配置正式更新源，已处于本地 beta 更新策略。"
    };
  }
  const manifest = await requestJson(manifestUrl, { timeoutMs: 8000 });
  return {
    ok: true,
    currentVersion: app.getVersion(),
    channel: manifest.channel || "beta",
    latestVersion: manifest.version || "",
    updateAvailable: Boolean(manifest.version && manifest.version !== app.getVersion()),
    downloadUrl: manifest.downloadUrl || "",
    releaseNotes: manifest.releaseNotes || "",
    message: manifest.version && manifest.version !== app.getVersion()
      ? "发现新版本，可按更新策略下载并重启安装。"
      : "当前已是最新版本。"
  };
});

ipcMain.handle("desktop:get-backend-config", () => readDesktopSettings());

ipcMain.handle("desktop:set-capture-session", (_event, payload = {}) => {
  const userId = textValue(payload.userId);
  const userName = textValue(payload.userName);
  const email = textValue(payload.email);
  if (!userId) {
    return writeDesktopSettings({ captureSession: null });
  }
  return writeDesktopSettings({
    captureSession: {
      userId,
      userName,
      email,
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

ipcMain.handle("desktop:get-cached-pdf", async (_event, payload = {}) => {
  return getCachedPdf(payload);
});

ipcMain.handle("desktop:get-cache-info", async () => {
  return getCacheInfo();
});

ipcMain.handle("desktop:clear-pdf-cache", async () => {
  return clearPdfCache();
});

ipcMain.handle("desktop:open-cache-dir", async () => {
  await fs.promises.mkdir(pdfCacheDir(), { recursive: true });
  const error = await shell.openPath(pdfCacheDir());
  if (error) throw new Error(error);
  return { ok: true, path: pdfCacheDir() };
});

ipcMain.handle("desktop:get-translation-providers", () => {
  const settings = readDesktopSettings();
  const providers = [
    { id: "google", label: DESKTOP_TRANSLATION_LABELS.google, configured: true, local: true },
    { id: "bing", label: DESKTOP_TRANSLATION_LABELS.bing, configured: true, local: true },
    { id: "youdao", label: DESKTOP_TRANSLATION_LABELS.youdao, configured: true, local: true },
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

ipcMain.handle("desktop:local-dependency-status", async () => {
  return getLocalDependencyStatus();
});

ipcMain.handle("desktop:download-local-dependency", async (event, payload = {}) => {
  return downloadAndInstallLocalDependency(event.sender, payload);
});

ipcMain.handle("desktop:start-local-dependency", async () => {
  try {
    return await startLocalDependencyServices({ waitForReady: true });
  } catch (error) {
    const logTail = await tailFile(localDependencyLogPath(), 2200);
    const detail = logTail ? `\n\n最近日志：\n${logTail.slice(-1200)}` : "";
    throw new Error(`${error?.message || "本机依赖启动失败"}${detail}`);
  }
});

ipcMain.handle("desktop:open-local-dependency-log", async () => {
  return openLocalDependencyLog();
});

ipcMain.handle("desktop:pdfmath-start", async (_event, payload = {}) => {
  return startDesktopPdfMathTranslation(payload);
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
        send(200, { ...result, paperUrl: `desktop-cache://${result.workspaceId}`, path: pdfCacheDir() });
        return;
      }
      send(404, { ok: false, error: "Not found" });
    } catch (error) {
      send(400, { ok: false, error: error?.message || "PaperSolver Desktop 本机接收失败" });
    }
  });
  localCaptureServer.on("error", () => {
    localCaptureServer = null;
  });
  localCaptureServer.listen(LOCAL_CAPTURE_PORT, "127.0.0.1");
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

async function translateOnDesktop(payload) {
  const provider = normalizeDesktopTranslationProvider(payload.provider);
  const text = textValue(payload.text);
  if (!text) throw new Error("待翻译文本不能为空");
  const sourceLang = normalizeTranslationLang(payload.sourceLang, "auto");
  const targetLang = normalizeTranslationLang(payload.targetLang, "zh-CN");
  const settings = readDesktopSettings();
  const { translatedText, actualProvider } = await translateWithDesktopFallback(provider, text, sourceLang, targetLang, settings);
  return {
    provider: actualProvider,
    requestedProvider: provider,
    providerLabel: DESKTOP_TRANSLATION_LABELS[actualProvider] || actualProvider,
    sourceLang,
    targetLang,
    translatedText,
    fallback: actualProvider !== provider,
    local: true
  };
}

async function getLocalDependencyStatus() {
  const settings = readDesktopSettings();
  const baseUrl = pdfMathBaseUrl(settings);
  const startedAt = Date.now();
  const structuredParserAvailable = Boolean(desktopStructuredParserBinary());
  const installed = localDependencyInstalled();
  try {
    const response = await fetchWithTimeout(baseUrl, {
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
      }
    }, 2200);
    return {
      ok: true,
      installed: true,
      running: true,
      structuredParserAvailable,
      latencyMs: Date.now() - startedAt,
      label: "PaperSolver 本机依赖包",
      message: response.ok
        ? "本机依赖运行正常，沉浸翻译和对照阅读可使用本机能力。"
        : "已检测到本机依赖服务，部分接口可能仍在初始化。"
    };
  } catch {
    const logTail = await tailFile(localDependencyLogPath(), 1800);
    return {
      ok: false,
      installed: installed || structuredParserAvailable,
      running: false,
      structuredParserAvailable,
      latencyMs: Date.now() - startedAt,
      label: "PaperSolver 本机依赖包",
      dependencyDir: localDependencyDir(),
      logPath: localDependencyLogPath(),
      logTail,
      message: installed || structuredParserAvailable
        ? dependencyStatusMessageFromLog(logTail)
        : "未检测到本机依赖。沉浸翻译会先尝试备用模式，建议安装依赖包以获得完整体验。"
    };
  }
}

async function downloadAndInstallLocalDependency(webContents, options = {}) {
  const tempRoot = path.join(app.getPath("temp"), `papersolver-dependency-${Date.now()}`);
  const bundledArchive = bundledLocalDependencyArchive();
  const url = bundledArchive ? "" : localDependencyDownloadUrl();
  if (!bundledArchive && !url) {
    throw new Error("当前版本没有内置 PaperSolver 本机依赖包，也未配置备用下载源。");
  }
  const archivePath = bundledArchive
    ? bundledArchive
    : path.join(tempRoot, path.basename(new URL(url).pathname) || "papersolver-dependency.zip");
  const installDir = localDependencyDir();
  const force = Boolean(options?.force);
  const emit = (payload) => emitDependencyProgress(webContents, payload);
  await fs.promises.mkdir(tempRoot, { recursive: true });
  try {
    if (force) {
      stopLocalDependencyProcesses();
      await fs.promises.rm(localDependencyLogPath(), { force: true }).catch(() => {});
    }
    if (bundledArchive) {
      emit({ stage: "prepare", progress: 12, message: "正在读取内置 PaperSolver 本机依赖包..." });
    } else {
      emit({ stage: "download", progress: 3, message: "正在连接 PaperSolver 依赖服务..." });
      await downloadFileToPath(url, archivePath, (progress) => {
        emit({
          stage: "download",
          progress: Math.max(4, Math.min(72, Math.round(progress * 0.68 + 4))),
          message: `正在下载本机依赖包 ${Math.round(progress)}%`
        });
      });
    }
    emit({ stage: "verify", progress: 76, message: "正在校验依赖包..." });
    const stat = await fs.promises.stat(archivePath);
    if (!stat.size || stat.size < 1024 * 100) {
      throw new Error("依赖包下载不完整，请稍后重试。");
    }
    emit({ stage: "extract", progress: 82, message: force ? "正在重新安装到用户本机目录..." : "正在安装到用户本机目录..." });
    await fs.promises.rm(installDir, { recursive: true, force: true });
    await fs.promises.mkdir(installDir, { recursive: true });
    await extractArchive(archivePath, installDir);
    await normalizeInstalledDependencyLayout(installDir);
    emit({ stage: "start", progress: 92, message: "正在启动本机依赖服务..." });
    await startLocalDependencyServices({ waitForReady: true });
    const status = await getLocalDependencyStatus();
    if (!status.running) {
      throw new Error(status.message || "依赖已安装，但服务启动失败。");
    }
    emit({ stage: "done", progress: 100, message: "本机依赖包安装完成。" });
    return { ok: true, ...status };
  } catch (error) {
    const logTail = await tailFile(localDependencyLogPath(), 2200);
    const detail = logTail ? `\n\n最近日志：\n${logTail.slice(-1200)}` : "";
    throw new Error(`${error?.message || "依赖处理失败"}${detail}`);
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

function stopLocalDependencyProcesses() {
  for (const child of localDependencyProcesses.values()) {
    try { child.kill(); } catch {}
  }
  localDependencyProcesses.clear();
}

function dependencyStatusMessageFromLog(logTail) {
  const text = textValue(logTail);
  if (/No module named|ModuleNotFoundError|DistributionNotFound/i.test(text)) {
    return "依赖包已安装，但运行环境缺少组件。请点击强制重装。";
  }
  if (/timed out|timeout|ReadTimeout|ConnectTimeout/i.test(text)) {
    return "依赖包初始化超时，可能正在下载组件或网络较慢。可稍等后重新检测。";
  }
  if (/Permission denied|operation not permitted/i.test(text)) {
    return "依赖包没有执行权限，请点击强制重装或检查系统安全设置。";
  }
  return "已检测到本机依赖包，但服务暂未运行。请点击启动依赖，失败时可查看日志。";
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
  return path.join(app.getPath("userData"), "dependencies");
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

function readLocalDependencyManifest() {
  const manifestPath = path.join(localDependencyDir(), "papersolver-dependency.json");
  try {
    return JSON.parse(fs.readFileSync(manifestPath, "utf8"));
  } catch {
    return {};
  }
}

async function startLocalDependencyServices({ waitForReady = false } = {}) {
  const settings = readDesktopSettings();
  const baseUrl = pdfMathBaseUrl(settings);
  try {
    const response = await fetchWithTimeout(baseUrl, {
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
      }
    }, 1200);
    if (response.ok) return { ok: true, alreadyRunning: true, running: true };
  } catch {}

  const command = localDependencyStartCommand();
  if (!command?.binary) {
    throw new Error("未找到本机依赖启动入口，请重新下载依赖包。");
  }
  if (!localDependencyProcesses.has(command.id)) {
    await fs.promises.mkdir(path.dirname(localDependencyLogPath()), { recursive: true });
    const out = fs.openSync(localDependencyLogPath(), "a");
    const child = spawn(command.binary, command.args || [], {
      cwd: command.cwd || localDependencyDir(),
      detached: true,
      stdio: ["ignore", out, out],
      env: {
        ...process.env,
        PAPER_SOLVER_PDFMATH_BASE: baseUrl,
        PAPER_SOLVER_PORT: String(new URL(baseUrl).port || 11008)
      }
    });
    child.unref();
    localDependencyProcesses.set(command.id, child);
    child.on("exit", () => localDependencyProcesses.delete(command.id));
  }
  if (waitForReady) {
    await waitForLocalDependencyReady(baseUrl, 90000);
  }
  return { ok: true, started: true, running: true };
}

function localDependencyStartCommand() {
  const root = localDependencyDir();
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

async function waitForLocalDependencyReady(baseUrl, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  let lastError = "";
  while (Date.now() < deadline) {
    try {
      const response = await fetchWithTimeout(baseUrl, {
        headers: {
          "Accept": "application/json,text/plain,*/*",
          "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
        }
      }, 1200);
      if (response.ok || response.status < 500) return true;
    } catch (error) {
      lastError = error?.message || String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, 850));
  }
  throw new Error(`本机依赖服务启动超时${lastError ? `：${lastError}` : ""}`);
}

function localDependencyLogPath() {
  return path.join(app.getPath("userData"), "logs", "local-dependency.log");
}

function downloadFileToPath(url, targetPath, onProgress) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const client = parsed.protocol === "http:" ? http : https;
    const request = client.get(parsed, {
      headers: {
        "User-Agent": "PaperSolver Desktop",
        "Accept": "application/octet-stream,*/*"
      }
    }, (response) => {
      if ([301, 302, 303, 307, 308].includes(response.statusCode) && response.headers.location) {
        response.resume();
        downloadFileToPath(new URL(response.headers.location, url).toString(), targetPath, onProgress).then(resolve, reject);
        return;
      }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        response.resume();
        reject(new Error(`依赖包下载失败（HTTP ${response.statusCode}）`));
        return;
      }
      const total = Number(response.headers["content-length"]) || 0;
      let received = 0;
      const output = fs.createWriteStream(targetPath);
      response.on("data", (chunk) => {
        received += chunk.length;
        if (total) onProgress?.(Math.min(100, (received / total) * 100));
      });
      response.pipe(output);
      output.on("finish", () => {
        output.close(() => {
          onProgress?.(100);
          resolve();
        });
      });
      output.on("error", reject);
    });
    request.setTimeout(30000, () => {
      request.destroy(new Error("依赖包下载超时"));
    });
    request.on("error", reject);
  });
}

function extractArchive(archivePath, targetDir) {
  const lower = archivePath.toLowerCase();
  if (lower.endsWith(".zip")) {
    if (process.platform === "darwin") {
      return runCommand("ditto", ["-x", "-k", archivePath, targetDir]);
    }
    return runCommand("tar", ["-xf", archivePath, "-C", targetDir]);
  }
  if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
    return runCommand("tar", ["-xzf", archivePath, "-C", targetDir]);
  }
  throw new Error("依赖包格式不支持，请使用 zip 或 tar.gz。");
}

function runCommand(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: ["ignore", "pipe", "pipe"] });
    let stderr = "";
    child.stderr.on("data", (chunk) => { stderr += chunk.toString(); });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} 执行失败：${textValue(stderr) || `退出码 ${code}`}`));
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
  const cachedDualPath = desktopDualPdfPath(workspaceId);
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

  const cachedPdf = await getCachedPdf({ workspaceId });
  if (!cachedPdf?.found || !cachedPdf.base64) {
    throw new Error("本机没有缓存这篇文献的 PDF，无法在桌面端运行对照翻译。请先导入或关联 PDF。");
  }

  const settings = readDesktopSettings();
  const baseUrl = pdfMathBaseUrl(settings);
  const service = textValue(payload.service) || "google";
  const thread = Math.max(1, Math.min(8, Number(payload.thread) || 4));
  const pdfBuffer = Buffer.from(cachedPdf.base64, "base64");
  if (!looksLikePdfBuffer(pdfBuffer)) {
    throw new Error("本机缓存文件不是有效 PDF，无法提交对照翻译。");
  }

  const boundary = `PaperSolverDesktop-${randomUUID()}`;
  const body = buildPdfMathMultipartBody(boundary, {
    fileName: cachedPdf.fileName || `${workspaceId}.pdf`,
    pdfBuffer,
    data: {
      lang_in: "en",
      lang_out: "zh",
      service,
      thread,
      skip_subset_fonts: true
    }
  });
  const response = await fetch(`${baseUrl}/v1/translate`, {
    method: "POST",
    headers: {
      "Accept": "application/json,text/plain,*/*",
      "Content-Type": `multipart/form-data; boundary=${boundary}`,
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
    },
    body
  });
  if (!response.ok) {
    throw new Error(`PaperSolver 本机依赖提交失败（HTTP ${response.status}）：${await safeResponseText(response)}`);
  }
  const result = await response.json();
  const taskId = textValue(result.id || result.taskId || result.task_id);
  if (!taskId) {
    throw new Error("PaperSolver 本机依赖没有返回任务编号。");
  }
  desktopPdfMathTasks.set(workspaceId, {
    taskId,
    baseUrl,
    service,
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
  const cachedDualPath = desktopDualPdfPath(workspaceId);
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
  const task = desktopPdfMathTasks.get(workspaceId);
  if (!task?.taskId) {
    throw new Error("尚未创建本机对照翻译任务。");
  }
  const response = await fetch(`${task.baseUrl}/v1/translate/${encodeURIComponent(task.taskId)}`, {
    headers: {
      "Accept": "application/json,text/plain,*/*",
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
    }
  });
  if (!response.ok) {
    throw new Error(`PaperSolver 本机依赖状态查询失败（HTTP ${response.status}）：${await safeResponseText(response)}`);
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
  const cachedDualPath = desktopDualPdfPath(workspaceId);
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
  const task = desktopPdfMathTasks.get(workspaceId);
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
    throw new Error(`PaperSolver 本机依赖下载双语 PDF 失败（HTTP ${response.status}）：${await safeResponseText(response)}`);
  }
  const buffer = Buffer.from(await response.arrayBuffer());
  if (!looksLikePdfBuffer(buffer)) {
    throw new Error("PaperSolver 本机依赖返回的双语文件不是有效 PDF。");
  }
  await fs.promises.mkdir(path.dirname(cachedDualPath), { recursive: true });
  await fs.promises.writeFile(cachedDualPath, buffer);
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
  if (!force && findDesktopContentList(workspaceId)) {
    const ready = desktopStructuredTaskState("SUCCESS", "本机结构化解析已就绪", "", 100);
    desktopStructuredParseTasks.set(workspaceId, ready);
    return desktopStructuredStatusPayload(workspaceId, ready);
  }
  const existing = desktopStructuredParseTasks.get(workspaceId);
  if (!force && existing?.state === "RUNNING") {
    return desktopStructuredStatusPayload(workspaceId, existing);
  }
  const cachedPdf = await getCachedPdf({ workspaceId });
  if (!cachedPdf?.found || !cachedPdf.base64) {
    throw new Error("本机没有缓存这篇文献的 PDF，无法进行沉浸翻译解析。请先导入或关联 PDF。");
  }
  const parserBinary = desktopStructuredParserBinary();
  if (!parserBinary) {
    throw new Error("未检测到 PaperSolver 本机依赖的结构化解析组件。");
  }
  const running = desktopStructuredTaskState("RUNNING", "正在调用本机依赖识别段落、图表与阅读顺序", "", 18);
  desktopStructuredParseTasks.set(workspaceId, running);
  parseDesktopStructuredInBackground(workspaceId, cachedPdf, parserBinary, force);
  return desktopStructuredStatusPayload(workspaceId, running);
}

async function getDesktopStructuredParseStatus(payload = {}) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) {
    throw new Error("缺少文献 workspaceId，无法查询本机结构化解析状态。");
  }
  if (findDesktopContentList(workspaceId)) {
    const ready = desktopStructuredTaskState("SUCCESS", "段落、图表与阅读顺序解析完成", "", 100);
    desktopStructuredParseTasks.set(workspaceId, ready);
    return desktopStructuredStatusPayload(workspaceId, ready);
  }
  const state = desktopStructuredParseTasks.get(workspaceId);
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
    engine: "PaperSolver 本机依赖",
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

async function parseDesktopStructuredInBackground(workspaceId, cachedPdf, parserBinary, force) {
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
    desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState("RUNNING", "本机依赖正在解析论文版面", "", 45));
    const result = await runDesktopStructuredParser(parserBinary, inputPath, outputDir, logPath);
    if (result.exitCode !== 0 || !findDesktopContentList(workspaceId)) {
      const detail = await tailFile(logPath, 1800);
      desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState("FAILURE", "论文结构化解析失败", detail || result.error, 100));
      return;
    }
    desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState("SUCCESS", "段落、图表与阅读顺序解析完成", "", 100));
  } catch (error) {
    desktopStructuredParseTasks.set(workspaceId, desktopStructuredTaskState("FAILURE", "论文结构化解析失败", error?.message || String(error), 100));
  }
}

function runDesktopStructuredParser(parserBinary, inputPath, outputDir, logPath) {
  return new Promise((resolve) => {
    const args = [
      "-p", inputPath,
      "-o", outputDir,
      "-b", "pipeline",
      "-f", "false",
      "-t", "true"
    ];
    const logStream = fs.createWriteStream(logPath, { flags: "a" });
    const child = spawn(parserBinary, args, {
      env: {
        ...process.env,
        MINERU_MODEL_SOURCE: process.env.PAPER_SOLVER_MODEL_SOURCE || "modelscope"
      },
      stdio: ["ignore", "pipe", "pipe"]
    });
    child.stdout.on("data", (chunk) => logStream.write(chunk));
    child.stderr.on("data", (chunk) => logStream.write(chunk));
    child.on("error", (error) => {
      logStream.end();
      resolve({ exitCode: -1, error: error?.message || String(error) });
    });
    child.on("close", (exitCode) => {
      logStream.end();
      resolve({ exitCode: Number(exitCode) || 0, error: "" });
    });
  });
}

function desktopStructuredTaskState(state, message, detail = "", progress = 20) {
  return { state, message, detail, progress, updatedAt: Date.now() };
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

function findDesktopContentList(workspaceId) {
  const root = desktopStructuredOutputDir(workspaceId);
  return findFileRecursive(root, (filePath) => {
    const name = path.basename(filePath);
    return name.endsWith("_content_list.json") || name === "content_list.json";
  });
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
  return textValue(value)
    .replace(/<[^>]+>/gis, "")
    .replaceAll("&nbsp;", " ")
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replace(/\s+/g, " ")
    .trim();
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
  return normalizeApiBaseUrl(settings?.pdfMathTranslateBaseUrl)
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

function desktopDualPdfPath(workspaceId) {
  return path.join(pdfCacheDir(), "translations", `${workspaceId}-dual.pdf`);
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

async function translateWithDesktopProvider(provider, text, sourceLang, targetLang, settings) {
  if (provider === "google" || provider === "google-web") {
    return translateWithGoogleWeb(text, sourceLang, targetLang);
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

async function translateWithDesktopFallback(provider, text, sourceLang, targetLang, settings) {
  const candidates = desktopTranslationFallbackChain(provider, settings);
  const errors = [];
  for (const candidate of candidates) {
    try {
      const translatedText = await translateWithDesktopProvider(candidate, text, sourceLang, targetLang, settings);
      return { translatedText, actualProvider: candidate };
    } catch (error) {
      errors.push(`${DESKTOP_TRANSLATION_LABELS[candidate] || candidate}: ${error.message}`);
    }
  }
  throw new Error(errors[0] || "翻译失败，请稍后重试。");
}

function desktopTranslationFallbackChain(provider, settings) {
  const chain = [provider];
  return [...new Set(chain)];
}

async function translateWithGoogleWeb(text, sourceLang, targetLang) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const sl = sourceLang.toLowerCase() === "auto" ? "auto" : sourceLang;
    const url = "https://translate.googleapis.com/translate_a/single"
      + `?client=gtx&sl=${encodeURIComponent(sl)}&tl=${encodeURIComponent(targetLang)}&dt=t&q=${encodeURIComponent(chunk)}`;
    const response = await fetch(url, {
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
      }
    });
    if (!response.ok) {
      throw new Error(`谷歌翻译返回 ${response.status}`);
    }
    const root = await response.json();
    const textPart = extractGoogleWebTranslation(root);
    if (textPart) translatedChunks.push(textPart.trim());
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("未获取到译文");
  return result;
}

async function translateWithYoudaoWeb(text) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const url = "https://dict.youdao.com/jsonapi_s"
      + `?doctype=json&jsonversion=4&q=${encodeURIComponent(chunk)}`;
    const response = await fetch(url, {
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
      }
    });
    if (!response.ok) {
      throw new Error(`有道翻译返回 ${response.status}`);
    }
    const root = await response.json();
    const translated = textValue(root?.fanyi?.tran)
      || firstJsonText(root, "tran", "translation", "translatedText", "value");
    if (translated) translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("有道翻译未返回译文");
  return result;
}

async function translateWithBingWeb(text, sourceLang, targetLang) {
  const tokenResponse = await fetch("https://edge.microsoft.com/translate/auth", {
    headers: {
      "Accept": "text/plain,*/*",
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
    }
  });
  if (!tokenResponse.ok) {
    throw new Error(`微软翻译授权返回 ${tokenResponse.status}`);
  }
  const token = textValue(await tokenResponse.text());
  if (!token) throw new Error("微软翻译未返回授权信息");

  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const from = mapMicrosoftLang(sourceLang);
    const to = mapMicrosoftLang(targetLang);
    const endpoint = "https://api-edge.cognitive.microsofttranslator.com/translate"
      + `?api-version=3.0${from === "auto" ? "" : `&from=${encodeURIComponent(from)}`}&to=${encodeURIComponent(to)}`;
    const response = await fetch(endpoint, {
      method: "POST",
      headers: {
        "Accept": "application/json,text/plain,*/*",
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
      },
      body: JSON.stringify([{ Text: chunk }])
    });
    if (!response.ok) {
      throw new Error(`微软翻译返回 ${response.status}`);
    }
    const root = await response.json();
    const translated = Array.isArray(root)
      ? root.map((item) => firstJsonText(item, "translations", "text")).filter(Boolean).join("")
      : firstJsonText(root, "translations", "text", "translatedText");
    if (translated) translatedChunks.push(translated.trim());
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("微软翻译未返回译文");
  return result;
}

async function translateWithTencentTransmartWeb(text, sourceLang, targetLang) {
  const chunks = splitTranslationText(text);
  const translatedChunks = [];
  for (const chunk of chunks) {
    const data = await postJson("https://transmart.qq.com/api/imt", {
      header: { fn: "auto_translation" },
      type: "plain",
      model_category: "normal",
      text_domain: "general",
      source: {
        lang: mapTransmartLang(sourceLang),
        text_list: [chunk]
      },
      target: {
        lang: mapTransmartLang(targetLang)
      }
    });
    const translated = firstJsonText(data, "auto_translation", "translation", "target_text", "text");
    if (translated) translatedChunks.push(translated);
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("腾讯 TranSmart 网页接口未返回译文，可能需要站点令牌。");
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
      "User-Agent": "Mozilla/5.0 PaperSolver Desktop"
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
  if (!Array.isArray(root) || !Array.isArray(root[0])) return "";
  return root[0]
    .map((segment) => Array.isArray(segment) ? textValue(segment[0]) : "")
    .join("");
}

function splitTranslationText(text) {
  if (text.length <= MAX_TRANSLATION_CHUNK) return [text];
  const chunks = [];
  let current = "";
  for (const paragraph of text.split(/\n{2,}/)) {
    if (paragraph.length > MAX_TRANSLATION_CHUNK) {
      if (current) {
        chunks.push(current);
        current = "";
      }
      for (let index = 0; index < paragraph.length; index += MAX_TRANSLATION_CHUNK) {
        chunks.push(paragraph.slice(index, index + MAX_TRANSLATION_CHUNK));
      }
      continue;
    }
    if (current && current.length + paragraph.length + 2 > MAX_TRANSLATION_CHUNK) {
      chunks.push(current);
      current = "";
    }
    current = current ? `${current}\n\n${paragraph}` : paragraph;
  }
  if (current) chunks.push(current);
  return chunks;
}

function normalizeDesktopTranslationProvider(provider) {
  const normalized = textValue(provider).toLowerCase();
  if (!normalized || normalized === "google") return "google";
  if (normalized === "google-web") return "google-web";
  if (["youdao", "bing", "microsoft-edge", "360-web", "tencent-transmart", "deeplx", "libretranslate", "mtranserver"].includes(normalized)) {
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
  return readDesktopSettings().pdfStorageDir || defaultPdfStorageDir();
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
  await fs.promises.mkdir(pdfCacheDir(), { recursive: true });
  const pdfPath = path.join(pdfCacheDir(), `${workspaceId}.pdf`);
  const metaPath = path.join(pdfCacheDir(), `${workspaceId}.json`);
  await fs.promises.writeFile(pdfPath, buffer);
  await fs.promises.writeFile(metaPath, JSON.stringify({
    workspaceId,
    fileName: normalizedPdfName(payload.fileName || `${workspaceId}.pdf`),
    mimeType: "application/pdf",
    size: buffer.length,
    cachedAt: new Date().toISOString()
  }, null, 2));
  return { ok: true, workspaceId, size: buffer.length };
}

async function getCachedPdf(payload) {
  const workspaceId = safeCacheKey(payload.workspaceId);
  if (!workspaceId) return { found: false };
  const pdfPath = path.join(pdfCacheDir(), `${workspaceId}.pdf`);
  try {
    const buffer = await fs.promises.readFile(pdfPath);
    if (!looksLikePdfBuffer(buffer)) return { found: false };
    let meta = {};
    try {
      meta = JSON.parse(await fs.promises.readFile(path.join(pdfCacheDir(), `${workspaceId}.json`), "utf8"));
    } catch {
      meta = {};
    }
    return {
      found: true,
      workspaceId,
      fileName: normalizedPdfName(meta.fileName || `${workspaceId}.pdf`),
      mimeType: "application/pdf",
      size: buffer.length,
      base64: buffer.toString("base64")
    };
  } catch {
    return { found: false };
  }
}

async function getCacheInfo() {
  const dir = pdfCacheDir();
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
  return {
    ok: true,
    clearedBytes: before.bytes,
    clearedFiles: before.files,
    label: formatBytes(before.bytes),
    path: pdfCacheDir()
  };
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
    if (!entry.isFile()) continue;
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
    translationEndpoints: defaultTranslationEndpoints()
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
      translationEndpoints: normalizeTranslationEndpoints(parsed.translationEndpoints)
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
  nextSettings.translationEndpoints = normalizeTranslationEndpoints(nextSettings.translationEndpoints);
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

function assertReasonablePdfSize(size) {
  const maxBytes = 80 * 1024 * 1024;
  if (size > maxBytes) {
    throw new Error("Zotero PDF 超过 80MB，请手动上传或压缩后再导入。");
  }
}

function looksLikePdfBuffer(buffer) {
  return Buffer.isBuffer(buffer) && buffer.subarray(0, 5).toString("utf8") === "%PDF-";
}
