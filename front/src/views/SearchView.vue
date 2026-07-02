<template>
  <div class="spatial-page search-spatial">
    <section class="plugin-experience">
      <div class="plugin-page-shell">

        <!-- Hero -->
        <div class="plugin-hero" data-reveal="left">
          <div class="plugin-hero-copy">
            <span class="plugin-badge">PaperSolver Capture · 浏览器插件</span>
            <h1>从官网到文献库，<br/>一次完成。</h1>
            <p>
              适配 Chrome 与 Microsoft Edge。在 ScienceDirect、PubMed、知网、Semantic Scholar 等官网检索时，插件自动识别题名、作者、来源与 PDF，并一键导入 PaperSolver。
            </p>
            <div class="plugin-channel-grid">
              <a class="plugin-channel-card" href="/downloads/papersolver-capture-chrome-v0.2.4.zip" download>
                <span class="browser-logo chrome-logo"></span>
                <strong>Chrome 下载</strong>
                <small>v0.2.4 · 开发者模式加载</small>
              </a>
              <a class="plugin-channel-card" href="/downloads/papersolver-capture-edge-v0.2.4.zip" download>
                <span class="browser-logo edge-logo"></span>
                <strong>Edge 下载</strong>
                <small>v0.2.4 · 开发者模式加载</small>
              </a>
            </div>
          </div>

          <div class="plugin-demo" data-reveal="right">
            <div class="plugin-demo-window">
              <div class="plugin-demo-bar">
                <span></span><span></span><span></span>
                <b>sciencedirect.com / article</b>
              </div>
              <div class="plugin-demo-stage">
                <div class="plugin-demo-paper">
                  <small>Article page</small>
                  <strong>Semantic frameworks to support implementation of the EU AI Act</strong>
                  <span class="scan-line"></span>
                  <span class="scan-line short"></span>
                </div>
                <div class="plugin-demo-toast">
                  <i>P</i>
                  <div>
                    <strong>发现可导入文献</strong>
                    <span>标题、作者、PDF 与 DOI 已识别</span>
                  </div>
                  <button>导入</button>
                </div>
              </div>
            </div>
            <div class="plugin-demo-caption">
              <span>官网页面</span>
              <span class="arrow">→</span>
              <span>PaperSolver 文献库</span>
            </div>
          </div>
        </div>

        <!-- Feature cards -->
        <section class="plugin-features" data-reveal>
          <div class="plugin-features-head">
            <span class="plugin-badge">Core features</span>
            <h2>插件能力一览</h2>
          </div>
          <div class="plugin-feature-grid">
            <article v-for="f in pluginFeatures" :key="f.title" class="plugin-feature-card">
              <span class="plugin-feature-icon" :class="'icon-' + f.icon"></span>
              <strong>{{ f.title }}</strong>
              <p>{{ f.desc }}</p>
            </article>
          </div>
        </section>

        <!-- Why choose -->
        <section class="plugin-why" data-reveal>
          <div class="plugin-why-head">
            <span class="plugin-badge">Why PaperSolver Capture</span>
            <h2>为什么用插件导入文献</h2>
            <p>手动复制题录费时、易错，PDF 链接常散落在不同页面。插件在官网详情页直接完成识别与入库。</p>
          </div>
          <div class="plugin-why-grid">
            <article v-for="item in pluginWhyChoose" :key="item.title" class="plugin-why-card">
              <span class="plugin-why-num"></span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </article>
          </div>
        </section>

        <!-- Install tutorial -->
        <section class="plugin-install" data-reveal>
          <div class="plugin-install-copy">
            <span class="plugin-badge">Browser tutorial</span>
            <h2>浏览器安装教学，按动画做就行。</h2>
            <p>Chrome 与 Edge 步骤一致：下载、解压、打开扩展管理页、开启开发者模式、加载解压目录。</p>
          </div>
          <div class="plugin-install-window">
            <div class="extension-window">
              <div class="extension-address">
                <span class="browser-logo chrome-logo"></span>
                <strong>chrome://extensions</strong>
              </div>
              <div class="extension-toggle-row">
                <span>开发者模式</span>
                <b></b>
              </div>
              <div class="extension-action-row">
                <button>加载已解压的扩展程序</button>
                <button>打包扩展</button>
              </div>
              <div class="folder-drop">
                <i>P</i>
                <span>papersolver-capture-v0.2.4</span>
              </div>
              <div class="pin-extension">
                <span>固定到工具栏</span>
                <b>P</b>
              </div>
            </div>
          </div>
          <div class="plugin-install-steps">
            <article v-for="lesson in browserLessons" :key="lesson.title">
              <i>{{ lesson.step }}</i>
              <strong>{{ lesson.title }}</strong>
              <span>{{ lesson.desc }}</span>
            </article>
          </div>
        </section>

        <!-- Source launcher -->
        <section class="plugin-sources" data-reveal>
          <div class="plugin-sources-head">
            <div>
              <span class="plugin-badge">Official source launcher</span>
              <h2>丰富搜索源，一键跳转官网。</h2>
              <p>这里不做站内伪搜索。输入关键词后选择来源，直接打开对应官网结果页，插件负责后续捕获。</p>
            </div>
            <div class="plugin-search-box">
              <input v-model="query" placeholder="输入关键词、DOI、题名或作者..." @keyup.enter="runSearch" />
              <input v-model="author" placeholder="作者筛选，可选" @keyup.enter="runSearch" />
            </div>
          </div>

          <div class="plugin-term-chips">
            <span class="chips-label">热搜词</span>
            <button
              v-for="sug in searchSuggestions"
              :key="sug"
              class="term-chip"
              @click="applySuggestion(sug)"
            >
              {{ sug }}
            </button>
          </div>

          <div class="source-galaxy">
            <button
              v-for="source in sourceLaunchers"
              :key="source.id"
              class="source-orbit-card"
              :class="source.tone"
              type="button"
              @click="openSourceLauncher(source)"
            >
              <i class="source-site-icon">
                <img
                  :src="source.icon"
                  :alt="`${source.name} logo`"
                  loading="lazy"
                  @error="$event.currentTarget.closest('.source-site-icon')?.classList.add('icon-failed')"
                />
                <b>{{ source.initial }}</b>
              </i>
              <strong>{{ source.name }}</strong>
              <span>{{ source.desc }}</span>
              <small>{{ source.region }}</small>
            </button>
          </div>

          <div class="url-direct-import source-url-import">
            <div class="url-import-header">
              <span class="pulse-dot"></span>
              <span>已有论文详情页 URL？直接解析导入</span>
            </div>
            <div class="url-import-input-group">
              <input
                v-model="importUrl"
                class="url-import-input"
                placeholder="粘贴 ScienceDirect、arXiv、Nature、ACL 等论文详情页 URL..."
                @keyup.enter="importByUrl"
              />
              <button
                class="spatial-btn"
                :class="importUrl.trim() ? 'spatial-btn-ghost-glow' : 'spatial-btn-disabled'"
                :disabled="urlImporting || !importUrl.trim()"
                @click="importByUrl"
              >
                <svg v-if="urlImporting" class="spinner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10" stroke-opacity="0.25"/><path d="M12 2v4"/>
                </svg>
                <span>{{ urlImporting ? '解析并导入...' : '直接导入' }}</span>
              </button>
            </div>
            <div v-if="urlImportMsg" :class="['url-import-msg-badge', urlImportOk ? 'ok' : 'err']">
              {{ urlImportMsg }}
            </div>
          </div>
        </section>

        <!-- FAQ -->
        <section class="plugin-faq" data-reveal>
          <div class="plugin-faq-head">
            <span class="plugin-badge">FAQ</span>
            <h2>关于插件的常见疑问</h2>
          </div>
          <div class="plugin-faq-list">
            <details v-for="(item, idx) in pluginFaqs" :key="idx" class="plugin-faq-item" :open="idx === 0">
              <summary>{{ item.q }}<span class="faq-caret"></span></summary>
              <p>{{ item.a }}</p>
            </details>
          </div>
        </section>

        <!-- Related tools -->
        <section class="plugin-related" data-reveal>
          <div class="plugin-related-head">
            <span class="plugin-badge">More in PaperSolver</span>
            <h2>更多相关工具</h2>
          </div>
          <div class="plugin-related-grid">
            <router-link
              v-for="tool in pluginRelatedTools"
              :key="tool.to"
              :to="tool.to"
              class="plugin-related-card"
            >
              <strong>{{ tool.name }}</strong>
              <span>{{ tool.desc }}</span>
            </router-link>
          </div>
        </section>

        <footer class="plugin-copyright">
          © 2026 PaperSolver Capture · Designed for official academic sources · yuan
        </footer>
      </div>
    </section>

    <!-- Search Results Section -->
    <section v-if="false" class="spatial-chapter-inner search-results-section">
      <!-- Centered Loading Overlay for Pagination -->
      <div v-if="loading && results.length > 0" class="results-page-loading-overlay">
        <div class="premium-loader-content">
          <div class="premium-loader-ring-container">
            <svg class="premium-loader-spinner" viewBox="0 0 50 50">
              <circle class="premium-loader-bg-ring" cx="25" cy="25" r="20" fill="none" stroke="rgba(0, 102, 255, 0.08)" stroke-width="3.5" />
              <circle class="premium-loader-glow-ring" cx="25" cy="25" r="20" fill="none" stroke="var(--spatial-accent, #0071e3)" stroke-width="3.5" stroke-linecap="round" />
            </svg>
          </div>
          <div class="premium-loader-text">正在为您载入下一页文献...</div>
        </div>
      </div>

      <div class="results-header-bar">
        <h2>
          检索结果
          <template v-if="totalResults > filteredResults.length">
            (已载入 {{ filteredResults.length }} / 官网约 {{ totalResults }} 篇)
          </template>
          <template v-else>
            ({{ filteredResults.length }} 篇)
          </template>
        </h2>
        <span class="results-hint">提示：有 PDF 可直接预览；无 PDF 时可前往 DOI 或出版方原文页面。</span>
      </div>

      <div v-if="loading && results.length === 0" class="results-loader">
        <div class="premium-loader-content">
          <div class="premium-loader-ring-container">
            <svg class="premium-loader-spinner" viewBox="0 0 50 50">
              <circle class="premium-loader-bg-ring" cx="25" cy="25" r="20" fill="none" stroke="rgba(0, 102, 255, 0.08)" stroke-width="3.5" />
              <circle class="premium-loader-glow-ring" cx="25" cy="25" r="20" fill="none" stroke="var(--spatial-accent, #0071e3)" stroke-width="3.5" stroke-linecap="round" />
            </svg>
          </div>
          <div class="premium-loader-text">正在查询学术数据库...</div>
        </div>
      </div>

      <div v-else-if="results.length === 0" class="results-empty">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/><path d="M8 12h8M12 8v8"/>
        </svg>
        <p>{{ searchMessage || "暂无检索数据。请在上方输入关键词检索，或粘贴文献 URL 导入。" }}</p>
      </div>

      <div v-else class="search-results-layout">
        <aside class="search-filter-panel">
          <div class="filter-panel-head">
            <strong>筛选结果</strong>
            <button v-if="hasActiveFilters" type="button" @click="clearFilters">清除</button>
          </div>

          <div class="filter-group">
            <h3>Years</h3>
            <label v-for="option in visibleFilterOptions.year" :key="option.value" class="filter-option">
              <input type="checkbox" :checked="filters.years.includes(option.value)" @change="toggleFilter('years', option.value)" />
              <span>{{ option.value }} <small>({{ option.count }})</small></span>
            </label>
            <button v-if="filterOptions.year.length > 3" class="filter-more-btn" type="button" @click="toggleFilterExpanded('year')">
              {{ expandedFilters.year ? "Show less" : "Show more" }}
              <span>⌄</span>
            </button>
          </div>

          <div class="filter-group">
            <h3>Article type <small title="来自 Crossref 类型字段">?</small></h3>
            <label v-for="option in visibleFilterOptions.articleType" :key="option.value" class="filter-option">
              <input type="checkbox" :checked="filters.articleTypes.includes(option.value)" @change="toggleFilter('articleTypes', option.value)" />
              <span>{{ option.value }} <small>({{ option.count }})</small></span>
            </label>
            <button v-if="filterOptions.articleType.length > 3" class="filter-more-btn" type="button" @click="toggleFilterExpanded('articleType')">
              {{ expandedFilters.articleType ? "Show less" : "Show more" }}
              <span>⌄</span>
            </button>
          </div>

          <div class="filter-group">
            <h3>Publication title</h3>
            <label v-for="option in visibleFilterOptions.source" :key="option.value" class="filter-option">
              <input type="checkbox" :checked="filters.sources.includes(option.value)" @change="toggleFilter('sources', option.value)" />
              <span>{{ option.value }} <small>({{ option.count }})</small></span>
            </label>
            <button v-if="filterOptions.source.length > 3" class="filter-more-btn" type="button" @click="toggleFilterExpanded('source')">
              {{ expandedFilters.source ? "Show less" : "Show more" }}
              <span>⌄</span>
            </button>
          </div>

          <div class="filter-group">
            <h3>Subject areas</h3>
            <label v-for="option in visibleFilterOptions.subject" :key="option.value" class="filter-option">
              <input type="checkbox" :checked="filters.subjects.includes(option.value)" @change="toggleFilter('subjects', option.value)" />
              <span>{{ option.value }} <small>({{ option.count }})</small></span>
            </label>
            <button v-if="filterOptions.subject.length > 3" class="filter-more-btn" type="button" @click="toggleFilterExpanded('subject')">
              {{ expandedFilters.subject ? "Show less" : "Show more" }}
              <span>⌄</span>
            </button>
          </div>
        </aside>

        <div class="search-results-grid">
          <div 
            v-for="item in paginatedResults" 
            :key="item.id" 
            class="search-result-card"
            :class="{ 'already-imported': isAlreadyImported(item) }"
          >
            <div class="card-badge-row">
              <span class="card-badge engine-badge" :class="String(item.source || '').toLowerCase()">{{ item.source || "Crossref" }}</span>
              <span v-if="item.year" class="card-badge year-badge">{{ item.year }}</span>
              <span v-if="item.articleType" class="card-badge type-badge">{{ item.articleType }}</span>
              <span v-if="item.pdfUrl" class="card-badge pdf-available">PDF 可阅读</span>
              <span v-else class="card-badge pdf-missing">暂无直链</span>
              <span class="card-badge import-status" :class="{ imported: isAlreadyImported(item) }">
                {{ isAlreadyImported(item) ? "已添加至文献库" : "未添加" }}
              </span>
            </div>

            <h3 class="card-title" v-html="highlightTitle(item.title)"></h3>
            
            <div class="card-authors">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
              </svg>
              <span>{{ item.authors || "未署名作者" }}</span>
            </div>

            <div v-if="item.abstractText" class="card-abstract-box">
              <p class="card-abstract" :class="{ expanded: expandedAbstracts[item.id] }">
                {{ item.abstractText }}
              </p>
              <button 
                v-if="item.abstractText.length > 130" 
                class="abstract-toggle" 
                @click="toggleAbstract(item.id)"
              >
                {{ expandedAbstracts[item.id] ? "收起摘要 ↑" : "展开摘要 ↓" }}
              </button>
            </div>
            <div v-else class="card-abstract-empty">
              <div class="empty-abstract-info">
                <span v-if="item.id && item.id.startsWith('10.')" class="info-doi" :title="item.id">
                  <strong>DOI:</strong> {{ item.id }}
                </span>
                <span v-else-if="item.id" class="info-tag" :title="item.id">
                  <strong>标识符:</strong> {{ item.id }}
                </span>
                <p class="empty-ai-tip">
                  <svg class="ai-sparkle-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364-6.364l-.707.707M6.343 17.657l-.707.707m0-12.728l.707.707m11.314 11.314l.707-.707M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"/>
                  </svg>
                  <span>未提供数据库摘要。可点击右侧<b>「导入并阅读」</b>，系统将使用 AI 自动解析并生成大纲。</span>
                </p>
              </div>
            </div>

            <div class="card-actions-row">
              <a 
                v-if="item.pdfUrl" 
                class="card-action-link" 
                :href="paperpilotApi.buildPdfProxyUrl(item.pdfUrl)" 
                target="_blank" 
                rel="noreferrer"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14L21 3"/>
                </svg>
                预览 PDF
              </a>
              <a
                v-else-if="originalPaperUrl(item)"
                class="card-action-link source-link"
                :href="originalPaperUrl(item)"
                target="_blank"
                rel="noreferrer"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                  <path d="M14 5h5v5M10 14 19 5M19 14v5H5V5h5"/>
                </svg>
                查看原文
              </a>
              <span v-else class="card-action-link disabled">暂无原文地址</span>

              <div class="card-action-buttons">
                <button 
                  class="spatial-btn"
                  :class="isAlreadyImported(item) ? 'spatial-btn-disabled' : 'spatial-btn-ghost'"
                  :disabled="isAlreadyImported(item) || importingItems[item.id]"
                  @click="importToLibrary(item)"
                >
                  <svg v-if="importingItems[item.id]" class="spinner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" stroke-opacity="0.25"/><path d="M12 2v4"/>
                  </svg>
                  <span>{{ isAlreadyImported(item) ? '已入库' : (importingItems[item.id] ? '导入中...' : '导入文献') }}</span>
                </button>
                <button 
                  class="spatial-btn spatial-btn-accent" 
                  :disabled="importingItems[item.id]"
                  @click="importAndOpenPdf(item)"
                >
                  <span>导入并打开原文</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Search Pagination Bar -->
      <div v-if="totalPages > 1" class="search-pagination-bar">
        <span class="pagination-info">共 {{ filteredResults.length }} 条结果</span>
        <div class="pagination-buttons">
          <button 
            class="pagination-btn" 
            :disabled="currentPage === 1 || loading" 
            @click="changePage(currentPage - 1)"
          >
            上一页
          </button>
          
          <button 
            v-for="p in visiblePages" 
            :key="p" 
            class="pagination-page-btn" 
            :class="{ active: p === currentPage }"
            :disabled="loading"
            @click="changePage(p)"
          >
            {{ p }}
          </button>
          
          <button 
            class="pagination-btn" 
            :disabled="currentPage === totalPages || loading" 
            @click="changePage(currentPage + 1)"
          >
            下一页
          </button>
        </div>
        <span class="pagination-page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useScrollReveal } from "../composables/useScrollReveal";
import { searchEnginePresets, searchSuggestions } from "../constants/pages";
import { paperpilotApi } from "../services/paperpilotApi";
import { useAuthStore } from "../stores/auth";
import { useLibraryStore } from "../stores/library";
import { useWorkspaceStore } from "../stores/workspace";

useScrollReveal(".search-spatial");

const authStore = useAuthStore();
const router = useRouter();
const libraryStore = useLibraryStore();
const workspaceStore = useWorkspaceStore();

const activeEngineId = ref(searchEnginePresets[0].id);
const query = ref("");
const author = ref("");
const results = ref([]);
const loading = ref(false);
const importingItems = ref({});
const searchMessage = ref("");

// Pagination state
const currentPage = ref(1);
const pageSize = ref(12);
const totalResults = ref(0);

// URL import state
const importUrl = ref("");
const urlImporting = ref(false);
const urlImportMsg = ref("");
const urlImportOk = ref(false);

// Abstract expansion state
const expandedAbstracts = ref({});
// Search History
const searchHistory = ref([]);
const filters = ref({
  years: [],
  articleTypes: [],
  sources: [],
  subjects: [],
});
const expandedFilters = ref({
  year: false,
  articleType: false,
  source: false,
  subject: false,
});

const pluginFaqs = [
  { q: "PaperSolver Capture 是免费的吗？", a: "完全免费。插件本身、识别与导入功能均不收费，配合 PaperSolver 账号即可使用。" },
  { q: "支持哪些网站？", a: "适配 ScienceDirect、PubMed、知网、Semantic Scholar、arXiv、Nature、ACL Anthology 等主流学术来源，并在持续扩展。" },
  { q: "插件会上传我的浏览数据吗？", a: "不会。插件只在论文详情页或 PDF 页本地识别题名、作者、DOI 与 PDF 链接，按你点击导入时才上传到 PaperSolver。" },
  { q: "没有 PDF 的文献能导入吗？", a: "可以。插件会捕获题名、作者、来源、DOI 等元数据并入库，后续可手动补充 PDF 或通过 DOI 跳转原文。" },
  { q: "Edge 也能用吗？", a: "可以。Edge 与 Chrome 同为 Chromium 内核，安装步骤一致，均支持开发者模式加载已解压扩展。" },
];

const pluginFeatures = [
  { icon: "scan", title: "扫描式识别", desc: "在论文详情页自动识别题名、作者、来源与 PDF。" },
  { icon: "bolt", title: "一键导入", desc: "点击导入即入库，无需手动复制粘贴题录信息。" },
  { icon: "globe", title: "多源支持", desc: "覆盖中英文主流学术数据库与开放预印本平台。" },
  { icon: "shield", title: "本地识别", desc: "识别在浏览器本地完成，仅导入时才上传数据。" },
];

const pluginWhyChoose = [
  { title: "无需复制粘贴", desc: "传统方式要手动复制题名、作者、DOI 再回到 PaperSolver 添加，插件在官网页直接完成捕获与入库。" },
  { title: "适配主流学术源", desc: "ScienceDirect、PubMed、知网、Semantic Scholar 等官网均适配，详情页与 PDF 页均可识别。" },
  { title: "保留 PDF 与元数据", desc: "识别内容包括题名、作者、来源、出版年、DOI 与 PDF 链接，入库后直接进入文献库与阅读。" },
  { title: "Chrome 与 Edge 通用", desc: "基于 Chromium 扩展标准开发，两套浏览器安装步骤一致，开发者模式加载即用。" },
];

const pluginRelatedTools = [
  { name: "文献库", desc: "统一管理已导入论文与 PDF", to: "/library" },
  { name: "文献阅读", desc: "原文与译文双栏逐段对照", to: "/reading" },
  { name: "学术社区", desc: "与同行讨论论文与研究方向", to: "/forum" },
  { name: "团队协作", desc: "实验室席位与共享 Token", to: "/team" },
];

const browserLessons = [
  { step: "01", title: "下载并解压", desc: "选择 Chrome 或 Edge 压缩包，解压后不要只打开 zip 内文件。" },
  { step: "02", title: "打开扩展页", desc: "Chrome 输入 chrome://extensions，Edge 输入 edge://extensions。" },
  { step: "03", title: "开发者模式", desc: "开启开发者模式，再选择“加载已解压的扩展程序”。" },
  { step: "04", title: "固定插件", desc: "将 PaperSolver 图标固定到工具栏，打开论文页即可捕获。" },
];

const sourceMeta = {
  sciencedirect: {
    desc: "Elsevier 期刊与图书平台",
    region: "Publisher",
    icon: "https://www.google.com/s2/favicons?domain=sciencedirect.com&sz=64",
  },
  "semantic-scholar": {
    desc: "AI 驱动的论文索引与引用网络",
    region: "Index",
    icon: "https://www.google.com/s2/favicons?domain=semanticscholar.org&sz=64",
  },
  pubmed: {
    desc: "医学与生命科学文献检索",
    region: "Biomedical",
    icon: "https://www.google.com/s2/favicons?domain=pubmed.ncbi.nlm.nih.gov&sz=64",
  },
  "web-of-science": {
    desc: "引文索引与核心合集检索",
    region: "Citation",
    icon: "https://www.google.com/s2/favicons?domain=webofscience.com&sz=64",
  },
  cnki: {
    desc: "中文期刊、学位与会议文献",
    region: "CN",
    icon: "https://www.google.com/s2/favicons?domain=cnki.net&sz=64",
  },
  wanfang: {
    desc: "中文论文、期刊与学位资源",
    region: "CN",
    icon: "https://www.google.com/s2/favicons?domain=wanfangdata.com.cn&sz=64",
  },
  "research-rabbit": {
    desc: "论文图谱与相似文献发现",
    region: "Discovery",
    icon: "https://www.google.com/s2/favicons?domain=researchrabbit.ai&sz=64",
  },
  "connected-papers": {
    desc: "基于论文关系的可视化图谱",
    region: "Graph",
    icon: "https://www.google.com/s2/favicons?domain=connectedpapers.com&sz=64",
  },
};

const faviconFor = (domain) => `https://www.google.com/s2/favicons?domain=${domain}&sz=64`;

const sourceLaunchers = [
  ...searchEnginePresets.map((item, index) => ({
    ...item,
    initial: item.shortName.slice(0, 1).toUpperCase(),
    desc: sourceMeta[item.id]?.desc || "官方学术检索入口",
    region: sourceMeta[item.id]?.region || "Source",
    icon: sourceMeta[item.id]?.icon || faviconFor(new URL(item.url).hostname.replace(/^www\./, "")),
    tone: ["blue", "violet", "green", "ink", "red", "amber", "cyan", "purple"][index % 8],
  })),
  {
    id: "google-scholar",
    name: "Google Scholar",
    shortName: "Scholar",
    url: "https://scholar.google.com/",
    searchPrefix: "https://scholar.google.com/scholar?q=",
    initial: "G",
    desc: "跨出版商论文与引用检索",
    region: "Index",
    icon: faviconFor("scholar.google.com"),
    tone: "blue",
  },
  {
    id: "arxiv",
    name: "arXiv",
    shortName: "arXiv",
    url: "https://arxiv.org/",
    searchPrefix: "https://arxiv.org/search/?query=",
    initial: "A",
    desc: "开放预印本与 PDF 原文",
    region: "Open",
    icon: faviconFor("arxiv.org"),
    tone: "red",
  },
  {
    id: "acl",
    name: "ACL Anthology",
    shortName: "ACL",
    url: "https://aclanthology.org/",
    searchPrefix: "https://aclanthology.org/search/?q=",
    initial: "A",
    desc: "ACL 系列 NLP 论文库",
    region: "NLP",
    icon: faviconFor("aclanthology.org"),
    tone: "green",
  },
  {
    id: "dblp",
    name: "DBLP",
    shortName: "DBLP",
    url: "https://dblp.org/",
    searchPrefix: "https://dblp.org/search?q=",
    initial: "D",
    desc: "计算机作者、会议与期刊索引",
    region: "CS",
    icon: faviconFor("dblp.org"),
    tone: "ink",
  },
  {
    id: "ieee",
    name: "IEEE Xplore",
    shortName: "IEEE",
    url: "https://ieeexplore.ieee.org/",
    searchPrefix: "https://ieeexplore.ieee.org/search/searchresult.jsp?queryText=",
    initial: "I",
    desc: "工程与电气电子",
    region: "Engineering",
    icon: faviconFor("ieeexplore.ieee.org"),
    tone: "cyan",
  },
  {
    id: "springer",
    name: "SpringerLink",
    shortName: "Springer",
    url: "https://link.springer.com/",
    searchPrefix: "https://link.springer.com/search?query=",
    initial: "S",
    desc: "Springer 期刊、会议与图书章节",
    region: "Publisher",
    icon: faviconFor("link.springer.com"),
    tone: "violet",
  },
];

onMounted(async () => {
  // Load history from localStorage
  const saved = localStorage.getItem("paperpilot-search-history");
  if (saved) {
    try {
      searchHistory.value = JSON.parse(saved);
    } catch {
      searchHistory.value = [];
    }
  }
});

const activeEngine = computed(
  () => searchEnginePresets.find((item) => item.id === activeEngineId.value) || searchEnginePresets[0],
);

const isExternalEngine = computed(() => {
  return true;
});

const filteredResults = computed(() => results.value.filter((item) => {
  if (filters.value.years.length && !filters.value.years.includes(String(item.year || "未知年份"))) return false;
  if (filters.value.articleTypes.length && !filters.value.articleTypes.includes(String(item.articleType || "Other"))) return false;
  if (filters.value.sources.length && !filters.value.sources.includes(String(item.source || "未知来源"))) return false;
  if (filters.value.subjects.length) {
    const subjects = Array.isArray(item.subjects) ? item.subjects : [];
    if (!subjects.some(subject => filters.value.subjects.includes(subject))) return false;
  }
  return true;
}));

const hasActiveFilters = computed(() =>
  filters.value.years.length
  || filters.value.articleTypes.length
  || filters.value.sources.length
  || filters.value.subjects.length,
);

const filterOptions = computed(() => ({
  year: countOptions(results.value.map(item => String(item.year || "未知年份")), true),
  articleType: countOptions(results.value.map(item => String(item.articleType || "Other"))),
  source: countOptions(results.value.map(item => String(item.source || "未知来源"))),
  subject: countOptions(results.value.flatMap(item => Array.isArray(item.subjects) ? item.subjects : [])),
}));

const visibleFilterOptions = computed(() => ({
  year: visibleOptions(filterOptions.value.year, expandedFilters.value.year),
  articleType: visibleOptions(filterOptions.value.articleType, expandedFilters.value.articleType),
  source: visibleOptions(filterOptions.value.source, expandedFilters.value.source),
  subject: visibleOptions(filterOptions.value.subject, expandedFilters.value.subject),
}));

const totalPages = computed(() => Math.max(1, Math.ceil(filteredResults.value.length / pageSize.value)));

const paginatedResults = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredResults.value.slice(start, start + pageSize.value);
});

const visiblePages = computed(() => {
  const current = currentPage.value;
  const total = totalPages.value;
  const pages = [];
  let start = Math.max(1, current - 2);
  let end = Math.min(total, start + 4);
  if (end - start < 4) {
    start = Math.max(1, end - 4);
  }
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

async function changePage(p) {
  if (p < 1 || p > totalPages.value) return;
  currentPage.value = p;
  document.querySelector(".search-results-section")?.scrollIntoView({ behavior: "smooth" });
}

function isAlreadyImported(item) {
  const normalizedId = String(item.id || "").toLowerCase();
  return libraryStore.state.documents.some(
    (doc) =>
      doc.title.toLowerCase() === item.title.toLowerCase()
      || (normalizedId && String(doc.paperId || doc.id || "").toLowerCase() === normalizedId)
  );
}

function originalPaperUrl(item) {
  if (item.sourceUrl) return item.sourceUrl;
  if (String(item.id || "").startsWith("10.")) return `https://doi.org/${item.id}`;
  if (String(item.id || "").startsWith("arxiv-")) {
    return `https://arxiv.org/abs/${String(item.id).replace("arxiv-", "")}`;
  }
  return "";
}

function toggleAbstract(id) {
  expandedAbstracts.value[id] = !expandedAbstracts.value[id];
}

function applySuggestion(term) {
  query.value = term;
  runSearch();
}

function truncateHistory(text) {
  return text.length > 18 ? text.slice(0, 16) + "..." : text;
}

function clearHistory() {
  searchHistory.value = [];
  localStorage.removeItem("paperpilot-search-history");
}

function countOptions(values, sortDesc = false) {
  const counts = new Map();
  values
    .map(value => String(value || "").trim())
    .filter(Boolean)
    .forEach(value => counts.set(value, (counts.get(value) || 0) + 1));
  return Array.from(counts.entries())
    .map(([value, count]) => ({ value, count }))
    .sort((a, b) => {
      if (sortDesc && !Number.isNaN(Number(a.value)) && !Number.isNaN(Number(b.value))) {
        return Number(b.value) - Number(a.value);
      }
      return b.count - a.count || a.value.localeCompare(b.value);
    });
}

function visibleOptions(options, expanded) {
  return expanded ? options : options.slice(0, 3);
}

function toggleFilter(key, value) {
  const list = filters.value[key];
  const index = list.indexOf(value);
  if (index >= 0) {
    list.splice(index, 1);
  } else {
    list.push(value);
  }
  currentPage.value = 1;
}

function toggleFilterExpanded(key) {
  expandedFilters.value[key] = !expandedFilters.value[key];
}

function clearFilters() {
  filters.value = {
    years: [],
    articleTypes: [],
    sources: [],
    subjects: [],
  };
  currentPage.value = 1;
}

function escapeHtml(text) {
  return String(text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function highlightTitle(title) {
  const safeTitle = escapeHtml(title);
  const terms = query.value
    .split(/[\s,;，；]+/)
    .map(term => term.trim())
    .filter(term => term.length >= 2)
    .sort((a, b) => b.length - a.length);
  if (!terms.length) return safeTitle;
  const pattern = terms.map(term => term.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|");
  return safeTitle.replace(new RegExp(`(${pattern})`, "gi"), '<span class="title-keyword">$1</span>');
}

function addSearchHistory(q) {
  if (!q || q.trim().length < 2) return;
  const term = q.trim();
  const index = searchHistory.value.indexOf(term);
  if (index !== -1) {
    searchHistory.value.splice(index, 1);
  }
  searchHistory.value.unshift(term);
  if (searchHistory.value.length > 8) {
    searchHistory.value.pop();
  }
  localStorage.setItem("paperpilot-search-history", JSON.stringify(searchHistory.value));
}

function normalizeDocument(item) {
  const pdfSource = paperpilotApi.isLikelyPdfUrl(item.pdfUrl) ? item.pdfUrl : "";
  return {
    id: item.id,
    workspaceId: item.id,
    title: item.title,
    source: item.source,
    authors: item.authors,
    note: "来自学术搜索导入",
    journalTags: [item.source, "PDF", activeEngineId.value],
    publishYear: item.year,
    progress: "0%",
    readAt: "-",
    uploadedAt: new Date().toISOString().slice(0, 10),
    importance: "B",
    abstract: item.abstractText,
    paperUrl: item.pdfUrl || originalPaperUrl(item),
    pdfUrl: pdfSource ? paperpilotApi.buildPdfProxyUrl(pdfSource) : "",
    url: item.sourceUrl || item.pdfUrl,
  };
}

async function importToLibrary(item) {
  if (isAlreadyImported(item)) return;
  importingItems.value[item.id] = true;
  try {
    const response = await workspaceStore.importPaperFromSearch(item);
    await libraryStore.hydrateLibrary();
    authStore.addNotification({
      title: "论文已导入文献库",
      desc: `《${item.title}》已成功导入文献库。`,
    });
    return response;
  } catch (err) {
    authStore.addNotification({
      title: "导入失败",
      desc: "写入库时出错，请重试。",
    });
  } finally {
    importingItems.value[item.id] = false;
  }
}

async function importAndOpenPdf(item) {
  importingItems.value[item.id] = true;
  try {
    let doc = libraryStore.state.documents.find(
      (d) => d.title.toLowerCase() === item.title.toLowerCase()
    );
    if (!doc) {
      const response = await workspaceStore.importPaperFromSearch(item);
      await libraryStore.hydrateLibrary();
      doc = libraryStore.state.documents.find((d) => d.id === response.workspaceId);
    }
    if (doc) {
      const pdfSource = paperpilotApi.isLikelyPdfUrl(doc.pdfUrl)
        ? doc.pdfUrl
        : paperpilotApi.isLikelyPdfUrl(doc.paperUrl)
          ? doc.paperUrl
          : "";
      libraryStore.setActiveDocument(doc.id);
      authStore.addNotification({
        title: "论文已导入",
        desc: `《${item.title}》已成功加入文献库。`,
      });
      if (pdfSource) {
        window.open(paperpilotApi.buildPdfProxyUrl(pdfSource), "_blank", "noopener,noreferrer");
      } else {
        router.push("/library");
      }
    }
  } catch (err) {
    authStore.addNotification({
      title: "导入失败",
      desc: "导入或打开原文时出错，请重试。",
    });
  } finally {
    importingItems.value[item.id] = false;
  }
}

async function runSearch(resetPage = true) {
  if (!query.value.trim()) return;
  if (resetPage) {
    currentPage.value = 1;
      clearFilters();
  }
  loading.value = true;
  searchMessage.value = "";
  if (resetPage) {
    addSearchHistory(query.value);
  }
  try {
    await paperpilotApi.createSearchSession({
      engineId: activeEngine.value.id,
      engineName: activeEngine.value.name,
      query: query.value,
      journal: "",
      author: author.value,
      url: activeEngine.value.searchPrefix,
    });
    const target = officialSearchUrl(activeEngine.value, query.value);
    window.open(target, "_blank", "noopener,noreferrer");
    results.value = [];
    totalResults.value = 0;
    searchMessage.value = `已打开 ${activeEngine.value.name} 官网检索页。安装 PaperSolver 捕获插件后，插件会在论文详情页或 PDF 页面提示是否导入 PaperSolver。`;
    authStore.addNotification({
      title: "已跳转官网检索",
      desc: "请在官网结果中打开论文详情或 PDF，PaperSolver 插件会自动识别并提示导入。",
    });
  } catch (err) {
    authStore.addNotification({
      title: "查询错误",
      desc: "未能获取数据，请检查网络或更换关键词。",
    });
  } finally {
    loading.value = false;
  }
}

function officialSearchUrl(engine, term) {
  const encoded = encodeURIComponent(term.trim());
  if (engine.id === "research-rabbit" || engine.id === "connected-papers") {
    return engine.url;
  }
  return `${engine.searchPrefix}${encoded}`;
}

async function openSourceLauncher(source) {
  activeEngineId.value = searchEnginePresets.some((item) => item.id === source.id)
    ? source.id
    : activeEngineId.value;
  const term = query.value.trim();
  const target = term ? officialSearchUrl(source, term) : source.url;
  if (term) addSearchHistory(term);
  try {
    await paperpilotApi.createSearchSession({
      engineId: source.id,
      engineName: source.name,
      query: term,
      journal: "",
      author: author.value,
      url: source.searchPrefix || source.url,
    });
  } catch {
    // Opening the official source should not depend on session logging.
  }
  window.open(target, "_blank", "noopener,noreferrer");
  authStore.addNotification({
    title: `已打开 ${source.name}`,
    desc: term ? `关键词：${term}` : "已打开官网首页，可在官网继续检索。",
  });
}

async function importByUrl() {
  const url = importUrl.value.trim();
  if (!url) return;
  urlImporting.value = true;
  urlImportMsg.value = "";
  urlImportOk.value = false;
  try {
    const result = await paperpilotApi.importByUrl({ url });
    if (result && result.title) {
      results.value = [result, ...results.value];
      await workspaceStore.importPaperFromSearch(result);
      await libraryStore.hydrateLibrary();
      authStore.addNotification({
        title: "解析成功并导入",
        desc: `《${result.title}》已成功导入文献库。`,
      });
      urlImportMsg.value = `已解析导入：《${result.title}》${result.pdfUrl ? '' : '（暂无免费 PDF，需手动关联）'}`;
      urlImportOk.value = true;
      importUrl.value = "";
    } else {
      urlImportMsg.value = "未能提取到论文元数据，请尝试通过关键词检索。";
    }
  } catch (e) {
    urlImportMsg.value = e?.response?.data?.message || "导入失败，请检查链接或网络连接。";
  } finally {
    urlImporting.value = false;
  }
}
</script>

<style scoped>
.search-spatial .spatial-chapter {
  margin: 0;
  padding-left: 0;
  padding-right: 0;
}

.plugin-page-shell {
  display: grid;
  gap: 30px;
}

.plugin-experience {
  position: relative;
  padding: 10px 0 46px;
}

.capture-hero {
  position: relative;
  min-height: 500px;
  display: grid;
  grid-template-columns: minmax(0, .96fr) minmax(420px, .82fr);
  gap: clamp(34px, 5vw, 72px);
  align-items: center;
  overflow: hidden;
  padding: clamp(36px, 5vw, 68px);
  border: 1px solid rgba(37, 99, 235, .12);
  border-radius: 20px;
  background:
    radial-gradient(44% 60% at 92% 12%, rgba(37, 99, 235, .15), transparent 68%),
    radial-gradient(38% 42% at 12% 92%, rgba(16, 185, 129, .13), transparent 70%),
    linear-gradient(135deg, #ffffff 0%, #f7faff 58%, #eef5ff 100%);
  box-shadow: 0 24px 54px rgba(23, 32, 51, .08);
  isolation: isolate;
}

.hero-orb {
  position: absolute;
  z-index: -1;
  border-radius: 50%;
  filter: blur(42px);
  opacity: .72;
  animation: hero-orb-drift 11s var(--spatial-ease) infinite alternate;
}

.hero-orb-blue {
  width: 360px;
  height: 360px;
  right: -90px;
  top: -80px;
  background: rgba(37, 99, 235, .18);
}

.hero-orb-green {
  width: 300px;
  height: 300px;
  left: 28%;
  bottom: -130px;
  background: rgba(15, 159, 143, .15);
  animation-delay: -3s;
}

.capture-hero-copy {
  min-width: 0;
}

.capture-hero h1 {
  max-width: 15ch;
  margin: 16px 0 18px;
  color: #111827;
  font-size: clamp(36px, 4.1vw, 56px);
  line-height: 1.04;
  letter-spacing: -.025em;
  text-wrap: balance;
}

.capture-hero p {
  max-width: 62ch;
  margin: 0;
  color: #536074;
  font-size: 17px;
  line-height: 1.78;
}

.browser-theater {
  position: relative;
  min-height: 410px;
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, .14);
  border-radius: 24px;
  background: rgba(255,255,255,.84);
  box-shadow: 0 30px 70px rgba(31,47,78,.14);
  transform: perspective(1100px) rotateY(-4deg) rotateX(2deg);
  animation: plugin-float-card 7s var(--spatial-ease) infinite alternate;
}

.browser-shell-top {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid #e7edf6;
  background: #f8fafc;
}

.browser-shell-top span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5e1;
}

.browser-shell-top b {
  min-width: 0;
  margin-left: 8px;
  color: #7b8798;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-stage {
  position: relative;
  min-height: 362px;
  padding: 34px;
}

.paper-scan-card {
  width: min(360px, 80%);
  display: grid;
  gap: 12px;
  padding: 22px;
  border: 1px solid #e1e8f2;
  border-radius: 18px;
  background: #fff;
}

.paper-scan-card small {
  color: #2563eb;
  font-weight: 850;
}

.paper-scan-card strong {
  color: #172033;
  font-size: 18px;
  line-height: 1.35;
}

.scan-line {
  display: block;
  height: 11px;
  width: 82%;
  border-radius: 999px;
  background: linear-gradient(90deg, #e7eef8, #cbdcff, #e7eef8);
  background-size: 220% 100%;
  animation: scan-shimmer 2.8s ease-in-out infinite;
}

.scan-line.short {
  width: 54%;
  animation-delay: .35s;
}

.capture-beam {
  position: absolute;
  left: 32px;
  right: 32px;
  top: 156px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #2563eb, #10b981, transparent);
  box-shadow: 0 0 18px rgba(37,99,235,.36);
  animation: capture-beam-sweep 3.8s var(--spatial-ease) infinite;
}

.capture-toast {
  position: absolute;
  right: 28px;
  bottom: 28px;
  width: min(330px, calc(100% - 56px));
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid rgba(37,99,235,.18);
  border-radius: 18px;
  background: rgba(255,255,255,.96);
  box-shadow: 0 22px 44px rgba(23,32,51,.16);
  animation: preview-toast-rise 4.8s var(--spatial-ease) infinite;
}

.capture-toast i {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #315fe9;
  color: #fff;
  font-style: normal;
  font-weight: 900;
}

.capture-toast strong {
  display: block;
  color: #172033;
  font-size: 14px;
}

.capture-toast span {
  display: block;
  margin-top: 3px;
  color: #667085;
  font-size: 12px;
}

.capture-toast button {
  border: 0;
  border-radius: 999px;
  padding: 9px 12px;
  color: #fff;
  background: #111827;
  font-size: 12px;
  font-weight: 850;
}

.browser-classroom,
.source-launcher {
  position: relative;
  overflow: hidden;
  border: 1px solid #e1e8f2;
  border-radius: 20px;
  background: rgba(255,255,255,.86);
  box-shadow: 0 16px 38px rgba(23,32,51,.055);
}

.browser-classroom {
  display: grid;
  grid-template-columns: .78fr minmax(380px, .95fr);
  gap: 26px;
  align-items: center;
  padding: clamp(28px, 4vw, 48px);
}

.classroom-copy h2,
.launcher-head h2 {
  margin: 12px 0 12px;
  color: #172033;
  font-size: clamp(28px, 3vw, 46px);
  line-height: 1.1;
  letter-spacing: -.025em;
}

.classroom-copy p,
.launcher-head p {
  max-width: 62ch;
  margin: 0;
  color: #667085;
  font-size: 15px;
  line-height: 1.75;
}

.install-animation-panel {
  min-height: 380px;
  display: grid;
  place-items: center;
}

.extension-window {
  position: relative;
  width: min(520px, 100%);
  display: grid;
  gap: 16px;
  padding: 22px;
  border: 1px solid #dbe5f2;
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  box-shadow: 0 28px 64px rgba(23,32,51,.1);
}

.extension-address,
.extension-toggle-row,
.extension-action-row,
.folder-drop,
.pin-extension {
  display: flex;
  align-items: center;
  gap: 12px;
}

.extension-address {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 14px;
  background: #f1f5fb;
}

.extension-address strong {
  color: #172033;
  font-size: 14px;
}

.extension-toggle-row {
  justify-content: space-between;
  color: #536074;
  font-weight: 750;
}

.extension-toggle-row b {
  position: relative;
  width: 54px;
  height: 30px;
  border-radius: 999px;
  background: #2563eb;
}

.extension-toggle-row b::after {
  content: "";
  position: absolute;
  top: 4px;
  left: 26px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  animation: toggle-pop 3.2s var(--spatial-ease) infinite;
}

.extension-action-row button {
  min-height: 40px;
  border: 1px solid #dbe5f2;
  border-radius: 12px;
  padding: 0 14px;
  background: #fff;
  color: #172033;
  font-weight: 800;
}

.folder-drop {
  justify-content: center;
  min-height: 104px;
  border: 1px dashed rgba(37,99,235,.28);
  border-radius: 18px;
  color: #2563eb;
  background: rgba(37,99,235,.045);
  animation: folder-drop-in 4.2s var(--spatial-ease) infinite;
}

.folder-drop i,
.pin-extension b {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: #315fe9;
  color: #fff;
  font-style: normal;
  font-weight: 900;
}

.folder-drop span,
.pin-extension span {
  color: #172033;
  font-size: 14px;
  font-weight: 800;
}

.pin-extension {
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 16px;
  background: #f8fafc;
}

.lesson-steps {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.lesson-steps article {
  flex: 1 1 220px;
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fbff;
}

.lesson-steps i,
.plugin-copyright i {
  color: #2563eb;
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
}

.lesson-steps strong,
.plugin-copyright strong {
  color: #172033;
}

.lesson-steps span,
.plugin-copyright span {
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.source-launcher {
  padding: clamp(28px, 4vw, 48px);
}

.launcher-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, .7fr);
  gap: 24px;
  align-items: end;
}

.launcher-search-box {
  display: grid;
  gap: 10px;
}

.launcher-search-box input {
  height: 46px;
  border: 1px solid #dbe5f2;
  border-radius: 14px;
  padding: 0 14px;
  color: #172033;
  background: #fff;
  font: inherit;
  outline: none;
}

.launcher-search-box input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,.1);
}

.source-galaxy {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
}

.source-orbit-card {
  position: relative;
  width: min(248px, 100%);
  min-height: 118px;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 6px 12px;
  align-items: start;
  text-align: left;
  padding: 16px;
  border: 1px solid #e1e8f2;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #fbfdff);
  cursor: pointer;
  transition: transform .22s var(--spatial-ease), box-shadow .22s ease, border-color .22s ease;
}

.source-orbit-card:hover {
  transform: translateY(-5px);
  border-color: rgba(37,99,235,.24);
  box-shadow: 0 18px 40px rgba(23,32,51,.09);
}

.source-orbit-card i {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  grid-row: 1 / span 3;
  border: 1px solid #e8eef7;
  border-radius: 12px;
  background: #fff;
  font-style: normal;
  box-shadow: 0 6px 14px rgba(23,32,51,.06);
  overflow: hidden;
}

.source-site-icon img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.source-site-icon b {
  display: none;
  color: #2563eb;
  font-size: 15px;
  font-style: normal;
  font-weight: 900;
}

.source-site-icon.icon-failed img {
  display: none;
}

.source-site-icon.icon-failed b {
  display: block;
}

.source-orbit-card strong {
  color: #172033;
  font-size: 15px;
}

.source-orbit-card span {
  color: #667085;
  font-size: 12.5px;
  line-height: 1.45;
  padding-right: 54px;
}

.source-orbit-card small {
  position: absolute;
  top: 16px;
  right: 16px;
  color: #8b98ab;
  font-size: 11px;
  font-weight: 800;
}

.source-url-import {
  margin-top: 24px;
}

.plugin-copyright {
  padding: 4px 16px 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.7;
  text-align: center;
}

.plugin-hero {
  position: relative;
  min-height: 520px;
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(360px, .72fr);
  gap: 42px;
  align-items: stretch;
  padding: clamp(34px, 5vw, 72px);
  overflow: hidden;
  border: 1px solid rgba(37, 99, 235, .12);
  border-radius: 22px;
  background:
    radial-gradient(54% 64% at 85% 12%, rgba(37, 99, 235, .13), transparent 64%),
    radial-gradient(42% 50% at 8% 100%, rgba(15, 159, 143, .10), transparent 70%),
    linear-gradient(135deg, rgba(255,255,255,.99), rgba(246,249,255,.95) 58%, rgba(238,245,255,.92));
  box-shadow: 0 30px 80px rgba(23, 32, 51, .08);
  isolation: isolate;
}

.plugin-hero::before {
  content: "";
  position: absolute;
  inset: 12% -16% auto auto;
  z-index: -1;
  width: 56%;
  height: 58%;
  border-radius: 999px;
  background:
    linear-gradient(115deg, rgba(37, 99, 235, .12), rgba(15, 159, 143, .08), transparent 72%);
  filter: blur(36px);
  opacity: .86;
  transform: rotate(-8deg);
}

.plugin-hero::after {
  content: "";
  position: absolute;
  inset: -40% auto auto -20%;
  width: 70%;
  height: 160%;
  background: linear-gradient(100deg, transparent 24%, rgba(255,255,255,.48) 45%, transparent 66%);
  transform: translateX(-30%) rotate(8deg);
  animation: plugin-sheen 8s var(--spatial-ease) infinite;
  pointer-events: none;
}

.plugin-hero-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.plugin-hero h1 {
  max-width: 13ch;
  margin: 18px 0 14px;
  color: #172033;
  font-size: clamp(42px, 5.8vw, 76px);
  line-height: .98;
  letter-spacing: -0.03em;
  text-wrap: balance;
}

.plugin-hero-copy > p {
  max-width: 64ch;
  margin: 0;
  color: #4b5563;
  font-size: 17px;
  line-height: 1.72;
  text-wrap: pretty;
}

.plugin-channel-grid {
  margin-top: 28px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.plugin-channel-card {
  min-height: 66px;
  display: inline-grid;
  grid-template-columns: 34px minmax(0, 1fr);
  grid-template-rows: auto auto;
  column-gap: 11px;
  align-content: center;
  width: min(250px, 100%);
  padding: 12px 15px;
  border: 1px solid rgba(37, 99, 235, .14);
  border-radius: 999px;
  background: rgba(255,255,255,.78);
  color: #172033;
  text-decoration: none;
  transition: transform .22s var(--spatial-ease), border-color .22s ease, background .22s ease, box-shadow .22s ease;
}

.plugin-channel-card:hover {
  transform: translateY(-4px);
  border-color: #2563eb;
  background: #fbfdff;
  box-shadow: 0 18px 34px rgba(37, 99, 235, .12);
}

.plugin-channel-card .browser-logo {
  grid-row: 1 / span 2;
  width: 34px;
  height: 34px;
}

.plugin-channel-card strong {
  align-self: end;
  font-size: 14px;
}

.plugin-channel-card small {
  align-self: start;
  color: #667085;
  font-size: 12px;
}

.browser-logo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-block;
}

.chrome-logo {
  background:
    radial-gradient(circle, #fff 0 30%, #4285f4 31% 45%, transparent 46%),
    conic-gradient(#ea4335 0 33%, #fbbc05 0 66%, #34a853 0 100%);
}

.edge-logo {
  background:
    radial-gradient(circle at 62% 60%, #fff 0 18%, transparent 19%),
    conic-gradient(from 205deg, #0ea5e9, #2563eb, #22c55e, #0ea5e9);
}

.plugin-preview {
  position: relative;
  min-height: 380px;
  align-self: center;
  border: 1px solid rgba(37, 99, 235, .12);
  border-radius: 20px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 28px 70px rgba(31, 47, 78, .14);
  transform: perspective(1000px) rotateY(-5deg) rotateX(2deg);
  animation: plugin-float-card 7s var(--spatial-ease) infinite alternate;
}

.plugin-preview-bar {
  height: 42px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border-bottom: 1px solid #edf1f7;
  background: #f8fafc;
}

.plugin-preview-bar span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #cbd5e1;
}

.plugin-preview-body {
  position: relative;
  min-height: 338px;
  padding: 32px 24px;
}

.preview-paper-line {
  width: 64%;
  height: 16px;
  margin-bottom: 14px;
  border-radius: 999px;
  background: #dbe5f2;
  animation: preview-line-flow 3.8s ease-in-out infinite;
}

.preview-paper-line.wide { width: 82%; }
.preview-paper-line.short { width: 44%; animation-delay: .25s; }

.preview-toast {
  position: absolute;
  right: 20px;
  bottom: 20px;
  width: 250px;
  display: grid;
  gap: 7px;
  padding: 14px;
  border: 1px solid rgba(37, 99, 235, .18);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, .14);
  animation: preview-toast-rise 4.8s var(--spatial-ease) infinite;
}

.preview-toast strong {
  color: #172033;
  font-size: 14px;
}

.preview-toast span {
  color: #667085;
  font-size: 12px;
}

.preview-toast button {
  width: fit-content;
  border: 0;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  padding: 8px 11px;
  font-size: 12px;
  font-weight: 800;
}

.plugin-guide-strip {
  position: relative;
  display: block;
  max-width: 920px;
  margin: 6px auto 0;
  padding: 8px 0 8px 38px;
}

.plugin-guide-step {
  position: relative;
  width: min(520px, 100%);
  display: grid;
  gap: 6px;
  margin: 0 0 16px;
  padding: 16px 18px;
  border: 1px solid #e1e8f2;
  border-radius: 16px;
  background: rgba(255,255,255,.74);
  transition: transform .22s var(--spatial-ease), background .22s ease, box-shadow .22s ease;
}

.plugin-guide-step:nth-child(2) {
  margin-left: clamp(24px, 16vw, 180px);
}

.plugin-guide-step:nth-child(3) {
  margin-left: clamp(8px, 8vw, 90px);
  margin-bottom: 0;
}

.plugin-guide-step::before {
  content: "";
  position: absolute;
  left: -31px;
  top: 22px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #2563eb;
  box-shadow: 0 0 0 6px rgba(37,99,235,.09);
}

.plugin-guide-step::after {
  content: "";
  position: absolute;
  left: -26px;
  top: 42px;
  width: 1px;
  height: calc(100% + 4px);
  background: linear-gradient(#d9e4f4, transparent);
}

.plugin-guide-step:last-child::after {
  display: none;
}

.plugin-guide-step:hover {
  transform: translateY(-2px);
  background: #fbfdff;
  box-shadow: 0 14px 30px rgba(23,32,51,.06);
}

.plugin-guide-step strong {
  color: #172033;
  font-size: 14px;
}

.plugin-guide-step span {
  color: #667085;
  font-size: 13px;
  line-height: 1.6;
}

.plugin-manual {
  display: grid;
  gap: 16px;
  padding: 24px;
  border: 1px solid #e1e8f2;
  border-radius: 18px;
  background: #fff;
}

.plugin-manual-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.plugin-manual-head h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
}

.plugin-manual-head p {
  max-width: 58ch;
  margin: 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.65;
}

.plugin-manual-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 14px;
}

.plugin-manual-card {
  flex: 1 1 320px;
  padding: 18px;
  border: 1px solid #e1e8f2;
  border-radius: 14px;
  background: #f8fafc;
  transition: transform .22s var(--spatial-ease), border-color .22s ease, background .22s ease;
}

.plugin-manual-card:nth-child(2) {
  margin-top: 28px;
}

.plugin-manual-card:hover {
  transform: translateY(-3px);
  border-color: rgba(37, 99, 235, .22);
  background: #fff;
}

.plugin-brand-steps {
  display: grid;
  gap: 22px;
  padding: clamp(28px, 4vw, 46px);
  border: 1px solid #e1e8f2;
  border-radius: 22px;
  background:
    radial-gradient(42% 64% at 100% 0%, rgba(37,99,235,.08), transparent 70%),
    linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
}

.brand-steps-head {
  display: grid;
  gap: 10px;
  max-width: 760px;
}

.brand-steps-head span {
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  color: #1d4ed8;
  background: rgba(37, 99, 235, .08);
  font-size: 12px;
  font-weight: 850;
}

.brand-steps-head h2 {
  margin: 0;
  color: #172033;
  font-size: clamp(28px, 3vw, 44px);
  line-height: 1.12;
  letter-spacing: -.02em;
}

.brand-steps-head p {
  margin: 0;
  color: #667085;
  font-size: 15px;
  line-height: 1.75;
}

.brand-steps-grid {
  position: relative;
  display: block;
  padding: 8px 0;
}

.brand-steps-grid article {
  position: relative;
  width: min(590px, 100%);
  min-height: 142px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  margin: 0 0 18px;
  padding: 20px;
  overflow: hidden;
  border: 1px solid rgba(225,232,242,.86);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255,255,255,.88), rgba(248,251,255,.72));
  transition: flex .28s var(--spatial-ease), transform .24s var(--spatial-ease), box-shadow .24s ease, border-color .24s ease;
}

.brand-steps-grid article:nth-child(even) {
  margin-left: auto;
}

.brand-steps-grid article:nth-child(3) {
  margin-left: clamp(0px, 10vw, 140px);
}

.brand-steps-grid article:last-child {
  margin-bottom: 0;
}

.brand-steps-grid article::after {
  content: "";
  position: absolute;
  inset: auto 18px 18px auto;
  width: 46px;
  height: 2px;
  border-radius: 999px;
  background: #2563eb;
  transform: scaleX(.35);
  transform-origin: right;
  opacity: .28;
  transition: transform .24s var(--spatial-ease), opacity .24s ease;
}

.brand-steps-grid article:hover {
  transform: translateY(-5px);
  border-color: rgba(37, 99, 235, .24);
  box-shadow: 0 18px 40px rgba(23, 32, 51, .08);
}

.brand-steps-grid article:hover::after {
  transform: scaleX(1);
  opacity: .9;
}

.brand-steps-grid i {
  color: #9aa7b8;
  font-style: normal;
  font-size: 13px;
  font-weight: 850;
}

.brand-steps-grid strong {
  color: #172033;
  font-size: 17px;
}

.brand-steps-grid span {
  color: #667085;
  font-size: 13px;
  line-height: 1.65;
}

.manual-card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.manual-card-title .browser-logo {
  width: 32px;
  height: 32px;
}

.manual-card-title strong {
  color: #172033;
  font-size: 15px;
}

.plugin-manual-card ol {
  margin: 0;
  padding-left: 20px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.75;
}

.plugin-manual-card code {
  padding: 2px 5px;
  border-radius: 5px;
  background: #e9eef6;
  color: #172033;
}

.plugin-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.plugin-download-panel {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 22px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 14px;
  background: #ffffff;
}

.plugin-download-main {
  min-width: 0;
}

.plugin-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 800;
}

.plugin-download-main h2 {
  margin: 10px 0 6px;
  color: #172033;
  font-size: 24px;
  line-height: 1.2;
}

.plugin-download-main p {
  max-width: 62ch;
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.65;
}

.plugin-download-actions {
  width: 210px;
  flex: 0 0 210px;
  display: grid;
  gap: 8px;
}

.plugin-download-btn {
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 900;
}

.plugin-download-btn:hover {
  background: #2563eb;
}

.plugin-download-actions span {
  color: #667085;
  font-size: 12px;
  line-height: 1.45;
}

/* Official Launcher */
.search-glass-dashboard {
  margin-top: 16px;
  background: #f8fafc;
  border: 1px solid #e4e9f2;
  border-radius: 14px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card-headline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  border-bottom: 1px solid #e4e9f2;
  padding-bottom: 14px;
}

.search-card-headline h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.search-card-headline p {
  max-width: 52ch;
  margin: 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.55;
}

.dashboard-row {
  display: flex;
  gap: 16px;
  width: 100%;
}

.dashboard-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.select-field {
  width: 180px;
  flex-shrink: 0;
}

.main-search-field {
  flex: 1;
}

.dashboard-field label {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--text-secondary, #6e6e73);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.custom-select-wrapper {
  position: relative;
}

.dashboard-select {
  width: 100%;
  height: 42px;
  border-radius: 8px;
  border: 1px solid var(--border-strong, rgba(29, 29, 31, 0.12));
  background: var(--bg-card, #ffffff);
  color: var(--text-main, #1d1d1f);
  padding: 0 12px;
  font-size: 0.88rem;
  outline: none;
  cursor: pointer;
  appearance: none;
  font-weight: 600;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .field-icon {
  position: absolute;
  left: 12px;
  width: 18px;
  height: 18px;
  color: var(--text-muted, #8f8f94);
  pointer-events: none;
}

.input-with-icon input {
  width: 100%;
  height: 42px;
  border-radius: 8px;
  border: 1px solid var(--border-strong, rgba(29, 29, 31, 0.12));
  background: var(--bg-card, #ffffff);
  color: var(--text-main, #1d1d1f);
  padding: 0 12px 0 38px;
  font-size: 0.88rem;
  outline: none;
  transition: all 0.2s;
}

.input-with-icon input:focus {
  border-color: var(--accent, #0071e3);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.15);
}

.second-row {
  align-items: flex-end;
}

.second-row .dashboard-field {
  flex: 1;
}

.dashboard-actions {
  display: flex;
  gap: 12px;
}

.spinner-icon {
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes plugin-sheen {
  0%, 46% { transform: translateX(-42%) rotate(8deg); opacity: 0; }
  56% { opacity: .82; }
  78%, 100% { transform: translateX(185%) rotate(8deg); opacity: 0; }
}

@keyframes plugin-float-card {
  from { transform: perspective(1000px) rotateY(-5deg) rotateX(2deg) translateY(0); }
  to { transform: perspective(1000px) rotateY(-2deg) rotateX(1deg) translateY(-10px); }
}

@keyframes preview-line-flow {
  0%, 100% { opacity: .52; transform: translateX(0); }
  50% { opacity: 1; transform: translateX(10px); }
}

@keyframes preview-toast-rise {
  0%, 100% { transform: translateY(0); box-shadow: 0 10px 28px rgba(15, 23, 42, .14); }
  50% { transform: translateY(-8px); box-shadow: 0 22px 42px rgba(15, 23, 42, .18); }
}

@keyframes hero-orb-drift {
  from { transform: translate3d(0, 0, 0) scale(1); }
  to { transform: translate3d(24px, -18px, 0) scale(1.06); }
}

@keyframes scan-shimmer {
  0%, 100% { background-position: 0% 50%; opacity: .58; }
  50% { background-position: 100% 50%; opacity: 1; }
}

@keyframes capture-beam-sweep {
  0%, 100% { transform: translateY(-40px); opacity: 0; }
  18%, 70% { opacity: .9; }
  55% { transform: translateY(118px); }
}

@keyframes toggle-pop {
  0%, 100% { transform: translateX(0) scale(1); }
  50% { transform: translateX(-3px) scale(1.08); }
}

@keyframes folder-drop-in {
  0%, 100% { transform: translateY(0); border-color: rgba(37,99,235,.26); }
  50% { transform: translateY(-5px); border-color: rgba(37,99,235,.54); }
}

.search-term-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.chips-label {
  font-size: 0.8rem;
  color: var(--text-muted, #8f8f94);
}

.term-chip {
  border: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.6);
  padding: 4px 12px;
  font-size: 0.8rem;
  color: var(--text-secondary, #6e6e73);
  cursor: pointer;
  transition: all 0.15s;
}

.term-chip:hover {
  background: var(--bg-card, #ffffff);
  border-color: var(--accent, #0071e3);
  color: var(--accent, #0071e3);
}

/* URL Direct Import */
.url-direct-import {
  margin-top: 12px;
  padding-top: 16px;
  border-top: 1px dashed var(--border, rgba(29, 29, 31, 0.08));
}

.url-import-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-main, #1d1d1f);
  margin-bottom: 8px;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent, #0071e3);
  box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.6; }
  50% { transform: scale(1.1); opacity: 1; box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.3); }
  100% { transform: scale(0.9); opacity: 0.6; }
}

@media (prefers-reduced-motion: reduce) {
  .plugin-hero::after,
  .hero-orb,
  .capture-beam,
  .extension-toggle-row b::after,
  .folder-drop,
  .plugin-preview,
  .browser-theater,
  .scan-line,
  .capture-toast,
  .preview-paper-line,
  .preview-toast,
  .pulse-dot,
  .spinner-icon {
    animation: none !important;
  }

  .plugin-channel-card,
  .plugin-guide-step,
  .plugin-manual-card,
  .brand-steps-grid article {
    transition: none !important;
  }
}

.url-import-input-group {
  display: flex;
  gap: 8px;
}

.url-import-input {
  flex: 1;
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--border-strong, rgba(29, 29, 31, 0.12));
  background: var(--bg-card, #ffffff);
  color: var(--text-main, #1d1d1f);
  padding: 0 12px;
  font-size: 0.82rem;
  outline: none;
  transition: border-color 0.2s;
}

.url-import-input:focus {
  border-color: var(--accent, #0071e3);
}

.spatial-btn-ghost-glow {
  background: rgba(0, 113, 227, 0.05);
  border: 1px solid rgba(0, 113, 227, 0.2);
  color: var(--accent, #0071e3);
}

.spatial-btn-ghost-glow:hover {
  background: var(--accent, #0071e3);
  color: #fff;
}

.spatial-btn-disabled {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  color: var(--text-muted, #8f8f94);
  cursor: not-allowed;
}

.url-import-msg-badge {
  font-size: 0.78rem;
  padding: 6px 12px;
  border-radius: 6px;
  margin-top: 10px;
  font-weight: 500;
  display: inline-block;
}

.url-import-msg-badge.ok {
  background: rgba(52, 199, 89, 0.08);
  color: var(--success, #34c759);
}

.url-import-msg-badge.err {
  background: rgba(255, 59, 48, 0.08);
  color: #ff3b30;
}

/* Search Assistant Card */
.search-assistant-card {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.plugin-guide-card {
  border: 1px solid #e4e9f2;
  border-radius: 14px;
  background: #ffffff;
}

.assistant-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-main, #1d1d1f);
}

.title-icon {
  width: 20px;
  height: 20px;
  color: var(--accent, #0071e3);
}

.plugin-flow-list {
  display: grid;
  gap: 10px;
}

.plugin-flow-item {
  display: grid;
  gap: 4px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
}

.plugin-flow-item:last-child {
  border-bottom: 0;
}

.plugin-flow-item strong {
  color: #172033;
  font-size: 14px;
}

.plugin-flow-item span {
  color: #667085;
  font-size: 12.5px;
  line-height: 1.55;
}

.engine-status-widget {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  border-radius: 10px;
  padding: 12px 16px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 10px;
}

.status-indicator {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 99px;
  font-weight: 600;
}

.status-indicator.online {
  background: rgba(52, 199, 89, 0.1);
  color: var(--success, #34c759);
}

.status-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.78rem;
}

.status-name {
  color: var(--text-secondary, #6e6e73);
}

.status-value {
  font-weight: 500;
  color: var(--text-main, #1d1d1f);
}

.font-highlight {
  color: var(--accent, #0071e3);
}

.font-success {
  color: var(--success, #34c759);
}

.search-history-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-main, #1d1d1f);
}

.history-empty {
  font-size: 0.78rem;
  color: var(--text-muted, #8f8f94);
  font-style: italic;
}

.history-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.history-chip {
  font-size: 0.75rem;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid transparent;
  color: var(--text-secondary, #6e6e73);
  padding: 3px 8px;
  border-radius: 4px;
  cursor: pointer;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: all 0.1s;
}

.history-chip:hover {
  background: rgba(0, 113, 227, 0.05);
  color: var(--accent, #0071e3);
}

.clear-history-btn {
  font-size: 0.72rem;
  color: var(--text-muted, #8f8f94);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
}

.clear-history-btn:hover {
  color: #ff3b30;
}

.pro-tips-section {
  border-top: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  padding-top: 16px;
}

.tips-list {
  margin: 8px 0 0 0;
  padding-left: 16px;
  font-size: 0.78rem;
  color: var(--text-secondary, #6e6e73);
  line-height: 1.6;
}

.tips-list li {
  margin-bottom: 6px;
}

/* Results Grid Section */
.search-results-section {
  margin-top: 48px;
  position: relative;
}

.results-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border-bottom: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  padding-bottom: 12px;
  margin-bottom: 24px;
}

.results-header-bar h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
}

.results-hint {
  font-size: 0.8rem;
  color: var(--text-muted, #8f8f94);
}

.results-empty {
  text-align: center;
  padding: 80px 40px;
  color: var(--text-muted, #8f8f94);
}

.results-empty svg {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.results-loader {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.results-page-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(4px);
  z-index: 100;
  display: flex;
  justify-content: center;
  border-radius: 16px;
}

.results-page-loading-overlay .premium-loader-content {
  position: sticky;
  top: 40vh;
  height: fit-content;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.search-results-layout {
  display: block;
}

.search-filter-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 24px;
  padding: 18px 20px;
  border: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  border-radius: 14px;
  background: #fff;
  color: #1f2329;
}

.filter-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  grid-column: 1 / -1;
  margin-bottom: 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
}

.filter-panel-head strong {
  color: #1f2329;
  font-size: 16px;
}

.filter-panel-head button,
.filter-more-btn {
  border: 0;
  background: transparent;
  color: #0969f7;
  font: inherit;
  cursor: pointer;
}

.filter-group {
  min-width: 0;
  margin-bottom: 0;
}

.filter-group h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  color: #4b5563;
  font-size: 14px;
  font-weight: 600;
}

.filter-group h3 small {
  display: inline-grid;
  place-items: center;
  width: 17px;
  height: 17px;
  border: 1px solid #0969f7;
  border-radius: 50%;
  color: #0969f7;
  font-size: 11px;
}

.filter-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 7px 0;
  color: #20242b;
  font-size: 13px;
  line-height: 1.35;
  cursor: pointer;
}

.filter-option input {
  width: 17px;
  height: 17px;
  flex: 0 0 auto;
  margin: 0;
  accent-color: #0969f7;
}

.filter-option small {
  color: #4b5563;
  font-size: inherit;
}

.filter-more-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 7px;
  padding: 0;
  color: #4b5563;
  font-size: 13px;
}

.filter-more-btn span {
  color: #0969f7;
  font-size: 18px;
}

.search-results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.search-result-card {
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  border-radius: var(--radius-md, 18px);
  padding: 24px;
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(15, 23, 42, 0.04));
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.search-result-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md, 0 10px 24px rgba(15, 23, 42, 0.05));
  border-color: rgba(0, 113, 227, 0.2);
}

.search-result-card.already-imported {
  background: rgba(243, 244, 246, 0.5);
}

.card-badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 14px;
}

.card-badge {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  text-transform: uppercase;
}

.engine-badge {
  background: rgba(0, 113, 227, 0.06);
  color: var(--accent, #0071e3);
}

.engine-badge.sciencedirect {
  background: rgba(249, 115, 22, 0.08);
  color: #f97316;
}

.engine-badge.arxiv {
  background: rgba(185, 28, 28, 0.08);
  color: #b91c1c;
}

.year-badge {
  background: rgba(0, 0, 0, 0.04);
  color: var(--text-secondary, #6e6e73);
}

.type-badge {
  background: rgba(94, 71, 199, 0.08);
  color: #5e47c7;
}

.pdf-available {
  background: rgba(52, 199, 89, 0.08);
  color: var(--success, #34c759);
}

.pdf-missing {
  background: rgba(245, 158, 11, 0.08);
  color: #d97706;
}

.import-status {
  margin-left: auto;
  border: 1px solid rgba(142, 142, 147, 0.14);
  color: #737b88;
  background: #f4f5f7;
}

.import-status.imported {
  border-color: rgba(0, 113, 227, 0.16);
  color: #0066e6;
  background: #eaf3ff;
}

.card-title {
  font-size: 1.05rem;
  font-weight: 600;
  line-height: 1.4;
  margin: 0 0 8px 0;
  color: var(--text-main, #1d1d1f);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-title :deep(.title-keyword) {
  color: #0969f7;
  background: #eaf3ff;
  border-radius: 5px;
  padding: 0 3px;
  font-weight: 800;
}

.card-authors {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  color: var(--text-secondary, #6e6e73);
  margin-bottom: 16px;
}

.card-authors svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.card-abstract-box {
  background: var(--bg-body-2, #eceff3);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 20px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.card-abstract {
  font-size: 0.8rem;
  color: var(--text-secondary, #6e6e73);
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: all 0.3s;
}

.card-abstract.expanded {
  -webkit-line-clamp: unset;
  display: block;
}

.abstract-toggle {
  align-self: flex-start;
  font-size: 0.75rem;
  color: var(--accent, #0071e3);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 0 0 0;
  font-weight: 600;
}

.card-abstract-empty {
  background: rgba(0, 113, 227, 0.03);
  border: 1px solid rgba(0, 113, 227, 0.08);
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 0.76rem;
  color: var(--text-secondary, #6e6e73);
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
}

.empty-abstract-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  text-align: left;
}

.info-doi, .info-tag {
  font-family: monospace;
  font-size: 0.72rem;
  color: var(--text-muted, #8f8f94);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.empty-ai-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0;
  line-height: 1.4;
  color: #323236;
}

.ai-sparkle-icon {
  width: 14px;
  height: 14px;
  color: var(--accent, #0071e3);
  flex-shrink: 0;
  margin-top: 2px;
}

.card-actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--border, rgba(29, 29, 31, 0.08));
  padding-top: 16px;
  margin-top: auto;
}

.card-action-link {
  font-size: 0.8rem;
  color: var(--accent, #0071e3);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.card-action-link svg {
  width: 14px;
  height: 14px;
}

.card-action-link.disabled {
  color: var(--text-muted, #8f8f94);
  cursor: not-allowed;
}

.card-action-link.source-link {
  color: #5e47c7;
}

.card-action-buttons {
  display: flex;
  gap: 8px;
}

.card-action-buttons .spatial-btn {
  padding: 6px 12px;
  font-size: 0.8rem;
  height: 32px;
}

.spatial-btn-disabled {
  background: rgba(0,0,0,0.03);
  border-color: transparent;
  color: var(--text-muted, #8f8f94) !important;
  cursor: not-allowed;
}

/* Vertical Animated Workflow Tutorial */
.search-tutorial-banner-vertical {
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px dashed rgba(29, 29, 31, 0.12);
}

.vertical-steps {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tutorial-step-vert {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 10px;
  padding: 8px 12px;
  transition: all 0.2s ease;
}

.tutorial-step-vert:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.55);
  border-color: rgba(0, 113, 227, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.step-animation-vert {
  width: 90px;
  height: 46px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.04);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
}

.step-text-vert {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.step-title-text {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-main, #1d1d1f);
}

.step-desc-text {
  font-size: 0.72rem;
  color: var(--text-secondary, #6e6e73);
  line-height: 1.3;
}

.vertical-arrow {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 14px;
  color: var(--text-muted, #8f8f94);
  margin: -1px 0;
}

.vertical-arrow svg {
  width: 14px;
  height: 14px;
  opacity: 0.4;
  animation: bounceDown 2s infinite ease-in-out;
}

.step-num {
  width: 16px;
  height: 16px;
  background: var(--accent, #0071e3);
  color: #fff;
  border-radius: 50%;
  font-size: 0.68rem;
  display: grid;
  place-items: center;
  font-weight: 800;
}

/* Animations for step 1 */
.step-animation-vert .typing-bar {
  padding: 3px 6px;
  font-size: 0.58rem;
}

.typing-bar {
  display: inline-flex;
  align-items: center;
  background: #fff;
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 0.68rem;
  font-family: monospace;
  color: var(--text-secondary, #6e6e73);
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.typing-text-vert {
  display: inline-block;
  overflow: hidden;
  white-space: nowrap;
  border-right: 2px solid transparent;
  width: 0;
  animation: typingVert 6s steps(17, end) infinite;
}

.blinking-cursor {
  font-weight: bold;
  color: var(--accent, #0071e3);
  animation: blink 0.8s step-end infinite;
}

@keyframes typingVert {
  0%, 10% { width: 0; }
  40%, 80% { width: 78px; }
  90%, 100% { width: 0; }
}

@keyframes blink {
  50% { opacity: 0; }
}

@keyframes bounceDown {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(3px); }
}

/* Animations for step 2 */
.parsing-radar {
  position: relative;
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
}

.radar-dot {
  width: 6px;
  height: 6px;
  background: var(--accent, #0071e3);
  border-radius: 50%;
  z-index: 2;
}

.radar-circle {
  position: absolute;
  border: 1.5px solid var(--accent, #0071e3);
  border-radius: 50%;
  width: 30px;
  height: 30px;
  animation: radar 2s cubic-bezier(0.215, 0.610, 0.355, 1) infinite;
  opacity: 0;
}

.radar-circle.circle-2 {
  animation-delay: 1s;
}

@keyframes radar {
  0% { transform: scale(0.2); opacity: 0.8; }
  80% { opacity: 0.4; }
  100% { transform: scale(1.6); opacity: 0; }
}

/* Animations for step 3 */
.reader-split-demo {
  display: flex;
  width: 90px;
  height: 38px;
  border: 1px solid rgba(0,0,0,0.08);
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.demo-pane {
  flex: 1;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.demo-pane.demo-left {
  border-right: 1px solid rgba(0,0,0,0.06);
}

.demo-line {
  height: 4px;
  background: rgba(0,0,0,0.08);
  border-radius: 2px;
  width: 90%;
}

.demo-line:nth-child(2) {
  width: 60%;
}

.demo-line.highlight {
  background: var(--accent, #0071e3);
  animation: highlightLine 2.5s infinite;
}

@keyframes highlightLine {
  0%, 100% { background: var(--accent, #0071e3); opacity: 0.7; }
  50% { background: var(--success, #34c759); opacity: 1; }
}

/* Search Pagination Bar */
.search-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 36px;
  padding: 16px 24px;
  border-top: 1px solid var(--border, rgba(29, 29, 31, 0.08));
}

.pagination-info,
.pagination-page-info {
  font-size: 0.82rem;
  color: var(--text-secondary, #6e6e73);
}

.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-btn,
.pagination-page-btn {
  height: 32px;
  border: 1px solid var(--border-strong, rgba(29, 29, 31, 0.12));
  background: var(--bg-card, #ffffff);
  color: var(--text-main, #1d1d1f);
  border-radius: 6px;
  font-size: 0.82rem;
  cursor: pointer;
  outline: none;
  transition: all 0.15s;
}

.pagination-btn {
  padding: 0 14px;
  font-weight: 500;
}

.pagination-btn:hover:not(:disabled) {
  border-color: var(--accent, #0071e3);
  color: var(--accent, #0071e3);
}

.pagination-btn:disabled {
  background: rgba(0, 0, 0, 0.02);
  color: var(--text-muted, #8f8f94);
  cursor: not-allowed;
  border-color: var(--border, rgba(29, 29, 31, 0.08));
}

.pagination-page-btn {
  width: 32px;
  display: grid;
  place-items: center;
  font-weight: 600;
}

.pagination-page-btn:hover:not(.active) {
  border-color: var(--accent, #0071e3);
  color: var(--accent, #0071e3);
}

.pagination-page-btn.active {
  background: var(--accent, #0071e3);
  color: #ffffff;
  border-color: var(--accent, #0071e3);
}

@media (max-width: 768px) {
  .capture-hero,
  .browser-classroom,
  .launcher-head,
  .plugin-hero,
  .plugin-manual-grid,
  .plugin-workbench {
    grid-template-columns: 1fr;
  }

  .capture-hero {
    min-height: auto;
    padding: 26px;
  }

  .capture-hero h1 {
    font-size: 40px;
  }

  .browser-theater {
    min-height: 340px;
    transform: none;
  }

  .browser-stage {
    min-height: 292px;
    padding: 20px;
  }

  .paper-scan-card {
    width: 100%;
  }

  .capture-toast {
    left: 16px;
    right: 16px;
    width: auto;
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .capture-toast button {
    grid-column: 1 / -1;
    width: 100%;
  }

  .browser-classroom,
  .source-launcher {
    padding: 22px;
  }

  .extension-action-row,
  .url-import-input-group {
    flex-direction: column;
    align-items: stretch;
  }

  .source-orbit-card {
    width: 100%;
  }

  .plugin-guide-strip {
    padding-left: 28px;
  }

  .plugin-guide-step,
  .plugin-guide-step:nth-child(2),
  .plugin-guide-step:nth-child(3),
  .brand-steps-grid article,
  .brand-steps-grid article:nth-child(even),
  .brand-steps-grid article:nth-child(3) {
    width: 100%;
    margin-left: 0;
  }

  .plugin-hero {
    padding: 22px;
  }

  .plugin-hero h1 {
    max-width: none;
    font-size: 36px;
  }

  .plugin-channel-grid {
    flex-direction: column;
  }

  .plugin-channel-card {
    width: 100%;
    border-radius: 16px;
  }

  .plugin-preview {
    min-height: 300px;
    transform: none;
  }

  .plugin-brand-steps {
    padding: 22px;
  }

  .brand-steps-grid article {
    min-height: 142px;
  }

  .preview-toast {
    left: 16px;
    right: 16px;
    width: auto;
  }

  .plugin-manual-head,
  .search-card-headline,
  .dashboard-row,
  .url-import-input-group {
    flex-direction: column;
    align-items: stretch;
  }

  .select-field {
    width: 100%;
    flex-basis: auto;
  }

  .search-card-headline {
    align-items: flex-start;
  }

  .search-results-layout {
    grid-template-columns: 1fr;
  }

  .search-filter-panel {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
    gap: 18px;
    padding: 18px;
    border: 1px solid var(--border, rgba(29, 29, 31, 0.08));
    border-radius: 12px;
    background: #fff;
  }

  .filter-panel-head {
    grid-column: 1 / -1;
    margin-bottom: 0;
  }

  .filter-group {
    margin-bottom: 0;
  }

  .search-results-grid {
    grid-template-columns: 1fr;
  }

  .search-tutorial-banner-vertical {
    display: none;
  }
}

/* ===== Belindoc-style plugin page ===== */
.plugin-page-shell { max-width: 1180px; margin: 0 auto; padding: 40px 24px 80px; display:flex; flex-direction:column; gap:64px; }

.plugin-hero { display:grid; grid-template-columns: 1.1fr 0.9fr; gap:48px; align-items:center; }
.plugin-hero-copy h1 { font-size:48px; line-height:1.15; margin:18px 0 16px; letter-spacing:-.01em; }
.plugin-hero-copy p { font-size:17px; line-height:1.7; color:#475569; margin:0 0 28px; max-width:520px; }

.plugin-channel-grid { display:flex; gap:14px; flex-wrap:wrap; }
.plugin-channel-card {
  display:flex; flex-direction:column; align-items:flex-start; gap:4px;
  padding:18px 22px; border-radius:16px; border:1px solid rgba(15,23,42,.1);
  background:#fff; text-decoration:none; color:#0f172a; transition:transform .18s ease, box-shadow .18s ease;
}
.plugin-channel-card:hover { transform:translateY(-2px); box-shadow:0 14px 34px rgba(15,23,42,.12); }
.plugin-channel-card strong { font-size:16px; }
.plugin-channel-card small { color:#64748b; font-size:12px; }

.plugin-demo { display:flex; flex-direction:column; gap:14px; }
.plugin-demo-window { border-radius:18px; overflow:hidden; box-shadow:0 24px 56px rgba(15,23,42,.16); border:1px solid rgba(15,23,42,.08); background:#fff; }
.plugin-demo-bar { display:flex; align-items:center; gap:8px; padding:12px 16px; background:#f1f5f9; }
.plugin-demo-bar span { width:11px; height:11px; border-radius:50%; background:#cbd5e1; }
.plugin-demo-bar span:nth-child(1){ background:#ff5f57; } .plugin-demo-bar span:nth-child(2){ background:#febc2e; } .plugin-demo-bar span:nth-child(3){ background:#28c840; }
.plugin-demo-bar b { margin-left:10px; font-size:12px; color:#64748b; font-weight:500; }
.plugin-demo-stage { position:relative; padding:28px; min-height:240px; background:linear-gradient(180deg,#fafbff,#f3f6ff); }
.plugin-demo-paper { background:#fff; border:1px solid rgba(15,23,42,.08); border-radius:14px; padding:18px; max-width:340px; box-shadow:0 8px 22px rgba(15,23,42,.06); }
.plugin-demo-paper small { color:#2563eb; font-size:11px; font-weight:700; letter-spacing:.08em; text-transform:uppercase; }
.plugin-demo-paper strong { display:block; margin-top:8px; font-size:15px; line-height:1.5; }
.scan-line { display:block; height:8px; border-radius:4px; background:#e2e8f0; margin-top:10px; }
.scan-line.short { width:60%; }
.plugin-demo-toast {
  position:absolute; right:24px; bottom:24px; display:flex; align-items:center; gap:12px;
  background:#0f172a; color:#fff; border-radius:14px; padding:12px 16px; box-shadow:0 16px 36px rgba(15,23,42,.3);
}
.plugin-demo-toast i { width:32px; height:32px; border-radius:9px; background:linear-gradient(135deg,#2563eb,#06b6d4); display:grid; place-items:center; font-style:normal; font-weight:700; }
.plugin-demo-toast strong { display:block; font-size:13px; }
.plugin-demo-toast span { font-size:11px; color:#94a3b8; }
.plugin-demo-toast button { margin-left:8px; background:#2563eb; color:#fff; border:0; border-radius:9px; padding:7px 14px; font-weight:600; cursor:pointer; }
.plugin-demo-caption { display:flex; align-items:center; gap:12px; justify-content:center; color:#64748b; font-size:13px; }
.plugin-demo-caption .arrow { color:#2563eb; font-weight:700; }

.plugin-features-head, .plugin-why-head, .plugin-install-copy, .plugin-sources-head > div, .plugin-faq-head, .plugin-related-head { display:flex; flex-direction:column; gap:8px; margin-bottom:28px; }
.plugin-features-head h2, .plugin-why-head h2, .plugin-install-copy h2, .plugin-sources-head h2, .plugin-faq-head h2, .plugin-related-head h2 { font-size:30px; margin:0; letter-spacing:-.01em; }
.plugin-why-head p, .plugin-install-copy p, .plugin-sources-head p { color:#64748b; line-height:1.7; margin:0; max-width:640px; }

.plugin-feature-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:20px; }
.plugin-feature-card { padding:24px; border-radius:18px; border:1px solid rgba(15,23,42,.08); background:#fff; display:flex; flex-direction:column; gap:10px; }
.plugin-feature-card strong { font-size:17px; }
.plugin-feature-card p { color:#64748b; line-height:1.6; margin:0; font-size:14px; }
.plugin-feature-icon { width:44px; height:44px; border-radius:12px; background:linear-gradient(135deg,#eff6ff,#dbeafe); display:grid; place-items:center; }
.plugin-feature-icon.icon-scan { background:linear-gradient(135deg,#eff6ff,#dbeafe); }
.plugin-feature-icon.icon-bolt { background:linear-gradient(135deg,#fef3c7,#fde68a); }
.plugin-feature-icon.icon-globe { background:linear-gradient(135deg,#dcfce7,#bbf7d0); }
.plugin-feature-icon.icon-shield { background:linear-gradient(135deg,#fae8ff,#f5d0fe); }
.plugin-feature-icon::after { content:""; width:22px; height:22px; border-radius:6px; background:#2563eb; opacity:.7; }
.plugin-feature-icon.icon-bolt::after { background:#f59e0b; }
.plugin-feature-icon.icon-globe::after { background:#16a34a; }
.plugin-feature-icon.icon-shield::after { background:#c026d3; }

.plugin-why-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:24px; }
.plugin-why-card { padding:28px; border-radius:18px; background:linear-gradient(180deg,#fff,#f8fafc); border:1px solid rgba(15,23,42,.08); position:relative; }
.plugin-why-num { display:block; width:36px; height:36px; border-radius:10px; background:#eff6ff; margin-bottom:14px; position:relative; }
.plugin-why-num::after { content:""; position:absolute; inset:8px; border-radius:5px; background:#2563eb; opacity:.6; }
.plugin-why-card strong { display:block; font-size:18px; margin-bottom:8px; }
.plugin-why-card p { color:#64748b; line-height:1.7; margin:0; }

.plugin-install { display:grid; grid-template-columns:1fr 1.1fr; gap:48px; align-items:start; }
.plugin-install-steps { grid-column:1 / -1; display:grid; grid-template-columns:repeat(4,1fr); gap:18px; margin-top:8px; }
.plugin-install-steps article { display:flex; flex-direction:column; gap:6px; padding:20px; border-radius:14px; border:1px solid rgba(15,23,42,.08); background:#fff; }
.plugin-install-steps i { font-style:normal; font-size:13px; font-weight:700; color:#2563eb; }
.plugin-install-steps strong { font-size:15px; }
.plugin-install-steps span { color:#64748b; font-size:13px; line-height:1.6; }

.plugin-sources-head { display:flex; justify-content:space-between; align-items:flex-end; gap:32px; margin-bottom:24px; }
.plugin-search-box { display:flex; gap:10px; }
.plugin-search-box input { border:1px solid rgba(15,23,42,.12); border-radius:12px; padding:12px 16px; outline:none; min-width:220px; }
.plugin-term-chips { display:flex; align-items:center; gap:10px; flex-wrap:wrap; margin-bottom:24px; }
.plugin-term-chips .chips-label { color:#94a3b8; font-size:13px; }
.term-chip { border:1px solid rgba(15,23,42,.1); background:#fff; border-radius:999px; padding:7px 14px; font-weight:600; cursor:pointer; }
.term-chip:hover { background:#eff6ff; color:#2563eb; }

.plugin-faq-list { display:flex; flex-direction:column; gap:12px; max-width:820px; }
.plugin-faq-item { border:1px solid rgba(15,23,42,.1); border-radius:14px; background:#fff; padding:0; overflow:hidden; }
.plugin-faq-item summary { list-style:none; cursor:pointer; padding:18px 22px; font-weight:600; display:flex; justify-content:space-between; align-items:center; }
.plugin-faq-item summary::-webkit-details-marker { display:none; }
.faq-caret { width:14px; height:14px; border-right:2px solid #94a3b8; border-bottom:2px solid #94a3b8; transform:rotate(45deg); transition:transform .2s; }
.plugin-faq-item[open] .faq-caret { transform:rotate(-135deg); }
.plugin-faq-item p { margin:0; padding:0 22px 18px; color:#64748b; line-height:1.7; }

.plugin-related-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:18px; }
.plugin-related-card { padding:22px; border-radius:16px; border:1px solid rgba(15,23,42,.08); background:#fff; text-decoration:none; color:#0f172a; display:flex; flex-direction:column; gap:8px; transition:transform .18s ease, box-shadow .18s ease; }
.plugin-related-card:hover { transform:translateY(-2px); box-shadow:0 12px 28px rgba(15,23,42,.1); }
.plugin-related-card strong { font-size:16px; }
.plugin-related-card span { color:#64748b; font-size:13px; line-height:1.6; }

.plugin-copyright { text-align:center; color:#94a3b8; font-size:13px; padding-top:24px; border-top:1px solid rgba(15,23,42,.08); }

@media (max-width: 960px) {
  .plugin-hero { grid-template-columns:1fr; }
  .plugin-feature-grid, .plugin-related-grid { grid-template-columns:repeat(2,1fr); }
  .plugin-why-grid { grid-template-columns:1fr; }
  .plugin-install { grid-template-columns:1fr; }
  .plugin-install-steps { grid-template-columns:repeat(2,1fr); }
  .plugin-sources-head { flex-direction:column; align-items:flex-start; }
}
@media (max-width: 600px) {
  .plugin-hero-copy h1 { font-size:34px; }
  .plugin-feature-grid, .plugin-related-grid, .plugin-install-steps { grid-template-columns:1fr; }
}
</style>
