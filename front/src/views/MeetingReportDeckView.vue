<template>
  <div class="meeting-page">
    <header class="meeting-topbar">
      <div class="title-cluster">
        <span class="section-label">组会汇报</span>
        <h1>上传论文，交给 PPT Master 生成组会汇报</h1>
        <p>只需要上传本次真正要汇报的 PDF。参数选择、设计确认、逐页 SVG、质检和导出交给已安装的 PPT Master skill 流程处理。</p>
      </div>
    </header>

    <main class="meeting-shell">
      <section class="single-upload-panel">
        <div class="upload-copy">
          <span class="section-label">PDF Source</span>
          <h2>汇报主论文</h2>
          <p>上传一篇 PDF 后，系统会使用组会汇报专用模型池里的 GPT5.5 中转进行论文精读，并调用 PPT Master skill 生成 PPTX。</p>
        </div>

        <label class="pdf-dropzone" :class="{ ready: reportPaperFile }">
          <input type="file" accept="application/pdf,.pdf" @change="selectReportPaper" />
          <span class="drop-icon">{{ reportPaperFile ? "PDF" : "+" }}</span>
          <div>
            <strong>{{ reportPaperFile?.name || "选择或拖入 PDF 论文" }}</strong>
            <small>{{ reportPaperFile ? formatFileSize(reportPaperFile.size) : "不再需要选择 3-5 篇对比文献，也不需要手动设置模板参数。" }}</small>
          </div>
        </label>
      </section>

      <section class="deck-dock">
        <div class="dock-status" :class="{ ready: canSubmitDeck }">
          <span>{{ canSubmitDeck ? "Ready" : "Waiting" }}</span>
          <strong>{{ canSubmitDeck ? "可以生成 PPT" : "等待上传 PDF" }}</strong>
          <small>PPT Master skill 会接管后续参数确认和设计流程。</small>
        </div>
        <div class="dock-actions">
          <button type="button" class="primary-action" :disabled="!canSubmitDeck || generating" @click="generateDeck">
            {{ generating ? `${Math.round(deckJob.progress || 1)}%` : "生成 PPT" }}
          </button>
          <small class="upload-hint">{{ reportPaperFile ? `主论文：${reportPaperFile.name}` : "请选择一篇 PDF" }}</small>
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

    <Transition name="slide-up">
      <div v-if="toastMessage" class="custom-toast meeting-toast">
        {{ toastMessage }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { API_BASE_URL } from "../services/apiClient";

const generating = ref(false);
const reportPaperFile = ref(null);
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

const canSubmitDeck = computed(() => Boolean(reportPaperFile.value));

onBeforeUnmount(() => {
  stopDeckPolling();
  if (toastTimer) clearTimeout(toastTimer);
});

function selectReportPaper(event) {
  reportPaperFile.value = event.target.files?.[0] || null;
}

function formatFileSize(bytes) {
  const size = Number(bytes || 0);
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  if (size >= 1024) return `${Math.round(size / 1024)} KB`;
  return `${size} B`;
}

async function generateDeck() {
  if (!reportPaperFile.value) {
    showToast("请先上传一篇 PDF 论文");
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
      engine: "ppt-master-skill",
    };
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
.deck-dock,
.single-upload-panel {
  border: 1px solid rgba(20, 32, 51, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
}

.paper-flow,
.single-upload-panel {
  padding: 18px;
}

.single-upload-panel {
  display: grid;
  grid-template-columns: minmax(260px, 0.7fr) minmax(360px, 1fr);
  gap: 18px;
  align-items: stretch;
}

.upload-copy h2 {
  margin: 6px 0 0;
  color: #142033;
  font-size: 22px;
}

.upload-copy p {
  margin: 10px 0 0;
  color: #56657a;
  font-size: 14px;
  line-height: 1.7;
}

.pdf-dropzone {
  position: relative;
  display: flex;
  min-height: 168px;
  align-items: center;
  gap: 18px;
  padding: 24px;
  border: 1px dashed #9ab5dc;
  border-radius: 12px;
  background: #f7fbff;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
}

.pdf-dropzone.ready {
  border-style: solid;
  border-color: #2563eb;
  background: #eef5ff;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.14);
}

.pdf-dropzone input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.drop-icon {
  display: grid;
  width: 68px;
  height: 68px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  background: #2563eb;
  color: #fff;
  font-size: 22px;
  font-weight: 900;
}

.pdf-dropzone strong {
  display: block;
  color: #142033;
  font-size: 18px;
}

.pdf-dropzone small {
  display: block;
  max-width: 620px;
  margin-top: 8px;
  color: #5b6b80;
  font-size: 13px;
  line-height: 1.55;
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
