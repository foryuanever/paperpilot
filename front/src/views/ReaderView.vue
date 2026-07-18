<template>
  <div class="reader-workbench">
    <header class="reader-toolbar">
      <div class="reader-toolbar-start">
        <router-link class="reader-back" to="/library" title="返回文献库" aria-label="返回文献库">←</router-link>
        <div class="reader-document-meta">
          <span class="reader-document-title" :title="activePaper.title">{{ activePaper.title }}</span>
          <small class="reader-document-source">{{ paperSourceLabel || "文献阅读" }}</small>
        </div>
      </div>

      <div class="reader-tools" role="toolbar" aria-label="阅读设置">
        <button
          class="reader-translate-toggle"
          :class="{ active: autoTranslate }"
          :aria-pressed="autoTranslate"
          :title="autoTranslate ? '关闭全文翻译' : '开启全文翻译'"
          @click="toggleTranslation"
        >
          <span class="reader-translate-icon" aria-hidden="true">
            <span class="lang-mark-source">文</span>
            <span class="lang-mark-target">A</span>
          </span>
          <span class="reader-translate-label">全文翻译</span>
        </button>
        <button
          class="reader-eraser-button"
          :class="{ restorable: !annotations.length && clearedAnnotationSnapshot.length }"
          :title="!annotations.length && clearedAnnotationSnapshot.length ? '恢复刚清除的标注' : '清除全部标注'"
          :aria-label="!annotations.length && clearedAnnotationSnapshot.length ? '恢复刚清除的标注' : '清除全部标注'"
          @click="clearAllAnnotations"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M16.7 3.9 21 8.2a2 2 0 0 1 0 2.8l-8.1 8.1H6.5L3 15.6a2 2 0 0 1 0-2.8l10.9-8.9a2 2 0 0 1 2.8 0Z" />
            <path d="m11.2 6.1 6.7 6.7" />
            <path d="M3 21h18" />
          </svg>
        </button>
        <div class="reader-zoom-control" role="group" aria-label="正文缩放">
          <button title="缩小正文" aria-label="缩小正文" @click="contentScale = Math.max(0.8, contentScale - 0.1)">−</button>
          <span class="scale-value">{{ Math.round(contentScale * 100) }}%</span>
          <button title="放大正文" aria-label="放大正文" @click="contentScale = Math.min(1.5, contentScale + 0.1)">＋</button>
        </div>
      </div>

      <div class="reader-toolbar-end">
        <div class="reader-status" aria-label="阅读状态">
          <span class="reader-status-item">
            <small>进度</small>
            <strong>{{ readingProgress }}%</strong>
          </span>
          <span class="reader-status-item">
            <small>页码</small>
            <strong>{{ loadedPages }}/{{ totalPages || "−" }}</strong>
          </span>
        </div>
        <button class="reader-pdf-action" title="打开原始 PDF" @click="openOriginalPdf">
          <span class="pdf-label-long">原文 PDF</span>
          <span class="pdf-label-short">PDF</span>
        </button>
      </div>
      <div class="reader-progress-track" aria-label="阅读进度">
        <div class="reader-progress-fill" :style="{ width: `${readingProgress}%` }"></div>
      </div>
    </header>

    <div class="reader-body" :class="{ 'assistant-wide': assistantExpanded }">
      <aside class="reader-assistant" :class="{ collapsed: assistantCollapsed, expanded: assistantExpanded }">
        <div class="assistant-tabs">
          <button :class="{ active: assistantTab === 'chat' }" @click="assistantTab = 'chat'">内容详解</button>
          <button :class="{ active: assistantTab === 'outline' }" @click="assistantTab = 'outline'">目录</button>
          <button :class="{ active: assistantTab === 'figures' }" @click="assistantTab = 'figures'">图表</button>
          <button
            v-if="assistantTab === 'chat' && !assistantCollapsed"
            class="expand-button"
            :title="assistantExpanded ? '收回分析内容' : '向右展开分析内容'"
            @click="assistantExpanded = !assistantExpanded"
          >
            {{ assistantExpanded ? "‹" : "›" }}
          </button>
          <button class="collapse-button" @click="toggleAssistantCollapse">
            {{ assistantCollapsed ? "›" : "‹" }}
          </button>
        </div>

        <template v-if="!assistantCollapsed">
          <div v-if="assistantTab === 'chat'" class="assistant-scroll">
            <ReaderReportPanel :workspace-id="workspaceId" :paper="activePaper" :wide="assistantExpanded" />

          </div>

          <div v-else-if="assistantTab === 'outline'" class="assistant-scroll">
            <section>
              <h3>论文目录</h3>
              <button
                v-for="item in documentOutline"
                :key="`${item.page}-${item.text}`"
                class="outline-item"
                @click="scrollToPage(item.page)"
              >
                <span>{{ item.text }}</span>
                <small>{{ item.page }}</small>
              </button>
            </section>
          </div>

          <div v-else class="assistant-scroll">
            <section>
              <h3>论文图表</h3>
              <button
                v-for="item in documentFigures"
                :key="item.block.id"
                class="outline-item"
                @click="scrollToPage(item.pageNumber)"
              >
                <span>{{ item.block.text || (item.block.kind === "table" ? "表格" : "图像") }}</span>
                <small>{{ item.pageNumber }}</small>
              </button>
              <p v-if="!documentFigures.length" class="assistant-empty">暂未识别到图表</p>
            </section>
          </div>

        </template>
      </aside>

      <main ref="readingScroll" class="reading-stage" @mouseup="captureSelection" @contextmenu.prevent="openColorMenu" @scroll="handleReadingScroll" @click="closeColorMenu">
        <div
          class="reading-column"
          :style="{ '--reader-scale': contentScale }"
        >
          <div v-if="loadingPdf" class="reader-state reader-loading-state">
            <div class="reader-state-mark"><span></span></div>
            <h1>正在准备逐段翻译</h1>
            <p>{{ parsingMessage }}</p>
            <div class="reader-loading-track"><i :style="{ width: `${parsingProgress}%` }"></i></div>
            <small>{{ parsingProgress }}%</small>
            <p class="reader-process-note">正在识别完整段落、阅读顺序与图表位置。</p>
          </div>
          <div v-else-if="loadError" class="reader-state error">{{ loadError }}</div>

          <article v-else class="reflow-document">
            <header class="paper-heading">
              <span class="paper-source">{{ paperSourceLabel || "Academic paper" }}</span>
              <h1>{{ activePaper.title }}</h1>
              <p>{{ activePaper.authors }}</p>
              <div v-if="hasAbstract && !structuredHasAbstract" class="paper-abstract">
                <strong>Abstract</strong>
                <p class="source-paragraph selectable-paragraph" data-block-id="abstract">
                  <template v-for="segment in annotationSegments('abstract', abstractText)" :key="segment.key">
                    <span v-if="segment.annotated" class="annotation-highlight" :title="segment.note" @click="editAnnotation(segment.annotation, $event)">
                      {{ segment.text }}<button type="button" class="annotation-delete" title="删除这条标注" aria-label="删除这条标注" @click.stop="removeAnnotation(segment.annotation.id)">×</button>
                    </span>
                    <template v-else>{{ segment.text }}</template>
                  </template>
                </p>
                <p v-if="autoTranslate" class="translated-paragraph selectable-paragraph" data-block-id="abstract" :class="{ pending: abstractTranslating || !abstractTranslation }">
                  {{ abstractTranslating ? "摘要重新翻译中…" : abstractTranslation || "摘要译文准备中…" }}<button
                    v-if="!abstractTranslating"
                    class="translation-reload"
                    :disabled="abstractTranslating"
                    title="重新翻译摘要"
                    aria-label="重新翻译摘要"
                    @click="translateAbstract(true)"
                  >↻</button>
                </p>
                <aside
                  v-if="annotationForBlock('abstract')"
                  class="block-annotation-note"
                  :title="annotationForBlock('abstract').note"
                >
                  <div class="block-annotation-actions">
                    <button type="button" aria-label="编辑摘要批注" title="编辑批注" @click.stop="editAnnotation(annotationForBlock('abstract'), $event)">✎</button>
                    <button type="button" class="block-annotation-remove" aria-label="删除摘要批注" title="删除批注" @click.stop="removeAnnotation(annotationForBlock('abstract').id)">×</button>
                  </div>
                  <p @click="editAnnotation(annotationForBlock('abstract'), $event)">{{ annotationForBlock('abstract').note }}</p>
                </aside>
              </div>
            </header>

            <section
              v-for="page in pages"
              :key="page.pageNumber"
              class="reflow-page"
              :data-page="page.pageNumber"
            >
              <div class="page-marker">第 {{ page.pageNumber }} 页</div>
              <template v-for="block in page.blocks" :key="block.id">
                <h2 v-if="block.kind === 'heading'" class="source-heading">{{ block.text }}</h2>
                <figure v-else-if="block.kind === 'figure' || block.kind === 'table'" class="pdf-figure-card">
                  <button
                    v-if="block.imageUrl"
                    class="pdf-figure-image-button"
                    :aria-label="`查看${block.text}原图`"
                    @click="viewParsedFigure(block, page.pageNumber)"
                  >
                    <img :src="block.imageUrl" :alt="block.text" class="pdf-figure-image" />
                  </button>
                  <div v-else-if="block.html" class="mineru-table" v-html="block.html"></div>
                  <div v-else class="pdf-figure-placeholder">图表已识别，暂无可显示的预览</div>
                  <figcaption>
                    <span class="pdf-figure-caption">{{ block.text || (block.kind === "table" ? "表格" : "图像") }}</span>
                    <button v-if="block.imageUrl" class="pdf-figure-view" @click="viewParsedFigure(block, page.pageNumber)">查看原图</button>
                  </figcaption>
                </figure>
                <div v-else-if="block.kind === 'equation'" class="mineru-equation">{{ block.text }}</div>
                <div v-else-if="block.kind === 'references'" class="reference-block selectable-paragraph" :data-block-id="block.id">
                  <template v-for="segment in annotationSegments(block.id, block.text)" :key="segment.key">
                    <span v-if="segment.annotated" class="annotation-highlight" :title="segment.note" @click="editAnnotation(segment.annotation, $event)">
                      {{ segment.text }}<button type="button" class="annotation-delete" title="删除这条标注" aria-label="删除这条标注" @click.stop="removeAnnotation(segment.annotation.id)">×</button>
                    </span>
                    <template v-else>{{ segment.text }}</template>
                  </template>
                </div>
                <p v-else class="source-paragraph selectable-paragraph" :data-block-id="block.id">
                  <template v-for="segment in annotationSegments(block.id, block.text)" :key="segment.key">
                    <span v-if="segment.annotated" class="annotation-highlight" :title="segment.note" @click="editAnnotation(segment.annotation, $event)">
                      {{ segment.text }}<button type="button" class="annotation-delete" title="删除这条标注" aria-label="删除这条标注" @click.stop="removeAnnotation(segment.annotation.id)">×</button>
                    </span>
                    <template v-else>{{ segment.text }}</template>
                  </template>
                </p>
                <div v-if="autoTranslate && !['figure', 'table', 'equation', 'references'].includes(block.kind)" class="translation-unit">
                  <p
                    class="translated-paragraph selectable-paragraph"
                    :data-block-id="block.id"
                    :class="{ pending: block.translating || !block.translation, error: block.translationError }"
                  >
                    {{ block.translating ? "本段翻译中…" : block.translationError || block.translation || "译文准备中…" }}<button
                      v-if="!block.translating"
                      class="translation-reload"
                      :disabled="block.translating"
                      :title="`重新翻译（${providerLabel(block.translationProvider)}）`"
                      aria-label="重新翻译本段"
                      @click="cycleAndRetranslate(block)"
                    >↻</button>
                  </p>
                </div>
                <aside
                  v-if="annotationForBlock(block.id)"
                  class="block-annotation-note"
                  :title="annotationForBlock(block.id).note"
                >
                  <div class="block-annotation-actions">
                    <button type="button" aria-label="编辑本段批注" title="编辑批注" @click.stop="editAnnotation(annotationForBlock(block.id), $event)">✎</button>
                    <button type="button" class="block-annotation-remove" aria-label="删除本段批注" title="删除批注" @click.stop="removeAnnotation(annotationForBlock(block.id).id)">×</button>
                  </div>
                  <p @click="editAnnotation(annotationForBlock(block.id), $event)">{{ annotationForBlock(block.id).note }}</p>
                </aside>
              </template>
            </section>
          </article>
        </div>
      </main>

      <nav class="reader-paper-rail" aria-label="文献切换">
        <div class="reader-paper-rail-head">
          <span>文献切换</span>
          <small>{{ currentPage }}/{{ totalPages || loadedPages || "−" }} 页</small>
        </div>
        <button
          v-for="paper in readerPaperTabs"
          :key="paper.id"
          class="reader-paper-tab"
          :class="{ active: paper.id === activePaper.id }"
          :title="paper.title"
          @click="switchReaderPaper(paper.id)"
        >
          <span class="reader-paper-tab-mark">{{ paperInitial(paper) }}</span>
          <span class="reader-paper-tab-text">
            <strong>{{ shortPaperTitle(paper.title, 28) }}</strong>
            <small>{{ paper.source || paper.publishYear || "文献库" }}</small>
          </span>
        </button>
        <div class="reader-page-mini" aria-label="当前论文页码">
          <button
            v-for="page in compactPageTabs"
            :key="page.key"
            :class="{ active: currentPage === page.pageNumber, muted: page.ellipsis }"
            :disabled="page.ellipsis"
            @click="scrollToPage(page.pageNumber)"
          >
            {{ page.label }}
          </button>
        </div>
      </nav>
    </div>

    <div
      v-if="colorMenu.open"
      class="reader-color-menu"
      :style="{ left: colorMenu.x + 'px', top: colorMenu.y + 'px' }"
      @click.stop
    >
      <span class="reader-color-menu-title">字体颜色</span>
      <div class="reader-color-menu-swatches">
        <button
          v-for="color in textColors"
          :key="color.id"
          class="reader-color-menu-swatch"
          :style="{ '--swatch': color.value }"
          :title="color.label"
          :aria-label="`${color.label}字体`"
          @click="applyTextColor(color.value); closeColorMenu()"
        ></button>
      </div>
    </div>
    <section
      v-if="selectionTranslator.open"
      class="selection-translate-popover"
      :style="{ left: `${selectionTranslator.x}px`, top: `${selectionTranslator.y}px` }"
      @click.stop
    >
      <header class="selection-popover-head">
        <div>
          <span class="selection-kicker">Selection</span>
          <strong>选区工具</strong>
          <small>{{ selectionTranslator.source.length }} 字符</small>
        </div>
        <button aria-label="关闭选中内容工具" @click="closeSelectionTranslator">×</button>
      </header>
      <p class="selection-source">{{ selectionTranslator.preview }}</p>
      <div class="selection-color-tools" aria-label="设置选中文字颜色">
        <span>标记颜色</span>
        <button
          v-for="color in textColors"
          :key="color.id"
          :style="{ '--swatch': color.value }"
          :title="`${color.label}标记`"
          :aria-label="`${color.label}标记`"
          @mousedown.prevent
          @click="applySelectionColor(color.value)"
        ></button>
      </div>
      <div class="selection-actions">
        <button
          class="primary-action"
          :disabled="selectionTranslator.loading"
          @click="translateSelection"
        >
          选中翻译
        </button>
        <button :disabled="selectionTranslator.loading" @click="explainSelection">AI 解读</button>
        <button class="annotation-action" @click="openAnnotationEditor">批注</button>
      </div>
      <div v-if="selectionTranslator.loading || selectionTranslator.result || selectionTranslator.error" class="selection-result" :class="{ pending: selectionTranslator.loading, error: selectionTranslator.error }">
        <span v-if="selectionTranslator.loading" class="selection-spinner"></span>
        <div>
          <strong>{{ selectionTranslator.resultTitle || "处理结果" }}</strong>
          <p>{{ selectionTranslator.loading ? selectionTranslator.loadingText : selectionTranslator.error || selectionTranslator.result }}</p>
        </div>
      </div>
      <p v-else class="selection-hint">选中后可翻译、解读，也可以保存为批注。</p>
      <div v-if="selectionTranslator.annotating" class="selection-annotation-editor">
        <textarea v-model="selectionTranslator.annotationDraft" rows="3" placeholder="写下对这段内容的理解、疑问或提醒…"></textarea>
        <div>
          <button @click="selectionTranslator.annotating = false">取消</button>
          <button :disabled="!selectionTranslator.annotationDraft.trim()" @click="saveAnnotation">保存批注</button>
        </div>
      </div>
    </section>

    <button
      class="paper-chat-launcher"
      :class="{ open: paperChat.open }"
      :aria-label="paperChat.open ? '关闭论文问答' : '打开论文问答'"
      @click="paperChat.open = !paperChat.open"
    >
      <svg v-if="!paperChat.open" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3h11A2.5 2.5 0 0 1 20 5.5v8a2.5 2.5 0 0 1-2.5 2.5H11l-4.8 4v-4A2.5 2.5 0 0 1 4 13.5z"/>
        <path d="M8 8h8M8 11.5h5"/>
      </svg>
      <span v-else>×</span>
    </button>

    <Transition name="paper-chat">
      <section v-if="paperChat.open" class="paper-chat-panel">
        <header>
          <div class="paper-chat-mark">PS</div>
          <div>
            <strong>论文问答</strong>
            <span>{{ shortPaperTitle(activePaper.title, 34) }}</span>
          </div>
        </header>
        <div class="paper-chat-messages">
          <article
            v-for="message in paperChat.messages"
            :key="message.id"
            :class="['paper-chat-message', message.role]"
          >
            <span v-if="message.role === 'assistant'">P</span>
            <p>{{ message.content }}</p>
          </article>
          <article v-if="paperChat.loading" class="paper-chat-message assistant">
            <span>P</span>
            <p class="paper-chat-thinking"><i></i><i></i><i></i></p>
          </article>
        </div>
        <form @submit.prevent="askPaperChat">
          <textarea
            v-model="paperChat.question"
            rows="2"
            :disabled="paperChat.loading"
            placeholder="询问研究方法、数据、结论、图表或论文中的概念…"
            @keydown.enter.exact.prevent="askPaperChat"
          ></textarea>
          <button :disabled="paperChat.loading || !paperChat.question.trim()" aria-label="发送问题">
            ↑
          </button>
        </form>
        <small>支持论文阅读与学术问题，非学术内容不会回答。</small>
      </section>
    </Transition>

    <Transition name="reader-toast">
      <div v-if="readerToast" class="reader-toast">{{ readerToast }}</div>
    </Transition>

    <div v-if="readerTour.open" class="reader-tour-layer">
      <div class="reader-tour-shade"></div>
      <div
        v-if="readerTour.rect"
        class="reader-tour-focus"
        :style="{
          left: `${readerTour.rect.left - 7}px`,
          top: `${readerTour.rect.top - 7}px`,
          width: `${readerTour.rect.width + 14}px`,
          height: `${readerTour.rect.height + 14}px`,
        }"
      ></div>
      <section class="reader-tour-card">
        <span>第 {{ readerTour.index + 1 }} 步 / {{ tourSteps.length }}</span>
        <h2>{{ tourSteps[readerTour.index].title }}</h2>
        <p>{{ tourSteps[readerTour.index].description }}</p>
        <div>
          <button v-if="readerTour.index > 0" @click="previousTourStep">上一步</button>
          <button @click="nextTourStep">
            {{ readerTour.index === tourSteps.length - 1 ? "朕知道了" : "朕知道了，下一步" }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="figureViewer.open" class="pdf-figure-overlay" @click="closeFigureViewer">
      <div class="pdf-figure-modal" @click.stop>
        <header>
          <strong>{{ figureViewer.caption }}</strong>
          <button @click="closeFigureViewer">关闭</button>
        </header>
        <img v-if="figureViewer.imageUrl" :src="figureViewer.imageUrl" :alt="figureViewer.caption" />
        <canvas v-else ref="figureCanvasRef"></canvas>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useLibraryStore } from "../stores/library";
import ReaderReportPanel from "../components/ReaderReportPanel.vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { rememberLastReading } from "../utils/readingMemory";

const libraryStore = useLibraryStore();
const authStore = useAuthStore();
const route = useRoute();
const pages = reactive([]);
const pageCanvasElements = new Map();
const readingScroll = ref(null);
const loadingPdf = ref(true);
const loadError = ref("");
const parsingMessage = ref("正在进行论文版面解析（完整段落、图表、表格）");
const parsingProgress = ref(8);
const totalPages = ref(0);
const loadedPages = ref(0);
const currentPage = ref(1);
const readingProgress = ref(0);
const autoTranslate = ref(true);
const assistantCollapsed = ref(false);
const assistantExpanded = ref(false);
const assistantTab = ref("chat");
const contentScale = ref(1);
const abstractTranslation = ref("");
const abstractTranslating = ref(false);
const abstractProvider = ref("google");
const abstractFromPdf = ref("");
const structuredHasAbstract = ref(false);
const translationProviders = ref([
  { id: "google", label: "谷歌翻译" },
  { id: "ai", label: "AI 学术翻译" },
  { id: "youdao", label: "有道翻译" },
]);
const selectionReady = ref(false);
const selectedRange = shallowRef(null);
const colorMenu = reactive({ open: false, x: 0, y: 0 });
const selectionTranslator = reactive({
  open: false,
  x: 0,
  y: 0,
  source: "",
  sentence: "",
  paragraph: "",
  preview: "",
  result: "",
  resultTitle: "",
  loadingText: "正在处理选区…",
  error: "",
  loading: false,
  blockId: "",
  start: -1,
  end: -1,
  annotating: false,
  annotationDraft: "",
  editingAnnotationId: "",
});
const annotations = reactive([]);
const clearedAnnotationSnapshot = ref([]);
const readerToast = ref("");
const paperChat = reactive({
  open: false,
  question: "",
  loading: false,
  nextId: 2,
  messages: [
    { id: 1, role: "assistant", content: "你好，我会优先结合当前论文回答，也可以协助其他学术研究问题。你可以问研究方法、数据、实验结论或学术概念。" },
  ],
});
const tourSteps = [
  { selector: ".reader-assistant", title: "先看左侧论文内容详解", description: "整个左侧区域就是论文内容详解，集中展示研究背景、问题、方法、实验结论、创新与局限。" },
  { selector: ".reader-tools", title: "开启全文翻译", description: "这里可以开关全文翻译并调整阅读比例。" },
  { selector: ".reading-column", title: "选中文字进行操作", description: "拖选原文或译文后，可以翻译、修改字体颜色，也可以写下批注。" },
  { selector: ".paper-chat-launcher", title: "使用学术问答", description: "右下角可以询问当前论文或其他学术相关问题，生活娱乐等非学术问题不会回答。" },
];
const readerTour = reactive({ open: false, index: 0, rect: null });
const figureViewer = reactive({ open: false, pageNumber: 0, caption: "", imageUrl: "" });
const figureCanvasRef = ref(null);
let pdfDocument = null;
let pdfObjectUrl = "";
let destroyed = false;
let activeTranslationJobs = 0;
let figurePreviewQueue = Promise.resolve();
const mineruAssetUrls = [];
const translationWaiters = [];
let readerToastTimer;

const activePaper = computed(() => libraryStore.activeDocument || {
  id: "",
  title: "未选择文献",
  authors: "",
  source: "",
});

const readerPaperTabs = computed(() =>
  (libraryStore.state.documents || [])
    .filter(paper => paper?.id)
    .slice(0, 10),
);

const compactPageTabs = computed(() => {
  const count = Number(totalPages.value || pages.length || 0);
  if (!count) return [];
  const current = Number(currentPage.value || 1);
  const candidates = [1, current - 1, current, current + 1, count]
    .filter(page => page >= 1 && page <= count);
  const unique = Array.from(new Set(candidates)).sort((a, b) => a - b);
  return unique.reduce((items, page, index) => {
    const previous = unique[index - 1];
    if (previous && page - previous > 1) {
      items.push({ key: `gap-${previous}-${page}`, label: "…", pageNumber: previous, ellipsis: true });
    }
    items.push({ key: `page-${page}`, label: page, pageNumber: page, ellipsis: false });
    return items;
  }, []);
});

const paperSourceLabel = computed(() => String(activePaper.value?.source || "")
  .replace(/&amp;/gi, "&")
  .replace(/&quot;/gi, "\"")
  .replace(/&#39;|&apos;/gi, "'")
  .replace(/&lt;/gi, "<")
  .replace(/&gt;/gi, ">"));

const workspaceId = computed(() => String(activePaper.value?.workspaceId || activePaper.value?.id || ""));

const abstractText = computed(() => String(activePaper.value?.abstract || "").trim() || abstractFromPdf.value || "");
const hasAbstract = computed(() => Boolean(abstractText.value));

const pdfSource = computed(() => {
  const paper = activePaper.value || {};
  const source = paper.pdfUrl || paper.paperUrl || "";
  return paperpilotApi.buildPdfProxyUrl(source);
});

function shortPaperTitle(title, max = 32) {
  const value = String(title || "未命名文献").replace(/\s+/g, " ").trim();
  return value.length > max ? `${value.slice(0, max)}…` : value;
}

function paperInitial(paper) {
  const title = String(paper?.title || "").trim();
  const first = title.match(/[A-Za-z0-9\u4e00-\u9fa5]/)?.[0] || "P";
  return /[A-Za-z]/.test(first) ? first.toUpperCase() : first;
}

const textColors = [
  { id: "black", label: "黑色", value: "#20242c" },
  { id: "red", label: "红色", value: "#d92d20" },
  { id: "blue", label: "蓝色", value: "#1769e0" },
  { id: "yellow", label: "黄色", value: "#a86b00" },
  { id: "green", label: "绿色", value: "#16845b" },
];

const documentOutline = computed(() =>
  pages.flatMap(page =>
    page.blocks
      .filter(block => block.kind === "heading")
      .slice(0, 8)
      .map(block => ({ page: page.pageNumber, text: block.text.slice(0, 72) })),
  ).slice(0, 80),
);

const documentFigures = computed(() =>
  pages.flatMap(page =>
    page.blocks
      .filter(block => block.kind === "figure" || block.kind === "table")
      .map(block => ({ pageNumber: page.pageNumber, block })),
  ),
);

function normalizeText(text) {
  return String(text || "").replace(/\s+/g, " ").trim();
}

function groupTextItems(items, viewport, pageNumber) {
  const physicalLines = [];
  for (const item of items) {
    const text = normalizeText(item.str);
    if (!text) continue;
    const x = item.transform[4];
    const y = viewport.height - item.transform[5];
    if (y < 32 || y > viewport.height - 30) continue;
    const height = Math.max(1, Math.abs(item.transform[3] || 10));
    let line = physicalLines.find(candidate => Math.abs(candidate.y - y) < Math.max(height, candidate.height) * 0.55);
    if (!line) {
      line = { y, height, items: [] };
      physicalLines.push(line);
    }
    line.items.push({ text, x, width: Number(item.width || 0) });
  }
  physicalLines.sort((a, b) => a.y - b.y);

  const lines = [];
  physicalLines.forEach((physicalLine) => {
    physicalLine.items.sort((a, b) => a.x - b.x);
    let segment = [];
    physicalLine.items.forEach((item) => {
      const previous = segment[segment.length - 1];
      const gap = previous ? item.x - (previous.x + previous.width) : 0;
      if (segment.length && gap > Math.max(28, viewport.width * 0.055)) {
        lines.push(createLine(segment, physicalLine));
        segment = [];
      }
      segment.push(item);
    });
    if (segment.length) lines.push(createLine(segment, physicalLine));
  });

  const medianHeight = [...lines].sort((a, b) => a.height - b.height)[Math.floor(lines.length / 2)]?.height || 10;
  const midX = viewport.width / 2;
  // 列识别：跨中线或宽度超过 56% 视为单栏行（标题/通栏段），其余按左右半边分栏
  let leftCount = 0, rightCount = 0;
  lines.forEach((line) => {
    const crossesMiddle = line.left < midX && line.right > midX;
    const wide = line.right - line.left > viewport.width * 0.56;
    if (crossesMiddle || wide) {
      line.column = "span";
    } else if (line.left < midX) {
      line.column = "left"; leftCount++;
    } else {
      line.column = "right"; rightCount++;
    }
  });
  // 只有左右栏都有足够行数时才按双栏阅读顺序，否则视为单栏按 y 顺序（避免单栏论文被误切）
  const isTwoColumn = leftCount >= 5 && rightCount >= 5;
  let readingLines;
  if (isTwoColumn) {
    const topSpans = lines.filter(line => line.column === "span" && line.y < viewport.height * 0.32);
    const leftLines = lines.filter(line => line.column === "left").sort((a, b) => a.y - b.y);
    const rightLines = lines.filter(line => line.column === "right").sort((a, b) => a.y - b.y);
    const lowerSpans = lines.filter(line => line.column === "span" && line.y >= viewport.height * 0.32);
    readingLines = [...topSpans, ...leftLines, ...rightLines, ...lowerSpans];
  } else {
    readingLines = [...lines].sort((a, b) => a.y - b.y);
  }

  const blocks = [];
  let current = [];
  for (const line of readingLines) {
    const text = line.text;
    const figureCaption = /^(fig(?:ure|\.)?\s*\d+|table\s*\d+|图\s*\d+|表\s*\d+)/i.test(text);
    if (figureCaption) {
      if (current.length) { blocks.push(current); current = []; }
        blocks.push([{
          text,
          heading: false,
          figure: true,
          y: line.y,
          left: line.left,
          right: line.right,
          height: line.height,
          column: line.column,
        }]);
      continue;
    }
    const heading = /^(\d{1,2}(?:\.\d{1,2})*\.?\s+[A-Z]|abstract$|introduction$|conclusions?$|references$|materials?\s+and\s+methods?|results?$|discussion$|摘要|引言|结论)/i.test(text)
      || (text.length < 100 && line.height > medianHeight * 1.65);
    if (heading) {
      if (current.length && !current.every(item => item.heading)) {
        blocks.push(current);
        current = [];
      }
      if (current.length && current.every(item => item.heading)) {
        current.push({ text, heading: true, y: line.y, left: line.left, right: line.right, height: line.height, column: line.column });
      } else {
        current = [{ text, heading: true, y: line.y, left: line.left, right: line.right, height: line.height, column: line.column }];
      }
      continue;
    }
    if (current.length && current.every(item => item.heading)) {
      blocks.push(current);
      current = [];
    }
    const previous = current[current.length - 1];
    const verticalGap = previous ? Math.abs(line.y - previous.y) : 0;
    const startsIndentedParagraph = previous
      && line.left - previous.left > medianHeight * 1.2
      && /[.!?。！？]$/.test(previous.text);
    const separatedParagraph = previous && verticalGap > Math.max(18, medianHeight * 2.05);
    if (current.length && (startsIndentedParagraph || separatedParagraph)) {
      blocks.push(current);
      current = [];
    }
    current.push({
      text,
      heading: false,
      y: line.y,
      left: line.left,
      right: line.right,
      height: line.height,
      column: line.column,
    });
  }
  if (current.length) blocks.push(current);

  let normalizedBlocks = blocks
    .map((linesInBlock, index) => {
      const top = Math.min(...linesInBlock.map(line => line.y));
      const bottom = Math.max(...linesInBlock.map(line => line.y + line.height));
      return {
        id: `p${pageNumber}_b${index}`,
        kind: linesInBlock.some(line => line.figure) ? "figure" : (linesInBlock.every(line => line.heading) ? "heading" : "paragraph"),
        text: normalizeText(linesInBlock.map(line => line.text).join(" ")),
        column: linesInBlock.find(line => line.column && line.column !== "span")?.column || "span",
        bbox: {
          top,
          bottom,
          left: Math.min(...linesInBlock.map(line => line.left)),
          right: Math.max(...linesInBlock.map(line => line.right || line.left)),
        },
        imageUrl: "",
        headingLabel: "",
        translation: "",
        translationProvider: "google",
        translating: false,
        translationError: "",
      };
    })
    .filter(block => block.text.length > 2);
  if (pageNumber === 1) {
    // 摘要 = 第 1 页第一个真正的章节标题之前的全部段落
    const sectionStartIndex = normalizedBlocks.findIndex(block =>
      block.kind === "heading"
      && /^(introduction|1\.?\s*introduction|background|2\b|related\s+work|1\s+introduction)/i.test(block.text.trim())
    );
    const cutoff = sectionStartIndex > 0 ? sectionStartIndex : normalizedBlocks.findIndex(block => block.kind === "heading" && /\bintroduction\b/i.test(block.text));
    if (cutoff > 0) {
      abstractFromPdf.value = normalizedBlocks
        .slice(0, cutoff)
        .filter(block => block.kind === "paragraph")
        .map(block => block.text)
        .join(" ");
    } else if (!abstractFromPdf.value) {
      // 没有明确章节分界时，取第 1 页前 6 个段落作为摘要兜底
      abstractFromPdf.value = normalizedBlocks
        .filter(block => block.kind === "paragraph")
        .slice(0, 6)
        .map(block => block.text)
        .join(" ");
    }
  }
  let lastHeading = "";
  normalizedBlocks.forEach((block) => {
    if (block.kind === "heading") {
      lastHeading = block.text;
    } else if (lastHeading) {
      block.headingLabel = lastHeading;
    }
  });
  return normalizedBlocks;
}

function createLine(items, physicalLine) {
  return {
    y: physicalLine.y,
    height: physicalLine.height,
    left: Math.min(...items.map(item => item.x)),
    right: Math.max(...items.map(item => item.x + item.width)),
    text: normalizeText(items.map(item => item.text).join(" ")),
  };
}

async function attachFigurePreviews(pdfPage, viewport, blocks) {
  const figures = blocks.filter(block => block.kind === "figure" && block.bbox);
  if (!figures.length) return;

  const renderScale = 1.35;
  const renderViewport = pdfPage.getViewport({ scale: renderScale });
  const pageCanvas = document.createElement("canvas");
  pageCanvas.width = Math.ceil(renderViewport.width);
  pageCanvas.height = Math.ceil(renderViewport.height);
  await pdfPage.render({
    canvasContext: pageCanvas.getContext("2d", { alpha: false }),
    viewport: renderViewport,
  }).promise;

  figures.forEach((figure) => {
    const captionTop = figure.bbox.top;
    const captionBottom = figure.bbox.bottom;
    const isTable = /^(table|表)\s*\d+/i.test(figure.text);
    const sameColumn = blocks.filter(block =>
      block !== figure
      && block.bbox
      && (figure.column === "span" || block.column === figure.column || block.column === "span")
    );

    let cropTop;
    let cropBottom;
    if (isTable) {
      cropTop = captionBottom + 6;
      const next = sameColumn
        .filter(block => block.bbox.top > captionBottom + 28)
        .sort((a, b) => a.bbox.top - b.bbox.top)[0];
      cropBottom = Math.min(next?.bbox.top - 6 || captionBottom + 250, viewport.height - 24);
      if (cropBottom - cropTop < 80) cropBottom = Math.min(cropTop + 230, viewport.height - 24);
    } else {
      cropBottom = captionTop - 6;
      const previous = sameColumn
        .filter(block => block.bbox.bottom < captionTop - 24)
        .sort((a, b) => b.bbox.bottom - a.bbox.bottom)[0];
      cropTop = Math.max(24, previous?.bbox.bottom + 8 || captionTop - 250);
      if (cropBottom - cropTop < 80) cropTop = Math.max(24, cropBottom - 230);
    }

    const half = viewport.width / 2;
    const cropLeft = figure.column === "right" ? half + 8 : 24;
    const cropRight = figure.column === "left" ? half - 8 : viewport.width - 24;
    const width = Math.max(120, cropRight - cropLeft);
    const height = Math.max(70, cropBottom - cropTop);
    const previewCanvas = document.createElement("canvas");
    previewCanvas.width = Math.ceil(width * renderScale);
    previewCanvas.height = Math.ceil(height * renderScale);
    previewCanvas.getContext("2d", { alpha: false }).drawImage(
      pageCanvas,
      cropLeft * renderScale,
      cropTop * renderScale,
      width * renderScale,
      height * renderScale,
      0,
      0,
      previewCanvas.width,
      previewCanvas.height,
    );
    figure.imageUrl = previewCanvas.toDataURL("image/jpeg", 0.9);
  });
}

function queueFigurePreviews(pdfPage, viewport, blocks) {
  if (!blocks.some(block => block.kind === "figure")) return;
  figurePreviewQueue = figurePreviewQueue
    .then(() => destroyed ? undefined : attachFigurePreviews(pdfPage, viewport, blocks))
    .catch(error => console.warn("reader figure preview failed", error));
}

function parseMarkedTranslation(raw, blocks) {
  const value = String(raw || "");
  const regex = /\[\[\[\s*RID[_\s-]*(\d+)\s*\]\]\]/gi;
  const markers = [];
  let match;
  while ((match = regex.exec(value)) !== null) {
    markers.push({ index: Number(match[1]), start: match.index, end: regex.lastIndex });
  }
  const output = {};
  markers.forEach((marker, index) => {
    const next = markers[index + 1];
    output[blocks[marker.index]?.id] = value
      .slice(marker.end, next ? next.start : value.length)
      .replace(/\[\[\[[^\]]+\]\]\]/g, "")
      .trim();
  });
  return output;
}

function providerLabel(providerId) {
  return translationProviders.value.find(provider => provider.id === providerId)?.label || providerId || "翻译引擎";
}

function cycleAndRetranslate(block) {
  const ids = translationProviders.value.map(provider => provider.id);
  const current = block.translationProvider || ids[0];
  const nextIndex = (ids.indexOf(current) + 1) % ids.length;
  block.translationProvider = ids[nextIndex];
  block.translation = "";
  translateBlock(block, true);
}

function viewPageFigure(pageNumber, caption) {
  figureViewer.open = true;
  figureViewer.pageNumber = pageNumber;
  figureViewer.caption = caption || `第 ${pageNumber} 页图表`;
  figureViewer.imageUrl = "";
  nextTick(() => renderPagePreview(pageNumber, figureCanvasRef.value));
}

function viewParsedFigure(block, pageNumber) {
  figureViewer.open = true;
  figureViewer.pageNumber = pageNumber;
  figureViewer.caption = block.text || `第 ${pageNumber} 页图表`;
  figureViewer.imageUrl = block.imageUrl || "";
}

function closeFigureViewer() {
  figureViewer.open = false;
  figureViewer.imageUrl = "";
}

function openColorMenu(event) {
  const selection = window.getSelection();
  if (!selection || selection.isCollapsed || !selection.rangeCount) {
    closeColorMenu();
    return;
  }
  const range = selection.getRangeAt(0);
  const documentRoot = range.commonAncestorContainer.nodeType === Node.ELEMENT_NODE
    ? range.commonAncestorContainer
    : range.commonAncestorContainer.parentElement;
  if (!documentRoot?.closest?.(".reflow-document")) {
    closeColorMenu();
    return;
  }
  selectedRange.value = range.cloneRange();
  selectionReady.value = true;
  colorMenu.x = event.clientX;
  colorMenu.y = event.clientY;
  colorMenu.open = true;
}

function closeColorMenu() {
  colorMenu.open = false;
}

function sentenceAroundSelection(paragraph, selected) {
  const source = normalizeText(paragraph);
  const needle = normalizeText(selected);
  if (!source || !needle) return needle;
  const start = source.indexOf(needle);
  if (start < 0) return needle;
  const punctuation = /[.!?。！？；;]/;
  let sentenceStart = start;
  while (sentenceStart > 0 && !punctuation.test(source[sentenceStart - 1])) sentenceStart -= 1;
  while (sentenceStart < source.length && /\s/.test(source[sentenceStart])) sentenceStart += 1;
  let sentenceEnd = start + needle.length;
  while (sentenceEnd < source.length && !punctuation.test(source[sentenceEnd])) sentenceEnd += 1;
  if (sentenceEnd < source.length) sentenceEnd += 1;
  return source.slice(sentenceStart, sentenceEnd).trim() || needle;
}

function closeSelectionTranslator() {
  selectionTranslator.open = false;
  selectionTranslator.loading = false;
  selectionTranslator.annotating = false;
}

async function translateSelection() {
  const text = selectionTranslator.source;
  if (!text || selectionTranslator.loading) return;
  selectionTranslator.loading = true;
  selectionTranslator.resultTitle = "选中翻译";
  selectionTranslator.loadingText = "正在生成译文…";
  selectionTranslator.error = "";
  selectionTranslator.result = "";
  try {
    const result = await paperpilotApi.translate({
      text,
      provider: "google",
      sourceLang: "auto",
      targetLang: "zh-CN",
    }, { timeout: 45000 });
    selectionTranslator.result = String(result?.translatedText || "").trim();
    if (!selectionTranslator.result) selectionTranslator.error = "本次没有返回译文，请重试。";
  } catch (error) {
    console.warn("selection translation failed", error);
    selectionTranslator.error = "选区翻译失败，请稍后重试。";
  } finally {
    selectionTranslator.loading = false;
  }
}

async function explainSelection() {
  const text = selectionTranslator.source;
  if (!text || selectionTranslator.loading) return;
  selectionTranslator.loading = true;
  selectionTranslator.resultTitle = "AI 解读";
  selectionTranslator.loadingText = "正在结合论文语境解读…";
  selectionTranslator.error = "";
  selectionTranslator.result = "";
  const localContext = selectionTranslator.sentence || selectionTranslator.paragraph || text;
  try {
    const result = await paperpilotApi.askPaperSelection(workspaceId.value, {
      question: [
        "请用中文解释下面这段论文选中内容。",
        "要求：先说明它在论文中的作用，再解释关键概念，最后给出读者需要注意的研究含义。",
        "不要泛泛复述，不要回答非学术内容。",
        `选中内容：${text}`,
        `所在语境：${localContext}`,
      ].join("\n"),
    });
    selectionTranslator.result = cleanChatAnswer(result?.answer);
    if (!selectionTranslator.result) selectionTranslator.error = "本次没有返回解读，请重试。";
  } catch (error) {
    console.warn("selection explanation failed", error);
    selectionTranslator.error = error?.response?.data?.message || "AI 解读失败，请稍后重试。";
  } finally {
    selectionTranslator.loading = false;
  }
}

async function askPaperChat() {
  const question = paperChat.question.trim();
  if (!question || paperChat.loading) return;
  paperChat.messages.push({ id: paperChat.nextId++, role: "user", content: question });
  paperChat.question = "";
  paperChat.loading = true;
  try {
    const result = await paperpilotApi.askPaperSelection(workspaceId.value, {
      question,
    });
    const answer = cleanChatAnswer(result?.answer) || "本次没有返回回答，请重试。";
    paperChat.messages.push({ id: paperChat.nextId++, role: "assistant", content: answer });
  } catch (error) {
    console.warn("paper chat failed", error);
    paperChat.messages.push({
      id: paperChat.nextId++,
      role: "assistant",
      content: error?.response?.data?.message || "PaperSolver 暂时无法回答，请稍后重试。",
    });
  } finally {
    paperChat.loading = false;
    nextTick(() => {
      const container = document.querySelector(".paper-chat-messages");
      if (container) container.scrollTop = container.scrollHeight;
    });
  }
}

function cleanChatAnswer(value) {
  return String(value || "")
    .replace(/\*\*/g, "")
    .replace(/^\s*[-*]\s+/gm, "")
    .replace(/^\s*#{1,6}\s+/gm, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function annotationStorageKey() {
  return `papersolver-reader-annotations:${workspaceId.value}`;
}

function loadAnnotations() {
  annotations.splice(0);
  try {
    const stored = JSON.parse(localStorage.getItem(annotationStorageKey()) || "[]");
    if (Array.isArray(stored)) annotations.push(...stored);
  } catch {
    // 忽略损坏的本地批注缓存。
  }
}

function persistAnnotations() {
  localStorage.setItem(annotationStorageKey(), JSON.stringify(annotations));
}

function showReaderToast(message) {
  readerToast.value = message;
  clearTimeout(readerToastTimer);
  readerToastTimer = setTimeout(() => { readerToast.value = ""; }, 1800);
}

function removeAnnotation(id) {
  const index = annotations.findIndex(item => item.id === id);
  if (index < 0) return;
  clearedAnnotationSnapshot.value = [];
  annotations.splice(index, 1);
  persistAnnotations();
  closeSelectionTranslator();
}

function clearAllAnnotations() {
  if (!annotations.length) {
    if (clearedAnnotationSnapshot.value.length) {
      annotations.push(...clearedAnnotationSnapshot.value);
      clearedAnnotationSnapshot.value = [];
      persistAnnotations();
      showReaderToast("已恢复刚清除的标注");
      return;
    }
    showReaderToast("当前没有可清除的标注");
    return;
  }
  clearedAnnotationSnapshot.value = annotations.map(item => ({ ...item }));
  annotations.splice(0, annotations.length);
  persistAnnotations();
  closeSelectionTranslator();
  showReaderToast("全局内容已清除");
}

function annotationForBlock(blockId) {
  return annotations.find(item => item.blockId === blockId);
}

function annotationsForBlock(blockId) {
  return annotations.filter(item => item.blockId === blockId);
}

function annotationRange(annotation, text) {
  const source = String(text || "");
  const quote = String(annotation?.quote || "");
  let start = Number.isInteger(annotation?.start) ? annotation.start : -1;
  let end = Number.isInteger(annotation?.end) ? annotation.end : -1;
  if (start >= 0 && end > start && end <= source.length) {
    return {
      ...annotation,
      start,
      end,
    };
  }
  if (start < 0 || end <= start) {
    start = source.indexOf(quote);
    end = start >= 0 ? start + quote.length : -1;
  }
  if (start < 0 || end <= start) return null;
  return {
    ...annotation,
    start: Math.max(0, start),
    end: Math.min(source.length, end),
  };
}

function annotationSegments(blockId, text) {
  const source = String(text || "");
  const ranges = annotationsForBlock(blockId)
    .map(item => annotationRange(item, source))
    .filter(Boolean)
    .sort((a, b) => a.start - b.start)
    .reduce((items, item) => {
      const previous = items[items.length - 1];
      if (previous && item.start < previous.end) return items;
      items.push(item);
      return items;
    }, []);
  if (!ranges.length) return [{ key: `${blockId}-plain`, text: source, annotated: false }];
  const segments = [];
  let cursor = 0;
  ranges.forEach((range, index) => {
    if (range.start > cursor) {
      segments.push({ key: `${blockId}-plain-${index}`, text: source.slice(cursor, range.start), annotated: false });
    }
    segments.push({
      key: `${blockId}-anno-${range.id}`,
      text: source.slice(range.start, range.end),
      annotated: true,
      note: range.note,
      annotation: range,
    });
    cursor = range.end;
  });
  if (cursor < source.length) {
    segments.push({ key: `${blockId}-plain-tail`, text: source.slice(cursor), annotated: false });
  }
  return segments;
}

function getSelectionOffsets(paragraphElement, range) {
  if (!paragraphElement || !range) return null;
  if (!paragraphElement.contains(range.startContainer) || !paragraphElement.contains(range.endContainer)) return null;
  const before = range.cloneRange();
  before.selectNodeContents(paragraphElement);
  before.setEnd(range.startContainer, range.startOffset);
  const start = before.toString().length;
  return {
    start,
    end: start + range.toString().length,
  };
}

function openAnnotationEditor() {
  selectionTranslator.annotating = true;
  selectionTranslator.annotationDraft = "";
  selectionTranslator.editingAnnotationId = "";
  nextTick(() => fitSelectionPopover(true));
}

function fitSelectionPopover(focusTextarea = false) {
  const popover = document.querySelector(".selection-translate-popover");
  if (!popover) return;
  const edge = 12;
  const height = Math.min(popover.scrollHeight, window.innerHeight - edge * 2);
  selectionTranslator.y = Math.max(edge, Math.min(selectionTranslator.y, window.innerHeight - height - edge));
  if (focusTextarea) {
    nextTick(() => popover.querySelector("textarea")?.focus());
  }
}

function saveAnnotation() {
  const note = selectionTranslator.annotationDraft.trim();
  if (!note || !selectionTranslator.blockId) return;
  if (selectionTranslator.editingAnnotationId) {
    const existing = annotations.find(item => item.id === selectionTranslator.editingAnnotationId);
    if (existing) existing.note = note;
  } else {
    annotations.push({
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      blockId: selectionTranslator.blockId,
      quote: selectionTranslator.source,
      start: selectionTranslator.start,
      end: selectionTranslator.end,
      note,
      createdAt: new Date().toISOString(),
    });
  }
  persistAnnotations();
  closeSelectionTranslator();
}

function editAnnotation(annotation, event) {
  selectionTranslator.source = annotation.quote;
  selectionTranslator.preview = annotation.quote.length > 180 ? `${annotation.quote.slice(0, 180)}…` : annotation.quote;
  selectionTranslator.blockId = annotation.blockId;
  selectionTranslator.result = "";
  selectionTranslator.resultTitle = "";
  selectionTranslator.loadingText = "正在处理选区…";
  selectionTranslator.error = "";
  selectionTranslator.annotating = true;
  selectionTranslator.annotationDraft = annotation.note;
  selectionTranslator.editingAnnotationId = annotation.id;
  selectionTranslator.start = Number.isInteger(annotation.start) ? annotation.start : -1;
  selectionTranslator.end = Number.isInteger(annotation.end) ? annotation.end : -1;
  selectionTranslator.x = Math.max(12, Math.min(window.innerWidth - 404, event.clientX - 360));
  selectionTranslator.y = Math.max(56, Math.min(window.innerHeight - 330, event.clientY - 60));
  selectionTranslator.open = true;
  nextTick(() => fitSelectionPopover(true));
}

async function translateBlock(block, force = false) {
  if (!autoTranslate.value || block.translating || (!force && block.translation)) return;
  block.translating = true;
  block.translationError = "";
  await acquireTranslationSlot();
  try {
    const result = await paperpilotApi.translate({
      text: block.text,
      provider: block.translationProvider || "google",
      sourceLang: "auto",
      targetLang: "zh-CN",
    }, { timeout: 45000 });
    block.translation = String(result?.translatedText || "").trim();
    if (!block.translation) block.translationError = "本段暂时没有返回译文，请更换翻译引擎重试。";
  } catch (error) {
    console.warn("reader paragraph translation failed", block.id, error);
    block.translationError = "本段翻译失败，请更换翻译引擎重试。";
  } finally {
    block.translating = false;
    releaseTranslationSlot();
  }
}

function acquireTranslationSlot() {
  if (activeTranslationJobs < 3) {
    activeTranslationJobs += 1;
    return Promise.resolve();
  }
  return new Promise(resolve => translationWaiters.push(resolve));
}

function releaseTranslationSlot() {
  const next = translationWaiters.shift();
  if (next) next();
  else activeTranslationJobs = Math.max(0, activeTranslationJobs - 1);
}

async function translatePage(page) {
  if (!autoTranslate.value || page.translating) return;
  page.translating = true;
  const queue = page.blocks.filter(block =>
    !["figure", "table", "equation", "references"].includes(block.kind) && !block.translation
  );
  const workers = Array.from({ length: Math.min(2, queue.length) }, async () => {
    while (queue.length && autoTranslate.value) {
      const block = queue.shift();
      if (block) await translateBlock(block);
    }
  });
  await Promise.all(workers);
  page.translated = page.blocks.every(block =>
    ["figure", "table", "equation", "references"].includes(block.kind) || block.translation
  );
  page.translating = false;
}

async function translateAbstract(force = false) {
  const text = (String(activePaper.value.abstract || "").trim() || abstractFromPdf.value || "").trim();
  if (!text || abstractTranslating.value || (!force && abstractTranslation.value)) return;
  abstractTranslating.value = true;
  try {
    const result = await paperpilotApi.translate({
      text,
      provider: abstractProvider.value,
      sourceLang: "auto",
      targetLang: "zh-CN",
    }, { timeout: 45000 });
    abstractTranslation.value = result?.translatedText || "";
  } catch {
    abstractTranslation.value = "";
  } finally {
    abstractTranslating.value = false;
  }
}

async function renderPagePreview(pageNumber, overrideCanvas) {
  const canvas = overrideCanvas || pageCanvasElements.get(pageNumber);
  if (!canvas || !pdfDocument) return;
  const page = await pdfDocument.getPage(pageNumber);
  const baseViewport = page.getViewport({ scale: 1 });
  const maxWidth = 760;
  const scale = Math.min(1.35, maxWidth / baseViewport.width);
  const viewport = page.getViewport({ scale });
  const outputScale = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = Math.floor(viewport.width * outputScale);
  canvas.height = Math.floor(viewport.height * outputScale);
  canvas.style.width = `${viewport.width}px`;
  canvas.style.height = `${viewport.height}px`;
  await page.render({
    canvasContext: canvas.getContext("2d"),
    viewport,
    transform: outputScale === 1 ? null : [outputScale, 0, 0, outputScale, 0, 0],
  }).promise;
}

function setPageCanvas(pageNumber, element) {
  if (!element) return;
  pageCanvasElements.set(pageNumber, element);
  nextTick(() => renderPagePreview(pageNumber));
}

async function loadPdf() {
  loadingPdf.value = true;
  loadError.value = "";
  try {
    if (!pdfSource.value) throw new Error("当前文献尚未关联 PDF");
    const [pdfjs, workerModule] = await Promise.all([
      import("pdfjs-dist"),
      import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
    ]);
    pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;
    const loadingTask = pdfjs.getDocument(pdfSource.value);
    pdfDocument = await loadingTask.promise;
    totalPages.value = pdfDocument.numPages;

    for (let pageNumber = 1; pageNumber <= pdfDocument.numPages; pageNumber += 1) {
      if (destroyed) return;
      const pdfPage = await pdfDocument.getPage(pageNumber);
      const viewport = pdfPage.getViewport({ scale: 1 });
      const textContent = await pdfPage.getTextContent();
      const blocks = groupTextItems(textContent.items || [], viewport, pageNumber);
      const page = reactive({
        pageNumber,
        blocks,
        translating: false,
        translated: false,
      });
      pages.push(page);
      loadedPages.value = pageNumber;
      await nextTick();
      queueFigurePreviews(pdfPage, viewport, blocks);
      if (autoTranslate.value && pageNumber <= 3) translatePage(page);
    }
  } catch (error) {
    loadError.value = error?.message || "PDF 加载失败";
  } finally {
    loadingPdf.value = false;
  }
}

async function hydrateMineruPages(document) {
  pages.splice(0, pages.length);
  const parsedPages = Array.isArray(document?.pages) ? document.pages : [];
  structuredHasAbstract.value = parsedPages.some(page =>
    (Array.isArray(page.blocks) ? page.blocks : []).some(block =>
      block.kind === "heading" && /^abstract$/i.test(String(block.text || "").trim())
    )
  );
  parsedPages.forEach((sourcePage) => {
    const blocks = (Array.isArray(sourcePage.blocks) ? sourcePage.blocks : []).map(block => ({
      ...block,
      assetPath: block.imageUrl || "",
      imageUrl: "",
      translation: "",
      translationProvider: block.translationProvider || "google",
      translating: false,
      translationError: "",
    }));
    pages.push(reactive({
      pageNumber: Number(sourcePage.pageNumber || pages.length + 1),
      blocks,
      translating: false,
      translated: false,
    }));
  });
  totalPages.value = Number(document?.totalPages || pages.length);
  loadedPages.value = pages.length;
  if (!pages.length) throw new Error("未从论文中识别到可重排的内容");
  const imageBlocks = pages.flatMap(page => page.blocks.filter(block => block.assetPath));
  await Promise.all(imageBlocks.map(async (block) => {
    try {
      const blob = await paperpilotApi.getMineruAsset(block.assetPath);
      if (destroyed) return;
      const objectUrl = URL.createObjectURL(blob);
      mineruAssetUrls.push(objectUrl);
      block.imageUrl = objectUrl;
    } catch (error) {
      console.warn("mineru asset load failed", block.assetPath, error);
    }
  }));
}

async function loadStructuredDocument() {
  loadingPdf.value = true;
  loadError.value = "";
  parsingMessage.value = "正在进行论文版面解析（完整段落、图表、表格）";
  parsingProgress.value = 8;
  try {
    if (!workspaceId.value) throw new Error("当前文献缺少工作区编号");
    let status = await paperpilotApi.startMineruParse(workspaceId.value);
    while (!destroyed && status?.state !== "SUCCESS") {
      if (status?.state === "FAILURE") {
        throw new Error(status?.message || "论文结构化解析失败");
      }
      parsingMessage.value = status?.message || "正在识别段落、阅读顺序与图表";
      parsingProgress.value = Math.min(88, parsingProgress.value + 6);
      await new Promise(resolve => window.setTimeout(resolve, 1600));
      status = await paperpilotApi.getMineruParseStatus(workspaceId.value);
    }
    if (destroyed) return;
    parsingMessage.value = "正在生成连续段落与图表版式";
    parsingProgress.value = 96;
    const document = await paperpilotApi.getMineruDocument(workspaceId.value);
    await hydrateMineruPages(document);
    parsingProgress.value = 100;
    if (autoTranslate.value) {
      pages.filter(page => page.pageNumber <= 2).forEach(page => translatePage(page));
    }
  } catch (error) {
    const detail = error?.response?.data?.message || error?.message;
    loadError.value = detail || "论文结构化解析失败";
    translateAbstract();
  } finally {
    loadingPdf.value = false;
  }
}

function toggleTranslation() {
  autoTranslate.value = !autoTranslate.value;
  if (autoTranslate.value) {
    translateAbstract();
    pages
      .filter(page => Math.abs(page.pageNumber - currentPage.value) <= 1)
      .forEach(page => translatePage(page));
  }
}

function toggleAssistantCollapse() {
  assistantCollapsed.value = !assistantCollapsed.value;
  if (assistantCollapsed.value) assistantExpanded.value = false;
}

async function loadTranslationProviders() {
  try {
    const providers = await paperpilotApi.getTranslationProviders();
    const available = (Array.isArray(providers) ? providers : [])
      .filter(provider => String(provider.configured) === "true")
      .map(provider => ({ id: provider.id, label: provider.label }));
    if (available.length) translationProviders.value = available;
    if (!available.some(provider => provider.id === abstractProvider.value)) {
      abstractProvider.value = available[0]?.id || "google";
    }
  } catch {
    // 保留内置翻译引擎选项。
  }
}

function captureSelection() {
  const selection = window.getSelection();
  if (!selection || selection.isCollapsed || !selection.rangeCount) {
    selectionReady.value = false;
    selectedRange.value = null;
    closeSelectionTranslator();
    return;
  }
  const range = selection.getRangeAt(0);
  const documentRoot = range.commonAncestorContainer.nodeType === Node.ELEMENT_NODE
    ? range.commonAncestorContainer
    : range.commonAncestorContainer.parentElement;
  if (!documentRoot?.closest?.(".reflow-document")) {
    selectionReady.value = false;
    selectedRange.value = null;
    closeSelectionTranslator();
    return;
  }
  selectedRange.value = range.cloneRange();
  selectionReady.value = true;
  const selectedText = normalizeText(selection.toString());
  if (!selectedText) {
    closeSelectionTranslator();
    return;
  }
  const paragraphElement = documentRoot.closest?.(".selectable-paragraph")
    || range.startContainer.parentElement?.closest?.(".selectable-paragraph");
  const selectionOffsets = getSelectionOffsets(paragraphElement, range);
  const paragraphText = normalizeText(paragraphElement?.innerText || selectedText);
  const rect = range.getBoundingClientRect();
  const popoverWidth = 392;
  selectionTranslator.source = selectedText;
  selectionTranslator.paragraph = paragraphText;
  selectionTranslator.sentence = sentenceAroundSelection(paragraphText, selectedText);
  selectionTranslator.preview = selectedText.length > 180 ? `${selectedText.slice(0, 180)}…` : selectedText;
  selectionTranslator.result = "";
  selectionTranslator.resultTitle = "";
  selectionTranslator.loadingText = "正在处理选区…";
  selectionTranslator.error = "";
  selectionTranslator.blockId = paragraphElement?.dataset?.blockId || "";
  selectionTranslator.start = selectionOffsets?.start ?? -1;
  selectionTranslator.end = selectionOffsets?.end ?? -1;
  selectionTranslator.annotating = false;
  selectionTranslator.annotationDraft = "";
  selectionTranslator.editingAnnotationId = "";
  selectionTranslator.x = Math.max(12, Math.min(window.innerWidth - popoverWidth - 12, rect.left + rect.width / 2 - popoverWidth / 2));
  selectionTranslator.y = Math.max(56, Math.min(window.innerHeight - 260, rect.bottom + 10));
  selectionTranslator.open = true;
  nextTick(() => fitSelectionPopover(false));
}

function applyTextColor(color) {
  const range = selectedRange.value;
  if (!range || range.collapsed) return;
  const root = range.commonAncestorContainer.nodeType === Node.ELEMENT_NODE
    ? range.commonAncestorContainer
    : range.commonAncestorContainer.parentElement;
  if (!root) return;

  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
  const nodes = [];
  let node = walker.nextNode();
  while (node) {
    if (node.nodeValue?.trim() && range.intersectsNode(node)) nodes.push(node);
    node = walker.nextNode();
  }

  nodes.reverse().forEach((textNode) => {
    const start = textNode === range.startContainer ? range.startOffset : 0;
    const end = textNode === range.endContainer ? range.endOffset : textNode.nodeValue.length;
    if (start >= end) return;
    const part = document.createRange();
    part.setStart(textNode, start);
    part.setEnd(textNode, end);
    const span = document.createElement("span");
    span.className = "reader-font-color";
    span.style.color = color;
    span.appendChild(part.extractContents());
    part.insertNode(span);
  });

  window.getSelection()?.removeAllRanges();
  selectedRange.value = null;
  selectionReady.value = false;
}

function applySelectionColor(color) {
  applyTextColor(color);
  closeSelectionTranslator();
}

function updateTourRect(attempt = 0) {
  if (!readerTour.open) return;
  const target = document.querySelector(tourSteps[readerTour.index].selector);
  const rect = target?.getBoundingClientRect();
  if (!rect || rect.width < 2 || rect.height < 2) {
    readerTour.rect = null;
    if (attempt < 20) window.setTimeout(() => updateTourRect(attempt + 1), 120);
    return;
  }
  const left = Math.max(4, rect.left);
  const top = Math.max(4, rect.top);
  const right = Math.min(window.innerWidth - 4, rect.right);
  const bottom = Math.min(window.innerHeight - 4, rect.bottom);
  readerTour.rect = { left, top, width: right - left, height: bottom - top };
}

function nextTourStep() {
  if (readerTour.index >= tourSteps.length - 1) {
    readerTour.open = false;
    localStorage.setItem("papersolver-reader-tour-v3", "done");
    return;
  }
  readerTour.index += 1;
  nextTick(updateTourRect);
}

function previousTourStep() {
  readerTour.index = Math.max(0, readerTour.index - 1);
  nextTick(updateTourRect);
}

function startReaderTour() {
  if (localStorage.getItem("papersolver-reader-tour-v3") === "done") return;
  assistantCollapsed.value = false;
  assistantTab.value = "chat";
  readerTour.open = true;
  readerTour.index = 0;
  nextTick(updateTourRect);
}

function resetReaderDocumentState() {
  closeSelectionTranslator();
  closeColorMenu();
  window.getSelection()?.removeAllRanges();
  selectedRange.value = null;
  selectionReady.value = false;
  pages.splice(0, pages.length);
  pageCanvasElements.clear();
  mineruAssetUrls.splice(0).forEach(url => URL.revokeObjectURL(url));
  if (pdfObjectUrl) {
    URL.revokeObjectURL(pdfObjectUrl);
    pdfObjectUrl = "";
  }
  pdfDocument?.destroy?.();
  pdfDocument = null;
  abstractTranslation.value = "";
  abstractTranslating.value = false;
  abstractFromPdf.value = "";
  structuredHasAbstract.value = false;
  totalPages.value = 0;
  loadedPages.value = 0;
  currentPage.value = 1;
  readingProgress.value = 0;
  parsingMessage.value = "正在进行论文版面解析（完整段落、图表、表格）";
  parsingProgress.value = 8;
  readingScroll.value?.scrollTo({ top: 0, left: 0 });
}

async function switchReaderPaper(id) {
  if (!id || id === activePaper.value?.id || loadingPdf.value) return;
  libraryStore.setActiveDocument(id);
  if (activePaper.value) rememberLastReading(authStore.session.user, activePaper.value);
  resetReaderDocumentState();
  loadAnnotations();
  await nextTick();
  loadStructuredDocument();
}

function scrollToPage(pageNumber) {
  const target = readingScroll.value?.querySelector(`[data-page="${pageNumber}"]`);
  target?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function handleReadingScroll() {
  const container = readingScroll.value;
  if (!container) return;
  const scrollable = container.scrollHeight - container.clientHeight;
  readingProgress.value = scrollable > 0
    ? Math.min(100, Math.max(0, Math.round((container.scrollTop / scrollable) * 100)))
    : 0;
  const sections = Array.from(container.querySelectorAll(".reflow-page"));
  const nearest = sections.reduce((best, section) => {
    const distance = Math.abs(section.getBoundingClientRect().top - container.getBoundingClientRect().top - 70);
    return !best || distance < best.distance ? { page: Number(section.dataset.page), distance } : best;
  }, null);
  if (nearest) {
    currentPage.value = nearest.page;
    if (autoTranslate.value) {
      pages
        .filter(page => page.pageNumber === nearest.page || page.pageNumber === nearest.page + 1)
        .forEach(page => translatePage(page));
    }
  }
}

function openOriginalPdf() {
  if (pdfSource.value) window.open(pdfSource.value, "_blank", "noopener,noreferrer");
}

onMounted(async () => {
  await libraryStore.hydrateLibrary();
  if (activePaper.value) rememberLastReading(authStore.session.user, activePaper.value);
  if (route.query.panel === "analysis") {
    assistantTab.value = "chat";
    assistantExpanded.value = true;
  }
  await loadTranslationProviders();
  loadAnnotations();
  loadStructuredDocument();
  window.addEventListener("resize", updateTourRect);
  window.setTimeout(startReaderTour, 900);
});

onBeforeUnmount(() => {
  destroyed = true;
  clearTimeout(readerToastTimer);
  if (pdfObjectUrl) URL.revokeObjectURL(pdfObjectUrl);
  mineruAssetUrls.forEach(url => URL.revokeObjectURL(url));
  window.removeEventListener("resize", updateTourRect);
  pdfDocument?.destroy?.();
});
</script>

<style scoped>
.reader-workbench {
  --reader-accent: #2563eb;
  --reader-accent-soft: #eef4ff;
  --reader-canvas: #f3f6fa;
  --reader-line: #e3e9f2;
  --reader-panel: #f7f9fc;
  --reader-toolbar-height: 60px;
  height: 100vh;
  overflow: hidden;
  background: var(--reader-canvas);
  color: #20242c;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif;
}

.reader-toolbar {
  position: relative;
  z-index: 30;
  height: var(--reader-toolbar-height);
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto minmax(240px, 1fr);
  align-items: center;
  gap: 18px;
  padding: 0 16px;
  color: #263244;
  background: #ffffff;
  border-bottom: 0;
  box-sizing: border-box;
}

.reader-progress-track {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 0;
  overflow: hidden;
  background: transparent;
}

.reader-progress-fill {
  width: 0;
  height: 100%;
  background: var(--reader-accent);
  transition: width 120ms ease-out;
}

.reader-toolbar-start,
.reader-toolbar-end,
.reader-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.reader-toolbar-start { gap: 10px; }
.reader-toolbar-end { justify-content: flex-end; gap: 12px; color: #64748b; }
.reader-tools {
  gap: 18px;
  height: 38px;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
  white-space: nowrap;
}

.reader-back,
.reader-toolbar button,
.reader-tools button {
  height: 32px;
  border: 1px solid transparent;
  border-radius: 5px;
  background: transparent;
  color: #52637a;
  cursor: pointer;
  font: 600 12px/1 Inter, "PingFang SC", sans-serif;
}

.reader-back {
  width: 34px;
  height: 34px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--reader-line);
  border-radius: 6px;
  text-decoration: none;
  color: #4b596d;
  font-size: 17px;
  font-weight: 700;
}

.reader-back:hover { border-color: #b9c5d5; color: var(--reader-accent); background: var(--reader-panel); }

.reader-toolbar button:hover {
  color: #1d4ed8;
  background: var(--reader-accent-soft);
}

.reader-document-meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.reader-document-title {
  display: block;
  overflow: hidden;
  color: #202938;
  font-family: "Times New Roman", Georgia, "Songti SC", serif;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.05;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reader-document-source {
  overflow: hidden;
  color: #8290a3;
  font-size: 10px;
  font-weight: 600;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reader-translate-toggle {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 0 2px;
  border-color: transparent !important;
  background: transparent !important;
  color: #66758a;
  font-size: 14px !important;
  font-weight: 750 !important;
  transition: color 150ms ease;
}
.reader-translate-toggle:hover {
  color: #4b5a70 !important;
  background: transparent !important;
}
.reader-translate-toggle.active,
.reader-translate-toggle.active:hover {
  color: #4f46e5 !important;
  background: transparent !important;
}
.reader-translate-icon {
  position: relative;
  display: inline-block;
  width: 28px;
  height: 22px;
  flex: 0 0 auto;
  color: currentColor;
}
.lang-mark-source,
.lang-mark-target {
  position: absolute;
  font-family: Inter, "PingFang SC", sans-serif;
  font-weight: 800;
  line-height: 1;
}
.lang-mark-source {
  left: 0;
  top: 0;
  font-size: 18px;
}
.lang-mark-target {
  right: 0;
  bottom: 0;
  font-size: 14px;
}
.reader-translate-label { line-height: 1; }

.reader-eraser-button {
  display: inline-grid;
  width: 30px;
  place-items: center;
  padding: 0;
  color: #66758a !important;
  background: transparent !important;
}
.reader-eraser-button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.reader-eraser-button:hover {
  color: #4f46e5 !important;
  background: #eef2ff !important;
}
.reader-eraser-button.restorable {
  color: #4f46e5 !important;
  background: #eef2ff !important;
}

.reader-zoom-control {
  height: 24px;
  display: flex;
  align-items: center;
  margin-left: 2px;
  padding-left: 4px;
  border-left: 1px solid var(--reader-line);
}

.reader-zoom-control button { width: 28px; height: 28px; padding: 0; font-size: 15px; }
.scale-value { width: 40px; text-align: center; color: #66758a; font-size: 10px; font-variant-numeric: tabular-nums; }

.reader-status { display: flex; align-items: center; height: 32px; }
.reader-status-item { display: flex; align-items: baseline; gap: 5px; padding: 0 12px; border-right: 1px solid var(--reader-line); white-space: nowrap; }
.reader-status-item:first-child { padding-left: 0; }
.reader-status-item small { color: #8a96a7; font-size: 9px; font-weight: 650; }
.reader-status-item strong { color: #46556a; font-size: 11px; font-variant-numeric: tabular-nums; }

.reader-pdf-action { padding: 0 11px; border-color: var(--reader-line) !important; color: #344256 !important; background: #fff !important; }
.reader-pdf-action:hover { border-color: #aebbd0 !important; color: var(--reader-accent) !important; background: var(--reader-panel) !important; }
.pdf-label-short { display: none; }

.reader-body {
  height: calc(100vh - var(--reader-toolbar-height));
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr) 226px;
  min-height: 0;
  background:
    linear-gradient(90deg, #f8fafc 0, #f7f9fc 300px, #f4f7fb 430px, #edf3f8 100%);
}

.reader-assistant {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: transparent;
  border-right: 0;
  box-shadow: none;
  transition: width 200ms cubic-bezier(.22, 1, .36, 1);
}

.reader-assistant.collapsed { width: 46px; }
.reader-body:has(.reader-assistant.collapsed) { grid-template-columns: 46px minmax(0, 1fr) 226px; }
.reader-body.assistant-wide { grid-template-columns: clamp(440px, 38vw, 580px) minmax(440px, 1fr) 226px; }
.reader-assistant.expanded { width: auto; }

.assistant-tabs {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 4px;
  border-bottom: 0;
  padding: 6px 12px 4px;
  background: transparent;
  flex: 0 0 auto;
}

.assistant-tabs button {
  height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #667085;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}

.assistant-tabs button:hover { color: #334155; background: rgba(255, 255, 255, .66); }
.assistant-tabs button.active { color: var(--reader-accent); background: rgba(47, 109, 246, .1); }
.expand-button { margin-left: auto; color: var(--reader-accent) !important; font-size: 18px !important; }
.collapse-button { margin-left: auto; font-size: 17px !important; }
.expand-button + .collapse-button { margin-left: 2px; }
.reader-assistant.collapsed .assistant-tabs button:not(.collapse-button) { display: none; }

.assistant-scroll {
  flex: 1 1 0;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  background: transparent;
}
.assistant-scroll:has(.reader-report) { padding: 0; background: transparent; }

.assistant-scroll section { margin-bottom: 22px; }
.assistant-scroll h3 { margin: 0 0 9px; font-size: 13px; color: #1f2937; }
.assistant-scroll p,
.assistant-scroll li { font-size: 12px; line-height: 1.75; color: #4b5563; }
.assistant-scroll ul { margin: 0; padding-left: 18px; }

.question-chip {
  width: 100%;
  margin-bottom: 7px;
  padding: 9px 10px;
  border: 1px solid #e2e7ee;
  border-radius: 6px;
  background: #f8fafc;
  color: #3f4a5a;
  text-align: left;
  font-size: 12px;
  line-height: 1.5;
  cursor: pointer;
}

.outline-item {
  width: 100%;
  min-height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 8px;
  border: 0;
  border-bottom: 1px solid #edf0f3;
  background: transparent;
  color: #3f4a5a;
  text-align: left;
  font-size: 12px;
  line-height: 1.4;
  cursor: pointer;
}

.outline-item:hover { background: #f5f8fc; color: #1769e0; }
.outline-item small { flex: 0 0 auto; color: #98a2b3; }

.reading-stage {
  min-width: 0;
  overflow: auto;
  background: transparent;
  scroll-behavior: smooth;
}

.reading-column {
  width: min(100% - 86px, 1120px);
  min-height: 100%;
  margin: 0 auto;
  padding: 34px 62px 100px;
  border: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, .98), #fff 180px),
    #fff;
  box-sizing: border-box;
  box-shadow: 0 22px 58px rgba(31, 45, 68, .07);
  font-size: calc(16px * var(--reader-scale));
}

.paper-heading {
  padding-bottom: 24px;
  border-bottom: 1px solid #d9dee5;
}

.paper-source { color: #1769e0; font-size: 12px; }
.paper-heading h1 { max-width: 900px; margin: 10px 0 12px; font: 700 1.75em/1.35 "Times New Roman", "Songti SC", serif; }
.paper-heading p { margin: 0; color: #667085; font-size: 0.82em; }

.paper-abstract {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #e1e5ea;
}

.paper-abstract > strong {
  display: block;
  margin-bottom: 8px;
  font: 700 0.9em/1.4 "Times New Roman", "Songti SC", serif;
}

.paper-abstract .source-paragraph {
  color: #3f4857;
  font-size: 0.92em;
}

.reflow-page { position: relative; padding-top: 30px; scroll-margin-top: 16px; }
.page-marker { position: sticky; top: 10px; z-index: 2; float: right; padding: 4px 8px; border-radius: 4px; background: #eef2f6; color: #7a8494; font-size: 11px; }

.source-heading {
  clear: both;
  margin: 24px 0 10px;
  font: 700 1.1em/1.5 "Times New Roman", "Songti SC", serif;
  color: #1769e0;
}

.source-paragraph,
.translated-paragraph {
  max-width: 100%;
  margin: 0 0 7px;
  font-family: "Times New Roman", "Songti SC", serif;
  line-height: 1.75;
  text-align: justify;
}

.source-paragraph { color: #303846; }
.selectable-paragraph::selection { color: #172033; background: rgba(113, 126, 242, .2); }
.annotation-highlight {
  position: relative;
  padding: 0 .08em .08em;
  border-radius: 3px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 42%, rgba(137, 151, 255, .2) 42%, rgba(137, 151, 255, .2) 90%, rgba(255,255,255,0) 90%);
  box-shadow: inset 0 -1px rgba(79, 70, 229, .2);
  cursor: pointer;
}
.annotation-highlight:hover {
  background: linear-gradient(180deg, rgba(255,255,255,0) 34%, rgba(137, 151, 255, .3) 34%, rgba(137, 151, 255, .3) 92%, rgba(255,255,255,0) 92%);
}
.annotation-delete {
  display: inline-grid;
  width: 14px;
  height: 14px;
  margin-left: 3px;
  padding: 0;
  place-items: center;
  vertical-align: 2px;
  border: 1px solid rgba(79, 70, 229, .26);
  border-radius: 50%;
  color: #4f46e5;
  background: #f7f7ff;
  font: 800 10px/1 Inter, "PingFang SC", sans-serif;
  opacity: .72;
  cursor: pointer;
}
.annotation-delete:hover {
  color: #3730a3;
  border-color: rgba(79, 70, 229, .46);
  background: #eef2ff;
  opacity: 1;
}
.translation-block { margin: 3px 0 14px; padding: 0; border: 0; border-radius: 0; background: transparent; }
.translation-unit { margin: 4px 0 16px; padding-left: 12px; border-left: 2px solid #b9c4d6; }
.translated-paragraph {
  margin: 0;
  padding: 0;
  border-left: 0;
  color: #1f2a3d;
  font-family: "Songti SC", "STSong", "SimSun", "Times New Roman", serif;
  font-size: .96em;
  font-weight: 500;
  line-height: 1.9;
  letter-spacing: .012em;
}
.translated-paragraph.pending { color: #a0a8b5; }
.translated-paragraph.error { color: #b42318; }
.translation-reload {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-left: 6px;
  padding: 0;
  vertical-align: -3px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #9aa6b8;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: color 120ms ease, transform 400ms ease;
}
.translation-reload:hover { color: #1769e0; }
.translation-reload:active { transform: rotate(180deg); }
.translation-reload:disabled { color: #c4ccd6; cursor: default; }
.translated-paragraph.pending .translation-reload { display: none; }

.reader-color-menu {
  position: fixed;
  z-index: 60;
  display: grid;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #dfe5ee;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.16);
}
.reader-color-menu-title { color: #667085; font-size: 11px; }
.reader-color-menu-swatches { display: flex; gap: 8px; }
.reader-color-menu-swatch {
  position: relative;
  width: 22px;
  min-width: 22px;
  height: 22px;
  padding: 0;
  border: 1px solid #d8dee7;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
}
.reader-color-menu-swatch::after { position: absolute; inset: 4px; border-radius: 50%; content: ""; background: var(--swatch); }
.reader-font-color { background: transparent !important; }

.selection-translate-popover {
  position: fixed;
  z-index: 75;
  width: 392px;
  max-height: calc(100dvh - 24px);
  overflow-x: hidden;
  overflow-y: auto;
  border: 1px solid rgba(166, 176, 199, .42);
  border-radius: 18px;
  color: #263244;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, .98), rgba(249, 251, 255, .98));
  box-shadow: 0 18px 46px rgba(30, 41, 59, .16), 0 0 0 1px rgba(255, 255, 255, .72) inset;
  backdrop-filter: blur(14px);
}
.selection-popover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 15px 10px;
}
.selection-popover-head > div { display: grid; gap: 3px; }
.selection-popover-head strong { color: #172033; font-size: 14px; font-weight: 780; }
.selection-popover-head small { color: #8a94a4; font-size: 10px; font-weight: 650; }
.selection-kicker {
  color: #6366f1;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: .12em;
  text-transform: uppercase;
}
.selection-popover-head > button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 9px;
  color: #64748b;
  background: #eef2f7;
  cursor: pointer;
}
.selection-popover-head > button:hover { color: #1f2937; background: #e2e8f0; }
.selection-color-tools button {
  position: relative;
  width: 20px;
  min-width: 20px;
  height: 20px;
  padding: 0;
  border: 1px solid #d5dce6;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
}
.selection-color-tools button::after { position: absolute; inset: 4px; border-radius: 50%; content: ""; background: var(--swatch); }
.selection-source {
  max-height: 76px;
  overflow: auto;
  margin: 0 15px;
  padding: 11px 12px;
  border: 1px solid rgba(226, 232, 240, .9);
  border-radius: 12px;
  color: #475569;
  background: rgba(248, 250, 252, .78);
  font: 12px/1.65 "Times New Roman", "Songti SC", serif;
}
.selection-color-tools {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 11px 15px 0;
  color: #7c8798;
  font-size: 10px;
  font-weight: 650;
}
.selection-actions {
  display: grid;
  grid-template-columns: 1fr 1fr 74px;
  gap: 8px;
  padding: 12px 15px 10px;
}
.selection-actions button {
  min-height: 34px;
  border: 1px solid #d7dfeb;
  border-radius: 10px;
  color: #334155;
  background: #fff;
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
  transition: transform 140ms ease, border-color 140ms ease, background 140ms ease, box-shadow 140ms ease;
}
.selection-actions button:hover { transform: translateY(-1px); border-color: #b8c5d9; box-shadow: 0 8px 18px rgba(30, 41, 59, .08); }
.selection-actions .primary-action { color: #fff; border-color: #4f46e5; background: linear-gradient(135deg, #5b5ff0, #2563eb); }
.selection-actions button:disabled { opacity: .55; cursor: default; }
.selection-actions .annotation-action { color: #334155; background: #eef3f8; }
.selection-result {
  min-height: 64px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0 15px 14px;
  padding: 12px;
  border: 1px solid rgba(199, 210, 254, .72);
  border-radius: 13px;
  background: linear-gradient(180deg, #f8faff, #ffffff);
}
.selection-result strong { display: block; margin-bottom: 5px; color: #334155; font-size: 11px; }
.selection-result p { margin: 0; color: #243147; font: 12px/1.72 "Songti SC", "STSong", serif; white-space: pre-wrap; }
.selection-result.error p { color: #b42318; }
.selection-spinner {
  flex: 0 0 auto;
  width: 12px;
  height: 12px;
  margin-top: 3px;
  border: 2px solid #d9e4f2;
  border-top-color: #087f8c;
  border-radius: 50%;
  animation: spin 700ms linear infinite;
}
.selection-hint { margin: 0 15px 14px; color: #8792a3; font-size: 11px; line-height: 1.6; }
.selection-annotation-editor { margin: 0 15px 15px; padding: 10px; border: 1px solid #dfe6ef; border-radius: 13px; background: #f8fafc; }
.selection-annotation-editor textarea { width: 100%; box-sizing: border-box; resize: vertical; padding: 10px 11px; border: 1px solid #cfd7e2; border-radius: 10px; outline: 0; color: #263244; background: #fff; font: 11px/1.6 inherit; }
.selection-annotation-editor textarea:focus { border-color: #2f6df6; box-shadow: 0 0 0 2px rgba(47, 109, 246, .12); }
.selection-annotation-editor > div { display: flex; justify-content: flex-end; gap: 7px; margin-top: 8px; }
.selection-annotation-editor button { min-height: 30px; padding: 0 12px; border: 0; border-radius: 9px; color: #4b5563; background: #e7ebf0; font-size: 10px; cursor: pointer; }
.selection-annotation-editor button:last-child { color: #fff; background: #2563eb; }
.selection-annotation-editor button:disabled { opacity: .45; cursor: default; }
.block-annotation-note {
  position: relative;
  float: right;
  width: clamp(176px, 15vw, 232px);
  min-height: 42px;
  display: grid;
  grid-template-columns: 31px minmax(0, 1fr);
  align-items: start;
  gap: 9px;
  margin: -42px max(-258px, -18vw) 10px 0;
  color: #674d20;
  cursor: pointer;
}
.block-annotation-note::before {
  position: absolute;
  top: 15px;
  right: 100%;
  width: clamp(36px, 6vw, 92px);
  height: 1px;
  content: "";
  background: #cda960;
}
.block-annotation-actions {
  display: grid;
  gap: 6px;
}
.block-annotation-actions button {
  width: 31px;
  height: 31px;
  padding: 0;
  border: 1px solid #cda960;
  border-radius: 8px;
  color: #7a581d;
  background: #fff8e8;
  cursor: pointer;
}
.block-annotation-actions button:hover {
  color: #6f4306;
  border-color: #b9892f;
  background: #ffefd0;
}
.block-annotation-actions .block-annotation-remove {
  color: #a63a25;
  border-color: #efb0a5;
  background: #fff1ef;
}
.block-annotation-actions .block-annotation-remove:hover {
  color: #8c1d18;
  border-color: #e07362;
  background: #ffe3df;
}
.block-annotation-note p {
  max-height: 108px;
  overflow: auto;
  margin: 0;
  padding: 9px 10px;
  border: 1px solid #dcc590;
  border-radius: 8px;
  color: #5d4825;
  background: #fffaf0;
  font: 11px/1.55 "PingFang SC", sans-serif;
  overflow-wrap: anywhere;
}

.paper-chat-launcher {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 82;
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid rgba(148, 163, 184, .28);
  border-radius: 50%;
  color: #2563eb;
  background: rgba(255, 255, 255, .92);
  box-shadow: 0 16px 34px rgba(30, 41, 59, .16);
  backdrop-filter: blur(14px);
  cursor: pointer;
}
.paper-chat-launcher:hover { color: #1d4ed8; background: #fff; transform: translateY(-1px); }
.paper-chat-launcher.open { color: #fff; background: #263a5c; }
.paper-chat-launcher svg { width: 25px; height: 25px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.paper-chat-launcher > span { font-size: 26px; font-weight: 300; line-height: 1; }
.paper-chat-panel {
  position: fixed;
  right: 24px;
  bottom: 88px;
  z-index: 81;
  width: min(420px, calc(100vw - 32px));
  height: min(600px, calc(100vh - 120px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  overflow: hidden;
  border: 1px solid rgba(203, 213, 225, .82);
  border-radius: 20px;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 26px 60px rgba(30, 41, 59, .18);
  backdrop-filter: blur(18px);
}
.paper-chat-panel > header {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 15px 16px 13px;
  border-bottom: 1px solid #e8edf5;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, .92), rgba(248, 250, 252, .92));
}
.paper-chat-mark { width: 34px; height: 34px; display: grid; flex: 0 0 auto; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(135deg, #2563eb, #5b5ff0); font: 800 11px/1 Inter, sans-serif; }
.paper-chat-panel > header > div:last-child { display: grid; gap: 3px; }
.paper-chat-panel > header strong { color: #172033; font-size: 13px; }
.paper-chat-panel > header span { max-width: 320px; overflow: hidden; color: #64748b; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.paper-chat-messages { overflow-y: auto; padding: 17px 15px; background: linear-gradient(180deg, #fbfcff, #f7f9fc); }
.paper-chat-message { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 13px; }
.paper-chat-message > span { width: 25px; height: 25px; display: grid; flex: 0 0 auto; place-items: center; border-radius: 9px; color: #3153a3; background: #e8efff; font-size: 9px; font-weight: 800; }
.paper-chat-message p { max-width: 84%; margin: 0; padding: 10px 12px; border: 1px solid #eef2f7; border-radius: 13px; color: #273244; background: #fff; font-size: 11px; line-height: 1.68; white-space: pre-wrap; box-shadow: 0 8px 18px rgba(30, 41, 59, .045); }
.paper-chat-message.user { justify-content: flex-end; }
.paper-chat-message.user p { color: #fff; border-color: transparent; background: linear-gradient(135deg, #3558d8, #243b75); }
.paper-chat-thinking { display: flex; gap: 4px; align-items: center; min-height: 18px; }
.paper-chat-thinking i { width: 5px; height: 5px; border-radius: 50%; background: #7b8798; animation: chat-dot 1s ease-in-out infinite; }
.paper-chat-thinking i:nth-child(2) { animation-delay: .15s; }
.paper-chat-thinking i:nth-child(3) { animation-delay: .3s; }
.paper-chat-panel form { display: grid; grid-template-columns: minmax(0, 1fr) 38px; align-items: end; gap: 9px; padding: 12px 13px 8px; border-top: 1px solid #e5e9ef; background: #fff; }
.paper-chat-panel textarea { min-height: 44px; max-height: 110px; resize: none; box-sizing: border-box; padding: 11px 12px; border: 1px solid #cfd7e2; border-radius: 13px; outline: 0; color: #263244; background: #fff; font: 11px/1.45 inherit; }
.paper-chat-panel textarea:focus { border-color: #2f6df6; box-shadow: 0 0 0 2px rgba(47, 109, 246, .12); }
.paper-chat-panel form button { width: 38px; height: 38px; border: 0; border-radius: 12px; color: #fff; background: #2563eb; font-size: 18px; cursor: pointer; }
.paper-chat-panel form button:disabled { opacity: .4; cursor: default; }
.paper-chat-panel > small { padding: 0 14px 12px; color: #8993a2; background: #fff; font-size: 9px; }
.paper-chat-enter-active,
.paper-chat-leave-active { transition: opacity 160ms ease, transform 180ms cubic-bezier(.22, 1, .36, 1); transform-origin: bottom right; }
.paper-chat-enter-from,
.paper-chat-leave-to { opacity: 0; transform: translateY(8px) scale(.98); }
@keyframes chat-dot { 0%, 60%, 100% { opacity: .35; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-3px); } }
.reader-toast {
  position: fixed;
  left: 50%;
  bottom: 28px;
  z-index: 120;
  transform: translateX(-50%);
  padding: 9px 14px;
  border: 1px solid rgba(15, 23, 42, .08);
  border-radius: 999px;
  color: #fff;
  background: rgba(23, 32, 51, .92);
  box-shadow: 0 14px 32px rgba(15, 23, 42, .18);
  font-size: 12px;
  font-weight: 650;
}
.reader-toast-enter-active,
.reader-toast-leave-active { transition: opacity 160ms ease, transform 180ms ease; }
.reader-toast-enter-from,
.reader-toast-leave-to { opacity: 0; transform: translate(-50%, 8px); }
.reader-tour-layer { position: fixed; inset: 0; z-index: 100; pointer-events: auto; }
.reader-tour-shade { position: absolute; inset: 0; background: transparent; }
.reader-tour-focus {
  position: fixed;
  z-index: 1;
  border: 2px solid #3b82f6;
  border-radius: 11px;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, .18), 0 0 0 100vmax rgba(10, 16, 26, .7);
  pointer-events: none;
}
.reader-tour-card { position: absolute; z-index: 2; left: 50%; bottom: 46px; width: min(430px, calc(100vw - 32px)); transform: translateX(-50%); padding: 19px 20px; border-radius: 14px; background: #fff; box-shadow: 0 10px 30px rgba(0, 0, 0, .28); }
.reader-tour-card > span { color: #2451a6; font-size: 10px; font-weight: 750; }

@media (max-width: 1100px) {
  .reader-toolbar { grid-template-columns: minmax(180px, 1fr) auto auto; gap: 8px; padding-inline: 8px; }
  .reader-document-source { display: none; }
  .reader-status-item small { display: none; }
  .reader-body { grid-template-columns: 260px minmax(0, 1fr) 190px; }
  .reader-body:has(.reader-assistant.collapsed) { grid-template-columns: 46px minmax(0, 1fr) 190px; }
  .reader-body.assistant-wide { grid-template-columns: clamp(360px, 40vw, 460px) minmax(360px, 1fr) 190px; }
  .reading-column { width: calc(100% - 32px); padding: 30px 36px 90px; }
  .block-annotation-note { width: 31px; margin-right: -28px; }
  .block-annotation-note p { display: none; }
  .reader-paper-tab { grid-template-columns: 1fr; min-height: 50px; }
  .reader-paper-tab-mark { display: none; }
}

@media (max-width: 820px) {
  .reader-toolbar { grid-template-columns: minmax(0, 1fr) auto auto; }
  .reader-document-title { font-size: 12px; }
  .reader-status { display: none; }
  .reader-zoom-control { display: none; }
  .reader-body { position: relative; grid-template-columns: minmax(0, 1fr); }
  .reader-paper-rail { display: none; }
  .reader-assistant {
    position: absolute;
    inset: 0 auto 0 0;
    z-index: 22;
    width: min(84vw, 330px);
    box-shadow: 4px 0 12px rgba(15, 23, 42, .12);
  }
  .reader-assistant.collapsed { width: 42px; transform: translateX(-42px); }
  .reader-body:has(.reader-assistant.collapsed),
  .reader-body.assistant-wide { grid-template-columns: minmax(0, 1fr); }
  .reader-assistant.expanded { width: min(88vw, 520px); }
  .reading-column { width: 100%; padding: 26px 28px 86px; }
  .selection-translate-popover { width: min(360px, calc(100vw - 20px)); }
}

@media (max-width: 560px) {
  .reader-workbench { --reader-toolbar-height: 56px; }
  .reader-toolbar { gap: 4px; padding-inline: 6px; }
  .reader-toolbar-start { gap: 6px; }
  .reader-back { width: 32px; height: 32px; }
  .reader-document-title { font-size: 11px; }
  .reader-tools { height: 34px; padding: 0; }
  .reader-tools button { height: 28px; font-size: 10px; }
  .reader-translate-toggle { gap: 5px; padding-inline: 2px; }
  .reader-pdf-action { height: 32px !important; padding-inline: 8px; font-size: 10px !important; }
  .pdf-label-long { display: none; }
  .pdf-label-short { display: inline; }
  .reading-column { padding: 22px 18px 82px; font-size: calc(15px * var(--reader-scale)); }
  .paper-heading h1 { font-size: 1.42em; }
  .block-annotation-note {
    float: none;
    width: min(100%, 320px);
    grid-template-columns: 31px minmax(0, 1fr);
    margin: -3px 0 15px auto;
  }
  .block-annotation-note::before { width: 34px; }
  .block-annotation-note p { display: block; }
  .paper-chat-launcher { right: 14px; bottom: 14px; width: 48px; height: 48px; }
  .paper-chat-panel { right: 8px; bottom: 70px; width: calc(100vw - 16px); height: min(590px, calc(100vh - 82px)); }
  .reader-tour-card { bottom: 18px; width: calc(100vw - 20px); box-sizing: border-box; }
}
.reader-tour-card h2 { margin: 7px 0 7px; color: #24324a; font-size: 17px; }
.reader-tour-card p { margin: 0; color: #596579; font-size: 12px; line-height: 1.7; }
.reader-tour-card > div { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.reader-tour-card button { min-height: 34px; padding: 0 14px; border: 0; border-radius: 8px; color: #4b5563; background: #e9edf2; font-size: 11px; font-weight: 700; cursor: pointer; }
.reader-tour-card button:last-child { color: #fff; background: #2f6df6; }

.pdf-figure-card {
  display: block;
  margin: 22px auto 28px;
  padding: 0;
  border: 0;
  background: transparent;
}
.pdf-figure-image-button {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: #f7f8fa;
  cursor: zoom-in;
}
.pdf-figure-image {
  display: block;
  width: 100%;
  max-height: 640px;
  object-fit: contain;
  background: #fff;
}
.pdf-figure-placeholder {
  display: grid;
  min-height: 150px;
  place-items: center;
  color: #8a94a4;
  background: #f6f8fb;
  font-size: 12px;
}
.pdf-figure-card figcaption { display: flex; align-items: flex-start; gap: 12px; padding-top: 9px; }
.pdf-figure-caption { color: #303846; font-size: 0.86em; font-weight: 600; line-height: 1.55; font-family: "Times New Roman", serif; }
.pdf-figure-view { margin-left: auto; padding: 4px 10px; border: 1px solid #1769e0; border-radius: 5px; color: #1769e0; background: #fff; font-size: 11px; cursor: pointer; }
.pdf-figure-view:hover { background: #1769e0; color: #fff; }
.pdf-figure-overlay { position: fixed; inset: 0; z-index: 80; display: grid; place-items: center; background: rgba(15, 23, 42, 0.55); }
.pdf-figure-modal { width: min(90vw, 760px); max-height: 90vh; overflow: auto; padding: 14px; border-radius: 12px; background: #fff; }
.pdf-figure-modal header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.pdf-figure-modal header strong { color: #1769e0; font-size: 13px; }
.pdf-figure-modal header button { padding: 4px 10px; border: 0; border-radius: 5px; background: #eef2f6; color: #4a5568; font-size: 12px; cursor: pointer; }
.pdf-figure-modal canvas { max-width: 100%; height: auto !important; background: #fff; }
.pdf-figure-modal > img { display: block; max-width: 100%; max-height: calc(90vh - 70px); margin: 0 auto; object-fit: contain; }
.mineru-table { overflow-x: auto; padding: 14px; border: 1px solid #e2e7ef; background: #fff; }
.mineru-table :deep(table) { width: 100%; border-collapse: collapse; font: 12px/1.55 "Times New Roman", serif; }
.mineru-table :deep(th),
.mineru-table :deep(td) { padding: 7px 9px; border: 1px solid #cfd6e0; text-align: left; vertical-align: top; }
.mineru-equation { overflow-x: auto; margin: 18px 0; padding: 14px 18px; border-left: 3px solid #6d5dfc; background: #f7f6ff; color: #25233d; font: 15px/1.7 "Times New Roman", serif; white-space: pre-wrap; }
.reference-block { margin: 12px 0; color: #4a5362; font: 0.82em/1.75 "Times New Roman", "Songti SC", serif; white-space: pre-wrap; }
.assistant-empty { padding: 10px 12px; color: #8a94a4; font-size: 11px; }

.reader-state { min-height: 70vh; display: flex; align-items: center; justify-content: center; gap: 10px; color: #667085; font-size: 13px; }
.reader-state.error { color: #b42318; }
.reader-loading-state { width: min(520px, calc(100% - 40px)); min-height: 0; margin: 0 auto; padding-top: min(18vh, 170px); display: block; text-align: center; }
.reader-state-mark { width: 44px; height: 44px; display: grid; place-items: center; margin: 0 auto 18px; border-radius: 50%; background: #087f8c; }
.reader-state-mark span { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,.42); border-top-color: #fff; border-radius: 50%; animation: spin .8s linear infinite; }
.reader-loading-state h1 { margin: 0 0 10px; color: #202733; font-size: 20px; }
.reader-loading-state > p { margin: 0 auto; color: #667085; font-size: 13px; line-height: 1.7; }
.reader-loading-track { height: 5px; overflow: hidden; margin: 24px 0 8px; border-radius: 99px; background: #cbd2dd; }
.reader-loading-track i { display: block; height: 100%; border-radius: inherit; background: #087f8c; transition: width 180ms ease-out; }
.reader-loading-state small { color: #7a8494; font-size: 11px; }
.reader-loading-state .reader-process-note { margin-top: 28px; color: #8792a3; font-size: 11px; }

.reader-paper-rail {
  min-width: 0;
  overflow-y: auto;
  padding: 14px 10px;
  background: linear-gradient(180deg, rgba(248, 250, 252, .82), rgba(238, 243, 248, .92));
  border-left: 1px solid rgba(226, 232, 240, .9);
}

.reader-paper-rail-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin: 2px 4px 11px;
  color: #475569;
}
.reader-paper-rail-head span { font-size: 12px; font-weight: 800; }
.reader-paper-rail-head small { color: #8a96a7; font-size: 10px; font-weight: 650; }

.reader-paper-tab {
  width: 100%;
  min-height: 58px;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  margin-bottom: 8px;
  padding: 8px 9px;
  border: 1px solid transparent;
  border-radius: 14px;
  color: #475569;
  background: rgba(255, 255, 255, .58);
  text-align: left;
  cursor: pointer;
  transition: transform 150ms ease, border-color 150ms ease, background 150ms ease, box-shadow 150ms ease;
}
.reader-paper-tab:hover {
  transform: translateY(-1px);
  border-color: rgba(148, 163, 184, .3);
  background: rgba(255, 255, 255, .92);
  box-shadow: 0 12px 22px rgba(30, 41, 59, .07);
}
.reader-paper-tab.active {
  border-color: rgba(37, 99, 235, .42);
  color: #1d4ed8;
  background: linear-gradient(135deg, rgba(239, 246, 255, .98), rgba(255, 255, 255, .94));
  box-shadow: 0 12px 24px rgba(37, 99, 235, .09);
}
.reader-paper-tab-mark {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: #3153a3;
  background: #e8efff;
  font-size: 12px;
  font-weight: 850;
}
.reader-paper-tab.active .reader-paper-tab-mark {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #5b5ff0);
}
.reader-paper-tab-text {
  min-width: 0;
  display: grid;
  gap: 4px;
}
.reader-paper-tab-text strong {
  overflow: hidden;
  color: inherit;
  font-size: 11px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reader-paper-tab-text small {
  overflow: hidden;
  color: #8a96a7;
  font-size: 9px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reader-page-mini {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 14px 4px 0;
  padding-top: 12px;
  border-top: 1px solid rgba(226, 232, 240, .9);
}
.reader-page-mini button {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid transparent;
  border-radius: 9px;
  color: #64748b;
  background: rgba(255, 255, 255, .64);
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
}
.reader-page-mini button:hover { color: #334155; background: #fff; }
.reader-page-mini button.active { color: #fff; background: #2563eb; }
.reader-page-mini button.muted { color: #a0a8b5; cursor: default; background: transparent; }

@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
  .reader-assistant,
  .reading-stage { transition: none; scroll-behavior: auto; }
  .reader-state-mark span,
  .selection-spinner,
  .paper-chat-thinking i { animation: none; }
  .reader-progress-fill { transition: none; }
  .reader-loading-track i { transition: none; }
  .paper-chat-enter-active,
  .paper-chat-leave-active { transition: none; }
}
</style>
