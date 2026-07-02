<template>
  <div class="meeting-page">
    <header class="meeting-topbar">
      <div class="title-cluster">
        <span class="section-label">组会汇报</span>
        <h1>主论文精读，对比文献做参照</h1>
        <p>上方选择 3-5 篇对比文献，用来生成 AI 对比矩阵；真正要汇报的主论文在 PPT 参数里单独上传。</p>
      </div>
      <button type="button" class="primary-action" :disabled="selectedPapers.length >= maxPapers" @click="openLibraryPicker">
        添加对比文献
      </button>
    </header>

    <main class="meeting-shell">
      <section class="paper-flow">
        <div class="flow-head">
          <div>
            <span class="section-label">Paper Set</span>
            <h2>对比文献</h2>
          </div>
          <div class="flow-count" :data-ready="canGenerate">
            <strong>{{ selectedPapers.length }}</strong>
            <span>/ 5</span>
          </div>
        </div>

        <div class="paper-lanes" :class="{ empty: !selectedPapers.length }">
          <article v-for="paper in selectedPapers" :key="paper.id" class="paper-lane">
            <div class="lane-number">{{ selectedPapers.indexOf(paper) + 1 }}</div>
            <div class="lane-content">
              <h3>{{ paper.title }}</h3>
              <p>{{ compactMeta(paper) }}</p>
              <div class="tag-row">
                <span v-for="tag in displayTags(paper)" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <button type="button" class="quiet-button" @click="removePaper(paper.id)">移除</button>
          </article>

          <button v-if="selectedPapers.length < maxPapers" type="button" class="paper-add-lane" @click="openLibraryPicker">
            <span>+</span>
            <strong>{{ selectedPapers.length ? "继续添加" : "从文献库选择对比文献" }}</strong>
            <small>{{ selectedPapers.length ? "最多 5 篇，不等于主论文" : "不会自动填入演示数据" }}</small>
          </button>
        </div>
      </section>

      <section class="matrix-area">
        <div class="matrix-toolbar">
          <div>
            <span class="section-label">AI Comparison Matrix</span>
            <h2>内容对比工作表</h2>
            <p>不是题录表。点击生成后，AI 会从论文内容里提炼每个维度的可汇报结论。</p>
          </div>
          <div class="matrix-actions">
            <button type="button" class="secondary-action" @click="showDimensionPanel = !showDimensionPanel">
              {{ showDimensionPanel ? "收起维度" : "调整维度" }}
            </button>
            <button type="button" class="primary-action" :disabled="!canAnalyze || analyzing" @click="generateAiComparison">
              {{ analyzing ? "AI 分析中..." : "生成 AI 对比" }}
            </button>
          </div>
        </div>

        <div v-if="showDimensionPanel" class="dimension-strip">
          <label v-for="dimension in dimensions" :key="dimension.key" :class="{ checked: selectedDimensionKeys.includes(dimension.key) }">
            <input v-model="selectedDimensionKeys" type="checkbox" :value="dimension.key" />
            <span>{{ dimension.label }}</span>
          </label>
        </div>

        <div class="matrix-frame">
          <table class="comparison-table">
            <thead>
              <tr>
                <th>分析角度</th>
                <th v-for="paper in selectedPapers" :key="paper.id">
                  <span>{{ paper.title }}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!selectedPapers.length">
                <td colspan="2" class="empty-matrix">
                  <strong>先添加论文，再生成 AI 对比。</strong>
                  <span>矩阵不会展示作者、年份这类简单字段，而是用于组会讲解的内容分析。</span>
                </td>
              </tr>
              <tr v-else-if="!analysisReady">
                <td :colspan="selectedPapers.length + 1" class="empty-matrix">
                  <strong>{{ canAnalyze ? "论文已就绪，等待 AI 分析。" : "至少选择 3 篇论文后才能生成 AI 对比。" }}</strong>
                  <span>生成后这里会展示研究问题、方法路线、实验设计、贡献、局限等维度。</span>
                </td>
              </tr>
              <tr v-for="dimension in analysisReady ? selectedDimensions : []" :key="dimension.key">
                <th>
                  <strong>{{ dimension.label }}</strong>
                  <small>{{ dimension.hint }}</small>
                </th>
                <td v-for="paper in selectedPapers" :key="`${dimension.key}-${paper.id}`">
                  <span v-if="analysisCellState(paper, dimension.key) === 'ready'" class="cell-value">
                    {{ analysisValue(paper, dimension.key) }}
                  </span>
                  <span v-else-if="analysisCellState(paper, dimension.key) === 'insufficient'" class="insufficient-value">
                    {{ dimension.key === firstSelectedDimensionKey ? "材料不足：请补充 PDF、摘要或笔记后重新生成。" : "—" }}
                  </span>
                  <span v-else class="missing-value">AI 未返回该项</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="deck-dock">
        <div class="dock-status" :class="{ ready: canGenerate }">
          <span>{{ canGenerate ? "Ready" : "Draft" }}</span>
          <strong>{{ canGenerate ? "可以生成 PPT" : "还差论文" }}</strong>
          <small>{{ canGenerate ? "主论文需在详细参数里单独上传" : "至少选择 3 篇对比文献" }}</small>
        </div>

        <div class="template-row">
          <button
            v-for="template in templates"
            :key="template.id"
            type="button"
            class="template-tile"
            :class="{ active: selectedTemplateId === template.id }"
            @click="selectedTemplateId = template.id"
          >
            <span class="template-cover" :class="template.previewClass">
              <i></i><i></i><i></i>
            </span>
            <span class="template-copy">
              <strong>{{ template.name }}</strong>
              <small>{{ template.description }}</small>
            </span>
          </button>
        </div>

        <div class="dock-actions">
          <button type="button" class="secondary-action" @click="settingsOpen = true">详细参数</button>
          <button type="button" class="primary-action" :disabled="!canSubmitDeck || generating" @click="generateDeck">
            {{ generating ? `${Math.round(deckJob.progress || 1)}%` : "生成 PPT" }}
          </button>
          <small class="upload-hint">{{ reportPaperFile ? `主论文：${reportPaperFile.name}` : "需单独上传一篇汇报主论文" }}</small>
        </div>

        <div v-if="generating || deckJob.jobId" class="deck-progress" :data-status="deckJob.status">
          <div class="progress-head">
            <strong>{{ deckJob.stage || "PPT Master" }}</strong>
            <span>{{ Math.round(deckJob.progress || 0) }}%</span>
          </div>
          <div class="progress-track" aria-hidden="true">
            <i :style="{ width: `${Math.max(2, deckJob.progress || 0)}%` }"></i>
          </div>
          <small>{{ deckJob.message || "正在准备生成任务" }}</small>
        </div>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="settingsOpen" class="settings-backdrop" @click.self="settingsOpen = false">
        <section class="settings-modal" role="dialog" aria-modal="true" aria-labelledby="settings-title">
          <header>
            <div>
              <span class="section-label">PPT 生成参数</span>
              <h2 id="settings-title">详细生成设置</h2>
            </div>
            <button type="button" aria-label="关闭参数设置" @click="settingsOpen = false">×</button>
          </header>

          <div class="settings-grid">
            <label>
              <span>Academic 流程</span>
              <select v-model="pptSettings.generationMode">
                <option value="academic_ppt_master">Academic PPT Master</option>
                <option value="academic_beautify">论文结构美化</option>
                <option value="academic_from_scratch">从论文重构汇报</option>
              </select>
            </label>
            <label>
              <span>画布比例</span>
              <select v-model="pptSettings.aspectRatio">
                <option value="16:9">16:9 宽屏</option>
                <option value="4:3">4:3 标准</option>
              </select>
            </label>
            <label>
              <span>页数</span>
              <select v-model="pptSettings.slideCount">
                <option value="6">6 页</option>
                <option value="8-10">8-10 页</option>
                <option value="10-12">10-12 页</option>
                <option value="12-15">12-15 页</option>
                <option value="15-18">15-18 页</option>
              </select>
            </label>
            <label>
              <span>汇报时长</span>
              <select v-model="pptSettings.duration">
                <option value="8 分钟">8 分钟</option>
                <option value="10 分钟">10 分钟</option>
                <option value="15 分钟">15 分钟</option>
                <option value="20 分钟">20 分钟</option>
              </select>
            </label>
            <label>
              <span>汇报对象</span>
              <select v-model="pptSettings.audience">
                <option value="导师与课题组">导师与课题组</option>
                <option value="论文精读小组">论文精读小组</option>
                <option value="开题预汇报">开题预汇报</option>
                <option value="项目评审">项目评审</option>
              </select>
            </label>
            <label>
              <span>叙事模式</span>
              <select v-model="pptSettings.languageTone">
                <option value="Background-Method-Results-Outlook">Background → Method → Results → Outlook</option>
                <option value="problem-method-evidence">Problem → Method → Evidence</option>
                <option value="defense-style">答辩式：问题-贡献-验证</option>
                <option value="journal-club">组会精读式</option>
              </select>
            </label>
            <label>
              <span>视觉风格</span>
              <select v-model="pptSettings.visualStyle">
                <option value="academic_editorial">Academic Editorial</option>
                <option value="journal_minimal">Journal Minimal</option>
                <option value="conference_blue">Conference Blue</option>
                <option value="dark_lab">Dark Lab</option>
              </select>
            </label>
            <label>
              <span>输出内容密度</span>
              <select v-model="pptSettings.density">
                <option value="中等密度">中等密度</option>
                <option value="高密度">高密度</option>
                <option value="少字讲解">少字讲解</option>
              </select>
            </label>
            <label>
              <span>图表/公式策略</span>
              <select v-model="pptSettings.imageMode">
                <option value="preserve_paper_assets">保留论文公式、图、表线索</option>
                <option value="redraw_figures">重画方法图/结果图</option>
                <option value="text_only">只生成文字版</option>
              </select>
            </label>
            <label>
              <span>演讲备注</span>
              <select v-model="pptSettings.notesMode">
                <option value="speaker_notes">写入备注页</option>
                <option value="brief_notes">简短备注</option>
                <option value="none">不写备注</option>
              </select>
            </label>
            <label>
              <span>导出策略</span>
              <select v-model="pptSettings.animation">
                <option value="native_editable">原生可编辑 PPTX</option>
                <option value="native_with_svg_snapshot">原生 PPTX + SVG 预览备份</option>
                <option value="strict_line_fidelity">严格行布局 fidelity</option>
              </select>
            </label>
            <label class="field-wide">
              <span>汇报主论文 *</span>
              <div class="report-upload">
                <input type="file" accept="application/pdf,.pdf,.doc,.docx" @change="selectReportPaper" />
                <strong>{{ reportPaperFile?.name || "上传本次真正要讲的论文文件" }}</strong>
                <small>这篇论文会作为 PPT 主线；上方 3-5 篇只作为对比文献，不会替代主论文。</small>
              </div>
            </label>
            <label class="field-wide">
              <span>汇报重点</span>
              <textarea v-model="pptSettings.focus" rows="4" placeholder="例如：重点比较方法差异、数据集、实验指标和对本课题的启发。"></textarea>
            </label>
            <label class="field-wide">
              <span>Academic 章节结构</span>
              <div class="checkbox-grid">
                <label v-for="section in pptSections" :key="section">
                  <input v-model="pptSettings.sections" type="checkbox" :value="section" />
                  <span>{{ section }}</span>
                </label>
              </div>
            </label>
            <label class="field-wide">
              <span>质量检查</span>
              <div class="report-upload compact">
                <span class="inline-toggle">
                  <input v-model="pptSettings.visualReview" type="checkbox" />
                  <strong>生成后进行逐页视觉自检</strong>
                </span>
                <small>对应 PPT Master skill 的 visual check / post-process 阶段，会优先修复重叠、溢出和图表错位。</small>
              </div>
            </label>
            <label class="field-wide">
              <span>对比文献附录</span>
              <div class="report-upload compact">
                <span class="inline-toggle">
                  <input v-model="pptSettings.includeComparisonAppendix" type="checkbox" />
                  <strong>在末尾追加 comparison appendix</strong>
                </span>
                <small>默认关闭；主线仍按 Background / Methodology / Results / Outlook 汇报主论文。</small>
              </div>
            </label>
            <label class="field-wide">
              <span>额外要求</span>
              <textarea v-model="pptSettings.extraInstructions" rows="4" placeholder="例如：每篇论文都要给出一句可在组会上讨论的问题；避免编造实验数值。"></textarea>
            </label>
          </div>

          <footer>
            <button type="button" class="secondary-action" @click="settingsOpen = false">取消</button>
            <button type="button" class="primary-action" @click="settingsOpen = false">保存参数</button>
          </footer>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="libraryPickerOpen" class="picker-backdrop" @click.self="libraryPickerOpen = false">
        <section class="paper-picker" role="dialog" aria-modal="true" aria-labelledby="paper-picker-title">
          <header>
            <div>
              <span class="section-label">Library</span>
              <h2 id="paper-picker-title">选择组会论文</h2>
            </div>
            <button type="button" aria-label="关闭" @click="libraryPickerOpen = false">×</button>
          </header>

          <div class="picker-toolbar">
            <input v-model="pickerKeyword" type="search" placeholder="搜索标题、作者、来源" />
            <span>{{ filteredLibraryPapers.length }} 篇</span>
          </div>

          <div class="picker-list">
            <article v-for="paper in filteredLibraryPapers" :key="paper.id" class="picker-row">
              <div>
                <h3>{{ paper.title }}</h3>
                <p>{{ compactMeta(paper) }}</p>
                <div class="tag-row">
                  <span v-for="tag in displayTags(paper)" :key="tag">{{ tag }}</span>
                </div>
              </div>
              <button type="button" :disabled="isPaperSelected(paper.id) || selectedPapers.length >= maxPapers" @click="addPaper(paper)">
                {{ isPaperSelected(paper.id) ? "已添加" : "添加" }}
              </button>
            </article>
            <div v-if="!filteredLibraryPapers.length" class="picker-empty">文献库没有匹配结果。</div>
          </div>
        </section>
      </div>
    </Teleport>

    <Transition name="slide-up">
      <div v-if="toastMessage" class="custom-toast meeting-toast">
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useLibraryStore } from "../stores/library";
import { paperpilotApi } from "../services/paperpilotApi";
import { API_BASE_URL } from "../services/apiClient";

const libraryStore = useLibraryStore();
const maxPapers = 5;
const selectedPaperIds = ref([]);
const libraryPickerOpen = ref(false);
const settingsOpen = ref(false);
const pickerKeyword = ref("");
const showDimensionPanel = ref(false);
const selectedTemplateId = ref("journal-club");
const generating = ref(false);
const analyzing = ref(false);
const reportPaperFile = ref(null);
const analysisMatrix = ref({});
const analysisReady = ref(false);
const toastMessage = ref("");
const deckJob = reactive({
  jobId: "",
  status: "idle",
  progress: 0,
  stage: "",
  message: "",
  downloadUrl: "",
});
let toastTimer = null;
let deckPollTimer = null;

const dimensions = [
  { key: "researchProblem", label: "研究问题", hint: "解决什么问题，为什么重要" },
  { key: "method", label: "方法路线", hint: "核心模型、框架或技术路径" },
  { key: "dataExperiment", label: "数据与实验", hint: "数据、任务、指标与实验设置" },
  { key: "results", label: "结果证据", hint: "主要发现、对比结果与支撑证据" },
  { key: "contribution", label: "贡献与创新", hint: "相对已有工作的增量" },
  { key: "limitation", label: "局限与风险", hint: "假设、边界、缺陷和复现风险" },
  { key: "discussion", label: "组会讨论点", hint: "值得提问或延伸的方向" },
];

const selectedDimensionKeys = ref(dimensions.map((item) => item.key));

const templates = [
  { id: "journal-club", name: "主论文精读型", description: "围绕上传主论文展开，适合正式组会汇报。", previewClass: "journal" },
  { id: "roadmap", name: "研究脉络型", description: "按问题背景、方法路径和结论组织。", previewClass: "roadmap" },
  { id: "minimal", name: "极简汇报型", description: "8-10 页快速讲清主要贡献和局限。", previewClass: "minimal" },
];

const pptSections = ["Cover", "Background", "Methodology", "Experiment", "Results", "Conclusion", "Outlook", "Discussion"];
const pptSettings = reactive({
  generationMode: "academic_ppt_master",
  aspectRatio: "16:9",
  slideCount: "10-12",
  duration: "10 分钟",
  audience: "导师与课题组",
  languageTone: "Background-Method-Results-Outlook",
  visualStyle: "academic_editorial",
  density: "中等密度",
  imageMode: "preserve_paper_assets",
  notesMode: "speaker_notes",
  animation: "native_editable",
  focus: "",
  visualReview: true,
  includeComparisonAppendix: false,
  sections: ["Cover", "Background", "Methodology", "Experiment", "Results", "Conclusion", "Outlook"],
  extraInstructions: "按 PPT Master skill 工作流：保留论文公式、图、表线索；按 Background / Methodology / Experiment / Results / Conclusion / Outlook 重构；避免编造实验数值。",
});

const selectedTemplate = computed(() => templates.find((item) => item.id === selectedTemplateId.value) || templates[0]);
const selectedPapers = computed(() =>
  selectedPaperIds.value
    .map((id) => libraryStore.state.documents.find((paper) => paper.id === id))
    .filter(Boolean),
);
const selectedDimensions = computed(() =>
  dimensions.filter((dimension) => selectedDimensionKeys.value.includes(dimension.key)),
);
const firstSelectedDimensionKey = computed(() => selectedDimensions.value[0]?.key || "");
const filteredLibraryPapers = computed(() => {
  const keyword = pickerKeyword.value.trim().toLowerCase();
  return libraryStore.state.documents.filter((paper) => {
    if (!keyword) return true;
    return [paper.title, paper.authors, paper.source, paper.importSource, paper.publishYear]
      .some((field) => String(field || "").toLowerCase().includes(keyword));
  });
});
const canGenerate = computed(() => selectedPapers.value.length >= 3 && selectedPapers.value.length <= 5);
const canAnalyze = computed(() => canGenerate.value && selectedDimensions.value.length > 0);
const canSubmitDeck = computed(() => canGenerate.value && Boolean(reportPaperFile.value));

watch(selectedPaperIds, () => {
  analysisMatrix.value = {};
  analysisReady.value = false;
});

onMounted(async () => {
  try {
    await libraryStore.hydrateLibrary();
  } catch (error) {
    console.warn("Failed to hydrate library", error);
    showToast("文献库同步失败，已使用本地缓存");
  }
});

onBeforeUnmount(() => {
  stopDeckPolling();
  if (toastTimer) clearTimeout(toastTimer);
});

function openLibraryPicker() {
  if (selectedPapers.value.length >= maxPapers) return;
  libraryPickerOpen.value = true;
}

function addPaper(paper) {
  if (selectedPaperIds.value.includes(paper.id) || selectedPaperIds.value.length >= maxPapers) return;
  selectedPaperIds.value.push(paper.id);
  if (selectedPaperIds.value.length >= maxPapers) libraryPickerOpen.value = false;
}

function removePaper(id) {
  selectedPaperIds.value = selectedPaperIds.value.filter((paperId) => paperId !== id);
}

function isPaperSelected(id) {
  return selectedPaperIds.value.includes(id);
}

function selectReportPaper(event) {
  reportPaperFile.value = event.target.files?.[0] || null;
}

function displayTags(paper) {
  const tags = Array.isArray(paper.journalTags) ? paper.journalTags.filter(Boolean) : [];
  return (tags.length ? tags : [cleanValue(paper.venueType), cleanValue(paper.importSource) || cleanValue(paper.source)]).filter(Boolean).slice(0, 3);
}

function compactMeta(paper) {
  return [cleanValue(paper.authors), cleanValue(paper.source), cleanValue(paper.publishYear)].filter(Boolean).join(" · ") || "元数据待补充";
}

function analysisValue(paper, key) {
  return String(analysisMatrix.value?.[paper.id]?.[key] || "").trim();
}

function analysisCellState(paper, key) {
  const value = analysisValue(paper, key);
  if (!value) return "missing";
  const insufficientPhrases = ["论文材料未明确说明", "材料未明确", "信息不足", "未提供足够信息", "无法判断"];
  return insufficientPhrases.some((phrase) => value.includes(phrase)) ? "insufficient" : "ready";
}

function cleanValue(value) {
  const text = String(value || "").replace(/\s+/g, " ").trim();
  if (!text) return "";
  const placeholders = ["尚未添加标注", "摘要待补充", "待补全", "待补充", "元数据待补全"];
  return placeholders.some((placeholder) => text.includes(placeholder)) ? "" : text;
}

async function generateAiComparison() {
  if (!canAnalyze.value) {
    showToast("请先选择 3-5 篇论文");
    return;
  }
  analyzing.value = true;
  try {
    const result = await paperpilotApi.analyzeMeetingDeck({
      paperIds: selectedPaperIds.value,
      dimensions: selectedDimensions.value,
    });
    analysisMatrix.value = result?.matrix || {};
    analysisReady.value = true;
    showToast(result?.message || "AI 对比已生成");
  } catch (error) {
    console.warn("AI comparison failed", error);
    showToast("AI 对比生成失败，请检查模型配置");
  } finally {
    analyzing.value = false;
  }
}

async function generateDeck() {
  if (!canGenerate.value) {
    showToast("请先选择 3-5 篇论文");
    return;
  }
  if (!reportPaperFile.value) {
    showToast("请先在详细参数里上传一篇汇报论文");
    settingsOpen.value = true;
    return;
  }
  generating.value = true;
  stopDeckPolling();
  applyDeckJob({
    jobId: "",
    status: "running",
    progress: 1,
    stage: "提交任务",
    message: "正在提交 PPT Master 生成任务",
  });
  let startedJob = false;
  try {
    const payload = {
      paperIds: selectedPaperIds.value,
      dimensions: selectedDimensions.value,
      analysisMatrix: analysisMatrix.value,
      template: selectedTemplate.value,
      pptSettings: { ...pptSettings },
      slideCount: pptSettings.slideCount,
      audience: pptSettings.audience,
      focus: pptSettings.focus,
      engine: "ppt-master-skill",
    };
    payload.pptSettings.includeComparisonAppendix = Boolean(pptSettings.includeComparisonAppendix);
    const formData = new FormData();
    formData.append("payload", JSON.stringify(payload));
    formData.append("reportPaper", reportPaperFile.value);
    const result = await paperpilotApi.generateMeetingDeck(formData);
    applyDeckJob(result);
    if (result?.status === "generated") {
      if (result.downloadUrl) {
        window.open(absoluteApiUrl(result.downloadUrl), "_blank");
      }
      showToast("PPT 已生成，正在打开下载链接");
      generating.value = false;
    } else if (result?.jobId) {
      startedJob = true;
      showToast(result?.message || "PPT Master 任务已开始");
      scheduleDeckPolling(result.jobId, 900);
    } else {
      showToast(result?.message || "PPT 生成请求已提交");
      generating.value = false;
    }
  } catch (error) {
    console.warn("Meeting deck generation failed", error);
    showToast(error?.response?.data?.message || "PPT 生成失败，请检查模型或渲染环境");
    applyDeckJob({
      ...deckJob,
      status: "failed",
      stage: "生成失败",
      message: error?.response?.data?.message || "PPT 生成失败，请检查模型或渲染环境",
    });
    generating.value = false;
  } finally {
    if (!startedJob && deckJob.status !== "running") {
      generating.value = false;
    }
  }
}

function applyDeckJob(payload = {}) {
  deckJob.jobId = payload.jobId || deckJob.jobId || "";
  deckJob.status = payload.status || deckJob.status || "idle";
  deckJob.progress = Number(payload.progress ?? deckJob.progress ?? 0);
  deckJob.stage = payload.stage || deckJob.stage || "";
  deckJob.message = payload.message || deckJob.message || "";
  deckJob.downloadUrl = payload.downloadUrl || deckJob.downloadUrl || "";
}

function scheduleDeckPolling(jobId, delay = 1200) {
  stopDeckPolling();
  deckPollTimer = window.setTimeout(() => {
    refreshDeckJob(jobId);
  }, delay);
}

async function refreshDeckJob(jobId) {
  try {
    const result = await paperpilotApi.getMeetingDeckStatus(jobId);
    applyDeckJob(result);
    if (result?.done) {
      stopDeckPolling();
      generating.value = false;
      if (result.success && result.downloadUrl) {
        window.open(absoluteApiUrl(result.downloadUrl), "_blank");
        showToast("PPT 已生成，正在打开下载链接");
      } else {
        showToast(result?.message || "PPT 生成失败");
      }
      return;
    }
    scheduleDeckPolling(jobId);
  } catch (error) {
    console.warn("Meeting deck status polling failed", error);
    showToast("PPT 状态刷新失败，稍后会继续尝试");
    scheduleDeckPolling(jobId, 2200);
  }
}

function stopDeckPolling() {
  if (!deckPollTimer) return;
  window.clearTimeout(deckPollTimer);
  deckPollTimer = null;
}

function absoluteApiUrl(url) {
  if (!url) return "";
  if (/^https?:\/\//i.test(url)) return url;
  return `${API_BASE_URL}${url.startsWith("/") ? "" : "/"}${url}`;
}

function showToast(message) {
  toastMessage.value = message;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastMessage.value = "";
  }, 2800);
}
</script>

<style scoped>
.meeting-page {
  min-height: 100vh;
  padding: 30px min(34px, 4vw) 52px;
  background: linear-gradient(180deg, #f3f7fb 0, #eef3f8 330px, #f7f9fc 100%);
  color: #142033;
}

.meeting-topbar,
.flow-head,
.matrix-toolbar,
.matrix-actions,
.paper-lane,
.picker-row,
.picker-toolbar,
.paper-picker header,
.settings-modal header,
.settings-modal footer,
.deck-dock,
.template-tile,
.dock-actions {
  display: flex;
}

.meeting-topbar {
  max-width: 1480px;
  margin: 0 auto 22px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22px;
}

.title-cluster {
  max-width: 820px;
}

.section-label {
  display: inline-block;
  color: #2563eb;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.02em;
}

.meeting-topbar h1 {
  margin: 6px 0 0;
  font-size: 32px;
  line-height: 1.18;
  letter-spacing: -0.02em;
}

.meeting-topbar p,
.matrix-toolbar p,
.engine-note {
  margin: 8px 0 0;
  color: #56657a;
  font-size: 14px;
  line-height: 1.65;
}

.meeting-shell {
  max-width: 1480px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.paper-flow,
.matrix-area,
.deck-dock {
  border: 1px solid rgba(20, 32, 51, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
}

.paper-flow {
  padding: 18px;
}

.flow-head,
.matrix-toolbar {
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.flow-head h2,
.matrix-toolbar h2 {
  margin: 4px 0 0;
  color: #142033;
  font-size: 18px;
  line-height: 1.25;
}

.flow-count {
  min-width: 58px;
  text-align: right;
  color: #66758a;
}

.flow-count strong {
  color: #142033;
  font-size: 30px;
  line-height: 1;
}

.flow-count[data-ready="true"] strong {
  color: #047857;
}

.paper-lanes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.paper-lane,
.paper-add-lane {
  min-height: 132px;
  border-radius: 11px;
}

.paper-lane {
  align-items: flex-start;
  gap: 12px;
  padding: 13px;
  background: #f8fafc;
}

.lane-number {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 850;
  flex: 0 0 auto;
}

.lane-content {
  min-width: 0;
  flex: 1 1 auto;
}

.lane-content h3,
.picker-row h3 {
  margin: 0;
  color: #142033;
  font-size: 14px;
  line-height: 1.45;
}

.lane-content p,
.picker-row p {
  margin: 5px 0 0;
  color: #5b6a7f;
  font-size: 12px;
  line-height: 1.55;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 9px;
}

.tag-row span {
  padding: 3px 7px;
  border-radius: 999px;
  background: #edf2f7;
  color: #42526a;
  font-size: 12px;
}

.quiet-button {
  border: 0;
  background: transparent;
  color: #b42318;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 760;
}

.paper-add-lane {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  border: 1px dashed rgba(37, 99, 235, 0.45);
  background: #f7fbff;
  color: #1d4ed8;
  cursor: pointer;
}

.paper-add-lane span {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #dbeafe;
  font-size: 24px;
}

.paper-add-lane strong,
.paper-add-lane small {
  display: block;
}

.paper-add-lane small {
  color: #66758a;
}

.matrix-area {
  overflow: hidden;
}

.matrix-toolbar {
  padding: 18px 18px 12px;
}

.matrix-actions {
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.dimension-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(118px, 118px));
  gap: 8px;
  padding: 0 18px 14px;
  align-items: center;
}

.dimension-strip label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 118px;
  height: 44px;
  padding: 0 12px;
  border: 1px solid rgba(20, 32, 51, 0.1);
  border-radius: 999px;
  background: #fff;
  color: #42526a;
  font-size: 13px;
  line-height: 1.15;
  contain: layout paint;
  box-sizing: border-box;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.dimension-strip input {
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  margin: 0;
}

.dimension-strip label span {
  width: 52px;
  text-align: center;
}

.dimension-strip label.checked {
  border-color: rgba(37, 99, 235, 0.35);
  background: #eff6ff;
  color: #1d4ed8;
}

.matrix-frame {
  overflow-x: auto;
  border-top: 1px solid rgba(20, 32, 51, 0.08);
}

.comparison-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.comparison-table th,
.comparison-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(20, 32, 51, 0.08);
  text-align: left;
  vertical-align: top;
  font-size: 13px;
}

.comparison-table thead th {
  background: #f7f9fc;
  color: #142033;
  font-weight: 850;
}

.comparison-table thead th:first-child,
.comparison-table tbody th {
  width: 170px;
}

.comparison-table thead th span {
  display: block;
  min-width: 220px;
  max-width: 360px;
  line-height: 1.35;
}

.comparison-table tbody th {
  background: #fbfcfe;
}

.comparison-table tbody th strong,
.comparison-table tbody th small {
  display: block;
}

.comparison-table tbody th small {
  margin-top: 4px;
  color: #7b8798;
  font-weight: 520;
}

.comparison-table td {
  min-width: 240px;
  max-width: 420px;
  color: #26354a;
  line-height: 1.6;
}

.empty-matrix {
  height: 240px;
  color: #64748b;
  text-align: center;
}

.empty-matrix strong,
.empty-matrix span {
  display: block;
}

.empty-matrix strong {
  margin-top: 72px;
  color: #26354a;
  font-size: 15px;
}

.empty-matrix span {
  margin-top: 8px;
}

.missing-value {
  color: #8a96a8;
}

.insufficient-value {
  display: inline-block;
  max-width: 100%;
  color: #8a5a10;
  font-weight: 760;
  line-height: 1.55;
}

.deck-dock {
  align-items: stretch;
  gap: 16px;
  padding: 16px;
  flex-wrap: wrap;
}

.dock-status {
  width: 190px;
  flex: 0 0 190px;
  display: grid;
  align-content: center;
  gap: 5px;
  padding: 13px;
  border-radius: 10px;
  background: #fff7ed;
  color: #9a3412;
}

.dock-status.ready {
  background: #ecfdf5;
  color: #047857;
}

.dock-status span,
.dock-status small {
  font-size: 12px;
  font-weight: 760;
}

.dock-status strong {
  font-size: 18px;
}

.template-row {
  min-width: 0;
  flex: 1 1 auto;
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 10px;
}

.template-tile {
  align-items: stretch;
  gap: 10px;
  padding: 9px;
  border: 1px solid rgba(20, 32, 51, 0.1);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.template-tile.active {
  border-color: rgba(37, 99, 235, 0.55);
  background: #f0f7ff;
}

.template-cover {
  width: 48px;
  min-height: 62px;
  display: grid;
  gap: 5px;
  padding: 8px;
  border-radius: 8px;
  background: #142033;
  flex: 0 0 auto;
}

.template-cover i {
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.9);
}

.template-cover i:first-child {
  height: 15px;
}

.template-cover.journal { background: #0f766e; }
.template-cover.roadmap { background: #4338ca; }
.template-cover.minimal { background: #475569; }

.template-copy {
  min-width: 0;
  padding-top: 2px;
}

.template-copy strong,
.template-copy small {
  display: block;
}

.template-copy strong {
  color: #142033;
  font-size: 14px;
}

.template-copy small {
  margin-top: 4px;
  color: #5b6a7f;
  font-size: 12px;
  line-height: 1.45;
}

.dock-actions {
  width: 190px;
  flex: 0 0 190px;
  flex-direction: column;
  gap: 10px;
}

.deck-progress {
  width: 100%;
  flex: 1 0 100%;
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid rgba(37, 99, 235, 0.14);
  border-radius: 8px;
  background: #f8fbff;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #142033;
}

.progress-head strong {
  font-size: 13px;
  font-weight: 820;
}

.progress-head span,
.deck-progress small {
  color: #64748b;
  font-size: 12px;
  font-weight: 720;
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #dbe6f3;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #0f766e);
  transition: width 0.28s ease;
}

.deck-progress[data-status="generated"] .progress-track i {
  background: #0f766e;
}

.deck-progress[data-status="failed"] {
  border-color: rgba(185, 28, 28, 0.18);
  background: #fff7f7;
}

.deck-progress[data-status="failed"] .progress-track i {
  background: #dc2626;
}

.upload-hint {
  color: #66758a;
  font-size: 12px;
  line-height: 1.4;
}

.primary-action,
.secondary-action,
.paper-add-lane,
.picker-row button {
  border: 0;
  font: inherit;
}

.primary-action {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 9px;
  background: #1d4ed8;
  color: #fff;
  cursor: pointer;
  font-weight: 780;
}

.primary-action:disabled,
.picker-row button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.secondary-action {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(20, 32, 51, 0.12);
  border-radius: 9px;
  background: #fff;
  color: #142033;
  cursor: pointer;
  font-weight: 750;
}

.settings-backdrop,
.picker-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.45);
}

.settings-modal,
.paper-picker {
  width: min(980px, 100%);
  max-height: min(800px, calc(100vh - 48px));
  display: grid;
  grid-template-rows: auto 1fr auto;
  overflow: hidden;
  border-radius: 14px;
  background: #fff;
}

.settings-modal header,
.paper-picker header {
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-bottom: 1px solid rgba(20, 32, 51, 0.1);
}

.settings-modal h2,
.paper-picker h2 {
  margin: 4px 0 0;
  color: #142033;
  font-size: 21px;
}

.settings-modal header button,
.paper-picker header button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 9px;
  background: #eef2f7;
  color: #475569;
  cursor: pointer;
  font-size: 20px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 18px;
  overflow: auto;
}

.settings-grid label,
.checkbox-grid label {
  display: grid;
  gap: 6px;
}

.settings-grid label > span {
  color: #26354a;
  font-size: 13px;
  font-weight: 780;
}

.settings-grid select,
.settings-grid textarea,
.picker-toolbar input {
  width: 100%;
  border: 1px solid rgba(20, 32, 51, 0.13);
  border-radius: 9px;
  background: #fff;
  color: #142033;
  font: inherit;
}

.report-upload {
  position: relative;
  display: grid;
  gap: 5px;
  padding: 16px;
  border: 1px dashed rgba(37, 99, 235, 0.45);
  border-radius: 10px;
  background: #f7fbff;
  color: #1d4ed8;
}

.report-upload input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.report-upload.compact {
  border-style: solid;
  background: #f8fafc;
  color: #142033;
}

.report-upload.compact input {
  position: static;
  width: 16px;
  height: 16px;
  opacity: 1;
}

.inline-toggle {
  display: inline-flex !important;
  grid-template-columns: none !important;
  align-items: center;
  gap: 10px;
  width: fit-content;
  cursor: pointer;
}

.report-upload strong {
  color: #142033;
  font-size: 14px;
}

.report-upload small {
  color: #5b6a7f;
}

.settings-grid select {
  height: 40px;
  padding: 0 10px;
}

.settings-grid textarea,
.picker-toolbar input {
  padding: 10px;
  resize: vertical;
}

.field-wide {
  grid-column: 1 / -1;
}

.checkbox-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.checkbox-grid label {
  grid-template-columns: auto 1fr;
  align-items: center;
  padding: 9px;
  border: 1px solid rgba(20, 32, 51, 0.1);
  border-radius: 9px;
  background: #f8fafc;
  font-size: 13px;
}

.settings-modal footer {
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid rgba(20, 32, 51, 0.1);
}

.paper-picker {
  grid-template-rows: auto auto 1fr;
}

.picker-toolbar {
  align-items: center;
  gap: 12px;
  padding: 13px 18px;
  border-bottom: 1px solid rgba(20, 32, 51, 0.1);
}

.picker-toolbar span {
  white-space: nowrap;
  color: #5b6a7f;
  font-size: 13px;
}

.picker-list {
  display: grid;
  gap: 10px;
  padding: 18px;
  overflow: auto;
}

.picker-row {
  align-items: flex-start;
  gap: 14px;
  padding: 13px;
  border: 1px solid rgba(20, 32, 51, 0.1);
  border-radius: 11px;
  background: #fff;
}

.picker-row > div {
  min-width: 0;
  flex: 1 1 auto;
}

.picker-row button {
  min-width: 68px;
  min-height: 35px;
  border-radius: 9px;
  background: #1d4ed8;
  color: #fff;
  cursor: pointer;
  font-weight: 780;
}

.picker-empty {
  padding: 48px;
  color: #5b6a7f;
  text-align: center;
}

.meeting-toast {
  position: fixed;
  left: 50%;
  bottom: 24px;
  z-index: 120;
  transform: translateX(-50%);
}

@media (max-width: 1050px) {
  .deck-dock,
  .meeting-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .dock-status,
  .dock-actions {
    width: auto;
    flex-basis: auto;
  }

  .template-row {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 680px) {
  .meeting-page {
    padding: 18px 12px 38px;
  }

  .meeting-topbar h1 {
    font-size: 24px;
  }

  .matrix-toolbar,
  .matrix-actions,
  .paper-lane,
  .picker-row,
  .template-tile {
    flex-direction: column;
  }

  .template-row,
  .settings-grid,
  .checkbox-grid {
    grid-template-columns: 1fr;
  }
}
</style>
