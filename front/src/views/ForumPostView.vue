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
                <button v-if="post.authorUserId" class="message-author" @click="messageAuthor">私信作者</button>
              </div>
            </div>

            <h1>{{ post.title }}</h1>
            <div class="article-content markdown-rendered" v-html="renderMarkdown(post.content)"></div>

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
                <span class="like-flame">🔥</span>
                赞同 {{ post.likes }}
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
              <textarea
                v-model="replyContent"
                rows="4"
                placeholder="提供数据线索、方法建议或可验证的研究观点；可直接粘贴图片"
                @paste="handleReplyPaste"
              ></textarea>
              <div>
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
                    <button :class="{ active: reply.hasLiked }" @click="forumStore.likeReply(post.id, reply.id)">赞同 {{ reply.likes }}</button>
                    <button @click="setReplyTarget(reply)">回复</button>
                  </header>
                  <small v-if="reply.replyToAuthor" class="reply-to-note">回复 {{ reply.replyToAuthor }}</small>
                  <div class="comment-content markdown-rendered" v-html="renderMarkdown(reply.content)"></div>
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
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import MarkdownIt from "markdown-it";
import { useAuthStore } from "../stores/auth";
import { useForumStore } from "../stores/forum";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const forumStore = useForumStore();
const replyContent = ref("");
const submitting = ref(false);
const previewImage = ref("");
const replyTarget = ref(null);
const likeBurst = ref(false);
const showReportModal = ref(false);
const reportDetail = ref("");
const reporting = ref(false);
const replyPasteHint = ref("");
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
  return markdown.render(String(value || "").trim() || "_暂无内容_");
}

function messageAuthor() {
  router.push({ path: "/messages", query: { contact: post.value.authorUserId } });
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
  } finally {
    submitting.value = false;
  }
}

function openReportModal() {
  showReportModal.value = true;
  reportDetail.value = "";
}

function closeReportModal() {
  showReportModal.value = false;
  reportDetail.value = "";
}

async function submitReport() {
  if (!post.value || reportDetail.value.length < 6 || reporting.value) return;
  reporting.value = true;
  try {
    await forumStore.reportPost(post.value.id, { detail: reportDetail.value });
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
  return `member-${plan || "free"}`;
}

</script>

<style scoped>
.post-detail-page { width: min(1240px, calc(100vw - 48px)); margin: 0 auto; padding: 28px 0 80px; color: #172033; font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif; }
button, textarea { font: inherit; }
.back-button { margin-bottom: 16px; padding: 8px 12px; border: 0; border-radius: 9px; color: #526077; background: #fff; cursor: pointer; }
.back-button:hover { color: #0865ee; }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 250px; gap: 18px; align-items: start; }
.post-article, .comments-card, .side-card, .detail-status { background: #fff; border: 1px solid #e5eaf1; border-radius: 20px; box-shadow: 0 10px 32px rgba(36, 57, 94, .05); }
.post-article { padding: 26px 34px 24px; }
.label-row { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; color: #59667b; font-size: 11px; }
.label-row > span { padding: 5px 9px; border-radius: 7px; background: #eef2f7; font-weight: 700; }
.type-label { display: inline-flex; align-items: center; padding: 3px 7px; border-radius: 6px; font-size: 11px; font-weight: 900; }
.type-label.dataset { color: #1267e8; background: #e7f0ff; }
.type-label.benefit { color: #b86500; background: #fff1d8; }
.type-label.paper { color: #6554d9; background: #eeeaff; }
.type-label.research { color: #087d5e; background: #ddf7ef; }
.type-label.competition { color: #c83e5d; background: #ffe8ee; }
.type-label.fish { color: #0f766e; background: #dff7f2; }
.direction-pill { padding: 3px 7px; border-radius: 999px; color: #385b85; background: #eef5fb; font-size: 11px; font-weight: 850; }
.state-badge { padding: 5px 9px; border-radius: 7px; font-size: 11px; font-weight: 900; }
.pin-badge { color: #b91c1c; background: #fee2e2; border: 1px solid #fecaca; }
.ban-badge { color: #b4233a; background: #fff0f2; }
.label-row time { margin-left: auto; color: #97a1b1; }
.post-article h1 { max-width: 760px; margin: 22px 0 18px 56px; font-size: 24px; line-height: 1.45; letter-spacing: 0; text-wrap: balance; }
.post-article.pinned h1 { color: #c81e1e; }
.author-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; padding-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.author-profile-trigger { display: flex; align-items: center; gap: 10px; margin-left: -4px; padding: 4px 8px 4px 4px; border-radius: 11px; transition: color .18s ease, background-color .18s ease; }
.author-profile-trigger[data-user-id]:hover { color: #075ee5; background: #f1f6ff; }
.avatar, .comment-avatar { width: 42px; height: 42px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #176ce4, #643bd4); font-size: 11px; font-weight: 800; }
.avatar-img, .comment-avatar-img { width: 42px; height: 42px; flex: 0 0 auto; border-radius: 50%; object-fit: cover; }
.author-profile-trigger > div { display: flex; flex-direction: column; gap: 3px; }
.author-row strong { font-size: 14px; }
.member-name { font-weight: 850; }
.member-free { color: #667085; }
.member-light { color: #12815f; }
.member-study { color: #2463eb; }
.member-lab { color: #7c3aed; text-shadow: 0 0 14px rgba(124, 58, 237, .14); }
.member-team { color: #c2410c; text-shadow: 0 0 14px rgba(194, 65, 12, .14); }
.member-team_plus { color: #a855f7; text-shadow: 0 0 14px rgba(168, 85, 247, .16); }
.author-row small { display: flex; align-items: center; gap: 7px; color: #96a0b0; }
.article-meta-stack { display: grid; justify-items: end; gap: 8px; color: #929dae; font-size: 12px; }
.article-meta-stack > div { display: flex; align-items: center; gap: 8px; }
.message-author { padding: 7px 12px; border: 1px solid #cfe0fb; border-radius: 8px; color: #0865ee; background: #f4f8ff; font-size: 11px; font-weight: 800; cursor: pointer; }
.article-content { max-width: 820px; padding: 2px 0 26px 56px; color: #1f2937; font-size: 16px; line-height: 2; }
.markdown-rendered :deep(h1),
.markdown-rendered :deep(h2),
.markdown-rendered :deep(h3) { margin: 18px 0 10px; color: #172033; line-height: 1.35; letter-spacing: 0; }
.markdown-rendered :deep(h1) { font-size: 24px; }
.markdown-rendered :deep(h2) { font-size: 19px; }
.markdown-rendered :deep(h3) { font-size: 16px; }
.markdown-rendered :deep(p) { margin: 0 0 18px; }
.markdown-rendered :deep(p:last-child) { margin-bottom: 0; }
.markdown-rendered :deep(ul),
.markdown-rendered :deep(ol) { margin: 10px 0 14px; padding-left: 22px; }
.markdown-rendered :deep(li) { margin: 6px 0; }
.markdown-rendered :deep(blockquote) { margin: 20px 0 22px; padding: 18px 22px; border: 1px solid #e5e7eb; color: #2b313d; background: #f6f6f7; box-shadow: inset 1px 0 0 #c5cbd4; }
.markdown-rendered :deep(blockquote p) { margin-bottom: 8px; }
.markdown-rendered :deep(code) { padding: 2px 5px; border-radius: 5px; color: #0f4fb7; background: #eef4ff; font-size: .92em; }
.markdown-rendered :deep(pre) { overflow: auto; margin: 12px 0; padding: 13px; border-radius: 12px; color: #dbe7ff; background: #121b2e; }
.markdown-rendered :deep(pre code) { padding: 0; color: inherit; background: transparent; }
.markdown-rendered :deep(a) { color: #075ee5; font-weight: 800; text-decoration: none; }
.markdown-rendered :deep(a:hover) { text-decoration: underline; }
.markdown-rendered :deep(img) { max-width: min(360px, 100%); display: block; margin: 20px 0; border: 1px solid #e4e9f1; border-radius: 10px; }
.markdown-rendered :deep(table) { width: 100%; margin: 12px 0; border-collapse: collapse; font-size: 13px; }
.markdown-rendered :deep(th),
.markdown-rendered :deep(td) { padding: 9px 10px; border: 1px solid #dfe7f2; text-align: left; }
.markdown-rendered :deep(th) { color: #172033; background: #f3f6fb; }
.article-images { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; margin-bottom: 18px; }
.article-images button { padding: 0; overflow: hidden; aspect-ratio: 4 / 3; border: 1px solid #e0e7f1; border-radius: 12px; background: #f5f7fa; cursor: zoom-in; }
.article-images img { width: 100%; height: 100%; object-fit: cover; }
.article-files { display: flex; flex-direction: column; gap: 8px; margin-bottom: 18px; }
.article-files a { display: grid; grid-template-columns: 36px minmax(0, 1fr) auto; align-items: center; gap: 10px; padding: 10px 12px; border: 1px solid #e0e6ef; border-radius: 11px; color: #344158; background: #fbfcfe; text-decoration: none; }
.article-files a > span { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 9px; color: #fff; background: #2e73e3; font-size: 9px; font-weight: 900; }
.article-files strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }
.article-files small { color: #929cac; font-size: 10px; }
.resource-card { padding: 15px; display: flex; align-items: center; gap: 14px; border: 1px solid #d9e6fa; border-radius: 13px; background: #f7faff; }
.paper-info { min-width: 0; flex: 1; display: grid; grid-template-columns: auto minmax(0,1fr) auto; align-items: center; gap: 9px; }
.paper-info small { color: #0865ee; font-weight: 800; }
.paper-info strong { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; font-size: 12px; }
.paper-info span { color: #8b95a5; }
.venue-info { display: flex; gap: 8px; align-items: center; font-size: 11px; }
.venue-info strong { padding: 5px 8px; border-radius: 7px; color: #805100; background: #fff0c8; }
.resource-card a { color: #0865ee; font-size: 11px; font-weight: 800; text-decoration: none; white-space: nowrap; }
.tag-row { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 18px; }
.tag-row span { padding: 5px 9px; border: 1px solid #e2e7ee; border-radius: 8px; color: #687489; font-size: 11px; }
.article-actions { display: flex; align-items: center; gap: 8px; margin-top: 20px; padding-top: 16px; border-top: 1px solid #edf0f4; }
.article-actions button { padding: 7px 12px; border: 0; border-radius: 8px; color: #697589; background: #f2f5f8; cursor: pointer; }
.article-actions button.active { color: #0865ee; background: #eaf2ff; }
.article-actions span { margin-left: auto; color: #8b95a5; font-size: 11px; }
.article-actions .report-action { color: #9b4553; background: #fff4f5; }
.article-actions .report-action:hover { color: #be123c; background: #ffe8ec; }
.like-action {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  overflow: visible;
}
.like-action.active {
  color: #d65d0e !important;
  background: #fff3e8 !important;
}
.like-flame {
  display: inline-block;
  transform-origin: 50% 70%;
}
.like-action.burst .like-flame {
  animation: like-flame-pop .48s cubic-bezier(.2,.9,.2,1.25);
}
.like-action.burst::after {
  content: "+1";
  position: absolute;
  top: -18px;
  right: 8px;
  color: #e15f13;
  font-size: 12px;
  font-weight: 900;
  animation: like-count-float .5s ease-out forwards;
}
@keyframes like-flame-pop {
  0% { transform: scale(.82) rotate(-8deg); }
  45% { transform: scale(1.38) rotate(7deg); }
  100% { transform: scale(1) rotate(0); }
}
@keyframes like-count-float {
  from { opacity: 0; transform: translateY(6px); }
  20% { opacity: 1; }
  to { opacity: 0; transform: translateY(-8px); }
}
.comments-card { margin-top: 16px; padding: 24px 26px; }
.comments-card > header { display: flex; justify-content: space-between; align-items: center; }
.comments-card > header span, .side-kicker { color: #65a0fa; font-size: 10px; letter-spacing: .13em; font-weight: 900; }
.comments-card h2 { margin: 4px 0 0; font-size: 20px; }
.comments-card > header > strong { width: 35px; height: 35px; display: grid; place-items: center; border-radius: 10px; color: #0865ee; background: #eaf2ff; }
.comment-editor { margin: 20px 0; padding: 13px; border: 1px solid #dde4ed; border-radius: 13px; }
.reply-target-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 9px; padding: 8px 10px; border-radius: 9px; color: #075ee5; background: #edf4ff; font-size: 12px; font-weight: 800; }
.reply-target-bar button { border: 0; color: #667085; background: transparent; font-size: 11px; }
.comment-editor textarea { width: 100%; border: 0; outline: 0; resize: vertical; box-sizing: border-box; color: #344157; }
.comment-editor > div { display: flex; justify-content: space-between; align-items: center; padding-top: 10px; border-top: 1px solid #edf0f4; color: #8b95a5; font-size: 10px; }
.comment-editor button { padding: 8px 14px; border: 0; border-radius: 8px; color: #fff; background: #0865ee; font-weight: 800; cursor: pointer; }
.comment-editor button:disabled { opacity: .45; cursor: not-allowed; }
.comment-list { display: flex; flex-direction: column; gap: 10px; }
.comment-item { display: flex; gap: 11px; padding: 15px; border-radius: 13px; background: #f7f9fc; }
.comment-avatar, .comment-avatar-img { width: 32px; height: 32px; }
.comment-item > div { min-width: 0; flex: 1; }
.comment-item header { display: flex; align-items: center; gap: 10px; }
.comment-item header strong { font-size: 12px; }
.comment-item time { color: #98a2b1; font-size: 10px; }
.comment-item button { margin-left: auto; border: 0; color: #8792a3; background: transparent; cursor: pointer; font-size: 10px; }
.comment-item button + button { margin-left: 0; }
.comment-item button.active { color: #0865ee; }
.reply-to-note { display: inline-block; margin-top: 7px; padding: 4px 8px; border-radius: 999px; color: #075ee5; background: #edf4ff; font-size: 10px; font-weight: 800; }
.comment-content { margin-top: 8px; color: #4e5b70; font-size: 12px; line-height: 1.75; }
.empty-comments { padding: 40px; text-align: center; color: #929cac; background: #f8fafc; border-radius: 13px; font-size: 12px; }
aside { position: sticky; top: 116px; display: flex; flex-direction: column; gap: 14px; }
.side-card { padding: 20px; }
.side-card h3 { margin: 6px 0 14px; }
.side-card dl { margin: 0; }
.side-card dl div { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-top: 1px solid #edf0f4; font-size: 11px; }
.side-card dt { color: #8a95a6; }
.side-card dd { margin: 0; color: #344157; text-align: right; }
.notice-card { background: linear-gradient(145deg, #f5f9ff, #fff); }
.notice-card p { margin: 0; color: #647187; font-size: 11px; line-height: 1.8; }
.detail-status { padding: 80px 30px; text-align: center; }
.detail-status button { padding: 9px 16px; border: 0; border-radius: 9px; color: #fff; background: #0865ee; }
.preview-overlay { position: fixed; inset: 0; z-index: 10000; display: grid; place-items: center; padding: 40px; background: rgba(14, 22, 37, .84); }
.preview-overlay img { max-width: 100%; max-height: 100%; object-fit: contain; border-radius: 12px; }
.preview-overlay button { position: absolute; top: 24px; right: 24px; width: 40px; height: 40px; border: 0; border-radius: 50%; color: #fff; background: rgba(255,255,255,.16); font-size: 26px; }
.report-overlay { background: rgba(15, 23, 42, .55); }
.report-card { width: min(520px, calc(100vw - 32px)); padding: 22px; border-radius: 18px; background: #fff; box-shadow: 0 24px 70px rgba(14, 27, 52, .28); }
.report-card header span { color: #ef4565; font-size: 10px; font-weight: 900; letter-spacing: .14em; }
.report-card h3 { margin: 5px 0 4px; font-size: 20px; }
.report-card p { margin: 0; color: #64748b; font-size: 12px; line-height: 1.6; }
.report-card label { display: grid; gap: 8px; margin-top: 18px; color: #334155; font-size: 12px; font-weight: 850; }
.report-card textarea { width: 100%; box-sizing: border-box; resize: vertical; padding: 12px; border: 1px solid #dfe5ee; border-radius: 12px; outline: 0; color: #172033; line-height: 1.6; }
.report-card textarea:focus { border-color: #ef8fa0; box-shadow: 0 0 0 3px #fff1f3; }
.report-card small { color: #94a3b8; font-size: 11px; font-weight: 500; }
.report-card footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
.report-card footer button { position: static; width: auto; height: 38px; padding: 0 16px; border-radius: 10px; font-size: 12px; font-weight: 850; }
.report-card footer button:first-child { border: 1px solid #dfe4ec; color: #64748b; background: #fff; }
.report-card footer button:last-child { color: #fff; background: #be123c; }
.report-card footer button:disabled { opacity: .45; cursor: not-allowed; }
@media (max-width: 900px) {
  .detail-layout { grid-template-columns: 1fr; }
  aside { position: static; }
}
@media (max-width: 640px) {
  .post-detail-page { width: min(100% - 24px, 1320px); }
  .post-article, .comments-card { padding: 20px; }
  .resource-card, .paper-info { align-items: flex-start; grid-template-columns: 1fr; flex-direction: column; }
  .label-row time { width: 100%; margin-left: 0; }
}
</style>
