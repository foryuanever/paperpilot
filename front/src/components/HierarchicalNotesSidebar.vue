<template>
  <aside class="hierarchical-notes-sidebar" :class="{ collapsed: isCollapsed }">
    <!-- 折叠沉浸模式竖排展开控制轴 (方便随时点击展开) -->
    <div v-if="isCollapsed" class="collapsed-strip" @click="$emit('toggle-collapse')">
      <button class="expand-edge-btn instant-tooltip" data-tip="展开层级笔记栏">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6"/>
        </svg>
      </button>
      <div class="vertical-title-wrap">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
        <span class="vertical-text">文献笔记</span>
      </div>
      <span class="collapsed-count-pill">{{ totalNotesCount }}</span>
    </div>

    <template v-else>
      <!-- 顶栏标题与控制轴 -->
      <header class="notes-sidebar-header">
        <div class="header-title-group">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
          <span>文献层级笔记</span>
          <span class="notes-count-badge">{{ totalNotesCount }}</span>
        </div>

        <div class="header-actions">
          <button
            class="icon-action-btn instant-tooltip"
            data-tip="添加顶级根目录"
            @click="openAddModal(null, 'folder')"
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>

          <button
            class="icon-action-btn instant-tooltip"
            data-tip="导出 Markdown 笔记"
            @click="exportNotesMarkdown"
          >
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>

          <button
            class="icon-action-btn collapse-toggle-btn instant-tooltip"
            data-tip="收起层级笔记栏"
            @click="$emit('toggle-collapse')"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
        </div>
      </header>
      <!-- 快速快捷按钮：添加子笔记、引用选中文本 -->
      <div class="notes-quick-toolbar">
        <button class="quick-btn primary" @click="openAddModal(selectedNodeId || null, 'note')">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          <span>新建节点</span>
        </button>
        <button class="quick-btn outline" @click="addExcerptFromSelection">
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
          <span>摘录划词</span>
        </button>
      </div>

      <!-- 层级树列表区域 -->
      <div class="notes-tree-container">
        <!-- 本文划词高亮自动汇总节点 -->
        <div v-if="annotations && annotations.length" class="auto-annotations-section">
          <div class="auto-section-head" @click="autoAnnotationsExpanded = !autoAnnotationsExpanded">
            <span class="tree-arrow" :class="{ open: autoAnnotationsExpanded }">▶</span>
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" style="color: #6366f1;"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
            <span class="tree-label">本文划词与高亮标记 ({{ annotations.length }})</span>
          </div>

          <div v-if="autoAnnotationsExpanded" class="auto-section-list">
            <div
              v-for="anno in annotations"
              :key="anno.id"
              class="tree-node-item excerpt-node"
              @click="handleJumpToAnnotation(anno)"
            >
              <div class="node-content">
                <span class="node-icon">
                  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
                </span>
                <div class="node-text-wrap">
                  <span class="node-title">{{ anno.text || anno.quoteText || "划词标注" }}</span>
                  <small v-if="anno.note" class="node-subnote">批注: {{ anno.note }}</small>
                  <small class="node-meta">页码 P.{{ anno.page || 1 }} · {{ anno.type === 'highlight' ? '高亮' : '划线' }}</small>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="tree-divider"></div>

        <!-- 用户层级自定义结构树 -->
        <div v-if="filteredNotesTree.length" class="custom-tree-root">
          <HierarchicalTreeNode
            v-for="node in filteredNotesTree"
            :key="node.id"
            :node="node"
            :depth="0"
            :selected-node-id="selectedNodeId"
            @select="selectNode"
            @save="saveNotesToStorage"
            @toggle-expand="toggleNodeExpand"
            @add-child="openAddModal"
            @edit="openEditModal"
            @delete="deleteNode"
            @jump="handleJumpToNode"
          />
        </div>

        <div v-else-if="searchQuery" class="empty-tree-state">
          <p>未找到包含 “{{ searchQuery }}” 的笔记</p>
        </div>

        <div v-else class="empty-tree-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.8" style="color: #94a3b8;"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
          </div>
          <p>暂无自定义结构化笔记</p>
          <small>点击上方 “新建节点” 或 “摘录划词” 开始组织层级大纲</small>
        </div>
      </div>

    </template>

    <!-- 新增 / 编辑 节点 Popover Modal -->
    <Teleport to="body">
      <div v-if="modal.open" class="notes-modal-backdrop" @click="closeModal">
        <div class="notes-modal-card" :class="`modal-type-${modal.type}`" @click.stop>
          <header class="modal-head">
            <h3>{{ modal.isEdit ? '编辑笔记节点' : '新增笔记节点' }}</h3>
            <button class="modal-close" @click="closeModal">✕</button>
          </header>

          <div class="modal-form">
            <div class="form-group">
              <label>节点类型</label>
              <div class="type-segmented">
                <button :class="{ active: modal.type === 'folder' }" @click="modal.type = 'folder'">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px;"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>
                  <span>目录 / 分类</span>
                </button>
                <button :class="{ active: modal.type === 'note' }" @click="modal.type = 'note'">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px;"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                  <span>笔记节点</span>
                </button>
                <button :class="{ active: modal.type === 'excerpt' }" @click="modal.type = 'excerpt'">
                  <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px;"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
                  <span>摘录引用</span>
                </button>
              </div>
            </div>

            <div class="form-group">
              <label>节点标题</label>
              <input v-model="modal.title" type="text" placeholder="例如：1. 研究方法论 / 关键对比" />
            </div>

            <div v-if="modal.type !== 'folder'" class="form-group">
              <label>笔记细节 (Markdown)</label>
              <textarea v-model="modal.content" rows="4" placeholder="在此输入笔记核心内容、思考与延伸总结…"></textarea>
            </div>

            <div v-if="modal.type === 'excerpt'" class="form-group">
              <label>摘录原文引文</label>
              <textarea v-model="modal.quoteText" rows="2" placeholder="引用的论文原文句段…"></textarea>
            </div>
          </div>

          <footer class="modal-foot">
            <button class="modal-btn secondary" @click="closeModal">取消</button>
            <button class="modal-btn primary" @click="submitModal">保存</button>
          </footer>
        </div>
      </div>
    </Teleport>
  </aside>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import HierarchicalTreeNode from "./HierarchicalTreeNode.vue";
import { useDialogStore } from "../stores/dialog";

const props = defineProps({
  paperId: { type: String, default: "" },
  paperTitle: { type: String, default: "" },
  paperNote: { type: String, default: "" },
  annotations: { type: Array, default: () => [] },
  isCollapsed: { type: Boolean, default: false }
});

const emit = defineEmits(["toggle-collapse", "jump-to-page", "jump-to-annotation", "show-toast", "sync-note"]);
const dialogStore = useDialogStore();

const searchQuery = ref("");
const selectedNodeId = ref(null);
const autoAnnotationsExpanded = ref(true);

const modal = reactive({
  open: false,
  isEdit: false,
  targetNodeId: null,
  parentId: null,
  type: "note",
  title: "",
  content: "",
  quoteText: "",
  page: 1
});

function createDefaultNotesTemplate() {
  return [
    {
      id: "template-background",
      title: "1. 研究背景与问题",
      type: "folder",
      expanded: true,
      children: [
        {
          id: "template-background-gap",
          title: "研究背景 / 领域现状",
          type: "note",
          content: "记录论文所在领域、已有方法、关键概念与作者关注的问题。",
          expanded: false,
          children: []
        },
        {
          id: "template-background-question",
          title: "核心问题 / 研究目标",
          type: "note",
          content: "概括论文真正要解决的科学问题、任务定义或假设。",
          expanded: false,
          children: []
        }
      ]
    },
    {
      id: "template-method",
      title: "2. 方法设计与实验设置",
      type: "folder",
      expanded: true,
      children: [
        {
          id: "template-method-main",
          title: "方法框架 / 模型设计",
          type: "note",
          content: "梳理论文方法的输入、关键模块、训练或推理流程。",
          expanded: false,
          children: []
        },
        {
          id: "template-method-data",
          title: "数据集 / 对照组 / 指标",
          type: "note",
          content: "记录数据来源、实验分组、评价指标和消融设置。",
          expanded: false,
          children: []
        }
      ]
    },
    {
      id: "template-results",
      title: "3. 关键结果与证据",
      type: "folder",
      expanded: true,
      children: [
        {
          id: "template-results-finding",
          title: "主要发现",
          type: "note",
          content: "记录最能支撑论文结论的实验结果、图表或统计证据。",
          expanded: false,
          children: []
        },
        {
          id: "template-results-comparison",
          title: "对比与消融",
          type: "note",
          content: "整理相对基线方法的提升、失败案例和消融结论。",
          expanded: false,
          children: []
        }
      ]
    },
    {
      id: "template-value",
      title: "4. 创新价值与局限",
      type: "folder",
      expanded: true,
      children: [
        {
          id: "template-value-contribution",
          title: "核心贡献",
          type: "note",
          content: "总结论文相对已有工作的新增价值、适用场景和启发。",
          expanded: false,
          children: []
        },
        {
          id: "template-value-limit",
          title: "局限与后续问题",
          type: "note",
          content: "记录数据、方法、实验或结论外推上的限制，以及可延伸研究方向。",
          expanded: false,
          children: []
        }
      ]
    }
  ];
}

const notesTree = ref(createDefaultNotesTemplate());

// 加载持久化笔记数据
const storageKey = computed(() => `paperpilot_hierarchical_notes_${props.paperId || 'default'}`);
const noteMirrorKey = computed(() => `${storageKey.value}_markdown_mirror`);

function loadNotesFromStorage() {
  try {
    const raw = localStorage.getItem(storageKey.value);
    const externalNote = String(props.paperNote || "").trim();
    const mirroredNote = localStorage.getItem(noteMirrorKey.value) || "";
    if (!externalNote && mirroredNote) {
      notesTree.value = createDefaultNotesTemplate();
      saveNotesToStorage({ sync: false });
      localStorage.removeItem(noteMirrorKey.value);
      return;
    }
    if (externalNote && externalNote !== mirroredNote) {
      notesTree.value = createTemplateFromPaperNote(externalNote);
      saveNotesToStorage({ sync: false });
      localStorage.setItem(noteMirrorKey.value, externalNote);
      return;
    }
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length) {
        notesTree.value = isLegacySampleNotes(parsed) ? createDefaultNotesTemplate() : parsed;
        if (isLegacySampleNotes(parsed)) saveNotesToStorage({ sync: false });
        return;
      }
    }
    notesTree.value = props.paperNote?.trim()
      ? createTemplateFromPaperNote(props.paperNote)
      : createDefaultNotesTemplate();
    saveNotesToStorage({ sync: false });
    if (props.paperNote?.trim()) localStorage.setItem(noteMirrorKey.value, props.paperNote.trim());
  } catch (e) {
    console.warn("Failed to load hierarchical notes", e);
    notesTree.value = createDefaultNotesTemplate();
  }
}

function isLegacySampleNotes(list) {
  const text = JSON.stringify(list || []);
  return text.includes("土耳其法律小语言模型") || text.includes("SLM 微调成本优势") || text.includes("LightEval 框架");
}

function saveNotesToStorage(options = {}) {
  const { sync = true } = options;
  try {
    localStorage.setItem(storageKey.value, JSON.stringify(notesTree.value));
    const markdown = renderNotesMarkdown();
    localStorage.setItem(noteMirrorKey.value, markdown);
    if (sync) emit("sync-note", markdown);
  } catch (e) {
    console.warn("Failed to save hierarchical notes", e);
  }
}

function createTemplateFromPaperNote(note) {
  const template = createDefaultNotesTemplate();
  template.unshift({
    id: "library-note-synced",
    title: "文献库笔记",
    type: "note",
    content: String(note || "").trim(),
    expanded: true,
    children: []
  });
  return template;
}

watch(() => props.paperId, () => {
  loadNotesFromStorage();
}, { immediate: true });

watch(() => props.paperNote, () => {
  loadNotesFromStorage();
});

// 统计总节点数
function countNodes(list) {
  let count = 0;
  for (const item of list) {
    count++;
    if (item.children && item.children.length) {
      count += countNodes(item.children);
    }
  }
  return count;
}

const totalNotesCount = computed(() => countNodes(notesTree.value));

// 过滤后的层级树
function filterTree(list, query) {
  if (!query) return list;
  const q = query.toLowerCase();
  return list.reduce((acc, item) => {
    const titleMatch = item.title?.toLowerCase().includes(q);
    const contentMatch = item.content?.toLowerCase().includes(q);
    const quoteMatch = item.quoteText?.toLowerCase().includes(q);
    const filteredChildren = item.children ? filterTree(item.children, query) : [];

    if (titleMatch || contentMatch || quoteMatch || filteredChildren.length) {
      acc.push({
        ...item,
        expanded: true,
        children: filteredChildren
      });
    }
    return acc;
  }, []);
}

const filteredNotesTree = computed(() => filterTree(notesTree.value, searchQuery.value.trim()));

// 查找节点
function findNodeById(list, id) {
  for (const item of list) {
    if (item.id === id) return item;
    if (item.children) {
      const found = findNodeById(item.children, id);
      if (found) return found;
    }
  }
  return null;
}

const activeNode = computed(() => {
  if (!selectedNodeId.value) return null;
  return findNodeById(notesTree.value, selectedNodeId.value);
});

function selectNode(node) {
  selectedNodeId.value = node.id;
}

function toggleNodeExpand(node) {
  node.expanded = !node.expanded;
  saveNotesToStorage();
}

function getNodeTypeLabel(type) {
  return {
    folder: "目录",
    note: "笔记",
    excerpt: "原文摘录"
  }[type] || "节点";
}

// 弹窗操作
function openAddModal(parentId = null, defaultType = "note") {
  modal.open = true;
  modal.isEdit = false;
  modal.parentId = parentId;
  modal.type = defaultType;
  modal.title = "";
  modal.content = "";
  modal.quoteText = "";
  modal.page = 1;
}

function openEditModal(node) {
  modal.open = true;
  modal.isEdit = true;
  modal.targetNodeId = node.id;
  modal.type = node.type || "note";
  modal.title = node.title || "";
  modal.content = node.content || "";
  modal.quoteText = node.quoteText || "";
  modal.page = node.page || 1;
}

function closeModal() {
  modal.open = false;
}

function insertNode(list, parentId, newNode) {
  if (!parentId) {
    list.push(newNode);
    return true;
  }
  for (const item of list) {
    if (item.id === parentId) {
      item.children = item.children || [];
      item.children.push(newNode);
      item.expanded = true;
      return true;
    }
    if (item.children && insertNode(item.children, parentId, newNode)) {
      return true;
    }
  }
  return false;
}

function submitModal() {
  if (!modal.title.trim()) return;

  if (modal.isEdit) {
    const target = findNodeById(notesTree.value, modal.targetNodeId);
    if (target) {
      target.type = modal.type;
      target.title = modal.title.trim();
      target.content = modal.content.trim();
      target.quoteText = modal.quoteText.trim();
    }
  } else {
    const newNode = {
      id: `node-${Date.now()}`,
      title: modal.title.trim(),
      type: modal.type,
      content: modal.content.trim(),
      quoteText: modal.quoteText.trim(),
      page: modal.page || 1,
      expanded: true,
      children: []
    };
    insertNode(notesTree.value, modal.parentId, newNode);
    selectedNodeId.value = newNode.id;
  }

  saveNotesToStorage();
  closeModal();
  emit("show-toast", modal.isEdit ? "层级笔记已更新" : "新建层级节点成功");
}

function deleteNodeFromList(list, id) {
  const idx = list.findIndex(item => item.id === id);
  if (idx !== -1) {
    list.splice(idx, 1);
    return true;
  }
  for (const item of list) {
    if (item.children && deleteNodeFromList(item.children, id)) {
      return true;
    }
  }
  return false;
}

async function deleteNode(node) {
  const ok = await dialogStore.confirm(`确定删除笔记节点“${node.title}”及其子级项吗？`, {
    title: "删除笔记节点",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  deleteNodeFromList(notesTree.value, node.id);
  if (selectedNodeId.value === node.id) selectedNodeId.value = null;
  saveNotesToStorage();
  emit("show-toast", "节点已删除");
}

// 划词快速摘录为层级节点
function addExcerptFromSelection() {
  const sel = window.getSelection()?.toString()?.trim();
  if (!sel) {
    emit("show-toast", "请先在论文正文中划词选中需要摘录的语句");
    return;
  }
  const newNode = {
    id: `node-excerpt-${Date.now()}`,
    title: `摘录: ${sel.slice(0, 20)}…`,
    type: "excerpt",
    content: "",
    quoteText: sel,
    page: 1,
    expanded: true,
    children: []
  };
  notesTree.value.push(newNode);
  selectedNodeId.value = newNode.id;
  saveNotesToStorage();
  emit("show-toast", "已将所选文本添加为层级摘录节点");
}

// 跳转到论文页面/批注
function handleJumpToNode(node) {
  if (node.page) {
    emit("jump-to-page", node.page);
  }
}

function handleJumpToAnnotation(anno) {
  emit("jump-to-annotation", anno);
}

function renderMarkdownNode(node, depth = 0) {
  const indent = "  ".repeat(depth);
  let md = "";
  if (node.type === "folder") {
    md += `${indent}#`.repeat(Math.min(depth + 2, 6)) + ` ${node.title}\n\n`;
  } else if (node.type === "excerpt") {
    md += `${indent}- **${node.title}** (P.${node.page || 1})\n`;
    if (node.quoteText) md += `${indent}  > “${node.quoteText}”\n`;
    if (node.content) md += `${indent}  ${node.content}\n`;
    md += "\n";
  } else {
    md += `${indent}- **${node.title}**\n`;
    if (node.content) md += `${indent}  ${node.content}\n`;
    md += "\n";
  }

  if (node.children && node.children.length) {
    for (const child of node.children) {
      md += renderMarkdownNode(child, depth + 1);
    }
  }
  return md;
}

function renderNotesMarkdown() {
  let text = `# ${props.paperTitle || '文献层级笔记大纲'}\n\n`;
  for (const rootNode of notesTree.value) {
    text += renderMarkdownNode(rootNode, 0);
  }
  return text.trim();
}

// 导出为 Markdown
function exportNotesMarkdown() {
  const text = `${renderNotesMarkdown()}\n`;
  const blob = new Blob([text], { type: "text/markdown;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${props.paperTitle || 'Literature'}_Notes_Tree.md`;
  a.click();
  URL.revokeObjectURL(url);
  emit("show-toast", "已成功导出 Markdown 层级笔记大纲");
}

onMounted(() => {
  loadNotesFromStorage();
});
</script>

<style scoped>
.hierarchical-notes-sidebar {
  --notes-bg: #ffffff;
  --notes-strip-bg: #f8fafc;
  --notes-header-bg: #f8fafc;
  --notes-border: #e2e8f0;
  --notes-text: #0f172a;
  --notes-subtext: #475569;
  --notes-muted: #94a3b8;
  --notes-card-bg: #ffffff;
  --notes-input-bg: #f1f5f9;
  --notes-input-border: #cbd5e1;
  --notes-input-text: #0f172a;
  --notes-hover: rgba(99, 102, 241, 0.08);

  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--notes-bg);
  backdrop-filter: blur(20px);
  border-left: 1px solid var(--notes-border);
  box-shadow: -4px 0 20px rgba(15, 23, 42, 0.03);
  box-sizing: border-box;
  overflow: hidden;
  transition: width 200ms cubic-bezier(.22, 1, .36, 1);
}

:global(:root[data-theme="dark"]) .hierarchical-notes-sidebar {
  --notes-bg: linear-gradient(180deg, rgba(16, 23, 37, 0.98), rgba(7, 10, 18, 0.99));
  --notes-strip-bg: rgba(11, 16, 28, 0.98);
  --notes-header-bg: linear-gradient(135deg, rgba(49, 46, 129, 0.2), rgba(8, 145, 178, 0.12));
  --notes-border: rgba(148, 163, 184, 0.22);
  --notes-text: #f3f7ff;
  --notes-subtext: #cbd5e1;
  --notes-muted: #8795aa;
  --notes-card-bg: rgba(20, 27, 43, 0.86);
  --notes-input-bg: rgba(15, 23, 42, 0.8);
  --notes-input-border: rgba(148, 163, 184, 0.26);
  --notes-input-text: #f3f7ff;
  --notes-hover: rgba(34, 211, 238, 0.11);
  box-shadow: -14px 0 42px rgba(4, 2, 12, 0.46), inset 1px 0 0 rgba(255, 255, 255, 0.05);
}

.hierarchical-notes-sidebar.collapsed {
  width: 44px;
}

.collapsed-strip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  height: 100%;
  padding-top: 12px;
  padding-bottom: 80px;
  cursor: pointer;
  background: var(--notes-strip-bg);
  transition: background 0.15s ease;
}

.collapsed-strip:hover {
  background: var(--notes-hover);
}

:global(:root[data-theme="dark"]) .collapsed-strip {
  border-right: 1px solid rgba(196, 181, 253, 0.12);
}

.expand-edge-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--notes-border);
  border-radius: 6px;
  background: var(--notes-card-bg);
  color: #8b5cf6;
  cursor: pointer;
  margin-bottom: 18px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
}

.vertical-title-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--notes-subtext);
}

.vertical-text {
  writing-mode: vertical-rl;
  letter-spacing: 0.15em;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--notes-text);
}

.collapsed-count-pill {
  margin-top: 12px;
  font-size: 10.5px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(139, 92, 246, 0.24);
  color: #c4b5fd;
}

.notes-sidebar-header {
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  gap: 8px;
  border-bottom: 1px solid var(--notes-border);
  background: var(--notes-header-bg);
  flex-shrink: 0;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--notes-text);
  white-space: nowrap;
}

.header-title-group > span:first-of-type {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notes-count-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(139, 92, 246, 0.22);
  color: #8b5cf6;
}

:global(:root[data-theme="dark"]) .collapsed-count-pill,
:global(:root[data-theme="dark"]) .notes-count-badge,
:global(:root[data-theme="dark"]) .detail-type-badge {
  background: rgba(168, 85, 247, 0.2);
  color: #e9d5ff;
  border: 1px solid rgba(216, 180, 254, 0.18);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}

.icon-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--notes-subtext);
  cursor: pointer;
  transition: all 0.15s ease;
}

.icon-action-btn svg {
  width: 16px;
  height: 16px;
}

.icon-action-btn:hover {
  background: var(--notes-hover);
  color: #8b5cf6;
}

.notes-search-bar {
  position: relative;
  display: flex;
  align-items: center;
  width: auto;
  margin: 10px 14px 6px;
  padding: 0 10px;
  height: 32px;
  border-radius: 8px;
  background: var(--notes-input-bg);
  border: 1px solid var(--notes-input-border);
  color: var(--notes-muted);
  box-sizing: border-box;
  min-width: 0;
  max-width: none;
}

:global(:root[data-theme="dark"]) .notes-search-bar {
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.notes-search-bar input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 12px;
  color: var(--notes-input-text);
  outline: none;
  padding-left: 6px;
}

.clear-search-btn {
  border: none;
  background: transparent;
  color: var(--notes-muted);
  cursor: pointer;
  font-size: 11px;
}

.notes-quick-toolbar {
  display: flex;
  gap: 8px;
  padding: 4px 12px 8px;
  border-bottom: 1px solid var(--notes-border);
}

.quick-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 28px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.quick-btn.primary {
  border: none;
  background: linear-gradient(135deg, #4f46e5, #0891b2);
  color: #ffffff;
}

.quick-btn.primary:hover {
  background: linear-gradient(135deg, #6366f1, #06b6d4);
  box-shadow: 0 6px 16px rgba(6, 182, 212, 0.22);
}

.quick-btn.outline {
  border: 1px solid var(--notes-input-border);
  background: var(--notes-card-bg);
  color: var(--notes-text);
}

.quick-btn.outline:hover {
  border-color: #8b5cf6;
  color: #8b5cf6;
}

:global(:root[data-theme="dark"]) .quick-btn.outline {
  background: rgba(15, 23, 42, 0.72);
  border-color: rgba(148, 163, 184, 0.28);
  color: #cbd5e1;
}

:global(:root[data-theme="dark"]) .quick-btn.outline:hover {
  background: rgba(8, 145, 178, 0.14);
  border-color: rgba(34, 211, 238, 0.34);
  color: #e0faff;
}

.notes-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px;
}

.auto-annotations-section {
  margin-bottom: 8px;
}

.auto-section-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--notes-input-bg);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
  color: var(--notes-text);
}

.tree-arrow {
  font-size: 9px;
  color: var(--notes-muted);
  transition: transform 0.15s ease;
}

.tree-arrow.open {
  transform: rotate(90deg);
}

.auto-section-list {
  margin-top: 4px;
  padding-left: 14px;
}

.tree-node-item {
  display: flex;
  align-items: flex-start;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background 0.15s ease;
}

.tree-node-item:hover {
  background: var(--notes-hover);
}

:global(:root[data-theme="dark"]) .tree-node-item {
  border: 1px solid transparent;
}

:global(:root[data-theme="dark"]) .tree-node-item:hover {
  border-color: rgba(196, 181, 253, 0.18);
  background: rgba(139, 92, 246, 0.14);
}

.node-content {
  display: flex;
  gap: 6px;
}

.node-text-wrap {
  display: flex;
  flex-direction: column;
}

.node-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--notes-text);
  line-height: 1.3;
}

.node-subnote {
  font-size: 11px;
  color: var(--notes-subtext);
  margin-top: 2px;
}

.node-meta {
  font-size: 10px;
  color: var(--notes-muted);
  margin-top: 2px;
}

.tree-divider {
  height: 1px;
  background: var(--notes-border);
  margin: 8px 0;
}

.empty-tree-state {
  text-align: center;
  padding: 30px 16px;
  color: var(--notes-muted);
}

.node-detail-panel {
  margin: 8px 12px 6px;
  border: 1px solid var(--notes-border);
  border-radius: 10px;
  background: var(--notes-card-bg);
  padding: 10px;
  flex-shrink: 0;
  max-height: min(34vh, 300px);
  overflow-y: auto;
  box-sizing: border-box;
}

:global(:root[data-theme="dark"]) .node-detail-panel {
  border-color: rgba(148, 163, 184, 0.24);
  background:
    linear-gradient(180deg, rgba(20, 27, 43, 0.96), rgba(11, 16, 28, 0.98));
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.detail-type-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(139, 92, 246, 0.22);
  color: #8b5cf6;
}

.detail-title {
  flex: 1;
  font-size: 12px;
  font-weight: 700;
  color: var(--notes-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-close-btn {
  border: none;
  background: transparent;
  color: var(--notes-muted);
  cursor: pointer;
}

.detail-body textarea {
  width: 100%;
  border: 1px solid var(--notes-input-border);
  border-radius: 6px;
  padding: 6px 8px;
  min-height: 76px;
  font-size: 11.5px;
  color: var(--notes-input-text);
  background: var(--notes-input-bg);
  outline: none;
  resize: vertical;
  box-sizing: border-box;
}

.detail-quote-box {
  margin-top: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--notes-input-bg);
  border-left: 3px solid #8b5cf6;
  max-height: 110px;
  overflow-y: auto;
}

.detail-quote-box small {
  font-size: 10px;
  color: var(--notes-muted);
}

.detail-quote-box blockquote {
  margin: 4px 0;
  font-size: 11px;
  color: var(--notes-subtext);
  font-style: italic;
}

.jump-quote-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #8b5cf6;
  font-size: 10.5px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-top: 4px;
}

/* 弹窗适配 */
.notes-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

:global(:root[data-theme="dark"]) .notes-modal-backdrop {
  background: rgba(5, 3, 12, 0.58);
}

.notes-modal-card {
  width: 420px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.25);
  padding: 18px;
  box-sizing: border-box;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.modal-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.modal-close {
  border: none;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}

.form-group {
  margin-bottom: 12px;
}

.form-group label {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 4px;
}

.type-segmented {
  display: flex;
  gap: 6px;
}

.type-segmented button {
  flex: 1;
  padding: 6px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}

.type-segmented button.active {
  background: #4f46e5;
  color: #ffffff;
  border-color: #4f46e5;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 7px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 12px;
  color: #0f172a;
  outline: none;
  box-sizing: border-box;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.modal-btn {
  padding: 7px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.modal-btn.secondary {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #475569;
}

.modal-btn.primary {
  border: none;
  background: #4f46e5;
  color: #ffffff;
}

:global(:root[data-theme="dark"]) .notes-modal-card {
  background: linear-gradient(180deg, rgba(30, 24, 52, 0.98), rgba(15, 12, 28, 0.98));
  border: 1px solid rgba(167, 139, 250, 0.22);
  color: #f6f3ff;
  box-shadow: 0 24px 70px rgba(4, 2, 12, 0.58);
}

:global(:root[data-theme="dark"]) .modal-head h3,
:global(:root[data-theme="dark"]) .form-group label {
  color: #f6f3ff;
}

:global(:root[data-theme="dark"]) .form-group input[type="text"],
:global(:root[data-theme="dark"]) .form-group textarea {
  background: rgba(16, 13, 30, 0.9);
  border-color: rgba(167, 139, 250, 0.24);
  color: #f6f3ff;
}

:global(:root[data-theme="dark"]) .type-segmented button {
  background: rgba(16, 13, 30, 0.88);
  border-color: rgba(167, 139, 250, 0.22);
  color: #b7a8df;
}

:global(:root[data-theme="dark"]) .type-segmented button.active {
  background: linear-gradient(135deg, #7c3aed, #a855f7);
  color: #ffffff;
  border-color: #a78bfa;
}

:global(:root[data-theme="dark"]) .modal-btn.secondary {
  border-color: rgba(167, 139, 250, 0.22);
  background: rgba(16, 13, 30, 0.88);
  color: #d8ccff;
}

:global(:root[data-theme="dark"]) .modal-btn.primary {
  background: linear-gradient(135deg, #7c3aed, #a855f7);
  box-shadow: 0 8px 18px rgba(124, 58, 237, 0.28);
}

:global(:root[data-theme="dark"]) .notes-search-bar:focus-within,
:global(:root[data-theme="dark"]) .detail-body textarea:focus {
  border-color: rgba(192, 132, 252, 0.52);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.16);
}

:global(:root[data-theme="dark"]) .auto-section-head,
:global(:root[data-theme="dark"]) .detail-quote-box {
  background: rgba(139, 92, 246, 0.12);
}

:global(:root[data-theme="dark"]) .auto-section-head {
  border: 1px solid rgba(196, 181, 253, 0.14);
}

:global(:root[data-theme="dark"]) .detail-quote-box {
  border-left-color: #c084fc;
}

:global(:root[data-theme="dark"]) .node-detail-panel {
  box-shadow: 0 -12px 28px rgba(4, 2, 12, 0.22);
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar) {
  --notes-bg: linear-gradient(180deg, rgba(16, 23, 37, 0.98), rgba(7, 10, 18, 0.99));
  --notes-strip-bg: rgba(11, 16, 28, 0.98);
  --notes-header-bg: linear-gradient(135deg, rgba(49, 46, 129, 0.2), rgba(8, 145, 178, 0.12));
  --notes-border: rgba(148, 163, 184, 0.22);
  --notes-text: #f3f7ff;
  --notes-subtext: #cbd5e1;
  --notes-muted: #8795aa;
  --notes-card-bg: rgba(20, 27, 43, 0.86);
  --notes-input-bg: rgba(15, 23, 42, 0.8);
  --notes-input-border: rgba(148, 163, 184, 0.26);
  --notes-input-text: #f3f7ff;
  --notes-hover: rgba(34, 211, 238, 0.11);
  background: var(--notes-bg) !important;
  border-left-color: var(--notes-border) !important;
  box-shadow: -14px 0 42px rgba(4, 2, 12, 0.46), inset 1px 0 0 rgba(255, 255, 255, 0.05) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .notes-sidebar-header) {
  background: var(--notes-header-bg) !important;
  border-bottom-color: var(--notes-border) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .notes-search-bar),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .quick-btn.outline),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .auto-section-head),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .node-detail-panel),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-body textarea),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-quote-box) {
  background: var(--notes-input-bg) !important;
  border-color: var(--notes-input-border) !important;
  color: var(--notes-text) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .notes-tree-container) {
  background: transparent !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .node-detail-panel) {
  background: linear-gradient(180deg, rgba(20, 27, 43, 0.96), rgba(11, 16, 28, 0.98)) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .header-title-group),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .node-title),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-title),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .empty-tree-state p) {
  color: var(--notes-text) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .node-subnote),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-quote-box blockquote),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .quick-btn.outline) {
  color: var(--notes-subtext) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .node-meta),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .empty-tree-state small),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-quote-box small),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .notes-search-bar input::placeholder) {
  color: var(--notes-muted) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .notes-search-bar input),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-body textarea) {
  color: var(--notes-input-text) !important;
}

:global(html[data-theme="dark"] .hierarchical-notes-sidebar .collapsed-count-pill),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .notes-count-badge),
:global(html[data-theme="dark"] .hierarchical-notes-sidebar .detail-type-badge) {
  background: rgba(8, 145, 178, 0.18) !important;
  color: #a5f3fc !important;
  border: 1px solid rgba(34, 211, 238, 0.22) !important;
}

:global(html[data-theme="dark"] .notes-modal-card) {
  background:
    linear-gradient(180deg, rgba(20, 27, 43, 0.98), rgba(10, 14, 24, 0.99)) !important;
  border: 1px solid rgba(148, 163, 184, 0.24) !important;
  color: #edf2ff !important;
  box-shadow: 0 26px 74px rgba(2, 6, 23, 0.62), inset 0 1px 0 rgba(255, 255, 255, 0.04) !important;
}

:global(html[data-theme="dark"] .notes-modal-card.modal-type-folder) {
  border-top: 3px solid #8b5cf6 !important;
}

:global(html[data-theme="dark"] .notes-modal-card.modal-type-note) {
  border-top: 3px solid #22d3ee !important;
}

:global(html[data-theme="dark"] .notes-modal-card.modal-type-excerpt) {
  border-top: 3px solid #f59e0b !important;
}

:global(html[data-theme="dark"] .notes-modal-card .modal-head h3) {
  color: #f8fbff !important;
}

:global(html[data-theme="dark"] .notes-modal-card .modal-close) {
  color: #9fb0cc !important;
}

:global(html[data-theme="dark"] .notes-modal-card .modal-close:hover) {
  color: #f8fbff !important;
}

:global(html[data-theme="dark"] .notes-modal-card .form-group label) {
  color: #b8c5dc !important;
}

:global(html[data-theme="dark"] .notes-modal-card .type-segmented button) {
  background: rgba(15, 23, 42, 0.78) !important;
  border-color: rgba(148, 163, 184, 0.32) !important;
  color: #cbd5e1 !important;
}

:global(html[data-theme="dark"] .notes-modal-card .type-segmented button:hover) {
  background: rgba(30, 41, 59, 0.92) !important;
  border-color: rgba(125, 211, 252, 0.34) !important;
  color: #f8fbff !important;
}

:global(html[data-theme="dark"] .notes-modal-card.modal-type-folder .type-segmented button.active) {
  background: linear-gradient(135deg, #6d5dfc, #8b5cf6) !important;
  border-color: rgba(196, 181, 253, 0.75) !important;
  color: #ffffff !important;
}

:global(html[data-theme="dark"] .notes-modal-card.modal-type-note .type-segmented button.active) {
  background: linear-gradient(135deg, #0891b2, #22d3ee) !important;
  border-color: rgba(103, 232, 249, 0.7) !important;
  color: #ecfeff !important;
}

:global(html[data-theme="dark"] .notes-modal-card.modal-type-excerpt .type-segmented button.active) {
  background: linear-gradient(135deg, #b45309, #f59e0b) !important;
  border-color: rgba(251, 191, 36, 0.7) !important;
  color: #fff7ed !important;
}

:global(html[data-theme="dark"] .notes-modal-card .form-group input[type="text"]),
:global(html[data-theme="dark"] .notes-modal-card .form-group textarea) {
  background: rgba(15, 23, 42, 0.88) !important;
  border-color: rgba(148, 163, 184, 0.32) !important;
  color: #edf2ff !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03) !important;
}

:global(html[data-theme="dark"] .notes-modal-card .form-group input[type="text"]::placeholder),
:global(html[data-theme="dark"] .notes-modal-card .form-group textarea::placeholder) {
  color: #7e8ba3 !important;
}

:global(html[data-theme="dark"] .notes-modal-card .form-group input[type="text"]:focus),
:global(html[data-theme="dark"] .notes-modal-card .form-group textarea:focus) {
  border-color: rgba(34, 211, 238, 0.5) !important;
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.12) !important;
}

:global(html[data-theme="dark"] .notes-modal-card .modal-btn.secondary) {
  background: rgba(15, 23, 42, 0.72) !important;
  border-color: rgba(148, 163, 184, 0.36) !important;
  color: #cbd5e1 !important;
}

:global(html[data-theme="dark"] .notes-modal-card .modal-btn.secondary:hover) {
  background: rgba(30, 41, 59, 0.92) !important;
  color: #f8fbff !important;
}

:global(html[data-theme="dark"] .notes-modal-card .modal-btn.primary) {
  background: linear-gradient(135deg, #6d5dfc, #22d3ee) !important;
  color: #ffffff !important;
  box-shadow: 0 10px 22px rgba(34, 211, 238, 0.22) !important;
}

.instant-tooltip {
  position: relative;
}

.instant-tooltip::after {
  content: attr(data-tip);
  position: fixed;
  z-index: 2000;
  top: var(--tooltip-y, 44px);
  right: 14px;
  max-width: 180px;
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
