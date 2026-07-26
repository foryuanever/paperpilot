(() => {
  if (window.__paperSolverCaptureLoaded) return;
  window.__paperSolverCaptureLoaded = true;

  if (isPaperSolverAppHost()) {
    return;
  }

  checkPendingPdfCapture().then((handled) => {
    if (handled) return;
    const paper = detectPaper();
    const detectedCount = Array.isArray(paper?.pdfCandidates) ? paper.pdfCandidates.length : 0;
    notifyPageDetected(detectedCount);
    if (!shouldOfferCapture(paper)) return;
    showPrompt(paper);
  });

  function isPaperSolverAppHost() {
    return /^(127\.0\.0\.1|localhost)$/i.test(location.hostname);
  }

  function notifyPageDetected(count) {
    chrome.runtime.sendMessage({
      type: "PAPERSOLVER_PAGE_DETECTED",
      count: Number(count) || 0
    }, () => {});
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
    const paperCandidates = collectPaperCandidates({ jsonLd, doi, pii, title });
    const pdfCandidates = paperCandidates.length ? paperCandidates : collectPdfCandidates({ jsonLd, doi, pii, title });
    const pdfUrl = pdfCandidates[0]?.url || "";
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
      year,
      pdfCandidates
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
    const pdfCandidates = Array.isArray(paper.pdfCandidates) ? paper.pdfCandidates : [];
    const hasPdfList = pdfCandidates.length > 0;
    const hasManyPdfs = pdfCandidates.length > 1;

    const root = document.createElement("div");
    root.id = "papersolver-capture-root";
    root.innerHTML = `
      <div class="ps-card ps-quiet ${hasPdfList ? "ps-card-list" : ""}">
        <button type="button" class="ps-close" aria-label="关闭 PaperSolver Capture">×</button>
        <div class="ps-mark" aria-hidden="true"><img src="${chrome.runtime.getURL("icon-active-128.png")}" alt="" /></div>
        <div class="ps-main">
          <strong>${hasPdfList ? `识别到 ${pdfCandidates.length} 篇可导入文献` : "可导入 PaperSolver"}</strong>
          <span>${escapeHtml(paper.title || "当前论文页面")}</span>
          <small>${escapeHtml(importHint(paper))}</small>
          <em class="ps-status" hidden></em>
        </div>
        ${hasPdfList ? `
          <div class="ps-candidates">
            ${pdfCandidates.map((item, index) => `
              <label class="ps-candidate" data-pdf-index="${index}">
                <input type="checkbox" class="ps-candidate-check" data-pdf-index="${index}" ${index === 0 ? "checked" : ""} />
                <span>
                  <b>${escapeHtml(item.label || `PDF ${index + 1}`)}</b>
                  <small>${escapeHtml(item.reason || hostLabel(item.url))}</small>
                </span>
              </label>
            `).join("")}
          </div>
        ` : ""}
        <div class="ps-actions">
          ${hasPdfList ? `
            <button type="button" class="ps-select-all">${hasManyPdfs ? "全选" : "选中"}</button>
            <button type="button" class="ps-import">下载并导入客户端</button>
          ` : `<button type="button" class="ps-import">导入题录</button>`}
        </div>
      </div>
    `;
    document.documentElement.appendChild(root);
    root.querySelector(".ps-close").addEventListener("click", () => root.remove());
    root.querySelector(".ps-import").addEventListener("click", async () => {
      const checked = Array.from(root.querySelectorAll(".ps-candidate-check:checked"))
        .map((input) => Number(input.dataset.pdfIndex))
        .filter((index) => Number.isInteger(index) && pdfCandidates[index]);
      if (hasPdfList && checked.length === 0) {
        const status = root.querySelector(".ps-status");
        status.hidden = false;
        status.textContent = "请先勾选要保存到客户端的 PDF。";
        return;
      }
      if (checked.length > 1) {
        await importSelectedPdfs(root, paper, checked.map((index) => pdfCandidates[index]));
        return;
      }
      await importPaperFromPrompt(root, buildCandidatePaper(paper, pdfCandidates[checked[0]]), null, { closeOnSuccess: true });
    });
    root.querySelector(".ps-select-all")?.addEventListener("click", () => {
      const checks = Array.from(root.querySelectorAll(".ps-candidate-check"));
      const shouldCheck = checks.some((input) => !input.checked);
      checks.forEach((input) => { input.checked = shouldCheck; });
    });
  }

  async function importSelectedPdfs(root, paper, candidates) {
    const status = root.querySelector(".ps-status");
    const button = root.querySelector(".ps-import");
    root.querySelectorAll(".ps-import, .ps-select-all, .ps-candidate-check").forEach((node) => { node.disabled = true; });
    status.hidden = false;
    let success = 0;
    for (let index = 0; index < candidates.length; index += 1) {
      status.textContent = `正在保存第 ${index + 1}/${candidates.length} 篇到客户端...`;
      const result = await importPaperViaMessage(buildCandidatePaper(paper, candidates[index]), status);
      if (result?.ok) success += 1;
    }
    root.classList.add(success ? "ps-success" : "ps-error");
    button.textContent = success ? "已保存" : "重试";
    status.textContent = success
      ? `已保存 ${success}/${candidates.length} 篇到客户端。`
      : "保存失败：请确认 PaperSolver 桌面端已打开、已登录，并已配置 PDF 保存目录。";
    if (success === candidates.length) setTimeout(() => root.remove(), 1800);
    root.querySelectorAll(".ps-import, .ps-select-all, .ps-candidate-check").forEach((node) => { node.disabled = false; });
  }

  async function importPaperViaMessage(paper, status) {
    const payload = await attachPdfDataUrlIfPossible(paper, status);
    return new Promise((resolve) => {
      chrome.runtime.sendMessage({ type: "PAPERSOLVER_IMPORT", payload }, (response) => {
        resolve(response || { ok: false, error: chrome.runtime.lastError?.message || "" });
      });
    });
  }

  async function importPaperFromPrompt(root, paper, sourceButton, options = {}) {
      const button = root.querySelector(".ps-import");
      const status = root.querySelector(".ps-status");
      const activeButton = sourceButton || button;
      root.querySelectorAll(".ps-import, .ps-select-all, .ps-candidate-check").forEach((node) => { node.disabled = true; });
      activeButton.textContent = sourceButton ? "导入中..." : "导入中...";
      status.hidden = false;
      status.textContent = paper.pdfUrl ? "正在读取官网 PDF..." : "正在导入元数据...";
      const payload = await attachPdfDataUrlIfPossible(paper, status);
      let answered = false;
      const messageTimer = setTimeout(() => {
        if (answered) return;
        answered = true;
        root.querySelectorAll(".ps-import, .ps-select-all, .ps-candidate-check").forEach((node) => { node.disabled = false; });
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
            status.textContent = "已保存到文献库，PDF 已保存到客户端";
            if (options.closeOnSuccess !== false) setTimeout(() => root.remove(), 1800);
          } else if (response?.result?.pdfCapturePending && response?.result?.pdfUrl) {
            button.disabled = false;
            button.textContent = "重试下载";
            status.textContent = response?.result?.pdfUploadError
              ? `PDF 下载到客户端失败：${response.result.pdfUploadError}`
              : "题录已入库，但 PDF 下载到客户端失败。请确认桌面端已打开。";
          } else {
            button.textContent = "已导入";
            status.textContent = "题录已保存到文献库；当前页面没有提供可读取的 PDF 链接。";
          }
        } else {
          root.querySelectorAll(".ps-import, .ps-select-all, .ps-candidate-check").forEach((node) => { node.disabled = false; });
          button.textContent = "重试导入";
          root.classList.add("ps-error");
          status.hidden = false;
          status.textContent = response?.error || runtimeError || "导入失败，请确认 PaperSolver 后端已启动";
        }
      });
  }

  function buildCandidatePaper(paper, candidate) {
    if (!candidate?.url) return paper;
    const suffix = candidate.label && candidate.label !== "PDF 全文" ? ` - ${candidate.label}` : "";
    return {
      ...paper,
      pdfUrl: candidate.url,
      pdfLikely: Boolean(candidate.probable),
      paperId: paper.doi || paper.paperId || "",
      title: candidate.title || candidate.label || paper.title || "未命名论文",
      sourceUrl: candidate.sourceUrl || paper.sourceUrl || location.href,
      authors: candidate.authors || paper.authors || "",
      year: candidate.year || paper.year || "",
      importSource: `${paper.importSource || paper.source || hostLabel(location.href)}${suffix}`,
      source: paper.source || hostLabel(location.href)
    };
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
      <div class="ps-card ps-quiet">
        <button type="button" class="ps-close" aria-label="关闭 PaperSolver Capture">×</button>
        <div class="ps-mark" aria-hidden="true">P</div>
        <div class="ps-main">
          <strong>补传 PDF 到 PaperSolver</strong>
          <span>${escapeHtml(pending.title || "当前论文")}</span>
          <small>当前是 PDF 页，将补传到刚才导入的文献</small>
          <em class="ps-status" hidden></em>
        </div>
        <div class="ps-actions">
          <button type="button" class="ps-import">补传 PDF</button>
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
    if (!pdfUrl || (!isPdfUrl(pdfUrl) && !paper?.pdfLikely)) return paper;
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
      status.textContent = "PDF 已读取，正在保存到桌面端...";
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
    const count = Array.isArray(paper?.pdfCandidates) ? paper.pdfCandidates.length : 0;
    if (count > 1) return `当前页面发现 ${count} 个 PDF 下载入口，可选择具体文件导入`;
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

  function collectPaperCandidates(context = {}) {
    const candidates = [];
    const seen = new Set();
    const scienceDirectList = collectScienceDirectListCandidates();
    if (scienceDirectList.length >= 2) return scienceDirectList;

    const pdfAnchors = Array.from(document.querySelectorAll("a[href]")).filter((anchor) => isPdfDownloadAnchor(anchor));
    for (const pdfLink of pdfAnchors) {
      const container = bestResultContainer(pdfLink);
      const pdfUrl = normalizePdfImportUrl(pdfLink.href);
      if (!container || !pdfUrl || seen.has(pdfUrl)) continue;
      const title = extractPaperTitleFromContainer(container, pdfLink);
      if (!title || title.length < 8) continue;
      seen.add(pdfUrl);
      candidates.push({
        url: pdfUrl,
        label: title,
        title,
        reason: "PDF 全文",
        sourceUrl: normalizeUrl(findBestDetailUrl(container, pdfLink) || pdfLink.href || location.href),
        authors: extractAuthorsFromContainer(container),
        year: extractYear(container.textContent || ""),
        priority: 5
      });
      if (candidates.length >= 30) break;
    }
    if (candidates.length >= 2) return candidates.slice(0, 30);

    const containers = Array.from(document.querySelectorAll([
      "article",
      "li",
      "tr",
      ".gs_r",
      ".result",
      ".search-result",
      ".result-item",
      ".paper",
      ".paper-item",
      ".publication",
      ".publication-item",
      "[data-testid*='result']",
      "[class*='result']",
      "[class*='paper']",
      "[class*='article']"
    ].join(",")));

    for (const container of containers) {
      if (!(container instanceof Element)) continue;
      const pdfLink = findPdfAnchorIn(container);
      if (!pdfLink) continue;
      const pdfUrl = normalizePdfImportUrl(pdfLink.href);
      if (!pdfUrl || seen.has(pdfUrl)) continue;
      const title = extractPaperTitleFromContainer(container, pdfLink);
      if (!title || title.length < 8) continue;
      seen.add(pdfUrl);
      candidates.push({
        url: pdfUrl,
        label: title,
        title,
        reason: "PDF 全文",
        sourceUrl: normalizeUrl(findBestDetailUrl(container, pdfLink) || location.href),
        authors: extractAuthorsFromContainer(container),
        year: extractYear(container.textContent || ""),
        priority: 8
      });
      if (candidates.length >= 30) break;
    }

    if (candidates.length) return candidates.slice(0, 30);
    return collectPdfCandidates(context)
      .filter((item) => !item.probable)
      .map((item) => ({
        ...item,
        title: context.title || item.label,
        label: context.title || item.label,
        sourceUrl: location.href
      }));
  }

  function collectScienceDirectListCandidates() {
    if (!/(^|\.)sciencedirect\.com$/i.test(location.hostname)) return [];
    const seen = new Set();
    const candidates = [];
    const articleLinks = Array.from(document.querySelectorAll("a[href*='/science/article/pii/']"));

    for (const link of articleLinks) {
      const pii = extractScienceDirectPii(link.href);
      if (!pii || seen.has(pii)) continue;
      const container = bestScienceDirectResultContainer(link);
      const title = extractScienceDirectListTitle(container, link);
      if (!title) continue;
      const sourceUrl = `https://www.sciencedirect.com/science/article/pii/${pii}`;
      seen.add(pii);
      candidates.push({
        url: `${sourceUrl}/pdfft`,
        label: title,
        title,
        reason: "ScienceDirect 搜索结果",
        sourceUrl,
        authors: container ? extractAuthorsFromContainer(container) : "",
        year: container ? extractYear(container.textContent || "") : "",
        priority: 1
      });
      if (candidates.length >= 50) break;
    }

    return candidates.slice(0, 50);
  }

  function bestScienceDirectResultContainer(anchor) {
    const preferred = anchor.closest("li, article, [data-testid*='result'], [class*='result'], [class*='SearchResult'], [class*='ResultItem']");
    if (preferred) return preferred;
    let current = anchor;
    for (let depth = 0; current && depth < 7; depth += 1) {
      current = current.parentElement;
      if (!current) break;
      const text = clean(current.textContent || "");
      const piiLinks = new Set(Array.from(current.querySelectorAll("a[href*='/science/article/pii/']")).map((node) => extractScienceDirectPii(node.href)).filter(Boolean));
      if (text.length > 80 && text.length < 1800 && piiLinks.size <= 2) return current;
    }
    return anchor.parentElement;
  }

  function extractScienceDirectListTitle(container, link) {
    const linkText = clean(link.textContent || "");
    if (looksLikePaperTitle(linkText) && !/pdf|download|view/i.test(linkText)) return linkText;
    const scope = container || document;
    const titleNodes = Array.from(scope.querySelectorAll([
      "h2 a[href*='/science/article/pii/']",
      "h3 a[href*='/science/article/pii/']",
      "h4 a[href*='/science/article/pii/']",
      "a.anchor",
      "a[class*='title']",
      "a[href*='/science/article/pii/']"
    ].join(",")));
    const titles = titleNodes
      .map((node) => clean(node.textContent || ""))
      .filter((text) => looksLikePaperTitle(text) && !/^(view|download)\s+pdf$/i.test(text))
      .sort((a, b) => b.length - a.length);
    return titles[0] || "";
  }

  function findPdfAnchorIn(container) {
    const anchors = Array.from(container.querySelectorAll("a[href]"));
    return anchors.find(isPdfDownloadAnchor);
  }

  function isPdfDownloadAnchor(anchor) {
    const text = clean(anchor?.textContent || anchor?.getAttribute?.("aria-label") || anchor?.getAttribute?.("title") || "");
    const href = String(anchor?.href || "");
    if (!href || isSupplementaryPdf(anchor)) return false;
    if (isPdfUrl(href)) return true;
    if (isScienceDirectArticleUrl(href) && /pdf/i.test(text)) return true;
    return /(pdf|download\s*pdf|article\s*pdf|view\s*pdf|下载全文|全文下载|PDF下载|下载PDF)/i.test(text)
      && /(pdf|download|content|full|doi|article|pii|pdfdirect|pdfft|viewcontent)/i.test(href);
  }

  function bestResultContainer(anchor) {
    let current = anchor;
    for (let depth = 0; current && depth < 8; depth += 1) {
      current = current.parentElement;
      if (!current) break;
      const text = clean(current.textContent || "");
      const titleCount = Array.from(current.querySelectorAll("h1,h2,h3,h4,a")).filter((node) => looksLikePaperTitle(node.textContent || "")).length;
      const hasOnlyOnePdf = current.querySelectorAll("a[href]").length <= 12 || current.querySelectorAll("a[href]").length / Math.max(1, titleCount) <= 6;
      if (text.length > 80 && text.length < 2400 && titleCount >= 1 && hasOnlyOnePdf) return current;
      if (/^(article|li|tr)$/i.test(current.tagName) && text.length > 40) return current;
    }
    return anchor.closest("article, li, tr, section, div") || anchor.parentElement;
  }

  function normalizePdfImportUrl(url) {
    const normalized = normalizeUrl(url);
    if (!normalized) return "";
    if (isScienceDirectArticleUrl(normalized)) {
      return normalized.replace(/\/science\/article\/pii\/([^/?#]+).*$/i, "/science/article/pii/$1/pdfft");
    }
    return normalized;
  }

  function isScienceDirectArticleUrl(url) {
    try {
      const parsed = new URL(url, location.href);
      return /(^|\.)sciencedirect\.com$/i.test(parsed.hostname) && /\/science\/article\/pii\/[^/?#]+/i.test(parsed.pathname);
    } catch {
      return false;
    }
  }

  function extractPaperTitleFromContainer(container, pdfLink) {
    const selectors = [
      'meta[name="citation_title"]',
      "h1",
      "h2",
      "h3",
      "h4",
      ".title",
      "[class*='title']",
      "[data-testid*='title']",
      "a[href*='/doi/']",
      "a[href*='/abs/']",
      "a[href*='/article/']",
      "a[href*='/document/']"
    ];
    for (const selector of selectors) {
      const node = container.querySelector(selector);
      const text = clean(node?.content || node?.textContent || "");
      if (looksLikePaperTitle(text)) return text;
    }
    const anchorTexts = Array.from(container.querySelectorAll("a[href]"))
      .map((anchor) => clean(anchor.textContent || ""))
      .filter(looksLikePaperTitle)
      .sort((a, b) => b.length - a.length);
    if (anchorTexts[0]) return anchorTexts[0];
    const lines = clean(container.textContent || "")
      .split(/(?<=[。.!?])\s+|\n+/)
      .map(clean)
      .filter(looksLikePaperTitle)
      .sort((a, b) => b.length - a.length);
    return lines[0] || clean(pdfLink.textContent || contextTitleFallback());
  }

  function looksLikePaperTitle(text) {
    const value = clean(text);
    if (value.length < 8 || value.length > 260) return false;
    if (/^(pdf|download|view|full text|article|abstract|摘要|下载|全文|引用|cited by)$/i.test(value)) return false;
    return /[A-Za-z\u4e00-\u9fa5]/.test(value);
  }

  function findBestDetailUrl(container, pdfLink) {
    const anchors = Array.from(container.querySelectorAll("a[href]"));
    const detail = anchors.find((anchor) => {
      const href = String(anchor.href || "");
      if (href === pdfLink.href || isPdfUrl(href)) return false;
      return /\/(doi|abs|article|articles|document|paper|publication|pubmed)\//i.test(href)
        || /^https:\/\/doi\.org\//i.test(href);
    });
    return detail?.href || "";
  }

  function extractAuthorsFromContainer(container) {
    const node = container.querySelector("[class*='author'], [data-testid*='author'], .gs_a");
    return clean(node?.textContent || "").slice(0, 300);
  }

  function extractYear(text) {
    return clean((String(text || "").match(/\b(19|20)\d{2}\b/) || [])[0] || "");
  }

  function contextTitleFallback() {
    return cleanupTitle(document.title) || "未命名论文";
  }

  function collectPdfCandidates(context = {}) {
    const candidates = [];
    const add = (url, label, reason, priority = 20, probable = false) => {
      const normalized = normalizePdfImportUrl(url);
      if (!normalized || (!isPdfUrl(normalized) && !probable)) return;
      if (isSupplementaryPdfText(`${label} ${normalized}`)) return;
      const existing = candidates.find((item) => item.url === normalized);
      if (existing) {
        existing.priority = Math.min(existing.priority, priority);
        existing.label = existing.label || clean(label);
        existing.reason = existing.reason || reason;
        return;
      }
      candidates.push({
        url: normalized,
        label: clean(label) || filenameFromUrl(normalized) || "PDF 全文",
        reason: probable ? "PDF 全文" : (clean(reason) || "PDF 全文"),
        priority,
        probable: Boolean(probable)
      });
    };

    add(currentPdfDocumentUrl(), "当前 PDF", "当前标签页", 1);
    add(firstMeta(["citation_pdf_url", "pdf_url", "bepress_citation_pdf_url"]), "官网 PDF", "页面元数据", 2);
    add(clean(context.jsonLd?.encoding?.contentUrl || context.jsonLd?.associatedMedia?.contentUrl), "结构化 PDF", "JSON-LD", 3);
    add(inferKnownPublisherPdfUrl(location.href, context.doi, context.pii), "推断 PDF", "出版商规则", 4);

    document.querySelectorAll('link[href][type="application/pdf"], link[rel="alternate"][href*=".pdf"]').forEach((node) => {
      add(node.href, node.title || context.title || "PDF 全文", "页面 link 标签", 5);
    });

    document.querySelectorAll('iframe[src], embed[src], object[data]').forEach((node) => {
      add(node.src || node.data || node.getAttribute("src") || node.getAttribute("data"), node.title || "嵌入 PDF", "页面嵌入文件", 6);
    });

    Array.from(document.querySelectorAll("a[href]")).forEach((anchor, index) => {
      const href = anchor.href;
      const text = clean(anchor.textContent || anchor.getAttribute("aria-label") || anchor.getAttribute("title") || "");
      const around = clean(anchor.closest("article, li, tr, section, div")?.textContent || "");
      const label = text || around.slice(0, 80) || filenameFromUrl(href) || `PDF ${index + 1}`;
      if (isSupplementaryPdf(anchor)) return;
      if (isPdfUrl(href)) {
        add(href, label, "页面下载链接", 10);
        return;
      }
      if (/(pdf|full\s*text|download\s*pdf|article\s*pdf|view\s*pdf|下载全文|全文下载|PDF下载|下载PDF|Download)/i.test(text)
        && /(pdf|download|article|content|full|doi|pdfdirect|pdfft|viewcontent)/i.test(href)) {
        add(href, label, "PDF 全文", 12, true);
      }
    });

    return candidates
      .sort((a, b) => a.priority - b.priority || a.label.localeCompare(b.label))
      .slice(0, 12);
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
    return isSupplementaryPdfText(text + " " + href);
  }

  function isSupplementaryPdfText(text) {
    return /(supplement|supporting|appendix|附件|补充材料|附录)/i.test(text || "");
  }

  function shouldOfferCapture(paper) {
    if (!paper || isPaperSolverAppHost()) return false;
    const pdfCount = Array.isArray(paper.pdfCandidates) ? paper.pdfCandidates.length : 0;
    if (pdfCount >= 2) return true;
    if (pdfCount > 0 && (isAcademicHost() || hasCitationMeta() || isCurrentPdfDocument())) return true;
    if (shouldSilenceForThisUrl()) return false;
    const title = clean(paper.title);
    const academicHost = isAcademicHost();
    const hasUsefulTitle = title.length >= 18 && !/^(home|search|login|sign in|settings|dashboard|results|文献搜索|学术搜索)$/i.test(title);
    const obviousArticlePath = /\/(science\/article\/pii|article|articles|document|abs|paper|pubmed|doi|content\/pdf)\//i.test(location.pathname)
      || location.search.includes("arnumber=");
    let score = 0;
    if (academicHost) score += 1;
    if (hasCitationMeta()) score += 3;
    if (paper.doi && (/^10\.\d{4,9}\//i.test(paper.doi) || /^S[A-Z0-9]{15,30}$/i.test(paper.doi))) score += 3;
    if (paper.pdfUrl || isCurrentPdfDocument()) score += 2;
    if (paper.abstractText) score += 1;
    if (hasUsefulTitle) score += 1;
    if (obviousArticlePath) score += 1;
    return score >= 4 && (academicHost || hasCitationMeta() || isCurrentPdfDocument());
  }

  function shouldSilenceForThisUrl() {
    if (isCurrentPdfDocument() || hasCitationMeta()) return false;
    return /\/(login|signin|signup|account|settings|dashboard|admin|search|results?|home|profile)\b/i.test(location.pathname);
  }

  function isAcademicHost() {
    return /doi\.org|sciencedirect|sciencedirectassets|semanticscholar|pubmed|ncbi\.nlm\.nih|webofscience|cnki|wanfang|researchrabbit|connectedpapers|scholar\.google|arxiv|aclanthology|springer|nature|ieeexplore|ieee|dl\.acm/i
      .test(location.hostname + location.pathname);
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
