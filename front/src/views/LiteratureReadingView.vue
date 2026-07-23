<template>
  <div class="reading-entry spatial-page">
    <section v-if="loading" class="reading-entry-state">
      <span class="reading-entry-pulse"></span>
      <h1>正在恢复上次阅读</h1>
      <p>正在读取当前账号最后打开的论文。</p>
    </section>
    <section v-else class="reading-entry-state">
      <div class="reading-empty-mark">文</div>
      <h1>先添加一篇论文，再开始阅读</h1>
      <p>这里会持续保留当前账号最后一次打开的论文。下次进入“文献阅读”，会直接回到原来的阅读内容。</p>
      <div class="reading-entry-actions">
        <router-link class="spatial-btn spatial-btn-accent" to="/library?tab=add">去添加论文</router-link>
        <router-link class="spatial-btn spatial-btn-ghost" to="/library">查看文献库</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { useLibraryStore } from "../stores/library";
import { getLastReadingId } from "../utils/readingMemory";

const loading = ref(true);
const router = useRouter();
const authStore = useAuthStore();
const libraryStore = useLibraryStore();

onMounted(async () => {
  try {
    await libraryStore.hydrateLibrary();
    const lastId = getLastReadingId(authStore.session.user);
    const lastPaper = libraryStore.state.documents.find(
      paper => String(paper.workspaceId || paper.id) === String(lastId),
    );
    if (lastPaper) {
      libraryStore.setActiveDocument(lastPaper.id);
      await router.replace({ path: "/reader", query: { mode: "line", panel: "analysis" } });
      return;
    }
  } catch (error) {
    console.warn("restore last reading failed", error);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.reading-entry {
  min-height: calc(100vh - 138px);
  display: grid;
  place-items: center;
  padding: 40px 20px 80px;
  box-sizing: border-box;
}
.reading-entry-state { width: min(620px, 100%); display: grid; justify-items: center; text-align: center; }

.reading-entry-state h1 {
  margin: 18px 0 10px;
  font-size: 26px;
  color: #172033;
}
:root[data-theme="dark"] .reading-entry-state h1 { color: #f1f5f9; }

.reading-entry-state p {
  max-width: 52ch;
  margin: 0;
  color: #5f6c80;
  font-size: 14px;
  line-height: 1.8;
}
:root[data-theme="dark"] .reading-entry-state p { color: #94a3b8; }

.reading-entry-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; margin-top: 26px; }

.reading-empty-mark {
  width: 62px; height: 62px;
  display: grid; place-items: center;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  font: 700 25px/1 "Songti SC", serif;
  box-shadow: 0 8px 24px rgba(99,102,241,.3);
}

.reading-entry-pulse {
  width: 34px; height: 34px;
  border: 3px solid rgba(99,102,241,.2);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: reading-spin 700ms linear infinite;
}
:root[data-theme="dark"] .reading-entry-pulse {
  border-color: rgba(99,102,241,.2);
  border-top-color: #818cf8;
}

@keyframes reading-spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) {
  .reading-entry { min-height: calc(100vh - 112px); padding-inline: 16px; }
  .reading-entry-state h1 { font-size: 22px; }
}
@media (prefers-reduced-motion: reduce) { .reading-entry-pulse { animation: none; } }
</style>
