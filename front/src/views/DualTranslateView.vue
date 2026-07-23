<template>
  <div class="dual-reader">
    <header class="dual-toolbar">
      <div class="toolbar-title">
        <router-link to="/library" class="back-link">‹</router-link>
        <strong>对照翻译</strong>
        <span :title="paper?.title">{{ paper?.title || "当前论文" }}</span>
      </div>
      <nav>
        <span class="layout-label" :title="isDualPdfMode ? '开源 PDFMathTranslate (pdf2zh) 模型驱动' : '内置 AI 高保真双栏引擎驱动'">
          {{ isDualPdfMode ? '开源 pdf2zh 模型渲染' : '左侧原文 · 右侧中文译文' }}
        </span>
        <router-link :to="{ path: '/reader', query: { mode: 'line', panel: 'analysis' } }">
          逐段翻译 + AI
        </router-link>
        <button
          class="dual-theme-toggle-btn"
          :title="isDarkTheme ? '切换为日间明亮模式' : '切换为夜间深色模式'"
          @click="toggleTheme"
        >
          <svg v-if="isDarkTheme" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
        </button>
        <button v-if="state === 'FAILURE' || error" @click="startTranslation">重新生成</button>
      </nav>
    </header>

    <main>
      <section v-if="pagePairs.length" class="spread-reader">
        <header class="spread-head">
          <span>左侧：英文原文 PDF</span>
          <span>右侧：中文译文对照</span>
        </header>
        <article v-for="pair in pagePairs" :key="pair.index" class="page-spread">
          <!-- 左侧：英文原文 PDF Canvas -->
          <figure class="pdf-canvas-figure">
            <canvas :ref="element => setCanvas(`left-${pair.index}`, element)"></canvas>
            <figcaption>英文原文 · 第 {{ pair.index }} 页</figcaption>
          </figure>

          <!-- 右侧：中文译文板 (PDF Canvas 或 结构化中文段落) -->
          <figure v-if="isDualPdfMode" class="pdf-canvas-figure">
            <canvas :ref="element => setCanvas(`right-${pair.index}`, element)"></canvas>
            <figcaption>中文译文 · 第 {{ pair.index }} 页</figcaption>
          </figure>

          <div v-else class="translated-text-card-column">
            <header class="translated-column-head">
              <span>中文译文 · 第 {{ pair.index }} 页</span>
            </header>
            <div class="translated-blocks-wrapper">
              <template v-if="pair.blocks && pair.blocks.length">
                <div
                  v-for="block in pair.blocks"
                  :key="block.id"
                  class="translated-block-item"
                  :class="`kind-${block.kind}`"
                >
                  <p class="source-text-muted">{{ block.text }}</p>
                  <p class="target-translation-text" :class="{ loading: !block.translation }">
                    {{ block.translation || '正在翻译本段…' }}
                  </p>
                </div>
              </template>
              <div v-else class="empty-block-note">
                <p class="target-translation-text">正在读取并翻译本页段落…</p>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section v-else class="translation-state">
        <div class="state-mark" :class="{ failed: Boolean(error) }">
          <span v-if="!error"></span>
          <b v-else>!</b>
        </div>
        <h1>{{ error ? "对照翻译暂不可用" : stateTitle }}</h1>
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
useScrollReveal(".dual-translate-page");
import { useScrollReveal } from "../composables/useScrollReveal";
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useLibraryStore } from "../stores/library";

const libraryStore = useLibraryStore();
const state = ref("PENDING");
const progress = ref(3);
const error = ref("");
const pagePairs = reactive([]);
const canvasElements = new Map();
const pageBlocksMap = reactive({});
const isDualPdfMode = ref(false);
let pollTimer;
let pdfDocument = null;

const currentTheme = ref(localStorage.getItem("paperpilot_theme") || "dark");
const isDarkTheme = computed(() => currentTheme.value === "dark");

function applyTheme(theme) {
  currentTheme.value = theme;
  document.documentElement.setAttribute("data-theme", theme);
  localStorage.setItem("paperpilot_theme", theme);
}

function toggleTheme() {
  applyTheme(currentTheme.value === "dark" ? "light" : "dark");
}

const paper = computed(() => libraryStore.activeDocument);
const workspaceId = computed(() => String(paper.value?.workspaceId || paper.value?.id || ""));
const stateTitle = computed(() => {
  if (state.value === "PROGRESS") return "正在生成对照译文";
  if (state.value === "SUCCESS") return "译文已生成，正在排版";
  return "正在准备对照翻译";
});
const stateDescription = computed(() => {
  if (state.value === "PROGRESS") return "首次生成需要分析页面结构；完成后再次打开会直接读取缓存。";
  return "正在读取论文并建立原文与译文的页面对照关系。";
});

function friendlyError(requestError, fallback) {
  const raw = requestError?.response?.data?.message || requestError?.response?.data?.detail || "";
  return String(raw || fallback)
    .replaceAll("PDFMathTranslate", "对照翻译")
    .replaceAll("pdf2zh", "翻译引擎");
}

function setCanvas(key, element) {
  if (key && element) {
    canvasElements.set(key, element);
  }
}

async function renderSingleCanvas(doc, pageNum, canvas) {
  if (!canvas || !doc) return;
  try {
    const page = await doc.getPage(pageNum);
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
  } catch (err) {
    console.warn("render single canvas error", pageNum, err);
  }
}

async function translateAllPageBlocks() {
  pagePairs.forEach(pair => {
    if (Array.isArray(pair.blocks)) {
      pair.blocks.forEach(async block => {
        if (!block.translation && block.text && !['figure', 'table', 'equation'].includes(block.kind)) {
          try {
            const res = await paperpilotApi.translate({
              text: block.text,
              provider: "google",
              sourceLang: "auto",
              targetLang: "zh-CN",
            });
            block.translation = String(res?.translatedText || res?.text || "").trim();
          } catch (e) {
            console.warn("dual block translate error", e);
          }
        }
      });
    }
  });
}

async function loadNativePdfDualView() {
  try {
    state.value = "PROGRESS";
    progress.value = 30;
    isDualPdfMode.value = false;

    const paperObj = paper.value || {};
    const pdfUrl = paperpilotApi.buildPdfProxyUrl(paperObj.pdfUrl || paperObj.paperUrl || "");
    if (!pdfUrl) throw new Error("缺失论文 PDF 资源");

    const [pdfjs, workerModule] = await Promise.all([
      import("pdfjs-dist"),
      import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
    ]);
    pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;
    const loadingTask = pdfjs.getDocument(pdfUrl);
    pdfDocument = await loadingTask.promise;

    // 获取 Mineru 结构化页面段落数据
    try {
      const parsed = await paperpilotApi.getParsedDocument(workspaceId.value);
      if (parsed && Array.isArray(parsed.pages)) {
        parsed.pages.forEach(p => {
          pageBlocksMap[p.pageNumber] = p.blocks || [];
        });
      }
    } catch (e) {
      console.warn("fetch parsed pages for dual view failed", e);
    }

    pagePairs.splice(0);
    for (let i = 1; i <= pdfDocument.numPages; i++) {
      const blocks = pageBlocksMap[i] || [];
      pagePairs.push({
        index: i,
        leftPageNum: i,
        blocks: blocks,
      });
    }

    state.value = "SUCCESS";
    progress.value = 100;
    await nextTick();

    // 渲染左侧原版英文 PDF
    await Promise.all(pagePairs.map(async pair => {
      const leftCanvas = canvasElements.get(`left-${pair.index}`);
      if (leftCanvas) await renderSingleCanvas(pdfDocument, pair.leftPageNum, leftCanvas);
    }));

    // 自动为右侧段落填充中文译文
    translateAllPageBlocks();
  } catch (err) {
    console.warn("native pdf dual view fallback failed", err);
    error.value = "对照翻译暂不可用，请确保论文已上传 PDF。";
  }
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
    console.warn("pdfmath translation server offline, switching to native dual reader", requestError);
    await loadNativePdfDualView();
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
      await loadNativePdfDualView();
    }
  } catch (requestError) {
    clearInterval(pollTimer);
    await loadNativePdfDualView();
  }
}

async function loadTranslatedPdf() {
  isDualPdfMode.value = true;
  const blob = await paperpilotApi.getPdfMathDualPdf(workspaceId.value);
  const [pdfjs, workerModule] = await Promise.all([
    import("pdfjs-dist"),
    import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
  ]);
  pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;
  pdfDocument = await pdfjs.getDocument({ data: await blob.arrayBuffer() }).promise;
  pagePairs.splice(0);
  for (let page = 1, index = 1; page <= pdfDocument.numPages; page += 2, index += 1) {
    pagePairs.push({
      index,
      hasRight: page + 1 <= pdfDocument.numPages,
      leftPageNum: page,
      rightPageNum: page + 1 <= pdfDocument.numPages ? page + 1 : null,
    });
  }
  await nextTick();
  await Promise.all(pagePairs.map(async pair => {
    const leftCanvas = canvasElements.get(`left-${pair.index}`);
    const rightCanvas = canvasElements.get(`right-${pair.index}`);
    if (leftCanvas && pair.leftPageNum) await renderSingleCanvas(pdfDocument, pair.leftPageNum, leftCanvas);
    if (rightCanvas && pair.rightPageNum) await renderSingleCanvas(pdfDocument, pair.rightPageNum, rightCanvas);
  }));
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
/* ── DARK MODE ADAPTATIONS FOR DUAL TRANSLATE VIEW ── */
:root[data-theme="dark"] .dual-reader {
  background: #08080c;
  color: #e2e2e6;
}

:root[data-theme="dark"] .dual-toolbar {
  background: rgba(14, 14, 20, 0.95);
  border-bottom-color: rgba(255, 255, 255, 0.08);
  color: #f4f4f6;
}

:root[data-theme="dark"] .dual-toolbar strong {
  color: #f4f4f6;
}

:root[data-theme="dark"] .toolbar-title > span {
  color: #a1a1aa;
}

:root[data-theme="dark"] .back-link {
  color: #f4f4f6;
}

:root[data-theme="dark"] .spread-head {
  background: #08080c;
}

:root[data-theme="dark"] .spread-head span {
  background: rgba(255, 255, 255, 0.06);
  color: #cbd5e1;
}

:root[data-theme="dark"] .page-spread figure {
  background: #0e0e14;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

:root[data-theme="dark"] .page-spread canvas {
  background: #0e0e14;
}

:root[data-theme="dark"] .page-spread figcaption {
  border-top-color: rgba(255, 255, 255, 0.08);
  color: #a1a1aa;
}

:root[data-theme="dark"] .empty-page {
  color: #71717a;
}

:root[data-theme="dark"] .translation-state h1 {
  color: #f4f4f6;
}

:root[data-theme="dark"] .translation-state p {
  color: #a1a1aa;
}
</style>

