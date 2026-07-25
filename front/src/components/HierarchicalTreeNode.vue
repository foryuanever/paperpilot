<template>
  <div class="tree-node-wrapper" :style="{ paddingLeft: `${depth * 12}px` }">
    <div
      class="tree-node-row"
      :class="{ selected: selectedNodeId === node.id }"
      @click="$emit('select', node)"
    >
      <!-- 展开/折叠箭头 (若是目录/非空节点) -->
      <button
        v-if="node.type === 'folder' || (node.children && node.children.length)"
        class="arrow-btn"
        :class="{ open: node.expanded }"
        @click.stop="$emit('toggle-expand', node)"
      >
        <svg viewBox="0 0 24 24" width="10" height="10" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
      </button>
      <span v-else class="arrow-placeholder"></span>

      <!-- 类型图标 (SVG 矢量替代 Emoji) -->
      <span class="node-icon" :class="`node-icon-${node.type || 'note'}`">
        <svg v-if="node.type === 'folder'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
        </svg>
        <svg v-else-if="node.type === 'excerpt'" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
          <line x1="7" y1="7" x2="7.01" y2="7"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
      </span>

      <!-- 标题 -->
      <span class="node-title" :title="node.title">{{ node.title }}</span>

      <!-- 悬浮操作组 (全矢量 SVG 按钮) -->
      <div class="node-hover-actions">
        <button
          v-if="node.type === 'folder'"
          class="mini-btn instant-tooltip"
          data-tip="添加子节点"
          @click.stop="$emit('add-child', node.id)"
        >
          <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        </button>
        <button
          class="mini-btn instant-tooltip"
          data-tip="编辑节点"
          @click.stop="$emit('edit', node)"
        >
          <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
        </button>
        <button
          class="mini-btn delete-btn instant-tooltip"
          data-tip="删除节点"
          @click.stop="$emit('delete', node)"
        >
          <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </div>
    </div>

    <div v-if="selectedNodeId === node.id" class="inline-node-detail" @click.stop>
      <textarea
        v-model="node.content"
        rows="3"
        placeholder="在这个层级下记录笔记、疑问、总结或 Markdown 要点…"
        @change="$emit('save')"
      ></textarea>
      <div v-if="node.quoteText" class="inline-quote-box">
        <small>关联论文原文</small>
        <blockquote>“{{ node.quoteText }}”</blockquote>
        <button type="button" @click="$emit('jump', node)">跳转到 P.{{ node.page || 1 }}</button>
      </div>
    </div>

    <!-- 子级节点递归渲染 -->
    <div v-if="node.expanded && node.children && node.children.length" class="tree-node-children">
      <HierarchicalTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :selected-node-id="selectedNodeId"
        @select="$emit('select', $event)"
        @save="$emit('save')"
        @toggle-expand="$emit('toggle-expand', $event)"
        @add-child="$emit('add-child', $event)"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
        @jump="$emit('jump', $event)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 },
  selectedNodeId: { type: String, default: null }
});

defineEmits(["select", "save", "toggle-expand", "add-child", "edit", "delete", "jump"]);
</script>

<style scoped>
.tree-node-wrapper {
  user-select: none;
}

.tree-node-row {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 30px;
  padding: 0 6px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: all 0.15s ease;
}

.tree-node-row:hover {
  background: var(--notes-hover);
}

.tree-node-row.selected {
  background: rgba(8, 145, 178, 0.12);
  font-weight: 700;
}

:global(:root[data-theme="dark"]) .tree-node-row.selected {
  background: rgba(8, 145, 178, 0.16);
  box-shadow: inset 0 0 0 1px rgba(34, 211, 238, 0.2);
}

:global(:root[data-theme="dark"]) .tree-node-row:hover {
  background: rgba(8, 145, 178, 0.11);
}

.arrow-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border: none;
  background: transparent;
  color: var(--notes-muted);
  cursor: pointer;
  transition: transform 0.15s ease;
}

.arrow-btn.open {
  transform: rotate(90deg);
}

.arrow-placeholder {
  width: 14px;
}

.node-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  flex-shrink: 0;
}

:global(:root[data-theme="dark"]) .node-icon {
  color: #7dd3fc;
}

.node-icon-folder {
  color: #4f46e5;
}

.node-icon-note {
  color: #0891b2;
}

.node-icon-excerpt {
  color: #d97706;
}

:global(html[data-theme="dark"] .node-icon-folder) {
  color: #a5b4fc !important;
}

:global(html[data-theme="dark"] .node-icon-note) {
  color: #67e8f9 !important;
}

:global(html[data-theme="dark"] .node-icon-excerpt) {
  color: #fbbf24 !important;
}

.node-title {
  flex: 1;
  font-size: 11.8px;
  color: var(--notes-subtext);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tree-node-row.selected .node-title {
  color: var(--notes-text);
}

.node-hover-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.mini-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 6px;
  background: var(--notes-card-bg);
  color: var(--notes-subtext);
  cursor: pointer;
}

.mini-btn svg {
  width: 13px;
  height: 13px;
}

.tree-node-row:hover .node-hover-actions,
.tree-node-row.selected .node-hover-actions {
  opacity: 1;
}

.mini-btn:hover {
  background: #6366f1;
  color: #ffffff;
}

:global(:root[data-theme="dark"]) .mini-btn {
  background: rgba(15, 23, 42, 0.9);
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.32);
  box-shadow: none;
}

:global(:root[data-theme="dark"]) .mini-btn:hover {
  background: rgba(8, 145, 178, 0.28);
  color: #ffffff;
  border-color: rgba(34, 211, 238, 0.42);
  box-shadow: 0 6px 14px rgba(8, 145, 178, 0.18);
}

.mini-btn.delete-btn:hover {
  background: #ef4444;
  color: #ffffff;
}

:global(html[data-theme="dark"] .tree-node-row:hover) {
  background: rgba(8, 145, 178, 0.11) !important;
}

:global(html[data-theme="dark"] .tree-node-row.selected) {
  background: rgba(8, 145, 178, 0.16) !important;
  box-shadow: inset 0 0 0 1px rgba(34, 211, 238, 0.2) !important;
}

:global(html[data-theme="dark"] .tree-node-row .node-icon) {
  color: #7dd3fc !important;
}

:global(html[data-theme="dark"] .tree-node-row .node-title) {
  color: #d8e2f0 !important;
}

:global(html[data-theme="dark"] .tree-node-row.selected .node-title) {
  color: #fbf8ff !important;
}

:global(html[data-theme="dark"] .tree-node-row .arrow-btn) {
  color: #a797d2 !important;
}

:global(html[data-theme="dark"] .tree-node-row .mini-btn) {
  background: rgba(15, 23, 42, 0.9) !important;
  color: #cbd5e1 !important;
  border-color: rgba(148, 163, 184, 0.32) !important;
  box-shadow: none !important;
}

:global(html[data-theme="dark"] .tree-node-row .node-icon-folder) {
  color: #a5b4fc !important;
}

:global(html[data-theme="dark"] .tree-node-row .node-icon-note) {
  color: #67e8f9 !important;
}

:global(html[data-theme="dark"] .tree-node-row .node-icon-excerpt) {
  color: #fbbf24 !important;
}

.tree-node-children {
  margin-top: 2px;
}

.inline-node-detail {
  margin: 1px 0 8px 31px;
  padding: 2px 0 2px 8px;
  border-left: 2px solid rgba(8, 145, 178, 0.34);
  background: transparent;
  box-sizing: border-box;
}

.inline-node-detail textarea {
  width: 100%;
  min-height: 56px;
  box-sizing: border-box;
  resize: vertical;
  border: 0;
  border-radius: 6px;
  padding: 5px 7px;
  color: var(--notes-input-text);
  background: rgba(248, 250, 252, 0.06);
  outline: none;
  font: 11px/1.48 inherit;
}

.inline-node-detail textarea:focus {
  background: rgba(8, 145, 178, 0.1);
  box-shadow: inset 0 0 0 1px rgba(34, 211, 238, 0.24);
}

.inline-quote-box {
  margin-top: 8px;
  padding: 7px 8px;
  border-left: 3px solid #f59e0b;
  border-radius: 7px;
  background: rgba(245, 158, 11, 0.08);
}

.inline-quote-box small {
  color: var(--notes-muted);
  font-size: 10px;
}

.inline-quote-box blockquote {
  max-height: 82px;
  overflow: auto;
  margin: 4px 0;
  color: var(--notes-subtext);
  font-size: 11px;
  line-height: 1.55;
}

.inline-quote-box button {
  padding: 0;
  border: 0;
  color: #0891b2;
  background: transparent;
  cursor: pointer;
  font-size: 10.5px;
  font-weight: 700;
}

:global(html[data-theme="dark"] .inline-node-detail) {
  border-left-color: rgba(34, 211, 238, 0.36) !important;
  background: transparent !important;
}

:global(html[data-theme="dark"] .inline-node-detail textarea) {
  color: #d8e2f0 !important;
  background: rgba(15, 23, 42, 0.42) !important;
}

:global(html[data-theme="dark"] .inline-node-detail textarea::placeholder) {
  color: #7e8ba3 !important;
}

:global(html[data-theme="dark"] .inline-quote-box) {
  background: rgba(245, 158, 11, 0.1) !important;
}

.instant-tooltip {
  position: relative;
}

.instant-tooltip::after {
  content: attr(data-tip);
  position: fixed;
  z-index: 2000;
  top: 72px;
  right: 14px;
  max-width: 170px;
  padding: 7px 9px;
  border-radius: 7px;
  color: #f8fbff;
  background: rgba(15, 23, 42, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.28);
  box-shadow: 0 10px 28px rgba(2, 6, 23, 0.38);
  font-size: 11px;
  font-weight: 650;
  line-height: 1.3;
  opacity: 0;
  pointer-events: none;
  transform: translateY(-2px);
  transition: opacity 40ms linear, transform 40ms linear;
  white-space: normal;
}

.instant-tooltip:hover::after {
  opacity: 1;
  transform: translateY(0);
}
</style>
