<template>
  <div class="tutorial-page spatial-page">
    <section class="tutorial-shell">
      <aside class="tutorial-rail">
        <div class="rail-head">
          <span>PaperSolver Guide</span>
          <h1>使用教程</h1>
          <p>更新通知、维护记录和常用操作说明都会沉淀在这里。</p>
        </div>

        <div class="tutorial-filter">
          <input v-model.trim="query" type="search" placeholder="搜索标题或正文" />
        </div>

        <nav class="tutorial-list" aria-label="教程目录">
          <button
            v-for="article in filteredArticles"
            :key="article.id"
            type="button"
            :class="{ active: activeArticle?.id === article.id }"
            @click="activeId = article.id"
          >
            <small>{{ article.category || "使用教程" }}</small>
            <strong>{{ article.title }}</strong>
            <span>{{ formatDate(article.updatedAt || article.createdAt) }}</span>
          </button>
        </nav>
      </aside>

      <main class="tutorial-doc">
        <div v-if="loading" class="tutorial-state">正在加载教程...</div>
        <div v-else-if="!articles.length" class="tutorial-empty">
          <span>暂无教程</span>
          <h2>管理员发布 Markdown 后，这里会形成文档目录。</h2>
          <p>适合维护版本记录、常用功能说明、开通套餐说明和实验室使用规范。</p>
        </div>
        <article v-else-if="activeArticle" class="doc-paper">
          <header>
            <span>{{ activeArticle.category || "使用教程" }}</span>
            <h2>{{ activeArticle.title }}</h2>
            <time>更新于 {{ formatDate(activeArticle.updatedAt || activeArticle.createdAt) }}</time>
          </header>
          <div class="doc-content" v-html="renderMarkdown(activeArticle.content)"></div>
        </article>
        <div v-else class="tutorial-empty">
          <span>没有匹配内容</span>
          <h2>换一个关键词试试看。</h2>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import MarkdownIt from "markdown-it";
import { paperpilotApi } from "../services/paperpilotApi";

const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true });
const articles = ref([]);
const activeId = ref(null);
const query = ref("");
const loading = ref(false);

const filteredArticles = computed(() => {
  const keyword = query.value.toLowerCase();
  if (!keyword) return articles.value;
  return articles.value.filter((article) => {
    return `${article.title || ""} ${article.category || ""} ${article.content || ""}`.toLowerCase().includes(keyword);
  });
});

const activeArticle = computed(() => {
  return filteredArticles.value.find((article) => article.id === activeId.value) || filteredArticles.value[0] || null;
});

watch(filteredArticles, (items) => {
  if (!items.some((article) => article.id === activeId.value)) {
    activeId.value = items[0]?.id || null;
  }
});

onMounted(loadTutorials);

async function loadTutorials() {
  loading.value = true;
  try {
    articles.value = await paperpilotApi.getTutorials();
    activeId.value = articles.value[0]?.id || null;
  } finally {
    loading.value = false;
  }
}

function renderMarkdown(value) {
  return markdown.render(String(value || "").trim() || "_暂无内容_");
}

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10);
  return new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(date).replace(/\//g, "-");
}
</script>

<style scoped>

/* ═══ TutorialsView — Premium Dual-Theme ═══ */
.tutorials-page, [class*="tutorial"] {
  --c-bg:      #f4f5f8;
  --c-surface: #ffffff;
  --c-border:  rgba(15,23,42,.08);
  --c-text:    #0f172a;
  --c-muted:   #64748b;
  --c-accent:  #6366f1;
  --c-accent2: #a855f7;
  --sh-sm: 0 2px 8px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04);
  --r: 16px; --r-sm: 10px; --r-pill: 999px;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background .3s, color .3s;
}
:root[data-theme="dark"] .tutorials-page,
:root[data-theme="dark"] [class*="tutorial"] {
  --c-bg:      #09090e;
  --c-surface: rgba(18,24,40,.88);
  --c-border:  rgba(255,255,255,.07);
  --c-text:    #f1f5f9;
  --c-muted:   #94a3b8;
}
.tutorials-page h1, .tutorials-page h2, .tutorials-page h3 { color: var(--c-text) !important; }
.tutorials-page p, .tutorials-page li { color: var(--c-muted) !important; }
.tutorials-page .tutorial-card, .tutorials-page .step-card, .tutorials-page article {
  background: var(--c-surface) !important;
  border: 1px solid var(--c-border) !important;
  border-radius: var(--r) !important;
  box-shadow: var(--sh-sm) !important;
  backdrop-filter: blur(16px);
  color: var(--c-text) !important;
  transition: all .25s cubic-bezier(.16,1,.3,1);
}
.tutorials-page .tutorial-card:hover, .tutorials-page article:hover {
  transform: translateY(-3px);
  border-color: rgba(99,102,241,.25) !important;
  box-shadow: 0 8px 28px rgba(15,23,42,.1) !important;
}
.tutorials-page .step-number {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #fff;
  font-size: 14px;
  font-weight: 900;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}
.tutorials-page .tag-chip {
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: rgba(99,102,241,.1);
  color: var(--c-accent);
  font-size: 11.5px;
  font-weight: 750;
  border: 1px solid rgba(99,102,241,.15);
}

.tutorial-page {
  min-height: 100vh;
  padding: 104px clamp(22px, 5vw, 72px) 56px;
  color: #142033;
  background:
    linear-gradient(90deg, rgba(230, 238, 250, .72), rgba(249, 251, 255, .96) 32%, rgba(246, 250, 255, .92)),
    #f5f8fc;
}

.tutorial-shell {
  width: min(1420px, 100%);
  min-height: calc(100vh - 168px);
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 0;
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid #dce6f4;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(27, 45, 79, .08);
}

.tutorial-rail {
  min-height: 0;
  padding: 34px 24px;
  border-right: 1px solid #e3eaf4;
  background: linear-gradient(180deg, #f7faff, #eef5ff);
}

.rail-head span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 850;
}

.rail-head h1 {
  margin: 10px 0 10px;
  font-size: 32px;
  letter-spacing: 0;
}

.rail-head p {
  margin: 0;
  color: #5d6a7f;
  font-size: 14px;
  line-height: 1.7;
}

.tutorial-filter input {
  width: 100%;
  height: 42px;
  box-sizing: border-box;
  margin: 24px 0 18px;
  padding: 0 14px;
  border: 1px solid #d6e0ed;
  border-radius: 10px;
  color: #142033;
  background: rgba(255,255,255,.82);
  font: inherit;
  outline: none;
}

.tutorial-list {
  display: grid;
  gap: 10px;
}

.tutorial-list button {
  width: 100%;
  padding: 14px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: #31415a;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.tutorial-list button.active,
.tutorial-list button:hover {
  border-color: #cddcf2;
  background: #fff;
}

.tutorial-list small,
.tutorial-list span {
  display: block;
  color: #718096;
  font-size: 12px;
}

.tutorial-list strong {
  display: block;
  margin: 4px 0 7px;
  color: #142033;
  font-size: 15px;
  line-height: 1.45;
}

.tutorial-doc {
  min-width: 0;
  overflow: auto;
  padding: clamp(34px, 5vw, 72px);
  background: #fff;
}

.doc-paper {
  max-width: 820px;
}

.doc-paper header {
  margin-bottom: 34px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e6edf6;
}

.doc-paper header span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 850;
}

.doc-paper h2 {
  margin: 12px 0;
  color: #111827;
  font-size: clamp(32px, 4vw, 54px);
  line-height: 1.12;
  letter-spacing: -0.02em;
  text-wrap: balance;
}

.doc-paper time {
  color: #6b778c;
  font-size: 13px;
}

.doc-content {
  color: #202b3d;
  font-size: 16px;
  line-height: 1.85;
}

.doc-content :deep(h1),
.doc-content :deep(h2),
.doc-content :deep(h3) {
  margin: 32px 0 12px;
  color: #111827;
  letter-spacing: 0;
}

.doc-content :deep(p) { margin: 0 0 18px; }
.doc-content :deep(ul),
.doc-content :deep(ol) { padding-left: 24px; margin: 10px 0 20px; }
.doc-content :deep(blockquote) {
  margin: 24px 0;
  padding: 18px 20px;
  border: 1px solid #dce6f4;
  border-radius: 12px;
  color: #32415a;
  background: #f6f9fe;
}
.doc-content :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  color: #0f5bd7;
  background: #edf4ff;
}
.doc-content :deep(pre) {
  overflow: auto;
  padding: 18px;
  border-radius: 12px;
  color: #e8efff;
  background: #111827;
}
.doc-content :deep(a) { color: #075ee5; font-weight: 800; text-decoration: none; }
.doc-content :deep(table) { width: 100%; border-collapse: collapse; margin: 18px 0; }
.doc-content :deep(th),
.doc-content :deep(td) { padding: 10px 12px; border: 1px solid #dfe7f2; text-align: left; }
.doc-content :deep(th) { background: #f4f7fb; }

.tutorial-state,
.tutorial-empty {
  min-height: 360px;
  display: grid;
  align-content: center;
  justify-items: start;
  color: #667085;
}

.tutorial-empty span {
  color: #2563eb;
  font-weight: 850;
}

.tutorial-empty h2 {
  margin: 12px 0 8px;
  color: #142033;
  font-size: 30px;
}

.tutorial-empty p {
  max-width: 560px;
  line-height: 1.7;
}

@media (max-width: 860px) {
  .tutorial-page { padding: 84px 14px 30px; }
  .tutorial-shell { grid-template-columns: minmax(0, 1fr); }
  .tutorial-rail { border-right: 0; border-bottom: 1px solid #e3eaf4; }
  .tutorial-doc { padding: 28px 22px; }
}
</style>
