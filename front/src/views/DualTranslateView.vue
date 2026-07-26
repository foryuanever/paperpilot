<template>
  <div class="dual-reader">
    <ReaderMultiTabBar target-path="/reader/dual" :target-query="{}" />
    <header class="dual-reader-toolbar-row">
      <div class="dual-toolbar-stage-area">
        <div class="dual-toolbar-center-dock">
          <div class="dock-zoom-widget">
            <button class="dock-zoom-btn instant-tooltip" data-tip="缩小正文 (-)" @click="zoomDualOut">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.8"><path d="M5 12h14"/></svg>
            </button>
            <div class="zoom-dropdown-wrap">
              <button class="dock-scale-chip instant-tooltip" data-tip="缩放预设 / 自适应" @click.stop="showZoomPresets = !showZoomPresets">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                <span>{{ Math.round(dualScale * 100) }}%</span>
                <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2"><path d="m6 9 6 6 6-6"/></svg>
              </button>
              <Transition name="tab-popover-fade">
                <div v-if="showZoomPresets" class="zoom-presets-popover" @click.stop>
                  <div class="zoom-popover-head">正文缩放预设</div>
                  <button
                    v-for="preset in zoomPresetList"
                    :key="preset.scale"
                    class="zoom-preset-item"
                    :class="{ active: Math.round(dualScale * 100) === Math.round(preset.scale * 100) }"
                    @click="setScalePreset(preset.scale)"
                  >
                    <span>{{ preset.label }}</span>
                    <svg v-if="Math.round(dualScale * 100) === Math.round(preset.scale * 100)" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                  </button>
                  <div class="preset-divider"></div>
                  <button class="zoom-preset-item fit-width-item" @click="fitWidth">
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="3" width="16" height="18" rx="2"/><line x1="8" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="16" y2="21"/></svg>
                    <span>自适应页宽</span>
                  </button>
                </div>
              </Transition>
            </div>
            <button class="dock-zoom-btn instant-tooltip" data-tip="放大正文 (+)" @click="zoomDualIn">
              <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.8"><path d="M12 5v14M5 12h14"/></svg>
            </button>
          </div>

          <span class="dock-divider"></span>

          <button
            class="dock-tool-btn instant-tooltip"
            :class="{ active: !isDrawingPenActive && activeAnnotateTool === 'select' }"
            data-tip="移动 / 划词选择 (V)"
            @click="setMoveTool"
          >
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6.3 3.4 18 15.1l-6.2 1.1-2.9 5.7L6.3 3.4Z"/>
              <path d="m12.2 15.8 4.8 4.8"/>
            </svg>
            <span>选择</span>
          </button>

          <button
            class="dock-tool-btn instant-tooltip"
            :class="{ active: isDrawingPenActive }"
            data-tip="自由画笔涂鸦 (Pen Drawing)"
            @click="toggleDrawingPen"
          >
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 20h9"/>
              <path d="M15.8 4.2a2.2 2.2 0 0 1 3.1 3.1L8.2 18 3.5 19.4 4.9 14.8 15.8 4.2Z"/>
            </svg>
            <span>画笔</span>
          </button>

          <div class="dock-style-wrapper">
            <div class="dock-color-swatches">
              <span
                v-for="color in textColors"
                :key="color.id"
                class="dock-color-dot instant-tooltip"
                :class="{ active: selectedColor.toLowerCase() === color.value.toLowerCase() }"
                :style="{ '--dot-color': color.value }"
                :data-tip="color.label"
                @click="handleDockColor(color.value)"
              ></span>
              <button
                class="dock-color-more-btn instant-tooltip"
                :class="{ active: showStylePopover }"
                data-tip="粗细大小与高级调色盘"
                @click.stop="showStylePopover = !showStylePopover"
              >
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 2a7 7 0 1 0 7 7"/>
                  <circle cx="7.5" cy="10.5" r=".5" fill="currentColor"/>
                  <circle cx="12" cy="7.5" r=".5" fill="currentColor"/>
                  <circle cx="16.5" cy="10.5" r=".5" fill="currentColor"/>
                </svg>
              </button>
            </div>

            <Transition name="tab-popover-fade">
              <div v-if="showStylePopover" class="dock-style-popover" @click.stop>
                <header class="style-popover-head">
                  <strong>标注样式与粗细设置</strong>
                  <span class="style-tool-badge">{{ currentToolLabel }}</span>
                </header>
                <div class="style-section">
                  <label>预设颜色</label>
                  <div class="style-color-grid">
                    <button
                      v-for="color in brushColors"
                      :key="color.id"
                      class="style-color-dot"
                      :class="{ active: selectedColor.toLowerCase() === color.value.toLowerCase(), light: color.light }"
                      :style="{ backgroundColor: color.value }"
                      :title="color.label"
                      @click="selectBrushColor(color.value)"
                    ></button>
                  </div>
                </div>
                <div class="style-custom-color-row">
                  <span>自定义颜色</span>
                  <button class="style-picker-trigger" title="打开颜色选择器" @click="activateNativeMarkColorPicker">
                    <i class="color-preview-circle" :style="{ background: selectedColor }"></i>
                    <span>选择…</span>
                  </button>
                  <input
                    ref="markColorPicker"
                    type="color"
                    class="hidden-color-input"
                    :value="selectedColor"
                    @input="selectBrushColor($event.target.value)"
                  />
                </div>
                <div class="style-divider"></div>
                <div class="style-control-row">
                  <div class="label-with-value">
                    <label>线条粗细 / 画笔大小</label>
                    <output>{{ brushWidth }}pt</output>
                  </div>
                  <div class="slider-with-preview">
                    <input v-model.number="brushWidth" type="range" min="1" max="12" step="1" />
                    <span class="stroke-preview-dot" :style="{ width: `${Math.max(3, brushWidth * 1.5)}px`, height: `${Math.max(3, brushWidth * 1.5)}px`, background: selectedColor }"></span>
                  </div>
                </div>
                <div class="style-control-row">
                  <div class="label-with-value">
                    <label>不透明度</label>
                    <output>{{ brushOpacity }}%</output>
                  </div>
                  <input v-model.number="brushOpacity" type="range" min="10" max="100" step="5" />
                </div>
              </div>
            </Transition>
          </div>

          <span class="dock-divider"></span>

          <div class="dock-actions-group">
            <button class="icon-tool-btn undo-action-btn instant-tooltip" data-tip="撤回上一条 (Undo)" @click="undoLastAnnotation">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 7v6h6"/>
                <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"/>
              </svg>
            </button>
            <button class="icon-tool-btn clear-action-btn instant-tooltip" data-tip="清除全部标记" @click="clearAllAnnotations">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 6h18"/>
                <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
              </svg>
            </button>
            <button class="icon-tool-btn pdf-icon-btn instant-tooltip" data-tip="打开原文 PDF" @click="openOriginalPdf">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </button>
            <button class="icon-tool-btn instant-tooltip" :data-tip="isDarkTheme ? '切换日间模式' : '切换夜间模式'" @click="toggleTheme">
              <svg v-if="isDarkTheme" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
              <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
            </button>
            <button class="icon-tool-btn instant-tooltip" data-tip="工具栏说明" @click="relaunchReaderTour">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 1 1 5.8 1c-.6 1.1-1.7 1.5-2.4 2.4-.4.5-.5 1-.5 1.6"/><circle cx="12" cy="17" r=".8" fill="currentColor"/></svg>
            </button>
            <button v-if="state === 'FAILURE' || error" class="icon-tool-btn instant-tooltip" data-tip="重新生成" @click="startTranslation">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M21 12a9 9 0 1 1-2.64-6.36"/><path d="M21 3v6h-6"/></svg>
            </button>
          </div>
        </div>
      </div>
      <div class="reader-progress-track-bottom" aria-label="翻译进度">
        <div class="reader-progress-fill-bottom" :style="{ width: `${progress}%` }"></div>
      </div>
    </header>

    <main ref="readerMain" @scroll.passive="handleReaderScroll">
      <div
        v-if="pagePairs.length"
        ref="annotationSurface"
        class="dual-annotation-surface"
        :class="{ annotating: drawingModeActive }"
      >
        <section class="spread-reader">
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
                    <p class="source-text-muted selectable-paragraph" :data-block-id="`${block.id}-source`">{{ block.text }}</p>
                    <p class="target-translation-text selectable-paragraph" :data-block-id="`${block.id}-target`" :class="{ loading: !block.translation }">
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
        <canvas
          ref="drawingCanvas"
          class="dual-drawing-layer"
          :class="{ active: drawingModeActive }"
          @pointerdown="startInkStroke"
          @pointermove="moveInkStroke"
          @pointerup="finishInkStroke"
          @pointercancel="cancelInkStroke"
          @pointerleave="finishInkStroke"
        ></canvas>
      </div>

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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useLibraryStore } from "../stores/library";
import ReaderMultiTabBar from "../components/ReaderMultiTabBar.vue";

const libraryStore = useLibraryStore();
const state = ref("PENDING");
const generationProgress = ref(3);
const readingProgress = ref(0);
const progress = computed(() => pagePairs.length ? readingProgress.value : generationProgress.value);
const error = ref("");
const pagePairs = reactive([]);
const canvasElements = new Map();
const readerMain = ref(null);
const annotationSurface = ref(null);
const drawingCanvas = ref(null);
const pageBlocksMap = reactive({});
const isDualPdfMode = ref(false);
const dualScale = ref(1);
const showZoomPresets = ref(false);
const zoomPresetList = [
  { label: "80%", scale: 0.8 },
  { label: "100%", scale: 1 },
  { label: "125%", scale: 1.25 },
  { label: "150%", scale: 1.5 },
];
const showStylePopover = ref(false);
const markColorPicker = ref(null);
const activeAnnotateTool = ref("select");
const selectedColor = ref("#eab308");
const isDrawingPenActive = ref(false);
const brushOpacity = ref(100);
const brushWidth = ref(3);
const drawingModeActive = computed(() => isDrawingPenActive.value);
const drawingStrokes = reactive([]);
let activeInkStroke = null;
let drawingFrame = 0;
const textColors = [
  { id: "white", label: "白色", value: "#ffffff" },
  { id: "black", label: "黑色", value: "#20242c" },
  { id: "red", label: "红色", value: "#ef4444" },
  { id: "blue", label: "蓝色", value: "#3b82f6" },
  { id: "yellow", label: "黄色", value: "#eab308" },
  { id: "green", label: "绿色", value: "#22c55e" },
  { id: "purple", label: "紫色", value: "#a855f7" },
];
const brushColors = [
  { id: "rose-200", label: "浅红", value: "#eea29b" },
  { id: "amber-200", label: "浅橙", value: "#f6c47a" },
  { id: "yellow-200", label: "浅黄", value: "#fee59a" },
  { id: "green-200", label: "浅绿", value: "#8ddda8" },
  { id: "cyan-200", label: "浅青", value: "#87dadd" },
  { id: "indigo-200", label: "浅蓝紫", value: "#9b99df" },
  { id: "purple-200", label: "浅紫", value: "#d491db" },
  { id: "red-500", label: "红色", value: "#e34b3f" },
  { id: "orange-500", label: "橙色", value: "#f28c22" },
  { id: "yellow-400", label: "黄色", value: "#f8c84f" },
  { id: "green-500", label: "绿色", value: "#4bc66b" },
  { id: "teal-400", label: "蓝绿", value: "#54c7c6" },
  { id: "blue-500", label: "蓝色", value: "#527ce0" },
  { id: "violet-500", label: "紫色", value: "#b54ac8" },
  { id: "red-900", label: "深红", value: "#8f2d25" },
  { id: "orange-900", label: "棕橙", value: "#a84e19" },
  { id: "amber-600", label: "深黄", value: "#ec9e2c" },
  { id: "green-800", label: "深绿", value: "#2d7f40" },
  { id: "teal-800", label: "深青", value: "#337d7d" },
  { id: "blue-900", label: "深蓝", value: "#334d8b" },
  { id: "purple-900", label: "深紫", value: "#6e2b7f" },
  { id: "white", label: "白色", value: "#ffffff", light: true },
  { id: "gray-300", label: "浅灰", value: "#d1d5db", light: true },
  { id: "gray-500", label: "灰色", value: "#9ca3af" },
  { id: "gray-700", label: "深灰", value: "#4b5563" },
  { id: "black", label: "黑色", value: "#111827" },
  { id: "true-black", label: "纯黑", value: "#000000" },
];
const currentToolLabel = computed(() => {
  if (isDrawingPenActive.value) return "自由手绘画笔";
  return {
    select: "划词选择",
  }[activeAnnotateTool.value] || "标注线形";
});
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

function setMoveTool() {
  activeAnnotateTool.value = "select";
  isDrawingPenActive.value = false;
  showStylePopover.value = false;
}

function toggleDrawingPen() {
  isDrawingPenActive.value = !isDrawingPenActive.value;
  activeAnnotateTool.value = isDrawingPenActive.value ? "pen" : "select";
}

function selectBrushColor(color) {
  selectedColor.value = color;
}

function handleDockColor(color) {
  selectBrushColor(color);
}

function activateNativeMarkColorPicker() {
  markColorPicker.value?.click();
}

function undoLastAnnotation() {
  drawingStrokes.pop();
  persistDrawingStrokes();
  redrawDrawingCanvas();
}

function clearAllAnnotations() {
  drawingStrokes.splice(0);
  activeInkStroke = null;
  persistDrawingStrokes();
  redrawDrawingCanvas();
  selectedColor.value = "#eab308";
  brushOpacity.value = 100;
  brushWidth.value = 3;
}

function relaunchReaderTour() {
  showStylePopover.value = !showStylePopover.value;
}

function handleGlobalClick() {
  showZoomPresets.value = false;
  showStylePopover.value = false;
}

function dprScale() {
  return Math.min(2.5, window.devicePixelRatio || 1);
}

function resizeDrawingCanvas() {
  const canvas = drawingCanvas.value;
  const surface = annotationSurface.value;
  if (!canvas || !surface) return;
  const width = Math.max(1, Math.ceil(surface.scrollWidth || surface.clientWidth));
  const height = Math.max(1, Math.ceil(surface.scrollHeight || surface.clientHeight));
  const dpr = dprScale();
  if (canvas.width !== Math.ceil(width * dpr) || canvas.height !== Math.ceil(height * dpr)) {
    canvas.width = Math.ceil(width * dpr);
    canvas.height = Math.ceil(height * dpr);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
  }
  redrawDrawingCanvas();
}

function surfacePointFromEvent(event) {
  const rect = annotationSurface.value?.getBoundingClientRect();
  if (!rect) return { x: 0, y: 0 };
  return {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  };
}

function linePointFromEvent(event) {
  const surfaceRect = annotationSurface.value?.getBoundingClientRect();
  if (!surfaceRect) return null;
  const paragraph = document.elementsFromPoint(event.clientX, event.clientY)
    .find(element => element?.classList?.contains("selectable-paragraph"));
  if (!paragraph) {
    const point = surfacePointFromEvent(event);
    if (activeInkStroke?.freeLineKey) {
      return {
        x: point.x,
        y: activeInkStroke.freeLineY,
        lineKey: activeInkStroke.freeLineKey,
      };
    }
    const rowHeight = 24;
    const snappedY = Math.round(point.y / rowHeight) * rowHeight;
    return {
      x: point.x,
      y: snappedY,
      lineKey: `free:${Math.round(snappedY / rowHeight)}`,
    };
  }
  const paragraphRect = paragraph.getBoundingClientRect();
  const style = window.getComputedStyle(paragraph);
  const fontSize = Number.parseFloat(style.fontSize) || 16;
  const lineHeight = Number.parseFloat(style.lineHeight) || fontSize * 1.65;
  const relativeY = Math.max(0, Math.min(paragraphRect.height, event.clientY - paragraphRect.top));
  const lineIndex = Math.max(0, Math.floor(relativeY / lineHeight));
  const lineTop = paragraphRect.top + lineIndex * lineHeight;
  const y = activeAnnotateTool.value === "strike"
    ? lineTop + lineHeight * 0.52
    : lineTop + lineHeight * 0.88;
  return {
    x: Math.max(paragraphRect.left + 2, Math.min(event.clientX, paragraphRect.right - 2)) - surfaceRect.left,
    y: y - surfaceRect.top,
    lineKey: `${paragraph.dataset?.blockId || "paragraph"}:${lineIndex}`,
  };
}

function drawWavyLine(context, startX, endX, y, amplitude = 3) {
  const left = Math.min(startX, endX);
  const right = Math.max(startX, endX);
  if (right - left < 2) return;
  const wavelength = 13;
  const steps = Math.max(8, Math.ceil((right - left) / 4));
  context.beginPath();
  context.moveTo(left, y);
  for (let index = 1; index <= steps; index += 1) {
    const t = index / steps;
    const x = left + (right - left) * t;
    context.lineTo(x, y + Math.sin(((right - left) * t / wavelength) * Math.PI * 2) * amplitude);
  }
  context.stroke();
}

function drawStroke(context, stroke) {
  if (!stroke) return;
  context.save();
  context.strokeStyle = stroke.color;
  context.lineWidth = stroke.tool === "highlight" ? Math.max(8, stroke.width * 2.8) : stroke.width;
  context.lineCap = "round";
  context.lineJoin = "round";
  context.globalAlpha = stroke.tool === "highlight"
    ? Math.min(0.42, Math.max(0.12, stroke.opacity))
    : Math.min(1, Math.max(0.1, stroke.opacity));
  if (Array.isArray(stroke.segments)) {
    stroke.segments.forEach(segment => {
      if (stroke.tool === "wavy") {
        drawWavyLine(context, segment.x1, segment.x2, segment.y, Math.max(2.5, stroke.width * 1.35));
      } else {
        context.beginPath();
        context.moveTo(segment.x1, segment.y);
        context.lineTo(segment.x2, segment.y);
        context.stroke();
      }
    });
    context.restore();
    return;
  }
  const points = Array.isArray(stroke.points) ? stroke.points : [];
  if (!points.length) {
    context.restore();
    return;
  }
  context.beginPath();
  context.moveTo(points[0].x, points[0].y);
  points.slice(1).forEach(point => context.lineTo(point.x, point.y));
  if (points.length === 1) context.lineTo(points[0].x + 0.1, points[0].y + 0.1);
  context.stroke();
  context.restore();
}

function redrawDrawingCanvas() {
  const canvas = drawingCanvas.value;
  if (!canvas) return;
  const dpr = dprScale();
  const context = canvas.getContext("2d");
  const width = Number.parseFloat(canvas.style.width) || canvas.width / dpr;
  const height = Number.parseFloat(canvas.style.height) || canvas.height / dpr;
  context.setTransform(dpr, 0, 0, dpr, 0, 0);
  context.clearRect(0, 0, width, height);
  drawingStrokes.forEach(stroke => drawStroke(context, stroke));
  if (activeInkStroke) drawStroke(context, activeInkStroke);
}

function scheduleDrawingResize() {
  if (drawingFrame) return;
  drawingFrame = window.requestAnimationFrame(() => {
    drawingFrame = 0;
    resizeDrawingCanvas();
  });
}

function extendLineStroke(stroke, point) {
  let segment = stroke.segments[stroke.segments.length - 1];
  if (!segment || segment.lineKey !== point.lineKey) {
    stroke.segments.push({
      lineKey: point.lineKey,
      x1: point.x,
      x2: point.x,
      y: point.y,
    });
    return;
  }
  segment.x1 = Math.min(segment.x1, point.x);
  segment.x2 = Math.max(segment.x2, point.x);
}

function startInkStroke(event) {
  if (!drawingModeActive.value) return;
  event.preventDefault();
  event.currentTarget?.setPointerCapture?.(event.pointerId);
  const tool = isDrawingPenActive.value ? "pen" : activeAnnotateTool.value;
  const firstPoint = tool === "pen" ? surfacePointFromEvent(event) : linePointFromEvent(event);
  if (!firstPoint) return;
  activeInkStroke = {
    id: `dual-ink-${Date.now()}`,
    tool,
    color: selectedColor.value,
    width: tool === "pen" ? Math.max(1, Number(brushWidth.value || 3)) : Math.max(1, Number(brushWidth.value || 2)),
    opacity: Math.min(1, Math.max(0.1, Number(brushOpacity.value || 100) / 100)),
    points: [firstPoint],
  };
  if (tool !== "pen") {
    if (String(firstPoint.lineKey || "").startsWith("free:")) {
      activeInkStroke.freeLineKey = firstPoint.lineKey;
      activeInkStroke.freeLineY = firstPoint.y;
    }
    activeInkStroke.segments = [];
    extendLineStroke(activeInkStroke, firstPoint);
  }
  redrawDrawingCanvas();
}

function moveInkStroke(event) {
  if (!drawingModeActive.value || !activeInkStroke) return;
  event.preventDefault();
  const tool = activeInkStroke.tool;
  const next = tool === "pen" ? surfacePointFromEvent(event) : linePointFromEvent(event);
  if (!next) return;
  const previous = activeInkStroke.points[activeInkStroke.points.length - 1];
  if (previous && Math.hypot(next.x - previous.x, next.y - previous.y) < 1.2) return;
  activeInkStroke.points.push(next);
  if (tool !== "pen") extendLineStroke(activeInkStroke, next);
  redrawDrawingCanvas();
}

function finishInkStroke(event) {
  if (!activeInkStroke) return;
  event?.preventDefault?.();
  if ((activeInkStroke.points || []).length > 1 || (activeInkStroke.segments || []).some(segment => Math.abs(segment.x2 - segment.x1) > 2)) {
    drawingStrokes.push(activeInkStroke);
    persistDrawingStrokes();
  }
  activeInkStroke = null;
  redrawDrawingCanvas();
}

function cancelInkStroke() {
  activeInkStroke = null;
  redrawDrawingCanvas();
}

function handleReaderScroll(event) {
  const target = event?.currentTarget || readerMain.value;
  if (!target) return;
  const maxScroll = Math.max(1, target.scrollHeight - target.clientHeight);
  readingProgress.value = Math.max(0, Math.min(100, Math.round((target.scrollTop / maxScroll) * 100)));
}

function drawingStorageKey() {
  return `paperpilot:dual-drawing:${workspaceId.value || "unknown"}`;
}

function loadDrawingStrokes() {
  drawingStrokes.splice(0);
  activeInkStroke = null;
  try {
    const stored = JSON.parse(localStorage.getItem(drawingStorageKey()) || "[]");
    if (Array.isArray(stored)) drawingStrokes.push(...stored);
  } catch (cacheError) {
    console.warn("dual drawing cache ignored", cacheError);
  }
  nextTick(scheduleDrawingResize);
}

function persistDrawingStrokes() {
  localStorage.setItem(drawingStorageKey(), JSON.stringify(drawingStrokes));
}

const paper = computed(() => libraryStore.activeDocument);
const workspaceId = computed(() => String(paper.value?.workspaceId || paper.value?.id || ""));
const stateTitle = computed(() => {
  if (state.value === "NATIVE") return "正在打开内置对照阅读";
  if (state.value === "PROGRESS") return "正在生成对照译文";
  if (state.value === "SUCCESS") return "译文已生成，正在排版";
  return "正在准备对照翻译";
});
const stateDescription = computed(() => {
  if (state.value === "PROGRESS") return "首次生成需要分析页面结构；完成后再次打开会直接读取缓存。";
  if (state.value === "NATIVE") return "开源对照翻译服务暂不可用，当前使用内置 PDF 阅读和段落翻译备用模式。";
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
    const targetWidth = Math.min(820, Math.max(420, (window.innerWidth - 72) / 2)) * dualScale.value;
    const scale = targetWidth / baseViewport.width;
    const viewport = page.getViewport({ scale });
    const outputScale = Math.min(2.5, window.devicePixelRatio || 1);
    canvas.width = Math.floor(viewport.width * outputScale);
    canvas.height = Math.floor(viewport.height * outputScale);
    canvas.style.width = `${viewport.width}px`;
    canvas.style.height = `${viewport.height}px`;
    const canvasContext = canvas.getContext("2d", { alpha: false });
    if (canvasContext) {
      canvasContext.imageSmoothingEnabled = true;
      canvasContext.imageSmoothingQuality = "high";
    }
    await page.render({
      canvasContext,
      viewport,
      transform: outputScale === 1 ? null : [outputScale, 0, 0, outputScale, 0, 0],
    }).promise;
  } catch (err) {
    console.warn("render single canvas error", pageNum, err);
  }
}

async function rerenderCanvases() {
  if (!pdfDocument || !pagePairs.length) return;
  await nextTick();
  await Promise.all(pagePairs.map(async pair => {
    const leftCanvas = canvasElements.get(`left-${pair.index}`);
    const rightCanvas = canvasElements.get(`right-${pair.index}`);
    if (leftCanvas && pair.leftPageNum) await renderSingleCanvas(pdfDocument, pair.leftPageNum, leftCanvas);
    if (rightCanvas && pair.rightPageNum) await renderSingleCanvas(pdfDocument, pair.rightPageNum, rightCanvas);
  }));
  scheduleDrawingResize();
}

function setScalePreset(scale) {
  dualScale.value = Math.min(1.8, Math.max(0.65, Number(scale) || 1));
  showZoomPresets.value = false;
  rerenderCanvases();
}

function fitWidth() {
  dualScale.value = 1;
  showZoomPresets.value = false;
  rerenderCanvases();
}

function zoomDualIn() {
  setScalePreset(Number((dualScale.value + 0.1).toFixed(2)));
}

function zoomDualOut() {
  setScalePreset(Number((dualScale.value - 0.1).toFixed(2)));
}

function openOriginalPdf() {
  const paperObj = paper.value || {};
  const source = paperObj.pdfUrl || paperObj.paperUrl || "";
  if (String(source).toLowerCase().startsWith("desktop-cache://")) return;
  const url = paperpilotApi.buildPdfProxyUrl(source);
  if (url) window.open(url, "_blank", "noopener,noreferrer");
}

async function resolveDualPdfSource() {
  const id = workspaceId.value;
  if (window.paperSolverDesktop?.getCachedPdf && id) {
    try {
      const cached = await window.paperSolverDesktop.getCachedPdf({ workspaceId: id });
      if (cached?.found && cached.base64) {
        return base64ToUint8Array(cached.base64);
      }
    } catch (error) {
      console.warn("desktop dual pdf cache read failed", error);
    }
  }
  const paperObj = paper.value || {};
  const source = paperObj.pdfUrl || paperObj.paperUrl || "";
  if (String(source).toLowerCase().startsWith("desktop-cache://")) return "";
  return paperpilotApi.buildPdfProxyUrl(source);
}

function base64ToUint8Array(base64) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
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

async function loadNativePdfDualView(reason = "") {
  try {
    state.value = "NATIVE";
    generationProgress.value = 30;
    readingProgress.value = 0;
    isDualPdfMode.value = false;
    error.value = "";

    const documentSource = await resolveDualPdfSource();
    if (!documentSource) throw new Error("缺失论文 PDF 资源，或本机缓存不存在");

    const [pdfjs, workerModule] = await Promise.all([
      import("pdfjs-dist"),
      import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
    ]);
    pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;
    const loadingTask = pdfjs.getDocument(documentSource);
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
    canvasElements.clear();
    for (let i = 1; i <= pdfDocument.numPages; i++) {
      const blocks = pageBlocksMap[i] || [];
      pagePairs.push({
        index: i,
        leftPageNum: i,
        blocks: blocks,
      });
    }

    state.value = "SUCCESS";
    generationProgress.value = 100;
    await nextTick();
    readerMain.value?.scrollTo?.({ top: 0, behavior: "auto" });
    loadDrawingStrokes();

    // 渲染左侧原版英文 PDF
    await Promise.all(pagePairs.map(async pair => {
      const leftCanvas = canvasElements.get(`left-${pair.index}`);
      if (leftCanvas) await renderSingleCanvas(pdfDocument, pair.leftPageNum, leftCanvas);
    }));
    scheduleDrawingResize();

    // 自动为右侧段落填充中文译文
    translateAllPageBlocks();
  } catch (err) {
    console.warn("native pdf dual view fallback failed", err);
    const fallbackReason = friendlyError(err, "内置对照阅读也无法打开 PDF");
    error.value = reason
      ? `${reason}；备用模式也失败：${fallbackReason}`
      : `对照翻译暂不可用：${fallbackReason}`;
  }
}

async function startTranslation() {
  clearInterval(pollTimer);
  error.value = "";
  state.value = "PENDING";
  generationProgress.value = 3;
  readingProgress.value = 0;
  pagePairs.splice(0);
  canvasElements.clear();
  loadDrawingStrokes();
  activeInkStroke = null;
  Object.keys(pageBlocksMap).forEach(key => delete pageBlocksMap[key]);
  pdfDocument?.destroy?.();
  pdfDocument = null;
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
    await loadNativePdfDualView(friendlyError(requestError, "开源对照翻译服务未启动或模型加载失败"));
  }
}

async function refreshStatus() {
  try {
    const result = await paperpilotApi.getPdfMathTranslationStatus(workspaceId.value);
    state.value = String(result?.state || "PENDING").toUpperCase();
    const info = result?.info || {};
    const current = Number(info.n || 0);
    const total = Number(info.total || 0);
    generationProgress.value = state.value === "SUCCESS"
      ? 100
      : total > 0
        ? Math.max(5, Math.min(98, Math.round((current / total) * 100)))
        : Math.min(92, generationProgress.value + 3);
    if (state.value === "SUCCESS") {
      clearInterval(pollTimer);
      await loadTranslatedPdf();
    } else if (state.value === "FAILURE") {
      clearInterval(pollTimer);
      await loadNativePdfDualView(String(result?.message || "开源对照翻译任务失败，已尝试切换备用模式"));
    }
  } catch (requestError) {
    clearInterval(pollTimer);
    await loadNativePdfDualView(friendlyError(requestError, "开源对照翻译状态服务暂不可用"));
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
  canvasElements.clear();
  for (let page = 1, index = 1; page <= pdfDocument.numPages; page += 2, index += 1) {
    pagePairs.push({
      index,
      hasRight: page + 1 <= pdfDocument.numPages,
      leftPageNum: page,
      rightPageNum: page + 1 <= pdfDocument.numPages ? page + 1 : null,
    });
  }
  await nextTick();
  readerMain.value?.scrollTo?.({ top: 0, behavior: "auto" });
  loadDrawingStrokes();
  await Promise.all(pagePairs.map(async pair => {
    const leftCanvas = canvasElements.get(`left-${pair.index}`);
    const rightCanvas = canvasElements.get(`right-${pair.index}`);
    if (leftCanvas && pair.leftPageNum) await renderSingleCanvas(pdfDocument, pair.leftPageNum, leftCanvas);
    if (rightCanvas && pair.rightPageNum) await renderSingleCanvas(pdfDocument, pair.rightPageNum, rightCanvas);
  }));
  scheduleDrawingResize();
}

onMounted(async () => {
  window.addEventListener("click", handleGlobalClick);
  window.addEventListener("resize", scheduleDrawingResize);
  await libraryStore.hydrateLibrary();
  if (!workspaceId.value) {
    error.value = "未选择需要翻译的文献，请返回文献库重新打开。";
    return;
  }
  startTranslation();
});

watch(workspaceId, (nextId, previousId) => {
  if (!nextId || !previousId || nextId === previousId) return;
  startTranslation();
});

onBeforeUnmount(() => {
  window.removeEventListener("click", handleGlobalClick);
  window.removeEventListener("resize", scheduleDrawingResize);
  if (drawingFrame) window.cancelAnimationFrame(drawingFrame);
  clearInterval(pollTimer);
  pdfDocument?.destroy?.();
});
</script>

<style scoped>
.dual-reader { height: 100vh; overflow: hidden; color: #202733; background: #dfe4eb; font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif; }
.dual-reader-toolbar-row {
  position: relative;
  z-index: 24;
  height: 46px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: center;
  box-sizing: border-box;
  border-bottom: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(248, 250, 252, 0.94);
  backdrop-filter: blur(16px);
}
.dual-toolbar-stage-area { display: flex; justify-content: center; min-width: 0; padding: 0 12px; }
.dual-toolbar-center-dock {
  display: inline-flex;
  align-items: center;
  max-width: calc(100vw - 24px);
  min-width: 0;
  height: 34px;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  overflow-x: auto;
  scrollbar-width: none;
}
.dual-toolbar-center-dock::-webkit-scrollbar {
  display: none;
}
.dock-zoom-widget,
.dock-actions-group {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.dock-zoom-btn,
.icon-tool-btn {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 0;
  border-radius: 999px;
  color: #64748b;
  background: transparent;
  cursor: pointer;
}
.dock-zoom-btn:hover,
.icon-tool-btn:hover,
.dock-tool-btn:hover {
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.1);
}
.zoom-dropdown-wrap { position: relative; }
.dock-scale-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 8px;
  border: 0;
  border-radius: 999px;
  color: #334155;
  background: rgba(15, 23, 42, 0.06);
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
}
.zoom-presets-popover {
  position: absolute;
  top: 34px;
  left: 50%;
  z-index: 40;
  width: 156px;
  padding: 8px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.16);
  transform: translateX(-50%);
}
.zoom-popover-head { padding: 4px 6px 7px; color: #64748b; font-size: 11px; font-weight: 800; }
.zoom-preset-item {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 8px;
  border: 0;
  border-radius: 8px;
  color: #334155;
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.zoom-preset-item:hover,
.zoom-preset-item.active { color: #4f46e5; background: rgba(99, 102, 241, 0.1); }
.preset-divider,
.dock-divider { width: 1px; height: 18px; background: rgba(148, 163, 184, 0.24); }
.zoom-presets-popover .preset-divider { width: 100%; height: 1px; margin: 5px 0; }
.dock-tool-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 8px;
  border: 0;
  border-radius: 999px;
  color: #64748b;
  background: transparent;
  font-size: 11.5px;
  font-weight: 750;
  cursor: default;
  white-space: nowrap;
  flex: 0 0 auto;
}
.dock-tool-btn.active { color: #4f46e5; background: rgba(99, 102, 241, 0.12); }
.dock-tool-btn svg,
.icon-tool-btn svg,
.dock-zoom-btn svg {
  flex: 0 0 auto;
}
.mark-letter {
  position: relative;
  font-family: Georgia, "Times New Roman", serif;
  font-weight: 800;
  font-size: 13px;
  line-height: 1;
}
.mark-underline::after {
  content: "";
  position: absolute;
  left: -1px;
  right: -1px;
  bottom: -2px;
  height: 2px;
  background: currentColor;
}
.mark-strike::after {
  content: "";
  position: absolute;
  left: -1px;
  right: -1px;
  top: 50%;
  height: 2px;
  background: currentColor;
  transform: translateY(-50%);
}
.mark-wavy::after {
  content: "";
  position: absolute;
  left: -1px;
  right: -1px;
  bottom: -2px;
  height: 3px;
  background: radial-gradient(circle at 2px 2px, transparent 1.5px, currentColor 1.7px, currentColor 2.5px, transparent 2.7px) 0 0 / 6px 3px repeat-x;
}
.dock-style-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}
.dock-color-swatches {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 2px;
}
.dock-color-dot {
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
  border: 2px solid transparent;
  border-radius: 50%;
  background: var(--dot-color);
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;
}
.dock-color-dot:hover,
.dock-color-dot.active {
  transform: scale(1.24);
  border-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.5);
}
.dock-color-more-btn {
  display: inline-grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border: 1px solid rgba(148, 163, 184, 0.42);
  border-radius: 50%;
  color: #64748b;
  background: rgba(255, 255, 255, 0.84);
  cursor: pointer;
  transition: all 0.15s ease;
}
.dock-color-more-btn:hover,
.dock-color-more-btn.active {
  color: #ffffff;
  border-color: #4f46e5;
  background: #4f46e5;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.28);
}
.dock-style-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 70;
  width: 280px;
  padding: 14px;
  box-sizing: border-box;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(20px);
}
.style-popover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.78);
}
.style-popover-head strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}
.style-tool-badge {
  padding: 2px 8px;
  border-radius: 999px;
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.12);
  font-size: 11px;
  font-weight: 800;
}
.style-section {
  margin-bottom: 10px;
}
.style-section label,
.style-custom-color-row span {
  color: #64748b;
  font-size: 11.5px;
  font-weight: 700;
}
.style-color-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-top: 7px;
}
.style-color-dot {
  width: 24px;
  height: 24px;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.14s ease, box-shadow 0.14s ease;
}
.style-color-dot.light {
  border-color: rgba(148, 163, 184, 0.55);
}
.style-color-dot:hover,
.style-color-dot.active {
  transform: scale(1.16);
  border-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.52);
}
.style-custom-color-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}
.style-picker-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 9px;
  border: 1px solid #cbd5e1;
  border-radius: 7px;
  color: #334155;
  background: #ffffff;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
}
.color-preview-circle {
  width: 14px;
  height: 14px;
  border: 1px solid rgba(15, 23, 42, 0.16);
  border-radius: 50%;
}
.hidden-color-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}
.style-divider {
  height: 1px;
  margin: 10px 0;
  background: rgba(226, 232, 240, 0.8);
}
.style-control-row {
  margin-bottom: 10px;
}
.label-with-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 11.5px;
  font-weight: 700;
}
.label-with-value output {
  color: #0f172a;
  font-weight: 800;
}
.slider-with-preview {
  display: flex;
  align-items: center;
  gap: 10px;
}
.slider-with-preview input[type="range"],
.style-control-row input[type="range"] {
  flex: 1;
  accent-color: #6366f1;
}
.stroke-preview-dot {
  flex: 0 0 auto;
  border-radius: 50%;
  transition: all 0.12s ease;
}
.layout-label { color: #667085; font-size: 11px; white-space: nowrap; }
.translate-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.1);
  font-size: 11.5px;
  font-weight: 800;
  text-decoration: none;
  animation: immersive-link-blink 1.55s ease-in-out infinite;
}
.translate-pill-btn .lang-mark { color: #6366f1; font-size: 11px; font-weight: 900; }
.reader-progress-track-bottom { position: absolute; left: 0; right: 0; bottom: 0; height: 2px; background: rgba(148, 163, 184, 0.16); }
.reader-progress-fill-bottom { height: 100%; background: linear-gradient(90deg, #6366f1, #22d3ee); transition: width 180ms ease-out; }
.instant-tooltip {
  position: relative;
}
.instant-tooltip::after {
  content: attr(data-tip);
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  z-index: 999;
  padding: 4px 9px;
  border-radius: 6px;
  color: #ffffff;
  background: #0f172a;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transform: translateX(-50%) translateY(4px) scale(0.96);
  transition: opacity 0.08s ease, transform 0.08s ease, visibility 0.08s ease;
}
.instant-tooltip:hover::after {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0) scale(1);
}
.dual-reader main { height: calc(100vh - 84px); overflow: auto; }
.dual-annotation-surface {
  position: relative;
  min-height: 100%;
}
.dual-annotation-surface.annotating {
  cursor: crosshair;
}
.dual-drawing-layer {
  position: absolute;
  inset: 0 auto auto 0;
  z-index: 12;
  display: block;
  pointer-events: none;
  touch-action: none;
}
.dual-drawing-layer.active {
  pointer-events: auto;
}
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
@keyframes immersive-link-blink {
  0%, 100% { box-shadow: 0 0 0 rgba(91, 53, 213, 0); opacity: 0.82; }
  50% { box-shadow: 0 0 18px rgba(91, 53, 213, 0.42); opacity: 1; }
}
@media (max-width: 820px) {
  .layout-label { display: none; }
  .dual-toolbar-center-dock { overflow-x: auto; justify-content: flex-start; }
  .page-spread { grid-template-columns: minmax(0, 1fr); }
  .spread-head { display: none; }
}
@media (prefers-reduced-motion: reduce) { .state-mark span, .translate-pill-btn { animation: none; } .progress-track i { transition: none; } }
/* ── DARK MODE ADAPTATIONS FOR DUAL TRANSLATE VIEW ── */
:root[data-theme="dark"] .dual-reader {
  background: #08080c;
  color: #e2e2e6;
}

:root[data-theme="dark"] .dual-reader-toolbar-row {
  border-bottom-color: rgba(148, 163, 184, 0.16);
  background: rgba(15, 23, 42, 0.94);
}

:root[data-theme="dark"] .dual-toolbar-center-dock {
  border-color: rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.76);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.32);
}

:root[data-theme="dark"] .dock-scale-chip {
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .dock-zoom-btn,
:root[data-theme="dark"] .icon-tool-btn,
:root[data-theme="dark"] .dock-tool-btn {
  color: #cbd5e1;
}

:root[data-theme="dark"] .dock-zoom-btn:hover,
:root[data-theme="dark"] .icon-tool-btn:hover,
:root[data-theme="dark"] .dock-tool-btn:hover,
:root[data-theme="dark"] .dock-tool-btn.active {
  color: #e0e7ff;
  background: rgba(99, 102, 241, 0.2);
}

:root[data-theme="dark"] .dock-color-more-btn {
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .dock-color-more-btn:hover,
:root[data-theme="dark"] .dock-color-more-btn.active {
  color: #f8fafc;
  border-color: #6366f1;
  background: rgba(99, 102, 241, 0.34);
}

:root[data-theme="dark"] .dock-style-popover {
  border-color: rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.98);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.58);
}

:root[data-theme="dark"] .style-popover-head {
  border-bottom-color: rgba(148, 163, 184, 0.18);
}

:root[data-theme="dark"] .style-popover-head strong,
:root[data-theme="dark"] .label-with-value output {
  color: #f8fafc;
}

:root[data-theme="dark"] .style-section label,
:root[data-theme="dark"] .style-custom-color-row span,
:root[data-theme="dark"] .label-with-value {
  color: #94a3b8;
}

:root[data-theme="dark"] .style-picker-trigger {
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .style-divider {
  background: rgba(148, 163, 184, 0.18);
}

:root[data-theme="dark"] .layout-label {
  color: #94a3b8;
}

:root[data-theme="dark"] .zoom-presets-popover {
  border-color: rgba(148, 163, 184, 0.22);
  background: #111827;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.42);
}

:root[data-theme="dark"] .zoom-popover-head {
  color: #94a3b8;
}

:root[data-theme="dark"] .zoom-preset-item {
  color: #cbd5e1;
}

:root[data-theme="dark"] .zoom-preset-item:hover,
:root[data-theme="dark"] .zoom-preset-item.active {
  color: #e0e7ff;
  background: rgba(99, 102, 241, 0.2);
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
