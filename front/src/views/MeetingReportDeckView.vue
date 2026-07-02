<template>
  <div class="meeting-page">
    <header class="meeting-hero">
      <div class="hero-copy">
        <span class="eyebrow">Group Meeting Workspace</span>
        <h1>把一篇论文整理成可以讲、可以问、可以沉淀的组会材料</h1>
        <p>从已保存论文中选择本次汇报对象，生成规范分点综述并永久保存，再进入 PPT Master 的网页参数流程制作 PPT。</p>
      </div>

      <label class="upload-button" :class="{ busy: uploading }" title="上传 PDF">
        <input type="file" accept="application/pdf,.pdf" :disabled="uploading" @change="uploadPaper" />
        <span aria-hidden="true">+</span>
        <strong>{{ uploading ? "上传中" : "上传论文" }}</strong>
      </label>
    </header>

    <main class="meeting-workbench">
      <section class="featured-paper" aria-label="当前汇报论文">
        <div v-if="selectedPaper" class="paper-cover">
          <div class="cover-topline">
            <span>{{ selectedPaper.publishYear || "年份待补" }}</span>
            <strong>{{ hasPdf(selectedPaper) ? "PDF 已就绪" : "缺少 PDF" }}</strong>
          </div>
          <div class="cover-title">
            <span>本次汇报主论文</span>
            <h2>{{ selectedPaper.title || "未命名论文" }}</h2>
            <p>{{ compactMeta(selectedPaper) }}</p>
          </div>
          <div class="cover-bottomline">
            <span v-for="tag in normalizedTags(selectedPaper)" :key="tag">{{ tag }}</span>
          </div>
        </div>

        <div v-else class="paper-cover empty">
          <div class="cover-title">
            <span>本次汇报主论文</span>
            <h2>选择或上传一篇论文</h2>
            <p>上传 PDF 后会自动保存到文献库，并出现在下方论文架里。</p>
          </div>
        </div>

        <div class="paper-brief">
          <section>
            <span>讲述入口</span>
            <p>{{ selectedPaper?.abstract || selectedPaper?.note || "暂无摘要。生成论文综述时会读取已保存信息和 PDF 正文，整理成组会可讲的结构。" }}</p>
          </section>
          <section>
            <span>综述规范</span>
            <ul>
              <li>先用一页综述概括论文精髓。</li>
              <li>再分点沉淀研究问题、方法路线、证据结果和局限讨论。</li>
              <li>保存后会绑定到该论文，后续打开仍然保留。</li>
            </ul>
          </section>
        </div>
      </section>

      <aside class="action-suite" aria-label="论文操作">
        <section class="suite-card review-card">
          <span>论文综述</span>
          <h2>{{ selectedPaper ? "生成规范分点综述" : "等待选择论文" }}</h2>
          <p>内容按组会汇报习惯拆成基本信息、研究问题、理论背景、方法、结果、数据、贡献局限等模块。</p>
          <button
            type="button"
            class="secondary-action"
            :disabled="!selectedPaper || isReviewBusy(selectedPaper)"
            @click="selectedPaper && openReview(selectedPaper)"
          >
            {{ selectedPaper && isReviewBusy(selectedPaper) ? reviewProgressLabel(selectedPaper) : "打开论文综述" }}
          </button>
        </section>

        <section class="suite-card deck-card" :data-status="deckJob.status">
          <span>PPT Master</span>
          <h2>{{ deckJob.paperTitle || "制作组会 PPT" }}</h2>
          <p>{{ deckJob.message || "选择带 PDF 的论文后，会打开 PPT Master 参数页，并在后台执行制作流程。" }}</p>
          <div v-if="deckJob.jobId" class="progress-line" aria-label="PPT 制作进度">
            <i :style="{ width: `${Math.max(2, deckJob.progress || 0)}%` }"></i>
            <strong>{{ Math.round(deckJob.progress || 0) }}%</strong>
          </div>
          <div class="deck-actions">
            <button
              type="button"
              class="primary-action"
              :disabled="!selectedPaper || !hasPdf(selectedPaper) || isDeckBusy(selectedPaper)"
              @click="selectedPaper && makePpt(selectedPaper)"
            >
              {{ selectedPaper && isDeckBusy(selectedPaper) ? "制作中" : "PPT 制作" }}
            </button>
            <button v-if="deckJob.confirmUrl && deckJob.status === 'running'" type="button" class="ghost-action" @click="openConfirmUrl">
              参数页
            </button>
          </div>
        </section>
      </aside>
    </main>

    <section class="paper-library" aria-label="已添加论文">
      <div class="library-head">
        <div>
          <span>Paper Shelf</span>
          <h2>已添加论文</h2>
        </div>
        <div class="library-tools">
          <strong>{{ filteredPapers.length }} 篇</strong>
          <input v-model="keyword" type="search" placeholder="搜索标题、作者、年份" />
        </div>
      </div>

      <div v-if="loadingPapers" class="paper-skeletons">
        <span v-for="item in 4" :key="item"></span>
      </div>

      <div v-else-if="!filteredPapers.length" class="empty-library">
        <strong>{{ papers.length ? "没有匹配的论文" : "还没有组会论文" }}</strong>
        <p>{{ papers.length ? "换一个关键词，或清空搜索后查看全部。" : "点击右上角上传 PDF，论文会保存到文献库并进入这里。" }}</p>
      </div>

      <div v-else class="paper-grid">
        <article
          v-for="paper in filteredPapers"
          :key="paper.workspaceId"
          class="paper-card"
          :class="{ active: selectedPaper?.workspaceId === paper.workspaceId }"
          @click="selectPaper(paper)"
        >
          <div class="paper-card-index">{{ paperIndex(paper) }}</div>
          <div class="paper-card-body">
            <div class="paper-card-meta">
              <span :class="['pdf-badge', hasPdf(paper) ? 'ready' : 'missing']">{{ hasPdf(paper) ? "PDF" : "待补 PDF" }}</span>
              <span>{{ paper.publishYear || "年份未知" }}</span>
            </div>
            <h3>{{ paper.title || "未命名论文" }}</h3>
            <p>{{ compactMeta(paper) }}</p>
            <div class="paper-tags">
              <span v-for="tag in normalizedTags(paper)" :key="tag">{{ tag }}</span>
            </div>
          </div>
          <div class="paper-card-actions" @click.stop>
            <button type="button" :disabled="isReviewBusy(paper)" @click="openReview(paper)">
              {{ isReviewBusy(paper) ? reviewProgressLabel(paper) : "论文综述" }}
            </button>
            <button type="button" class="make-deck" :disabled="!hasPdf(paper) || isDeckBusy(paper)" @click="makePpt(paper)">
              {{ isDeckBusy(paper) ? `${Math.round(deckJob.progress || 1)}%` : "PPT 制作" }}
            </button>
          </div>
        </article>
      </div>
    </section>

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
            <div class="progress-line">
              <i :style="{ width: `${Math.max(8, reviewDrawer.progress)}%` }"></i>
              <strong>{{ Math.round(reviewDrawer.progress || 0) }}%</strong>
            </div>
          </div>

          <template v-else>
            <div class="review-actions">
              <button type="button" class="primary-action" :disabled="reviewDrawer.generating" @click="generateReview">
                {{ reviewDrawer.generating ? reviewProgressLabel(reviewDrawer.paper) : reviewDrawer.generated ? "重新生成综述" : "生成论文综述" }}
              </button>
              <button type="button" class="secondary-action" :disabled="reviewDrawer.saving" @click="saveReview">
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
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
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
const selectedWorkspaceId = ref("");
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

const selectedPaper = computed(() =>
  filteredPapers.value.find((paper) => paper.workspaceId === selectedWorkspaceId.value)
  || filteredPapers.value[0]
  || null
);

watch(selectedPaper, (paper) => {
  if (paper) selectedWorkspaceId.value = paper.workspaceId;
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
    if (!selectedWorkspaceId.value && papers.value[0]) selectedWorkspaceId.value = papers.value[0].workspaceId;
  } catch (error) {
    showToast(error?.response?.data?.message || "论文列表加载失败");
  } finally {
    loadingPapers.value = false;
  }
}

function selectPaper(paper) {
  selectedWorkspaceId.value = paper.workspaceId;
}

function paperIndex(paper) {
  const index = filteredPapers.value.findIndex((item) => item.workspaceId === paper.workspaceId);
  return String(index + 1).padStart(2, "0");
}

function compactMeta(paper) {
  return [paper.authors || "作者待补全", paper.publishYear || "年份未知", paper.source || "来源未记录"]
    .filter(Boolean)
    .join(" · ");
}

async function uploadPaper(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file || uploading.value) return;
  uploading.value = true;
  try {
    const paper = await paperpilotApi.uploadLibraryPaper(file);
    papers.value = [paper, ...papers.value.filter((item) => item.workspaceId !== paper.workspaceId)];
    selectedWorkspaceId.value = paper.workspaceId;
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
  return tags.length ? tags.slice(0, 3) : [paper?.venueType || "待分类"];
}

function isReviewBusy(paper) {
  return Boolean(paper?.workspaceId && reviewJobs[paper.workspaceId]);
}

function reviewProgressLabel(paper) {
  const job = paper ? reviewJobs[paper.workspaceId] : null;
  const progress = job?.progress ?? reviewDrawer.progress ?? 1;
  return `综述 ${Math.round(progress)}%`;
}

function isDeckBusy(paper) {
  return Boolean(paper?.workspaceId && deckJob.status === "running" && deckJob.paperWorkspaceId === paper.workspaceId);
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
  padding: 30px min(40px, 4vw) 56px;
  background: linear-gradient(180deg, #eef4fb 0, #f7f9fc 330px, #f8fafc 100%);
  color: #162033;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
}

.meeting-hero,
.meeting-workbench,
.paper-library {
  width: min(1500px, 100%);
  margin-inline: auto;
}

.meeting-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.hero-copy {
  max-width: 900px;
}

.eyebrow,
.library-head span,
.suite-card > span,
.cover-title > span,
.paper-brief span {
  color: #1556d6;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: .04em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 9px 0 0;
  color: #111827;
  font-size: clamp(26px, 2.2vw, 38px);
  line-height: 1.22;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 760px;
  margin: 12px 0 0;
  color: #526277;
  font-size: 14px;
  line-height: 1.75;
}

.upload-button {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-width: 136px;
  height: 46px;
  border: 1px solid #1d5be3;
  border-radius: 8px;
  background: #1d5be3;
  color: #fff;
  cursor: pointer;
}

.upload-button input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.upload-button span {
  font-size: 22px;
  line-height: 1;
}

.upload-button strong {
  font-size: 14px;
}

.upload-button:hover {
  background: #194fc6;
}

.upload-button.busy {
  cursor: wait;
  opacity: .7;
}

.meeting-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  align-items: stretch;
}

.featured-paper,
.action-suite,
.paper-library {
  border: 1px solid #dce5ef;
  border-radius: 8px;
  background: rgba(255, 255, 255, .92);
}

.featured-paper {
  display: grid;
  grid-template-columns: minmax(360px, .95fr) minmax(300px, .75fr);
  gap: 18px;
  min-height: 390px;
  padding: 18px;
}

.paper-cover {
  position: relative;
  display: grid;
  align-content: space-between;
  min-height: 354px;
  overflow: hidden;
  border-radius: 8px;
  padding: 24px;
  background:
    linear-gradient(140deg, #12203a 0 62%, #edf5ff 62% 100%);
  color: #fff;
}

.paper-cover::before {
  content: "";
  position: absolute;
  right: 46px;
  top: 32px;
  width: 156px;
  height: 156px;
  border: 1px solid rgba(29, 91, 227, .25);
  border-radius: 50%;
  background: rgba(255, 255, 255, .22);
}

.paper-cover.empty {
  background: linear-gradient(140deg, #1f2a44 0 62%, #eef4fb 62% 100%);
}

.cover-topline,
.cover-bottomline {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.cover-topline {
  justify-content: space-between;
}

.cover-topline span,
.cover-topline strong,
.cover-bottomline span {
  border: 1px solid rgba(255, 255, 255, .25);
  border-radius: 999px;
  padding: 5px 9px;
  background: rgba(255, 255, 255, .1);
  color: #eaf2ff;
  font-size: 11px;
  font-weight: 800;
}

.cover-title {
  position: relative;
  z-index: 1;
  max-width: min(86%, 660px);
}

.cover-title h2 {
  margin: 13px 0 0;
  color: #fff;
  font-size: clamp(23px, 1.9vw, 31px);
  line-height: 1.18;
  letter-spacing: 0;
  text-wrap: balance;
  overflow-wrap: anywhere;
}

.cover-title p {
  margin: 15px 0 0;
  color: #d8e3f4;
  font-size: 14px;
  line-height: 1.7;
}

.paper-brief {
  display: grid;
  gap: 14px;
}

.paper-brief section {
  border: 1px solid #e0e8f1;
  border-radius: 8px;
  padding: 18px;
  background: #fbfdff;
}

.paper-brief p,
.paper-brief li {
  color: #3f5067;
  font-size: 14px;
  line-height: 1.8;
}

.paper-brief p {
  margin: 10px 0 0;
}

.paper-brief ul {
  margin: 12px 0 0;
  padding-left: 19px;
}

.action-suite {
  display: grid;
  gap: 1px;
  overflow: hidden;
}

.suite-card {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 190px;
  padding: 20px;
  background: #fff;
}

.suite-card + .suite-card {
  border-top: 1px solid #e2e9f2;
}

.suite-card h2 {
  margin: 0;
  color: #111827;
  font-size: 19px;
  line-height: 1.35;
}

.suite-card p {
  margin: 0;
  color: #55657a;
  font-size: 13px;
  line-height: 1.75;
}

.primary-action,
.secondary-action,
.ghost-action,
.paper-card-actions button {
  min-height: 40px;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 850;
  cursor: pointer;
}

.primary-action {
  border: 1px solid #1d5be3;
  background: #1d5be3;
  color: #fff;
}

.secondary-action,
.ghost-action {
  border: 1px solid #cbd8e7;
  background: #fff;
  color: #162033;
}

.ghost-action {
  color: #1556d6;
}

button:disabled {
  cursor: not-allowed;
  opacity: .55;
}

.deck-actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.progress-line {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.progress-line i {
  display: block;
  height: 8px;
  border-radius: 999px;
  background: #1d5be3;
  transition: width .2s ease;
}

.progress-line::before {
  content: "";
  grid-column: 1;
  grid-row: 1;
  height: 8px;
  border-radius: 999px;
  background: #e4edf7;
}

.progress-line i {
  grid-column: 1;
  grid-row: 1;
}

.progress-line strong {
  color: #526277;
  font-size: 12px;
}

.deck-card[data-status="failed"] .progress-line i {
  background: #dc2626;
}

.deck-card[data-status="generated"] .progress-line i {
  background: #0f766e;
}

.paper-library {
  margin-top: 18px;
  padding: 18px;
}

.library-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.library-head h2 {
  margin: 4px 0 0;
  color: #111827;
  font-size: 22px;
}

.library-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.library-tools strong {
  color: #526277;
  font-size: 13px;
}

.library-tools input {
  width: min(320px, 42vw);
  height: 40px;
  border: 1px solid #d5e0eb;
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: #162033;
  outline: none;
}

.library-tools input:focus {
  border-color: #1d5be3;
  box-shadow: 0 0 0 3px rgba(29, 91, 227, .1);
}

.paper-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.paper-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 116px;
  gap: 14px;
  align-items: stretch;
  min-height: 176px;
  border: 1px solid #dde6ef;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  cursor: pointer;
  transition: border-color .16s ease, background .16s ease;
}

.paper-card:hover,
.paper-card.active {
  border-color: #1d5be3;
  background: #f7fbff;
}

.paper-card-index {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #edf3fa;
  color: #4b5f78;
  font-size: 13px;
  font-weight: 900;
}

.paper-card.active .paper-card-index {
  background: #1d5be3;
  color: #fff;
}

.paper-card-body {
  min-width: 0;
}

.paper-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #718098;
  font-size: 12px;
}

.pdf-badge {
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 850;
}

.pdf-badge.ready {
  color: #047857;
  background: #e4f6ed;
}

.pdf-badge.missing {
  color: #8a4b00;
  background: #fff1d7;
}

.paper-card h3 {
  margin: 11px 0 0;
  color: #152033;
  font-size: 17px;
  line-height: 1.45;
  text-wrap: pretty;
}

.paper-card p {
  margin: 9px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  background: #eef4fb;
  color: #40536b;
  font-size: 11px;
  font-weight: 700;
}

.paper-card-actions {
  display: grid;
  gap: 8px;
  align-content: center;
}

.paper-card-actions button {
  border: 1px solid #cbd8e7;
  background: #fff;
  color: #162033;
  font-size: 12px;
}

.paper-card-actions .make-deck {
  border-color: #1d5be3;
  background: #1d5be3;
  color: #fff;
}

.paper-skeletons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.paper-skeletons span {
  height: 176px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f5f7fb, #edf3fa, #f5f7fb);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

.empty-library {
  display: grid;
  place-content: center;
  min-height: 220px;
  text-align: center;
}

.empty-library strong {
  color: #172033;
  font-size: 18px;
}

.empty-library p {
  max-width: 360px;
  margin: 9px auto 0;
  color: #64748b;
  line-height: 1.7;
}

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
  border-left: 1px solid #dce5ef;
  background: #fff;
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
  font-weight: 850;
}

.review-drawer header h2 {
  margin: 6px 0 0;
  color: #111827;
  font-size: 18px;
  line-height: 1.45;
}

.review-drawer header button {
  width: 34px;
  height: 34px;
  border: 1px solid #d7e0ea;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 20px;
}

.review-actions {
  display: flex;
  gap: 10px;
  padding: 16px 22px 0;
}

.review-loading {
  padding: 28px 22px;
}

.review-section-list {
  display: grid;
  gap: 14px;
  padding: 18px 22px 28px;
}

.review-section {
  border: 1px solid #dfe7f1;
  border-radius: 8px;
  overflow: hidden;
}

.review-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #e7edf5;
  background: #f8fafc;
}

.review-section-head strong {
  color: #162033;
  font-size: 14px;
}

.review-section-head small {
  color: #64748b;
  font-size: 12px;
}

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

.review-section textarea::placeholder {
  color: #697891;
}

.meeting-toast {
  position: fixed;
  left: 50%;
  bottom: 22px;
  z-index: 60;
  transform: translateX(-50%);
  max-width: min(620px, calc(100vw - 32px));
  border-radius: 8px;
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
.drawer-fade-leave-to {
  opacity: 0;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}

@keyframes shimmer {
  from { background-position: 120% 0; }
  to { background-position: -120% 0; }
}

@media (max-width: 1180px) {
  .meeting-workbench,
  .featured-paper {
    grid-template-columns: 1fr;
  }

  .action-suite {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .suite-card + .suite-card {
    border-top: 0;
    border-left: 1px solid #e2e9f2;
  }
}

@media (max-width: 900px) {
  .meeting-page {
    padding: 20px 12px 42px;
  }

  .meeting-hero,
  .library-head {
    flex-direction: column;
    align-items: stretch;
  }

  .upload-button,
  .library-tools input {
    width: 100%;
  }

  .library-tools {
    align-items: stretch;
    flex-direction: column;
  }

  .paper-grid,
  .paper-skeletons,
  .action-suite {
    grid-template-columns: 1fr;
  }

  .suite-card + .suite-card {
    border-left: 0;
    border-top: 1px solid #e2e9f2;
  }
}

@media (max-width: 620px) {
  .paper-cover {
    min-height: 320px;
    padding: 20px;
  }

  .cover-title {
    max-width: 100%;
  }

  .cover-title h2 {
    font-size: 23px;
  }

  .paper-card {
    grid-template-columns: 40px minmax(0, 1fr);
  }

  .paper-card-actions {
    grid-column: 1 / -1;
    grid-template-columns: 1fr 1fr;
  }

  .review-actions {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    transition-duration: .01ms !important;
    animation-duration: .01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
