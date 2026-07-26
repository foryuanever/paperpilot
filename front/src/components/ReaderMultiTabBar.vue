<template>
  <div class="reader-multi-tab-bar" aria-label="文献阅读多标签页导航">
    <div class="tab-bar-container">
      <!-- 首页 / 文献库 标签 (无框浮动胶囊) -->
      <button
        class="tab-item home-tab"
        :class="{ active: isHomeActive }"
        title="返回文献库首页"
        @click="goHome"
      >
        <svg class="home-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
        <span>首页</span>
      </button>

      <!-- 打开的论文标签页列表 (无框无线条自然排列) -->
      <div class="open-tabs-scroll">
        <div
          v-for="tab in libraryStore.openTabs"
          :key="tab.id"
          class="tab-item paper-tab"
          :class="{ active: !isHomeActive && libraryStore.state.activeDocumentId === tab.id }"
          :title="tab.title"
          @click="switchTab(tab)"
        >
          <svg class="paper-doc-icon" viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
            <line x1="16" y1="13" x2="8" y2="13"></line>
            <line x1="16" y1="17" x2="8" y2="17"></line>
          </svg>

          <span class="tab-title">{{ tab.title }}</span>

          <button
            class="tab-close-btn"
            title="关闭此标签页"
            @click.stop="closeTab(tab.id)"
          >
            <svg viewBox="0 0 24 24" width="10" height="10" stroke="currentColor" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>

      <!-- 标签页下拉选择菜单按钮 (无框) -->
      <div class="tab-dropdown-wrap">
        <button
          class="tab-dropdown-trigger"
          :class="{ active: showDropdown }"
          title="查看所有打开的文献标签"
          @click.stop="showDropdown = !showDropdown"
        >
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>

        <!-- 下拉弹出层 -->
        <Transition name="tab-popover-fade">
          <div v-if="showDropdown" class="tab-dropdown-popover" @click.stop>
            <header class="popover-head">
              <div class="popover-head-title">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h16M4 18h16"/></svg>
                <strong>打开的文献 ({{ libraryStore.openTabs.length }})</strong>
              </div>
              <button
                v-if="libraryStore.openTabs.length"
                class="close-all-btn"
                @click="closeAllTabs"
              >
                关闭全部
              </button>
            </header>

            <!-- 搜索框 -->
            <div v-if="libraryStore.openTabs.length > 3" class="popover-search">
              <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              <input v-model="filterQuery" type="text" placeholder="搜索已打开文献..." />
            </div>

            <div v-if="filteredTabs.length" class="popover-list">
              <div
                v-for="tab in filteredTabs"
                :key="tab.id"
                class="popover-item"
                :class="{ active: !isHomeActive && libraryStore.state.activeDocumentId === tab.id }"
                @click="switchTabFromDropdown(tab)"
              >
                <span class="popover-item-dot"></span>
                <span class="popover-item-title">{{ tab.title }}</span>
                <button
                  class="popover-item-close"
                  title="关闭"
                  @click.stop="closeTab(tab.id)"
                >
                  <svg viewBox="0 0 24 24" width="10" height="10" stroke="currentColor" stroke-width="2.5" fill="none"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </div>
            <div v-else class="popover-empty">
              {{ filterQuery ? '没有找到匹配的文献' : '暂未打开任何文献' }}
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useLibraryStore } from "../stores/library";

const props = defineProps({
  targetPath: {
    type: String,
    default: "/reader",
  },
  targetQuery: {
    type: Object,
    default: () => ({ mode: "line", panel: "analysis" }),
  },
});

const router = useRouter();
const route = useRoute();
const libraryStore = useLibraryStore();

const showDropdown = ref(false);
const filterQuery = ref("");

const isHomeActive = computed(() => {
  return route.path === "/library" || route.path === "/dashboard";
});

const filteredTabs = computed(() => {
  const query = String(filterQuery.value || "").trim().toLowerCase();
  if (!query) return libraryStore.openTabs;
  return libraryStore.openTabs.filter(tab =>
    String(tab.title || "").toLowerCase().includes(query)
  );
});

function goHome() {
  router.push("/library");
}

function switchTab(tab) {
  libraryStore.setActiveDocument(tab.id);
  if (route.path !== props.targetPath) {
    router.push({ path: props.targetPath, query: props.targetQuery });
  }
}

function switchTabFromDropdown(tab) {
  showDropdown.value = false;
  switchTab(tab);
}

function closeTab(id) {
  const nextActiveId = libraryStore.closeTab(id);
  if (!isHomeActive.value && libraryStore.state.activeDocumentId === id) {
    if (nextActiveId) {
      libraryStore.setActiveDocument(nextActiveId);
    } else {
      router.push("/library");
    }
  }
}

function closeAllTabs() {
  libraryStore.closeAllTabs();
  showDropdown.value = false;
  if (!isHomeActive.value) {
    router.push("/library");
  }
}

function handleGlobalClick() {
  showDropdown.value = false;
}

onMounted(() => {
  window.addEventListener("click", handleGlobalClick);
});

onUnmounted(() => {
  window.removeEventListener("click", handleGlobalClick);
});
</script>

<style scoped>
.reader-multi-tab-bar {
  width: 100%;
  height: 40px;
  background: transparent;
  user-select: none;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "PingFang SC", sans-serif;
  box-sizing: border-box;
  position: relative;
  z-index: 30;
  padding: 4px 10px 3px;
  overflow: hidden;
}

.tab-bar-container {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 6px;
  min-width: 0;
}

/* 无框自然胶囊标签基类 */
.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  padding: 0 12px;
  border-radius: 9px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  position: relative;
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-item:hover:not(.active) {
  background: rgba(99, 102, 241, 0.08);
  color: #1e293b;
  transform: translateY(-1px);
}

/* 首页标签页 (无框浮动胶囊) */
.home-tab {
  flex-shrink: 0;
  margin-right: 2px;
  font-weight: 600;
  border-radius: 9px;
  height: 30px;
}

.home-icon {
  color: #64748b;
  transition: color 0.2s ease;
}

.home-tab:hover .home-icon {
  color: #6366f1;
}

.home-tab.active {
  background: #ffffff;
  color: #4f46e5;
  font-weight: 700;
  box-shadow: 0 3px 12px rgba(99, 102, 241, 0.16);
}

.home-tab.active .home-icon {
  color: #6366f1;
}

/* 打开的论文标签页列表 (无框自然滑块) */
.open-tabs-scroll {
  display: flex;
  align-items: center;
  overflow-x: auto;
  scrollbar-width: none;
  flex: 1 1 0;
  height: 100%;
  gap: 4px;
  min-width: 0;
  overscroll-behavior-x: contain;
}

.open-tabs-scroll::-webkit-scrollbar {
  display: none;
}

.paper-tab {
  width: clamp(150px, 18vw, 260px);
  min-width: 150px;
  max-width: 260px;
  flex: 0 0 auto;
  justify-content: space-between;
}

.paper-doc-icon {
  color: #94a3b8;
  flex-shrink: 0;
  transition: color 0.2s ease;
}

.paper-tab.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.14);
}

.paper-tab.active .paper-doc-icon {
  color: #6366f1;
}

.tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  letter-spacing: -0.01em;
}

.tab-close-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin-left: 6px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.18s ease;
  flex-shrink: 0;
  opacity: 0.6;
}

.paper-tab:hover .tab-close-btn,
.paper-tab.active .tab-close-btn {
  opacity: 1;
}

.tab-close-btn:hover {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
  transform: scale(1.1);
}

/* 下拉菜单按钮 (无框) */
.tab-dropdown-wrap {
  position: relative;
  flex-shrink: 0;
}

.tab-dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-dropdown-trigger:hover,
.tab-dropdown-trigger.active {
  background: rgba(99, 102, 241, 0.1);
  color: #4f46e5;
}

@media (max-width: 920px) {
  .reader-multi-tab-bar {
    padding-inline: 6px;
  }

  .tab-item {
    height: 30px;
    padding-inline: 10px;
  }

  .paper-tab {
    width: 170px;
    min-width: 170px;
    max-width: 170px;
  }

  .home-tab {
    gap: 5px;
    padding-inline: 9px;
  }
}

@media (max-width: 560px) {
  .paper-tab {
    width: 148px;
    min-width: 148px;
    max-width: 148px;
  }

  .home-tab span {
    display: none;
  }

  .home-tab {
    width: 34px;
    justify-content: center;
    padding-inline: 0;
  }

  .tab-dropdown-popover {
    right: -4px;
    width: min(320px, calc(100vw - 16px));
  }
}

/* 下拉弹出层 */
.tab-dropdown-popover {
  position: absolute;
  top: 36px;
  right: 0;
  z-index: 200;
  width: 340px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(20px);
  border: none;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.18);
  padding: 12px;
  box-sizing: border-box;
}

.popover-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 9px;
  margin-bottom: 8px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
}

.popover-head-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6366f1;
}

.popover-head strong {
  font-size: 12.5px;
  font-weight: 700;
  color: #0f172a;
}

.close-all-btn {
  border: none;
  background: transparent;
  color: #ef4444;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 3px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.close-all-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}

.popover-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: rgba(241, 245, 249, 0.8);
  border: none;
}

.popover-search svg {
  color: #94a3b8;
}

.popover-search input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 11.5px;
  color: #0f172a;
  width: 100%;
}

.popover-list {
  max-height: 280px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.popover-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 9px;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s ease;
}

.popover-item:hover {
  background: rgba(241, 245, 249, 0.9);
  color: #0f172a;
}

.popover-item.active {
  background: rgba(99, 102, 241, 0.12);
  color: #4f46e5;
  font-weight: 600;
}

.popover-item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #cbd5e1;
  flex-shrink: 0;
}

.popover-item.active .popover-item-dot {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.popover-item-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.popover-item-close {
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  padding: 3px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.popover-item-close:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.popover-empty {
  padding: 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
}

.tab-popover-fade-enter-active,
.tab-popover-fade-leave-active {
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1), transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-popover-fade-enter-from,
.tab-popover-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}

/* ── 🌙 DARK MODE ── */
:root[data-theme="dark"] .reader-multi-tab-bar {
  background: transparent;
}

:root[data-theme="dark"] .tab-item {
  color: #94a3b8;
}

:root[data-theme="dark"] .tab-item:hover:not(.active) {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

:root[data-theme="dark"] .home-tab.active {
  background: #1e293b;
  color: #818cf8;
  box-shadow: 0 3px 12px rgba(129, 140, 248, 0.25);
}

:root[data-theme="dark"] .home-tab.active .home-icon {
  color: #818cf8;
}

:root[data-theme="dark"] .paper-tab.active {
  background: #1e293b;
  color: #f8fafc;
  box-shadow: 0 4px 16px rgba(129, 140, 248, 0.25), 0 4px 12px rgba(0, 0, 0, 0.4);
}

:root[data-theme="dark"] .paper-tab.active .paper-doc-icon {
  color: #818cf8;
}

:root[data-theme="dark"] .tab-close-btn {
  color: #64748b;
}

:root[data-theme="dark"] .tab-close-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

:root[data-theme="dark"] .tab-dropdown-trigger:hover,
:root[data-theme="dark"] .tab-dropdown-trigger.active {
  background: rgba(255, 255, 255, 0.1);
  color: #818cf8;
}

:root[data-theme="dark"] .tab-dropdown-popover {
  background: rgba(15, 23, 42, 0.96);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.7);
}

:root[data-theme="dark"] .popover-head {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

:root[data-theme="dark"] .popover-head strong {
  color: #f8fafc;
}

:root[data-theme="dark"] .popover-search {
  background: rgba(255, 255, 255, 0.06);
}

:root[data-theme="dark"] .popover-search input {
  color: #f8fafc;
}

:root[data-theme="dark"] .popover-item {
  color: #94a3b8;
}

:root[data-theme="dark"] .popover-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #f8fafc;
}

:root[data-theme="dark"] .popover-item.active {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}
</style>
