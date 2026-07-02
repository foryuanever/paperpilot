<template>
  <div class="research-community">
    <section class="community-hero">
      <div class="notice-heading">
        <span class="notice-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 3 3.8 6.5v5.3c0 4.8 3.4 8 8.2 9.2 4.8-1.2 8.2-4.4 8.2-9.2V6.5L12 3Z"></path>
            <path d="M12 8v4.5M12 16h.01"></path>
          </svg>
        </span>
        <div>
          <span class="eyebrow">COMMUNITY NOTICE</span>
          <h1>站内发帖须知</h1>
          <p>共同维护真实、专业、可信的学术交流环境</p>
        </div>
      </div>
      <div class="notice-rules">
        <div><span>01</span><p><strong>尊重学术真实</strong>不得伪造论文、数据、成果或身份信息</p></div>
        <div><span>02</span><p><strong>保持内容相关</strong>不可发布与科研学习无关或无实际意义的帖子</p></div>
        <div><span>03</span><p><strong>遵守内容规范</strong>严禁暴力、色情、违法及攻击性内容</p></div>
        <div><span>04</span><p><strong>谨慎私下交易</strong>任何交易均与本站无关，违规内容发现即封号</p></div>
      </div>
      <button class="hero-publish-button" @click="openCreateModal">
        <span class="plus-icon">+</span>
        我已知晓，去发帖
      </button>
    </section>

    <section class="module-strip">
      <button
        v-for="module in postModules"
        :key="module.value"
        class="module-card"
        :class="[module.className, { active: activeType === module.value }]"
        @click="toggleType(module.value)"
      >
        <span class="module-mark">{{ module.short }}</span>
        <span class="module-copy">
          <strong>{{ module.label }}</strong>
          <small>{{ module.description }}</small>
        </span>
        <span class="module-count">{{ getTypeCount(module.value) }}</span>
      </button>
    </section>

    <div class="community-layout">
      <main class="community-main">
        <section class="filter-panel">
          <div class="search-row">
            <label class="community-search">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="11" cy="11" r="6"></circle>
                <path d="M20 20l-3.5-3.5"></path>
              </svg>
              <input v-model="searchQuery" type="search" placeholder="搜索研究问题、数据集、论文、期刊或标签" />
            </label>
            <select v-model="sortMode" class="sort-select" aria-label="排序方式">
              <option value="latest">最新发布</option>
              <option value="popular">最多讨论</option>
              <option value="liked">最多点赞</option>
            </select>
          </div>

          <div class="filter-row">
            <span class="filter-label">方向</span>
            <button
              v-for="item in directions"
              :key="item"
              class="filter-button"
              :class="{ active: activeDirection === item }"
              @click="activeDirection = item"
            >
              {{ item }}
            </button>
          </div>

          <div v-if="hasFilters" class="filter-summary">
            <span>当前找到 {{ filteredPosts.length }} 个相关主题</span>
            <button @click="clearFilters">清除筛选</button>
          </div>
        </section>

        <section v-if="forumStore.state.loading" class="status-card">正在加载研究社区...</section>
        <section v-else-if="forumStore.state.error" class="status-card error">
          {{ forumStore.state.error }}
          <button @click="forumStore.fetchPosts()">重新加载</button>
        </section>

        <section v-else-if="filteredPosts.length" class="post-list">
          <article
            v-for="post in filteredPosts"
            :key="post.id"
            class="research-post"
          >
            <header class="post-label-row">
              <div class="primary-labels">
                <button class="type-label" :class="typeClass(post.postType)" @click="activeType = post.postType">
                  {{ post.postType }}
                </button>
                <button class="discipline-label" @click="activeDirection = post.direction">{{ post.direction }}</button>
                <span v-if="post.resolved" class="resolved-label">已解决</span>
              </div>
              <time>{{ post.time }}</time>
            </header>

            <div class="post-author-row" :data-user-id="post.authorUserId" title="查看个人卡片">
              <span class="post-avatar">{{ post.avatar }}</span>
              <div>
                <strong>{{ post.author }}</strong>
                <span>发布于 {{ post.direction }}</span>
              </div>
            </div>

            <h2 @click="openPost(post.id)">{{ post.title }}</h2>
            <p class="post-content">{{ post.content }}</p>

            <div v-if="post.images?.length" class="post-image-grid">
              <button v-for="(image, index) in post.images" :key="`${post.id}-image-${index}`" @click="previewImage = image">
                <img :src="image.data" :alt="image.name" />
                <span>{{ image.name }}</span>
              </button>
            </div>

            <div v-if="post.attachments?.length" class="post-attachment-list">
              <a
                v-for="(file, index) in post.attachments"
                :key="`${post.id}-attachment-${index}`"
                :href="file.data"
                :download="file.name"
              >
                <span class="attachment-file-icon">附</span>
                <span><strong>{{ file.name }}</strong><small>{{ file.size }}</small></span>
                <b>下载</b>
              </a>
            </div>

            <div v-if="post.paperTitle || post.venueName || post.resourceLink" class="resource-panel">
              <div v-if="post.paperTitle" class="resource-main">
                <span class="resource-kind">关联论文</span>
                <strong>{{ post.paperTitle }}</strong>
                <small v-if="post.publishYear">{{ post.publishYear }}</small>
              </div>
              <div v-if="post.venueName || post.venueLevel" class="venue-meta">
                <span v-if="post.venueName">{{ post.venueName }}</span>
                <strong v-if="post.venueLevel">{{ post.venueLevel }}</strong>
              </div>
              <a
                v-if="post.resourceLink"
                :href="normalizeLink(post.resourceLink)"
                target="_blank"
                rel="noreferrer"
                class="resource-link"
              >
                查看资源
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M7 17 17 7M8 7h9v9"></path>
                </svg>
              </a>
            </div>

            <div class="post-tag-row">
              <button
                v-for="tag in post.tags"
                :key="tag"
                class="topic-tag"
                :class="{ active: activeTag === tag }"
                @click="activeTag = activeTag === tag ? '' : tag"
              >
                # {{ tag }}
              </button>
            </div>

            <footer class="post-footer">
              <div class="post-actions">
                <button :class="{ active: post.hasLiked }" @click="forumStore.likePost(post.id)">
                  <svg viewBox="0 0 24 24" :fill="post.hasLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8">
                    <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.3a2 2 0 0 0 2-1.7l1.4-9A2 2 0 0 0 19.7 9H14ZM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"></path>
                  </svg>
                  {{ post.likes }} 赞同
                </button>
                <button @click="openPost(post.id)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                    <path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5Z"></path>
                  </svg>
                  {{ post.replies.length }} 回复
                </button>
                <button :class="{ active: post.hasBookmarked }" @click="forumStore.bookmarkPost(post.id)">
                  <svg viewBox="0 0 24 24" :fill="post.hasBookmarked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.8">
                    <path d="M6 3h12v18l-6-4-6 4V3Z"></path>
                  </svg>
                  {{ post.bookmarks }} 收藏
                </button>
              </div>
              <button class="discussion-toggle" @click="openPost(post.id)">
                进入帖子查看评论
              </button>
            </footer>
          </article>
        </section>

        <section v-else class="empty-state">
          <span>NO RESULT</span>
          <h2>暂时没有匹配的研究主题</h2>
          <p>你可以清除筛选，或向相同专业的研究者发起第一个求助。</p>
          <button @click="openCreateModal">发布研究主题</button>
        </section>
      </main>

      <aside class="community-sidebar">
        <section class="sidebar-card quick-publish">
          <span class="card-kicker">快速发布</span>
          <h3>你现在需要什么？</h3>
          <button v-for="module in postModules.slice(0, 4)" :key="module.value" @click="openCreateModal(module.value)">
            <span :class="module.className">{{ module.short }}</span>
            <span>
              <strong>{{ module.label }}</strong>
              <small>{{ module.action }}</small>
            </span>
            <b>›</b>
          </button>
        </section>

        <section class="sidebar-card">
          <div class="sidebar-title-row">
            <h3>热门研究标签</h3>
            <button v-if="activeTag" @click="activeTag = ''">清除</button>
          </div>
          <div class="hot-tags">
            <button
              v-for="tag in popularTags"
              :key="tag.name"
              :class="{ active: activeTag === tag.name }"
              @click="activeTag = activeTag === tag.name ? '' : tag.name"
            >
              <span># {{ tag.name }}</span>
              <small>{{ tag.count }}</small>
            </button>
          </div>
        </section>

        <section class="sidebar-card community-guide">
          <span class="card-kicker">COMMUNITY GUIDE</span>
          <h3>让求助更快得到回应</h3>
          <ol>
            <li>选择与站内一致的所属方向</li>
            <li>描述已尝试的方法与数据条件</li>
            <li>论文推荐注明期刊、会议与等级</li>
            <li>资源分享附上有效期与使用条件</li>
          </ol>
        </section>
      </aside>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="closeCreateModal">
      <section class="publish-modal">
        <header>
          <div>
            <span>CREATE RESEARCH TOPIC</span>
            <h2>发布研究主题</h2>
          </div>
          <button class="modal-close" @click="closeCreateModal">×</button>
        </header>

        <div class="publish-form">
          <div class="form-section">
            <h3><span>1</span> 选择研究模块</h3>
            <div class="type-picker">
              <button
                v-for="module in postModules"
                :key="module.value"
                :class="[module.className, { active: form.postType === module.value }]"
                @click="form.postType = module.value"
              >
                <span>{{ module.short }}</span>
                <strong>{{ module.label }}</strong>
              </button>
            </div>
          </div>

          <div class="form-section">
            <h3><span>2</span> 选择所属方向</h3>
            <label class="wide-field">
              <span>所属方向</span>
              <select v-model="form.direction">
                <option v-for="item in directions.slice(1)" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
          </div>

          <div class="form-section">
            <h3><span>3</span> 填写主题内容</h3>
            <label class="wide-field">
              <span>主题标题</span>
              <input v-model="form.title" maxlength="120" placeholder="用一句话说明问题、资源或推荐价值" />
            </label>
            <label class="wide-field">
              <span>详细内容</span>
              <textarea v-model="form.content" rows="5" placeholder="补充研究背景、已有条件、使用限制或推荐理由"></textarea>
            </label>
            <label class="wide-field">
              <span>研究标签 <small>最多 8 个，用空格或逗号分隔</small></span>
              <input v-model="form.tagsRaw" placeholder="例如：多模态 医学影像 小样本" />
            </label>
            <div class="upload-grid">
              <label class="upload-card">
                <input type="file" accept="image/*" multiple @change="handleImageUpload" />
                <span class="upload-card-icon">图</span>
                <strong>上传图片</strong>
                <small>支持多张图片，单张不超过 4MB</small>
              </label>
              <label class="upload-card">
                <input type="file" multiple @change="handleAttachmentUpload" />
                <span class="upload-card-icon file">附</span>
                <strong>上传附件</strong>
                <small>支持文档、表格、压缩包等文件</small>
              </label>
            </div>
            <div v-if="form.images.length" class="upload-preview-grid">
              <div v-for="(image, index) in form.images" :key="`${image.name}-${index}`">
                <img :src="image.data" :alt="image.name" />
                <span>{{ image.name }}</span>
                <button @click="form.images.splice(index, 1)">×</button>
              </div>
            </div>
            <div v-if="form.attachments.length" class="upload-file-list">
              <div v-for="(file, index) in form.attachments" :key="`${file.name}-${index}`">
                <span><strong>{{ file.name }}</strong><small>{{ file.size }}</small></span>
                <button @click="form.attachments.splice(index, 1)">移除</button>
              </div>
            </div>
          </div>

          <div v-if="form.postType === '论文期刊' || form.postType === '研究讨论'" class="form-section optional-section">
            <h3><span>4</span> 关联论文与期刊</h3>
            <label class="wide-field">
              <span>从文献库关联论文</span>
              <select v-model="form.paperId">
                <option value="">不关联文献</option>
                <option v-for="doc in libraryStore.state.documents" :key="doc.id" :value="doc.id">{{ doc.title }}</option>
              </select>
            </label>
            <div class="form-grid">
              <label>
                <span>期刊 / 会议名称</span>
                <input v-model="form.venueName" placeholder="例如：Nature Communications" />
              </label>
              <label>
                <span>等级 / 分区</span>
                <select v-model="form.venueLevel">
                  <option value="">请选择</option>
                  <option>SCI 一区</option>
                  <option>SCI 二区</option>
                  <option>SCI 三区</option>
                  <option>SCI 四区</option>
                  <option>CCF A</option>
                  <option>CCF B</option>
                  <option>CCF C</option>
                  <option>核心期刊</option>
                </select>
              </label>
            </div>
          </div>

          <div v-if="form.postType === '数据集求助' || form.postType === '科研羊毛'" class="form-section optional-section">
            <h3><span>4</span> 补充资源信息</h3>
            <label class="wide-field">
              <span>资源链接 <small>可选</small></span>
              <input v-model="form.resourceLink" type="url" placeholder="https://" />
            </label>
          </div>

          <div class="ai-review-note" :class="{ rejected: moderationError }">
            <span class="ai-review-icon">AI</span>
            <div>
              <strong>{{ moderationError ? "AI 审核未通过" : "发布前自动审核" }}</strong>
              <p>{{ moderationError || "系统将自动检查学术相关性、真实性风险、违规内容和交易引流。" }}</p>
            </div>
          </div>
        </div>

        <footer>
          <span>{{ publishing ? "AI 正在审核帖子内容，请稍候..." : "审核通过后将立即公开发布" }}</span>
          <div>
            <button class="cancel-button" @click="closeCreateModal">取消</button>
            <button class="submit-button" :disabled="!canSubmit || publishing" @click="submitPost">
              {{ publishing ? "AI 审核中..." : "审核并发布" }}
            </button>
          </div>
        </footer>
      </section>
    </div>

    <div v-if="previewImage" class="modal-overlay image-preview-overlay" @click="previewImage = null">
      <div class="image-preview-card" @click.stop>
        <button @click="previewImage = null">×</button>
        <img :src="previewImage.data" :alt="previewImage.name" />
        <span>{{ previewImage.name }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { useForumStore } from "../stores/forum";
import { useLibraryStore } from "../stores/library";

const authStore = useAuthStore();
const forumStore = useForumStore();
const libraryStore = useLibraryStore();
const router = useRouter();

const postModules = [
  { value: "数据集求助", label: "数据集求助", short: "数", description: "寻找同领域数据与标注", action: "向同行求助数据", className: "dataset" },
  { value: "科研羊毛", label: "科研羊毛", short: "享", description: "算力、软件与学术优惠", action: "分享限时科研资源", className: "benefit" },
  { value: "论文期刊", label: "论文期刊", short: "刊", description: "好论文与投稿期刊推荐", action: "推荐论文或期刊", className: "paper" },
  { value: "研究讨论", label: "研究讨论", short: "研", description: "方法、实验与科研问题", action: "发起方法讨论", className: "research" },
  { value: "比赛组队", label: "比赛组队", short: "赛", description: "科研竞赛与建模组队", action: "寻找比赛队友", className: "competition" }
];

const researchAreaMap = {
  "计算机科学": ["人工智能", "自然语言处理", "计算机视觉", "多模态学习", "数据科学", "数据挖掘", "网络安全", "软件工程", "人机交互"],
  "医学与生命科学": ["基础医学", "临床医学", "医学人工智能", "公共卫生", "护理学", "生物信息学", "神经科学", "药学与药物研发"],
  "数学与物理": ["基础数学", "应用数学", "概率论", "统计学", "运筹学", "计算数学", "凝聚态物理", "粒子与核物理", "光学", "量子信息", "天体物理", "计算物理"],
  "材料与化学": ["有机化学", "无机化学", "分析化学", "催化化学", "高分子材料", "新能源材料", "计算材料学"],
  "电子与通信": ["信号处理", "通信工程", "集成电路", "微电子", "控制科学", "机器人"],
  "机械与制造": ["机械设计", "智能制造", "车辆工程", "航空航天", "能源动力", "工业工程"],
  "土木建筑": ["结构工程", "岩土工程", "交通工程", "建筑学", "城乡规划", "工程管理"],
  "环境与地球科学": ["环境科学", "生态学", "地理信息", "地质学", "海洋科学", "气象学"],
  "农业与食品": ["作物科学", "植物保护", "动物科学", "林学", "食品科学", "智慧农业"],
  "经济与管理": ["理论经济学", "应用经济学", "计量经济学", "金融科技", "工商管理", "会计学", "公共管理"],
  "教育与社会科学": ["教育学", "教育技术", "高等教育", "心理学", "认知科学", "社会学", "传播学", "体育科学"],
  "法学与政治": ["法学", "知识产权", "国际关系", "政治学", "社会治理"],
  "文学与语言": ["中国语言文学", "外国语言文学", "翻译学", "新闻传播", "数字人文"],
  "历史哲学与艺术": ["历史学", "考古学", "哲学", "伦理学", "艺术设计", "音乐与影视"]
};

const directions = ["全部方向", ...new Set(Object.values(researchAreaMap).flat())];
const searchQuery = ref("");
const activeType = ref("");
const activeDirection = ref("全部方向");
const activeTag = ref("");
const sortMode = ref("latest");
const showCreateModal = ref(false);
const publishing = ref(false);
const moderationError = ref("");
const previewImage = ref(null);

const blankForm = () => ({
  postType: "数据集求助",
  direction: "人工智能",
  title: "",
  content: "",
  tagsRaw: "",
  paperId: "",
  venueName: "",
  venueLevel: "",
  resourceLink: "",
  images: [],
  attachments: []
});
const form = reactive(blankForm());

onMounted(async () => {
  await Promise.all([forumStore.fetchPosts(), libraryStore.hydrateLibrary()]);
});

const filteredPosts = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();
  const result = forumStore.state.posts.filter(post => {
    const searchText = [
      post.title,
      post.content,
      post.author,
      post.paperTitle,
      post.venueName,
      post.postType,
      post.direction,
      ...(post.tags || [])
    ].filter(Boolean).join(" ").toLowerCase();
    return (!query || searchText.includes(query))
      && (!activeType.value || post.postType === activeType.value)
      && (activeDirection.value === "全部方向" || post.direction === activeDirection.value)
      && (!activeTag.value || post.tags?.includes(activeTag.value));
  });
  return result.sort((a, b) => {
    if (sortMode.value === "popular") return b.replies.length - a.replies.length;
    if (sortMode.value === "liked") return b.likes - a.likes;
    return String(b.time).localeCompare(String(a.time));
  });
});

const totalReplies = computed(() => forumStore.state.posts.reduce((sum, post) => sum + post.replies.length, 0));
const hasFilters = computed(() => Boolean(searchQuery.value || activeType.value || activeTag.value || activeDirection.value !== "全部方向"));

const popularTags = computed(() => {
  const counts = {};
  forumStore.state.posts.flatMap(post => post.tags || []).forEach(tag => {
    counts[tag] = (counts[tag] || 0) + 1;
  });
  return Object.entries(counts).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count).slice(0, 10);
});

const selectedPaper = computed(() => libraryStore.state.documents.find(doc => String(doc.id) === String(form.paperId)));
const canSubmit = computed(() => form.title.trim() && form.content.trim().length > 5 && form.postType && form.direction);

function typeClass(type) {
  return postModules.find(item => item.value === type)?.className || "research";
}

function getTypeCount(type) {
  return forumStore.state.posts.filter(post => post.postType === type).length;
}

function toggleType(type) {
  activeType.value = activeType.value === type ? "" : type;
}

function clearFilters() {
  searchQuery.value = "";
  activeType.value = "";
  activeDirection.value = "全部方向";
  activeTag.value = "";
}

function openPost(postId) {
  router.push(`/forum/post/${postId}`);
}

function normalizeLink(value) {
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

function openCreateModal(type = "") {
  Object.assign(form, blankForm());
  moderationError.value = "";
  if (type) form.postType = type;
  showCreateModal.value = true;
}

function closeCreateModal() {
  showCreateModal.value = false;
}

async function submitPost() {
  if (!canSubmit.value || publishing.value) return;
  publishing.value = true;
  moderationError.value = "";
  const tags = form.tagsRaw.split(/[\s,，#]+/).map(tag => tag.trim()).filter(Boolean).slice(0, 8);
  try {
    const result = await forumStore.addPost({
      title: form.title.trim(),
      content: form.content.trim(),
      author: authStore.profile.name,
      postType: form.postType,
      direction: form.direction,
      tags,
      paperTitle: selectedPaper.value?.title || "",
      publishYear: selectedPaper.value?.publishYear || selectedPaper.value?.year || "",
      venueName: form.venueName.trim(),
      venueLevel: form.venueLevel,
      resourceLink: form.resourceLink.trim(),
      images: form.images,
      attachments: form.attachments
    });
    closeCreateModal();
    authStore.addNotification({
      title: "研究主题发布成功",
      desc: `《${form.title.slice(0, 18)}》已通过自动审核并发布。`
    });
  } catch (error) {
    moderationError.value = error?.response?.data?.message
      || error?.response?.data?.detail
      || "自动审核未通过，请修改内容后重试。";
  } finally {
    publishing.value = false;
  }
}

async function handleImageUpload(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (file.size > 4 * 1024 * 1024) {
      moderationError.value = `图片 ${file.name} 超过 4MB`;
      continue;
    }
    form.images.push(await readFile(file));
  }
  event.target.value = "";
}

async function handleAttachmentUpload(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (file.size > 8 * 1024 * 1024) {
      moderationError.value = `附件 ${file.name} 超过 8MB`;
      continue;
    }
    form.attachments.push(await readFile(file));
  }
  event.target.value = "";
}

function readFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve({
      name: file.name,
      type: file.type || "application/octet-stream",
      size: formatFileSize(file.size),
      data: reader.result
    });
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function formatFileSize(bytes) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
  if (bytes >= 1024) return `${Math.round(bytes / 1024)}KB`;
  return `${bytes}B`;
}

</script>

<style scoped>
.research-community {
  width: min(1480px, calc(100vw - 48px));
  margin: 0 auto;
  padding: 28px 0 80px;
  color: #172033;
  font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif;
}

button, input, select, textarea { font: inherit; }
button { cursor: pointer; }

.community-hero {
  min-height: 168px;
  padding: 28px 32px;
  border-radius: 28px;
  display: grid;
  grid-template-columns: minmax(220px, .72fr) minmax(520px, 1.65fr) auto;
  align-items: center;
  gap: 28px;
  color: #17243e;
  background: linear-gradient(120deg, #f8fbff 0%, #eef5ff 57%, #f8faff 100%);
  border: 1px solid #d9e6f8;
  box-shadow: 0 16px 42px rgba(42, 74, 124, .09);
}

.eyebrow, .card-kicker, .publish-modal header span {
  display: block;
  font-size: 11px;
  letter-spacing: .14em;
  font-weight: 800;
  color: #75aaff;
}

.notice-heading { display: flex; align-items: center; gap: 16px; }
.notice-icon { width: 54px; height: 54px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 16px; color: #0865ee; background: #fff; box-shadow: 0 8px 22px rgba(34, 91, 172, .12); }
.notice-icon svg { width: 28px; height: 28px; }
.notice-heading h1 { margin: 6px 0 4px; font-size: 26px; letter-spacing: -.03em; }
.notice-heading p { margin: 0; color: #75839a; font-size: 12px; line-height: 1.6; }
.notice-rules { display: grid; grid-template-columns: 1fr 1fr; gap: 9px 12px; }
.notice-rules > div { min-width: 0; display: flex; align-items: flex-start; gap: 9px; padding: 10px 12px; border: 1px solid rgba(180, 201, 231, .62); border-radius: 12px; background: rgba(255,255,255,.72); }
.notice-rules > div > span { flex: 0 0 auto; color: #0865ee; font-size: 10px; font-weight: 900; letter-spacing: .06em; }
.notice-rules p { margin: 0; color: #68758a; font-size: 10px; line-height: 1.55; }
.notice-rules strong { display: block; margin-bottom: 1px; color: #26344c; font-size: 11px; }
.hero-publish-button {
  flex: 0 0 auto;
  height: 48px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 0;
  border-radius: 12px;
  color: #fff;
  background: #0865ee;
  font-weight: 800;
  font-size: 12px;
  box-shadow: 0 10px 22px rgba(8, 101, 238, .22);
}
.plus-icon { font-size: 24px; line-height: 1; }

.module-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0;
}
.module-card {
  min-width: 0;
  padding: 17px;
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: 11px;
  align-items: center;
  text-align: left;
  background: #fff;
  border: 1px solid #e5eaf2;
  border-radius: 17px;
  color: #18233a;
  transition: .2s ease;
}
.module-card:hover, .module-card.active { transform: translateY(-2px); border-color: #86afff; box-shadow: 0 12px 30px rgba(40, 83, 153, .1); }
.module-card.active { background: #f6f9ff; }
.module-mark, .type-picker button > span, .quick-publish button > span {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-weight: 900;
}
.dataset .module-mark, .dataset > span:first-child, .quick-publish .dataset { color: #1267e8; background: #e7f0ff; }
.benefit .module-mark, .benefit > span:first-child, .quick-publish .benefit { color: #c56a00; background: #fff1d8; }
.paper .module-mark, .paper > span:first-child, .quick-publish .paper { color: #6554d9; background: #eeeaff; }
.research .module-mark, .research > span:first-child, .quick-publish .research { color: #0a8b67; background: #ddf7ef; }
.competition .module-mark, .competition > span:first-child, .quick-publish .competition { color: #d14b68; background: #ffe8ee; }
.module-copy { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.module-copy strong { font-size: 14px; }
.module-copy small { color: #8993a5; font-size: 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.module-count { color: #a0a9b8; font-size: 11px; }

.community-layout { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 18px; align-items: start; }
.filter-panel, .research-post, .sidebar-card, .status-card, .empty-state {
  background: #fff;
  border: 1px solid #e7ebf2;
  border-radius: 20px;
  box-shadow: 0 8px 28px rgba(38, 57, 91, .045);
}
.filter-panel { padding: 18px 20px; margin-bottom: 16px; }
.search-row { display: grid; grid-template-columns: 1fr 126px; gap: 12px; }
.community-search { height: 46px; display: flex; align-items: center; gap: 10px; padding: 0 14px; background: #f6f8fb; border: 1px solid #e5e9f0; border-radius: 12px; }
.community-search svg { width: 19px; color: #8490a3; }
.community-search input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; color: #172033; }
.sort-select, .publish-form select, .publish-form input, .publish-form textarea {
  border: 1px solid #dfe5ee;
  border-radius: 11px;
  background: #fff;
  color: #243048;
  outline: 0;
}
.sort-select { padding: 0 12px; }
.filter-row { display: flex; align-items: center; gap: 7px; margin-top: 12px; overflow-x: auto; padding-bottom: 3px; scrollbar-width: thin; }
.filter-label { width: 40px; color: #8993a5; font-size: 12px; font-weight: 700; }
.filter-button { flex: 0 0 auto; padding: 6px 10px; border: 0; border-radius: 8px; background: transparent; color: #59657a; font-size: 12px; }
.filter-button:hover, .filter-button.active { color: #075ee5; background: #eaf2ff; font-weight: 700; }
.filter-summary { display: flex; justify-content: space-between; margin-top: 13px; padding-top: 12px; border-top: 1px solid #edf0f5; color: #818da0; font-size: 12px; }
.filter-summary button, .sidebar-title-row button { border: 0; background: transparent; color: #0865ee; }

.post-list { display: flex; flex-direction: column; gap: 11px; }
.research-post { padding: 16px 20px 14px; transition: .2s ease; }
.research-post:hover { border-color: #cbd8ed; box-shadow: 0 13px 36px rgba(38, 57, 91, .08); }
.post-label-row, .post-footer, .reply-head, .sidebar-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.primary-labels { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; }
.post-label-row time { color: #9aa4b5; font-size: 11px; white-space: nowrap; }
.type-label, .discipline-label, .area-label, .resolved-label {
  padding: 5px 9px;
  border-radius: 7px;
  border: 0;
  font-size: 11px;
  font-weight: 800;
}
.type-label.dataset { color: #1267e8; background: #e7f0ff; }
.type-label.benefit { color: #b86500; background: #fff1d8; }
.type-label.paper { color: #6554d9; background: #eeeaff; }
.type-label.research { color: #087d5e; background: #ddf7ef; }
.type-label.competition { color: #c83e5d; background: #ffe8ee; }
.discipline-label { color: #344158; background: #eef1f6; }
.area-label { color: #48698f; background: #edf5fb; }
.resolved-label { color: #14815f; background: #e6f8f1; }
.post-author-row { display: flex; align-items: center; gap: 9px; margin-top: 11px; }
.post-author-row[data-user-id] { width: fit-content; margin-left: -4px; padding: 4px 8px 4px 4px; border-radius: 10px; transition: color .18s ease, background-color .18s ease; }
.post-author-row[data-user-id]:hover { color: #075ee5; background: #f1f6ff; }
.post-avatar, .reply-avatar { flex: 0 0 auto; width: 32px; height: 32px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #176ce4, #643bd4); font-size: 10px; font-weight: 800; }
.post-author-row div { display: flex; flex-direction: column; }
.post-author-row strong { font-size: 13px; }
.post-author-row span { color: #98a2b2; font-size: 10px; margin-top: 2px; }
.research-post h2 { margin: 10px 0 5px; font-size: 17px; line-height: 1.45; cursor: pointer; }
.research-post h2:hover { color: #0865ee; }
.post-content { margin: 0; color: #5c687b; font-size: 12px; line-height: 1.65; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.post-image-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 180px)); gap: 9px; margin-top: 11px; }
.post-image-grid button { padding: 0; overflow: hidden; aspect-ratio: 4 / 3; border: 1px solid #e0e7f1; border-radius: 10px; background: #f4f7fb; cursor: zoom-in; }
.post-image-grid img { width: 100%; height: 100%; display: block; object-fit: cover; transition: transform .2s ease; }
.post-image-grid button:hover img { transform: scale(1.025); }
.post-attachment-list { display: flex; flex-direction: column; gap: 7px; margin-top: 10px; }
.post-attachment-list a { min-width: 0; display: grid; grid-template-columns: 32px minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 9px 11px; border: 1px solid #e1e7f0; border-radius: 10px; color: #344158; background: #fbfcfe; text-decoration: none; }
.post-attachment-list a:hover { border-color: #bad0f2; background: #f4f8ff; }
.attachment-file-icon { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 8px; color: #fff; background: linear-gradient(135deg, #397ee9, #1763d6); font-size: 9px; font-weight: 900; }
.post-attachment-list strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 11px; }
.post-attachment-list small { color: #929dae; font-size: 10px; }
.resource-panel { margin-top: 10px; padding: 9px 12px; display: flex; align-items: center; gap: 12px; border: 1px solid #dce8fb; background: #f6f9fe; border-radius: 10px; }
.resource-main { min-width: 0; flex: 1; display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 9px; align-items: center; }
.resource-kind { color: #1465d8; font-size: 10px; font-weight: 800; }
.resource-main strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.resource-main small { color: #8d98aa; }
.venue-meta { display: flex; gap: 7px; align-items: center; font-size: 11px; }
.venue-meta span { color: #59657a; }
.venue-meta strong { padding: 4px 7px; border-radius: 6px; color: #7a4c00; background: #fff0c8; }
.resource-link { display: flex; align-items: center; gap: 4px; color: #075ee5; font-size: 11px; font-weight: 700; text-decoration: none; white-space: nowrap; }
.resource-link svg { width: 14px; }
.post-tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 9px; }
.topic-tag { padding: 4px 8px; border: 1px solid #e4e8ef; border-radius: 7px; background: #fff; color: #6b7688; font-size: 11px; }
.topic-tag:hover, .topic-tag.active { border-color: #b9d0f7; color: #075ee5; background: #f1f6ff; }
.post-footer { margin-top: 10px; padding-top: 9px; border-top: 1px solid #edf0f4; }
.post-actions { display: flex; align-items: center; gap: 4px; }
.post-actions button, .discussion-toggle { display: flex; align-items: center; gap: 5px; padding: 6px 9px; border: 0; border-radius: 7px; background: transparent; color: #758195; font-size: 11px; }
.post-actions button:hover, .post-actions button.active { color: #075ee5; background: #edf4ff; }
.post-actions svg { width: 15px; height: 15px; }
.discussion-toggle { color: #075ee5; font-weight: 700; }

.replies-panel { margin-top: 18px; padding-top: 18px; border-top: 1px solid #e9edf3; }
.replies-panel h3 { margin: 0 0 14px; font-size: 14px; }
.replies-panel h3 span { color: #8c97a7; font-weight: 500; }
.reply-list { display: flex; flex-direction: column; gap: 10px; }
.reply-item { display: flex; gap: 10px; padding: 13px; border-radius: 12px; background: #f7f9fc; }
.reply-avatar { width: 30px; height: 30px; }
.reply-body { min-width: 0; flex: 1; }
.reply-head { justify-content: flex-start; }
.reply-head strong { font-size: 12px; }
.reply-head time { color: #9da6b4; font-size: 10px; }
.reply-head button { margin-left: auto; border: 0; background: transparent; color: #8b95a5; font-size: 10px; }
.reply-head button.active { color: #075ee5; }
.reply-body p { margin: 7px 0 0; color: #536074; font-size: 12px; line-height: 1.7; }
.no-replies { padding: 24px; text-align: center; color: #929cac; background: #f8fafc; border-radius: 12px; font-size: 12px; }
.reply-editor { margin-top: 12px; padding: 12px; border: 1px solid #dfe5ed; border-radius: 12px; }
.reply-editor textarea { width: 100%; border: 0; outline: 0; resize: vertical; color: #2d394e; box-sizing: border-box; }
.reply-editor > div { display: flex; justify-content: space-between; align-items: center; padding-top: 9px; border-top: 1px solid #eef1f5; color: #8d97a7; font-size: 10px; }
.reply-editor button, .empty-state button { padding: 8px 14px; border: 0; border-radius: 8px; color: #fff; background: #0865ee; font-weight: 700; font-size: 11px; }
.reply-editor button:disabled { opacity: .45; cursor: not-allowed; }

.community-sidebar { display: flex; flex-direction: column; gap: 14px; position: sticky; top: 116px; }
.sidebar-card { padding: 20px; }
.sidebar-card h3 { margin: 7px 0 15px; font-size: 16px; }
.quick-publish > button { width: 100%; display: grid; grid-template-columns: 38px 1fr auto; align-items: center; gap: 10px; padding: 10px 0; border: 0; border-top: 1px solid #edf0f4; background: transparent; text-align: left; }
.quick-publish > button > span:first-child { width: 34px; height: 34px; border-radius: 10px; }
.quick-publish > button > span:nth-child(2) { display: flex; flex-direction: column; gap: 2px; }
.quick-publish strong { color: #2a3549; font-size: 12px; }
.quick-publish small { color: #98a2b2; font-size: 10px; }
.quick-publish b { color: #adb5c1; font-size: 20px; }
.sidebar-title-row h3 { margin: 0; }
.hot-tags { display: flex; flex-direction: column; gap: 4px; margin-top: 12px; }
.hot-tags button { display: flex; justify-content: space-between; padding: 8px 9px; border: 0; border-radius: 8px; background: transparent; color: #59657a; font-size: 11px; }
.hot-tags button:hover, .hot-tags button.active { background: #edf4ff; color: #075ee5; }
.hot-tags small { color: #a0a9b7; }
.community-guide ol { margin: 0; padding-left: 20px; color: #667287; font-size: 11px; line-height: 2; }
.community-guide li::marker { color: #1670ed; font-weight: 800; }
.status-card, .empty-state { padding: 60px 30px; text-align: center; color: #758195; }
.status-card button { margin-left: 8px; color: #075ee5; border: 0; background: transparent; }
.status-card.error { color: #bc3d52; }
.empty-state span { color: #166bed; font-size: 10px; font-weight: 800; letter-spacing: .15em; }
.empty-state h2 { margin: 10px 0 7px; color: #243048; }
.empty-state p { margin: 0 0 20px; }

.modal-overlay { position: fixed; inset: 0; z-index: 10000; display: grid; place-items: center; padding: 24px; background: rgba(16, 25, 43, .48); }
.publish-modal { width: min(820px, calc(100vw - 32px)); max-height: calc(100vh - 48px); display: flex; flex-direction: column; overflow: hidden; background: #fff; border-radius: 22px; box-shadow: 0 28px 80px rgba(14, 27, 52, .28); }
.publish-modal > header, .publish-modal > footer { flex: 0 0 auto; padding: 20px 24px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.publish-modal > header { border-bottom: 1px solid #e8ecf2; }
.publish-modal header h2 { margin: 4px 0 0; font-size: 22px; }
.modal-close { width: 36px; height: 36px; border: 0; border-radius: 10px; background: #f1f3f7; color: #657084; font-size: 24px; }
.publish-form { padding: 22px 24px; overflow-y: auto; }
.form-section + .form-section { margin-top: 24px; padding-top: 21px; border-top: 1px solid #edf0f4; }
.form-section h3 { display: flex; align-items: center; gap: 8px; margin: 0 0 14px; font-size: 14px; }
.form-section h3 > span { width: 22px; height: 22px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: #0865ee; font-size: 10px; }
.type-picker { display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; }
.type-picker button { padding: 11px 7px; display: flex; align-items: center; justify-content: center; gap: 7px; border: 1px solid #e1e6ee; border-radius: 11px; background: #fff; color: #435067; }
.type-picker button > span { width: 27px; height: 27px; border-radius: 8px; font-size: 11px; }
.type-picker button strong { font-size: 11px; }
.type-picker button.active { border-color: #79a8f5; box-shadow: 0 0 0 3px #edf4ff; color: #075ee5; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.publish-form label { display: flex; flex-direction: column; gap: 7px; }
.publish-form label > span { color: #59657a; font-size: 11px; font-weight: 700; }
.publish-form label small { color: #a0a9b7; font-weight: 400; }
.publish-form input, .publish-form select { height: 42px; padding: 0 12px; box-sizing: border-box; }
.publish-form textarea { padding: 12px; resize: vertical; line-height: 1.6; box-sizing: border-box; }
.publish-form input:focus, .publish-form select:focus, .publish-form textarea:focus { border-color: #75a6f6; box-shadow: 0 0 0 3px #edf4ff; }
.wide-field + .wide-field { margin-top: 12px; }
.upload-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 14px; }
.upload-card { min-height: 82px; display: flex !important; flex-direction: row !important; align-items: center; gap: 12px !important; padding: 13px; border: 1px dashed #bfd0ea; border-radius: 12px; background: #f8fbff; cursor: pointer; }
.upload-card:hover { border-color: #6fa0ee; background: #f2f7ff; }
.upload-card input { display: none; }
.upload-card-icon { width: 38px; height: 38px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 10px; color: #0865ee; background: #e8f1ff; font-size: 13px; font-weight: 900; }
.upload-card-icon.file { color: #6752d6; background: #efebff; }
.upload-card strong { color: #344158; font-size: 12px; }
.upload-card small { color: #8d98aa; font-size: 10px; font-weight: 400; }
.upload-preview-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 9px; margin-top: 11px; }
.upload-preview-grid > div { position: relative; overflow: hidden; aspect-ratio: 4 / 3; border: 1px solid #dfe6f0; border-radius: 10px; background: #f4f7fb; }
.upload-preview-grid img { width: 100%; height: 100%; object-fit: cover; }
.upload-preview-grid > div > span { position: absolute; inset: auto 0 0; overflow: hidden; padding: 18px 8px 7px; color: #fff; background: linear-gradient(transparent, rgba(18, 27, 43, .72)); text-overflow: ellipsis; white-space: nowrap; font-size: 9px; }
.upload-preview-grid button { position: absolute; top: 6px; right: 6px; width: 24px; height: 24px; border: 0; border-radius: 50%; color: #fff; background: rgba(24, 32, 47, .72); }
.upload-file-list { display: flex; flex-direction: column; gap: 7px; margin-top: 10px; }
.upload-file-list > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 9px 11px; border: 1px solid #e1e7f0; border-radius: 10px; }
.upload-file-list > div > span { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.upload-file-list strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #344158; font-size: 11px; }
.upload-file-list small { color: #929dae; font-size: 10px; }
.upload-file-list button { border: 0; color: #b04357; background: transparent; font-size: 11px; }
.optional-section { padding: 18px !important; border: 1px solid #dce8fb !important; border-radius: 14px; background: #f8fbff; }
.ai-review-note { margin-top: 18px; padding: 13px 15px; display: flex; align-items: flex-start; gap: 11px; border: 1px solid #cfe0fb; border-radius: 13px; background: #f4f8ff; }
.ai-review-note.rejected { border-color: #f3c8cf; background: #fff5f6; }
.ai-review-icon { width: 34px; height: 34px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 10px; color: #fff; background: linear-gradient(135deg, #0865ee, #6b45db); font-size: 10px; font-weight: 900; }
.ai-review-note.rejected .ai-review-icon { background: #d74c63; }
.ai-review-note strong { color: #2f3d54; font-size: 12px; }
.ai-review-note p { margin: 4px 0 0; color: #748096; font-size: 10px; line-height: 1.6; }
.ai-review-note.rejected p { color: #b13f53; }
.publish-modal > footer { border-top: 1px solid #e8ecf2; color: #8b95a6; font-size: 10px; }
.publish-modal > footer > div { display: flex; gap: 9px; }
.cancel-button, .submit-button { height: 40px; padding: 0 18px; border-radius: 10px; font-weight: 700; font-size: 12px; }
.cancel-button { border: 1px solid #dfe4ec; background: #fff; color: #59657a; }
.submit-button { border: 0; background: #0865ee; color: #fff; }
.submit-button:disabled { opacity: .45; cursor: not-allowed; }
.image-preview-overlay { background: rgba(14, 22, 37, .82); }
.image-preview-card { position: relative; width: min(1100px, calc(100vw - 48px)); height: min(82vh, 820px); display: grid; place-items: center; }
.image-preview-card img { max-width: 100%; max-height: 100%; object-fit: contain; border-radius: 12px; box-shadow: 0 24px 80px rgba(0, 0, 0, .32); }
.image-preview-card button { position: absolute; top: 0; right: 0; width: 38px; height: 38px; border: 0; border-radius: 50%; color: #fff; background: rgba(255, 255, 255, .18); font-size: 24px; }

@media (max-width: 1180px) {
  .community-hero { grid-template-columns: 1fr auto; }
  .notice-rules { grid-column: 1 / -1; }
  .module-strip { grid-template-columns: repeat(3, 1fr); }
  .community-layout { grid-template-columns: 1fr; }
  .community-sidebar { position: static; display: grid; grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 760px) {
  .research-community { width: min(100% - 24px, 1480px); padding-top: 16px; }
  .community-hero { padding: 22px; grid-template-columns: 1fr; align-items: flex-start; }
  .notice-rules { grid-column: auto; grid-template-columns: 1fr; }
  .module-strip { grid-template-columns: 1fr 1fr; }
  .community-sidebar { grid-template-columns: 1fr; }
  .search-row, .form-grid { grid-template-columns: 1fr; }
  .upload-grid { grid-template-columns: 1fr; }
  .type-picker { grid-template-columns: repeat(2, 1fr); }
  .post-label-row { align-items: flex-start; }
  .resource-panel, .post-footer { align-items: flex-start; flex-direction: column; }
  .resource-main { width: 100%; grid-template-columns: 1fr; }
  .publish-modal > footer { align-items: flex-start; flex-direction: column; }
}
</style>
