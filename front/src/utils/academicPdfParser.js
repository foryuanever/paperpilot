/**
 * Academic PDF Metadata Extraction Engine
 * Specially engineered for real-world scientific & academic papers (ACS, IEEE, Nature, Elsevier, Springer, Wiley, arXiv, MDPI, etc.)
 * Resolves drop-cap size anomalies, multi-line titles, and affiliation/footnote author pollution.
 */

export async function extractAcademicPdfMetadata(fileOrBuffer) {
  const [pdfjs, workerModule] = await Promise.all([
    import("pdfjs-dist"),
    import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
  ]);
  pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;

  const data = fileOrBuffer instanceof ArrayBuffer
    ? fileOrBuffer
    : fileOrBuffer instanceof Uint8Array
      ? fileOrBuffer
      : await fileOrBuffer.arrayBuffer();

  const document = await pdfjs.getDocument({ data }).promise;
  try {
    return await extractMetadataFromPdfDocument(document);
  } finally {
    try {
      await document.destroy();
    } catch {}
  }
}

export async function extractMetadataFromPdfDocument(document) {
  const metadata = await document.getMetadata().catch(() => ({}));
  const info = metadata?.info || {};

  const pageCount = Math.min(2, document.numPages || 1);
  const firstPageRows = [];
  const allLines = [];

  for (let pageNumber = 1; pageNumber <= pageCount; pageNumber += 1) {
    const page = await document.getPage(pageNumber);
    const viewport = page.getViewport({ scale: 1.0 });
    const content = await page.getTextContent();
    const rows = new Map();

    for (const item of content.items || []) {
      const text = String(item.str || "").replace(/\s+/g, " ").trim();
      if (!text) continue;

      const scaleX = Math.hypot(Number(item.transform?.[0] || 0), Number(item.transform?.[1] || 0));
      const scaleY = Math.hypot(Number(item.transform?.[2] || 0), Number(item.transform?.[3] || 0));
      const fontSize = Math.round(Math.max(scaleX, scaleY, Number(item.height || 0)) * 10) / 10;
      const x = Number(item.transform?.[4] || 0);
      const y = Number(item.transform?.[5] || 0);

      // Group baselines within 3.5px
      const matchedY = [...rows.keys()].find(k => Math.abs(k - y) <= 3.5);
      const rowKey = matchedY == null ? y : matchedY;
      if (!rows.has(rowKey)) rows.set(rowKey, []);
      rows.get(rowKey).push({ x, y, text, fontSize });
    }

    // Sort rows from top of the page (highest y) to bottom
    const sortedEntries = [...rows.entries()].sort((a, b) => b[0] - a[0]);

    for (const [y, items] of sortedEntries) {
      // Sort items left-to-right
      items.sort((a, b) => a.x - b.x);
      const lineText = items.map(it => it.text).join(" ").replace(/\s+/g, " ").trim();
      if (!lineText) continue;

      // Calculate character-weighted font size to neutralize giant initial Drop-Caps
      let totalWeightedSize = 0;
      let totalChars = 0;
      for (const it of items) {
        const len = it.text.length;
        totalWeightedSize += it.fontSize * len;
        totalChars += len;
      }
      const weightedFontSize = totalChars > 0 ? (totalWeightedSize / totalChars) : (items[0]?.fontSize || 10);
      const maxFontSize = Math.max(...items.map(it => it.fontSize));

      const rowObj = {
        y,
        pageNumber,
        text: lineText,
        weightedFontSize,
        maxFontSize,
        items,
        pageHeight: viewport.height
      };

      if (pageNumber === 1) {
        firstPageRows.push(rowObj);
      }
      allLines.push(lineText);
    }
  }

  return parseAcademicMetadata(firstPageRows, allLines, info);
}

function parseAcademicMetadata(firstPageRows, allLines, info = {}) {
  // 1. Identify Candidate Title Rows
  const titleCandidateRows = [];
  const totalRows = firstPageRows.length;

  for (let i = 0; i < Math.min(25, totalRows); i++) {
    const row = firstPageRows[i];
    const text = row.text.trim();

    // Must not be noise or header
    if (isHeaderOrNoise(text)) continue;
    if (isSectionBreak(text)) break; // Reached Abstract / Introduction

    // If character-weighted size is too small (e.g. body text), skip
    if (row.weightedFontSize < 11.5 && row.maxFontSize < 12.5) continue;

    titleCandidateRows.push({ ...row, index: i });
  }

  // Find the prominent font size among candidates
  const maxWeightedSize = Math.max(0, ...titleCandidateRows.map(r => r.weightedFontSize));
  const primaryTitleRow = titleCandidateRows.find(r => r.weightedFontSize >= maxWeightedSize * 0.90);

  let title = "";
  const titleIndices = new Set();

  if (primaryTitleRow) {
    const titleRows = [primaryTitleRow];
    titleIndices.add(primaryTitleRow.index);
    const startIndex = primaryTitleRow.index;

    // Check if next rows are also part of a multi-line title
    for (let nextIdx = startIndex + 1; nextIdx < Math.min(startIndex + 4, firstPageRows.length); nextIdx++) {
      const nextRow = firstPageRows[nextIdx];
      const nextText = nextRow.text.trim();
      if (isHeaderOrNoise(nextText) || isSectionBreak(nextText) || isAffiliationOrInstitution(nextText)) break;
      if (nextRow.weightedFontSize >= maxWeightedSize * 0.78) {
        titleRows.push(nextRow);
        titleIndices.add(nextIdx);
      } else {
        break;
      }
    }

    title = titleRows.map(r => r.text).join(" ").replace(/\s+/g, " ").trim();
  }

  // Fallback or Clean Title with PDF Info Metadata
  const infoTitle = cleanInfoTitle(info.Title);
  if (infoTitle && (!title || title.length < 15 || isBodySnippet(title))) {
    title = infoTitle;
  } else if (infoTitle && title && isMatchingTitle(title, infoTitle)) {
    title = infoTitle.length >= title.length ? infoTitle : title;
  }

  // Mark all rows that overlap with title as title rows so they will never be mistaken as authors
  if (title) {
    for (let i = 0; i < Math.min(25, firstPageRows.length); i++) {
      if (isTitleOverlap(firstPageRows[i].text, title)) {
        titleIndices.add(i);
      }
    }
  }

  // 2. Identify Candidate Authors
  let authors = "";
  const maxTitleIndex = titleIndices.size > 0 ? Math.max(...titleIndices) : -1;
  const authorSearchStart = maxTitleIndex >= 0 ? maxTitleIndex + 1 : 0;
  const authorCandidateRows = [];

  for (let i = authorSearchStart; i < Math.min(authorSearchStart + 8, firstPageRows.length); i++) {
    const row = firstPageRows[i];
    const text = row.text.trim();

    if (isSectionBreak(text)) break;
    if (isHeaderOrNoise(text)) continue;
    if (isAffiliationOrInstitution(text)) continue;
    if (isTitleOverlap(text, title)) continue;

    if (isAuthorLikely(text, title)) {
      const cleaned = cleanAuthorString(text);
      if (cleaned && !isTitleOverlap(cleaned, title) && !isAffiliationOrInstitution(cleaned)) {
        authorCandidateRows.push(cleaned);
      }
    }
  }

  if (authorCandidateRows.length > 0) {
    authors = cleanAndJoinAuthors(authorCandidateRows);
  }

  // Fallback to info.Author if extracted authors are empty or invalid
  const infoAuthor = cleanAuthorString(info.Author || "");
  if (!authors && infoAuthor && !isHeaderOrNoise(infoAuthor) && !isTitleOverlap(infoAuthor, title)) {
    authors = infoAuthor;
  }

  // 3. Identify Publish Year
  let year = "";
  const citationRow = firstPageRows.find(r => /cite this:/i.test(r.text));
  if (citationRow) {
    const yearMatch = citationRow.text.match(/\b(19|20)\d{2}\b/);
    if (yearMatch) year = yearMatch[0];
  }
  if (!year) {
    const firstPageText = firstPageRows.slice(0, 15).map(r => r.text).join(" ");
    const match = firstPageText.match(/(?:(?:19|20)\d{2}(?:–\d{2,4})?|\b(19|20)\d{2}\b)/);
    if (match) year = match[0].slice(0, 4);
  }

  // 4. Identify Source / Journal
  let source = "个人文献";
  if (citationRow) {
    const srcMatch = citationRow.text.match(/cite this:\s*([^,\d]+)/i);
    if (srcMatch && srcMatch[1]) {
      source = srcMatch[1].trim();
      if (/acs sens/i.test(source)) source = "ACS Sensors";
    }
  }
  if (source === "个人文献") {
    const journalRow = firstPageRows.slice(0, 6).find(r =>
      /^(acs sensors|nature|ieee|science|cell|advanced materials|angewandte|journal of|biomaterials|talanta|pnas|nucleic acids)\b/i.test(r.text)
    );
    if (journalRow) {
      source = journalRow.text.trim();
      if (/acs sensors/i.test(source)) source = "ACS Sensors";
    }
  }

  return {
    title: sanitizeTitle(title),
    authors: sanitizeAuthors(authors),
    year: year || "",
    source: source || "个人文献"
  };
}

function isHeaderOrNoise(text) {
  const t = text.trim();
  if (!t || t.length < 2) return true;
  if (/https?:|www\.|pubs\.|doi\.org|@|\.com|\.org|\.net|\.edu|\.cn/i.test(t)) return true;
  if (/\b(read online|access\b|metrics\s*&?\s*more|article recommendations?|supporting info|toc graphic|table of contents|contents)\b/i.test(t)) return true;
  if (/^(cite this|citation|issn|isbn|e-issn|copyright|all rights reserved|licensed under|open access|creative commons|received|accepted|published|online|available online|volume\s*\d|issue\s*\d|pp\.\s*\d)/i.test(t)) return true;
  if (/^(review|article|research article|perspective|editorial|communication|short communication|letter|full paper|original article|manuscript|case report|progress report|review article|special issue)$/i.test(t)) return true;
  if (/^[\d\s,.\-—–/()\[\]#*†‡:]+$/.test(t)) return true;
  return false;
}

function isSectionBreak(text) {
  return /^(abstract|摘要|keywords?|关键词|introduction|引言|references|参考文献)\b/i.test(text.trim());
}

function isAffiliationOrInstitution(text) {
  const t = text.trim();
  if (!t) return false;
  if (/^(department of|school of|faculty of|college of|institute of|key laboratory|state key|centre for|center for|academy of|division of|laboratory of|chinese academy|ministry of|national lab|hospital|clinic|university|univ\.|campus|college)\b/i.test(t)) return true;
  if (/\b(e-mail|email|correspondence to|corresponding author|postal address|tel:|fax:)\b/i.test(t)) return true;
  if (/\b(republic of korea|south korea|pr china|p\.r\. china|united states|germany|singapore|japan|united kingdom)\b/i.test(t)) return true;
  return false;
}

function isBodySnippet(text) {
  const lower = text.toLowerCase();
  return /applications have expanded|in this study|we demonstrate|herein we report|we investigate|the proposed method/i.test(lower);
}

const COMMON_TITLE_WORDS = new Set([
  "recent", "advances", "advance", "review", "perspective", "progress",
  "therapy", "therapeutic", "imaging", "synthesis", "characterization",
  "application", "applications", "engineering", "mechanism", "mechanisms",
  "study", "studies", "investigation", "development", "evaluation",
  "performance", "detection", "enhancement", "analysis", "analyses",
  "design", "method", "methods", "methodology", "based", "using",
  "novel", "towards", "through", "role", "roles", "effects", "effect",
  "impact", "overview", "system", "systems", "approach", "approaches",
  "structure", "structures", "properties", "functional", "nanoparticles",
  "materials", "sensors", "biosensors", "sensor", "platform", "platforms",
  "technology", "technologies", "insights", "challenges", "opportunities",
  "current", "state", "art", "future", "trends", "nanotechnology",
  "machine", "learning", "deep", "neural", "network", "networks",
  "artificial", "intelligence", "generation", "generative", "model", "models"
]);

function isAuthorLikely(text, title = "") {
  const t = text.trim();
  if (!t || t.length < 3 || t.length > 300) return false;
  if (isHeaderOrNoise(t) || isSectionBreak(t) || isAffiliationOrInstitution(t)) return false;
  if (isTitleOverlap(t, title)) return false;
  if (/\b(read online|access|metrics|recommendations?|supporting|graphic|toc)\b/i.test(t)) return false;

  // Reject pure numbers/punctuation
  const letters = t.replace(/[^a-zA-Z\u4e00-\u9fa5]/g, "");
  const digits = t.replace(/[^0-9]/g, "");
  if (letters.length < 3) return false;
  if (digits.length > letters.length * 0.7) return false;

  // Check against common non-name scientific topic words
  const words = t.toLowerCase().split(/[^a-z0-9]+/);
  let titleWordCount = 0;
  for (const w of words) {
    if (COMMON_TITLE_WORDS.has(w)) titleWordCount++;
  }
  if (words.length > 0 && (titleWordCount / words.length) >= 0.4) {
    return false;
  }

  // Name patterns: capitalized names joined by commas, 'and', '*'
  const tokens = t.split(/[\s,]+/);
  const capitalizedNames = tokens.filter(w => /^[A-Z][a-z]+/.test(w) || /^[\u4e00-\u9fa5]{2,4}$/.test(w));
  return capitalizedNames.length >= 2 || (/[,，]/.test(t) && capitalizedNames.length >= 1);
}

function isTitleOverlap(text, title) {
  if (!text || !title) return false;
  const cleanT = text.toLowerCase().replace(/[^a-z0-9]/g, " ").trim();
  const cleanTitle = title.toLowerCase().replace(/[^a-z0-9]/g, " ").trim();
  if (!cleanT || !cleanTitle) return false;

  if (cleanTitle.includes(cleanT) || cleanT.includes(cleanTitle)) {
    return true;
  }

  const tWords = cleanT.split(/\s+/).filter(w => w.length > 2);
  const titleWords = new Set(cleanTitle.split(/\s+/).filter(w => w.length > 2));
  if (tWords.length === 0) return false;

  let matchCount = 0;
  for (const w of tWords) {
    if (titleWords.has(w)) matchCount++;
  }
  return (matchCount / tWords.length) >= 0.4;
}

function cleanAuthorString(raw) {
  let str = String(raw || "");
  str = str.replace(/\s+(?:and|&)\s+/gi, ", ");
  str = str.replace(/\b(access|metrics\s*&?\s*more|article recommendations?|read online|supporting info\w*|toc graphic|table of contents?)\b.*$/gi, "");
  str = str.replace(/([A-Za-z\u4e00-\u9fa5]+)\s*[*,†‡#§\d]+(?:,[*,†‡#§\d]+)*\s*(?=[A-Z\u4e00-\u9fa5])/g, "$1, ");
  str = str.replace(/[*†‡#§\d]+(?:,[*†‡#§\d]+)*/g, "");
  str = str.replace(/[*†‡#§]/g, "");
  str = str.replace(/\b(iD|orcid)\b/gi, "");
  str = str.replace(/\s*,\s*/g, ", ");
  str = str.replace(/\s+/g, " ");
  str = str.replace(/^[\s,]+|[\s,]+$/g, "");
  return str.trim();
}

function cleanAndJoinAuthors(authorRows) {
  const allNames = [];
  const seen = new Set();

  for (const row of authorRows) {
    const rawNames = row.split(/[,，、]+/).map(s => s.trim()).filter(Boolean);
    for (const name of rawNames) {
      let cleanName = name.replace(/^and\s+/i, "").replace(/^&\s+/i, "").trim();
      cleanName = cleanName.replace(/[*†‡#§\d]+(?:,[*†‡#§\d]+)*/g, "").replace(/[*†‡#§]/g, "").trim();
      if (!cleanName || cleanName.length < 2) continue;
      if (isAffiliationOrInstitution(cleanName)) continue;
      if (COMMON_TITLE_WORDS.has(cleanName.toLowerCase())) continue;
      if (/\b(access|metrics|recommendations?|supporting)\b/i.test(cleanName)) continue;

      const norm = cleanName.toLowerCase();
      if (!seen.has(norm)) {
        seen.add(norm);
        allNames.push(cleanName);
      }
    }
  }

  return allNames.join(", ");
}

function cleanInfoTitle(infoTitle) {
  if (!infoTitle || typeof infoTitle !== "string") return "";
  const t = infoTitle.replace(/\s+/g, " ").trim();
  if (t.length < 8 || /^(untitled|microsoft word|document|未命名)/i.test(t)) return "";
  if (/\.pdf$/i.test(t)) return "";
  return t;
}

function isMatchingTitle(t1, t2) {
  const norm1 = t1.toLowerCase().replace(/[^a-z0-9]/g, "");
  const norm2 = t2.toLowerCase().replace(/[^a-z0-9]/g, "");
  return norm1.includes(norm2) || norm2.includes(norm1);
}

function sanitizeTitle(title) {
  if (!title) return "未命名学术文献";
  return title.replace(/\s+/g, " ").trim().slice(0, 300);
}

function sanitizeAuthors(authors) {
  if (!authors) return "作者待补全";
  return authors.replace(/\s+/g, " ").trim().slice(0, 300);
}
