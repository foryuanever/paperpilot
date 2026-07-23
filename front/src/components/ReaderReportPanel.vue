<template>
  <div class="reader-report">
    <header class="report-head">
      <div>
        <strong>论文内容详解</strong>
        <span>{{ report.generated ? "AI 已分析完成" : "等待 AI 分析" }}</span>
      </div>
      <button :disabled="busy || !workspaceId" @click="generateReport">
        {{ busy ? `${progress}%` : report.generated ? "重新分析" : "开始分析" }}
      </button>
    </header>

    <section class="review-scope" aria-label="论文内容详解包含内容">
      <strong>{{ report.paper?.title || paper?.title || "当前论文" }}</strong>
      <p>内容详解包含</p>
      <div>
        <span v-for="item in reviewScope" :key="item">{{ item }}</span>
      </div>
    </section>

    <div v-if="busy" class="report-progress">
      <span><i :style="{ transform: `scaleX(${Math.max(.05, progress / 100)})` }"></i></span>
      <p>{{ progressMessage || "正在逐章精读论文，结果会自动保存。" }}</p>
    </div>

    <div v-if="!workspaceId" class="report-empty">
      当前论文缺少工作区编号，重新从文献库打开后即可生成七章分析。
    </div>

    <div v-else-if="wide" class="report-expanded-list">
      <article
        v-for="(chapter, chapterIndex) in displayChapters"
        :key="chapter.key"
        class="expanded-chapter"
        :class="{ 'is-synthesis': chapter.key === 'synthesis' }"
      >
        <header class="expanded-chapter-head">
          <span>{{ chapterIndex + 1 }}</span>
          <div>
            <strong>{{ chapter.short }}</strong>
            <small>{{ chapter.subtitle }} · {{ chapter.pointCount }} 点</small>
          </div>
        </header>
        <div class="expanded-chapter-points">
          <section
            v-for="(point, pointIndex) in chapter.blocks"
            :key="`${chapter.key}-${point.title}`"
            class="report-point expanded-card"
            :class="`tone-${(chapterIndex + pointIndex) % 6}`"
          >
            <header>
              <span>{{ pointIndex + 1 }}</span>
              <strong>{{ point.title }}</strong>
              <em>{{ point.items.length + (point.lead ? 1 : 0) }} 点</em>
              <button
                :title="`将${point.title}加入笔记`"
                :aria-label="`将${point.title}加入笔记`"
                @click.stop="addBlockToNote(point, chapter)"
              >＋</button>
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
          <span class="chapter-count">{{ chapter.pointCount }} 点</span>
          <span class="chapter-arrow" :class="{ open: expanded.has(chapter.key) }">›</span>
        </button>

        <div v-if="expanded.has(chapter.key)" class="chapter-content">
          <section
            v-for="(block, blockIndex) in chapter.blocks"
            :key="`${chapter.key}-${block.title}`"
            class="report-point"
            :class="`tone-${(chapterIndex + blockIndex) % 6}`"
          >
            <header>
              <span>{{ blockIndex + 1 }}</span>
              <strong>{{ block.title }}</strong>
              <em>{{ block.items.length + (block.lead ? 1 : 0) }} 点</em>
              <button
                :title="`将${block.title}加入笔记`"
                :aria-label="`将${block.title}加入笔记`"
                @click.stop="addBlockToNote(block, chapter)"
              >＋</button>
            </header>
            <p v-if="block.lead" class="point-lead">{{ block.lead }}</p>
            <ol v-if="block.items.length">
              <li v-for="item in block.items" :key="item">
                <span></span>
                <p>{{ item }}</p>
              </li>
            </ol>
          </section>
        </div>
      </article>
    </div>

    <section class="report-note">
      <header>
        <div>
          <strong>文献库笔记</strong>
          <small>点击分析小点右侧加号即可加入。</small>
        </div>
        <button :disabled="savingNote" @click="savePaperNote">{{ savingNote ? "保存中" : "保存" }}</button>
      </header>
      <textarea v-model="noteDraft" rows="5" placeholder="记录实验数据、图表页码或导师建议…"></textarea>
    </section>

    <Transition name="report-toast">
      <div v-if="toast" class="report-toast">{{ toast }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
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
const expanded = reactive(new Set(["synthesis", "basicInfo"]));
let pollTimer;
let toastTimer;

const chapters = [
  { key: "synthesis", short: "论文内容详解", subtitle: "六项核心解读" },
  { key: "basicInfo", short: "基本信息", subtitle: "论文身份" },
  { key: "overview", short: "文章概述", subtitle: "领域现状" },
  { key: "background", short: "研究背景", subtitle: "问题与动机" },
  { key: "method", short: "研究思路", subtitle: "整体框架" },
  { key: "results", short: "研究结果", subtitle: "量化与对比" },
  { key: "conclusion", short: "结论展望", subtitle: "优势与局限" },
  { key: "datasets", short: "数据集", subtitle: "数据与指标" },
];

const blockTitles = {
  synthesis: ["研究背景", "研究问题", "研究方法与数据", "实验与结论", "创新点与启示", "局限性"],
  basicInfo: ["论文定位", "发表信息", "汇报价值"],
  overview: ["核心要点", "研究问题与主要贡献"],
  background: ["关键问题", "本文思想与贡献"],
  method: ["整体框架", "关键模块与实现流程"],
  results: ["主要发现", "对比结果与实验结论"],
  conclusion: ["研究结论", "不足与未来展望"],
  datasets: ["数据来源与设置", "评测指标"],
};

const reviewScope = ["研究背景", "研究问题", "研究方法与数据", "实验与结论", "创新点与启示", "局限性"];

const report = reactive({
  paper: { title: "", abstract: "", note: "" },
  sections: Object.fromEntries(chapters.map(chapter => [chapter.key, ""])),
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
    pointCount: blocks.reduce((sum, block) => sum + block.items.length + (block.lead ? 1 : 0), 0),
  };
}));

function cleanLine(line) {
  return String(line || "")
    .replace(/\\[rnt]/g, " ")
    .replace(/^[\-•·○◦▪▫\d.、\s]+/, "")
    .replace(/(?:在)?汇报时可(?:以)?[^。！？；]*[。！？；]?/g, "")
    .replace(/[{}"“”]+/g, "")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function isMeaningfulLine(line) {
  const value = cleanLine(line);
  if (value.length <= 4) return false;
  if (/^(?:要点|概述|总结|分析|论文定位|发表信息|发布信息|汇报价值|研究背景|研究问题|研究方法与数据|实验与结论|创新点与启示|局限性|核心要点|主要贡献|关键问题|本文思想|关键贡献|整体框架|关键模块|实现流程|主要发现|对比结果|研究结论|现有不足|未来展望|数据来源|数据设置|评测指标)\s*[：:]?$/.test(value)) {
    return false;
  }
  return !/^[\u4e00-\u9fa5A-Za-z0-9与及、\s]{2,20}[：:]$/.test(value);
}

function sectionLines(key) {
  const raw = report.sections[key] || (key === "basicInfo" ? report.paper.abstract : "") || "";
  return raw
    .split(/\n+|(?<=[。！？；])\s*/)
    .map(cleanLine)
    .filter(line => isMeaningfulLine(line) && !/等待 AI|原文未明确|HTTP\s*5/.test(line));
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
  const titles = blockTitles[chapter.key];
  const fallback = sectionLines(chapter.key);
  return titles.map((title, index) => {
    const parsed = extractBlock(report.sections[chapter.key], title, titles);
    const mappedFallback = chapter.key === "synthesis" ? synthesisFallback(title) : [];
    const distributedFallback = fallback.filter((_, lineIndex) => lineIndex % titles.length === index);
    const lines = dedupe(parsed.length ? parsed : mappedFallback.length ? mappedFallback : distributedFallback).slice(0, 4);
    return {
      title,
      lead: lines[0] || (report.generated ? "暂未从论文中提取到这一项，请点击重新分析。" : "等待 AI 精读后生成内容。"),
      items: lines.slice(1),
    };
  });
}

function synthesisFallback(title) {
  const sources = {
    研究背景: ["background", "overview"],
    研究问题: ["background", "overview"],
    研究方法与数据: ["method", "datasets"],
    实验与结论: ["results", "conclusion"],
    创新点与启示: ["overview", "results", "conclusion"],
    局限性: ["conclusion"],
  };
  const hints = {
    研究背景: /背景|现状|动机|领域|需求/,
    研究问题: /问题|目标|假设|挑战|缺口/,
    研究方法与数据: /方法|框架|模型|数据|样本|指标|训练/,
    实验与结论: /实验|结果|发现|结论|提升|对比/,
    创新点与启示: /创新|贡献|价值|启示|优势|首次/,
    局限性: /局限|不足|限制|未来|未能|尚未/,
  };
  const pool = (sources[title] || []).flatMap(sectionLines);
  const preferred = pool.filter(line => hints[title]?.test(line));
  if (preferred.length) return preferred;
  if (title === "研究背景" || title === "研究问题") {
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
    const hasContent = Object.values(report.sections || {}).some(value => cleanLine(value).length > 12);
    if (report.generated && !hasContent && !busy.value) {
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
  progressMessage.value = "正在后台精读全文并生成七章分析。";
  try {
    const data = await paperpilotApi.generateMeetingReport(props.workspaceId);
    updateStatus(data);
    startPolling();
  } catch {
    busy.value = false;
    showToast("AI 分析启动失败，请检查模型配置");
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
    if (showDone) showToast("七章分析已更新");
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
  padding: 10px 16px 14px;
  border-bottom: 0;
  background: transparent;
}
.review-scope > strong {
  display: -webkit-box;
  overflow: hidden;
  color: #263244;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.review-scope p { margin: 8px 0 6px; color: #7b8798; font-size: 10px; }
.review-scope div { display: flex; flex-wrap: wrap; gap: 5px; }
.review-scope span {
  padding: 3px 7px;
  border-radius: 999px;
  color: #2451a6;
  background: #eef5ff;
  font-size: 9.5px;
  line-height: 1.4;
}
.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px 8px;
  border-bottom: 0;
  background: transparent;
}
.report-head div { display: grid; gap: 3px; }
.report-head strong { color: #182230; font-size: 13px; }
.report-head span { color: #7b8798; font-size: 10px; }
.report-head button,
.report-note button {
  padding: 7px 10px;
  border: 1px solid var(--report-accent);
  border-radius: 7px;
  color: #fff;
  background: var(--report-accent);
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
}
.report-head button:disabled { opacity: .5; cursor: default; }
.report-progress { padding: 10px 14px; border-bottom: 1px solid #d9e6fb; background: #f3f7ff; }
.report-progress > span { display: block; height: 5px; overflow: hidden; border-radius: 99px; background: #dbe7fb; }
.report-progress i { display: block; width: 100%; height: 100%; transform-origin: left; background: var(--report-accent); transition: transform 220ms ease; }
.report-progress p { margin: 7px 0 0; color: #52657d; font-size: 10px; line-height: 1.5; }
.report-empty { margin: 14px; padding: 14px; border: 1px solid #e1e7ef; border-radius: 9px; color: #697586; background: #fff; font-size: 11px; line-height: 1.65; }
.report-list { display: grid; gap: 10px; padding: 12px; }
.report-expanded-list { display: grid; gap: 14px; padding: 14px; }
.expanded-chapter { overflow: hidden; border: 1px solid #dbe3ee; border-radius: 12px; background: #fff; }
.expanded-chapter.is-synthesis { border-color: var(--report-accent-line); }
.expanded-chapter-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #e4e9f0;
  background: #f7f9fc;
}
.expanded-chapter.is-synthesis .expanded-chapter-head { border-bottom: 1px solid #dbe7fb; background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%); }
.expanded-chapter-head > span { display: grid; width: 26px; height: 26px; place-items: center; border-radius: 8px; color: var(--report-accent-dark); background: var(--report-accent-soft); font-size: 10px; font-weight: 800; }
.expanded-chapter.is-synthesis .expanded-chapter-head > span { color: #fff; background: var(--report-accent); }
.expanded-chapter-head div { display: grid; gap: 2px; }
.expanded-chapter-head strong { color: #263244; font-size: 13px; }
.expanded-chapter-head small { color: #7b8798; font-size: 9.5px; }
.expanded-chapter.is-synthesis .expanded-chapter-head strong { color: #12346f; }
.expanded-chapter.is-synthesis .expanded-chapter-head small { color: #5b6f91; }
.expanded-chapter-points { display: grid; gap: 10px; padding: 12px; }
.report-chapter { --tone: #4267b2; overflow: hidden; border: 1px solid #dfe3e8; border-radius: 11px; background: #fff; }
.report-chapter.is-synthesis { border-color: var(--report-accent-line); box-shadow: 0 6px 14px rgba(37, 88, 176, 0.09); }
.report-chapter.is-synthesis .chapter-toggle { background: linear-gradient(180deg, #f7fbff 0%, #eef5ff 100%); }
.report-chapter.is-synthesis .chapter-index { color: #fff; background: var(--report-accent); }
.report-chapter.is-synthesis .chapter-heading strong { color: #12346f; font-size: 14px; letter-spacing: 0.01em; }
.report-chapter.is-synthesis .chapter-heading small,
.report-chapter.is-synthesis .chapter-count { color: #5b6f91; }
.report-chapter.is-synthesis .chapter-arrow { color: var(--report-accent-dark); }
.report-chapter.tone-1 { --tone: #4267b2; }
.report-chapter.tone-2 { --tone: #54739e; }
.report-chapter.tone-3 { --tone: #356a8f; }
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
.chapter-index { display: grid; place-items: center; width: 26px; height: 26px; border-radius: 8px; color: var(--tone); background: rgba(99, 102, 241, 0.08); font-size: 11px; font-weight: 800; }
.chapter-heading { display: grid; min-width: 0; gap: 2px; }
.chapter-heading strong { color: #273244; font-size: 12px; }
.chapter-heading small, .chapter-count { color: #8792a3; font-size: 9px; }
.chapter-arrow { color: #6d7a8d; font-size: 22px; line-height: 1; transition: transform 180ms ease; }
.chapter-arrow.open { transform: rotate(90deg); }
.chapter-content { display: grid; gap: 10px; padding: 0 12px 12px; }
.report-point {
  padding: 11px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: #fbfcfe;
  box-shadow: inset 3px 0 var(--tone);
}
.report-point.tone-0 { --tone: #2f6df6; }
.report-point.tone-1 { --tone: #14a38b; }
.report-point.tone-2 { --tone: #8b5cf6; }
.report-point.tone-3 { --tone: #f97316; }
.report-point.tone-4 { --tone: #0ea5e9; }
.report-point.tone-5 { --tone: #e11d48; }
.report-point.expanded-card {
  padding: 16px 18px;
  border-color: #dce5ef;
  border-radius: 12px;
  background: #fff;
}
.expanded-card > header { grid-template-columns: 28px minmax(0, 1fr) auto 30px; }
.expanded-card > header > span { width: 26px; height: 26px; border-radius: 8px; font-size: 11px; }
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
  color: #f4f4f6;
}

:root[data-theme="dark"] .review-scope p {
  color: #a1a1aa;
}

:root[data-theme="dark"] .review-scope span {
  background: rgba(59, 130, 246, 0.15);
  color: #60a5fa;
}

:root[data-theme="dark"] .report-head strong {
  color: #f4f4f6;
}

:root[data-theme="dark"] .report-head span {
  color: #a1a1aa;
}

:root[data-theme="dark"] .expanded-chapter,
:root[data-theme="dark"] .report-chapter,
:root[data-theme="dark"] .report-empty {
  background: #101827 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: #e2e2e6 !important;
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
  border-color: rgba(255, 255, 255, 0.08) !important;
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .report-point.expanded-card {
  background: #101827 !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

:root[data-theme="dark"] .point-lead,
:root[data-theme="dark"] .expanded-card .point-lead {
  color: #e2e2e6 !important;
}

:root[data-theme="dark"] .point-details li {
  color: #cbd5e1 !important;
}

:global(html[data-theme="dark"] body .chapter-index),
:global(html[data-theme="dark"] body .expanded-chapter-head > span),
:global(html[data-theme="dark"] body .report-point > header > span),
:global(html[data-theme="dark"] body .report-point li > span),
:global(html[data-theme="dark"] body .expanded-card li > span) {
  color: #ffffff !important;
  background: rgba(99, 102, 241, 0.45) !important;
  border: 1px solid rgba(165, 180, 252, 0.5) !important;
  font-weight: 800 !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.35) !important;
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
  color: #ffffff !important;
  background: rgba(99, 102, 241, 0.45) !important;
  border: 1px solid rgba(165, 180, 252, 0.5) !important;
  font-weight: 800 !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.35) !important;
}

:global(html[data-theme="dark"] body .report-point li > span),
:global(html[data-theme="dark"] body .expanded-card li > span) {
  display: inline-block !important;
  width: 6px !important;
  height: 6px !important;
  min-width: 6px !important;
  margin-top: 7px !important;
  border-radius: 50% !important;
  background: #818cf8 !important;
  box-shadow: 0 0 8px rgba(129, 140, 248, 0.7) !important;
  border: none !important;
}
</style>
