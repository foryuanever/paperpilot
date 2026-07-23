<template>
  <div class="thesis-page">
    <header class="thesis-head">
      <div class="thesis-title-block">
        <div class="title-row">
          <input v-model="thesis.title" class="title-input" aria-label="论文题目" @input="queueSave" />
          <button class="star-button" :class="{ active: thesis.starred }" title="标记为重点论文" @click="thesis.starred = !thesis.starred">
            {{ thesis.starred ? "已重点" : "设为重点" }}
          </button>
        </div>
        <div class="thesis-meta">
          <span class="status-dot"></span>
          <span>{{ thesis.stage }}</span>
          <span>第 {{ thesis.version }} 版</span>
          <span class="save-state" :class="saveState">{{ saveLabel }}</span>
          <span>全文 {{ formatNumber(totalWords) }} 字</span>
          <span>距离计划提交 {{ thesis.daysLeft }} 天</span>
        </div>
      </div>

      <div class="thesis-actions">
        <button class="action-button" @click="openCreate">新建</button>
        <label class="action-button file-action">
          上传
          <input type="file" accept=".doc,.docx,.md,.txt,.pdf" @change="handleUpload($event, false)" />
        </label>
        <label class="action-button file-action">
          覆盖更新
          <input type="file" accept=".doc,.docx,.md,.txt" @change="handleUpload($event, true)" />
        </label>
        <button class="action-button" @click="activeWorkspace = activeWorkspace === 'writing' ? 'versions' : 'writing'">
          {{ activeWorkspace === "writing" ? "版本记录" : "返回写作" }}
        </button>
        <button class="action-button primary" :class="{ success: submitted }" @click="submitAdvisor">
          {{ submitted ? "已提交导师" : "提交导师" }}
        </button>
        <button class="action-button primary quiet" @click="exportThesis">导出</button>
      </div>
    </header>

    <section v-if="activeWorkspace === 'writing'" class="writing-shell">
      <aside class="outline-pane">
        <div class="pane-head">
          <div>
            <strong>论文大纲</strong>
            <span>{{ completedChapters }}/{{ chapters.length }} 章已完成</span>
          </div>
          <button title="新建章节" @click="addChapter">＋</button>
        </div>

        <div class="thesis-progress">
          <div class="progress-copy">
            <span>硕士学位论文</span>
            <strong>{{ Math.round((totalWords / thesis.goalWords) * 100) }}%</strong>
          </div>
          <div class="progress-track"><span :style="{ transform: `scaleX(${Math.min(1, totalWords / thesis.goalWords)})` }"></span></div>
          <small>{{ formatNumber(totalWords) }} / {{ formatNumber(thesis.goalWords) }} 字</small>
        </div>

        <nav class="chapter-list" aria-label="论文章节">
          <button
            v-for="chapter in chapters"
            :key="chapter.id"
            class="chapter-item"
            :class="{ active: chapter.id === activeChapterId }"
            @click="selectChapter(chapter.id)"
          >
            <span class="chapter-state" :class="chapter.status"></span>
            <span class="chapter-copy">
              <strong>{{ chapter.title }}</strong>
              <small>{{ chapter.label }}</small>
            </span>
            <span class="chapter-words">{{ formatNumber(chapter.words) }}</span>
          </button>
        </nav>

        <button class="outline-settings" @click="outlineCompact = !outlineCompact">
          {{ outlineCompact ? "展开大纲详情" : "收起大纲详情" }}
        </button>
      </aside>

      <main class="editor-pane">
        <div class="editor-commandbar">
          <div class="format-group">
            <button :class="{ active: textStyle === '正文' }" @click="textStyle = '正文'">正文</button>
            <button :class="{ active: textStyle === '标题' }" @click="textStyle = '标题'">标题</button>
            <button :class="{ active: textStyle === '引用' }" @click="textStyle = '引用'">引用</button>
          </div>
          <div class="format-group compact">
            <button title="加粗" @click="toggleMark('**')"><b>B</b></button>
            <button title="斜体" @click="toggleMark('*')"><i>I</i></button>
            <button title="插入引用" @click="insertCitation">[引]</button>
            <button title="插入批注" @click="insertComment">批注</button>
          </div>
          <div class="editor-tools">
            <button @click="rightTab = 'ai'">AI 辅助</button>
            <button @click="focusMode = !focusMode">{{ focusMode ? "退出专注" : "专注模式" }}</button>
          </div>
        </div>

        <section class="paper-editor" :class="{ focused: focusMode }">
          <h1 class="paper-document-title">{{ thesis.title }}</h1>
          <input v-model="activeChapter.title" class="chapter-title-input" aria-label="章节标题" @input="queueSave" />
          <textarea
            ref="editor"
            v-model="activeChapter.content"
            class="manuscript-editor"
            :class="`style-${textStyle}`"
            spellcheck="false"
            aria-label="论文正文"
            @input="onEditorInput"
          ></textarea>
          <div class="editor-foot">
            <span>当前章节 {{ formatNumber(activeChapter.words) }} 字</span>
            <span>引用 {{ activeChapter.citations }} 篇</span>
            <span>{{ unresolvedComments }} 条批注待处理</span>
          </div>
        </section>
      </main>

      <aside class="research-pane">
        <div class="research-tabs">
          <button :class="{ active: rightTab === 'citations' }" @click="rightTab = 'citations'">引用建议</button>
          <button :class="{ active: rightTab === 'ai' }" @click="rightTab = 'ai'">AI 辅助</button>
          <button :class="{ active: rightTab === 'comments' }" @click="rightTab = 'comments'">导师批注</button>
        </div>

        <div v-if="rightTab === 'citations'" class="research-content">
          <div class="section-line">
            <strong>相关文献推荐</strong>
            <button @click="citationRefresh += 1">刷新</button>
          </div>
          <article v-for="paper in citationPapers" :key="paper.title" class="citation-row">
            <label>
              <input v-model="paper.selected" type="checkbox" />
              <span></span>
            </label>
            <div>
              <strong>{{ paper.title }}</strong>
              <p>{{ paper.authors }}</p>
              <small>{{ paper.venue }} · 相关度 {{ paper.relevance }}%</small>
              <button @click="addCitation(paper)">{{ paper.added ? "已加入引用" : "加入引用" }}</button>
            </div>
          </article>
          <button class="full-list-button">查看全部文献（28）</button>
        </div>

        <div v-else-if="rightTab === 'ai'" class="research-content ai-panel">
          <div class="ai-context">
            <span>当前上下文</span>
            <strong>{{ activeChapter.title }}</strong>
            <p>AI 仅基于本章内容和已关联文献给出建议，不会直接覆盖正文。</p>
          </div>
          <button v-for="action in aiActions" :key="action" class="ai-action" @click="runAi(action)">
            <span>{{ action }}</span>
            <small>生成可插入的修改建议</small>
          </button>
          <div v-if="aiResult" class="ai-result">
            <strong>建议草稿</strong>
            <p>{{ aiResult }}</p>
            <button @click="applyAiResult">插入文末</button>
          </div>
        </div>

        <div v-else class="research-content">
          <div class="section-line">
            <strong>待处理批注</strong>
            <span>{{ unresolvedComments }} 条</span>
          </div>
          <article v-for="comment in comments" :key="comment.id" class="comment-row" :class="{ resolved: comment.resolved }">
            <header>
              <span class="comment-avatar">{{ comment.author.slice(0, 1) }}</span>
              <div>
                <strong>{{ comment.author }}</strong>
                <small>{{ comment.time }}</small>
              </div>
              <span>{{ comment.resolved ? "已解决" : "待处理" }}</span>
            </header>
            <p>{{ comment.content }}</p>
            <button @click="comment.resolved = !comment.resolved">
              {{ comment.resolved ? "重新打开" : "标记为已解决" }}
            </button>
          </article>
          <label class="comment-reply">
            <input v-model="reply" placeholder="回复导师意见…" @keyup.enter="sendReply" />
            <button @click="sendReply">发送</button>
          </label>
        </div>
      </aside>
    </section>

    <section v-else class="version-workspace">
      <header>
        <div>
          <span>论文版本库</span>
          <h2>每次覆盖更新和重要保存都会留下可恢复版本</h2>
        </div>
        <button class="action-button primary" @click="createSnapshot">保存当前版本</button>
      </header>
      <div class="version-layout">
        <div class="version-list">
          <button
            v-for="version in versions"
            :key="version.id"
            :class="{ active: selectedVersion.id === version.id }"
            @click="selectedVersionId = version.id"
          >
            <span class="version-node">{{ version.label }}</span>
            <span>
              <strong>{{ version.title }}</strong>
              <small>{{ version.time }} · {{ version.author }}</small>
            </span>
            <span class="version-delta">{{ version.delta }}</span>
          </button>
        </div>
        <article class="version-detail">
          <div class="version-detail-head">
            <div>
              <span>{{ selectedVersion.label }}</span>
              <h3>{{ selectedVersion.title }}</h3>
            </div>
            <button class="action-button" @click="restoreVersion(selectedVersion)">恢复此版本</button>
          </div>
          <p>{{ selectedVersion.summary }}</p>
          <div class="version-stats">
            <span><strong>{{ formatNumber(selectedVersion.words) }}</strong>全文字数</span>
            <span><strong>{{ selectedVersion.citations }}</strong>引用文献</span>
            <span><strong>{{ selectedVersion.comments }}</strong>未解决批注</span>
          </div>
          <div class="diff-block">
            <span class="diff-add">＋ 增加多阶段检索框架的消融实验说明。</span>
            <span class="diff-add">＋ 补充 RRF 重排策略的参数依据。</span>
            <span class="diff-remove">－ 删除与研究问题重复的背景描述。</span>
          </div>
        </article>
      </div>
    </section>

    <footer class="thesis-insights">
      <div class="insight-progress">
        <span class="ring" :style="{ '--progress': `${chapterProgress * 3.6}deg` }"><strong>{{ chapterProgress }}%</strong></span>
        <div><strong>章节完成度</strong><small>{{ completedChapters }} 章完成 · {{ activeChapters }} 章进行中</small></div>
      </div>
      <div class="insight-list">
        <span><strong>46</strong>已引用文献</span>
        <span><strong>3</strong>待核查引用</span>
        <span><strong>91%</strong>证据链完整度</span>
      </div>
      <div class="insight-list">
        <span><strong>{{ unresolvedComments }}</strong>未解决批注</span>
        <span><strong>{{ resolvedComments }}</strong>已解决批注</span>
        <span><strong>3</strong>待办事项</span>
      </div>
      <div class="mini-contribution">
        <div class="mini-head">
          <span>近 12 周写作与阅读贡献</span>
          <strong>本周阅读 17 篇 · 写作 8.6 千字</strong>
        </div>
        <div class="mini-grid">
          <span v-for="cell in contributionCells" :key="cell.id" :class="`level-${cell.level}`" :title="cell.title"></span>
        </div>
      </div>
    </footer>

    <Transition name="desk-dialog">
      <div v-if="createOpen" class="desk-overlay" @click.self="createOpen = false">
        <form class="desk-dialog" @submit.prevent="createThesis">
          <header><div><span>新建论文</span><h2>建立一个可持续管理的写作空间</h2></div><button type="button" @click="createOpen = false">关闭</button></header>
          <label>论文题目<input v-model="newThesis.title" required /></label>
          <div class="dialog-grid">
            <label>论文类型<select v-model="newThesis.type"><option>硕士学位论文</option><option>博士学位论文</option><option>期刊论文</option><option>会议论文</option></select></label>
            <label>计划字数<input v-model.number="newThesis.goalWords" type="number" min="1000" /></label>
          </div>
          <label>研究方向<input v-model="newThesis.direction" placeholder="例如：自然语言处理 / 科学智能" /></label>
          <footer><button type="button" @click="createOpen = false">取消</button><button class="primary" type="submit">创建写作空间</button></footer>
        </form>
      </div>
    </Transition>

    <Transition name="toast">
      <div v-if="toast" class="desk-toast">{{ toast }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from "vue";

const STORAGE_KEY = "papersolver-thesis-workspace-v1";
const initialContent = `为提升检索的召回率与精度，本文提出一个多阶段检索框架。该框架结合稀疏检索、向量检索与重排序策略，通过逐级过滤与语义对齐，最终向生成模块提供高质量的证据上下文。

如图 3-2 所示，整体流程分为查询理解、候选检索、精排重排与上下文组装四个阶段。在查询理解阶段，利用大语言模型对用户问题进行意图识别与查询重写，生成多个潜在子问题以覆盖不同表达方式，从而缓解用户提问的歧义性与稀疏性[12]。

候选检索阶段同时部署 BM25 稀疏检索与向量检索，两路检索结果通过倒排融合获得更大覆盖面的候选文档集合。在精排重排阶段，采用交叉编码器对候选文档与查询的相关性进行语义匹配。实验表明，该策略将 Top-50 的相关文档平均精度提升 18.7%[18]。

最后，在上下文组装阶段，依据领域分布与段落长度约束构建最终输入上下文，并附加文献元数据，以增强回答的可解释性与引用可追溯性。`;

const stored = localStorage.getItem(STORAGE_KEY);
const thesis = reactive(stored ? JSON.parse(stored).thesis : {
  title: "面向科学文献的检索增强生成方法研究",
  stage: "初稿",
  version: 18,
  goalWords: 60000,
  daysLeft: 28,
  starred: true,
});
const chapters = reactive(stored ? JSON.parse(stored).chapters : [
  { id: 1, title: "摘要", label: "已完成", status: "done", words: 1268, citations: 4, content: "本文围绕科学文献场景中的检索增强生成问题展开研究，重点解决证据召回、引用追溯与生成可靠性问题。" },
  { id: 2, title: "第 1 章 绪论", label: "已完成", status: "done", words: 5328, citations: 12, content: "随着科学文献数量持续增长，研究者面临信息检索效率低、证据整合成本高等现实问题。" },
  { id: 3, title: "第 2 章 相关工作", label: "已完成", status: "done", words: 8752, citations: 38, content: "本章从稀疏检索、密集检索、重排序和检索增强生成四个方向梳理相关研究。" },
  { id: 4, title: "3.2 多阶段检索框架", label: "进行中", status: "active", words: 4256, citations: 18, content: initialContent },
  { id: 5, title: "第 4 章 实验", label: "进行中", status: "active", words: 9810, citations: 22, content: "本章介绍实验设置、数据集、评价指标以及与主流方法的对比结果。" },
  { id: 6, title: "第 5 章 结论与展望", label: "待开始", status: "todo", words: 0, citations: 0, content: "" },
]);
const activeChapterId = ref(stored ? JSON.parse(stored).activeChapterId : 4);
const activeWorkspace = ref("writing");
const rightTab = ref("citations");
const textStyle = ref("正文");
const saveState = ref("saved");
const focusMode = ref(false);
const outlineCompact = ref(false);
const submitted = ref(false);
const createOpen = ref(false);
const reply = ref("");
const toast = ref("");
const aiResult = ref("");
const citationRefresh = ref(0);
const editor = ref(null);
const selectedVersionId = ref("v18");
let saveTimer;
let toastTimer;

const newThesis = reactive({ title: "", type: "硕士学位论文", goalWords: 60000, direction: "" });
const citationPapers = reactive([
  { title: "Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks", authors: "Patrick Lewis, Ethan Perez, et al.", venue: "NeurIPS 2020", relevance: 98, selected: true, added: false },
  { title: "Fusion in Information Retrieval", authors: "Gordon Cormack, Charles Clarke", venue: "ACM TOIS 2009", relevance: 93, selected: true, added: false },
  { title: "Dense Passage Retrieval for Open-Domain Question Answering", authors: "Vladimir Karpukhin, et al.", venue: "EMNLP 2020", relevance: 90, selected: false, added: false },
]);
const comments = reactive([
  { id: 1, author: "李导师", time: "今天 09:41", content: "这里的三点挑战建议结合具体文献中的案例或实验结果进行支撑，并说明它们与本文研究问题的关系。", resolved: false },
  { id: 2, author: "李导师", time: "昨天 18:07", content: "补充多阶段检索中子查询生成策略的实现细节。", resolved: false },
  { id: 3, author: "张同学", time: "周一 16:20", content: "RAGAS 评测结果已经补充到实验章节。", resolved: true },
]);
const versions = reactive([
  { id: "v18", label: "v18", title: "当前版本（初稿）", time: "今天 10:24", author: "你", delta: "+1,324 字", summary: "优化第 3 章结构，补充重排策略与消融实验，并统一引用格式。", words: 42680, citations: 46, comments: 2 },
  { id: "v17", label: "v17", title: "方法章节细化", time: "今天 08:12", author: "你", delta: "+2,105 字", summary: "新增方法总览与多阶段检索流程描述。", words: 41356, citations: 43, comments: 4 },
  { id: "上传", label: "上传", title: "初稿_v16.docx 覆盖", time: "昨天 23:48", author: "你", delta: "+3,876 字", summary: "从本地 Word 文件覆盖更新，保留原有章节映射。", words: 39251, citations: 38, comments: 5 },
  { id: "v16", label: "v16", title: "相关工作重构", time: "昨天 20:31", author: "你", delta: "+1,982 字", summary: "按检索技术路线重构第 2 章相关工作。", words: 35375, citations: 35, comments: 3 },
]);
const aiActions = ["续写当前段落", "压缩并提升学术表达", "检查论证链条", "根据文献补充证据"];
const contributionCells = Array.from({ length: 84 }, (_, index) => {
  const level = index % 13 === 0 ? 0 : Math.min(4, ((index * 7 + index % 5) % 5));
  return { id: index, level, title: `第 ${Math.floor(index / 7) + 1} 周 · 贡献等级 ${level}` };
});

const activeChapter = computed(() => chapters.find((item) => item.id === activeChapterId.value) || chapters[0]);
const totalWords = computed(() => chapters.reduce((sum, item) => sum + item.words, 0));
const completedChapters = computed(() => chapters.filter((item) => item.status === "done").length);
const activeChapters = computed(() => chapters.filter((item) => item.status === "active").length);
const chapterProgress = computed(() => Math.round((completedChapters.value / chapters.length) * 100));
const unresolvedComments = computed(() => comments.filter((item) => !item.resolved).length);
const resolvedComments = computed(() => comments.filter((item) => item.resolved).length);
const selectedVersion = computed(() => versions.find((item) => item.id === selectedVersionId.value) || versions[0]);
const saveLabel = computed(() => saveState.value === "saving" ? "正在自动保存…" : saveState.value === "error" ? "保存失败" : "已自动保存");

watch([thesis, chapters], queueSave, { deep: true });

function formatNumber(value) {
  return new Intl.NumberFormat("zh-CN").format(value || 0);
}

function showToast(message) {
  toast.value = message;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value = ""; }, 2400);
}

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ thesis, chapters, activeChapterId: activeChapterId.value }));
    saveState.value = "saved";
  } catch {
    saveState.value = "error";
  }
}

function queueSave() {
  saveState.value = "saving";
  clearTimeout(saveTimer);
  saveTimer = setTimeout(persist, 650);
}

function onEditorInput() {
  const contentLength = activeChapter.value.content.replace(/\s/g, "").length;
  activeChapter.value.words = Math.max(contentLength, activeChapter.value.words);
  queueSave();
}

function selectChapter(id) {
  activeChapterId.value = id;
  activeWorkspace.value = "writing";
  nextTick(() => editor.value?.focus());
}

function addChapter() {
  const id = Math.max(...chapters.map((item) => item.id)) + 1;
  chapters.push({ id, title: `第 ${id - 1} 章 新章节`, label: "待开始", status: "todo", words: 0, citations: 0, content: "" });
  selectChapter(id);
  showToast("新章节已创建");
}

function openCreate() {
  newThesis.title = "";
  newThesis.type = "硕士学位论文";
  newThesis.goalWords = 60000;
  newThesis.direction = "";
  createOpen.value = true;
}

function createThesis() {
  thesis.title = newThesis.title.trim();
  thesis.goalWords = Number(newThesis.goalWords) || 60000;
  thesis.version = 1;
  thesis.stage = "提纲";
  chapters.splice(0, chapters.length, { id: 1, title: "摘要", label: "待开始", status: "todo", words: 0, citations: 0, content: "" });
  activeChapterId.value = 1;
  createOpen.value = false;
  persist();
  showToast(`${newThesis.type}写作空间已创建`);
}

function handleUpload(event, replace) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (replace) {
    thesis.version += 1;
    versions.unshift({
      id: `upload-${Date.now()}`,
      label: "上传",
      title: `${file.name} 覆盖更新`,
      time: "刚刚",
      author: "你",
      delta: "待解析",
      summary: `已使用 ${file.name} 覆盖当前论文文件，原版本已安全保留。`,
      words: totalWords.value,
      citations: 46,
      comments: unresolvedComments.value,
    });
    showToast(`已覆盖更新：${file.name}`);
  } else {
    thesis.title = file.name.replace(/\.(docx?|md|txt|pdf)$/i, "");
    showToast(`已导入论文：${file.name}`);
  }
  event.target.value = "";
  persist();
}

function submitAdvisor() {
  submitted.value = true;
  showToast("已提交导师审阅，可在批注区跟踪反馈");
}

function exportThesis() {
  const content = `${thesis.title}\n\n${chapters.map((item) => `${item.title}\n${item.content}`).join("\n\n")}`;
  const url = URL.createObjectURL(new Blob([content], { type: "text/plain;charset=utf-8" }));
  const link = document.createElement("a");
  link.href = url;
  link.download = `${thesis.title}-v${thesis.version}.txt`;
  link.click();
  URL.revokeObjectURL(url);
  showToast("论文已导出");
}

function toggleMark(mark) {
  const target = editor.value;
  if (!target) return;
  const start = target.selectionStart;
  const end = target.selectionEnd;
  const content = activeChapter.value.content;
  activeChapter.value.content = `${content.slice(0, start)}${mark}${content.slice(start, end)}${mark}${content.slice(end)}`;
  queueSave();
}

function insertCitation() {
  activeChapter.value.content += " [待补引用]";
  activeChapter.value.citations += 1;
  queueSave();
}

function insertComment() {
  comments.unshift({ id: Date.now(), author: "你", time: "刚刚", content: `请复核“${activeChapter.value.title}”中的论证与数据来源。`, resolved: false });
  rightTab.value = "comments";
  showToast("批注已添加");
}

function addCitation(paper) {
  paper.added = true;
  activeChapter.value.citations += 1;
  activeChapter.value.content += ` [${activeChapter.value.citations}]`;
  queueSave();
  showToast("引用已加入当前章节");
}

function runAi(action) {
  aiResult.value = `${action}建议：当前段落可进一步明确各检索阶段的输入、输出与评价指标，并将实验结果与研究问题逐项对应，以增强论证的可验证性。`;
}

function applyAiResult() {
  activeChapter.value.content += `\n\n${aiResult.value}`;
  aiResult.value = "";
  queueSave();
  showToast("AI 建议已插入文末");
}

function sendReply() {
  if (!reply.value.trim()) return;
  comments.unshift({ id: Date.now(), author: "你", time: "刚刚", content: reply.value.trim(), resolved: false });
  reply.value = "";
}

function createSnapshot() {
  thesis.version += 1;
  const version = {
    id: `v${thesis.version}`,
    label: `v${thesis.version}`,
    title: "手动保存版本",
    time: "刚刚",
    author: "你",
    delta: "当前快照",
    summary: "保存当前全文、章节结构、引用关系和批注状态。",
    words: totalWords.value,
    citations: chapters.reduce((sum, item) => sum + item.citations, 0),
    comments: unresolvedComments.value,
  };
  versions.unshift(version);
  selectedVersionId.value = version.id;
  persist();
  showToast(`版本 ${version.label} 已保存`);
}

function restoreVersion(version) {
  thesis.version += 1;
  showToast(`已从 ${version.label} 创建恢复副本，当前版本为 v${thesis.version}`);
}
</script>

<style scoped>

/* ═══ ThesisWritingView — Premium Dual-Theme ═══ */
.thesis-page {
  --c-bg:      #f4f5f8;
  --c-surface: #ffffff;
  --c-border:  rgba(15,23,42,.08);
  --c-text:    #0f172a;
  --c-muted:   #64748b;
  --c-subtle:  #94a3b8;
  --c-accent:  #6366f1;
  --c-accent2: #a855f7;
  --sh-sm: 0 2px 8px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04);
  --r: 14px; --r-sm: 8px; --r-pill: 999px;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background .3s, color .3s;
  display: flex;
  flex-direction: column;
}
:root[data-theme="dark"] .thesis-page {
  --c-bg:      #09090e;
  --c-surface: rgba(18,24,40,.9);
  --c-border:  rgba(255,255,255,.07);
  --c-text:    #f1f5f9;
  --c-muted:   #94a3b8;
  --c-subtle:  #64748b;
}

.thesis-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface) !important;
  backdrop-filter: blur(20px);
  box-shadow: 0 1px 4px rgba(15,23,42,.06);
  flex-wrap: wrap;
  position: sticky;
  top: 0;
  z-index: 50;
}
.thesis-title-block { flex: 1; min-width: 0; }
.title-input {
  width: 100%;
  font-size: 17px;
  font-weight: 800;
  color: var(--c-text) !important;
  border: none;
  background: transparent;
  outline: none;
  padding: 0;
}
.title-input::placeholder { color: var(--c-subtle); }
.thesis-meta { display: flex; align-items: center; gap: 10px; margin-top: 4px; font-size: 12.5px; color: var(--c-muted); }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #10b981; display: inline-block; }
.save-state { font-size: 12px; }
.save-state.saving { color: #f59e0b; }
.save-state.saved  { color: #10b981; }

.thesis-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.action-button {
  height: 36px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 700;
  transition: all .18s;
  white-space: nowrap;
}
.action-button:hover { border-color: var(--c-accent); color: var(--c-accent); }
.action-button.primary {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  border-color: transparent;
  color: #fff;
  box-shadow: 0 3px 10px rgba(99,102,241,.28);
}
.action-button.primary:hover { transform: translateY(-1px); box-shadow: 0 6px 18px rgba(99,102,241,.38); }
.star-button {
  width: 32px; height: 32px;
  border-radius: 50%;
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-subtle);
  font-size: 16px;
  display: grid;
  place-items: center;
  transition: all .18s;
}
.star-button.active { color: #f59e0b; border-color: rgba(245,158,11,.3); background: rgba(245,158,11,.08); }

.writing-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  flex: 1;
  height: calc(100vh - 65px);
  overflow: hidden;
}
@media (max-width: 768px) { .writing-shell { grid-template-columns: 1fr; } }

.outline-pane {
  border-right: 1px solid var(--c-border);
  background: var(--c-surface) !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.pane-head {
  padding: 14px 16px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-bg);
  font-size: 12.5px;
  font-weight: 800;
  color: var(--c-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.editor-pane {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--c-surface);
}
.editor-pane textarea, .section-editor {
  flex: 1;
  padding: 28px;
  border: none;
  background: var(--c-surface) !important;
  color: var(--c-text) !important;
  font-size: 15px;
  line-height: 1.85;
  resize: none;
  outline: none;
  font-family: inherit;
}

.thesis-progress {
  padding: 10px 16px;
  border-top: 1px solid var(--c-border);
}
.progress-label { font-size: 12px; color: var(--c-muted); margin-bottom: 6px; }
.progress-bar {
  height: 4px;
  border-radius: 99px;
  background: var(--c-border);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, var(--c-accent), var(--c-accent2));
  transition: width .3s ease;
}

.thesis-page {
  --desk-blue: #1769e8;
  --desk-blue-dark: #0b55c7;
  --desk-ink: #172033;
  --desk-muted: #68758a;
  --desk-line: #e1e7ef;
  min-height: calc(100vh - 116px);
  color: var(--desk-ink);
  background: #f5f7fa;
}

button, input, textarea, select { font: inherit; }
button { color: inherit; }

.thesis-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 26px;
  border-bottom: 1px solid var(--desk-line);
  background: #fff;
}

.thesis-title-block { min-width: 0; flex: 1; }
.title-row { display: flex; align-items: center; gap: 10px; }
.title-input {
  width: min(680px, 100%);
  padding: 0;
  border: 0;
  outline: 0;
  color: var(--desk-ink);
  background: transparent;
  font-size: 21px;
  font-weight: 760;
  letter-spacing: -0.02em;
}
.star-button { padding: 5px 8px; border: 0; border-radius: 7px; color: #7c8798; background: #f1f4f8; font-size: 11px; cursor: pointer; }
.star-button.active { color: #a46600; background: #fff6dc; }
.thesis-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px 14px; margin-top: 8px; color: var(--desk-muted); font-size: 12px; }
.thesis-meta span + span::before { content: ""; display: inline-block; width: 1px; height: 10px; margin-right: 14px; vertical-align: -1px; background: #d7dee8; }
.thesis-meta .status-dot::before { display: none; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--desk-blue); }
.save-state.saved { color: #278459; }
.save-state.saving { color: #9b6b09; }
.save-state.error { color: #c64255; }

.thesis-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }
.action-button {
  min-height: 36px;
  padding: 8px 13px;
  border: 1px solid #d8e0ea;
  border-radius: 9px;
  background: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: color 150ms ease, border-color 150ms ease, background 150ms ease, transform 100ms ease;
}
.action-button:hover { color: var(--desk-blue); border-color: #9dbff3; background: #f8fbff; }
.action-button:active { transform: scale(.97); }
.action-button.primary { color: #fff; border-color: var(--desk-blue); background: var(--desk-blue); }
.action-button.primary:hover { border-color: var(--desk-blue-dark); background: var(--desk-blue-dark); }
.action-button.primary.quiet { color: var(--desk-blue); background: #edf5ff; }
.action-button.success { border-color: #23845a; background: #23845a; }
.file-action { position: relative; cursor: pointer; }
.file-action input { position: absolute; width: 1px; height: 1px; opacity: 0; }

.writing-shell {
  display: grid;
  grid-template-columns: 280px minmax(480px, 1fr) 320px;
  height: min(690px, calc(100vh - 236px));
  min-height: 560px;
  border-bottom: 1px solid var(--desk-line);
  background: #fff;
}
.outline-pane, .research-pane { min-width: 0; background: #fbfcfe; }
.outline-pane { border-right: 1px solid var(--desk-line); overflow: auto; }
.research-pane { border-left: 1px solid var(--desk-line); overflow: auto; }
.pane-head { display: flex; align-items: center; justify-content: space-between; padding: 18px 18px 12px; }
.pane-head div { display: grid; gap: 3px; }
.pane-head strong { font-size: 14px; }
.pane-head span { color: var(--desk-muted); font-size: 11px; }
.pane-head button { width: 29px; height: 29px; border: 1px solid var(--desk-line); border-radius: 8px; background: #fff; cursor: pointer; }
.thesis-progress { padding: 8px 18px 16px; border-bottom: 1px solid var(--desk-line); }
.progress-copy { display: flex; justify-content: space-between; font-size: 12px; }
.progress-copy strong { color: var(--desk-blue); }
.progress-track { height: 5px; margin: 9px 0 7px; overflow: hidden; border-radius: 999px; background: #e6ebf2; }
.progress-track span { display: block; width: 100%; height: 100%; transform-origin: left; border-radius: inherit; background: var(--desk-blue); transition: transform 400ms cubic-bezier(.22,1,.36,1); }
.thesis-progress small { color: var(--desk-muted); font-size: 10px; }
.chapter-list { display: grid; padding: 8px; }
.chapter-item {
  display: grid;
  grid-template-columns: 11px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 11px 9px;
  border: 0;
  border-radius: 8px;
  text-align: left;
  background: transparent;
  cursor: pointer;
  transition: background 160ms ease, color 160ms ease;
}
.chapter-item:hover { background: #f1f5fa; }
.chapter-item.active { color: var(--desk-blue); background: #eaf3ff; }
.chapter-state { width: 8px; height: 8px; border: 1.5px solid #bcc6d4; border-radius: 50%; }
.chapter-state.done { border-color: #2ea66e; background: #2ea66e; box-shadow: inset 0 0 0 2px #fff; }
.chapter-state.active { border-color: var(--desk-blue); box-shadow: inset 0 0 0 2px #fff; background: var(--desk-blue); }
.chapter-copy { min-width: 0; display: grid; gap: 3px; }
.chapter-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.chapter-copy small { color: #8691a2; font-size: 10px; }
.chapter-words { color: #8994a5; font-size: 10px; }
.outline-settings { width: calc(100% - 32px); margin: 12px 16px 18px; padding: 9px; border: 1px solid var(--desk-line); border-radius: 8px; color: #57657b; background: #fff; font-size: 11px; cursor: pointer; }

.editor-pane { min-width: 0; display: flex; flex-direction: column; background: #fff; }
.editor-commandbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 52px; padding: 8px 18px; border-bottom: 1px solid var(--desk-line); }
.format-group, .editor-tools { display: flex; align-items: center; gap: 4px; }
.format-group button, .editor-tools button { padding: 7px 9px; border: 0; border-radius: 7px; color: #657186; background: transparent; font-size: 11px; cursor: pointer; }
.format-group button:hover, .format-group button.active, .editor-tools button:hover { color: var(--desk-blue); background: #edf4ff; }
.format-group.compact { padding-left: 10px; border-left: 1px solid var(--desk-line); }
.paper-editor { display: flex; flex: 1; min-height: 0; flex-direction: column; width: min(820px, 100%); margin: 0 auto; padding: 34px clamp(28px, 5vw, 72px) 14px; }
.paper-document-title {
  margin: 0 auto 34px;
  color: #111827;
  font-size: 24px;
  font-weight: 760;
  letter-spacing: -0.025em;
  line-height: 1.35;
  text-align: center;
  text-wrap: balance;
}
.chapter-title-input { padding: 0 0 18px; border: 0; outline: 0; color: #171d2a; background: transparent; font-size: 23px; font-weight: 760; letter-spacing: -0.025em; }
.manuscript-editor { flex: 1; min-height: 0; padding: 0; resize: none; border: 0; outline: 0; color: #303a49; background: transparent; font-family: "Songti SC", "Noto Serif CJK SC", serif; font-size: 16px; line-height: 2.05; text-align: justify; }
.manuscript-editor.style-标题 { font-size: 18px; line-height: 1.9; font-weight: 650; }
.manuscript-editor.style-引用 { padding-left: 18px; border-left: 2px solid #a9c9f7; color: #52617a; }
.editor-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 10px; border-top: 1px solid var(--desk-line); color: #7d899a; font-size: 10px; }

.research-tabs { position: sticky; top: 0; z-index: 2; display: grid; grid-template-columns: repeat(3, 1fr); background: #fff; border-bottom: 1px solid var(--desk-line); }
.research-tabs button { padding: 17px 4px 13px; border: 0; border-bottom: 2px solid transparent; color: #6c788b; background: transparent; font-size: 12px; font-weight: 700; cursor: pointer; }
.research-tabs button.active { color: var(--desk-blue); border-bottom-color: var(--desk-blue); }
.research-content { padding: 14px; }
.section-line { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 12px; }
.section-line button, .section-line span { border: 0; color: #7d8999; background: transparent; font-size: 10px; }
.citation-row { display: grid; grid-template-columns: 20px 1fr; gap: 8px; padding: 13px 0; border-bottom: 1px solid var(--desk-line); }
.citation-row label input { width: 15px; height: 15px; accent-color: var(--desk-blue); }
.citation-row strong { display: block; font-size: 11px; line-height: 1.45; }
.citation-row p { margin: 5px 0; color: #748094; font-size: 10px; line-height: 1.4; }
.citation-row small { display: block; color: #929dad; font-size: 9px; }
.citation-row button { margin-top: 7px; padding: 0; border: 0; color: var(--desk-blue); background: transparent; font-size: 10px; font-weight: 700; cursor: pointer; }
.full-list-button { width: 100%; margin-top: 12px; padding: 10px; border: 0; color: var(--desk-blue); background: #eef5ff; border-radius: 8px; font-size: 11px; font-weight: 700; }
.ai-context { padding: 13px; border-radius: 10px; background: #eef5ff; }
.ai-context span, .ai-context strong { display: block; }
.ai-context span { color: #6a7890; font-size: 10px; }
.ai-context strong { margin-top: 4px; font-size: 12px; }
.ai-context p { margin: 7px 0 0; color: #64738a; font-size: 10px; line-height: 1.55; }
.ai-action { display: grid; gap: 4px; width: 100%; padding: 12px 2px; border: 0; border-bottom: 1px solid var(--desk-line); text-align: left; background: transparent; cursor: pointer; }
.ai-action span { font-size: 12px; font-weight: 700; }
.ai-action small { color: #8490a2; font-size: 10px; }
.ai-result { margin-top: 13px; padding: 13px; border: 1px solid #bcd5f7; border-radius: 10px; background: #f8fbff; }
.ai-result strong { font-size: 11px; }
.ai-result p { color: #52617a; font-size: 11px; line-height: 1.65; }
.ai-result button { padding: 7px 9px; border: 0; border-radius: 7px; color: #fff; background: var(--desk-blue); font-size: 10px; }
.comment-row { padding: 13px 0; border-bottom: 1px solid var(--desk-line); }
.comment-row.resolved { opacity: .58; }
.comment-row header { display: grid; grid-template-columns: 28px 1fr auto; align-items: center; gap: 8px; }
.comment-avatar { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 50%; color: #fff; background: #7457dd; font-size: 11px; font-weight: 700; }
.comment-row header div { display: grid; }
.comment-row header strong { font-size: 11px; }
.comment-row header small, .comment-row header > span:last-child { color: #8b96a7; font-size: 9px; }
.comment-row p { margin: 10px 0; color: #556278; font-size: 11px; line-height: 1.65; }
.comment-row button { padding: 0; border: 0; color: var(--desk-blue); background: transparent; font-size: 10px; }
.comment-reply { position: sticky; bottom: 0; display: grid; grid-template-columns: 1fr auto; margin-top: 14px; border: 1px solid var(--desk-line); border-radius: 9px; background: #fff; }
.comment-reply input { min-width: 0; padding: 9px; border: 0; outline: 0; background: transparent; font-size: 10px; }
.comment-reply button { border: 0; color: var(--desk-blue); background: transparent; font-size: 10px; font-weight: 700; }

.version-workspace { min-height: 620px; padding: 28px; background: #f5f7fa; }
.version-workspace > header { display: flex; justify-content: space-between; align-items: end; margin-bottom: 20px; }
.version-workspace > header span { color: var(--desk-blue); font-size: 11px; font-weight: 700; }
.version-workspace h2 { margin: 5px 0 0; font-size: 22px; }
.version-layout { display: grid; grid-template-columns: minmax(300px, .8fr) minmax(440px, 1.2fr); border: 1px solid var(--desk-line); border-radius: 12px; overflow: hidden; background: #fff; }
.version-list { display: grid; border-right: 1px solid var(--desk-line); }
.version-list button { display: grid; grid-template-columns: 48px 1fr auto; align-items: center; gap: 10px; padding: 18px; border: 0; border-bottom: 1px solid var(--desk-line); text-align: left; background: #fff; cursor: pointer; }
.version-list button.active { background: #edf5ff; }
.version-node { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: var(--desk-blue); border: 2px solid #9ec3f7; font-size: 11px; font-weight: 800; }
.version-list button.active .version-node { color: #fff; border-color: var(--desk-blue); background: var(--desk-blue); }
.version-list button > span:nth-child(2) { display: grid; gap: 5px; }
.version-list strong { font-size: 12px; }
.version-list small, .version-delta { color: #8290a3; font-size: 10px; }
.version-detail { padding: 28px; }
.version-detail-head { display: flex; align-items: start; justify-content: space-between; gap: 18px; }
.version-detail-head span { color: var(--desk-blue); font-size: 12px; font-weight: 800; }
.version-detail h3 { margin: 5px 0 0; font-size: 21px; }
.version-detail > p { max-width: 65ch; margin: 22px 0; color: #5d6b80; line-height: 1.7; }
.version-stats { display: flex; gap: 1px; overflow: hidden; border-radius: 10px; background: var(--desk-line); }
.version-stats span { flex: 1; display: grid; gap: 4px; padding: 16px; color: #718095; background: #f8fafc; font-size: 10px; }
.version-stats strong { color: var(--desk-ink); font-size: 18px; }
.diff-block { display: grid; gap: 7px; margin-top: 24px; padding: 16px; border-radius: 10px; background: #f7f9fc; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; }
.diff-add { color: #237b55; background: #e9f7f0; }
.diff-remove { color: #b64154; background: #fff0f2; }
.diff-block span { padding: 7px; border-radius: 5px; }

.thesis-insights { display: grid; grid-template-columns: 220px 210px 200px minmax(360px, 1fr); gap: 0; min-height: 112px; padding: 15px 24px; background: #fff; }
.thesis-insights > div { padding: 4px 20px; border-right: 1px solid var(--desk-line); }
.thesis-insights > div:last-child { border-right: 0; }
.insight-progress { display: flex; align-items: center; gap: 12px; }
.ring { --progress: 220deg; display: grid; place-items: center; width: 58px; height: 58px; border-radius: 50%; background: conic-gradient(var(--desk-blue) var(--progress), #e5ebf3 0); position: relative; }
.ring::after { content: ""; position: absolute; inset: 6px; border-radius: 50%; background: #fff; }
.ring strong { position: relative; z-index: 1; font-size: 13px; }
.insight-progress div { display: grid; gap: 5px; }
.insight-progress div strong { font-size: 11px; }
.insight-progress small { color: #7c8899; font-size: 9px; }
.insight-list { display: grid; align-content: center; gap: 8px; }
.insight-list span { display: flex; justify-content: space-between; color: #68758a; font-size: 10px; }
.insight-list strong { color: var(--desk-ink); }
.mini-contribution { display: grid; align-content: center; gap: 9px; }
.mini-head { display: flex; justify-content: space-between; gap: 16px; font-size: 10px; }
.mini-head strong { color: #68758a; font-size: 9px; }
.mini-grid { display: grid; grid-template-columns: repeat(12, 1fr); grid-template-rows: repeat(7, 7px); grid-auto-flow: column; gap: 3px; }
.mini-grid span { border-radius: 2px; background: #edf1f6; }
.mini-grid .level-1 { background: #d8e8ff; }
.mini-grid .level-2 { background: #a8ccfb; }
.mini-grid .level-3 { background: #5d9af1; }
.mini-grid .level-4 { background: #1769e8; }

.desk-overlay { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 24px; background: rgba(15, 25, 42, .42); }
.desk-dialog { width: min(620px, 100%); padding: 24px; border-radius: 14px; background: #fff; box-shadow: 0 14px 42px rgba(12, 24, 45, .18); }
.desk-dialog header { display: flex; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.desk-dialog header span { color: var(--desk-blue); font-size: 11px; font-weight: 800; }
.desk-dialog h2 { margin: 5px 0 0; font-size: 20px; }
.desk-dialog header button { height: 32px; border: 0; color: #6c788b; background: transparent; }
.desk-dialog label { display: grid; gap: 7px; margin-top: 14px; color: #536177; font-size: 11px; font-weight: 700; }
.desk-dialog input, .desk-dialog select { padding: 10px 11px; border: 1px solid #dbe2eb; border-radius: 8px; outline: 0; background: #fff; }
.desk-dialog input:focus, .desk-dialog select:focus { border-color: var(--desk-blue); box-shadow: 0 0 0 3px rgba(23,105,232,.11); }
.dialog-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.desk-dialog footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 24px; }
.desk-dialog footer button { padding: 9px 14px; border: 1px solid #dbe2eb; border-radius: 8px; background: #fff; }
.desk-dialog footer .primary { color: #fff; border-color: var(--desk-blue); background: var(--desk-blue); }
.desk-toast { position: fixed; z-index: 1200; right: 28px; bottom: 28px; padding: 11px 15px; border-radius: 9px; color: #fff; background: #172033; box-shadow: 0 8px 24px rgba(14, 28, 50, .16); font-size: 12px; }

.desk-dialog-enter-active, .desk-dialog-leave-active { transition: opacity 180ms ease; }
.desk-dialog-enter-active .desk-dialog, .desk-dialog-leave-active .desk-dialog { transition: transform 220ms cubic-bezier(.22,1,.36,1), opacity 180ms ease; }
.desk-dialog-enter-from, .desk-dialog-leave-to { opacity: 0; }
.desk-dialog-enter-from .desk-dialog, .desk-dialog-leave-to .desk-dialog { opacity: 0; transform: translateY(12px) scale(.985); }
.toast-enter-active, .toast-leave-active { transition: transform 220ms cubic-bezier(.22,1,.36,1), opacity 180ms ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(10px); }

@media (max-width: 1180px) {
  .writing-shell { grid-template-columns: 240px minmax(440px, 1fr); }
  .research-pane { position: fixed; z-index: 20; top: 118px; right: 0; bottom: 0; width: 320px; box-shadow: -8px 0 22px rgba(17, 31, 53, .08); transform: translateX(100%); }
  .thesis-insights { grid-template-columns: repeat(3, 1fr); }
  .mini-contribution { grid-column: 1 / -1; border-top: 1px solid var(--desk-line); }
}

@media (max-width: 760px) {
  .thesis-head { align-items: flex-start; flex-direction: column; padding: 16px; }
  .thesis-actions { justify-content: flex-start; }
  .writing-shell { display: block; height: auto; }
  .outline-pane { max-height: 300px; border-right: 0; border-bottom: 1px solid var(--desk-line); }
  .editor-pane { min-height: 650px; }
  .editor-commandbar { overflow-x: auto; }
  .paper-editor { padding-inline: 22px; }
  .thesis-insights { grid-template-columns: 1fr; }
  .thesis-insights > div { border-right: 0; border-bottom: 1px solid var(--desk-line); }
  .version-layout { grid-template-columns: 1fr; }
  .version-list { border-right: 0; border-bottom: 1px solid var(--desk-line); }
  .dialog-grid { grid-template-columns: 1fr; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { scroll-behavior: auto !important; animation: none !important; transition-duration: .01ms !important; }
}
</style>
