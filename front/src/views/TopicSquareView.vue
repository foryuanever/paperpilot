<template>
  <div class="topic-square-page spatial-page">

    <!-- Ambient atmosphere -->
    <div class="tsq-orb tsq-orb-a"></div>
    <div class="tsq-orb tsq-orb-b"></div>

    <!-- ───────────────── Page Header ───────────────── -->
    <div class="tsq-page-head" data-reveal>
      <div class="tsq-page-title">
        <h1>选题广场</h1>
        <p>{{ filteredTopics.length }} / {{ topics.length }} 个研究方向 · AI deep-research 驱动</p>
      </div>
      <button type="button" class="tsq-btn tsq-btn-primary" @click="openGenerator">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        发起调研
      </button>
      <button v-if="isAdmin" type="button" class="tsq-btn tsq-btn-ghost" :disabled="adminGenerating" @click="generateOfficialHotTopics">
        {{ adminGenerating ? "AI 思考中…" : "AI 生成官方热点" }}
      </button>
    </div>

    <!-- ───────────────── Filter Bar ───────────────── -->
    <div class="tsq-filter-bar" data-reveal>
      <!-- Tab switcher -->
      <div class="tsq-tabs">
        <button type="button" :class="['tsq-tab', { active: !savedOnly }]" @click="setSavedOnly(false)">全部选题</button>
        <button type="button" :class="['tsq-tab', { active: savedOnly }]" @click="setSavedOnly(true)">我的收藏</button>
      </div>

      <!-- Search -->
      <label class="tsq-search">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input v-model.trim="filters.keyword" type="search" placeholder="搜索选题、方向或关键词…" @keyup.enter="loadTopics" />
      </label>

      <!-- Sort -->
      <div class="tsq-sort">
        <button
          v-for="item in sortTabs" :key="item.value"
          type="button"
          :class="['tsq-sort-btn', { active: filters.sort === item.value }]"
          @click="setSort(item.value)"
        >{{ item.label }}</button>
      </div>

      <!-- Refresh -->
      <button type="button" class="tsq-refresh" :disabled="loading || adminGenerating" @click="loadTopics" :title="loading ? '刷新中' : '刷新'">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" :class="{ 'tsq-spin': loading }"><path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2"/></svg>
      </button>
    </div>

    <!-- ───────────────── Card Grid ───────────────── -->
    <main class="tsq-grid" :class="{ 'tsq-loading': loading }">

      <!-- Skeleton -->
      <div v-if="loading && !topics.length" v-for="n in 6" :key="n" class="tsq-skeleton"></div>

      <!-- Cards -->
      <article
        v-for="topic in filteredTopics"
        :key="topic.id"
        class="tsq-card"
        :class="{ 'tsq-card-admin': isAdmin }"
        @click="selectedTopic = topic"
      >
        <!-- Hero Section -->
        <div class="tsq-card-hero">
          <div class="tsq-hero-badges">
            <span class="tsq-provider-badge" :class="{ official: topicProviderLabel(topic) === '官方', personal: isPersonalTopic(topic) }">
              <svg v-if="isPersonalTopic(topic)" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></svg>
              {{ topicProviderLabel(topic) }}
            </span>
            <span v-if="isHotTopic(topic)" class="tsq-hot-badge">热</span>
            <button
              v-if="isAdmin"
              type="button"
              class="tsq-admin-del"
              :disabled="isDeletingTopic(topic)"
              @click.stop="deleteTopicAsAdmin(topic)"
            >{{ isDeletingTopic(topic) ? "删除中" : "删除" }}</button>
          </div>
          <div class="tsq-hero-content">
            <h3 class="tsq-hero-title">{{ topic.title }}</h3>
            <p class="tsq-hero-subtitle">{{ cardSubtitle(topic) }}</p>
          </div>
        </div>

        <!-- Body -->
        <div class="tsq-card-body">
          <h2 class="tsq-body-title">{{ topic.title }}：{{ topic.goal }}</h2>
          <p class="tsq-body-summary">{{ topic.summary }}</p>

          <!-- Tag chips -->
          <div v-if="visibleTags(topic).length" class="tsq-chips new-green-chips">
            <span v-for="tag in visibleTags(topic)" :key="tag">{{ tag }}</span>
            <span v-if="hiddenTagCount(topic) > 0" class="tsq-chip-more">+{{ hiddenTagCount(topic) }}</span>
          </div>

          <div class="tsq-publish-date">发布于 {{ topic.updatedAt || topic.createdAt || "2026-06-30" }}</div>
        </div>

        <!-- Footer -->
        <footer class="tsq-card-footer new-footer">
          <div class="tsq-stats-group">
            <button
              type="button"
              class="tsq-stat-action tsq-interest-meter"
              :class="{ active: topic.interested }"
              :disabled="topic.interestPending"
              @click.stop="markInterested(topic)"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="3"/><path d="M12 3v3M12 18v3M3 12h3M18 12h3"/></svg>
              {{ topic.likes || 0 }} 人想做
            </button>
          </div>
          <div class="tsq-footer-actions" style="display: flex; gap: 8px;">
            <button type="button" class="tsq-action-btn tsq-wish-btn" @click.stop="toggleSave(topic)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
              {{ topic.saved ? "已收藏" : "收藏" }}
            </button>
            <button
              type="button"
              class="tsq-action-btn tsq-download-btn"
              :disabled="topic.interestPending"
              @click.stop="markInterested(topic)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              {{ topic.interested ? "取消想做" : topic.interestPending ? "记录中" : "想做" }}
            </button>
          </div>
        </footer>
      </article>

      <!-- Empty state -->
      <div v-if="!loading && !filteredTopics.length" class="tsq-empty">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" opacity="0.4"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <strong>暂无匹配的选题</strong>
        <span>{{ topics.length ? "试试换一个标签，或清空筛选条件。" : "换一个关键词，或发起一次 deep-research 调研。" }}</span>
        <button type="button" class="tsq-btn tsq-btn-primary" @click="openGenerator">发起调研</button>
      </div>
    </main>

    <!-- ───────────────── Detail Modal ───────────────── -->
    <Transition name="tsq-modal">
      <div v-if="selectedTopic" class="tsq-modal-backdrop" @click.self="selectedTopic = null">
        <section class="tsq-detail">
          <button type="button" class="tsq-modal-close" @click="selectedTopic = null">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>

          <!-- Hero -->
          <header class="tsq-detail-hero">
            <div class="tsq-detail-meta">
              <span class="tsq-detail-discipline">{{ selectedTopic.discipline }}</span>
              <span class="tsq-detail-dot">·</span>
              <span>{{ selectedTopic.stage }}</span>
              <span class="tsq-detail-dot">·</span>
              <span>{{ selectedTopic.goal }}</span>
            </div>
            <h2>{{ selectedTopic.title }}</h2>
            <p>{{ selectedTopic.summary }}</p>
          </header>

          <!-- Score Row -->
          <div class="tsq-score-row">
            <div class="tsq-score-item">
              <span>可行性</span>
              <strong>{{ selectedTopic.feasibility }}</strong>
            </div>
            <div class="tsq-score-item">
              <span>创新度</span>
              <strong>{{ selectedTopic.innovation }}</strong>
            </div>
            <div class="tsq-score-item">
              <span>难度</span>
              <strong>{{ selectedTopic.difficulty }}</strong>
            </div>
          </div>

          <!-- Detail grid -->
          <div class="tsq-detail-grid">
            <div class="tsq-detail-block">
              <h3>研究问题</h3>
              <p>{{ detailText(selectedTopic, "question") }}</p>
            </div>
            <div class="tsq-detail-block">
              <h3>研究空白</h3>
              <p>{{ detailText(selectedTopic, "gap") }}</p>
            </div>
            <div class="tsq-detail-block">
              <h3>方法路线</h3>
              <p>{{ detailText(selectedTopic, "method") }}</p>
            </div>
            <div class="tsq-detail-block">
              <h3>风险提醒</h3>
              <p>{{ detailText(selectedTopic, "risk") }}</p>
            </div>
          </div>

          <!-- Subtopic directions -->
          <section class="tsq-directions">
            <div class="tsq-directions-head">
              <h3>推荐研究方向</h3>
              <span>每个方向含完整调研结构</span>
            </div>
            <article
              v-for="(item, index) in detailSubtopics(selectedTopic)"
              :key="item.name"
              class="tsq-direction-card"
            >
              <div class="tsq-direction-header">
                <div>
                  <small>方向 {{ String(index + 1).padStart(2, "0") }}</small>
                  <strong>{{ item.name }}</strong>
                </div>
                <span class="tsq-direction-score">推荐度 {{ directionScore(item, selectedTopic, index) }}</span>
              </div>

              <div class="tsq-direction-blocks">
                <div
                  v-for="block in directionReportBlocks(item, selectedTopic)"
                  :key="block.label"
                  :class="['tsq-direction-block', block.key]"
                >
                  <b>{{ block.label }}</b>
                  <ul v-if="blockHasHierarchy(block.text)">
                    <li v-for="point in blockPoints(block.text)" :key="point">{{ point }}</li>
                  </ul>
                  <p v-else>{{ blockPoints(block.text)[0] }}</p>
                </div>
              </div>

              <div v-if="subtopicPapers(item, selectedTopic).length" class="tsq-direction-papers">
                <div v-for="paper in subtopicPapers(item, selectedTopic)" :key="paper.title" class="tsq-paper-row">
                  <div class="tsq-paper-info">
                    <b>{{ paper.title }}</b>
                    <span>{{ paperSourceMeta(paper) }}</span>
                  </div>
                  <button v-if="paperSourceUrl(paper)" type="button" class="tsq-paper-link" @click.stop="openPaperSource(paper)">查看来源</button>
                </div>
              </div>
            </article>
          </section>

          <!-- Detail footer -->
          <footer class="tsq-detail-footer">
            <button type="button" class="tsq-btn tsq-btn-ghost" :disabled="selectedTopic.interestPending" @click="markInterested(selectedTopic)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              {{ selectedTopic.interested ? "取消想做" : "想做这个方向" }}
            </button>
            <button type="button" class="tsq-btn tsq-btn-primary" @click="toggleSave(selectedTopic)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"/></svg>
              {{ selectedTopic.saved ? "已复用收藏" : "复用到我的收藏" }}
            </button>
          </footer>
        </section>
      </div>
    </Transition>

    <!-- ───────────────── Generator Modal ───────────────── -->
    <Transition name="tsq-modal">
      <div v-if="showGenerator" class="tsq-modal-backdrop" @click.self="closeGenerator">
        <section class="tsq-generator">
          <button type="button" class="tsq-modal-close" @click="closeGenerator">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>

          <header class="tsq-gen-header">
            <span class="tsq-gen-label">deep-research</span>
            <h2>生成一个可继续推进的选题</h2>
          </header>

          <form v-if="!generating" class="tsq-gen-form" @submit.prevent="generateTopic">
            <label class="tsq-field tsq-field-full">
              <span>研究方向 <em>*</em></span>
              <input v-model.trim="generatorForm.direction" required placeholder="例如：低资源场景下的多模态医学影像分析" />
            </label>
            <label class="tsq-field tsq-field-full">
              <span>研究方向大类 <em>*</em></span>
              <input v-model.trim="generatorForm.discipline" required list="discipline-presets" placeholder="例如：医学影像 / 药物发现 / 教育技术" />
              <datalist id="discipline-presets">
                <option v-for="item in disciplinePresets" :key="item" :value="item" />
              </datalist>
            </label>
            <div class="tsq-field-row">
              <label class="tsq-field">
                <span>学历阶段</span>
                <select v-model="generatorForm.stage">
                  <option v-for="item in stages" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>目标用途</span>
                <select v-model="generatorForm.goal">
                  <option v-for="item in goals" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>资源条件</span>
                <select v-model="generatorForm.resource">
                  <option v-for="item in resources" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>数据来源</span>
                <select v-model="generatorForm.dataAccess">
                  <option v-for="item in dataAccessOptions" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>数据形态</span>
                <select v-model="generatorForm.sampleType">
                  <option v-for="item in sampleTypes" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>方法偏好</span>
                <select v-model="generatorForm.methodPreference">
                  <option v-for="item in methodOptions" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>选题尺度</span>
                <select v-model="generatorForm.topicScale">
                  <option v-for="item in topicScales" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
              <label class="tsq-field">
                <span>期望贡献</span>
                <select v-model="generatorForm.expectedContribution">
                  <option v-for="item in contributionOptions" :key="item" :value="item">{{ item }}</option>
                </select>
              </label>
            </div>

            <fieldset class="tsq-constraint-field">
              <legend>重点约束</legend>
              <div class="tsq-constraint-chips">
                <button
                  v-for="item in constraintOptions"
                  :key="item"
                  type="button"
                  :class="['tsq-constraint-chip', { active: generatorForm.constraints.includes(item) }]"
                  @click="toggleConstraint(item)"
                >{{ item }}</button>
              </div>
            </fieldset>

            <label class="tsq-field">
              <span>英文关键词</span>
              <input v-model.trim="generatorForm.keywords" placeholder="例如：few-shot segmentation, foundation model" />
            </label>
            <label class="tsq-field">
              <span>避开路线</span>
              <input v-model.trim="generatorForm.avoidRoutes" placeholder="例如：不做纯综述、不做模型堆叠" />
            </label>
            <label class="tsq-field tsq-field-full">
              <span>已读 / 想参考的论文</span>
              <textarea v-model.trim="generatorForm.seedPapers" rows="3" placeholder="可粘贴 1-5 篇论文题名、DOI 或 arXiv 号"></textarea>
            </label>
            <label class="tsq-field tsq-field-full">
              <span>补充说明</span>
              <textarea v-model.trim="generatorForm.note" rows="4" placeholder="专业、可拿到的数据、导师方向、已有论文、希望偏理论/工程/应用等"></textarea>
            </label>

            <label class="tsq-public-switch" :class="{ active: generatorForm.publicShare }">
              <input v-model="generatorForm.publicShare" type="checkbox" />
              <span class="tsq-public-switch-knob"></span>
              <span class="tsq-public-switch-copy">
                <b>{{ generatorForm.publicShare ? "公开提供到选题广场" : "仅个人可见" }}</b>
                <small>{{ generatorForm.publicShare ? "卡片显示为匿名用户提供，其他用户可以想做和收藏。" : "卡片左上角显示个人锁标识，只会出现在你的列表和收藏里。" }}</small>
              </span>
            </label>

            <div class="tsq-gen-submit">
              <button type="submit" class="tsq-btn tsq-btn-primary tsq-btn-lg">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                开始调研
              </button>
            </div>
          </form>

          <!-- Progress -->
          <div v-else class="tsq-progress">
            <div class="tsq-progress-header">
              <strong>{{ generationThinkingTitle }}</strong>
              <span>{{ generationThinkingCopy }}</span>
            </div>
            <div class="tsq-progress-track">
              <div class="tsq-progress-fill" :style="{ width: generationProgressWidth }"></div>
            </div>
            <ol class="tsq-progress-steps">
              <li
                v-for="(step, i) in generationSteps"
                :key="step"
                :class="{ active: i <= generationIndex, current: i === generationIndex }"
              >
                <i>{{ i + 1 }}</i>
                <span>{{ step }}</span>
              </li>
            </ol>
            <p class="tsq-progress-note">系统先扩展检索词，再筛选真实文献，最后按七段结构输出推荐方向与代表论文。</p>
          </div>
        </section>
      </div>
    </Transition>

    <!-- Toast -->
    <Transition name="tsq-toast">
      <div v-if="toastMessage" class="tsq-toast">{{ toastMessage }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { useScrollReveal } from "../composables/useScrollReveal";
useScrollReveal(".topic-square-page");
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
  { label: "想做最多", value: "liked" },
];
const generationSteps = ["读取研究 brief", "拆解对象和数据", "扩展中英文检索词", "检索真实文献", "生成 3-5 个推荐方向", "筛掉泛题和重复方向", "匹配代表论文来源", "写调研报告", "写入我的收藏"];
const generationThinking = [
  ["正在读你的约束", "会把学历阶段、资源条件、避开路线和目标用途一起送进模型，不直接套模板。"],
  ["正在收窄研究对象", "先判断对象、样本、数据来源和可验证指标，防止生成“提升方法”这种泛方向。"],
  ["正在扩展检索词", "会组合中文方向、英文关键词、种子论文和学科词，优先找能支撑开题的小切口。"],
  ["正在查真实来源", "从可用学术来源拉候选论文，后面每个小方向都必须绑定能对上的文献。"],
  ["正在生成小方向", "每个小方向需要写清摘要、具体方法、发文现状、优势、局限、潜在论文和代表论文。"],
  ["正在做质量门检查", "如果段落太短、论文不对应、或者话术太空，会自动要求模型返工。"],
  ["正在匹配文献", "只保留能在检索候选中找到的论文来源，避免把不相关论文塞进推荐方向。"],
  ["正在整理报告", "把模型结果排成可读的分点结构，不让大段文字糊成一团。"],
  ["正在写入收藏", "生成完成后会进入我的收藏，同时在 AI 调用明细中留下真实模型调用记录。"],
];

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
  publicShare: false,
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
const registeredUserCount = ref(0);
let progressTimer = null;

const generationProgressWidth = computed(() => {
  const total = Math.max(1, generationSteps.length - 1);
  return `${Math.round((generationIndex.value / total) * 100)}%`;
});

const generationThinkingTitle = computed(() => generationThinking[generationIndex.value]?.[0] || "正在生成");
const generationThinkingCopy = computed(() => generationThinking[generationIndex.value]?.[1] || "正在等待模型返回结构化结果。");

onMounted(() => {
  loadTopics();
  loadTopicStats();
});
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

async function loadTopicStats() {
  try {
    const stats = await paperpilotApi.getForumStats();
    registeredUserCount.value = Number(stats?.registeredUserCount || 0);
  } catch {
    registeredUserCount.value = 0;
  }
}

async function generateTopic() {
  generating.value = true;
  generationIndex.value = 0;
  const startedAt = Date.now();
  progressTimer = setInterval(() => {
    generationIndex.value = Math.min(generationSteps.length - 1, generationIndex.value + 1);
  }, 2300);
  try {
    const result = await paperpilotApi.generateTopic({ ...generatorForm, maxTopics: 1 });
    const minimumMs = 19000;
    const elapsed = Date.now() - startedAt;
    if (elapsed < minimumMs) {
      await new Promise(resolve => setTimeout(resolve, minimumMs - elapsed));
    }
    const createdTopics = Array.isArray(result) ? result : [result];
    const createdIds = new Set(createdTopics.map(item => item.id));
    generationIndex.value = generationSteps.length - 1;
    await new Promise(resolve => setTimeout(resolve, 650));
    topics.value = [...createdTopics, ...topics.value.filter(item => !createdIds.has(item.id))];
    selectedTopic.value = createdTopics[0] || null;
    savedOnly.value = !generatorForm.publicShare;
    showGenerator.value = false;
    toast(generatorForm.publicShare ? "已公开到选题广场，来源显示为匿名用户提供" : "已生成个人选题，只在你的列表中可见");
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
    const minimumMs = 24000;
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
  if (topic.interestPending) return;
  const previous = Number(topic.likes || 0);
  const previousInterested = Boolean(topic.interested);
  topic.interestPending = true;
  topic.interested = !previousInterested;
  topic.likes = Math.max(0, previous + (topic.interested ? 1 : -1));
  try {
    const result = await paperpilotApi.markTopicInterested(topic.id);
    const nextInterested = Object.prototype.hasOwnProperty.call(result, "interested")
      ? Boolean(result.interested)
      : topic.interested;
    const nextLikes = Number.isFinite(Number(result.likes)) ? Number(result.likes) : topic.likes;
    applyTopicInterestState(topic.id, nextInterested, nextLikes);
    toast(nextInterested ? "已记录为想做" : "已取消想做");
  } catch (error) {
    topic.likes = previous;
    topic.interested = previousInterested;
    toast(error.response?.data?.message || "操作失败");
  } finally {
    topic.interestPending = false;
  }
}

function applyTopicInterestState(topicId, interested, likes) {
  topics.value = topics.value.map(item => {
    if (item.id !== topicId) return item;
    return {
      ...item,
      interested,
      likes,
      interestPending: false,
    };
  });
  if (selectedTopic.value?.id === topicId) {
    selectedTopic.value = {
      ...selectedTopic.value,
      interested,
      likes,
      interestPending: false,
    };
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

function cardSubtitle(topic) {
  const pieces = [
    ...(topic.themeClusters || []),
    ...(topic.tags || []),
    topic?.discipline,
    topic?.stage,
  ]
    .map(item => String(item || "").trim())
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index)
    .slice(0, 3);
  return pieces.length ? pieces.join(" × ") : "真实文献检索 × 方法路径 × 可行性评估";
}

function firstCluster(topic) {
  return (topic.themeClusters || [])[0] || (topic.tags || [])[0] || "专题调研";
}

function topicProviderLabel(topic) {
  if (topic?.providerLabel) return topic.providerLabel;
  const source = String(topic?.source || "");
  const modelName = String(topic?.modelName || "");
  if (source.includes("官方") || source.includes("daily-frontier") || modelName === "seed") return "官方";
  if (source.includes("个人")) return "个人";
  return "匿名用户提供";
}

function isPersonalTopic(topic) {
  return topicProviderLabel(topic) === "个人";
}

function isHotTopic(topic) {
  const registered = Number(registeredUserCount.value || 0);
  if (registered <= 1) return Number(topic?.likes || 0) > 0;
  return Number(topic?.likes || 0) >= Math.ceil(registered / 2);
}

function detailSubtopics(topic) {
  const items = Array.isArray(topic?.subtopics) ? topic.subtopics : [];
  if (items.length) return items.slice(0, 5);
  return [{
    name: "等待重新调研",
    analysis: "【摘要】这张旧选题卡缺少模型返回的结构化小方向，不能当作完整调研结果使用。【具体方法】请点击发起调研或由管理员重新生成官方热点，系统会重新检索真实代表论文并让模型按七段结构输出。【发文现状】旧数据只保留标题和标签，无法判断发文热度、数据条件和论文来源是否匹配。【优势】重新生成后会把研究对象、样本形态、指标和代表论文绑定到每个小方向下。【局限】如果检索不到足够真实论文，系统会提示失败，不再用宽泛模板补齐。【潜在论文】需要先完成一次真实调研后再给出可写题目。【代表论文】暂无匹配论文。",
    papers: [],
    stale: true,
  }];
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
    "摘要": "该段缺少模型返回内容，建议重新发起调研后再使用。",
    "具体方法": "该段缺少模型返回内容，不能自动补成通用方法。",
    "发文现状": papers.length
      ? `当前代表论文主要来自 ${papers.map(paper => paper.source || paper.verifiedBy || "academic-search").filter(Boolean).slice(0, 3).join("、")}；建议按年份、数据集、方法和指标整理发文矩阵，再判断是热点延伸还是应用补洞。`
      : "当前方向还缺少足够真实来源，建议扩大英文关键词后再判断发文热度。",
    "优势": "该段缺少模型返回内容，重新调研后会补充与小方向对应的优势。",
    "局限": "该段缺少模型返回内容，重新调研后会补充真实风险与边界。",
    "潜在论文": "该段缺少模型返回内容，不能据此直接开题。",
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
  const raw = String(value || "").trim();
  const text = raw.replace(/\s+/g, " ").trim();
  if (!text) return [];
  if (blockHasHierarchy(raw)) {
    return raw
      .split(/\n+/)
      .flatMap(line => line.split(/(?=[·•]\s)|(?=\d+[.、]\s*)/g))
      .map(item => item.replace(/^[·•\-\s*]+/, "").replace(/^\d+[.、]\s*/, "").trim())
      .filter(item => item.length > 3)
      .slice(0, 5);
  }
  if (text.length <= 72) return [text];
  const chunks = [];
  let rest = text;
  while (rest.length > 0 && chunks.length < 3) {
    const slice = rest.slice(0, 96);
    const cut = Math.max(slice.lastIndexOf("，"), slice.lastIndexOf("、"), slice.lastIndexOf(" "));
    const end = cut > 36 ? cut + 1 : Math.min(96, rest.length);
    chunks.push(rest.slice(0, end).trim());
    rest = rest.slice(end).trim();
  }
  return [chunks.join("").trim() || text];
}

function blockHasHierarchy(value) {
  const raw = String(value || "").trim();
  if (!raw) return false;
  const lines = raw.split(/\n+/).map(item => item.trim()).filter(Boolean);
  if (lines.length >= 2) return true;
  return /(^|\n)\s*(?:[·•\-*]|\d+[.、])\s+/.test(raw);
}

function directionScore(item, topic, index) {
  const explicit = Number(item?.recommendationScore || item?.score || item?.recommendation);
  if (Number.isFinite(explicit) && explicit > 0) {
    const value = explicit <= 1 ? explicit * 100 : explicit;
    return `${Math.max(1, Math.min(100, Math.round(value)))}%`;
  }
  const feasibility = Number(topic?.feasibility) || 72;
  const innovation = Number(topic?.innovation) || 72;
  const paperBoost = Math.min(8, subtopicPapers(item, topic).length * 3);
  return `${Math.max(68, Math.min(100, Math.round(feasibility * 0.48 + innovation * 0.34 + paperBoost + 10 - index * 3)))}%`;
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

function visualClass(topic, index = 0) {
  const classes = ["visual-indigo", "visual-purple", "visual-emerald", "visual-amber", "visual-rose", "visual-cyan"];
  const idx = typeof index === "number" && !isNaN(index) ? index : 0;
  const colorIdx = (idx * 2 + Math.floor(idx / 3)) % classes.length;
  return classes[colorIdx];
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
/* =========================================================================
   TOPIC SQUARE — Natural · Refined · Dual-theme
   ========================================================================= */

/* ── CSS variables ─────────────────────────────────────── */
.topic-square-page {
  --c-bg:         #f5f6f8;
  --c-surface:    #ffffff;
  --c-border:     rgba(15, 23, 42, 0.08);
  --c-text:       #0f172a;
  --c-muted:      #64748b;
  --c-subtle:     #94a3b8;
  --c-accent:     #6366f1;
  --c-accent2:    #a855f7;
  --c-strip-a:    #0f766e;
  --c-strip-b:    #1e40af;
  --c-strip-c:    #7c3aed;
  --c-strip-d:    #92400e;
  --sh-card:      0 2px 8px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04);
  --sh-float:     0 12px 40px rgba(15,23,42,.12), 0 2px 8px rgba(15,23,42,.06);
  --r-card:       16px;
  --r-sm:         10px;
  --r-pill:       999px;
}

:root[data-theme="dark"] .topic-square-page {
  --c-bg:         #0b0d14;
  --c-surface:    rgba(18, 24, 40, 0.85);
  --c-border:     rgba(255, 255, 255, 0.07);
  --c-text:       #f1f5f9;
  --c-muted:      #94a3b8;
  --c-subtle:     #64748b;
  --sh-card:      0 2px 8px rgba(0,0,0,.3), 0 8px 24px rgba(0,0,0,.24);
  --sh-float:     0 20px 60px rgba(0,0,0,.5);
}

/* ── Page base ─────────────────────────────────────────── */
.topic-square-page {
  position: relative;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  padding: 36px min(48px, 5vw) 100px;
  overflow-x: hidden;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  transition: background 0.3s, color 0.3s;
}

/* ── Ambient orbs ──────────────────────────────────────── */
.tsq-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(80px);
  z-index: 0;
  animation: tsq-float 14s ease-in-out infinite alternate;
}
.tsq-orb-a {
  width: 500px; height: 500px;
  top: -60px; left: -140px;
  background: radial-gradient(circle, rgba(99,102,241,0.16) 0%, transparent 70%);
}
.tsq-orb-b {
  width: 420px; height: 420px;
  top: 400px; right: -100px;
  background: radial-gradient(circle, rgba(168,85,247,0.14) 0%, transparent 70%);
  animation-delay: -7s;
}
@keyframes tsq-float {
  from { transform: translate(0,0) scale(1); }
  to   { transform: translate(24px,-20px) scale(1.06); }
}

/* ── Inner content wrapper ─────────────────────────────── */
.tsq-page-head,
.tsq-filter-bar,
.tsq-grid {
  position: relative;
  z-index: 1;
  max-width: 100%;
  margin-inline: auto;
}

/* ── Page head ─────────────────────────────────────────── */
.tsq-page-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.tsq-page-title { flex: 1; min-width: 0; }
.tsq-page-title h1 {
  margin: 0 0 4px;
  font-size: 26px;
  font-weight: 900;
  letter-spacing: -0.4px;
  color: var(--c-text);
}
.tsq-page-title p {
  margin: 0;
  font-size: 13px;
  color: var(--c-muted);
}

/* ── Buttons ───────────────────────────────────────────── */
.tsq-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: none;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  white-space: nowrap;
}
.tsq-btn-primary {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #ffffff;
  box-shadow: 0 4px 16px rgba(99,102,241,0.32);
}
.tsq-btn-primary:hover { transform: translateY(-1.5px); box-shadow: 0 8px 24px rgba(99,102,241,0.42); }
.tsq-btn:disabled {
  opacity: 0.62;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}
.tsq-btn-ghost {
  background: transparent;
  border: 1px solid var(--c-border);
  color: var(--c-muted);
}
.tsq-btn-ghost:hover { border-color: var(--c-accent); color: var(--c-accent); background: rgba(99,102,241,0.06); }
.tsq-btn-lg { height: 46px; padding: 0 28px; font-size: 15px; }
/* ── Filter bar ────────────────────────────────────────── */
.tsq-filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  margin-bottom: 28px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 14px;
  box-shadow: var(--sh-card);
  backdrop-filter: blur(20px);
  flex-wrap: wrap;
}

/* Tabs */
.tsq-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  background: var(--c-bg);
  border-radius: var(--r-pill);
  flex-shrink: 0;
}
.tsq-tab {
  height: 32px;
  padding: 0 16px;
  border-radius: var(--r-pill);
  border: none;
  background: transparent;
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.tsq-tab.active {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #fff;
  box-shadow: 0 3px 10px rgba(99,102,241,0.28);
}

/* Search */
.tsq-search {
  flex: 1;
  min-width: 200px;
  display: flex;
  align-items: center;
  gap: 9px;
  height: 38px;
  padding: 0 14px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--r-pill);
  color: var(--c-muted);
  transition: border-color 0.2s;
}
.tsq-search:focus-within { border-color: var(--c-accent); }
.tsq-search input {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--c-text);
  font-size: 13px;
  outline: none;
}
.tsq-search input::placeholder { color: var(--c-subtle); }

/* Sort */
.tsq-sort {
  display: inline-flex;
  gap: 2px;
  flex-shrink: 0;
}
.tsq-sort-btn {
  height: 32px;
  padding: 0 14px;
  border-radius: var(--r-pill);
  border: 1px solid transparent;
  background: transparent;
  color: var(--c-muted);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}
.tsq-sort-btn.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: rgba(99,102,241,0.08);
}
.tsq-sort-btn:hover:not(.active) { color: var(--c-text); background: var(--c-bg); }

/* Select */
.tsq-select {
  height: 38px;
  padding: 0 14px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 13px;
  outline: none;
  cursor: pointer;
  flex-shrink: 0;
}

/* Refresh */
.tsq-refresh {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-muted);
  display: grid;
  place-items: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s ease;
}
.tsq-refresh:hover { border-color: var(--c-accent); color: var(--c-accent); transform: rotate(30deg); }
.tsq-spin { animation: tsq-spin 0.8s linear infinite; }
@keyframes tsq-spin { to { transform: rotate(360deg); } }

/* ── Card grid ─────────────────────────────────────────── */
.tsq-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

/* ── Card ──────────────────────────────────────────────── */
.tsq-card {
  display: flex;
  flex-direction: column;
  border-radius: var(--r-card);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--sh-card);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.28s cubic-bezier(0.16,1,0.3,1), box-shadow 0.28s ease, border-color 0.22s ease;
}
.tsq-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--sh-float);
  border-color: rgba(99,102,241,0.28);
}

/* Strip */
.tsq-card-strip {
  position: relative;
  height: 130px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  background: linear-gradient(135deg, var(--c-strip-a), #115e59);
}
.visual-green .tsq-card-strip, .tsq-card-strip.visual-green { background: linear-gradient(135deg, #0f766e, #115e59); }
.visual-blue  .tsq-card-strip, .tsq-card-strip.visual-blue  { background: linear-gradient(135deg, #1d4ed8, #1e40af); }
.visual-purple.tsq-card-strip, .tsq-card-strip.visual-purple { background: linear-gradient(135deg, #7c3aed, #6d28d9); }
.visual-amber .tsq-card-strip, .tsq-card-strip.visual-amber  { background: linear-gradient(135deg, #d97706, #92400e); }
.visual-rose  .tsq-card-strip, .tsq-card-strip.visual-rose   { background: linear-gradient(135deg, #e11d48, #be123c); }
.visual-teal  .tsq-card-strip, .tsq-card-strip.visual-teal   { background: linear-gradient(135deg, #0891b2, #0e7490); }

.tsq-provider-badge {
  align-self: flex-start;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  font-size: 11px;
  font-weight: 800;
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(6px);
  color: rgba(255,255,255,0.92);
  letter-spacing: 0.2px;
}
.tsq-provider-badge.official {
  background: rgba(251,191,36,0.24);
  color: #fde68a;
}

.tsq-admin-del {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  border: none;
  background: rgba(239,68,68,0.2);
  color: #fca5a5;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

/* Data-viz dots */
.tsq-dot-system {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
}
.tsq-axis {
  position: absolute;
  background: rgba(255,255,255,0.35);
}
.tsq-axis-x { bottom: 36px; left: 0; right: 0; height: 1px; }
.tsq-axis-y { top: 0; bottom: 0; left: 50%; width: 1px; }
.tsq-dot-system span {
  position: absolute;
  width: 5px; height: 5px;
  border-radius: 50%;
  background: rgba(255,255,255,0.7);
}

.tsq-strip-caption {
  position: relative;
  z-index: 1;
}
.tsq-strip-caption strong {
  display: block;
  font-size: 15px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0,0,0,0.2);
}
.tsq-strip-caption span {
  font-size: 12px;
  color: rgba(255,255,255,0.76);
}

/* Card body */
.tsq-card-body {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.tsq-card-body h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.45;
  color: var(--c-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tsq-card-body p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--c-muted);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Chips */
.tsq-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tsq-chips-sub span {
  padding: 4px 11px;
  border-radius: var(--r-pill);
  font-size: 11.5px;
  font-weight: 700;
  background: rgba(99,102,241,0.08);
  color: var(--c-accent);
  border: 1px solid rgba(99,102,241,0.15);
}
.tsq-chips-tag span {
  padding: 4px 11px;
  border-radius: var(--r-pill);
  font-size: 11.5px;
  font-weight: 700;
  background: var(--c-bg);
  color: var(--c-muted);
  border: 1px solid var(--c-border);
}
.tsq-chip-more {
  padding: 4px 10px;
  border-radius: var(--r-pill);
  font-size: 11.5px;
  font-weight: 700;
  background: var(--c-bg);
  color: var(--c-subtle);
  border: 1px dashed var(--c-border);
}

/* Card footer */
.tsq-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid var(--c-border);
}
.tsq-meta-time {
  font-size: 11.5px;
  color: var(--c-subtle);
}
.tsq-card-actions { display: flex; gap: 6px; }
.tsq-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-muted);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}
.tsq-action-btn:hover { border-color: var(--c-accent); color: var(--c-accent); background: rgba(99,102,241,0.07); }
.tsq-action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.tsq-action-save {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  border-color: transparent;
  color: #fff !important;
  box-shadow: 0 3px 10px rgba(99,102,241,0.3);
}
.tsq-action-save:hover { transform: translateY(-1px); box-shadow: 0 5px 16px rgba(99,102,241,0.42); border-color: transparent; }

/* Skeleton */
.tsq-skeleton {
  height: 340px;
  border-radius: var(--r-card);
  background: linear-gradient(90deg, var(--c-bg) 25%, var(--c-surface) 50%, var(--c-bg) 75%);
  background-size: 300% 100%;
  animation: tsq-shimmer 1.4s ease infinite;
  border: 1px solid var(--c-border);
}
@keyframes tsq-shimmer { to { background-position: -300% 0; } }

/* Empty state */
.tsq-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 80px 24px;
  text-align: center;
}
.tsq-empty strong { font-size: 18px; color: var(--c-text); }
.tsq-empty span { font-size: 14px; color: var(--c-muted); }

/* ── Modal backdrop ────────────────────────────────────── */
.tsq-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(10,14,28,0.48);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* ── Detail modal ──────────────────────────────────────── */
.tsq-detail {
  position: relative;
  width: min(860px, 100%);
  max-height: 88vh;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 20px;
  box-shadow: var(--sh-float);
  backdrop-filter: blur(24px);
  overflow-y: auto;
  padding: 36px 36px 28px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tsq-modal-close {
  position: absolute;
  top: 18px; right: 18px;
  width: 36px; height: 36px;
  border-radius: 50%;
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-muted);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.18s;
  z-index: 2;
}
.tsq-modal-close:hover { background: rgba(239,68,68,0.1); border-color: rgba(239,68,68,0.3); color: #ef4444; }

.tsq-detail-hero { }
.tsq-detail-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--c-muted);
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.tsq-detail-discipline {
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: rgba(99,102,241,0.1);
  color: var(--c-accent);
  font-weight: 750;
}
.tsq-detail-dot { color: var(--c-border); }
.tsq-detail-hero h2 {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.4;
  color: var(--c-text);
}
.tsq-detail-hero p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--c-muted);
}

.tsq-score-row {
  display: flex;
  gap: 12px;
}
.tsq-score-item {
  flex: 1;
  padding: 14px 18px;
  border-radius: var(--r-sm);
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  text-align: center;
}
.tsq-score-item span {
  display: block;
  font-size: 12px;
  color: var(--c-muted);
  margin-bottom: 6px;
}
.tsq-score-item strong {
  font-size: 22px;
  font-weight: 900;
  color: var(--c-accent);
}

.tsq-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.tsq-detail-block {
  padding: 16px;
  border-radius: var(--r-sm);
  background: var(--c-bg);
  border: 1px solid var(--c-border);
}
.tsq-detail-block h3 {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 800;
  color: var(--c-accent);
}
.tsq-detail-block p {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.68;
  color: var(--c-muted);
}

/* Directions */
.tsq-directions-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}
.tsq-directions-head h3 { margin: 0; font-size: 16px; font-weight: 900; color: var(--c-text); }
.tsq-directions-head span { font-size: 12.5px; color: var(--c-muted); }

.tsq-direction-card {
  padding: 20px;
  border-radius: var(--r-card);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.tsq-direction-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.tsq-direction-header small {
  display: block;
  font-size: 11.5px;
  font-weight: 800;
  color: var(--c-accent);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.tsq-direction-header strong {
  font-size: 16px;
  font-weight: 800;
  color: var(--c-text);
  line-height: 1.4;
}
.tsq-direction-score {
  flex-shrink: 0;
  padding: 4px 12px;
  border-radius: var(--r-pill);
  background: rgba(99,102,241,0.1);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 750;
}

.tsq-direction-blocks {
  display: grid;
  gap: 10px;
}
.tsq-direction-block {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--c-border);
}
.tsq-direction-block:last-child { border-bottom: none; }
.tsq-direction-block b {
  font-size: 12px;
  font-weight: 800;
  color: var(--c-accent);
  align-self: flex-start;
  padding-top: 2px;
}
.tsq-direction-block ul {
  margin: 0; padding: 0; list-style: none;
  display: flex; flex-direction: column; gap: 4px;
}
.tsq-direction-block li {
  font-size: 13px;
  line-height: 1.65;
  color: var(--c-muted);
}
.tsq-direction-block p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--c-muted);
}
.tsq-direction-block li::before {
  content: "·";
  margin-right: 6px;
  color: var(--c-accent);
  font-weight: 900;
}
.tsq-direction-block.paper-block {
  background: rgba(99,102,241,0.05);
  padding: 12px 14px;
  border-radius: var(--r-sm);
  border: 1px solid rgba(99,102,241,0.12);
  grid-template-columns: 1fr;
}
.tsq-direction-block.paper-block b { margin-bottom: 8px; }

.tsq-direction-papers { display: flex; flex-direction: column; gap: 8px; }
.tsq-paper-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--c-border);
  background: var(--c-surface);
}
.tsq-paper-info { flex: 1; min-width: 0; }
.tsq-paper-info b {
  display: block;
  font-size: 13px;
  font-weight: 800;
  color: var(--c-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tsq-paper-info span { font-size: 12px; color: var(--c-muted); }
.tsq-paper-link {
  flex-shrink: 0;
  height: 28px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  border: 1px solid rgba(99,102,241,0.3);
  background: rgba(99,102,241,0.08);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
}

.tsq-detail-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid var(--c-border);
}

/* ── Generator modal ───────────────────────────────────── */
.tsq-generator {
  position: relative;
  width: min(760px, 100%);
  max-height: 90vh;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 20px;
  box-shadow: var(--sh-float);
  backdrop-filter: blur(24px);
  overflow-y: auto;
  padding: 36px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tsq-gen-header { }
.tsq-gen-label {
  display: inline-block;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: rgba(99,102,241,0.1);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: 10px;
}
.tsq-gen-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
  color: var(--c-text);
}
.tsq-gen-form { display: flex; flex-direction: column; gap: 14px; }

.tsq-public-switch {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-radius: var(--r-sm);
  border: 1px solid var(--c-border);
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(14,165,233,0.05));
  cursor: pointer;
}
.tsq-public-switch input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
.tsq-public-switch-knob {
  width: 44px;
  height: 24px;
  border-radius: 999px;
  background: rgba(100,116,139,0.28);
  border: 1px solid var(--c-border);
  position: relative;
  transition: all 0.2s ease;
}
.tsq-public-switch-knob::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(15,23,42,0.28);
  transition: transform 0.2s ease;
}
.tsq-public-switch.active {
  border-color: rgba(56,189,248,0.36);
  background: linear-gradient(135deg, rgba(14,165,233,0.12), rgba(99,102,241,0.09));
}
.tsq-public-switch.active .tsq-public-switch-knob {
  background: linear-gradient(135deg, #38bdf8, #6366f1);
  border-color: transparent;
}
.tsq-public-switch.active .tsq-public-switch-knob::after {
  transform: translateX(20px);
}
.tsq-public-switch-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.tsq-public-switch-copy b {
  color: var(--c-text);
  font-size: 13.5px;
}
.tsq-public-switch-copy small {
  color: var(--c-muted);
  font-size: 12.5px;
  line-height: 1.5;
}

.tsq-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.tsq-field span {
  font-size: 12.5px;
  font-weight: 750;
  color: var(--c-muted);
}
.tsq-field em { color: var(--c-accent); font-style: normal; }
.tsq-field input,
.tsq-field select,
.tsq-field textarea {
  width: 100%;
  padding: 10px 14px;
  border-radius: var(--r-sm);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.tsq-field input:focus,
.tsq-field select:focus,
.tsq-field textarea:focus { border-color: var(--c-accent); }
.tsq-field-full { width: 100%; }
.tsq-field-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.tsq-constraint-field {
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  padding: 14px;
  background: var(--c-bg);
}
.tsq-constraint-field legend {
  padding: 0 8px;
  font-size: 12.5px;
  font-weight: 750;
  color: var(--c-muted);
}
.tsq-constraint-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.tsq-constraint-chip {
  padding: 5px 12px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-muted);
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s;
}
.tsq-constraint-chip.active {
  border-color: var(--c-accent);
  background: rgba(99,102,241,0.1);
  color: var(--c-accent);
}

.tsq-gen-submit {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}

/* Progress */
.tsq-progress { display: flex; flex-direction: column; gap: 18px; }
.tsq-progress-header strong { display: block; font-size: 16px; font-weight: 900; color: var(--c-text); margin-bottom: 4px; }
.tsq-progress-header span { font-size: 13.5px; color: var(--c-muted); }
.tsq-progress-track {
  height: 5px;
  border-radius: 99px;
  background: var(--c-border);
  overflow: hidden;
}
.tsq-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--c-accent), var(--c-accent2));
  border-radius: 99px;
  transition: width 0.5s ease;
}
.tsq-progress-steps {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 8px;
}
.tsq-progress-steps li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--r-sm);
  border: 1px solid var(--c-border);
  background: var(--c-bg);
  font-size: 12.5px;
  color: var(--c-subtle);
  transition: all 0.3s;
}
.tsq-progress-steps li.active { color: var(--c-text); background: rgba(99,102,241,0.06); border-color: rgba(99,102,241,0.15); }
.tsq-progress-steps li.current { border-color: var(--c-accent); color: var(--c-accent); background: rgba(99,102,241,0.1); }
.tsq-progress-steps li i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px; height: 20px;
  border-radius: 50%;
  background: var(--c-border);
  font-size: 11px;
  font-weight: 900;
  font-style: normal;
  color: var(--c-muted);
  flex-shrink: 0;
}
.tsq-progress-steps li.active i { background: rgba(99,102,241,0.15); color: var(--c-accent); }
.tsq-progress-steps li.current i { background: var(--c-accent); color: #fff; }
.tsq-progress-note { font-size: 12.5px; color: var(--c-subtle); line-height: 1.7; margin: 0; }

/* ── Toast ─────────────────────────────────────────────── */
.tsq-toast {
  position: fixed;
  bottom: 28px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 300;
  padding: 12px 24px;
  border-radius: var(--r-pill);
  background: #1e293b;
  color: #f8fafc;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 8px 30px rgba(0,0,0,0.25);
  white-space: nowrap;
}
:root[data-theme="dark"] .tsq-toast { background: #334155; }

/* ── Modal transitions ─────────────────────────────────── */
.tsq-modal-enter-active { transition: all 0.3s cubic-bezier(0.16,1,0.3,1); }
.tsq-modal-leave-active { transition: all 0.2s ease; }
.tsq-modal-enter-from, .tsq-modal-leave-to {
  opacity: 0;
}
.tsq-modal-enter-from .tsq-detail,
.tsq-modal-enter-from .tsq-generator {
  transform: scale(0.95) translateY(12px);
}

.tsq-toast-enter-active { transition: all 0.3s cubic-bezier(0.16,1,0.3,1); }
.tsq-toast-leave-active { transition: all 0.2s ease; }
.tsq-toast-enter-from, .tsq-toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(12px); }

/* ── Responsive ────────────────────────────────────────── */
@media (max-width: 900px) {
  .tsq-grid { grid-template-columns: repeat(2, 1fr); }
  .tsq-detail-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .topic-square-page { padding: 20px 16px 80px; }
  .tsq-grid { grid-template-columns: 1fr; }
  .tsq-filter-bar { gap: 8px; }
  .tsq-score-row { flex-direction: column; }
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: .01ms !important;
    transition-duration: .01ms !important;
  }
}


/* ════════════════════════════════════════════════════════════
   ALTERNATING VIBRANT CARD THEMES & HIGH-END CARD REDESIGN
   ════════════════════════════════════════════════════════════ */

.tsq-card {
  border-radius: 20px !important;
  overflow: hidden !important;
  transition: all 0.28s cubic-bezier(0.16, 1, 0.3, 1) !important;
  cursor: pointer !important;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04) !important;
}

.tsq-card:hover {
  transform: translateY(-5px) scale(1.01) !important;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.12) !important;
}

:root[data-theme="dark"] .tsq-card:hover {
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.5) !important;
}

/* Theme 1: Cyan / Blue Tint (科技蓝) */
:root[data-theme="light"] .tsq-grid > .tsq-card:nth-child(5n+1) {
  background: linear-gradient(145deg, #f0f7ff 0%, #e0f2fe 100%) !important;
  border: 1px solid rgba(186, 230, 253, 0.9) !important;
}
:root[data-theme="dark"] .tsq-grid > .tsq-card:nth-child(5n+1) {
  background: linear-gradient(145deg, #0f172a 0%, #1e293b 100%) !important;
  border: 1px solid rgba(56, 189, 248, 0.25) !important;
}

/* Theme 2: Purple / Violet Tint (极光紫) */
:root[data-theme="light"] .tsq-grid > .tsq-card:nth-child(5n+2) {
  background: linear-gradient(145deg, #faf5ff 0%, #f3e8ff 100%) !important;
  border: 1px solid rgba(233, 213, 255, 0.9) !important;
}
:root[data-theme="dark"] .tsq-grid > .tsq-card:nth-child(5n+2) {
  background: linear-gradient(145deg, #18122b 0%, #271b44 100%) !important;
  border: 1px solid rgba(192, 132, 252, 0.25) !important;
}

/* Theme 3: Emerald / Mint Tint (翡翠绿) */
:root[data-theme="light"] .tsq-grid > .tsq-card:nth-child(5n+3) {
  background: linear-gradient(145deg, #f0fdf4 0%, #dcfce7 100%) !important;
  border: 1px solid rgba(187, 247, 208, 0.9) !important;
}
:root[data-theme="dark"] .tsq-grid > .tsq-card:nth-child(5n+3) {
  background: linear-gradient(145deg, #0d1f1a 0%, #143027 100%) !important;
  border: 1px solid rgba(52, 211, 153, 0.25) !important;
}

/* Theme 4: Amber / Gold Tint (琥珀金) */
:root[data-theme="light"] .tsq-grid > .tsq-card:nth-child(5n+4) {
  background: linear-gradient(145deg, #fffbe0 0%, #fef3c7 100%) !important;
  border: 1px solid rgba(253, 230, 138, 0.9) !important;
}
:root[data-theme="dark"] .tsq-grid > .tsq-card:nth-child(5n+4) {
  background: linear-gradient(145deg, #23180c 0%, #382611 100%) !important;
  border: 1px solid rgba(251, 191, 36, 0.25) !important;
}

/* Theme 5: Rose / Sunset Tint (晚霞粉) */
:root[data-theme="light"] .tsq-grid > .tsq-card:nth-child(5n+0) {
  background: linear-gradient(145deg, #fff1f2 0%, #ffe4e6 100%) !important;
  border: 1px solid rgba(254, 205, 211, 0.9) !important;
}
:root[data-theme="dark"] .tsq-grid > .tsq-card:nth-child(5n+0) {
  background: linear-gradient(145deg, #241018 0%, #3a1724 100%) !important;
  border: 1px solid rgba(251, 113, 133, 0.25) !important;
}

/* Card Body & Text Polish */
.tsq-card-body h2 {
  font-size: 16.5px !important;
  font-weight: 900 !important;
  line-height: 1.4 !important;
  margin-bottom: 8px !important;
}

:root[data-theme="light"] .tsq-card-body h2 {
  color: #0f172a !important;
}
:root[data-theme="dark"] .tsq-card-body h2 {
  color: #f8fafc !important;
}

.tsq-card-body p {
  font-size: 13px !important;
  line-height: 1.6 !important;
}

:root[data-theme="light"] .tsq-card-body p {
  color: #475569 !important;
}
:root[data-theme="dark"] .tsq-card-body p {
  color: #94a3b8 !important;
}

/* Chips Polish */
.tsq-chips span {
  border-radius: 999px !important;
  font-size: 11px !important;
  font-weight: 750 !important;
  padding: 3px 10px !important;
}



/* ════════════════════════════════════════════════════════════
   VIBRANT DISTINCT HEADER STRIPS FOR ADJACENT TOPIC CARDS
   ════════════════════════════════════════════════════════════ */

.tsq-card-strip {
  height: 120px !important;
  position: relative !important;
  padding: 14px 16px !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: space-between !important;
  overflow: hidden !important;
  transition: all 0.25s ease !important;
}

/* 6 High-End Distinct Gradients for Header Strips */
.visual-indigo.tsq-card-strip, .tsq-card-strip.visual-indigo {
  background: linear-gradient(135deg, #4f46e5 0%, #3730a3 100%) !important;
}

.visual-purple.tsq-card-strip, .tsq-card-strip.visual-purple {
  background: linear-gradient(135deg, #9333ea 0%, #6b21a8 100%) !important;
}

.visual-emerald.tsq-card-strip, .tsq-card-strip.visual-emerald {
  background: linear-gradient(135deg, #059669 0%, #064e3b 100%) !important;
}

.visual-amber.tsq-card-strip, .tsq-card-strip.visual-amber {
  background: linear-gradient(135deg, #d97706 0%, #78350f 100%) !important;
}

.visual-rose.tsq-card-strip, .tsq-card-strip.visual-rose {
  background: linear-gradient(135deg, #e11d48 0%, #881337 100%) !important;
}

.visual-cyan.tsq-card-strip, .tsq-card-strip.visual-cyan {
  background: linear-gradient(135deg, #0891b2 0%, #164e63 100%) !important;
}

/* Badge & Caption Polish */
.tsq-provider-badge {
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(8px) !important;
  color: #ffffff !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  font-size: 11px !important;
  font-weight: 850 !important;
  padding: 3px 10px !important;
  border-radius: 999px !important;
  width: fit-content !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
}

.tsq-strip-caption strong {
  color: #ffffff !important;
  font-size: 15px !important;
  font-weight: 900 !important;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3) !important;
  display: block !important;
}

.tsq-strip-caption span {
  color: rgba(255, 255, 255, 0.82) !important;
  font-size: 11.5px !important;
  font-weight: 700 !important;
  display: block !important;
  margin-top: 2px !important;
}



/* ════════════════════════════════════════════════════════════
   SOFT ELEGANT PASTEL HEADER STRIPS (ZERO NEIGHBOR COLLISION)
   ════════════════════════════════════════════════════════════ */

.tsq-card-strip {
  height: 115px !important;
  position: relative !important;
  padding: 14px 16px !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: space-between !important;
  overflow: hidden !important;
}

/* Light Mode: Soft Elegant Pastel Tint Gradients with Crisp Dark Text */
:root[data-theme="light"] .visual-indigo.tsq-card-strip {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%) !important;
}
:root[data-theme="light"] .visual-purple.tsq-card-strip {
  background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%) !important;
}

:root[data-theme="dark"] .tsq-card-body p {
  color: #94a3b8 !important;
}

.tsq-chips span {
  padding: 3px 10px !important;
  border-radius: 999px !important;
  font-size: 11px !important;
  font-weight: 750 !important;
}

:root[data-theme="light"] .tsq-chips span {
  background: #ecfdf3 !important;
  color: #067647 !important;
  border: 1px solid #bbf7d0 !important;
}
:root[data-theme="dark"] .tsq-chips span {
  background: rgba(16, 185, 129, 0.1) !important;
  color: #34d399 !important;
  border: 1px solid rgba(16, 185, 129, 0.2) !important;
}

/* Remove ambient atmosphere background */
.tsq-orb {
  display: none !important;
}

.tsq-card-footer {
  border-top: 1px solid rgba(255, 255, 255, 0.06) !important;
  padding-top: 14px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
}

:root[data-theme="light"] .tsq-card-footer {
  border-top: 1px solid #f1f5f9 !important;
}

.tsq-meta-time {
  font-size: 11.5px !important;
}

:root[data-theme="light"] .tsq-meta-time {
  color: #94a3b8 !important;
}
:root[data-theme="dark"] .tsq-meta-time {
  color: #64748b !important;
}

.tsq-action-btn {
  height: 28px !important;
  padding: 0 12px !important;
  border-radius: 999px !important;
  font-size: 11.5px !important;
  font-weight: 800 !important;
  cursor: pointer !important;
}

:root[data-theme="light"] .tsq-action-btn {
  background: #ffffff !important;
  color: #475569 !important;
  border: 1px solid #e2e8f0 !important;
}
:root[data-theme="light"] .tsq-action-btn:hover {
  background: linear-gradient(135deg, #6366f1, #a855f7) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}

:root[data-theme="dark"] .tsq-action-btn {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #94a3b8 !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}
:root[data-theme="dark"] .tsq-action-btn:hover {
  background: linear-gradient(135deg, #6366f1, #a855f7) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}
/* ----------------- New Masterpiece Card Design ----------------- */
.tsq-card {
  position: relative !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: rgba(18, 18, 22, 0.85) !important; 
  backdrop-filter: blur(24px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(24px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.06) !important;
  display: flex !important;
  flex-direction: column !important;
  border-radius: 20px !important;
  box-shadow: 0 10px 30px rgba(0,0,0,0.3) !important;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

.tsq-card:hover {
  transform: translateY(-8px) scale(1.02) !important;
  border-color: rgba(99, 102, 241, 0.4) !important;
  box-shadow: 0 20px 40px -10px rgba(99, 102, 241, 0.2), 
              0 0 24px rgba(236, 72, 153, 0.1) !important;
}

/* Ambient Animated Glows behind the card contents */
.tsq-card::before {
  content: "";
  position: absolute;
  top: -20%; left: -10%; width: 120%; height: 60%;
  background-image: 
    radial-gradient(ellipse at 20% 40%, rgba(99, 102, 241, 0.25), transparent 50%),
    radial-gradient(ellipse at 80% 60%, rgba(236, 72, 153, 0.15), transparent 50%);
  z-index: 0;
  pointer-events: none;
  opacity: 0.8;
  transition: opacity 0.5s ease;
}

.tsq-card:hover::before {
  opacity: 1;
  background-image: 
    radial-gradient(ellipse at 30% 50%, rgba(99, 102, 241, 0.35), transparent 60%),
    radial-gradient(ellipse at 70% 50%, rgba(236, 72, 153, 0.25), transparent 60%);
}

/* Abstract Tech Dot Matrix Pattern overlay */
.tsq-card::after {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0; height: 180px;
  background-image: radial-gradient(rgba(255, 255, 255, 0.15) 1px, transparent 1px);
  background-size: 16px 16px;
  opacity: 0.6;
  z-index: 0;
  pointer-events: none;
  mask-image: linear-gradient(to bottom, rgba(0,0,0,1) 0%, rgba(0,0,0,0) 100%);
  -webkit-mask-image: linear-gradient(to bottom, rgba(0,0,0,1) 0%, rgba(0,0,0,0) 100%);
}

.tsq-card-hero {
  position: relative;
  z-index: 1;
  background: transparent !important;
  padding: 24px 24px 0;
  min-height: auto;
  display: flex;
  flex-direction: column;
}

.tsq-hero-badges {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.tsq-provider-badge {
  background: linear-gradient(135deg, rgba(255,255,255,0.1), rgba(255,255,255,0.02)) !important;
  color: #e2e8f0 !important;
  border-radius: 999px !important;
  padding: 4px 12px !important;
  font-size: 11.5px !important;
  font-weight: 800 !important;
  border: 1px solid rgba(255,255,255,0.15) !important;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
  backdrop-filter: blur(10px) !important;
  display: inline-flex !important;
  align-items: center !important;
  gap: 5px !important;
}
.tsq-provider-badge.personal {
  background: rgba(15,23,42,0.42) !important;
  color: #cbd5e1 !important;
  border-color: rgba(203,213,225,0.22) !important;
}
.tsq-hot-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(248,113,113,0.96), rgba(245,158,11,0.92));
  color: #fff7ed;
  border: 1px solid rgba(254,215,170,0.45);
  font-size: 11.5px;
  font-weight: 900;
  letter-spacing: 0.08em;
  box-shadow: 0 8px 18px rgba(248,113,113,0.22);
}

.tsq-hero-content {
  color: #fff;
}

.tsq-hero-title {
  font-size: 20px !important;
  font-weight: 900 !important;
  margin: 0 0 8px !important;
  background: linear-gradient(to right, #fff, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.tsq-hero-subtitle {
  font-size: 12.5px !important;
  color: #cbd5e1 !important;
  margin: 0 !important;
  font-weight: 600 !important;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
}
.tsq-hero-subtitle::before {
  content: "";
  display: inline-block;
  width: 6px; height: 6px;
  background: #38bdf8;
  border-radius: 50%;
  margin-right: 8px;
  box-shadow: 0 0 8px #38bdf8;
}

.tsq-card-body {
  position: relative;
  z-index: 1;
  padding: 24px 24px 0 !important;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.tsq-body-title {
  font-size: 17px !important;
  font-weight: 800 !important;
  color: #f8fafc !important;
  margin: 0 0 14px !important;
  line-height: 1.5 !important;
}

.tsq-body-summary {
  font-size: 14px !important;
  color: #94a3b8 !important;
  line-height: 1.65 !important;
  margin: 0 0 24px !important;
  display: -webkit-box !important;
  -webkit-line-clamp: 3 !important;
  -webkit-box-orient: vertical !important;
  overflow: hidden !important;
}

.new-green-chips {
  gap: 8px !important;
  display: flex !important;
  flex-wrap: wrap !important;
}

.new-green-chips span {
  background: rgba(56, 189, 248, 0.08) !important;
  color: #38bdf8 !important;
  border: 1px solid rgba(56, 189, 248, 0.25) !important;
  border-radius: 999px !important;
  padding: 4px 10px !important;
  font-size: 11.5px !important;
  font-weight: 700 !important;
  transition: all 0.2s ease !important;
  white-space: nowrap !important;
}
.tsq-card:hover .new-green-chips span {
  background: rgba(56, 189, 248, 0.15) !important;
  border-color: rgba(56, 189, 248, 0.4) !important;
}

.tsq-chip-more {
  background: rgba(255,255,255,0.06) !important;
  color: #94a3b8 !important;
  border-color: transparent !important;
}

.tsq-publish-date {
  font-size: 13px !important;
  color: #475569 !important;
  margin-top: 20px !important;
  margin-bottom: 24px !important;
  font-weight: 600 !important;
}

.tsq-card-footer.new-footer {
  position: relative;
  z-index: 1;
  padding: 0 24px 24px !important;
  display: flex !important;
  justify-content: space-between !important;
  align-items: center !important;
  border-top: none !important;
  background: transparent !important;
}

.tsq-stats-group {
  display: flex;
  gap: 8px;
  color: #64748b;
  font-size: 14px;
  min-width: 0;
}

.tsq-stat-item,
.tsq-stat-action {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tsq-stat-action {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(56,189,248,0.18);
  background: rgba(8, 47, 73, 0.22);
  color: #a5f3fc;
  font-size: 12.5px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
  white-space: nowrap;
}
.tsq-stat-action:hover {
  transform: translateY(-1px);
  border-color: rgba(56,189,248,0.46);
  color: #ecfeff;
  background: rgba(8,145,178,0.16);
}
.tsq-stat-action.active {
  border-color: rgba(129,140,248,0.58);
  color: #eef2ff;
  background: linear-gradient(135deg, rgba(79,70,229,0.34), rgba(14,165,233,0.18));
  box-shadow: 0 8px 20px rgba(79,70,229,0.2);
}
.tsq-stat-action:disabled {
  cursor: default;
  opacity: 0.86;
}

.tsq-wish-btn, .tsq-download-btn {
  font-weight: 800 !important;
  font-size: 13.5px !important;
  border-radius: 10px !important;
  padding: 8px 18px !important;
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
  cursor: pointer !important;
  transition: all 0.2s ease;
  white-space: nowrap !important;
  flex-shrink: 0 !important;
}

.tsq-wish-btn {
  background: rgba(255,255,255,0.05) !important;
  color: #cbd5e1 !important;
  border: 1px solid rgba(148,163,184,0.2) !important;
}

.tsq-download-btn {
  background: linear-gradient(135deg, #4f46e5, #0891b2) !important;
  color: #ffffff !important;
  border: none !important;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.28) !important;
}
.tsq-download-btn svg {
  transition: transform .18s ease;
}
.tsq-download-btn:hover svg {
  transform: scale(1.08);
}
.tsq-download-btn:disabled {
  background: linear-gradient(135deg, #64748b, #475569) !important;
  box-shadow: none !important;
  cursor: default !important;
  opacity: 0.82;
}

.tsq-wish-btn:hover {
  background: rgba(255,255,255,0.1) !important;
  transform: translateY(-2px);
}

.tsq-download-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(79, 70, 229, 0.38) !important;
}

/* ---------------- Force Overrides for Perfect Light / Dark Adaptation ---------------- */

/* Dark Mode Masterpiece */
:root[data-theme="dark"] .tsq-grid > .tsq-card:nth-child(n) {
  background: rgba(18, 18, 22, 0.85) !important;
  border: 1px solid rgba(255,255,255,0.06) !important;
}
:root[data-theme="dark"] .tsq-card-body h2.tsq-body-title {
  color: #f8fafc !important;
}
:root[data-theme="dark"] .tsq-card-body p.tsq-body-summary {
  color: #94a3b8 !important;
}

/* Light Mode Glassmorphism */
:root[data-theme="light"] .tsq-grid > .tsq-card:nth-child(n) {
  background: rgba(255, 255, 255, 0.85) !important;
  border: 1px solid rgba(15, 23, 42, 0.08) !important;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05) !important;
}
:root[data-theme="light"] .tsq-card-body h2.tsq-body-title {
  color: #0f172a !important;
}
:root[data-theme="light"] .tsq-card-body p.tsq-body-summary {
  color: #475569 !important;
}
:root[data-theme="light"] .tsq-publish-date {
  color: #94a3b8 !important;
}
:root[data-theme="light"] .tsq-stats-group {
  color: #94a3b8 !important;
}
:root[data-theme="light"] .tsq-stat-action {
  background: #f8fbff;
  border-color: #dbeafe;
  color: #2563eb;
}
:root[data-theme="light"] .tsq-stat-action:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}
:root[data-theme="light"] .tsq-stat-action.active {
  background: linear-gradient(135deg, #eef2ff, #ecfeff);
  border-color: #818cf8;
  color: #3730a3;
  box-shadow: 0 8px 18px rgba(79,70,229,.14);
}
:root[data-theme="light"] .tsq-action-btn.tsq-wish-btn {
  background: #f1f5f9 !important;
  color: #475569 !important;
  border: 1px solid #e2e8f0 !important;
}
:root[data-theme="light"] .tsq-action-btn.tsq-wish-btn:hover {
  background: #e2e8f0 !important;
  color: #0f172a !important;
}
:root[data-theme="light"] .tsq-action-btn.tsq-download-btn {
  background: linear-gradient(135deg, #4f46e5, #0891b2) !important;
  color: #ffffff !important;
  border: none !important;
}
:root[data-theme="light"] .tsq-hero-subtitle {
  color: #475569 !important;
}
:root[data-theme="light"] .tsq-hero-title {
  background: none !important;
  color: #0f172a !important;
  -webkit-text-fill-color: #0f172a !important;
}
:root[data-theme="light"] .tsq-provider-badge {
  background: rgba(15, 23, 42, 0.04) !important;
  color: #334155 !important;
  border-color: rgba(15, 23, 42, 0.15) !important;
  box-shadow: none !important;
}
:root[data-theme="light"] .tsq-provider-badge.personal {
  background: #f8fafc !important;
  color: #475569 !important;
  border-color: #cbd5e1 !important;
}
:root[data-theme="light"] .tsq-card::before {
  opacity: 0.4 !important;
}

.tsq-card-strip {
  display: none !important;
}

/* Base override for layout to force 4 columns */
.tsq-grid {
  grid-template-columns: repeat(4, 1fr) !important;
}
@media (max-width: 1500px) {
  .tsq-grid {
    grid-template-columns: repeat(3, 1fr) !important;
  }
}
@media (max-width: 1100px) {
  .tsq-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}
@media (max-width: 768px) {
  .tsq-grid {
    grid-template-columns: 1fr !important;
  }
  .tsq-card-footer.new-footer {
    align-items: stretch !important;
    flex-direction: column;
    gap: 12px;
  }
  .tsq-stats-group,
  .tsq-footer-actions {
    width: 100%;
    flex-wrap: wrap;
  }
  .tsq-stat-action,
  .tsq-action-btn {
    flex: 1;
    justify-content: center;
  }
}
</style>
