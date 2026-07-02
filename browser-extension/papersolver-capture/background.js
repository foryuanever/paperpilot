const DEFAULT_API_BASE = "http://127.0.0.1:8080";

chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.sync.set({ apiBase: DEFAULT_API_BASE });
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
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

async function importPaper(payload) {
  const { apiBase = DEFAULT_API_BASE, userId = "" } = await chrome.storage.sync.get(["apiBase", "userId"]);
  const body = normalizePayload(payload);
  if (isPaperSolverAppPayload(body)) {
    throw new Error("当前是 PaperSolver 应用页面，不是论文 PDF 页面。请到原始 PDF 标签页导入。");
  }
  const headers = { "Content-Type": "application/json" };
  if (/^\d+$/.test(String(userId))) {
    headers["X-PaperPilot-User-Id"] = String(userId);
  }
  const response = await fetch(`${apiBase.replace(/\/$/, "")}/api/papers/import`, {
    method: "POST",
    headers,
    body: JSON.stringify(body)
  });
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
    const uploadResult = await withTimeout(
      uploadPdfBlobResult(apiBase, headers, clean(result?.workspaceId), dataUrlToBlob(body.pdfDataUrl), clean(body.pdfFileName) || `${clean(result?.workspaceId)}.pdf`),
      20000,
      { ok: false, error: "上传接口超时" }
    );
    result.pdfUploaded = Boolean(uploadResult?.ok);
    result.pdfUploadError = clean(uploadResult?.error);
    if (!result.pdfUploaded) {
      result.pdfCapturePending = true;
      await storePendingPdfCapture(result, body);
    }
  } else if (isLikelyPdfUrl(clean(body.paperUrl))) {
    result.pdfCapturePending = true;
    await storePendingPdfCapture(result, body);
    queuePdfCapture(apiBase, headers, result, body);
  }
  return result;
}

async function openPdfCaptureTab(payload = {}) {
  const workspaceId = clean(payload.workspaceId);
  const pdfUrl = clean(payload.pdfUrl);
  if (!workspaceId || !isLikelyPdfUrl(pdfUrl)) {
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
  const { apiBase = DEFAULT_API_BASE, userId = "" } = await chrome.storage.sync.get(["apiBase", "userId"]);
  const headers = {};
  if (/^\d+$/.test(String(userId))) {
    headers["X-PaperPilot-User-Id"] = String(userId);
  }
  const uploaded = await withTimeout(
    uploadPdfBlob(apiBase, headers, workspaceId, dataUrlToBlob(pdfDataUrl), clean(payload.pdfFileName) || `${workspaceId}.pdf`),
    20000,
    false
  );
  if (uploaded) {
    await chrome.storage.local.remove("pendingPdfCapture");
  }
  return uploaded;
}

async function storePendingPdfCapture(result, body) {
  const workspaceId = clean(result?.workspaceId);
  const pdfUrl = clean(body?.paperUrl);
  if (!workspaceId || !isLikelyPdfUrl(pdfUrl)) return;
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
        message: uploaded ? "官网 PDF 已自动导入文献库" : "文献已入库，PDF 捕获超时。可打开 PDF 原标签页再次导入。"
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
    return uploadPdfBlob(apiBase, headers, workspaceId, dataUrlToBlob(body.pdfDataUrl), clean(body.pdfFileName) || `${workspaceId}.pdf`);
  }
  if (!isLikelyPdfUrl(pdfUrl)) return false;
  try {
    const response = await fetchWithTimeout(pdfUrl, {
      method: "GET",
      credentials: "include",
      headers: {
        "Accept": "application/pdf,application/octet-stream,*/*"
      }
    }, 12000);
    if (!response.ok) return;
    const blob = await response.blob();
    if (!blob || blob.size < 16) return;
    const header = await blob.slice(0, 4).text();
    if (header !== "%PDF") return;
    return uploadPdfBlob(apiBase, headers, workspaceId, blob, `${workspaceId}.pdf`);
  } catch {
    // Fall through to tab capture. Some publishers only expose the PDF inside a browser tab.
  }
  return capturePdfViaBrowserTab(apiBase, headers, workspaceId, pdfUrl);
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
  if (headers["X-PaperPilot-User-Id"]) {
    uploadHeaders["X-PaperPilot-User-Id"] = headers["X-PaperPilot-User-Id"];
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
      return uploadPdfBlob(apiBase, headers, workspaceId, dataUrlToBlob(value.dataUrl), value.fileName || `${workspaceId}.pdf`);
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
