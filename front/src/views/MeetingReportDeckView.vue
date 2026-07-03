<template>
  <div class="meeting-timeline-page">
    <header class="timeline-header">
      <div>
        <span>组会汇报</span>
        <h1>按时间沉淀每一次组会材料</h1>
        <p>每张卡片是一场组会：选择汇报文献、补充重点内容、生成论文综述，并把 PPT 任务留在后台执行。</p>
      </div>
      <button type="button" class="add-meeting-button" @click="addMeeting">
        <span aria-hidden="true">+</span>
        添加组会
      </button>
    </header>

    <main class="timeline-shell">
      <section class="timeline-list" aria-label="组会时间轴">
        <article
          v-for="(meeting, index) in sortedMeetings"
          :key="meeting.id"
          class="meeting-card"
          :class="[`tone-${index % 4}`, { active: activeMeetingId === meeting.id }]"
        >
          <div class="timeline-pin" aria-hidden="true">
            <span></span>
          </div>

          <div class="meeting-card-head">
            <div class="meeting-date-block">
              <div class="date-summary">
                <span>组会时间</span>
                <strong>{{ formatMeetingDate(meeting.meetingTime) }}</strong>
              </div>
              <input
                v-model="meeting.meetingTime"
                type="datetime-local"
                aria-label="组会时间"
                @change="persistMeetings"
              />
            </div>
            <div class="meeting-head-actions">
              <div class="meeting-status" :data-status="meetingStatus(meeting)">
                <span></span>
                {{ statusText(meeting) }}
              </div>
              <button type="button" class="delete-meeting-button" title="删除组会" @click="removeMeeting(meeting.id)">
                删除
              </button>
            </div>
          </div>

          <div class="meeting-card-body">
            <section class="meeting-main">
              <input
                v-model="meeting.title"
                class="meeting-title-input"
                placeholder="例如：欧盟 AI 法案合规自动化组会汇报"
                @change="persistMeetings"
              />

              <label class="meeting-notes">
                <span>组会重点内容</span>
                <textarea
                  v-model="meeting.notes"
                  placeholder="记录这次要讲清楚的核心问题、导师可能追问的点、需要讨论的实验或方法缺口。"
                  @change="persistMeetings"
                ></textarea>
              </label>
            </section>

            <aside class="meeting-side">
              <div class="paper-box">
                <div class="box-title">
                  <span>组会汇报文献</span>
                  <button type="button" @click="openPaperPicker(meeting)">点击添加</button>
                </div>
                <div v-if="meeting.papers.length" class="selected-papers">
                  <button
                    v-for="paperId in meeting.papers"
                    :key="paperId"
                    type="button"
                    class="selected-paper"
                    @click="setPrimaryPaper(meeting, paperId)"
                  >
                    <strong>{{ paperTitle(paperId) }}</strong>
                    <small>{{ paperMeta(paperId) }}</small>
                  </button>
                </div>
                <p v-else>从已导入论文中选择，也可以直接上传 PDF。</p>
              </div>

              <div class="generation-grid">
                <div class="generation-action">
                  <div class="progress-label">
                    <span>论文综述</span>
                    <strong>{{ reviewPercent(meeting) }}%</strong>
                  </div>
                  <p class="generation-step">{{ reviewStepText(meeting) }}</p>
                  <div class="generation-progress" aria-hidden="true">
                    <i :style="{ width: `${reviewPercent(meeting)}%` }"></i>
                  </div>
                  <button
                    type="button"
                    class="soft-button"
                    :disabled="!primaryPaper(meeting) || isReviewBusy(meeting)"
                    @click="openReview(meeting)"
                  >
                    {{ isReviewBusy(meeting) ? "综述生成中" : "查看 / 生成综述" }}
                  </button>
                </div>

                <div class="generation-action">
                  <div class="progress-label">
                    <span>PPT</span>
                    <strong>{{ deckPercent(meeting) }}%</strong>
                  </div>
                  <p class="generation-step">{{ deckStepText(meeting) }}</p>
                  <div class="generation-progress ppt-progress" aria-hidden="true">
                    <i :style="{ width: `${deckPercent(meeting)}%` }"></i>
                  </div>
                  <a
                    v-if="deckJobs[meeting.id]?.downloadUrl"
                    class="download-button"
                    :href="absoluteApiUrl(deckJobs[meeting.id].downloadUrl)"
                    target="_blank"
                    rel="noreferrer"
                  >
                    下载 PPT
                  </a>
                  <button
                    v-else
                    type="button"
                    class="primary-button"
                    :disabled="!primaryPaper(meeting) || !hasPdf(primaryPaper(meeting)) || isDeckBusy(meeting)"
                    @click="makePpt(meeting)"
                  >
                    {{ isDeckBusy(meeting) ? "后台生成中" : "生成汇报 PPT" }}
                  </button>
                </div>
              </div>
            </aside>
          </div>
        </article>
      </section>
    </main>

    <Transition name="modal-fade">
      <div v-if="paperPicker.open" class="modal-backdrop" @click.self="closePaperPicker">
        <section class="paper-picker modal-panel" aria-label="选择组会文献">
          <header>
            <div>
              <span>添加汇报文献</span>
              <h2>{{ paperPicker.meeting?.title || "选择论文" }}</h2>
            </div>
            <button type="button" aria-label="关闭" @click="closePaperPicker">×</button>
          </header>

          <div class="picker-tools">
            <input v-model="keyword" type="search" placeholder="搜索标题、作者或年份" />
            <label class="upload-inline" :class="{ busy: uploading }">
              <input type="file" accept="application/pdf,.pdf" :disabled="uploading" @change="uploadPaper" />
              {{ uploading ? "上传中" : "上传 PDF" }}
            </label>
          </div>

          <div v-if="loadingPapers" class="paper-loading">
            <span v-for="item in 4" :key="item"></span>
          </div>
          <div v-else class="picker-paper-list">
            <button
              v-for="paper in filteredPapers"
              :key="paper.workspaceId"
              type="button"
              class="picker-paper"
              :class="{ selected: paperPicker.meeting?.papers.includes(paper.workspaceId) }"
              @click="toggleMeetingPaper(paperPicker.meeting, paper)"
            >
              <span :class="['pdf-badge', hasPdf(paper) ? 'ready' : 'missing']">
                {{ hasPdf(paper) ? "PDF" : "待补 PDF" }}
              </span>
              <strong>{{ paper.title || "未命名论文" }}</strong>
              <small>{{ compactMeta(paper) }}</small>
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="reviewModal.open" class="modal-backdrop" @click.self="closeReview">
        <section class="review-modal modal-panel" aria-label="组会论文综述">
          <header>
            <div>
              <span>{{ reviewModal.generated ? "已保存综述" : "组会汇报综述" }}</span>
              <h2>{{ reviewModal.paper?.title || "论文综述" }}</h2>
            </div>
            <button type="button" aria-label="关闭" @click="closeReview">×</button>
          </header>

          <div v-if="reviewModal.loading" class="review-loading">
            <strong>{{ reviewModal.message || "正在读取综述" }}</strong>
            <div class="wide-progress">
              <i :style="{ width: `${Math.max(8, reviewModal.progress)}%` }"></i>
            </div>
          </div>

          <template v-else>
            <div class="review-modal-actions">
              <button type="button" class="primary-button" :disabled="reviewModal.generating" @click="generateReview">
                {{ reviewModal.generating ? `生成中 ${reviewModal.progress}%` : reviewModal.generated ? "重新生成综述" : "生成论文综述" }}
              </button>
              <button type="button" class="soft-button" :disabled="reviewModal.saving" @click="saveReview">
                {{ reviewModal.saving ? "保存中" : "保存编辑" }}
              </button>
            </div>

            <div class="review-point-list">
              <section v-for="section in reviewSections" :key="section.key" class="review-point">
                <div class="review-point-meta">
                  <strong>{{ section.title }}</strong>
                  <small>{{ section.hint }}</small>
                </div>
                <button
                  type="button"
                  class="copy-section-button"
                  :title="`复制${section.title}`"
                  @click="copyReviewSection(section)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <rect x="8" y="8" width="10" height="10" rx="2"></rect>
                    <path d="M6 16H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                  </svg>
                </button>
                <textarea
                  v-model="reviewModal.sections[section.key]"
                  :placeholder="section.placeholder"
                  @input="resizeReviewTextarea"
                ></textarea>
              </section>
            </div>
          </template>
        </section>
      </div>
    </Transition>

    <Transition name="toast-slide">
      <div v-if="toastMessage" class="meeting-toast">{{ toastMessage }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { API_BASE_URL } from "../services/apiClient";

const STORAGE_KEY = "paperpilot-meeting-timeline-v1";
const DECK_STORAGE_KEY = "paperpilot-meeting-deck-jobs-v1";
const REVIEW_STORAGE_KEY = "paperpilot-meeting-review-jobs-v1";

const reviewSections = [
  { key: "basicInfo", title: "基本信息", hint: "题录、来源与研究对象", placeholder: "作者、年份、期刊/会议、研究对象、数据来源。" },
  { key: "overview", title: "研究问题", hint: "为什么要做", placeholder: "背景痛点、研究缺口、本文要回答的问题。" },
  { key: "background", title: "理论背景", hint: "相关工作与概念框架", placeholder: "关键概念、相关理论、与既有工作的关系。" },
  { key: "method", title: "方法路线", hint: "模型、框架或实验路径", placeholder: "输入、方法模块、实验流程、变量/指标设置。" },
  { key: "results", title: "结果证据", hint: "主要发现与支撑证据", placeholder: "核心结果、对比、消融、统计或案例证据。" },
  { key: "datasets", title: "数据与评测", hint: "数据来源、设置和指标", placeholder: "样本、数据集、划分、评价指标、可复现性。" },
  { key: "conclusion", title: "贡献与局限", hint: "组会讨论点", placeholder: "贡献、边界、局限、后续研究方向。" },
];

const meetings = ref([]);
const papers = ref([]);
const keyword = ref("");
const loadingPapers = ref(false);
const uploading = ref(false);
const activeMeetingId = ref("");
const toastMessage = ref("");
const deckJobs = reactive({});
const reviewJobs = reactive({});
const paperPicker = reactive({ open: false, meeting: null });
const reviewModal = reactive({
  open: false,
  meeting: null,
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

let toastTimer = null;
const deckTimers = new Map();
const reviewTimers = new Map();
const confirmOpened = ref("");

const sortedMeetings = computed(() => [...meetings.value].sort((a, b) => new Date(b.meetingTime) - new Date(a.meetingTime)));

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

watch(meetings, persistMeetings, { deep: true });
watch(() => ({ ...deckJobs }), persistDeckJobs, { deep: true });
watch(() => ({ ...reviewJobs }), persistReviewJobs, { deep: true });

onMounted(async () => {
  loadPersistedState();
  await loadPapers();
  ensureFirstMeeting();
  resumeDeckJobs();
  resumeReviewJobs();
});

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer);
  deckTimers.forEach((timer) => window.clearTimeout(timer));
  reviewTimers.forEach((timer) => window.clearTimeout(timer));
});

function emptySections() {
  return Object.fromEntries(reviewSections.map((section) => [section.key, ""]));
}

function loadPersistedState() {
  try {
    const savedMeetings = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    if (Array.isArray(savedMeetings)) meetings.value = savedMeetings.map(normalizeMeeting);
  } catch {
    meetings.value = [];
  }
  try {
    Object.assign(deckJobs, JSON.parse(localStorage.getItem(DECK_STORAGE_KEY) || "{}"));
  } catch {
    Object.keys(deckJobs).forEach((key) => delete deckJobs[key]);
  }
  try {
    Object.assign(reviewJobs, JSON.parse(localStorage.getItem(REVIEW_STORAGE_KEY) || "{}"));
  } catch {
    Object.keys(reviewJobs).forEach((key) => delete reviewJobs[key]);
  }
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

function ensureFirstMeeting() {
  if (meetings.value.length) return;
  const firstPaper = papers.value.find(hasPdf) || papers.value[0];
  meetings.value = [createMeeting(firstPaper ? [firstPaper.workspaceId] : [])];
  activeMeetingId.value = meetings.value[0].id;
}

function createMeeting(paperIds = []) {
  const now = new Date();
  now.setMinutes(Math.ceil(now.getMinutes() / 15) * 15, 0, 0);
  return {
    id: `meeting-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    meetingTime: toDatetimeLocal(now),
    title: "新组会汇报",
    notes: "本次重点：先讲清研究问题，再讨论方法路线、证据质量和后续可推进方向。",
    tags: ["待汇报"],
    tagDraft: "",
    params: {
      reportType: "paper",
      audience: "导师与课题组",
      slideCount: "10-12",
    },
    papers: paperIds,
    primaryPaperId: paperIds[0] || "",
  };
}

function normalizeMeeting(meeting = {}) {
  const paperIds = Array.isArray(meeting.papers) ? meeting.papers.filter(Boolean) : [];
  return {
    id: meeting.id || `meeting-${Date.now()}`,
    meetingTime: meeting.meetingTime || toDatetimeLocal(new Date()),
    title: meeting.title || "组会汇报",
    notes: meeting.notes || "",
    tags: Array.isArray(meeting.tags) ? meeting.tags : [],
    tagDraft: "",
    params: {
      reportType: meeting.params?.reportType || "paper",
      audience: meeting.params?.audience || "导师与课题组",
      slideCount: meeting.params?.slideCount || "10-12",
    },
    papers: paperIds,
    primaryPaperId: meeting.primaryPaperId || paperIds[0] || "",
  };
}

function addMeeting() {
  const firstPaper = papers.value.find(hasPdf) || papers.value[0];
  const meeting = createMeeting(firstPaper ? [firstPaper.workspaceId] : []);
  meetings.value.unshift(meeting);
  activeMeetingId.value = meeting.id;
  showToast("已添加一场组会");
}

function removeMeeting(meetingId) {
  const meeting = meetings.value.find((item) => item.id === meetingId);
  if (!meeting) return;
  if (!window.confirm(`删除「${meeting.title || "组会汇报"}」？`)) return;
  meetings.value = meetings.value.filter((item) => item.id !== meetingId);
  delete deckJobs[meetingId];
  if (deckTimers.has(meetingId)) {
    window.clearTimeout(deckTimers.get(meetingId));
    deckTimers.delete(meetingId);
  }
  activeMeetingId.value = meetings.value[0]?.id || "";
  persistMeetings();
  persistDeckJobs();
  showToast("已删除组会");
}

function persistMeetings() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(meetings.value.map(({ tagDraft, ...meeting }) => meeting)));
}

function persistDeckJobs() {
  localStorage.setItem(DECK_STORAGE_KEY, JSON.stringify(deckJobs));
}

function persistReviewJobs() {
  localStorage.setItem(REVIEW_STORAGE_KEY, JSON.stringify(reviewJobs));
}

function formatMeetingDate(value) {
  if (!value) return "未定时间";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "未定时间";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function toDatetimeLocal(date) {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60000).toISOString().slice(0, 16);
}

function openPaperPicker(meeting) {
  paperPicker.open = true;
  paperPicker.meeting = meeting;
  activeMeetingId.value = meeting.id;
}

function closePaperPicker() {
  paperPicker.open = false;
  paperPicker.meeting = null;
}

function toggleMeetingPaper(meeting, paper) {
  if (!meeting || !paper?.workspaceId) return;
  const exists = meeting.papers.includes(paper.workspaceId);
  meeting.papers = exists
    ? meeting.papers.filter((id) => id !== paper.workspaceId)
    : [paper.workspaceId, ...meeting.papers];
  if (!meeting.papers.includes(meeting.primaryPaperId)) {
    meeting.primaryPaperId = meeting.papers[0] || "";
  }
  persistMeetings();
}

async function uploadPaper(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file || uploading.value) return;
  uploading.value = true;
  try {
    const paper = await paperpilotApi.uploadLibraryPaper(file);
    papers.value = [paper, ...papers.value.filter((item) => item.workspaceId !== paper.workspaceId)];
    if (paperPicker.meeting) {
      paperPicker.meeting.papers = [paper.workspaceId, ...paperPicker.meeting.papers.filter((id) => id !== paper.workspaceId)];
      paperPicker.meeting.primaryPaperId = paper.workspaceId;
      persistMeetings();
    }
    showToast("论文已上传并加入组会文献");
  } catch (error) {
    showToast(error?.response?.data?.message || "论文上传失败");
  } finally {
    uploading.value = false;
  }
}

function setPrimaryPaper(meeting, paperId) {
  meeting.primaryPaperId = paperId;
  activeMeetingId.value = meeting.id;
  persistMeetings();
}

function primaryPaper(meeting) {
  const paperId = meeting?.primaryPaperId || meeting?.papers?.[0];
  return papers.value.find((paper) => paper.workspaceId === paperId) || null;
}

function paperTitle(paperId) {
  return papers.value.find((paper) => paper.workspaceId === paperId)?.title || "未命名论文";
}

function paperMeta(paperId) {
  const paper = papers.value.find((item) => item.workspaceId === paperId);
  return paper ? compactMeta(paper) : "论文信息待同步";
}

function compactMeta(paper) {
  return [paper?.authors || "作者待补全", paper?.publishYear || "年份未知", paper?.source || "来源未记录"]
    .filter(Boolean)
    .join(" · ");
}

function hasPdf(paper) {
  return paperpilotApi.isLikelyPdfUrl(paper?.paperUrl || "");
}

function meetingStatus(meeting) {
  const deck = deckJobs[meeting.id];
  const paper = primaryPaper(meeting);
  const review = paper ? reviewJobs[paper.workspaceId] : null;
  if (isDeckRunning(deck) || review?.status === "running") return "running";
  if (deck?.downloadUrl || deck?.status === "generated") return "ready";
  if (review?.status === "generated") return "ready";
  if (deck?.status === "failed") return "failed";
  if (review?.status === "failed") return "failed";
  return "idle";
}

function statusText(meeting) {
  const status = meetingStatus(meeting);
  if (status === "ready") return "已生成";
  if (status === "running") return "执行中";
  if (status === "failed") return "失败";
  return "未开始";
}

function reviewPercent(meeting) {
  const paper = primaryPaper(meeting);
  const job = paper ? reviewJobs[paper.workspaceId] : null;
  return Math.round(job?.progress || 0);
}

function deckPercent(meeting) {
  const job = deckJobs[meeting.id];
  if (job?.downloadUrl || job?.status === "generated") return 100;
  return Math.round(job?.progress || 0);
}

function isReviewBusy(meeting) {
  const paper = primaryPaper(meeting);
  return Boolean(paper && reviewJobs[paper.workspaceId]?.status === "running");
}

function isDeckBusy(meeting) {
  return isDeckRunning(deckJobs[meeting.id]);
}

function isDeckRunning(job) {
  return job?.status === "running" || job?.status === "awaiting_agent";
}

function reviewStepText(meeting) {
  const paper = primaryPaper(meeting);
  const job = paper ? reviewJobs[paper.workspaceId] : null;
  if (!paper) return "先添加汇报文献";
  if (job?.status === "running") return job.message || "正在生成综述";
  if (job?.status === "generated") return "已保存，可重新生成";
  if (job?.status === "failed") return "生成失败，可重试";
  return "等待生成或查看";
}

function deckStepText(meeting) {
  const job = deckJobs[meeting.id];
  if (!primaryPaper(meeting)) return "先添加汇报文献";
  if (!job) return "等待启动 PPT 任务";
  if (job.status === "generated") return "已生成，可下载";
  if (job.status === "failed") return job.message || "生成失败";
  return [job.stage, job.message].filter(Boolean).join(" · ") || "后台生成中";
}

async function openReview(meeting) {
  const paper = primaryPaper(meeting);
  if (!paper) {
    showToast("请先添加组会汇报文献");
    return;
  }
  activeMeetingId.value = meeting.id;
  Object.assign(reviewModal, {
    open: true,
    meeting,
    paper,
    sections: emptySections(),
    generated: false,
    loading: true,
    generating: false,
    saving: false,
    progress: reviewJobs[paper.workspaceId]?.progress || 10,
    message: "正在读取已保存综述",
    modelName: "",
  });
  try {
    const data = await paperpilotApi.getMeetingReport(paper.workspaceId);
    applyReviewData(data);
    if (data.generated) {
      reviewJobs[paper.workspaceId] = { status: "generated", progress: 100, message: "已读取历史综述" };
      persistReviewJobs();
    }
    if (!data.generated && !isReviewBusy(meeting)) {
      await generateReview();
    }
  } catch (error) {
    showToast(error?.response?.data?.message || "综述读取失败");
  } finally {
    reviewModal.loading = false;
  }
}

function closeReview() {
  reviewModal.open = false;
}

async function generateReview() {
  const paper = reviewModal.paper;
  if (!paper?.workspaceId || reviewModal.generating) return;
  reviewModal.generating = true;
  reviewJobs[paper.workspaceId] = { status: "running", progress: 1, message: "提交生成任务" };
  persistReviewJobs();
  try {
    const result = await paperpilotApi.generateMeetingReport(paper.workspaceId);
    if (result?.status === "completed" || result?.generated) {
      applyReviewData(result);
      reviewJobs[paper.workspaceId] = { status: "generated", progress: 100, message: "已生成" };
      showToast("论文综述已生成并永久保存");
    } else {
      pollReview(paper.workspaceId);
    }
  } catch (error) {
    reviewJobs[paper.workspaceId] = { status: "failed", progress: 0, message: error?.response?.data?.message || "论文综述生成失败" };
    reviewModal.generating = false;
    showToast(reviewJobs[paper.workspaceId].message);
  }
}

function pollReview(workspaceId) {
  if (reviewTimers.has(workspaceId)) window.clearTimeout(reviewTimers.get(workspaceId));
  const timer = window.setTimeout(async () => {
    try {
      const status = await paperpilotApi.getMeetingReportGenerateStatus(workspaceId);
      reviewJobs[workspaceId] = {
        status: status.done ? (status.success ? "generated" : "failed") : "running",
        progress: status.success ? 100 : (status.progress || 0),
        message: status.message || "",
      };
      if (reviewModal.paper?.workspaceId === workspaceId) {
        reviewModal.progress = reviewJobs[workspaceId].progress;
        reviewModal.message = reviewJobs[workspaceId].message;
      }
      if (status.done) {
        reviewTimers.delete(workspaceId);
        reviewModal.generating = false;
        if (status.success) {
          if (reviewModal.paper?.workspaceId === workspaceId) {
            applyReviewData(await paperpilotApi.getMeetingReport(workspaceId));
          }
          showToast("论文综述已生成并永久保存");
        } else {
          showToast(status.message || "论文综述生成失败");
        }
        return;
      }
      pollReview(workspaceId);
    } catch (error) {
      reviewJobs[workspaceId] = { status: "failed", progress: reviewJobs[workspaceId]?.progress || 0, message: error?.response?.data?.message || "综述状态刷新失败" };
      reviewModal.generating = false;
      showToast(reviewJobs[workspaceId].message);
    }
  }, 1400);
  reviewTimers.set(workspaceId, timer);
}

async function saveReview() {
  const paper = reviewModal.paper;
  if (!paper?.workspaceId || reviewModal.saving) return;
  reviewModal.saving = true;
  try {
    const data = await paperpilotApi.saveMeetingReport(paper.workspaceId, {
      sections: reviewModal.sections,
      modelName: reviewModal.modelName || "人工编辑",
    });
    applyReviewData(data);
    reviewJobs[paper.workspaceId] = { status: "generated", progress: 100, message: "已保存" };
    showToast("综述编辑已保存");
  } catch (error) {
    showToast(error?.response?.data?.message || "综述保存失败");
  } finally {
    reviewModal.saving = false;
  }
}

function applyReviewData(data = {}) {
  reviewModal.sections = formatReviewSections({ ...emptySections(), ...(data.sections || {}) });
  reviewModal.generated = Boolean(data.generated);
  reviewModal.modelName = data.modelName || "";
  reviewModal.progress = data.generated ? 100 : reviewModal.progress;
  reviewModal.message = data.generated ? "已读取历史保存的论文综述" : "尚未生成";
  nextTick(resizeAllReviewTextareas);
}

function formatReviewSections(sections) {
  return Object.fromEntries(Object.entries(sections).map(([key, value]) => [key, formatReviewParagraphs(value)]));
}

function formatReviewParagraphs(value = "") {
  const labels = [
    "研究背景", "论文定位", "核心要点", "要点", "研究问题", "整体框架", "关键模块", "实现流程",
    "主要发现", "关键证据", "实验设置", "数据来源", "方法路线", "结果证据", "贡献", "局限", "讨论点", "启发"
  ];
  const labelPattern = labels.join("|");
  return String(value || "")
    .replace(/\r\n/g, "\n")
    .replace(new RegExp(`([^\\n])((?:${labelPattern})[：:])`, "g"), "$1\n\n$2")
    .replace(/([。；;])((?:第二|第三|第四|第五|第六|第七|其次|再次|最后)[，,])/g, "$1\n\n$2")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function resizeReviewTextarea(event) {
  const textarea = event?.target;
  if (!textarea) return;
  textarea.style.height = "auto";
  textarea.style.height = `${textarea.scrollHeight}px`;
}

function resizeAllReviewTextareas() {
  document.querySelectorAll(".review-point textarea").forEach((textarea) => {
    textarea.style.height = "auto";
    textarea.style.height = `${textarea.scrollHeight}px`;
  });
}

async function copyReviewSection(section) {
  const content = (reviewModal.sections[section.key] || "").trim();
  if (!content) {
    showToast("这一段还没有内容");
    return;
  }
  const text = `${section.title}\n${content}`;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
    } else {
      const textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.setAttribute("readonly", "");
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
    }
    showToast(`已复制${section.title}`);
  } catch {
    showToast("复制失败，请手动选择文本复制");
  }
}

async function makePpt(meeting) {
  const paper = primaryPaper(meeting);
  if (!paper || !hasPdf(paper) || isDeckBusy(meeting)) return;
  activeMeetingId.value = meeting.id;
  deckJobs[meeting.id] = {
    status: "running",
    progress: 1,
    stage: "提交任务",
    message: "正在提交 PPT Master 任务",
    paperWorkspaceId: paper.workspaceId,
    paperTitle: paper.title,
    jobId: "",
    confirmUrl: "",
    downloadUrl: "",
  };
  persistDeckJobs();
  try {
    const result = await paperpilotApi.generateMeetingDeck({
      engine: "ppt-master-skill",
      reportWorkspaceId: paper.workspaceId,
      paperIds: meeting.papers,
      slideCount: meeting.params.slideCount,
      audience: meeting.params.audience,
      focus: meeting.notes,
      templateName: meeting.params.reportType,
    });
    applyDeckJob(meeting, result, paper);
    if (result?.jobId && !result.done) {
      pollDeck(meeting.id, result.jobId, paper);
      showToast("PPT 已进入后台生成，可以离开页面");
    } else if (result?.success && result.downloadUrl) {
      showToast("PPT 已生成，可以下载");
    }
  } catch (error) {
    deckJobs[meeting.id] = {
      ...deckJobs[meeting.id],
      status: "failed",
      progress: deckJobs[meeting.id]?.progress || 0,
      stage: "生成失败",
      message: error?.response?.data?.message || "PPT 制作失败",
    };
    showToast(deckJobs[meeting.id].message);
  }
}

function pollDeck(meetingId, jobId, paper) {
  if (deckTimers.has(meetingId)) window.clearTimeout(deckTimers.get(meetingId));
  const timer = window.setTimeout(async () => {
    try {
      const result = await paperpilotApi.getMeetingDeckStatus(jobId);
      const meeting = meetings.value.find((item) => item.id === meetingId);
      applyDeckJob(meeting || { id: meetingId }, result, paper);
      if (result.done) {
        deckTimers.delete(meetingId);
        showToast(result.success ? "PPT 已生成，可以下载" : (result.message || "PPT 制作失败"));
        return;
      }
      pollDeck(meetingId, jobId, paper);
    } catch (error) {
      deckJobs[meetingId] = {
        ...deckJobs[meetingId],
        status: "running",
        stage: deckJobs[meetingId]?.stage || "刷新状态",
        message: error?.response?.data?.message || "PPT 状态刷新失败，稍后自动重试",
      };
      pollDeck(meetingId, jobId, paper);
    }
  }, 1600);
  deckTimers.set(meetingId, timer);
}

function applyDeckJob(meeting, payload = {}, paper = {}) {
  if (!meeting?.id) return;
  deckJobs[meeting.id] = {
    ...(deckJobs[meeting.id] || {}),
    status: payload.status || (payload.success ? "generated" : "running"),
    progress: payload.success ? 100 : Number(payload.progress ?? deckJobs[meeting.id]?.progress ?? 0),
    stage: payload.stage || deckJobs[meeting.id]?.stage || "",
    message: payload.message || deckJobs[meeting.id]?.message || "",
    paperWorkspaceId: paper.workspaceId || deckJobs[meeting.id]?.paperWorkspaceId || "",
    paperTitle: paper.title || deckJobs[meeting.id]?.paperTitle || "",
    jobId: payload.jobId || deckJobs[meeting.id]?.jobId || "",
    confirmUrl: payload.confirmUrl || deckJobs[meeting.id]?.confirmUrl || "",
    downloadUrl: payload.downloadUrl || deckJobs[meeting.id]?.downloadUrl || "",
  };
  if (deckJobs[meeting.id].downloadUrl) deckJobs[meeting.id].status = "generated";
  if (deckJobs[meeting.id].confirmUrl && confirmOpened.value !== deckJobs[meeting.id].confirmUrl) {
    confirmOpened.value = deckJobs[meeting.id].confirmUrl;
    window.open(deckJobs[meeting.id].confirmUrl, "_blank");
  }
  persistDeckJobs();
}

function resumeDeckJobs() {
  Object.entries(deckJobs).forEach(([meetingId, job]) => {
    if (job?.status === "running" && job.jobId) {
      const paper = papers.value.find((item) => item.workspaceId === job.paperWorkspaceId) || {};
      pollDeck(meetingId, job.jobId, paper);
    }
  });
}

function resumeReviewJobs() {
  Object.entries(reviewJobs).forEach(([workspaceId, job]) => {
    if (job?.status === "running") pollReview(workspaceId);
  });
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
  }, 3000);
}
</script>

<style scoped>
.meeting-timeline-page {
  min-height: 100vh;
  padding: 30px min(44px, 4vw) 64px;
  background: linear-gradient(180deg, #eef4fb 0, #f6f8fb 360px, #f9fbfd 100%);
  color: #152033;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
}

.timeline-header,
.timeline-shell {
  width: min(1420px, 100%);
  margin-inline: auto;
}

.timeline-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.timeline-header span,
.box-title span,
.review-modal header span,
.paper-picker header span {
  color: #1f5ed8;
  font-size: 13px;
  font-weight: 850;
}

.timeline-header h1 {
  margin: 8px 0 0;
  color: #111827;
  font-size: 32px;
  line-height: 1.25;
  letter-spacing: 0;
}

.timeline-header p {
  max-width: 760px;
  margin: 10px 0 0;
  color: #4b5f76;
  font-size: 14px;
  line-height: 1.75;
}

.add-meeting-button,
.primary-button,
.download-button,
.soft-button,
.upload-inline {
  min-height: 42px;
  border-radius: 10px;
  padding: 0 16px;
  font-weight: 850;
  cursor: pointer;
}

.add-meeting-button,
.primary-button,
.download-button {
  border: 0;
  background: #225ce0;
  color: #fff;
}

.add-meeting-button {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  flex: 0 0 auto;
}

.add-meeting-button span {
  color: inherit;
  font-size: 21px;
}

.soft-button,
.upload-inline {
  border: 1px solid #cdd9e8;
  background: #fff;
  color: #172033;
}

.download-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  background: #0f8b65;
}

button:disabled {
  cursor: not-allowed;
  opacity: .56;
}

.timeline-list {
  position: relative;
  display: grid;
  gap: 18px;
  padding-left: 30px;
}

.timeline-list::before {
  content: "";
  position: absolute;
  left: 9px;
  top: 16px;
  bottom: 16px;
  width: 2px;
  border-radius: 999px;
  background: #c8d7e8;
}

.meeting-card {
  position: relative;
  border-radius: 14px;
  padding: 24px;
  background: #fff;
  box-shadow: 0 8px 14px rgba(21, 32, 51, .07);
}

.meeting-card.tone-0 { background: #fff; }
.meeting-card.tone-1 { background: #f7fbff; }
.meeting-card.tone-2 { background: #fbfcf7; }
.meeting-card.tone-3 { background: #f8fbf8; }

.timeline-pin {
  position: absolute;
  left: -31px;
  top: 30px;
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #e9f1fb;
}

.timeline-pin span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #225ce0;
}

.meeting-card-head,
.meeting-card-body,
.generation-grid,
.picker-tools {
  display: grid;
  gap: 14px;
}

.meeting-card-head {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  margin-bottom: 16px;
}

.meeting-date-block {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meeting-date-block input,
.meeting-title-input,
.library-search,
.picker-tools input {
  border: 1px solid #cfdbea;
  border-radius: 10px;
  background: #fff;
  color: #162033;
  outline: 0;
}

.date-summary {
  min-width: 136px;
  border-radius: 12px;
  padding: 10px 12px;
  background: #eef5ff;
}

.date-summary span {
  display: block;
  color: #2457b8;
  font-size: 12px;
  font-weight: 850;
}

.date-summary strong {
  display: block;
  margin-top: 3px;
  color: #111827;
  font-size: 19px;
  line-height: 1.15;
}

.meeting-date-block input {
  height: 38px;
  max-width: 230px;
  padding: 0 10px;
  color: #46576e;
  background: rgba(255, 255, 255, .82);
}

.meeting-head-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.meeting-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  border-radius: 999px;
  padding: 0 12px;
  background: #eef3f8;
  color: #43546a;
  font-size: 12px;
  font-weight: 850;
}

.delete-meeting-button {
  min-height: 34px;
  border: 1px solid #f0c5c5;
  border-radius: 999px;
  padding: 0 12px;
  background: #fff7f7;
  color: #b42323;
  font-size: 12px;
  font-weight: 850;
  cursor: pointer;
}

.delete-meeting-button:hover {
  background: #fee2e2;
}

.meeting-status span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #94a3b8;
}

.meeting-status[data-status="running"] {
  background: #fff1f1;
  color: #b42323;
}

.meeting-status[data-status="running"] span { background: #dc2626; }
.meeting-status[data-status="ready"] {
  background: #e5f7ee;
  color: #047857;
}
.meeting-status[data-status="ready"] span { background: #10b981; }
.meeting-status[data-status="failed"] {
  background: #fff7dc;
  color: #8a4b00;
}
.meeting-status[data-status="failed"] span { background: #d97706; }

.meeting-card-body {
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  align-items: stretch;
}

.meeting-main,
.meeting-side {
  min-width: 0;
}

.meeting-title-input {
  width: 100%;
  min-height: 54px;
  box-sizing: border-box;
  border-color: transparent;
  padding: 0 4px;
  background: transparent;
  color: #111827;
  font-size: 22px;
  font-weight: 900;
}

.meeting-title-input:focus {
  border-color: #b9cbed;
  padding-inline: 12px;
  background: #fff;
}

.meeting-notes {
  display: grid;
  gap: 9px;
  margin-top: 10px;
}

.meeting-notes span {
  color: #27364a;
  font-size: 13px;
  font-weight: 850;
}

.meeting-notes textarea {
  width: 100%;
  min-height: 134px;
  max-height: 220px;
  box-sizing: border-box;
  border: 1px solid #d8e2ee;
  border-radius: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, .74);
  resize: none;
  color: #243247;
  outline: 0;
  font: 14px/1.75 inherit;
}

.meeting-side {
  display: grid;
  gap: 14px;
}

.paper-box,
.generation-action {
  border-radius: 12px;
  background: rgba(255, 255, 255, .78);
  padding: 16px;
}

.box-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.box-title button {
  border: 0;
  border-radius: 999px;
  padding: 7px 11px;
  background: #e8f0fb;
  color: #174fbf;
  font-weight: 850;
  cursor: pointer;
}

.paper-box p {
  margin: 16px 0 0;
  color: #526277;
  line-height: 1.7;
}

.selected-papers {
  display: grid;
  gap: 8px;
  margin-top: 13px;
}

.selected-paper {
  display: grid;
  gap: 5px;
  width: 100%;
  border: 1px solid #d7e2ee;
  border-radius: 10px;
  padding: 11px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.selected-paper strong {
  color: #152033;
  line-height: 1.45;
}

.selected-paper small {
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.generation-grid {
  grid-template-columns: 1fr 1fr;
  align-items: stretch;
  gap: 10px;
}

.progress-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 9px;
}

.progress-label span {
  color: #526277;
  font-size: 12px;
  font-weight: 850;
}

.progress-label strong {
  color: #152033;
  font-size: 16px;
}

.generation-action {
  display: grid;
  grid-template-rows: auto auto auto 1fr;
  min-height: 116px;
  border: 1px solid rgba(203, 216, 231, .72);
}

.generation-step {
  min-height: 34px;
  margin: 0 0 9px;
  color: #52637a;
  font-size: 12px;
  line-height: 1.45;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.generation-progress {
  height: 6px;
  margin: 0 0 11px;
  border-radius: 999px;
  background: #d9e4f5;
  overflow: hidden;
}

.generation-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #225ce0, #7aa2ff);
}

.ppt-progress i {
  background: linear-gradient(90deg, #2563eb, #10b981);
}

.generation-action button,
.generation-action a {
  width: 100%;
  box-sizing: border-box;
  align-self: end;
  min-height: 38px;
  border-radius: 9px;
}

.generation-action .soft-button {
  border-color: #c7d8ef;
  background: #f8fbff;
  color: #194fbf;
}

.generation-action .primary-button,
.generation-action .download-button {
  box-shadow: none;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, .34);
}

.modal-panel {
  width: min(980px, 100%);
  max-height: min(82vh, 900px);
  overflow: auto;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, .18);
}

.modal-panel header {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  border-bottom: 1px solid #e5edf6;
  background: #fff;
}

.modal-panel header h2 {
  margin: 6px 0 0;
  color: #111827;
  font-size: 22px;
  line-height: 1.4;
}

.modal-panel header button {
  width: 36px;
  height: 36px;
  border: 1px solid #d5e0eb;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font-size: 20px;
}

.picker-tools {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px 24px 0;
}

.picker-tools input {
  height: 42px;
  padding: 0 13px;
}

.upload-inline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.upload-inline input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.picker-paper-list,
.paper-loading {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 18px 24px 24px;
}

.picker-paper {
  display: grid;
  gap: 7px;
  border: 1px solid #dce6f1;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.picker-paper.selected {
  border-color: #225ce0;
  background: #f2f7ff;
}

.picker-paper strong {
  color: #152033;
  line-height: 1.45;
}

.picker-paper small {
  color: #617188;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pdf-badge {
  width: fit-content;
  border-radius: 999px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 850;
}

.pdf-badge.ready {
  color: #047857;
  background: #dff6ec;
}

.pdf-badge.missing {
  color: #8a4b00;
  background: #fff1d7;
}

.paper-loading span {
  height: 110px;
  border-radius: 12px;
  background: linear-gradient(90deg, #f4f7fb, #eaf0f7, #f4f7fb);
  background-size: 220% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

.review-modal {
  width: min(1080px, 100%);
}

.review-loading,
.review-modal-actions {
  padding: 20px 24px 0;
}

.wide-progress {
  height: 10px;
  margin-top: 14px;
  border-radius: 999px;
  background: #e5edf7;
  overflow: hidden;
}

.wide-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #225ce0;
}

.review-modal-actions {
  display: flex;
  gap: 10px;
}

.review-point-list {
  display: grid;
  gap: 12px;
  padding: 18px 24px 26px;
}

.review-point {
  position: relative;
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: 14px;
  border-radius: 12px;
  padding: 14px 48px 14px 14px;
  background: #f8fbff;
}

.review-point:nth-child(4n + 1) { background: #f6faff; color: #17427a; }
.review-point:nth-child(4n + 2) { background: #f8fbf7; color: #276749; }
.review-point:nth-child(4n + 3) { background: #fffaf3; color: #8a4b00; }
.review-point:nth-child(4n + 4) { background: #f8f7ff; color: #4c3b8f; }

.review-point strong {
  display: block;
  color: currentColor;
  font-size: 15px;
}

.review-point small {
  display: block;
  margin-top: 6px;
  color: color-mix(in srgb, currentColor 64%, #64748b);
  line-height: 1.5;
}

.copy-section-button {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 30px;
  height: 30px;
  border: 1px solid color-mix(in srgb, currentColor 28%, #d5e0eb);
  border-radius: 9px;
  padding: 0;
  background: rgba(255, 255, 255, .72);
  color: currentColor;
  cursor: pointer;
}

.copy-section-button:hover {
  background: #fff;
}

.copy-section-button svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.review-point textarea {
  min-height: 92px;
  height: auto;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, currentColor 20%, #d5e0eb);
  border-radius: 10px;
  padding: 12px;
  resize: none;
  background: rgba(255, 255, 255, .86);
  color: currentColor;
  outline: 0;
  font: 14px/1.75 inherit;
  white-space: pre-wrap;
}

.review-point textarea::placeholder {
  color: color-mix(in srgb, currentColor 46%, #94a3b8);
}

.meeting-toast {
  position: fixed;
  left: 50%;
  bottom: 24px;
  z-index: 60;
  transform: translateX(-50%);
  max-width: min(640px, calc(100vw - 32px));
  border-radius: 10px;
  padding: 12px 16px;
  background: #152033;
  color: #fff;
  font-size: 13px;
}

.modal-fade-enter-active,
.modal-fade-leave-active,
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}

@keyframes shimmer {
  from { background-position: 120% 0; }
  to { background-position: -120% 0; }
}

@media (max-width: 1120px) {
  .meeting-card-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .meeting-timeline-page {
    padding: 20px 12px 42px;
  }

  .timeline-header,
  .meeting-card-head {
    grid-template-columns: 1fr;
    display: grid;
  }

  .add-meeting-button,
  .picker-tools {
    width: 100%;
  }

  .generation-grid,
  .picker-tools,
  .picker-paper-list,
  .paper-loading,
  .review-point {
    grid-template-columns: 1fr;
  }

  .timeline-list {
    padding-left: 24px;
  }

  .timeline-pin {
    left: -27px;
  }
}

@media (max-width: 520px) {
  .meeting-card {
    padding: 16px;
  }

  .meeting-title-input {
    font-size: 18px;
  }

  .modal-backdrop {
    padding: 10px;
  }

  .review-modal-actions {
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
