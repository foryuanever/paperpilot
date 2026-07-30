<template>
  <div class="library-route-root">
    <Teleport to="body">
      <div
        v-if="openFilter"
        class="library-filter-menu-portal"
        :style="filterMenuStyle"
        @click.stop
      >
        <label
          v-for="opt in currentFilterOptions"
          :key="opt"
          class="library-filter-option"
        >
          <input
            type="checkbox"
            :checked="currentFilterSelected.includes(opt)"
            @change="toggleFilterOption(openFilter, opt)"
          />
          <span>{{ opt }}</span>
        </label>
        <div v-if="!currentFilterOptions.length" class="library-filter-empty">暂无选项</div>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="zoteroGuideOpen" class="zotero-guide-backdrop" @click.self="closeZoteroGuide">
        <section class="zotero-guide-dialog" role="dialog" aria-modal="true" aria-labelledby="zotero-guide-title">
          <header class="zotero-guide-head">
            <div>
              <span>ZOTERO LOCAL ACCESS</span>
              <h2 id="zotero-guide-title">本机同步设置教程</h2>
              <p>开启 Zotero 本机通讯后，PaperSolver 才能读取题录并把条目下的 PDF 保存到桌面端本机缓存。</p>
            </div>
            <button type="button" aria-label="关闭 Zotero 设置教程" @click="closeZoteroGuide">×</button>
          </header>

          <div class="zotero-guide-body">
            <figure class="zotero-guide-image">
              <img src="/tutorials/zotero-local-api-setting.png" alt="Zotero 高级设置中的本机通讯开关" />
            </figure>

            <div class="zotero-guide-content">
              <section>
                <h3>需要打开的位置</h3>
                <ol>
                  <li>打开 Zotero Desktop。</li>
                  <li>进入 <strong>Zotero 设置</strong>。</li>
                  <li>选择左侧 <strong>高级</strong>。</li>
                  <li>在 <strong>杂项</strong> 中勾选 <strong>允许此计算机上的其他应用程序与 Zotero 通讯</strong>。</li>
                  <li>确认页面显示本机接口：<code>http://localhost:23119/api/</code>。</li>
                </ol>
              </section>

              <section>
                <h3>同步内容</h3>
                <ul>
                  <li>题录：标题、作者、期刊/会议、年份、摘要、DOI。</li>
                  <li>PDF 附件：如果 Zotero 条目下已保存 PDF，桌面端会保存到本机缓存，不上传服务器。</li>
                  <li>再次同步：会尝试补全已导入文献的 PDF，不需要重复手动上传。</li>
                </ul>
              </section>
            </div>
          </div>
        </section>
      </div>
    </Teleport>
    <div class="spatial-page library-spatial">
    <section class="spatial-chapter library-workbench-head" data-reveal="off">
      <div class="spatial-chapter-inner library-head-inner" data-reveal="off">
        <div class="library-head-actions" data-reveal>
          <CheckinLottery @toast="showToast" />
        </div>
      </div>
    </section>

    <section class="spatial-chapter-inner">
      <div class="library-nav-row">
        <nav class="library-subnav" aria-label="文献库二级导航">
          <button
            v-for="item in libraryTabs"
            :key="item.id"
            :class="{ active: activeTab === item.id }"
            @click="selectTab(item.id)"
          >
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </button>
        </nav>
        <div class="library-head-stats library-stats-row">
          <div class="library-head-stat">
            <span>{{ libraryStore.state.documents.length }}</span>
            <small>总文献</small>
          </div>
          <div class="library-head-stat">
            <span>{{ readableCount }}</span>
            <small>可阅读</small>
          </div>
          <div class="library-head-stat">
            <span>{{ notesCount }}</span>
            <small>有笔记</small>
          </div>
        </div>
      </div>

      <template v-if="activeTab === 'papers'">
      <div class="spatial-command-strip library-toolbar">
        <div class="library-toolbar-left">
          <input v-model="keyword" class="toolbar-search" placeholder="搜索标题、作者、备注..." />
          <div class="library-filters">
            <div class="library-filter" v-for="filter in filterDefs" :key="filter.key">
              <button
                type="button"
                class="toolbar-chip"
                :class="{ active: filter.selected.length }"
                :ref="(el) => registerFilterButton(filter.key, el)"
                @click.stop="toggleFilter(filter.key)"
              >
                {{ filter.label }}
                <span v-if="filter.selected.length" class="toolbar-chip-count">{{ filter.selected.length }}</span>
                <em>▾</em>
              </button>
            </div>
          </div>
        </div>
        <div class="library-toolbar-right">
          <router-link class="spatial-btn spatial-btn-ghost" to="/search">去学术搜索</router-link>
          <span class="toolbar-count">{{ filteredDocuments.length }} 篇</span>
        </div>
      </div>

      <div class="spatial-table-river">
        <div class="library-table-scroll">
          <table class="library-table">
            <colgroup>
              <col class="col-title" />
              <col class="col-authors" />
              <col class="col-type" />
              <col class="col-ranking" />
              <col class="col-import-source" />
              <col class="col-publish" />
              <col class="col-progress" />
              <col class="col-time" />
              <col class="col-actions" />
              <col class="col-note" />
            </colgroup>
            <thead>
              <tr>
                <th>标题与来源</th>
                <th>作者</th>
                <th>文献类型</th>
                <th>期刊标签</th>
                <th>导入源头</th>
                <th>发表时间</th>
                <th>阅读进度</th>
                <th>阅读时间</th>
                <th class="action-cell">操作</th>
                <th>我的笔记</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="paper in paginatedDocuments" :key="paper.id">
                <td class="doc-title-cell">
                  <div
                    class="doc-title-main"
                    :title="paper.title + (paper.abstract ? '\n\n【Abstract】\n' + paper.abstract : '')"
                  >
                    {{ paper.title }}
                  </div>
                  <div class="doc-title-sub">
                    <span class="source-text" :title="paper.source">{{ paper.source }}</span>
                    <span v-if="paper.publishYear"> · {{ paper.publishYear }}</span>
                    <span v-if="!canTryRead(paper)" class="missing-pdf-badge">暂无 PDF</span>
                  </div>
                </td>
                <td class="doc-authors-cell" :title="paper.authors">
                  <span :class="{ missing: paper.authors === '作者待补全' }">{{ paper.authors || "作者待补全" }}</span>
                </td>
                <td>
                  <span class="venue-type-badge" :class="venueTypeClass(paper.venueType)">
                    {{ paper.venueType || "待分类" }}
                  </span>
                </td>
                <td>
                  <div class="journal-metric-row journal-metric-row-editable" @click="openJournalTagEditor(paper)" :title="journalTagsSummary(paper)">
                    <span
                      v-for="metric in journalMetricTags(paper)"
                      :key="`${metric.type}-${metric.label}`"
                      class="journal-metric-badge"
                      :class="metric.type"
                    >
                      {{ journalMetricDisplayLabel(metric) }}
                    </span>
                    <span v-if="!journalMetricTags(paper).length" class="journal-metric-empty">点击设置</span>
                  </div>
                </td>
                <td class="import-source-cell">
                  <a
                    v-if="paper.sourceUrl"
                    :href="paper.sourceUrl"
                    target="_blank"
                    rel="noreferrer"
                    :title="paper.sourceUrl"
                  >
                    {{ paper.importSource || sourceHost(paper.sourceUrl) || "来源页面" }}
                  </a>
                  <span v-else>{{ paper.importSource || "未记录" }}</span>
                </td>
                <td class="publish-time-cell">
                  {{ publishTimeLabel(paper) }}
                </td>
                <td><span class="progress-text">{{ paper.progress }}</span></td>
                <td>{{ paper.readAt }}</td>
                <td class="action-cell">
                  <div class="action-inline">
                    <template v-if="canTryRead(paper)">
                      <button class="spatial-btn spatial-btn-dual" @click="openDualReader(paper)">对照翻译</button>
                      <button class="spatial-btn spatial-btn-line-ai" @click="openLineAiReader(paper)">
                        <span>沉浸翻译</span>
                        <em class="reader-recommend-badge">荐</em>
                      </button>
                    </template>
                    <button v-else class="spatial-btn spatial-btn-warning" @click="openPdfLinkEditor(paper)">关联 PDF</button>
                    <button class="spatial-btn spatial-btn-danger" @click="directDelete(paper)">删除</button>
                    <button
                      v-if="officialPdfCandidate(paper) && !canTryRead(paper)"
                      class="action-link action-link-button"
                      type="button"
                      @click="openPdfLinkEditor(paper)"
                    >
                      PDF未导入
                    </button>
                    <a
                      v-else-if="pdfHref(paper)"
                      class="action-link"
                      :href="pdfHref(paper)"
                      target="_blank"
                      rel="noreferrer"
                    >
                      PDF
                    </a>
                  </div>
                </td>
                <td class="doc-note-cell">
                  <button class="note-edit-btn" type="button" @click="openNoteEditor(paper)">
                    {{ paper.note ? "查看笔记" : "添加笔记" }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination-bar">
          <span class="muted">共 {{ filteredDocuments.length }} 条</span>
          <span class="pagination-summary">第 {{ currentPage }} / {{ totalPages }} 页</span>
          <button class="pagination-btn" type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹</button>
          <button
            v-for="page in visiblePageNumbers"
            :key="page"
            class="page-number-pill"
            type="button"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
          <button class="pagination-btn" type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">›</button>
          <label class="page-size-control">
            <select v-model.number="pageSize" aria-label="每页显示文献数量" :title="`${pageSize} 条/页`">
              <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }} 条/页</option>
            </select>
          </label>
        </div>
      </div>
      </template>

      <section v-else-if="activeTab === 'add'" class="library-management-panel">
        <header>
          <div>
            <h2>添加个人文献</h2>
            <p>手动补充单篇论文并上传本地 PDF；导入后会进入当前账号文献库。</p>
          </div>
        </header>
        <form class="personal-paper-form" @submit.prevent="submitPersonalPaper">
          <label class="field-wide">
            <span>论文标题 *</span>
            <input v-model="personalPaper.title" required placeholder="输入完整论文标题" />
          </label>
          <label>
            <span>作者</span>
            <input v-model="personalPaper.authors" placeholder="作者之间用逗号分隔" />
          </label>
          <label>
            <span>发表年份</span>
            <input v-model="personalPaper.publishYear" inputmode="numeric" placeholder="2026" />
          </label>
          <label class="field-wide">
            <span>来源 / 期刊</span>
            <input v-model="personalPaper.source" placeholder="个人文献、期刊或会议名称" />
          </label>
          <label class="field-wide">
            <span>摘要</span>
            <textarea v-model="personalPaper.abstractText" rows="5" placeholder="可选：粘贴论文摘要，便于后续 AI 分析"></textarea>
          </label>
          <label class="file-drop field-wide">
            <input type="file" accept="application/pdf,.pdf" @change="selectPersonalPdf" />
            <strong>{{ personalPdf?.name || "选择本地 PDF" }}</strong>
            <small>{{ isDesktopApp ? "桌面端会保存到本机，不上传服务器。" : "上传后由 PaperSolver 储存，并可直接进入对照或沉浸翻译。" }}</small>
          </label>
          <footer class="field-wide">
            <button type="button" class="spatial-btn spatial-btn-ghost" @click="resetPersonalPaper">清空</button>
            <button type="submit" class="spatial-btn spatial-btn-accent" :disabled="personalImporting">
              {{ personalImporting ? "正在添加…" : "添加到个人文献库" }}
            </button>
          </footer>
        </form>
      </section>

      <section v-else-if="activeTab === 'zotero'" class="library-management-panel zotero-tab-panel">
        <header>
          <div>
            <h2>从 Zotero 导入</h2>
            <p>读取本机 Zotero Desktop 文献库，自动导入题录并复制已保存的 PDF 附件。</p>
          </div>
        </header>
        <section class="zotero-import-panel">
          <div class="zotero-copy">
            <span>LOCAL ZOTERO</span>
            <h3>同步文献和本机 PDF</h3>
            <p>保持 Zotero Desktop 打开，PaperSolver 会读取当前本机文库；如果条目下有 PDF 附件，会保存到桌面端本机缓存，进入阅读器后不需要再次手动上传。</p>
            <div class="zotero-step-grid" aria-label="Zotero 同步流程">
              <div>
                <b>1</b>
                <strong>连接本机</strong>
                <small>读取 23119 本机服务</small>
              </div>
              <div>
                <b>2</b>
                <strong>同步题录</strong>
                <small>标题、作者、摘要、年份</small>
              </div>
              <div>
                <b>3</b>
                <strong>复制 PDF</strong>
                <small>Zotero storage 附件</small>
              </div>
            </div>
            <div class="zotero-format-row">
              <b>本机同步</b>
              <b>PDF 附件</b>
              <b>BibTeX</b>
              <b>RIS</b>
              <b>CSL JSON</b>
            </div>
          </div>
          <div class="zotero-action-box">
            <div class="zotero-online-card">
              <div class="zotero-card-head">
                <div>
                  <strong>本机 Zotero 同步</strong>
                  <small>推荐方式，可自动保存本地 PDF 附件。</small>
                </div>
                <span>推荐</span>
              </div>
              <label class="zotero-limit-field">
                <span>同步上限</span>
                <input v-model.number="zoteroOnline.limit" type="number" min="1" max="200" />
                <em>篇</em>
              </label>
              <button class="spatial-btn spatial-btn-accent" type="button" :disabled="zoteroOnline.importing" @click="submitZoteroOnlineImport">
                {{ zoteroOnline.importing ? "读取本机 Zotero 中…" : "检测本机 Zotero 并同步" }}
              </button>
              <div class="zotero-help-strip">
                <small>不读取或保存 Zotero 密码。若 Zotero 未开启本机通信，先按教程打开本机通讯。</small>
                <button type="button" @click="openZoteroGuide">查看设置教程</button>
              </div>
            </div>
            <div class="zotero-divider"><span>或上传导出文件</span></div>
            <label class="zotero-file-drop">
              <input type="file" accept=".bib,.ris,.json,application/json,text/plain" @change="selectZoteroFile" />
              <span class="zotero-file-icon">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M8 13h8M8 17h5"/></svg>
              </span>
              <strong>{{ zoteroFile?.name || "选择 Zotero 导出文件" }}</strong>
              <small>{{ zoteroFile ? formatFileSize(zoteroFile.size) : "从 Zotero 导出的 .bib / .ris / .json" }}</small>
            </label>
            <button class="spatial-btn spatial-btn-ghost zotero-file-import-btn" type="button" :disabled="zoteroImporting || !zoteroFile" @click="submitZoteroImport">
              {{ zoteroImporting ? "导入中…" : "从 Zotero 导入" }}
            </button>
            <div v-if="zoteroResult" class="zotero-result" :class="{ partial: zoteroResult.failed > 0 || zoteroResult.pdfSkipped > 0 }">
              <strong>识别 {{ zoteroResult.detected }} 篇，已导入 {{ zoteroResult.imported }} 篇</strong>
              <span v-if="zoteroResult.pdfUploaded">{{ isDesktopApp ? "已本机保存 PDF" : "已自动补充 PDF" }} {{ zoteroResult.pdfUploaded }} 篇。</span>
              <span v-if="zoteroResult.pdfSkipped">有 {{ zoteroResult.pdfSkipped }} 篇未补 PDF，展开下方可查看原因。</span>
              <span v-if="zoteroResult.failed">失败 {{ zoteroResult.failed }} 篇，可能触发每日导入额度或缺少标题。</span>
              <span v-else-if="!zoteroResult.pdfSkipped">导入完成，文献已进入当前账号文献库。</span>
              <span v-else>题录已导入，缺失 PDF 的文献可后续手动上传或回 Zotero 下载附件后再次同步。</span>
            </div>
            <details v-if="zoteroIssueItems.length" class="zotero-failed-details">
              <summary>查看未完成明细</summary>
              <p v-for="item in zoteroIssueItems" :key="`${item.title}-${item.pdfStatus || item.status}`">
                <strong>{{ item.title }}</strong>
                <span>{{ item.message || item.pdfMessage || pdfStatusText(item.pdfStatus) }}</span>
              </p>
            </details>
          </div>
        </section>
      </section>

      <section v-else-if="activeTab === 'storage'" class="library-management-panel">
        <header class="storage-head">
          <div>
            <h2>PDF 文件管理</h2>
            <p>{{ isDesktopApp ? "集中管理桌面端本机 PDF、替换文件与缓存状态。" : "集中管理本地 PDF、替换文件与云端储存状态。" }}</p>
          </div>
          <div class="storage-summary">
            <strong>{{ storedCount }}</strong>
            <span>{{ isDesktopApp ? "本机 PDF" : "已储存 PDF" }} / {{ libraryStore.state.documents.length }} 篇</span>
          </div>
        </header>
        <div class="storage-list">
          <article v-for="paper in libraryStore.state.documents" :key="paper.id">
            <div>
              <strong>{{ paper.title }}</strong>
              <span>{{ canTryRead(paper) ? "PDF 已关联，可用于双栏与沉浸翻译" : "尚未关联 PDF" }}</span>
            </div>
            <label class="replace-upload">
              <input type="file" accept="application/pdf,.pdf" @change="uploadReplacementPdf(paper, $event)" />
              {{ uploadingWorkspace === paper.workspaceId ? (isDesktopApp ? "保存中…" : "上传中…") : canTryRead(paper) ? "替换 PDF" : "关联 PDF" }}
            </label>
          </article>
        </div>
      </section>

    </section>

    <!-- Custom Slide Up Toast -->
    <Transition name="slide-up">
      <div v-if="toastMessage" class="custom-toast">
        {{ toastMessage }}
      </div>
    </Transition>

    <div v-if="noteEditor.open" class="note-modal-backdrop" @click.self="closeNoteEditor">
      <section class="note-modal note-modal-wide">
        <header>
          <div>
            <span>{{ noteEditor.loading ? "正在同步阅读页笔记" : noteEditor.text ? "已同步阅读页笔记" : "阅读页右侧暂无笔记" }}</span>
            <h3>{{ noteEditor.paper?.title || "查看笔记" }}</h3>
          </div>
          <button type="button" @click="closeNoteEditor">×</button>
        </header>
        <div class="note-mode-bar">
          <button type="button" :class="{ active: noteEditor.mode === 'tree' }" @click="noteEditor.mode = 'tree'">层级笔记</button>
          <button type="button" :class="{ active: noteEditor.mode === 'markdown' }" @click="noteEditor.mode = 'markdown'">Markdown</button>
          <span>{{ noteEditor.loading ? "同步中..." : "与文献阅读右侧笔记同步" }}</span>
        </div>
        <div v-if="noteEditor.mode === 'tree'" class="library-note-tree-panel">
          <div v-if="noteEditor.loading" class="library-note-loading">
            <span class="library-note-spinner"></span>
            <strong>正在读取最新层级笔记</strong>
          </div>
          <div v-else-if="parsedNoteTree.length" class="library-note-tree">
            <article v-for="section in parsedNoteTree" :key="section.id" class="library-note-section" :class="`note-level-${section.level || 1}`">
              <div class="library-note-section-head">
                <span class="library-note-section-index">{{ section.index }}</span>
                <div>
                  <strong>{{ section.title }}</strong>
                </div>
                <small class="library-note-section-count">{{ section.children.length || section.content.length }} 点</small>
              </div>
              <p v-for="line in section.content" :key="`${section.id}-${line}`" class="library-note-section-text">{{ line }}</p>
              <div v-if="section.children.length" class="library-note-child-list">
                <div v-for="child in section.children" :key="child.id" class="library-note-child" :class="`note-level-${child.level || 2}`">
                  <span>{{ child.index }}</span>
                  <div>
                    <strong>{{ child.title }}</strong>
                    <p v-for="line in child.content" :key="`${child.id}-${line}`">{{ line }}</p>
                    <div v-if="child.children?.length" class="library-note-grandchild-list">
                      <div v-for="grandchild in child.children" :key="grandchild.id" class="library-note-grandchild note-level-3">
                        <span>{{ grandchild.index }}</span>
                        <div>
                          <strong>{{ grandchild.title }}</strong>
                          <p v-for="line in grandchild.content" :key="`${grandchild.id}-${line}`">{{ line }}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="library-note-empty">
            <strong>暂无层级笔记</strong>
            <p>在文献阅读页面右侧的“文献层级笔记”中新增或编辑节点后，这里会同步展示同一份内容。</p>
          </div>
        </div>
        <div v-else class="library-note-markdown-view">
          <div v-if="noteEditor.loading" class="library-note-loading">
            <span class="library-note-spinner"></span>
            <strong>正在读取最新 Markdown 笔记</strong>
          </div>
          <div v-else-if="parsedNoteTree.length" class="library-note-markdown-outline">
            <template v-for="section in parsedNoteTree" :key="`md-${section.id}`">
              <div class="library-note-markdown-line note-level-1">
                <span>{{ section.index }}</span>
                <strong>{{ section.title }}</strong>
                <em>{{ section.children.length || section.content.length }} 点</em>
              </div>
              <p v-for="line in section.content" :key="`md-${section.id}-${line}`" class="library-note-markdown-text note-level-1">{{ line }}</p>
              <template v-for="child in section.children" :key="`md-${child.id}`">
                <div class="library-note-markdown-line note-level-2">
                  <span>{{ child.index }}</span>
                  <strong>{{ child.title }}</strong>
                </div>
                <p v-for="line in child.content" :key="`md-${child.id}-${line}`" class="library-note-markdown-text note-level-2">{{ line }}</p>
                <template v-for="grandchild in child.children || []" :key="`md-${grandchild.id}`">
                  <div class="library-note-markdown-line note-level-3">
                    <span>{{ grandchild.index }}</span>
                    <strong>{{ grandchild.title }}</strong>
                  </div>
                  <p v-for="line in grandchild.content" :key="`md-${grandchild.id}-${line}`" class="library-note-markdown-text note-level-3">{{ line }}</p>
                </template>
              </template>
            </template>
          </div>
          <div v-else class="library-note-empty">
            <strong>暂无 Markdown 笔记</strong>
            <p>这里会同步展示文献阅读右侧笔记，并自动去掉 Markdown 符号。</p>
          </div>
        </div>
        <footer>
          <button type="button" class="spatial-btn spatial-btn-ghost" @click="closeNoteEditor">取消</button>
          <button type="button" class="spatial-btn spatial-btn-accent" :disabled="noteEditor.saving" @click="saveNoteEditor">
            {{ noteEditor.saving ? "保存中..." : "保存笔记" }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="pdfLinkEditor.open" class="note-modal-backdrop" @click.self="closePdfLinkEditor">
      <section class="note-modal pdf-link-modal">
        <header>
          <div>
            <span>关联 PDF</span>
            <h3>{{ pdfLinkEditor.paper?.title || "关联 PDF" }}</h3>
          </div>
          <button type="button" @click="closePdfLinkEditor">×</button>
        </header>
        <p class="note-paper-title">选择本地 PDF 文件上传，上传后即可进入双栏或沉浸翻译。</p>
        <label class="pdf-upload-drop field-wide">
          <input type="file" accept="application/pdf,.pdf" @change="pickPdfUploadFile" />
          <strong>{{ pdfLinkEditor.fileName || "选择本地 PDF 文件" }}</strong>
          <small>{{ isDesktopApp ? "支持 .pdf 格式，桌面端会保存到本机缓存。" : "支持 .pdf 格式，上传后由 PaperSolver 储存。" }}</small>
        </label>
        <p v-if="pdfLinkEditor.error" class="pdf-link-error">{{ pdfLinkEditor.error }}</p>
        <footer>
          <button type="button" class="spatial-btn spatial-btn-ghost" @click="closePdfLinkEditor">取消</button>
          <button type="button" class="spatial-btn spatial-btn-accent" :disabled="pdfLinkEditor.saving" @click="savePdfLinkEditor">
            {{ pdfLinkEditor.saving ? (isDesktopApp ? "保存中..." : "上传中...") : (isDesktopApp ? "保存到本机" : "上传 PDF") }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="journalTagEditor.open" class="note-modal-backdrop" @click.self="closeJournalTagEditor">
      <section class="note-modal journal-tag-modal">
        <header>
          <div>
            <span>期刊标签分级</span>
            <h3>{{ journalTagEditor.paper?.title || "期刊标签" }}</h3>
          </div>
          <button type="button" @click="closeJournalTagEditor">×</button>
        </header>
        <p class="note-paper-title">选择适合本文的期刊分级标签，可多选；顶部切换分类，确定后保存到该文献。</p>
        <nav class="journal-tag-tabs">
          <button
            v-for="(group, index) in journalTagGroups"
            :key="group.name"
            type="button"
            :class="{ active: journalTagEditor.activeGroup === index }"
            @click="journalTagEditor.activeGroup = index"
          >
            {{ group.name }}
          </button>
        </nav>
        <div class="journal-tag-panel">
          <button
            v-for="tag in journalTagGroups[journalTagEditor.activeGroup]?.tags || []"
            :key="tag"
            type="button"
            class="journal-tag-chip"
            :class="journalTagChipClass(tag)"
            :data-selected="journalTagEditor.selected.includes(tag)"
            @click="toggleJournalTag(tag)"
          >
            {{ tag }}
          </button>
        </div>
        <div class="journal-tag-selected-summary">
          <span>已选 ({{ journalTagEditor.selected.length }})</span>
          <strong v-if="!journalTagEditor.selected.length">未选择任何标签</strong>
          <strong v-else>{{ journalTagEditor.selected.join("、") }}</strong>
        </div>
        <p v-if="journalTagEditor.error" class="pdf-link-error">{{ journalTagEditor.error }}</p>
        <footer>
          <button type="button" class="spatial-btn spatial-btn-ghost" @click="closeJournalTagEditor">取消</button>
          <button type="button" class="spatial-btn spatial-btn-accent" :disabled="journalTagEditor.saving" @click="saveJournalTagEditor">
            {{ journalTagEditor.saving ? "保存中..." : "确定" }}
          </button>
        </footer>
      </section>
    </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useScrollReveal } from "../composables/useScrollReveal";
import { useLibraryStore } from "../stores/library";
import { useAuthStore } from "../stores/auth";
import { useDialogStore } from "../stores/dialog";
import { paperpilotApi } from "../services/paperpilotApi";
import { rememberLastReading } from "../utils/readingMemory";
import CheckinLottery from "../components/CheckinLottery.vue";

useScrollReveal(".library-spatial");

const router = useRouter();
const route = useRoute();
const libraryStore = useLibraryStore();
const authStore = useAuthStore();
const dialogStore = useDialogStore();
const isDesktopApp = Boolean(window.paperSolverDesktop?.isDesktop);
const keyword = ref("");
const openFilter = ref("");
const pageSizeOptions = [5, 10, 20, 50, 100];
const pageSize = ref(Number(localStorage.getItem("papersolver-library-page-size")) || 20);
const currentPage = ref(1);
const filterButtonRefs = ref({});
function registerFilterButton(key, el) {
  if (el) {
    filterButtonRefs.value[key] = el;
  } else {
    delete filterButtonRefs.value[key];
  }
}
const currentFilter = computed(() => filterDefs.find((f) => f.key === openFilter.value) || null);
const currentFilterOptions = computed(() => currentFilter.value?.options || []);
const currentFilterSelected = computed(() => currentFilter.value?.selected || []);
const filterMenuStyle = computed(() => {
  if (!openFilter.value) return {};
  const btn = filterButtonRefs.value[openFilter.value];
  if (!btn) return {};
  const rect = btn.getBoundingClientRect();
  return {
    position: "fixed",
    top: `${Math.round(rect.bottom + 6)}px`,
    left: `${Math.round(rect.left)}px`,
    minWidth: `${Math.max(Math.round(rect.width), 200)}px`,
    zIndex: 120,
  };
});
const filterDefs = reactive([
  { key: "venueType", label: "文献类型", selected: [], options: ["期刊", "会议", "预印本", "综述", "待分类"] },
  { key: "journalTag", label: "期刊标签", selected: [], options: [] },
  { key: "importSource", label: "导入源头", selected: [], options: [] },
  { key: "publishYear", label: "发表时间", selected: [], options: [] },
  { key: "progress", label: "阅读进度", selected: [], options: ["未读", "进行中", "已精读", "已读完"] },
]);
const noteEditor = ref({
  open: false,
  saving: false,
  loading: false,
  paper: null,
  text: "",
  plainText: "",
  mode: "tree",
});
const pdfLinkEditor = ref({
  open: false,
  saving: false,
  paper: null,
  file: null,
  fileName: "",
  error: "",
});

const journalTagEditor = ref({
  open: false,
  saving: false,
  paper: null,
  selected: [],
  activeGroup: 0,
  error: "",
});

const journalTagGroups = [
  { name: "JCR 分区", tags: ["JCR Q1", "JCR Q2", "JCR Q3", "JCR Q4"] },
  { name: "中科院分区", tags: ["中科院1区", "中科院2区", "中科院3区", "中科院4区"] },
  { name: "CCF 等级", tags: ["CCF A", "CCF B", "CCF C", "CCF 其他"] },
  { name: "影响因子", tags: ["IF 高", "IF 中", "IF 低", "IF 待查"] },
  { name: "索引收录", tags: ["SCI", "SSCI", "EI", "Scopus", "PubMed", "ESCI", "DOAJ"] },
];
const toastMessage = ref("");
const libraryTabs = [
  { id: "papers", label: "全部文献", description: "阅读、翻译与分析" },
  { id: "add", label: "个人文献添加", description: "题录与本地 PDF" },
  { id: "zotero", label: "Zotero 导入", description: "批量题录导入" },
  { id: "storage", label: "PDF 管理", description: "文件管理与替换" },
];
const validTabs = new Set(libraryTabs.map(item => item.id));
const activeTab = ref(validTabs.has(String(route.query.tab)) ? String(route.query.tab) : "papers");
const personalPaper = reactive({
  title: "",
  authors: "",
  publishYear: "",
  source: "个人文献",
  abstractText: "",
});
const personalPdf = ref(null);
const personalImporting = ref(false);
const zoteroFile = ref(null);
const zoteroImporting = ref(false);
const zoteroResult = ref(null);
const zoteroGuideOpen = ref(false);
const zoteroOnline = reactive({
  limit: 100,
  importing: false,
});
const uploadingWorkspace = ref("");
let toastTimer = null;

function progressBucket(paper) {
  const text = String(paper?.progress || "").trim();
  const num = parseInt(text.replace("%", ""), 10);
  if (Number.isNaN(num) || num <= 0) return "未读";
  if (num < 60) return "进行中";
  if (num < 100) return "已精读";
  return "已读完";
}

function matchesFilter(paper) {
  for (const filter of filterDefs) {
    if (!filter.selected.length) continue;
    let values = [];
    if (filter.key === "venueType") {
      values = [String(paper.venueType || "待分类")];
    } else if (filter.key === "journalTag") {
      values = journalFilterTags(paper);
    } else if (filter.key === "importSource") {
      values = [String(paper.importSource || sourceHost(paper.sourceUrl) || "未记录")];
    } else if (filter.key === "publishYear") {
      values = [String(paper.publishYear || "待补充")];
    } else if (filter.key === "progress") {
      values = [progressBucket(paper)];
    }
    if (!values.some((v) => filter.selected.includes(v))) return false;
  }
  return true;
}

const filteredDocuments = computed(() => {
  const text = keyword.value.trim().toLowerCase();
  const documents = libraryStore.state.documents.filter((paper) => {
    if (!matchesFilter(paper)) return false;
    if (!text) return true;
    return [
      paper.title,
      paper.authors,
      paper.note,
      paper.source,
      paper.workspaceId,
      paper.paperUrl,
      ...(paper.journalTags || []),
    ].some((field) =>
      String(field || "").toLowerCase().includes(text),
    );
  });
  return documents;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredDocuments.value.length / pageSize.value)));
const paginatedDocuments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredDocuments.value.slice(start, start + pageSize.value);
});
const visiblePageNumbers = computed(() => {
  const total = totalPages.value;
  const current = currentPage.value;
  const start = Math.max(1, Math.min(current - 2, total - 4));
  const end = Math.min(total, start + 4);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});
const filterSignature = computed(() =>
  filterDefs.map((filter) => `${filter.key}:${filter.selected.join("|")}`).join(";"),
);

function goToPage(page) {
  currentPage.value = Math.min(totalPages.value, Math.max(1, Number(page) || 1));
}

function refreshFilterOptions() {
  const docs = libraryStore.state.documents;
  const tagSet = new Set();
  const importSet = new Set();
  const yearSet = new Set();
  for (const paper of docs) {
    journalFilterTags(paper).forEach((t) => tagSet.add(t));
    importSet.add(String(paper.importSource || sourceHost(paper.sourceUrl) || "未记录"));
    yearSet.add(String(paper.publishYear || "待补充"));
  }
  const tagFilter = filterDefs.find((f) => f.key === "journalTag");
  const importFilter = filterDefs.find((f) => f.key === "importSource");
  const yearFilter = filterDefs.find((f) => f.key === "publishYear");
  if (tagFilter) tagFilter.options = Array.from(tagSet).sort();
  if (importFilter) importFilter.options = Array.from(importSet).sort();
  if (yearFilter) yearFilter.options = Array.from(yearSet).sort((a, b) => {
    const an = parseInt(a, 10);
    const bn = parseInt(b, 10);
    if (Number.isNaN(an) && Number.isNaN(bn)) return a.localeCompare(b);
    if (Number.isNaN(an)) return 1;
    if (Number.isNaN(bn)) return -1;
    return bn - an;
  });
}

watch([keyword, filterSignature, pageSize], () => {
  currentPage.value = 1;
  localStorage.setItem("papersolver-library-page-size", String(pageSize.value));
});

watch(totalPages, (pages) => {
  if (currentPage.value > pages) currentPage.value = pages;
});

function toggleFilter(key) {
  openFilter.value = openFilter.value === key ? "" : key;
}

function toggleFilterOption(key, option) {
  const filter = filterDefs.find((f) => f.key === key);
  if (!filter) return;
  const index = filter.selected.indexOf(option);
  if (index >= 0) filter.selected.splice(index, 1);
  else filter.selected.push(option);
}

function closeAllFilters(event) {
  const target = event?.target;
  if (target && (target.closest?.(".library-filter") || target.closest?.(".library-filters") || target.closest?.(".library-filter-menu-portal"))) return;
  openFilter.value = "";
}

const readableCount = computed(() => libraryStore.state.documents.filter((paper) => canTryRead(paper)).length);
const notesCount = computed(() => libraryStore.state.documents.filter((paper) => String(paper.note || "").trim()).length);
const storedCount = computed(() => libraryStore.state.documents.filter((paper) =>
  String(paper.paperUrl || "").includes("/api/papers/uploads/") || isDesktopCacheUrl(paper.paperUrl),
).length);
watch(() => route.query.tab, (tab) => {
  activeTab.value = validTabs.has(String(tab)) ? String(tab) : "papers";
});

function selectTab(tab) {
  activeTab.value = tab;
  router.replace({ path: "/library", query: tab === "papers" ? {} : { tab } });
}

function openZoteroGuide() {
  zoteroGuideOpen.value = true;
}

function closeZoteroGuide() {
  zoteroGuideOpen.value = false;
}

function venueTypeClass(type) {
  if (type === "会议") return "conference";
  if (type === "预印本") return "preprint";
  return "journal";
}

const JOURNAL_METRIC_PRESETS = [
  { key: "iscience", tags: ["IF 4.1", "JCR Q1", "中科院2区", "SCI"] },
  { key: "international dental journal", tags: ["IF 3.4", "JCR Q1", "中科院2区", "SCI"] },
  { key: "procedia computer science", tags: ["Scopus"] },
  { key: "findings of the association for computational linguistics", tags: ["ACL Findings", "CCF A"] },
  { key: "association for computational linguistics", tags: ["ACL", "CCF A"] },
  { key: "arxiv", tags: [] },
  { key: "nature", tags: ["IF 高", "JCR Q1", "中科院1区", "SCI"] },
  { key: "science", tags: ["IF 高", "JCR Q1", "中科院1区", "SCI"] },
];

function normalizeJournalTagLabel(tag) {
  let text = String(tag || "").trim().replace(/\(补充\)$/u, "").replace(/\s+/g, " ");
  if (!text) return "";
  if (/^(preprint|pre-print)$/i.test(text)) return "预印本";
  if (/^(research article|article)$/i.test(text)) return "研究论文";
  if (/^(conference proceedings|proceedings)$/i.test(text)) return "会议论文集";
  text = text
    .replace(/^JCR\s*Q([1-4])$/i, "JCR Q$1")
    .replace(/^JCRQ([1-4])$/i, "JCR Q$1")
    .replace(/^Q([1-4])$/i, "JCR Q$1")
    .replace(/^CAS\s*([1-4])$/i, "中科院$1区")
    .replace(/^中科院\s*([1-4])\s*区$/u, "中科院$1区")
    .replace(/^CCF\s*([ABC])$/i, (_, level) => `CCF ${level.toUpperCase()}`)
    .replace(/^IF[:：]?\s*(\d+(?:\.\d+)?)$/i, "IF $1");
  const upper = text.toUpperCase();
  if (["SCI", "SSCI", "EI", "ESCI", "DOAJ"].includes(upper)) return upper;
  if (upper === "SCOPUS") return "Scopus";
  if (upper === "PUBMED" || upper === "MEDLINE") return upper === "MEDLINE" ? "MEDLINE" : "PubMed";
  return text;
}

function isIgnoredJournalTag(tag) {
  const text = String(tag || "").trim().toLowerCase();
  return !text
    || /^(doi\.org|pdf|pdf已缓存|待关联pdf|已导入元数据|研究论文|article|research article)$/i.test(text)
    || /^https?:\/\//i.test(text)
    || /\.(com|cn|org|net|edu)(\/|$)/i.test(text);
}

function classifyJournalTag(tag) {
  const text = normalizeJournalTagLabel(tag);
  if (!text || isIgnoredJournalTag(text) || /^(IF|JCR|中科院)\s*--?$/i.test(text)) return null;
  if (/^(期刊|会议|预印本|会议论文|会议论文集|综述|review|proceedings|conference proceedings)$/iu.test(text)) return null;
  const ifMatch = text.match(/^IF\s*(高|中|低|待查|-|--|\d+(?:\.\d+)?)$/i);
  if (ifMatch) {
    const value = ifMatch[1].replace(/^--?$/, "--");
    const numeric = Number(value);
    const strength = value === "高" || numeric >= 10 ? "top" : value === "中" || numeric >= 5 ? "strong" : "";
    return { label: value, prefix: "IF", type: `if ${strength}`.trim() };
  }
  const jcrMatch = text.match(/^JCR\s*Q([1-4])$/i);
  if (jcrMatch) {
    const value = `Q${jcrMatch[1]}`;
    const strength = value === "Q1" ? "top" : value === "Q2" ? "strong" : "";
    return { label: value, prefix: "JCR", type: `jcr ${strength}`.trim() };
  }
  const casMatch = text.match(/^中科院([1-4])区$/u);
  if (casMatch) {
    const value = `${casMatch[1]}区`;
    const strength = value === "1区" ? "top" : value === "2区" ? "strong" : "";
    return { label: value, prefix: "中科院", type: `cas ${strength}`.trim() };
  }
  const ccfMatch = text.match(/^CCF\s*(A|B|C|其他)$/i);
  if (ccfMatch) {
    const value = ccfMatch[1].toUpperCase();
    const strength = value === "A" ? "top" : value === "B" ? "strong" : "";
    return { label: value, prefix: "CCF", type: `ccf ${strength}`.trim() };
  }
  if (/^(SCI|SSCI|EI|ESCI|DOAJ|Scopus|PubMed|MEDLINE)$/i.test(text)) {
    return { label: text, prefix: "索引", type: "index" };
  }
  if (/journal/i.test(text)) return null;
  if (/会议|ACL|NeurIPS|ICML|ICLR|AAAI|IJCAI|CVPR|ICCV|ECCV|SIGGRAPH|WWW|SIGIR|KDD|CHI|EMNLP|NAACL/i.test(text)) {
    return { label: text, prefix: "会议", type: "conference" };
  }
  return null;
}

function cleanJournalTags(tags) {
  return Array.from(new Set((Array.isArray(tags) ? tags : [])
    .map(normalizeJournalTagLabel)
    .filter((tag) => tag && tag.length <= 32 && classifyJournalTag(tag))));
}

function journalMetricTags(paper) {
  const manual = cleanJournalTags(paper?.journalTags);
  const tags = [...manual];
  const source = String(paper?.source || "").trim().toLowerCase();
  const ranking = String(paper?.venueRanking || "").trim().replace(/待核验|待自动核验|待补充|待查|待补/g, "");
  const preset = JOURNAL_METRIC_PRESETS.find((item) => source.includes(item.key));
  if (preset) tags.push(...preset.tags);
  if (ranking) {
    tags.push(...String(ranking).split(/[、,，/|]+/u).map((item) => item.trim()));
  }
  const classified = [];
  const seen = new Set();
  for (const tag of tags) {
    const item = classifyJournalTag(tag);
    if (!item) continue;
    const key = `${item.prefix}:${item.label}`;
    if (seen.has(key)) continue;
    seen.add(key);
    classified.push(item);
  }
  if (classified.length) return classified.slice(0, 5);
  return [{ label: "待核验", prefix: "标签", type: "muted" }];
}

function journalFilterTags(paper) {
  return journalMetricTags(paper)
    .map(journalMetricFullLabel)
    .map(normalizeJournalTagLabel)
    .filter((tag) => tag && tag !== "--");
}

function journalMetricFullLabel(metric) {
  if (!metric?.label || metric.label === "--") return metric?.label || "";
  if (["IF", "JCR", "中科院", "CCF"].includes(metric.prefix)) return `${metric.prefix} ${metric.label}`;
  return metric.label;
}

function journalMetricDisplayLabel(metric) {
  return journalMetricFullLabel(metric);
}

function publishTimeLabel(paper) {
  const year = String(paper?.publishYear || "").trim();
  if (year && year !== "-") return year;
  return "发表时间待补充";
}

function showToast(msg) {
  toastMessage.value = msg;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastMessage.value = "";
  }, 3000);
}

async function directDelete(paper) {
  const ok = await dialogStore.confirm(`确定删除文献《${paper.title || "未命名论文"}》吗？删除后会从文献库移除，已保存的阅读进度和关联信息也会一起清理。`, {
    title: "删除文献",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  try {
    await libraryStore.deleteDocument(paper.id);
    showToast(`文献《${paper.title}》已成功删除`);
  } catch (error) {
    console.error("Failed to delete paper", error);
    showToast("删除失败，请稍后重试");
  }
}

function openLineAiReader(paper) {
  libraryStore.setActiveDocument(paper.id);
  rememberLastReading(authStore.session.user, paper);
  const pdfSource = resolveReadablePdfSource(paper);
  if (!pdfSource) {
    openPdfLinkEditor(paper);
    return;
  }
  router.push({ path: "/reader", query: { mode: "line", panel: "analysis" } });
}

function openDualReader(paper) {
  libraryStore.setActiveDocument(paper.id);
  rememberLastReading(authStore.session.user, paper);
  if (!resolveReadablePdfSource(paper)) {
    openPdfLinkEditor(paper);
    return;
  }
  router.push("/reader/dual");
}

function resolveReadablePdfSource(paper) {
  const candidates = [paper?.pdfUrl, paper?.paperUrl].map((item) => String(item || "").trim()).filter(Boolean);
  return candidates.find((url) => isReadablePdfUrl(url)) || "";
}

function isReadablePdfUrl(url) {
  const normalized = paperpilotApi.normalizePdfUrl(url);
  const lower = normalized.toLowerCase();
  if (!normalized) return false;
  if (isDesktopCacheUrl(normalized)) return true;
  if (lower.startsWith("blob:") || lower.startsWith("data:")) return true;
  if (lower.includes("/api/papers/uploads/")) return true;
  if (lower.includes("sciencedirect.com/science/article/pii/") || lower.includes("pdf.sciencedirectassets.com")) return false;
  return paperpilotApi.isLikelyPdfUrl(normalized);
}

function officialPdfCandidate(paper) {
  const url = String(paper?.paperUrl || paper?.pdfUrl || "").trim();
  if (!url) return "";
  if (isReadablePdfUrl(url)) return "";
  const lower = url.toLowerCase();
  return lower.includes("sciencedirect.com/science/article/pii/")
    || lower.includes("pdf.sciencedirectassets.com")
    || lower.includes("doi.org")
    ? url
    : "";
}

function canTryRead(paper) {
  return Boolean(resolveReadablePdfSource(paper));
}

function isDesktopCacheUrl(url) {
  return String(url || "").trim().toLowerCase().startsWith("desktop-cache://");
}

function desktopCacheUrl(workspaceId) {
  return workspaceId ? `desktop-cache://${workspaceId}` : "";
}

function pdfHref(paper) {
  const source = resolveReadablePdfSource(paper);
  if (isDesktopCacheUrl(source)) return "";
  return source ? paperpilotApi.buildPdfProxyUrl(source) : "";
}

function sourceHost(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return "";
  }
}

function openPdfLinkEditor(paper) {
  pdfLinkEditor.value = {
    open: true,
    saving: false,
    paper,
    file: null,
    fileName: "",
    error: "",
  };
}

function editablePdfCandidate(paper) {
  const url = officialPdfCandidate(paper);
  if (!url) return "";
  const lower = url.toLowerCase();
  if (lower.includes("doi.org") || lower.includes("/pdfft")) return "";
  return url;
}

function closePdfLinkEditor() {
  if (pdfLinkEditor.value.saving) return;
  pdfLinkEditor.value.open = false;
}

function pickPdfUploadFile(event) {
  const file = event.target.files?.[0] || null;
  pdfLinkEditor.value.file = file;
  pdfLinkEditor.value.fileName = file ? file.name : "";
  pdfLinkEditor.value.error = "";
}

async function savePdfLinkEditor() {
  const paper = pdfLinkEditor.value.paper;
  const file = pdfLinkEditor.value.file;
  if (!paper) return;
  if (!file) {
    pdfLinkEditor.value.error = "请先选择本地 PDF 文件。";
    return;
  }
  pdfLinkEditor.value.saving = true;
  pdfLinkEditor.value.error = "";
  try {
    if (paper.workspaceId) {
      await cacheDesktopPdfFromFile(paper.workspaceId, file);
      if (isDesktopApp) {
        await libraryStore.persistDocumentPatch(paper.id, { paperUrl: desktopCacheUrl(paper.workspaceId) });
      } else {
        await paperpilotApi.uploadPaperPdf(paper.workspaceId, file);
      }
    } else {
      await libraryStore.persistDocumentPatch(paper.id, { paperUrl: "" });
    }
    await libraryStore.hydrateLibrary();
    const updated = libraryStore.state.documents.find((item) => item.id === paper.id);
    showToast(isDesktopApp ? "PDF 已保存到本机" : "PDF 已上传");
    pdfLinkEditor.value.open = false;
    if (updated && canTryRead(updated)) {
      openLineAiReader(updated);
    }
  } catch (error) {
    console.error("Failed to upload PDF", error);
    pdfLinkEditor.value.error = "上传失败，请稍后重试。";
  } finally {
    pdfLinkEditor.value.saving = false;
  }
}

function journalTagsSummary(paper) {
  const tags = journalMetricTags(paper);
  return tags.length
    ? tags.map(journalMetricFullLabel).join("、")
    : "点击设置期刊标签";
}

function openJournalTagEditor(paper) {
  const current = cleanJournalTags(paper?.journalTags);
  journalTagEditor.value = {
    open: true,
    saving: false,
    paper,
    selected: current,
    activeGroup: 0,
    error: "",
  };
}

function closeJournalTagEditor() {
  if (journalTagEditor.value.saving) return;
  journalTagEditor.value.open = false;
}

function toggleJournalTag(tag) {
  const selected = journalTagEditor.value.selected;
  const index = selected.indexOf(tag);
  if (index >= 0) {
    selected.splice(index, 1);
  } else {
    selected.push(tag);
  }
}

function journalTagChipClass(tag) {
  if (tag.startsWith("JCR Q")) return "chip-jcr";
  if (tag.startsWith("中科院")) return "chip-cas";
  if (tag.startsWith("CCF")) return "chip-ccf";
  if (tag.startsWith("IF")) return "chip-if";
  if (["SCI", "SSCI", "EI", "Scopus", "PubMed", "ESCI", "DOAJ"].includes(tag)) return "chip-index";
  if (["期刊", "会议", "会议论文集", "预印本", "综述"].includes(tag)) return "chip-type";
  return "chip-other";
}

async function saveJournalTagEditor() {
  const paper = journalTagEditor.value.paper;
  if (!paper) return;
  journalTagEditor.value.saving = true;
  journalTagEditor.value.error = "";
  try {
    const selected = cleanJournalTags(journalTagEditor.value.selected);
    await libraryStore.persistDocumentPatch(paper.id, { journalTags: selected });
    showToast("期刊标签已保存");
    journalTagEditor.value.open = false;
  } catch (error) {
    console.error("Failed to save journal tags", error);
    journalTagEditor.value.error = "保存失败，请稍后重试。";
  } finally {
    journalTagEditor.value.saving = false;
  }
}

function selectPersonalPdf(event) {
  personalPdf.value = event.target.files?.[0] || null;
}

function selectZoteroFile(event) {
  zoteroFile.value = event.target.files?.[0] || null;
  zoteroResult.value = null;
}

const zoteroIssueItems = computed(() =>
  (zoteroResult.value?.items || [])
    .filter((item) => item.status === "failed" || ["failed", "skipped"].includes(item.pdfStatus))
    .slice(0, 12),
);

function formatFileSize(size) {
  const value = Number(size || 0);
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

async function submitZoteroImport() {
  if (!zoteroFile.value || zoteroImporting.value) return;
  zoteroImporting.value = true;
  zoteroResult.value = null;
  try {
    const result = await paperpilotApi.importZoteroFile(zoteroFile.value);
    zoteroResult.value = result;
    await refreshLibraryFromBackend();
    refreshFilterOptions();
    showToast(`Zotero 已导入 ${result.imported || 0} 篇文献`);
    if (result.imported > 0) selectTab("papers");
  } catch (error) {
    console.error("zotero import failed", error);
    showToast(error?.response?.data?.message || "Zotero 导入失败，请检查导出文件格式");
  } finally {
    zoteroImporting.value = false;
  }
}

async function submitZoteroOnlineImport() {
  if (zoteroOnline.importing) return;
  zoteroOnline.importing = true;
  zoteroResult.value = null;
  try {
    const limit = Math.max(1, Math.min(200, Number(zoteroOnline.limit) || 100));
    const result = isDesktopApp
      ? await importZoteroLocalFromDesktop(limit)
      : await paperpilotApi.importZoteroLocal({ limit });
    zoteroResult.value = result;
    await refreshLibraryFromBackend();
    refreshFilterOptions();
    const pdfText = result.pdfUploaded ? `，${isDesktopApp ? "已本机保存 PDF" : "已补 PDF"} ${result.pdfUploaded} 篇` : "";
    const skippedText = result.pdfSkipped ? `，${result.pdfSkipped} 篇未补 PDF` : "";
    showToast(`Zotero 验证成功，已同步 ${result.imported || 0} 篇文献${pdfText}${skippedText}`);
    if (result.imported > 0 && !result.failed && !result.pdfSkipped) selectTab("papers");
  } catch (error) {
    console.error("zotero online import failed", error);
    showToast(error?.response?.data?.message || "未检测到本机 Zotero，请先打开 Zotero Desktop");
  } finally {
    zoteroOnline.importing = false;
  }
}

async function importZoteroLocalFromDesktop(limit) {
  if (!window.paperSolverDesktop?.importZoteroLocal) {
    throw new Error("当前桌面端不支持本机 Zotero 同步");
  }
  const localResult = await window.paperSolverDesktop.importZoteroLocal({ limit });
  const sourceItems = Array.isArray(localResult?.items) ? localResult.items.slice(0, limit) : [];
  const resultItems = [];
  let imported = 0;
  let failed = 0;
  let pdfUploaded = 0;
  let pdfSkipped = 0;
  for (const item of sourceItems) {
    const { localPdf, ...paperPayload } = item;
    const row = { title: item.title || "未命名文献" };
    try {
      const workspace = await paperpilotApi.importPaper(paperPayload);
      imported += 1;
      row.status = "imported";
      row.workspaceId = workspace?.workspaceId || "";
      if (row.workspaceId && localPdf) {
        try {
          const payload = await readDesktopZoteroPdfPayload(localPdf);
          await cacheDesktopPdf(row.workspaceId, payload);
          await paperpilotApi.updateLibraryPaper(row.workspaceId, { paperUrl: desktopCacheUrl(row.workspaceId) });
          pdfUploaded += 1;
          row.pdfStatus = "cached";
        } catch (pdfError) {
          pdfSkipped += 1;
          row.pdfStatus = "failed";
          row.pdfMessage = pdfError?.message || (isDesktopApp ? "PDF 附件保存到本机失败" : "PDF 附件上传失败");
        }
      } else {
        pdfSkipped += 1;
        row.pdfStatus = "skipped";
        row.pdfMessage = item.localPdfMessage || pdfStatusText(item.localPdfStatus) || "Zotero 条目下没有可读取的 PDF 附件。";
      }
    } catch (error) {
      failed += 1;
      row.status = "failed";
      row.message = error?.response?.data?.message || error?.message || "导入失败";
    }
    resultItems.push(row);
  }
  return {
    fileName: "Zotero 本机同步（桌面端）",
    detected: sourceItems.length,
    imported,
    failed,
    limited: Boolean(localResult?.limited) || sourceItems.length >= limit,
    verified: true,
    local: true,
    desktop: true,
    pdfUploaded,
    pdfSkipped,
    items: resultItems,
  };
}

function pdfStatusText(status) {
  if (status === "missing") return "Zotero 条目下没有可读取的 PDF 附件。";
  if (status === "failed") return "读取 Zotero PDF 附件信息失败。";
  if (status === "found") return "已找到 PDF，但保存流程未完成。";
  return "";
}

async function readDesktopZoteroPdfPayload(pdfRef) {
  if (!window.paperSolverDesktop?.readZoteroPdf) {
    throw new Error("当前桌面端不支持自动读取 Zotero PDF");
  }
  const payload = await window.paperSolverDesktop.readZoteroPdf(pdfRef);
  if (!payload?.base64) {
    throw new Error("Zotero PDF 内容为空");
  }
  return payload;
}

async function cacheDesktopPdf(workspaceId, payload) {
  if (!window.paperSolverDesktop?.cachePdf || !workspaceId || !payload?.base64) return;
  try {
    await window.paperSolverDesktop.cachePdf({
      workspaceId,
      fileName: payload.fileName || "zotero-attachment.pdf",
      mimeType: payload.mimeType || "application/pdf",
      base64: payload.base64,
    });
  } catch (error) {
    console.warn("desktop pdf cache failed", error);
  }
}

async function cacheDesktopPdfFromFile(workspaceId, file) {
  if (!window.paperSolverDesktop?.cachePdf || !workspaceId || !file) return;
  const base64 = await fileToBase64(file);
  await cacheDesktopPdf(workspaceId, {
    fileName: file.name || `${workspaceId}.pdf`,
    mimeType: file.type || "application/pdf",
    base64,
  });
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const value = String(reader.result || "");
      resolve(value.includes(",") ? value.split(",").pop() : value);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function desktopPdfPayloadToFile(payload) {
  const binary = atob(payload.base64);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  const fileName = payload.fileName || "zotero-attachment.pdf";
  return new File([bytes], fileName, { type: payload.mimeType || "application/pdf" });
}

function resetPersonalPaper() {
  Object.assign(personalPaper, {
    title: "",
    authors: "",
    publishYear: "",
    source: "个人文献",
    abstractText: "",
  });
  personalPdf.value = null;
}

async function submitPersonalPaper() {
  if (!personalPaper.title.trim() || personalImporting.value) return;
  if (!personalPdf.value) {
    showToast("请选择本地 PDF 文件后再添加");
    return;
  }
  personalImporting.value = true;
  try {
    const result = await paperpilotApi.importPaper({
      source: personalPaper.source.trim() || "个人文献",
      importSource: "个人添加",
      title: personalPaper.title.trim(),
      authors: personalPaper.authors.trim(),
      publishYear: personalPaper.publishYear.trim(),
      abstractText: personalPaper.abstractText.trim(),
    });
    if (result?.workspaceId) {
      await cacheDesktopPdfFromFile(result.workspaceId, personalPdf.value);
      if (isDesktopApp) {
        await paperpilotApi.updateLibraryPaper(result.workspaceId, { paperUrl: desktopCacheUrl(result.workspaceId) });
      } else {
        await paperpilotApi.uploadPaperPdf(result.workspaceId, personalPdf.value);
      }
    }
    await refreshLibraryFromBackend();
    showToast("个人文献已添加到个人文献库");
    resetPersonalPaper();
    selectTab("papers");
  } catch (error) {
    console.error("personal paper import failed", error);
    showToast(error?.response?.data?.message || "个人文献添加失败");
  } finally {
    personalImporting.value = false;
  }
}

async function uploadReplacementPdf(paper, event) {
  const file = event.target.files?.[0];
  if (!file || !paper?.workspaceId || uploadingWorkspace.value) return;
  uploadingWorkspace.value = paper.workspaceId;
  try {
    await cacheDesktopPdfFromFile(paper.workspaceId, file);
    if (isDesktopApp) {
      await libraryStore.persistDocumentPatch(paper.id, { paperUrl: desktopCacheUrl(paper.workspaceId) });
    } else {
      await paperpilotApi.uploadPaperPdf(paper.workspaceId, file);
    }
    await refreshLibraryFromBackend();
    showToast(isDesktopApp ? `《${paper.title}》PDF 已保存到本机` : `《${paper.title}》PDF 已更新`);
  } catch (error) {
    console.error("paper upload failed", error);
    showToast(isDesktopApp ? "PDF 保存失败" : "PDF 上传失败");
  } finally {
    uploadingWorkspace.value = "";
    event.target.value = "";
  }
}

async function openNoteEditor(paper) {
  const initialNote = readMirroredHierarchicalNote(paper) || paper.note || "";
  noteEditor.value = {
    open: true,
    saving: false,
    loading: Boolean(paper.workspaceId),
    paper,
    text: initialNote,
    plainText: markdownToReadableNoteText(initialNote),
    mode: "tree",
  };
  if (!paper.workspaceId) return;
  try {
    const latest = await paperpilotApi.getLibraryPaper(paper.workspaceId);
    const latestNote = String(latest?.note || "");
    libraryStore.updateDocument(paper.id, {
      note: latestNote,
      paperUrl: latest?.paperUrl ?? paper.paperUrl,
      sourceUrl: latest?.sourceUrl ?? paper.sourceUrl,
    });
    if (noteEditor.value.paper?.id === paper.id) {
      noteEditor.value.paper = { ...paper, ...latest, id: paper.id, workspaceId: paper.workspaceId };
      noteEditor.value.text = readMirroredHierarchicalNote(paper) || latestNote;
      noteEditor.value.plainText = markdownToReadableNoteText(noteEditor.value.text);
    }
  } catch (error) {
    console.warn("Failed to fetch latest library note", error);
    showToast("读取最新笔记失败，已显示本地同步内容");
  } finally {
    if (noteEditor.value.paper?.id === paper.id) {
      noteEditor.value.loading = false;
    }
  }
}

function closeNoteEditor() {
  if (noteEditor.value.saving) return;
  noteEditor.value.open = false;
}

async function saveNoteEditor() {
  const paper = noteEditor.value.paper;
  if (!paper) return;
  noteEditor.value.saving = true;
  try {
    const note = noteEditor.value.mode === "markdown"
      ? markdownToReadableNoteText(noteEditor.value.plainText)
      : markdownToReadableNoteText(noteEditor.value.text);
    await libraryStore.persistDocumentPatch(paper.id, { note });
    noteEditor.value.text = note;
    noteEditor.value.plainText = note;
    showToast("笔记已保存");
    noteEditor.value.open = false;
  } catch (error) {
    console.error("Failed to save note", error);
    showToast("笔记保存失败，请稍后重试");
  } finally {
    noteEditor.value.saving = false;
  }
}

function readMirroredHierarchicalNote(paper) {
  const key = String(paper?.workspaceId || paper?.id || "");
  if (!key || typeof localStorage === "undefined") return "";
  return String(localStorage.getItem(`paperpilot_hierarchical_notes_${key}_markdown_mirror`) || "").trim();
}

const parsedNoteTree = computed(() => parseLibraryNoteTree(noteEditor.value.text, noteEditor.value.paper?.title || ""));

function parseLibraryNoteTree(markdown, paperTitle = "") {
  const source = String(markdown || "").replace(/\r\n/g, "\n").trim();
  if (!source) return [];
  const normalizedPaperTitle = normalizeNoteTitle(paperTitle);
  const roots = [];
  const stack = [];
  let autoId = 0;
  const pushContent = (text) => {
    const cleaned = cleanReadableNoteLine(text);
    if (!cleaned) return;
    const target = stack[stack.length - 1];
    if (!target) {
      roots.push({
        id: `note-auto-${autoId++}`,
        title: "阅读笔记",
        level: 1,
        content: [cleaned],
        children: [],
      });
      stack[0] = roots[roots.length - 1];
      return;
    }
    target.content.push(cleaned);
  };

  for (const rawLine of source.split("\n")) {
    const line = rawLine.trim();
    if (!line) continue;
    const heading = line.match(/^(#{1,6})\s+(.+)$/u);
    if (heading) {
      const title = cleanReadableNoteLine(heading[2]);
      if (heading[1].length === 1 && normalizeNoteTitle(title) === normalizedPaperTitle) continue;
      const node = {
        id: `note-heading-${autoId++}`,
        title,
        level: Math.min(heading[1].length, 3),
        content: [],
        children: [],
      };
      while (stack.length && stack[stack.length - 1].level >= node.level) stack.pop();
      if (stack.length) stack[stack.length - 1].children.push(node);
      else roots.push(node);
      stack.push(node);
      continue;
    }
    const indentedTitle = line.match(/^(\s*)(?:[一二三四五六七八九十]+[、.．]|[0-9]+(?:\.[0-9]+)*[、.．]?)\s*(.+)$/u);
    if (indentedTitle && indentedTitle[1].length <= 2 && !/[。！？；：:]$/u.test(indentedTitle[2])) {
      const node = {
        id: `note-heading-${autoId++}`,
        title: cleanReadableNoteLine(line),
        level: indentedTitle[1].length ? 2 : 1,
        content: [],
        children: [],
      };
      while (stack.length && stack[stack.length - 1].level >= node.level) stack.pop();
      if (stack.length) stack[stack.length - 1].children.push(node);
      else roots.push(node);
      stack.push(node);
      continue;
    }
    const bullet = line.match(/^([-*+]|\d+[.)])\s+(.+)$/u);
    if (bullet) {
      const bulletText = cleanReadableNoteLine(bullet[2]);
      if (/^[^。！？；:：]{2,32}$/u.test(bulletText)) {
        const node = {
          id: `note-heading-${autoId++}`,
          title: bulletText,
          level: Math.min((stack[stack.length - 1]?.level || 1) + 1, 3),
          content: [],
          children: [],
        };
        while (stack.length && stack[stack.length - 1].level >= node.level) stack.pop();
        if (stack.length) stack[stack.length - 1].children.push(node);
        else roots.push(node);
        stack.push(node);
        continue;
      }
      pushContent(bullet[2]);
      continue;
    }
    pushContent(line);
  }

  return roots.map((section, index) => numberLibraryNoteNode(section, `${index + 1}`));
}

function normalizeNoteTitle(text) {
  return String(text || "").replace(/\s+/g, " ").trim().toLowerCase();
}

function numberLibraryNoteNode(node, index) {
  return {
    ...node,
    index,
    children: (node.children || []).map((child, childIndex) => numberLibraryNoteNode(child, `${index}.${childIndex + 1}`)),
  };
}

function cleanReadableNoteLine(text) {
  return String(text || "")
    .replace(/^>\s?/, "")
    .replace(/^\s*[-•·○◦▪▫]+\s*/u, "")
    .replace(/[#*_`~]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function markdownToReadableNoteText(text) {
  return String(text || "")
    .replace(/\r\n/g, "\n")
    .split("\n")
    .map(cleanReadableNoteLine)
    .filter(Boolean)
    .join("\n");
}

async function refreshLibraryFromBackend() {
  try {
    await libraryStore.hydrateLibrary();
  } catch (error) {
    console.warn("library hydrate fallback", error);
  }
}

function handleVisibilityRefresh() {
  if (!document.hidden) {
    refreshLibraryFromBackend();
  }
}

function handleDocumentClick(event) {
  const target = event.target;
  if (target && (target.closest?.(".library-filter") || target.closest?.(".library-filter-menu") || target.closest?.(".library-filter-menu-portal"))) return;
  openFilter.value = "";
}

onMounted(async () => {
  await refreshLibraryFromBackend();
  refreshFilterOptions();
  window.addEventListener("focus", refreshLibraryFromBackend);
  document.addEventListener("visibilitychange", handleVisibilityRefresh);
  document.addEventListener("click", handleDocumentClick);
});

watch(
  () => libraryStore.state.documents,
  () => refreshFilterOptions(),
  { deep: true },
);

onUnmounted(() => {
  window.removeEventListener("focus", refreshLibraryFromBackend);
  document.removeEventListener("visibilitychange", handleVisibilityRefresh);
  document.removeEventListener("click", handleDocumentClick);
});
</script>

<style scoped>
.library-spatial .spatial-chapter {
  margin: 0;
  padding-left: 0;
  padding-right: 0;
}

.library-workbench-head {
  padding-top: 8px !important;
  padding-bottom: 16px !important;
}

.library-head-inner {
  display: block;
}

.library-head-actions {
  display: flex;
  align-items: stretch;
  justify-content: stretch;
  gap: 12px;
  width: 100%;
  min-width: 0;
}

.library-nav-row {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 24px;
  margin: 0;
  border-bottom: 1px solid var(--spatial-line);
}

.library-head-stats {
  display: flex;
  align-items: stretch;
  gap: 8px;
  flex: 0 0 auto;
}

.library-stats-row {
  justify-content: flex-end;
  align-self: stretch;
  margin: 0;
}

.library-head-stat {
  width: 86px;
  min-height: 68px;
  display: grid;
  align-content: center;
  padding: 0 16px;
  border: 0;
  border-left: 1px solid var(--spatial-line);
  border-radius: 0;
  background: transparent;
}

.library-head-stat span {
  display: block;
  color: var(--spatial-graphite);
  font-size: 22px;
  font-weight: 850;
  line-height: 1;
  letter-spacing: -0.02em;
}

.library-head-stat small {
  display: block;
  margin-top: 7px;
  color: var(--spatial-gray);
  font-size: 12px;
  font-weight: 700;
}

.toolbar-count {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid var(--spatial-line);
  border-radius: 4px;
  color: var(--spatial-gray);
  background: var(--spatial-surface);
  font-size: 12px;
  font-weight: 800;
}

.library-subnav {
  display: flex;
  align-items: stretch;
  gap: 0;
  flex: 0 0 min(860px, calc(100% - 310px));
  min-width: 0;
  margin: 0;
  padding: 0;
}

.library-subnav button {
  position: relative;
  min-width: 0;
  flex: 1 1 0;
  display: grid;
  gap: 4px;
  align-content: center;
  min-height: 68px;
  padding: 10px 18px;
  border: 0;
  border-radius: 0;
  color: var(--spatial-gray);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: color .15s ease, background-color .15s ease;
}

.library-subnav button::after {
  position: absolute;
  right: 18px;
  bottom: -1px;
  left: 18px;
  height: 2px;
  background: transparent;
  content: "";
}

.library-subnav button:hover { color: var(--spatial-graphite); background: color-mix(in srgb, var(--spatial-warm-2) 55%, transparent); }
.library-subnav button.active { color: var(--spatial-accent); background: transparent; }
.library-subnav button.active::after { background: var(--spatial-accent); }
.library-subnav button:focus-visible {
  z-index: 1;
  outline: 2px solid var(--spatial-accent);
  outline-offset: -2px;
}
.library-subnav strong { font-size: 13px; }
.library-subnav small { color: inherit; font-size: 10px; opacity: .78; }

.library-management-panel {
  min-height: 420px;
  padding: 24px;
  border: 1px solid var(--spatial-line);
  border-radius: 14px;
  background: var(--spatial-surface);
}

.library-management-panel > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--spatial-line);
}

.library-management-panel h2 { margin: 0; color: var(--spatial-graphite); font-size: 20px; }
.library-management-panel header p { max-width: 70ch; margin: 7px 0 0; color: var(--spatial-gray); font-size: 13px; line-height: 1.6; }

.zotero-import-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(340px, 440px);
  gap: 20px;
  margin-top: 22px;
  padding: 18px;
  border: 1px solid #cfddec;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.94), rgba(255, 255, 255, 0.96) 48%, rgba(236, 253, 245, 0.88)),
    #ffffff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.zotero-copy {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 100%;
  padding: 20px;
  border: 1px solid rgba(59, 130, 246, 0.16);
  border-radius: 14px;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.16), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(248, 250, 252, 0.92));
}

.zotero-copy > span {
  width: max-content;
  padding: 5px 10px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.13);
  font-size: 10px;
  font-weight: 850;
  letter-spacing: .08em;
}

.zotero-copy h3 {
  margin: 0;
  color: var(--spatial-graphite);
  font-size: 24px;
  line-height: 1.22;
  letter-spacing: 0;
}

.zotero-copy p {
  max-width: 68ch;
  margin: 0;
  color: #53647a;
  font-size: 13.5px;
  line-height: 1.7;
}

.zotero-step-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 4px;
}

.zotero-step-grid div {
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
}

.zotero-step-grid b {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  margin-bottom: 9px;
  border-radius: 999px;
  color: #ffffff;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-size: 11px;
}

.zotero-step-grid strong,
.zotero-step-grid small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.zotero-step-grid strong {
  color: #1f2a3d;
  font-size: 12px;
}

.zotero-step-grid small {
  margin-top: 4px;
  color: #64748b;
  font-size: 10.5px;
}

.zotero-format-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.zotero-format-row b {
  padding: 5px 9px;
  border: 1px solid #d5e0ee;
  border-radius: 999px;
  color: #36506f;
  background: #ffffff;
  font-size: 11px;
}

.zotero-action-box {
  display: grid;
  align-content: start;
  gap: 12px;
}

.zotero-online-card {
  display: grid;
  gap: 13px;
  padding: 16px;
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 14px 36px rgba(37, 99, 235, 0.08);
}

.zotero-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.zotero-card-head strong,
.zotero-card-head small {
  display: block;
}

.zotero-card-head strong {
  color: #172033;
  font-size: 15px;
}

.zotero-card-head small {
  margin-top: 4px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.45;
}

.zotero-card-head > span {
  flex: 0 0 auto;
  padding: 4px 8px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.14);
  font-size: 11px;
  font-weight: 850;
}

.zotero-online-card label {
  display: flex;
  align-items: center;
  gap: 10px;
}

.zotero-online-card label span {
  flex: 0 0 auto;
  color: #53647a;
  font-size: 11px;
  font-weight: 800;
}

.zotero-online-card input {
  min-width: 0;
  flex: 1 1 auto;
  box-sizing: border-box;
  border: 1px solid #cbd8e8;
  border-radius: 9px;
  padding: 9px 10px;
  color: #1f2a3d;
  background: #fff;
  font-size: 13px;
}

.zotero-limit-field em {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.zotero-online-card small {
  color: #6c7e93;
  font-size: 11px;
  line-height: 1.55;
}

.zotero-safe-note {
  padding: 9px 10px;
  border-radius: 10px;
  background: rgba(15, 118, 110, 0.08);
}

.zotero-help-strip {
  display: grid;
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(20, 184, 166, 0.14);
  border-radius: 12px;
  background: rgba(15, 118, 110, 0.07);
}

.zotero-help-strip small {
  color: #5e7087;
  font-size: 11px;
  line-height: 1.55;
}

.zotero-help-strip button {
  width: max-content;
  border: 0;
  border-bottom: 1px solid currentColor;
  padding: 0 0 2px;
  color: #0f766e;
  background: transparent;
  font-size: 12px;
  font-weight: 850;
  cursor: pointer;
}

.zotero-help-strip button:hover {
  color: #2563eb;
}

.zotero-guide-backdrop {
  position: fixed;
  inset: 0;
  z-index: 520;
  display: grid;
  place-items: center;
  padding: 28px;
  background: rgba(2, 6, 23, 0.62);
}

.zotero-guide-dialog {
  width: min(1040px, calc(100vw - 48px));
  max-height: min(860px, calc(100vh - 56px));
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.28);
}

.zotero-guide-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid #e2e8f0;
  background: linear-gradient(180deg, #f8fbff, #ffffff);
}

.zotero-guide-head span {
  color: #2563eb;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: .08em;
}

.zotero-guide-head h2 {
  margin: 7px 0 7px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

.zotero-guide-head p {
  margin: 0;
  color: #52637a;
  font-size: 13px;
  line-height: 1.6;
}

.zotero-guide-head button {
  width: 34px;
  height: 34px;
  border: 1px solid #d7e2ef;
  border-radius: 10px;
  color: #64748b;
  background: #ffffff;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.zotero-guide-body {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(300px, .88fr);
  gap: 0;
  max-height: calc(min(860px, 100vh - 56px) - 102px);
  overflow: auto;
}

.zotero-guide-image {
  margin: 0;
  padding: 22px;
  border-right: 1px solid #e2e8f0;
  background: #eef4fb;
}

.zotero-guide-image img {
  display: block;
  width: 100%;
  height: auto;
  border: 1px solid #d7e2ef;
  border-radius: 12px;
  background: #111827;
}

.zotero-guide-content {
  display: grid;
  align-content: start;
  gap: 20px;
  padding: 24px;
}

.zotero-guide-content h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 16px;
}

.zotero-guide-content ol,
.zotero-guide-content ul {
  margin: 0;
  padding-left: 22px;
  color: #243246;
  font-size: 14px;
  line-height: 1.8;
}

.zotero-guide-content li + li {
  margin-top: 6px;
}

.zotero-guide-content strong {
  color: #0f172a;
}

.zotero-guide-content code {
  padding: 2px 7px;
  border-radius: 7px;
  color: #2563eb;
  background: #eef5ff;
  font-weight: 800;
}

.zotero-divider {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 9px;
  color: #8794a8;
  font-size: 11px;
  font-weight: 800;
}

.zotero-divider::before,
.zotero-divider::after {
  content: "";
  height: 1px;
  background: #d9e3f0;
}

.zotero-file-drop {
  position: relative;
  display: grid;
  gap: 4px;
  min-height: 92px;
  align-content: center;
  padding: 15px 16px 15px 54px;
  border: 1px dashed #91b3df;
  border-radius: 12px;
  color: #244a7b;
  background: #f4f8ff;
  cursor: pointer;
  transition: border-color .18s ease, background .18s ease, transform .18s ease;
}

.zotero-file-drop:hover {
  transform: translateY(-1px);
  border-color: #2563eb;
  background: #eef6ff;
}

.zotero-file-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
  transform: translateY(-50%);
}

.zotero-file-drop input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.zotero-file-drop strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.zotero-file-drop small {
  color: #6c7e93;
  font-size: 11px;
}

.zotero-file-import-btn {
  width: 100%;
}

.zotero-result {
  display: grid;
  gap: 3px;
  padding: 11px 12px;
  border: 1px solid #a9efd2;
  border-radius: 11px;
  color: #047857;
  background: #edfff7;
}

.zotero-result.partial {
  border-color: #fed7aa;
  color: #9a3412;
  background: #fff7ed;
}

.zotero-result strong,
.zotero-result span {
  font-size: 12px;
  line-height: 1.45;
}

.zotero-failed-details {
  padding: 10px 12px;
  border: 1px solid #e5edf6;
  border-radius: 11px;
  background: #fff;
}

.zotero-failed-details summary {
  color: #36506f;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.zotero-failed-details p {
  display: grid;
  gap: 2px;
  margin: 9px 0 0;
  color: #6b7280;
  font-size: 11px;
  line-height: 1.5;
}

.zotero-failed-details strong {
  color: #26364d;
}

.personal-paper-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 0;
}

.personal-paper-form label { display: grid; gap: 7px; }
.personal-paper-form label > span { color: var(--spatial-graphite); font-size: 12px; font-weight: 750; }
.personal-paper-form input,
.personal-paper-form textarea {
  box-sizing: border-box;
  width: 100%;
  border: 1px solid var(--spatial-line);
  border-radius: 9px;
  padding: 10px 12px;
  outline: none;
  color: var(--spatial-graphite);
  background: var(--spatial-surface-2);
  font: 13px/1.6 inherit;
}
.personal-paper-form input:focus,
.personal-paper-form textarea:focus { border-color: #7fb1ff; box-shadow: 0 0 0 3px rgba(9,105,247,.08); }
.personal-paper-form textarea { resize: vertical; }
.field-wide { grid-column: 1 / -1; }
.personal-paper-form footer { display: flex; justify-content: flex-end; gap: 10px; }

.file-drop {
  position: relative;
  padding: 18px;
  border: 1px dashed #9bb8dc;
  border-radius: 10px;
  color: #315a8a;
  background: #f3f7fc;
  cursor: pointer;
}
.file-drop input, .replace-upload input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.file-drop strong { font-size: 13px; }
.file-drop small { color: #6d7e92; font-size: 11px; }

.storage-summary { display: grid; justify-items: end; }
.storage-summary strong { color: var(--spatial-accent); font-size: 26px; line-height: 1; }
.storage-summary span { margin-top: 6px; color: var(--spatial-gray); font-size: 11px; }
.storage-list { display: grid; margin-top: 8px; }
.storage-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 15px 2px;
  border-bottom: 1px solid var(--spatial-line);
}
.storage-list article > div { min-width: 0; display: grid; gap: 4px; }
.storage-list article strong { overflow: hidden; color: var(--spatial-graphite); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.storage-list article span { color: var(--spatial-gray); font-size: 11px; }
.replace-upload {
  position: relative;
  flex: 0 0 auto;
  padding: 7px 11px;
  border: 1px solid #a9c7ef;
  border-radius: 7px;
  color: var(--spatial-accent);
  background: #f3f7ff;
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
}

.sync-facts { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1px; margin-top: 26px; background: var(--spatial-line); }
.sync-facts div { display: grid; gap: 7px; padding: 26px; background: var(--spatial-surface); text-align: center; }
.sync-facts strong { color: var(--spatial-graphite); font-size: 25px; }
.sync-facts span { color: var(--spatial-gray); font-size: 12px; }

.spatial-btn-dual {
  border-color: #087f8c;
  color: #fff;
  background: #087f8c;
}
.spatial-btn-dual:hover { background: #066a75; }

.spatial-btn-line-ai {
  position: relative;
  display: inline-flex !important;
  align-items: center;
  border-color: #6d28d9;
  color: #fff;
  background: #7c3aed;
}
.spatial-btn-line-ai:hover { color: #fff; background: #6d28d9; }
.reader-recommend-badge {
  position: absolute;
  top: -9px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border: 2px solid #fff;
  border-radius: 999px;
  color: #7a2e00;
  background: linear-gradient(135deg, #fde68a, #f59e0b);
  box-shadow: 0 6px 14px rgba(245, 158, 11, 0.28);
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  line-height: 1;
}

.library-toolbar-left,
.library-toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.library-toolbar-left {
  flex: 1 1 auto;
  flex-wrap: wrap;
}

.library-toolbar-right {
  flex: 0 0 auto;
}

.toolbar-search {
  width: min(360px, 100%);
  min-height: 40px;
  border: 1px solid var(--spatial-line);
  border-radius: 4px;
  padding: 0 12px;
  color: var(--spatial-graphite);
  background: var(--spatial-surface);
  font: inherit;
  font-size: 14px;
  outline: none;
}

.toolbar-search:focus {
  border-color: var(--spatial-accent);
  box-shadow: 0 0 0 2px var(--spatial-accent-soft);
}

.toolbar-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--spatial-line);
  border-radius: 4px;
  color: var(--spatial-gray);
  background: var(--spatial-surface);
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition: color .15s ease, border-color .15s ease, background-color .15s ease;
}

.toolbar-chip em {
  font-style: normal;
  font-size: 10px;
  opacity: .6;
}

.toolbar-chip:hover {
  color: var(--spatial-graphite);
  border-color: color-mix(in srgb, var(--spatial-gray) 40%, var(--spatial-line));
  background: var(--spatial-warm-2);
}

.toolbar-chip.active {
  color: var(--spatial-accent);
  background: var(--spatial-accent-soft);
  border-color: color-mix(in srgb, var(--spatial-accent) 45%, var(--spatial-line));
}

.toolbar-chip.active em {
  color: var(--spatial-accent);
  opacity: .9;
}

.toolbar-chip-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 3px;
  background: var(--spatial-surface);
  color: var(--spatial-accent);
  font-size: 10px;
  font-weight: 800;
}

.library-toolbar {
  position: relative;
  z-index: 50;
  isolation: isolate;
  min-height: 64px;
  margin: 0;
  padding: 14px 0;
  border: 0;
  border-bottom: 1px solid var(--spatial-line);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
  contain: layout paint;
}

.library-toolbar .spatial-btn-ghost {
  border-radius: 4px;
  box-shadow: none;
}

.library-filters {
  position: relative;
  z-index: 50;
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
  contain: layout;
}

.library-filter {
  position: relative;
  display: inline-flex;
}

.library-filter-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 60;
  min-width: 200px;
  max-height: 280px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid var(--spatial-line);
  border-radius: 12px;
  background: var(--spatial-surface);
  box-shadow: 0 12px 32px rgba(15, 23, 42, .12);
}

.library-filter-menu-portal {
  min-width: 150px;
  max-height: 320px;
  overflow-y: auto;
  padding: 7px;
  border: 1px solid var(--spatial-line);
  border-radius: 12px;
  background: var(--spatial-surface);
  box-shadow: 0 12px 32px rgba(15, 23, 42, .18);
}

.library-filter-option {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  align-items: center;
  column-gap: 10px;
  min-width: 0;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 650;
  color: var(--spatial-graphite);
  cursor: pointer;
  white-space: nowrap;
  word-break: keep-all;
}

.library-filter-option span {
  display: inline-block;
  min-width: 0;
  white-space: nowrap;
  word-break: keep-all;
  text-align: left;
}

.library-filter-option:hover {
  background: var(--spatial-warm-2);
}

.library-filter-option input {
  width: 16px;
  height: 16px;
  margin: 0;
  accent-color: #7c3aed;
}

.library-filter-empty {
  padding: 8px;
  color: var(--spatial-gray);
  font-size: 12px;
}

/* Make table headers and cells compact */
:deep(.library-table) {
  min-width: 1974px !important;
  width: max(100%, 1974px) !important;
  table-layout: fixed !important;
  border-collapse: collapse !important;
  background: var(--spatial-surface) !important;
}

.col-check { width: 44px; }
.col-title { width: 390px; }
.col-note { width: 120px; }
.col-authors { width: 250px; }
.col-type { width: 132px; }
.col-ranking { width: 245px; }
.col-import-source { width: 145px; }
.col-publish { width: 115px; }
.col-progress { width: 100px; }
.col-time { width: 160px; }
.col-actions { width: 300px; }

.library-table-scroll {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-gutter: stable;
  overscroll-behavior-x: contain;
  contain: layout paint;
}

.library-table-scroll::-webkit-scrollbar {
  height: 10px;
}

.library-table-scroll::-webkit-scrollbar-track {
  border-radius: 999px;
  background: var(--spatial-warm-2);
}

.library-table-scroll::-webkit-scrollbar-thumb {
  border: 2px solid var(--spatial-warm-2);
  border-radius: 999px;
  background: var(--spatial-silver);
}

.library-table-scroll::-webkit-scrollbar-thumb:hover {
  background: #748399;
}

:deep(.library-table thead th) {
  padding: 9px 10px !important;
  background: var(--spatial-surface-2) !important;
  font-size: 12px !important;
  font-weight: 760 !important;
  color: var(--spatial-gray) !important;
  border-bottom: 1px solid var(--spatial-line) !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
}

:deep(.library-table tbody td) {
  padding: 11px 10px !important;
  font-size: 12.5px !important;
  vertical-align: middle !important;
  border-bottom: 1px solid var(--spatial-line) !important;
  color: var(--spatial-graphite) !important;
}

:deep(.library-table tbody tr) {
  height: 92px;
  transition: background-color .15s ease;
}

:deep(.library-table .action-cell) {
  width: 300px !important;
  min-width: 300px !important;
  overflow: visible !important;
  text-overflow: clip !important;
}

/* Document title metadata layout */
:deep(.doc-title-cell) {
  overflow: hidden;
}

.doc-title-main {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--text-main);
  cursor: help;
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.doc-title-sub {
  display: flex;
  align-items: center;
  min-width: 0;
  margin-top: 5px;
  font-size: 10.5px;
  color: var(--text-secondary);
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.source-text {
  max-width: 285px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-note-cell {
  width: 120px;
  max-width: 120px;
  overflow: hidden;
  color: #4b5563;
  text-align: center;
}

.note-edit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  min-height: 30px;
  border: 1px solid color-mix(in srgb, var(--spatial-accent) 24%, var(--spatial-line));
  border-radius: 999px;
  padding: 0 12px;
  color: var(--spatial-accent);
  background: var(--spatial-accent-soft);
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  text-align: center;
  cursor: pointer;
}

.doc-authors-cell span {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.45;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.note-edit-btn:hover {
  border-color: #9ec5ff;
  background: #eaf3ff;
}

.doc-authors-cell span { color: var(--spatial-graphite); font-weight: 600; }
.doc-authors-cell span.missing { color: #b7791f; font-weight: 500; }

.import-source-cell {
  color: var(--spatial-gray);
  font-weight: 650;
  white-space: normal;
  word-break: break-word;
}

.import-source-cell a {
  color: #0969f7;
  text-decoration: none;
}

.import-source-cell a:hover {
  text-decoration: underline;
}

.venue-type-badge,
.venue-ranking-badge,
.research-tag,
.journal-metric-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  font-weight: 700;
  white-space: nowrap;
}

.venue-type-badge {
  min-width: 52px;
  min-height: 28px;
  padding: 0 11px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 850;
  word-break: keep-all;
  letter-spacing: 0;
}
.venue-type-badge.journal {
  color: #0b5cad;
  border-color: #b7d7ff;
  background: #eef7ff;
}
.venue-type-badge.conference {
  color: #7a3d00;
  border-color: #fed7aa;
  background: #fff7ed;
}
.venue-type-badge.preprint {
  color: #475569;
  border-color: #cbd5e1;
  background: #f8fafc;
}

.venue-ranking-badge { padding: 7px 11px; font-size: 11px; }
.venue-ranking-badge.top { color: #fff; background: #0f9f67; box-shadow: 0 4px 12px rgba(15,159,103,.18); }
.venue-ranking-badge.strong { color: #075fcf; background: #dceaff; }
.venue-ranking-badge.normal { color: #6d28d9; background: #eee7ff; }
.venue-ranking-badge.pending { color: #9a6700; background: #fff4d6; }

.journal-tag-row,
.journal-metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.journal-metric-row {
  align-items: center;
  min-width: 0;
}

.journal-metric-badge {
  min-height: 28px;
  padding: 0 11px;
  border: 1px solid transparent;
  font-size: 12px;
  line-height: 1;
  font-weight: 850;
  letter-spacing: 0;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, .44);
}

.journal-metric-badge.if {
  color: #063f9e;
  border-color: #82b7ff;
  background: #dcebff;
}

.journal-metric-badge.jcr {
  color: #006b45;
  border-color: #5fd69f;
  background: #d8faea;
}

.journal-metric-badge.cas {
  color: #8a4200;
  border-color: #f2b64e;
  background: #ffedbf;
}

.journal-metric-badge.ccf {
  color: #6515a3;
  border-color: #b69bff;
  background: #eadfff;
}

.journal-metric-badge.index {
  color: #006a78;
  border-color: #5fd2df;
  background: #d9f8fc;
}

.journal-metric-badge.top {
  font-weight: 850;
}

.journal-metric-badge.muted,
.journal-metric-badge.conference {
  color: #3d4b5f;
  border-color: #c7d0dc;
  background: #edf2f8;
}

.publish-time-cell {
  color: var(--spatial-graphite);
  font-weight: 700;
  white-space: nowrap;
}

.research-tag {
  max-width: 150px;
  padding: 5px 8px;
  overflow: hidden;
  color: var(--spatial-gray);
  background: var(--spatial-warm-2);
  font-size: 10px;
  text-overflow: ellipsis;
}

.spatial-btn-danger {
  background: rgba(255, 59, 48, 0.08);
  color: #ff3b30;
  border: 1px solid rgba(255, 59, 48, 0.15);
  font-size: 12px;
  min-height: 28px;
  padding: 0 10px;
}

.spatial-btn-danger:hover {
  background: #ff3b30;
  color: #fff;
  transform: translateY(-1px);
}

.action-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  width: max-content;
  min-width: 0;
}

.action-inline .spatial-btn {
  min-height: 28px;
  font-size: 12px;
  padding: 0 10px;
}

.action-link {
  border: 0;
  background: transparent;
  font-size: 12px;
  color: var(--spatial-accent);
  text-decoration: none;
  border-bottom: 1px solid transparent;
  padding: 2px 0;
  cursor: pointer;
}
.action-link:hover {
  border-color: var(--spatial-accent);
}

.action-link-button {
  color: #b7791f;
}

.note-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, .42);
  backdrop-filter: blur(8px);
}

.note-modal {
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--spatial-line);
  border-radius: 16px;
  background: var(--spatial-surface);
  box-shadow: 0 30px 80px rgba(15, 23, 42, .18);
}

.note-modal-wide {
  width: min(1080px, 100%);
}

.note-modal header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 12px;
}

.note-modal header > div {
  min-width: 0;
}

.note-modal header span {
  color: #0969f7;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .12em;
}

.note-modal header h3 {
  max-width: 58ch;
  margin: 4px 0 0;
  color: var(--spatial-graphite);
  font-size: 18px;
  line-height: 1.45;
  word-break: break-word;
}

.note-modal header button {
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: var(--spatial-gray);
  background: var(--spatial-warm-2);
  font-size: 24px;
  cursor: pointer;
}

.note-mode-bar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 24px 14px;
  padding: 5px;
  border: 1px solid var(--spatial-line);
  border-radius: 10px;
  background: var(--spatial-surface-2);
}

.note-mode-bar button {
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  color: var(--spatial-gray);
  background: transparent;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.note-mode-bar button:hover {
  color: var(--spatial-graphite);
  background: var(--spatial-warm-2);
}

.note-mode-bar button.active {
  color: #fff;
  background: #0969f7;
  border-color: #0969f7;
}

.note-mode-bar span {
  margin-left: auto;
  color: var(--spatial-gray);
  font-size: 12px;
  font-weight: 650;
}

.library-note-tree-panel,
.library-note-markdown-view {
  min-height: min(460px, calc(100vh - 280px));
  max-height: calc(100vh - 280px);
  overflow: auto;
  margin: 0 24px;
  padding: 4px 2px 10px;
}

.library-note-markdown-view {
  padding: 8px 2px 12px;
}

.library-note-tree {
  display: grid;
  gap: 12px;
}

.library-note-section {
  padding: 16px 16px 14px;
  border: 1px solid rgba(99, 102, 241, 0.24);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 250, 255, 0.96), rgba(255, 255, 255, 0.98));
}

.library-note-section.note-level-1 {
  border-color: rgba(37, 99, 235, 0.28);
}

.library-note-section.note-level-2 {
  border-color: rgba(14, 165, 233, 0.24);
}

.library-note-section.note-level-3 {
  border-color: rgba(124, 58, 237, 0.24);
}

.library-note-section-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.library-note-section-head > div {
  min-width: 0;
}

.library-note-section-index {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  flex: 0 0 auto;
  border: 1px solid rgba(59, 130, 246, 0.35);
  border-radius: 50%;
  color: #2563eb;
  background: rgba(239, 246, 255, 0.95);
  font-size: 12px;
  font-weight: 900;
}

.library-note-section-head strong {
  display: block;
  color: var(--spatial-graphite);
  font-size: 15px;
  line-height: 1.45;
}

.library-note-section.note-level-1 .library-note-section-head strong {
  color: #1d4ed8;
}

.library-note-section.note-level-2 .library-note-section-head strong,
.library-note-child.note-level-2 strong {
  color: #0369a1;
}

.library-note-section.note-level-3 .library-note-section-head strong,
.library-note-child.note-level-3 strong,
.library-note-grandchild.note-level-3 strong {
  color: #6d28d9;
}

.library-note-section-head small {
  margin-left: auto;
  white-space: nowrap;
  color: var(--spatial-gray);
  font-size: 11px;
  font-weight: 750;
}

.library-note-section-text,
.library-note-child p {
  margin: 7px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.75;
}

.library-note-child-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.library-note-grandchild-list {
  display: grid;
  gap: 7px;
  margin-top: 9px;
}

.library-note-child,
.library-note-grandchild {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(241, 245, 249, 0.78);
}

.library-note-grandchild {
  padding: 8px 10px;
  background: rgba(245, 243, 255, 0.72);
}

.library-note-child > span,
.library-note-grandchild > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 22px;
  border-radius: 999px;
  color: #0369a1;
  background: rgba(224, 242, 254, 0.92);
  font-size: 11px;
  font-weight: 900;
}

.library-note-grandchild > span {
  color: #6d28d9;
  background: rgba(237, 233, 254, 0.92);
}

.library-note-child strong,
.library-note-grandchild strong {
  display: block;
  color: #1e293b;
  font-size: 13px;
  line-height: 1.45;
}

.library-note-child.note-level-2 strong {
  color: #0369a1;
}

.library-note-child.note-level-3 strong,
.library-note-grandchild.note-level-3 strong {
  color: #6d28d9;
}

.library-note-markdown-outline {
  display: grid;
  gap: 8px;
}

.library-note-markdown-line {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(248, 250, 252, 0.74);
}

.library-note-markdown-line.note-level-2 {
  margin-left: 28px;
  background: rgba(240, 249, 255, 0.78);
}

.library-note-markdown-line.note-level-3 {
  margin-left: 56px;
  background: rgba(245, 243, 255, 0.72);
}

.library-note-markdown-line span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 900;
}

.library-note-markdown-line.note-level-1 span {
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.9);
}

.library-note-markdown-line.note-level-2 span {
  color: #0369a1;
  background: rgba(224, 242, 254, 0.92);
}

.library-note-markdown-line.note-level-3 span {
  color: #6d28d9;
  background: rgba(237, 233, 254, 0.92);
}

.library-note-markdown-line strong {
  min-width: 0;
  overflow: hidden;
  color: #1d4ed8;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 1.4;
}

.library-note-markdown-line.note-level-2 strong {
  color: #0369a1;
}

.library-note-markdown-line.note-level-3 strong {
  color: #6d28d9;
}

.library-note-markdown-line em {
  color: var(--spatial-gray);
  font-size: 11px;
  font-style: normal;
  font-weight: 750;
}

.library-note-markdown-text {
  margin: -2px 0 4px 49px;
  color: #475569;
  font-size: 13px;
  line-height: 1.75;
}

.library-note-markdown-text.note-level-2 {
  margin-left: 77px;
}

.library-note-markdown-text.note-level-3 {
  margin-left: 105px;
}

.library-note-empty,
.library-note-loading {
  min-height: 240px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  border: 1px dashed rgba(148, 163, 184, 0.42);
  border-radius: 14px;
  color: var(--spatial-gray);
  background: rgba(248, 250, 252, 0.76);
  text-align: center;
}

.library-note-empty strong,
.library-note-loading strong {
  color: var(--spatial-graphite);
  font-size: 15px;
}

.library-note-empty p {
  max-width: 48ch;
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}

.library-note-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(37, 99, 235, 0.18);
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: library-note-spin 0.8s linear infinite;
}

@keyframes library-note-spin {
  to { transform: rotate(360deg); }
}

.note-paper-title {
  margin: 0 24px 14px;
  overflow: hidden;
  color: var(--spatial-gray);
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.note-editor-grid {
  min-height: 0;
  flex: 1 1 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, .9fr);
  gap: 14px;
  margin: 0 24px;
}

.note-editor-grid.plain-mode {
  grid-template-columns: 1fr;
}

.note-modal-editor {
  display: block;
  width: 100%;
  min-height: min(380px, calc(100vh - 300px));
  max-height: calc(100vh - 300px);
  margin: 0;
  padding: 14px;
  box-sizing: border-box;
  resize: vertical;
  border: 1px solid var(--spatial-line);
  border-radius: 14px;
  color: var(--spatial-graphite);
  background: var(--spatial-surface-2);
  font: 14px/1.8 inherit;
  outline: none;
}

.note-modal-editor:focus { border-color: #7fb1ff; box-shadow: 0 0 0 3px rgba(9,105,247,.08); }

.note-markdown-preview {
  min-height: min(380px, calc(100vh - 300px));
  max-height: calc(100vh - 300px);
  overflow: auto;
  padding: 16px;
  border: 1px solid var(--spatial-line);
  border-radius: 14px;
  color: var(--spatial-graphite);
  background: var(--spatial-surface);
  font-size: 13.5px;
  line-height: 1.75;
}

.note-markdown-preview :deep(h1),
.note-markdown-preview :deep(h2),
.note-markdown-preview :deep(h3) {
  margin: 0 0 10px;
  color: var(--spatial-graphite);
  font-size: 15px;
}

.note-markdown-preview :deep(p) {
  margin: 0 0 10px;
}

.note-markdown-preview :deep(ul),
.note-markdown-preview :deep(ol) {
  margin: 0 0 12px 18px;
  padding: 0;
}

.note-markdown-preview :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-radius: 8px;
  color: #315a8a;
  background: #f3f7fc;
}

.note-markdown-preview :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: var(--spatial-warm-2);
  font-size: 12px;
}

.note-preview-empty {
  margin: 0;
  color: var(--spatial-gray);
}

.pdf-link-modal {
  width: min(780px, 100%);
}

.pdf-link-modal .note-paper-title {
  white-space: normal;
  line-height: 1.6;
}

.pdf-link-input {
  display: block;
  width: calc(100% - 48px);
  height: 48px;
  margin: 0 24px;
  padding: 0 14px;
  box-sizing: border-box;
  border: 1px solid var(--spatial-line);
  border-radius: 12px;
  color: var(--spatial-graphite);
  background: var(--spatial-surface-2);
  font: 14px/1.4 inherit;
  outline: none;
}

.pdf-link-input:focus {
  border-color: #7fb1ff;
  box-shadow: 0 0 0 3px rgba(9,105,247,.08);
}

.pdf-link-error {
  margin: 10px 24px 0;
  color: #b42318;
  font-size: 12.5px;
  line-height: 1.5;
}

.journal-metric-row-editable {
  cursor: pointer;
  border-radius: 6px;
  padding: 2px 0;
}
.journal-metric-row-editable:hover {
  background: var(--spatial-warm-2);
}
.journal-metric-empty {
  color: #b7791f;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
}

.journal-tag-modal {
  width: min(820px, 100%);
}

.journal-tag-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0 24px 14px;
  padding: 6px;
  border: 1px solid var(--spatial-line);
  border-radius: 12px;
  background: var(--spatial-surface-2);
}

.journal-tag-tabs button {
  padding: 8px 14px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--spatial-gray);
  background: transparent;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s ease;
}

.journal-tag-tabs button:hover {
  color: var(--spatial-graphite);
  background: var(--spatial-warm-2);
}

.journal-tag-tabs button.active {
  color: #fff;
  background: #7c3aed;
  border-color: #6d28d9;
}

.journal-tag-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 24px 18px;
  min-height: 120px;
  padding: 16px;
  border: 1px solid var(--spatial-line);
  border-radius: 12px;
  background: var(--spatial-surface);
}

.journal-tag-chip {
  padding: 9px 16px;
  border: 2px solid transparent;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s ease;
  opacity: .55;
  white-space: nowrap;
  word-break: keep-all;
}

.journal-tag-chip:hover {
  opacity: 1;
  transform: translateY(-1px);
}

.journal-tag-chip[data-selected="true"] {
  opacity: 1;
  border-color: currentColor;
  box-shadow: 0 0 0 2px rgba(255,255,255,.7), 0 4px 14px rgba(0,0,0,.10);
}

.chip-jcr { color: #1d4ed8; background: #dbeafe; }
.chip-cas { color: #b45309; background: #fef3c7; }
.chip-ccf { color: #7c3aed; background: #ede9fe; }
.chip-if { color: #047857; background: #d1fae5; }
.chip-type { color: #be185d; background: #fce7f3; }
.chip-other { color: #475569; background: #f1f5f9; }

.journal-tag-selected-summary {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 10px;
  min-height: 72px;
  margin: 0 24px 12px;
  padding: 16px 14px;
  border-top: 1px solid var(--spatial-line);
  border-radius: 0;
  background: var(--spatial-surface-2);
  font-size: 12px;
}

.journal-tag-selected-summary span {
  color: var(--spatial-gray);
  font-weight: 800;
  line-height: 1.6;
  white-space: nowrap;
  word-break: keep-all;
}

.journal-tag-selected-summary strong {
  min-width: 0;
  color: var(--spatial-graphite);
  font-weight: 750;
  line-height: 1.5;
  white-space: normal;
  word-break: keep-all;
}

.pdf-upload-drop {
  display: grid;
  gap: 8px;
  margin: 0 24px 14px;
  padding: 18px;
  border: 1px dashed #9bb8dc;
  border-radius: 10px;
  color: #315a8a;
  background: #f3f7fc;
  cursor: pointer;
}
.pdf-upload-drop input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.pdf-upload-drop strong { font-size: 13px; }
.pdf-upload-drop small { color: #6d7e92; font-size: 11px; }

@media (max-width: 720px) {
  .journal-tag-tabs { gap: 4px; }
  .journal-tag-tabs button { padding: 6px 10px; font-size: 11px; }
  .journal-tag-panel { padding: 12px; gap: 8px; }
  .note-editor-grid {
    grid-template-columns: 1fr;
  }
  .note-mode-bar {
    align-items: stretch;
    flex-wrap: wrap;
  }
  .note-mode-bar span {
    width: 100%;
    margin-left: 0;
  }
}

.note-modal footer {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 18px 24px 24px;
}

/* Toast styling */
.custom-toast {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(28, 28, 30, 0.94);
  backdrop-filter: blur(12px);
  color: #ffffff;
  padding: 10px 20px;
  border-radius: 999px;
  font-size: 13.5px;
  font-weight: 600;
  box-shadow: 0 16px 48px rgba(10, 10, 12, 0.3);
  z-index: 2000;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translate(-50%, 20px);
  opacity: 0;
}

.missing-pdf-badge {
  color: #d97706;
  background: rgba(217, 119, 6, 0.08);
  font-size: 11px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 8px;
  display: inline-block;
  vertical-align: middle;
}

.spatial-btn-warning {
  background: #f59e0b;
  color: #fff !important;
  border-color: #f59e0b;
}

.spatial-btn-warning:hover {
  background: #d97706;
  border-color: #d97706;
}

@media (max-width: 900px) {
  .library-head-inner {
    display: block;
  }

  .library-head-actions {
    flex-direction: column;
  }

  .library-head-stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    width: 100%;
  }

  .library-nav-row {
    display: block;
  }

  .library-stats-row {
    border-top: 1px solid var(--spatial-line);
  }

  .library-head-stat {
    width: auto;
    min-height: 58px;
  }

  .library-head-stat {
    min-width: 0;
  }

  .library-toolbar-left,
  .library-toolbar-right {
    width: 100%;
  }

  .library-toolbar-right {
    justify-content: space-between;
  }

  .toolbar-search {
    width: 100%;
  }

  .library-subnav {
    width: 100%;
    overflow-x: auto;
    flex: none;
  }
  .library-subnav button { min-width: 138px; }
  .zotero-import-panel { grid-template-columns: 1fr; }
  .personal-paper-form { grid-template-columns: 1fr; }
  .field-wide { grid-column: auto; }
  .sync-facts { grid-template-columns: 1fr; }
}

@media (max-width: 560px) {
  .library-head-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .library-head-stat { padding: 0 12px; }

  .pagination-bar {
    justify-content: flex-start;
  }

  .page-size-control {
    order: 10;
  }
}

/* ── PAGINATION BAR BASE STYLES ── */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  padding: 14px 20px;
  font-size: 13px;
}

.pagination-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--spatial-line);
  border-radius: 8px;
  color: var(--spatial-gray);
  background: var(--spatial-surface);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .2s ease;
}

.pagination-btn:disabled {
  cursor: not-allowed;
  opacity: .45;
}

.pagination-btn:hover {
  color: var(--spatial-graphite);
  background: var(--spatial-warm-2);
}

.pagination-btn:disabled:hover {
  color: var(--spatial-gray);
  background: var(--spatial-surface);
}

.page-number-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--spatial-line);
  border-radius: 8px;
  color: var(--spatial-gray);
  background: var(--spatial-surface);
  font-size: 13px;
  font-weight: 700;
  font-family: 'Geist Mono', monospace, sans-serif;
  transition: all .2s ease;
  cursor: pointer;
}

.page-number-pill.active {
  background: #6366f1;
  border-color: #4f46e5;
  color: #ffffff;
}

.pagination-summary {
  display: inline-flex;
  align-items: center;
  height: 32px;
  color: var(--spatial-gray);
  font-size: 12.5px;
  font-weight: 650;
  white-space: nowrap;
}

.page-size-control {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  height: 32px;
  min-width: 96px;
  padding: 0;
  border: 1px solid var(--spatial-line);
  border-radius: 8px;
  color: var(--spatial-gray);
  background: var(--spatial-surface);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  word-break: keep-all;
}

.page-size-control select {
  width: 100%;
  height: 30px;
  box-sizing: border-box;
  padding: 0 8px;
  border: 0;
  border-radius: 8px;
  appearance: menulist;
  color: var(--spatial-graphite);
  background: transparent;
  font: inherit;
  font-weight: 800;
  line-height: 1;
  outline: none;
  cursor: pointer;
}

.page-size-control select:focus-visible {
  border-color: #7fb1ff;
  box-shadow: 0 0 0 3px rgba(9, 105, 247, .1);
}

/* ── DARK MODE ADAPTATIONS FOR LIBRARY VIEW PANELS & CARDS ── */
:root[data-theme="dark"] .pagination-bar {
  background: transparent !important;
  border-top-color: rgba(255, 255, 255, 0.08) !important;
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .pagination-btn {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .pagination-btn:hover {
  background: rgba(255, 255, 255, 0.14) !important;
  color: #ffffff !important;
}

:root[data-theme="dark"] .pagination-btn:disabled:hover {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .page-number-pill {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .page-number-pill.active {
  background: rgba(99, 102, 241, 0.22) !important;
  border: 1px solid rgba(129, 140, 248, 0.45) !important;
  color: #a5b4fc !important;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.25) !important;
}

:root[data-theme="dark"] .page-size-control {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .page-size-control select {
  background: transparent !important;
  border-color: transparent !important;
  color: #eef4ff !important;
}

:root[data-theme="dark"] .journal-metric-badge small {
  background: rgba(2, 6, 23, .42);
  color: inherit;
  opacity: .96;
}

:root[data-theme="dark"] .journal-metric-badge.if {
  color: #dbeafe;
  border-color: rgba(96, 165, 250, .56);
  background: rgba(37, 99, 235, .24);
}

:root[data-theme="dark"] .journal-metric-badge.jcr {
  color: #dcfce7;
  border-color: rgba(34, 197, 94, .54);
  background: rgba(22, 163, 74, .22);
}

:root[data-theme="dark"] .journal-metric-badge.cas {
  color: #fef3c7;
  border-color: rgba(245, 158, 11, .55);
  background: rgba(180, 83, 9, .22);
}

:root[data-theme="dark"] .journal-metric-badge.ccf {
  color: #ede9fe;
  border-color: rgba(168, 85, 247, .56);
  background: rgba(126, 34, 206, .24);
}

:root[data-theme="dark"] .journal-metric-badge.index {
  color: #ccfbf1;
  border-color: rgba(20, 184, 166, .54);
  background: rgba(13, 148, 136, .22);
}

:root[data-theme="dark"] .journal-metric-badge.review,
:root[data-theme="dark"] .journal-metric-badge.proceedings,
:root[data-theme="dark"] .journal-metric-badge.muted,
:root[data-theme="dark"] .journal-metric-badge.conference {
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, .34);
  background: rgba(71, 85, 105, .26);
}

:root[data-theme="dark"] .venue-type-badge.journal {
  color: #dbeafe;
  border-color: rgba(96, 165, 250, .48);
  background: rgba(30, 64, 175, .22);
}

:root[data-theme="dark"] .venue-type-badge.conference {
  color: #ffedd5;
  border-color: rgba(251, 146, 60, .48);
  background: rgba(154, 52, 18, .24);
}

:root[data-theme="dark"] .venue-type-badge.preprint {
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, .38);
  background: rgba(51, 65, 85, .28);
}

:root[data-theme="dark"] .library-management-panel {
  background: #101827 !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #eef4ff !important;
}

:root[data-theme="dark"] .zotero-import-panel {
  border-color: rgba(96, 165, 250, 0.16) !important;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.98), rgba(17, 24, 39, 0.96) 48%, rgba(8, 47, 73, 0.58)),
    #0f172a !important;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.32) !important;
}

:root[data-theme="dark"] .zotero-copy {
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.14), transparent 36%),
    rgba(15, 23, 42, 0.74) !important;
  border-color: rgba(45, 212, 191, 0.18) !important;
  color: #eef4ff !important;
}

:root[data-theme="dark"] .zotero-copy h3 {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .zotero-copy p {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .zotero-copy span {
  color: #5eead4 !important;
  background: rgba(20, 184, 166, 0.12) !important;
}

:root[data-theme="dark"] .zotero-step-grid div {
  background: rgba(15, 23, 42, 0.58) !important;
  border-color: rgba(148, 163, 184, 0.16) !important;
}

:root[data-theme="dark"] .zotero-step-grid strong {
  color: #eef4ff !important;
}

:root[data-theme="dark"] .zotero-step-grid small {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .zotero-format-row b {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .zotero-file-drop {
  background: rgba(37, 99, 235, 0.08) !important;
  border-color: rgba(96, 165, 250, 0.34) !important;
  color: #93c5fd !important;
}

:root[data-theme="dark"] .zotero-file-drop strong {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .zotero-file-drop small {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .zotero-file-icon {
  color: #7dd3fc !important;
  background: rgba(14, 165, 233, 0.13) !important;
}

:root[data-theme="dark"] .zotero-online-card {
  background: rgba(15, 23, 42, 0.74) !important;
  border-color: rgba(96, 165, 250, 0.2) !important;
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.22) !important;
}

:root[data-theme="dark"] .zotero-card-head strong {
  color: #eef4ff !important;
}

:root[data-theme="dark"] .zotero-card-head small {
  color: #9aa9bd !important;
}

:root[data-theme="dark"] .zotero-card-head > span {
  color: #5eead4 !important;
  background: rgba(20, 184, 166, 0.12) !important;
}

:root[data-theme="dark"] .zotero-online-card label span,
:root[data-theme="dark"] .zotero-online-card small {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .zotero-limit-field em {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .zotero-online-card input {
  background: rgba(15, 23, 42, 0.86) !important;
  border-color: rgba(148, 163, 184, 0.28) !important;
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .zotero-safe-note {
  background: rgba(20, 184, 166, 0.08) !important;
}

:root[data-theme="dark"] .zotero-help-strip {
  border-color: rgba(45, 212, 191, 0.18) !important;
  background: rgba(20, 184, 166, 0.08) !important;
}

:root[data-theme="dark"] .zotero-help-strip small {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .zotero-help-strip button {
  color: #5eead4 !important;
}

:root[data-theme="dark"] .zotero-guide-dialog {
  border-color: rgba(148, 163, 184, 0.2) !important;
  background: #0f1726 !important;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.46) !important;
}

:root[data-theme="dark"] .zotero-guide-head {
  border-bottom-color: rgba(148, 163, 184, 0.18) !important;
  background: linear-gradient(180deg, #111b2b, #0f1726) !important;
}

:root[data-theme="dark"] .zotero-guide-head h2,
:root[data-theme="dark"] .zotero-guide-content h3,
:root[data-theme="dark"] .zotero-guide-content strong {
  color: #f6f8fb !important;
}

:root[data-theme="dark"] .zotero-guide-head p,
:root[data-theme="dark"] .zotero-guide-content ol,
:root[data-theme="dark"] .zotero-guide-content ul {
  color: #dbe7f7 !important;
}

:root[data-theme="dark"] .zotero-guide-head button {
  border-color: rgba(148, 163, 184, 0.22) !important;
  color: #cbd5e1 !important;
  background: rgba(15, 23, 42, 0.86) !important;
}

:root[data-theme="dark"] .zotero-guide-image {
  border-right-color: rgba(148, 163, 184, 0.18) !important;
  background: #08111f !important;
}

:root[data-theme="dark"] .zotero-guide-image img {
  border-color: rgba(148, 163, 184, 0.22) !important;
}

:root[data-theme="dark"] .zotero-guide-content code {
  color: #93c5fd !important;
  background: rgba(37, 99, 235, 0.18) !important;
}

:root[data-theme="dark"] .zotero-divider {
  color: #8795aa !important;
}

:root[data-theme="dark"] .zotero-divider::before,
:root[data-theme="dark"] .zotero-divider::after {
  background: rgba(148, 163, 184, 0.2) !important;
}

:root[data-theme="dark"] .file-drop,
:root[data-theme="dark"] .pdf-upload-drop {
  background: rgba(59, 130, 246, 0.06) !important;
  border-color: rgba(59, 130, 246, 0.35) !important;
  color: #60a5fa !important;
}

:root[data-theme="dark"] .file-drop strong,
:root[data-theme="dark"] .pdf-upload-drop strong {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .file-drop small,
:root[data-theme="dark"] .pdf-upload-drop small {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .zotero-result {
  color: #bbf7d0 !important;
  background: rgba(20, 83, 45, 0.24) !important;
  border-color: rgba(74, 222, 128, 0.32) !important;
}

:root[data-theme="dark"] .zotero-result.partial {
  color: #fed7aa !important;
  background: rgba(124, 45, 18, 0.24) !important;
  border-color: rgba(251, 146, 60, 0.34) !important;
}

:root[data-theme="dark"] .zotero-failed-details {
  background: rgba(18, 26, 40, 0.9) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

:root[data-theme="dark"] .zotero-failed-details summary {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .zotero-failed-details strong {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .note-modal,
:root[data-theme="dark"] .pdf-link-modal,
:root[data-theme="dark"] .journal-tag-modal {
  background: #121a28 !important;
  border-color: rgba(226, 235, 255, 0.14) !important;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6) !important;
  color: #eef4ff !important;
}

:root[data-theme="dark"] .note-modal header h3 {
  color: #f4f4f6 !important;
}

:root[data-theme="dark"] .note-paper-title {
  color: #a8b3c7 !important;
}

:root[data-theme="dark"] .note-mode-bar,
:root[data-theme="dark"] .note-modal-editor,
:root[data-theme="dark"] .note-markdown-preview,
:root[data-theme="dark"] .note-markdown-preview :deep(blockquote),
:root[data-theme="dark"] .note-markdown-preview :deep(code) {
  background: rgba(15, 23, 42, 0.72) !important;
  border-color: rgba(148, 163, 184, 0.22) !important;
}

:root[data-theme="dark"] .note-mode-bar button {
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .note-mode-bar button:hover {
  color: #f8fbff !important;
  background: rgba(148, 163, 184, 0.12) !important;
}

:root[data-theme="dark"] .note-mode-bar button.active {
  color: #ffffff !important;
  background: #2563eb !important;
  border-color: #3b82f6 !important;
}

:root[data-theme="dark"] .note-mode-bar span,
:root[data-theme="dark"] .note-preview-empty {
  color: #94a3b8 !important;
}

:root[data-theme="dark"] .note-modal-editor,
:root[data-theme="dark"] .note-markdown-preview {
  color: #eef4ff !important;
}

:root[data-theme="dark"] .note-modal-editor::placeholder {
  color: #8795aa !important;
}

:root[data-theme="dark"] .note-markdown-preview :deep(h1),
:root[data-theme="dark"] .note-markdown-preview :deep(h2),
:root[data-theme="dark"] .note-markdown-preview :deep(h3) {
  color: #f8fbff !important;
}

:root[data-theme="dark"] .note-markdown-preview :deep(blockquote) {
  color: #c7d2fe !important;
}

:root[data-theme="dark"] .library-note-tree-panel,
:root[data-theme="dark"] .library-note-markdown-view {
  scrollbar-color: rgba(148, 163, 184, 0.38) transparent;
}

:root[data-theme="dark"] .library-note-section {
  border-color: rgba(99, 102, 241, 0.34) !important;
  background: linear-gradient(180deg, rgba(18, 27, 44, 0.94), rgba(12, 18, 31, 0.96)) !important;
}

:root[data-theme="dark"] .library-note-section-index {
  color: #93c5fd !important;
  border-color: rgba(96, 165, 250, 0.45) !important;
  background: rgba(37, 99, 235, 0.18) !important;
}

:root[data-theme="dark"] .library-note-section.note-level-1 {
  border-color: rgba(96, 165, 250, 0.38) !important;
}

:root[data-theme="dark"] .library-note-section.note-level-2 {
  border-color: rgba(56, 189, 248, 0.32) !important;
}

:root[data-theme="dark"] .library-note-section.note-level-3 {
  border-color: rgba(167, 139, 250, 0.32) !important;
}

:root[data-theme="dark"] .library-note-section-head strong,
:root[data-theme="dark"] .library-note-child strong,
:root[data-theme="dark"] .library-note-empty strong,
:root[data-theme="dark"] .library-note-loading strong {
  color: #f8fbff !important;
}

:root[data-theme="dark"] .library-note-section.note-level-1 .library-note-section-head strong {
  color: #bfdbfe !important;
}

:root[data-theme="dark"] .library-note-section.note-level-2 .library-note-section-head strong,
:root[data-theme="dark"] .library-note-child.note-level-2 strong {
  color: #bae6fd !important;
}

:root[data-theme="dark"] .library-note-section.note-level-3 .library-note-section-head strong,
:root[data-theme="dark"] .library-note-child.note-level-3 strong,
:root[data-theme="dark"] .library-note-grandchild.note-level-3 strong {
  color: #c4b5fd !important;
}

:root[data-theme="dark"] .library-note-section-head small,
:root[data-theme="dark"] .library-note-section-text,
:root[data-theme="dark"] .library-note-child p,
:root[data-theme="dark"] .library-note-empty {
  color: #cbd5e1 !important;
}

:root[data-theme="dark"] .library-note-child,
:root[data-theme="dark"] .library-note-grandchild {
  background: rgba(15, 23, 42, 0.78) !important;
}

:root[data-theme="dark"] .library-note-child > span {
  color: #bae6fd !important;
  background: rgba(14, 165, 233, 0.17) !important;
}

:root[data-theme="dark"] .library-note-grandchild > span {
  color: #c4b5fd !important;
  background: rgba(124, 58, 237, 0.18) !important;
}

:root[data-theme="dark"] .library-note-empty,
:root[data-theme="dark"] .library-note-loading {
  border-color: rgba(148, 163, 184, 0.24) !important;
  background: rgba(15, 23, 42, 0.62) !important;
}

:root[data-theme="dark"] .library-note-spinner {
  border-color: rgba(103, 232, 249, 0.2) !important;
  border-top-color: #67e8f9 !important;
}

:root[data-theme="dark"] .library-note-markdown-line {
  background: rgba(15, 23, 42, 0.74) !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-2 {
  background: rgba(8, 47, 73, 0.36) !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-3 {
  background: rgba(46, 16, 101, 0.26) !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-1 span {
  color: #93c5fd !important;
  background: rgba(37, 99, 235, 0.18) !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-2 span {
  color: #bae6fd !important;
  background: rgba(14, 165, 233, 0.17) !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-3 span {
  color: #c4b5fd !important;
  background: rgba(124, 58, 237, 0.18) !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-1 strong {
  color: #bfdbfe !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-2 strong {
  color: #bae6fd !important;
}

:root[data-theme="dark"] .library-note-markdown-line.note-level-3 strong {
  color: #c4b5fd !important;
}

:root[data-theme="dark"] .library-note-markdown-line em,
:root[data-theme="dark"] .library-note-markdown-text {
  color: #cbd5e1 !important;
}
</style>
