<template>
  <div class="dual-reader">
    <header class="dual-toolbar">
      <div class="toolbar-title">
        <router-link to="/library" class="back-link">‹</router-link>
        <strong>左右双栏翻译</strong>
        <span :title="paper?.title">{{ paper?.title || "当前论文" }}</span>
      </div>
      <nav>
        <span class="layout-label">左侧原文 · 右侧译文</span>
        <router-link :to="{ path: '/reader', query: { mode: 'line', panel: 'analysis' } }">
          逐段翻译 + AI
        </router-link>
        <button v-if="state === 'FAILURE' || error" @click="startTranslation">重新生成</button>
      </nav>
    </header>

    <main>
      <section v-if="pagePairs.length" class="spread-reader">
        <header class="spread-head">
          <span>原文</span>
          <span>中文译文</span>
        </header>
        <article v-for="pair in pagePairs" :key="pair.index" class="page-spread">
          <figure>
            <canvas :ref="element => setCanvas(pair.left, element)"></canvas>
            <figcaption>原文 · 第 {{ pair.index }} 页</figcaption>
          </figure>
          <figure :class="{ empty: !pair.right }">
            <canvas v-if="pair.right" :ref="element => setCanvas(pair.right, element)"></canvas>
            <div v-else class="empty-page">本页暂无对应译文</div>
            <figcaption>译文 · 第 {{ pair.index }} 页</figcaption>
          </figure>
        </article>
      </section>

      <section v-else class="translation-state">
        <div class="state-mark" :class="{ failed: Boolean(error) }">
          <span v-if="!error"></span>
          <b v-else>!</b>
        </div>
        <h1>{{ error ? "双栏翻译暂不可用" : stateTitle }}</h1>
        <p>{{ error || stateDescription }}</p>
        <div v-if="!error" class="progress-track">
          <i :style="{ width: `${progress}%` }"></i>
        </div>
        <small v-if="!error">{{ progress }}%</small>
        <button v-else @click="startTranslation">再次连接</button>
        <p class="process-note">正在保留公式、图表、目录与页面排版，并生成左右对照稿。</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useLibraryStore } from "../stores/library";

const libraryStore = useLibraryStore();
const state = ref("PENDING");
const progress = ref(3);
const error = ref("");
const pagePairs = reactive([]);
const canvasElements = new Map();
let pollTimer;
let pdfDocument;

const paper = computed(() => libraryStore.activeDocument);
const workspaceId = computed(() => String(paper.value?.workspaceId || paper.value?.id || ""));
const stateTitle = computed(() => {
  if (state.value === "PROGRESS") return "正在生成左右对照译文";
  if (state.value === "SUCCESS") return "译文已生成，正在排版";
  return "正在准备双栏翻译";
});
const stateDescription = computed(() => {
  if (state.value === "PROGRESS") return "首次生成需要分析页面结构；完成后再次打开会直接读取缓存。";
  return "正在读取论文并建立原文与译文的页面对应关系。";
});

function friendlyError(requestError, fallback) {
  const raw = requestError?.response?.data?.message || requestError?.response?.data?.detail || "";
  return String(raw || fallback)
    .replaceAll("PDFMathTranslate", "双栏翻译")
    .replaceAll("pdf2zh", "翻译引擎");
}

async function startTranslation() {
  clearInterval(pollTimer);
  error.value = "";
  state.value = "PENDING";
  progress.value = 3;
  try {
    const started = await paperpilotApi.startPdfMathTranslation(workspaceId.value, "google");
    if (String(started?.state || "").toUpperCase() === "SUCCESS") {
      await loadTranslatedPdf();
      return;
    }
    await refreshStatus();
    pollTimer = setInterval(refreshStatus, 1200);
  } catch (requestError) {
    error.value = friendlyError(requestError, "双栏翻译服务暂时不可用，请稍后重试。");
  }
}

async function refreshStatus() {
  try {
    const result = await paperpilotApi.getPdfMathTranslationStatus(workspaceId.value);
    state.value = String(result?.state || "PENDING").toUpperCase();
    const info = result?.info || {};
    const current = Number(info.n || 0);
    const total = Number(info.total || 0);
    progress.value = state.value === "SUCCESS"
      ? 100
      : total > 0
        ? Math.max(5, Math.min(98, Math.round((current / total) * 100)))
        : Math.min(92, progress.value + 3);
    if (state.value === "SUCCESS") {
      clearInterval(pollTimer);
      await loadTranslatedPdf();
    } else if (state.value === "FAILURE") {
      clearInterval(pollTimer);
      error.value = "生成失败，请检查翻译服务后重试。";
    }
  } catch (requestError) {
    clearInterval(pollTimer);
    error.value = friendlyError(requestError, "查询翻译进度失败。");
  }
}

async function loadTranslatedPdf() {
  const blob = await paperpilotApi.getPdfMathDualPdf(workspaceId.value);
  const [pdfjs, workerModule] = await Promise.all([
    import("pdfjs-dist"),
    import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
  ]);
  pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;
  pdfDocument = await pdfjs.getDocument({ data: await blob.arrayBuffer() }).promise;
  pagePairs.splice(0);
  for (let page = 1, index = 1; page <= pdfDocument.numPages; page += 2, index += 1) {
    pagePairs.push({ index, left: page, right: page + 1 <= pdfDocument.numPages ? page + 1 : null });
  }
  await nextTick();
  await Promise.all(pagePairs.flatMap(pair => [pair.left, pair.right].filter(Boolean).map(renderPage)));
}

function setCanvas(pageNumber, element) {
  if (pageNumber && element) canvasElements.set(pageNumber, element);
}

async function renderPage(pageNumber) {
  const canvas = canvasElements.get(pageNumber);
  if (!canvas || !pdfDocument) return;
  const page = await pdfDocument.getPage(pageNumber);
  const baseViewport = page.getViewport({ scale: 1 });
  const targetWidth = Math.min(820, Math.max(420, (window.innerWidth - 72) / 2));
  const scale = targetWidth / baseViewport.width;
  const viewport = page.getViewport({ scale });
  const outputScale = Math.min(1.5, window.devicePixelRatio || 1);
  canvas.width = Math.floor(viewport.width * outputScale);
  canvas.height = Math.floor(viewport.height * outputScale);
  canvas.style.width = `${viewport.width}px`;
  canvas.style.height = `${viewport.height}px`;
  await page.render({
    canvasContext: canvas.getContext("2d", { alpha: false }),
    viewport,
    transform: outputScale === 1 ? null : [outputScale, 0, 0, outputScale, 0, 0],
  }).promise;
}

onMounted(async () => {
  await libraryStore.hydrateLibrary();
  if (!workspaceId.value) {
    error.value = "未选择需要翻译的文献，请返回文献库重新打开。";
    return;
  }
  startTranslation();
});

onBeforeUnmount(() => {
  clearInterval(pollTimer);
  pdfDocument?.destroy?.();
});
</script>

<style scoped>
.dual-reader { height: 100vh; overflow: hidden; color: #202733; background: #dfe4eb; font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif; }
.dual-toolbar { height: 50px; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 0 16px; box-sizing: border-box; border-bottom: 1px solid #d4dae3; background: #fff; }
.toolbar-title, .dual-toolbar nav { display: flex; align-items: center; gap: 12px; min-width: 0; }
.dual-toolbar strong { flex: 0 0 auto; color: #273149; font-size: 13px; }
.toolbar-title > span { overflow: hidden; max-width: 48vw; color: #637083; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.dual-toolbar nav a, .dual-toolbar nav button { padding: 7px 11px; border: 0; border-radius: 7px; color: #fff; background: #5b35d5; font-size: 11px; font-weight: 750; text-decoration: none; cursor: pointer; }
.layout-label { color: #667085; font-size: 11px; }
.back-link { display: grid; width: 30px; height: 30px; place-items: center; color: #263244; font-size: 24px; text-decoration: none; }
.dual-reader main { height: calc(100vh - 50px); overflow: auto; }
.spread-reader { width: min(1760px, calc(100% - 28px)); margin: 0 auto; padding: 14px 0 56px; }
.spread-head { position: sticky; top: 0; z-index: 3; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; padding: 7px 0; background: #dfe4eb; }
.spread-head span { padding: 7px 12px; border-radius: 7px; color: #445064; background: #f7f8fa; font-size: 11px; font-weight: 750; text-align: center; }
.page-spread { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); align-items: start; gap: 12px; margin-bottom: 14px; }
.page-spread figure { min-width: 0; margin: 0; overflow: hidden; background: #fff; box-shadow: 0 2px 7px rgba(20, 31, 48, .16); }
.page-spread canvas { display: block; max-width: 100%; height: auto !important; margin: 0 auto; background: #fff; }
.page-spread figcaption { padding: 6px 10px; border-top: 1px solid #e5e8ed; color: #7a8494; font-size: 10px; text-align: center; }
.empty-page { min-height: 420px; display: grid; place-items: center; color: #98a2b3; font-size: 12px; }
.translation-state { width: min(520px, calc(100% - 40px)); margin: 0 auto; padding-top: min(18vh, 170px); text-align: center; }
.state-mark { width: 44px; height: 44px; display: grid; place-items: center; margin: 0 auto 18px; border-radius: 50%; background: #087f8c; }
.state-mark span { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,.42); border-top-color: #fff; border-radius: 50%; animation: spin .8s linear infinite; }
.state-mark.failed { color: #fff; background: #c4322b; }
.translation-state h1 { margin: 0 0 10px; font-size: 20px; }
.translation-state p { margin: 0 auto; color: #667085; font-size: 13px; line-height: 1.7; }
.progress-track { height: 5px; overflow: hidden; margin: 24px 0 8px; border-radius: 99px; background: #cbd2dd; }
.progress-track i { display: block; height: 100%; border-radius: inherit; background: #087f8c; transition: width 180ms ease-out; }
.translation-state small { color: #7a8494; font-size: 11px; }
.translation-state > button { margin-top: 20px; padding: 8px 14px; border: 0; border-radius: 7px; color: #fff; background: #5b35d5; cursor: pointer; }
.process-note { margin-top: 28px !important; color: #8792a3 !important; font-size: 11px !important; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 820px) {
  .layout-label { display: none; }
  .page-spread { grid-template-columns: minmax(0, 1fr); }
  .spread-head { display: none; }
}
@media (prefers-reduced-motion: reduce) { .state-mark span { animation: none; } .progress-track i { transition: none; } }
</style>
