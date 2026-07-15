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
              <col class="col-check" />
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
                <th class="checkbox-cell"><input type="checkbox" /></th>
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
              <tr v-for="paper in filteredDocuments" :key="paper.id">
                <td class="checkbox-cell"><input type="checkbox" /></td>
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
                      :key="metric.label"
                      class="journal-metric-badge"
                      :class="metric.type"
                    >
                      {{ metric.label }}
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
                      <button class="spatial-btn spatial-btn-dual" @click="openDualReader(paper)">双栏翻译</button>
                      <button class="spatial-btn spatial-btn-line-ai" @click="openLineAiReader(paper)">
                        <span>逐段翻译</span>
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
                    {{ paper.note ? "展示笔记" : "添加笔记" }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination-bar">
          <span class="muted">共 {{ filteredDocuments.length }} 条</span>
          <button class="pagination-btn">‹</button>
          <span class="page-number-pill active">1</span>
          <button class="pagination-btn">›</button>
          <span class="page-size-pill">20 条/页</span>
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
            <small>上传后由 PaperSolver 储存，并可直接进入双栏或逐段翻译。</small>
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
            <p>上传 Zotero 导出的 BibTeX、RIS 或 CSL JSON 文件，批量写入当前账号文献库。</p>
          </div>
        </header>
        <section class="zotero-import-panel">
          <div class="zotero-copy">
            <span>Zotero 导入</span>
            <h3>把 Zotero 文件夹批量带进文献库</h3>
            <p>在 Zotero 里选择条目或文件夹，导出为 BibTeX、RIS 或 CSL JSON 后上传。系统会读取标题、作者、年份、期刊、DOI/URL，并自动合并重复文献。</p>
            <div class="zotero-format-row">
              <b>BibTeX</b>
              <b>RIS</b>
              <b>CSL JSON</b>
            </div>
          </div>
          <div class="zotero-action-box">
            <label class="zotero-file-drop">
              <input type="file" accept=".bib,.ris,.json,application/json,text/plain" @change="selectZoteroFile" />
              <strong>{{ zoteroFile?.name || "选择 Zotero 导出文件" }}</strong>
              <small>{{ zoteroFile ? formatFileSize(zoteroFile.size) : "从 Zotero 导出的 .bib / .ris / .json" }}</small>
            </label>
            <button class="spatial-btn spatial-btn-accent" type="button" :disabled="zoteroImporting || !zoteroFile" @click="submitZoteroImport">
              {{ zoteroImporting ? "导入中…" : "从 Zotero 导入" }}
            </button>
            <div v-if="zoteroResult" class="zotero-result" :class="{ partial: zoteroResult.failed > 0 }">
              <strong>识别 {{ zoteroResult.detected }} 篇，已导入 {{ zoteroResult.imported }} 篇</strong>
              <span v-if="zoteroResult.failed">失败 {{ zoteroResult.failed }} 篇，可能触发每日导入额度或缺少标题。</span>
              <span v-else>导入完成，文献已进入当前账号文献库。</span>
            </div>
            <details v-if="zoteroFailedItems.length" class="zotero-failed-details">
              <summary>查看失败明细</summary>
              <p v-for="item in zoteroFailedItems" :key="item.title">
                <strong>{{ item.title }}</strong>
                <span>{{ item.message }}</span>
              </p>
            </details>
          </div>
        </section>
      </section>

      <section v-else-if="activeTab === 'storage'" class="library-management-panel">
        <header class="storage-head">
          <div>
            <h2>上传与储存</h2>
            <p>集中管理本地 PDF、替换文件与云端储存状态。</p>
          </div>
          <div class="storage-summary">
            <strong>{{ storedCount }}</strong>
            <span>已储存 PDF / {{ libraryStore.state.documents.length }} 篇</span>
          </div>
        </header>
        <div class="storage-list">
          <article v-for="paper in libraryStore.state.documents" :key="paper.id">
            <div>
              <strong>{{ paper.title }}</strong>
              <span>{{ canTryRead(paper) ? "PDF 已关联，可用于双栏与逐段翻译" : "尚未上传 PDF" }}</span>
            </div>
            <label class="replace-upload">
              <input type="file" accept="application/pdf,.pdf" @change="uploadReplacementPdf(paper, $event)" />
              {{ uploadingWorkspace === paper.workspaceId ? "上传中…" : canTryRead(paper) ? "替换 PDF" : "上传 PDF" }}
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
      <section class="note-modal">
        <header>
          <div>
            <span>{{ noteEditor.paper?.note ? "已保存笔记" : "尚未添加笔记" }}</span>
            <h3>{{ noteEditor.paper?.title || "我的笔记" }}</h3>
          </div>
          <button type="button" @click="closeNoteEditor">×</button>
        </header>
        <p class="note-paper-title">{{ noteEditor.paper?.title }}</p>
        <textarea
          v-model="noteEditor.text"
          class="note-modal-editor"
          placeholder="补充阅读结论、正文页码、实验结果、组会问题、导师建议..."
        ></textarea>
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
            <h3>{{ pdfLinkEditor.paper?.title || "上传 PDF" }}</h3>
          </div>
          <button type="button" @click="closePdfLinkEditor">×</button>
        </header>
        <p class="note-paper-title">选择本地 PDF 文件上传，上传后即可进入双栏或逐段翻译。</p>
        <label class="pdf-upload-drop field-wide">
          <input type="file" accept="application/pdf,.pdf" @change="pickPdfUploadFile" />
          <strong>{{ pdfLinkEditor.fileName || "选择本地 PDF 文件" }}</strong>
          <small>支持 .pdf 格式，上传后由 PaperSolver 储存。</small>
        </label>
        <p v-if="pdfLinkEditor.error" class="pdf-link-error">{{ pdfLinkEditor.error }}</p>
        <footer>
          <button type="button" class="spatial-btn spatial-btn-ghost" @click="closePdfLinkEditor">取消</button>
          <button type="button" class="spatial-btn spatial-btn-accent" :disabled="pdfLinkEditor.saving" @click="savePdfLinkEditor">
            {{ pdfLinkEditor.saving ? "上传中..." : "上传 PDF" }}
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
import { paperpilotApi } from "../services/paperpilotApi";
import { rememberLastReading } from "../utils/readingMemory";
import CheckinLottery from "../components/CheckinLottery.vue";

useScrollReveal(".library-spatial");

const router = useRouter();
const route = useRoute();
const libraryStore = useLibraryStore();
const authStore = useAuthStore();
const keyword = ref("");
const openFilter = ref("");
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
  paper: null,
  text: "",
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
];
const JOURNAL_LEVEL_TAGS = new Set(journalTagGroups.flatMap((group) => group.tags));
const toastMessage = ref("");
const libraryTabs = [
  { id: "papers", label: "全部文献", description: "阅读、翻译与分析" },
  { id: "add", label: "个人文献添加", description: "题录与本地 PDF" },
  { id: "zotero", label: "Zotero 导入", description: "批量题录导入" },
  { id: "storage", label: "上传与储存", description: "文件管理与替换" },
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
      values = cleanJournalTags(paper.journalTags);
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

function refreshFilterOptions() {
  const docs = libraryStore.state.documents;
  const tagSet = new Set();
  const importSet = new Set();
  const yearSet = new Set();
  for (const paper of docs) {
    cleanJournalTags(paper.journalTags).forEach((t) => tagSet.add(t));
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
  String(paper.paperUrl || "").includes("/api/papers/uploads/"),
).length);
watch(() => route.query.tab, (tab) => {
  activeTab.value = validTabs.has(String(tab)) ? String(tab) : "papers";
});

function selectTab(tab) {
  activeTab.value = tab;
  router.replace({ path: "/library", query: tab === "papers" ? {} : { tab } });
}

function venueTypeClass(type) {
  if (type === "会议") return "conference";
  if (type === "预印本") return "preprint";
  return "journal";
}

function rankingClass(ranking) {
  const text = String(ranking || "").toUpperCase();
  if (text.includes("Q1") || text.includes("CCF A")) return "top";
  if (text.includes("Q2") || text.includes("CCF B")) return "strong";
  return "normal";
}

const JOURNAL_METRIC_PRESETS = [
  { key: "iscience", tags: [["IF 4.1", "if"], ["JCRQ1", "jcr"], ["中科院2区", "cas"]] },
  { key: "international dental journal", tags: [["IF 3.4", "if"], ["JCRQ1", "jcr"], ["中科院2区", "cas"]] },
  { key: "procedia computer science", tags: [["IF -", "if muted"], ["Scopus", "jcr"], ["会议论文集", "cas conference"]] },
  { key: "findings of the association for computational linguistics", tags: [["ACL Findings", "if conference"], ["CCF A", "jcr top"], ["会议论文", "cas conference"]] },
  { key: "association for computational linguistics", tags: [["ACL", "if conference"], ["CCF A", "jcr top"], ["会议论文", "cas conference"]] },
  { key: "arxiv", tags: [["IF -", "if muted"], ["预印本", "jcr muted"], ["非期刊", "cas muted"]] },
  { key: "nature", tags: [["IF 高", "if top"], ["JCRQ1", "jcr top"], ["中科院1区", "cas top"]] },
  { key: "science", tags: [["IF 高", "if top"], ["JCRQ1", "jcr top"], ["中科院1区", "cas top"]] },
];

function makeMetricTags(rows) {
  return rows.map(([label, type]) => ({ label, type }));
}

function tagClassForLabel(label) {
  const text = String(label || "").replace(/\(补充\)$/u, "");
  if (text.startsWith("JCR Q")) return "jcr";
  if (text.startsWith("中科院")) return "cas";
  if (text.startsWith("CCF")) return "jcr top";
  if (text.startsWith("IF")) return "if";
  if (["期刊", "会议", "预印本", "综述"].includes(text)) return "cas";
  return "cas muted";
}

function cleanJournalTags(tags) {
  return Array.from(new Set((Array.isArray(tags) ? tags : [])
    .map((tag) => String(tag || "").trim().replace(/\(补充\)$/u, ""))
    .filter((tag) => JOURNAL_LEVEL_TAGS.has(tag))));
}

function journalMetricTags(paper) {
  const manual = cleanJournalTags(paper?.journalTags);
  if (manual.length) {
    return manual.map((label) => {
      const display = String(label).includes("(补充)") ? String(label) : `${label}(补充)`;
      return { label: display, type: tagClassForLabel(label) };
    });
  }
  const source = String(paper?.source || "").trim().toLowerCase();
  const ranking = String(paper?.venueRanking || "").trim().replace(/待核验|待自动核验|待补充|待查|待补/g, "");
  const type = String(paper?.venueType || "").trim();
  const preset = JOURNAL_METRIC_PRESETS.find((item) => source.includes(item.key));
  if (preset) return makeMetricTags(preset.tags);
  if (type === "会议" || ranking.includes("CCF")) {
    return [
      { label: ranking || "会议来源", type: rankingClass(ranking) },
      { label: "会议论文", type: "jcr conference" },
      { label: paper?.publishYear || "年份 --", type: "cas muted" },
    ];
  }
  return [
    { label: "IF --", type: "if muted" },
    { label: ranking || "JCR --", type: "jcr muted" },
    { label: "中科院 --", type: "cas muted" },
  ];
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

function pdfHref(paper) {
  const source = resolveReadablePdfSource(paper);
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
      await paperpilotApi.uploadPaperPdf(paper.workspaceId, file);
    } else {
      await libraryStore.persistDocumentPatch(paper.id, { paperUrl: "" });
    }
    await libraryStore.hydrateLibrary();
    const updated = libraryStore.state.documents.find((item) => item.id === paper.id);
    showToast("PDF 已上传");
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
  return tags.length ? tags.map((t) => t.label).join("、") : "点击设置期刊标签";
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
  if (["期刊", "会议", "预印本", "综述"].includes(tag)) return "chip-type";
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

const zoteroFailedItems = computed(() =>
  (zoteroResult.value?.items || []).filter((item) => item.status === "failed").slice(0, 8),
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
      await paperpilotApi.uploadPaperPdf(result.workspaceId, personalPdf.value);
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
    await paperpilotApi.uploadPaperPdf(paper.workspaceId, file);
    await refreshLibraryFromBackend();
    showToast(`《${paper.title}》PDF 已更新`);
  } catch (error) {
    console.error("paper upload failed", error);
    showToast("PDF 上传失败");
  } finally {
    uploadingWorkspace.value = "";
    event.target.value = "";
  }
}

function openNoteEditor(paper) {
  noteEditor.value = {
    open: true,
    saving: false,
    paper,
    text: paper.note || "",
  };
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
    await libraryStore.persistDocumentPatch(paper.id, { note: noteEditor.value.text });
    showToast("笔记已保存");
    noteEditor.value.open = false;
  } catch (error) {
    console.error("Failed to save note", error);
    showToast("笔记保存失败，请稍后重试");
  } finally {
    noteEditor.value.saving = false;
  }
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
  grid-template-columns: minmax(0, 1fr) minmax(320px, 430px);
  gap: 18px;
  margin-top: 22px;
  padding: 20px;
  border: 1px solid #d8e5f6;
  border-radius: 14px;
  background: linear-gradient(135deg, #f8fbff 0%, #ffffff 58%, #f7fff9 100%);
}

.zotero-copy {
  display: grid;
  align-content: start;
  gap: 9px;
}

.zotero-copy > span {
  width: max-content;
  padding: 4px 9px;
  border-radius: 999px;
  color: #0f766e;
  background: #dffcf3;
  font-size: 11px;
  font-weight: 850;
}

.zotero-copy h3 {
  margin: 0;
  color: var(--spatial-graphite);
  font-size: 18px;
  line-height: 1.35;
}

.zotero-copy p {
  max-width: 68ch;
  margin: 0;
  color: #53647a;
  font-size: 13px;
  line-height: 1.7;
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
  gap: 10px;
}

.zotero-file-drop {
  position: relative;
  display: grid;
  gap: 4px;
  min-height: 88px;
  align-content: center;
  padding: 15px 16px;
  border: 1px dashed #91b3df;
  border-radius: 12px;
  color: #244a7b;
  background: #f4f8ff;
  cursor: pointer;
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
  display: inline-flex !important;
  align-items: center;
  border-color: #6d28d9;
  color: #fff;
  background: #7c3aed;
}
.spatial-btn-line-ai:hover { color: #fff; background: #6d28d9; }

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
  padding: 6px 10px;
  font-size: 11px;
  word-break: keep-all;
}
.venue-type-badge.journal { color: #075fcf; background: #eaf2ff; }
.venue-type-badge.conference { color: #7c3aed; background: #f2eaff; }
.venue-type-badge.preprint { color: #64748b; background: #eef1f5; }

.venue-ranking-badge { padding: 7px 11px; font-size: 11px; }
.venue-ranking-badge.top { color: #fff; background: #0f9f67; box-shadow: 0 4px 12px rgba(15,159,103,.18); }
.venue-ranking-badge.strong { color: #075fcf; background: #dceaff; }
.venue-ranking-badge.normal { color: #6d28d9; background: #eee7ff; }
.venue-ranking-badge.pending { color: #9a6700; background: #fff4d6; }

.journal-tag-row,
.journal-metric-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.journal-metric-row {
  align-items: center;
  min-width: 0;
}

.journal-metric-badge {
  min-height: 28px;
  padding: 0 9px;
  font-size: 12px;
  line-height: 1;
}

.journal-metric-badge.if {
  color: #2563eb;
  background: #eaf2ff;
}

.journal-metric-badge.jcr {
  color: #1f7a38;
  background: #e8f6e8;
}

.journal-metric-badge.cas {
  color: #b7791f;
  background: #fff0cf;
}

.journal-metric-badge.pending {
  color: #667085;
  background: #f1f4f8;
}

.journal-metric-badge.muted,
.journal-metric-badge.conference {
  color: #526074;
  background: #eef2f7;
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
  border: 1px solid var(--spatial-line);
  border-radius: 16px;
  background: var(--spatial-surface);
  box-shadow: 0 30px 80px rgba(15, 23, 42, .18);
}

.note-modal header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 24px 12px;
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
}

.note-modal header button {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: var(--spatial-gray);
  background: var(--spatial-warm-2);
  font-size: 24px;
  cursor: pointer;
}

.note-paper-title {
  margin: 0 24px 14px;
  overflow: hidden;
  color: var(--spatial-gray);
  font-size: 13px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.note-modal-editor {
  display: block;
  width: calc(100% - 48px);
  min-height: 300px;
  margin: 0 24px;
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
}

.note-modal footer {
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
}
</style>
