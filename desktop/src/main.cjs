const { app, BrowserWindow, Menu, shell, dialog, ipcMain } = require("electron");
const path = require("node:path");
const fs = require("node:fs");
const http = require("node:http");

const isPackaged = app.isPackaged;
const ZOTERO_LOCAL_BASE = "http://127.0.0.1:23119";
const DEFAULT_API_BASE_URL = normalizeApiBaseUrl(process.env.PAPER_SOLVER_API_BASE) || "http://127.0.0.1:8080";
const DESKTOP_TRANSLATION_LABELS = {
  "google-web": "Google 网页翻译（本机）",
  google: "谷歌翻译（本机）",
  deeplx: "DeepLX（本机）",
  libretranslate: "LibreTranslate（本机）",
  mtranserver: "MTranServer（本机）"
};
const MAX_TRANSLATION_CHUNK = 4500;
const LOCAL_CAPTURE_PORT = Number(process.env.PAPER_SOLVER_DESKTOP_CAPTURE_PORT) || 18765;
let localCaptureServer = null;

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
  packaged: isPackaged,
  apiBaseUrl: readDesktopSettings().apiBaseUrl,
  pdfStorageDir: readDesktopSettings().pdfStorageDir
}));

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
  return writeDesktopSettings(nextSettings);
});

ipcMain.handle("desktop:reset-backend-config", () => writeDesktopSettings({
  apiBaseUrl: DEFAULT_API_BASE_URL,
  pdfStorageDir: defaultPdfStorageDir(),
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
  return [
    { id: "google-web", label: DESKTOP_TRANSLATION_LABELS["google-web"], configured: true, local: true },
    { id: "google", label: DESKTOP_TRANSLATION_LABELS.google, configured: true, local: true },
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
  const translatedText = await translateWithDesktopProvider(provider, text, sourceLang, targetLang, settings);
  return {
    provider,
    providerLabel: DESKTOP_TRANSLATION_LABELS[provider] || provider,
    sourceLang,
    targetLang,
    translatedText,
    fallback: false,
    local: true
  };
}

async function translateWithDesktopProvider(provider, text, sourceLang, targetLang, settings) {
  if (provider === "google" || provider === "google-web") {
    return translateWithGoogleWeb(text, sourceLang, targetLang);
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
      throw new Error(`Google 网页翻译返回 ${response.status}`);
    }
    const root = await response.json();
    const textPart = extractGoogleWebTranslation(root);
    if (textPart) translatedChunks.push(textPart.trim());
  }
  const result = translatedChunks.join("\n\n").trim();
  if (!result) throw new Error("未获取到译文");
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
  if (["deeplx", "libretranslate", "mtranserver"].includes(normalized)) return normalized;
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
