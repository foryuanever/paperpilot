<template>
  <div class="tutorial-page">
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
import { useRoute } from "vue-router";
import MarkdownIt from "markdown-it";
import { paperpilotApi } from "../services/paperpilotApi";

const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true });
const route = useRoute();
const articles = ref([]);
const activeId = ref(null);
const query = ref(String(route.query.q || ""));
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

/* ═══ TutorialsView — readable dual theme ═══ */
.tutorial-page {
  --guide-bg: #eef4fb;
  --guide-shell: #ffffff;
  --guide-rail: #f7faff;
  --guide-rail-strong: #edf5ff;
  --guide-border: #d6e1ef;
  --guide-text: #142033;
  --guide-heading: #0f172a;
  --guide-muted: #5f6f85;
  --guide-soft: #eef5ff;
  --guide-accent: #2563eb;
  --guide-accent-2: #0f766e;
  min-height: 100vh;
  padding: 82px clamp(18px, 4vw, 54px) 46px;
  color: var(--guide-text);
  background:
    linear-gradient(90deg, rgba(226, 235, 247, .96), rgba(248, 251, 255, 1) 34%, rgba(241, 247, 255, .98)),
    var(--guide-bg);
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background .2s ease, color .2s ease;
  opacity: 1 !important;
}

.tutorial-shell {
  width: min(1500px, 100%);
  min-height: calc(100vh - 128px);
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  gap: 0;
  margin: 0 auto;
  overflow: hidden;
  border: 1px solid var(--guide-border);
  border-radius: 14px;
  background: var(--guide-shell);
  box-shadow: none;
  opacity: 1 !important;
}

.tutorial-rail {
  min-height: 0;
  padding: 30px 24px;
  border-right: 1px solid var(--guide-border);
  background: linear-gradient(180deg, var(--guide-rail), var(--guide-rail-strong));
  opacity: 1 !important;
}

.rail-head span {
  color: var(--guide-accent);
  font-size: 12px;
  font-weight: 850;
}

.rail-head h1 {
  margin: 10px 0 10px;
  color: var(--guide-heading);
  font-size: 28px;
  letter-spacing: 0;
}

.rail-head p {
  margin: 0;
  color: var(--guide-muted);
  font-size: 14px;
  line-height: 1.7;
  opacity: 1;
}

.tutorial-filter input {
  width: 100%;
  height: 42px;
  box-sizing: border-box;
  margin: 24px 0 18px;
  padding: 0 14px;
  border: 1px solid var(--guide-border);
  border-radius: 10px;
  color: var(--guide-text);
  background: var(--guide-shell);
  font: inherit;
  outline: none;
  opacity: 1;
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
  color: var(--guide-text);
  background: transparent;
  text-align: left;
  cursor: pointer;
  opacity: 1;
}

.tutorial-list button.active,
.tutorial-list button:hover {
  border-color: color-mix(in srgb, var(--guide-accent) 24%, var(--guide-border));
  background: #ffffff;
}

.tutorial-list small,
.tutorial-list span {
  display: block;
  color: var(--guide-muted);
  font-size: 12px;
  opacity: 1;
}

.tutorial-list strong {
  display: block;
  margin: 4px 0 7px;
  color: var(--guide-heading);
  font-size: 15px;
  line-height: 1.45;
  opacity: 1;
}

.tutorial-doc {
  min-width: 0;
  overflow: auto;
  padding: clamp(36px, 5vw, 68px);
  background: var(--guide-shell);
  opacity: 1 !important;
}

.doc-paper {
  max-width: 940px;
  color: var(--guide-text);
  opacity: 1 !important;
}

.doc-paper header {
  margin-bottom: 34px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--guide-border);
}

.doc-paper header span {
  color: var(--guide-accent);
  font-size: 13px;
  font-weight: 850;
}

.doc-paper h2 {
  margin: 12px 0;
  color: var(--guide-heading);
  font-size: 42px;
  line-height: 1.18;
  letter-spacing: 0;
  text-wrap: balance;
  opacity: 1;
}

.doc-paper time {
  color: var(--guide-muted);
  font-size: 13px;
  opacity: 1;
}

.doc-content {
  color: var(--guide-text);
  font-size: 16.5px;
  line-height: 1.85;
  opacity: 1 !important;
}

.doc-content :deep(h1),
.doc-content :deep(h2),
.doc-content :deep(h3) {
  margin: 34px 0 12px;
  color: var(--guide-heading);
  letter-spacing: 0;
  opacity: 1;
}

.doc-content :deep(p) {
  margin: 0 0 18px;
  color: var(--guide-text);
  opacity: 1;
}
.doc-content :deep(li) {
  color: var(--guide-text);
  opacity: 1;
}
.doc-content :deep(ul),
.doc-content :deep(ol) { padding-left: 24px; margin: 10px 0 20px; }
.doc-content :deep(blockquote) {
  margin: 24px 0;
  padding: 18px 20px;
  border: 1px solid var(--guide-border);
  border-radius: 12px;
  color: var(--guide-text);
  background: var(--guide-soft);
  opacity: 1;
}
.doc-content :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  color: var(--guide-accent);
  background: color-mix(in srgb, var(--guide-accent) 12%, var(--guide-shell));
}
.doc-content :deep(pre) {
  overflow: auto;
  padding: 18px;
  border-radius: 12px;
  color: #e8efff;
  background: #111827;
}
.doc-content :deep(a) { color: #075ee5; font-weight: 800; text-decoration: none; }
.doc-content :deep(img) {
  display: block;
  width: min(840px, 100%);
  height: auto;
  margin: 18px 0 28px;
  border: 1px solid var(--guide-border);
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, .1);
}
.doc-content :deep(table) { width: 100%; border-collapse: collapse; margin: 18px 0; }
.doc-content :deep(th),
.doc-content :deep(td) { padding: 10px 12px; border: 1px solid var(--guide-border); text-align: left; }
.doc-content :deep(th) { background: var(--guide-soft); color: var(--guide-heading); }

.tutorial-state,
.tutorial-empty {
  min-height: 360px;
  display: grid;
  align-content: center;
  justify-items: start;
  color: var(--guide-muted);
}

.tutorial-empty span {
  color: var(--guide-accent);
  font-weight: 850;
}

.tutorial-empty h2 {
  margin: 12px 0 8px;
  color: var(--guide-heading);
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

:global(:root[data-theme="dark"]) .tutorial-page {
  --guide-bg: #070b14;
  --guide-shell: #0f1726;
  --guide-rail: #111b2b;
  --guide-rail-strong: #0b1220;
  --guide-border: rgba(148, 163, 184, .2);
  --guide-text: #dbe7f7;
  --guide-heading: #f6f8fb;
  --guide-muted: #9ba9bd;
  --guide-soft: rgba(30, 41, 59, .72);
  --guide-accent: #60a5fa;
  --guide-accent-2: #2dd4bf;
  background:
    radial-gradient(circle at 16% 10%, rgba(37, 99, 235, .2), transparent 28%),
    linear-gradient(90deg, #07101f, #0b1020 42%, #05070d);
}

:global(:root[data-theme="dark"]) .tutorial-shell {
  box-shadow: none;
}

:global(:root[data-theme="dark"]) .tutorial-filter input::placeholder {
  color: #7f8da3;
}

:global(:root[data-theme="dark"]) .tutorial-list button.active,
:global(:root[data-theme="dark"]) .tutorial-list button:hover {
  background: rgba(96, 165, 250, .11);
}

:global(:root[data-theme="dark"]) .doc-content :deep(img) {
  background: #111827;
  box-shadow: 0 16px 38px rgba(0, 0, 0, .34);
}

:global(:root[data-theme="dark"]) .doc-content :deep(a) {
  color: #7dd3fc;
}

:global(:root[data-theme="dark"]) .doc-content :deep(code) {
  color: #93c5fd;
  background: rgba(37, 99, 235, .16);
}
</style>

<style>
.app-tutorial-root {
  min-height: 100vh;
  color: #142033;
  background: #eef4fb;
}

:root[data-theme="dark"] .app-tutorial-root {
  color: #e5edf8;
  background: #070b14;
}

.app-tutorial-main {
  min-height: 100vh;
  padding: 0;
}

.app-tutorial-root .tutorial-page,
.app-tutorial-root .tutorial-page * {
  opacity: 1;
}

.app-tutorial-root .tutorial-page .rail-head h1,
.app-tutorial-root .tutorial-page .doc-paper h2,
.app-tutorial-root .tutorial-page .doc-content h1,
.app-tutorial-root .tutorial-page .doc-content h2,
.app-tutorial-root .tutorial-page .doc-content h3,
.app-tutorial-root .tutorial-page .tutorial-list strong {
  color: #0f172a !important;
}

.app-tutorial-root .tutorial-page,
.app-tutorial-root .tutorial-page .doc-content,
.app-tutorial-root .tutorial-page .doc-content p,
.app-tutorial-root .tutorial-page .doc-content li,
.app-tutorial-root .tutorial-page .rail-head p {
  color: #142033 !important;
}

.app-tutorial-root .tutorial-page .tutorial-list small,
.app-tutorial-root .tutorial-page .tutorial-list span,
.app-tutorial-root .tutorial-page .doc-paper time {
  color: #5f6f85 !important;
}

:root[data-theme="dark"] .app-tutorial-root .tutorial-page .rail-head h1,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-paper h2,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-content h1,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-content h2,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-content h3,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .tutorial-list strong {
  color: #f6f8fb !important;
}

:root[data-theme="dark"] .app-tutorial-root .tutorial-page,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-content,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-content p,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-content li,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .rail-head p {
  color: #dbe7f7 !important;
}

:root[data-theme="dark"] .app-tutorial-root .tutorial-page .tutorial-list small,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .tutorial-list span,
:root[data-theme="dark"] .app-tutorial-root .tutorial-page .doc-paper time {
  color: #9ba9bd !important;
}
</style>
