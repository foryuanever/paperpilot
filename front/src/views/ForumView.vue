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
      <div class="hero-action-stack">
        <button class="hero-publish-button" @click="openCreateModal">
          <span class="plus-icon">+</span>
          我已知晓，去发帖
        </button>
        <button class="hero-manage-button" @click="showMyPostsManager = true">
          管理帖子
        </button>
      </div>
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
            <div class="search-mode-switch" aria-label="搜索模式">
              <button :class="{ active: searchMode === 'content' }" @click="searchMode = 'content'">搜内容</button>
              <button :class="{ active: searchMode === 'tag' }" @click="searchMode = 'tag'">搜标签</button>
            </div>
            <label class="community-search">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <circle cx="11" cy="11" r="6"></circle>
                <path d="M20 20l-3.5-3.5"></path>
              </svg>
              <input
                v-model="searchQuery"
                type="search"
                :placeholder="searchMode === 'tag' ? '输入方向标签或话题标签，例如 人工智能 / 组会吐槽' : '搜索标题、正文、作者、论文或期刊'"
              />
            </label>
            <select v-model="sortMode" class="sort-select" aria-label="排序方式">
              <option value="latest">最新发布</option>
              <option value="popular">最多讨论</option>
              <option value="liked">最多点赞</option>
            </select>
          </div>

          <div class="time-filter-row">
            <span>时间段</span>
            <label>
              <small>开始</small>
              <input v-model="dateStart" type="date" />
            </label>
            <label>
              <small>结束</small>
              <input v-model="dateEnd" type="date" />
            </label>
            <button v-if="dateStart || dateEnd" @click="dateStart = ''; dateEnd = ''">清除时间</button>
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
            v-for="post in paginatedPosts"
            :key="post.id"
            class="research-post"
            :class="[membershipClass(post.authorMembershipPlan), { 'premium-wave-post': hasPremiumWave(post), 'pinned-post': post.pinned }]"
          >
            <div class="forum-row-avatar" :data-user-id="post.authorUserId" title="查看个人卡片">
              <img v-if="avatarUrlFor(post)" :src="avatarUrlFor(post)" class="post-avatar-img" :alt="post.author" />
              <span v-else class="post-avatar">{{ post.avatar }}</span>
            </div>

            <div class="forum-row-main">
              <div class="forum-title-line">
                <h2 @click="openPost(post.id)">
                  <span v-if="post.pinned" class="title-icon">📌</span>
                  <span v-if="isHotPost(post)" class="title-icon">🔥</span>
                  {{ post.title }}
                </h2>
                <div class="forum-row-badges">
                  <span v-if="post.pinned" class="state-badge pin-badge">置顶</span>
                  <span v-if="isHotPost(post)" class="state-badge hot-badge">热帖</span>
                  <span v-if="post.banned" class="state-badge ban-badge">已封禁</span>
                  <button
                    v-if="post.direction"
                    class="direction-label"
                    @click="searchByTag(post.direction)"
                  >
                    {{ post.direction }}
                  </button>
                  <button class="type-label" :class="typeClass(post.postType)" @click="activeType = post.postType">
                    {{ post.postType }}
                  </button>
                </div>
              </div>

              <div class="forum-meta-line">
                <span class="meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20 21a8 8 0 0 0-16 0"></path><circle cx="12" cy="7" r="4"></circle></svg>
                  <b class="member-name" :class="membershipClass(post.authorMembershipPlan)">{{ post.author }}</b>
                </span>
                <span class="meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12Z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                  {{ post.views || 0 }}
                </span>
                <span class="meta-item">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 11.5a8.5 8.5 0 0 1-12.3 7.6L3 21l1.9-5.7A8.5 8.5 0 1 1 21 11.5Z"></path></svg>
                  {{ post.replies.length }}
                </span>
                <span class="meta-item">
                  <span class="heat-icon" aria-hidden="true">🔥</span>
                  {{ post.likes || 0 }}
                </span>
                <time>{{ post.time }}</time>
              </div>
            </div>

            <div class="forum-row-side">
              <div v-if="replyAvatars(post).length" class="reply-avatar-strip" aria-label="评论参与者">
                <span
                  v-for="avatar in replyAvatars(post)"
                  :key="`${post.id}-${avatar.key}`"
                  class="reply-mini-avatar"
                  :title="avatar.name"
                >
                  <img v-if="avatar.url" :src="avatar.url" :alt="avatar.name" />
                  <b v-else>{{ avatar.text }}</b>
                </span>
              </div>
              <div v-if="isAdmin" class="admin-post-actions">
                <button :disabled="moderationBusy[post.id]" :class="{ danger: post.banned }" @click="toggleModeration(post, 'ban')">
                  {{ post.banned ? "解封" : "封禁" }}
                </button>
                <button :disabled="moderationBusy[post.id]" :class="{ active: post.pinned }" @click="toggleModeration(post, 'pin')">
                  {{ post.pinned ? "取消置顶" : "置顶" }}
                </button>
              </div>
            </div>
          </article>
        </section>

        <nav v-if="filteredPosts.length > postPageSize" class="forum-pagination" aria-label="帖子分页">
          <span>共 {{ filteredPosts.length }} 条 · 第 {{ postPage }} / {{ postPageCount }} 页</span>
          <div>
            <button :disabled="postPage <= 1" @click="postPage -= 1">上一页</button>
            <button
              v-for="page in visiblePostPages"
              :key="page"
              :class="{ active: postPage === page }"
              @click="postPage = page"
            >
              {{ page }}
            </button>
            <button :disabled="postPage >= postPageCount" @click="postPage += 1">下一页</button>
          </div>
        </nav>

        <section v-else class="empty-state">
          <span>NO RESULT</span>
          <h2>暂时没有匹配的研究主题</h2>
          <p>你可以清除筛选，或向相同专业的研究者发起第一个求助。</p>
          <button @click="openCreateModal">发布研究主题</button>
        </section>
      </main>

      <aside class="community-sidebar">
        <section class="sidebar-card active-board">
          <div class="sidebar-title-row">
            <div>
              <span class="card-kicker">HOT TOPICS</span>
              <h3>帖子热度榜</h3>
            </div>
            <small>{{ hotPosts.length }} 条</small>
          </div>
          <div v-if="hotPosts.length" class="active-user-list hot-post-list">
            <article v-for="item in paginatedHotPosts" :key="item.id" @click="openPost(item.id)">
              <span class="active-rank" :class="{ top: item.rank <= 3 }">{{ item.rank }}</span>
              <div>
                <strong :class="{ pinned: item.pinned }">{{ item.title }}</strong>
                <small>热度 {{ item.heat }} · {{ item.views || 0 }} 浏览</small>
              </div>
            </article>
          </div>
          <div v-else class="active-board-empty">
            <strong>暂无热度数据</strong>
            <p>有浏览、点赞或回复后会自动生成榜单。</p>
          </div>
          <nav v-if="hotPostPageCount > 1" class="active-board-pager">
            <button :disabled="activePage <= 1" @click="activePage -= 1">‹</button>
            <span>{{ activePage }} / {{ hotPostPageCount }}</span>
            <button :disabled="activePage >= hotPostPageCount" @click="activePage += 1">›</button>
          </nav>
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

    <div v-if="showMyPostsManager" class="modal-overlay" @click.self="showMyPostsManager = false">
      <section class="post-manager-modal">
        <header>
          <div>
            <span>MY TOPICS</span>
            <h2>管理我发布的帖子</h2>
            <p>查看、进入或删除你在学术论坛发布过的主题。</p>
          </div>
          <button class="modal-close" @click="showMyPostsManager = false">×</button>
        </header>
        <div v-if="myPosts.length" class="post-manager-list">
          <article v-for="post in myPosts" :key="post.id">
            <div class="manager-post-icon" :class="typeClass(post.postType)">{{ post.avatar || "帖" }}</div>
            <div class="manager-post-copy">
              <div>
                <span>{{ post.postType }}</span>
                <span v-if="post.direction">{{ post.direction }}</span>
                <time>{{ post.time }}</time>
              </div>
              <strong>{{ post.title }}</strong>
            </div>
            <div class="manager-post-stats">
              <span>{{ post.likes }} 赞</span>
              <span>{{ post.replies?.length || 0 }} 回复</span>
            </div>
            <div class="manager-post-actions">
              <button @click="openEditPost(post)">编辑</button>
              <button @click="openPost(post.id)">查看</button>
              <button class="danger" @click="removeMyPost(post)">删除</button>
            </div>
          </article>
        </div>
        <div v-else class="post-manager-empty">
          <strong>还没有发布过帖子</strong>
          <p>发布后的主题会在这里集中管理。</p>
          <button @click="showMyPostsManager = false; openCreateModal()">去发帖</button>
        </div>
      </section>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="closeCreateModal">
      <section class="publish-modal">
        <header>
          <div>
            <span>{{ editingPost ? "EDIT RESEARCH TOPIC" : "CREATE RESEARCH TOPIC" }}</span>
            <h2>{{ editingPost ? "编辑研究主题" : "发布研究主题" }}</h2>
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
                @click="choosePostType(module.value)"
              >
                <span>{{ module.short }}</span>
                <strong>{{ module.label }}</strong>
              </button>
            </div>
          </div>

          <div class="form-section">
            <h3><span>2</span> 添加方向标签</h3>
            <label class="wide-field direction-combobox tag-field">
              <span>方向标签 <em>可选，不超过 10 字</em></span>
              <input
                v-model.trim="form.direction"
                maxlength="10"
                :placeholder="isFishPostType(form.postType) ? '例如：组会吐槽' : '例如：人工智能'"
              />
              <small>用于帖子卡片上的小标签，也可被搜索栏的“搜标签”检索。</small>
            </label>
          </div>

          <div class="form-section">
            <h3><span>3</span> 填写主题内容</h3>
            <label class="wide-field">
              <span>详细内容</span>
              <div class="markdown-editor">
                <input class="markdown-title-input" v-model="form.title" maxlength="120" placeholder="请输入标题" />
                <div class="markdown-tabbar">
                  <button type="button" :class="{ active: markdownMode === 'edit' }" title="只显示正文编辑区" @click="markdownMode = 'edit'">内容</button>
                  <button type="button" :class="{ active: markdownMode === 'preview' }" title="只显示发布后的预览效果" @click="markdownMode = 'preview'">预览</button>
                  <button type="button" :class="{ active: markdownMode === 'split' }" title="左边编辑，右边实时预览" @click="markdownMode = 'split'">对照</button>
                </div>
                <div class="markdown-toolbar">
                  <button type="button" title="加粗：**文字**" @click="insertMarkdown('**', '**')">B</button>
                  <button type="button" title="标题：# 标题" @click="insertMarkdown('# ', '')">H</button>
                  <button type="button" title="无序列表" @click="insertMarkdown('- ', '')">•</button>
                  <button type="button" title="引用块，适合放重点说明" @click="insertMarkdown('> ', '')">“</button>
                  <button type="button" title="插入链接" @click="insertMarkdown('[链接文字](', ')')">🔗</button>
                  <button type="button" title="插入公告式发帖模板" @click="insertAnnouncementTemplate">模板</button>
                  <button type="button" title="清空正文" @click="clearMarkdownContent">⌫</button>
                </div>
                <div v-if="markdownMode === 'edit'" class="markdown-body">
                  <div class="markdown-line-number">
                    <span v-for="line in editorLineNumbers" :key="line">{{ line }}</span>
                  </div>
                  <textarea
                    ref="contentEditor"
                    v-model="form.content"
                    rows="12"
                    placeholder="第一段写核心信息。\n\n第二段写补充说明。\n\n> 引用块适合放价格、步骤、实验条件或重点列表。\n\n官网：[链接文字](https://example.com)"
                    @paste="handleEditorPaste"
                  ></textarea>
                </div>
                <div v-else-if="markdownMode === 'split'" class="markdown-split">
                  <div class="markdown-body">
                    <div class="markdown-line-number">
                      <span v-for="line in editorLineNumbers" :key="line">{{ line }}</span>
                    </div>
                    <textarea
                      ref="contentEditor"
                      v-model="form.content"
                      rows="12"
                      placeholder="第一段写核心信息。"
                      @paste="handleEditorPaste"
                    ></textarea>
                  </div>
                  <div class="markdown-rendered" v-html="renderedMarkdown"></div>
                </div>
                <div v-else class="markdown-rendered" v-html="renderedMarkdown"></div>
                <div class="markdown-hints">
                  <span>{{ form.content.length }} 字符 · {{ editorLineNumbers.length }} 行</span>
                  <button type="button" @click="markdownMode = markdownMode === 'preview' ? 'split' : 'preview'">
                    {{ markdownMode === "preview" ? "对照编辑" : "预览正文" }}
                  </button>
                </div>
              </div>
            </label>
            <div class="upload-grid">
              <label class="upload-card">
                <input type="file" multiple @change="handleAttachmentUpload" />
                <span class="upload-card-icon file">附</span>
                <strong>上传并插入附件</strong>
                <small>图片请直接粘贴到正文；这里仅上传文档、表格、压缩包等附件。</small>
              </label>
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

          <div v-if="moderationError" class="publish-error-note">
            {{ moderationError }}
          </div>
        </div>

        <footer>
          <span>{{ publishing ? "正在提交并同步列表..." : editingPost ? "保存后立即更新帖子" : "发布后立即公开展示" }}</span>
          <div>
            <button class="cancel-button" @click="closeCreateModal">取消</button>
            <button class="submit-button" :disabled="!canSubmit || publishing" @click="submitPost">
              {{ publishing ? "提交中..." : editingPost ? "保存修改" : "发布帖子" }}
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

    <div v-if="reportTarget" class="modal-overlay" @click.self="closeReportModal">
      <section class="report-modal">
        <header>
          <div>
            <span>REPORT TOPIC</span>
            <h2>举报帖子</h2>
            <p>{{ reportTarget.title }}</p>
          </div>
          <button class="modal-close" @click="closeReportModal">×</button>
        </header>
        <label>
          <span>违规详情</span>
          <textarea v-model.trim="reportDetail" rows="5" maxlength="800" placeholder="请描述违规原因，例如：广告引流、辱骂攻击、虚假资源、违法内容等。"></textarea>
          <small>{{ reportDetail.length }}/800，至少 6 个字。</small>
        </label>
        <footer>
          <button class="cancel-button" @click="closeReportModal">取消</button>
          <button class="submit-button" :disabled="reporting || reportDetail.length < 6" @click="submitReport">
            {{ reporting ? "提交中..." : "提交举报" }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import MarkdownIt from "markdown-it";
import { useAuthStore } from "../stores/auth";
import { useForumStore } from "../stores/forum";
import { useLibraryStore } from "../stores/library";
import { paperpilotApi } from "../services/paperpilotApi";

const authStore = useAuthStore();
const forumStore = useForumStore();
const libraryStore = useLibraryStore();
const router = useRouter();

const postModules = [
  { value: "数据集求助", label: "数据集求助", short: "数", description: "寻找同领域数据与标注", action: "向同行求助数据", className: "dataset" },
  { value: "科研羊毛", label: "科研羊毛", short: "享", description: "算力、软件与学术优惠", action: "分享限时科研资源", className: "benefit" },
  { value: "论文期刊", label: "论文期刊", short: "刊", description: "好论文与投稿期刊推荐", action: "推荐论文或期刊", className: "paper" },
  { value: "研究讨论", label: "研究讨论", short: "研", description: "方法、实验与科研问题", action: "发起方法讨论", className: "research" },
  { value: "比赛组队", label: "比赛组队", short: "赛", description: "科研竞赛与建模组队", action: "寻找比赛队友", className: "competition" },
  { value: "摸鱼专区", label: "摸鱼专区", short: "鱼", description: "科研间隙闲聊与轻松分享", action: "发一条轻松动态", className: "fish" }
];

const announcementTemplate = [
  "一句话写清楚这次分享、求助或讨论的核心信息。",
  "",
  "补充背景：这里写你已经确认的信息、限制条件、适用范围或当前进展。",
  "",
  "官网 / 资料：[链接文字](https://example.com)",
  "",
  "> 重点一：用引用块承载价格、步骤、实验条件或关键结论。",
  "> 重点二：多行内容会保持成一个视觉块，发布后更接近公告式排版。",
  "",
  "## 社群 / 补充",
  "",
  "这里放联系方式、数据说明、复现实验条件或后续更新。"
].join("\n");
const searchQuery = ref("");
const searchMode = ref("content");
const activeType = ref("");
const activeTag = ref("");
const dateStart = ref("");
const dateEnd = ref("");
const sortMode = ref("latest");
const showCreateModal = ref(false);
const publishing = ref(false);
const moderationError = ref("");
const previewImage = ref(null);
const showMyPostsManager = ref(false);
const contentEditor = ref(null);
const markdownMode = ref("edit");
const editingPost = ref(null);
const reportTarget = ref(null);
const reportDetail = ref("");
const reporting = ref(false);
const moderationBusy = reactive({});
const postPage = ref(1);
const postPageSize = 15;
const activePage = ref(1);
const activePageSize = 6;
const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
});
const defaultValidateLink = markdown.validateLink;
markdown.validateLink = (url) => /^data:(image|application|text)\//i.test(url) || defaultValidateLink(url);

const blankForm = () => ({
  postType: "数据集求助",
  direction: "",
  title: "",
  content: announcementTemplate,
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
    const tagText = [post.direction, ...(post.tags || [])].filter(Boolean).join(" ").toLowerCase();
    const contentText = [
      post.title,
      post.content,
      post.author,
      post.paperTitle,
      post.venueName,
      post.postType
    ].filter(Boolean).join(" ").toLowerCase();
    const searchText = searchMode.value === "tag" ? tagText : `${contentText} ${tagText}`;
    return (!query || searchText.includes(query))
      && (!activeType.value || post.postType === activeType.value)
      && (!activeTag.value || post.tags?.includes(activeTag.value) || post.direction === activeTag.value)
      && isInDateRange(post);
  });
  return result.sort((a, b) => {
    if (sortMode.value === "popular") return b.replies.length - a.replies.length;
    if (sortMode.value === "liked") return b.likes - a.likes;
    return String(b.time).localeCompare(String(a.time));
  });
});

const totalReplies = computed(() => forumStore.state.posts.reduce((sum, post) => sum + post.replies.length, 0));
const hasFilters = computed(() => Boolean(searchQuery.value || activeType.value || activeTag.value || dateStart.value || dateEnd.value));
const isAdmin = computed(() => authStore.profile.role === "管理员" || authStore.session?.role === "管理员");
const myPosts = computed(() => forumStore.state.posts.filter(post => isMine(post)));
const postPageCount = computed(() => Math.max(1, Math.ceil(filteredPosts.value.length / postPageSize)));
const paginatedPosts = computed(() => filteredPosts.value.slice((postPage.value - 1) * postPageSize, postPage.value * postPageSize));
const visiblePostPages = computed(() => {
  const total = postPageCount.value;
  const start = Math.max(1, Math.min(postPage.value - 2, total - 4));
  return Array.from({ length: Math.min(5, total) }, (_, index) => start + index);
});
const hotPosts = computed(() => forumStore.state.posts
  .map((post) => ({
    ...post,
    heat: Number(post.likes || 0) * 3 + Number(post.replies?.length || 0) * 5 + Number(post.views || 0),
  }))
  .filter((post) => post.heat > 0)
  .sort((a, b) => b.heat - a.heat)
  .slice(0, 30)
  .map((post, index) => ({ ...post, rank: index + 1 })));
const hotPostPageCount = computed(() => Math.max(1, Math.ceil(hotPosts.value.length / activePageSize)));
const paginatedHotPosts = computed(() => hotPosts.value.slice((activePage.value - 1) * activePageSize, activePage.value * activePageSize));

const popularTags = computed(() => {
  const counts = {};
  forumStore.state.posts
    .flatMap(post => [post.direction, ...(post.tags || [])].filter(Boolean))
    .forEach(tag => {
    counts[tag] = (counts[tag] || 0) + 1;
  });
  return Object.entries(counts).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count).slice(0, 10);
});

const selectedPaper = computed(() => libraryStore.state.documents.find(doc => String(doc.id) === String(form.paperId)));
const canSubmit = computed(() => form.title.trim() && form.content.trim().length > 5 && form.postType && form.direction.length <= 10);
const editorLineNumbers = computed(() => {
  const count = Math.max(1, String(form.content || "").split("\n").length);
  return Array.from({ length: count }, (_, index) => index + 1);
});
const renderedMarkdown = computed(() => {
  const source = form.content?.trim() || "_预览会显示在这里。_";
  return markdown.render(source);
});

function typeClass(type) {
  return postModules.find(item => item.value === type)?.className || "research";
}

function isFishPostType(type) {
  return type === "摸鱼专区";
}

function choosePostType(type) {
  form.postType = type;
}

function getTypeCount(type) {
  return forumStore.state.posts.filter(post => post.postType === type).length;
}

function toggleType(type) {
  activeType.value = activeType.value === type ? "" : type;
}

function clearFilters() {
  searchQuery.value = "";
  searchMode.value = "content";
  activeType.value = "";
  activeTag.value = "";
  dateStart.value = "";
  dateEnd.value = "";
}

function openPost(postId) {
  router.push(`/forum/post/${postId}`);
}

function normalizeLink(value) {
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

function openCreateModal(type = "") {
  Object.assign(form, blankForm());
  editingPost.value = null;
  markdownMode.value = "split";
  moderationError.value = "";
  if (type) choosePostType(type);
  showCreateModal.value = true;
}

function closeCreateModal() {
  showCreateModal.value = false;
}

async function submitPost() {
  if (!canSubmit.value || publishing.value) return;
  publishing.value = true;
  moderationError.value = "";
  const tags = [form.direction, form.postType, ...form.title.split(/[\s,，#：:]+/)].map(tag => tag.trim()).filter(Boolean).slice(0, 8);
  const payload = {
    title: form.title.trim(),
    content: form.content.trim(),
    author: authStore.profile.name,
    postType: form.postType,
    direction: form.direction.trim().slice(0, 10),
    tags,
    paperTitle: selectedPaper.value?.title || form.paperTitle || "",
    publishYear: selectedPaper.value?.publishYear || selectedPaper.value?.year || form.publishYear || "",
    venueName: form.venueName.trim(),
    venueLevel: form.venueLevel,
    resourceLink: "",
    images: form.images.map(compactUploadFile),
    attachments: form.attachments.map(compactUploadFile)
  };
  try {
    if (editingPost.value) {
      await forumStore.updatePost(editingPost.value.id, payload);
    } else {
      await forumStore.addPost(payload);
    }
    closeCreateModal();
    authStore.addNotification({
      title: editingPost.value ? "研究主题已更新" : "研究主题发布成功",
      desc: `《${form.title.slice(0, 18)}》已${editingPost.value ? "保存" : "发布"}。`
    });
    window.dispatchEvent(new Event("paperpilot:forum-posts-changed"));
  } catch (error) {
    console.error("Failed to publish forum post:", error);
    moderationError.value = error?.response?.data?.message
      || error?.response?.data?.detail
      || (error?.code === "ECONNABORTED" ? "保存超时：请确认后端已启动，或减少正文里的大图后重试。" : "")
      || "帖子保存失败，请稍后重试。";
  } finally {
    publishing.value = false;
  }
}

function openEditPost(post) {
  editingPost.value = post;
  Object.assign(form, blankForm(), {
    postType: post.postType || "数据集求助",
    direction: post.direction || "",
    title: post.title || "",
    content: post.content || "",
    tagsRaw: (post.tags || []).join(" "),
    venueName: post.venueName || "",
    venueLevel: post.venueLevel || "",
    resourceLink: post.resourceLink || "",
    images: Array.isArray(post.images) ? [...post.images] : [],
    attachments: Array.isArray(post.attachments) ? [...post.attachments] : []
  });
  markdownMode.value = "split";
  moderationError.value = "";
  showMyPostsManager.value = false;
  showCreateModal.value = true;
}

async function insertMarkdown(before, after = "") {
  const textarea = contentEditor.value;
  if (!textarea) {
    form.content += `${before}${after}`;
    return;
  }
  const start = textarea.selectionStart || 0;
  const end = textarea.selectionEnd || 0;
  const selected = form.content.slice(start, end);
  form.content = `${form.content.slice(0, start)}${before}${selected}${after}${form.content.slice(end)}`;
  await nextTick();
  const cursor = start + before.length + selected.length + after.length;
  textarea.focus();
  textarea.setSelectionRange(cursor, cursor);
}

async function insertAnnouncementTemplate() {
  if (form.content.trim() && !window.confirm("当前正文已有内容，确定追加公告模板吗？")) return;
  form.content = form.content.trim() ? `${form.content.trim()}\n\n${announcementTemplate}` : announcementTemplate;
  await nextTick();
  contentEditor.value?.focus();
}

function clearMarkdownContent() {
  if (!form.content.trim() || window.confirm("确定清空正文内容吗？")) {
    form.content = "";
  }
}

async function handleEditorPaste(event) {
  const items = Array.from(event.clipboardData?.items || []);
  const pastedText = event.clipboardData?.getData("text/plain") || "";
  const pastedHtml = event.clipboardData?.getData("text/html") || "";
  const pastedImageDataUrl = extractImageDataUrl(pastedText) || extractImageDataUrl(pastedHtml);
  if (pastedImageDataUrl) {
    event.preventDefault();
    await addPastedDataUrlImage(pastedImageDataUrl);
    return;
  }
  if (looksLikeRawBase64(pastedText)) {
    event.preventDefault();
    moderationError.value = "检测到大段 base64 文本。请直接粘贴图片文件或上传附件，不要把 base64 原文放进帖子正文。";
    return;
  }
  const imageItems = items.filter(item => item.type?.startsWith("image/"));
  if (!imageItems.length) return;
  event.preventDefault();
  for (const item of imageItems) {
    const file = item.getAsFile();
    if (!file) continue;
    if (file.size > 4 * 1024 * 1024) {
      moderationError.value = "粘贴的图片超过 4MB，请压缩后再粘贴。";
      continue;
    }
    const image = await readFile(file);
    form.images.push(image);
    await insertMarkdown(`\n\n图片：${escapeMarkdownText(image.name || "粘贴图片")}\n\n`, "");
  }
}

async function addPastedDataUrlImage(dataUrl) {
  const parsed = parseImageDataUrl(dataUrl);
  if (!parsed) {
    moderationError.value = "无法识别粘贴的图片，请保存为图片文件后再粘贴或上传。";
    return;
  }
  if (parsed.bytes > 4 * 1024 * 1024) {
    moderationError.value = "粘贴的图片超过 4MB，请压缩后再粘贴。";
    return;
  }
  const image = {
    name: `粘贴图片-${Date.now()}.${parsed.ext}`,
    type: parsed.mime,
    size: formatFileSize(parsed.bytes),
    data: dataUrl
  };
  form.images.push(image);
  await insertMarkdown(`\n\n图片：${escapeMarkdownText(image.name)}\n\n`, "");
}

function extractImageDataUrl(value) {
  const match = String(value || "").match(/data:image\/(?:png|jpe?g|webp|gif);base64,[A-Za-z0-9+/=\r\n]+/i);
  return match ? match[0].replace(/\s+/g, "") : "";
}

function parseImageDataUrl(value) {
  const match = String(value || "").match(/^data:(image\/(png|jpe?g|webp|gif));base64,([A-Za-z0-9+/=]+)$/i);
  if (!match) return null;
  const ext = match[2].toLowerCase().replace("jpeg", "jpg");
  return {
    mime: match[1],
    ext,
    bytes: Math.floor(match[3].length * 3 / 4)
  };
}

function looksLikeRawBase64(value) {
  const text = String(value || "").trim();
  return text.length > 5000 && /^[A-Za-z0-9+/=\r\n]+$/.test(text);
}

function isMine(post) {
  return String(post.authorUserId || "") === String(authStore.profile.userId || "")
    || post.author === authStore.profile.name;
}

async function removeMyPost(post) {
  if (!window.confirm(`确定删除“${post.title}”吗？`)) return;
  await forumStore.deletePost(post.id);
  window.dispatchEvent(new Event("paperpilot:forum-posts-changed"));
}

async function toggleModeration(post, action) {
  if (moderationBusy[post.id]) return;
  moderationBusy[post.id] = true;
  try {
    if (action === "pin") await forumStore.togglePin(post.id);
    else await forumStore.toggleBan(post.id);
    await authStore.refreshNotifications().catch(() => {});
    window.dispatchEvent(new Event("paperpilot:site-messages-changed"));
  } finally {
    moderationBusy[post.id] = false;
  }
}

function searchByTag(tag) {
  searchMode.value = "tag";
  searchQuery.value = tag;
  activeTag.value = "";
}

function isInDateRange(post) {
  const value = parseForumTime(post?.time);
  if (!value) return true;
  if (dateStart.value) {
    const start = new Date(`${dateStart.value}T00:00:00`);
    if (value < start) return false;
  }
  if (dateEnd.value) {
    const end = new Date(`${dateEnd.value}T23:59:59`);
    if (value > end) return false;
  }
  return true;
}

function parseForumTime(value) {
  if (!value) return null;
  const normalized = String(value)
    .replace(/^(\d{2})-(\d{2})/, `${new Date().getFullYear()}-$1-$2`)
    .replace(" ", "T");
  const parsed = new Date(normalized);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function openReportModal(post) {
  reportTarget.value = post;
  reportDetail.value = "";
}

function closeReportModal() {
  reportTarget.value = null;
  reportDetail.value = "";
}

async function submitReport() {
  if (!reportTarget.value || reportDetail.value.length < 6 || reporting.value) return;
  reporting.value = true;
  try {
    await forumStore.reportPost(reportTarget.value.id, { detail: reportDetail.value });
    authStore.addNotification({
      title: "举报已提交",
      desc: "管理员会在后台查看并处理。"
    });
    closeReportModal();
  } catch (error) {
    moderationError.value = error?.response?.data?.message || error?.response?.data?.detail || "举报提交失败，请稍后重试。";
  } finally {
    reporting.value = false;
  }
}

function avatarUrlFor(postOrReply) {
  if (String(postOrReply?.authorUserId || "") === String(authStore.profile.userId || "")) {
    return authStore.profile.avatarUrl || "";
  }
  return postOrReply?.avatarUrl || "";
}

function replyAvatars(post) {
  const seen = new Set();
  return (post.replies || []).slice(-6).reverse().map(reply => ({
    key: reply.id,
    name: reply.author,
    text: reply.avatar || String(reply.author || "U").slice(0, 1).toUpperCase(),
    url: avatarUrlFor(reply)
  })).filter(item => {
    const key = item.url || item.text + item.name;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  }).slice(0, 5);
}

function lastActiveName(post) {
  const replies = post?.replies || [];
  const lastReply = replies.length ? replies[replies.length - 1] : null;
  return lastReply?.author || post?.author || "暂无互动";
}

function membershipClass(plan) {
  return `member-${plan || "free"}`;
}

function hasPremiumWave(post) {
  return ["lab", "team_plus"].includes(post?.authorMembershipPlan);
}

function isHotPost(post) {
  return Number(post.likes || 0) + Number(post.replies?.length || 0) >= 50;
}

async function handleAttachmentUpload(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (file.size > 8 * 1024 * 1024) {
      moderationError.value = `附件 ${file.name} 超过 8MB`;
      continue;
    }
    const attachment = await readFile(file);
    form.attachments.push(attachment);
    await insertMarkdown(`\n\n附件：${escapeMarkdownText(attachment.name)}\n\n`, "");
  }
  event.target.value = "";
}

function compactUploadFile(file) {
  return {
    name: file.name,
    type: file.type,
    size: file.size,
    data: file.data
  };
}

function escapeMarkdownText(value) {
  return String(value || "附件").replace(/[[\]()]/g, "");
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

watch([searchQuery, searchMode, activeType, activeTag, sortMode, dateStart, dateEnd], () => {
  postPage.value = 1;
});

watch(postPageCount, (count) => {
  if (postPage.value > count) postPage.value = count;
});

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
  grid-template-columns: repeat(6, minmax(0, 1fr));
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
.module-mark, .type-picker button > span {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-weight: 900;
}
.dataset .module-mark, .dataset > span:first-child { color: #1267e8; background: #e7f0ff; }
.benefit .module-mark, .benefit > span:first-child { color: #c56a00; background: #fff1d8; }
.paper .module-mark, .paper > span:first-child { color: #6554d9; background: #eeeaff; }
.research .module-mark, .research > span:first-child { color: #0a8b67; background: #ddf7ef; }
.competition .module-mark, .competition > span:first-child { color: #d14b68; background: #ffe8ee; }
.fish .module-mark, .fish > span:first-child { color: #0f766e; background: #dff7f2; }
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
.search-row { display: grid; grid-template-columns: auto minmax(0, 1fr) 126px; gap: 12px; }
.search-mode-switch {
  height: 46px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid #dfe7f2;
  border-radius: 12px;
  background: #f8fafc;
}
.search-mode-switch button {
  height: 36px;
  padding: 0 12px;
  border: 0;
  border-radius: 9px;
  color: #64748b;
  background: transparent;
  font-size: 12px;
  font-weight: 850;
}
.search-mode-switch button.active {
  color: #075ee5;
  background: #fff;
  box-shadow: 0 5px 14px rgba(34, 91, 172, .1);
}
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
.time-filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #edf1f6;
  border-radius: 13px;
  background: #fbfcfe;
}
.time-filter-row > span {
  color: #6b7688;
  font-size: 12px;
  font-weight: 850;
}
.time-filter-row label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.time-filter-row small {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 800;
}
.time-filter-row input {
  height: 32px;
  padding: 0 9px;
  border: 1px solid #dfe7f2;
  border-radius: 9px;
  color: #334155;
  background: #fff;
}
.time-filter-row button {
  height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: 9px;
  color: #075ee5;
  background: #edf4ff;
  font-size: 11px;
  font-weight: 850;
}
.filter-summary { display: flex; justify-content: space-between; margin-top: 13px; padding-top: 12px; border-top: 1px solid #edf0f5; color: #818da0; font-size: 12px; }
.filter-summary button, .sidebar-title-row button { border: 0; background: transparent; color: #0865ee; }

.post-list { display: flex; flex-direction: column; gap: 11px; }
.research-post { padding: 16px 20px 14px; transition: .2s ease; }
.research-post:hover { border-color: #cbd8ed; box-shadow: 0 13px 36px rgba(38, 57, 91, .08); }
.post-label-row, .post-footer, .reply-head, .sidebar-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.primary-labels { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; }
.post-time { margin-left: auto; color: #9aa4b5; font-size: 11px; white-space: nowrap; }
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
.type-label.fish { color: #0f766e; background: #dff7f2; }
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

.active-board .sidebar-title-row {
  align-items: flex-start;
}

.active-board .sidebar-title-row h3 {
  margin: 5px 0 0;
}

.active-board .sidebar-title-row small {
  padding: 5px 9px;
  border-radius: 999px;
  color: #075ee5;
  background: #eef5ff;
  font-size: 11px;
  font-weight: 850;
}

.active-user-list {
  display: grid;
  gap: 6px;
}

.active-user-list article {
  display: grid;
  grid-template-columns: 26px 34px minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  min-height: 50px;
  padding: 7px 0;
  border-top: 1px solid #edf0f4;
}

.active-rank {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #6b7280;
  background: #f4f6fa;
  font-size: 11px;
  font-weight: 900;
}

.active-rank.top {
  color: #a16207;
  background: #fff3ca;
}

.active-user-list img,
.active-user-list article > b {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #176ce4, #643bd4);
  object-fit: cover;
  font-size: 12px;
  font-weight: 900;
}

.active-user-list div {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.active-user-list strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.active-user-list small {
  color: #8b95a6;
  font-size: 10px;
}

.active-board-empty {
  padding: 18px 0 4px;
  border-top: 1px solid #edf0f4;
  color: #8b95a6;
  font-size: 12px;
  line-height: 1.6;
}

.active-board-empty strong {
  display: block;
  color: #334155;
}

.active-board-empty p {
  margin: 4px 0 0;
}

.active-board-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #edf0f4;
}

.active-board-pager button,
.forum-pagination button {
  min-width: 30px;
  height: 30px;
  border: 1px solid #dce4ef;
  border-radius: 9px;
  color: #334155;
  background: #fff;
  font-weight: 850;
}

.active-board-pager button:disabled,
.forum-pagination button:disabled {
  opacity: .45;
  cursor: not-allowed;
}
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
.type-picker { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
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
.tag-field {
  padding: 14px;
  border: 1px solid #e5ecf6;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfdff, #f7faff);
}
.tag-field > span {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tag-field em {
  color: #8b98aa;
  font-style: normal;
  font-weight: 600;
}
.tag-field input {
  background: #fff;
}
.tag-field small {
  color: #718096 !important;
  line-height: 1.55;
}
.upload-grid { display: grid; grid-template-columns: 1fr; gap: 12px; margin-top: 14px; }
.upload-card { min-height: 82px; display: flex !important; flex-direction: row !important; align-items: center; gap: 12px !important; padding: 13px; border: 1px dashed #bfd0ea; border-radius: 12px; background: #f8fbff; cursor: pointer; }
.upload-card:hover { border-color: #6fa0ee; background: #f2f7ff; }
.upload-card input { display: none; }
.upload-card-icon { width: 38px; height: 38px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 10px; color: #0865ee; background: #e8f1ff; font-size: 13px; font-weight: 900; }
.upload-card-icon.file { color: #6752d6; background: #efebff; }
.upload-card strong { color: #344158; font-size: 12px; }
.upload-card small { color: #8d98aa; font-size: 10px; font-weight: 400; }
.upload-file-list { display: flex; flex-direction: column; gap: 7px; margin-top: 10px; }
.upload-file-list > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 9px 11px; border: 1px solid #e1e7f0; border-radius: 10px; }
.upload-file-list > div > span { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.upload-file-list strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #344158; font-size: 11px; }
.upload-file-list small { color: #929dae; font-size: 10px; }
.upload-file-list button { border: 0; color: #b04357; background: transparent; font-size: 11px; }
.optional-section { padding: 18px !important; border: 1px solid #dce8fb !important; border-radius: 14px; background: #f8fbff; }
.publish-error-note { margin-top: 18px; padding: 12px 14px; border: 1px solid #f3c8cf; border-radius: 12px; color: #b13f53; background: #fff5f6; font-size: 12px; font-weight: 800; line-height: 1.6; }
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

.report-modal {
  width: min(520px, calc(100vw - 32px));
  padding: 22px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgba(14, 27, 52, .26);
}
.report-modal header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}
.report-modal header span {
  color: #be123c;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: .14em;
}
.report-modal h2 {
  margin: 5px 0 4px;
  font-size: 20px;
}
.report-modal p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}
.report-modal label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 12px;
  font-weight: 850;
}
.report-modal textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid #dfe5ee;
  border-radius: 12px;
  color: #172033;
  line-height: 1.6;
  outline: 0;
  resize: vertical;
}
.report-modal textarea:focus {
  border-color: #ef8fa0;
  box-shadow: 0 0 0 3px #fff1f3;
}
.report-modal small {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
}
.report-modal footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

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

.filter-row {
  display: grid;
  grid-template-columns: 40px repeat(auto-fill, minmax(92px, 1fr));
  max-height: 112px;
  overflow: hidden;
  gap: 8px;
  padding-bottom: 0;
}

.filter-label {
  grid-row: 1 / span 3;
  align-self: start;
  padding-top: 6px;
}

.filter-button {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f7f9fc;
}

.state-badge {
  padding: 5px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 900;
}

.pin-badge { color: #b91c1c; background: #fee2e2; border: 1px solid #fecaca; }
.hot-badge { color: #b85b00; background: #fff0d6; }
.ban-badge { color: #b4233a; background: #fff0f2; }
.title-icon { margin-right: 4px; font-size: 14px; }

.research-post.pinned-post .forum-title-line h2,
.hot-post-list strong.pinned {
  color: #c81e1e;
}

.hot-post-list article {
  cursor: pointer;
}

.post-author-row {
  justify-content: space-between;
}

.post-author-main {
  width: fit-content;
  display: flex;
  flex-direction: row !important;
  align-items: center;
  gap: 9px;
  margin-left: -4px;
  padding: 4px 8px 4px 4px;
  border-radius: 10px;
  transition: color .18s ease, background-color .18s ease;
}

.post-author-main > div {
  display: flex !important;
  flex-direction: column !important;
  align-items: flex-start;
}

.post-author-main[data-user-id]:hover {
  color: #075ee5;
  background: #f1f6ff;
}

.post-avatar-img,
.reply-mini-avatar img {
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
}

.admin-post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-post-actions button {
  height: 30px;
  padding: 0 11px;
  border: 1px solid #d8e2f1;
  border-radius: 9px;
  background: #fff;
  color: #46546b;
  font-size: 11px;
  font-weight: 800;
}

.admin-post-actions button:disabled {
  opacity: .55;
  cursor: wait;
}

.admin-post-actions button.active {
  border-color: #9ec1ff;
  color: #075ee5;
  background: #edf4ff;
}

.admin-post-actions button.danger {
  border-color: #f0bdc7;
  color: #b4233a;
  background: #fff5f6;
}

.reply-avatar-strip {
  position: absolute;
  top: 88px;
  right: 48px;
  transform: none;
  display: flex;
  align-items: center;
  gap: 0;
  margin-top: 0;
}

.reply-mini-avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  margin-right: -7px;
  overflow: hidden;
  border: 2px solid #fff;
  border-radius: 50%;
  background: linear-gradient(135deg, #176ce4, #643bd4);
}

.reply-mini-avatar b {
  color: #fff;
  font-size: 10px;
}

.markdown-text {
  white-space: pre-wrap;
}

/* Forum stream: compact topic rows inspired by classic forum indexes */
.post-list {
  gap: 0;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e8ebef;
  border-radius: 12px;
}

.research-post {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 78px;
  padding: 12px 14px;
  border: 0;
  border-bottom: 1px solid #eceff3;
  border-radius: 0;
  box-shadow: none;
  background: #ffffff;
}

.research-post:last-child {
  border-bottom: 0;
}

.research-post:hover {
  border-color: #eceff3;
  box-shadow: none;
  background: #fbfcfe;
}

.research-post:nth-child(4n + 1) {
  background: linear-gradient(90deg, rgba(255, 251, 238, .72), #fff 22%);
}

.research-post:nth-child(4n + 2) {
  background: linear-gradient(90deg, rgba(240, 247, 255, .82), #fff 22%);
}

.research-post:nth-child(4n + 3) {
  background: linear-gradient(90deg, rgba(244, 240, 255, .68), #fff 22%);
}

.research-post:nth-child(4n) {
  background: linear-gradient(90deg, rgba(238, 250, 245, .72), #fff 22%);
}

.research-post.premium-wave-post {
  position: relative;
  overflow: hidden;
}

.research-post.premium-wave-post::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(100deg, transparent 0%, rgba(124, 58, 237, .075) 34%, rgba(37, 99, 235, .11) 50%, rgba(124, 58, 237, .075) 66%, transparent 100%);
  transform: translateX(-120%);
  animation: forum-premium-wave 8.5s linear infinite;
}

@keyframes forum-premium-wave {
  0%, 12% { transform: translateX(-120%); }
  88%, 100% { transform: translateX(120%); }
}

@media (prefers-reduced-motion: reduce) {
  .research-post.premium-wave-post::before {
    animation: none;
    transform: translateX(0);
    opacity: .35;
  }
}

.forum-row-avatar {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid #e7ebf2;
  border-radius: 50%;
  background: #f4d86e;
  box-shadow: 0 4px 10px rgba(20, 31, 50, .06);
}

.forum-row-avatar .post-avatar,
.forum-row-avatar .post-avatar-img {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  font-size: 14px;
}

.forum-row-main {
  min-width: 0;
}

.forum-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.research-post h2 {
  min-width: 0;
  margin: 0;
  color: #373b43;
  font-size: 17px;
  font-weight: 760;
  line-height: 1.32;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.research-post h2:hover {
  color: #20242c;
}

.forum-row-badges {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: none;
}

.forum-row-badges .state-badge,
.forum-row-badges .type-label,
.forum-row-badges .direction-label {
  height: 20px;
  display: inline-flex;
  align-items: center;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 750;
}

.direction-label {
  border: 0;
  color: #385b85;
  background: #eef5fb;
}

.forum-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #e7ebf2;
  border-radius: 14px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
}

.forum-pagination > div {
  display: flex;
  align-items: center;
  gap: 6px;
}

.forum-pagination button.active {
  border-color: #8cb7ff;
  color: #075ee5;
  background: #edf4ff;
}

.forum-meta-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 13px;
  margin-top: 6px;
  color: #626b78;
  font-size: 13px;
  line-height: 1.25;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.meta-item svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: #7a8390;
}

.meta-item .heat-icon {
  display: inline-grid;
  place-items: center;
  width: 15px;
  height: 15px;
  font-size: 14px;
  line-height: 1;
  filter: saturate(1.15);
}

.member-name {
  font: inherit;
  font-weight: 800;
}

.member-free {
  color: #667085;
}

.member-light {
  color: #059669;
}

.member-study {
  color: #1d4ed8;
}

.member-lab {
  color: #7c3aed;
  text-shadow: 0 0 14px rgba(124, 58, 237, .14);
}

.member-team {
  color: #c2410c;
  text-shadow: 0 0 14px rgba(194, 65, 12, .14);
}

.member-team_plus {
  color: #a855f7;
  text-shadow: 0 0 14px rgba(168, 85, 247, .16);
}

.forum-meta-line time {
  color: #697381;
  white-space: nowrap;
}

.forum-row-side {
  min-width: 112px;
  display: grid;
  justify-items: end;
  gap: 7px;
}

.reply-avatar-strip {
  position: static;
  top: auto;
  right: auto;
  transform: none;
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin: 0;
}

.reply-mini-avatar {
  width: 23px;
  height: 23px;
  margin-right: -5px;
  border-width: 1.5px;
}

.admin-post-actions {
  justify-content: flex-end;
}

.admin-post-actions button {
  height: 26px;
  padding: 0 9px;
  border-radius: 7px;
  font-size: 10px;
}

.hero-action-stack {
  display: grid;
  gap: 10px;
  justify-items: stretch;
}

.hero-manage-button {
  height: 42px;
  border: 1px solid #cfe0fb;
  border-radius: 12px;
  color: #075ee5;
  background: #fff;
  font-size: 12px;
  font-weight: 900;
}

.research-post {
  position: relative;
  min-height: 172px;
  padding-right: 170px;
}

.direction-combobox {
  position: relative;
}

.direction-combobox > span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.direction-combobox > span em {
  color: #0f766e;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.direction-combobox > small {
  color: #075ee5;
  font-size: 11px;
  font-weight: 800;
}

.direction-suggestion-panel {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(118px, 1fr));
  gap: 8px;
  max-height: 174px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #dbe6f5;
  border-radius: 12px;
  background: #f8fbff;
}

.direction-suggestion-panel button {
  min-height: 32px;
  border: 1px solid #e2e8f2;
  border-radius: 9px;
  background: #fff;
  color: #45536a;
  font-size: 12px;
  font-weight: 700;
}

.direction-suggestion-panel button.active,
.direction-suggestion-panel button:hover {
  border-color: #8db7ff;
  color: #075ee5;
  background: #edf4ff;
}

.markdown-editor {
  overflow: hidden;
  border: 1px solid #dbe5f2;
  border-radius: 14px;
  background: linear-gradient(180deg, #fff, #fbfdff);
}

.markdown-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px;
  border-bottom: 1px solid #edf0f4;
  background: #f8fbff;
}

.markdown-toolbar button {
  height: 28px;
  padding: 0 11px;
  border: 1px solid #d7e4f6;
  border-radius: 999px;
  background: #fff;
  color: #30405a;
  font-size: 11px;
  font-weight: 800;
}

.markdown-toolbar button:hover {
  border-color: #8db7ff;
  color: #075ee5;
  background: #edf4ff;
}

.markdown-editor textarea {
  width: 100%;
  border: 0;
  border-radius: 0;
  box-shadow: none !important;
  background: transparent;
  font-size: 13px;
  line-height: 1.75;
}

.markdown-hints {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-top: 1px solid #edf0f4;
  color: #8a95a6;
  font-size: 11px;
}

.markdown-hints button {
  border: 0;
  background: transparent;
  color: #075ee5;
  font-weight: 800;
}

.markdown-preview {
  margin: 0 10px 10px;
  padding: 12px;
  border-radius: 10px;
  background: #f7f9fc;
  color: #344158;
  font-size: 12px;
  line-height: 1.8;
}

.manage-posts-entry {
  margin-top: 4px;
}

.my-post-manager {
  display: grid;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #edf0f4;
}

.my-post-manager article {
  display: grid;
  gap: 4px;
  padding: 10px;
  border: 1px solid #e3e9f2;
  border-radius: 11px;
  background: #f8fbff;
}

.my-post-manager strong {
  overflow: hidden;
  color: #243048;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.my-post-manager span,
.my-post-manager p {
  margin: 0;
  color: #8b95a6;
  font-size: 10px;
}

.my-post-manager article > div {
  display: flex;
  gap: 6px;
}

.my-post-manager button {
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d8e2f1;
  border-radius: 8px;
  background: #fff;
  color: #075ee5;
  font-size: 11px;
  font-weight: 800;
}

.my-post-manager button.danger {
  color: #b4233a;
}

.post-list {
  gap: 10px;
}

.research-post {
  min-height: auto;
  padding: 13px 20px 12px;
  padding-right: 20px;
  border-radius: 16px;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.research-post:hover {
  border-color: #cbd8ed;
  box-shadow: 0 10px 22px rgba(38, 57, 91, .055);
  transform: translateY(-1px);
}

.post-label-row {
  align-items: flex-start;
}

.post-author-row {
  margin-top: 8px;
}

.research-post h2 {
  margin: 8px 0 0;
  font-size: 16px;
  line-height: 1.35;
}

.post-footer {
  margin-top: 9px;
  padding-top: 8px;
}

.post-manager-modal {
  width: min(900px, calc(100vw - 36px));
  max-height: min(720px, calc(100vh - 48px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 26px 70px rgba(13, 25, 46, .26);
}

.post-manager-modal > header {
  flex: 0 0 auto;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid #e9edf4;
}

.post-manager-modal > header span {
  color: #075ee5;
  font-size: 11px;
  font-weight: 900;
}

.post-manager-modal h2 {
  margin: 6px 0 5px;
  color: #172033;
  font-size: 22px;
}

.post-manager-modal p {
  margin: 0;
  color: #68758a;
  font-size: 13px;
}

.post-manager-list {
  display: grid;
  gap: 10px;
  overflow-y: auto;
  padding: 16px 18px 20px;
}

.post-manager-list article {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border: 1px solid #e5ebf4;
  border-radius: 14px;
  background: #fbfdff;
}

.manager-post-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 900;
}

.manager-post-copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.manager-post-copy > div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.manager-post-copy span,
.manager-post-copy time,
.manager-post-stats span {
  color: #718098;
  font-size: 11px;
}

.manager-post-copy span {
  padding: 3px 7px;
  border-radius: 999px;
  background: #eef3fb;
  font-weight: 800;
}

.manager-post-copy strong {
  overflow: hidden;
  color: #18233a;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.manager-post-stats {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

.manager-post-actions {
  display: flex;
  gap: 8px;
}

.manager-post-actions button,
.post-manager-empty button {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #cfddf1;
  border-radius: 10px;
  background: #fff;
  color: #075ee5;
  font-size: 12px;
  font-weight: 900;
}

.manager-post-actions button.danger {
  border-color: #f1c5ce;
  color: #b4233a;
}

.post-manager-empty {
  display: grid;
  place-items: center;
  gap: 9px;
  min-height: 260px;
  padding: 30px;
  text-align: center;
}

.post-manager-empty strong {
  color: #18233a;
  font-size: 18px;
}

.markdown-editor {
  overflow: hidden;
  border-radius: 10px;
  border-color: #dfe4ec;
  background: #fff;
}

.markdown-title-input {
  width: 100%;
  height: 42px !important;
  border: 0 !important;
  border-bottom: 1px solid #e7ebf2 !important;
  border-radius: 0 !important;
  padding: 0 16px !important;
  font-size: 15px;
  font-weight: 800;
  box-shadow: none !important;
}

.markdown-tabbar {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 38px;
  padding: 0 14px;
  border-bottom: 1px solid #e7ebf2;
  background: #fbfcfe;
}

.markdown-tabbar button {
  height: 30px;
  min-width: 34px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #59657a;
  font-size: 13px;
  font-weight: 800;
}

.markdown-tabbar button.active {
  color: #18233a;
  background: #eef3fb;
}

.markdown-tabbar span {
  margin-left: auto;
  color: #68758a;
  font-size: 12px;
}

.markdown-tabbar .icon-tool {
  min-width: 28px;
  color: #526177;
  background: #f0f2f6;
}

.markdown-toolbar {
  gap: 3px;
  padding: 5px 12px;
  min-height: 40px;
  border-bottom: 1px solid #e7ebf2;
  background: #fff;
}

.markdown-toolbar button {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #5c6778;
  font-size: 13px;
  font-weight: 800;
}

.markdown-toolbar button:hover {
  color: #075ee5;
  background: #eef4ff;
}

.markdown-body {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  min-height: 320px;
  background: #fff;
}

.markdown-line-number {
  padding-top: 13px;
  border-right: 1px solid #edf0f4;
  color: #a5adba;
  text-align: center;
  font-size: 13px;
  line-height: 1.8;
  user-select: none;
}

.markdown-line-number span {
  display: block;
}

.markdown-editor textarea {
  min-height: 320px;
  padding: 13px 16px;
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.markdown-rendered {
  min-height: 320px;
  padding: 26px 32px;
  color: #202938;
  background: #fff;
  line-height: 1.9;
  font-size: 15px;
  text-wrap: pretty;
}

.markdown-split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  min-height: 320px;
}

.markdown-split .markdown-body,
.markdown-split .markdown-rendered {
  min-height: 320px;
}

.markdown-split .markdown-rendered {
  border-left: 1px solid #e7ebf2;
  background: #fbfcfe;
}

.markdown-rendered :deep(h1),
.markdown-rendered :deep(h2),
.markdown-rendered :deep(h3) {
  margin: 22px 0 12px;
  color: #172033;
  line-height: 1.35;
}

.markdown-rendered :deep(h1:first-child),
.markdown-rendered :deep(h2:first-child),
.markdown-rendered :deep(h3:first-child) {
  margin-top: 0;
}

.markdown-rendered :deep(p) {
  margin: 0 0 16px;
}

.markdown-rendered :deep(ul),
.markdown-rendered :deep(ol) {
  margin: 0 0 16px;
  padding-left: 22px;
}

.markdown-rendered :deep(blockquote) {
  margin: 18px 0 20px;
  padding: 16px 20px;
  border: 1px solid #e6e9ef;
  border-left-width: 1px;
  color: #252d3b;
  background: #f6f6f7;
  box-shadow: inset 1px 0 0 #c7ccd5;
}

.markdown-rendered :deep(blockquote p) {
  margin-bottom: 8px;
}

.markdown-rendered :deep(img) {
  max-width: min(360px, 100%);
  display: block;
  margin: 18px 0;
  border-radius: 10px;
  border: 1px solid #e6ebf2;
}

.markdown-rendered :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: #eef2f7;
  color: #17345f;
}

.markdown-rendered :deep(pre) {
  overflow-x: auto;
  padding: 12px;
  border-radius: 10px;
  background: #111827;
  color: #f8fafc;
}

.markdown-rendered :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 12px;
}

.markdown-rendered :deep(th),
.markdown-rendered :deep(td) {
  padding: 8px 10px;
  border: 1px solid #dfe6f0;
  text-align: left;
}

.markdown-rendered :deep(a) {
  color: #075ee5;
  font-weight: 800;
  text-decoration: none;
}

.markdown-rendered :deep(a:hover) {
  text-decoration: underline;
}

@media (max-width: 760px) {
  .filter-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: 150px;
  }

  .filter-label {
    grid-column: 1 / -1;
    grid-row: auto;
  }

  .post-author-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .research-post {
    padding-right: 20px;
  }

  .reply-avatar-strip {
    position: static;
    transform: none;
    margin-top: 10px;
  }

  .post-manager-list article {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .manager-post-stats,
  .manager-post-actions {
    grid-column: 2;
  }

  .markdown-tabbar {
    flex-wrap: wrap;
    padding: 6px 10px;
  }

  .markdown-tabbar span {
    width: 100%;
    margin-left: 0;
  }

  .admin-post-actions {
    width: 100%;
  }
}
</style>
