<template>
  <div class="post-detail-page">
    <button class="back-button" @click="router.push('/forum')">
      <span>←</span> 返回研究社区
    </button>

    <section v-if="forumStore.state.loading && !post" class="detail-status">正在加载帖子...</section>
    <section v-else-if="!post" class="detail-status">
      <h2>帖子不存在或已被删除</h2>
      <button @click="router.push('/forum')">返回论坛</button>
    </section>

    <template v-else>
      <div class="detail-layout">
        <main>
          <article class="post-article" :class="{ pinned: post.pinned }">
            <div class="author-row">
              <div class="author-profile-trigger" :data-user-id="post.authorUserId" title="查看个人卡片">
                <img v-if="avatarUrlFor(post)" :src="avatarUrlFor(post)" class="avatar-img" :alt="post.author" />
                <span v-else class="avatar">{{ post.avatar }}</span>
                <div>
                  <strong class="member-name" :class="membershipClass(post.authorMembershipPlan)">{{ post.author }}</strong>
                  <small>
                    <span class="type-label" :class="typeClass(post.postType)">{{ post.postType }}</span>
                    <span v-if="post.direction" class="direction-pill">{{ post.direction }}</span>
                  </small>
                </div>
              </div>
              <div class="article-meta-stack">
                <div>
                  <span v-if="post.pinned" class="state-badge pin-badge">📌 置顶</span>
                  <span v-if="post.banned" class="state-badge ban-badge">已封禁</span>
                  <time>{{ post.time }}</time>
                </div>
                <button v-if="post.authorUserId" class="message-author" @click="openAuthorCard">联系作者</button>
              </div>
            </div>

            <h1>{{ post.title }}</h1>
            <div class="article-content markdown-rendered" v-html="renderMarkdown(post.content)" @click="handleMarkdownImageClick"></div>

            <div v-if="post.images?.length" class="article-images">
              <button v-for="image in post.images" :key="image.name" @click="previewImage = image.data">
                <img :src="image.data" :alt="image.name" />
              </button>
            </div>

            <div v-if="post.attachments?.length" class="article-files">
              <a v-for="file in post.attachments" :key="file.name" :href="file.data" :download="file.name">
                <span>FILE</span>
                <strong>{{ file.name }}</strong>
                <small>{{ file.size }}</small>
              </a>
            </div>

            <div v-if="post.paperTitle || post.venueName || post.resourceLink" class="resource-card">
              <div v-if="post.paperTitle" class="paper-info">
                <small>关联论文</small>
                <strong>{{ post.paperTitle }}</strong>
                <span v-if="post.publishYear">{{ post.publishYear }}</span>
              </div>
              <div v-if="post.venueName || post.venueLevel" class="venue-info">
                <span>{{ post.venueName }}</span>
                <strong v-if="post.venueLevel">{{ post.venueLevel }}</strong>
              </div>
              <a v-if="post.resourceLink" :href="normalizeLink(post.resourceLink)" target="_blank" rel="noreferrer">打开资源 ↗</a>
            </div>

            <div class="tag-row">
              <span v-for="tag in post.tags" :key="tag"># {{ tag }}</span>
            </div>

            <footer class="article-actions">
              <button class="like-action" :class="{ active: post.hasLiked, burst: likeBurst }" @click="likePostWithBurst">
                <svg class="like-thumb" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M7 10v11H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3Z"/><path d="M7 10 11.2 2.8a1.7 1.7 0 0 1 3.1 1.2l-.8 4H19a3 3 0 0 1 2.9 3.7l-1.6 6.5A4 4 0 0 1 16.4 21H7V10Z"/></svg>
                点赞 {{ post.likes }}
              </button>
              <button class="report-action" @click="openReportModal">举报</button>
              <span>{{ post.replies.length }} 条评论</span>
            </footer>
          </article>

          <section class="comments-card">
            <header>
              <div>
                <span>DISCUSSION</span>
                <h2>评论与讨论</h2>
              </div>
              <strong>{{ post.replies.length }}</strong>
            </header>

            <div class="comment-editor">
              <div v-if="replyTarget" class="reply-target-bar">
                正在回复 {{ replyTarget.author }}
                <button @click="replyTarget = null">取消</button>
              </div>

              <div class="reply-markdown-editor markdown-editor">
                <div class="markdown-tabbar">
                  <button type="button" :class="{ active: replyMarkdownMode === 'edit' }" title="只显示回复编辑区" @click="replyMarkdownMode = 'edit'">内容</button>
                  <button type="button" :class="{ active: replyMarkdownMode === 'preview' }" title="只显示发布后的预览效果" @click="replyMarkdownMode = 'preview'">预览</button>
                  <button type="button" :class="{ active: replyMarkdownMode === 'split' }" title="左边编辑，右边实时预览" @click="replyMarkdownMode = 'split'">对照</button>
                </div>

                <div class="markdown-toolbar">
                  <button type="button" title="加粗：**文字**" @click="insertReplyMarkdown('**', '**')">B</button>
                  <button type="button" title="标题：# 标题" @click="insertReplyMarkdown('# ', '')">H</button>
                  <button type="button" title="无序列表" @click="insertReplyMarkdown('- ', '')">•</button>
                  <button type="button" title="引用块，适合放重点说明" @click="insertReplyMarkdown('> ', '')">“</button>
                  <button type="button" title="插入链接" @click="insertReplyMarkdown('[链接文字](', ')')">链接</button>
                  <button type="button" title="插入回复模板" @click="insertReplyTemplate">模板</button>
                  <button type="button" title="清空回复内容" @click="clearReplyContent">清空</button>
                </div>

                <div v-if="replyMarkdownMode === 'edit'" class="markdown-body">
                  <div class="markdown-line-number">
                    <span v-for="line in replyEditorLineNumbers" :key="line">{{ line }}</span>
                  </div>
                  <textarea
                    ref="replyEditorRef"
                    v-model="replyContent"
                    rows="7"
                    placeholder="提供数据线索、方法建议或可验证的研究观点；支持 Markdown 语法；可直接粘贴图片"
                    @paste="handleReplyPaste"
                  ></textarea>
                </div>

                <div v-else-if="replyMarkdownMode === 'split'" class="markdown-split">
                  <div class="markdown-body">
                    <div class="markdown-line-number">
                      <span v-for="line in replyEditorLineNumbers" :key="line">{{ line }}</span>
                    </div>
                    <textarea
                      ref="replyEditorRef"
                      v-model="replyContent"
                      rows="7"
                      placeholder="提供数据线索、方法建议或可验证的研究观点。"
                      @paste="handleReplyPaste"
                    ></textarea>
                  </div>
                  <div class="markdown-rendered reply-preview-box" v-html="renderMarkdown(replyContent)"></div>
                </div>

                <div v-else class="markdown-rendered reply-preview-box" v-html="renderMarkdown(replyContent)"></div>

                <div class="markdown-hints">
                  <span>{{ replyContent.length }} 字符 · {{ replyEditorLineNumbers.length }} 行</span>
                  <button type="button" @click="replyMarkdownMode = replyMarkdownMode === 'preview' ? 'split' : 'preview'">
                    {{ replyMarkdownMode === "preview" ? "对照编辑" : "预览回复" }}
                  </button>
                </div>
              </div>

              <div class="reply-submit-row">
                <span>{{ replyPasteHint || `以 ${authStore.profile.name} 身份回复` }}</span>
                <button :disabled="!replyContent.trim() || submitting" @click="submitReply">
                  {{ submitting ? "发表中..." : "发表评论" }}
                </button>
              </div>
            </div>

            <div v-if="post.replies.length" class="comment-list">
              <article v-for="reply in post.replies" :key="reply.id" class="comment-item">
                <img v-if="avatarUrlFor(reply)" :src="avatarUrlFor(reply)" class="comment-avatar-img" :data-user-id="reply.authorUserId" :alt="reply.author" title="查看个人卡片" />
                <span v-else class="comment-avatar" :data-user-id="reply.authorUserId" title="查看个人卡片">{{ reply.avatar }}</span>
                <div>
                  <header>
                    <strong class="member-name" :class="membershipClass(reply.authorMembershipPlan)">{{ reply.author }}</strong>
                    <time>{{ reply.time }}</time>
                    <button :class="{ active: reply.hasLiked }" @click="forumStore.likeReply(post.id, reply.id)">
                      <svg class="comment-like-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M7 10v11H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3Z"/><path d="M7 10 11.2 2.8a1.7 1.7 0 0 1 3.1 1.2l-.8 4H19a3 3 0 0 1 2.9 3.7l-1.6 6.5A4 4 0 0 1 16.4 21H7V10Z"/></svg>
                      点赞 {{ reply.likes }}
                    </button>
                    <button @click="setReplyTarget(reply)">回复</button>
                  </header>
                  <small v-if="reply.replyToAuthor" class="reply-to-note">回复 {{ reply.replyToAuthor }}</small>
                  <div class="comment-content markdown-rendered" v-html="renderMarkdown(reply.content)" @click="handleMarkdownImageClick"></div>
                </div>
              </article>
            </div>
            <div v-else class="empty-comments">暂无评论，欢迎发表第一条有价值的研究建议。</div>
          </section>
        </main>

        <aside>
          <section class="side-card">
            <span class="side-kicker">RESEARCH PROFILE</span>
            <h3>主题信息</h3>
            <dl>
              <div><dt>帖子类型</dt><dd>{{ post.postType }}</dd></div>
              <div v-if="post.direction"><dt>方向标签</dt><dd>{{ post.direction }}</dd></div>
              <div><dt>发布时间</dt><dd>{{ post.time }}</dd></div>
            </dl>
          </section>
          <section class="side-card notice-card">
            <span class="side-kicker">ACADEMIC NOTICE</span>
            <h3>讨论提醒</h3>
            <p>评论应基于学术事实，提供来源、方法或可验证的论据。请勿发布与主题无关的信息。</p>
          </section>
        </aside>
      </div>
    </template>

    <div v-if="previewImage" class="preview-overlay" @click="previewImage = ''">
      <button @click="previewImage = ''">×</button>
      <img :src="previewImage" alt="图片预览" @click.stop />
    </div>

    <div v-if="showReportModal" class="preview-overlay report-overlay" @click="closeReportModal">
      <section class="report-card" @click.stop>
        <button class="report-close" type="button" aria-label="关闭举报弹窗" @click="closeReportModal">×</button>
        <header>
          <span>REPORT TOPIC</span>
          <h3>举报帖子</h3>
          <p>{{ post.title }}</p>
        </header>
        <label>
          <span>违规详情</span>
          <textarea v-model.trim="reportDetail" rows="5" maxlength="800" placeholder="请描述违规原因，例如：广告引流、辱骂攻击、虚假资源、违法内容等。"></textarea>
          <small>{{ reportDetail.length }}/800，至少 6 个字。</small>
        </label>

        <div class="report-upload-section" style="margin-top: 16px;">
          <span style="font-size: 13px; font-weight: 600; color: var(--c-text); display: block; margin-bottom: 8px; text-align: left;">附上截图证据 (可选)</span>
          <div class="report-upload-row" style="display: flex; flex-direction: column; gap: 8px;">
            <label class="report-upload-box" style="position: relative; display: flex; align-items: center; justify-content: center; height: 100px; border: 2px dashed var(--c-border); border-radius: 8px; cursor: pointer; overflow: hidden; background: var(--c-bg); transition: all 0.2s;">
              <input type="file" accept="image/*" @change="handleReportScreenshotUpload" style="display: none;" />
              <div v-if="!reportScreenshot" class="upload-placeholder" style="display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--c-muted);">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width: 20px; height: 20px;"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
                <span style="font-size: 12px;">点击上传屏幕截图</span>
              </div>
              <div v-else class="upload-preview" style="position: relative; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;">
                <img :src="reportScreenshot" alt="举报截图预览" style="max-width: 100%; max-height: 100%; object-fit: contain;" />
                <button class="remove-preview-btn" style="position: absolute; top: 4px; right: 4px; background: rgba(0,0,0,0.6); color: #fff; border: none; border-radius: 4px; padding: 2px 6px; font-size: 10px; cursor: pointer;" @click.stop.prevent="reportScreenshot = ''">删除</button>
              </div>
            </label>
            <small v-if="reportUploadError" class="upload-error" style="color: #ef4444; font-size: 11px; text-align: left;">{{ reportUploadError }}</small>
          </div>
        </div>
        <footer>
          <button @click="closeReportModal">取消</button>
          <button :disabled="reporting || reportDetail.length < 6" @click="submitReport">
            {{ reporting ? "提交中..." : "提交举报" }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import MarkdownIt from "markdown-it";
import { useAuthStore } from "../stores/auth";
import { useForumStore } from "../stores/forum";
import { useUserCardStore } from "../stores/userCard";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const forumStore = useForumStore();
const userCardStore = useUserCardStore();
const replyContent = ref("");
const submitting = ref(false);
const previewImage = ref("");
const replyTarget = ref(null);
const likeBurst = ref(false);
const showReportModal = ref(false);
const reportDetail = ref("");
const reportScreenshot = ref("");
const reportUploadError = ref("");
const reporting = ref(false);
const replyPasteHint = ref("");
const replyMarkdownMode = ref("split");
const replyEditorRef = ref(null);
const replyEditorLineNumbers = computed(() => {
  const lines = String(replyContent.value || "").split("\n").length;
  return Array.from({ length: Math.max(1, lines) }, (_, index) => index + 1);
});

async function insertReplyMarkdown(before, after = "") {
  const textarea = replyEditorRef.value;
  if (!textarea) {
    replyContent.value += `${before}${after}`;
    return;
  }
  const start = textarea.selectionStart;
  const end = textarea.selectionEnd;
  const selected = replyContent.value.slice(start, end);
  replyContent.value = `${replyContent.value.slice(0, start)}${before}${selected}${after}${replyContent.value.slice(end)}`;
  await nextTick();
  textarea.focus();
  textarea.setSelectionRange(start + before.length, start + before.length + selected.length);
}

async function insertReplyTemplate() {
  const template = `一句话写清楚这次分享、求助或讨论的核心信息。\n\n补充背景：这里写你已经确认的信息、限制条件、适用范围或当前进展。\n\n官网 / 资料：[链接文字](https://example.com)\n\n> 重点一：用引用块承载价格、步骤、实验条件或关键结论。\n> 重点二：多行内容会保持成一个视觉块，发布后更接近公告式排版。\n\n## 社群 / 补充\n\n这里放联系方式、数据说明、复现实验条件或后续更新。`;
  if (replyContent.value.trim()) {
    await insertReplyMarkdown(`\n\n${template}`, "");
  } else {
    replyContent.value = template;
    await nextTick();
    replyEditorRef.value?.focus();
  }
}

function clearReplyContent() {
  if (confirm("确定清空回复内容吗？")) {
    replyContent.value = "";
  }
}

const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true });
const defaultValidateLink = markdown.validateLink;
markdown.validateLink = (url) => /^data:(image|application|text)\//i.test(url) || defaultValidateLink(url);

const post = computed(() => forumStore.state.posts.find(item => item.id === route.params.id));

onMounted(async () => {
  if (!post.value) await forumStore.fetchPosts();
  if (post.value) await forumStore.viewPost(post.value.id);
});

function typeClass(type) {
  return {
    "数据集求助": "dataset",
    "科研羊毛": "benefit",
    "论文期刊": "paper",
    "研究讨论": "research",
    "比赛组队": "competition",
    "摸鱼专区": "fish"
  }[type] || "research";
}

function normalizeLink(value) {
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

function renderMarkdown(value) {
  let cleaned = String(value || "")
    .replace(/<!--\s*(?:图片|附件)\s*[:：]\s*.*?-->/gi, "")
    .replace(/(?:图片|附件)\s*[:：]\s*[^\n\r]+/gi, "")
    .trim();
  return markdown.render(cleaned || "_暂无内容_");
}

function handleMarkdownImageClick(event) {
  const target = event.target;
  if (target instanceof HTMLImageElement && target.src) {
    previewImage.value = target.currentSrc || target.src;
  }
}

function openAuthorCard() {
  if (!post.value?.authorUserId) return;
  userCardStore.open(post.value.authorUserId);
}

async function likePostWithBurst() {
  if (!post.value) return;
  likeBurst.value = false;
  requestAnimationFrame(() => {
    likeBurst.value = true;
    window.setTimeout(() => { likeBurst.value = false; }, 520);
  });
  await forumStore.likePost(post.value.id);
}

async function submitReply() {
  const content = replyContent.value.trim();
  if (!content || submitting.value) return;
  submitting.value = true;
  try {
    await forumStore.addReply(post.value.id, {
      content,
      author: authStore.profile.name,
      replyToReplyId: replyTarget.value?.id || "",
      replyToAuthor: replyTarget.value?.author || ""
    });
    replyContent.value = "";
    replyTarget.value = null;
    replyMarkdownMode.value = "split";
  } finally {
    submitting.value = false;
  }
}

function openReportModal() {
  showReportModal.value = true;
  reportDetail.value = "";
  reportScreenshot.value = "";
  reportUploadError.value = "";
}

function closeReportModal() {
  showReportModal.value = false;
  reportDetail.value = "";
  reportScreenshot.value = "";
  reportUploadError.value = "";
}

async function handleReportScreenshotUpload(event) {
  const file = event.target.files?.[0];
  event.target.value = "";
  if (!file) return;
  if (!file.type?.startsWith("image/")) {
    reportUploadError.value = "请上传图片格式的截图。";
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    reportUploadError.value = "截图图片不能超过 5MB。";
    return;
  }
  try {
    const dataUrl = await fileToDataUrl(file);
    reportScreenshot.value = dataUrl;
    reportUploadError.value = "";
  } catch (e) {
    reportUploadError.value = "文件读取失败";
  }
}

async function submitReport() {
  if (!post.value || reportDetail.value.length < 6 || reporting.value) return;
  reporting.value = true;
  try {
    await forumStore.reportPost(post.value.id, {
      detail: reportDetail.value,
      screenshot: reportScreenshot.value
    });
    closeReportModal();
  } finally {
    reporting.value = false;
  }
}

function setReplyTarget(reply) {
  replyTarget.value = reply;
  replyContent.value = replyContent.value || `@${reply.author} `;
}

async function handleReplyPaste(event) {
  const items = Array.from(event.clipboardData?.items || []);
  const imageItem = items.find((item) => item.type?.startsWith("image/"));
  if (!imageItem) return;
  const file = imageItem.getAsFile();
  if (!file) return;
  event.preventDefault();
  if (file.size > 4 * 1024 * 1024) {
    replyPasteHint.value = "图片超过 4MB，请压缩后再粘贴。";
    return;
  }
  const dataUrl = await fileToDataUrl(file);
  replyContent.value = `${replyContent.value.trim()}\n\n![粘贴图片](${dataUrl})\n\n`;
  replyPasteHint.value = "图片已插入回复正文。";
  window.setTimeout(() => { replyPasteHint.value = ""; }, 2200);
}

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function avatarUrlFor(postOrReply) {
  if (String(postOrReply?.authorUserId || "") === String(authStore.profile.userId || "")) {
    return authStore.profile.avatarUrl || "";
  }
  return postOrReply?.avatarUrl || "";
}

function membershipClass(plan) {
  return `member-${normalizeMembershipPlan(plan)}`;
}

function normalizeMembershipPlan(plan) {
  return ({ light: "lite", study: "plus", lab: "pro", team: "team_plus" })[plan] || plan || "free";
}

</script>

<style scoped>
/* ═══════════════════════════════════════════════════════════
   ForumPostView — Clean Dual-Theme Design
   ═══════════════════════════════════════════════════════════ */

/* ── Variables ────────────────────────────────────────────── */
.post-detail-page {
  --c-bg:       #f5f6f8;
  --c-surface:  #ffffff;
  --c-border:   #e8edf4;
  --c-text:     #0f172a;
  --c-muted:    #64748b;
  --c-subtle:   #94a3b8;
  --c-accent:   #6366f1;
  --c-accent2:  #a855f7;
  --sh:  0 2px 8px rgba(15,23,42,.06), 0 8px 24px rgba(15,23,42,.04);
  --r:   16px; --r-sm: 10px; --r-pill: 999px;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  padding: 28px clamp(16px,4vw,52px) 80px;
  font-family: Inter, "PingFang SC", system-ui, sans-serif;
  transition: background .3s, color .3s;
}
:root[data-theme="dark"] .post-detail-page {
  --c-bg:      #09090e;
  --c-surface: #111827;
  --c-border:  rgba(255,255,255,.08);
  --c-text:    #f1f5f9;
  --c-muted:   #94a3b8;
  --c-subtle:  #64748b;
}
button, textarea { font: inherit; cursor: pointer; }

/* ── Back button ─────────────────────────────────────────── */
.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 18px;
  margin-bottom: 22px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 700;
  box-shadow: var(--sh);
  transition: all .18s;
}
.back-button:hover { border-color: var(--c-accent); color: var(--c-accent); }

/* ── Two-column layout ───────────────────────────────────── */
.detail-layout {
  display: grid;
  grid-template-columns: minmax(0,1fr) 260px;
  gap: 20px;
  align-items: start;
  max-width: 100%;
  margin: 0 auto;
}
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; } }

/* ── Article card ────────────────────────────────────────── */
.post-article {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  box-shadow: var(--sh);
  padding: 28px 32px;
  color: var(--c-text);
}

/* Author row */
.author-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 6px;
}
.author-profile-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 10px 4px 4px;
  border-radius: 12px;
  transition: background .15s;
}
.author-profile-trigger[data-user-id]:hover { background: rgba(99,102,241,.07); }

.avatar, .comment-avatar {
  width: 42px; height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 900;
  color: #fff;
  background: linear-gradient(135deg,#6366f1,#a855f7);
}
.avatar-img, .comment-avatar-img {
  width: 42px; height: 42px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.author-profile-trigger > div { display: flex; flex-direction: column; gap: 4px; }

.member-name { font-size: 14px; font-weight: 800; color: var(--c-text); }
.member-free  { color: var(--c-muted); }
.member-light,
.member-lite { color: #10b981; }
.member-study,
.member-plus {
  color: #8b5cf6;
  text-shadow: 0 0 14px rgba(139, 92, 246, .2);
}
.member-lab,
.member-pro {
  color: #5b6dff;
  background: linear-gradient(100deg, #8b5cf6 0%, #3b82f6 48%, #14b8a6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 16px rgba(59, 130, 246, .18);
}
.member-team  { color: #f97316; }
.member-team_plus { color: #a855f7; }

.author-row small { display: flex; align-items: center; gap: 7px; color: var(--c-muted); }
.type-label {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 900;
}
.type-label.dataset    { color: #2563eb; background: #dbeafe; }
.type-label.benefit    { color: #d97706; background: #fef3c7; }
.type-label.paper      { color: #7c3aed; background: #ede9fe; }
.type-label.research   { color: #059669; background: #d1fae5; }
.type-label.competition{ color: #e11d48; background: #ffe4e6; }
.type-label.fish       { color: #0891b2; background: #cffafe; }
:root[data-theme="dark"] .type-label.dataset    { color: #93c5fd; background: rgba(37,99,235,.2); }
:root[data-theme="dark"] .type-label.benefit    { color: #fcd34d; background: rgba(180,83,9,.2); }
:root[data-theme="dark"] .type-label.paper      { color: #c4b5fd; background: rgba(109,40,217,.2); }
:root[data-theme="dark"] .type-label.research   { color: #6ee7b7; background: rgba(6,78,59,.2); }
:root[data-theme="dark"] .type-label.competition{ color: #fda4af; background: rgba(159,18,57,.2); }
:root[data-theme="dark"] .type-label.fish       { color: #67e8f9; background: rgba(8,145,178,.2); }

.direction-pill {
  padding: 3px 10px;
  border-radius: var(--r-pill);
  font-size: 11px;
  font-weight: 800;
  background: rgba(99,102,241,.1);
  color: var(--c-accent);
}

.article-meta-stack { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.article-meta-stack > div { display: flex; align-items: center; gap: 8px; }
.article-meta-stack time { color: var(--c-subtle); font-size: 12px; }

.state-badge { padding: 4px 9px; border-radius: 7px; font-size: 11px; font-weight: 800; }
.pin-badge { color: #dc2626; background: rgba(220,38,38,.1); }
.ban-badge { color: #be123c; background: rgba(190,18,60,.1); }

.message-author {
  height: 32px;
  padding: 0 14px;
  border-radius: var(--r-pill);
  border: 1px solid rgba(99,102,241,.25);
  background: rgba(99,102,241,.07);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 750;
  transition: all .18s;
}
.message-author:hover { background: var(--c-accent); color: #fff; border-color: transparent; }

/* Article title */
.post-article h1 {
  margin: 20px 0 18px;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.45;
  color: var(--c-text);
}
.post-article.pinned h1 { color: #dc2626; }

/* Article content — markdown */
.article-content {
  color: var(--c-text);
  font-size: 15px;
  line-height: 1.9;
  padding-bottom: 20px;
}
.markdown-rendered :deep(h1),
.markdown-rendered :deep(h2),
.markdown-rendered :deep(h3) { margin: 20px 0 10px; color: var(--c-text); line-height: 1.35; }
.markdown-rendered :deep(h1) { font-size: 22px; }
.markdown-rendered :deep(h2) { font-size: 18px; }
.markdown-rendered :deep(h3) { font-size: 15px; }
.markdown-rendered :deep(p) { margin: 0 0 16px; }
.markdown-rendered :deep(p:last-child) { margin-bottom: 0; }
.markdown-rendered :deep(ul),
.markdown-rendered :deep(ol) { margin: 10px 0 14px; padding-left: 22px; }
.markdown-rendered :deep(li) { margin: 5px 0; }
.markdown-rendered :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 18px;
  border-left: 3px solid var(--c-accent);
  background: rgba(99,102,241,.05);
  color: var(--c-muted);
  border-radius: 0 var(--r-sm) var(--r-sm) 0;
}
.markdown-rendered :deep(code) {
  padding: 2px 6px;
  border-radius: 5px;
  color: var(--c-accent);
  background: rgba(99,102,241,.08);
  font-size: .9em;
}
.markdown-rendered :deep(pre) {
  overflow: auto;
  margin: 14px 0;
  padding: 14px 18px;
  border-radius: var(--r-sm);
  color: #e2e8f0;
  background: #0f172a;
  border: 1px solid var(--c-border);
}
.markdown-rendered :deep(pre code) { padding: 0; color: inherit; background: transparent; }
.markdown-rendered :deep(a) { color: var(--c-accent); font-weight: 750; text-decoration: none; }
.markdown-rendered :deep(a:hover) { text-decoration: underline; }
.markdown-rendered :deep(img) { max-width: min(400px,100%); display: block; margin: 18px 0; border: 1px solid var(--c-border); border-radius: var(--r-sm); }
.comment-content :deep(img) {
  max-width: min(260px, 100%);
  max-height: 170px;
  object-fit: contain;
  margin: 10px 0;
  cursor: zoom-in;
  background: var(--c-surface);
}
.markdown-rendered :deep(table) { width: 100%; margin: 12px 0; border-collapse: collapse; font-size: 13px; }
.markdown-rendered :deep(th),
.markdown-rendered :deep(td) { padding: 9px 12px; border: 1px solid var(--c-border); text-align: left; }
.markdown-rendered :deep(th) { background: var(--c-bg); font-weight: 800; color: var(--c-text); }

/* Images, attachments */
.article-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}
.article-images button {
  flex: 0 0 220px;
  max-width: 100%;
  height: 165px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  background: var(--c-bg);
  cursor: zoom-in;
}
.article-images img { width: 100%; height: 100%; object-fit: cover; transition: transform .2s; }
.article-images button:hover img { transform: scale(1.03); }

.article-files { display: flex; flex-direction: column; gap: 8px; margin-bottom: 18px; }
.article-files a {
  display: grid;
  grid-template-columns: 36px minmax(0,1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  background: var(--c-bg);
  color: var(--c-text);
  text-decoration: none;
  transition: border-color .18s;
}
.article-files a:hover { border-color: var(--c-accent); }
.article-files a > span { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 9px; color: #fff; background: linear-gradient(135deg,#6366f1,#a855f7); font-size: 9px; font-weight: 900; }
.article-files strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12.5px; color: var(--c-text); }
.article-files small { color: var(--c-subtle); font-size: 11px; }

/* Resource card */
.resource-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border: 1px solid rgba(99,102,241,.2);
  border-radius: var(--r-sm);
  background: rgba(99,102,241,.04);
  margin-bottom: 16px;
}
.paper-info { min-width: 0; flex: 1; }
.paper-info small { display: block; color: var(--c-accent); font-size: 11px; font-weight: 800; margin-bottom: 3px; }
.paper-info strong { display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; font-size: 13px; color: var(--c-text); }
.paper-info span { font-size: 12px; color: var(--c-muted); }
.venue-info { display: flex; gap: 8px; align-items: center; font-size: 12px; margin-top: 6px; }
.venue-info strong { padding: 3px 9px; border-radius: var(--r-pill); color: #d97706; background: rgba(217,119,6,.1); font-size: 11px; }
.resource-card a { color: var(--c-accent); font-size: 12px; font-weight: 750; text-decoration: none; white-space: nowrap; }
.resource-card a:hover { text-decoration: underline; }

/* Tags */
.tag-row { display: flex; flex-wrap: wrap; gap: 7px; margin-bottom: 18px; }
.tag-row span {
  padding: 4px 12px;
  border-radius: var(--r-pill);
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  font-size: 12px;
  font-weight: 700;
  color: var(--c-muted);
}

/* Article actions */
.article-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--c-border);
}
.article-actions button {
  height: 36px;
  padding: 0 14px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: transparent;
  color: var(--c-muted);
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all .18s;
}
.like-action:hover, .like-action.active { border-color: #f43f5e; color: #f43f5e; background: rgba(244,63,94,.08); }
.report-action:hover { border-color: var(--c-accent); color: var(--c-accent); }
.article-actions > span { margin-left: auto; font-size: 12px; color: var(--c-subtle); }

/* Like burst */
.like-action { position: relative; overflow: visible; }
.like-thumb,
.comment-like-icon {
  width: 15px;
  height: 15px;
  display: inline-block;
  flex-shrink: 0;
  transform-origin: 50% 70%;
}
.comment-like-icon {
  width: 13px;
  height: 13px;
}
.like-action.burst .like-thumb { animation: like-thumb-pop .48s cubic-bezier(.2,.9,.2,1.25); }
.like-action.burst::after {
  content: "+1"; position: absolute; top: -18px; right: 8px;
  color: #f43f5e; font-size: 12px; font-weight: 900;
  animation: like-count-float .5s ease-out forwards;
}
@keyframes like-thumb-pop {
  0% { transform: scale(.82) rotate(-8deg); }
  45% { transform: scale(1.38) rotate(7deg); }
  100% { transform: scale(1) rotate(0); }
}
@keyframes like-count-float {
  from { opacity: 0; transform: translateY(6px); }
  20% { opacity: 1; }
  to { opacity: 0; transform: translateY(-8px); }
}

/* ── Comments card ───────────────────────────────────────── */
.comments-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  box-shadow: var(--sh);
  padding: 24px 28px;
}
.comments-card > header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--c-border);
}
.comments-card > header span {
  display: block;
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: .1em;
  text-transform: uppercase;
  color: var(--c-accent);
  margin-bottom: 3px;
}
.comments-card h2 { margin: 0; font-size: 18px; font-weight: 900; color: var(--c-text); }
.comments-card > header > strong {
  width: 34px; height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: rgba(99,102,241,.1);
  color: var(--c-accent);
  font-size: 15px;
  font-weight: 900;
}

/* Comment editor */
.comment-editor {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  margin-bottom: 18px;
}
.reply-target-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 7px 12px;
  border-radius: 8px;
  background: rgba(99,102,241,.08);
  color: var(--c-accent);
  font-size: 12.5px;
  font-weight: 750;
}
.reply-target-bar button { border: none; background: transparent; color: var(--c-subtle); font-size: 12px; }
.comment-editor textarea {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  color: var(--c-text);
  font-size: 14px;
  line-height: 1.7;
  min-height: 90px;
}
.comment-editor textarea::placeholder { color: var(--c-subtle); }
.reply-submit-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid var(--c-border);
  margin-top: 8px;
}
.reply-submit-row > span { font-size: 12px; color: var(--c-subtle); }
.reply-submit-row button {
  height: 36px;
  padding: 0 18px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2));
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 3px 10px rgba(99,102,241,.28);
  transition: all .2s;
}
.reply-submit-row button:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(99,102,241,.38); }
.reply-submit-row button:disabled { opacity: .45; cursor: not-allowed; transform: none; }

.reply-markdown-editor {
  overflow: hidden;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  background: var(--c-surface);
}

.markdown-tabbar {
  display: flex;
  gap: 2px;
  padding: 0 10px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-bg);
}

.markdown-tabbar button,
.markdown-toolbar button,
.markdown-hints button {
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--c-subtle);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.markdown-tabbar button {
  height: 38px;
  padding: 0 12px;
  border-radius: 0;
}

.markdown-tabbar button.active {
  color: var(--c-accent);
  background: rgba(99,102,241,.1);
  box-shadow: inset 0 -2px 0 var(--c-accent);
}

.markdown-toolbar {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 9px 12px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-bg);
}

.markdown-toolbar button {
  min-width: 30px;
  height: 28px;
  padding: 0 9px;
  border: 0;
  background: transparent;
  color: var(--c-text);
}

.markdown-toolbar button:hover,
.markdown-hints button:hover {
  color: var(--c-accent);
  border-color: rgba(99,102,241,.35);
  background: rgba(99,102,241,.08);
}

.markdown-body {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  min-height: 310px;
  background: var(--c-surface);
}

.markdown-line-number {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0;
  padding: 13px 9px 13px 0;
  border-right: 1px solid var(--c-border);
  background: var(--c-bg);
  color: var(--c-subtle);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
  user-select: none;
}

.reply-markdown-editor textarea {
  width: 100%;
  min-height: 310px;
  padding: 15px 16px;
  border: 0;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
  background: transparent;
  color: var(--c-text);
  font-size: 14px;
  line-height: 1.7;
}

.markdown-split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  min-height: 340px;
}

.markdown-split .markdown-rendered {
  min-height: 340px;
  max-height: 480px;
  overflow: auto;
  padding: 24px 28px;
  border-left: 1px solid var(--c-border);
  background: var(--c-bg);
}

.reply-preview-box {
  min-height: 310px;
  max-height: 480px;
  overflow: auto;
  padding: 24px 28px;
  background: var(--c-bg);
}

.markdown-hints {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-top: 1px solid var(--c-border);
  color: var(--c-subtle);
  font-size: 12px;
}

.markdown-hints button {
  height: 28px;
  padding: 0 10px;
  border: 0;
  color: var(--c-accent);
  background: transparent;
}

@media (max-width: 820px) {
  .markdown-split {
    grid-template-columns: 1fr;
  }

  .markdown-split .markdown-rendered {
    border-left: 0;
    border-top: 1px solid var(--c-border);
  }
}

/* Comment list */
.comment-list { display: flex; flex-direction: column; gap: 12px; }
.comment-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--r-sm);
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  transition: border-color .18s;
}
.comment-item:hover { border-color: rgba(99,102,241,.2); }
.comment-avatar, .comment-avatar-img { width: 34px; height: 34px; }
.comment-item > div { flex: 1; min-width: 0; }
.comment-item header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.comment-item header strong { font-size: 13px; font-weight: 800; color: var(--c-text); }
.comment-item time { color: var(--c-subtle); font-size: 11.5px; }
.comment-item header button {
  margin-left: auto;
  height: auto;
  padding: 3px 10px;
  border: 1px solid var(--c-border);
  background: transparent;
  border-radius: var(--r-pill);
  color: var(--c-subtle);
  font-size: 12px;
  font-weight: 700;
  box-shadow: none;
  transition: all .15s;
}
.comment-item header button:hover { border-color: var(--c-accent); color: var(--c-accent); }
.comment-item header button + button { margin-left: 0; }
.comment-item header button.active { color: var(--c-accent); border-color: rgba(99,102,241,.3); }

.reply-to-note {
  display: inline-block;
  margin: 6px 0 4px;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: rgba(99,102,241,.08);
  color: var(--c-accent);
  font-size: 11.5px;
  font-weight: 750;
}
.comment-content {
  margin-top: 8px;
  color: var(--c-muted);
  font-size: 13.5px;
  line-height: 1.75;
}
.empty-comments {
  padding: 40px;
  text-align: center;
  color: var(--c-subtle);
  background: var(--c-bg);
  border-radius: var(--r-sm);
  font-size: 13px;
  border: 1px dashed var(--c-border);
}

/* ── Sidebar ─────────────────────────────────────────────── */
aside {
  position: sticky;
  top: 100px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
@media (max-width: 900px) { aside { position: static; } }

.side-card {
  padding: 20px 22px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  box-shadow: var(--sh);
}
.side-kicker {
  display: block;
  font-size: 10.5px;
  font-weight: 900;
  letter-spacing: .1em;
  text-transform: uppercase;
  color: var(--c-accent);
  margin-bottom: 4px;
}
.side-card h3 { margin: 0 0 14px; font-size: 15px; font-weight: 900; color: var(--c-text); }

.side-card dl { margin: 0; }
.side-card dl div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--c-border);
  font-size: 12.5px;
}
.side-card dt { color: var(--c-muted); }
.side-card dd { margin: 0; color: var(--c-text); font-weight: 750; text-align: right; }

.notice-card { background: rgba(99,102,241,.04); border-color: rgba(99,102,241,.15); }
.notice-card p { margin: 0; color: var(--c-muted); font-size: 12.5px; line-height: 1.75; }

/* ── Misc ────────────────────────────────────────────────── */
.detail-status {
  padding: 80px 30px;
  text-align: center;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r);
  max-width: 520px;
  margin: 0 auto;
  color: var(--c-text);
}
.detail-status button {
  margin-top: 16px;
  height: 40px;
  padding: 0 20px;
  border-radius: var(--r-pill);
  border: none;
  background: linear-gradient(135deg,var(--c-accent),var(--c-accent2));
  color: #fff;
  font-weight: 800;
}

.preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: grid;
  place-items: center;
  padding: 40px;
  background: rgba(0,0,0,.82);
}
.preview-overlay img { max-width: 100%; max-height: 90vh; object-fit: contain; border-radius: var(--r); }
.preview-overlay > button {
  position: absolute;
  top: 24px; right: 24px;
  width: 40px; height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,.2);
  background: rgba(255,255,255,.12);
  color: #fff;
  font-size: 20px;
  display: grid;
  place-items: center;
}

.report-overlay { background: rgba(15,23,42,.6); backdrop-filter: blur(6px); }
.report-card {
  position: relative;
  width: min(500px, calc(100vw - 32px));
  padding: 28px 30px;
  border-radius: var(--r);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: 0 24px 70px rgba(0,0,0,.25);
  color: var(--c-text);
}
.report-close {
  position: absolute;
  top: 18px;
  right: 18px;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 10px;
  background: var(--c-bg);
  color: var(--c-muted);
  font-size: 22px;
  line-height: 1;
}
.report-close:hover {
  color: var(--c-text);
  background: rgba(99, 102, 241, .12);
}
.report-card header span { display: block; font-size: 10.5px; font-weight: 900; letter-spacing: .1em; text-transform: uppercase; color: #f43f5e; margin-bottom: 3px; }
.report-card h3 { margin: 0 0 4px; font-size: 18px; font-weight: 900; }
.report-card p { margin: 0 0 16px; color: var(--c-muted); font-size: 13px; }
.report-card label { display: grid; gap: 8px; font-size: 12.5px; font-weight: 750; color: var(--c-muted); }
.report-card textarea {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 12px 14px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  background: var(--c-bg);
  color: var(--c-text);
  outline: none;
  font-size: 14px;
  line-height: 1.65;
}
.report-card textarea:focus { border-color: #f43f5e; }
.report-card small { color: var(--c-subtle); font-size: 11.5px; }
.report-card footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
.report-card footer button {
  height: 38px;
  padding: 0 18px;
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 800;
  border: none;
}
.report-card footer button:first-child { background: var(--c-bg); color: var(--c-muted); border: 1px solid var(--c-border); }
.report-card footer button:last-child  { background: #be123c; color: #fff; }
.report-card footer button:disabled { opacity: .45; cursor: not-allowed; }

:root[data-theme="dark"] .report-overlay {
  background: rgba(3, 7, 18, .72);
}
:root[data-theme="dark"] .report-card {
  background: #111827;
  border-color: rgba(148, 163, 184, .16);
  box-shadow: 0 28px 80px rgba(0, 0, 0, .62);
  color: #f8fafc;
}
:root[data-theme="dark"] .report-card h3 {
  color: #f8fafc;
}
:root[data-theme="dark"] .report-card p,
:root[data-theme="dark"] .report-card label,
:root[data-theme="dark"] .report-card small {
  color: #94a3b8;
}
:root[data-theme="dark"] .report-card textarea,
:root[data-theme="dark"] .report-upload-box {
  background: #0b1220 !important;
  border-color: rgba(148, 163, 184, .2) !important;
  color: #e2e8f0 !important;
}
:root[data-theme="dark"] .report-card textarea::placeholder {
  color: #64748b;
}
:root[data-theme="dark"] .report-close,
:root[data-theme="dark"] .report-card footer button:first-child {
  background: #1e293b;
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, .18);
}
:root[data-theme="dark"] .report-card footer button:last-child {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

@media (max-width: 640px) {
  .post-detail-page { padding-inline: 14px; }
  .post-article { padding: 20px; }
  .comments-card { padding: 18px; }
}
</style>
