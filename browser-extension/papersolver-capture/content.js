(() => {
  if (window.__paperSolverCaptureLoaded) return;
  window.__paperSolverCaptureLoaded = true;

  syncPaperSolverSession();
  if (isPaperSolverAppHost()) {
    setInterval(syncPaperSolverSession, 5000);
    return;
  }

  checkPendingPdfCapture().then((handled) => {
    if (handled) return;
    const paper = detectPaper();
    if (!paper || (!paper.doi && !paper.pdfUrl && !hasCitationMeta() && !looksLikePaperPage())) return;
    showPrompt(paper);
  });

  function syncPaperSolverSession() {
    if (!isPaperSolverAppHost()) return;
    try {
      const raw = localStorage.getItem("paperpilot-auth");
      if (!raw) return;
      const session = JSON.parse(raw);
      const userId = session?.user?.userId;
      if (!userId || !/^\d+$/.test(String(userId))) return;
      chrome.runtime.sendMessage({
        type: "PAPERSOLVER_SAVE_SESSION",
        payload: {
          userId: String(userId),
          userName: session?.user?.name || "",
          appUrl: location.origin
        }
      });
    } catch {
      // Ignore malformed local sessions; importing can still use the default local account.
    }
  }

  function isPaperSolverAppHost() {
    return /^(127\.0\.0\.1|localhost)$/i.test(location.hostname);
  }

  function detectPaper() {
    const url = location.href;
    const jsonLd = readJsonLd();
    const title = firstMeta([
      "citation_title",
      "dc.Title",
      "dc.title",
      "DC.Title",
      "crossmark_title",
      "og:title",
      "twitter:title"
    ]) || clean(jsonLd.title || jsonLd.name) || cleanupTitle(document.title);
    const doi = firstMeta([
      "citation_doi",
      "dc.Identifier",
      "dc.identifier",
      "DC.Identifier",
      "prism.doi",
      "dc.identifier.doi",
      "doi"
    ]) || clean(jsonLd.doi) || extractDoi(document.body?.innerText || "") || extractDoi(url);
    const pii = extractScienceDirectPii(url);
    const scienceDirectDetailUrl = pii ? `https://www.sciencedirect.com/science/article/pii/${pii}` : "";
    const pdfUrl = normalizeUrl(
      firstMeta(["citation_pdf_url", "pdf_url", "bepress_citation_pdf_url"]) ||
      clean(jsonLd.encoding?.contentUrl || jsonLd.associatedMedia?.contentUrl) ||
      inferKnownPublisherPdfUrl(url, doi, pii) ||
      findPdfLink() ||
      currentPdfDocumentUrl()
    );
    const abstractText = firstMeta([
      "citation_abstract",
      "dc.Description",
      "description",
      "dc.description",
      "dcterms.abstract",
      "og:description"
    ]) || clean(jsonLd.abstract || jsonLd.description);
    const authors = allMeta("citation_author").join(", ") ||
      firstMeta(["dc.creator", "DC.Creator", "article:author", "author"]) ||
      normalizeJsonLdAuthors(jsonLd.author);
    const year = firstMeta(["citation_publication_date", "citation_online_date", "dc.date"])
      .replace(/^(\d{4}).*$/, "$1");

    return {
      title,
      doi: doi || pii,
      pdfUrl,
      url,
      sourceUrl: isCurrentPdfDocument() ? (scienceDirectDetailUrl || document.referrer || url) : url,
      detailUrl: scienceDirectDetailUrl,
      importSource: hostLabel(url),
      source: hostLabel(url),
      abstractText,
      authors,
      year
    };
  }

  function readJsonLd() {
    const scripts = Array.from(document.querySelectorAll('script[type="application/ld+json"]'));
    for (const script of scripts) {
      try {
        const parsed = JSON.parse(script.textContent || "{}");
        const items = Array.isArray(parsed) ? parsed : [parsed, ...(parsed["@graph"] || [])];
        const paper = items.find((item) => {
          const type = String(item?.["@type"] || "").toLowerCase();
          return type.includes("scholarlyarticle") || type.includes("article") || item?.doi;
        });
        if (paper) return paper;
      } catch {
        // Ignore invalid JSON-LD blocks.
      }
    }
    return {};
  }

  function normalizeJsonLdAuthors(authors) {
    if (!authors) return "";
    const list = Array.isArray(authors) ? authors : [authors];
    return list.map((author) => {
      if (typeof author === "string") return author;
      return clean(author.name || [author.givenName, author.familyName].filter(Boolean).join(" "));
    }).filter(Boolean).join(", ");
  }

  function showPrompt(paper) {
    const existing = document.getElementById("papersolver-capture-root");
    if (existing) existing.remove();

    const root = document.createElement("div");
    root.id = "papersolver-capture-root";
    root.innerHTML = `
      <div class="ps-card">
        <div class="ps-mark">P</div>
        <div class="ps-main">
          <strong>发现可导入文献</strong>
          <span>${escapeHtml(paper.title || "当前论文页面")}</span>
          <small>${escapeHtml(importHint(paper))}</small>
          <em class="ps-status" hidden></em>
        </div>
        <div class="ps-actions">
          ${isCurrentPdfDocument() && paper.detailUrl ? `<a class="ps-detail" href="${escapeHtml(paper.detailUrl)}" target="_blank" rel="noopener">打开详情页</a>` : ""}
          <button type="button" class="ps-import">${isCurrentPdfDocument() ? "补传 PDF" : "导入题录和PDF"}</button>
          <button type="button" class="ps-close">×</button>
        </div>
      </div>
    `;
    document.documentElement.appendChild(root);
    root.querySelector(".ps-close").addEventListener("click", () => root.remove());
    root.querySelector(".ps-import").addEventListener("click", async () => {
      const button = root.querySelector(".ps-import");
      const status = root.querySelector(".ps-status");
      button.disabled = true;
      button.textContent = "导入中...";
      status.hidden = false;
      status.textContent = paper.pdfUrl ? "正在读取官网 PDF..." : "正在导入元数据...";
      const payload = await attachPdfDataUrlIfPossible(paper, status);
      let answered = false;
      const messageTimer = setTimeout(() => {
        if (answered) return;
        answered = true;
        button.disabled = false;
        button.textContent = "重试导入";
        root.classList.add("ps-error");
        status.textContent = "导入请求超时，请确认 PaperSolver 后端和插件账号连接正常";
      }, 45000);
      chrome.runtime.sendMessage({ type: "PAPERSOLVER_IMPORT", payload }, (response) => {
        if (answered) return;
        answered = true;
        clearTimeout(messageTimer);
        const runtimeError = chrome.runtime.lastError?.message;
        if (response?.ok) {
          status.hidden = false;
          root.classList.add("ps-success");
          if (response?.result?.pdfUploaded) {
            button.textContent = "已导入";
            status.textContent = "已保存到文献库，PDF 已同步";
            setTimeout(() => root.remove(), 1800);
          } else if (response?.result?.pdfCapturePending && response?.result?.pdfUrl) {
            button.disabled = false;
            if (isCurrentPdfDocument()) {
              button.textContent = "重试补传 PDF";
              status.textContent = response?.result?.pdfUploadError
                ? `PDF 已识别，但上传未完成：${response.result.pdfUploadError}`
                : "PDF 已识别，但上传未完成。请重试补传。";
              button.onclick = () => retryCurrentPdfUpload(response.result, button, status);
            } else {
              button.textContent = "打开 PDF 页补传";
              status.textContent = "题录已入库；自动读取 PDF 受限。请打开 PDF 页补传到同一篇文献。";
              button.onclick = () => openPdfCapture(response.result, status);
            }
          } else {
            button.textContent = "已导入";
            status.textContent = "题录已保存到文献库；当前页面没有提供可读取的 PDF 链接。";
          }
        } else {
          button.disabled = false;
          button.textContent = "重试导入";
          root.classList.add("ps-error");
          status.hidden = false;
          status.textContent = response?.error || runtimeError || "导入失败，请确认 PaperSolver 后端已启动";
        }
      });
    });
  }

  function openPdfCapture(result, status) {
    status.textContent = "正在打开 PDF 页...";
    chrome.runtime.sendMessage({
      type: "PAPERSOLVER_OPEN_PDF_CAPTURE",
      payload: {
        workspaceId: result?.workspaceId,
        pdfUrl: result?.pdfUrl,
        title: result?.title,
      }
    }, (response) => {
      if (response?.ok) {
        status.textContent = "已打开 PDF 页。进入 PDF 页后点击插件提示中的“补传 PDF”。";
      } else {
        status.textContent = response?.error || "无法打开 PDF 页，请在官网手动打开 PDF 后再点插件。";
      }
    });
  }

  async function retryCurrentPdfUpload(result, button, status) {
    button.disabled = true;
    button.textContent = "补传中...";
    status.textContent = "正在重新读取当前 PDF...";
    const payload = await attachPdfDataUrlIfPossible({ pdfUrl: currentPdfDocumentUrl() || result?.pdfUrl }, status);
    if (!payload.pdfDataUrl) {
      button.disabled = false;
      button.textContent = "重试补传 PDF";
      status.textContent = "当前 PDF 无法被浏览器扩展读取。请下载 PDF 后在文献库上传。";
      return;
    }
    chrome.runtime.sendMessage({
      type: "PAPERSOLVER_UPLOAD_PDF_DATA",
      payload: {
        workspaceId: result?.workspaceId,
        pdfDataUrl: payload.pdfDataUrl,
        pdfFileName: payload.pdfFileName,
      }
    }, (response) => {
      if (response?.ok) {
        button.textContent = "PDF 已补传";
        status.textContent = "PDF 已同步到文献库。";
        setTimeout(() => document.getElementById("papersolver-capture-root")?.remove(), 1800);
      } else {
        button.disabled = false;
        button.textContent = "重试补传 PDF";
        status.textContent = response?.error || "PDF 上传失败，请确认 PaperSolver 后端已启动。";
      }
    });
  }

  async function checkPendingPdfCapture() {
    if (!isCurrentPdfDocument()) return false;
    const pending = await new Promise((resolve) => {
      chrome.storage.local.get(["pendingPdfCapture"], (value) => resolve(value?.pendingPdfCapture || null));
    });
    if (!pending?.workspaceId || Date.now() - Number(pending.createdAt || 0) > 30 * 60 * 1000) return false;
    const expected = normalizeUrl(pending.pdfUrl || "");
    const current = normalizeUrl(location.href);
    if (expected && current && expected !== current && !current.includes(new URL(expected).pathname.split("/").pop())) {
      return false;
    }
    showPdfCapturePrompt(pending);
    return true;
  }

  function showPdfCapturePrompt(pending) {
    const paper = { pdfUrl: location.href };
    const existing = document.getElementById("papersolver-capture-root");
    if (existing) existing.remove();
    const root = document.createElement("div");
    root.id = "papersolver-capture-root";
    root.innerHTML = `
      <div class="ps-card">
        <div class="ps-mark">P</div>
        <div class="ps-main">
          <strong>补传 PDF 到 PaperSolver</strong>
          <span>${escapeHtml(pending.title || "当前论文")}</span>
          <small>当前是 PDF 页，将补传到刚才导入的文献</small>
          <em class="ps-status" hidden></em>
        </div>
        <div class="ps-actions">
          <button type="button" class="ps-import">补传 PDF</button>
          <button type="button" class="ps-close">×</button>
        </div>
      </div>
    `;
    document.documentElement.appendChild(root);
    root.querySelector(".ps-close").addEventListener("click", () => root.remove());
    root.querySelector(".ps-import").addEventListener("click", async () => {
      const button = root.querySelector(".ps-import");
      const status = root.querySelector(".ps-status");
      button.disabled = true;
      button.textContent = "补传中...";
      status.hidden = false;
      status.textContent = "正在读取当前 PDF...";
      const payload = await attachPdfDataUrlIfPossible(paper, status);
      if (!payload.pdfDataUrl) {
        button.disabled = false;
        button.textContent = "重试补传";
        root.classList.add("ps-error");
        status.textContent = "浏览器没有允许读取当前 PDF。请下载 PDF 后在文献库上传。";
        return;
      }
      chrome.runtime.sendMessage({
        type: "PAPERSOLVER_UPLOAD_PDF_DATA",
        payload: {
          workspaceId: pending.workspaceId,
          pdfDataUrl: payload.pdfDataUrl,
          pdfFileName: payload.pdfFileName,
        }
      }, (response) => {
        if (response?.ok) {
          button.textContent = "PDF 已补传";
          root.classList.add("ps-success");
          status.textContent = "PDF 已同步到文献库。";
          setTimeout(() => root.remove(), 1800);
        } else {
          button.disabled = false;
          button.textContent = "重试补传";
          root.classList.add("ps-error");
          status.textContent = response?.error || "PDF 上传失败，请确认 PaperSolver 后端已启动。";
        }
      });
    });
  }

  async function attachPdfDataUrlIfPossible(paper, status) {
    const pdfUrl = normalizeUrl(paper?.pdfUrl || currentPdfDocumentUrl());
    if (!pdfUrl || !isPdfUrl(pdfUrl)) return paper;
    try {
      const response = await fetchWithTimeout(pdfUrl, {
        method: "GET",
        credentials: "include",
        headers: { Accept: "application/pdf,application/octet-stream,*/*" }
      }, 12000);
      if (!response.ok) return paper;
      const blob = await response.blob();
      if (!blob || blob.size < 16 || blob.size > 100 * 1024 * 1024) return paper;
      const head = await blob.slice(0, 4).text();
      if (head !== "%PDF") return paper;
      status.textContent = "PDF 已读取，正在上传 PaperSolver...";
      const pdfDataUrl = await blobToDataUrl(blob);
      return { ...paper, pdfUrl, pdfDataUrl, pdfFileName: filenameFromUrl(pdfUrl) };
    } catch {
      return paper;
    }
  }

  function fetchWithTimeout(url, options = {}, timeoutMs = 12000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    return fetch(url, { ...options, signal: controller.signal }).finally(() => clearTimeout(timer));
  }

  function blobToDataUrl(blob) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ""));
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }

  function filenameFromUrl(url) {
    try {
      const name = new URL(url).pathname.split("/").filter(Boolean).pop() || "paper.pdf";
      return name.toLowerCase().endsWith(".pdf") ? name : `${name}.pdf`;
    } catch {
      return "paper.pdf";
    }
  }

  function importHint(paper) {
    if (isCurrentPdfDocument()) {
      return paper.doi
        ? `当前是 PDF 文件页；建议先打开详情页导入，作者和摘要更完整`
        : "当前是 PDF 文件页；建议回论文详情页获取作者、摘要和期刊信息";
    }
    if (paper.pdfUrl) return "论文详情页：已识别 PDF 链接和页面元数据";
    if (paper.doi) return `论文详情页：DOI/PII ${paper.doi}`;
    return paper.source;
  }

  function firstMeta(names) {
    for (const name of names) {
      const selector = [
        `meta[name="${cssEscape(name)}"]`,
        `meta[property="${cssEscape(name)}"]`,
        `meta[name="${cssEscape(name.toLowerCase())}"]`,
        `meta[property="${cssEscape(name.toLowerCase())}"]`
      ].join(",");
      const value = document.querySelector(selector)?.content;
      if (value && value.trim()) return value.trim();
    }
    return "";
  }

  function allMeta(name) {
    return Array.from(document.querySelectorAll(`meta[name="${cssEscape(name)}"]`))
      .map((node) => node.content?.trim())
      .filter(Boolean);
  }

  function findPdfLink() {
    const relPdf = document.querySelector('link[type="application/pdf"], link[rel="alternate"][href*=".pdf"], a[type="application/pdf"]')?.href;
    if (relPdf) return relPdf;
    const anchors = Array.from(document.querySelectorAll("a[href]"));
    const directPdf = anchors.find((a) => isPdfUrl(a.href) && !isSupplementaryPdf(a));
    if (directPdf) return directPdf.href;
    const textPdf = anchors.find((a) => {
      const text = clean(a.textContent || a.getAttribute("aria-label") || a.getAttribute("title") || "");
      const href = String(a.href || "");
      if (isSupplementaryPdf(a)) return false;
      return /(pdf|full\s*text|download\s*pdf|article\s*pdf|view\s*pdf|下载全文|全文下载|PDF下载)/i.test(text)
        && /(pdf|download|article|content|full|doi|pdfdirect|pdfft)/i.test(href);
    });
    return textPdf?.href || "";
  }

  function inferKnownPublisherPdfUrl(url, doi, pii) {
    const value = String(url || "");
    try {
      const parsed = new URL(value);
      const host = parsed.hostname.toLowerCase();
      if (host.includes("sciencedirect.com") && pii) {
        return `https://www.sciencedirect.com/science/article/pii/${pii}/pdfft`;
      }
      if (host.includes("arxiv.org") && /\/abs\//.test(parsed.pathname)) {
        return value.replace("/abs/", "/pdf/") + (value.toLowerCase().endsWith(".pdf") ? "" : ".pdf");
      }
      if (host.includes("aclanthology.org")) {
        const match = parsed.pathname.match(/\/([A-Z0-9][A-Z0-9.-]+)\/?$/i);
        if (match) return `${parsed.origin}/${match[1]}.pdf`;
      }
      if (host.includes("nature.com") && parsed.pathname.includes("/articles/")) {
        return `${parsed.origin}${parsed.pathname.replace(/\/$/, "")}.pdf`;
      }
      if (host.includes("link.springer.com") && parsed.pathname.includes("/article/")) {
        return `${parsed.origin}${parsed.pathname.replace("/article/", "/content/pdf/")}.pdf`;
      }
      if (host.includes("dl.acm.org") && doi) {
        return `https://dl.acm.org/doi/pdf/${doi}`;
      }
      if (host.includes("ieeexplore.ieee.org")) {
        const arnumber = parsed.searchParams.get("arnumber") || parsed.pathname.match(/\/document\/(\d+)/)?.[1];
        if (arnumber) return `https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=${arnumber}`;
      }
    } catch {
      // Keep generic link discovery as fallback.
    }
    return "";
  }

  function isSupplementaryPdf(anchor) {
    const text = clean(anchor?.textContent || anchor?.getAttribute?.("aria-label") || anchor?.getAttribute?.("title") || "");
    const href = String(anchor?.href || "");
    return /(supplement|supporting|appendix|附件|补充材料|附录)/i.test(text + " " + href);
  }

  function looksLikePaperPage() {
    return Boolean(
      document.querySelector('meta[name^="citation_"]') ||
      /doi\.org|sciencedirect|semanticscholar|pubmed|webofscience|cnki|wanfang|arxiv|aclanthology|springer|nature|ieee|acm/i.test(location.hostname + location.pathname)
    );
  }

  function hasCitationMeta() {
    return Boolean(document.querySelector('meta[name^="citation_"], meta[name="dc.title"], meta[name="DC.Title"]'));
  }

  function isPdfUrl(url) {
    return /\.pdf($|[?#])|\/pdf\/|\/pdfft($|[?#])|arxiv\.org\/pdf\/|pdf\.sciencedirectassets\.com|reader\.elsevier\.com|\/reader\/sd\/pii\/|\/science\/article\/pii\/[^/]+\/pdfft/i.test(url || "");
  }

  function isCurrentPdfDocument() {
    return Boolean(currentPdfDocumentUrl()) ||
      isPdfUrl(location.href) ||
      document.contentType === "application/pdf" ||
      document.querySelector("embed[type='application/pdf'], iframe[src*='.pdf']") !== null;
  }

  function currentPdfDocumentUrl() {
    if (isPdfUrl(location.href)) return location.href;
    const embedded = document.querySelector("embed[type='application/pdf'], iframe[src]");
    const src = embedded?.src || embedded?.getAttribute?.("original-url") || embedded?.getAttribute?.("src") || "";
    if (src && isPdfUrl(src)) return normalizeUrl(src);
    if (document.contentType === "application/pdf") return location.href;
    if (document.querySelector("embed[type='application/pdf']")) return location.href;
    return "";
  }

  function normalizeUrl(url) {
    if (!url) return "";
    try {
      return new URL(url, location.href).href;
    } catch {
      return url;
    }
  }

  function extractDoi(text) {
    return clean((String(text || "").match(/10\.\d{4,9}\/[-._;()/:A-Z0-9]+/i) || [])[0] || "")
      .replace(/[)\].,;，。；、]+$/g, "");
  }

  function extractScienceDirectPii(text) {
    const value = String(text || "");
    try {
      const parsed = new URL(value, location.href);
      const pii = parsed.searchParams.get("pii");
      if (pii && /^S[A-Z0-9]{15,30}$/i.test(pii)) return pii;
    } catch {
      // Fall back to path matching.
    }
    const match = value.match(/(?:\/pii\/|1-s2\.0-)(S[A-Z0-9]{15,30})/i);
    return match ? match[1] : "";
  }

  function cleanupTitle(text) {
    return String(text || "")
      .replace(/\.pdf\s*$/i, "")
      .replace(/\s*[-|]\s*(ScienceDirect|PubMed|Semantic Scholar|Web of Science|CNKI|万方|arXiv|SpringerLink|IEEE Xplore|ACM Digital Library).*$/i, "")
      .replace(/\s+/g, " ")
      .trim();
  }

  function hostLabel(url) {
    try {
      return new URL(url).hostname.replace(/^www\./, "");
    } catch {
      return "官网捕获";
    }
  }

  function cssEscape(value) {
    return String(value).replace(/"/g, '\\"');
  }

  function clean(value) {
    return String(value || "").replace(/\s+/g, " ").trim();
  }

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
})();
