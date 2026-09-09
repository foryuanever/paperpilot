const DEFAULT_API_BASE = "https://papersolver.cn";
const DESKTOP_CAPTURE_BASE = "http://127.0.0.1:18765";

chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.sync.get(["apiBase"], ({ apiBase }) => {
    if (!apiBase) chrome.storage.sync.set({ apiBase: DEFAULT_API_BASE });
  });
  chrome.action.setIcon({ path: { 128: "icon-gray-128.png" } });
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo) => {
  if (changeInfo.status === "loading") {
    setPageDetectionState(tabId, 0);
  }
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === "PAPERSOLVER_PAGE_DETECTED") {
    setPageDetectionState(sender?.tab?.id, Number(message.count) || 0);
    sendResponse({ ok: true });
    return false;
  }
  if (message?.type === "PAPERSOLVER_SAVE_SESSION") {
    saveSession(message.payload)
      .then(() => sendResponse({ ok: true }))
      .catch((error) => sendResponse({ ok: false, error: error?.message || "账号绑定失败" }));
    return true;
  }
  if (message?.type === "PAPERSOLVER_OPEN_PDF_CAPTURE") {
    openPdfCaptureTab(message.payload)
      .then(() => sendResponse({ ok: true }))
      .catch((error) => sendResponse({ ok: false, error: error?.message || "无法打开 PDF 页" }));
    return true;
  }
  if (message?.type === "PAPERSOLVER_UPLOAD_PDF_DATA") {
    uploadPdfDataFromPage(message.payload)
      .then((uploaded) => sendResponse({ ok: uploaded }))
      .catch((error) => sendResponse({ ok: false, error: error?.message || "PDF 上传失败" }));
    return true;
  }
  if (message?.type !== "PAPERSOLVER_IMPORT") return false;
  importPaper(message.payload)
    .then((result) => {
      chrome.notifications.create({
        type: "basic",
        iconUrl: "icon-128.svg",
        title: "PaperSolver 导入成功",
        message: result?.title ? `已导入：${result.title}` : "论文已导入文献库"
      });
      sendResponse({ ok: true, result });
    })
    .catch((error) => {
      chrome.notifications.create({
        type: "basic",
        iconUrl: "icon-128.svg",
        title: "PaperSolver 导入失败",
        message: error?.message || "请确认后端服务已启动"
      });
      sendResponse({ ok: false, error: error?.message || "导入失败" });
    });
  return true;
});

function setPageDetectionState(tabId, count) {
  if (!tabId || !chrome.action) return;
  const active = count > 0;
  chrome.action.setIcon({
    tabId,
    path: { 128: active ? "icon-active-128.png" : "icon-gray-128.png" }
  });
  chrome.action.setTitle({
    tabId,
    title: active ? `PaperSolver：识别到 ${count} 篇可导入文献` : "PaperSolver：当前页面未识别到可导入文献"
  });
  chrome.action.setBadgeText({ tabId, text: active ? String(Math.min(count, 99)) : "" });
  chrome.action.setBadgeBackgroundColor({ tabId, color: active ? "#2563eb" : "#6b7280" });
}

async function importPaper(payload) {
  const { apiBase = DEFAULT_API_BASE, accessToken = "" } = await chrome.storage.sync.get(["apiBase", "accessToken"]);
  const body = normalizePayload(payload);
  if (isPaperSolverAppPayload(body)) {
    throw new Error("当前是 PaperSolver 应用页面，不是论文 PDF 页面。请到原始 PDF 标签页导入。");
  }
  const headers = { "Content-Type": "application/json" };
  if (accessToken) {
    headers["X-PaperPilot-Session"] = String(accessToken);
  }
  const importBody = {
    ...body,
    pdfDataUrl: "",
    pdfFileName: ""
  };
  let response;
  try {
    response = await fetch(`${apiBase.replace(/\/$/, "")}/api/papers/import`, {
      method: "POST",
      headers,
      body: JSON.stringify(importBody)
    });
  } catch (error) {
    throw new Error(networkErrorMessage(error, body));
  }
  if (!response.ok) {
    throw new Error(await responseErrorMessage(response));
  }
  const result = await response.json();
  await verifyImported(apiBase, headers, result, body);
  result.pdfUrl = clean(body.paperUrl);
  result.pdfUploaded = false;
  result.pdfCapturePending = false;
  result.pdfUploadError = "";
  if (clean(body.pdfDataUrl).startsWith("data:application/pdf")) {
    const saved = await persistPdf(apiBase, headers, clean(result?.workspaceId), dataUrlToBlob(body.pdfDataUrl), clean(body.pdfFileName) || `${clean(result?.workspaceId)}.pdf`);
    result.pdfUploaded = Boolean(saved?.ok);
    result.pdfLocalCached = Boolean(saved?.local);
    result.paperUrl = saved?.paperUrl || result.paperUrl;
    result.pdfUploadError = clean(saved?.error);
    if (result.pdfUploaded) {
      await markBackendDesktopCache(apiBase, headers, clean(result?.workspaceId));
    } else {
      result.pdfCapturePending = true;
      await storePendingPdfCapture(result, body);
    }
  } else if (isLikelyPdfUrl(clean(body.paperUrl)) || isCnkiUrl(body.sourceUrl) || isCnkiUrl(body.paperUrl)) {
    const downloaded = await withTimeout(
      uploadCurrentPdfIfPossible(apiBase, headers, result, body),
      28000,
      false
    );
    result.pdfUploaded = Boolean(downloaded);
    result.pdfLocalCached = Boolean(downloaded);
    if (!downloaded) {
      result.pdfCapturePending = true;
      await storePendingPdfCapture(result, body);
      try {
        await openPdfCaptureTab({
          workspaceId: result.workspaceId,
          pdfUrl: body.paperUrl,
          title: result.title || body.title
        });
        result.pdfCaptureAutoOpened = true;
        result.pdfUploadError = "正在打开 PDF 页面并自动保存到客户端。";
      } catch {
        result.pdfUploadError = result.pdfUploadError || "无法直接下载 PDF。请确认桌面端已打开，或该网站允许插件读取 PDF。";
      }
    }
  } else if (isCnkiUrl(body.sourceUrl) || isCnkiUrl(body.paperUrl)) {
    result.pdfUploadError = "知网 PDF/CAJ 需要先登录学校或机构账号，并且当前账号必须有全文下载权限；当前仅保存了题录，请下载 PDF 后再在文献库关联。";
  }
  return result;
}

async function openPdfCaptureTab(payload = {}) {
  const workspaceId = clean(payload.workspaceId);
  const pdfUrl = clean(payload.pdfUrl);
  if (!workspaceId || (!isLikelyPdfUrl(pdfUrl) && !isCnkiUrl(pdfUrl))) {
    throw new Error("没有可补传的 PDF 链接");
  }
  await chrome.storage.local.set({
    pendingPdfCapture: {
      workspaceId,
      pdfUrl,
      title: clean(payload.title) || "当前论文",
      createdAt: Date.now()
    }
  });
  await chrome.tabs.create({ url: pdfUrl, active: true });
}

async function uploadPdfDataFromPage(payload = {}) {
  const workspaceId = clean(payload.workspaceId);
  const pdfDataUrl = clean(payload.pdfDataUrl);
  if (!workspaceId || !pdfDataUrl.startsWith("data:application/pdf")) return false;
  const { apiBase = DEFAULT_API_BASE, accessToken = "" } = await chrome.storage.sync.get(["apiBase", "accessToken"]);
  const headers = {};
  if (accessToken) {
    headers["X-PaperPilot-Session"] = String(accessToken);
  }
  const saved = await persistPdf(apiBase, headers, workspaceId, dataUrlToBlob(pdfDataUrl), clean(payload.pdfFileName) || `${workspaceId}.pdf`);
  if (saved?.ok) {
    await chrome.storage.local.remove("pendingPdfCapture");
  }
  return Boolean(saved?.ok);
}

async function storePendingPdfCapture(result, body) {
  const workspaceId = clean(result?.workspaceId);
  const pdfUrl = clean(body?.paperUrl);
  if (!workspaceId || (!isLikelyPdfUrl(pdfUrl) && !isCnkiUrl(pdfUrl))) return;
  await chrome.storage.local.set({
    pendingPdfCapture: {
      workspaceId,
      pdfUrl,
      title: clean(result?.title) || clean(body?.title) || "当前论文",
      createdAt: Date.now()
    }
  });
}

function queuePdfCapture(apiBase, headers, result, body) {
  uploadCurrentPdfIfPossible(apiBase, headers, result, body)
    .then((uploaded) => {
      chrome.notifications.create({
        type: "basic",
        iconUrl: "icon-128.svg",
        title: uploaded ? "PaperSolver PDF 已同步" : "PaperSolver PDF 捕获未完成",
        message: uploaded ? "官网 PDF 已保存到桌面端本机目录" : "文献已入库，PDF 捕获超时。可打开 PDF 原标签页再次导入。"
      });
    })
    .catch(() => {
      chrome.notifications.create({
        type: "basic",
        iconUrl: "icon-128.svg",
        title: "PaperSolver PDF 捕获未完成",
        message: "文献已入库，PDF 捕获超时。可打开 PDF 原标签页再次导入。"
      });
    });
}

async function uploadCurrentPdfIfPossible(apiBase, headers, result, body) {
  const workspaceId = clean(result?.workspaceId);
  const pdfUrl = clean(body.paperUrl);
  if (!workspaceId) return false;
  if (String(result?.paperUrl || "").includes("/api/papers/uploads/")) return true;
  if (clean(body.pdfDataUrl).startsWith("data:application/pdf")) {
    const saved = await persistPdf(apiBase, headers, workspaceId, dataUrlToBlob(body.pdfDataUrl), clean(body.pdfFileName) || `${workspaceId}.pdf`);
    if (saved?.ok) return true;
    result.pdfUploadError = saved?.error || "本地客户端保存 PDF 失败";
    return false;
  }
  const cnkiUrl = isCnkiUrl(pdfUrl) || isCnkiUrl(body?.sourceUrl);
  if (!isLikelyPdfUrl(pdfUrl) && !cnkiUrl) {
    result.pdfUploadError = "未能识别到有效的 PDF 下载链接";
    return false;
  }
  try {
    const response = await fetchWithTimeout(pdfUrl, {
      method: "GET",
      credentials: "include",
      headers: {
        "Accept": "application/pdf,application/octet-stream,*/*"
      }
    }, 12000);
    if (!response.ok) {
      result.pdfUploadError = `官网返回 HTTP ${response.status}（可能需要进入详情页或校验登录会话）`;
    } else {
      const blob = await response.blob();
      if (!blob || blob.size < 16) {
        result.pdfUploadError = "官网返回的 PDF 数据流为空";
      } else {
        const header = await blob.slice(0, 4).text();
        if (header !== "%PDF") {
          result.pdfUploadError = "官网链接返回的内容不是标准 PDF 文件（可能包含防爬验证或重定向页面）";
        } else {
          const saved = await persistPdf(apiBase, headers, workspaceId, blob, `${workspaceId}.pdf`);
          if (saved?.ok) return true;
          result.pdfUploadError = saved?.error || "PDF 下载成功，但保存到本地桌面端时失败";
          return false;
        }
      }
    }
  } catch (err) {
    result.pdfUploadError = isCnkiUrl(pdfUrl)
      ? "知网拒绝了 PDF 请求：请确认已登录学校/机构账号，并拥有 PDF/CAJ 下载权限。"
      : `直接抓取异常：${err?.message || "网络请求超时或跨域受限"}`;
  }
  return capturePdfViaBrowserTab(apiBase, headers, workspaceId, pdfUrl);
}

async function persistPdf(apiBase, headers, workspaceId, blob, fileName) {
  if (!workspaceId || !blob || blob.size < 16) return { ok: false, error: "PDF 内容为空" };
  try {
    const localResult = await cachePdfOnDesktop(workspaceId, await blobToDataUrl(blob), fileName);
    if (localResult?.ok) {
      await markBackendDesktopCache(apiBase, headers, workspaceId);
      return { ok: true, local: true, paperUrl: localResult.paperUrl || `desktop-cache://${workspaceId}` };
    }
    const remoteResult = await uploadPdfBlobResult(apiBase, headers, workspaceId, blob, fileName);
    return remoteResult?.ok
      ? { ok: true, local: false }
      : { ok: false, error: remoteResult?.error || localResult?.error || "PDF 保存失败" };
  } catch (error) {
    return { ok: false, error: error?.message || "PDF 保存失败" };
  }
}

async function uploadPdfBlob(apiBase, headers, workspaceId, blob, fileName) {
  const result = await uploadPdfBlobResult(apiBase, headers, workspaceId, blob, fileName);
  return Boolean(result.ok);
}

async function uploadPdfBlobResult(apiBase, headers, workspaceId, blob, fileName) {
  if (!blob || blob.size < 16) return false;
  const formData = new FormData();
  formData.append("file", blob, fileName || `${workspaceId}.pdf`);
  const uploadHeaders = {};
  if (headers["X-PaperPilot-Session"]) {
    uploadHeaders["X-PaperPilot-Session"] = headers["X-PaperPilot-Session"];
  }
  const response = await fetchWithTimeout(`${apiBase.replace(/\/$/, "")}/api/papers/${encodeURIComponent(workspaceId)}/upload`, {
    method: "POST",
    headers: uploadHeaders,
    body: formData
  }, 20000);
  if (response.ok) return { ok: true };
  let error = `HTTP ${response.status}`;
  try {
    const text = await response.text();
    if (text) error = `${error}: ${text.slice(0, 160)}`;
  } catch {
    // Keep HTTP status.
  }
  return { ok: false, error };
}

function dataUrlToBlob(dataUrl) {
  const [meta, data] = String(dataUrl || "").split(",");
  const mime = (meta.match(/^data:([^;]+)/) || [])[1] || "application/pdf";
  const binary = atob(data || "");
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
  return new Blob([bytes], { type: mime });
}

async function blobToDataUrl(blob) {
  const bytes = new Uint8Array(await blob.arrayBuffer());
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return `data:${blob.type || "application/pdf"};base64,${btoa(binary)}`;
}

async function cachePdfOnDesktop(workspaceId, pdfDataUrl, pdfFileName) {
  if (!workspaceId || !clean(pdfDataUrl).startsWith("data:application/pdf")) {
    return { ok: false, error: "PDF 内容为空" };
  }
  try {
    const response = await fetchWithTimeout(`${DESKTOP_CAPTURE_BASE}/cache-pdf`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ workspaceId, pdfDataUrl, pdfFileName })
    }, 22000);
    const result = await response.json().catch(() => ({}));
    if (response.ok && result?.ok) return result;
    return { ok: false, error: clean(result?.error) || `桌面端返回 HTTP ${response.status}` };
  } catch {
    return { ok: false, error: "桌面端未打开，或本机 PDF 接收服务不可用" };
  }
}

async function markBackendDesktopCache(apiBase, headers, workspaceId) {
  if (!workspaceId) return false;
  const response = await fetchWithTimeout(`${apiBase.replace(/\/$/, "")}/api/library/papers/${encodeURIComponent(workspaceId)}`, {
    method: "PATCH",
    headers: {
      ...headers,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ paperUrl: `desktop-cache://${workspaceId}` })
  }, 12000);
  return response.ok;
}

async function capturePdfViaBrowserTab(apiBase, headers, workspaceId, pdfUrl) {
  if (!chrome.tabs || !chrome.scripting) return false;
  let tabId = null;
  try {
    const tab = await chrome.tabs.create({ url: pdfUrl, active: false });
    tabId = tab.id;
    await waitForTabComplete(tabId, 20000);
    const [injection] = await withTimeout(chrome.scripting.executeScript({
      target: { tabId },
      func: async () => {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), 12000);
        const response = await fetch(location.href, {
          method: "GET",
          credentials: "include",
          headers: { Accept: "application/pdf,application/octet-stream,*/*" },
          signal: controller.signal
        }).finally(() => clearTimeout(timer));
        if (!response.ok) return { ok: false, reason: `HTTP ${response.status}` };
        const blob = await response.blob();
        if (!blob || blob.size < 16 || blob.size > 100 * 1024 * 1024) {
          return { ok: false, reason: "PDF 文件为空或超过 100MB" };
        }
        const head = await blob.slice(0, 4).text();
        if (head !== "%PDF") return { ok: false, reason: "当前标签页不是 PDF" };
        const dataUrl = await new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = () => resolve(String(reader.result || ""));
          reader.onerror = reject;
          reader.readAsDataURL(blob);
        });
        const fileName = (location.pathname.split("/").filter(Boolean).pop() || "paper.pdf").replace(/[?#].*$/, "");
        return { ok: true, dataUrl, fileName: fileName.toLowerCase().endsWith(".pdf") ? fileName : `${fileName}.pdf` };
      }
    }), 18000, []);
    const value = injection?.result;
    if (value?.ok && value.dataUrl) {
      const saved = await persistPdf(apiBase, headers, workspaceId, dataUrlToBlob(value.dataUrl), value.fileName || `${workspaceId}.pdf`);
      return Boolean(saved?.ok);
    }
  } catch {
    return false;
  } finally {
    if (tabId) {
      chrome.tabs.remove(tabId).catch(() => {});
    }
  }
  return false;
}

function fetchWithTimeout(url, options = {}, timeoutMs = 12000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  return fetch(url, { ...options, signal: controller.signal }).finally(() => clearTimeout(timer));
}

async function responseErrorMessage(response) {
  let detail = "";
  try {
    const text = await response.text();
    if (text) {
      try {
        const json = JSON.parse(text);
        detail = clean(json.message || json.error || text);
      } catch {
        detail = clean(text);
      }
    }
  } catch {
    // Keep status-only fallback.
  }
  if (response.status === 401) {
    return "插件未登录或登录令牌已失效。请先打开最新版 PaperSolver 桌面端登录 QQ，再打开插件重新绑定账号。";
  }
  return detail ? `后端返回 HTTP ${response.status}: ${detail}` : `后端返回 HTTP ${response.status}`;
}

function withTimeout(promise, timeoutMs, fallback) {
  return new Promise((resolve) => {
    const timer = setTimeout(() => resolve(fallback), timeoutMs);
    Promise.resolve(promise)
      .then((value) => {
        clearTimeout(timer);
        resolve(value);
      })
      .catch(() => {
        clearTimeout(timer);
        resolve(fallback);
      });
  });
}

function waitForTabComplete(tabId, timeoutMs) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      chrome.tabs.onUpdated.removeListener(listener);
      reject(new Error("PDF 标签页加载超时"));
    }, timeoutMs);
    const listener = (updatedTabId, changeInfo) => {
      if (updatedTabId === tabId && changeInfo.status === "complete") {
        clearTimeout(timer);
        chrome.tabs.onUpdated.removeListener(listener);
        resolve();
      }
    };
    chrome.tabs.onUpdated.addListener(listener);
  });
}

async function verifyImported(apiBase, headers, result, body) {
  const workspaceId = clean(result?.workspaceId);
  if (!workspaceId) {
    throw new Error("后端未返回文献 ID，无法确认是否导入成功");
  }
  const response = await fetch(`${apiBase.replace(/\/$/, "")}/api/library/papers/${encodeURIComponent(workspaceId)}`, {
    method: "GET",
    headers
  });
  if (response.status === 404) {
    throw new Error("后端已响应，但当前账号文献库未找到该论文。请先打开 PaperSolver 页面绑定登录账号。");
  }
  if (!response.ok) {
    throw new Error(`已提交但文献库校验失败 HTTP ${response.status}`);
  }
  const paper = await response.json();
  if (paper?.workspaceId !== workspaceId) {
    throw new Error("导入校验异常，请刷新文献库后确认。");
  }
}

async function saveSession(payload = {}) {
  const userId = clean(payload.userId);
  if (!/^\d+$/.test(userId)) return;
  await chrome.storage.sync.set({
    userId,
    userName: clean(payload.userName),
    accessToken: clean(payload.accessToken),
    appUrl: clean(payload.appUrl)
  });
}

function normalizePayload(payload = {}) {
  const title = clean(payload.title) || "未命名论文";
  const source = clean(payload.source) || hostLabel(payload.url || payload.paperUrl || "");
  return {
    source,
    paperId: clean(payload.doi) || clean(payload.paperId) || "",
    paperUrl: clean(payload.pdfUrl) || clean(payload.url) || "",
    sourceUrl: clean(payload.sourceUrl) || clean(payload.url) || "",
    importSource: clean(payload.importSource) || source,
    title,
    abstractText: clean(payload.abstractText) || "由 PaperSolver Capture 从官网页面导入，摘要待补充。",
    authors: clean(payload.authors) || "",
    publishYear: clean(payload.year) || "",
    articleType: clean(payload.articleType) || "",
    subjects: Array.isArray(payload.subjects) ? payload.subjects.map(clean).filter(Boolean) : [],
    pdfDataUrl: clean(payload.pdfDataUrl),
    pdfFileName: clean(payload.pdfFileName)
  };
}

function isPaperSolverAppPayload(body = {}) {
  const fields = [
    body.source,
    body.importSource,
    body.sourceUrl,
    body.paperUrl,
    body.title,
  ].map((value) => String(value || "").toLowerCase());
  const isLocalApp = fields.some((value) =>
    value.includes("localhost") ||
    value.includes("127.0.0.1") ||
    value.includes("paperslover ai workspace") ||
    value.includes("papersolver ai workspace")
  );
  const hasOnlyProxyPdf = String(body.paperUrl || "").includes("/api/papers/proxy?url=");
  return isLocalApp && hasOnlyProxyPdf;
}

function clean(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

function hostLabel(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "官网捕获";
  }
}

function isLikelyPdfUrl(url) {
  return /\.pdf($|[?#])|\/pdf\/|\/pdfft($|[?#])|arxiv\.org\/pdf\/|pdf\.sciencedirectassets\.com|reader\.elsevier\.com|\/reader\/sd\/pii\/|\/science\/article\/pii\/[^/]+\/pdfft/i.test(url || "");
}

function isCnkiUrl(url) {
  try {
    return /(^|\.)cnki\.net$/i.test(new URL(url).hostname);
  } catch {
    return false;
  }
}

function networkErrorMessage(error, body = {}) {
  if (isCnkiUrl(body?.sourceUrl) || isCnkiUrl(body?.paperUrl)) {
    return "知网请求失败：请先登录学校/机构账号，并确认当前账号有 PDF/CAJ 下载权限。若只能打开题录页，请先在知网下载 PDF，再到文献库手动关联。";
  }
  const message = String(error?.message || "");
  return /failed to fetch|networkerror|load failed/i.test(message)
    ? "插件无法连接 PaperSolver 后端，请检查网络、登录状态或扩展权限后重试。"
    : message || "网络请求失败，请稍后重试";
}
