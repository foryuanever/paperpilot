<template>
  <div class="messages-page">
    <header class="messages-heading">
      <div>
        <span>PRIVATE MESSAGES</span>
        <h1>站内私信</h1>
        <p>与站内研究者沟通，支持图片预览、科研附件与联系人管理。</p>
      </div>
      <div class="heading-stat">
        <span class="online-dot"></span>
        <b>{{ messagesStore.state.contacts.length }} 位联系人</b>
        <strong>{{ messagesStore.state.unreadCount }} 条未读</strong>
      </div>
    </header>

    <section class="message-shell">
      <aside class="contacts-panel">
        <div class="message-tabs">
          <button :class="{ active: activePanel === 'chats' }" @click="activePanel = 'chats'">会话</button>
          <button :class="{ active: activePanel === 'requests' }" @click="activePanel = 'requests'">
            好友申请
            <b v-if="messagesStore.state.friendRequests.pendingCount">{{ messagesStore.state.friendRequests.pendingCount }}</b>
          </button>
        </div>
        <label class="contact-search">
          <span>⌕</span>
          <input v-model="keyword" placeholder="搜索联系人" />
        </label>
        <div v-if="activePanel === 'chats'" class="contact-list">
          <button
            v-for="contact in filteredContacts"
            :key="contact.userId"
            :class="{ active: contact.userId === messagesStore.state.activeContactId }"
            @click="selectContact(contact.userId)"
          >
            <span class="contact-avatar" :data-user-id="contact.userId" title="查看个人卡片">{{ contact.avatar }}</span>
            <span class="contact-copy">
              <strong>{{ contact.name }}</strong>
              <small>{{ contact.lastMessage || contact.role }}</small>
            </span>
            <span class="contact-meta">
              <time>{{ contact.lastTime }}</time>
              <b v-if="contact.unreadCount">{{ contact.unreadCount }}</b>
            </span>
          </button>
        </div>
        <div v-else class="request-list">
          <section v-if="messagesStore.state.friendRequests.incoming.length">
            <h3>收到的申请</h3>
            <article v-for="request in messagesStore.state.friendRequests.incoming" :key="request.requestId">
              <span class="request-avatar" :data-user-id="request.userId">{{ request.avatar }}</span>
              <div>
                <strong>{{ request.name }}</strong>
                <small>{{ request.role }} · {{ request.message || "希望添加你为好友" }}</small>
                <time>{{ request.time }}</time>
              </div>
              <footer>
                <button @click="handleRequest(request.requestId, 'reject')">拒绝</button>
                <button class="accept" @click="handleRequest(request.requestId, 'accept')">同意</button>
              </footer>
            </article>
          </section>
          <section v-if="messagesStore.state.friendRequests.outgoing.length">
            <h3>已发送</h3>
            <article v-for="request in messagesStore.state.friendRequests.outgoing" :key="request.requestId">
              <span class="request-avatar" :data-user-id="request.userId">{{ request.avatar }}</span>
              <div>
                <strong>{{ request.name }}</strong>
                <small>等待对方处理</small>
                <time>{{ request.time }}</time>
              </div>
              <span class="pending-pill">待通过</span>
            </article>
          </section>
          <div v-if="!hasFriendRequests" class="requests-empty">暂无好友申请</div>
        </div>
      </aside>

      <main class="conversation-panel">
        <template v-if="activeContact">
          <header class="conversation-head">
            <span class="contact-avatar" :data-user-id="activeContact.userId" title="查看个人卡片">{{ activeContact.avatar }}</span>
            <div>
              <strong>{{ activeContact.name }}</strong>
              <small>{{ activeContact.role }} · {{ activeContact.email }}</small>
            </div>
            <span class="conversation-status"><i></i> 站内联系人</span>
          </header>

          <div ref="threadElement" class="message-thread">
            <div class="thread-date"><span>科研私信记录</span></div>
            <div v-if="messagesStore.state.loading" class="thread-empty">正在加载会话...</div>
            <article
              v-for="message in messagesStore.state.messages"
              v-else
              :key="message.id"
              :class="{ mine: message.mine }"
            >
              <span v-if="!message.mine" class="message-avatar" :data-user-id="activeContact.userId">{{ activeContact.avatar }}</span>
              <div class="message-bubble">
                <p v-if="message.content">{{ message.content }}</p>
                <button
                  v-for="image in imageAttachments(message)"
                  :key="image.name"
                  class="message-image"
                  @click="previewImage = image"
                >
                  <img :src="attachmentUrl(image)" :alt="image.name" />
                  <span>{{ image.name }}</span>
                </button>
                <a
                  v-for="file in fileAttachments(message)"
                  :key="file.name"
                  :href="attachmentUrl(file)"
                  :download="file.name"
                  class="message-file"
                >
                  <span>{{ fileExtension(file.name) }}</span>
                  <strong>{{ file.name }}</strong>
                  <small>{{ formatFileSize(file.size) }}</small>
                </a>
              </div>
              <time>{{ message.time }}</time>
            </article>
            <div v-if="!messagesStore.state.loading && !messagesStore.state.messages.length" class="thread-empty">
              还没有私信，发送第一条科研问候吧。
            </div>
          </div>

          <footer class="message-composer">
            <div v-if="attachments.length" class="composer-preview">
              <article v-for="(file, index) in attachments" :key="`${file.name}-${index}`" :class="{ image: isImage(file) }">
                <img v-if="isImage(file)" :src="attachmentUrl(file)" :alt="file.name" />
                <span v-else class="preview-file-icon">{{ fileExtension(file.name) }}</span>
                <div>
                  <strong>{{ file.name }}</strong>
                  <small>{{ formatFileSize(file.size) }}</small>
                </div>
                <button @click="attachments.splice(index, 1)">×</button>
              </article>
            </div>
            <div class="composer-input">
              <textarea v-model="content" rows="3" placeholder="输入消息，Enter 发送，Shift + Enter 换行" @keydown.enter.exact.prevent="send"></textarea>
            </div>
            <div class="composer-toolbar">
              <div>
                <label class="attach-button image-upload">
                  <input type="file" accept="image/*" multiple @change="onFiles" />
                  <span>▧</span> 图片
                </label>
                <label class="attach-button">
                  <input type="file" multiple @change="onFiles" />
                  <span>⌕</span> 附件
                </label>
                <small>最多 6 个文件，单个不超过 8MB</small>
              </div>
              <button class="send-button" :disabled="sending || (!content.trim() && !attachments.length)" @click="send">
                {{ sending ? "发送中..." : "发送" }} <span>↑</span>
              </button>
            </div>
          </footer>
        </template>
        <div v-else class="conversation-empty">
          <span>✉</span>
          <h2>选择一位联系人</h2>
          <p>你的站内私信、附件和未读消息会显示在这里。</p>
        </div>
      </main>
    </section>

    <div v-if="previewImage" class="image-lightbox" @click="previewImage = null">
      <div @click.stop>
        <header>
          <strong>{{ previewImage.name }}</strong>
          <a :href="attachmentUrl(previewImage)" :download="previewImage.name">下载原图</a>
          <button @click="previewImage = null">×</button>
        </header>
        <img :src="attachmentUrl(previewImage)" :alt="previewImage.name" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useMessagesStore } from "../stores/messages";

const route = useRoute();
const messagesStore = useMessagesStore();
const keyword = ref("");
const content = ref("");
const attachments = ref([]);
const sending = ref(false);
const threadElement = ref(null);
const previewImage = ref(null);
const activePanel = ref("chats");

const filteredContacts = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  if (!term) return messagesStore.state.contacts;
  return messagesStore.state.contacts.filter(item =>
    `${item.name} ${item.email} ${item.role}`.toLowerCase().includes(term),
  );
});

const activeContact = computed(() =>
  messagesStore.state.contacts.find(item => item.userId === messagesStore.state.activeContactId),
);
const hasFriendRequests = computed(() =>
  messagesStore.state.friendRequests.incoming.length || messagesStore.state.friendRequests.outgoing.length,
);

onMounted(async () => {
  const [contacts] = await Promise.all([messagesStore.fetchContacts(), messagesStore.fetchFriendRequests()]);
  const requested = Number(route.query.contact);
  const firstId = contacts.some(item => item.userId === requested) ? requested : contacts[0]?.userId;
  if (firstId) await selectContact(firstId);
});

function refreshFriendRequests() {
  messagesStore.fetchFriendRequests().catch(() => {});
}

onMounted(() => window.addEventListener("paperpilot:friend-requests-changed", refreshFriendRequests));
onUnmounted(() => window.removeEventListener("paperpilot:friend-requests-changed", refreshFriendRequests));

async function selectContact(userId) {
  await messagesStore.openThread(userId);
  await nextTick();
  if (threadElement.value) threadElement.value.scrollTop = threadElement.value.scrollHeight;
}

async function handleRequest(requestId, action) {
  await messagesStore.handleFriendRequest(requestId, action);
}

function fileToData(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve({ name: file.name, size: file.size, type: file.type, dataUrl: reader.result });
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

async function onFiles(event) {
  const remaining = Math.max(0, 6 - attachments.value.length);
  const files = Array.from(event.target.files || [])
    .filter(file => file.size <= 8 * 1024 * 1024)
    .slice(0, remaining);
  attachments.value.push(...await Promise.all(files.map(fileToData)));
  event.target.value = "";
}

async function send() {
  if (sending.value) return;
  sending.value = true;
  try {
    await messagesStore.sendMessage({ content: content.value.trim(), attachments: attachments.value });
    content.value = "";
    attachments.value = [];
    await nextTick();
    if (threadElement.value) threadElement.value.scrollTop = threadElement.value.scrollHeight;
  } finally {
    sending.value = false;
  }
}

function formatFileSize(size) {
  if (!size) return "";
  return size < 1024 * 1024 ? `${Math.ceil(size / 1024)}KB` : `${(size / 1024 / 1024).toFixed(1)}MB`;
}

function attachmentUrl(file) {
  return file?.dataUrl || file?.data || "";
}

function isImage(file) {
  return String(file?.type || "").startsWith("image/") || /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(file?.name || "");
}

function imageAttachments(message) {
  return (message.attachments || []).filter(isImage);
}

function fileAttachments(message) {
  return (message.attachments || []).filter(file => !isImage(file));
}

function fileExtension(name) {
  const extension = String(name || "").split(".").pop();
  return extension && extension.length <= 5 ? extension.toUpperCase() : "FILE";
}
</script>



<style scoped>

/* ════════════════════════════════════════════════════════════
   MESSAGES VIEW — COMPLETE DUAL-THEME & FLEX LAYOUT FIX
   ════════════════════════════════════════════════════════════ */

.messages-page {
  --c-bg:      #f8fafc;
  --c-surface: #ffffff;
  --c-border:  #e2e8f0;
  --c-text:    #0f172a;
  --c-muted:   #475569;
  --c-subtle:  #94a3b8;
  --c-accent:  #6366f1;
  --c-accent2: #a855f7;
  --sh-sm: 0 4px 20px rgba(15, 23, 42, 0.05);
  --sh-md: 0 12px 40px rgba(15, 23, 42, 0.08);

  max-width: 100% !important;
  margin: 0 auto !important;
  padding: 24px clamp(16px, 4vw, 48px) 80px !important;
  min-height: 100vh !important;
  background: var(--c-bg) !important;
  color: var(--c-text) !important;
  font-family: Inter, "PingFang SC", system-ui, sans-serif !important;
  box-sizing: border-box !important;
}

:root[data-theme="dark"] .messages-page {
  --c-bg:      #09090e;
  --c-surface: #111827;
  --c-border:  rgba(255, 255, 255, 0.08);
  --c-text:    #f1f5f9;
  --c-muted:   #94a3b8;
  --c-subtle:  #64748b;
  --sh-sm: 0 4px 20px rgba(0, 0, 0, 0.3);
  --sh-md: 0 12px 40px rgba(0, 0, 0, 0.5);
}

/* Page Header */
.messages-heading {
  display: flex !important;
  align-items: flex-end !important;
  justify-content: space-between !important;
  gap: 20px !important;
  margin-bottom: 20px !important;
}

.messages-heading span {
  font-size: 11px !important;
  font-weight: 900 !important;
  letter-spacing: 1.5px !important;
  color: var(--c-accent) !important;
  text-transform: uppercase !important;
}

.messages-heading h1 {
  margin: 4px 0 !important;
  font-size: 26px !important;
  font-weight: 950 !important;
  color: var(--c-text) !important;
}

.messages-heading p {
  margin: 0 !important;
  font-size: 13px !important;
  color: var(--c-muted) !important;
}

.heading-stat {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  padding: 8px 16px !important;
  border-radius: 999px !important;
  background: var(--c-surface) !important;
  border: 1px solid var(--c-border) !important;
  box-shadow: var(--sh-sm) !important;
}

.online-dot {
  width: 8px !important;
  height: 8px !important;
  border-radius: 50% !important;
  background: #10b981 !important;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2) !important;
}

.heading-stat b {
  font-size: 12px !important;
  color: var(--c-muted) !important;
  font-weight: 750 !important;
}

.heading-stat strong {
  font-size: 12px !important;
  color: var(--c-accent) !important;
  font-weight: 850 !important;
  padding: 2px 8px !important;
  border-radius: 999px !important;
  background: rgba(99, 102, 241, 0.12) !important;
}

/* Shell Layout */
.message-shell {
  display: grid !important;
  grid-template-columns: 340px minmax(0, 1fr) !important;
  height: calc(100vh - 200px) !important;
  min-height: 620px !important;
  max-height: 820px !important;
  border-radius: 24px !important;
  overflow: hidden !important;
  background: var(--c-surface) !important;
  border: 1px solid var(--c-border) !important;
  box-shadow: var(--sh-md) !important;
}

/* Left Contacts & Requests Panel */
.contacts-panel {
  display: flex !important;
  flex-direction: column !important;
  border-right: 1px solid var(--c-border) !important;
  background: rgba(255, 255, 255, 0.02) !important;
  min-height: 0 !important;
}
:root[data-theme="light"] .contacts-panel {
  background: #f8fafc !important;
}

.message-tabs {
  display: grid !important;
  grid-template-columns: 1fr 1fr !important;
  gap: 6px !important;
  padding: 12px !important;
  border-bottom: 1px solid var(--c-border) !important;
  flex-shrink: 0 !important;
}

.message-tabs button {
  height: 34px !important;
  border-radius: 10px !important;
  border: none !important;
  background: transparent !important;
  color: var(--c-muted) !important;
  font-size: 12.5px !important;
  font-weight: 800 !important;
  cursor: pointer !important;
  transition: all 0.18s ease !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
}

.message-tabs button.active {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.3) !important;
}

.message-tabs b {
  padding: 1px 6px !important;
  border-radius: 999px !important;
  background: #ef4444 !important;
  color: #ffffff !important;
  font-size: 10px !important;
  font-weight: 900 !important;
}

.contact-search {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  padding: 10px 16px !important;
  border-bottom: 1px solid var(--c-border) !important;
  color: var(--c-muted) !important;
  flex-shrink: 0 !important;
}

.contact-search input {
  flex: 1 !important;
  border: none !important;
  background: transparent !important;
  color: var(--c-text) !important;
  font-size: 13px !important;
  outline: none !important;
}

.contact-list {
  flex: 1 !important;
  min-height: 0 !important;
  overflow-y: auto !important;
  padding: 10px !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 6px !important;
}

.contact-list button {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  padding: 12px !important;
  border-radius: 14px !important;
  border: 1px solid transparent !important;
  background: transparent !important;
  color: var(--c-text) !important;
  cursor: pointer !important;
  text-align: left !important;
  transition: all 0.18s ease !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.contact-list button:hover {
  background: rgba(99, 102, 241, 0.06) !important;
}

.contact-list button.active {
  background: rgba(99, 102, 241, 0.12) !important;
  border-color: rgba(99, 102, 241, 0.3) !important;
}

.contact-avatar, .request-avatar {
  width: 40px !important;
  height: 40px !important;
  border-radius: 50% !important;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #ffffff !important;
  display: grid !important;
  place-items: center !important;
  font-size: 15px !important;
  font-weight: 900 !important;
  flex-shrink: 0 !important;
}

.contact-copy {
  flex: 1 !important;
  min-width: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 2px !important;
}

.contact-copy strong {
  font-size: 13.5px !important;
  font-weight: 850 !important;
  color: var(--c-text) !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.contact-copy small {
  font-size: 11.5px !important;
  color: var(--c-muted) !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.contact-meta {
  display: flex !important;
  flex-direction: column !important;
  align-items: flex-end !important;
  gap: 4px !important;
  flex-shrink: 0 !important;
}

.contact-meta time {
  font-size: 10.5px !important;
  color: var(--c-subtle) !important;
}

.contact-meta b {
  padding: 1px 6px !important;
  border-radius: 999px !important;
  background: #ef4444 !important;
  color: #ffffff !important;
  font-size: 10px !important;
  font-weight: 900 !important;
}

/* Friend Requests List Fix (No text stacking!) */
.request-list {
  flex: 1 !important;
  min-height: 0 !important;
  overflow-y: auto !important;
  padding: 12px !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 16px !important;
}

.request-list h3 {
  font-size: 11px !important;
  font-weight: 850 !important;
  color: var(--c-muted) !important;
  margin: 0 0 8px 0 !important;
  text-transform: uppercase !important;
  letter-spacing: 0.5px !important;
}

.request-list article {
  display: flex !important;
  flex-direction: column !important;
  gap: 10px !important;
  padding: 14px !important;
  border-radius: 14px !important;
  background: var(--c-surface) !important;
  border: 1px solid var(--c-border) !important;
  margin-bottom: 8px !important;
  box-shadow: var(--sh-sm) !important;
}

.request-list article > div {
  display: flex !important;
  flex-direction: column !important;
  gap: 4px !important;
  min-width: 0 !important;
}

.request-list strong {
  font-size: 13.5px !important;
  font-weight: 850 !important;
  color: var(--c-text) !important;
  display: block !important;
}

.request-list small {
  font-size: 11.5px !important;
  color: var(--c-muted) !important;
  display: block !important;
  line-height: 1.4 !important;
}

.request-list time {
  font-size: 10.5px !important;
  color: var(--c-subtle) !important;
  display: block !important;
  margin-top: 2px !important;
}

.request-list footer {
  display: flex !important;
  align-items: center !important;
  justify-content: flex-end !important;
  gap: 8px !important;
  margin-top: 4px !important;
}

.request-list footer button {
  height: 28px !important;
  padding: 0 14px !important;
  border-radius: 999px !important;
  border: 1px solid var(--c-border) !important;
  background: var(--c-bg) !important;
  color: var(--c-muted) !important;
  font-size: 11.5px !important;
  font-weight: 750 !important;
  cursor: pointer !important;
}

.request-list footer button.accept {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}

.pending-pill {
  display: inline-flex !important;
  align-items: center !important;
  padding: 3px 10px !important;
  border-radius: 999px !important;
  background: rgba(245, 158, 11, 0.15) !important;
  color: #fbbf24 !important;
  border: 1px solid rgba(245, 158, 11, 0.3) !important;
  font-size: 11px !important;
  font-weight: 850 !important;
  width: fit-content !important;
  margin-top: 4px !important;
}

/* Right Conversation Panel & Composer Fix */
.conversation-panel {
  display: flex !important;
  flex-direction: column !important;
  height: 100% !important;
  min-height: 0 !important;
  background: var(--c-surface) !important;
}

.conversation-head {
  display: flex !important;
  align-items: center !important;
  gap: 14px !important;
  padding: 16px 24px !important;
  border-bottom: 1px solid var(--c-border) !important;
  background: var(--c-surface) !important;
  flex-shrink: 0 !important;
}

.conversation-head strong {
  font-size: 15px !important;
  font-weight: 900 !important;
  color: var(--c-text) !important;
}

.conversation-head small {
  font-size: 12px !important;
  color: var(--c-muted) !important;
}

.conversation-status {
  margin-left: auto !important;
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
  padding: 4px 12px !important;
  border-radius: 999px !important;
  background: rgba(16, 185, 129, 0.1) !important;
  color: #10b981 !important;
  font-size: 11.5px !important;
  font-weight: 800 !important;
  border: 1px solid rgba(16, 185, 129, 0.2) !important;
}

.conversation-status i {
  width: 6px !important;
  height: 6px !important;
  border-radius: 50% !important;
  background: #10b981 !important;
}

/* Message Thread Scroll */
.message-thread {
  flex: 1 !important;
  min-height: 0 !important;
  overflow-y: auto !important;
  padding: 20px 24px !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 16px !important;
  background: var(--c-bg) !important;
}

.thread-date {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  color: var(--c-subtle) !important;
  font-size: 11px !important;
  margin: 8px 0 !important;
}

.thread-date::before, .thread-date::after {
  content: "" !important;
  height: 1px !important;
  flex: 1 !important;
  background: var(--c-border) !important;
}

.message-thread article {
  max-width: 75% !important;
  align-self: flex-start !important;
  display: flex !important;
  gap: 10px !important;
}

.message-thread article.mine {
  align-self: flex-end !important;
  flex-direction: row-reverse !important;
}

.message-avatar {
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #ffffff !important;
  display: grid !important;
  place-items: center !important;
  font-size: 12px !important;
  font-weight: 800 !important;
  flex-shrink: 0 !important;
}

.message-bubble {
  padding: 12px 16px !important;
  border-radius: 18px !important;
  font-size: 13.5px !important;
  line-height: 1.6 !important;
  background: var(--c-surface) !important;
  border: 1px solid var(--c-border) !important;
  color: var(--c-text) !important;
  box-shadow: var(--sh-sm) !important;
}

.mine .message-bubble {
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}

.message-bubble p {
  margin: 0 !important;
  white-space: pre-wrap !important;
}

.message-thread article time {
  font-size: 10px !important;
  color: var(--c-subtle) !important;
  margin-top: 4px !important;
  display: block !important;
}

/* Composer Area (Fixed Toolbar Visibility!) */
.message-composer {
  flex-shrink: 0 !important;
  padding: 16px 24px !important;
  border-top: 1px solid var(--c-border) !important;
  background: var(--c-surface) !important;
}

.composer-input textarea {
  width: 100% !important;
  height: 64px !important;
  padding: 10px 14px !important;
  border-radius: 12px !important;
  border: 1px solid var(--c-border) !important;
  background: var(--c-bg) !important;
  color: var(--c-text) !important;
  font-size: 13.5px !important;
  outline: none !important;
  resize: none !important;
  box-sizing: border-box !important;
}

.composer-toolbar {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  margin-top: 10px !important;
}

.composer-toolbar > div {
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
}

.attach-button {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
  padding: 6px 14px !important;
  border-radius: 999px !important;
  background: var(--c-bg) !important;
  color: var(--c-muted) !important;
  border: 1px solid var(--c-border) !important;
  font-size: 12px !important;
  font-weight: 750 !important;
  cursor: pointer !important;
}
.attach-button input { display: none !important; }

.send-button {
  height: 36px !important;
  padding: 0 20px !important;
  border-radius: 999px !important;
  background: linear-gradient(135deg, var(--c-accent), var(--c-accent2)) !important;
  color: #ffffff !important;
  font-size: 13px !important;
  font-weight: 850 !important;
  border: none !important;
  cursor: pointer !important;
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35) !important;
  transition: all 0.2s ease !important;
}
.send-button:hover {
  transform: translateY(-1px) !important;
  box-shadow: 0 8px 20px rgba(99, 102, 241, 0.45) !important;
}
.send-button:disabled {
  opacity: 0.5 !important;
  cursor: not-allowed !important;
  transform: none !important;
}
</style>