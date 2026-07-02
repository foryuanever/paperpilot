<template>
  <div class="meeting-workbench">
    <header class="meeting-header">
      <div>
        <p class="meeting-kicker">组会汇报</p>
        <h1>从文献库挑选论文，生成综述与汇报 PPT</h1>
        <p>这里罗列你已经添加的论文。每篇论文都可以单独生成规范综述，或直接进入 PPT Master 制作流程。</p>
      </div>
      <label class="upload-trigger" :class="{ busy: uploading }">
        <input type="file" accept="application/pdf,.pdf" :disabled="uploading" @change="uploadPaper" />
        <span aria-hidden="true">＋</span>
        <strong>{{ uploading ? "上传中" : "上传论文" }}</strong>
      </label>
    </header>

    <main class="meeting-layout">
      <section class="paper-panel">
        <div class="paper-toolbar">
          <div>
            <strong>{{ filteredPapers.length }} 篇论文</strong>
            <span>{{ papers.length ? "按最近添加排序" : "上传或从文献库添加后会出现在这里" }}</span>
          </div>
          <input v-model="keyword" type="search" placeholder="搜索标题、作者、年份" />
        </div>

        <div v-if="loadingPapers" class="paper-skeleton" aria-live="polite">
          <span v-for="item in 5" :key="item"></span>
        </div>

        <div v-else-if="!filteredPapers.length" class="empty-state">
          <strong>{{ papers.length ? "没有匹配的论文" : "还没有可用于组会汇报的论文" }}</strong>
          <p>{{ papers.length ? "换一个关键词试试。" : "点击右上角上传 PDF，系统会保存到文献库并补全题录。" }}</p>
        </div>

        <div v-else class="paper-list">
          <article v-for="paper in filteredPapers" :key="paper.workspaceId" class="paper-row">
            <div class="paper-main">
              <div class="paper-title-line">
                <h2>{{ paper.title || "未命名论文" }}</h2>
                <span :class="['pdf-state', hasPdf(paper) ? 'ready' : 'missing']">
                  {{ hasPdf(paper) ? "PDF 已就绪" : "缺少 PDF" }}
                </span>
              </div>
              <p class="paper-meta">
                <span>{{ paper.authors || "作者待补全" }}</span>
                <span>{{ paper.publishYear || "年份未知" }}</span>
                <span>{{ paper.source || "来源未记录" }}</span>
              </p>
              <p class="paper-abstract">{{ paper.abstract || paper.note || "暂无摘要；可先生成综述，系统会优先读取 PDF 正文。" }}</p>
              <div class="paper-tags">
                <span v-for="tag in normalizedTags(paper)" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <div class="paper-actions">
              <button type="button" class="action-secondary" :disabled="isReviewBusy(paper)" @click="openReview(paper)">
                {{ isReviewBusy(paper) ? reviewProgressLabel(paper) : "论文综述" }}
              </button>
              <button type="button" class="action-primary" :disabled="!hasPdf(paper) || isDeckBusy(paper)" @click="makePpt(paper)">
                {{ isDeckBusy(paper) ? `${Math.round(deckJob.progress || 1)}%` : "PPT 制作" }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <aside class="status-panel">
        <div class="status-block">
          <strong>综述规范</strong>
          <ol>
            <li>研究背景与问题</li>
            <li>方法路线与实验设置</li>
            <li>核心结果、贡献与局限</li>
            <li>可用于组会讨论的问题</li>
          </ol>
        </div>
        <div class="status-block deck-status" :data-status="deckJob.status">
          <strong>PPT 制作状态</strong>
          <p>{{ deckJob.paperTitle || "选择任意一篇带 PDF 的论文开始制作。" }}</p>
          <div v-if="deckJob.jobId" class="progress-track">
            <i :style="{ width: `${Math.max(2, deckJob.progress || 0)}%` }"></i>
          </div>
          <small>{{ deckJob.message || "PPT Master 会弹出参数页，并在后台完成逐页设计。" }}</small>
          <button
            v-if="deckJob.confirmUrl && deckJob.status === 'running'"
            type="button"
            class="status-link"
            @click="openConfirmUrl"
          >
            打开参数页
          </button>
        </div>
      </aside>
    </main>

    <Transition name="drawer-fade">
      <div v-if="reviewDrawer.open" class="review-backdrop" @click.self="closeReview">
        <section class="review-drawer" aria-label="论文综述">
          <header>
            <div>
              <span>{{ reviewDrawer.generated ? "已永久保存" : "待生成" }}</span>
              <h2>{{ reviewDrawer.paper?.title || "论文综述" }}</h2>
            </div>
            <button type="button" aria-label="关闭" @click="closeReview">×</button>
          </header>

          <div v-if="reviewDrawer.loading" class="review-loading">
            <strong>{{ reviewDrawer.message || "正在读取已保存综述" }}</strong>
            <div class="progress-track">
              <i :style="{ width: `${Math.max(8, reviewDrawer.progress)}%` }"></i>
            </div>
          </div>

          <template v-else>
            <div class="review-actions">
              <button type="button" class="action-primary" :disabled="reviewDrawer.generating" @click="generateReview">
                {{ reviewDrawer.generating ? reviewProgressLabel(reviewDrawer.paper) : reviewDrawer.generated ? "重新生成综述" : "生成论文综述" }}
              </button>
              <button type="button" class="action-secondary" :disabled="reviewDrawer.saving" @click="saveReview">
                {{ reviewDrawer.saving ? "保存中" : "保存编辑" }}
              </button>
            </div>

            <div class="review-section-list">
              <section v-for="section in reviewSections" :key="section.key" class="review-section">
                <div class="review-section-head">
                  <strong>{{ section.title }}</strong>
                  <small>{{ section.hint }}</small>
                </div>
                <textarea v-model="reviewDrawer.sections[section.key]" :placeholder="section.placeholder"></textarea>
              </section>
            </div>
          </template>
        </section>
      </div>
    </Transition>

    <Transition name="slide-up">
      <div v-if="toastMessage" class="meeting-toast">{{ toastMessage }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { API_BASE_URL } from "../services/apiClient";

const reviewSections = [
  { key: "synthesis", title: "一页综述", hint: "用 3-5 点讲清论文精髓", placeholder: "建议包含：一句话结论、核心贡献、关键证据、可讨论问题。" },
  { key: "basicInfo", title: "基本信息", hint: "题录、来源与研究对象", placeholder: "作者、年份、期刊/会议、研究对象、数据来源。" },
  { key: "overview", title: "研究问题", hint: "为什么要做，解决什么问题", placeholder: "背景痛点、研究缺口、本文要回答的问题。" },
  { key: "background", title: "理论背景", hint: "相关工作与概念框架", placeholder: "关键概念、相关理论、与既有工作的关系。" },
  { key: "method", title: "方法路线", hint: "模型、框架或实验路径", placeholder: "输入、方法模块、实验流程、变量/指标设置。" },
  { key: "results", title: "结果证据", hint: "主要发现与支撑证据", placeholder: "核心结果、对比、消融、统计或案例证据。" },
  { key: "datasets", title: "数据与评测", hint: "数据来源、设置和指标", placeholder: "样本、数据集、划分、评价指标、可复现性。" },
  { key: "conclusion", title: "贡献与局限", hint: "导师最关心的讨论点", placeholder: "贡献、边界、局限、后续研究方向。" },
];

const papers = ref([]);
const keyword = ref("");
const loadingPapers = ref(false);
const uploading = ref(false);
const toastMessage = ref("");
const reviewJobs = reactive({});
const reviewDrawer = reactive({
  open: false,
  paper: null,
  sections: emptySections(),
  generated: false,
  loading: false,
  generating: false,
  saving: false,
  progress: 0,
  message: "",
  modelName: "",
});
const deckJob = reactive({
  jobId: "",
  paperWorkspaceId: "",
  paperTitle: "",
  status: "idle",
  progress: 0,
  message: "",
  confirmUrl: "",
  downloadUrl: "",
});
let toastTimer = null;
let reviewPollTimer = null;
let deckPollTimer = null;
const confirmOpened = ref("");

const filteredPapers = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return papers.value;
  return papers.value.filter((paper) => [
    paper.title,
    paper.authors,
    paper.publishYear,
    paper.source,
    paper.note,
  ].some((value) => String(value || "").toLowerCase().includes(query)));
});

onMounted(loadPapers);
onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer);
  stopReviewPolling();
  stopDeckPolling();
});

function emptySections() {
  return Object.fromEntries(reviewSections.map((section) => [section.key, ""]));
}

async function loadPapers() {
  loadingPapers.value = true;
  try {
    papers.value = await paperpilotApi.getLibraryPapers();
  } catch (error) {
    showToast(error?.response?.data?.message || "论文列表加载失败");
  } finally {
    loadingPapers.value = false;
  }
}

async function uploadPaper(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file || uploading.value) return;
  uploading.value = true;
  try {
    const paper = await paperpilotApi.uploadLibraryPaper(file);
    papers.value = [paper, ...papers.value.filter((item) => item.workspaceId !== paper.workspaceId)];
    showToast("论文已上传并保存到文献库");
  } catch (error) {
    showToast(error?.response?.data?.message || "论文上传失败");
  } finally {
    uploading.value = false;
  }
}

async function openReview(paper) {
  reviewDrawer.open = true;
  reviewDrawer.paper = paper;
  reviewDrawer.loading = true;
  reviewDrawer.progress = 12;
  reviewDrawer.message = "正在读取已保存综述";
  reviewDrawer.sections = emptySections();
  try {
    const data = await paperpilotApi.getMeetingReport(paper.workspaceId);
    applyReviewData(data);
    if (!data.generated) {
      await generateReview();
    }
  } catch (error) {
    showToast(error?.response?.data?.message || "综述读取失败");
  } finally {
    reviewDrawer.loading = false;
  }
}

function closeReview() {
  reviewDrawer.open = false;
  stopReviewPolling();
}

async function generateReview() {
  const paper = reviewDrawer.paper;
  if (!paper?.workspaceId || reviewDrawer.generating) return;
  reviewDrawer.generating = true;
  reviewJobs[paper.workspaceId] = { progress: 1, message: "提交生成任务" };
  try {
    const result = await paperpilotApi.generateMeetingReport(paper.workspaceId);
    if (result?.status === "completed" || result?.generated) {
      applyReviewData(result);
      reviewJobs[paper.workspaceId] = null;
      showToast("论文综述已生成并永久保存");
    } else {
      pollReview(paper.workspaceId);
    }
  } catch (error) {
    showToast(error?.response?.data?.message || "论文综述生成失败");
    reviewJobs[paper.workspaceId] = null;
    reviewDrawer.generating = false;
  }
}

async function pollReview(workspaceId) {
  stopReviewPolling();
  reviewPollTimer = window.setTimeout(async () => {
    try {
      const status = await paperpilotApi.getMeetingReportGenerateStatus(workspaceId);
      reviewJobs[workspaceId] = { progress: status.progress || 0, message: status.message || "" };
      reviewDrawer.progress = status.progress || 0;
      reviewDrawer.message = status.message || "";
      if (status.done) {
        stopReviewPolling();
        reviewDrawer.generating = false;
        reviewJobs[workspaceId] = null;
        if (status.success) {
          applyReviewData(await paperpilotApi.getMeetingReport(workspaceId));
          showToast("论文综述已生成并永久保存");
        } else {
          showToast(status.message || "论文综述生成失败");
        }
        return;
      }
      pollReview(workspaceId);
    } catch (error) {
      stopReviewPolling();
      reviewDrawer.generating = false;
      reviewJobs[workspaceId] = null;
      showToast(error?.response?.data?.message || "综述状态刷新失败");
    }
  }, 1200);
}

async function saveReview() {
  const paper = reviewDrawer.paper;
  if (!paper?.workspaceId || reviewDrawer.saving) return;
  reviewDrawer.saving = true;
  try {
    const data = await paperpilotApi.saveMeetingReport(paper.workspaceId, {
      sections: reviewDrawer.sections,
      modelName: reviewDrawer.modelName || "人工编辑",
    });
    applyReviewData(data);
    showToast("综述编辑已保存");
  } catch (error) {
    showToast(error?.response?.data?.message || "综述保存失败");
  } finally {
    reviewDrawer.saving = false;
  }
}

function applyReviewData(data = {}) {
  reviewDrawer.sections = { ...emptySections(), ...(data.sections || {}) };
  reviewDrawer.generated = Boolean(data.generated);
  reviewDrawer.modelName = data.modelName || "";
  reviewDrawer.progress = 100;
  reviewDrawer.message = data.generated ? "已保存" : "尚未生成";
}

async function makePpt(paper) {
  if (!hasPdf(paper) || isDeckBusy(paper)) return;
  stopDeckPolling();
  Object.assign(deckJob, {
    jobId: "",
    paperWorkspaceId: paper.workspaceId,
    paperTitle: paper.title,
    status: "running",
    progress: 1,
    message: "正在提交 PPT Master 任务",
    confirmUrl: "",
    downloadUrl: "",
  });
  try {
    const result = await paperpilotApi.generateMeetingDeck({
      engine: "ppt-master-skill",
      reportWorkspaceId: paper.workspaceId,
      paperIds: [paper.workspaceId],
    });
    applyDeckJob(result, paper);
    if (result?.jobId && !result.done) {
      pollDeck(result.jobId, paper);
      showToast("PPT 制作任务已开始");
    } else if (result?.success && result.downloadUrl) {
      window.open(absoluteApiUrl(result.downloadUrl), "_blank");
    }
  } catch (error) {
    deckJob.status = "failed";
    deckJob.message = error?.response?.data?.message || "PPT 制作失败";
    showToast(deckJob.message);
  }
}

function pollDeck(jobId, paper) {
  stopDeckPolling();
  deckPollTimer = window.setTimeout(async () => {
    try {
      const result = await paperpilotApi.getMeetingDeckStatus(jobId);
      applyDeckJob(result, paper);
      if (result.done) {
        stopDeckPolling();
        if (result.success && result.downloadUrl) {
          window.open(absoluteApiUrl(result.downloadUrl), "_blank");
          showToast("PPT 已生成，正在打开下载链接");
        } else {
          showToast(result.message || "PPT 制作失败");
        }
        return;
      }
      pollDeck(jobId, paper);
    } catch (error) {
      showToast(error?.response?.data?.message || "PPT 状态刷新失败");
      pollDeck(jobId, paper);
    }
  }, 1400);
}

function applyDeckJob(payload = {}, paper = {}) {
  deckJob.jobId = payload.jobId || deckJob.jobId || "";
  deckJob.paperWorkspaceId = paper.workspaceId || deckJob.paperWorkspaceId || "";
  deckJob.paperTitle = paper.title || deckJob.paperTitle || "";
  deckJob.status = payload.status || deckJob.status || "running";
  deckJob.progress = Number(payload.progress ?? deckJob.progress ?? 0);
  deckJob.message = payload.message || deckJob.message || "";
  deckJob.confirmUrl = payload.confirmUrl || deckJob.confirmUrl || "";
  deckJob.downloadUrl = payload.downloadUrl || deckJob.downloadUrl || "";
  if (deckJob.confirmUrl && confirmOpened.value !== deckJob.confirmUrl) {
    openConfirmUrl();
  }
}

function openConfirmUrl() {
  if (!deckJob.confirmUrl) return;
  confirmOpened.value = deckJob.confirmUrl;
  window.open(deckJob.confirmUrl, "_blank");
}

function stopReviewPolling() {
  if (reviewPollTimer) window.clearTimeout(reviewPollTimer);
  reviewPollTimer = null;
}

function stopDeckPolling() {
  if (deckPollTimer) window.clearTimeout(deckPollTimer);
  deckPollTimer = null;
}

function hasPdf(paper) {
  return paperpilotApi.isLikelyPdfUrl(paper?.paperUrl || "");
}

function normalizedTags(paper) {
  const tags = Array.isArray(paper?.journalTags) ? paper.journalTags : [];
  return tags.length ? tags.slice(0, 4) : [paper?.venueType || "待分类"];
}

function isReviewBusy(paper) {
  return Boolean(reviewJobs[paper.workspaceId]);
}

function reviewProgressLabel(paper) {
  const job = paper ? reviewJobs[paper.workspaceId] : null;
  const progress = job?.progress ?? reviewDrawer.progress ?? 1;
  return `综述 ${Math.round(progress)}%`;
}

function isDeckBusy(paper) {
  return deckJob.status === "running" && deckJob.paperWorkspaceId === paper.workspaceId;
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
.meeting-workbench {
  min-height: 100vh;
  padding: 28px min(36px, 4vw) 56px;
  background: #f5f7fb;
  color: #172033;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
}

.meeting-header {
  max-width: 1440px;
  margin: 0 auto 22px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.meeting-kicker {
  margin: 0 0 8px;
  color: #2759d8;
  font-size: 13px;
  font-weight: 700;
}

.meeting-header h1 {
  margin: 0;
  color: #101827;
  font-size: 28px;
  line-height: 1.22;
  letter-spacing: 0;
  text-wrap: balance;
}

.meeting-header p:not(.meeting-kicker) {
  max-width: 760px;
  margin: 10px 0 0;
  color: #536176;
  font-size: 14px;
  line-height: 1.75;
}

.upload-trigger {
  flex: 0 0 auto;
  min-width: 132px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 12px;
  border: 1px solid #1f5be3;
  background: #235fe7;
  color: white;
  cursor: pointer;
  transition: transform .18s ease, background .18s ease;
}

.upload-trigger:hover { background: #174bd1; transform: translateY(-1px); }
.upload-trigger.busy { cursor: wait; opacity: .75; }
.upload-trigger input { position: absolute; inline-size: 1px; block-size: 1px; opacity: 0; }
.upload-trigger span { font-size: 20px; line-height: 1; }
.upload-trigger strong { font-size: 14px; }

.meeting-layout {
  max-width: 1440px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.paper-panel,
.status-panel,
.review-drawer {
  border: 1px solid #dfe6ef;
  border-radius: 14px;
  background: #fff;
}

.paper-toolbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
  border-bottom: 1px solid #e7edf5;
  border-radius: 14px 14px 0 0;
  background: rgba(255,255,255,.96);
}

.paper-toolbar div { display: grid; gap: 4px; }
.paper-toolbar strong { font-size: 15px; }
.paper-toolbar span { color: #64748b; font-size: 12px; }
.paper-toolbar input {
  width: min(360px, 42vw);
  height: 38px;
  box-sizing: border-box;
  border: 1px solid #d7e0ea;
  border-radius: 10px;
  padding: 0 12px;
  color: #172033;
  outline: 0;
}
.paper-toolbar input:focus { border-color: #2f6df6; box-shadow: 0 0 0 3px rgba(47,109,246,.1); }

.paper-list { display: grid; }

.paper-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
  padding: 18px;
  border-bottom: 1px solid #edf1f6;
}
.paper-row:last-child { border-bottom: 0; }
.paper-row:hover { background: #fbfdff; }

.paper-title-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.paper-title-line h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  line-height: 1.45;
  text-wrap: pretty;
}

.pdf-state {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 700;
}
.pdf-state.ready { color: #08745c; background: #e7f7ef; }
.pdf-state.missing { color: #9a4d00; background: #fff3dd; }

.paper-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin: 8px 0 0;
  color: #526176;
  font-size: 12px;
}
.paper-meta span:not(:last-child)::after { content: ""; }
.paper-abstract {
  max-width: 92ch;
  margin: 10px 0 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.paper-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}
.paper-tags span {
  border-radius: 999px;
  padding: 4px 8px;
  background: #eef3fa;
  color: #40516a;
  font-size: 11px;
}

.paper-actions {
  display: grid;
  align-content: center;
  gap: 10px;
}
.action-primary,
.action-secondary,
.status-link {
  height: 38px;
  border-radius: 10px;
  border: 1px solid transparent;
  padding: 0 14px;
  font-weight: 700;
  cursor: pointer;
}
.action-primary {
  background: #235fe7;
  color: #fff;
}
.action-primary:hover:not(:disabled) { background: #174bd1; }
.action-secondary {
  border-color: #cdd8e6;
  background: #fff;
  color: #172033;
}
.action-secondary:hover:not(:disabled) { border-color: #9fb3cf; background: #f8fbff; }
button:disabled { cursor: not-allowed; opacity: .55; }

.status-panel {
  position: sticky;
  top: 18px;
  display: grid;
  gap: 0;
  overflow: hidden;
}
.status-block {
  padding: 18px;
  border-bottom: 1px solid #e7edf5;
}
.status-block:last-child { border-bottom: 0; }
.status-block strong { display: block; margin-bottom: 10px; font-size: 14px; }
.status-block ol { margin: 0; padding-left: 18px; color: #46566d; font-size: 13px; line-height: 1.8; }
.status-block p { margin: 0 0 10px; color: #46566d; font-size: 13px; line-height: 1.65; }
.status-block small { display: block; color: #64748b; line-height: 1.6; }

.progress-track {
  height: 8px;
  margin: 12px 0;
  border-radius: 999px;
  background: #e5edf7;
  overflow: hidden;
}
.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #235fe7;
  transition: width .2s ease;
}
.deck-status[data-status="failed"] .progress-track i { background: #dc2626; }
.deck-status[data-status="generated"] .progress-track i { background: #0f766e; }
.status-link {
  width: 100%;
  margin-top: 12px;
  border-color: #b8c8df;
  background: #f8fbff;
  color: #174bd1;
}

.paper-skeleton { display: grid; gap: 1px; }
.paper-skeleton span {
  height: 96px;
  background: linear-gradient(90deg, #f5f7fb, #eef3f8, #f5f7fb);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}
.empty-state {
  padding: 48px 18px;
  text-align: center;
}
.empty-state strong { display: block; color: #172033; }
.empty-state p { margin: 8px auto 0; max-width: 420px; color: #64748b; line-height: 1.7; }

.review-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, .28);
}
.review-drawer {
  width: min(760px, 100vw);
  height: 100vh;
  overflow: auto;
  border-radius: 0;
  border-block: 0;
  border-right: 0;
}
.review-drawer header {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  border-bottom: 1px solid #e6edf5;
  background: #fff;
}
.review-drawer header span {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}
.review-drawer header h2 {
  margin: 6px 0 0;
  font-size: 18px;
  line-height: 1.45;
}
.review-drawer header button {
  width: 34px;
  height: 34px;
  border: 1px solid #d7e0ea;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font-size: 20px;
}
.review-actions {
  display: flex;
  gap: 10px;
  padding: 16px 22px 0;
}
.review-loading { padding: 28px 22px; }
.review-loading strong { font-size: 14px; }
.review-section-list {
  display: grid;
  gap: 14px;
  padding: 18px 22px 28px;
}
.review-section {
  border: 1px solid #dfe7f1;
  border-radius: 12px;
  overflow: hidden;
}
.review-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e7edf5;
}
.review-section-head strong { font-size: 14px; }
.review-section-head small { color: #64748b; font-size: 12px; }
.review-section textarea {
  width: 100%;
  min-height: 116px;
  box-sizing: border-box;
  border: 0;
  resize: vertical;
  padding: 14px;
  color: #1f2937;
  outline: 0;
  font: 13px/1.75 inherit;
}
.review-section textarea::placeholder { color: #697891; }

.meeting-toast {
  position: fixed;
  left: 50%;
  bottom: 22px;
  z-index: 60;
  transform: translateX(-50%);
  max-width: min(620px, calc(100vw - 32px));
  border-radius: 12px;
  padding: 12px 16px;
  background: #172033;
  color: #fff;
  font-size: 13px;
}

.drawer-fade-enter-active,
.drawer-fade-leave-active,
.slide-up-enter-active,
.slide-up-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}
.drawer-fade-enter-from,
.drawer-fade-leave-to { opacity: 0; }
.slide-up-enter-from,
.slide-up-leave-to { opacity: 0; transform: translate(-50%, 8px); }

@keyframes shimmer {
  from { background-position: 120% 0; }
  to { background-position: -120% 0; }
}

@media (max-width: 980px) {
  .meeting-layout { grid-template-columns: 1fr; }
  .status-panel { position: static; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .status-block { border-bottom: 0; border-right: 1px solid #e7edf5; }
  .status-block:last-child { border-right: 0; }
}

@media (max-width: 720px) {
  .meeting-workbench { padding: 18px 12px 40px; }
  .meeting-header { flex-direction: column; }
  .upload-trigger { width: 100%; }
  .paper-toolbar { align-items: stretch; flex-direction: column; }
  .paper-toolbar input { width: 100%; }
  .paper-row { grid-template-columns: 1fr; }
  .paper-actions { grid-template-columns: 1fr 1fr; }
  .status-panel { grid-template-columns: 1fr; }
  .status-block { border-right: 0; border-bottom: 1px solid #e7edf5; }
  .review-actions { flex-direction: column; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    transition-duration: .01ms !important;
    animation-duration: .01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
