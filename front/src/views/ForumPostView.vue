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
          <article class="post-article">
            <div class="label-row">
              <span class="type-label" :class="typeClass(post.postType)">{{ post.postType }}</span>
              <span v-if="post.pinned" class="state-badge pin-badge">📌 置顶</span>
              <span v-if="post.banned" class="state-badge ban-badge">已封禁</span>
              <span>{{ post.direction }}</span>
              <time>{{ post.time }}</time>
            </div>

            <h1>{{ post.title }}</h1>
            <div class="author-row">
              <div class="author-profile-trigger" :data-user-id="post.authorUserId" title="查看个人卡片">
                <img v-if="avatarUrlFor(post)" :src="avatarUrlFor(post)" class="avatar-img" :alt="post.author" />
                <span v-else class="avatar">{{ post.avatar }}</span>
                <div>
                  <strong>{{ post.author }}</strong>
                  <small>发布于 {{ post.direction }}</small>
                </div>
              </div>
              <button v-if="post.authorUserId" class="message-author" @click="messageAuthor">私信作者</button>
            </div>

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
              <button :class="{ active: post.hasLiked }" @click="forumStore.likePost(post.id)">赞同 {{ post.likes }}</button>
              <button :class="{ active: post.hasBookmarked }" @click="forumStore.bookmarkPost(post.id)">收藏 {{ post.bookmarks }}</button>
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
              <textarea v-model="replyContent" rows="4" placeholder="提供数据线索、方法建议或可验证的研究观点"></textarea>
              <div>
                <span>以 {{ authStore.profile.name }} 身份回复</span>
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
                    <strong>{{ reply.author }}</strong>
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
              <div><dt>所属方向</dt><dd>{{ post.direction }}</dd></div>
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
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true });

const post = computed(() => forumStore.state.posts.find(item => item.id === route.params.id));

onMounted(async () => {
  if (!post.value) await forumStore.fetchPosts();
});

function typeClass(type) {
  return {
    "数据集求助": "dataset",
    "科研羊毛": "benefit",
    "论文期刊": "paper",
    "研究讨论": "research",
    "比赛组队": "competition"
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

function setReplyTarget(reply) {
  replyTarget.value = reply;
  replyContent.value = replyContent.value || `@${reply.author} `;
}

function avatarUrlFor(postOrReply) {
  if (String(postOrReply?.authorUserId || "") === String(authStore.profile.userId || "")) {
    return authStore.profile.avatarUrl || "";
  }
  return postOrReply?.avatarUrl || "";
}
</script>

<style scoped>
.post-detail-page { width: min(1320px, calc(100vw - 48px)); margin: 0 auto; padding: 28px 0 80px; color: #172033; font-family: Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", sans-serif; }
button, textarea { font: inherit; }
.back-button { margin-bottom: 16px; padding: 8px 12px; border: 0; border-radius: 9px; color: #526077; background: #fff; cursor: pointer; }
.back-button:hover { color: #0865ee; }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 290px; gap: 18px; align-items: start; }
.post-article, .comments-card, .side-card, .detail-status { background: #fff; border: 1px solid #e5eaf1; border-radius: 20px; box-shadow: 0 10px 32px rgba(36, 57, 94, .05); }
.post-article { padding: 28px 30px 22px; }
.label-row { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; color: #59667b; font-size: 11px; }
.label-row > span { padding: 5px 9px; border-radius: 7px; background: #eef2f7; font-weight: 700; }
.label-row .type-label.dataset { color: #1267e8; background: #e7f0ff; }
.label-row .type-label.benefit { color: #b86500; background: #fff1d8; }
.label-row .type-label.paper { color: #6554d9; background: #eeeaff; }
.label-row .type-label.research { color: #087d5e; background: #ddf7ef; }
.label-row .type-label.competition { color: #c83e5d; background: #ffe8ee; }
.state-badge { padding: 5px 9px; border-radius: 7px; font-size: 11px; font-weight: 900; }
.pin-badge { color: #075ee5; background: #eaf2ff; }
.ban-badge { color: #b4233a; background: #fff0f2; }
.label-row time { margin-left: auto; color: #97a1b1; }
.post-article h1 { margin: 20px 0 14px; font-size: clamp(25px, 3vw, 36px); line-height: 1.35; letter-spacing: -.025em; }
.author-row { display: flex; align-items: center; gap: 10px; padding-bottom: 20px; border-bottom: 1px solid #edf0f4; }
.author-profile-trigger { display: flex; align-items: center; gap: 10px; margin-left: -4px; padding: 4px 8px 4px 4px; border-radius: 11px; transition: color .18s ease, background-color .18s ease; }
.author-profile-trigger[data-user-id]:hover { color: #075ee5; background: #f1f6ff; }
.avatar, .comment-avatar { width: 38px; height: 38px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #176ce4, #643bd4); font-size: 11px; font-weight: 800; }
.avatar-img, .comment-avatar-img { width: 38px; height: 38px; flex: 0 0 auto; border-radius: 50%; object-fit: cover; }
.author-profile-trigger > div { display: flex; flex-direction: column; gap: 3px; }
.author-row strong { font-size: 13px; }
.author-row small { color: #96a0b0; }
.message-author { margin-left: auto; padding: 7px 12px; border: 1px solid #cfe0fb; border-radius: 8px; color: #0865ee; background: #f4f8ff; font-size: 11px; font-weight: 800; cursor: pointer; }
.article-content { padding: 24px 0; color: #445166; font-size: 15px; line-height: 2; }
.markdown-rendered :deep(h1),
.markdown-rendered :deep(h2),
.markdown-rendered :deep(h3) { margin: 18px 0 10px; color: #172033; line-height: 1.35; letter-spacing: 0; }
.markdown-rendered :deep(h1) { font-size: 24px; }
.markdown-rendered :deep(h2) { font-size: 19px; }
.markdown-rendered :deep(h3) { font-size: 16px; }
.markdown-rendered :deep(p) { margin: 0 0 12px; }
.markdown-rendered :deep(p:last-child) { margin-bottom: 0; }
.markdown-rendered :deep(ul),
.markdown-rendered :deep(ol) { margin: 10px 0 14px; padding-left: 22px; }
.markdown-rendered :deep(li) { margin: 6px 0; }
.markdown-rendered :deep(blockquote) { margin: 12px 0; padding: 10px 14px; border-left: 4px solid #2f6fec; border-radius: 0 10px 10px 0; color: #3a4960; background: #f3f7ff; }
.markdown-rendered :deep(code) { padding: 2px 5px; border-radius: 5px; color: #0f4fb7; background: #eef4ff; font-size: .92em; }
.markdown-rendered :deep(pre) { overflow: auto; margin: 12px 0; padding: 13px; border-radius: 12px; color: #dbe7ff; background: #121b2e; }
.markdown-rendered :deep(pre code) { padding: 0; color: inherit; background: transparent; }
.markdown-rendered :deep(a) { color: #075ee5; font-weight: 800; text-decoration: none; }
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
