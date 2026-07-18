<template>
  <div class="topic-square-page">
    <header class="square-toolbar">
      <div class="toolbar-tabs">
        <button type="button" :class="{ active: !savedOnly }" @click="setSavedOnly(false)">全部选题</button>
        <button type="button" :class="{ active: savedOnly }" @click="setSavedOnly(true)">我的收藏</button>
      </div>

      <label class="toolbar-search">
        <span>搜索</span>
        <input v-model.trim="filters.keyword" type="search" placeholder="搜索选题 / 方向 / 关键词" @keyup.enter="loadTopics" />
      </label>

      <div class="toolbar-segment">
        <button v-for="item in sortTabs" :key="item.value" type="button" :class="{ active: filters.sort === item.value }" @click="setSort(item.value)">
          {{ item.label }}
        </button>
      </div>

      <select v-model="filters.tag" class="toolbar-select">
        <option value="">按当前标签筛选</option>
        <option v-for="item in availableTagFilters" :key="item.value" :value="item.value">
          {{ item.value }}（{{ item.count }}）
        </option>
      </select>

      <button type="button" class="toolbar-primary" @click="openGenerator">发起调研</button>
      <button v-if="isAdmin" type="button" class="toolbar-admin-generate" :disabled="adminGenerating" @click="generateOfficialHotTopics">
        {{ adminGenerating ? "AI 思考中" : "AI 生成官方热点" }}
      </button>
    </header>

    <section class="square-status-row">
      <div>
        <strong>选题广场</strong>
        <span>{{ filteredTopics.length }} / {{ topics.length }} 个方向 · deep-research 接入后端 · 可收藏、查看代表论文来源、生成综述/组会提纲</span>
      </div>
      <button type="button" @click="loadTopics" :disabled="loading || adminGenerating">{{ loading ? "刷新中" : "刷新" }}</button>
    </section>

    <main class="topic-board" :class="{ loading }">
      <article v-if="loading && !topics.length" v-for="index in 6" :key="index" class="topic-card skeleton-card"></article>

      <article v-for="topic in filteredTopics" :key="topic.id" class="topic-card" :class="{ 'admin-manageable': isAdmin }" @click="selectedTopic = topic">
        <div class="topic-visual" :class="visualClass(topic)">
          <span class="provider-badge" :class="{ official: topicProviderLabel(topic) === '官方' }">{{ topicProviderLabel(topic) }}</span>
          <button
            v-if="isAdmin"
            type="button"
            class="topic-admin-delete"
            :disabled="isDeletingTopic(topic)"
            title="删除这个选题"
            @click.stop="deleteTopicAsAdmin(topic)"
          >
            {{ isDeletingTopic(topic) ? "删除中" : "删除" }}
          </button>
          <div class="visual-system" aria-hidden="true">
            <i class="axis axis-x"></i>
            <i class="axis axis-y"></i>
            <span v-for="index in 9" :key="index" :style="visualPointStyle(topic, index)"></span>
          </div>
          <div class="visual-caption">
            <strong>{{ firstCluster(topic) }}</strong>
            <span>{{ topic.discipline }} · {{ topic.goal }} · {{ evidenceLabel(topic) }}</span>
          </div>
        </div>

        <div class="topic-body">
          <h2>{{ topic.title }}</h2>
          <p>{{ topic.summary }}</p>
          <div class="topic-subtopics">
            <span v-for="item in visibleSubtopics(topic)" :key="item.name || item">{{ item.name || item }}</span>
          </div>
          <div class="topic-tags">
            <span v-for="tag in visibleTags(topic)" :key="tag">{{ tag }}</span>
            <span v-if="hiddenTagCount(topic) > 0">+{{ hiddenTagCount(topic) }}</span>
          </div>
        </div>

        <footer class="topic-meta">
          <span>更新时间 {{ topic.updatedAt || topic.createdAt || "刚刚" }} · {{ topic.likes || 0 }} 人想做</span>
          <div>
            <button type="button" :disabled="topic.interested" @click.stop="markInterested(topic)">{{ topic.interested ? "已想做" : "想做" }}</button>
            <button type="button" @click.stop="toggleSave(topic)">{{ topic.saved ? "已复用" : "复用收藏" }}</button>
          </div>
        </footer>
      </article>

      <div v-if="!loading && !filteredTopics.length" class="empty-state">
        <strong>还没有匹配的选题</strong>
        <span>{{ topics.length ? "换一个标签，或清空筛选。" : "换一个关键词，或直接发起一次 deep-research 调研。" }}</span>
        <button type="button" @click="openGenerator">发起调研</button>
      </div>
    </main>

    <div v-if="selectedTopic" class="modal-backdrop" @click.self="selectedTopic = null">
      <section class="topic-detail">
        <button type="button" class="modal-close" @click="selectedTopic = null">×</button>
        <header>
          <span>{{ selectedTopic.discipline }} · {{ selectedTopic.stage }} · {{ selectedTopic.goal }}</span>
          <h2>{{ selectedTopic.title }}</h2>
          <p>{{ selectedTopic.summary }}</p>
        </header>

        <div class="score-strip">
          <article><span>可行</span><strong>{{ selectedTopic.feasibility }}</strong></article>
          <article><span>创新</span><strong>{{ selectedTopic.innovation }}</strong></article>
          <article><span>难度</span><strong>{{ selectedTopic.difficulty }}</strong></article>
        </div>

        <div class="detail-grid">
          <article>
            <h3>研究问题</h3>
            <p>{{ detailText(selectedTopic, "question") }}</p>
          </article>
          <article>
            <h3>研究空白</h3>
            <p>{{ detailText(selectedTopic, "gap") }}</p>
          </article>
          <article>
            <h3>方法路线</h3>
            <p>{{ detailText(selectedTopic, "method") }}</p>
          </article>
          <article>
            <h3>风险提醒</h3>
            <p>{{ detailText(selectedTopic, "risk") }}</p>
          </article>
        </div>

        <section class="subtopic-panel">
          <div class="paper-panel-head">
            <h3>推荐方向</h3>
            <span>每个方向都由模型生成完整调研结构</span>
          </div>
          <article v-for="(item, index) in detailSubtopics(selectedTopic)" :key="item.name" class="direction-report">
            <div class="subtopic-title-row">
              <div>
                <small>推荐方向 {{ String(index + 1).padStart(2, "0") }}</small>
                <strong>{{ item.name }}</strong>
              </div>
              <span>推荐度 {{ directionScore(item, selectedTopic, index) }}</span>
            </div>
            <div class="subtopic-analysis direction-report-body">
              <section v-for="block in directionReportBlocks(item, selectedTopic)" :key="block.label + block.text" :class="['direction-report-block', block.key]">
                <b>{{ block.label }}</b>
                <ul>
                  <li v-for="point in blockPoints(block.text)" :key="point">{{ point }}</li>
                </ul>
              </section>
            </div>
            <div v-if="subtopicPapers(item, selectedTopic).length" class="subtopic-papers">
              <div v-for="paper in subtopicPapers(item, selectedTopic)" :key="paper.title" class="subtopic-paper-row">
                <div>
                  <b>{{ paper.title }}</b>
                  <span>{{ paperSourceMeta(paper) }}</span>
                  <button v-if="paperSourceUrl(paper)" type="button" class="paper-source-link" @click.stop="openPaperSource(paper)">查看来源</button>
                </div>
              </div>
            </div>
          </article>
        </section>

        <footer>
          <button type="button" :disabled="selectedTopic.interested || selectedTopic.interestPending" @click="markInterested(selectedTopic)">{{ selectedTopic.interested ? "已想做" : "想做这个方向" }}</button>
          <button type="button" @click="toggleSave(selectedTopic)">{{ selectedTopic.saved ? "已复用收藏" : "复用到我的收藏" }}</button>
        </footer>
      </section>
    </div>

    <div v-if="showGenerator" class="modal-backdrop" @click.self="closeGenerator">
      <section class="generator-modal">
        <button type="button" class="modal-close" @click="closeGenerator">×</button>
        <header>
          <span>deep-research</span>
          <h2>生成一个可继续推进的选题。</h2>
        </header>

        <form v-if="!generating" class="generator-form" @submit.prevent="generateTopic">
          <label class="full">
            研究方向
            <input v-model.trim="generatorForm.direction" required placeholder="例如：低资源场景下的多模态医学影像分析" />
          </label>
          <label>
            研究方向大类
            <input v-model.trim="generatorForm.discipline" required list="discipline-presets" placeholder="例如：医学影像 / 药物发现 / 教育技术" />
            <datalist id="discipline-presets">
              <option v-for="item in disciplinePresets" :key="item" :value="item" />
            </datalist>
          </label>
          <label>
            学历阶段
            <select v-model="generatorForm.stage">
              <option v-for="item in stages" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            目标用途
            <select v-model="generatorForm.goal">
              <option v-for="item in goals" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            资源条件
            <select v-model="generatorForm.resource">
              <option v-for="item in resources" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            数据来源
            <select v-model="generatorForm.dataAccess">
              <option v-for="item in dataAccessOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            数据/样本形态
            <select v-model="generatorForm.sampleType">
              <option v-for="item in sampleTypes" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            方法偏好
            <select v-model="generatorForm.methodPreference">
              <option v-for="item in methodOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            选题尺度
            <select v-model="generatorForm.topicScale">
              <option v-for="item in topicScales" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            期望贡献
            <select v-model="generatorForm.expectedContribution">
              <option v-for="item in contributionOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <fieldset class="full chip-field">
            <legend>重点约束</legend>
            <button v-for="item in constraintOptions" :key="item" type="button" :class="{ active: generatorForm.constraints.includes(item) }" @click="toggleConstraint(item)">
              {{ item }}
            </button>
          </fieldset>
          <label>
            英文关键词
            <input v-model.trim="generatorForm.keywords" placeholder="例如：few-shot segmentation, foundation model" />
          </label>
          <label>
            避开路线
            <input v-model.trim="generatorForm.avoidRoutes" placeholder="例如：不做纯综述、不做模型堆叠" />
          </label>
          <label class="full">
            已读/想参考的论文
            <textarea v-model.trim="generatorForm.seedPapers" rows="3" placeholder="可粘贴 1-5 篇论文题名、DOI 或 arXiv 号；系统会尽量围绕这些论文扩展，而不是乱发散"></textarea>
          </label>
          <label class="full">
            补充说明
            <textarea v-model.trim="generatorForm.note" rows="4" placeholder="写清楚专业、可拿到的数据、导师方向、已有论文、希望偏理论/工程/应用，或明确不想做的路线"></textarea>
          </label>
          <button type="submit" class="toolbar-primary">开始调研</button>
        </form>

        <div v-else class="research-progress">
          <header class="research-progress-head">
            <strong>正在生成调研 brief</strong>
            <span>{{ generationSteps[generationIndex] }}</span>
          </header>
          <div v-for="(step, index) in generationSteps" :key="step" :class="{ active: index <= generationIndex }">
            <i>{{ index + 1 }}</i>
            <span>{{ step }}</span>
          </div>
          <p>会先用方向大类扩展检索词，再用真实候选文献筛掉泛题，最后把每个推荐方向写成摘要、具体方法、发文现状、优势、局限、潜在论文和代表论文。</p>
        </div>
      </section>
    </div>

    <div v-if="toastMessage" class="topic-toast">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { paperpilotApi } from "../services/paperpilotApi";
import { useAuthStore } from "../stores/auth";
import { useDialogStore } from "../stores/dialog";

const router = useRouter();
const authStore = useAuthStore();
const dialogStore = useDialogStore();
const isAdmin = computed(() => authStore.profile.role === "管理员" || authStore.session?.role === "管理员");
const disciplinePresets = [
  "人工智能",
  "LLM 大模型",
  "多模态学习",
  "医学影像",
  "药物发现",
  "生物信息学",
  "教育技术",
  "公共卫生",
  "材料计算",
  "管理科学",
  "数字人文",
  "心理测量",
];
const stages = ["本科", "硕士", "博士", "课程论文"];
const goals = ["课程论文", "开题", "综述", "基金背景", "投稿选题", "组会准备", "毕业论文", "横向项目"];
const resources = ["无实验，仅公开数据", "只有文献和开源代码", "有导师课题", "有实验室/设备", "有企业或医院数据", "有问卷/访谈对象", "已有一批论文想继续挖"];
const dataAccessOptions = ["公开数据集优先", "可爬取公开网页/报告", "问卷/访谈可获得", "实验室数据可获得", "医院/企业数据需脱敏", "暂不确定，需要推荐数据源"];
const sampleTypes = ["表格数据", "文本/论文语料", "图像/医学影像", "时序/传感器", "多模态数据", "问卷/访谈", "代码/日志", "暂不确定"];
const methodOptions = ["不限，先找可行路线", "深度学习/大模型", "传统机器学习 + 可解释", "统计建模/因果推断", "综述/计量分析", "实验设计/对照验证", "系统开发/工程落地"];
const topicScales = ["硕士可完成的小题", "本科可完成的小题", "博士投稿级问题", "基金背景方向", "课程作业快速成稿"];
const contributionOptions = ["提出一个可验证问题", "复现并改进代表方法", "建立评价指标/基准", "做跨场景对比", "整理综述框架", "形成系统原型", "给导师可改的开题方向"];
const constraintOptions = ["必须有公开数据", "必须可复现", "尽量少训练成本", "需要可解释性", "要有对照实验", "适合组会汇报", "适合投稿", "避开敏感数据", "需要导师可改", "不碰隐私数据", "能在 2 周内验证", "需要代码开源"];
const sortTabs = [
  { label: "最新", value: "latest" },
  { label: "最热", value: "hot" },
  { label: "点赞最多", value: "liked" },
];
const generationSteps = ["读取研究 brief", "拆解对象和数据", "扩展中英文检索词", "检索真实文献", "生成 3-5 个推荐方向", "筛掉泛题和重复方向", "匹配代表论文来源", "写调研报告", "写入我的收藏"];

const filters = reactive({
  keyword: "",
  tag: "",
  sort: "latest",
});
const generatorForm = reactive({
  direction: "",
  discipline: "",
  stage: "硕士",
  goal: "开题",
  resource: "无实验，仅公开数据",
  dataAccess: "公开数据集优先",
  sampleType: "暂不确定",
  methodPreference: "不限，先找可行路线",
  topicScale: "硕士可完成的小题",
  outputDepth: "详细：每个推荐方向都给 7 段调研报告",
  evaluationFocus: "准确率/效果提升",
  expectedContribution: "提出一个可验证问题",
  constraints: ["必须有公开数据", "必须可复现"],
  keywords: "",
  avoidRoutes: "",
  seedPapers: "",
  note: "",
});

const topics = ref([]);
const loading = ref(false);
const savedOnly = ref(false);
const selectedTopic = ref(null);
const showGenerator = ref(false);
const generating = ref(false);
const adminGenerating = ref(false);
const generationIndex = ref(0);
const toastMessage = ref("");
const deletingTopicIds = ref(new Set());
let progressTimer = null;

onMounted(loadTopics);
onBeforeUnmount(() => {
  if (progressTimer) clearInterval(progressTimer);
});

async function loadTopics() {
  loading.value = true;
  try {
    topics.value = await paperpilotApi.getTopics({
      keyword: filters.keyword,
      sort: filters.sort,
      savedOnly: savedOnly.value,
    });
  } catch (error) {
    toast(error.response?.data?.message || "选题广场加载失败");
  } finally {
    loading.value = false;
  }
}

const availableTagFilters = computed(() => {
  const counts = new Map();
  for (const topic of topics.value) {
    const values = [
      topic.discipline,
      topic.stage,
      topic.goal,
      ...(Array.isArray(topic.tags) ? topic.tags : []),
      ...(Array.isArray(topic.themeClusters) ? topic.themeClusters : []),
    ];
    values
      .map(value => String(value || "").trim())
      .filter(Boolean)
      .forEach(value => counts.set(value, (counts.get(value) || 0) + 1));
  }
  return Array.from(counts.entries())
    .map(([value, count]) => ({ value, count }))
    .sort((a, b) => b.count - a.count || a.value.localeCompare(b.value, "zh-Hans-CN"))
    .slice(0, 60);
});

const filteredTopics = computed(() => {
  const tag = String(filters.tag || "").trim();
  if (!tag) return topics.value;
  return topics.value.filter(topic => {
    const values = [
      topic.discipline,
      topic.stage,
      topic.goal,
      ...(Array.isArray(topic.tags) ? topic.tags : []),
      ...(Array.isArray(topic.themeClusters) ? topic.themeClusters : []),
    ];
    return values.some(value => String(value || "").trim() === tag);
  });
});

function setSavedOnly(value) {
  savedOnly.value = value;
  loadTopics();
}

function setSort(value) {
  filters.sort = value;
  loadTopics();
}

function openGenerator() {
  showGenerator.value = true;
}

function closeGenerator() {
  showGenerator.value = false;
  generating.value = false;
  generationIndex.value = 0;
  if (progressTimer) clearInterval(progressTimer);
}

async function generateTopic() {
  generating.value = true;
  generationIndex.value = 0;
  const startedAt = Date.now();
  progressTimer = setInterval(() => {
    generationIndex.value = Math.min(generationSteps.length - 1, generationIndex.value + 1);
  }, 1500);
  try {
    const result = await paperpilotApi.generateTopic({ ...generatorForm });
    const minimumMs = 12500;
    const elapsed = Date.now() - startedAt;
    if (elapsed < minimumMs) {
      await new Promise(resolve => setTimeout(resolve, minimumMs - elapsed));
    }
    const createdTopics = Array.isArray(result) ? result : [result];
    const createdIds = new Set(createdTopics.map(item => item.id));
    topics.value = [...createdTopics, ...topics.value.filter(item => !createdIds.has(item.id))];
    selectedTopic.value = createdTopics[0] || null;
    savedOnly.value = true;
    showGenerator.value = false;
    toast(`已生成 ${createdTopics.length} 张选题卡，并加入我的收藏`);
  } catch (error) {
    toast(error.response?.data?.message || "选题调研失败");
  } finally {
    generating.value = false;
    generationIndex.value = 0;
    if (progressTimer) clearInterval(progressTimer);
  }
}

async function generateOfficialHotTopics() {
  if (!isAdmin.value || adminGenerating.value) return;
  adminGenerating.value = true;
  const startedAt = Date.now();
  toast("管理员热点生成已开始：正在调用选题调研模型和真实文献检索");
  try {
    const generated = await paperpilotApi.generateAdminHotTopics({ maxTopics: 3 });
    const minimumMs = 16000;
    const elapsed = Date.now() - startedAt;
    if (elapsed < minimumMs) {
      await new Promise(resolve => setTimeout(resolve, minimumMs - elapsed));
    }
    const createdTopics = Array.isArray(generated) ? generated.slice(0, 3) : [];
    const createdIds = new Set(createdTopics.map(item => item.id));
    topics.value = [...createdTopics, ...topics.value.filter(item => !createdIds.has(item.id))];
    savedOnly.value = false;
    toast(`AI 已生成 ${createdTopics.length} 个官方热点选题`);
  } catch (error) {
    toast(error.response?.data?.message || "官方热点生成失败：请先在管理员模型配置里配置选题调研模型");
  } finally {
    adminGenerating.value = false;
  }
}

function isDeletingTopic(topic) {
  return deletingTopicIds.value.has(topic?.id);
}

async function deleteTopicAsAdmin(topic) {
  if (!isAdmin.value || !topic?.id || isDeletingTopic(topic)) return;
  const ok = await dialogStore.confirm(`确认删除选题「${topic.title}」吗？删除后用户侧选题大厅也会移除。`, {
    title: "删除选题",
    confirmText: "删除",
    cancelText: "取消",
    danger: true,
  });
  if (!ok) return;
  deletingTopicIds.value = new Set([...deletingTopicIds.value, topic.id]);
  try {
    await paperpilotApi.deleteAdminTopic(topic.id);
    topics.value = topics.value.filter(item => item.id !== topic.id);
    if (selectedTopic.value?.id === topic.id) selectedTopic.value = null;
    toast("已删除该选题");
  } catch (error) {
    toast(error.response?.data?.message || "删除选题失败");
  } finally {
    const next = new Set(deletingTopicIds.value);
    next.delete(topic.id);
    deletingTopicIds.value = next;
  }
}

function toggleConstraint(item) {
  const index = generatorForm.constraints.indexOf(item);
  if (index >= 0) {
    generatorForm.constraints.splice(index, 1);
  } else {
    generatorForm.constraints.push(item);
  }
}

async function toggleSave(topic) {
  const previous = topic.saved;
  topic.saved = !topic.saved;
  try {
    const result = await paperpilotApi.saveTopic(topic.id);
    topic.saved = result.saved;
  } catch (error) {
    topic.saved = previous;
    toast(error.response?.data?.message || "收藏失败");
  }
}

async function markInterested(topic) {
  if (topic.interested || topic.interestPending) {
    toast("已经记录过了");
    return;
  }
  const previous = Number(topic.likes || 0);
  const previousInterested = Boolean(topic.interested);
  topic.interestPending = true;
  topic.interested = true;
  try {
    const result = await paperpilotApi.markTopicInterested(topic.id);
    topic.likes = result.likes ?? topic.likes;
    topic.interested = Boolean(result.interested ?? true);
    toast("已记录为匿名想做");
  } catch (error) {
    topic.likes = previous;
    topic.interested = previousInterested;
    toast(error.response?.data?.message || "操作失败");
  } finally {
    topic.interestPending = false;
  }
}

function openPaperSource(paper) {
  const url = paperSourceUrl(paper);
  if (url) {
    window.location.assign(url);
    return;
  }
  router.push({ path: "/search", query: { q: paper?.title || "" } });
  toast("未找到来源链接，已转到站内检索");
}

async function exportOutline(topic, target) {
  try {
    await paperpilotApi.exportTopicOutline(topic.id, target);
    toast(target === "meeting" ? "已生成组会汇报提纲" : "已生成综述提纲");
  } catch (error) {
    toast(error.response?.data?.message || "生成提纲失败");
  }
}

function visibleTags(topic) {
  return (topic.tags || []).slice(0, 3);
}

function visibleSubtopics(topic) {
  return detailSubtopics(topic).slice(0, 3);
}

function hiddenTagCount(topic) {
  return Math.max(0, (topic.tags || []).length - 3);
}

function firstCluster(topic) {
  return (topic.themeClusters || [])[0] || (topic.tags || [])[0] || "专题调研";
}

function topicProviderLabel(topic) {
  if (topic?.providerLabel) return topic.providerLabel;
  const source = String(topic?.source || "");
  const modelName = String(topic?.modelName || "");
  if (source.includes("官方") || source.includes("daily-frontier") || modelName === "seed") return "官方";
  return "匿名用户提供";
}

function detailSubtopics(topic) {
  const items = Array.isArray(topic?.subtopics) ? topic.subtopics : [];
  if (items.length) return items.slice(0, 5);
  const names = [...(topic?.themeClusters || []), ...(topic?.tags || [])].filter(Boolean);
  return names.slice(0, 5).map(name => ({
    name,
    analysis: `从“${name}”切入，可以把大方向收窄到一个可检索、可复现的小问题。`,
    papers: (topic?.papers || []).slice(0, 2),
  }));
}

function analysisBlocks(value) {
  const text = String(value || "").trim();
  if (!text) return [{ label: "摘要", text: "该推荐方向还缺少足够分析，需要补充真实文献和数据条件后继续判断。" }];
  const parts = text
    .split(/(?=【[^】]+】)/g)
    .map(item => item.trim())
    .filter(Boolean)
    .map(item => {
      const match = item.match(/^【([^】]+)】\s*([\s\S]*)$/);
      if (match) return { label: match[1], text: match[2].trim() };
      return { label: "分析", text: item };
    });
  return parts.length ? parts : [{ label: "分析", text }];
}

function directionReportBlocks(item, topic) {
  const rawBlocks = analysisBlocks(item?.analysis);
  const alias = {
    "局限/风险": "局限",
    "潜在论文场景": "潜在论文",
    "代表文献": "代表论文",
  };
  const map = new Map();
  rawBlocks.forEach(block => {
    const label = alias[block.label] || block.label;
    if (!map.has(label)) map.set(label, block.text);
  });
  const papers = subtopicPapers(item, topic);
  const paperText = papers.length
    ? papers.map(paper => `《${paper.title}》（${paperSourceMeta(paper)}）`).join("；")
    : "候选文献不足，需要继续检索英文关键词、近三年综述和公开数据来源。";
  const fallback = {
    "摘要": `“${item?.name || "当前方向"}”需要先限定研究对象、数据来源和评价指标，再判断是否适合继续开题或投稿。`,
    "具体方法": detailText(topic, "method"),
    "发文现状": papers.length
      ? `当前代表论文主要来自 ${papers.map(paper => paper.source || paper.verifiedBy || "academic-search").filter(Boolean).slice(0, 3).join("、")}；建议按年份、数据集、方法和指标整理发文矩阵，再判断是热点延伸还是应用补洞。`
      : "当前方向还缺少足够真实来源，建议扩大英文关键词后再判断发文热度。",
    "优势": "这个方向如果能拿到可复现数据，容易形成清楚的问题边界、方法对照和可解释指标，后续也能自然进入综述、组会汇报和论文计划。",
    "局限": detailText(topic, "risk"),
    "潜在论文": `可围绕“${item?.name || topic?.title || "当前方向"}”写成小论文：先提出任务缺口，再给出数据、方法、实验和失败边界，避免只做泛泛综述。`,
    "代表论文": paperText,
  };
  const order = ["摘要", "具体方法", "发文现状", "优势", "局限", "潜在论文", "代表论文"];
  return order.map(label => ({
    key: label === "代表论文" ? "paper-block" : "",
    label,
    text: map.get(label) || fallback[label],
  }));
}

function blockPoints(value) {
  const text = String(value || "").replace(/\s+/g, " ").trim();
  if (!text) return [];
  const bulletParts = text
    .split(/[；;。]\s*|(?=·)|(?=•)|(?=^\d+[.、])/g)
    .map(item => item.replace(/^[·•\-\s]+/, "").trim())
    .filter(item => item.length > 3);
  if (bulletParts.length >= 2) return bulletParts.slice(0, 6);
  if (text.length <= 72) return [text];
  const chunks = [];
  let rest = text;
  while (rest.length > 0 && chunks.length < 5) {
    const slice = rest.slice(0, 66);
    const cut = Math.max(slice.lastIndexOf("，"), slice.lastIndexOf("、"), slice.lastIndexOf(" "));
    const end = cut > 24 ? cut + 1 : Math.min(66, rest.length);
    chunks.push(rest.slice(0, end).trim());
    rest = rest.slice(end).trim();
  }
  return chunks.filter(Boolean);
}

function directionScore(item, topic, index) {
  const explicit = Number(item?.recommendationScore || item?.score || item?.recommendation);
  if (Number.isFinite(explicit) && explicit > 0) return Math.max(1, Math.min(99, Math.round(explicit)));
  const feasibility = Number(topic?.feasibility) || 72;
  const innovation = Number(topic?.innovation) || 72;
  const paperBoost = Math.min(8, subtopicPapers(item, topic).length * 3);
  return Math.max(68, Math.min(96, Math.round(feasibility * 0.48 + innovation * 0.34 + paperBoost + 10 - index * 3)));
}

function subtopicPapers(item, topic) {
  const allPapers = Array.isArray(topic?.papers) ? topic.papers : [];
  if (Array.isArray(item?.papers)) {
    return item.papers
      .filter(Boolean)
      .map(paper => normalizeSubtopicPaper(paper, allPapers))
      .filter(paper => paper.title)
      .slice(0, 3);
  }
  if (typeof item?.papers === "string") {
    return item.papers
      .split(/[,，]/)
      .map(value => normalizeSubtopicPaper(value.trim(), allPapers))
      .filter(paper => paper.title)
      .slice(0, 3);
  }
  return [];
}

function normalizeSubtopicPaper(paper, allPapers) {
  if (!paper) return {};
  const title = typeof paper === "string" ? paper : paper.title;
  const match = allPapers.find(candidate => normalizeTitle(candidate.title) === normalizeTitle(title));
  return {
    ...(match || {}),
    ...(typeof paper === "object" ? paper : { title }),
  };
}

function normalizeTitle(title) {
  return String(title || "").toLowerCase().replace(/[^\p{L}\p{N}]+/gu, " ").trim();
}

function paperSourceMeta(paper) {
  const source = paper.source || paper.verifiedBy || "来源待补";
  const year = paper.year || paper.publishYear || "年份待补";
  const provider = paper.verifiedBy && paper.verifiedBy !== source ? ` · ${paper.verifiedBy}` : "";
  return `${source} · ${year}${provider}`;
}

function paperSourceUrl(paper) {
  const raw = String(paper?.url || paper?.sourceUrl || paper?.doi || "").trim();
  if (!raw) return "";
  if (/^https?:\/\//i.test(raw)) return raw;
  if (/^10\.\d{4,9}\//.test(raw)) return `https://doi.org/${raw}`;
  return "";
}

function evidenceLabel(topic) {
  const count = Number(topic?.evidenceCount || (topic?.papers || []).length || 0);
  const sources = (topic?.searchSources || []).filter(Boolean);
  if (count > 0 && sources.length > 0) return `${count} 篇真实候选 · ${sources.join(" / ")}`;
  if (count > 0) return `${count} 篇真实候选`;
  return "等待真实候选";
}

function detailText(topic, key) {
  const value = topic?.[key];
  if (value && String(value).trim()) return value;
  const title = topic?.title || "当前方向";
  const papers = topic?.papers || [];
  if (key === "question") return `围绕“${title}”，先确认代表文献共同回答的问题，再收窄成一个能被数据或案例验证的研究问题。`;
  if (key === "gap") return papers.length
    ? `已找到 ${papers.length} 篇真实候选文献，下一步要比较它们在数据来源、应用场景、评价指标和边界条件上的差异。`
    : "候选文献不足，建议增加英文关键词、具体对象或数据来源后重新调研。";
  if (key === "method") return "先用开放索引建立代表论文池，再做主题聚类、方法对照和数据可得性判断，最后形成可开题的小问题。";
  return "主要风险是题目过宽、真实数据不足或代表论文不稳定；需要在查看来源并核验 PDF 后继续补充。";
}

function visualClass(topic) {
  const value = `${topic.discipline || ""}${topic.title || ""}`;
  if (value.includes("医学") || value.includes("影像")) return "visual-cyan";
  if (value.includes("Mamba") || value.includes("目标")) return "visual-indigo";
  if (value.includes("材料") || value.includes("经济") || value.includes("管理")) return "visual-amber";
  if (value.includes("心理") || value.includes("教育")) return "visual-rose";
  return "visual-green";
}

function visualPointStyle(topic, index) {
  const seed = Array.from(`${topic?.title || ""}${index}`).reduce((sum, char) => sum + char.charCodeAt(0), 0);
  const left = 15 + ((seed * 17 + index * 11) % 70);
  const top = 24 + ((seed * 13 + index * 7) % 52);
  const size = 5 + (seed % 4);
  return {
    left: `${left}%`,
    top: `${top}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${index * 90}ms`,
  };
}

function toast(message) {
  toastMessage.value = message;
  setTimeout(() => {
    if (toastMessage.value === message) toastMessage.value = "";
  }, 1800);
}
</script>

<style scoped>
.topic-square-page {
  min-height: 100vh;
  padding: 24px 0 64px;
  color: #172033;
  background: #f4f7fb;
}

.square-toolbar {
  position: sticky;
  top: 86px;
  z-index: 20;
  display: grid;
  grid-template-columns: auto minmax(260px, 1fr) auto minmax(160px, 220px) auto auto;
  gap: 14px;
  align-items: center;
  width: min(1480px, calc(100% - 48px));
  margin: 0 auto 24px;
  padding: 10px;
  border: 1px solid rgba(23, 32, 51, .1);
  border-radius: 14px;
  background: rgba(255, 255, 255, .92);
  backdrop-filter: blur(14px);
}

.toolbar-tabs,
.toolbar-segment {
  display: flex;
  gap: 4px;
  padding: 4px;
  border-radius: 10px;
  background: #eef3fa;
}

button,
select,
input,
textarea {
  font: inherit;
}

button {
  border: 0;
  cursor: pointer;
}

button:disabled {
  cursor: not-allowed;
  opacity: .62;
}

.toolbar-tabs button,
.toolbar-segment button {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 8px;
  color: #66758c;
  background: transparent;
  font-weight: 850;
  transition: background 160ms ease, color 160ms ease, transform 160ms ease;
}

.toolbar-tabs button.active,
.toolbar-segment button.active {
  color: #111827;
  background: #fff;
  box-shadow: 0 6px 14px rgba(23, 32, 51, .08);
}

.toolbar-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  min-height: 46px;
  overflow: hidden;
  border: 1px solid #d9e2ef;
  border-radius: 10px;
  background: #fff;
}

.toolbar-search span {
  padding: 0 14px;
  color: #7b8798;
  font-size: 13px;
  font-weight: 900;
}

.toolbar-search input,
.toolbar-select,
.generator-form input,
.generator-form select,
.generator-form textarea {
  width: 100%;
  border: 1px solid #d9e2ef;
  outline: none;
  color: #172033;
  background: #fff;
}

.toolbar-search input {
  min-height: 44px;
  border: 0;
}

.toolbar-select {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 10px;
  font-weight: 800;
}

.toolbar-primary,
.toolbar-admin-generate,
.square-status-row button,
.download-btn,
.topic-detail footer .primary {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 9px;
  color: #fff;
  background: #23863a;
  font-weight: 900;
  transition: transform 160ms ease, filter 160ms ease;
}

.toolbar-primary:hover,
.toolbar-admin-generate:hover,
.square-status-row button:hover,
.download-btn:hover,
.topic-detail footer button:hover {
  transform: translateY(-1px);
  filter: brightness(1.04);
}

.toolbar-admin-generate {
  color: #0f3b2e;
  background: #dcfce7;
  box-shadow: inset 0 0 0 1px rgba(34, 197, 94, .26);
}

.square-status-row {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  width: min(1480px, calc(100% - 48px));
  margin: 0 auto 18px;
}

.square-status-row div {
  display: grid;
  gap: 5px;
}

.square-status-row strong {
  font-size: 24px;
}

.square-status-row span {
  color: #66758c;
  font-weight: 700;
}

.square-status-row button {
  color: #172033;
  background: #fff;
  box-shadow: inset 0 0 0 1px #d9e2ef;
}

.topic-board {
  width: min(1480px, calc(100% - 48px));
  display: grid;
  grid-template-columns: repeat(3, minmax(330px, 1fr));
  gap: 24px;
  margin: 0 auto;
}

.topic-board.loading {
  opacity: .78;
}

.topic-card {
  min-height: 438px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(23, 32, 51, .12);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 30px rgba(23, 32, 51, .06);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.topic-card:hover {
  transform: translateY(-3px);
  border-color: rgba(37, 99, 235, .36);
  box-shadow: 0 18px 36px rgba(23, 32, 51, .1);
}

.topic-visual {
  position: relative;
  height: 136px;
  flex: 0 0 136px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 78% 24%, rgba(255, 255, 255, .24), transparent 22%),
    linear-gradient(135deg, #053b31, #0d7b65);
}

.visual-cyan {
  background:
    radial-gradient(circle at 78% 24%, rgba(255, 255, 255, .22), transparent 22%),
    linear-gradient(135deg, #07344a, #0e8ea8);
}

.visual-indigo {
  background:
    radial-gradient(circle at 78% 24%, rgba(255, 255, 255, .22), transparent 22%),
    linear-gradient(135deg, #1b1b5b, #4f46e5);
}

.visual-amber {
  background:
    radial-gradient(circle at 78% 24%, rgba(255, 255, 255, .24), transparent 22%),
    linear-gradient(135deg, #493209, #bd7a12);
}

.visual-rose {
  background:
    radial-gradient(circle at 78% 24%, rgba(255, 255, 255, .22), transparent 22%),
    linear-gradient(135deg, #4a1024, #be345d);
}

.visual-green {
  background:
    radial-gradient(circle at 78% 24%, rgba(255, 255, 255, .22), transparent 22%),
    linear-gradient(135deg, #073a2e, #128060);
}

.topic-visual::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, .1) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, .1) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, .86), rgba(0, 0, 0, .24));
}

.provider-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 3;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 11px;
  border: 1px solid rgba(255, 255, 255, .5);
  border-radius: 999px;
  color: #eaf4ff;
  background: rgba(15, 23, 42, .28);
  backdrop-filter: blur(8px);
  font-size: 12px;
  font-weight: 900;
}

.provider-badge.official {
  color: #064e3b;
  background: #ecfdf5;
  border-color: rgba(236, 253, 245, .75);
}

.topic-admin-delete {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 4;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  color: #7f1d1d;
  background: rgba(255, 255, 255, .92);
  box-shadow: 0 4px 8px rgba(23, 32, 51, .12);
  font-size: 12px;
  font-weight: 900;
  transition: transform 160ms ease, background 160ms ease, color 160ms ease;
}

.topic-admin-delete:hover:not(:disabled) {
  transform: translateY(-1px);
  color: #fff;
  background: #dc2626;
}

.visual-system {
  position: absolute;
  inset: 18px 26px 42px;
  z-index: 1;
}

.visual-system::before,
.visual-system::after {
  content: "";
  position: absolute;
  inset: 8px 12%;
  border: 1px solid rgba(255, 255, 255, .32);
  border-radius: 999px;
  transform: rotate(-9deg);
}

.visual-system::after {
  inset: 14px 20%;
  opacity: .58;
  transform: rotate(13deg);
}

.visual-system .axis {
  position: absolute;
  background: rgba(255, 255, 255, .28);
}

.visual-system .axis-x {
  left: 6%;
  right: 6%;
  top: 50%;
  height: 1px;
}

.visual-system .axis-y {
  top: 5%;
  bottom: 5%;
  left: 50%;
  width: 1px;
}

.visual-system span {
  position: absolute;
  z-index: 2;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 0 0 5px rgba(255, 255, 255, .13);
  animation: pulsePoint 2.6s ease-in-out infinite;
}

@keyframes pulsePoint {
  0%, 100% { transform: scale(.82); opacity: .7; }
  50% { transform: scale(1.16); opacity: 1; }
}

.visual-caption {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 14px;
  z-index: 2;
  display: grid;
  gap: 4px;
}

.visual-caption strong {
  font-size: 18px;
}

.visual-caption span {
  opacity: .78;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topic-body {
  flex: 1;
  padding: 20px 22px 8px;
}

.topic-body h2 {
  min-height: 56px;
  margin: 0 0 10px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-size: 20px;
  line-height: 1.38;
  letter-spacing: 0;
}

.topic-body p {
  min-height: 54px;
  margin: 0 0 14px;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: #66758c;
  line-height: 1.7;
  font-weight: 680;
}

.topic-subtopics {
  min-height: 34px;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 6px;
  margin-bottom: 12px;
}

.topic-subtopics span {
  max-width: 100%;
  display: inline-flex;
  align-items: center;
  overflow: hidden;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  color: #1f3a5f;
  background: #f4f8ff;
  font-size: 12px;
  font-weight: 850;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-tags {
  min-height: 30px;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 6px;
}

.topic-tags span {
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid #b7ead2;
  border-radius: 999px;
  color: #047857;
  background: #ecfdf5;
  font-size: 12px;
  font-weight: 850;
}

.topic-meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  padding: 12px 22px 18px;
  color: #7b8798;
  font-weight: 750;
}

.topic-meta div {
  display: grid;
  grid-template-columns: auto auto;
  justify-content: flex-end;
  gap: 8px;
}

.topic-meta > span {
  min-width: 0;
  font-size: 13px;
}

.topic-meta button,
.topic-detail footer button {
  min-height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  color: #59677b;
  background: #eef3fa;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.topic-meta .download-btn {
  color: #fff;
  background: #23863a;
}

.empty-state {
  grid-column: 1 / -1;
  min-height: 280px;
  display: grid;
  place-items: center;
  gap: 10px;
  padding: 40px;
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
  color: #66758c;
  background: #fff;
  text-align: center;
}

.empty-state strong {
  color: #172033;
  font-size: 22px;
}

.empty-state button {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 8px;
  color: #fff;
  background: #2563eb;
  font-weight: 900;
}

.modal-backdrop {
  position: fixed;
  inset: 74px 0 0;
  z-index: 80;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: auto;
  padding: 22px 24px 28px;
  background: rgba(15, 23, 42, .46);
}

.topic-detail,
.generator-modal {
  position: relative;
  width: min(980px, 100%);
  max-height: calc(100vh - 120px);
  overflow: auto;
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, .22);
}

.generator-modal {
  width: min(1120px, 100%);
}

.modal-close {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  background: #eef3fa;
  font-size: 24px;
  font-weight: 800;
}

.topic-detail header,
.generator-modal header {
  max-width: 820px;
  padding-right: 48px;
}

.topic-detail header span,
.generator-modal header span {
  color: #2563eb;
  font-weight: 950;
}

.topic-detail h2,
.generator-modal h2 {
  margin: 10px 0 12px;
  font-size: 34px;
  line-height: 1.16;
  letter-spacing: 0;
}

.topic-detail header p {
  color: #66758c;
  line-height: 1.75;
  font-weight: 700;
}

.score-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 22px 0;
}

.score-strip article {
  display: flex;
  justify-content: space-between;
  padding: 16px;
  border-radius: 12px;
  background: #f4f8ff;
}

.score-strip span {
  color: #66758c;
  font-weight: 850;
}

.score-strip strong {
  font-size: 26px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-grid article,
.paper-panel,
.subtopic-panel {
  padding: 18px;
  border: 1px solid #dbe5f3;
  border-radius: 14px;
  background: #fbfdff;
}

.detail-grid h3,
.paper-panel h3,
.subtopic-panel h3 {
  margin: 0 0 10px;
}

.detail-grid p,
.paper-panel p,
.subtopic-panel p {
  margin: 0;
  color: #59677b;
  line-height: 1.75;
  font-weight: 680;
}

.paper-panel,
.subtopic-panel {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.subtopic-panel .direction-report {
  display: grid;
  gap: 15px;
  padding: 18px 20px;
  border: 1px solid rgba(15, 118, 110, .22);
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(240, 253, 250, .78), rgba(255, 255, 255, .98) 210px),
    #fff;
}

.subtopic-title-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5edf6;
}

.subtopic-title-row > div {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.subtopic-title-row small {
  color: #0f766e;
  font-size: 12px;
  font-weight: 950;
}

.subtopic-title-row strong {
  color: #172033;
  font-size: 18px;
  line-height: 1.35;
  text-wrap: balance;
}

.subtopic-title-row span {
  flex: 0 0 auto;
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 11px;
  border-radius: 999px;
  color: #92400e;
  background: #fff7df;
  border: 1px solid #fde68a;
  font-size: 12px;
  font-weight: 950;
}

.subtopic-analysis,
.direction-report-body {
  display: grid;
  gap: 0;
}

.direction-report-block {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 14px;
  padding: 13px 0;
  border-bottom: 1px dashed rgba(15, 23, 42, .12);
  background: transparent;
}

.direction-report-block:first-child {
  padding-top: 2px;
}

.direction-report-block:last-child {
  border-bottom: 0;
}

.subtopic-analysis b {
  position: sticky;
  top: 10px;
  align-self: start;
  color: #0f766e;
  font-size: 13px;
  font-weight: 950;
  line-height: 1.5;
}

.subtopic-analysis b::before {
  content: "";
  display: inline-block;
  width: 7px;
  height: 7px;
  margin-right: 8px;
  border-radius: 999px;
  background: currentColor;
  vertical-align: 1px;
}

.subtopic-analysis ul {
  min-width: 0;
  display: grid;
  gap: 5px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.subtopic-analysis li {
  position: relative;
  min-width: 0;
  max-width: 78ch;
  color: #405169;
  line-height: 1.68;
  font-size: 13px;
  font-weight: 690;
  text-wrap: pretty;
}

.subtopic-analysis li::before {
  content: "·";
  margin-right: 6px;
  color: #0f766e;
  font-weight: 950;
}

.direction-report-block.paper-block {
  padding: 12px 14px;
  border: 1px solid rgba(15, 118, 110, .16);
  border-radius: 14px;
  background: #f7fffd;
}

.subtopic-papers {
  display: grid;
  gap: 7px;
}

.subtopic-paper-row,
.represent-paper-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 9px 10px 9px 12px;
  border: 1px solid #d7efe9;
  border-radius: 12px;
  background: linear-gradient(135deg, #f7fffd 0%, #ffffff 78%);
}

.subtopic-paper-row > div,
.represent-paper-row > div {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.subtopic-paper-row b,
.represent-paper-row strong {
  overflow: hidden;
  color: #0f2b3b;
  font-size: 12px;
  font-weight: 880;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subtopic-paper-row span,
.subtopic-paper-row a,
.represent-paper-row span,
.represent-paper-row a {
  color: #64748b;
  font-size: 12px;
  font-weight: 760;
}

.subtopic-paper-row a,
.represent-paper-row a {
  color: #0f766e;
  text-decoration: none;
}

.paper-source-link {
  width: fit-content;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid #b7ead2;
  border-radius: 8px;
  color: #0f766e;
  background: #ecfdf5;
  font-size: 12px;
  font-weight: 900;
}

.represent-paper-row p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  font-weight: 680;
}

.represent-paper-row button {
  min-height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: #14804f;
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.represent-paper-row button:hover {
  background: #0f6a43;
  transform: translateY(-1px);
}

.paper-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.paper-panel-head h3 {
  margin: 0;
}

.paper-panel-head span {
  min-height: 28px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  color: #0f766e;
  background: #ccfbf1;
  font-size: 13px;
  font-weight: 900;
}

.paper-panel div {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 10px;
  background: #fff;
}

.paper-panel span {
  color: #66758c;
  font-weight: 750;
}

.paper-panel a {
  width: fit-content;
  color: #2563eb;
  font-weight: 900;
  text-decoration: none;
}

.paper-empty {
  padding: 14px;
  border-radius: 10px;
  color: #59677b;
  background: #f8fbff;
}

.topic-detail footer {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.generator-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.generator-form label {
  display: grid;
  gap: 8px;
  color: #475569;
  font-weight: 900;
}

.generator-form .full,
.chip-field {
  grid-column: 1 / -1;
}

.generator-form label:nth-of-type(1),
.generator-form label:nth-of-type(2) {
  grid-column: span 2;
}

.generator-form input,
.generator-form select,
.generator-form textarea {
  min-height: 46px;
  padding: 0 14px;
  border-radius: 10px;
}

.generator-form textarea {
  min-height: 120px;
  padding-top: 12px;
  resize: vertical;
}

.chip-field {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  padding: 12px;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  background: #f8fbff;
}

.chip-field legend {
  padding: 0 6px;
  color: #475569;
  font-weight: 900;
}

.chip-field button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #d5e0ee;
  border-radius: 999px;
  color: #526176;
  background: #fff;
  font-size: 13px;
  font-weight: 850;
}

.chip-field button.active {
  color: #065f46;
  border-color: #86efac;
  background: #ecfdf5;
}

.research-progress {
  display: grid;
  gap: 12px;
  margin-top: 24px;
}

.research-progress-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid #dbe5f3;
  border-radius: 14px;
  background: #f8fbff;
}

.research-progress-head strong {
  color: #172033;
  font-size: 18px;
}

.research-progress-head span {
  color: #2563eb;
  font-weight: 950;
}

.research-progress div {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 0 14px;
  border: 1px solid #dbe5f3;
  border-radius: 12px;
  color: #66758c;
  background: #f8fbff;
  font-weight: 900;
}

.research-progress div.active {
  border-color: #86efac;
  color: #047857;
  background: #ecfdf5;
}

.research-progress i {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  color: #fff;
  background: #2563eb;
  font-style: normal;
  font-size: 13px;
}

.research-progress p {
  color: #66758c;
  font-weight: 750;
}

.topic-toast {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 100;
  padding: 13px 16px;
  border-radius: 12px;
  color: #fff;
  background: #172033;
  font-weight: 900;
  box-shadow: 0 18px 40px rgba(15, 23, 42, .22);
}

.skeleton-card {
  min-height: 438px;
  background: linear-gradient(90deg, #eef3fa, #fff, #eef3fa);
  background-size: 220% 100%;
  animation: shimmer 1.2s linear infinite;
}

@keyframes shimmer {
  to { background-position: -220% 0; }
}

@media (max-width: 1180px) {
  .square-toolbar {
    grid-template-columns: 1fr 1fr;
  }

  .toolbar-search,
  .toolbar-select {
    min-width: 0;
  }

  .topic-board {
    grid-template-columns: repeat(2, minmax(300px, 1fr));
  }
}

@media (max-width: 720px) {
  .topic-square-page {
    padding-top: 18px;
  }

  .square-toolbar,
  .square-status-row,
  .topic-board {
    width: calc(100% - 28px);
  }

  .square-toolbar {
    position: static;
    grid-template-columns: 1fr;
  }

  .square-status-row {
    align-items: stretch;
    flex-direction: column;
  }

  .topic-board {
    grid-template-columns: 1fr;
  }

  .topic-meta div {
    grid-template-columns: 1fr;
  }

  .topic-meta button {
    width: 100%;
  }

  .topic-detail,
  .generator-modal {
    max-height: calc(100vh - 112px);
    padding: 22px;
  }

  .topic-detail h2,
  .generator-modal h2 {
    font-size: 26px;
  }

  .score-strip,
  .detail-grid,
  .generator-form {
    grid-template-columns: 1fr;
  }

  .generator-form label,
  .generator-form label:nth-of-type(1),
  .generator-form label:nth-of-type(2) {
    grid-column: 1 / -1;
  }

  .direction-report-block {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: .01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: .01ms !important;
  }
}
</style>
