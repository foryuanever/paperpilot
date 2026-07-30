<template>
  <div class="reader-report">
    <header class="report-head">
      <div>
        <div class="report-title-row">
          <strong>
            <svg class="ai-sparkle-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L14.5 9.5L22 12L14.5 14.5L12 22L9.5 14.5L2 12L9.5 9.5L12 2Z" fill="url(#aiGlowGrad)" stroke="none"/><defs><linearGradient id="aiGlowGrad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="#818cf8"/><stop offset="100%" stop-color="#3b82f6"/></linearGradient></defs></svg>
            文献综述
          </strong>
        </div>
        <span class="status-badge" :class="{ ready: report.generated }">
          <i class="status-dot"></i>
          {{ report.generated ? "综述已生成" : "等待综述生成" }}
        </span>
      </div>
      <button class="reanalyze-btn" :disabled="busy || !workspaceId" @click="generateReport">
        <svg v-if="busy" class="btn-icon spin" viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
        <svg v-else class="btn-icon" viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67"/></svg>
        <span>{{ busy ? `${progress}%` : report.generated ? "重新分析" : "开始分析" }}</span>
      </button>
    </header>

    <section class="review-scope" aria-label="文献综述包含内容">
      <strong>{{ report.paper?.title || paper?.title || "当前论文" }}</strong>
      <p class="scope-label">
        <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
        文献综述包含
      </p>
      <div class="scope-chips">
        <span v-for="item in reviewScope" :key="item" class="scope-chip">{{ item }}</span>
      </div>
    </section>

    <div v-if="busy" class="report-progress">
      <span><i :style="{ transform: `scaleX(${Math.max(.05, progress / 100)})` }"></i></span>
      <p>{{ progressMessage || "正在逐章精读论文，结果会自动保存。" }}</p>
    </div>

    <div v-if="!workspaceId" class="report-empty">
      当前论文缺少工作区编号，重新从文献库打开后即可生成文献综述。
    </div>

    <div v-else-if="wide" class="report-expanded-list">
      <article
        v-for="(chapter, chapterIndex) in displayChapters"
        :key="chapter.key"
        class="expanded-chapter"
        :class="[`tone-${chapterIndex % 6}`, { 'is-synthesis': chapter.key === 'synthesis' }]"
      >
        <header class="expanded-chapter-head">
          <span>{{ chapterIndex + 1 }}</span>
          <div>
            <strong>{{ chapter.short }}</strong>
            <small>{{ chapter.subtitle }} · {{ chapter.pointCount ? `${chapter.pointCount} 点` : "待补充" }}</small>
          </div>
        </header>
        <div class="expanded-chapter-points">
          <section
            v-for="(point, pointIndex) in chapter.blocks"
            :key="`${chapter.key}-${point.title}`"
            class="report-point expanded-card"
            :class="`tone-${chapterIndex % 6}`"
          >
            <header>
              <span>{{ pointIndex + 1 }}</span>
              <strong>{{ point.title }}</strong>
              <em>{{ point.items.length ? `${point.items.length} 点` : "待补充" }}</em>
            </header>
            <p v-if="point.lead" class="point-lead">{{ point.lead }}</p>
            <ol v-if="point.items.length">
              <li v-for="(item, itemIndex) in point.items" :key="item">
                <span>{{ itemIndex + 1 }}</span>
                <p>{{ item }}</p>
              </li>
            </ol>
          </section>
        </div>
      </article>
    </div>

    <div v-else class="report-list">
      <article
        v-for="(chapter, chapterIndex) in displayChapters"
        :key="chapter.key"
        class="report-chapter"
        :class="[`tone-${chapterIndex % 4}`, { 'is-synthesis': chapter.key === 'synthesis' }]"
      >
        <button class="chapter-toggle" @click="toggleChapter(chapter.key)">
          <span class="chapter-index">{{ chapterIndex + 1 }}</span>
          <span class="chapter-heading">
            <strong>{{ chapter.short }}</strong>
            <small>{{ chapter.subtitle }}</small>
          </span>
          <span class="chapter-count">{{ chapter.pointCount ? `${chapter.pointCount} 点` : "待补充" }}</span>
          <span class="chapter-arrow" :class="{ open: expanded.has(chapter.key) }">›</span>
        </button>

        <div v-if="expanded.has(chapter.key)" class="chapter-content">
          <section
            v-for="(block, blockIndex) in chapter.blocks"
            :key="`${chapter.key}-${block.title}`"
            class="report-point"
            :class="`tone-${chapterIndex % 6}`"
          >
            <header>
              <span>{{ blockIndex + 1 }}</span>
              <strong>{{ block.title }}</strong>
              <em>{{ block.items.length ? `${block.items.length} 点` : "待补充" }}</em>
            </header>
            <p v-if="block.lead" class="point-lead">{{ block.lead }}</p>
            <ol v-if="block.items.length">
              <li v-for="(item, itemIndex) in block.items" :key="item">
                <span>{{ itemIndex + 1 }}</span>
                <p>{{ item }}</p>
              </li>
            </ol>
          </section>
        </div>
      </article>
    </div>

    <Transition name="report-toast">
      <div v-if="toast" class="report-toast">{{ toast }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";

const props = defineProps({
  workspaceId: { type: String, default: "" },
  paper: { type: Object, default: () => ({}) },
  wide: { type: Boolean, default: false },
});

const busy = ref(false);
const savingNote = ref(false);
const progress = ref(0);
const progressMessage = ref("");
const noteDraft = ref("");
const toast = ref("");
const expanded = reactive(new Set(["context", "method"]));
let pollTimer;
let toastTimer;

const backendSectionKeys = ["synthesis", "basicInfo", "overview", "background", "method", "results", "conclusion", "datasets"];

const chapters = [
  {
    key: "context",
    short: "研究背景",
    subtitle: "现状、缺口、目标",
    sourceKeys: ["overview", "background", "synthesis"],
    points: ["领域现状", "研究缺口", "研究目标"],
  },
  {
    key: "method",
    short: "研究设计",
    subtitle: "对象、方法、指标",
    sourceKeys: ["method", "datasets", "synthesis"],
    points: ["研究对象", "方法设计", "评价指标"],
  },
  {
    key: "findings",
    short: "主要发现",
    subtitle: "结果、证据、解释",
    sourceKeys: ["results", "conclusion", "synthesis"],
    points: ["结果表现", "对比证据", "机制解释"],
  },
  {
    key: "contribution",
    short: "贡献价值",
    subtitle: "创新、意义、适用",
    sourceKeys: ["overview", "results", "conclusion", "synthesis"],
    points: ["主要创新", "研究意义", "适用场景"],
  },
  {
    key: "limits",
    short: "局限展望",
    subtitle: "不足、风险、方向",
    sourceKeys: ["conclusion", "synthesis", "results"],
    points: ["研究局限", "应用风险", "未来方向"],
  },
];

const reviewScope = chapters.map(chapter => chapter.short);

const report = reactive({
  paper: { title: "", abstract: "", note: "" },
  sections: Object.fromEntries(backendSectionKeys.map(key => [key, ""])),
  synthesisText: "",
  generated: false,
  modelName: "",
});

const synthesisText = computed(() => {
  const backend = String(report.sections?.synthesis || "").trim();
  if (backend) return backend;
  const overview = sectionLines("overview").join(" ");
  const background = sectionLines("background").join(" ");
  const abstract = String(report.paper?.abstract || "").trim();
  const combined = [overview, background].filter(Boolean).join(" ");
  const source = combined || abstract || "";
  if (!source) return report.generated ? "暂无综述内容。" : "等待 AI 精读后生成全文综述。";
  return source;
});

const synthesisParagraphs = computed(() =>
  synthesisText.value
    .split(/\n{2,}|(?<=[。！？])\s{2,}/)
    .map(p => p.replace(/\s+/g, " ").trim())
    .filter(p => p.length > 4)
);

const displayChapters = computed(() => chapters.map(chapter => {
  const blocks = blocksForChapter(chapter);
  return {
    ...chapter,
    blocks,
    pointCount: blocks.reduce((sum, block) => sum + block.items.length, 0),
  };
}));

const hasReviewContent = computed(() =>
  Object.entries(report.sections || {})
    .filter(([key]) => key !== "basicInfo")
    .some(([, value]) => sectionContentLines(value).length > 0),
);

function cleanLine(line) {
  return String(line || "")
    .replace(/\\[rnt]/g, " ")
    .replace(/^[\-•·○◦▪▫\d.、:：；;\s]+/, "")
    .replace(/^(?:[（(]\d+[）)]\s*)?(?:领域现状|研究缺口|研究目标|研究对象|方法设计|评价指标|结果表现|对比证据|机制解释|主要创新|研究意义|适用场景|研究局限|应用风险|未来方向)\s*[：:]\s*(?=\S)/, "")
    .replace(/^(?:待核对|待补充)\s*[：:]\s*(?=\S)/, "待核对：")
    .replace(/(?:在)?汇报时可(?:以)?[^。！？；]*[。！？；]?/g, "")
    .replace(/[{}"“”]+/g, "")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function isMeaningfulLine(line) {
  const value = cleanLine(line);
  if (value.length <= 4) return false;
  if (isWeakPlaceholderLine(value)) return false;
  if (isMostlyEnglishLine(value)) return false;
  if (/^(?:要点|概述|总结|分析|论文定位|发表信息|发布信息|汇报价值|研究背景|研究问题|研究方法与数据|实验与结论|创新点与启示|局限性|核心要点|主要贡献|关键问题|本文思想|关键贡献|整体框架|关键模块|实现流程|主要发现|对比结果|研究结论|现有不足|未来展望|数据来源|数据设置|评测指标)\s*[：:]?$/.test(value)) {
    return false;
  }
  if (/^(?:本文|该文|该论文)?(?:采用|使用|运用|包括|包含)?(?:了)?以下(?:方法|指标|内容|方面|步骤)\s*[：:；;。]?$/.test(value)) return false;
  if (/^(?:本文|该文|该论文)?(?:采用|使用|运用)(?:了)?(?:以下|如下)(?:方法|指标|内容|方面|步骤)/.test(value) && value.length < 24) return false;
  if (/^(?:临床研究数据共享面临挑战|当前研究多聚焦|本文的评价指标包括|本文研究的主要对象是)\s*[，,：:]?$/.test(value)) return false;
  return !/^[\u4e00-\u9fa5A-Za-z0-9与及、\s]{2,20}[：:]$/.test(value);
}

function isWeakPlaceholderLine(value) {
  const text = String(value || "").trim();
  if (/^(?:待核对|待补充|需核对|需回到|需要查阅|建议查看|摘要未明确|正文片段未明确|原文未明确)[：:，,]/.test(text)) return true;
  if (/^(?:当前材料|当前摘要|摘要|正文片段)(?:尚不足|不足以|未能|未明确)/.test(text)) return true;
  if (/需(?:要)?(?:回到|查阅|查看).{0,18}(?:章节|原文|正文)(?:确认|核对)/.test(text) && text.length < 46) return true;
  return false;
}

function isMostlyEnglishLine(value) {
  const text = String(value || "").trim();
  if (!text) return false;
  const letters = (text.match(/[A-Za-z]/g) || []).length;
  const chinese = (text.match(/[\u4e00-\u9fa5]/g) || []).length;
  return letters >= 28 && chinese < Math.max(8, letters * 0.25);
}

function sectionLines(key) {
  const raw = report.sections[key] || (key === "basicInfo" ? report.paper.abstract : "") || "";
  return sectionContentLines(raw);
}

function sectionContentLines(raw) {
  return raw
    .split(/\n+|(?<=[。！？；])\s*/)
    .map(cleanLine)
    .filter(line => isMeaningfulLine(line) && !/等待 AI|原文未明确|HTTP\s*5/.test(line));
}

function chapterRaw(chapter) {
  return (chapter.sourceKeys || [chapter.key])
    .map(key => report.sections[key])
    .filter(Boolean)
    .join("\n\n");
}

function chapterLines(chapter) {
  const lines = (chapter.sourceKeys || [chapter.key]).flatMap(sectionLines);
  if (chapter.key === "context" && report.generated && hasReviewContent.value) {
    const abstract = cleanLine(report.paper?.abstract || "");
    if (abstract) lines.push(abstract);
  }
  return dedupe(lines);
}

function extractBlock(raw, title, titles) {
  if (!raw) return [];
  const escaped = title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = raw.match(new RegExp(`${escaped}\\s*[：:]\\s*`, "m"));
  if (!match || match.index === undefined) return [];
  const start = match.index + match[0].length;
  let end = raw.length;
  titles.forEach((nextTitle) => {
    if (nextTitle === title) return;
    const next = nextTitle.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const nextMatch = raw.slice(start).match(new RegExp(`\\n?\\s*${next}\\s*[：:]`, "m"));
    if (nextMatch?.index !== undefined) end = Math.min(end, start + nextMatch.index);
  });
  return raw.slice(start, end)
    .split(/\n+|(?<=[。！？；])\s*/)
    .map(cleanLine)
    .filter(isMeaningfulLine);
}

function dedupe(lines) {
  const normalize = (text) => text.replace(/[^\u4e00-\u9fa5A-Za-z0-9]/g, "").toLowerCase();
  return lines.reduce((result, line) => {
    const fingerprint = normalize(line);
    if (!fingerprint) return result;
    const isDup = result.some((old) => {
      const oldNorm = normalize(old);
      if (!oldNorm) return false;
      if (oldNorm === fingerprint) return true;
      if (oldNorm.length > 12 && fingerprint.length > 12) {
        const shorter = oldNorm.length < fingerprint.length ? oldNorm : fingerprint;
        const longer = oldNorm.length < fingerprint.length ? fingerprint : oldNorm;
        if (longer.includes(shorter)) return true;
        // 连续 14 字相同视为重复
        for (let i = 0; i + 14 <= shorter.length; i += 4) {
          if (longer.includes(shorter.slice(i, i + 14))) return true;
        }
      }
      return false;
    });
    if (!isDup) result.push(line);
    return result;
  }, []);
}

function blocksForChapter(chapter) {
  const titles = chapter.points || [];
  const raw = chapterRaw(chapter);
  const fallback = chapterLines(chapter);
  return titles.map((title, index) => {
    const parsed = extractBlock(raw, title, titles);
    const mappedFallback = reviewFallback(chapter, title);
    const nearbyFallback = fallback.slice(index * 2, index * 2 + 2);
    const lines = limitReviewPointLines(dedupe(parsed.length ? parsed : mappedFallback.length ? mappedFallback : nearbyFallback));
    return {
      title,
      lead: lines.length ? "" : (hasReviewContent.value ? "暂未从论文中提取到这一项，请点击重新分析。" : "等待 AI 精读后生成内容。"),
      items: lines,
    };
  });
}

function limitReviewPointLines(lines) {
  const values = lines
    .map(cleanLine)
    .filter(isMeaningfulLine);
  if (values.length <= 5) return values;
  return values.slice(0, 5);
}

function reviewFallback(chapter, title) {
  const sources = {
    领域现状: ["overview", "background"],
    研究缺口: ["background", "overview"],
    研究目标: ["background", "synthesis"],
    研究对象: ["datasets", "method"],
    方法设计: ["method", "synthesis"],
    评价指标: ["datasets", "results"],
    结果表现: ["results", "conclusion"],
    对比证据: ["results"],
    机制解释: ["results", "conclusion"],
    主要创新: ["overview", "synthesis", "conclusion"],
    研究意义: ["conclusion", "overview"],
    适用场景: ["conclusion", "results"],
    研究局限: ["conclusion", "synthesis"],
    应用风险: ["conclusion", "results"],
    未来方向: ["conclusion"],
  };
  const hints = {
    领域现状: /背景|现状|领域|已有|目前|传统|既有/,
    研究缺口: /缺口|不足|问题|挑战|难以|尚未|限制/,
    研究目标: /目标|旨在|提出|解决|探索|验证/,
    研究对象: /数据|样本|对象|任务|场景|语料|病例|参与者/,
    方法设计: /方法|框架|模型|算法|策略|流程|设计|训练/,
    评价指标: /指标|评测|准确|性能|统计|显著|AUC|F1|precision|recall/i,
    结果表现: /结果|发现|提升|降低|显著|优于|达到/,
    对比证据: /对比|比较|基线|实验|消融|相较|优于/,
    机制解释: /说明|表明|意味着|原因|机制|解释|影响/,
    主要创新: /创新|贡献|首次|提出|新|改进/,
    研究意义: /意义|价值|启示|证明|支持|推动/,
    适用场景: /适用|应用|场景|实践|部署|临床|教学/,
    研究局限: /局限|不足|限制|偏倚|样本|未能/,
    应用风险: /风险|误差|偏差|不确定|泛化|安全/,
    未来方向: /未来|后续|进一步|可扩展|改进|展望/,
  };
  const pool = (sources[title] || chapter.sourceKeys || []).flatMap(sectionLines);
  const preferred = pool.filter(line => hints[title]?.test(line));
  if (preferred.length) return preferred;
  if (chapter.key === "context" && report.generated && hasReviewContent.value) {
    const abstract = cleanLine(report.paper?.abstract || "");
    if (abstract) return [abstract];
  }
  return pool;
}

function applyReport(data = {}) {
  report.paper = { ...report.paper, ...(data.paper || props.paper || {}) };
  report.sections = { ...report.sections, ...formatReportSections(data.sections || {}) };
  report.generated = Boolean(data.generated);
  report.modelName = data.modelName || "";
  noteDraft.value = data.paper?.note ?? noteDraft.value;
}

function resetReportState() {
  stopPolling();
  busy.value = false;
  progress.value = 0;
  progressMessage.value = "";
  report.paper = { ...(props.paper || {}) };
  report.sections = Object.fromEntries(backendSectionKeys.map(key => [key, ""]));
  report.synthesisText = "";
  report.generated = false;
  report.modelName = "";
  noteDraft.value = props.paper?.note || "";
}

function formatReportSections(sections) {
  return Object.fromEntries(Object.entries(sections).map(([key, value]) => [key, formatReportParagraphs(value)]));
}

function formatReportParagraphs(value = "") {
  const labels = [
    "论文定位", "发表信息", "发布信息", "汇报价值", "研究背景", "研究问题", "研究方法与数据", "实验与结论",
    "创新点与启示", "局限性", "核心要点", "主要贡献", "关键问题", "本文思想", "关键贡献", "整体框架",
    "关键模块", "实现流程", "主要发现", "对比结果", "实验结论", "研究结论", "现有不足", "未来展望",
    "数据来源", "数据设置", "评测指标"
  ];
  const labelPattern = labels.join("|");
  return String(value || "")
    .replace(/\r\n/g, "\n")
    .replace(/发布信息/g, "发表信息")
    .replace(new RegExp(`\\s*((?:${labelPattern})\\s*[：:])\\s*`, "g"), "\n\n$1\n")
    .replace(/([。；;])\s*((?:\d+[.、]|[（(]\d+[）)]))/g, "$1\n$2")
    .replace(/\n{3,}/g, "\n\n")
    .replace(/^\n+/, "")
    .trim();
}

function showToast(message) {
  toast.value = message;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value = ""; }, 2300);
}

function toggleChapter(key) {
  if (expanded.has(key)) expanded.delete(key);
  else expanded.add(key);
}

async function loadReport() {
  if (!props.workspaceId) return;
  try {
    applyReport(await paperpilotApi.getMeetingReport(props.workspaceId));
    await refreshStatus(false);
    if (report.generated && !hasReviewContent.value && !busy.value) {
      await generateReport();
    }
  } catch {
    applyReport({ paper: props.paper });
  }
}

async function generateReport() {
  if (!props.workspaceId || busy.value) return;
  busy.value = true;
  progress.value = 8;
  progressMessage.value = "正在后台精读全文并生成文献综述。";
  try {
    const data = await paperpilotApi.generateMeetingReport(props.workspaceId);
    updateStatus(data);
    startPolling();
  } catch (error) {
    busy.value = false;
    showToast(error?.response?.data?.message || error?.message || "AI 分析启动失败");
  }
}

function updateStatus(data = {}) {
  progress.value = Number(data.progress || 0);
  progressMessage.value = data.message || "";
  busy.value = data.status === "running";
}

async function refreshStatus(showDone = true) {
  if (!props.workspaceId) return;
  const data = await paperpilotApi.getMeetingReportGenerateStatus(props.workspaceId);
  updateStatus(data);
  if (data.status === "running") {
    startPolling();
    return;
  }
  stopPolling();
  if (data.status === "completed") {
    applyReport(await paperpilotApi.getMeetingReport(props.workspaceId));
    if (showDone) showToast("文献综述已更新");
  }
}

function startPolling() {
  if (pollTimer) return;
  pollTimer = setInterval(() => refreshStatus(true).catch(() => {}), 3000);
}

function stopPolling() {
  if (!pollTimer) return;
  clearInterval(pollTimer);
  pollTimer = null;
}

function addBlockToNote(block, chapter) {
  const body = [block.lead, ...block.items].filter(Boolean).join("\n- ");
  const next = `【${chapter.short} / ${block.title}】\n- ${body}`;
  noteDraft.value = noteDraft.value.trim() ? `${noteDraft.value.trim()}\n\n${next}` : next;
  showToast("已加入笔记");
}

async function savePaperNote() {
  if (!props.workspaceId) return;
  savingNote.value = true;
  try {
    const paper = await paperpilotApi.updateLibraryPaper(props.workspaceId, { note: noteDraft.value });
    noteDraft.value = paper.note || "";
    showToast("笔记已保存");
  } catch {
    showToast("笔记保存失败");
  } finally {
    savingNote.value = false;
  }
}

watch(
  () => props.workspaceId,
  async (next, previous) => {
    if (next === previous) return;
    resetReportState();
    if (next) await loadReport();
  },
);

watch(
  () => props.paper,
  (paper) => {
    if (!paper || props.workspaceId) return;
    applyReport({ paper });
  },
  { deep: true },
);

onMounted(loadReport);
onUnmounted(() => {
  stopPolling();
  clearTimeout(toastTimer);
});
</script>

<style scoped>
.reader-report {
  --report-ink: #1e4fd7;
  --report-accent: #2f6df6;
  --report-accent-dark: #1743a8;
  --report-accent-soft: #eef5ff;
  --report-accent-line: #b8cdfd;
  min-height: 100%;
  color: #344054;
  background: transparent;
}
.review-scope {
  padding: 12px 16px 16px;
  border-bottom: 0;
  background: transparent;
}
.review-scope > strong {
  display: -webkit-box;
  overflow: hidden;
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  letter-spacing: -0.01em;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.scope-label {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 12px 0 8px;
  color: #64748b;
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.scope-label svg {
  color: #6366f1;
}
.scope-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.scope-chip {
  padding: 4px 10px;
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.08);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.4;
  backdrop-filter: blur(8px);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.scope-chip:hover {
  background: rgba(99, 102, 241, 0.16);
  border-color: rgba(99, 102, 241, 0.35);
  transform: translateY(-1px);
}
.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 10px;
  border-bottom: 0;
  background: transparent;
}
.report-head div { display: grid; gap: 4px; }
.report-title-row {
  display: flex;
  align-items: center;
  gap: 7px;
}
.ai-sparkle-icon {
  flex-shrink: 0;
  filter: drop-shadow(0 2px 6px rgba(99, 102, 241, 0.4));
}
.report-head strong {
  display: inline-flex;
  align-items: center;
  gap: 6.5px;
  color: #0f172a;
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: -0.01em;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
}
.status-badge.ready {
  color: #3b82f6;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  display: inline-block;
}
.status-badge.ready .status-dot {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}
.reanalyze-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7.5px 14px;
  border: none;
  border-radius: 9px;
  color: #ffffff;
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3);
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}
.reanalyze-btn .btn-icon {
  flex-shrink: 0;
  transition: transform 0.3s ease;
}
.reanalyze-btn .btn-icon.spin {
  animation: btnSpin 1s linear infinite;
}
@keyframes btnSpin {
  100% { transform: rotate(360deg); }
}
.reanalyze-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.45);
}
.reanalyze-btn:hover:not(:disabled) .btn-icon:not(.spin) {
  transform: rotate(180deg);
}
.reanalyze-btn:disabled { opacity: .55; cursor: default; }
.report-progress { padding: 10px 14px; border-bottom: 1px solid #d9e6fb; background: #f3f7ff; }
.report-progress > span { display: block; height: 5px; overflow: hidden; border-radius: 99px; background: #dbe7fb; }
.report-progress i { display: block; width: 100%; height: 100%; transform-origin: left; background: var(--report-accent); transition: transform 220ms ease; }
.report-progress p { margin: 7px 0 0; color: #52657d; font-size: 10px; line-height: 1.5; }
.report-empty { margin: 14px; padding: 14px; border: 1px solid #e1e7ef; border-radius: 9px; color: #697586; background: #fff; font-size: 11px; line-height: 1.65; }
.report-list { display: grid; gap: 10px; padding: 12px; }
.report-expanded-list { display: grid; gap: 14px; padding: 14px; }
.expanded-chapter { --tone: var(--report-accent); overflow: hidden; border: 1px solid color-mix(in srgb, var(--tone) 26%, #dbe3ee); border-radius: 12px; background: #fff; }
.expanded-chapter.is-synthesis { border-color: var(--report-accent-line); }
.expanded-chapter.tone-0 { --tone: #2f6df6; }
.expanded-chapter.tone-1 { --tone: #14a38b; }
.expanded-chapter.tone-2 { --tone: #8b5cf6; }
.expanded-chapter.tone-3 { --tone: #f97316; }
.expanded-chapter.tone-4 { --tone: #0ea5e9; }
.expanded-chapter.tone-5 { --tone: #e11d48; }
.expanded-chapter-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #e4e9f0;
  background: #f7f9fc;
}
.expanded-chapter.is-synthesis .expanded-chapter-head { border-bottom: 1px solid #dbe7fb; background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%); }
.expanded-chapter-head > span { display: grid; width: 26px; height: 26px; place-items: center; border: 1px solid color-mix(in srgb, var(--tone) 58%, transparent); border-radius: 50%; color: var(--tone); background: color-mix(in srgb, var(--tone) 10%, #ffffff); font-size: 10px; font-weight: 800; }
.expanded-chapter.is-synthesis .expanded-chapter-head > span { color: var(--report-accent); background: var(--report-accent-soft); }
.expanded-chapter-head div { display: grid; gap: 2px; }
.expanded-chapter-head strong { color: #263244; font-size: 13px; }
.expanded-chapter-head small { color: #7b8798; font-size: 9.5px; }
.expanded-chapter.is-synthesis .expanded-chapter-head strong { color: #12346f; }
.expanded-chapter.is-synthesis .expanded-chapter-head small { color: #5b6f91; }
.expanded-chapter-points { display: grid; gap: 10px; padding: 12px; }
.report-chapter { --tone: #4267b2; overflow: hidden; border: 1px solid color-mix(in srgb, var(--tone) 28%, #dfe3e8); border-radius: 11px; background: #fff; }
.report-chapter.is-synthesis { border-color: var(--report-accent-line); box-shadow: 0 6px 14px rgba(37, 88, 176, 0.09); }
.report-chapter.is-synthesis .chapter-toggle { background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%); }
.report-chapter.is-synthesis .chapter-index { color: var(--report-accent); background: var(--report-accent-soft); border-color: var(--report-accent-line); }
.report-chapter.is-synthesis .chapter-heading strong { color: #12346f; font-size: 14px; letter-spacing: 0.01em; }
.report-chapter.is-synthesis .chapter-heading small,
.report-chapter.is-synthesis .chapter-count { color: #5b6f91; }
.report-chapter.is-synthesis .chapter-arrow { color: var(--report-accent-dark); }
.report-chapter.tone-0 { --tone: #2f6df6; }
.report-chapter.tone-1 { --tone: #14a38b; }
.report-chapter.tone-2 { --tone: #8b5cf6; }
.report-chapter.tone-3 { --tone: #f97316; }
.report-chapter.tone-4 { --tone: #0ea5e9; }
.report-chapter.tone-5 { --tone: #e11d48; }
.chapter-toggle {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto 22px;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 11px 12px;
  border: 0;
  text-align: left;
  background: #fff;
  cursor: pointer;
}
.chapter-index { display: grid; place-items: center; width: 26px; height: 26px; border: 1px solid color-mix(in srgb, var(--tone) 58%, transparent); border-radius: 50%; color: var(--tone); background: color-mix(in srgb, var(--tone) 10%, #ffffff); font-size: 11px; font-weight: 800; }
.chapter-heading { display: grid; min-width: 0; gap: 2px; }
.chapter-heading strong { color: #273244; font-size: 12px; }
.chapter-heading small, .chapter-count { color: #8792a3; font-size: 9px; }
.chapter-arrow { color: #6d7a8d; font-size: 22px; line-height: 1; transition: transform 180ms ease; }
.chapter-arrow.open { transform: rotate(90deg); }
.chapter-content { display: grid; gap: 10px; padding: 0 12px 12px; }
.report-point {
  padding: 11px 12px;
  border: 1px solid color-mix(in srgb, var(--tone) 32%, #e2e8f0);
  border-radius: 9px;
  background: color-mix(in srgb, var(--tone) 5%, #ffffff);
  box-shadow: inset 3px 0 var(--tone);
}
.report-point.tone-0 { --tone: #2f6df6; }
.report-point.tone-1 { --tone: #14a38b; }
.report-point.tone-2 { --tone: #8b5cf6; }
.report-point.tone-3 { --tone: #f97316; }
.report-point.tone-4 { --tone: #0ea5e9; }
.report-point.tone-5 { --tone: #e11d48; }
.report-point > header {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr) auto 28px;
  align-items: center;
  gap: 8px;
}
.report-point > header > span,
.report-point li > span,
.expanded-card li > span {
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--tone) 58%, transparent);
  border-radius: 50%;
  color: var(--tone);
  background: color-mix(in srgb, var(--tone) 10%, #ffffff);
  font-weight: 800;
  line-height: 1;
}
.report-point > header > span {
  width: 24px;
  height: 24px;
  font-size: 10.5px;
}
.report-point > header strong {
  overflow: hidden;
  color: #263244;
  font-size: 12px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.report-point > header em {
  color: #8792a3;
  font-size: 9px;
  font-style: normal;
}
.report-point > header button {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--tone) 34%, #dce5ef);
  border-radius: 50%;
  color: var(--tone);
  background: #ffffff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
}
.point-lead {
  margin: 9px 0 0 34px;
  color: #566173;
  font-size: 11px;
  line-height: 1.65;
}
.report-point ol {
  display: grid;
  gap: 8px;
  margin: 10px 0 0 34px;
  padding: 0;
}
.report-point li {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: start;
  gap: 8px;
  min-width: 0;
}
.report-point li > span,
.expanded-card li > span {
  width: 20px;
  height: 20px;
  margin-top: 2px;
  border-color: var(--tone);
  color: #ffffff;
  background: var(--tone);
  font-size: 10px;
  box-shadow: 0 3px 8px color-mix(in srgb, var(--tone) 26%, transparent);
}
.report-point li p {
  min-width: 0;
  margin: 0;
  color: #4f5d70;
  font-size: 11px;
  line-height: 1.65;
}
.report-point.expanded-card {
  padding: 16px 18px;
  border-color: color-mix(in srgb, var(--tone) 32%, #dce5ef);
  border-radius: 12px;
  background: color-mix(in srgb, var(--tone) 4%, #ffffff);
}
.expanded-card > header { grid-template-columns: 28px minmax(0, 1fr) auto 30px; }
.expanded-card > header > span { width: 26px; height: 26px; font-size: 11px; }
.expanded-card header strong { font-size: 13px; }
.expanded-card .point-lead { margin: 10px 18px 0 37px; font-size: 12px; line-height: 1.7; }
.expanded-card ol { gap: 10px; margin: 12px 18px 0 37px; }
.expanded-card li { grid-template-columns: 22px minmax(0, 1fr); gap: 9px; }
.expanded-card.report-note {
  margin: 0 12px 14px;
  padding: 14px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.report-note header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.report-note header div { display: grid; gap: 3px; }
.report-note strong { font-size: 12px; font-weight: 700; color: #1e293b; }
.report-note small { color: #64748b; font-size: 10px; }

.report-note textarea {
  box-sizing: border-box;
  width: 100%;
  margin-top: 10px;
  padding: 10px 12px;
  resize: vertical;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  outline: none;
  color: #1e293b;
  background: #f8fafc;
  font: 11.5px/1.6 inherit;
  transition: all 0.2s ease;
}

.report-note textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
  background: #ffffff;
}

.report-note header button {
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
  transition: all 0.2s ease;
}

.report-note header button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.report-toast { position: fixed; bottom: 18px; left: 18px; z-index: 40; padding: 9px 12px; border-radius: 8px; color: #fff; background: #172033; font-size: 10px; }
.report-toast-enter-active, .report-toast-leave-active { transition: opacity 160ms ease, transform 180ms ease; }
.report-toast-enter-from, .report-toast-leave-to { opacity: 0; transform: translateY(6px); }
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { transition-duration: .01ms !important; }
}
/* ── DARK MODE ADAPTATIONS FOR READER REPORT PANEL ── */
:root[data-theme="dark"] .reader-report {
  color: #cbd5e1;
}

:root[data-theme="dark"] .review-scope > strong {
  color: #f8fafc !important;
}

:root[data-theme="dark"] .scope-label {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .scope-chip {
  background: rgba(99, 102, 241, 0.14) !important;
  border-color: rgba(129, 140, 248, 0.28) !important;
  color: #a5b4fc !important;
}

:root[data-theme="dark"] .scope-chip:hover {
  background: rgba(99, 102, 241, 0.25) !important;
  border-color: rgba(129, 140, 248, 0.5) !important;
  color: #c7d2fe !important;
}

:root[data-theme="dark"] .report-head strong {
  color: #f8fafc !important;
}

:root[data-theme="dark"] .status-badge {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .status-badge.ready {
  color: #60a5fa !important;
}

:root[data-theme="dark"] .report-progress {
  border-bottom-color: rgba(96, 165, 250, 0.16) !important;
  background:
    linear-gradient(180deg, rgba(37, 99, 235, 0.12), rgba(15, 23, 42, 0.62)),
    rgba(15, 23, 42, 0.72) !important;
}

:root[data-theme="dark"] .report-progress > span {
  background: rgba(30, 41, 59, 0.92) !important;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.12) !important;
}

:root[data-theme="dark"] .report-progress i {
  background: linear-gradient(90deg, #60a5fa, #3b82f6, #818cf8) !important;
  box-shadow: 0 0 14px rgba(96, 165, 250, 0.45) !important;
}

:root[data-theme="dark"] .report-progress p {
  color: #a9b8cc !important;
}

:root[data-theme="dark"] .expanded-chapter,
:root[data-theme="dark"] .report-chapter,
:root[data-theme="dark"] .report-empty {
  background: rgba(15, 23, 42, 0.65) !important;
  backdrop-filter: blur(16px) !important;
  border-color: color-mix(in srgb, var(--tone, #60a5fa) 38%, rgba(255, 255, 255, 0.12)) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3) !important;
  color: #e2e8f0 !important;
}

:root[data-theme="dark"] .expanded-chapter-head,
:root[data-theme="dark"] .chapter-toggle {
  background: #172033 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

:root[data-theme="dark"] .expanded-chapter-head strong,
:root[data-theme="dark"] .chapter-heading strong {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .expanded-chapter-head small,
:root[data-theme="dark"] .chapter-heading small,
:root[data-theme="dark"] .chapter-count {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .report-point {
  background: #141e2e !important;
  border-color: color-mix(in srgb, var(--tone) 38%, rgba(255, 255, 255, 0.08)) !important;
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .report-point.expanded-card {
  background: #101827 !important;
  border-color: color-mix(in srgb, var(--tone) 38%, rgba(255, 255, 255, 0.1)) !important;
}

:root[data-theme="dark"] .point-lead,
:root[data-theme="dark"] .expanded-card .point-lead {
  color: #e2e2e6 !important;
}

:root[data-theme="dark"] .report-point > header strong,
:root[data-theme="dark"] .report-point li p {
  color: #e2e8f0 !important;
}

:root[data-theme="dark"] .report-point > header em {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .report-point > header button {
  border-color: color-mix(in srgb, var(--tone) 46%, rgba(255, 255, 255, 0.1)) !important;
  color: color-mix(in srgb, var(--tone) 74%, #ffffff) !important;
  background: rgba(15, 23, 42, 0.72) !important;
}

:root[data-theme="dark"] .point-details li {
  color: #cbd5e1 !important;
}

:global(html[data-theme="dark"] body .chapter-index),
:global(html[data-theme="dark"] body .expanded-chapter-head > span),
:global(html[data-theme="dark"] body .report-point > header > span),
:global(html[data-theme="dark"] body .report-point li > span),
:global(html[data-theme="dark"] body .expanded-card li > span) {
  color: color-mix(in srgb, var(--tone, #60a5fa) 76%, #ffffff) !important;
  background: color-mix(in srgb, var(--tone, #60a5fa) 18%, #101827) !important;
  border: 1px solid color-mix(in srgb, var(--tone, #60a5fa) 62%, rgba(255, 255, 255, 0.08)) !important;
  border-radius: 50% !important;
  font-weight: 800 !important;
  box-shadow: none !important;
}

:global(html[data-theme="dark"] body .report-note) {
  background: rgba(15, 23, 42, 0.8) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px !important;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35) !important;
}

:global(html[data-theme="dark"] body .report-note strong) {
  color: #f8fafc !important;
  font-size: 12px !important;
  font-weight: 700 !important;
}

:global(html[data-theme="dark"] body .report-note small) {
  color: #94a3b8 !important;
  font-size: 10px !important;
}

:global(html[data-theme="dark"] body .report-note textarea) {
  background: rgba(30, 41, 59, 0.65) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: #f1f5f9 !important;
  font-size: 11.5px !important;
  border-radius: 8px !important;
}

:global(html[data-theme="dark"] body .report-note textarea::placeholder) {
  color: #64748b !important;
}

:global(html[data-theme="dark"] body .report-note textarea:focus) {
  border-color: #818cf8 !important;
  box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.2) !important;
  background: rgba(30, 41, 59, 0.9) !important;
}

:global(html[data-theme="dark"] body .report-note header button) {
  background: linear-gradient(135deg, #6366f1, #818cf8) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4) !important;
}

.report-point ol,
.report-point li {
  list-style: none !important;
  list-style-type: none !important;
}

:global(html[data-theme="dark"] body .chapter-index),
:global(html[data-theme="dark"] body .expanded-chapter-head > span),
:global(html[data-theme="dark"] body .report-point > header > span) {
  color: color-mix(in srgb, var(--tone, #60a5fa) 76%, #ffffff) !important;
  background: color-mix(in srgb, var(--tone, #60a5fa) 18%, #101827) !important;
  border: 1px solid color-mix(in srgb, var(--tone, #60a5fa) 62%, rgba(255, 255, 255, 0.08)) !important;
  border-radius: 50% !important;
  font-weight: 800 !important;
  box-shadow: none !important;
}

:global(html[data-theme="dark"] body .report-point li > span),
:global(html[data-theme="dark"] body .expanded-card li > span) {
  display: grid !important;
  width: 20px !important;
  height: 20px !important;
  min-width: 20px !important;
  margin-top: 2px !important;
  border-radius: 50% !important;
  color: #ffffff !important;
  background: var(--tone, #60a5fa) !important;
  border: 1px solid var(--tone, #60a5fa) !important;
  box-shadow: 0 4px 12px color-mix(in srgb, var(--tone, #60a5fa) 34%, transparent) !important;
}
</style>
