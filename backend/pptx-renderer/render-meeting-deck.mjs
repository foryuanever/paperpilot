import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import os from "node:os";

const [, , inputPath, outputPath] = process.argv;

if (!inputPath || !outputPath) {
  console.error("Usage: node render-meeting-deck.mjs <input.json> <output.pptx>");
  process.exit(2);
}

const deck = JSON.parse(fs.readFileSync(inputPath, "utf8"));
const settings = deck.pptMasterSettings || {};
const outputDir = path.dirname(path.resolve(outputPath));
const projectDir = path.join(outputDir, "ppt-master-project");
const svgDir = path.join(projectDir, "svg_output");
const notesDir = path.join(projectDir, "notes");

const W = 1280;
const H = 720;
const P = palette(settings.visualStyle);
const FONT_BODY = 'Arial, &quot;Microsoft YaHei&quot;, sans-serif';
const FONT_TITLE = 'Georgia, &quot;Microsoft YaHei&quot;, serif';

function palette(style = "academic_blue") {
  if (style === "dark_tech") {
    return {
      bg: "#0B1020",
      paper: "#111A2E",
      ink: "#EAF1FF",
      muted: "#9CA9BD",
      faint: "#1F2A44",
      line: "#33415F",
      accent: "#4CC9F0",
      accent2: "#80ED99",
      warm: "#FFD166",
    };
  }
  if (style === "journal_minimal") {
    return {
      bg: "#FAFAF8",
      paper: "#FFFFFF",
      ink: "#151923",
      muted: "#5B6472",
      faint: "#F0F2F4",
      line: "#D8DEE6",
      accent: "#2F3A4A",
      accent2: "#2C7A7B",
      warm: "#A15C22",
    };
  }
  return {
    bg: "#FFFFFF",
    paper: "#F6F8FB",
    ink: "#162033",
    muted: "#526070",
    faint: "#ECF3FF",
    line: "#DDE5F0",
    accent: "#2457D6",
    accent2: "#0F9F8B",
    warm: "#E59D18",
  };
}

function clean(value, fallback = "") {
  return String(value ?? fallback)
    .replace(/\\{1,2}rightarrow/g, "→")
    .replace(/\\{1,2}leftarrow/g, "←")
    .replace(/\\{1,2}Rightarrow/g, "⇒")
    .replace(/\brightarrow\b/gi, "→")
    .replace(/\bleftarrow\b/gi, "←")
    .replace(/\\{1,2}geq/g, "≥")
    .replace(/\\{1,2}leq/g, "≤")
    .replace(/\$+/g, "")
    .replace(/\*\*(.*?)\*\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/^\s*[-*]\s+/gm, "")
    .replace(/\s+/g, " ")
    .trim();
}

function esc(value) {
  return clean(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function list(value, limit = 6) {
  if (!Array.isArray(value)) return [];
  return value.map((item) => clean(item)).filter(Boolean).slice(0, limit);
}

function primaryPaper() {
  return deck.primaryReportPaper || {};
}

function essence() {
  return deck.researchEssence && typeof deck.researchEssence === "object" ? deck.researchEssence : {};
}

function essenceList(key, limit = 4) {
  const value = essence()[key];
  if (Array.isArray(value)) return list(value, limit);
  const text = clean(value || "");
  return text ? [text] : [];
}

function includeComparisonAppendix() {
  return deck.includeComparisonAppendix === true || settings.includeComparisonAppendix === true;
}

function isComparisonText(text) {
  const value = clean(text).toLowerCase();
  return value.includes("对比")
    || value.includes("矩阵")
    || value.includes("横向")
    || value.includes("comparison")
    || value.includes("reference")
    || value.includes("matrix")
    || value.includes("synthesis");
}

function narrativeList(value, limit = 6) {
  const lines = list(value, limit + 4);
  return (includeComparisonAppendix() ? lines : lines.filter((line) => !isComparisonText(line))).slice(0, limit);
}

function wrapText(text, maxChars = 28, maxLines = 4) {
  const value = clean(text);
  if (!value) return [];
  const cjk = /[\u3400-\u9fff]/.test(value);
  const limit = Math.max(6, Math.floor(maxChars * (cjk ? 0.68 : 1)));
  const pushChunked = (lines, token) => {
    let rest = token;
    while (rest.length > limit && lines.length < maxLines) {
      lines.push(rest.slice(0, limit));
      rest = rest.slice(limit);
    }
    return rest;
  };
  if (/\s/.test(value)) {
    const words = value.split(/\s+/).filter(Boolean);
    const lines = [];
    let current = "";
    for (const word of words) {
      if (word.length > limit) {
        if (current) {
          lines.push(current);
          current = "";
        }
        current = pushChunked(lines, word);
        if (lines.length >= maxLines) break;
        continue;
      }
      const next = current ? `${current} ${word}` : word;
      if (next.length > limit && current) {
        lines.push(current);
        current = word;
      } else {
        current = next;
      }
      if (lines.length >= maxLines) break;
    }
    if (current && lines.length < maxLines) lines.push(current);
    if (lines.length === maxLines && lines[maxLines - 1].length > limit - 2) {
      lines[maxLines - 1] = lines[maxLines - 1].slice(0, limit - 2) + "…";
    }
    return lines;
  }
  const segments = value.split(/(?<=[。；;.!?？])\s*/).filter(Boolean);
  const lines = [];
  for (const segment of segments.length ? segments : [value]) {
    let current = "";
    for (const token of segment.split(/(\s+)/).filter(Boolean)) {
      if (/^\s+$/.test(token)) continue;
      if ((current + token).length > limit && current) {
        lines.push(current);
        current = token;
      } else {
        current += token;
      }
      while (current.length > limit + 8) {
        lines.push(current.slice(0, limit));
        current = current.slice(limit);
      }
      if (lines.length >= maxLines) break;
    }
    if (current && lines.length < maxLines) lines.push(current);
    if (lines.length >= maxLines) break;
  }
  if (lines.length === maxLines && lines[maxLines - 1].length > limit - 2) {
    lines[maxLines - 1] = lines[maxLines - 1].slice(0, limit - 2) + "…";
  }
  return lines;
}

function textBlock(text, x, y, opts = {}) {
  const {
    size = 24,
    weight = 400,
    fill = P.ink,
    width = 46,
    maxLines = 4,
    lineHeight = Math.round(size * 1.35),
    family = FONT_BODY,
    anchor = "start",
    style = "",
  } = opts;
  const lines = wrapText(text, width, maxLines);
  return lines.map((line, index) => (
    `<text x="${x}" y="${y + index * lineHeight}" text-anchor="${anchor}" font-family="${family}" font-size="${size}" font-weight="${weight}" fill="${fill}"${style ? ` ${style}` : ""}>${esc(line)}</text>`
  )).join("\n");
}

function oneLine(text, x, y, opts = {}) {
  const {
    size = 18,
    weight = 400,
    fill = P.ink,
    family = FONT_BODY,
    anchor = "start",
    style = "",
  } = opts;
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" font-family="${family}" font-size="${size}" font-weight="${weight}" fill="${fill}"${style ? ` ${style}` : ""}>${esc(text)}</text>`;
}

function bulletList(items, x, y, opts = {}) {
  const lines = list(items, opts.limit || 5);
  const size = opts.size || 22;
  const gap = opts.gap || 64;
  return lines.map((item, index) => {
    const top = y + index * gap;
    return `
      <circle cx="${x}" cy="${top - 7}" r="4" fill="${opts.dot || P.accent}"/>
      ${textBlock(item, x + 24, top, { size, fill: opts.fill || P.ink, width: opts.width || 44, maxLines: opts.maxLines || 2, lineHeight: Math.round(size * 1.32) })}
    `;
  }).join("\n");
}

function splitLead(text) {
  const value = clean(text);
  const parts = value.split(/[：:]/);
  if (parts.length > 1 && parts[0].length <= 14) {
    return { label: parts.shift(), rest: parts.join("：") };
  }
  return { label: "", rest: value };
}

function softGrid() {
  return `
    <g id="soft-grid">
      ${Array.from({ length: 9 }, (_, i) => `<line x1="${90 + i * 120}" y1="126" x2="${90 + i * 120}" y2="626" stroke="${P.line}" stroke-width="0.7" opacity="0.45"/>`).join("\n")}
      ${Array.from({ length: 5 }, (_, i) => `<line x1="72" y1="${168 + i * 96}" x2="1168" y2="${168 + i * 96}" stroke="${P.line}" stroke-width="0.7" opacity="0.45"/>`).join("\n")}
    </g>
  `;
}

function arrow(x1, y1, x2, y2, color = P.accent) {
  const dx = x2 - x1;
  const dy = y2 - y1;
  const angle = Math.atan2(dy, dx);
  const size = 9;
  const ax1 = x2 - size * Math.cos(angle - Math.PI / 6);
  const ay1 = y2 - size * Math.sin(angle - Math.PI / 6);
  const ax2 = x2 - size * Math.cos(angle + Math.PI / 6);
  const ay2 = y2 - size * Math.sin(angle + Math.PI / 6);
  return `
    <line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${color}" stroke-width="2"/>
    <path d="M ${x2} ${y2} L ${ax1} ${ay1} L ${ax2} ${ay2} Z" fill="${color}"/>
  `;
}

function nodeBox(x, y, w, h, title, body, color = P.accent, index = "") {
  return `
    <rect x="${x}" y="${y}" width="${w}" height="${h}" rx="0" fill="${P.bg}" stroke="${P.line}" stroke-width="1.2"/>
    <rect x="${x}" y="${y}" width="${w}" height="7" fill="${color}"/>
    ${index ? oneLine(index, x + 18, y + 38, { size: 13, weight: 700, fill: color }) : ""}
    ${textBlock(title, x + 18, y + 67, { size: 21, weight: 700, fill: P.ink, width: 11, maxLines: 2, lineHeight: 27 })}
    ${textBlock(body, x + 18, y + 128, { size: 14, fill: P.muted, width: 19, maxLines: 3, lineHeight: 19 })}
  `;
}

function keywordLayout(item) {
  const declared = clean(item.visualType || "").toLowerCase();
  const section = clean(item.section || "").toLowerCase();
  if (declared.includes("formula") || declared.includes("method") || declared.includes("pipeline") || declared.includes("flow") || section.includes("method")) return "method";
  if (declared.includes("figure") || declared.includes("table") || declared.includes("evidence") || declared.includes("result") || section.includes("experiment") || section.includes("result")) return "evidence";
  if (declared.includes("background") || declared.includes("problem") || declared.includes("storyline") || section.includes("background")) return "background";
  if (declared.includes("outlook") || declared.includes("conclusion") || declared.includes("contribution") || declared.includes("discussion") || declared.includes("limit") || section.includes("conclusion") || section.includes("outlook")) return "discussionish";
  const text = clean(`${item.eyebrow || ""} ${item.title || ""} ${item.subtitle || ""}`).toLowerCase();
  if (/method|framework|路径|框架|模型|变量|机制|theory/.test(text)) return "method";
  if (/experiment|data|evidence|results|findings|实验|数据|证据|结果|发现/.test(text)) return "evidence";
  if (/background|introduction|背景|问题|动机/.test(text)) return "background";
  if (/contribution|limitation|discussion|贡献|局限|启示|讨论/.test(text)) return "discussionish";
  return "default";
}

function slideFrame(slideNo, eyebrow, title, subtitle, body, opts = {}) {
  const band = opts.band === false ? "" : `<rect x="${opts.bandX || 1032}" y="0" width="${opts.bandW || 248}" height="${H}" fill="${opts.bandFill || P.faint}" opacity="${opts.bandOpacity || 0.7}"/>`;
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${P.bg}"/>
  ${band}
  ${opts.grid ? softGrid() : ""}
  <line x1="72" y1="88" x2="1168" y2="88" stroke="${P.line}" stroke-width="1"/>
  ${oneLine(eyebrow, 72, 70, { size: 13, weight: 700, fill: P.accent, style: 'letter-spacing="1.8"' })}
  ${textBlock(title, 72, 142, { size: opts.titleSize || 38, weight: 700, fill: P.ink, width: opts.titleWidth || 26, maxLines: 2, lineHeight: 46, family: FONT_TITLE })}
  ${subtitle ? textBlock(subtitle, 72, 230, { size: 18, fill: P.muted, width: 42, maxLines: 2, lineHeight: 26 }) : ""}
  ${body}
  ${oneLine("PaperPilot · PPT Master SVG export", 72, 676, { size: 12, fill: "#98A2B3" })}
  ${oneLine(String(slideNo).padStart(2, "0"), 1186, 676, { size: 16, weight: 700, fill: P.accent, anchor: "end" })}
</svg>`;
}

function shell(slideNo, eyebrow, title, subtitle, body) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <defs>
    <linearGradient id="softAccent" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${P.accent}" stop-opacity="0.16"/>
      <stop offset="100%" stop-color="${P.accent2}" stop-opacity="0.04"/>
    </linearGradient>
  </defs>
  <rect x="0" y="0" width="${W}" height="${H}" fill="${P.bg}"/>
  <rect x="980" y="0" width="300" height="${H}" fill="url(#softAccent)"/>
  <line x1="72" y1="88" x2="1168" y2="88" stroke="${P.line}" stroke-width="1"/>
  ${oneLine(eyebrow, 72, 70, { size: 13, weight: 700, fill: P.accent, style: 'letter-spacing="1.8"' })}
  ${textBlock(title, 72, 142, { size: 38, weight: 700, fill: P.ink, width: 24, maxLines: 2, lineHeight: 46, family: FONT_TITLE })}
  ${subtitle ? textBlock(subtitle, 72, 230, { size: 18, fill: P.muted, width: 44, maxLines: 2, lineHeight: 26 }) : ""}
  ${body}
  ${oneLine("PaperPilot · PPT Master SVG export", 72, 676, { size: 12, fill: "#98A2B3" })}
  ${oneLine(String(slideNo).padStart(2, "0"), 1186, 676, { size: 16, weight: 700, fill: P.accent, anchor: "end" })}
</svg>`;
}

function scheme2CoverSvg() {
  const primary = primaryPaper();
  const title = clean(deck.title || primary.shortTitle || primary.title || "组会论文汇报");
  const mainTitle = title.replace(/^组会汇报[：:]\s*/, "");
  const takeaways = narrativeList(deck.takeaways, 4);
  const lines = takeaways.length ? takeaways : ["研究背景与核心问题", "方法框架与证据链", "主要结论、贡献和局限", "组会讨论问题"];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <defs>
    <linearGradient id="coverBg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="${P.bg}"/>
      <stop offset="58%" stop-color="${P.bg}"/>
      <stop offset="100%" stop-color="${P.faint}"/>
    </linearGradient>
  </defs>
  <rect x="0" y="0" width="${W}" height="${H}" fill="url(#coverBg)"/>
  <path d="M 850 -20 C 1020 80 1090 210 1278 238 L 1278 720 L 906 720 C 984 554 986 370 850 -20 Z" fill="${P.faint}"/>
  <path d="M 904 0 L 1280 0 L 1280 720 L 1040 720 Z" fill="${P.accent}" opacity="0.055"/>
  <g id="cover-grid">
    ${Array.from({ length: 9 }, (_, i) => `<line x1="${890 + i * 48}" y1="70" x2="${780 + i * 48}" y2="650" stroke="${P.line}" stroke-width="0.8" opacity="0.45"/>`).join("\n")}
  </g>
  <rect x="78" y="96" width="88" height="5" fill="${P.accent}"/>
  ${oneLine("PAPER · DEEP READ", 78, 142, { size: 14, weight: 700, fill: P.accent, style: 'letter-spacing="2.2"' })}
  ${textBlock(mainTitle, 78, 250, { size: 54, weight: 700, fill: P.ink, width: 20, maxLines: 4, lineHeight: 64, family: FONT_TITLE })}
  <line x1="80" y1="560" x2="220" y2="560" stroke="${P.ink}" stroke-width="1.3"/>
  ${oneLine(clean(primary.title || primary.fileName || "上传主论文"), 80, 596, { size: 19, fill: P.muted, family: FONT_TITLE, style: 'font-style="italic"' })}
  <g id="cover-agenda">
    <rect x="822" y="198" width="314" height="336" rx="0" fill="${P.bg}" opacity="0.86" stroke="${P.line}"/>
    <rect x="822" y="198" width="7" height="336" fill="${P.accent}"/>
    ${oneLine("本次汇报主线", 866, 254, { size: 18, weight: 700, fill: P.ink })}
    ${lines.map((item, i) => `
      ${oneLine(String(i + 1).padStart(2, "0"), 866, 306 + i * 48, { size: 13, weight: 700, fill: P.accent })}
      ${textBlock(item, 906, 306 + i * 48, { size: 15, fill: P.ink, width: 16, maxLines: 1 })}
    `).join("\n")}
  </g>
  ${oneLine(clean(deck.generatedAt || new Date().toLocaleString("zh-CN")), 80, 666, { size: 12, fill: "#98A2B3" })}
  ${oneLine("01", 1198, 666, { size: 16, weight: 700, fill: P.accent, anchor: "end" })}
</svg>`;
}

function scheme2AgendaSvg(slideNo) {
  const agenda = narrativeList(deck.agenda, 7);
  const items = agenda.length ? agenda : ["研究背景与问题", "方法框架", "实验与证据", "主要结论", "贡献局限", "组会讨论"];
  const body = `
    <g id="agenda-line">
      <rect x="84" y="286" width="1040" height="140" fill="${P.paper}" stroke="${P.line}"/>
      <line x1="122" y1="356" x2="1040" y2="356" stroke="${P.line}" stroke-width="1.5"/>
      ${items.map((item, i) => {
        const x = 130 + i * Math.min(155, 910 / Math.max(items.length - 1, 1));
        return `
          <circle cx="${x}" cy="356" r="12" fill="${i === 0 ? P.accent : P.bg}" stroke="${P.accent}" stroke-width="3"/>
          ${oneLine(String(i + 1).padStart(2, "0"), x, 324, { size: 13, weight: 700, fill: P.accent, anchor: "middle" })}
          ${textBlock(item, x - 48, 464, { size: 18, weight: 700, fill: P.ink, width: 7, maxLines: 3, lineHeight: 26 })}
        `;
      }).join("\n")}
    </g>
  `;
  return shell(slideNo, "REPORT FLOW", "汇报结构", "围绕主论文的研究问题、方法、证据和讨论展开。", body);
}

function scheme2ContentSvg(item, slideNo, variant = 0) {
  const layout = keywordLayout(item);
  if (layout === "method") return scheme2MethodSvg(item, slideNo, variant);
  if (layout === "evidence") return scheme2EvidenceSvg(item, slideNo, variant);
  if (layout === "background") return scheme2BackgroundSvg(item, slideNo, variant);
  if (layout === "discussionish") return scheme2SynthesisSvg(item, slideNo, variant);
  const bullets = list(item.bullets, 5);
  const quote = clean(item.keyMessage || item.note || "");
  const accent = [P.accent, P.accent2, P.warm][variant % 3];
  const body = `
    <g id="editorial-column">
      <rect x="92" y="292" width="580" height="282" fill="${P.paper}" opacity="0.75"/>
      ${bulletList(bullets, 128, 350, { size: 23, width: 33, gap: 66, dot: accent, limit: 4 })}
    </g>
    ${quote ? `
    <g id="takeaway">
      <circle cx="954" cy="356" r="118" fill="${P.faint}"/>
      <rect x="790" y="245" width="348" height="265" rx="0" fill="${P.bg}" stroke="${P.line}"/>
      <line x1="824" y1="304" x2="900" y2="304" stroke="${accent}" stroke-width="4"/>
      ${oneLine("KEY MESSAGE", 824, 284, { size: 13, weight: 700, fill: accent, style: 'letter-spacing="1.6"' })}
      ${textBlock(quote, 824, 360, { size: 27, weight: 700, fill: P.ink, width: 15, maxLines: 4, lineHeight: 36, family: FONT_TITLE })}
    </g>` : ""}
  `;
  return slideFrame(slideNo, clean(item.eyebrow || "PAPER READING").toUpperCase(), clean(item.title || "结构化内容"), clean(item.subtitle || ""), body, { bandX: 998, bandW: 282 });
}

function scheme2BackgroundSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 4);
  const quote = clean(item.keyMessage || "");
  const body = `
    <g id="context-map">
      <rect x="72" y="292" width="508" height="286" fill="${P.paper}" stroke="${P.line}"/>
      ${bullets.map((bullet, i) => {
        const y = 342 + i * 58;
        const pair = splitLead(bullet);
        return `
          <circle cx="116" cy="${y - 8}" r="7" fill="${[P.accent, P.accent2, P.warm, P.accent][i % 4]}"/>
          ${textBlock(pair.label || bullet, 144, y, { size: 21, weight: 700, fill: P.ink, width: 18, maxLines: 1 })}
          ${pair.label ? textBlock(pair.rest, 144, y + 28, { size: 14, fill: P.muted, width: 31, maxLines: 1 }) : ""}
        `;
      }).join("\n")}
      <g id="radar">
        <path d="M 790 340 L 920 286 L 1050 340 L 1030 500 L 810 500 Z" fill="${P.faint}" stroke="${P.accent}" stroke-width="2"/>
        <path d="M 790 340 L 920 286 L 1050 340 L 1030 500 L 810 500 Z" fill="none" stroke="${P.line}" stroke-width="1" opacity="0.7"/>
        <line x1="920" y1="286" x2="920" y2="500" stroke="${P.line}"/>
        <line x1="790" y1="340" x2="1030" y2="500" stroke="${P.line}"/>
        <line x1="1050" y1="340" x2="810" y2="500" stroke="${P.line}"/>
        ${oneLine("问题场景", 920, 262, { size: 15, weight: 700, fill: P.accent, anchor: "middle" })}
        ${oneLine("伦理风险", 742, 344, { size: 14, weight: 700, fill: P.ink })}
        ${oneLine("组织治理", 1060, 344, { size: 14, weight: 700, fill: P.ink })}
        ${oneLine("实践痛点", 776, 534, { size: 14, weight: 700, fill: P.ink })}
        ${oneLine("信任关系", 1010, 534, { size: 14, weight: 700, fill: P.ink })}
      </g>
      ${quote ? textBlock(quote, 760, 606, { size: 18, weight: 700, fill: P.ink, width: 27, maxLines: 2, lineHeight: 25 }) : ""}
    </g>
  `;
  return slideFrame(slideNo, clean(item.eyebrow || "RESEARCH BACKGROUND").toUpperCase(), clean(item.title || "研究背景"), clean(item.subtitle || ""), body, { band: false });
}

function scheme2MethodSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 5);
  const nodes = bullets.slice(0, 4).map(splitLead);
  while (nodes.length < 4) nodes.push({ label: ["输入端", "中介路径", "调节因素", "输出结果"][nodes.length], rest: "待补充" });
  const body = `
    <g id="model-flow">
      ${nodeBox(82, 318, 232, 178, nodes[0].label || "输入端", nodes[0].rest, P.accent, "A")}
      ${arrow(326, 406, 410, 406, P.accent)}
      ${nodeBox(420, 290, 252, 210, nodes[1].label || "机制路径", nodes[1].rest, P.accent2, "B")}
      ${arrow(684, 406, 766, 406, P.accent2)}
      ${nodeBox(776, 318, 232, 178, nodes[3].label || "输出结果", nodes[3].rest, P.warm, "C")}
      <rect x="456" y="540" width="360" height="66" fill="${P.paper}" stroke="${P.line}"/>
      ${oneLine(nodes[2].label || "调节变量", 482, 568, { size: 14, weight: 700, fill: P.accent })}
      ${textBlock(nodes[2].rest, 580, 568, { size: 15, fill: P.ink, width: 22, maxLines: 1 })}
      ${arrow(638, 540, 638, 505, P.warm)}
    </g>
  `;
  return slideFrame(slideNo, clean(item.eyebrow || "METHOD FRAMEWORK").toUpperCase(), clean(item.title || "方法框架"), clean(item.subtitle || ""), body, { grid: true, band: false });
}

function scheme2EvidenceSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 5);
  const body = `
    <g id="evidence-board">
      ${bullets.slice(0, 4).map((bullet, i) => {
        const pair = splitLead(bullet);
        const x = 86 + (i % 2) * 520;
        const y = 294 + Math.floor(i / 2) * 148;
        const color = [P.accent, P.accent2, P.warm, P.accent][i % 4];
        return `
          <rect x="${x}" y="${y}" width="430" height="112" fill="${P.bg}" stroke="${P.line}"/>
          <rect x="${x}" y="${y}" width="430" height="8" fill="${color}"/>
          ${oneLine(pair.label || `证据 ${i + 1}`, x + 24, y + 42, { size: 18, weight: 700, fill: color })}
          ${textBlock(pair.rest || bullet, x + 24, y + 76, { size: 17, fill: P.ink, width: 26, maxLines: 2, lineHeight: 24 })}
        `;
      }).join("\n")}
      <g id="mini-bars">
        ${[0.68, 0.82, 0.56, 0.74].map((v, i) => `
          <rect x="${910 + i * 42}" y="${590 - v * 98}" width="22" height="${v * 98}" fill="${[P.accent, P.accent2, P.warm, P.accent][i]}"/>
        `).join("\n")}
        <line x1="890" y1="590" x2="1115" y2="590" stroke="${P.line}"/>
        ${oneLine("evidence strength", 890, 626, { size: 12, fill: P.muted, style: 'letter-spacing="1.2"' })}
      </g>
    </g>
  `;
  return slideFrame(slideNo, clean(item.eyebrow || "EVIDENCE").toUpperCase(), clean(item.title || "证据链"), clean(item.subtitle || ""), body, { bandX: 1040, bandW: 240, bandFill: P.paper });
}

function scheme2SynthesisSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 4);
  const quote = clean(item.keyMessage || "");
  const body = `
    <g id="synthesis">
      <rect x="92" y="296" width="330" height="260" fill="${P.faint}" stroke="${P.line}"/>
      ${oneLine("TAKEAWAY", 124, 344, { size: 14, weight: 700, fill: P.accent, style: 'letter-spacing="1.6"' })}
      ${textBlock(quote || bullets[0] || "待补充", 124, 404, { size: 30, weight: 700, fill: P.ink, width: 13, maxLines: 4, lineHeight: 39, family: FONT_TITLE })}
      <g id="next">
        ${bullets.slice(0, 4).map((bullet, i) => {
          const y = 304 + i * 72;
          return `
            ${oneLine(String(i + 1).padStart(2, "0"), 510, y, { size: 15, weight: 700, fill: P.accent })}
            ${textBlock(bullet, 562, y, { size: 20, weight: 700, fill: P.ink, width: 31, maxLines: 2, lineHeight: 28 })}
            <line x1="510" y1="${y + 36}" x2="1030" y2="${y + 36}" stroke="${P.line}"/>
          `;
        }).join("\n")}
      </g>
    </g>
  `;
  return slideFrame(slideNo, clean(item.eyebrow || "SYNTHESIS").toUpperCase(), clean(item.title || "贡献与局限"), clean(item.subtitle || ""), body, { band: false });
}

function scheme2DiscussionSvg(slideNo) {
  const qs = list(deck.discussionQuestions, 5);
  const questions = qs.length ? qs : ["这篇论文的核心假设是否成立？", "证据是否足以支撑主要结论？", "哪些部分可以迁移到我们的课题？"];
  const body = `
    <g id="questions">
      ${questions.map((q, i) => {
        const y = 286 + i * 74;
        return `
          ${oneLine(`Q${i + 1}`, 100, y, { size: 18, weight: 700, fill: P.accent })}
          <line x1="150" y1="${y - 8}" x2="214" y2="${y - 8}" stroke="${P.line}" stroke-width="1.2"/>
          ${textBlock(q, 240, y, { size: 24, weight: 700, fill: P.ink, width: 38, maxLines: 2, lineHeight: 32 })}
        `;
      }).join("\n")}
    </g>
  `;
  return shell(slideNo, "DISCUSSION", "组会讨论点", "把主论文的贡献、局限和可验证问题留给讨论。", body);
}

function comparisonSvg(slideNo) {
  const papers = Array.isArray(deck.papers) ? deck.papers.slice(0, 4) : [];
  const body = `
    <g id="appendix">
      ${papers.map((paper, i) => `
        <rect x="${90 + (i % 2) * 520}" y="${276 + Math.floor(i / 2) * 150}" width="430" height="98" fill="${P.bg}" stroke="${P.line}"/>
        ${oneLine(String(i + 1).padStart(2, "0"), 116 + (i % 2) * 520, 314 + Math.floor(i / 2) * 150, { size: 16, weight: 700, fill: P.accent })}
        ${textBlock(paper.shortTitle || paper.title || "参考文献", 160 + (i % 2) * 520, 314 + Math.floor(i / 2) * 150, { size: 18, weight: 700, width: 24, maxLines: 2 })}
      `).join("\n")}
    </g>
  `;
  return shell(slideNo, "APPENDIX", "对比文献附录", "仅在参数中显式开启时生成。", body);
}

function svgName(index, slug) {
  return `${String(index).padStart(2, "0")}_${slug}.svg`;
}

function notesText(item) {
  return clean(item?.speakerNotes || item?.notes || item?.keyMessage || "");
}

function findPython() {
  const candidates = [
    process.env.PPT_MASTER_PYTHON,
    "/Users/yuan/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3",
    "python3",
  ].filter(Boolean);
  return candidates.find((candidate) => candidate === "python3" || fs.existsSync(candidate)) || "python3";
}

function findPptMasterSkillDir() {
  const candidates = [
    process.env.PPT_MASTER_SKILL_DIR,
    path.resolve(process.cwd(), "backend/ppt-master-runtime"),
    path.resolve(process.cwd(), "ppt-master-runtime"),
    path.resolve(process.cwd(), "../ppt-master-runtime"),
    path.resolve(process.cwd(), "../ppt-master/skills/ppt-master"),
    path.resolve(process.cwd(), "ppt-master/skills/ppt-master"),
    "/tmp/ppt-master-inspect/skills/ppt-master",
  ].filter(Boolean);
  return candidates.find((candidate) => fs.existsSync(path.join(candidate, "scripts/svg_to_pptx.py")));
}

function runPythonTool(skillDir, args, label, allowFailure = false) {
  const python = findPython();
  const result = spawnSync(python, args, {
    cwd: projectDir,
    encoding: "utf8",
    env: { ...process.env, PYTHONPATH: path.join(skillDir, "scripts") },
    maxBuffer: 1024 * 1024 * 10,
  });
  const output = `${result.stdout || ""}${result.stderr || ""}`;
  fs.writeFileSync(path.join(projectDir, `${label}.log`), output);
  if (result.status !== 0 && !allowFailure) {
    throw new Error(`${label} failed\n${output}`);
  }
  return { status: result.status ?? 1, output };
}

function createPreviewContactSheet(pageCount) {
  const files = fs.readdirSync(svgDir).filter((name) => name.endsWith(".svg")).sort();
  const thumbW = 320;
  const thumbH = 180;
  const gap = 24;
  const cols = 2;
  const rows = Math.ceil(files.length / cols);
  const sheetW = cols * thumbW + (cols + 1) * gap;
  const sheetH = rows * (thumbH + 34) + (rows + 1) * gap;
  const body = files.map((file, i) => {
    const x = gap + (i % cols) * (thumbW + gap);
    const y = gap + Math.floor(i / cols) * (thumbH + 34 + gap);
    const href = `svg_output/${file}`;
    return `
      <rect x="${x - 1}" y="${y - 1}" width="${thumbW + 2}" height="${thumbH + 2}" fill="#FFFFFF" stroke="#DDE5F0"/>
      <image href="${href}" x="${x}" y="${y}" width="${thumbW}" height="${thumbH}" preserveAspectRatio="xMidYMid meet"/>
      <text x="${x}" y="${y + thumbH + 24}" font-family="Arial, Microsoft YaHei, sans-serif" font-size="14" fill="#526070">${esc(file)}</text>
    `;
  }).join("\n");
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${sheetW} ${sheetH}" width="${sheetW}" height="${sheetH}">
  <rect x="0" y="0" width="${sheetW}" height="${sheetH}" fill="#F6F8FB"/>
  ${body}
</svg>`;
  const previewPath = path.join(projectDir, "preview-contact-sheet.svg");
  fs.writeFileSync(previewPath, svg);
  return previewPath;
}

function runQualityPipeline(skillDir, pageCount) {
  const finalize = runPythonTool(
    skillDir,
    [path.join(skillDir, "scripts/finalize_svg.py"), projectDir, "--quiet"],
    "01-finalize-svg",
    false,
  );
  const quality = runPythonTool(
    skillDir,
    [path.join(skillDir, "scripts/svg_quality_checker.py"), svgDir, "--format", "ppt169"],
    "02-quality-check",
    true,
  );
  const previewPath = createPreviewContactSheet(pageCount);
  fs.writeFileSync(path.join(projectDir, "quality-summary.json"), JSON.stringify({
    finalized: finalize.status === 0,
    qualityExitCode: quality.status,
    qualityPassed: quality.status === 0,
    previewPath,
    generatedAt: new Date().toISOString(),
  }, null, 2));
  if (quality.status !== 0) {
    throw new Error(`PPT Master 质量检查失败，请查看 ${path.join(projectDir, "02-quality-check.log")}\n${quality.output.slice(-2400)}`);
  }
  return { previewPath, qualityLog: quality.output };
}

// Scheme 2: heavier slide art direction. The first renderer pass above kept the
// PPT Master export pipeline but still looked like a thin text template. These
// overrides keep the same native SVG/PPTX workflow while giving each page a
// distinct editorial layout.
const ART = {
  night: "#101827",
  night2: "#182338",
  paper: "#F7F4EC",
  cream: "#FFFDF8",
  ink: "#132033",
  muted: "#657083",
  blue: "#2E63E6",
  cyan: "#16B8C7",
  green: "#18A77B",
  orange: "#F2A51A",
  red: "#E45B4F",
  line: "#D7DFEA",
};

function artFooter(slideNo, dark = false) {
  const fill = dark ? "#9FB0C9" : "#8A97AA";
  const accent = dark ? ART.cyan : ART.blue;
  return `
    <line x1="72" y1="650" x2="150" y2="650" stroke="${accent}" stroke-width="3"/>
    ${oneLine("PaperPilot · Academic PPT Master", 72, 678, { size: 12, fill })}
    ${oneLine(String(slideNo).padStart(2, "0"), 1202, 678, { size: 18, weight: 800, fill: accent, anchor: "end" })}
  `;
}

function artTitle(eyebrow, title, subtitle, dark = false, x = 72, y = 78, width = 24) {
  const ink = dark ? "#FFFFFF" : ART.ink;
  const muted = dark ? "#B8C5D8" : ART.muted;
  return `
    ${oneLine(clean(eyebrow || "RESEARCH").toUpperCase(), x, y, { size: 12, weight: 800, fill: dark ? ART.cyan : ART.blue, style: 'letter-spacing="2.8"' })}
    ${textBlock(title, x, y + 58, { size: 43, weight: 800, fill: ink, width, maxLines: 2, lineHeight: 52, family: FONT_TITLE })}
    ${subtitle ? textBlock(subtitle, x, y + 160, { size: 18, fill: muted, width: 44, maxLines: 2, lineHeight: 26 }) : ""}
  `;
}

function chip(x, y, label, color = ART.blue, dark = false) {
  return `
    <rect x="${x}" y="${y}" width="${Math.min(210, 48 + clean(label).length * 15)}" height="34" rx="17" fill="${dark ? "#22314C" : "#F0F5FF"}" stroke="${color}" stroke-width="1.2"/>
    ${oneLine(label, x + 18, y + 23, { size: 13, weight: 800, fill: color })}
  `;
}

function artNodeBox(x, y, w, h, title, body, color, index) {
  return `
    <rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${ART.night}" stroke="#2C3A55"/>
    <rect x="${x}" y="${y}" width="${w}" height="8" fill="${color}"/>
    ${oneLine(index, x + 20, y + 42, { size: 13, weight: 800, fill: color })}
    ${textBlock(title, x + 20, y + 75, { size: 21, weight: 800, fill: "#FFFFFF", width: 8, maxLines: 2, lineHeight: 27 })}
    ${textBlock(body, x + 20, y + 126, { size: 14, fill: "#B8C5D8", width: Math.max(12, Math.floor(w / 18)), maxLines: h > 190 ? 3 : 2, lineHeight: 19 })}
  `;
}

function artBullets(items, x, y, opts = {}) {
  const values = list(items, opts.limit || 4);
  const colors = opts.colors || [ART.blue, ART.green, ART.orange, ART.cyan];
  return values.map((item, i) => {
    const pair = splitLead(item);
    const top = y + i * (opts.gap || 74);
    const color = colors[i % colors.length];
    return `
      <rect x="${x}" y="${top - 28}" width="14" height="50" fill="${color}"/>
      ${textBlock(pair.label || item, x + 34, top, { size: opts.titleSize || 22, weight: 800, fill: opts.fill || ART.ink, width: opts.titleWidth || 18, maxLines: 1 })}
      ${pair.label ? textBlock(pair.rest, x + 34, top + 28, { size: opts.bodySize || 15, fill: opts.muted || ART.muted, width: opts.bodyWidth || 34, maxLines: 2, lineHeight: 21 }) : ""}
    `;
  }).join("\n");
}

function coverSvg() {
  const primary = primaryPaper();
  const core = essence();
  const title = clean(deck.title || primary.shortTitle || primary.title || "组会论文汇报").replace(/^组会汇报[：:]\s*/, "");
  const takeaways = ["Background", "Methodology", "Experiment / Results", "Conclusion", "Outlook"];
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="#F8FAFC"/>
  <rect x="0" y="0" width="1280" height="88" fill="#FFFFFF"/>
  <line x1="72" y1="88" x2="1208" y2="88" stroke="#CBD5E1"/>
  <rect x="72" y="126" width="8" height="420" fill="${ART.blue}"/>
  ${oneLine("ACADEMIC PPT MASTER · PAPER PRESENTATION", 96, 142, { size: 13, weight: 800, fill: ART.blue, style: 'letter-spacing="2.2"' })}
  ${textBlock(title, 96, 225, { size: 44, weight: 800, fill: ART.ink, width: 16, maxLines: 4, lineHeight: 54, family: FONT_TITLE })}
  ${textBlock(clean(core.oneSentence || primary.title || primary.fileName || "上传主论文"), 96, 502, { size: 18, fill: ART.muted, width: 40, maxLines: 3, lineHeight: 25 })}
  <g>
    <rect x="812" y="150" width="344" height="380" fill="#FFFFFF" stroke="#CBD5E1"/>
    ${oneLine("Academic Structure", 850, 204, { size: 18, weight: 800, fill: ART.ink })}
    ${takeaways.map((item, i) => `
      <circle cx="862" cy="${258 + i * 52}" r="7" fill="${[ART.blue, ART.green, ART.orange, ART.cyan, ART.red][i]}"/>
      ${oneLine(String(i + 1).padStart(2, "0"), 890, 264 + i * 52, { size: 12, weight: 800, fill: [ART.blue, ART.green, ART.orange, ART.cyan, ART.red][i] })}
      ${oneLine(item, 930, 264 + i * 52, { size: 16, weight: 760, fill: ART.ink })}
    `).join("\n")}
  </g>
  <rect x="812" y="560" width="344" height="42" fill="#EAF2FF"/>
  ${oneLine("Formula · Figure · Table · Notes · Editable PPTX", 836, 586, { size: 13, weight: 760, fill: ART.blue })}
  ${oneLine("PaperPilot · Academic PPT Master", 72, 678, { size: 12, fill: "#64748B" })}
  ${oneLine("01", 1202, 678, { size: 18, weight: 800, fill: ART.blue, anchor: "end" })}
</svg>`;
}

function agendaSvg(slideNo) {
  const chain = essenceList("argumentChain", 6);
  const items = (chain.length ? chain : (narrativeList(deck.agenda, 6).length ? narrativeList(deck.agenda, 6) : ["Background: 研究背景与问题", "Methodology: 方法与模型", "Experiment: 实验设计", "Results: 结果解释", "Conclusion: 结论贡献", "Outlook: 局限与展望"]));
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.cream}"/>
  <rect x="0" y="0" width="360" height="${H}" fill="${ART.paper}"/>
  ${artTitle("Academic Structure", "Background → Method → Results → Outlook", clean(essence().coreClaim || "按学术汇报结构保留论文方法、证据、图表与结论。"), false, 72, 76, 14)}
  <g>
    <line x1="474" y1="166" x2="474" y2="592" stroke="${ART.line}" stroke-width="2"/>
    ${items.map((item, i) => {
      const y = 170 + i * 78;
      const color = [ART.blue, ART.green, ART.orange, ART.cyan, ART.red, ART.blue][i % 6];
      return `
        <circle cx="474" cy="${y}" r="13" fill="${ART.cream}" stroke="${color}" stroke-width="4"/>
        <rect x="526" y="${y - 32}" width="${520 - (i % 2) * 84}" height="62" fill="#FFFFFF" stroke="${ART.line}"/>
        <rect x="526" y="${y - 32}" width="7" height="62" fill="${color}"/>
        ${oneLine(String(i + 1).padStart(2, "0"), 554, y - 6, { size: 13, weight: 800, fill: color })}
        ${textBlock(item, 598, y - 6, { size: 21, weight: 800, fill: ART.ink, width: 26, maxLines: 1 })}
      `;
    }).join("\n")}
  </g>
  ${artFooter(slideNo)}
</svg>`;
}

function contentSvg(item, slideNo, variant = 0) {
  const layout = keywordLayout(item);
  if (layout === "method") return methodSvg(item, slideNo, variant);
  if (layout === "evidence") return evidenceSvg(item, slideNo, variant);
  if (layout === "background") return backgroundSvg(item, slideNo, variant);
  if (layout === "discussionish") return synthesisSvg(item, slideNo, variant);
  const bullets = list(item.bullets, 4);
  const evidence = list(item.evidence, 3);
  const assetCue = clean(item.assetCue || "");
  const quote = clean(item.keyMessage || assetCue || evidence[0] || "");
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.cream}"/>
  <rect x="858" y="0" width="422" height="${H}" fill="${ART.night}"/>
  ${artTitle(clean(item.eyebrow || "Deep Reading"), clean(item.title || "结构化内容"), clean(item.subtitle || ""), false, 72, 78, 18)}
  <g>
    <rect x="84" y="304" width="646" height="250" fill="#FFFFFF" stroke="${ART.line}"/>
    ${artBullets(bullets, 122, 356, { limit: 4, titleWidth: 24, bodyWidth: 34 })}
  </g>
  <g>
    <rect x="908" y="206" width="276" height="330" fill="#162238" stroke="#2C3A55"/>
    <rect x="908" y="206" width="276" height="8" fill="${ART.cyan}"/>
    ${oneLine(assetCue ? "ASSET / KEY MESSAGE" : "KEY MESSAGE", 942, 274, { size: 13, weight: 800, fill: ART.cyan, style: 'letter-spacing="1.8"' })}
    ${textBlock(quote || bullets[0] || "待补充", 942, 350, { size: 27, weight: 800, fill: "#FFFFFF", width: 12, maxLines: 4, lineHeight: 36, family: FONT_TITLE })}
  </g>
  ${artFooter(slideNo)}
</svg>`;
}

function backgroundSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 4);
  const evidence = list(item.evidence, 2);
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.cream}"/>
  <rect x="0" y="0" width="${W}" height="186" fill="${ART.night}"/>
  ${artTitle(clean(item.eyebrow || "Introduction"), clean(item.title || "研究背景"), clean(item.subtitle || ""), true, 72, 60, 24)}
  <g>
    <rect x="78" y="258" width="456" height="326" fill="#FFFFFF" stroke="${ART.line}"/>
    ${bullets.slice(0, 4).map((bullet, i) => {
      const pair = splitLead(bullet);
      const color = [ART.blue, ART.green, ART.orange, ART.cyan][i % 4];
      const y = 292 + i * 70;
      return `
        <rect x="112" y="${y}" width="12" height="48" fill="${color}"/>
        ${textBlock(pair.label || bullet, 150, y + 22, { size: 20, weight: 800, fill: ART.ink, width: 12, maxLines: 1 })}
        ${pair.label ? textBlock(pair.rest, 150, y + 48, { size: 13, fill: ART.muted, width: 28, maxLines: 1 }) : ""}
      `;
    }).join("\n")}
  </g>
  <g>
    <rect x="654" y="246" width="430" height="318" fill="${ART.paper}" stroke="${ART.line}"/>
    <line x1="704" y1="502" x2="1032" y2="274" stroke="${ART.line}" stroke-width="1.4"/>
    <line x1="704" y1="502" x2="1008" y2="502" stroke="${ART.line}" stroke-width="1.4"/>
    <line x1="704" y1="502" x2="704" y2="294" stroke="${ART.line}" stroke-width="1.4"/>
    <path d="M 704 500 C 790 450 826 385 888 360 C 944 338 984 304 1032 274" fill="none" stroke="${ART.blue}" stroke-width="5"/>
    <path d="M 704 500 C 776 486 806 456 856 444 C 924 428 966 390 1008 356" fill="none" stroke="${ART.green}" stroke-width="4"/>
    ${chip(728, 302, "法规压力", ART.blue)}
    ${chip(790, 418, "落地鸿沟", ART.green)}
    ${chip(914, 482, "治理机制", ART.orange)}
  </g>
  ${textBlock(clean(item.keyMessage || evidence[0] || ""), 654, 604, { size: 16, weight: 800, fill: ART.ink, width: 34, maxLines: 2, lineHeight: 23 })}
  ${artFooter(slideNo)}
</svg>`;
}

function methodSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 5);
  const evidence = list(item.evidence, 2);
  const assetCue = clean(item.assetCue || "");
  const nodes = bullets.slice(0, 4).map(splitLead);
  while (nodes.length < 4) nodes.push({ label: ["输入", "机制", "调节", "输出"][nodes.length], rest: "待补充" });
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.cream}"/>
  ${artTitle(clean(item.eyebrow || "Research Framework"), clean(item.title || "方法框架"), clean(item.subtitle || ""), false, 72, 74, 24)}
  <g>
    ${artNodeBox(74, 318, 226, 164, nodes[0].label || "输入", nodes[0].rest, ART.blue, "01")}
    ${arrow(314, 400, 396, 400, ART.blue)}
    ${artNodeBox(410, 286, 246, 218, nodes[1].label || "机制路径", nodes[1].rest, ART.green, "02")}
    ${arrow(672, 400, 754, 400, ART.green)}
    ${artNodeBox(768, 318, 226, 164, nodes[3].label || "输出", nodes[3].rest, ART.orange, "03")}
    <rect x="442" y="548" width="424" height="74" fill="${ART.night}" stroke="${ART.night2}"/>
    ${oneLine(assetCue ? "保留资产" : (nodes[2].label || "关键证据"), 474, 580, { size: 14, weight: 800, fill: ART.cyan })}
    ${textBlock(assetCue || evidence[0] || nodes[2].rest, 590, 580, { size: 16, fill: "#FFFFFF", width: 20, maxLines: 1 })}
    ${arrow(654, 548, 654, 506, ART.orange)}
  </g>
  ${artFooter(slideNo)}
</svg>`;
}

function evidenceSvg(item, slideNo, variant = 0) {
  const bullets = list(item.evidence, 5).length ? list(item.evidence, 5) : list(item.bullets, 5);
  const assetCue = clean(item.assetCue || "");
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.night}"/>
  ${artTitle(clean(item.eyebrow || "Evidence"), clean(item.title || "证据与结果"), clean(item.subtitle || ""), true, 72, 72, 24)}
  <g>
    ${bullets.slice(0, 4).map((bullet, i) => {
      const pair = splitLead(bullet);
      const x = 82 + (i % 2) * 554;
      const y = 292 + Math.floor(i / 2) * 152;
      const color = [ART.cyan, ART.green, ART.orange, ART.blue][i % 4];
      return `
        <rect x="${x}" y="${y}" width="486" height="118" fill="#162238" stroke="#2E3B55"/>
        <rect x="${x}" y="${y}" width="9" height="118" fill="${color}"/>
        ${oneLine(pair.label || `证据 ${i + 1}`, x + 34, y + 42, { size: 18, weight: 800, fill: color })}
        ${textBlock(pair.rest || bullet, x + 34, y + 76, { size: 17, fill: "#EAF1FF", width: 28, maxLines: 2, lineHeight: 23 })}
      `;
    }).join("\n")}
  </g>
  ${assetCue ? `
  <g>
    <rect x="82" y="610" width="1040" height="46" fill="#101827" stroke="#2E3B55"/>
    ${oneLine("FIGURE / TABLE CUE", 112, 640, { size: 13, weight: 800, fill: ART.cyan })}
    ${textBlock(assetCue, 280, 640, { size: 15, fill: "#EAF1FF", width: 46, maxLines: 1 })}
  </g>` : ""}
  ${artFooter(slideNo, true)}
</svg>`;
}

function synthesisSvg(item, slideNo, variant = 0) {
  const bullets = list(item.bullets, 4);
  const quote = clean(item.keyMessage || bullets[0] || "待补充");
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.cream}"/>
  <rect x="72" y="74" width="400" height="560" fill="${ART.night}"/>
  ${oneLine(clean(item.eyebrow || "Synthesis").toUpperCase(), 112, 132, { size: 12, weight: 800, fill: ART.cyan, style: 'letter-spacing="2.4"' })}
  ${textBlock(clean(item.title || "贡献与局限"), 112, 204, { size: 42, weight: 800, fill: "#FFFFFF", width: 12, maxLines: 3, lineHeight: 52, family: FONT_TITLE })}
  ${textBlock(quote, 112, 438, { size: 22, weight: 800, fill: "#EAF1FF", width: 14, maxLines: 4, lineHeight: 31 })}
  <g>
    ${bullets.map((bullet, i) => `
      <rect x="560" y="${208 + i * 88}" width="540" height="62" fill="#FFFFFF" stroke="${ART.line}"/>
      ${oneLine(String(i + 1).padStart(2, "0"), 590, 247 + i * 88, { size: 15, weight: 800, fill: [ART.blue, ART.green, ART.orange, ART.cyan][i] })}
      ${textBlock(bullet, 640, 247 + i * 88, { size: 20, weight: 800, fill: ART.ink, width: 30, maxLines: 1 })}
    `).join("\n")}
  </g>
  ${artFooter(slideNo)}
</svg>`;
}

function discussionSvg(slideNo) {
  const questions = (list(deck.discussionQuestions, 5).length ? list(deck.discussionQuestions, 5) : ["核心假设是否足够稳固？", "证据链还有哪些薄弱处？", "哪些结论能迁移到我们的课题？"]);
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" width="${W}" height="${H}">
  <rect x="0" y="0" width="${W}" height="${H}" fill="${ART.paper}"/>
  <rect x="0" y="0" width="${W}" height="210" fill="${ART.night}"/>
  ${artTitle("Discussion", "组会讨论问题", "把论文的贡献、边界和后续验证拆成可讨论的问题。", true, 72, 66, 24)}
  <g>
    ${questions.map((q, i) => {
      const x = 84 + (i % 2) * 540;
      const y = 286 + Math.floor(i / 2) * 122;
      return `
        <rect x="${x}" y="${y}" width="456" height="82" fill="#FFFFFF" stroke="${ART.line}"/>
        <rect x="${x}" y="${y}" width="76" height="82" fill="${[ART.blue, ART.green, ART.orange, ART.cyan, ART.red][i % 5]}"/>
        ${oneLine(`Q${i + 1}`, x + 38, y + 51, { size: 20, weight: 800, fill: "#FFFFFF", anchor: "middle" })}
        ${textBlock(q, x + 106, y + 36, { size: 19, weight: 800, fill: ART.ink, width: 24, maxLines: 2, lineHeight: 27 })}
      `;
    }).join("\n")}
  </g>
  ${artFooter(slideNo)}
</svg>`;
}

function writeProject() {
  fs.rmSync(projectDir, { recursive: true, force: true });
  fs.mkdirSync(svgDir, { recursive: true });
  fs.mkdirSync(notesDir, { recursive: true });
  fs.writeFileSync(path.join(projectDir, "project.json"), JSON.stringify({
    name: "paperpilot_meeting_report",
    format: "ppt169",
    width: W,
    height: H,
    created_at: new Date().toISOString(),
  }, null, 2));
  fs.writeFileSync(path.join(projectDir, "design_spec.md"), `# PaperPilot Meeting Report\n\nCanvas: PPT 16:9 (1280x720)\nStyle: academic paper deep-read, PPT Master SVG export\n`);
  fs.writeFileSync(path.join(projectDir, "spec_lock.md"), `# Spec Lock

## Canvas
- Format: ppt169
- ViewBox: 0 0 1280 720
- Safe margins: 72px left/right, 64px top, 44px bottom

## Color Tokens
- background: ${P.bg}
- paper: ${P.paper}
- ink: ${P.ink}
- muted: ${P.muted}
- faint: ${P.faint}
- line: ${P.line}
- accent: ${P.accent}
- accent2: ${P.accent2}
- warm: ${P.warm}
- footerMuted: #98A2B3
- white: #FFFFFF

## Typography
- title: Georgia, Microsoft YaHei, serif
- body: Arial, Microsoft YaHei, sans-serif
- title sizes: 38, 54
- body sizes: 12, 13, 14, 15, 16, 18, 19, 20, 21, 22, 23, 24, 25, 27, 30

## Layout Rules
- No generic text-only slides.
- Use a different visual structure for background, method, evidence, synthesis, and discussion pages.
- No forbidden group opacity; opacity belongs on child shapes.
- Keep every slide native-editable through PPT Master SVG export.
`);

  const pages = [];
  pages.push({ name: svgName(1, "cover"), svg: coverSvg(), notes: "开场说明本次汇报聚焦上传主论文。" });
  pages.push({ name: svgName(2, "agenda"), svg: agendaSvg(2), notes: "说明汇报顺序。" });
  const sourceSlides = Array.isArray(deck.slides)
    ? deck.slides.filter((item) => includeComparisonAppendix() || !isComparisonText(`${item.eyebrow || ""} ${item.title || ""} ${item.subtitle || ""}`))
    : [];
  const maxSlides = Math.max(2, Math.min(8, sourceSlides.length));
  sourceSlides.slice(0, maxSlides).forEach((item, i) => {
    pages.push({ name: svgName(pages.length + 1, `content_${i + 1}`), svg: contentSvg(item, pages.length + 1, i), notes: notesText(item) });
  });
  if (includeComparisonAppendix()) {
    pages.push({ name: svgName(pages.length + 1, "comparison_appendix"), svg: comparisonSvg(pages.length + 1), notes: "对比附录仅供讨论参考。" });
  }
  pages.push({ name: svgName(pages.length + 1, "discussion"), svg: discussionSvg(pages.length + 1), notes: "提出组会讨论问题。" });

  pages.forEach((page, i) => {
    fs.writeFileSync(path.join(svgDir, page.name), page.svg);
    fs.writeFileSync(path.join(notesDir, page.name.replace(/\.svg$/, ".md")), page.notes || "");
  });
  fs.writeFileSync(path.join(notesDir, "total.md"), pages.map((page, i) => `# ${i + 1}\n\n${page.notes || ""}`).join("\n\n"));
  return pages.length;
}

function exportWithPptMaster() {
  const skillDir = findPptMasterSkillDir();
  if (!skillDir) {
    throw new Error("未找到 PPT Master runtime，请设置 PPT_MASTER_SKILL_DIR 指向 skills/ppt-master 目录");
  }
  const quality = runQualityPipeline(skillDir, fs.readdirSync(svgDir).filter((name) => name.endsWith(".svg")).length);
  const python = findPython();
  const script = path.join(skillDir, "scripts/svg_to_pptx.py");
  const result = spawnSync(python, [
    script,
    projectDir,
    "-o",
    path.resolve(outputPath),
    "--only",
    "native",
    "--transition",
    "fade",
    "--transition-duration",
    "0.35",
    "--no-notes",
  ], {
    cwd: projectDir,
    encoding: "utf8",
    env: { ...process.env, PYTHONPATH: path.join(skillDir, "scripts") },
    maxBuffer: 1024 * 1024 * 10,
  });
  if (result.status !== 0) {
    throw new Error(`PPT Master export failed\n${result.stdout || ""}\n${result.stderr || ""}`);
  }
  return { stdout: result.stdout, quality };
}

fs.mkdirSync(outputDir, { recursive: true });
const slideCount = writeProject();
const exportResult = exportWithPptMaster();
console.log(JSON.stringify({
  status: "generated",
  renderer: "ppt-master-svg",
  outputPath: path.resolve(outputPath),
  projectDir,
  slideCount,
  previewPath: exportResult.quality.previewPath,
  qualityLogPath: path.join(projectDir, "02-quality-check.log"),
  qualitySummaryPath: path.join(projectDir, "quality-summary.json"),
  logTail: exportResult.stdout.split("\n").slice(-8).join("\n"),
}));
