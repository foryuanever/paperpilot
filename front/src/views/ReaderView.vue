<template>
  <div class="reader-workbench">
    <header class="reader-header-wrapper">
      <!-- 第一行：多文献多标签导航栏 -->
      <ReaderMultiTabBar />

      <!-- 第二行：极简集中式阅读工具栏 (精准自动匹配正文 reading-stage 列) -->
      <div
        class="reader-toolbar-row"
        :class="{
          'assistant-collapsed': assistantCollapsed,
          'assistant-wide': assistantExpanded && assistantTab === 'chat',
          'right-notes-open': rightNotesOpen,
          'right-notes-closed': !rightNotesOpen
        }"
      >
        <!-- 左侧边栏对应空占位块 (与左侧边栏宽度 1:1 像素同步) -->
        <div class="toolbar-sidebar-spacer"></div>

        <!-- 正文阅读区对应工具列 (绝对自动居中于正文阅读 Stage) -->
        <div class="toolbar-stage-area">
          <div class="toolbar-center-dock">
            <!-- 核心高阶缩放 Widget (- 🔍 130% ▾ +) -->
            <div class="dock-zoom-widget">
              <button class="dock-zoom-btn instant-tooltip" data-tip="缩小正文 (-)" @click="zoomReaderOut">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.8"><path d="M5 12h14"/></svg>
              </button>

              <!-- 缩放百分比 Pill + 下拉预设选单 -->
              <div class="zoom-dropdown-wrap">
                <button class="dock-scale-chip instant-tooltip" data-tip="缩放预设 / 自适应" @click.stop="showZoomPresets = !showZoomPresets">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                  <span>{{ Math.round(contentScale * 100) }}%</span>
                  <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2" style="margin-left: 1px;"><path d="m6 9 6 6 6-6"/></svg>
                </button>

                <!-- 预设下拉弹出窗 -->
                <Transition name="tab-popover-fade">
                  <div v-if="showZoomPresets" class="zoom-presets-popover" @click.stop>
                    <div class="zoom-popover-head">正文缩放预设</div>
                    <button
                      v-for="preset in zoomPresetList"
                      :key="preset.scale"
                      class="zoom-preset-item"
                      :class="{ active: Math.round(contentScale * 100) === Math.round(preset.scale * 100) }"
                      @click="setScalePreset(preset.scale)"
                    >
                      <span>{{ preset.label }}</span>
                      <svg v-if="Math.round(contentScale * 100) === Math.round(preset.scale * 100)" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                    </button>

                    <div class="preset-divider"></div>

                    <button class="zoom-preset-item fit-width-item" @click="fitWidth(); showZoomPresets = false;">
                      <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="3" width="16" height="18" rx="2"/><line x1="8" y1="3" x2="8" y2="21"/><line x1="16" y1="3" x2="16" y2="21"/></svg>
                      <span>自适应页宽</span>
                    </button>
                  </div>
                </Transition>
              </div>

              <button class="dock-zoom-btn instant-tooltip" data-tip="放大正文 (+)" @click="zoomReaderIn">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.8"><path d="M12 5v14M5 12h14"/></svg>
              </button>
            </div>

            <span class="dock-divider"></span>

            <!-- 1. 移动 / 选择模式 -->
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

            <!-- 2. 文本高亮 -->
            <button
              class="dock-tool-btn instant-tooltip"
              :class="{ active: !isDrawingPenActive && (activeAnnotateTool === 'highlight' || activeAnnotateTool === 'fontColor') }"
              data-tip="文本高亮 (Highlighter)"
              @click="activeAnnotateTool = 'highlight'; isDrawingPenActive = false;"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m9 11-6 6v3h3l6-6m-3-3 3-3 6 6-3 3m-6-6 6-6 3 3-6 6"/>
              </svg>
              <span>高亮</span>
            </button>

            <!-- 3. 下划线 (Underline) -->
            <button
              class="dock-tool-btn instant-tooltip"
              :class="{ active: !isDrawingPenActive && activeAnnotateTool === 'underline' }"
              data-tip="文本下划线 (Underline)"
              @click="setLineTool('underline')"
            >
              <span class="mark-letter mark-underline">U</span>
              <span>下划线</span>
            </button>

            <!-- 4. 删除线 (Strikethrough) -->
            <button
              class="dock-tool-btn instant-tooltip"
              :class="{ active: !isDrawingPenActive && activeAnnotateTool === 'strike' }"
              data-tip="文本删除线 (Strikethrough)"
              @click="setLineTool('strike')"
            >
              <span class="mark-letter mark-strike">S</span>
              <span>删除线</span>
            </button>

            <!-- 5. 波浪线 (Wavy Line) -->
            <button
              class="dock-tool-btn instant-tooltip"
              :class="{ active: !isDrawingPenActive && activeAnnotateTool === 'wavy' }"
              data-tip="文本波浪线 (Wavy Line)"
              @click="setLineTool('wavy')"
            >
              <span class="mark-letter mark-wavy">W</span>
              <span>波浪线</span>
            </button>

            <!-- 6. 自由手绘画笔图画 (Pen) -->
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

            <!-- 7. 色彩盘 + 全标注粗细大小与高级调色组件 -->
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

                <!-- 🎨 粗细与高级调色盘按钮 -->
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

              <!-- 🎨 高级色彩与粗细大小控制弹出框 (适用于画笔、高亮、划线等所有标注) -->
              <Transition name="tab-popover-fade">
                <div v-if="showStylePopover" class="dock-style-popover" @click.stop>
                  <header class="style-popover-head">
                    <strong>标注样式与粗细设置</strong>
                    <span class="style-tool-badge">{{ currentToolLabel }}</span>
                  </header>

                  <!-- 快速预设色彩网格 -->
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

                  <!-- 自定义颜色 -->
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

                  <!-- 粗细大小调节 -->
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

                  <!-- 不透明度调节 -->
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

            <!-- 右侧动作组向中间紧凑整合 -->
            <div class="dock-actions-group">
              <button
                class="icon-tool-btn undo-action-btn instant-tooltip"
                :disabled="!annotations.length && !drawingStrokes.length && !clearedAnnotationSnapshot.length"
                data-tip="撤回上一条 (Undo)"
                @click="undoLastAnnotation"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 7v6h6"/>
                  <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13"/>
                </svg>
              </button>

              <button
                class="icon-tool-btn clear-action-btn instant-tooltip"
                :disabled="!annotations.length && !drawingStrokes.length && !clearedAnnotationSnapshot.length"
                data-tip="清除全部标记"
                @click="clearAllAnnotations"
              >
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M3 6h18"/>
                  <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/>
                  <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
                </svg>
              </button>

              <button
                class="icon-tool-btn pdf-icon-btn instant-tooltip"
                data-tip="打开原文 PDF"
                @click="openOriginalPdf"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
              </button>

              <button
                class="translate-pill-btn instant-tooltip"
                :class="{ active: autoTranslate }"
                :data-tip="autoTranslate ? '关闭全文对照翻译' : '开启全文对照翻译'"
                @click="toggleTranslation"
              >
                <span class="lang-mark">文/A</span>
                <span>沉浸翻译</span>
              </button>

              <button
                class="icon-tool-btn instant-tooltip"
                :data-tip="isDarkTheme ? '切换日间模式' : '切换夜间模式'"
                @click="toggleTheme"
              >
                <svg v-if="isDarkTheme" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
                <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
              </button>

              <button
                class="icon-tool-btn instant-tooltip"
                data-tip="新手操作指引"
                @click="relaunchReaderTour"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 右侧边栏对应空占位块 -->
        <div class="toolbar-notes-spacer"></div>
      </div>

      <!-- 导航栏底部的微光阅读进度条 (100% 满屏通栏) -->
      <div class="reader-progress-track-bottom" aria-label="阅读进度">
        <div class="reader-progress-fill-bottom" :style="{ width: `${readingProgress}%` }"></div>
      </div>
    </header>

    <div
      class="reader-body"
      :class="{
        'assistant-wide': assistantExpanded && assistantTab === 'chat',
        'right-notes-closed': !rightNotesOpen
      }"
    >
      <aside class="reader-assistant" :class="{ collapsed: assistantCollapsed, expanded: assistantExpanded && assistantTab === 'chat' }">
        <div class="assistant-tabs">
          <button :class="{ active: assistantTab === 'chat' }" @click="assistantTab = 'chat'">文献综述</button>
          <button :class="{ active: assistantTab === 'outline' }" @click="assistantTab = 'outline'">目录</button>
          <button :class="{ active: assistantTab === 'figures' }" @click="assistantTab = 'figures'">图表</button>
          <button :class="{ active: mindMapModal.open }" @click="openMindMapModal">思维导图</button>
          <button
            v-if="assistantTab === 'chat' && !assistantCollapsed"
            class="expand-button icon-button"
            :title="assistantExpanded ? '收回分析内容' : '向右展开分析内容'"
            @click="assistantExpanded = !assistantExpanded"
          >
            <svg v-if="assistantExpanded" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
            <svg v-else viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </button>
          <button class="collapse-button icon-button" @click="toggleAssistantCollapse">
            <svg v-if="assistantCollapsed" viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"></polyline></svg>
            <svg v-else viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
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

          <div v-else-if="assistantTab === 'figures'" class="assistant-scroll">
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
          ref="readingColumn"
          class="reading-column"
          :style="{ '--reader-scale': contentScale }"
        >
          <div v-if="loadingPdf" class="reader-state reader-loading-state">
            <div class="reader-state-mark"><span></span></div>
            <h1>正在准备沉浸翻译</h1>
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
              <p class="paper-authors-line">
                <template v-for="part in authorDisplaySegments(activePaper.authors)" :key="part.key">
                  <sup v-if="part.sup" class="author-affiliation-sup">{{ part.text }}</sup>
                  <span v-else-if="part.orcid" class="author-orcid-badge">iD</span>
                  <span v-else :class="{ 'author-name-text': part.name }">{{ part.text }}</span>
                </template>
              </p>
              <div v-if="autoTranslate" class="paper-meta-translation">
                <p v-if="paperMetaTranslation.title || paperMetaTranslation.loading" class="paper-title-translation">
                  {{ paperMetaTranslation.title || "标题翻译中…" }}
                </p>
                <p v-if="paperMetaTranslation.authors || paperMetaTranslation.loadingAuthors" class="paper-author-translation">
                  <template v-if="paperMetaTranslation.loadingAuthors && !paperMetaTranslation.authors">作者翻译中…</template>
                  <template v-else>
                    <template v-for="part in authorDisplaySegments(paperMetaTranslation.authors)" :key="part.key">
                      <sup v-if="part.sup" class="author-affiliation-sup">{{ part.text }}</sup>
                      <span v-else-if="part.orcid" class="author-orcid-badge">iD</span>
                      <span v-else :class="{ 'author-name-text': part.name }">{{ part.text }}</span>
                    </template>
                  </template>
                </p>
              </div>
              <div v-if="hasAbstract && !structuredHasAbstract && !structuredDocumentReady" class="paper-abstract">
                <strong>Abstract</strong>
                <p class="source-paragraph selectable-paragraph" data-block-id="abstract">
                  <template v-for="segment in annotationSegments('abstract', abstractText)" :key="segment.key">
                    <span
                      v-if="segment.annotated"
                      class="annotation-highlight"
                      :class="`mark-${segment.annotation.style || 'highlight'}`"
                      :style="{ '--mark-color': segment.annotation.color || '#fef08a' }"
                      :title="segment.note"
                      @click="editAnnotation(segment.annotation, $event)"
                    >
                      {{ segment.text }}<button type="button" class="annotation-delete" title="删除这条标注" aria-label="删除这条标注" @click.stop="removeAnnotation(segment.annotation.id)">×</button><span v-if="segment.note" class="annotation-inline-note" @click.stop="editAnnotation(segment.annotation, $event)">{{ segment.note }}</span>
                    </span>
                    <template v-else>{{ segment.text }}</template>
                  </template>
                </p>
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
                <h2
                  v-if="block.kind === 'heading'"
                  class="source-heading"
                  :class="{ 'abstract-heading': isAbstractHeadingBlock(block) }"
                >{{ block.text }}</h2>
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
                    <div class="pdf-figure-actions">
                      <button v-if="block.imageUrl" class="pdf-figure-view" @click="viewParsedFigure(block, page.pageNumber)">查看原图</button>
                      <button class="pdf-figure-analyze" @click="analyzeFigure(block)">AI 分析</button>
                    </div>
                  </figcaption>
                </figure>
                <div
                  v-else-if="block.kind === 'equation'"
                  class="mineru-equation selectable-paragraph"
                  :data-block-id="block.id"
                  :data-selection-text="equationSelectionText(block, page.pageNumber)"
                >
                  <div v-if="block.imageUrl" class="mineru-equation-image-row">
                    <button
                      type="button"
                      class="mineru-equation-image-button"
                      :aria-label="`查看${block.equationNumber || '公式'}原图`"
                      @click="viewParsedFigure(block, page.pageNumber)"
                    >
                      <img :src="block.imageUrl" :alt="block.text || block.equationNumber || '论文公式'" class="mineru-equation-image" />
                    </button>
                    <span v-if="block.equationNumber" class="mineru-equation-number">{{ block.equationNumber }}</span>
                  </div>
                  <pre v-if="block.text" class="mineru-equation-text">{{ block.text }}</pre>
                </div>
                <div v-else-if="block.kind === 'references'" class="reference-block selectable-paragraph" :data-block-id="block.id">
                  <template v-for="segment in annotationSegments(block.id, block.text)" :key="segment.key">
                    <span
                      v-if="segment.annotated"
                      class="annotation-highlight"
                      :class="`mark-${segment.annotation.style || 'highlight'}`"
                      :style="{ '--mark-color': segment.annotation.color || '#fef08a' }"
                      :title="segment.note"
                      @click="editAnnotation(segment.annotation, $event)"
                    >
                      {{ segment.text }}<button type="button" class="annotation-delete" title="删除这条标注" aria-label="删除这条标注" @click.stop="removeAnnotation(segment.annotation.id)">×</button><span v-if="segment.note" class="annotation-inline-note" @click.stop="editAnnotation(segment.annotation, $event)">{{ segment.note }}</span>
                    </span>
                    <template v-else>{{ segment.text }}</template>
                  </template>
                </div>
                <p v-else class="source-paragraph selectable-paragraph" :data-block-id="block.id">
                  <template v-for="segment in annotationSegments(block.id, block.text)" :key="segment.key">
                    <span
                      v-if="segment.annotated"
                      class="annotation-highlight"
                      :class="`mark-${segment.annotation.style || 'highlight'}`"
                      :style="{ '--mark-color': segment.annotation.color || '#fef08a' }"
                      :title="segment.note"
                      @click="editAnnotation(segment.annotation, $event)"
                    >
                      <template v-for="fragment in inlineCitationSegments(segment.text)" :key="fragment.key">
                        <sup v-if="fragment.citation" class="paper-citation-sup">{{ fragment.text }}</sup>
                        <template v-else>{{ fragment.text }}</template>
                      </template><button type="button" class="annotation-delete" title="删除这条标注" aria-label="删除这条标注" @click.stop="removeAnnotation(segment.annotation.id)">×</button><span v-if="segment.note" class="annotation-inline-note" @click.stop="editAnnotation(segment.annotation, $event)">{{ segment.note }}</span>
                    </span>
                    <template v-else>
                      <template v-for="fragment in inlineCitationSegments(segment.text)" :key="fragment.key">
                        <sup v-if="fragment.citation" class="paper-citation-sup">{{ fragment.text }}</sup>
                        <template v-else>{{ fragment.text }}</template>
                      </template>
                    </template>
                  </template>
                </p>
                <div
                  v-if="autoTranslate && !['figure', 'table', 'equation', 'references', 'abstract'].includes(block.kind) && !isAbstractHeadingBlock(block)"
                  class="translation-unit"
                  :class="{ 'author-translation-unit': isLikelyAuthorBlock(block) }"
                >
                  <p
                    class="translated-paragraph selectable-paragraph"
                    :data-block-id="block.id"
                    :class="{ pending: block.translating || !block.translation, error: block.translationError, 'paper-author-translation': isLikelyAuthorBlock(block) }"
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
              </template>
            </section>
          </article>

          <canvas
            ref="drawingCanvas"
            class="reader-drawing-layer"
            :class="{ active: drawingModeActive }"
            :style="drawingCanvasStyle"
            @pointerdown="startInkStroke"
            @pointermove="moveInkStroke"
            @pointerup="finishInkStroke"
            @pointercancel="cancelInkStroke"
            @pointerleave="finishInkStroke"
            @wheel.passive="handleCanvasWheel"
          ></canvas>

          <!-- WPS 1:1 绿折线引用批注层 (参照图 2) -->
          <div v-if="noteAnnotations.length" class="wps-comments-container">
            <svg class="wps-leader-lines-svg">
              <g v-for="anno in noteAnnotations" :key="`wps-group-${anno.id}`">
                <line
                  :x1="anno.x1 || 10"
                  :y1="(anno.y1 || 40) - 8"
                  :x2="anno.x1 || 10"
                  :y2="(anno.y1 || 40) + 10"
                  stroke="#16a34a"
                  stroke-width="2"
                />
                <path
                  :d="`M ${anno.x1 || 10} ${anno.y1 || 40} L ${(anno.x1 || 10) + 50} ${anno.y1 || 40} L ${(anno.x2 || 220) - 10} ${anno.y2 || (anno.y1 || 40) + 20} L ${anno.x2 || 220} ${anno.y2 || (anno.y1 || 40) + 20}`"
                  fill="none"
                  stroke="#16a34a"
                  stroke-width="1.8"
                />
              </g>
            </svg>

            <div
              v-for="anno in noteAnnotations"
              :key="anno.id"
              class="wps-comment-card"
              :style="{ top: `${anno.top || 40}px` }"
              @click="selectedAnnotationId = anno.id"
            >
              <header class="wps-comment-head">
                <div class="wps-avatar-box">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
                </div>
                <div class="wps-comment-meta">
                  <time>{{ anno.createdAt || '2026-07-20 23:20' }}</time>
                  <button class="wps-del-btn" title="删除批注" @click.stop="removeAnnotation(anno.id)">×</button>
                </div>
              </header>
              <div class="wps-comment-body">
                <p v-if="anno.preview" class="wps-target-quote">“{{ anno.preview }}”</p>
                <input
                  v-model="anno.note"
                  class="wps-comment-input"
                  placeholder="编辑批注内容…"
                  @blur="persistAnnotations"
                  @keyup.enter="persistAnnotations"
                />
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 右侧层级文献笔记栏 (自适应 Grid 侧边栏) -->
      <HierarchicalNotesSidebar
        :paper-id="paperId"
        :paper-title="activePaper?.title || ''"
        :paper-note="activePaper?.note || ''"
        :annotations="annotations"
        :is-collapsed="!rightNotesOpen"
        @toggle-collapse="toggleRightNotes"
        @jump-to-page="scrollToPage"
        @jump-to-annotation="handleJumpToAnnotation"
        @show-toast="showReaderToast"
        @sync-note="syncHierarchicalNoteToLibrary"
      />
    </div>

    <div v-if="pinnedScreenshots.length" class="pinned-screenshot-dock" aria-label="固定截图">
      <article
        v-for="shot in pinnedScreenshots"
        :key="shot.id"
        class="pinned-screenshot-card"
        :style="{ left: `${shot.x}px`, top: `${shot.y}px`, width: `${shot.width}px` }"
      >
        <header>
          <strong>截图</strong>
          <button title="关闭截图" @click="removePinnedScreenshot(shot.id)">×</button>
        </header>
        <img :src="shot.dataUrl" alt="固定截图" />
      </article>
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
      :class="[`is-${selectionTranslator.placement}`, { expanded: selectionTranslator.result || selectionTranslator.error || selectionTranslator.loading || selectionTranslator.annotating }]"
      :style="{ left: `${selectionTranslator.x}px`, top: `${selectionTranslator.y}px` }"
      @click.stop
    >
      <div class="selection-command-bar">
        <button class="cmd-btn cmd-ai" :disabled="selectionTranslator.loading" @click="explainSelection">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>
          <span>AI 解读</span>
        </button>
        <button class="cmd-btn cmd-translate" :disabled="selectionTranslator.loading" @click="translateSelection">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="m5 8 6 6M4 14l6-6 2-3M2 5h12M7 2v3M22 22l-5-10-5 10M14 18h6"/></svg>
          <span>划词翻译</span>
        </button>
        <button class="cmd-btn cmd-chat" @click="addSelectionToChat">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>
          <span>加入对话</span>
        </button>
        <button class="cmd-btn cmd-note" @click="openAnnotationEditor">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
          <span>新建批注</span>
        </button>
        <div class="selection-mark-dots" aria-label="设置选中文字颜色">
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
        <button aria-label="关闭选中内容工具" @click="closeSelectionTranslator">×</button>
      </div>
      <div class="selection-provider-panel">
        <span>翻译引擎</span>
        <select v-model="abstractProvider" @change="handleProviderChange">
          <option v-for="provider in translationProviders" :key="provider.id" :value="provider.id" :disabled="provider.configured === false">
            {{ provider.label }}{{ provider.configured === false ? "（需配置）" : "" }}
          </option>
        </select>
      </div>
      <div v-if="selectionTranslator.loading || selectionTranslator.result || selectionTranslator.error" class="selection-result" :class="{ pending: selectionTranslator.loading, error: selectionTranslator.error, 'is-ai-mode': selectionTranslator.resultTitle === 'AI 解读' }">
        <span v-if="selectionTranslator.loading" class="selection-spinner"></span>
        <div>
          <header>
            <strong>{{ selectionTranslator.resultTitle || "处理结果" }}</strong>
            <small>{{ selectionTranslator.source.length }} 字符</small>
          </header>
          <p v-if="selectionTranslator.wasCompacted && !selectionTranslator.loading && !selectionTranslator.error" class="selection-compact-note">选区较长，已结合开头、结尾和所在段落进行摘要式解读。</p>
          <p>{{ selectionTranslator.loading ? selectionTranslator.loadingText : selectionTranslator.error || selectionTranslator.result }}</p>
        </div>
      </div>
      <div v-if="selectionTranslator.annotating" class="selection-annotation-editor">
        <textarea v-model="selectionTranslator.annotationDraft" rows="3" placeholder="写下对这段内容的理解、疑问或提醒…"></textarea>
        <div>
          <button @click="selectionTranslator.annotating = false">取消</button>
          <button :disabled="!selectionTranslator.annotationDraft.trim()" @click="saveAnnotation">保存批注</button>
        </div>
      </div>
    </section>

    <!-- Apple Intelligence Style Quantum Floating AI Assistant Launcher -->
    <button
      class="apple-ai-launcher"
      :class="{ expanded: paperChat.open, 'right-notes-open': rightNotesOpen, 'right-notes-closed': !rightNotesOpen }"
      :title="paperChat.open ? '收起 AI 研读助手' : '开启 AI 研读助手'"
      @click="togglePaperChatPanel"
    >
      <div class="ai-sparkle-halo"></div>
      <span class="ai-sparkle-icon">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 2L14.5 9.5L22 12L14.5 14.5L12 22L9.5 14.5L2 12L9.5 9.5L12 2Z" fill="url(#ai-grad-btn)"/><defs><linearGradient id="ai-grad-btn" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse"><stop stop-color="#EDE9FE"/><stop offset="0.48" stop-color="#C4B5FD"/><stop offset="1" stop-color="#8B5CF6"/></linearGradient></defs></svg>
      </span>
      <span class="ai-label-text">{{ paperChat.open ? '收起助手' : 'AI 研读助手' }}</span>
    </button>

    <Transition name="paper-chat">
      <section
        v-if="paperChat.open"
        class="paper-chat-panel futuristic-void-panel"
        :class="{ 'right-notes-open': rightNotesOpen, 'right-notes-closed': !rightNotesOpen, dragging: paperChatWindow.dragging, positioned: paperChatWindow.x !== null }"
        :style="paperChatPanelStyle"
      >
        <header class="void-chat-header" @pointerdown="startPaperChatDrag">
          <div class="quantum-ai-avatar">
            <svg class="quantum-orb-svg" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="9" stroke="url(#quantum-grad)" stroke-width="1.5" stroke-dasharray="4 2"/>
              <path d="M12 2L14.5 9.5L22 12L14.5 14.5L12 22L9.5 14.5L2 12L9.5 9.5L12 2Z" fill="url(#quantum-grad)"/>
              <defs>
                <linearGradient id="quantum-grad" x1="2" y1="2" x2="22" y2="22">
                  <stop stop-color="#818CF8"/>
                  <stop offset="0.5" stop-color="#C084FC"/>
                  <stop offset="1" stop-color="#38BDF8"/>
                </linearGradient>
              </defs>
            </svg>
            <span class="pulse-ring"></span>
          </div>
          <div class="void-title-box">
            <div class="void-main-title">
              <strong>PAPER INTELLIGENCE</strong>
              <span class="tech-tag-pill">NEURAL v5.4</span>
            </div>
            <span class="void-sub-title">{{ shortPaperTitle(activePaper.title, 32) }}</span>
          </div>
          <button class="void-close-btn" title="关闭助手" @click="closePaperChatPanel">
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </header>

        <div class="paper-chat-messages void-chat-body">
          <article
            v-for="(message, messageIndex) in paperChat.messages"
            :key="message.id"
            :class="['paper-chat-message', message.role, { 'has-figure': message.figure }]"
          >
            <div v-if="message.role === 'assistant'" class="assistant-sparkle-avatar">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M12 2L14.5 9.5L22 12L14.5 14.5L12 22L9.5 14.5L2 12L9.5 9.5L12 2Z" fill="url(#ast-spark)"/><defs><linearGradient id="ast-spark" x1="2" y1="2" x2="22" y2="22"><stop stop-color="#818CF8"/><stop offset="1" stop-color="#38BDF8"/></linearGradient></defs></svg>
            </div>

            <div class="message-content-wrapper">
              <div v-if="message.figure" class="sci-fi-figure-chip">
                <div class="figure-scanner-badge">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 12h20M12 2v20M5 5l14 14"/></svg>
                  <span>VISION ANALYSIS // 图像扫描解析</span>
                </div>
                <div class="figure-thumb-container">
                  <img v-if="message.figure.imageUrl" :src="message.figure.imageUrl" :alt="message.figure.caption" class="figure-thumb-img" />
                  <div class="figure-caption-tag">{{ message.figure.caption }}</div>
                </div>
              </div>

              <div v-if="message.role === 'assistant'" class="ai-meta-banner">
                <span class="status-dot"></span>
                <span>ACADEMIC NEURAL SYNAPSE</span>
              </div>

              <div v-if="message.role === 'assistant'" class="message-text markdown-rendered" v-html="renderMarkdown(message.content)"></div>
              <p v-else class="message-text">{{ message.content }}</p>
              <div v-if="message.role === 'assistant'" class="paper-chat-message-actions">
                <button type="button" title="重新生成" @click="retryPaperChatMessage(messageIndex)">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-2.64-6.36"/><path d="M21 3v6h-6"/></svg>
                </button>
                <button type="button" title="复制回答" @click="copyPaperChatMessage(message)">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                </button>
              </div>
            </div>
          </article>

          <article v-if="paperChat.loading" class="paper-chat-message assistant thinking">
            <div class="assistant-sparkle-avatar spinning">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M12 2L14.5 9.5L22 12L14.5 14.5L12 22L9.5 14.5L2 12L9.5 9.5L12 2Z" fill="#818CF8"/></svg>
            </div>
            <div class="message-content-wrapper">
              <div class="ai-meta-banner">
                <span class="status-dot pulse"></span>
                <span>NEURAL COMPUTING // 正在解析论文神经元...</span>
              </div>
              <p class="paper-chat-thinking"><i></i><i></i><i></i></p>
            </div>
          </article>
        </div>

        <div class="void-quick-prompts">
          <button type="button" @click="insertQuickPrompt('总结此论文的核心创新点与贡献')">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>
            <span>核心创新</span>
          </button>
          <button type="button" @click="insertQuickPrompt('详细拆解论文使用的研究方法与实验设计')">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>
            <span>实验方法</span>
          </button>
          <button type="button" @click="insertQuickPrompt('指出论文可能存在的局限性与未来方向')">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m12 8 4 4-4 4M8 12h8"/></svg>
            <span>局限突破</span>
          </button>
        </div>

        <form class="void-input-form" @submit.prevent="askPaperChat">
          <div class="cyber-input-wrapper">
            <textarea
              v-model="paperChat.question"
              rows="1"
              :disabled="paperChat.loading"
              placeholder="询问研究方法、数据指标、图表解析或核心结论…"
              @keydown.enter.exact.prevent="askPaperChat"
            ></textarea>
          </div>
          <button class="cyber-send-btn" :disabled="paperChat.loading || !paperChat.question.trim()" aria-label="发送问题">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"></line><polyline points="5 12 12 5 19 12"></polyline></svg>
          </button>
        </form>
        <div class="void-footer-note">QUANTUM NEURAL ENGINE · GROUNDED ON ACADEMIC CONTEXT</div>
      </section>
    </Transition>

    <Transition name="reader-toast">
      <div v-if="readerToast" class="reader-toast">{{ readerToast }}</div>
    </Transition>

    <div v-if="readerTour.open" class="reader-tour-layer">
      <div class="reader-tour-shade" @click="finishTour"></div>
      <div
        v-if="readerTour.rect"
        class="reader-tour-focus"
        :style="{
          left: `${readerTour.rect.left - 6}px`,
          top: `${readerTour.rect.top - 6}px`,
          width: `${readerTour.rect.width + 12}px`,
          height: `${readerTour.rect.height + 12}px`,
        }"
      ></div>
      <section class="reader-tour-card">
        <header class="tour-card-head">
          <span class="tour-step-badge">第 {{ readerTour.index + 1 }} 步 / {{ tourSteps.length }}</span>
          <button class="tour-skip-btn" title="跳过新手指引" @click="finishTour">跳过指引 ×</button>
        </header>
        <h2>{{ tourSteps[readerTour.index].title }}</h2>
        <p>{{ tourSteps[readerTour.index].description }}</p>
        <div class="tour-card-actions">
          <button v-if="readerTour.index > 0" class="tour-btn-prev" @click="previousTourStep">上一步</button>
          <button class="tour-btn-next" @click="nextTourStep">
            {{ readerTour.index === tourSteps.length - 1 ? "完成指引" : "下一步" }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="figureViewer.open" class="pdf-figure-overlay" @click="closeFigureViewer">
      <div class="pdf-figure-modal" @click.stop>
        <header>
          <strong>{{ figureViewer.caption }}</strong>
          <div class="pdf-figure-modal-actions">
            <button class="pdf-figure-analyze-modal" @click="analyzeFigure(figureViewer.block); closeFigureViewer()">AI 分析</button>
            <button @click="closeFigureViewer">关闭</button>
          </div>
        </header>
        <div class="pdf-figure-modal-stage">
          <img
            v-if="figureViewer.imageUrl"
            :src="figureViewer.imageUrl"
            :alt="figureViewer.caption"
            :style="{ transform: `rotate(${figureViewer.rotation}deg)` }"
            @load="autoRotateFigure"
          />
          <canvas
            v-else
            ref="figureCanvasRef"
            :style="{ transform: `rotate(${figureViewer.rotation}deg)` }"
          ></canvas>
        </div>
      </div>
    </div>

    <div v-if="mindMapModal.open" class="mind-map-overlay" @click="closeMindMapModal">
      <section class="mind-map-modal" @click.stop>
        <header>
          <div>
            <strong>论文思维导图</strong>
            <span>{{ shortPaperTitle(activePaper.title, 42) }}</span>
          </div>
          <div class="mind-map-modal-actions">
            <button class="btn-export" :disabled="mindMapState.loading" @click="exportMindMapSvg">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px; display: inline-block; vertical-align: middle;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              导出 SVG
            </button>
            <button class="btn-export" :disabled="mindMapState.loading" @click="exportMindMapPng">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 4px; display: inline-block; vertical-align: middle;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              导出 PNG
            </button>
            <button class="btn-close" @click="closeMindMapModal">关闭</button>
          </div>
        </header>
        <div class="mind-map-canvas">
          <div class="mind-map-zoom-controls" aria-label="思维导图缩放">
            <button title="放大" @click="zoomMindMap(1.25)">＋</button>
            <button title="缩小" @click="zoomMindMap(0.8)">－</button>
            <button title="适应窗口" @click="fitMindMap()">适应</button>
          </div>
          <svg ref="mindMapSvg" class="mind-map-svg" aria-label="论文思维导图"></svg>
          <div v-if="mindMapState.loading" class="mind-map-status">
            <span class="mind-map-spinner"></span>
            正在生成思维导图
          </div>
          <div v-else-if="mindMapState.error" class="mind-map-status error">{{ mindMapState.error }}</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from "vue";
import { paperpilotApi } from "../services/paperpilotApi";
import { useLibraryStore } from "../stores/library";
import ReaderReportPanel from "../components/ReaderReportPanel.vue";
import ReaderMultiTabBar from "../components/ReaderMultiTabBar.vue";
import HierarchicalNotesSidebar from "../components/HierarchicalNotesSidebar.vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { rememberLastReading } from "../utils/readingMemory";
import MarkdownIt from "markdown-it";
import html2canvas from "html2canvas";

const rightNotesOpen = ref(true);
function toggleRightNotes() {
  rightNotesOpen.value = !rightNotesOpen.value;
}

const markdownRenderer = new MarkdownIt({ html: false, linkify: true, breaks: true });
function renderMarkdown(text) {
  if (!text) return "";
  return markdownRenderer.render(String(text).trim());
}

const libraryStore = useLibraryStore();
const authStore = useAuthStore();
const route = useRoute();
const pages = reactive([]);
const pageCanvasElements = new Map();
const readingScroll = ref(null);
const readingColumn = ref(null);
const drawingCanvas = ref(null);
const loadingPdf = ref(true);
const loadError = ref("");
const parsingMessage = ref("正在进行论文版面解析（完整段落、图表、表格）");
const parsingProgress = ref(8);
const totalPages = ref(0);
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

const loadedPages = ref(0);
const currentPage = ref(1);
const readingProgress = ref(0);
const autoTranslate = ref(true);
const assistantCollapsed = ref(false);
const assistantExpanded = ref(false);
const assistantTab = ref("chat");
const showZoomPresets = ref(false);
const zoomPresetList = [
  { label: "50%", scale: 0.5 },
  { label: "75%", scale: 0.75 },
  { label: "100% (标准)", scale: 1.0 },
  { label: "125%", scale: 1.25 },
  { label: "150%", scale: 1.5 },
  { label: "175%", scale: 1.75 },
  { label: "200%", scale: 2.0 },
];

const showStylePopover = ref(false);
const markColorPicker = ref(null);

const currentToolLabel = computed(() => {
  if (isDrawingPenActive.value) return "自由手绘画笔";
  return {
    select: "划词选择",
    highlight: "文本高亮",
    fontColor: "字体颜色",
    underline: "文本下划线",
    strike: "文本删除线",
    wavy: "文本波浪线",
    pen: "自由手绘画笔"
  }[activeAnnotateTool.value] || "标注线形";
});

function activateNativeMarkColorPicker() {
  markColorPicker.value?.click();
}

function handleGlobalClick() {
  showZoomPresets.value = false;
  showStylePopover.value = false;
}

const contentScale = ref(1);
const activeMouseMode = ref("select");
const activeToolTab = ref("annotate");
const activeAnnotateTool = ref("highlight");
const selectedColor = ref("#eab308");
const isDrawingPenActive = ref(false);
const drawingToolPanelOpen = ref(false);
const textMarkPanelOpen = ref("");
const brushOpacity = ref(100);
const brushWidth = ref(3);
const brushColorPicker = ref(null);
const drawingStrokes = reactive([]);
const pinnedScreenshots = reactive([]);
const drawingCanvasBox = reactive({ left: 0, top: 0, width: 0, height: 0 });
const drawingCanvasStyle = computed(() => ({
  left: "0px",
  top: "0px",
  width: `${drawingCanvasBox.width}px`,
  height: `${drawingCanvasBox.height}px`,
}));
const lineTools = ["underline", "strike", "wavy"];
const lineToolActive = computed(() => lineTools.includes(activeAnnotateTool.value) && activeMouseMode.value === "draw-line");
const drawingModeActive = computed(() => isDrawingPenActive.value || lineToolActive.value);
let activeInkStroke = null;
let drawingFrame = 0;

function activateBrushTool(closePanel = true) {
  isDrawingPenActive.value = true;
  activeMouseMode.value = "draw";
  activeAnnotateTool.value = "pen";
  closeSelectionTranslator();
  window.getSelection()?.removeAllRanges();
  if (closePanel) drawingToolPanelOpen.value = false;
  nextTick(resizeDrawingCanvas);
}

function setMoveTool() {
  isDrawingPenActive.value = false;
  activeMouseMode.value = "select";
  activeAnnotateTool.value = "select";
  drawingToolPanelOpen.value = false;
  textMarkPanelOpen.value = "";
  showReaderToast("已切换为移动/选择模式");
}

function setLineTool(tool) {
  isDrawingPenActive.value = false;
  drawingToolPanelOpen.value = false;
  textMarkPanelOpen.value = "";
  activeMouseMode.value = "draw-line";
  activeAnnotateTool.value = tool;
  closeSelectionTranslator();
  window.getSelection()?.removeAllRanges();
  nextTick(resizeDrawingCanvas);
  showReaderToast(`已开启${textMarkLabel(tool)}，在正文文字行上拖动即可划线`);
}

function setTextMarkTool(tool) {
  isDrawingPenActive.value = false;
  drawingToolPanelOpen.value = false;
  textMarkPanelOpen.value = tool;
  activeMouseMode.value = "select";
  activeAnnotateTool.value = tool;
  showReaderToast(`已选择${textMarkLabel(tool)}工具`);
}

function toggleTextMarkPanel(tool) {
  isDrawingPenActive.value = false;
  drawingToolPanelOpen.value = false;
  activeMouseMode.value = "select";
  activeAnnotateTool.value = tool;
  textMarkPanelOpen.value = textMarkPanelOpen.value === tool ? "" : tool;
}

function textMarkLabel(tool = activeAnnotateTool.value) {
  return {
    fontColor: "字体颜色",
    underline: "下划线",
    strike: "删除线",
    wavy: "波浪线",
  }[tool] || "字体颜色";
}

function toggleBrushPanel() {
  textMarkPanelOpen.value = "";
  drawingToolPanelOpen.value = !drawingToolPanelOpen.value;
  activateBrushTool(false);
}

function selectBrushColor(color) {
  selectedColor.value = color;
  if (!drawingModeActive.value) applyTextColor(color);
  showStylePopover.value = false;
}

function handleDockColor(color) {
  selectedColor.value = color;
  if (drawingModeActive.value) {
    showReaderToast(`已切换线条颜色：${textMarkLabel(activeAnnotateTool.value)}`);
    return;
  }
  applyTextColor(color);
}

function handleCanvasWheel(e) {
  if (readingScroll.value) {
    readingScroll.value.scrollTop += e.deltaY;
  }
}

function selectMarkColor(color) {
  selectedColor.value = color;
  applyToolbarMark(color);
}

function selectBrushPreset(color) {
  selectedColor.value = color;
  activateBrushTool(false);
}

function activateNativeColorPicker() {
  brushColorPicker.value?.click?.();
}

function toggleDrawingPen() {
  isDrawingPenActive.value = !isDrawingPenActive.value;
  activeAnnotateTool.value = isDrawingPenActive.value ? "pen" : "highlight";
  if (isDrawingPenActive.value) {
    closeSelectionTranslator();
    window.getSelection()?.removeAllRanges();
    nextTick(resizeDrawingCanvas);
    showReaderToast("已开启自由画笔涂鸦模式，可在页面上随意绘图图画");
  } else {
    showReaderToast("已退出画笔图画模式");
  }
}

function undoLastAnnotation() {
  if (drawingStrokes.length) {
    drawingStrokes.pop();
    persistDrawingStrokes();
    redrawDrawingCanvas();
    showReaderToast("已撤销上一条手绘痕迹");
  } else if (annotations.length) {
    annotations.pop();
    persistAnnotations();
    showReaderToast("已撤销上一条文字批注");
  } else if (clearedAnnotationSnapshot.value.length) {
    annotations.push(...clearedAnnotationSnapshot.value);
    clearedAnnotationSnapshot.value = [];
    persistAnnotations();
    showReaderToast("已恢复清除的图画痕迹");
  } else {
    showReaderToast("暂无图画痕迹可撤销");
  }
}

function drawingStorageKey() {
  return `papersolver-reader-ink:${workspaceId.value}`;
}

function loadDrawingStrokes() {
  drawingStrokes.splice(0);
  try {
    const stored = JSON.parse(localStorage.getItem(drawingStorageKey()) || "[]");
    if (Array.isArray(stored)) drawingStrokes.push(...stored);
  } catch {
    // 忽略损坏的本地手绘缓存。
  }
  nextTick(resizeDrawingCanvas);
}

function persistDrawingStrokes() {
  localStorage.setItem(drawingStorageKey(), JSON.stringify(drawingStrokes));
}

function resizeDrawingCanvas() {
  const canvas = drawingCanvas.value;
  const column = readingColumn.value;
  if (!canvas || !column) return;
  drawingCanvasBox.left = 0;
  drawingCanvasBox.top = 0;
  drawingCanvasBox.width = Math.max(1, Math.ceil(column.scrollWidth || column.clientWidth));
  drawingCanvasBox.height = Math.max(1, Math.ceil(column.scrollHeight || column.clientHeight));
  const dpr = Math.min(1.5, window.devicePixelRatio || 1);
  const width = drawingCanvasBox.width;
  const height = drawingCanvasBox.height;
  if (canvas.width !== Math.ceil(width * dpr) || canvas.height !== Math.ceil(height * dpr)) {
    canvas.width = Math.ceil(width * dpr);
    canvas.height = Math.ceil(height * dpr);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
  }
  redrawDrawingCanvas();
}

function drawInkStroke(context, stroke) {
  if (Array.isArray(stroke?.segments) && stroke.segments.length) {
    drawLineStroke(context, stroke);
    return;
  }
  const points = Array.isArray(stroke?.points) ? stroke.points : [];
  if (points.length < 1) return;
  const first = viewportInkPoint(points[0]);
  context.save();
  context.strokeStyle = stroke.color || selectedColor.value;
  context.lineWidth = stroke.tool === "highlight" ? Math.max(8, (stroke.width || 3) * 2.8) : stroke.width || 3;
  context.lineCap = "round";
  context.lineJoin = "round";
  context.globalAlpha = stroke.tool === "highlight"
    ? Math.min(0.45, Math.max(0.12, stroke.opacity ?? 0.35))
    : Math.min(1, Math.max(0.1, stroke.opacity ?? 0.95));
  if (stroke.tool === "wavy") {
    drawWavyStroke(context, points.map(viewportInkPoint), Math.max(5, (stroke.width || 2) * 2.2));
  } else {
    context.beginPath();
    context.moveTo(first.x, first.y);
    points.slice(1).forEach(point => {
      const next = viewportInkPoint(point);
      context.lineTo(next.x, next.y);
    });
    if (points.length === 1) context.lineTo(first.x + 0.1, first.y + 0.1);
    context.stroke();
  }
  context.restore();
}

function drawLineStroke(context, stroke) {
  context.save();
  context.strokeStyle = stroke.color || selectedColor.value;
  context.lineWidth = Math.max(1, stroke.width || 2);
  context.lineCap = "round";
  context.lineJoin = "round";
  context.globalAlpha = Math.min(1, Math.max(0.1, stroke.opacity ?? 0.95));
  stroke.segments.forEach(segment => {
    const startX = Math.min(segment.x1, segment.x2);
    const endX = Math.max(segment.x1, segment.x2);
    if (Math.abs(endX - startX) < 2) return;
    if (stroke.tool === "wavy") {
      drawWavyLineSegment(context, startX, endX, segment.y, Math.max(3, (stroke.width || 2) * 1.45));
    } else {
      context.beginPath();
      context.moveTo(startX, segment.y);
      context.lineTo(endX, segment.y);
      context.stroke();
    }
  });
  context.restore();
}

function drawWavyLineSegment(context, startX, endX, y, amplitude = 3) {
  const wavelength = 14;
  context.beginPath();
  context.moveTo(startX, y);
  const distance = Math.max(1, endX - startX);
  const steps = Math.max(8, Math.ceil(distance / 4));
  for (let index = 1; index <= steps; index += 1) {
    const t = index / steps;
    const x = startX + distance * t;
    const wave = Math.sin((distance * t / wavelength) * Math.PI * 2) * amplitude;
    context.lineTo(x, y + wave);
  }
  context.stroke();
}

function drawWavyStroke(context, points, amplitude = 6) {
  if (!points.length) return;
  const wavelength = 12;
  context.beginPath();
  points.forEach((point, pointIndex) => {
    if (pointIndex === 0) {
      context.moveTo(point.x, point.y);
      return;
    }
    const previous = points[pointIndex - 1];
    const dx = point.x - previous.x;
    const dy = point.y - previous.y;
    const distance = Math.max(1, Math.hypot(dx, dy));
    const steps = Math.max(2, Math.ceil(distance / 5));
    const nx = -dy / distance;
    const ny = dx / distance;
    for (let index = 1; index <= steps; index += 1) {
      const t = index / steps;
      const wave = Math.sin(((pointIndex + t) * distance / wavelength) * Math.PI * 2) * amplitude * 0.45;
      context.lineTo(previous.x + dx * t + nx * wave, previous.y + dy * t + ny * wave);
    }
  });
  context.stroke();
}

function redrawDrawingCanvas() {
  const canvas = drawingCanvas.value;
  if (!canvas) return;
  const dpr = window.devicePixelRatio || 1;
  const context = canvas.getContext("2d");
  const width = Number.parseFloat(canvas.style.width) || canvas.width / dpr;
  const height = Number.parseFloat(canvas.style.height) || canvas.height / dpr;
  context.setTransform(dpr, 0, 0, dpr, 0, 0);
  context.clearRect(0, 0, width, height);
  drawingStrokes.forEach(stroke => drawInkStroke(context, stroke));
  if (activeInkStroke) drawInkStroke(context, activeInkStroke);
}

function drawInkSegment(from, to, stroke) {
  const canvas = drawingCanvas.value;
  if (!canvas || !from || !to) return;
  if (Array.isArray(stroke?.segments)) {
    redrawDrawingCanvas();
    return;
  }
  const dpr = Math.min(1.5, window.devicePixelRatio || 1);
  const context = canvas.getContext("2d");
  const start = viewportInkPoint(from);
  const end = viewportInkPoint(to);
  context.setTransform(dpr, 0, 0, dpr, 0, 0);
  context.save();
  context.strokeStyle = stroke.color || selectedColor.value;
  context.lineWidth = stroke.tool === "highlight" ? Math.max(8, (stroke.width || 3) * 2.8) : stroke.width || 3;
  context.lineCap = "round";
  context.lineJoin = "round";
  context.globalAlpha = stroke.tool === "highlight"
    ? Math.min(0.45, Math.max(0.12, stroke.opacity ?? 0.35))
    : Math.min(1, Math.max(0.1, stroke.opacity ?? 0.95));
  if (stroke.tool === "wavy") {
    drawWavyStroke(context, [start, end], Math.max(5, (stroke.width || 2) * 2.2));
  } else {
    context.beginPath();
    context.moveTo(start.x, start.y);
    context.lineTo(end.x, end.y);
    context.stroke();
  }
  context.restore();
}

function inkPointFromEvent(event) {
  const column = readingColumn.value;
  const rect = column?.getBoundingClientRect();
  if (!rect) return { x: 0, y: 0 };
  return {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  };
}

function linePointFromEvent(event) {
  const columnRect = readingColumn.value?.getBoundingClientRect();
  if (!columnRect) return null;
  const paragraph = document.elementsFromPoint(event.clientX, event.clientY)
    .find(element => element?.classList?.contains("selectable-paragraph"));
  if (!paragraph) return null;
  const paragraphRect = paragraph.getBoundingClientRect();
  const style = window.getComputedStyle(paragraph);
  const fontSize = Number.parseFloat(style.fontSize) || 16;
  const lineHeight = Number.parseFloat(style.lineHeight) || fontSize * 1.75;
  const relativeY = Math.max(0, Math.min(paragraphRect.height, event.clientY - paragraphRect.top));
  const lineIndex = Math.max(0, Math.floor(relativeY / lineHeight));
  const lineTop = paragraphRect.top + lineIndex * lineHeight;
  const lineMid = lineTop + lineHeight * 0.52;
  const lineBase = lineTop + lineHeight * 0.88;
  const y = activeAnnotateTool.value === "strike" ? lineMid : lineBase;
  const blockId = paragraph.dataset?.blockId || "paragraph";
  return {
    x: Math.max(paragraphRect.left + 2, Math.min(event.clientX, paragraphRect.right - 2)) - columnRect.left,
    y: y - columnRect.top,
    lineKey: `${blockId}:${lineIndex}:${Math.round((y - columnRect.top) * 2) / 2}`,
  };
}

function viewportInkPoint(point) {
  return point;
}

function scheduleDrawingRedraw() {
  if (drawingFrame) return;
  drawingFrame = window.requestAnimationFrame(() => {
    drawingFrame = 0;
    resizeDrawingCanvas();
  });
}

function startInkStroke(event) {
  if (!drawingModeActive.value) return;
  event.preventDefault();
  event.currentTarget?.setPointerCapture?.(event.pointerId);
  const tool = isDrawingPenActive.value ? "pen" : activeAnnotateTool.value;
  const firstPoint = tool === "pen" ? inkPointFromEvent(event) : linePointFromEvent(event);
  if (!firstPoint) {
    showReaderToast("请在正文文字行上拖动划线");
    return;
  }
  activeInkStroke = {
    id: `ink-${Date.now()}`,
    color: selectedColor.value,
    width: tool === "pen" ? Math.max(1, Number(brushWidth.value || 3)) : Math.max(1, Number(brushWidth.value || 2)),
    opacity: Math.min(1, Math.max(0.1, Number(brushOpacity.value || 100) / 100)),
    tool,
    points: [firstPoint],
  };
  if (tool !== "pen") {
    activeInkStroke.segments = [{
      lineKey: firstPoint.lineKey,
      x1: firstPoint.x,
      x2: firstPoint.x,
      y: firstPoint.y,
    }];
    redrawDrawingCanvas();
  } else {
    drawInkSegment(activeInkStroke.points[0], { x: activeInkStroke.points[0].x + 0.1, y: activeInkStroke.points[0].y + 0.1 }, activeInkStroke);
  }
}

function moveInkStroke(event) {
  if (!drawingModeActive.value || !activeInkStroke) return;
  event.preventDefault();
  const previous = activeInkStroke.points[activeInkStroke.points.length - 1];
  const next = activeInkStroke.tool === "pen" ? inkPointFromEvent(event) : linePointFromEvent(event);
  if (!next) return;
  if (Math.hypot(next.x - previous.x, next.y - previous.y) < 1.5) return;
  activeInkStroke.points.push(next);
  if (activeInkStroke.tool !== "pen") {
    extendLineStroke(activeInkStroke, next);
    redrawDrawingCanvas();
  } else {
    drawInkSegment(previous, next, activeInkStroke);
  }
}

function extendLineStroke(stroke, point) {
  const segments = stroke.segments || [];
  let segment = segments[segments.length - 1];
  if (!segment || segment.lineKey !== point.lineKey) {
    segment = {
      lineKey: point.lineKey,
      x1: point.x,
      x2: point.x,
      y: point.y,
    };
    segments.push(segment);
  } else {
    segment.x1 = Math.min(segment.x1, point.x);
    segment.x2 = Math.max(segment.x2, point.x);
  }
  stroke.segments = segments;
}

function finishInkStroke(event) {
  if (!activeInkStroke) return;
  event?.preventDefault?.();
  if (activeInkStroke.points.length > 1) {
    drawingStrokes.push(activeInkStroke);
    persistDrawingStrokes();
  }
  activeInkStroke = null;
}

function cancelInkStroke() {
  activeInkStroke = null;
  redrawDrawingCanvas();
}

async function pinReadingScreenshot() {
  const target = readingScroll.value;
  if (!target) return;
  showReaderToast("正在截图并固定到屏幕");
  const canvas = await html2canvas(target, {
    backgroundColor: null,
    scale: Math.min(2, window.devicePixelRatio || 1.5),
    useCORS: true,
  });
  const width = Math.min(360, Math.max(260, Math.round(window.innerWidth * 0.24)));
  pinnedScreenshots.push({
    id: `shot-${Date.now()}`,
    dataUrl: canvas.toDataURL("image/png"),
    x: Math.max(16, window.innerWidth - width - 24),
    y: 88 + pinnedScreenshots.length * 24,
    width,
  });
  showReaderToast("截图已固定在屏幕右侧");
}

function removePinnedScreenshot(id) {
  const index = pinnedScreenshots.findIndex(item => item.id === id);
  if (index >= 0) pinnedScreenshots.splice(index, 1);
}

function fitWidth() {
  const containerWidth = window.innerWidth - (assistantCollapsed.value ? 60 : 360) - 220;
  if (containerWidth > 400) {
    const scale = containerWidth / 800;
    contentScale.value = Math.min(1.6, Math.max(0.8, Number(scale.toFixed(2))));
    showReaderToast(`已自适应页宽: ${Math.round(contentScale.value * 100)}%`);
  } else {
    contentScale.value = 1.0;
  }
}

function zoomReaderIn() {
  contentScale.value = Math.min(1.6, Number((contentScale.value + 0.1).toFixed(2)));
  showReaderToast(`正文缩放 ${Math.round(contentScale.value * 100)}%`);
}

function zoomReaderOut() {
  contentScale.value = Math.max(0.8, Number((contentScale.value - 0.1).toFixed(2)));
  showReaderToast(`正文缩放 ${Math.round(contentScale.value * 100)}%`);
}

function showAssistantTab(tab) {
  assistantTab.value = tab;
  assistantCollapsed.value = false;
  if (tab !== "chat") assistantExpanded.value = false;
}

function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen?.().catch(() => {});
    showReaderToast("已开启全屏沉浸阅读模式");
  } else {
    document.exitFullscreen?.().catch(() => {});
    showReaderToast("已退出全屏模式");
  }
}

function triggerSnapshot() {
  showReaderToast("截图分析功能已开启，框选页面后即可提取图表与公式");
}
const abstractTranslation = ref("");
const abstractTranslating = ref(false);
const abstractProvider = ref("google");
const abstractFromPdf = ref("");
const structuredHasAbstract = ref(false);
const structuredDocumentReady = ref(false);
const paperMetaTranslation = reactive({
  title: "",
  authors: "",
  loading: false,
  loadingAuthors: false,
  provider: "",
  paperId: "",
});
const translationProviders = ref([
  { id: "google-web", label: "Google 网页翻译", configured: true },
  { id: "google", label: "谷歌翻译", configured: true },
  { id: "google-api", label: "Google(API)", configured: false },
  { id: "bing", label: "必应翻译", configured: false },
  { id: "cnki", label: "CNKI 翻译", configured: false },
  { id: "deeplx", label: "DeepLX", configured: false },
  { id: "baidu", label: "百度翻译", configured: true },
  { id: "youdao", label: "有道翻译", configured: true },
  { id: "huoshan-web", label: "火山网页翻译", configured: false },
  { id: "tencent-transmart", label: "腾讯 TranSmart", configured: false },
  { id: "haici", label: "海词翻译", configured: false },
  { id: "libretranslate", label: "LibreTranslate", configured: false },
  { id: "mtranserver", label: "MTranServer", configured: false },
  { id: "microsoft", label: "微软翻译", configured: false },
  { id: "tencent", label: "腾讯翻译", configured: false },
  { id: "deepl", label: "DeepL", configured: false }
]);
const selectionReady = ref(false);
const selectedRange = shallowRef(null);
const colorMenu = reactive({ open: false, x: 0, y: 0 });
const selectionTranslator = reactive({
  open: false,
  x: 0,
  y: 0,
  placement: "below",
  source: "",
  sentence: "",
  paragraph: "",
  preview: "",
  result: "",
  resultTitle: "",
  loadingText: "正在处理选区…",
  wasCompacted: false,
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
const noteAnnotations = computed(() => annotations.filter(item => item.type !== "mark"));
function handleProviderChange() {
  const provider = abstractProvider.value;
  abstractTranslation.value = "";
  translatePaperMetadata(true);
  translateAbstract(true);
  pages.forEach(page => {
    if (Array.isArray(page.blocks)) {
      page.blocks.forEach(block => {
        block.translationProvider = provider;
        block.translation = "";
        block.translationError = "";
      });
      translatePage(page);
    }
  });
  showReaderToast(`已切换至 ${providerLabel(provider)}，正在重新翻译全文`);
}

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
const paperChatWindow = reactive({
  x: null,
  y: null,
  width: 0,
  height: 0,
  dragging: false,
  userMoved: false,
  offsetX: 0,
  offsetY: 0,
});
const paperChatPanelStyle = computed(() => ({
  "--paper-chat-x": `${paperChatWindow.x ?? 0}px`,
  "--paper-chat-y": `${paperChatWindow.y ?? 92}px`,
  "--paper-chat-w": `${paperChatWindow.width || Math.min(1320, Math.max(360, window.innerWidth - 48))}px`,
  "--paper-chat-h": `${paperChatWindow.height || Math.min(920, Math.max(480, window.innerHeight - 116))}px`,
  left: `${paperChatWindow.x ?? 0}px`,
  top: `${paperChatWindow.y ?? 92}px`,
  width: `${paperChatWindow.width || Math.min(1320, Math.max(360, window.innerWidth - 48))}px`,
  height: `${paperChatWindow.height || Math.min(920, Math.max(480, window.innerHeight - 116))}px`,
  right: "auto",
  bottom: "auto",
  transform: "none",
}));
let noteSyncTimer = null;
const tourSteps = [
  {
    selector: ".reader-assistant",
    title: "① 文献综述区",
    description: "整个左侧区域展示精读后的文献综述，集中提炼研究背景、研究设计、主要发现、贡献价值与局限展望。",
  },
  {
    selector: ".expand-button",
    title: "② 左侧栏第 1 级折叠：向右大屏全景展开",
    description: "点击 Tab 栏右侧的【向右展开】按钮，可以将左侧详解面板扩展为全景大屏模式，排版字幅更宽，适合沉浸研读长篇解析报告。再次点击即可收回。",
  },
  {
    selector: ".collapse-button",
    title: "③ 左侧栏第 2 级折叠：完全收起侧边栏",
    description: "点击 Tab 栏最右侧的【收起侧边栏】按钮，可以彻底关闭左侧助手栏，让中间区域获得 100% 全屏视觉宽度，专注看 PDF 原文。",
  },
  {
    selector: ".assistant-tabs",
    title: "④ 目录、图表与思维导图视图",
    description: "在 Tab 栏可快速切换【论文目录】与【论文图表】，点击【思维导图】还可一键唤起全屏逻辑架构导图并导出 SVG/PNG 图片。",
  },
  {
    selector: ".reader-tools",
    title: "⑤ 全文双语对照与缩放控制",
    description: "顶部控制栏支持一键开启/关闭中英双语对照翻译、调节 50%-200% 页面缩放比例以及全屏沉浸阅读模式。",
  },
  {
    selector: ".reading-column",
    title: "⑥ 划词翻译、高亮与 AI 批注",
    description: "在原文或译文区鼠标拖选任意词句，即可唤起划词菜单：实时多语种翻译、多色高亮划线、记录笔记或让 AI 深入解释。",
  },
  {
    selector: ".paper-chat-launcher",
    title: "⑦ 智能学术 AI 问答助手",
    description: "随时点击右下角浮标唤起学术对话框，针对当前论文提问公式推导、实验参数细节或深度科研问题。",
  },
];
const readerTour = reactive({ open: false, index: 0, rect: null });
const figureViewer = reactive({ open: false, pageNumber: 0, caption: "", imageUrl: "", rotation: 0, block: null });
const mindMapModal = reactive({ open: false });
const mindMapState = reactive({ loading: false, error: "", report: null });
const mindMapSvg = ref(null);
const mindMapInstance = shallowRef(null);
let mindMapRuntimePromise = null;
let mindMapTransformer = null;
let mindMapDeriveOptions = null;

function createMindMapOptions(dark = isDarkTheme.value) {
  const palette = dark
    ? ["#818cf8", "#34d399", "#c084fc", "#fb923c", "#38bdf8", "#f472b6"]
    : ["#2f6df6", "#14a38b", "#8b5cf6", "#f97316", "#0ea5e9", "#e11d48"];
  return mindMapDeriveOptions
    ? mindMapDeriveOptions({
      color: palette,
      colorFreezeLevel: 2,
      duration: 240,
      fitRatio: 0.92,
      maxInitialScale: 1.8,
      maxWidth: 460,
      paddingX: 18,
    })
    : null;
}

function loadMindMapRuntime() {
  if (!mindMapRuntimePromise) {
    mindMapRuntimePromise = Promise.all([
      import("markmap-lib"),
      import("markmap-view"),
    ]).then(([markmapLib, markmapView]) => {
      mindMapTransformer = new markmapLib.Transformer();
      mindMapDeriveOptions = markmapView.deriveOptions;
      return { Markmap: markmapView.Markmap, deriveOptions: markmapView.deriveOptions };
    });
  }
  return mindMapRuntimePromise;
}

function openMindMapModal() {
  mindMapModal.open = true;
  loadMindMapReport();
}

function closeMindMapModal() {
  mindMapModal.open = false;
  destroyMindMap();
}

async function loadMindMapReport() {
  // Render abstract mind map instantly so the user has immediate access
  mindMapState.report = null;
  mindMapState.error = "";
  mindMapState.loading = false;
  renderMindMap();
  
  if (!workspaceId.value) {
    return;
  }
  
  // Asynchronously load the full meeting report details in the background
  mindMapState.loading = true;
  try {
    const data = await paperpilotApi.getMeetingReport(workspaceId.value);
    if (data && data.generated) {
      mindMapState.report = data;
      renderMindMap();
    }
  } catch (err) {
    console.warn("Failed to fetch full meeting report for mind map:", err);
  } finally {
    mindMapState.loading = false;
  }
}
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
  if (String(source).toLowerCase().startsWith("desktop-cache://")) return "";
  return paperpilotApi.buildPdfProxyUrl(source);
});

async function resolvePdfSourceForDocument() {
  const id = workspaceId.value;
  if (window.paperSolverDesktop?.getCachedPdf && id) {
    try {
      const cached = await window.paperSolverDesktop.getCachedPdf({ workspaceId: id });
      if (cached?.found && cached.base64) {
        return base64ToUint8Array(cached.base64);
      }
    } catch (error) {
      console.warn("desktop pdf cache read failed", error);
    }
  }
  return pdfSource.value;
}

function base64ToUint8Array(base64) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
}

function shortPaperTitle(title, max = 32) {
  const value = String(title || "未命名文献").replace(/\s+/g, " ").trim();
  return value.length > max ? `${value.slice(0, max)}…` : value;
}

function authorDisplaySegments(authors) {
  const value = String(authors || "").replace(/\s+/g, " ").trim();
  if (!value) return [];
  const parts = [];
  let cursor = 0;
  const pattern = /(\biD\b|\bORCID\b|\s+\^?[a-z](?![A-Z])(?:\s*,\s*\^?[a-z](?![A-Z]))*|\s+\^?\d+(?:\s*,\s*\^?\d+)*|\*|†|‡|§)/g;
  let match;
  while ((match = pattern.exec(value)) !== null) {
    const token = match[0];
    const start = match.index;
    if (start > cursor) {
      pushAuthorNameParts(parts, value.slice(cursor, start));
    }
    if (/^(iD|ORCID)$/i.test(token)) {
      parts.push({ key: `orcid-${start}`, text: "iD", orcid: true });
    } else if (isAuthorSupToken(token, value, start)) {
      parts.push({ key: `sup-${start}`, text: token.replace(/\s+/g, ""), sup: true });
    } else {
      pushAuthorNameParts(parts, token);
    }
    cursor = pattern.lastIndex;
  }
  if (cursor < value.length) {
    pushAuthorNameParts(parts, value.slice(cursor));
  }
  return parts.map((part, index) => ({ ...part, key: `${part.key}-${index}` }));
}

function pushAuthorNameParts(parts, text) {
  const value = String(text || "");
  if (!value) return;
  const split = value.split(/([,;，；]\s*)/);
  split.forEach((item, index) => {
    if (!item) return;
    parts.push({
      key: `author-${parts.length}-${index}`,
      text: item,
      name: /[A-Za-z\u4e00-\u9fa5]/.test(item),
    });
  });
}

function isAuthorSupToken(token, value, start) {
  if (/^[*†‡§]$/.test(token)) return true;
  if (token.startsWith("^")) return true;
  const previous = value.slice(Math.max(0, start - 2), start);
  const next = value.slice(start + token.length, start + token.length + 2);
  if (/^\d/.test(token)) return /[A-Za-z\u4e00-\u9fa5),，]\s*$/.test(previous);
  return /[A-Z][a-z]+(?:\s+[A-Z][a-z]+)?\s*$/.test(value.slice(0, start)) || /^[,，\s*†‡§]/.test(next);
}

function normalizedAuthorSignature(text) {
  return String(text || "")
    .toLowerCase()
    .replace(/\\/g, "")
    .replace(/\b(orcid|id)\b/g, "")
    .replace(/\b[a-z]\b\*?/g, "")
    .replace(/[\s,.;，；、^*†‡§-]+/g, "");
}

function isLikelyAuthorText(text) {
  const value = String(text || "").replace(/\\/g, "").replace(/\s+/g, " ").trim();
  if (!value || value.length > 520) return false;
  if (/^(abstract|keywords?|introduction|references)\b/i.test(value)) return false;
  if (isLikelyAffiliationText(value)) return false;
  const compactValue = value.replace(/\s+\b[a-z]\b\*?/g, "").replace(/\s+/g, " ");
  const knownAuthors = String(activePaper.value?.authors || "").trim();
  const knownSignature = normalizedAuthorSignature(knownAuthors);
  const valueSignature = normalizedAuthorSignature(value);
  if (knownSignature && valueSignature) {
    return knownSignature.includes(valueSignature) || valueSignature.includes(knownSignature);
  }
  const authorItems = value.split(/\s*[,;，；]\s*/).filter(item =>
    /^[A-Z][A-Za-z.'-]+(?:\s+[A-Z][A-Za-z.'-]+){1,4}(?:\s+[a-z]\*?)?$/.test(item.trim())
  );
  const nameMatches = compactValue.match(/\b[A-Z][A-Za-z.'-]+(?:\s+[A-Z][A-Za-z.'-]+){1,4}\b/g) || [];
  const separators = (value.match(/[,;，；]/g) || []).length;
  const sentenceWords = /\b(study|paper|method|result|conclusion|diagnosis|treatment|planning|computed|tomography)\b/i.test(value);
  return !sentenceWords && ((authorItems.length >= 3 && separators >= 2) || (nameMatches.length >= 3 && separators >= 2));
}

function isLikelyAffiliationText(text) {
  const value = String(text || "").replace(/\s+/g, " ").trim();
  if (!value) return false;
  return /\b(university|institute|college|school|department|faculty|hospital|clinic|laboratory|lab|center|centre|teknopark|technopark|park|ministry|academy|corporation|company|ltd|inc|gmbh|city|province|state|country|turkey|türkiye|china|usa|uk|ai)\b/i.test(value);
}

function isLikelyAuthorBlock(block) {
  if (!block || ["heading", "figure", "table", "equation", "references"].includes(block.kind)) return false;
  return isLikelyAuthorText(block.text);
}

function parsedAuthorText() {
  for (const page of pages.slice(0, 2)) {
    const blocks = Array.isArray(page.blocks) ? page.blocks : [];
    for (const block of blocks) {
      if (isLikelyAuthorBlock(block)) return String(block.text || "").trim();
      if (/^abstract\b/i.test(String(block.text || "").trim())) break;
    }
  }
  return "";
}

function authorsForMetadataTranslation() {
  return String(activePaper.value?.authors || "").trim() || parsedAuthorText();
}

const exactAuthorNameTranslations = new Map(Object.entries({
  "rongli zhang": "张荣莉",
  "kuo feng hung": "洪国峰",
  "jiegang yang": "杨杰刚",
  "andrew nalley": "安德鲁·纳利",
  "xin li": "李鑫",
  "mohamad koohi-moghadam": "穆罕默德·库希-莫加达姆",
  "reza safdari": "礼萨·萨夫达里",
  "dariush lotfi": "达里乌什·洛特菲",
  "qi yong h. ai": "艾启勇 H.",
  "yiu yan leung": "梁耀恩",
  "kyongtae ty bae": "裴京泰",
  "elaine r. peskind": "伊莱恩·R.·佩斯金德",
  "murray a. raskind": "默里·A.·拉斯金德",
  "michelle a. herman": "米歇尔·A.·赫尔曼",
  "rosemary morrison": "罗丝玛丽·莫里森",
  "genevieve matthews": "吉纳维芙·马修斯",
  "a. carol evans": "A.·卡罗尔·埃文斯",
  "ben boyarko": "本·博亚科",
  "guerry peavy": "格雷·皮维",
  "gabriel c. léger": "加布里埃尔·C.·莱杰",
  "gabriel c. leger": "加布里埃尔·C.·莱杰",
  "gregory a. jicha": "格雷戈里·A.·吉查",
  "neela patel": "尼拉·帕特尔",
  "sharon a. brangman": "莎朗·A.·布兰格曼",
  "aimee l. pierce": "艾米·L.·皮尔斯",
  "lon s. schneider": "朗·S.·施奈德",
  "shelia jin": "谢莉亚·金",
}));

const authorNameTokenTranslations = new Map(Object.entries({
  zhang: "张", hung: "洪", yang: "杨", li: "李", ai: "艾", leung: "梁", bae: "裴",
  rongli: "荣莉", kuo: "国", feng: "峰", jiegang: "杰刚", xin: "鑫", yiu: "耀", yan: "恩", kyongtae: "京泰", ty: "泰",
  andrew: "安德鲁", nalley: "纳利", mohamad: "穆罕默德", mohammad: "穆罕默德", koohi: "库希", moghadam: "莫加达姆",
  reza: "礼萨", safdari: "萨夫达里", dariush: "达里乌什", lotfi: "洛特菲",
  elaine: "伊莱恩", murray: "默里", raskind: "拉斯金德", michelle: "米歇尔", herman: "赫尔曼",
  rosemary: "罗丝玛丽", morrison: "莫里森", genevieve: "吉纳维芙", matthews: "马修斯",
  carol: "卡罗尔", evans: "埃文斯", ben: "本", boyarko: "博亚科", guerry: "格雷", peavy: "皮维",
  gabriel: "加布里埃尔", leger: "莱杰", gregory: "格雷戈里", jicha: "吉查", neela: "尼拉", patel: "帕特尔",
  sharon: "莎朗", brangman: "布兰格曼", aimee: "艾米", pierce: "皮尔斯", lon: "朗", schneider: "施奈德",
  shelia: "谢莉亚", sheila: "希拉", jin: "金",
}));

function containsCjk(text) {
  return /[\u3400-\u9fff]/.test(String(text || ""));
}

function isUntranslatedAuthorResult(source, translated) {
  const result = String(translated || "").trim();
  if (!result) return true;
  if (containsCjk(result)) return false;
  return normalizedAuthorSignature(source) === normalizedAuthorSignature(result) || isLikelyAuthorText(source);
}

function fallbackAuthorTranslation(authors) {
  const value = String(authors || "").replace(/\s+/g, " ").trim();
  if (!value) return "";
  return value
    .split(/\s*[,，;；]\s*/)
    .map(translateSingleAuthorName)
    .filter(Boolean)
    .join("、");
}

function translateSingleAuthorName(name) {
  const clean = String(name || "")
    .replace(/\\/g, "")
    .replace(/\b(ORCID|iD)\b/gi, "")
    .replace(/[\^*†‡§]+/g, "")
    .replace(/\s+/g, " ")
    .replace(/\s+\b[a-z]\b\.?\s*$/i, "")
    .trim();
  if (!clean) return "";
  const exact = exactAuthorNameTranslations.get(clean.toLowerCase());
  if (exact) return exact;
  const translated = clean
    .split(/(\s+|-)/)
    .map(part => {
      if (!part.trim() || part === "-") return part === "-" ? "-" : "";
      if (/^[A-Z]\.?$/i.test(part)) return part.toUpperCase().replace(/\.$/, ".");
      return authorNameTokenTranslations.get(part.toLowerCase().replace(/\.$/, "")) || part;
    })
    .filter(Boolean)
    .join("·")
    .replace(/·-·/g, "-")
    .replace(/\s+/g, "");
  return translated || clean;
}

const textColors = [
  { id: "white", label: "白色", value: "#ffffff" },
  { id: "black", label: "黑色", value: "#20242c" },
  { id: "red", label: "红色", value: "#ef4444" },
  { id: "blue", label: "蓝色", value: "#3b82f6" },
  { id: "yellow", label: "黄色", value: "#eab308" },
  { id: "green", label: "绿色", value: "#22c55e" },
  { id: "purple", label: "紫色", value: "#a855f7" },
];

const brushPresets = [
  { id: "blue", value: "#4f7ee8" },
  { id: "black", value: "#111827" },
  { id: "green", value: "#2f8f4e" },
];

const textMarkTools = [
  { id: "fontColor", label: "字体颜色", tip: "字体颜色 (C)", iconClass: "mark-font-color" },
  { id: "underline", label: "下划线", tip: "下划线 (U)", iconClass: "mark-underline" },
  { id: "strike", label: "删除线", tip: "删除线 (S)", iconClass: "mark-strike" },
  { id: "wavy", label: "波浪线", tip: "波浪线 (G)", iconClass: "mark-wavy" },
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
  { id: "gray-500", label: "中灰", value: "#9ca3af" },
  { id: "gray-700", label: "深灰", value: "#666666" },
  { id: "gray-900", label: "近黑", value: "#222222" },
  { id: "black", label: "黑色", value: "#000000" },
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

const mindMapMarkdown = computed(() => {
  const title = mindMapText(activePaper.value?.title || "当前论文", 80);
  const lines = [`# ${title}`];
  const sections = normalizeMindMapSections(mindMapState.report?.sections || {});
  const nodes = buildMindMapNodes(sections);
  nodes.forEach(node => {
    if (!node.items.length) return;
    lines.push(`## ${mindMapText(node.title, 36)}`);
    node.items.slice(0, 5).forEach(item => {
      lines.push(`### ${mindMapText(item.title, 42)}`);
      item.details.slice(0, 3).forEach(detail => {
        lines.push(`#### ${mindMapText(detail, 168)}`);
      });
    });
  });
  if (lines.length === 1) {
    const abstract = mindMapText(abstractText.value, 220);
    lines.push(
      "## 核心内容",
      `### ${abstract || "等待 AI 精读报告生成后展示论文内容导图"}`,
      "## 阅读线索",
      "### 研究背景",
      "### 研究方法",
      "### 实验结果",
      "### 结论启发",
    );
  }
  return lines.join("\n");
});

const mindMapLabels = [
  "研究背景", "研究问题", "研究方法与数据", "实验与结论", "创新点与启示", "局限性",
  "论文定位", "发表信息", "发布信息", "汇报价值", "核心要点", "主要贡献", "关键问题", "本文思想",
  "关键贡献", "整体框架", "关键模块", "实现流程", "主要发现", "对比结果", "实验结论", "研究结论",
  "现有不足", "未来展望", "数据来源", "数据设置", "评测指标",
];

function normalizeMindMapSections(sections = {}) {
  return Object.fromEntries(Object.entries(sections).map(([key, value]) => [key, formatMindMapParagraphs(value)]));
}

function formatMindMapParagraphs(value = "") {
  const labelPattern = mindMapLabels.join("|");
  return String(value || "")
    .replace(/\r\n/g, "\n")
    .replace(/发布信息/g, "发表信息")
    .replace(new RegExp(`\\s*((?:${labelPattern})\\s*[：:])\\s*`, "g"), "\n\n$1\n")
    .replace(/([。；;])\s*((?:\d+[.、]|[（(]\d+[）)]))/g, "$1\n$2")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function buildMindMapNodes(sections = {}) {
  const groups = [
    { title: "一、研究背景", entries: [["关键问题", "background", /背景|现状|问题|挑战|缺口|需求/], ["研究动机", "overview", /动机|目标|问题|挑战|需求/]] },
    { title: "二、研究问题", entries: [["核心问题", "synthesis", /研究问题|问题|目标|挑战/], ["主要贡献", "overview", /贡献|创新|提出|解决/]] },
    { title: "三、研究思路", entries: [["构建研究框架", "method", /框架|模型|架构|流程|模块/], ["选择研究方法", "method", /方法|策略|算法|训练|优化|实现/]] },
    { title: "四、数据与实验", entries: [["分析数据", "datasets", /数据|样本|指标|评测|设置/], ["验证结果", "results", /实验|结果|对比|提升|性能|发现/]] },
    { title: "五、结论启发", entries: [["得出结论", "conclusion", /结论|优势|价值|证明|表明/], ["创新点与局限", "synthesis", /创新|启示|局限|不足|未来/]] },
  ];
  return groups.map(group => ({
    title: group.title,
    items: group.entries.map(([title, key, hint]) => buildMindMapItem(title, sections, key, hint)).filter(Boolean),
  }));
}

function buildMindMapItem(title, sections, key, hint) {
  const lines = collectMindMapLines(sections[key], hint);
  const fallbackKeys = {
    background: ["overview", "synthesis"],
    overview: ["synthesis", "background"],
    method: ["synthesis"],
    datasets: ["method", "results"],
    results: ["synthesis", "conclusion"],
    conclusion: ["synthesis", "results"],
    synthesis: ["overview", "background", "method", "results", "conclusion"],
  }[key] || [];
  const details = lines.length
    ? lines
    : fallbackKeys.flatMap(nextKey => collectMindMapLines(sections[nextKey], hint)).slice(0, 3);
  const looseDetails = details.length ? details : collectMindMapLines(sections[key], null).slice(0, 3);
  if (!looseDetails.length) return null;
  return { title, details: dedupeMindMapLines(looseDetails).slice(0, 4) };
}

function collectMindMapLines(raw, hint) {
  return String(raw || "")
    .split(/\n+|(?<=[。！？；])\s*/)
    .map(cleanMindMapLine)
    .filter(line => isMindMapLine(line) && (!hint || hint.test(line)))
    .slice(0, 6);
}

function cleanMindMapLine(line) {
  return String(line || "")
    .replace(/\\[rnt]/g, " ")
    .replace(/^[\-•·○◦▪▫\d.、\s]+/, "")
    .replace(/[{}"“”]+/g, "")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function isMindMapLine(line) {
  const value = cleanMindMapLine(line);
  if (value.length <= 6) return false;
  if (value.length > 180) return true;
  if (mindMapLabels.some(label => value === label || value === `${label}：` || value === `${label}:`)) return false;
  return !/等待 AI|第\s*\d+\s*页|暂无|HTTP\s*5/.test(value);
}

function dedupeMindMapLines(lines) {
  const seen = new Set();
  return lines.filter(line => {
    const key = line.replace(/[^\u4e00-\u9fa5A-Za-z0-9]/g, "").toLowerCase();
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function mindMapText(value, max = 80) {
  return String(value || "")
    .replace(/\s+/g, " ")
    .replace(/[\\`*_{}[\]()#+\-.!|>]/g, "\\$&")
    .trim()
    .slice(0, max);
}

async function renderMindMap() {
  if (!mindMapModal.open) return;
  await nextTick();
  try {
    const { Markmap } = await loadMindMapRuntime();
    if (!mindMapModal.open) return;
    const svg = mindMapSvg.value;
    if (!svg) return;
    const { root } = mindMapTransformer.transform(mindMapMarkdown.value);
    const mindMapOptions = createMindMapOptions();
    if (!mindMapOptions) throw new Error("markmap 配置初始化失败");
    
    if (mindMapInstance.value) {
      mindMapInstance.value.setOptions(mindMapOptions);
      await mindMapInstance.value.setData(root);
    } else {
      mindMapInstance.value = Markmap.create(svg, mindMapOptions, root);
    }
    mindMapState.error = "";
    requestAnimationFrame(() => focusMindMapReadable());
  } catch (err) {
    console.error("Failed to render mind map:", err);
    mindMapState.error = "渲染思维导图失败，请刷新页面重试。(" + (err.message || String(err)) + ")";
  }
}

function destroyMindMap() {
  mindMapInstance.value?.destroy();
  mindMapInstance.value = null;
}

function zoomMindMap(scale) {
  return mindMapInstance.value?.rescale(scale);
}

function fitMindMap(maxScale = 1.35) {
  return mindMapInstance.value?.fit(maxScale);
}

async function focusMindMapReadable() {
  await fitMindMap(1.2);
  await zoomMindMap(2.2);
}

function mindMapExportName(ext) {
  const title = String(activePaper.value?.title || "论文思维导图")
    .replace(/[\\/:*?"<>|]/g, "")
    .replace(/\s+/g, "-")
    .slice(0, 48) || "论文思维导图";
  return `${title}-思维导图.${ext}`;
}

function serializeMindMapSvg() {
  const svg = mindMapSvg.value;
  if (!svg) return "";
  const rect = svg.getBoundingClientRect();
  const clone = svg.cloneNode(true);
  clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
  clone.setAttribute("width", String(Math.max(1, Math.round(rect.width || 980))));
  clone.setAttribute("height", String(Math.max(1, Math.round(rect.height || 620))));
  const style = document.createElementNS("http://www.w3.org/2000/svg", "style");
  
  const textColor = isDarkTheme.value ? "#f1f5f9" : "#172033";
  const circleFill = isDarkTheme.value ? "#090d16" : "#ffffff";
  const linkOpacity = isDarkTheme.value ? ".58" : ".72";
  
  style.textContent = `
    .markmap-node text { font: 650 15px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; fill: ${textColor} !important; }
    .markmap-node circle { stroke-width: 1.5px; fill: ${circleFill} !important; }
    .markmap-link { stroke-opacity: ${linkOpacity} !important; }
    .markmap-node div,
    .markmap-node span,
    .markmap-foreign,
    .markmap-foreign * {
      fill: ${textColor} !important;
      color: ${textColor} !important;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif !important;
      font-size: 15px !important;
      font-weight: 650 !important;
      background: transparent !important;
    }
  `;
  clone.insertBefore(style, clone.firstChild);
  
  // Prepend a background rect so the SVG has a solid theme color background
  const bgRect = document.createElementNS("http://www.w3.org/2000/svg", "rect");
  bgRect.setAttribute("width", "100%");
  bgRect.setAttribute("height", "100%");
  bgRect.setAttribute("fill", isDarkTheme.value ? "#090d16" : "#ffffff");
  clone.insertBefore(bgRect, clone.firstChild);
  
  return new XMLSerializer().serializeToString(clone);
}

function downloadMindMapBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function exportMindMapSvg() {
  await fitMindMap();
  const source = serializeMindMapSvg();
  if (!source) return;
  downloadMindMapBlob(new Blob([source], { type: "image/svg+xml;charset=utf-8" }), mindMapExportName("svg"));
}

async function exportMindMapPng() {
  await fitMindMap();
  const source = serializeMindMapSvg();
  if (!source) return;
  const svg = mindMapSvg.value;
  const rect = svg.getBoundingClientRect();
  const width = Math.max(1, Math.round(rect.width || 980));
  const height = Math.max(1, Math.round(rect.height || 620));
  const encoded = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(source)}`;
  const image = new Image();
  image.decoding = "async";
  image.onload = () => {
    const canvas = document.createElement("canvas");
    canvas.width = width * 2;
    canvas.height = height * 2;
    const context = canvas.getContext("2d");
    context.fillStyle = isDarkTheme.value ? "#090d16" : "#ffffff";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    canvas.toBlob((pngBlob) => {
      if (pngBlob) {
        downloadMindMapBlob(pngBlob, mindMapExportName("png"));
      } else {
        readerToast.value = "PNG 导出失败，请先导出 SVG";
      }
    }, "image/png");
  };
  image.onerror = () => {
    readerToast.value = "PNG 导出失败，请先导出 SVG";
  };
  image.src = encoded;
}

watch(mindMapMarkdown, () => {
  renderMindMap();
});

watch(isDarkTheme, () => {
  if (mindMapModal.open) {
    renderMindMap();
  }
});

function normalizeText(text) {
  return String(text || "").replace(/\s+/g, " ").trim();
}

function inlineCitationSegments(text) {
  const value = String(text || "");
  if (!value) return [];
  const patterns = [
    /\[(?:\d{1,3}\s*(?:[-–,]\s*\d{1,3})*)\]/g,
    /(?<!\d)(?<=[,.;，。；、])\s*\d{1,3}(?:\s*[,，]\s*\d{1,3})*(?=(?:\s|[,.;:，。；：、)\]]|$))/g,
    /(?<=[A-Za-z\u4e00-\u9fa5)\]])\d{1,3}(?=(?:[,.;:，。；：、)\]]|\s|$))/g,
  ];
  const ranges = [];
  patterns.forEach((pattern) => {
    let match;
    while ((match = pattern.exec(value)) !== null) {
      const raw = match[0];
      const leading = raw.match(/^\s*/)?.[0]?.length || 0;
      const start = match.index + leading;
      const end = match.index + raw.length;
      const token = value.slice(start, end);
      if (!token.trim()) continue;
      if (isDecimalNumberCitationFalsePositive(value, start)) continue;
      if (ranges.some(range => start < range.end && end > range.start)) continue;
      ranges.push({ start, end });
    }
  });
  if (!ranges.length) return [{ key: "plain-0", text: value, citation: false }];
  ranges.sort((a, b) => a.start - b.start);
  const parts = [];
  let cursor = 0;
  ranges.forEach((range, index) => {
    if (range.start > cursor) {
      parts.push({ key: `text-${index}-${cursor}`, text: value.slice(cursor, range.start), citation: false });
    }
    parts.push({ key: `cite-${index}-${range.start}`, text: value.slice(range.start, range.end), citation: true });
    cursor = range.end;
  });
  if (cursor < value.length) {
    parts.push({ key: `text-tail-${cursor}`, text: value.slice(cursor), citation: false });
  }
  return parts;
}

function isDecimalNumberCitationFalsePositive(value, start) {
  const before = value.slice(Math.max(0, start - 2), start);
  return /\d[.,]$/.test(before);
}

function translationTextWithoutCitations(text) {
  return inlineCitationSegments(text)
    .filter(part => !part.citation)
    .map(part => part.text)
    .join("")
    .replace(/\s+([,.;:，。；：])/g, "$1")
    .replace(/\s{2,}/g, " ")
    .trim();
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

function providerShortLabel(providerId) {
  const label = providerLabel(providerId);
  if (providerId === "google-web") return "Google 网页";
  if (providerId === "google-api") return "Google API";
  if (providerId === "microsoft") return "微软翻译";
  if (providerId === "tencent") return "腾讯翻译";
  if (providerId === "tencent-transmart") return "腾讯 TranSmart";
  if (providerId === "huoshan-web") return "火山网页";
  if (providerId === "youdao") return "有道翻译";
  if (providerId === "deeplx") return "DeepLX";
  if (providerId === "deepl") return "DeepL";
  if (providerId === "ai") return "AI 翻译";
  return label;
}

function cycleAndRetranslate(block) {
  const ids = translationProviders.value.filter(provider => provider.configured !== false).map(provider => provider.id);
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
  figureViewer.rotation = 0;
  figureViewer.block = null;
  nextTick(() => renderPagePreview(pageNumber, figureCanvasRef.value));
}

function viewParsedFigure(block, pageNumber) {
  figureViewer.open = true;
  figureViewer.pageNumber = pageNumber;
  figureViewer.caption = block.text || block.equationNumber || `第 ${pageNumber} 页图表`;
  figureViewer.imageUrl = block.imageUrl || "";
  figureViewer.rotation = 0;
  figureViewer.block = block;
}

function closeFigureViewer() {
  figureViewer.open = false;
  figureViewer.imageUrl = "";
  figureViewer.rotation = 0;
  figureViewer.block = null;
}

function autoRotateFigure(event) {
  const image = event?.target;
  if (!image?.naturalWidth || !image?.naturalHeight) return;
  figureViewer.rotation = shouldAutoRotateFigure(image.naturalWidth, image.naturalHeight) ? 90 : 0;
}

function shouldAutoRotateFigure(width, height) {
  return Number(height || 0) > Number(width || 0) * 1.18;
}

function equationSelectionText(block, pageNumber) {
  const text = normalizeText(block?.text || "");
  if (text) return text;
  const number = normalizeText(block?.equationNumber || "");
  const page = Number(pageNumber || 0) > 0 ? `，位于第 ${pageNumber} 页` : "";
  return `论文公式${number ? ` ${number}` : ""}${page}，请结合公式原图和上下文解读其含义。`;
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

function compactForSelectionAi(text, limit = 760) {
  const value = normalizeText(text);
  if (value.length <= limit) {
    return { text: value, compacted: false };
  }
  const headLength = Math.floor(limit * 0.56);
  const tailLength = limit - headLength;
  return {
    text: `${value.slice(0, headLength)}\n...[中间选区已压缩]...\n${value.slice(-tailLength)}`,
    compacted: true,
  };
}

function placeSelectionPopover(rect, width = 392) {
  const edge = 14;
  const toolbarHeight = 66;
  const gap = 12;
  const left = Math.max(edge, Math.min(window.innerWidth - width - edge, rect.left + rect.width / 2 - width / 2));
  const hasRoomBelow = rect.bottom + gap + toolbarHeight < window.innerHeight - edge;
  const top = hasRoomBelow
    ? rect.bottom + gap
    : Math.max(edge, rect.top - toolbarHeight - gap);
  selectionTranslator.x = left;
  selectionTranslator.y = top;
  selectionTranslator.placement = hasRoomBelow ? "below" : "above";
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
  selectionTranslator.wasCompacted = false;
  selectionTranslator.error = "";
  selectionTranslator.result = "";
  nextTick(() => fitSelectionPopover(false));
  try {
    const result = await paperpilotApi.translate({
      text,
      provider: abstractProvider.value || "google",
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
    nextTick(() => fitSelectionPopover(false));
  }
}

async function explainSelection() {
  const text = selectionTranslator.source;
  if (!text || selectionTranslator.loading) return;
  const selected = compactForSelectionAi(text, 3600);
  const context = compactForSelectionAi(selectionTranslator.paragraph || selectionTranslator.sentence || text, 6400);
  selectionTranslator.loading = true;
  selectionTranslator.resultTitle = "AI 解读";
  selectionTranslator.loadingText = "正在结合论文语境解读…";
  selectionTranslator.wasCompacted = selected.compacted || context.compacted;
  selectionTranslator.error = "";
  selectionTranslator.result = "";
  nextTick(() => fitSelectionPopover(false));
  try {
    const result = await paperpilotApi.askPaperSelection(workspaceId.value, {
      selection: selected.text,
      paragraph: context.text,
      question: "请用中文解读选中内容：1.它在论文中的作用；2.关键概念；3.读者应注意的研究含义。若选区被压缩，请结合上下文概括，不要要求用户重选。",
    });
    selectionTranslator.result = cleanChatAnswer(result?.answer);
    if (!selectionTranslator.result) selectionTranslator.error = "本次没有返回解读，请重试。";
  } catch (error) {
    console.warn("selection explanation failed", error);
    if (isSelectionLengthError(error)) {
      try {
        const retrySelected = compactForSelectionAi(text, 1800);
        const retryContext = compactForSelectionAi(selectionTranslator.sentence || selectionTranslator.paragraph || text, 2200);
        selectionTranslator.wasCompacted = true;
        const retry = await paperpilotApi.askPaperSelection(workspaceId.value, {
          selection: retrySelected.text,
          paragraph: retryContext.text,
          question: "请用中文摘要式解读这段长选区：说明论文作用、关键概念和研究含义，控制在300字以内。",
        });
        selectionTranslator.result = cleanChatAnswer(retry?.answer);
        if (!selectionTranslator.result) selectionTranslator.error = "本次没有返回解读，请重试。";
      } catch (retryError) {
        console.warn("selection explanation retry failed", retryError);
        selectionTranslator.error = "选区已自动压缩，但 AI 解读仍未返回，请稍后重试。";
      }
    } else {
      selectionTranslator.error = error?.response?.data?.message || "AI 解读失败，请稍后重试。";
    }
  } finally {
    selectionTranslator.loading = false;
    nextTick(() => fitSelectionPopover(false));
  }
}

function isSelectionLengthError(error) {
  const message = String(error?.response?.data?.message || error?.message || "");
  return /过长|太长|length|too long|limit|字符|范围/.test(message);
}

function addSelectionToChat() {
  const text = selectionTranslator.source;
  if (!text) return;
  const selected = compactForSelectionAi(text, 900);
  openPaperChatPanel();
  paperChat.question = `请结合当前论文解释这段选中内容：\n${selected.text}`;
  closeSelectionTranslator();
  nextTick(() => document.querySelector(".paper-chat-panel textarea")?.focus());
}

function syncHierarchicalNoteToLibrary(markdown) {
  const id = activePaper.value?.id;
  if (!id) return;
  const note = String(markdown || "").trim();
  libraryStore.updateDocument(id, { note });
  clearTimeout(noteSyncTimer);
  noteSyncTimer = window.setTimeout(() => {
    libraryStore.persistDocumentPatch(id, { note }).catch((error) => {
      console.warn("Failed to sync hierarchical notes to library", error);
      showReaderToast("文献库笔记同步失败，已先保存在本地");
    });
  }, 650);
}

function preferredPaperChatRect() {
  const marginX = 24;
  const marginTop = 92;
  const marginBottom = 24;
  const width = Math.min(1320, Math.max(360, window.innerWidth - marginX * 2));
  const height = Math.min(920, Math.max(480, window.innerHeight - marginTop - marginBottom));
  return {
    width,
    height,
    x: Math.max(12, Math.round((window.innerWidth - width) / 2)),
    y: marginTop,
  };
}

function resetPaperChatWindowPosition() {
  const rect = preferredPaperChatRect();
  paperChatWindow.width = rect.width;
  paperChatWindow.height = rect.height;
  paperChatWindow.x = rect.x;
  paperChatWindow.y = rect.y;
  paperChatWindow.userMoved = false;
}

function openPaperChatPanel() {
  resetPaperChatWindowPosition();
  paperChat.open = true;
}

function closePaperChatPanel() {
  paperChat.open = false;
  paperChatWindow.dragging = false;
}

function togglePaperChatPanel() {
  if (paperChat.open) closePaperChatPanel();
  else openPaperChatPanel();
}

function startPaperChatDrag(event) {
  if (event.target?.closest?.("button, textarea, input, a")) return;
  event.preventDefault();
  const panel = event.currentTarget?.closest?.(".paper-chat-panel");
  if (!panel) return;
  const rect = panel.getBoundingClientRect();
  paperChatWindow.dragging = true;
  paperChatWindow.userMoved = true;
  paperChatWindow.offsetX = event.clientX - rect.left;
  paperChatWindow.offsetY = event.clientY - rect.top;
  paperChatWindow.x = rect.left;
  paperChatWindow.y = rect.top;
  try {
    event.currentTarget?.setPointerCapture?.(event.pointerId);
  } catch {
    // Window-level listeners below keep dragging reliable even when capture is unavailable.
  }
  window.addEventListener("pointermove", movePaperChatDrag);
  window.addEventListener("pointercancel", stopPaperChatDrag, { once: true });
  window.addEventListener("pointerup", stopPaperChatDrag, { once: true });
}

function movePaperChatDrag(event) {
  if (!paperChatWindow.dragging) return;
  event.preventDefault();
  const width = paperChatWindow.width || Math.min(1180, window.innerWidth - 92);
  const height = paperChatWindow.height || Math.min(880, window.innerHeight - 104);
  const minTop = 84;
  const nextX = event.clientX - paperChatWindow.offsetX;
  const nextY = event.clientY - paperChatWindow.offsetY;
  paperChatWindow.x = Math.min(Math.max(12, nextX), Math.max(12, window.innerWidth - width - 12));
  paperChatWindow.y = Math.min(Math.max(minTop, nextY), Math.max(minTop, window.innerHeight - height - 12));
}

function stopPaperChatDrag() {
  paperChatWindow.dragging = false;
  window.removeEventListener("pointermove", movePaperChatDrag);
  window.removeEventListener("pointercancel", stopPaperChatDrag);
}

function analyzeFigure(block) {
  if (!block) return;
  const caption = block.text || (block.kind === "table" ? "表格" : "图像");
  openPaperChatPanel();

  let pageText = "";
  for (const page of pages) {
    if (page.blocks && page.blocks.includes(block)) {
      pageText = page.blocks.map(b => b.text || "").filter(Boolean).join("\n\n");
      break;
    }
  }

  const userMsgId = paperChat.nextId++;
  paperChat.messages.push({
    id: userMsgId,
    role: "user",
    content: `请结合论文上下文，深度剖析图表【${caption}】的架构体系、数据关联与得出的核心结论。`,
    figure: {
      caption,
      imageUrl: block.imageUrl || ""
    }
  });

  paperChat.loading = true;
  nextTick(() => {
    const container = document.querySelector(".paper-chat-messages");
    if (container) container.scrollTop = container.scrollHeight;
  });

  paperpilotApi.askPaperSelection(workspaceId.value, {
    question: `请结合上下文分析这幅图表的内容及其得出的结论：\n【${caption}】`,
    selection: caption,
    paragraph: pageText || caption
  }).then(result => {
    const answer = cleanChatAnswer(result?.answer) || "本次没有返回回答，请重试。";
    paperChat.messages.push({ id: paperChat.nextId++, role: "assistant", content: answer });
  }).catch(error => {
    console.warn("paper chat figure analysis failed", error);
    paperChat.messages.push({
      id: paperChat.nextId++,
      role: "assistant",
      content: error?.response?.data?.message || "PaperSolver 暂时无法解析该图表，请稍后重试。",
    });
  }).finally(() => {
    paperChat.loading = false;
    nextTick(() => {
      const container = document.querySelector(".paper-chat-messages");
      if (container) container.scrollTop = container.scrollHeight;
    });
  });
}

function insertQuickPrompt(text) {
  paperChat.question = text;
  askPaperChat();
}

function findPreviousPaperChatPrompt(messageIndex) {
  for (let index = messageIndex - 1; index >= 0; index -= 1) {
    const message = paperChat.messages[index];
    if (message?.role === "user" && String(message.content || "").trim()) {
      return String(message.content || "").trim();
    }
  }
  return "";
}

async function retryPaperChatMessage(messageIndex) {
  if (paperChat.loading) return;
  const question = findPreviousPaperChatPrompt(messageIndex);
  if (!question) {
    showReaderToast("这条欢迎语没有可重新分析的问题");
    return;
  }
  paperChat.messages.splice(messageIndex, 1);
  paperChat.question = question;
  await askPaperChat({ appendUserMessage: false });
}

async function copyPaperChatMessage(message) {
  const text = String(message?.content || "").trim();
  if (!text) return;
  try {
    await navigator.clipboard?.writeText(text);
    showReaderToast("已复制 AI 回答");
  } catch {
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand("copy");
    document.body.removeChild(textarea);
    showReaderToast("已复制 AI 回答");
  }
}

async function askPaperChat(options = {}) {
  const { appendUserMessage = true } = options;
  const question = paperChat.question.trim();
  if (!question || paperChat.loading) return;
  if (appendUserMessage) {
    paperChat.messages.push({ id: paperChat.nextId++, role: "user", content: question });
  }
  paperChat.question = "";
  paperChat.loading = true;
  try {
    const result = await paperpilotApi.askPaperSelection(workspaceId.value, {
      question,
      paragraph: currentReaderContextForChat(),
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

function currentReaderContextForChat() {
  const chunks = [];
  for (const page of pages) {
    const text = (page.blocks || [])
      .map(block => String(block.text || "").trim())
      .filter(Boolean)
      .join("\n");
    if (text) chunks.push(`第 ${page.pageNumber} 页\n${text}`);
    if (chunks.join("\n\n").length > 12000) break;
  }
  return chunks.join("\n\n").slice(0, 14000);
}

function cleanChatAnswer(value) {
  return String(value || "")
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

function clearCustomFontColors() {
  const container = document.querySelector(".reflow-document");
  if (!container) return;
  const coloredSpans = container.querySelectorAll(".reader-font-color");
  coloredSpans.forEach(span => {
    const parent = span.parentNode;
    while (span.firstChild) {
      parent.insertBefore(span.firstChild, span);
    }
    parent.removeChild(span);
  });
}

function clearAllAnnotations() {
  clearCustomFontColors();
  if (!annotations.length && !drawingStrokes.length) {
    if (clearedAnnotationSnapshot.value.length) {
      annotations.push(...clearedAnnotationSnapshot.value);
      clearedAnnotationSnapshot.value = [];
      persistAnnotations();
      showReaderToast("已恢复刚清除的标注与字体颜色");
      return;
    }
    showReaderToast("已清除字体颜色与全部标注");
    return;
  }
  clearedAnnotationSnapshot.value = annotations.map(item => ({ ...item }));
  annotations.splice(0, annotations.length);
  drawingStrokes.splice(0, drawingStrokes.length);
  persistAnnotations();
  persistDrawingStrokes();
  redrawDrawingCanvas();
  closeSelectionTranslator();
  showReaderToast("已清除字体颜色与全部标注");
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
  const edge = 14;
  const width = Math.min(popover.offsetWidth || 392, window.innerWidth - edge * 2);
  const height = Math.min(popover.scrollHeight, window.innerHeight - edge * 2);
  selectionTranslator.x = Math.max(edge, Math.min(selectionTranslator.x, window.innerWidth - width - edge));
  selectionTranslator.y = Math.max(edge, Math.min(selectionTranslator.y, window.innerHeight - height - edge));
  if (focusTextarea) {
    nextTick(() => popover.querySelector("textarea")?.focus());
  }
}

function saveAnnotation() {
  const note = selectionTranslator.annotationDraft.trim();
  if (!note || !selectionTranslator.blockId) return;

  const range = selectedRange.value;
  let coords = { x1: 20, y1: 40, x2: 230, y2: 50, top: 40 };
  if (range) {
    const r = range.getBoundingClientRect();
    const doc = document.querySelector(".reflow-document")?.getBoundingClientRect() || { left: 0, top: 0 };
    const relativeTop = Math.max(10, r.top - doc.top);
    const relativeLeft = Math.max(10, r.right - doc.left);
    coords = {
      x1: relativeLeft,
      y1: relativeTop + 8,
      x2: 230,
      y2: relativeTop + 16,
      top: relativeTop,
    };
  }

  if (selectionTranslator.editingAnnotationId) {
    const existing = annotations.find(item => item.id === selectionTranslator.editingAnnotationId);
    if (existing) existing.note = note;
  } else {
    annotations.push({
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      type: "note",
      blockId: selectionTranslator.blockId,
      preview: selectionTranslator.source.slice(0, 40),
      quote: selectionTranslator.source,
      start: selectionTranslator.start,
      end: selectionTranslator.end,
      note,
      style: "highlight",
      color: "#bbf7d0",
      createdAt: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      ...coords,
    });
  }
  persistAnnotations();
  closeSelectionTranslator();
  showReaderToast("已添加 WPS 绿折线引线批注");
}

function editAnnotation(annotation, event) {
  selectionTranslator.source = annotation.quote;
  selectionTranslator.preview = annotation.quote.length > 180 ? `${annotation.quote.slice(0, 180)}…` : annotation.quote;
  selectionTranslator.blockId = annotation.blockId;
  selectionTranslator.result = "";
  selectionTranslator.resultTitle = "";
  selectionTranslator.loadingText = "正在处理选区…";
  selectionTranslator.wasCompacted = false;
  selectionTranslator.error = "";
  selectionTranslator.annotating = true;
  selectionTranslator.annotationDraft = annotation.note;
  selectionTranslator.editingAnnotationId = annotation.id;
  selectionTranslator.start = Number.isInteger(annotation.start) ? annotation.start : -1;
  selectionTranslator.end = Number.isInteger(annotation.end) ? annotation.end : -1;
  selectionTranslator.x = Math.max(12, Math.min(window.innerWidth - 404, event.clientX - 360));
  selectionTranslator.y = Math.max(56, Math.min(window.innerHeight - 330, event.clientY - 60));
  selectionTranslator.placement = "below";
  selectionTranslator.open = true;
  nextTick(() => fitSelectionPopover(true));
}

function isAbstractHeadingBlock(block) {
  if (!block) return false;
  const text = String(block.text || "").trim();
  return block.kind === "heading" && /^abstract\s*[:：]?$/i.test(text);
}

function isKeywordBlockText(text) {
  return /^(keywords?|key\s+words)\s*[:：]/i.test(String(text || "").trim());
}

function normalizeKeywordBlockText(text) {
  const raw = String(text || "").replace(/\s+/g, " ").trim();
  if (!isKeywordBlockText(raw)) return raw;
  const matches = [...raw.matchAll(/\bkeywords?\s*[:：]/gi)];
  if (matches.length > 1 && matches[1].index > 0) {
    return raw.slice(0, matches[1].index).trim();
  }
  return raw;
}

function normalizeKeywordTranslation(sourceText, translatedText) {
  const raw = String(translatedText || "").replace(/\s+/g, " ").trim();
  if (!isKeywordBlockText(sourceText) || !raw) return raw;
  const withoutRepeatedLabels = raw
    .replace(/^(关键词|关键字|keywords?|key\s+words)\s*[:：]\s*/i, "")
    .replace(/\s+(关键词|关键字|keywords?|key\s+words)\s*[:：]\s*.*$/i, "")
    .trim();
  return withoutRepeatedLabels ? `关键词：${withoutRepeatedLabels}` : raw;
}

async function translateBlock(block, force = false) {
  if (!block || !block.text || block.translating) return;
  if (isAbstractHeadingBlock(block)) return;
  if (!force && block.translation) return;
  const normalizedBlockText = normalizeKeywordBlockText(block.text);
  if (normalizedBlockText && normalizedBlockText !== block.text) block.text = normalizedBlockText;
  const textForTranslation = translationTextWithoutCitations(normalizedBlockText);
  if (!textForTranslation) return;

  block.translating = true;
  block.translationError = "";
  await acquireTranslationSlot();
  try {
    const provider = block.translationProvider || abstractProvider.value || "google";
    const result = await paperpilotApi.translate({
      text: textForTranslation,
      provider: provider,
      sourceLang: "auto",
      targetLang: "zh-CN",
    }, { timeout: 45000 });
    const translated = String(result?.translatedText || result?.text || "").trim();
    const normalizedTranslation = normalizeKeywordTranslation(block.text, translated);
    block.translation = isLikelyAuthorBlock(block) && isUntranslatedAuthorResult(block.text, normalizedTranslation)
      ? fallbackAuthorTranslation(block.text)
      : normalizedTranslation;
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

async function translatePaperMetadata(force = false) {
  const paperId = String(activePaper.value?.id || workspaceId.value || "");
  const provider = abstractProvider.value || "google";
  if (
    !force
    && paperMetaTranslation.paperId === paperId
    && paperMetaTranslation.provider === provider
    && (paperMetaTranslation.title || paperMetaTranslation.authors)
  ) {
    return;
  }
  paperMetaTranslation.paperId = paperId;
  paperMetaTranslation.provider = provider;
  paperMetaTranslation.title = "";
  paperMetaTranslation.authors = "";

  const title = String(activePaper.value?.title || "").trim();
  const authors = authorsForMetadataTranslation();
  if (title) {
    paperMetaTranslation.loading = true;
    try {
      const result = await paperpilotApi.translate({
        text: title,
        provider,
        sourceLang: "auto",
        targetLang: "zh-CN",
      }, { timeout: 45000 });
      paperMetaTranslation.title = String(result?.translatedText || "").trim();
    } catch (error) {
      console.warn("paper title translation failed", error);
      paperMetaTranslation.title = "";
    } finally {
      paperMetaTranslation.loading = false;
    }
  }
  if (authors) {
    paperMetaTranslation.loadingAuthors = true;
    try {
      const result = await paperpilotApi.translate({
        text: authors,
        provider,
        sourceLang: "auto",
        targetLang: "zh-CN",
      }, { timeout: 45000 });
      const translated = String(result?.translatedText || "").trim();
      paperMetaTranslation.authors = isUntranslatedAuthorResult(authors, translated)
        ? fallbackAuthorTranslation(authors)
        : translated;
    } catch (error) {
      console.warn("paper author translation failed", error);
      paperMetaTranslation.authors = "";
    } finally {
      paperMetaTranslation.loadingAuthors = false;
    }
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
  if (overrideCanvas) {
    figureViewer.rotation = shouldAutoRotateFigure(canvas.width, canvas.height) ? 90 : 0;
  }
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
    const [pdfjs, workerModule] = await Promise.all([
      import("pdfjs-dist"),
      import("pdfjs-dist/build/pdf.worker.min.mjs?url"),
    ]);
    pdfjs.GlobalWorkerOptions.workerSrc = workerModule.default;
    const documentSource = await resolvePdfSourceForDocument();
    if (!documentSource) throw new Error("当前文献尚未关联 PDF，或本机缓存不存在");
    const loadingTask = pdfjs.getDocument(documentSource);
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
  structuredDocumentReady.value = parsedPages.length > 0;
  structuredHasAbstract.value = parsedPages.some(page =>
    (Array.isArray(page.blocks) ? page.blocks : []).some(block =>
      /^abstract\b/i.test(String(block.text || "").trim())
    )
  );
  parsedPages.forEach((sourcePage) => {
    const blocks = (Array.isArray(sourcePage.blocks) ? sourcePage.blocks : []).map(block => ({
      ...block,
      text: normalizeKeywordBlockText(block.text || ""),
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
  await nextTick();
  resizeDrawingCanvas();
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
      translatePaperMetadata(true);
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
    translatePaperMetadata();
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
    const normalized = (Array.isArray(providers) ? providers : [])
      .filter(provider => provider?.id && provider?.label)
      .map(provider => ({
        id: provider.id,
        label: provider.label,
        configured: String(provider.configured) === "true"
      }));
    if (normalized.length) translationProviders.value = normalized;
    const selectable = translationProviders.value.filter(provider => provider.configured !== false);
    if (!selectable.some(provider => provider.id === abstractProvider.value)) {
      abstractProvider.value = selectable[0]?.id || "google";
    }
  } catch {
    // 保留内置翻译引擎选项。
  }
}

function captureSelection(event) {
  if (isDrawingPenActive.value) return;
  const selection = window.getSelection();
  if (!selection || selection.isCollapsed || !selection.rangeCount) {
    if (captureBlockSelectionFromEvent(event)) return;
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
    if (captureBlockSelectionFromEvent(event)) return;
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
  selectionTranslator.wasCompacted = false;
  selectionTranslator.error = "";
  selectionTranslator.blockId = paragraphElement?.dataset?.blockId || "";
  selectionTranslator.start = selectionOffsets?.start ?? -1;
  selectionTranslator.end = selectionOffsets?.end ?? -1;
  selectionTranslator.annotating = false;
  selectionTranslator.annotationDraft = "";
  selectionTranslator.editingAnnotationId = "";
  placeSelectionPopover(rect, popoverWidth);
  selectionTranslator.open = true;
  nextTick(() => fitSelectionPopover(false));
}

function captureBlockSelectionFromEvent(event) {
  const target = event?.target;
  const blockElement = target?.closest?.(".mineru-equation.selectable-paragraph");
  if (!blockElement) return false;
  const selectedText = normalizeText(blockElement.dataset.selectionText || blockElement.innerText || "");
  if (!selectedText) return false;
  const rect = blockElement.getBoundingClientRect();
  selectionReady.value = true;
  selectedRange.value = null;
  selectionTranslator.source = selectedText;
  selectionTranslator.paragraph = selectedText;
  selectionTranslator.sentence = selectedText;
  selectionTranslator.preview = selectedText.length > 180 ? `${selectedText.slice(0, 180)}…` : selectedText;
  selectionTranslator.result = "";
  selectionTranslator.resultTitle = "";
  selectionTranslator.loadingText = "正在处理公式…";
  selectionTranslator.wasCompacted = false;
  selectionTranslator.error = "";
  selectionTranslator.blockId = blockElement.dataset.blockId || "";
  selectionTranslator.start = -1;
  selectionTranslator.end = -1;
  selectionTranslator.annotating = false;
  selectionTranslator.annotationDraft = "";
  selectionTranslator.editingAnnotationId = "";
  placeSelectionPopover(rect, 392);
  selectionTranslator.open = true;
  nextTick(() => fitSelectionPopover(false));
  return true;
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

function syncSelectionForToolbarMark() {
  if (selectionTranslator.blockId && selectionTranslator.start >= 0 && selectionTranslator.end > selectionTranslator.start) return true;
  const selection = window.getSelection();
  const range = selectedRange.value || (selection && selection.rangeCount ? selection.getRangeAt(0).cloneRange() : null);
  if (!range || range.collapsed) return false;
  const root = range.commonAncestorContainer.nodeType === Node.ELEMENT_NODE
    ? range.commonAncestorContainer
    : range.commonAncestorContainer.parentElement;
  const paragraphElement = root?.closest?.(".selectable-paragraph")
    || range.startContainer.parentElement?.closest?.(".selectable-paragraph");
  const selectedText = normalizeText(range.toString());
  const selectionOffsets = getSelectionOffsets(paragraphElement, range);
  if (!paragraphElement || !selectedText || !selectionOffsets) return false;
  selectedRange.value = range.cloneRange();
  selectionReady.value = true;
  selectionTranslator.source = selectedText;
  selectionTranslator.preview = selectedText.length > 180 ? `${selectedText.slice(0, 180)}…` : selectedText;
  selectionTranslator.blockId = paragraphElement.dataset?.blockId || "";
  selectionTranslator.start = selectionOffsets.start;
  selectionTranslator.end = selectionOffsets.end;
  return Boolean(selectionTranslator.blockId);
}

function applyToolbarMark(color) {
  selectedColor.value = color;
  if (!["fontColor", "underline", "strike", "wavy"].includes(activeAnnotateTool.value)) {
    activeAnnotateTool.value = "fontColor";
  }
  if (!syncSelectionForToolbarMark()) {
    showReaderToast(`请先选中文本，再添加${textMarkLabel()}标记`);
    return;
  }
  const existingIndex = annotations.findIndex(item =>
    item.type === "mark"
    && item.blockId === selectionTranslator.blockId
    && item.start === selectionTranslator.start
    && item.end === selectionTranslator.end
  );
  const mark = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    type: "mark",
    blockId: selectionTranslator.blockId,
    preview: selectionTranslator.source.slice(0, 40),
    quote: selectionTranslator.source,
    start: selectionTranslator.start,
    end: selectionTranslator.end,
    note: "",
    style: activeAnnotateTool.value,
    color,
    createdAt: new Date().toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" }),
  };
  if (existingIndex >= 0) annotations.splice(existingIndex, 1, mark);
  else annotations.push(mark);
  persistAnnotations();
  textMarkPanelOpen.value = "";
  closeSelectionTranslator();
  window.getSelection()?.removeAllRanges();
  selectedRange.value = null;
  selectionReady.value = false;
  selectionTranslator.blockId = "";
  selectionTranslator.start = -1;
  selectionTranslator.end = -1;
  showReaderToast(`已添加${textMarkLabel()}标记`);
}

function applySelectionColor(color) {
  activeAnnotateTool.value = "fontColor";
  applyToolbarMark(color);
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

function prepareTourStepState(index) {
  if (index <= 3) {
    assistantCollapsed.value = false;
    assistantTab.value = "chat";
  }
}

function nextTourStep() {
  if (readerTour.index >= tourSteps.length - 1) {
    finishTour();
    return;
  }
  readerTour.index += 1;
  prepareTourStepState(readerTour.index);
  nextTick(updateTourRect);
}

function previousTourStep() {
  readerTour.index = Math.max(0, readerTour.index - 1);
  prepareTourStepState(readerTour.index);
  nextTick(updateTourRect);
}

function finishTour() {
  readerTour.open = false;
  localStorage.setItem("papersolver-reader-tour-v3", "done");
}

function relaunchReaderTour() {
  assistantCollapsed.value = false;
  assistantExpanded.value = false;
  assistantTab.value = "chat";
  readerTour.open = true;
  readerTour.index = 0;
  nextTick(updateTourRect);
}

function startReaderTour() {
  if (localStorage.getItem("papersolver-reader-tour-v3") === "done") return;
  relaunchReaderTour();
}

function resetReaderDocumentState() {
  closeSelectionTranslator();
  closeColorMenu();
  window.getSelection()?.removeAllRanges();
  selectedRange.value = null;
  selectionReady.value = false;
  activeInkStroke = null;
  drawingStrokes.splice(0);
  pinnedScreenshots.splice(0);
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
  structuredDocumentReady.value = false;
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
  loadDrawingStrokes();
  await nextTick();
  if (autoTranslate.value) translatePaperMetadata(true);
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
  if (scrollable <= 0 || container.scrollTop + container.clientHeight >= container.scrollHeight - 12) {
    readingProgress.value = 100;
  } else {
    readingProgress.value = Math.min(100, Math.max(0, Math.round((container.scrollTop / scrollable) * 100)));
  }
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

function handleReaderResize() {
  updateTourRect();
  resizeDrawingCanvas();
  if (paperChat.open && !paperChatWindow.userMoved) {
    resetPaperChatWindowPosition();
  }
}

onMounted(async () => {
  applyTheme(currentTheme.value);
  await libraryStore.hydrateLibrary();
  if (activePaper.value) rememberLastReading(authStore.session.user, activePaper.value);
  if (route.query.panel === "analysis") {
    assistantTab.value = "chat";
    assistantExpanded.value = true;
  }
  await loadTranslationProviders();
  if (autoTranslate.value) translatePaperMetadata();
  loadAnnotations();
  loadDrawingStrokes();
  loadStructuredDocument();
  window.addEventListener("resize", handleReaderResize);
  window.setTimeout(startReaderTour, 900);
});

onBeforeUnmount(() => {
  destroyed = true;
  clearTimeout(readerToastTimer);
  clearTimeout(noteSyncTimer);
  destroyMindMap();
  if (pdfObjectUrl) URL.revokeObjectURL(pdfObjectUrl);
  mineruAssetUrls.forEach(url => URL.revokeObjectURL(url));
  window.removeEventListener("resize", handleReaderResize);
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
  --reader-tabbar-height: 34px;
  height: 100vh;
  overflow: hidden;
  background: var(--reader-canvas);
  color: #20242c;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif;
}

.reader-header-wrapper {
  position: relative;
  z-index: 120;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.reader-toolbar-row {
  position: relative;
  height: 38px;
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr) 300px;
  align-items: center;
  box-sizing: border-box;
  transition: grid-template-columns 200ms cubic-bezier(.22, 1, .36, 1);
}

.reader-toolbar-row.assistant-collapsed {
  grid-template-columns: 46px minmax(0, 1fr) 300px;
}

.reader-toolbar-row.assistant-wide {
  grid-template-columns: clamp(440px, 38vw, 580px) minmax(440px, 1fr) 300px;
}

.reader-toolbar-row.right-notes-closed {
  grid-template-columns: 340px minmax(0, 1fr) 44px;
}

.reader-toolbar-row.assistant-collapsed.right-notes-closed {
  grid-template-columns: 46px minmax(0, 1fr) 44px;
}

.reader-toolbar-row.assistant-wide.right-notes-closed {
  grid-template-columns: clamp(440px, 38vw, 580px) minmax(440px, 1fr) 44px;
}

.toolbar-sidebar-spacer {
  min-width: 0;
  height: 100%;
}

.toolbar-stage-area {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-width: 0;
  padding: 0 12px;
  box-sizing: border-box;
}

.toolbar-notes-spacer {
  min-width: 0;
  height: 100%;
}

/* 核心集中控制总成 Dock */
.toolbar-center-dock {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  max-width: 100%;
  min-width: 0;
  overflow: visible;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(241, 245, 249, 0.9);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.7), 0 2px 10px rgba(15, 23, 42, 0.05);
}

.dock-zoom-widget {
  display: flex;
  align-items: center;
  gap: 3px;
  flex: 0 0 auto;
  background: #ffffff;
  padding: 2px 4px;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
}

.dock-zoom-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: #f1f5f9;
  color: #0f172a;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dock-zoom-btn svg {
  stroke-width: 2.8;
  color: #0f172a;
}

.dock-zoom-btn:hover {
  background: #6366f1;
  color: #ffffff;
}

.dock-zoom-btn:hover svg {
  color: #ffffff;
}

.dock-scale-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border: none;
  background: transparent;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.dock-style-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}

.dock-color-more-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-left: 2px;
  border: 1px solid rgba(203, 213, 225, 0.8);
  border-radius: 50%;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  transition: all 0.18s ease;
}

.dock-color-more-btn:hover,
.dock-color-more-btn.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.dock-style-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 220;
  width: 280px;
  padding: 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
  box-sizing: border-box;
}

.style-popover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.style-popover-head strong {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.style-tool-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
}

.style-section {
  margin-bottom: 10px;
}

.style-section label,
.style-custom-color-row span {
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
}

.style-color-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  margin-top: 6px;
}

.style-color-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.style-color-dot:hover,
.style-color-dot.active {
  transform: scale(1.2);
  border-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.5);
}

.style-custom-color-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.style-picker-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #ffffff;
  color: #334155;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}

.color-preview-circle {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.1);
}

.hidden-color-input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
  width: 1px;
  height: 1px;
}

.style-divider {
  height: 1px;
  background: rgba(226, 232, 240, 0.8);
  margin: 10px 0;
}

.style-control-row {
  margin-bottom: 10px;
}

.label-with-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: #64748b;
  font-weight: 600;
  margin-bottom: 5px;
}

.label-with-value output {
  font-weight: 700;
  color: #0f172a;
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
  border-radius: 50%;
  flex-shrink: 0;
  transition: all 0.15s ease;
}

:root[data-theme="dark"] .dock-zoom-widget {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.15);
}

:root[data-theme="dark"] .dock-zoom-btn {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

:root[data-theme="dark"] .dock-zoom-btn svg {
  color: #ffffff;
}

:root[data-theme="dark"] .dock-zoom-btn:hover {
  background: #818cf8;
  color: #0f172a;
}

:root[data-theme="dark"] .dock-zoom-btn:hover svg {
  color: #0f172a;
}

:root[data-theme="dark"] .dock-scale-chip {
  color: #f8fafc;
}

:root[data-theme="dark"] .dock-style-popover {
  background: rgba(15, 23, 42, 0.96);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.7);
}

:root[data-theme="dark"] .style-popover-head strong,
:root[data-theme="dark"] .label-with-value output {
  color: #f8fafc;
}

:root[data-theme="dark"] .style-picker-trigger {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  color: #f8fafc;
}

.zoom-presets-popover {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  width: 140px;
  padding: 6px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.16);
  box-sizing: border-box;
}

.zoom-popover-head {
  padding: 4px 8px 6px;
  font-size: 10.5px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  margin-bottom: 4px;
}

.zoom-preset-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.zoom-preset-item:hover {
  background: rgba(241, 245, 249, 0.9);
  color: #0f172a;
}

.zoom-preset-item.active {
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
  font-weight: 700;
}

.preset-divider {
  height: 1px;
  background: rgba(226, 232, 240, 0.8);
  margin: 4px 0;
}

.fit-width-item {
  justify-content: flex-start;
  gap: 6px;
  color: #6366f1;
}

:root[data-theme="dark"] .zoom-presets-popover {
  background: rgba(15, 23, 42, 0.96);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
}

:root[data-theme="dark"] .zoom-preset-item {
  color: #94a3b8;
}

:root[data-theme="dark"] .zoom-preset-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

:root[data-theme="dark"] .zoom-preset-item.active {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}

.dock-divider {
  flex: 0 0 auto;
  width: 1px;
  height: 16px;
  background: rgba(203, 213, 225, 0.8);
  margin: 0 4px;
}

.dock-actions-group {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 0 0 auto;
}

/* ⚡ 即时 0 延迟浮动解释提示框 (Instant Zero-Delay Tooltips) */
.instant-tooltip {
  position: relative;
}

.instant-tooltip::after {
  content: attr(data-tip);
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%) translateY(4px) scale(0.95);
  padding: 4px 9px;
  border-radius: 6px;
  background: #0f172a;
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transition: opacity 0.08s ease, transform 0.08s ease, visibility 0.08s ease;
  z-index: 999;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
}

.instant-tooltip:hover::after {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0) scale(1);
}

.dock-tool-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: 0 0 auto;
  height: 28px;
  padding: 0 11px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}

.reader-toolbar-row.right-notes-open .toolbar-center-dock,
.reader-toolbar-row.assistant-wide .toolbar-center-dock {
  gap: 2px;
  padding-inline: 6px;
}

.reader-toolbar-row.right-notes-open .dock-tool-btn,
.reader-toolbar-row.assistant-wide .dock-tool-btn {
  width: 30px;
  justify-content: center;
  gap: 0;
  padding-inline: 0;
}

.reader-toolbar-row.right-notes-open .dock-tool-btn > span:not(.mark-letter),
.reader-toolbar-row.assistant-wide .dock-tool-btn > span:not(.mark-letter) {
  display: none;
}

.reader-toolbar-row.right-notes-open .translate-pill-btn,
.reader-toolbar-row.assistant-wide .translate-pill-btn {
  width: 34px;
  justify-content: center;
  padding-inline: 0;
}

.reader-toolbar-row.right-notes-open .translate-pill-btn span:not(.lang-mark),
.reader-toolbar-row.assistant-wide .translate-pill-btn span:not(.lang-mark) {
  display: none;
}

.reader-toolbar-row.right-notes-open .dock-color-swatches,
.reader-toolbar-row.assistant-wide .dock-color-swatches {
  gap: 4px;
  padding-inline: 3px;
}

.dock-tool-btn:hover {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.6);
}

.dock-tool-btn.active {
  background: #ffffff;
  color: #4f46e5;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.18);
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

/* 调色盘色点 */
.dock-color-swatches {
  display: flex;
  align-items: center;
  gap: 5px;
  flex: 0 0 auto;
  padding: 0 4px 0 6px;
}

.dock-color-dot {
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: var(--dot-color);
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.16s ease;
}

.dock-color-dot:hover,
.dock-color-dot.active {
  transform: scale(1.25);
  border-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.5);
}

/* 通用图标按钮 */
.icon-tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.18s ease;
}

.icon-tool-btn:hover:not(:disabled) {
  background: rgba(241, 245, 249, 0.9);
  color: #0f172a;
}

.icon-tool-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.undo-action-btn:hover:not(:disabled) {
  color: #6366f1;
  background: rgba(99, 102, 241, 0.1);
  transform: rotate(-15deg);
}

.clear-action-btn:hover:not(:disabled) {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.translate-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
  animation: immersive-translate-blink 1.55s ease-in-out infinite;
}

.translate-pill-btn:hover {
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
}

.translate-pill-btn.active {
  color: #4f46e5;
  font-weight: 700;
  background: rgba(99, 102, 241, 0.12);
}

.translate-pill-btn .lang-mark {
  font-size: 11px;
  font-weight: 800;
  color: #6366f1;
}

@keyframes immersive-translate-blink {
  0%, 100% {
    box-shadow: 0 0 0 rgba(99, 102, 241, 0);
    opacity: 0.78;
  }
  50% {
    box-shadow: 0 0 18px rgba(99, 102, 241, 0.34);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .translate-pill-btn {
    animation: none;
  }
}

.download-pdf-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border: none;
  border-radius: 8px;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.08);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.download-pdf-pill-btn:hover {
  background: rgba(99, 102, 241, 0.16);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.15);
}

/* 导航栏底部的微光阅读进度条 */
.reader-progress-track-bottom {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2.5px;
  background: rgba(226, 232, 240, 0.5);
  overflow: hidden;
}

.reader-progress-fill-bottom {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #3b82f6, #10b981);
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.6);
  transition: width 0.15s ease-out;
}

/* 🌙 暗色模式适配 */
:root[data-theme="dark"] .reader-header-wrapper {
  background: rgba(15, 23, 42, 0.9);
  border-bottom-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}

:root[data-theme="dark"] .toolbar-center-dock {
  background: rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .dock-tool-btn {
  color: #94a3b8;
}

:root[data-theme="dark"] .dock-tool-btn:hover {
  color: #f8fafc;
  background: rgba(255, 255, 255, 0.1);
}

:root[data-theme="dark"] .dock-tool-btn.active {
  background: #1e293b;
  color: #818cf8;
  box-shadow: 0 2px 10px rgba(129, 140, 248, 0.25);
}

:root[data-theme="dark"] .icon-tool-btn {
  color: #94a3b8;
}

:root[data-theme="dark"] .icon-tool-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

:root[data-theme="dark"] .download-pdf-pill-btn {
  color: #a5b4fc;
  background: rgba(99, 102, 241, 0.16);
}

:root[data-theme="dark"] .download-pdf-pill-btn:hover {
  background: rgba(99, 102, 241, 0.28);
  color: #c7d2fe;
}

.reader-tab-tool {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #303640;
  cursor: pointer;
  transition: background 0.08s ease, color 0.08s ease, box-shadow 0.08s ease;
}

.reader-tab-tool:hover,
.reader-tab-tool.active {
  background: #dfe6ee;
  color: #172033;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.4);
}

.mark-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 24px;
  color: currentColor;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.mark-icon::after {
  position: absolute;
  left: 2px;
  right: 2px;
  content: "";
}

.font-color-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mark-underline::after {
  bottom: 1px;
  height: 2px;
  background: currentColor;
}

.mark-strike::after {
  top: 12px;
  height: 2px;
  background: currentColor;
}

.mark-wavy::after {
  bottom: 0;
  height: 5px;
  background:
    radial-gradient(circle at 3px 4px, transparent 3px, currentColor 3.3px, currentColor 4.2px, transparent 4.5px)
    0 0 / 8px 5px repeat-x;
}

.mark-tool-wrap {
  position: relative;
  display: inline-flex;
}

.mark-popover {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  z-index: 95;
  width: 286px;
  padding: 10px 12px 12px;
  border: 1px solid rgba(203, 213, 225, 0.82);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14), 0 2px 8px rgba(15, 23, 42, 0.08);
  color: #2f333a;
  transform: translateX(-50%);
}

.mark-popover-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #20242c;
}

.mark-popover-head .mark-icon {
  width: 24px;
  height: 24px;
  font-size: 22px;
}

.mark-popover-head strong {
  font-size: 13px;
  font-weight: 750;
}

.mark-color-grid {
  display: grid;
  grid-template-columns: repeat(7, 24px);
  justify-content: space-between;
  row-gap: 10px;
}

.mark-popover-hint {
  margin-top: 10px;
  color: #8a94a3;
  font-size: 11px;
  font-weight: 600;
}

.tooltip-fast {
  position: relative;
}

.tooltip-fast::after,
.tooltip-fast::before {
  position: absolute;
  left: 50%;
  z-index: 140;
  opacity: 0;
  pointer-events: none;
  transform: translate(-50%, 4px);
  transition: opacity 0.04s linear, transform 0.04s linear;
}

.tooltip-fast::after {
  top: calc(100% + 10px);
  content: attr(data-tip);
  white-space: nowrap;
  padding: 7px 10px;
  border-radius: 5px;
  background: #22242a;
  color: #ffffff;
  font-size: 13px;
  font-weight: 650;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.24);
}

.tooltip-fast::before {
  top: calc(100% + 4px);
  content: "";
  border-right: 6px solid transparent;
  border-bottom: 6px solid #22242a;
  border-left: 6px solid transparent;
}

.tooltip-fast:hover::after,
.tooltip-fast:hover::before {
  opacity: 1;
  transform: translate(-50%, 0);
}

.natural-tool-divider {
  width: 1px;
  height: 16px;
  background: rgba(226, 232, 240, 0.8);
  margin: 0 4px;
}

.natural-draw-tools {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.brush-tool-wrap {
  position: relative;
  display: inline-flex;
}

.move-tool-btn svg,
.natural-pen-btn svg {
  flex: 0 0 auto;
}

.cursor-tool-icon {
  stroke-width: 2.05;
}

/* 画笔涂鸦按钮 */
.natural-pen-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.natural-pen-btn:hover {
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
}

.natural-pen-btn.active {
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  color: #ffffff;
  box-shadow: 0 3px 10px rgba(99, 102, 241, 0.3);
}

.brush-popover {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 90;
  width: 286px;
  padding: 10px 12px 12px;
  border: 1px solid rgba(203, 213, 225, 0.82);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14), 0 2px 8px rgba(15, 23, 42, 0.08);
  color: #2f333a;
}

.brush-tool-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  align-items: center;
}

.brush-tool-choice {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 38px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #20242c;
  cursor: pointer;
  transition: background 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.brush-tool-choice:hover,
.brush-tool-choice.active {
  background: #dce3eb;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.35);
}

.brush-tool-choice:active {
  transform: scale(0.96);
}

.brush-tool-choice svg {
  width: 25px;
  height: 25px;
}

.brush-squiggle {
  position: absolute;
  left: 10px;
  bottom: 6px;
  width: 22px;
  height: 3px;
  border-radius: 999px;
  transform: rotate(-8deg);
}

.brush-divider {
  height: 1px;
  margin: 10px 4px 12px;
  background: #cfd6de;
}

.brush-color-grid {
  display: grid;
  grid-template-columns: repeat(7, 24px);
  justify-content: space-between;
  row-gap: 10px;
}

.brush-color-dot {
  width: 24px;
  height: 24px;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.14s ease, box-shadow 0.14s ease, border-color 0.14s ease;
}

.brush-color-dot.light {
  border-color: #d3d8df;
}

.brush-color-dot:hover,
.brush-color-dot.active {
  transform: scale(1.04);
  border-color: #ffffff;
  box-shadow: 0 0 0 3px #2f333a, 0 3px 10px rgba(15, 23, 42, 0.2);
}

.brush-custom-row {
  position: relative;
  display: grid;
  grid-template-columns: auto 30px 1fr;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #2f333a;
}

.brush-plus-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 2px solid #2f333a;
  border-radius: 50%;
  background: #ffffff;
  color: #2f333a;
  cursor: pointer;
}

.brush-native-picker {
  position: absolute;
  left: 0;
  bottom: 0;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.brush-control {
  margin-top: 12px;
}

.brush-control label {
  display: block;
  margin-bottom: 7px;
  font-size: 14px;
  font-weight: 650;
  color: #2f333a;
}

.brush-slider-row {
  display: grid;
  grid-template-columns: 1fr 62px;
  gap: 10px;
  align-items: center;
}

.brush-slider-row input[type="range"] {
  width: 100%;
  accent-color: #4f7ec8;
}

.brush-slider-row output {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 34px;
  border: 1px solid #cfd6de;
  border-radius: 7px;
  background: #f8fafc;
  color: #2f333a;
  font-size: 15px;
  font-weight: 500;
}

/* 通用无框图标按钮 */
.natural-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.18s ease;
}

.natural-icon-btn:hover:not(:disabled) {
  background: rgba(226, 232, 240, 0.6);
  color: #0f172a;
}

.natural-icon-btn.active {
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
}

.natural-icon-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.natural-scale-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 7px;
  background: rgba(248, 250, 252, 0.82);
  color: #475569;
  font-size: 12px;
  font-weight: 750;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.natural-scale-chip:hover {
  border-color: rgba(99, 102, 241, 0.38);
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
}

.undo-btn {
  color: #6366f1;
}

.undo-btn:hover:not(:disabled) {
  background: rgba(99, 102, 241, 0.1);
  color: #4338ca;
  transform: rotate(-15deg);
}

/* 调色盘色点组 */
.natural-color-swatches {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 4px;
}

.natural-color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--swatch-color);
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.18s ease;
}

.natural-color-dot:hover,
.natural-color-dot.active {
  transform: scale(1.25);
  border-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.5);
}

/* 全文翻译与 Engine 组 */
.natural-translate-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.natural-translate-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.natural-translate-btn:hover {
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
}

.natural-translate-btn.active {
  color: #4f46e5;
  font-weight: 700;
}

.natural-translate-btn .lang-icon {
  font-size: 11px;
  font-weight: 800;
  color: #6366f1;
}

.natural-provider-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: background 0.15s;
}

.natural-provider-wrapper:hover {
  background: rgba(226, 232, 240, 0.6);
  color: #0f172a;
}

.provider-text.google-color { color: #4285F4; }
.provider-text.baidu-color { color: #2932e1; }

/* 导航栏底部的微光阅读进度条 */
.reader-progress-track-bottom {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 3px;
  background: rgba(226, 232, 240, 0.5);
  overflow: hidden;
}

.reader-progress-fill-bottom {
  height: 100%;
  background: linear-gradient(90deg, #6366f1, #3b82f6, #10b981);
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.6);
  transition: width 0.15s ease-out;
}

/* 🌙 暗色模式 */
:root[data-theme="dark"] .reader-toolbar.natural-toolbar {
  background: rgba(15, 23, 42, 0.9);
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .reader-tab-center-tools {
  border-color: rgba(148, 163, 184, 0.18);
  background: rgba(15, 23, 42, 0.9);
  box-shadow: 0 1px 12px rgba(0, 0, 0, 0.28);
}

:root[data-theme="dark"] .reader-tab-tool {
  color: #cbd5e1;
}

:root[data-theme="dark"] .reader-tab-tool:hover,
:root[data-theme="dark"] .reader-tab-tool.active {
  background: rgba(148, 163, 184, 0.2);
  color: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.14);
}

:root[data-theme="dark"] .toolbar-color-dot:hover,
:root[data-theme="dark"] .toolbar-color-dot.active {
  box-shadow: 0 0 0 2px #0f172a, 0 0 0 4px rgba(248, 250, 252, 0.78);
}

:root[data-theme="dark"] .natural-pen-btn {
  color: #cbd5e1;
}

:root[data-theme="dark"] .natural-pen-btn:hover {
  background: rgba(99, 102, 241, 0.15);
  color: #a5b4fc;
}

:root[data-theme="dark"] .brush-popover {
  border-color: rgba(148, 163, 184, 0.34);
  background: rgba(15, 23, 42, 0.98);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.46);
  color: #f8fafc;
}

:root[data-theme="dark"] .mark-popover {
  border-color: rgba(148, 163, 184, 0.34);
  background: rgba(15, 23, 42, 0.98);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.46);
  color: #f8fafc;
}

:root[data-theme="dark"] .mark-popover-head {
  color: #f8fafc;
}

:root[data-theme="dark"] .mark-popover-hint {
  color: #94a3b8;
}

:root[data-theme="dark"] .brush-tool-choice {
  color: #f8fafc;
}

:root[data-theme="dark"] .brush-tool-choice:hover,
:root[data-theme="dark"] .brush-tool-choice.active {
  background: rgba(148, 163, 184, 0.22);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.14);
}

:root[data-theme="dark"] .brush-choice-label {
  color: #cbd5e1;
}

:root[data-theme="dark"] .brush-divider {
  background: rgba(226, 232, 240, 0.18);
}

:root[data-theme="dark"] .brush-color-dot:hover,
:root[data-theme="dark"] .brush-color-dot.active {
  border-color: #0f172a;
  box-shadow: 0 0 0 3px #f8fafc, 0 4px 14px rgba(0, 0, 0, 0.34);
}

:root[data-theme="dark"] .brush-custom-row,
:root[data-theme="dark"] .brush-control label {
  color: #f8fafc;
}

:root[data-theme="dark"] .brush-plus-btn,
:root[data-theme="dark"] .brush-slider-row output {
  border-color: rgba(226, 232, 240, 0.24);
  background: rgba(15, 23, 42, 0.88);
  color: #f8fafc;
}

:root[data-theme="dark"] .natural-scale-chip {
  border-color: rgba(226, 232, 240, 0.14);
  background: rgba(15, 23, 42, 0.76);
  color: #cbd5e1;
}

:root[data-theme="dark"] .natural-scale-chip:hover {
  border-color: rgba(129, 140, 248, 0.4);
  background: rgba(99, 102, 241, 0.18);
  color: #f8fafc;
}

:root[data-theme="dark"] .natural-icon-btn {
  color: #94a3b8;
}

:root[data-theme="dark"] .natural-icon-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

:root[data-theme="dark"] .undo-btn {
  color: #818cf8;
}

:root[data-theme="dark"] .reader-progress-track-bottom {
  background: rgba(255, 255, 255, 0.06);
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
  height: calc(100vh - var(--reader-toolbar-height) - var(--reader-tabbar-height));
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr) 300px;
  min-height: 0;
  background:
    linear-gradient(90deg, #f8fafc 0, #f7f9fc 300px, #f4f7fb 430px, #edf3f8 100%);
  transition: grid-template-columns 200ms cubic-bezier(.22, 1, .36, 1);
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
.reader-body:has(.reader-assistant.collapsed) { grid-template-columns: 46px minmax(0, 1fr) 300px; }
.reader-body.assistant-wide { grid-template-columns: clamp(440px, 38vw, 580px) minmax(440px, 1fr) 300px; }

.reader-body.right-notes-closed {
  grid-template-columns: 340px minmax(0, 1fr) 44px;
}
.reader-body.right-notes-closed:has(.reader-assistant.collapsed) {
  grid-template-columns: 46px minmax(0, 1fr) 44px;
}
.reader-body.right-notes-closed.assistant-wide {
  grid-template-columns: clamp(440px, 38vw, 580px) minmax(440px, 1fr) 44px;
}
.reader-assistant.expanded { width: auto; }

.assistant-tabs {
  height: 52px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  flex: 0 0 auto;
}

.assistant-tabs button {
  height: 36px;
  min-width: 52px;
  padding: 0 10px;
  border: 0;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  background: transparent;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
  writing-mode: horizontal-tb;
  word-break: keep-all;
  line-height: 1;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.assistant-tabs button:hover {
  color: #1e293b;
  background: transparent;
  border-bottom-color: rgba(37, 99, 235, 0.24);
}

.assistant-tabs button.active {
  color: #2563eb;
  background: transparent;
  border-bottom-color: #2563eb;
  box-shadow: none;
}

/* Left sidebar expand/collapse buttons */
.assistant-tabs .icon-button {
  width: 34px;
  height: 34px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  color: #64748b;
  background: rgba(241, 245, 249, 0.6);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.assistant-tabs .icon-button:hover {
  color: #0f172a;
  background: #e2e8f0;
}
.expand-button { margin-left: auto; }
.expand-button:hover, .collapse-button:hover {
  background: rgba(255, 255, 255, 1) !important;
  color: #0f172a !important;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transform: translateY(-1px);
}
.expand-button:active, .collapse-button:active {
  transform: translateY(0);
  box-shadow: none;
}
.collapse-button { margin-left: 2px; }
.reader-assistant.collapsed .assistant-tabs button:not(.collapse-button) { display: none; }
.reader-assistant:not(.expanded) .assistant-tabs {
  gap: 4px;
  padding-inline: 10px;
}

.reader-assistant:not(.expanded) .assistant-tabs button:not(.icon-button) {
  min-width: 50px;
  padding-inline: 6px;
  font-size: 12px;
  letter-spacing: 0;
}

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
  position: relative;
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

.reader-drawing-layer {
  position: absolute;
  z-index: 46;
  display: block;
  pointer-events: none;
  touch-action: none;
}

.reader-drawing-layer.active {
  cursor: crosshair;
  pointer-events: auto;
}

.pinned-screenshot-dock {
  position: fixed;
  inset: 0;
  z-index: 90;
  pointer-events: none;
}

.pinned-screenshot-card {
  position: fixed;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.24);
  pointer-events: auto;
}

.pinned-screenshot-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 28px;
  padding: 0 8px 0 10px;
  border-bottom: 1px solid #e5eaf1;
  color: #334155;
  font-size: 11px;
  font-weight: 750;
}

.pinned-screenshot-card header button {
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.pinned-screenshot-card header button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.pinned-screenshot-card img {
  display: block;
  width: 100%;
  max-height: min(62vh, 560px);
  object-fit: contain;
  background: #f8fafc;
}

.paper-heading {
  padding-bottom: 24px;
  border-bottom: 1px solid #d9dee5;
}

.paper-source { color: #1769e0; font-size: 12px; }
.paper-heading h1 { max-width: 900px; margin: 10px 0 12px; font: 700 1.75em/1.35 "Times New Roman", "Songti SC", serif; }
.paper-heading p { margin: 0; color: #667085; font-size: 0.82em; }
.paper-authors-line {
  max-width: 980px;
  color: #2f7fa9 !important;
  font-family: Georgia, "Times New Roman", "Songti SC", serif;
  font-size: clamp(15px, 1.18vw, 19px) !important;
  font-weight: 500;
  line-height: 1.5;
}
.author-name-text { color: #2f7fa9; }
.author-affiliation-sup {
  margin-inline: 2px;
  color: #2f7fa9;
  font-size: 0.48em;
  font-weight: 650;
  line-height: 0;
  vertical-align: super;
}
.author-orcid-badge {
  display: inline-grid;
  place-items: center;
  width: 0.9em;
  height: 0.9em;
  margin: 0 0.08em;
  border-radius: 999px;
  color: #ffffff;
  background: #95c93d;
  font: 700 0.48em/1 Arial, sans-serif;
  vertical-align: super;
}

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

.source-heading.abstract-heading {
  color: #1f2937;
  font-size: 0.95em;
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
.selectable-paragraph::selection { color: #132136; background: rgba(98, 123, 255, .16); }
.annotation-highlight {
  position: relative;
  padding: 0 .08em .08em;
  border-radius: 3px;
  background: transparent;
  box-shadow: none;
  cursor: pointer;
}
.annotation-highlight:hover {
  background: color-mix(in srgb, var(--mark-color, #527ce0) 10%, transparent);
}
.annotation-highlight.mark-fontColor {
  color: var(--mark-color, inherit);
}
.annotation-highlight.mark-underline {
  text-decoration-line: underline;
  text-decoration-color: var(--mark-color, #527ce0);
  text-decoration-thickness: 2px;
  text-underline-offset: 0.18em;
}
.annotation-highlight.mark-strike {
  text-decoration-line: line-through;
  text-decoration-color: var(--mark-color, #527ce0);
  text-decoration-thickness: 2px;
}
.annotation-highlight.mark-wavy {
  text-decoration-line: underline;
  text-decoration-style: wavy;
  text-decoration-color: var(--mark-color, #527ce0);
  text-decoration-thickness: 1.5px;
  text-underline-offset: 0.2em;
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
.annotation-inline-note {
  display: inline-flex;
  max-width: min(240px, 40vw);
  align-items: center;
  margin: 0 4px 0 5px;
  padding: 2px 8px 3px;
  vertical-align: 1px;
  border: 1px solid rgba(99, 102, 241, .18);
  border-radius: 999px;
  color: #42526f;
  background: rgba(248, 250, 255, .96);
  box-shadow: 0 1px 4px rgba(46, 60, 96, .08);
  font: 600 10px/1.45 Inter, "PingFang SC", sans-serif;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.annotation-inline-note:hover {
  color: #25345c;
  border-color: rgba(79, 70, 229, .32);
  background: #fff;
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
  width: max-content;
  max-width: min(720px, calc(100vw - 28px));
  max-height: calc(100dvh - 28px);
  overflow: visible;
  color: #263244;
  filter: drop-shadow(0 18px 34px rgba(15, 23, 42, .18));
}
.selection-command-bar {
  display: flex;
  align-items: center;
  gap: 7px;
  width: max-content;
  max-width: min(720px, calc(100vw - 28px));
  padding: 7px;
  border: 1px solid rgba(148, 163, 184, .32);
  border-radius: 999px;
  color: #eef2f7;
  background: rgba(20, 28, 24, .94);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .06);
  backdrop-filter: blur(16px);
}
.selection-translate-popover.is-below .selection-command-bar::before,
.selection-translate-popover.is-above .selection-command-bar::after {
  position: absolute;
  left: 50%;
  width: 12px;
  height: 12px;
  content: "";
  background: rgba(20, 28, 24, .94);
  transform: translateX(-50%) rotate(45deg);
}
.selection-translate-popover.is-below .selection-command-bar::before { top: -4px; }
.selection-translate-popover.is-above .selection-command-bar::after { bottom: -4px; }
.selection-command-bar > button {
  position: relative;
  z-index: 1;
  height: 38px;
  padding: 0 16px;
  border: 1px solid rgba(126, 163, 133, .45);
  border-radius: 999px;
  color: #f8fafc;
  background: rgba(255, 255, 255, .045);
  font-size: 14px;
  font-weight: 760;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 140ms ease, border-color 140ms ease, background 140ms ease;
}
.selection-command-bar > button:hover { transform: translateY(-1px); border-color: rgba(154, 210, 165, .7); background: rgba(255, 255, 255, .1); }
.selection-command-bar > button:disabled { opacity: .48; cursor: default; transform: none; }
.selection-command-bar > button:last-child {
  width: 30px;
  height: 30px;
  padding: 0;
  border-color: transparent;
  color: #cbd5e1;
  background: rgba(255, 255, 255, .08);
  font-size: 16px;
}
.selection-provider-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: fit-content;
  min-width: 260px;
  margin: 6px auto 0;
  padding: 8px 10px;
  border: 1px solid rgba(31, 42, 61, 0.08);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.1);
}
.selection-provider-panel select {
  min-width: 132px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid #d7dee9;
  border-radius: 8px;
  color: #1f2937;
  background: #ffffff;
  font-size: 11px;
  font-weight: 700;
}
.selection-note-action { color: #f6fbf7 !important; }
.selection-mark-dots {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 4px 0 7px;
}
.selection-mark-dots button {
  position: relative;
  width: 18px;
  min-width: 18px;
  height: 18px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, .28);
  border-radius: 50%;
  background: rgba(255, 255, 255, .9);
  cursor: pointer;
}
.selection-mark-dots button::after { position: absolute; inset: 4px; border-radius: 50%; content: ""; background: var(--swatch); }
.selection-result {
  width: min(420px, calc(100vw - 28px));
  max-height: min(46vh, 360px);
  display: flex;
  align-items: flex-start;
  gap: 10px;
  overflow-y: auto;
  margin: 10px auto 0;
  padding: 13px 14px;
  border: 1px solid rgba(203, 213, 225, .78);
  border-radius: 16px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 16px 42px rgba(30, 41, 59, .14);
  backdrop-filter: blur(16px);
}
.selection-result header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}
.selection-result strong { color: #172033; font-size: 13px; font-weight: 800; }
.selection-result small { color: #8a96a7; font-size: 10px; font-weight: 650; white-space: nowrap; }
.selection-result p { margin: 0; color: #243147; font: 12px/1.72 "Songti SC", "STSong", serif; white-space: pre-wrap; }
.selection-result.error p { color: #b42318; }
.selection-compact-note {
  margin: 0 0 7px !important;
  color: #5b6f95 !important;
  font: 11px/1.6 Inter, "PingFang SC", sans-serif !important;
}
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
.selection-annotation-editor {
  width: min(420px, calc(100vw - 28px));
  max-height: min(42vh, 330px);
  overflow-y: auto;
  margin: 10px auto 0;
  padding: 12px;
  border: 1px solid rgba(203, 213, 225, .78);
  border-radius: 16px;
  background: rgba(255, 255, 255, .97);
  box-shadow: 0 16px 42px rgba(30, 41, 59, .14);
  backdrop-filter: blur(16px);
}
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

.apple-ai-launcher,
.paper-chat-launcher {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 82;
  width: auto;
  min-width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border: 1px solid rgba(124, 58, 237, .24);
  border-radius: 999px;
  color: #ffffff;
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 48%, #4f46e5 100%);
  box-shadow: 0 16px 34px rgba(109, 40, 217, .28), 0 0 0 1px rgba(255, 255, 255, .22) inset;
  backdrop-filter: blur(14px);
  cursor: pointer;
  transition: right 200ms cubic-bezier(.22, 1, .36, 1), transform 160ms ease, box-shadow 160ms ease;
}

.apple-ai-launcher:hover,
.paper-chat-launcher:hover {
  color: #ffffff;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 52%, #5b21b6 100%);
  box-shadow: 0 18px 38px rgba(109, 40, 217, .34), 0 0 0 1px rgba(255, 255, 255, .26) inset;
  transform: translateY(-1px);
}

.apple-ai-launcher.expanded,
.paper-chat-launcher.open {
  color: #ffffff;
  background: linear-gradient(135deg, #5b21b6 0%, #6d28d9 54%, #4338ca 100%);
}

.ai-sparkle-halo {
  position: absolute;
  inset: -10px;
  z-index: -1;
  border-radius: inherit;
  background: rgba(124, 58, 237, .18);
  filter: blur(18px);
  pointer-events: none;
}

.ai-sparkle-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ai-label-text {
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: .01em;
}

.apple-ai-launcher.right-notes-open,
.paper-chat-launcher.right-notes-open {
  right: 324px;
}

.apple-ai-launcher.right-notes-closed,
.paper-chat-launcher.right-notes-closed {
  right: 60px;
}
.paper-chat-launcher svg { width: 25px; height: 25px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.paper-chat-launcher > span { font-size: 26px; font-weight: 300; line-height: 1; }
.paper-chat-panel {
  position: fixed;
  left: 50%;
  right: auto;
  top: 92px;
  bottom: auto;
  z-index: 81;
  width: min(1040px, calc(100vw - 96px));
  height: min(820px, calc(100vh - 120px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 12px 48px rgba(15, 23, 42, 0.12), 0 4px 16px rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(24px) saturate(180%);
  transform: translateX(-50%);
  transition: width 200ms cubic-bezier(.22, 1, .36, 1), height 200ms cubic-bezier(.22, 1, .36, 1);
}

.paper-chat-panel.right-notes-open {
  right: auto;
}

.paper-chat-panel.right-notes-closed {
  right: auto;
}
.paper-chat-panel > header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.6), rgba(255, 255, 255, 0.4));
  cursor: grab;
  user-select: none;
}
.paper-chat-panel.dragging > header { cursor: grabbing; }
.paper-chat-mark { width: 36px; height: 36px; display: grid; flex: 0 0 auto; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(135deg, #3b82f6, #6366f1); font: 800 13px/1 Inter, sans-serif; box-shadow: 0 4px 12px rgba(59,130,246,0.25); }
.paper-chat-panel > header > div:last-child { display: grid; gap: 4px; }
.paper-chat-panel > header strong { color: #0f172a; font-size: 15px; font-weight: 700; letter-spacing: 0.2px; }
.paper-chat-panel > header span { max-width: 320px; overflow: hidden; color: #64748b; font-size: 12px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.paper-chat-messages { overflow-y: auto; padding: 24px 32px; background: transparent; display: flex; flex-direction: column; gap: 18px; }
.paper-chat-message { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 0; }
.paper-chat-message.assistant { width: min(860px, 100%); margin-inline: auto; }
.paper-chat-message.user { width: min(860px, 100%); margin-inline: auto; }
.paper-chat-message > span { width: 28px; height: 28px; display: grid; flex: 0 0 auto; place-items: center; border-radius: 10px; color: #2563eb; background: rgba(59,130,246,0.1); font-size: 11px; font-weight: 800; }
.paper-chat-message p { max-width: min(760px, 92%); margin: 0; padding: 12px 16px; border: 1px solid rgba(226, 232, 240, 0.8); border-radius: 16px; border-top-left-radius: 4px; color: #334155; background: rgba(255,255,255,0.7); font-size: 13px; line-height: 1.6; white-space: pre-wrap; box-shadow: 0 2px 8px rgba(0,0,0,0.02); }
.paper-chat-message-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 8px;
}

.paper-chat-message-actions button {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(99, 102, 241, .16);
  border-radius: 9px;
  color: #6366f1;
  background: rgba(99, 102, 241, .08);
  cursor: pointer;
  transition: transform 120ms ease, background 120ms ease, color 120ms ease;
}

.paper-chat-message-actions button:hover {
  color: #ffffff;
  background: #6d28d9;
  transform: translateY(-1px);
}

.paper-chat-message.user { justify-content: flex-end; }
.paper-chat-message.user p { color: #fff; border-color: transparent; border-radius: 16px; border-top-right-radius: 4px; background: linear-gradient(135deg, #2563eb, #4f46e5); box-shadow: 0 4px 12px rgba(37,99,235,0.2); }
.paper-chat-thinking { display: flex; gap: 5px; align-items: center; min-height: 20px; }
.paper-chat-thinking i { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; animation: chat-dot 1s ease-in-out infinite; }
.paper-chat-thinking i:nth-child(2) { animation-delay: .15s; }
.paper-chat-thinking i:nth-child(3) { animation-delay: .3s; }
.paper-chat-panel form { display: grid; grid-template-columns: minmax(0, 1fr) 42px; align-items: end; gap: 12px; margin: 0 auto 16px; width: min(820px, calc(100% - 48px)); padding: 12px; border: 1px solid rgba(226, 232, 240, 0.8); border-radius: 20px; background: rgba(255, 255, 255, 0.6); box-shadow: 0 4px 16px rgba(0,0,0,0.03); }
.paper-chat-panel textarea { min-height: 42px; max-height: 120px; resize: none; box-sizing: border-box; padding: 12px 14px; border: none; border-radius: 12px; outline: 0; color: #1e293b; background: transparent; font: 13px/1.5 inherit; }
.paper-chat-panel textarea::placeholder { color: #94a3b8; }
.paper-chat-panel textarea:focus { background: rgba(255,255,255,0.9); }
.paper-chat-panel form button { width: 42px; height: 42px; display: grid; place-items: center; border: 0; border-radius: 14px; color: #fff; background: #2563eb; font-size: 20px; cursor: pointer; transition: all 0.2s ease; }
.paper-chat-panel form button:hover:not(:disabled) { background: #1d4ed8; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(37,99,235,0.3); }
.paper-chat-panel form button:disabled { opacity: .4; cursor: default; }
.paper-chat-panel > small { padding: 12px 20px 16px; color: #94a3b8; background: transparent; font-size: 11px; text-align: center; }
.paper-chat-enter-active,
.paper-chat-leave-active { transition: opacity 160ms ease, transform 180ms cubic-bezier(.22, 1, .36, 1); transform-origin: bottom right; }
.paper-chat-enter-from,
.paper-chat-leave-to { opacity: 0; transform: translateX(-50%) translateY(8px) scale(.98); }
@keyframes chat-dot { 0%, 60%, 100% { opacity: .35; transform: translateY(0); } 30% { opacity: 1; transform: translateY(-3px); } }

:root[data-theme="dark"] .apple-ai-launcher,
:root[data-theme="dark"] .paper-chat-launcher {
  border-color: rgba(192, 132, 252, 0.3);
  color: #f6f3ff;
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 48%, #4f46e5 100%);
  box-shadow: 0 18px 42px rgba(5, 3, 12, 0.42), 0 0 0 1px rgba(255, 255, 255, 0.12) inset;
}

:root[data-theme="dark"] .apple-ai-launcher:hover,
:root[data-theme="dark"] .paper-chat-launcher:hover {
  color: #ffffff;
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 52%, #5b21b6 100%);
  transform: translateY(-1px);
}

:root[data-theme="dark"] .futuristic-void-panel.paper-chat-panel {
  border-color: rgba(192, 132, 252, 0.24);
  background:
    radial-gradient(circle at 15% 0%, rgba(139, 92, 246, 0.22), transparent 34%),
    linear-gradient(180deg, rgba(24, 18, 43, 0.96), rgba(10, 8, 20, 0.96));
  box-shadow: 0 26px 78px rgba(3, 2, 10, 0.58), 0 0 0 1px rgba(255, 255, 255, 0.04) inset;
}

:root[data-theme="dark"] .futuristic-void-panel .void-chat-header,
:root[data-theme="dark"] .paper-chat-panel > header {
  border-bottom-color: rgba(192, 132, 252, 0.18);
  background:
    linear-gradient(135deg, rgba(109, 40, 217, 0.24), rgba(168, 85, 247, 0.1)),
    rgba(20, 14, 38, 0.82);
}

:root[data-theme="dark"] .futuristic-void-panel .void-main-title strong,
:root[data-theme="dark"] .paper-chat-panel > header strong {
  color: #f6f3ff;
}

:root[data-theme="dark"] .futuristic-void-panel .void-sub-title,
:root[data-theme="dark"] .paper-chat-panel > header span {
  color: #b9a8e8;
}

:root[data-theme="dark"] .futuristic-void-panel .tech-tag-pill,
:root[data-theme="dark"] .futuristic-void-panel .ai-meta-banner {
  border: 1px solid rgba(192, 132, 252, 0.22);
  color: #d8b4fe;
  background: rgba(139, 92, 246, 0.13);
}

:root[data-theme="dark"] .futuristic-void-panel .quantum-ai-avatar,
:root[data-theme="dark"] .futuristic-void-panel .assistant-sparkle-avatar,
:root[data-theme="dark"] .paper-chat-message > span {
  color: #d8b4fe;
  background: rgba(139, 92, 246, 0.18);
  box-shadow: 0 0 18px rgba(139, 92, 246, 0.22);
}

:root[data-theme="dark"] .futuristic-void-panel .void-close-btn {
  color: #c4b5fd;
  background: rgba(255, 255, 255, 0.04);
}

:root[data-theme="dark"] .futuristic-void-panel .void-close-btn:hover {
  color: #ffffff;
  background: rgba(139, 92, 246, 0.2);
}

:root[data-theme="dark"] .futuristic-void-panel .void-chat-body,
:root[data-theme="dark"] .paper-chat-messages {
  background: transparent;
}

:root[data-theme="dark"] .futuristic-void-panel .message-content-wrapper,
:root[data-theme="dark"] .paper-chat-message p {
  border-color: rgba(192, 132, 252, 0.18);
  color: #eee8ff;
  background: rgba(24, 18, 43, 0.72);
  box-shadow: 0 10px 28px rgba(5, 3, 12, 0.22);
}

:root[data-theme="dark"] .paper-chat-message-actions button {
  border-color: rgba(192, 132, 252, .22);
  color: #c4b5fd;
  background: rgba(139, 92, 246, .12);
}

:root[data-theme="dark"] .paper-chat-message-actions button:hover {
  color: #ffffff;
  background: #7c3aed;
}

:root[data-theme="dark"] .paper-chat-message.user p {
  border-color: transparent;
  color: #ffffff;
  background: linear-gradient(135deg, #7c3aed, #a855f7);
  box-shadow: 0 10px 24px rgba(124, 58, 237, 0.28);
}

:root[data-theme="dark"] .futuristic-void-panel .message-text,
:root[data-theme="dark"] .futuristic-void-panel .markdown-rendered {
  color: #eee8ff;
}

:root[data-theme="dark"] .futuristic-void-panel .void-quick-prompts button {
  border: 1px solid rgba(192, 132, 252, 0.18);
  color: #d8ccff;
  background: rgba(139, 92, 246, 0.1);
}

:root[data-theme="dark"] .futuristic-void-panel .void-quick-prompts button:hover {
  border-color: rgba(216, 180, 254, 0.34);
  color: #ffffff;
  background: rgba(139, 92, 246, 0.2);
}

:root[data-theme="dark"] .paper-chat-panel form,
:root[data-theme="dark"] .futuristic-void-panel .void-input-form {
  border-color: rgba(192, 132, 252, 0.2);
  background: rgba(16, 13, 30, 0.72);
  box-shadow: 0 10px 30px rgba(5, 3, 12, 0.22);
}

:root[data-theme="dark"] .paper-chat-panel textarea,
:root[data-theme="dark"] .futuristic-void-panel textarea {
  color: #f6f3ff;
  background: transparent;
}

:root[data-theme="dark"] .paper-chat-panel textarea::placeholder {
  color: #9b8ac7;
}

:root[data-theme="dark"] .paper-chat-panel textarea:focus {
  background: rgba(139, 92, 246, 0.08);
}

:root[data-theme="dark"] .paper-chat-panel form button,
:root[data-theme="dark"] .futuristic-void-panel .cyber-send-btn {
  background: linear-gradient(135deg, #7c3aed, #a855f7);
  box-shadow: 0 10px 22px rgba(124, 58, 237, 0.3);
}

:root[data-theme="dark"] .paper-chat-panel form button:hover:not(:disabled),
:root[data-theme="dark"] .futuristic-void-panel .cyber-send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #8b5cf6, #c084fc);
}

:root[data-theme="dark"] .futuristic-void-panel .void-footer-note,
:root[data-theme="dark"] .paper-chat-panel > small {
  color: #8f7bbd;
}

:root[data-theme="dark"] .paper-chat-thinking i {
  background: #c084fc;
}
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
.reader-tour-card {
  position: absolute;
  z-index: 10;
  left: 50%;
  bottom: 46px;
  width: min(460px, calc(100vw - 32px));
  transform: translateX(-50%);
  padding: 22px 24px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.22), 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.tour-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.tour-step-badge {
  padding: 3px 10px;
  border-radius: 999px;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(129, 140, 248, 0.25);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.tour-skip-btn {
  border: none;
  background: transparent;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  transition: all 0.2s;
}
.tour-skip-btn:hover {
  color: #64748b;
  background: rgba(0, 0, 0, 0.05);
}

@media (max-width: 1100px) {
  .reader-toolbar-row,
  .reader-toolbar-row.right-notes-closed,
  .reader-toolbar-row.assistant-wide,
  .reader-toolbar-row.assistant-wide.right-notes-closed {
    grid-template-columns: 260px minmax(0, 1fr) 190px;
  }

  .reader-toolbar-row.assistant-collapsed,
  .reader-toolbar-row.assistant-collapsed.right-notes-closed {
    grid-template-columns: 46px minmax(0, 1fr) 190px;
  }

  .toolbar-stage-area {
    padding-inline: 8px;
  }

  .reader-toolbar { grid-template-columns: minmax(180px, 1fr) auto auto; gap: 8px; padding-inline: 8px; }
  .reader-document-source { display: none; }
  .reader-status-item small { display: none; }
  .reader-body { grid-template-columns: 260px minmax(0, 1fr) 190px; }
  .reader-body:has(.reader-assistant.collapsed) { grid-template-columns: 46px minmax(0, 1fr) 190px; }
  .reader-body.assistant-wide { grid-template-columns: clamp(360px, 40vw, 460px) minmax(360px, 1fr) 190px; }
  .reading-column { width: calc(100% - 32px); padding: 30px 36px 90px; }
  .block-annotation-note { width: 31px; margin-right: -28px; }
  .block-annotation-note p { display: none; }
}

@media (max-width: 820px) {
  .reader-toolbar-row,
  .reader-toolbar-row.assistant-collapsed,
  .reader-toolbar-row.assistant-wide,
  .reader-toolbar-row.right-notes-closed,
  .reader-toolbar-row.assistant-collapsed.right-notes-closed,
  .reader-toolbar-row.assistant-wide.right-notes-closed {
    grid-template-columns: minmax(0, 1fr);
  }

  .toolbar-sidebar-spacer,
  .toolbar-notes-spacer {
    display: none;
  }

  .toolbar-center-dock {
    justify-content: flex-start;
    width: 100%;
  }

  .reader-toolbar { grid-template-columns: minmax(0, 1fr) auto auto; }
  .reader-document-title { font-size: 12px; }
  .reader-status { display: none; }
  .reader-zoom-control { display: none; }
  .reader-body { position: relative; grid-template-columns: minmax(0, 1fr); }
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
  .selection-translate-popover,
  .selection-command-bar { max-width: calc(100vw - 20px); }
  .selection-command-bar {
    flex-wrap: wrap;
    justify-content: center;
    border-radius: 18px;
  }
  .selection-command-bar > button { height: 34px; padding-inline: 12px; font-size: 12px; }
  .mind-map-overlay { padding: 8px; }
  .mind-map-modal { width: 100%; height: 96vh; }
  .mind-map-modal header { align-items: flex-start; flex-direction: column; }
  .mind-map-modal header span { max-width: calc(100vw - 40px); }
  .mind-map-modal-actions { flex-wrap: wrap; width: 100%; }
  .mind-map-modal-actions button { flex: 1 1 auto; }
  .mind-map-zoom-controls { left: 12px; right: auto; }
}

@media (max-width: 560px) {
  .reader-workbench { --reader-toolbar-height: 62px; }
  .reader-toolbar-row {
    height: 42px;
  }

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
  .apple-ai-launcher,
  .paper-chat-launcher {
    right: 14px !important;
    bottom: 14px;
    min-width: 48px;
    height: 48px;
    padding-inline: 14px;
  }

  .paper-chat-panel { left: 50%; right: auto; top: 92px; bottom: auto; width: calc(100vw - 16px); height: min(680px, calc(100vh - 104px)); transform: translateX(-50%); }
  .reader-tour-card { bottom: 18px; width: calc(100vw - 20px); box-sizing: border-box; }
}
.reader-tour-card h2 {
  margin: 4px 0 8px;
  color: #0f172a;
  font-size: 16.5px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.01em;
}
.reader-tour-card p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.65;
}
.tour-card-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}
.tour-btn-prev {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  color: #475569;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.tour-btn-prev:hover {
  color: #0f172a;
  background: #e2e8f0;
}
.tour-btn-next {
  height: 36px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  color: #ffffff;
  background: linear-gradient(135deg, #6366f1, #3b82f6);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
  transition: all 0.22s;
}
.tour-btn-next:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.5);
}

:root[data-theme="dark"] .reader-tour-card {
  background: rgba(15, 23, 42, 0.94) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.6) !important;
}
:root[data-theme="dark"] .reader-tour-card h2 {
  color: #f8fafc !important;
}
:root[data-theme="dark"] .reader-tour-card p {
  color: #cbd5e1 !important;
}
:root[data-theme="dark"] .tour-step-badge {
  color: #a5b4fc !important;
  background: rgba(99, 102, 241, 0.2) !important;
  border-color: rgba(129, 140, 248, 0.35) !important;
}
:root[data-theme="dark"] .tour-btn-prev {
  color: #cbd5e1 !important;
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}
:root[data-theme="dark"] .tour-btn-prev:hover {
  color: #ffffff !important;
  background: rgba(255, 255, 255, 0.16) !important;
}

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

.pdf-figure-overlay { position: fixed; left: 0; right: 0; top: var(--reader-toolbar-height); bottom: 0; z-index: 80; display: grid; place-items: center; background: rgba(15, 23, 42, 0.55); padding: 18px; }
.pdf-figure-modal { width: min(96vw, 1680px); height: calc(100vh - var(--reader-toolbar-height) - 36px); max-height: calc(100vh - var(--reader-toolbar-height) - 36px); display: grid; grid-template-rows: auto minmax(0, 1fr); overflow: hidden; padding: 14px; border-radius: 14px; background: #fff; box-shadow: 0 24px 64px -16px rgba(15, 23, 42, 0.22); }
.pdf-figure-modal header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; padding-bottom: 10px; border-bottom: 1px solid #f1f5f9; }
.pdf-figure-modal header strong { color: #0f172a; font-size: 14px; font-weight: 600; max-width: 60%; }
.pdf-figure-modal-actions { display: flex; gap: 8px; }
.pdf-figure-analyze-modal { padding: 5px 14px; border: 0; border-radius: 8px; background: #6366f1; color: #fff; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 6px rgba(99,102,241,0.25); }
.pdf-figure-analyze-modal:hover { background: #4f46e5; transform: translateY(-1px); box-shadow: 0 4px 10px rgba(99,102,241,0.35); }
.pdf-figure-modal header > button, .pdf-figure-modal-actions > button:last-child { padding: 5px 12px; border: 0; border-radius: 8px; background: #f1f5f9; color: #475569; font-size: 12px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.pdf-figure-modal header > button:hover, .pdf-figure-modal-actions > button:last-child:hover { background: #e2e8f0; color: #0f172a; }
.pdf-figure-modal-stage {
  display: grid;
  min-height: 0;
  overflow: auto;
  place-items: center;
  background: #ffffff;
}
.pdf-figure-modal-stage canvas,
.pdf-figure-modal-stage img {
  display: block;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  background: #fff;
  transform-origin: center center;
  transition: transform 180ms ease;
}
.pdf-figure-modal-stage canvas { height: auto !important; }
.mind-map-overlay { position: fixed; left: 0; right: 0; top: var(--reader-toolbar-height); bottom: 0; z-index: 82; display: grid; place-items: center; padding: 16px; background: rgba(15, 23, 42, 0.45); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); }
.mind-map-modal { width: min(98vw, 1680px); height: calc(100vh - var(--reader-toolbar-height) - 32px); max-height: calc(100vh - var(--reader-toolbar-height) - 32px); display: grid; grid-template-rows: auto minmax(0, 1fr); overflow: hidden; border: 1px solid rgba(23, 32, 51, 0.08); border-radius: 16px; background: #ffffff; box-shadow: 0 24px 64px -16px rgba(15, 23, 42, 0.22); }

.mind-map-modal header { display: flex; align-items: center; justify-content: space-between; gap: 18px; min-height: 64px; padding: 12px 20px; border-bottom: 1px solid #edf1f6; }
.mind-map-modal header div { display: grid; min-width: 0; gap: 3px; }
.mind-map-modal header strong { color: #172033; font-size: 18px; font-weight: 800; }
.mind-map-modal header span { overflow: hidden; max-width: min(680px, 42vw); color: #7b8798; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.mind-map-modal-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 8px; }
.mind-map-modal-actions button { height: 34px; padding: 0 13px; border-radius: 99px; font-size: 12px; font-weight: 700; cursor: pointer; white-space: nowrap; display: inline-flex; align-items: center; justify-content: center; transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.mind-map-modal-actions button.btn-export { border: 1px solid #dce5ef; color: #475569; background: #ffffff; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
.mind-map-modal-actions button.btn-export:hover { border-color: #94a3b8; color: #0f172a; background: #f8fafc; transform: translateY(-1.5px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.mind-map-modal-actions button.btn-export:active { transform: translateY(0); box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
.mind-map-modal-actions button.btn-close { border: 0; color: #ffffff; background: linear-gradient(135deg, #4f46e5, #6366f1); box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25); }
.mind-map-modal-actions button.btn-close:hover { background: linear-gradient(135deg, #4338ca, #4f46e5); transform: translateY(-1.5px); box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35); }
.mind-map-modal-actions button.btn-close:active { transform: translateY(0); box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25); }
.mind-map-modal-actions button:disabled { opacity: .48; cursor: not-allowed; transform: none !important; box-shadow: none !important; }
.mind-map-canvas { position: relative; min-height: 0; overflow: hidden; background: linear-gradient(180deg, #f8fafc, #f1f5f9); }
.mind-map-svg { display: block; width: 100%; height: 100%; min-height: 0; cursor: grab; }
.mind-map-svg:active { cursor: grabbing; }
.mind-map-svg :deep(.markmap-node text),
.mind-map-svg :deep(.markmap-node tspan),
.mind-map-svg :deep(.markmap-node div),
.mind-map-svg :deep(.markmap-node span),
.mind-map-svg :deep(.markmap-foreign),
.mind-map-svg :deep(.markmap-foreign *) {
  font: 650 15px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  fill: #172033 !important;
  color: #172033 !important;
}
.mind-map-svg :deep(.markmap-node circle) { r: 5; stroke-width: 2px; fill: #ffffff; }
.mind-map-svg :deep(.markmap-link) { stroke-width: 2px; stroke-opacity: 0.72; }
.mind-map-zoom-controls { position: absolute; bottom: 24px; right: 24px; z-index: 10; display: flex; gap: 6px; padding: 6px; border: 1px solid rgba(23, 32, 51, 0.08); border-radius: 99px; background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08); }
.mind-map-zoom-controls button { width: 32px; height: 32px; padding: 0; border: 0; border-radius: 50%; color: #475569; background: transparent; font-size: 14px; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s ease; }
.mind-map-zoom-controls button:hover { background: rgba(15, 23, 42, 0.06); color: #0f172a; transform: scale(1.08); }
.mind-map-status { position: absolute; left: 50%; top: 24px; transform: translateX(-50%); padding: 10px 20px; border: 1px solid rgba(99, 102, 241, 0.15); border-radius: 99px; color: #4f46e5; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px); font-size: 13px; font-weight: 650; box-shadow: 0 10px 30px rgba(99, 102, 241, 0.12); pointer-events: none; display: flex; align-items: center; gap: 8px; }
.mind-map-status.error { color: #9f1239; border-color: #fecdd3; background: rgba(255, 241, 242, 0.95); box-shadow: 0 10px 30px rgba(225, 29, 72, 0.12); }
.mind-map-spinner { display: inline-block; width: 12px; height: 12px; border: 2px solid rgba(99, 102, 241, 0.2); border-top-color: #6366f1; border-radius: 50%; animation: mind-map-spin 0.6s linear infinite; }
@keyframes mind-map-spin { to { transform: rotate(360deg); } }
.mineru-table { overflow-x: auto; padding: 14px; border: 1px solid #e2e7ef; background: #fff; }
.mineru-table :deep(table) { width: 100%; border-collapse: collapse; font: 12px/1.55 "Times New Roman", serif; }
.mineru-table :deep(th),
.mineru-table :deep(td) { padding: 7px 9px; border: 1px solid #cfd6e0; text-align: left; vertical-align: top; }
.paper-meta-translation { margin: 12px 0 6px; padding-left: 12px; border-left: 2px solid #9fb5d8; }
.paper-heading .paper-meta-translation p { margin: 4px 0; color: #26364d !important; font-family: "Songti SC", "Noto Serif SC", serif; line-height: 1.74; }
.paper-heading .paper-title-translation { font-size: clamp(24px, 1.72vw, 30px) !important; font-weight: 850; color: #172842 !important; }
.paper-author-translation,
.paper-heading .paper-author-translation {
  color: #2f7fa9 !important;
  font-family: Georgia, "Times New Roman", "Songti SC", serif !important;
  font-size: clamp(15px, 1.18vw, 19px) !important;
  font-weight: 500;
  line-height: 1.55 !important;
  letter-spacing: 0;
}
.paper-heading .paper-author-translation .author-name-text,
.paper-author-translation .author-name-text,
.paper-heading .paper-author-translation .author-affiliation-sup,
.paper-author-translation .author-affiliation-sup {
  color: inherit !important;
}
.author-translation-unit { border-left-color: #2f7fa9; }
.paper-citation-sup { margin-inline: 1px; color: #2563eb; font-size: 0.68em; font-weight: 650; line-height: 0; vertical-align: super; }
.mineru-equation { overflow-x: auto; margin: 18px 0; padding: 14px 18px; border-left: 3px solid #6d5dfc; background: #f7f6ff; color: #25233d; font: 15px/1.7 "Times New Roman", serif; white-space: pre-wrap; user-select: text; }
.mineru-equation-image-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-width: max-content;
}
.mineru-equation-image-button {
  display: block;
  width: fit-content;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}
.mineru-equation-image {
  display: block;
  max-width: min(100%, 720px);
  max-height: 150px;
  object-fit: contain;
  object-position: left center;
  padding: 6px 10px;
  border-radius: 6px;
  background: #ffffff;
}
.mineru-equation-number {
  color: #475569;
  font: 600 13px/1 "Times New Roman", serif;
  white-space: nowrap;
}
.mineru-equation-text { margin: 8px 0 0; color: inherit; background: transparent; font: inherit; white-space: pre-wrap; }
.reference-block { margin: 12px 0; color: #4a5362; font: 0.82em/1.75 "Times New Roman", "Songti SC", serif; white-space: pre-wrap; }
:root[data-theme="dark"] .reference-block { color: #cbd5e1; background: rgba(30, 41, 59, 0.35); border-left: 3px solid rgba(148, 163, 184, 0.4); padding: 10px 14px; border-radius: 0 8px 8px 0; }
:root[data-theme="dark"] .paper-meta-translation { border-left-color: #607da8; }
:root[data-theme="dark"] .paper-heading .paper-meta-translation p { color: #f1f6ff !important; }
:root[data-theme="dark"] .paper-heading .paper-title-translation { color: #ffffff !important; }
:root[data-theme="dark"] .paper-author-translation,
:root[data-theme="dark"] .paper-heading .paper-author-translation { color: #7dc4ec !important; }
:root[data-theme="dark"] .author-translation-unit { border-left-color: #7dc4ec; }
:root[data-theme="dark"] .paper-citation-sup { color: #93c5fd; }
:root[data-theme="dark"] .mineru-equation { background: rgba(30, 41, 59, 0.6); border-left-color: #818cf8; color: #f1f5f9; }
:root[data-theme="dark"] .mineru-equation-image { background: #ffffff; }
:root[data-theme="dark"] .mineru-equation-number { color: #cbd5e1; }
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
/* ── DARK MODE ADAPTATIONS FOR READER VIEW ── */
:root[data-theme="dark"] .reader-workbench {
  background: #08080c;
  color: #e2e2e6;
}

:root[data-theme="dark"] .reader-toolbar {
  background: rgba(14, 14, 20, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #f4f4f6;
}

:root[data-theme="dark"] .reader-document-title {
  color: #f4f4f6;
}
:root[data-theme="dark"] .reader-document-source {
  color: #a1a1aa;
}

:root[data-theme="dark"] .reader-back,
:root[data-theme="dark"] .reader-zoom-control,
:root[data-theme="dark"] .reader-pdf-action {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .reader-zoom-control button {
  color: #f4f4f6;
}

:root[data-theme="dark"] .reader-status-item strong {
  color: #60a5fa;
}

:root[data-theme="dark"] .reader-body {
  background: #08080c;
}

:root[data-theme="dark"] .reading-column {
  background: #0e0e14 !important;
  color: #e2e2e6 !important;
  box-shadow: 0 22px 58px rgba(0, 0, 0, 0.5);
}

:root[data-theme="dark"] .pinned-screenshot-card {
  border-color: rgba(255, 255, 255, 0.12);
  background: #111827;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.5);
}

:root[data-theme="dark"] .pinned-screenshot-card header {
  border-bottom-color: rgba(255, 255, 255, 0.1);
  color: #e5e7eb;
}

:root[data-theme="dark"] .pinned-screenshot-card img {
  background: #0f172a;
}

:root[data-theme="dark"] .paper-heading {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

:root[data-theme="dark"] .paper-heading h1 {
  color: #f4f4f6;
}

:root[data-theme="dark"] .paper-authors-line,
:root[data-theme="dark"] .author-name-text,
:root[data-theme="dark"] .author-affiliation-sup {
  color: #7dc4ec !important;
}

:root[data-theme="dark"] .source-paragraph {
  color: #e2e2e6;
}

:root[data-theme="dark"] .source-heading {
  color: #60a5fa;
}

:root[data-theme="dark"] .source-heading.abstract-heading {
  color: #f4f4f6;
}

:root[data-theme="dark"] .translated-paragraph {
  color: #cbd5e1;
}

:root[data-theme="dark"] .translation-unit {
  border-left-color: rgba(99, 102, 241, 0.5);
}

.reflow-document .paper-heading .paper-meta-translation .paper-author-translation,
.reflow-document .translation-unit.author-translation-unit .translated-paragraph.paper-author-translation {
  color: #2f7fa9 !important;
  font-family: Georgia, "Times New Roman", "Songti SC", serif !important;
  font-size: clamp(15px, 1.18vw, 19px) !important;
  font-weight: 500 !important;
  line-height: 1.55 !important;
  letter-spacing: 0 !important;
}

.reflow-document .translation-unit.author-translation-unit {
  border-left-color: #2f7fa9 !important;
}

.reflow-document .paper-author-translation .author-name-text,
.reflow-document .paper-author-translation .author-affiliation-sup {
  color: inherit !important;
}

:root[data-theme="dark"] .reflow-document .paper-heading .paper-meta-translation .paper-author-translation,
:root[data-theme="dark"] .reflow-document .translation-unit.author-translation-unit .translated-paragraph.paper-author-translation {
  color: #7dc4ec !important;
}

:root[data-theme="dark"] .reflow-document .translation-unit.author-translation-unit {
  border-left-color: #7dc4ec !important;
}

:root[data-theme="dark"] .assistant-tabs {
  background: linear-gradient(180deg, rgba(16, 23, 37, 0.94), rgba(11, 16, 28, 0.96)) !important;
  border-bottom-color: rgba(148, 163, 184, 0.18) !important;
  backdrop-filter: blur(16px) !important;
}

:root[data-theme="dark"] .assistant-tabs button {
  color: #aab7ca !important;
  background: transparent !important;
  border-color: transparent !important;
}

:root[data-theme="dark"] .assistant-tabs button:hover {
  color: #e0faff !important;
  background: transparent !important;
  border-bottom-color: rgba(34, 211, 238, 0.38) !important;
}

:root[data-theme="dark"] .assistant-tabs button.active {
  color: #ecfeff !important;
  background: transparent !important;
  border-bottom-color: #22d3ee !important;
  box-shadow: 0 6px 14px -14px rgba(34, 211, 238, 0.5) !important;
}

:root[data-theme="dark"] .assistant-tabs .icon-button {
  color: #cbd5e1 !important;
  background: rgba(15, 23, 42, 0.78) !important;
  border: 1px solid rgba(148, 163, 184, 0.22) !important;
}

:root[data-theme="dark"] .assistant-tabs .icon-button:hover {
  color: #e0faff !important;
  background: rgba(8, 145, 178, 0.18) !important;
  border-color: rgba(34, 211, 238, 0.34) !important;
}

:root[data-theme="dark"] .assistant-scroll h3 {
  color: #f4f4f6;
}

:root[data-theme="dark"] .outline-item {
  border-bottom-color: rgba(255, 255, 255, 0.06);
  color: #a1a1aa;
}

:root[data-theme="dark"] .outline-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #60a5fa;
}

:root[data-theme="dark"] .mind-map-modal {
  border-color: rgba(255, 255, 255, 0.08);
  background: #0e0e14;
  box-shadow: 0 24px 64px -16px rgba(0, 0, 0, 0.6);
}

:root[data-theme="dark"] .mind-map-modal header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
  background: #14141d;
}

:root[data-theme="dark"] .mind-map-modal header strong {
  color: #f4f4f6;
}

:root[data-theme="dark"] .mind-map-modal header span {
  color: #94a3b8;
}

:root[data-theme="dark"] .mind-map-modal-actions button.btn-export {
  border-color: rgba(255, 255, 255, 0.12);
  color: #cbd5e1;
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

:root[data-theme="dark"] .mind-map-modal-actions button.btn-export:hover {
  border-color: rgba(255, 255, 255, 0.24);
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

:root[data-theme="dark"] .mind-map-modal-actions button.btn-close {
  background: linear-gradient(135deg, #6366f1, #818cf8);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

:root[data-theme="dark"] .mind-map-modal-actions button.btn-close:hover {
  background: linear-gradient(135deg, #818cf8, #93c5fd);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.5);
}

:root[data-theme="dark"] .mind-map-canvas {
  color: #e2e8f0;
  background: #090d16;
}

:root[data-theme="dark"] .mind-map-svg :deep(.markmap-node text),
:root[data-theme="dark"] .mind-map-svg :deep(.markmap-node tspan),
:root[data-theme="dark"] .mind-map-svg :deep(.markmap-node div),
:root[data-theme="dark"] .mind-map-svg :deep(.markmap-node span),
:root[data-theme="dark"] .mind-map-svg :deep(.markmap-foreign),
:root[data-theme="dark"] .mind-map-svg :deep(.markmap-foreign *) {
  fill: #f1f5f9 !important;
  color: #f1f5f9 !important;
}

:root[data-theme="dark"] .mind-map-svg :deep(.markmap-node circle) {
  fill: #090d16 !important;
}

:root[data-theme="dark"] .mind-map-svg :deep(.markmap-link) {
  stroke-opacity: 0.58;
}

:root[data-theme="dark"] .mind-map-zoom-controls {
  border-color: rgba(255, 255, 255, 0.08);
  background: rgba(14, 14, 20, 0.85);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

:root[data-theme="dark"] .mind-map-zoom-controls button {
  color: #cbd5e1;
}

:root[data-theme="dark"] .mind-map-zoom-controls button:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

:root[data-theme="dark"] .mind-map-status {
  color: #818cf8;
  border-color: rgba(99, 102, 241, 0.3);
  background: rgba(14, 14, 20, 0.9);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
}

:root[data-theme="dark"] .mind-map-status.error {
  color: #fecdd3;
  border-color: rgba(251, 113, 133, 0.36);
  background: rgba(127, 29, 29, 0.72);
}

:root[data-theme="dark"] .reader-theme-toggle-btn {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #a8b3c7;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  width: 28px;
  height: 28px;
  transition: all .2s;
}
:root[data-theme="dark"] .reader-theme-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

:root[data-theme="dark"] .icon-button {
  color: #94a3b8 !important;
}
:root[data-theme="dark"] .icon-button:hover {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #f8fafc !important;
}

/* Text selection translator popover and annotation editor */
:root[data-theme="dark"] .selection-result,
:root[data-theme="dark"] .selection-provider-panel,
:root[data-theme="dark"] .selection-annotation-editor {
  background: rgba(14, 14, 20, 0.96) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #f4f4f6 !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.7) !important;
}

:root[data-theme="dark"] .selection-result p,
:root[data-theme="dark"] .selection-provider-panel select,
:root[data-theme="dark"] .selection-annotation-editor textarea {
  color: #e2e2e6 !important;
  background: #141e2e !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

:root[data-theme="dark"] .selectable-paragraph::selection {
  background: rgba(59, 130, 246, 0.35) !important;
  color: #ffffff !important;
}

.paper-chat-panel.futuristic-void-panel {
  left: var(--paper-chat-x) !important;
  top: var(--paper-chat-y) !important;
  right: auto !important;
  bottom: auto !important;
  width: var(--paper-chat-w) !important;
  max-width: none !important;
  height: var(--paper-chat-h) !important;
  max-height: none !important;
  transform: none !important;
}

.paper-chat-panel.futuristic-void-panel.positioned {
  right: auto !important;
  bottom: auto !important;
  transform: none !important;
}

.paper-chat-panel.futuristic-void-panel .paper-chat-messages,
.paper-chat-panel.futuristic-void-panel .void-chat-body {
  padding: 28px clamp(30px, 5vw, 72px) !important;
}

.paper-chat-panel.futuristic-void-panel .paper-chat-message.assistant,
.paper-chat-panel.futuristic-void-panel .paper-chat-message.user {
  width: min(940px, 100%) !important;
}

.paper-chat-panel.futuristic-void-panel .paper-chat-message p,
.paper-chat-panel.futuristic-void-panel .message-content-wrapper {
  max-width: 100% !important;
}

.paper-chat-panel.futuristic-void-panel form,
.paper-chat-panel.futuristic-void-panel .void-input-form {
  width: min(940px, calc(100% - 64px)) !important;
}

.paper-chat-panel.futuristic-void-panel .markdown-rendered strong,
.paper-chat-panel.futuristic-void-panel .markdown-rendered b,
.paper-chat-panel.futuristic-void-panel .message-text strong,
.paper-chat-panel.futuristic-void-panel .message-text b {
  color: #0f766e !important;
  font-weight: 800;
}

.paper-chat-panel.futuristic-void-panel .markdown-rendered code,
.paper-chat-panel.futuristic-void-panel .message-text code {
  color: #0f766e !important;
  background: rgba(20, 184, 166, 0.1) !important;
  border: 1px solid rgba(20, 184, 166, 0.18) !important;
}

:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .markdown-rendered strong,
:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .markdown-rendered b,
:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .message-text strong,
:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .message-text b {
  color: #e8fbff !important;
}

:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .markdown-rendered code,
:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .message-text code,
:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .markdown-rendered a,
:root[data-theme="dark"] .paper-chat-panel.futuristic-void-panel .message-text a {
  color: #67e8f9 !important;
  background: rgba(8, 145, 178, 0.16) !important;
  border-color: rgba(34, 211, 238, 0.18) !important;
}

@media (max-width: 760px) {
  .paper-chat-panel.futuristic-void-panel form,
  .paper-chat-panel.futuristic-void-panel .void-input-form {
    width: calc(100% - 24px) !important;
  }
}
</style>
